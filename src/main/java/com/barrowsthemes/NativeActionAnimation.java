package com.barrowsthemes;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.NPC;

/** A mapped native attack uses its own duration/frames, triggered by the brother's action cycle. */
final class NativeActionAnimation extends AnimationController
{
	private final Client client;
	private final NPC brother;
	private final Set<Integer> failed = new HashSet<>();
	private int sequence = -1, brotherAction = -1, brotherFrame = -1, attempt = Integer.MIN_VALUE;
	private boolean finished;
	private String note = "No mapped native attack";

	NativeActionAnimation(Client client, NPC brother)
	{
		super(client, (Animation) null);
		this.client = client; this.brother = brother;
		setOnFinished(controller -> { finished = true; controller.setAnimation(null); });
	}

	void restart()
	{
		brotherAction = -1; brotherFrame = -1; finished = false; attempt = Integer.MIN_VALUE; setAnimation(null);
	}

	void select(int id)
	{
		int action = brother.getAnimation(), frame = brother.getAnimationFrame();
		if (id != sequence || action != brotherAction || (frame >= 0 && brotherFrame >= 0 && frame < brotherFrame))
		{
			sequence = id; brotherAction = action; finished = false; attempt = Integer.MIN_VALUE; setAnimation(null);
		}
		brotherFrame = frame;
		if (id < 0) { note = "No mapped native attack for current brother action"; return; }
		if (failed.contains(id)) { note = "Native attack #" + id + " failed; gait fallback"; return; }
		if (finished) { note = "Native attack #" + id + " finished; awaiting next attack cycle"; return; }
		if (getAnimation() == null && attempt != client.getTickCount())
		{
			attempt = client.getTickCount();
			try { setAnimation(client.loadAnimation(id)); }
			catch (RuntimeException ex) { fail(); }
		}
		note = getAnimation() == null ? "Waiting for native attack #" + id : "Native attack #" + id + ", frame " + getFrame();
	}

	@Override public void tick(int ticks)
	{
		try { super.tick(ticks); }
		catch (RuntimeException ex) { fail(); }
	}
	@Override public Model animate(Model model, AnimationController other)
	{
		Animation animation = getAnimation();
		if (animation == null || getFrame() < 0 || getFrame() >= animation.getNumFrames()) { return model; }
		try
		{
			Model result = super.animate(model, null);
			note = result == null ? "Native attack #" + sequence + " returned no model" : "Native attack #" + sequence + ", frame " + getFrame();
			return result == null ? model : result;
		}
		catch (RuntimeException ex) { fail(); return model; }
	}
	private void fail() { failed.add(sequence); setAnimation(null); note = "Native attack #" + sequence + " failed; gait fallback"; }
	String summary() { return note; }
}
