package com.barrowsthemes;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Model;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ThemeScopeTest
{
	@Test
	public void exposesOnlyTheThreeRetainedThemes()
	{
		assertArrayEquals(new BarrowsTheme[] {BarrowsTheme.ZANARIS, BarrowsTheme.CHAMBERS_OF_XERIC, BarrowsTheme.INFERNO}, BarrowsTheme.values());
	}

	@Test
	public void retainedThemesHaveDistinctCacheSampledMaterialsAndFloors()
	{
		int[][] materials = {{24524,10326,30156,21714}, {28950,29194,28830,28830}, {8,12,949,272}};
		int[] brown = {10762,10638,10518,10394};
		Set<Integer> walls = new HashSet<>(), floors = new HashSet<>();
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			for (int role = 0; role < brown.length; role++)
			{
				assertEquals(materials[theme.ordinal()][role], CuratedMaterials.recolour(theme, brown[role], false, false));
			}
			walls.add(CuratedMaterials.recolour(theme, brown[0], false, false));
			floors.add(CuratedMaterials.recolour(theme, 12341, true, false));
			assertTrue(theme.supportsWallModels());
		}
		assertEquals(3, walls.size()); assertEquals(3, floors.size());
	}

	@Test
	public void retainedPalettesKeepLightingVariationAndRestoreOriginalArrays()
	{
		Model model = mock(Model.class);
		int[] colours = {10762,10638,10518,10394};
		when(model.getFaceColors1()).thenReturn(colours);
		when(model.getFaceColors2()).thenReturn(colours.clone());
		when(model.getFaceColors3()).thenReturn(colours.clone());
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			assertNotEquals(CuratedMaterials.recolour(theme, 10762, false, false), CuratedMaterials.recolour(theme, 10782, false, false));
			SceneEdits edits = new SceneEdits(theme, true);
			assertTrue(edits.wall(model));
			assertEquals(CuratedMaterials.recolour(theme, 10762, false, false), colours[0]);
			edits.restore();
			assertArrayEquals(new int[] {10762,10638,10518,10394}, colours);
		}
	}

	@Test
	public void removedOrUnknownSavedThemeResetsToDefault()
	{
		for (String saved : new String[] {"PRIFDDINAS", "ANCIENT_PYRAMID", "DARKMEYER", "FREMENNIK_ICE_CAVES", "DORGESH_KAAN", "ABYSS", "UNKNOWN"})
		{
			ConfigManager manager = mock(ConfigManager.class);
			when(manager.getConfiguration(BarrowsThemesConfig.GROUP, "theme")).thenReturn(saved);
			BarrowsThemesPlugin.migrateThemeConfig(manager);
			verify(manager).setConfiguration(BarrowsThemesConfig.GROUP, "theme", BarrowsTheme.ZANARIS);
		}
	}

	@Test
	public void validOrUnsetThemeIsPreserved()
	{
		for (String saved : new String[] {null, "ZANARIS", "CHAMBERS_OF_XERIC", "INFERNO"})
		{
			ConfigManager manager = mock(ConfigManager.class);
			when(manager.getConfiguration(BarrowsThemesConfig.GROUP, "theme")).thenReturn(saved);
			BarrowsThemesPlugin.migrateThemeConfig(manager);
			verify(manager, never()).setConfiguration(eq(BarrowsThemesConfig.GROUP), eq("theme"), any(Object.class));
		}
		BarrowsThemesPlugin.migrateThemeConfig(null);
	}

	@Test
	public void brotherActionBorrowingIsOffByDefault()
	{
		assertEquals(NpcAnimationMode.NATIVE, new BarrowsThemesConfig() { }.npcAnimationMode());
	}
}
