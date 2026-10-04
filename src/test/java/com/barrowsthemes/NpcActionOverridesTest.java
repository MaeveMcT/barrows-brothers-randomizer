package com.barrowsthemes;

import org.junit.Test;
import static org.junit.Assert.*;

public class NpcActionOverridesTest
{
	@Test public void exactActionsBeatWildcardsAndNativeValuesAreDistinctFromBorrowRules()
	{
		NpcActionOverrides rules = new NpcActionOverrides("1173:*=deny; 1173:2075=5387; 42:9=allow");
		assertEquals(5387, rules.rule(1173, 2075));
		assertEquals(NpcActionOverrides.DENY, rules.rule(1173, 729));
		assertEquals(NpcActionOverrides.ALLOW, rules.rule(42, 9));
		assertEquals(NpcActionOverrides.NONE, rules.rule(1173, -1));
		assertEquals(NpcActionOverrides.NONE, rules.rule(43, 9));
	}
	@Test public void malformedNegativeOverflowAndOversizedRulesAreIgnoredSafely()
	{
		NpcActionOverrides rules = new NpcActionOverrides("x; -1:2=allow; 1:2=-4; 1:2=99999999999; 1:2=0; 1:2=deny");
		assertEquals(NpcActionOverrides.DENY, rules.rule(1, 2));
		assertTrue(rules.summary().contains("4 invalid"));
		assertEquals(NpcActionOverrides.NONE, new NpcActionOverrides("x".repeat(16385)).rule(1, 2));
		assertEquals(NpcActionOverrides.NONE, new NpcActionOverrides(null).rule(1, 2));
	}
}
