package com.barrowsthemes;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.ModelData;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WallModelSourcesTest
{
	private WallModelSources.Source select(BarrowsTheme theme, int id, int type, int x, boolean vary)
	{
		return WallModelSources.select(theme, id, type, new WorldPoint(x, 9690, 0), vary);
	}

	@Test
	public void stableStraightWallVariantsCoverEachThemesPool()
	{
		int[] counts = {4, 2, 6, 2, 1, 2, 1, 3, 2};
		int index = 0;
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			if (!theme.supportsWallModels()) { continue; }
			Set<Integer> selected = new HashSet<>();
			for (int x = 3550; x < 3574; x++)
			{
				WallModelSources.Source source = select(theme, ObjectID.BARROWS_CRYPT, 0, x, true);
				assertSame(source, select(theme, ObjectID.BARROWS_CRYPT_PURPLE, 0, x, true));
				selected.add(source.modelId);
			}
			assertEquals(counts[index++], selected.size());
			assertSame(select(theme, ObjectID.BARROWS_CRYPT, 0, 3550, false), select(theme, ObjectID.BARROWS_CRYPT, 0, 3551, false));
		}
	}

	@Test
	public void includesSmallStraightAndCornerStepsInBrownAndPurple()
	{
		int[] ids = {ObjectID.BARROWS_SKEWSTEPS, ObjectID.BARROWS_SKEWSTEPS_CORNER,
			ObjectID.BARROWS_SKEWSTEPS_PURPLE, ObjectID.BARROWS_SKEWSTEPS_CORNER_PURPLE};
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			if (theme != BarrowsTheme.ZANARIS && theme != BarrowsTheme.CHAMBERS_OF_XERIC && theme != BarrowsTheme.INFERNO) { continue; }
			for (int id : ids)
			{
				assertNotNull(select(theme, id, 22, 3550, false));
				assertNull(select(theme, id, 10, 3550, false));
			}
		}
	}

	@Test
	public void explicitlyMapsStaticRubbleAndSoilButNotFurnitureDoorsOrChest()
	{
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			if (theme != BarrowsTheme.ZANARIS && theme != BarrowsTheme.CHAMBERS_OF_XERIC && theme != BarrowsTheme.INFERNO) { continue; }
			for (int id : new int[] {ObjectID.BARROWS_BRICKS_PILE_1, ObjectID.BARROWS_BRICKS_PILE_2_PURPLE,
				ObjectID.BARROWS_DUGUPSOIL_1_PURPLE, ObjectID.BARROWS_MOUNTAINROCKS_1})
			{
				assertNotNull(select(theme, id, 22, 3550, true));
				assertNotNull(select(theme, id, 10, 3550, true));
				assertNull(select(theme, id, 2, 3550, true));
			}
			for (int id : new int[] {ObjectID.BARROWS_STONE_CHEST_CLOSED, ObjectID.BARROW_AHRIM_SARCOPHAGUS,
				ObjectID.BARROWS_STAIRS_AHRIM, 0}) { assertNull(select(theme, id, 10, 3550, true)); }
		}
	}

	@Test
	public void everyThemeIncludingFutureAdditionsMustSupplyItsOwnWallShapes()
	{
		int[][] models = {{11909, 11889, 11890}, {32435, 32439, 32432}, {33064, 33064, 33063},
			{37493, 37474, 37500}, {6254, 6254, 6247}, {60820, 60821, 60823},
			{9963, 9964, 9965}, {23757, 23758, 23760}, {7413, 7414, 7415}};
		assertEquals(models.length, BarrowsTheme.values().length);
		int[] types = {0, 1, 9};
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			assertTrue(theme.toString(), theme.supportsWallModels());
			for (int i = 0; i < types.length; i++)
			{
				assertEquals(models[theme.ordinal()][i], select(theme, ObjectID.BARROWS_CRYPT, types[i], 3550, false).modelId);
			}
			assertNull(select(theme, ObjectID.BARROWS_CRYPT, 2, 3550, true));
			if (theme.ordinal() > BarrowsTheme.INFERNO.ordinal())
			{
				assertNull(select(theme, ObjectID.BARROWS_SKEWSTEPS, 22, 3550, false));
				assertNull(select(theme, ObjectID.BARROWS_BRICKS_PILE_1, 10, 3550, false));
			}
		}
	}

	@Test
	public void distinguishesInnerOuterCornerNativeFacingsWithoutChangingCox()
	{
		assertEquals(1024, WallModelSources.extraRotation(BarrowsTheme.ZANARIS, ObjectID.BARROWS_CRYPT, 9));
		assertEquals(0, WallModelSources.extraRotation(BarrowsTheme.ZANARIS, ObjectID.BARROWS_CRYPT_INNER_PURPLE, 9));
		assertEquals(512, WallModelSources.extraRotation(BarrowsTheme.INFERNO, ObjectID.BARROWS_CRYPT, 9));
		assertEquals(33063, select(BarrowsTheme.INFERNO, ObjectID.BARROWS_CRYPT, 9, 3550, false).modelId);
		assertEquals(33074, select(BarrowsTheme.INFERNO, ObjectID.BARROWS_CRYPT_INNER, 9, 3550, false).modelId);
		assertEquals(0, WallModelSources.extraRotation(BarrowsTheme.CHAMBERS_OF_XERIC, ObjectID.BARROWS_CRYPT, 9));
	}

	@Test
	public void newSourcesReproduceDefinitionColoursAndLighting()
	{
		BarrowsTheme[] themes = {BarrowsTheme.PRIFDDINAS, BarrowsTheme.ANCIENT_PYRAMID, BarrowsTheme.DARKMEYER,
			BarrowsTheme.FREMENNIK_ICE_CAVES, BarrowsTheme.DORGESH_KAAN, BarrowsTheme.ABYSS};
		int[] ambient = {20, 0, 10, 30, 35, 0}, contrast = {250, 1000, 2500, 750, 0, 0};
		for (int i = 0; i < themes.length; i++)
		{
			WallModelSources.Source source = select(themes[i], ObjectID.BARROWS_CRYPT, 0, 3550, false);
			assertEquals(ModelData.DEFAULT_AMBIENT + ambient[i], source.ambient());
			assertEquals(ModelData.DEFAULT_CONTRAST + contrast[i], source.contrast());
		}
		ModelData copy = mock(ModelData.class);
		select(themes[0], ObjectID.BARROWS_CRYPT, 0, 3550, false).recolor(copy);
		verify(copy).recolor((short) 7465, (short) 3350);
		verify(copy).recolor((short) -22423, (short) 7465);
		copy = mock(ModelData.class);
		select(themes[1], ObjectID.BARROWS_CRYPT, 0, 3550, false).recolor(copy);
		verify(copy).recolor((short) 10434, (short) 8526);
		copy = mock(ModelData.class);
		select(themes[2], ObjectID.BARROWS_CRYPT, 0, 3550, false).recolor(copy);
		verify(copy).recolor((short) 284, (short) 5404);
		copy = mock(ModelData.class);
		select(themes[3], ObjectID.BARROWS_CRYPT, 0, 3550, false).recolor(copy);
		verify(copy).recolor((short) 7442, (short) -29238);
	}

	@Test
	public void sourceSpecificRecoloursAndLightingAreNotAppliedToUnrelatedModels()
	{
		ModelData copy = mock(ModelData.class);
		WallModelSources.Source mushrooms = select(BarrowsTheme.ZANARIS, ObjectID.BARROWS_BRICKS_PILE_1, 10, 3550, false);
		mushrooms.recolor(copy);
		verify(copy).recolor((short) 7442, (short) 227);
		assertEquals(ModelData.DEFAULT_AMBIENT + 20, mushrooms.ambient());
		copy = mock(ModelData.class);
		select(BarrowsTheme.CHAMBERS_OF_XERIC, ObjectID.BARROWS_DUGUPSOIL_1, 22, 3550, false).recolor(copy);
		verifyNoInteractions(copy);
	}
}
