package com.barrowsthemes;

import java.util.HashMap;
import java.util.Map;

/** Reviewed rules, keyed by resolved NPC definition and real brother action. Never learns from a nonthrowing transform. */
final class NpcActionOverrides
{
	static final int NONE = -1, ALLOW = -2, DENY = -3;
	private final Map<Long, Integer> rules = new HashMap<>();
	private int invalid;

	NpcActionOverrides(String text)
	{
		if (text == null || text.trim().isEmpty()) { return; }
		if (text.length() > 16384) { invalid++; return; }
		String[] entries = text.split(";", -1);
		if (entries.length > 128) { invalid++; return; }
		for (String entry : entries)
		{
			if (entry.trim().isEmpty()) { continue; }
			try
			{
				String[] assignment = entry.trim().split("=", -1);
				if (assignment.length != 2) { throw new IllegalArgumentException(); }
				String[] pair = assignment[0].trim().split(":", -1);
				if (pair.length != 2) { throw new IllegalArgumentException(); }
				int npc = number(pair[0]), action = pair[1].trim().equals("*") ? -1 : number(pair[1]);
				String value = assignment[1].trim();
				int rule = value.equalsIgnoreCase("allow") ? ALLOW : value.equalsIgnoreCase("deny") ? DENY : number(value);
				rules.put(key(npc, action), rule); // Last duplicate wins; exact action beats wildcard.
			}
			catch (IllegalArgumentException ex) { invalid++; }
		}
	}

	private static int number(String text)
	{
		int value = Integer.parseInt(text.trim());
		if (value < 0) { throw new IllegalArgumentException(); }
		return value;
	}
	private static long key(int npc, int action) { return ((long) npc << 32) | (action & 0xffffffffL); }
	int rule(int npc, int action)
	{
		if (action < 0) { return NONE; }
		return rules.getOrDefault(key(npc, action), rules.getOrDefault(key(npc, -1), NONE));
	}
	String summary() { return rules.size() + " action overrides; " + invalid + " invalid entries ignored"; }
}
