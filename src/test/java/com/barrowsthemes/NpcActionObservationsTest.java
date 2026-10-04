package com.barrowsthemes;

import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NpcActionObservationsTest
{
	@Test public void recordsResolvedIdsAndActionChangesWithoutCallingThemAttacksOrResolvingModels()
	{
		NPC npc = mock(NPC.class); NPCComposition child = mock(NPCComposition.class);
		when(npc.getId()).thenReturn(42); when(child.getId()).thenReturn(43); when(npc.getTransformedComposition()).thenReturn(child);
		when(npc.getName()).thenReturn("Example"); when(npc.getAnimation()).thenReturn(100);
		NpcActionObservations observations = new NpcActionObservations(); observations.record(npc, 1); observations.record(npc, 2);
		String report = observations.summary();
		assertTrue(report.contains("NPC #42 -> #43")); assertTrue(report.contains("changes=2"));
		assertTrue(report.contains("not automatically classified as attacks"));
		verify(npc, never()).getModel(); verify(child, never()).getModels();
		observations.clear(); assertTrue(observations.summary().contains("No actions recorded"));
	}
	@Test public void retainsAtMost256CombinationsAndIgnoresNoAction()
	{
		NPC npc = mock(NPC.class); when(npc.getId()).thenReturn(42);
		NpcActionObservations observations = new NpcActionObservations();
		for (int i = 0; i < 300; i++) { when(npc.getAnimation()).thenReturn(i); observations.record(npc, i); }
		String report = observations.summary();
		assertEquals(256, report.split("NPC #", -1).length - 1);
		assertFalse(report.contains("action=0;")); assertTrue(report.contains("action=299;"));
		when(npc.getAnimation()).thenReturn(-1); observations.record(npc, 301); assertEquals(report, observations.summary());
	}
}
