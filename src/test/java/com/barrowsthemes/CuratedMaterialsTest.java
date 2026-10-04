package com.barrowsthemes;

import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CuratedMaterialsTest
{
	@Test
	public void mapsBrownAndPurpleBarrowsRolesToSampledZanarisColours()
	{
		assertEquals(24524, map(BarrowsTheme.ZANARIS, 10762));
		assertEquals(24524, map(BarrowsTheme.ZANARIS, 64018));
		assertEquals(10326, map(BarrowsTheme.ZANARIS, 10638));
		assertEquals(10326, map(BarrowsTheme.ZANARIS, 63894));
		assertEquals(30156, map(BarrowsTheme.ZANARIS, 10518));
		assertEquals(30156, map(BarrowsTheme.ZANARIS, 63774));
		assertEquals(21714, map(BarrowsTheme.ZANARIS, 10394));
	}

	@Test
	public void mapsXericAndInfernoUsingTheirSampledMaterials()
	{
		assertEquals(28950, map(BarrowsTheme.CHAMBERS_OF_XERIC, 10762));
		assertEquals(29194, map(BarrowsTheme.CHAMBERS_OF_XERIC, 10638));
		assertEquals(28830, map(BarrowsTheme.CHAMBERS_OF_XERIC, 10518));
		assertEquals(8, map(BarrowsTheme.INFERNO, 10762));
		assertEquals(12, map(BarrowsTheme.INFERNO, 10638));
		assertEquals(949, map(BarrowsTheme.INFERNO, 10518));
	}

	@Test
	public void preservesLightingVariationAndAllSentinels()
	{
		assertEquals(38, map(BarrowsTheme.ZANARIS, 10752 | 5) & 127);
		assertEquals(76, map(BarrowsTheme.ZANARIS, 10752 | 10) & 127);
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			for (int value : new int[] {-1, -2, 0, ThemeColors.INVISIBLE})
			{
				assertEquals(value, map(theme, value));
				assertEquals(value, CuratedMaterials.recolour(theme, value, true, false));
			}
		}
	}

	@Test
	public void floorUsesSampledHueAndSaturationButKeepsGradients()
	{
		int target = JagexColor.rgbToHSL(0x50a0b8, 1.0) & 0xffff;
		int result = CuratedMaterials.recolour(BarrowsTheme.ZANARIS, 12341, true, false);
		assertEquals(target & ~127, result & ~127);
		assertEquals(12341 & 127, result & 127);
	}

	@Test
	public void curatedEditsRestoreOriginalsAndManualTextureWins()
	{
		Model model = mock(Model.class);
		int[] colours = {64018, 50};
		short[] textures = {-1, 3};
		when(model.getFaceColors1()).thenReturn(colours);
		when(model.getFaceColors2()).thenReturn(new int[] {64018, 51});
		when(model.getFaceColors3()).thenReturn(new int[] {64018, -1});
		when(model.getFaceTextures()).thenReturn(textures);
		SceneEdits edits = new SceneEdits(BarrowsTheme.ZANARIS, true);
		edits.wall(model, 7);
		assertArrayEquals(new int[] {24524, 50}, colours);
		assertArrayEquals(new short[] {-1, 7}, textures);
		edits.restore();
		assertArrayEquals(new int[] {64018, 50}, colours);
		assertArrayEquals(new short[] {-1, 3}, textures);
	}

	private int map(BarrowsTheme theme, int colour) { return CuratedMaterials.recolour(theme, colour, false, false); }
}
