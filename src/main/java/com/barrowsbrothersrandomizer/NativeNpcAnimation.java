package com.barrowsbrothersrandomizer;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.NPC;

/** Native gait plus evidence-gated brother actions. Never changes the real actor. */
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
	private boolean compatible;
	private int selected = -1;

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
		preparedTick = Integer.MIN_VALUE; preparedAction = -1; compatible = false;
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
				compatible = mode == NpcAnimationMode.AUTO && action >= 0
					&& sequences != null && sequences.complete()
					&& rigs.compare(selected, action) == AnimationRigCache.Compatibility.COMPATIBLE;
			}
			if (borrow()) { brotherActions.prepare(); }
		}
		catch (RuntimeException ex)
		{
			compatible = false;
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
		catch (RuntimeException ex) { fallback(); }
	}

	@Override public Model animate(Model model, AnimationController other)
	{
		try
		{
			if (borrow())
			{
				Model action = brotherActions.animate(model, null);
				if (action != model) { return action; }
			}
			// A changed pose waits for tick-owned preparation instead of discovering assets here.
			if (selected != currentGait()) { return model; }
			Animation animation = getAnimation();
			if (animation == null || getFrame() < 0 || getFrame() >= animation.getNumFrames()) { return model; }
			Model animated = super.animate(model, null);
			return animated != null ? animated : model;
		}
		catch (RuntimeException ex) { fallback(); return model; }
	}

	private void fallback()
	{
		if (selected >= 0) { unsupported.add(selected); }
		setAnimation(null);
	}
}
