package com.barrowsthemes;

import net.runelite.api.Model;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SceneEditsTest
{
	@Test
	public void recolouringPreservesLightAndSentinels()
	{
		assertEquals(-1, ThemeColors.recolour(-1, true, false));
		assertEquals(-2, ThemeColors.recolour(-2, false, false));
		assertEquals(ThemeColors.INVISIBLE, ThemeColors.recolour(ThemeColors.INVISIBLE, true, false));
		assertEquals(53, ThemeColors.recolour(12341, true, false) & 127);
		assertNotEquals(ThemeColors.recolour(12341, true, false), ThemeColors.recolour(12341, false, false));
	}

	@Test
	public void palettesAreDistinctAndPreserveSentinels()
	{
		java.util.Set<Integer> floors = new java.util.HashSet<>();
		java.util.Set<Integer> walls = new java.util.HashSet<>();
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			floors.add(ThemeColors.recolour(12341, true, false, theme));
			walls.add(ThemeColors.recolour(12341, false, false, theme));
			assertEquals(-1, ThemeColors.recolour(-1, true, false, theme));
			assertEquals(-2, ThemeColors.recolour(-2, false, false, theme));
			assertEquals(ThemeColors.INVISIBLE, ThemeColors.recolour(ThemeColors.INVISIBLE, true, false, theme));
		}
		assertEquals(BarrowsTheme.values().length, floors.size());
		assertEquals(BarrowsTheme.values().length, walls.size());
	}

	@Test
	public void themeSwitchUsesOriginalColoursWithoutAccumulatedDarkening()
	{
		Model model = mock(Model.class);
		int[] colours = {12341};
		when(model.getFaceColors1()).thenReturn(colours);
		when(model.getFaceColors2()).thenReturn(new int[] {12342});
		when(model.getFaceColors3()).thenReturn(new int[] {12343});
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			SceneEdits edits = new SceneEdits(theme);
			edits.wall(model);
			assertEquals(ThemeColors.recolour(12341, false, false, theme), colours[0]);
			edits.restore();
			assertEquals(12341, colours[0]);
		}
	}

	@Test
	public void paintIsCapturedOnceAndFullyRestored()
	{
		SceneTilePaint paint = mock(SceneTilePaint.class);
		when(paint.getSwColor()).thenReturn(12341);
		when(paint.getSeColor()).thenReturn(12342);
		when(paint.getNwColor()).thenReturn(12343);
		when(paint.getNeColor()).thenReturn(ThemeColors.INVISIBLE);
		when(paint.getTexture()).thenReturn(4);
		SceneEdits edits = new SceneEdits();
		assertTrue(edits.paint(paint, -1));
		assertFalse(edits.paint(paint, -1));
		edits.restore();
		verify(paint).setSwColor(12341);
		verify(paint).setSeColor(12342);
		verify(paint).setNwColor(12343);
		verify(paint).setTexture(4);
		verify(paint, times(2)).setNeColor(ThemeColors.INVISIBLE);
		assertTrue(edits.paint(paint, -1));
	}

	@Test
	public void wallArraysAndTexturesAreRestored()
	{
		Model model = mock(Model.class);
		int[] a = {12050, 12060};
		int[] b = {12051, -1};
		int[] c = {12052, -2};
		short[] textures = {3, -1};
		when(model.getFaceColors1()).thenReturn(a);
		when(model.getFaceColors2()).thenReturn(b);
		when(model.getFaceColors3()).thenReturn(c);
		when(model.getFaceTextures()).thenReturn(textures);
		SceneEdits edits = new SceneEdits();
		assertTrue(edits.wall(model));
		assertFalse(edits.wall(model));
		assertArrayEquals(new short[] {-1, -1}, textures);
		assertEquals(-1, b[1]);
		assertEquals(-2, c[1]);
		edits.restore();
		assertArrayEquals(new int[] {12050, 12060}, a);
		assertArrayEquals(new int[] {12051, -1}, b);
		assertArrayEquals(new int[] {12052, -2}, c);
		assertArrayEquals(new short[] {3, -1}, textures);
	}

	@Test
	public void wallTextureReplacementPreservesMappingAndUntypedFaces()
	{
		Model model = mock(Model.class);
		int[] a = {50, 12341}, b = {51, 12342}, c = {-1, -2};
		short[] textures = {3, -1};
		byte[] textureFaces = {0, -1};
		when(model.getFaceColors1()).thenReturn(a);
		when(model.getFaceColors2()).thenReturn(b);
		when(model.getFaceColors3()).thenReturn(c);
		when(model.getFaceTextures()).thenReturn(textures);
		when(model.getTextureFaces()).thenReturn(textureFaces);
		SceneEdits edits = new SceneEdits();
		assertTrue(edits.wall(model, 7));
		assertArrayEquals(new short[] {7, -1}, textures);
		assertEquals(50, a[0]);
		assertEquals(ThemeColors.recolour(12341, false, false), a[1]);
		assertArrayEquals(new int[] {-1, -2}, c);
		assertSame(textureFaces, model.getTextureFaces());
		assertArrayEquals(new byte[] {0, -1}, textureFaces);
		edits.restore();
		assertArrayEquals(new short[] {3, -1}, textures);
		assertArrayEquals(new int[] {50, 12341}, a);
		assertArrayEquals(new int[] {51, 12342}, b);
	}

	@Test
	public void wallWithoutTextureArrayFallsBackToPalette()
	{
		Model model = mock(Model.class);
		int[] a = {12341};
		when(model.getFaceColors1()).thenReturn(a);
		when(model.getFaceColors2()).thenReturn(new int[] {12342});
		when(model.getFaceColors3()).thenReturn(new int[] {12343});
		SceneEdits edits = new SceneEdits();
		edits.wall(model, 7);
		assertNull(model.getFaceTextures());
		assertEquals(ThemeColors.recolour(12341, false, false), a[0]);
		edits.restore();
		assertArrayEquals(new int[] {12341}, a);
	}

	@Test
	public void untexturedTriangleModelStaysUntexturedAndRestores()
	{
		SceneTileModel model = mock(SceneTileModel.class);
		int[] a = {12050}, b = {12051}, c = {12052};
		when(model.getTriangleColorA()).thenReturn(a);
		when(model.getTriangleColorB()).thenReturn(b);
		when(model.getTriangleColorC()).thenReturn(c);
		SceneEdits edits = new SceneEdits();
		edits.floorModel(model, 7);
		assertEquals(ThemeColors.recolour(12050, true, false), a[0]);
		edits.restore();
		assertArrayEquals(new int[] {12050}, a);
		assertArrayEquals(new int[] {12051}, b);
		assertArrayEquals(new int[] {12052}, c);
	}

	@Test
	public void texturedTriangleModelUsesBrightnessAndRestores()
	{
		SceneTileModel model = mock(SceneTileModel.class);
		int[] a = {50}, b = {51}, c = {52}, textures = {3};
		when(model.getTriangleColorA()).thenReturn(a);
		when(model.getTriangleColorB()).thenReturn(b);
		when(model.getTriangleColorC()).thenReturn(c);
		when(model.getTriangleTextureId()).thenReturn(textures);
		SceneEdits edits = new SceneEdits();
		edits.floorModel(model, 7);
		assertArrayEquals(new int[] {7}, textures);
		assertArrayEquals(new int[] {50}, a);
		edits.restore();
		assertArrayEquals(new int[] {3}, textures);
	}
}
