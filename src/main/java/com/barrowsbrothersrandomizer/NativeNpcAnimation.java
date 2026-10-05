package com.barrowsbrothersrandomizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.NPC;

/** Native gait plus evidence-gated brother actions. Never changes the real actor. */
@Slf4j
final class NativeNpcAnimation extends AnimationController
{
	private final Client client;
	private final NPC brother;
	private final int sourceId;
	private final BrotherActionAnimation brotherActions;
	private final AnimationRigCache rigs;
	private final Map<Integer, Animation> loaded = new HashMap<>();
	private final Set<Integer> unsupported = new HashSet<>();
	private NativeNpcAnimations sequences;
	private NpcAnimationMode mode = NpcAnimationMode.NATIVE;
	private int metadataAttempt = Integer.MIN_VALUE, animationAttempt = Integer.MIN_VALUE;
	private int preparedTick = Integer.MIN_VALUE, preparedAction = -1;
	private String evidence = "Compatibility not prepared";
	private int selected = -1;
	private State state = State.NOT_RENDERED;
	private boolean invalidFrame;
	private int actionId, actionFrame;

	NativeNpcAnimation(Client client, NPC brother, int sourceId) { this(client, brother, sourceId, false); }
	NativeNpcAnimation(Client client, NPC brother, int sourceId, boolean useBrotherActions)
	{
		this(client, brother, sourceId, new AnimationRigCache(client));
		mode = useBrotherActions ? NpcAnimationMode.FORCE : NpcAnimationMode.NATIVE;
	}
	NativeNpcAnimation(Client client, NPC brother, int sourceId, AnimationRigCache rigs)
	{
		super(client, (Animation) null);
		this.client = client; this.brother = brother; this.sourceId = sourceId; this.rigs = rigs;
		brotherActions = new BrotherActionAnimation(client, brother);
	}

	void options(NpcAnimationMode mode)
	{
		NpcAnimationMode next = mode == null ? NpcAnimationMode.NATIVE : mode;
		if (this.mode == next) { return; }
		this.mode = next;
		preparedTick = Integer.MIN_VALUE; preparedAction = -1; evidence = "Compatibility not prepared";
	}

	void actionChanged() { preparedTick = Integer.MIN_VALUE; }

	private void metadata()
	{
		int tick = client.getTickCount();
		if (sequences == null && metadataAttempt != tick)
		{
			metadataAttempt = tick;
			IndexDataBase configs = client.getIndexConfig();
			byte[] data = configs == null ? null : configs.loadData(9, sourceId);
			if (data != null) { sequences = NativeNpcAnimations.decode(data); }
		}
	}

	/** Called on client/game ticks, not from rendering. Missing assets retry once per game tick. */
	void prepare()
	{
		int action = brother.getAnimation(), tick = client.getTickCount();
		if (action == preparedAction && tick == preparedTick) { return; }
		preparedAction = action; preparedTick = tick;
		try
		{
			metadata();
			if (action < 0) { evidence = "No current brother action"; }
			else if (mode == NpcAnimationMode.NATIVE) { evidence = "Native-only mode"; }
			else if (mode == NpcAnimationMode.FORCE) { evidence = "Forced brother action; no compatibility guarantee"; }
			else if (sequences == null || !sequences.complete()) { evidence = "Incomplete NPC metadata; automatic borrowing disabled"; }
			else
			{
				evidence = rigs.compare(sequences.sequenceFor(brother), action);
				if (sequences.sharesGaits(brother)) { evidence += "; matching idle/walk IDs (hint only)"; }
			}
		}
		catch (RuntimeException ex) { evidence = "Metadata preparation failed: " + ex.getClass().getSimpleName(); }
	}

	private boolean borrow()
	{
		int action = brother.getAnimation();
		if (action < 0 || mode == NpcAnimationMode.NATIVE) { return false; }
		return mode == NpcAnimationMode.FORCE
			|| (preparedAction == action && AnimationRigCache.shared(evidence));
	}

	private void synchronize()
	{
		metadata();
		int id = sequences == null ? -1 : sequences.sequenceFor(brother);
		if (id != selected) { selected = id; animationAttempt = Integer.MIN_VALUE; setAnimation(null); }
		if (id < 0 || unsupported.contains(id)) { return; }
		int tick = client.getTickCount();
		if (getAnimation() == null && animationAttempt != tick)
		{
			animationAttempt = tick;
			Animation animation = loaded.get(id);
			if (animation == null) { animation = client.loadAnimation(id); }
			if (animation != null) { loaded.put(id, animation); setAnimation(animation); }
		}
	}

	@Override public void tick(int ticks)
	{
		try
		{
			synchronize(); super.tick(ticks);
		}
		catch (RuntimeException ex) { fallback(ex); }
	}

	@Override public Model animate(Model model, AnimationController other)
	{
		try
		{
			if (borrow())
			{
				Model action = borrowed(model);
				if (action != model) { return action; }
			}
			synchronize();
			Animation animation = getAnimation();
			invalidFrame = animation != null && (getFrame() < 0 || getFrame() >= animation.getNumFrames());
			if (animation == null || invalidFrame) { state = State.STATIC; return model; }
			Model animated = super.animate(model, null);
			state = animated == null || animated == model ? State.STATIC : State.NATIVE;
			return animated != null ? animated : model;
		}
		catch (RuntimeException ex) { fallback(ex); return model; }
	}

	private Model borrowed(Model model)
	{
		Model action = brotherActions.animate(model, null);
		if (action != model)
		{
			state = State.BROTHER_ACTION; actionId = brother.getAnimation(); actionFrame = brother.getAnimationFrame();
		}
		return action;
	}

	String summary()
	{
		String playback;
		if (state == State.NOT_RENDERED) { playback = "Not rendered yet"; }
		else if (state == State.BROTHER_ACTION) { playback = "Brother action #" + actionId + ", frame " + actionFrame; }
		else if (state == State.NATIVE && getAnimation() != null && selected >= 0 && !unsupported.contains(selected)) { playback = "Native sequence #" + selected + ", frame " + getFrame(); }
		else if (sequences == null) { playback = "Static: waiting for NPC definition bytes"; }
		else if (selected < 0) { playback = "Static: no matching native gait sequence"; }
		else if (unsupported.contains(selected)) { playback = "Static: native sequence #" + selected + " failed"; }
		else if (getAnimation() == null) { playback = "Static: waiting for animation #" + selected; }
		else if (invalidFrame) { playback = "Static: invalid native frame for sequence #" + selected; }
		else { playback = "Static: native sequence #" + selected + " returned no animated model"; }
		return playback + "; mode=" + mode + "; " + (sequences == null ? "Metadata pending" : sequences.gaits() + "; " + sequences.note())
			+ "\n  Compatibility: " + evidence + "; action=" + brother.getAnimation()
			+ "\n  " + brotherActions.summary();
	}

	private enum State { NOT_RENDERED, BROTHER_ACTION, NATIVE, STATIC }
	private void fallback(RuntimeException ex)
	{
		if (selected >= 0) { unsupported.add(selected); }
		setAnimation(null); state = State.STATIC;
		log.debug("Native animation {} for NPC {} unavailable/incompatible; using static pose", selected, sourceId, ex);
	}
}
