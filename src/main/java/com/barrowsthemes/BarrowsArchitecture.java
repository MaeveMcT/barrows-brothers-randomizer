package com.barrowsthemes;

import net.runelite.api.gameval.ObjectID;

/** Explicit crypt scenery eligible outside the WallObject slot; never changes object actions. */
final class BarrowsArchitecture
{
	private BarrowsArchitecture() { }

	static boolean contains(int id)
	{
		if (allowsInteractions(id)) { return true; }
		switch (id)
		{
			case ObjectID.BARROWS_CRYPT:
			case ObjectID.BARROWS_CRYPT_INNER:
			case ObjectID.BARROWS_CRYPT_MISSING_BRICK:
			case ObjectID.BARROWS_CRYPT_PURPLE:
			case ObjectID.BARROWS_CRYPT_INNER_PURPLE:
			case ObjectID.BARROWS_CRYPT_MISSING_BRICK_PURPLE:
			case ObjectID.BARROWS_SKEWSTEPS:
			case ObjectID.BARROWS_SKEWSTEPS_CORNER:
			case ObjectID.BARROWS_SKEWSTEPS_PURPLE:
			case ObjectID.BARROWS_SKEWSTEPS_CORNER_PURPLE:
			case ObjectID.BARROWS_DUGUPSOIL_1:
			case ObjectID.BARROWS_DUGUPSOIL_2:
			case ObjectID.BARROWS_DUGUPSOIL_3:
			case ObjectID.BARROWS_DUGUPSOIL_1_PURPLE:
			case ObjectID.BARROWS_DUGUPSOIL_2_PURPLE:
			case ObjectID.BARROWS_DUGUPSOIL_3_PURPLE:
			case ObjectID.BARROWS_BRICKS_PILE_1:
			case ObjectID.BARROWS_BRICKS_PILE_2:
			case ObjectID.BARROWS_BRICKS_PILE_1_PURPLE:
			case ObjectID.BARROWS_BRICKS_PILE_2_PURPLE:
			case ObjectID.BARROWS_MOUNTAINROCKS_1:
			case ObjectID.BARROWS_MOUNTAINROCKS_1_LARGE:
			case ObjectID.BARROWS_ROCKSLIDE_A:
			case ObjectID.BARROWS_ROCKSLIDE_B:
			case ObjectID.BARROWS_ROCKSLIDE_C:
			case ObjectID.BARROWS_ROCKSLIDE_D:
			case ObjectID.BARROWS_ROCKSLIDE_E:
			case ObjectID.BARROWS_ROCKSLIDE_F:
			case ObjectID.BARROWS_ROCKSLIDE_G:
			case ObjectID.BARROWS_ROCKSLIDE_H:
			case ObjectID.BARROWS_ROCKSLIDE_I:
			case ObjectID.BARROWS_ROCKSLIDE_J:
			case ObjectID.BARROWS_ROCKSLIDE_K:
			case ObjectID.BARROWS_ROCKSLIDE_L:
			case ObjectID.BARROWS_ROCKSLIDE_M:
			case ObjectID.BARROWS_ROCKSLIDE_N:
			case ObjectID.BARROWS_ROCKSLIDE_O:
			case ObjectID.BARROWS_ROCKSLIDE_P:
			case ObjectID.BARROWS_ROCKSLIDE1:
			case ObjectID.BARROWS_ROCKSLIDE2:
			case ObjectID.BARROWS_ROCKSLIDE3:
			case ObjectID.BARROWS_ROCKSLIDE4:
			case ObjectID.BARROWS_ROCKSLIDE5:
			case ObjectID.BARROWS_ROCKSLIDE6:
				return true;
			default:
				return false;
		}
	}

	static boolean allowsInteractions(int id)
	{
		switch (id)
		{
			case ObjectID.BARROWS_STAIRS_AHRIM:
			case ObjectID.BARROWS_STAIRS_DHAROK:
			case ObjectID.BARROWS_STAIRS_GUTHAN:
			case ObjectID.BARROWS_STAIRS_KARIL:
			case ObjectID.BARROWS_STAIRS_TORAG:
			case ObjectID.BARROWS_STAIRS_VERAC:
			case ObjectID.BARROW_AHRIM_SARCOPHAGUS:
			case ObjectID.BARROW_DHAROK_SARCOPHAGUS:
			case ObjectID.BARROW_GUTHAN_SARCOPHAGUS:
			case ObjectID.BARROW_KARIL_SARCOPHAGUS:
			case ObjectID.BARROW_TORAG_SARCOPHAGUS:
			case ObjectID.BARROW_VERAC_SARCOPHAGUS:
				return true;
			default:
				return false;
		}
	}
}
