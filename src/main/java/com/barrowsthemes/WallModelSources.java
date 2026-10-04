package com.barrowsthemes;

import net.runelite.api.ModelData;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/** Cache-backed source definitions. Model IDs have no gameval constant class. */
final class WallModelSources
{
	private WallModelSources() { }

	private static final Source[] ZANARIS_WALLS = {zanaris(11909), zanaris(11910), zanaris(11911), zanaris(11912)};
	private static final Source[] COX_WALLS = {coxWall(32435), coxWall(32437)};
	private static final Source[] INFERNO_WALLS = {plain(33064), plain(33076), plain(33077), plain(33065), plain(33067), plain(33066)};
	private static final Source ZANARIS_DIAGONAL = zanaris(11889);
	private static final Source ZANARIS_CORNER = zanaris(11890);
	private static final Source COX_DIAGONAL = coxWall(32439);
	private static final Source COX_CORNER = coxWall(32432);
	private static final Source INFERNO_OUTER = plain(33063);
	private static final Source INFERNO_INNER = plain(33074);
	private static final Source ZANARIS_STEP = zanaris(11942);
	private static final Source ZANARIS_STEP_CORNER = zanaris(11941);
	private static final Source COX_STEP = plain(32384);
	private static final Source INFERNO_STEP = plain(33060);
	private static final Source INFERNO_STEP_CORNER = plain(33072);
	private static final Source ZANARIS_MUSHROOMS = new Source(11985, 20,
		new short[] {7442, 16454, 7265}, new short[] {227, 957, 127});
	private static final Source[] ZANARIS_ROCKS = {
		new Source(12125, 20, new short[] {43, 39, 35, 30}, new short[] {6705, 6699, 6695, 6819}),
		new Source(12126, 20, new short[] {43, 39, 35, 30}, new short[] {6705, 6699, 6695, 6819})
	};
	private static final Source[] COX_ROCKS = {plain(32474), plain(32501), plain(32459), plain(32454)};
	private static final Source[] INFERNO_ROCKS = {plain(33051), plain(33057)};

	// Definition/model provenance and calibrated facing: docs/theme-models.md.
	private static final WallSet ZANARIS = new WallSet(ZANARIS_WALLS, ZANARIS_DIAGONAL, ZANARIS_CORNER, ZANARIS_CORNER, 0, 1024, 0);
	// CoX front skins are conformed to the original wall profiles; a diagonal half-turn reverses winding.
	private static final WallSet COX = new WallSet(COX_WALLS, COX_DIAGONAL, COX_CORNER, COX_CORNER, 0, 0, 0);
	private static final WallSet INFERNO = new WallSet(INFERNO_WALLS, INFERNO_WALLS[0], INFERNO_OUTER, INFERNO_INNER, 256, 512, 512);

	private static WallSet walls(BarrowsTheme theme)
	{
		switch (theme)
		{
			case ZANARIS: return ZANARIS;
			case CHAMBERS_OF_XERIC: return COX;
			case INFERNO: return INFERNO;
			default: return null; // Never borrow unrelated geometry for an unmapped theme.
		}
	}

	static boolean hasWalls(BarrowsTheme theme) { return walls(theme) != null; }

	static boolean isCryptWall(int id)
	{
		switch (id)
		{
			case ObjectID.BARROWS_CRYPT:
			case ObjectID.BARROWS_CRYPT_INNER:
			case ObjectID.BARROWS_CRYPT_MISSING_BRICK:
			case ObjectID.BARROWS_CRYPT_PURPLE:
			case ObjectID.BARROWS_CRYPT_INNER_PURPLE:
			case ObjectID.BARROWS_CRYPT_MISSING_BRICK_PURPLE: return true;
			default: return false;
		}
	}

	static boolean isStep(int id)
	{
		return id == ObjectID.BARROWS_SKEWSTEPS || id == ObjectID.BARROWS_SKEWSTEPS_CORNER
			|| id == ObjectID.BARROWS_SKEWSTEPS_PURPLE || id == ObjectID.BARROWS_SKEWSTEPS_CORNER_PURPLE;
	}

	static boolean isScenery(int id)
	{
		// Reuse the explicit rocky/soil architecture allowlist, never interactive furniture.
		return BarrowsArchitecture.contains(id) && !isCryptWall(id) && !BarrowsArchitecture.allowsInteractions(id);
	}

	static Source select(BarrowsTheme theme, int id, int type, WorldPoint point, boolean varyWalls)
	{
		WallSet set = walls(theme);
		if (set == null) { return null; }
		if (isCryptWall(id))
		{
			if (type == 0) { return set.straight[varyWalls ? variant(point, set.straight.length) : 0]; }
			if (type == 1) { return set.diagonal; }
			if (type == 9) { return inner(id) ? set.inner : set.outer; }
			return null;
		}
		// Parked scenery experiments. Production disables them.
		if (isStep(id) && type == 22)
		{
			boolean corner = id == ObjectID.BARROWS_SKEWSTEPS_CORNER || id == ObjectID.BARROWS_SKEWSTEPS_CORNER_PURPLE;
			return theme == BarrowsTheme.ZANARIS ? (corner ? ZANARIS_STEP_CORNER : ZANARIS_STEP)
				: theme == BarrowsTheme.CHAMBERS_OF_XERIC ? (corner ? COX_CORNER : COX_STEP)
				: corner ? INFERNO_STEP_CORNER : INFERNO_STEP;
		}
		if (!isScenery(id) || isStep(id) || (type != 10 && type != 11 && type != 22)) { return null; }
		if (theme == BarrowsTheme.ZANARIS && brickPile(id)) { return ZANARIS_MUSHROOMS; }
		Source[] rocks = theme == BarrowsTheme.ZANARIS ? ZANARIS_ROCKS : theme == BarrowsTheme.CHAMBERS_OF_XERIC ? COX_ROCKS : INFERNO_ROCKS;
		return rocks[variant(point, rocks.length)];
	}

	static int variant(WorldPoint point, int count)
	{
		// Template coordinates survive instance relocation and scene/inspector rebuilds.
		return Math.floorMod(point.getX() * 31 + point.getY() * 17 + point.getPlane() * 13, count);
	}

	static int extraRotation(BarrowsTheme theme, int id, int type)
	{
		WallSet set = walls(theme);
		if (set == null || !isCryptWall(id)) { return 0; }
		if (type == 1) { return set.diagonalRotation; }
		return type == 9 ? (inner(id) ? set.innerRotation : set.outerRotation) : 0;
	}

	private static boolean inner(int id) { return id == ObjectID.BARROWS_CRYPT_INNER || id == ObjectID.BARROWS_CRYPT_INNER_PURPLE; }
	private static boolean brickPile(int id)
	{
		return id == ObjectID.BARROWS_BRICKS_PILE_1 || id == ObjectID.BARROWS_BRICKS_PILE_2
			|| id == ObjectID.BARROWS_BRICKS_PILE_1_PURPLE || id == ObjectID.BARROWS_BRICKS_PILE_2_PURPLE;
	}

	static int wallRotation(int mask)
	{
		switch (mask)
		{
			case 1: case 16: return 0;
			case 2: case 32: return 512;
			case 4: case 64: return 1024;
			case 8: case 128: return 1536;
			default: return -1;
		}
	}

	private static Source plain(int id) { return new Source(id, 0, null, null); }
	private static Source zanaris(int id) { return new Source(id, 20, null, null); }
	private static Source coxWall(int id) { return new Source(id, 0, new short[] {29574}, new short[] {29194}); }

	private static final class WallSet
	{
		final Source[] straight;
		final Source diagonal, outer, inner;
		final int diagonalRotation, outerRotation, innerRotation;
		WallSet(Source[] straight, Source diagonal, Source outer, Source inner, int diagonalRotation, int outerRotation, int innerRotation)
		{
			this.straight = straight; this.diagonal = diagonal; this.outer = outer; this.inner = inner;
			this.diagonalRotation = diagonalRotation; this.outerRotation = outerRotation; this.innerRotation = innerRotation;
		}
	}

	static final class Source
	{
		final int modelId;
		private final int ambient;
		private final short[] from, to;
		private Source(int modelId, int ambient, short[] from, short[] to)
		{
			this.modelId = modelId; this.ambient = ambient; this.from = from; this.to = to;
		}

		void recolor(ModelData copy)
		{
			if (from != null) { for (int i = 0; i < from.length; i++) { copy.recolor(from[i], to[i]); } }
		}

		int ambient() { return ModelData.DEFAULT_AMBIENT + ambient; }
		int contrast() { return ModelData.DEFAULT_CONTRAST; }
	}
}
