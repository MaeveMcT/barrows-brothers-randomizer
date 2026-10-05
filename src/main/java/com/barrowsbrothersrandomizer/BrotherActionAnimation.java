package com.barrowsbrothersrandomizer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;

/** Optional action layer synchronized to the real brother, not independently looped. */
@Slf4j
final class BrotherActionAnimation extends AnimationController
{
	private final Client client;
	private final NPC brother;
	private final Set<Integer> unsupported = new HashSet<>();
	private final Set<Integer> attempted = new HashSet<>();
	private final Cache<Integer, Animation> loaded = CacheBuilder.newBuilder().maximumSize(32).build();
	private int action = -1;
	private int attemptTick = Integer.MIN_VALUE;
	private String note = "No brother action attempted";
	String summary() { return note; }

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
			catch (RuntimeException ex) { fail(id, ex); }
		}
	}

	@Override
	public Model animate(Model model, AnimationController other)
	{
		int id = brother.getAnimation();
		if (id < 0) { note = "No current brother action"; return model; }
		if (id != action) { note = "Waiting for brother action preparation #" + id; return model; }
		if (unsupported.contains(id)) { note = "Brother action #" + id + " failed; suppressed for this disguise"; return model; }
		try
		{
			Animation animation = getAnimation();
			int frame = brother.getAnimationFrame();
			if (animation == null) { note = "Waiting for brother action #" + id; return model; }
			if (frame < 0 || frame >= animation.getNumFrames()) { note = "Invalid brother frame " + frame + " for action #" + id; return model; }
			Model animated = client.applyTransformations(model, animation, frame, null, 0);
			note = animated == null || animated == model ? "Brother action #" + id + " returned no animated model" : "Brother action #" + id + ", frame " + frame;
			return animated != null ? animated : model;
		}
		catch (RuntimeException ex)
		{
			fail(id, ex);
			return model;
		}
	}

	private void fail(int id, RuntimeException ex)
	{
		unsupported.add(id);
		setAnimation(null);
		note = "Brother action #" + id + " failed: " + ex.getClass().getSimpleName();
		log.debug("Brother action {} incompatible with disguise; retaining native gait/static pose", id, ex);
	}
}
