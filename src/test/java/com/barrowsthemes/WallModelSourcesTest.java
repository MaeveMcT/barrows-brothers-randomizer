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
		int[] counts = {4, 2, 6};
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
		int[][] models = {{11909, 11889, 11890}, {32435, 32439, 32432}, {33064, 33064, 33063}};
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
	public void retainedSourcesReproduceDefinitionColoursAndLighting()
	{
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			WallModelSources.Source source = select(theme, ObjectID.BARROWS_CRYPT, 0, 3550, false);
			assertEquals(ModelData.DEFAULT_AMBIENT + (theme == BarrowsTheme.ZANARIS ? 20 : 0), source.ambient());
			assertEquals(ModelData.DEFAULT_CONTRAST, source.contrast());
			ModelData copy = mock(ModelData.class);
			source.recolor(copy);
			if (theme == BarrowsTheme.CHAMBERS_OF_XERIC) { verify(copy).recolor((short) 29574, (short) 29194); }
			else { verifyNoInteractions(copy); }
		}
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
