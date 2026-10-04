package com.barrowsthemes;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import static org.junit.Assert.*;

public class NativeAttackMappingsTest
{
	static byte[] metadata(int idle, int walk)
	{
		return new byte[] {13, (byte) (idle >>> 8), (byte) idle, 14, (byte) (walk >>> 8), (byte) walk, 0};
	}
	@Test public void mapsOnlyExplicitIdsWithTheirCurrentCacheCheckedGaitFamily()
	{
		int[][] families = {
			{NpcID.CHICKEN, AnimationID.LORE_CHICKEN_READY, AnimationID.LORE_CHICKEN_WALK, AnimationID.LORE_CHICKEN_ATTACK},
			{NpcID.COW, AnimationID.COW_JUST_READY_UPDATE, AnimationID.COW_UPDATE_WALK, AnimationID.COW_UPDATE_ATTACK},
			{NpcID.SMALL_BAT, AnimationID.BAT_REWORK_READY, AnimationID.BAT_REWORK_FLYING, AnimationID.BAT_REWORK_ATTACK},
			{NpcID.BROWNBEAR, AnimationID.BEAR_REWORK_READY, AnimationID.BEAR_REWORK_WALK, AnimationID.BEAR_REWORK_ATTACK},
			{NpcID.GIANTRAT, AnimationID.GIANT_RAT_UPDATE_READY, AnimationID.GIANT_RAT_UPDATE_WALK, AnimationID.GIANT_RAT_UPDATE_ATTACK}
		};
		for (int[] family : families)
		{
			assertEquals(family[3], NativeAttackMappings.attack(family[0], NativeNpcAnimations.decode(metadata(family[1], family[2]))));
			assertEquals(-1, NativeAttackMappings.attack(family[0], NativeNpcAnimations.decode(metadata(100, 101))));
			assertEquals(-1, NativeAttackMappings.attack(90000, NativeNpcAnimations.decode(metadata(family[1], family[2]))));
		}
		assertEquals(-1, NativeAttackMappings.attack(NpcID.CHICKEN, NativeNpcAnimations.decode(new byte[] {13, 0})));
	}
	@Test public void brotherClassificationDoesNotMistakeDefendDeathOrOtherNpcsForAttacks()
	{
		assertTrue(NativeAttackMappings.brotherAttack(NpcID.BARROWS_DHAROK, AnimationID.BARROW_DHAROK_CRUSH));
		assertTrue(NativeAttackMappings.brotherAttack(NpcID.BARROWS_KARIL, AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE));
		assertFalse(NativeAttackMappings.brotherAttack(NpcID.BARROWS_GUTHAN, AnimationID.BARROW_GUTHAN_DEFEND));
		assertFalse(NativeAttackMappings.brotherAttack(NpcID.CHICKEN, AnimationID.BARROW_DHAROK_CRUSH));
		assertFalse(NativeAttackMappings.brotherAttack(NpcID.BARROWS_DHAROK, -1));
	}
}
