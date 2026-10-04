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

/** Native gait plus explicitly mapped attacks or evidence-gated brother actions. Never changes the real actor. */
@Slf4j
final class NativeNpcAnimation extends AnimationController
{
	private final Client client;
	private final NPC brother;
	private final int sourceId;
	private final BrotherActionAnimation brotherActions;
	private final NativeActionAnimation nativeActions;
	private final AnimationRigCache rigs;
	private final Map<Integer, Animation> loaded = new HashMap<>();
	private final Set<Integer> unsupported = new HashSet<>();
	private NativeNpcAnimations sequences;
	private NpcAnimationMode mode = NpcAnimationMode.NATIVE;
	private NpcActionOverrides overrides = new NpcActionOverrides("");
	private boolean nativeAttacks;
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
		nativeActions = new NativeActionAnimation(client, brother);
	}

	void options(NpcAnimationMode mode, boolean nativeAttacks, NpcActionOverrides overrides)
	{
		NpcAnimationMode next = mode == null ? NpcAnimationMode.NATIVE : mode;
		if (this.mode == next && this.nativeAttacks == nativeAttacks && this.overrides == overrides) { return; }
		this.mode = next; this.nativeAttacks = nativeAttacks; this.overrides = overrides;
		preparedTick = Integer.MIN_VALUE; preparedAction = -1; evidence = "Compatibility not prepared";
	}

	void actionChanged() { nativeActions.restart(); preparedTick = Integer.MIN_VALUE; }

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
			int rule = overrides.rule(sourceId, action);
			if (action < 0) { evidence = "No current brother action"; }
			else if (rule == NpcActionOverrides.DENY) { evidence = "Explicit deny override"; }
			else if (rule >= 0) { evidence = "Explicit native sequence override #" + rule; }
			else if (mode == NpcAnimationMode.NATIVE) { evidence = "Native-only mode"; }
			else if (rule == NpcActionOverrides.ALLOW) { evidence = "Explicit allow override (user-reviewed)"; }
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
		int action = brother.getAnimation(), rule = overrides.rule(sourceId, action);
		if (action < 0 || mode == NpcAnimationMode.NATIVE || rule == NpcActionOverrides.DENY || rule >= 0) { return false; }
		return mode == NpcAnimationMode.FORCE || rule == NpcActionOverrides.ALLOW
			|| (preparedAction == action && AnimationRigCache.shared(evidence));
	}

	private int nativeAttack()
	{
		if (!nativeAttacks || brother.getAnimation() < 0) { return -1; }
		int rule = overrides.rule(sourceId, brother.getAnimation());
		if (rule >= 0) { return rule; } // Explicitly reviewed mappings can classify additional actions.
		return NativeAttackMappings.brotherAttack(brother.getId(), brother.getAnimation()) ? NativeAttackMappings.attack(sourceId, sequences) : -1;
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
			nativeActions.select(nativeAttack()); nativeActions.tick(ticks);
		}
		catch (RuntimeException ex) { fallback(ex); }
	}

	@Override public Model animate(Model model, AnimationController other)
	{
		try
		{
			// Force preserves the old behaviour. Auto prefers a native attack where one is mapped.
			if (mode == NpcAnimationMode.FORCE && borrow())
			{
				Model action = borrowed(model);
				if (action != model) { return action; }
			}
			synchronize();
			nativeActions.select(nativeAttack());
			Model nativeAction = nativeActions.animate(model, null);
			if (nativeAction != model) { state = State.NATIVE_ATTACK; return nativeAction; }
			if (mode == NpcAnimationMode.AUTO && borrow())
			{
				Model action = borrowed(model);
				if (action != model) { return action; }
			}
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
		else if (state == State.NATIVE_ATTACK) { playback = nativeActions.summary(); }
		else if (state == State.NATIVE && getAnimation() != null && selected >= 0 && !unsupported.contains(selected)) { playback = "Native sequence #" + selected + ", frame " + getFrame(); }
		else if (sequences == null) { playback = "Static: waiting for NPC definition bytes"; }
		else if (selected < 0) { playback = "Static: no matching native gait sequence"; }
		else if (unsupported.contains(selected)) { playback = "Static: native sequence #" + selected + " failed"; }
		else if (getAnimation() == null) { playback = "Static: waiting for animation #" + selected; }
		else if (invalidFrame) { playback = "Static: invalid native frame for sequence #" + selected; }
		else { playback = "Static: native sequence #" + selected + " returned no animated model"; }
		return playback + "; mode=" + mode + "; " + (sequences == null ? "Metadata pending" : sequences.gaits() + "; " + sequences.note())
			+ "\n  Compatibility: " + evidence + "; action=" + brother.getAnimation()
			+ "\n  " + nativeActions.summary() + "; " + brotherActions.summary();
	}

	private enum State { NOT_RENDERED, BROTHER_ACTION, NATIVE_ATTACK, NATIVE, STATIC }
	private void fallback(RuntimeException ex)
	{
		if (selected >= 0) { unsupported.add(selected); }
		setAnimation(null); state = State.STATIC;
		log.debug("Native animation {} for NPC {} unavailable/incompatible; using static pose", selected, sourceId, ex);
	}
}
