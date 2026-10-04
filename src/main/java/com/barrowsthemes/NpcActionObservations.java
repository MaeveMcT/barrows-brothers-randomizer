package com.barrowsthemes;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

/** Optional bounded session notes. Action changes are evidence to review, never automatic attack labels. */
final class NpcActionObservations
{
	private final Map<Long, Observation> observations = new LinkedHashMap<>();
	void record(NPC npc, int tick)
	{
		int action = npc.getAnimation();
		if (action < 0) { return; }
		NPCComposition child = npc.getTransformedComposition();
		int resolved = child == null ? npc.getId() : child.getId();
		long key = ((long) resolved << 32) | (action & 0xffffffffL);
		Observation observation = observations.get(key);
		if (observation == null)
		{
			if (observations.size() >= 256) { observations.remove(observations.keySet().iterator().next()); }
			observation = new Observation(npc.getId(), resolved, action, npc.getName(), npc.getIdlePoseAnimation(), npc.getWalkAnimation());
			observations.put(key, observation);
		}
		observation.count++; observation.lastTick = tick;
		observation.targeted |= npc.getInteracting() != null;
	}
	String summary()
	{
		StringBuilder out = new StringBuilder("Observed NPC actions (session-only; not automatically classified as attacks):\n");
		if (observations.isEmpty()) { out.append("No actions recorded. Enable Record NPC actions and observe real NPC combat.\n"); }
		for (Observation observation : observations.values())
		{
			out.append("NPC #").append(observation.source).append(" -> #").append(observation.resolved).append(" ")
				.append(observation.name).append("; action=").append(observation.action).append("; changes=").append(observation.count)
				.append("; idle/walk=").append(observation.idle).append('/').append(observation.walk)
				.append("; targeted actor observed=").append(observation.targeted).append("; last game tick=").append(observation.lastTick).append('\n');
		}
		return out.append("Review attack vs hit/death/special actions before adding overrides; nothing is saved or uploaded.\n").toString();
	}
	void clear() { observations.clear(); }
	private static final class Observation
	{
		final int source, resolved, action, idle, walk;
		final String name;
		int count, lastTick;
		boolean targeted;
		Observation(int source, int resolved, int action, String name, int idle, int walk)
		{
			this.source = source; this.resolved = resolved; this.action = action; this.name = name;
			this.idle = idle; this.walk = walk;
		}
	}
}
