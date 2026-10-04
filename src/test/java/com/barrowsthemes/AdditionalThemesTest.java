package com.barrowsthemes;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AdditionalThemesTest
{
	private static final BarrowsTheme[] THEMES = {BarrowsTheme.PRIFDDINAS, BarrowsTheme.ANCIENT_PYRAMID,
		BarrowsTheme.DARKMEYER, BarrowsTheme.FREMENNIK_ICE_CAVES, BarrowsTheme.DORGESH_KAAN, BarrowsTheme.ABYSS};
	private static final int[][] COLOURS = {
		{0xd3d0df, 0x9e8ab5, 0x76bda6, 0xefe7ff},
		{0xc8aa6d, 0xa28145, 0x49a5a2, 0xe7ce87},
		{0x696371, 0x46404e, 0x872c3c, 0xb9a6a5},
		{0xb3d2dc, 0x7ca9bd, 0x72c7e4, 0xe5f5fa},
		{0x9a8053, 0x625343, 0xcfa450, 0x83965c},
		{0x934553, 0x623347, 0xbc5f73, 0xb98aab}
	};

	@Test
	public void eachNewThemeHasExplicitMaterialRolesNotAnOldThemeFallback()
	{
		int[] brown = {10762, 10638, 10518, 10394};
		for (int i = 0; i < THEMES.length; i++)
		{
			for (int role = 0; role < brown.length; role++)
			{
				int target = JagexColor.rgbToHSL(COLOURS[i][role], 1.0) & 0xffff;
				assertEquals(THEMES[i] + " role " + role, target, CuratedMaterials.recolour(THEMES[i], brown[role], false, false));
			}
			assertTrue(THEMES[i].supportsWallModels());
		}
	}

	@Test
	public void allNineDetailedPalettesHaveDistinctMainMaterialsAndFloors()
	{
		Set<Integer> walls = new HashSet<>(), floors = new HashSet<>();
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			walls.add(CuratedMaterials.recolour(theme, 10762, false, false));
			floors.add(CuratedMaterials.recolour(theme, 12341, true, false));
		}
		assertEquals(9, walls.size());
		assertEquals(9, floors.size());
	}

	@Test
	public void newMaterialPalettesKeepLightingVariationAndRestoreOriginalArrays()
	{
		Model model = mock(Model.class);
		int[] colours = {10762, 10638, 10518, 10394};
		when(model.getFaceColors1()).thenReturn(colours);
		when(model.getFaceColors2()).thenReturn(colours.clone());
		when(model.getFaceColors3()).thenReturn(colours.clone());
		for (BarrowsTheme theme : THEMES)
		{
			assertNotEquals(CuratedMaterials.recolour(theme, 10753, false, false), CuratedMaterials.recolour(theme, 10754, false, false));
			SceneEdits edits = new SceneEdits(theme, true);
			assertTrue(edits.wall(model));
			assertEquals(CuratedMaterials.recolour(theme, 10762, false, false), colours[0]);
			edits.restore();
			assertArrayEquals(new int[] {10762, 10638, 10518, 10394}, colours);
		}
	}

	@Test
	public void brotherActionBorrowingIsOffByDefault()
	{
		assertEquals(NpcAnimationMode.NATIVE, new BarrowsThemesConfig() { }.npcAnimationMode());
	}
}
