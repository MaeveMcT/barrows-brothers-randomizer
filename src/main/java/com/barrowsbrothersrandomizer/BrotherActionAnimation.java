package com.barrowsbrothersrandomizer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;

/** Optional action layer synchronized to the real brother, not independently looped. */
final class BrotherActionAnimation extends AnimationController
{
	private final Client client;
	private final NPC brother;
	private final Set<Integer> unsupported = new HashSet<>();
	private final Set<Integer> attempted = new HashSet<>();
	private final Cache<Integer, Animation> loaded = CacheBuilder.newBuilder().maximumSize(32).build();
	private int action = -1;
	private int attemptTick = Integer.MIN_VALUE;

	BrotherActionAnimation(Client client, NPC brother)
	{
		super(client, (Animation) null);
		this.client = client; this.brother = brother;
	}

	@Override
	public void tick(int ticks) { /* Playback belongs to the real NPC. */ }

	/** Discover/load actions outside rendering, with at most one attempt per action per game tick. */
	void prepare()
	{
		int id = brother.getAnimation();
		if (id != action)
		{
			action = id;
			setAnimation(null);
		}
		if (id < 0 || unsupported.contains(id)) { return; }
		int tick = client.getTickCount();
		if (attemptTick != tick) { attemptTick = tick; attempted.clear(); }
		if (getAnimation() == null)
		{
			try
			{
				Animation animation = loaded.getIfPresent(id);
				if (animation == null && attempted.add(id)) { animation = client.loadAnimation(id); }
				if (animation != null) { loaded.put(id, animation); setAnimation(animation); }
			}
			catch (RuntimeException ex) { fail(id); }
		}
	}

	@Override
	public Model animate(Model model, AnimationController other)
	{
		int id = brother.getAnimation();
		if (id < 0 || id != action || unsupported.contains(id)) { return model; }
		try
		{
			Animation animation = getAnimation();
			int frame = brother.getAnimationFrame();
			if (animation == null || frame < 0 || frame >= animation.getNumFrames()) { return model; }
			Model animated = client.applyTransformations(model, animation, frame, null, 0);
			return animated != null ? animated : model;
		}
		catch (RuntimeException ex)
		{
			fail(id);
			return model;
		}
	}

	private void fail(int id)
	{
		unsupported.add(id);
		setAnimation(null);
	}
}
