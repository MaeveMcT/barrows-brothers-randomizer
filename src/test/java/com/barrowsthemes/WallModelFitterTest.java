package com.barrowsthemes;

import net.runelite.api.Model;
import net.runelite.api.ModelData;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WallModelFitterTest
{
	private Model original(float[] x, float[] y, float[] z)
	{
		Model model = mock(Model.class);
		when(model.getVerticesCount()).thenReturn(x.length);
		when(model.getVerticesX()).thenReturn(x);
		when(model.getVerticesY()).thenReturn(y);
		when(model.getVerticesZ()).thenReturn(z);
		return model;
	}

	private ModelData source(float[] x, float[] y, float[] z)
	{
		ModelData data = mock(ModelData.class);
		when(data.getVerticesCount()).thenReturn(x.length);
		when(data.getVerticesX()).thenReturn(x);
		when(data.getVerticesY()).thenReturn(y);
		when(data.getVerticesZ()).thenReturn(z);
		return data;
	}

	@Test
	public void fitsEveryVertexToOriginalBoundsWithoutChangingOriginal()
	{
		float[] originalX = {-64, 73}, originalY = {-250, 0}, originalZ = {-71, 68};
		Model original = original(originalX, originalY, originalZ);
		float[] x = {-128, 0, 128}, y = {-400, -200, 0}, z = {-128, 0, 128};
		assertTrue(WallModelFitter.fit(source(x, y, z), original, 0));
		assertArrayEquals(new float[] {-64, 5, 73}, x, 0f);
		assertArrayEquals(new float[] {-250, -125, 0}, y, 0f);
		assertArrayEquals(new float[] {-71, -1, 68}, z, 0f);
		assertArrayEquals(new float[] {-64, 73}, originalX, 0f);
		assertArrayEquals(new float[] {-250, 0}, originalY, 0f);
		assertArrayEquals(new float[] {-71, 68}, originalZ, 0f);
		assertEquals(74, WallModelFitter.radius(original));
	}

	@Test
	public void allQuarterTurnsAndDiagonalRotationStayWithinBounds()
	{
		Model original = original(new float[] {-64, 64}, new float[] {-250, 160}, new float[] {-68, 64});
		for (int rotation : new int[] {0, 256, 512, 768, 1024, 1536})
		{
			float[] x = {-128, 128, -128, 128}, y = {-434, 0, -434, 0}, z = {-64, -64, 64, 64};
			assertTrue(WallModelFitter.fit(source(x, y, z), original, rotation));
			for (int i = 0; i < x.length; i++)
			{
				assertTrue(x[i] >= -64 && x[i] <= 64);
				assertTrue(y[i] >= -250 && y[i] <= 160);
				assertTrue(z[i] >= -68 && z[i] <= 64);
			}
		}
	}

	@Test
	public void coxProfileFitDoesNotReverseNativeDiagonalWinding()
	{
		// The earlier distance-only half-turn failed live review: it ignored face winding.
		for (int id : new int[] {net.runelite.api.gameval.ObjectID.BARROWS_CRYPT, net.runelite.api.gameval.ObjectID.BARROWS_CRYPT_PURPLE})
		{
			assertEquals(0, WallModelSources.extraRotation(BarrowsTheme.CHAMBERS_OF_XERIC, id, 1));
			assertEquals(0, WallModelSources.extraRotation(BarrowsTheme.CHAMBERS_OF_XERIC, id, 9));
		}
	}

	@Test
	public void rejectsMissingInvalidAndUnexpandableMeshes()
	{
		Model target = original(new float[] {-64, 64}, new float[] {-250, 0}, new float[] {-64, 64});
		assertFalse(WallModelFitter.fit(mock(ModelData.class), target, 0));
		assertFalse(WallModelFitter.fit(source(new float[] {0, 0}, new float[] {-250, 0}, new float[] {-64, 64}), target, 0));
		assertFalse(WallModelFitter.fit(source(new float[] {Float.NaN, 128}, new float[] {-250, 0}, new float[] {-64, 64}), target, 0));
	}

	@Test
	public void flattensToZeroExtentWhenOriginalHasAFlatAxis()
	{
		Model target = original(new float[] {-64, -64}, new float[] {-250, 0}, new float[] {-64, 64});
		float[] x = {-128, 128};
		assertTrue(WallModelFitter.fit(source(x, new float[] {-300, 0}, new float[] {-64, 64}), target, 0));
		assertArrayEquals(new float[] {-64, -64}, x, 0f);
	}

	@Test
	public void sourceMappingsCoverAllThemesAndSupportedWallShapes()
	{
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			if (!theme.supportsWallModels()) { continue; }
			for (int type : new int[] {0, 1, 9})
			{
				assertTrue(WallModelSources.select(theme, net.runelite.api.gameval.ObjectID.BARROWS_CRYPT, type,
					new net.runelite.api.coords.WorldPoint(3550, 9690, 0), false).modelId > 0);
			}
			assertNull(WallModelSources.select(theme, net.runelite.api.gameval.ObjectID.BARROWS_CRYPT, 10,
				new net.runelite.api.coords.WorldPoint(3550, 9690, 0), false));
		}
		assertEquals(0, WallModelSources.wallRotation(1));
		assertEquals(512, WallModelSources.wallRotation(2));
		assertEquals(1024, WallModelSources.wallRotation(64));
		assertEquals(1536, WallModelSources.wallRotation(128));
		assertEquals(-1, WallModelSources.wallRotation(0));
	}
}
