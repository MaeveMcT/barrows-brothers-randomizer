package com.barrowsbrothersrandomizer;

import com.google.common.base.Preconditions;
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
	private final Set<Integer> attempted = new HashSet<>();
	private NativeNpcAnimations sequences;
	private NpcAnimationMode mode;
	private int metadataAttempt = Integer.MIN_VALUE, attemptTick = Integer.MIN_VALUE;
	private int preparedTick = Integer.MIN_VALUE, preparedAction = -1, preparedSequence = -1;
	private String evidence = "Compatibility not prepared";
	private boolean compatible;
	private int selected = -1;
	private State state = State.NOT_RENDERED;
	private boolean invalidFrame;
	private int actionId, actionFrame;

	NativeNpcAnimation(Client client, NPC brother, int sourceId, AnimationRigCache rigs, NpcAnimationMode mode)
	{
		super(client, (Animation) null);
		this.client = client; this.brother = brother; this.sourceId = sourceId; this.rigs = rigs;
		brotherActions = new BrotherActionAnimation(client, brother);
		options(mode);
	}

	void options(NpcAnimationMode mode)
	{
		Preconditions.checkNotNull(mode, "Animation mode");
		if (this.mode == mode) { return; }
		this.mode = mode;
		preparedTick = Integer.MIN_VALUE; preparedAction = -1; compatible = false; evidence = "Compatibility not prepared";
	}

	void actionChanged() { preparedTick = Integer.MIN_VALUE; compatible = false; }

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

	/** Called on client/game/animation ticks, never from rendering. */
	void prepare()
	{
		try
		{
			metadata();
			prepareGait();
			int action = brother.getAnimation(), tick = client.getTickCount();
			if (action != preparedAction || tick != preparedTick || selected != preparedSequence)
			{
				preparedAction = action; preparedTick = tick; preparedSequence = selected; compatible = false;
				if (action < 0) { evidence = "No current brother action"; }
				else if (mode == NpcAnimationMode.NATIVE) { evidence = "Native-only mode"; }
				else if (mode == NpcAnimationMode.FORCE) { evidence = "Forced brother action; no compatibility guarantee"; }
				else if (sequences == null || !sequences.complete()) { evidence = "Incomplete NPC metadata; automatic borrowing disabled"; }
				else
				{
					AnimationRigCache.Evidence result = rigs.compare(selected, action);
					compatible = result.compatible();
					evidence = result.description;
					if (sequences.sharesGaits(brother)) { evidence += "; matching idle/walk IDs (hint only)"; }
				}
			}
			if (borrow()) { brotherActions.prepare(); }
		}
		catch (RuntimeException ex)
		{
			compatible = false;
			evidence = "Animation preparation failed: " + ex.getClass().getSimpleName();
		}
	}

	private boolean borrow()
	{
		int action = brother.getAnimation();
		if (action < 0 || mode == NpcAnimationMode.NATIVE) { return false; }
		return mode == NpcAnimationMode.FORCE
			|| (preparedAction == action && preparedTick == client.getTickCount()
				&& preparedSequence == currentGait() && compatible);
	}

	private int currentGait() { return sequences == null ? -1 : sequences.sequenceFor(brother); }

	private void prepareGait()
	{
		int id = currentGait();
		if (id != selected) { selected = id; setAnimation(null); }
		if (id < 0 || unsupported.contains(id) || getAnimation() != null) { return; }
		int tick = client.getTickCount();
		if (attemptTick != tick) { attemptTick = tick; attempted.clear(); }
		Animation animation = loaded.get(id);
		if (animation == null && attempted.add(id)) { animation = client.loadAnimation(id); }
		if (animation != null) { loaded.put(id, animation); setAnimation(animation); }
	}

	@Override public void tick(int ticks)
	{
		try
		{
			prepare(); super.tick(ticks);
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
			// A changed pose waits for tick-owned preparation instead of discovering assets here.
			if (selected != currentGait()) { state = State.STATIC; return model; }
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
