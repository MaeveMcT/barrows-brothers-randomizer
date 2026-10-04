package com.barrowsthemes;

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
	private int action = -1;
	private int lastAttemptTick = Integer.MIN_VALUE;
	private String note = "No brother action attempted";
	String summary() { return note; }

	BrotherActionAnimation(Client client, NPC brother)
	{
		super(client, (Animation) null);
		this.client = client; this.brother = brother;
	}

	@Override
	public void tick(int ticks) { /* Playback belongs to the real NPC. */ }

	@Override
	public Model animate(Model model, AnimationController other)
	{
		int id = brother.getAnimation();
		if (id != action)
		{
			action = id; lastAttemptTick = Integer.MIN_VALUE;
			setAnimation(null);
		}
		if (id < 0) { note = "No current brother action"; return model; }
		if (unsupported.contains(id)) { note = "Brother action #" + id + " failed; suppressed for this disguise"; return model; }
		try
		{
			int tick = client.getTickCount();
			if (getAnimation() == null && lastAttemptTick != tick)
			{
				lastAttemptTick = tick;
				setAnimation(client.loadAnimation(id));
			}
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
			unsupported.add(id);
			setAnimation(null);
			note = "Brother action #" + id + " failed: " + ex.getClass().getSimpleName();
			log.debug("Brother action {} incompatible with disguise; retaining native gait/static pose", id, ex);
			return model;
		}
	}
}
