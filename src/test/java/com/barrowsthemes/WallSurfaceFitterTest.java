package com.barrowsthemes;

import net.runelite.api.Mesh;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WallSurfaceFitterTest
{
	private <T extends Mesh<?>> T strip(Class<T> api, float[] x, float[] y, float[] z)
	{
		T mesh = mock(api);
		when(mesh.getVerticesCount()).thenReturn(x.length);
		when(mesh.getVerticesX()).thenReturn(x); when(mesh.getVerticesY()).thenReturn(y); when(mesh.getVerticesZ()).thenReturn(z);
		when(mesh.getFaceCount()).thenReturn(4);
		when(mesh.getFaceIndices1()).thenReturn(new int[] {0, 0, 1, 1});
		when(mesh.getFaceIndices2()).thenReturn(new int[] {1, 4, 2, 5});
		when(mesh.getFaceIndices3()).thenReturn(new int[] {4, 3, 5, 4});
		if (mesh instanceof Model) { when(((Model) mesh).getFaceColors3()).thenReturn(new int[] {10, 11, 12, 13}); }
		return mesh;
	}

	private void rotate(float[] x, float[] z, int quarter)
	{
		for (int i = 0; i < x.length; i++) { for (int q = 0; q < quarter; q++) { float oldX = x[i]; x[i] = z[i]; z[i] = -oldX; } }
	}

	@Test
	public void unbendsNativeCornerToOriginalDiagonalForAllFourOrientationsWithoutReversingFaces()
	{
		for (int quarter = 0; quarter < 4; quarter++)
		{
			float[] x = {-16, -16, 64, -16, -16, 64, -64, 64};
			float[] z = {-64, 16, 16, -64, 16, 16, 64, -64};
			ModelData source = strip(ModelData.class, x, new float[] {-400,-400,-400,0,0,0,0,0}, z);
			float[] targetX = {-64,-8,64,-64,-8,64,64}, targetZ = {-64,8,64,-64,8,64,-64};
			rotate(targetX, targetZ, quarter);
			float[] originalX = targetX.clone(), originalZ = targetZ.clone();
			Model target = strip(Model.class, targetX, new float[] {-250,-250,-250,0,0,0,160}, targetZ);
			int[] indices = source.getFaceIndices2().clone();
			assertTrue(WallSurfaceFitter.fit(source, target, quarter * 512, 9));
			for (int i = 0; i < 6; i++) { assertEquals(targetX[i], x[i], 1); assertEquals(targetZ[i], z[i], 1); }
			assertEquals(-250, source.getVerticesY()[0], 0); assertEquals(0, source.getVerticesY()[3], 0);
			assertArrayEquals(originalX, target.getVerticesX(), 0); assertArrayEquals(originalZ, target.getVerticesZ(), 0);
			assertArrayEquals(indices, source.getFaceIndices2());
		}
	}

	@Test
	public void triangularCornerPostUsesItsActualSkinNotTheUndergroundTileBacking()
	{
		ModelData source = strip(ModelData.class, new float[] {-64,-40,0,-64,-40,0,64},
			new float[] {-400,-400,-400,0,0,0,0}, new float[] {0,40,64,0,40,64,-64});
		Model target = strip(Model.class, new float[] {-64,-59,-54,-64,-59,-54,64},
			new float[] {-250,-250,-250,0,0,0,160}, new float[] {54,59,64,54,59,64,-64});
		assertTrue(WallSurfaceFitter.fit(source, target, 0, 1));
		for (int i = 0; i < 6; i++) { assertEquals(target.getVerticesX()[i], source.getVerticesX()[i], 1); assertEquals(target.getVerticesZ()[i], source.getVerticesZ()[i], 1); }
	}

	@Test
	public void unknownReversedOrMalformedSkinRetainsDetachedVertices()
	{
		float[] x = {-16,-16,64,-16,-16,64}, z = {-64,16,16,-64,16,16};
		ModelData source = strip(ModelData.class, x, new float[] {-400,-400,-400,0,0,0}, z);
		Model target = strip(Model.class, x.clone(), new float[] {-250,-250,-250,0,0,0}, z.clone());
		float[] before = x.clone();
		assertFalse(WallSurfaceFitter.fit(source, target, 0, 2));
		when(source.getFaceIndices2()).thenReturn(new int[] {4,3,5,4});
		when(source.getFaceIndices3()).thenReturn(new int[] {1,4,2,5});
		assertFalse(WallSurfaceFitter.fit(source, target, 0, 9)); assertArrayEquals(before, x, 0);
		when(source.getFaceIndices1()).thenReturn(new int[] {99,0,1,1});
		assertFalse(WallSurfaceFitter.fit(source, target, 0, 9)); assertArrayEquals(before, x, 0);
		when(source.getVerticesCount()).thenReturn(9000);
		assertFalse(WallSurfaceFitter.fit(source, target, 0, 9)); assertArrayEquals(before, x, 0);
	}

	@Test
	public void hiddenOriginalFacesAreNotUsedAsAnchors()
	{
		float[] x = {-16,-16,64,-16,-16,64}, z = {-64,16,16,-64,16,16};
		ModelData source = strip(ModelData.class, x, new float[] {-400,-400,-400,0,0,0}, z);
		Model target = strip(Model.class, x.clone(), new float[] {-250,-250,-250,0,0,0}, z.clone());
		when(target.getFaceColors3()).thenReturn(new int[] {-2,-2,-2,-2});
		float[] before = x.clone();
		assertFalse(WallSurfaceFitter.fit(source, target, 0, 9)); assertArrayEquals(before, x, 0);
	}
}
