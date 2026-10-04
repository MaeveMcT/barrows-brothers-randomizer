package com.barrowsbrothersrandomizer;

import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;

/** Small source-backed mapping, guarded by the cache-checked gait family. Not universal attack discovery. */
final class NativeAttackMappings
{
	private NativeAttackMappings() { }

	static int attack(int npc, NativeNpcAnimations metadata)
	{
		if (metadata == null || !metadata.complete()) { return -1; }
		switch (npc)
		{
			case NpcID.CHICKEN: case NpcID.CHICKEN_BROWN:
			case NpcID.FARM_CHICKEN_BROWN: case NpcID.FARM_CHICKEN_DARK_BROWN: case NpcID.FARM_CHICKEN_TAN:
				return family(metadata, AnimationID.LORE_CHICKEN_READY, AnimationID.LORE_CHICKEN_WALK, AnimationID.LORE_CHICKEN_ATTACK);
			case NpcID.COW: case NpcID.COW2: case NpcID.COW3:
				return family(metadata, AnimationID.COW_JUST_READY_UPDATE, AnimationID.COW_UPDATE_WALK, AnimationID.COW_UPDATE_ATTACK);
			case NpcID.SMALL_BAT: case NpcID.BAT:
				return family(metadata, AnimationID.BAT_REWORK_READY, AnimationID.BAT_REWORK_FLYING, AnimationID.BAT_REWORK_ATTACK);
			case NpcID.BROWNBEAR: case NpcID.DARKBEAR:
				return family(metadata, AnimationID.BEAR_REWORK_READY, AnimationID.BEAR_REWORK_WALK, AnimationID.BEAR_REWORK_ATTACK);
			case NpcID.GIANTRAT: case NpcID.GIANTRAT_GREY:
				return family(metadata, AnimationID.GIANT_RAT_UPDATE_READY, AnimationID.GIANT_RAT_UPDATE_WALK, AnimationID.GIANT_RAT_UPDATE_ATTACK);
			default: return -1;
		}
	}

	private static int family(NativeNpcAnimations data, int idle, int walk, int attack)
	{
		return data.idle() == idle && data.walk() == walk ? attack : -1;
	}

	/** Narrow weapon/cast action classification: never replace arbitrary hit/death/other actions with attacks. */
	static boolean brotherAttack(int npc, int action)
	{
		switch (npc)
		{
			case NpcID.BARROWS_AHRIM: return action == AnimationID.HUMAN_CASTSTUN;
			case NpcID.BARROWS_DHAROK: return action == AnimationID.BARROW_DHAROK_SLASH || action == AnimationID.BARROW_DHAROK_CRUSH;
			case NpcID.BARROWS_GUTHAN: return action == AnimationID.BARROWS_WAR_SPEAR_STAB
				|| action == AnimationID.BARROWS_WAR_SPEAR_SLASH || action == AnimationID.BARROWS_WAR_SPEAR_CRUSH;
			case NpcID.BARROWS_KARIL: return action == AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE;
			case NpcID.BARROWS_TORAG: return action == AnimationID.BARROW_TORAG_CRUSH;
			case NpcID.BARROWS_VERAC: return action == AnimationID.BARROW_GUTHAN_CRUSH;
			default: return false;
		}
	}
}
