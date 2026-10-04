package com.barrowsthemes;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WallModelReplacementsTest
{
	private Client client;
	private Scene scene;
	private WallModelReplacements replacements;
	private Model original;
	private int[] colours;
	private final List<RuneLiteObject> created = new ArrayList<>();
	private final List<ModelData> fittedCopies = new ArrayList<>();

	@Before
	public void setup()
	{
		client = mock(Client.class);
		scene = mock(Scene.class);
		when(scene.getBaseX()).thenReturn(3550);
		when(scene.getBaseY()).thenReturn(9690);
		replacements = new WallModelReplacements();
		original = mock(Model.class);
		when(original.getVerticesCount()).thenReturn(4);
		when(original.getVerticesX()).thenReturn(new float[] {-64, 64, -64, 64});
		when(original.getVerticesY()).thenReturn(new float[] {-250, -250, 160, 160});
		when(original.getVerticesZ()).thenReturn(new float[] {-64, 64, -64, 64});
		when(original.getFaceCount()).thenReturn(2);
		when(original.getFaceIndices1()).thenReturn(new int[] {0, 0});
		when(original.getFaceIndices2()).thenReturn(new int[] {1, 3});
		when(original.getFaceIndices3()).thenReturn(new int[] {3, 2});
		colours = new int[] {10, -1, -2};
		when(original.getFaceColors3()).thenReturn(colours);
		when(client.createRuneLiteObject()).thenAnswer(invocation -> {
			RuneLiteObject object = mock(RuneLiteObject.class);
			when(object.isActive()).thenReturn(true);
			created.add(object);
			return object;
		});
	}

	private ModelData source()
	{
		return source(new float[] {-128,128,-128,128}, new float[] {-400,-400,0,0}, new float[] {-128,128,-128,128});
	}

	private ModelData source(float[] x, float[] y, float[] z)
	{
		ModelData source = mock(ModelData.class);
		when(source.shallowCopy()).thenAnswer(invocation -> {
			ModelData copy = mock(ModelData.class);
			fittedCopies.add(copy);
			when(copy.cloneVertices()).thenReturn(copy);
			when(copy.cloneColors()).thenReturn(copy);
			when(copy.getVerticesCount()).thenReturn(4);
			when(copy.getVerticesX()).thenReturn(x.clone());
			when(copy.getVerticesY()).thenReturn(y.clone());
			when(copy.getVerticesZ()).thenReturn(z.clone());
			when(copy.getFaceCount()).thenReturn(2);
			when(copy.getFaceIndices1()).thenReturn(new int[] {0, 0});
			when(copy.getFaceIndices2()).thenReturn(new int[] {1, 3});
			when(copy.getFaceIndices3()).thenReturn(new int[] {3, 2});
			when(copy.light(anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mock(Model.class));
			return copy;
		});
		return source;
	}

	private Tile wall(int type, int orientation, int localX)
	{
		Tile tile = mock(Tile.class);
		when(tile.getLocalLocation()).thenReturn(new LocalPoint(localX, 64));
		when(tile.getGameObjects()).thenReturn(new GameObject[0]);
		WallObject wall = mock(WallObject.class);
		when(tile.getWallObject()).thenReturn(wall);
		when(wall.getId()).thenReturn(ObjectID.BARROWS_CRYPT);
		when(wall.getConfig()).thenReturn(type);
		when(wall.getOrientationA()).thenReturn(orientation);
		when(wall.getRenderable1()).thenReturn(original);
		when(wall.getLocalLocation()).thenReturn(new LocalPoint(localX, 64));
		when(wall.getZ()).thenReturn(-100);
		return tile;
	}

	private void tiles(Tile... tiles) { when(scene.getTiles()).thenReturn(new Tile[][][] {{tiles}}); }

	private void originalQuarter(int quarter)
	{
		float[] x = {-64, 64, -64, 64}, z = {-64, 64, -64, 64};
		for (int i = 0; i < x.length; i++)
		{
			for (int turn = 0; turn < quarter; turn++) { float oldX = x[i]; x[i] = z[i]; z[i] = -oldX; }
		}
		when(original.getVerticesX()).thenReturn(x); when(original.getVerticesZ()).thenReturn(z);
	}

	@Test
	public void createsVisualOnlyObjectsAndRestoresOriginalOnClear()
	{
		tiles(wall(0, 1, 64));
		ModelData source = source();
		when(client.loadModelData(11909)).thenReturn(source);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {-2, -2, -2}, colours);
		assertEquals(1, created.size());
		verify(created.get(0)).setActive(true);
		verify(created.get(0)).setLocation(new LocalPoint(64, 64), 0);
		verify(created.get(0)).setZ(-100);
		verify(created.get(0)).setOrientation(0);
		verify(created.get(0)).setRadius(65);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		verify(client, times(1)).createRuneLiteObject();
		verify(client, times(1)).loadModelData(11909);
		assertTrue(replacements.clear());
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(created.get(0)).setActive(false);
		verify(scene, never()).removeGameObject(any());
		verify(scene, never()).removeTile(any());
		assertFalse(replacements.clear());
	}

	@Test
	public void missingAssetRetainsOriginalAndRetriesWhenAvailable()
	{
		tiles(wall(0, 1, 64));
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		assertTrue(created.isEmpty());
		assertTrue(replacements.summary().contains("1 source models awaiting cache"));
		doReturn(source()).when(client).loadModelData(11909);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {-2, -2, -2}, colours);
	}

	@Test
	public void themeSwitchStopsRetryingAssetsThatAreNoLongerRequired()
	{
		tiles(wall(0, 1, 64));
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		doReturn(source()).when(client).loadModelData(32435);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.CHAMBERS_OF_XERIC));
		verify(client, times(1)).loadModelData(11909);
		assertTrue(replacements.summary().contains("0 source models awaiting cache"));
	}

	@Test
	public void sharedFacesRequireEveryWallPlacementToBeReady()
	{
		tiles(wall(0, 1, 64), wall(1, 16, 192));
		doReturn(source()).when(client).loadModelData(11909);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		assertEquals(1, created.size());
		verify(created.get(0), never()).setActive(true);
		verify(created.get(0)).setActive(false);
		doReturn(source()).when(client).loadModelData(11889);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {-2, -2, -2}, colours);
		assertTrue(replacements.summary().startsWith("2/2"));
	}

	@Test
	public void protectsAliasesSharedWithFurnitureAndWallsOutsideCrypt()
	{
		Tile tile = wall(0, 1, 64);
		GameObject chest = mock(GameObject.class);
		Model alias = mock(Model.class);
		when(alias.getFaceColors3()).thenReturn(colours);
		when(chest.getId()).thenReturn(ObjectID.BARROWS_STONE_CHEST_CLOSED);
		when(chest.getRenderable()).thenReturn(alias);
		when(tile.getGameObjects()).thenReturn(new GameObject[] {chest});
		tiles(tile);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		assertTrue(created.isEmpty());
		replacements.clear();
		tiles(wall(0, 1, 64), wall(0, 1, 16448));
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
	}

	@Test
	public void activationFailureLeavesOriginalAndCleansStagedObjects()
	{
		tiles(wall(0, 1, 64));
		doReturn(source()).when(client).loadModelData(11909);
		RuneLiteObject object = mock(RuneLiteObject.class);
		when(client.createRuneLiteObject()).thenReturn(object);
		doThrow(new IllegalStateException("test activation failure")).when(object).setActive(true);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(object).setActive(false);
	}

	@Test
	public void silentRegistrationRejectionKeepsOriginalFacesVisible()
	{
		tiles(wall(0, 1, 64));
		doReturn(source()).when(client).loadModelData(11909);
		RuneLiteObject rejected = mock(RuneLiteObject.class);
		when(client.createRuneLiteObject()).thenReturn(rejected);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(rejected).setActive(false);
	}

	@Test
	public void cornerGameObjectsAreDeduplicatedAndKeepPlaneAndRenderOrientation()
	{
		Tile first = wall(0, 1, 64), second = wall(0, 1, 192);
		when(first.getWallObject()).thenReturn(null);
		when(second.getWallObject()).thenReturn(null);
		GameObject corner = mock(GameObject.class);
		when(corner.getId()).thenReturn(ObjectID.BARROWS_CRYPT_INNER_PURPLE);
		when(corner.getConfig()).thenReturn(9 | 64);
		when(corner.getRenderable()).thenReturn(original);
		when(corner.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(corner.getPlane()).thenReturn(3);
		when(corner.getZ()).thenReturn(200);
		when(corner.getOrientation()).thenReturn(512);
		when(corner.getModelOrientation()).thenReturn(256);
		when(first.getGameObjects()).thenReturn(new GameObject[] {corner});
		when(second.getGameObjects()).thenReturn(new GameObject[] {corner});
		tiles(first, second);
		doReturn(source()).when(client).loadModelData(11890);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertEquals(1, created.size());
		verify(created.get(0)).setLocation(new LocalPoint(64, 64), 3);
		verify(created.get(0)).setZ(200);
		verify(created.get(0)).setOrientation(256);
		verify(fittedCopies.get(0)).light(ModelData.DEFAULT_AMBIENT + 20, ModelData.DEFAULT_CONTRAST * 2,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
	}

	@Test
	public void animatedRenderablesAreNotReplacedOrResolvedToCachedModels()
	{
		Tile tile = wall(0, 1, 64);
		Renderable animated = mock(Renderable.class);
		when(tile.getWallObject().getRenderable1()).thenReturn(animated);
		tiles(tile);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(animated, never()).getModel();
		assertTrue(created.isEmpty());
	}

	@Test
	public void cleanupFailureDoesNotPreventRestoringArraysOrRemovingOtherObjects()
	{
		tiles(wall(0, 1, 64), wall(0, 1, 192));
		doReturn(source()).when(client).loadModelData(11909);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		doThrow(new IllegalStateException("test disappearing view")).when(created.get(0)).setActive(false);
		assertTrue(replacements.clear());
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(created.get(1)).setActive(false);
	}

	@Test
	public void independentlyReplacesSmallStepsWithOriginalHeightAndRestoresOnToggle()
	{
		Tile tile = wall(0, 1, 64);
		when(tile.getWallObject()).thenReturn(null);
		GroundObject step = mock(GroundObject.class);
		when(step.getId()).thenReturn(ObjectID.BARROWS_SKEWSTEPS_PURPLE);
		when(client.getObjectDefinition(ObjectID.BARROWS_SKEWSTEPS_PURPLE)).thenReturn(mock(net.runelite.api.ObjectComposition.class));
		when(step.getConfig()).thenReturn(22 | 64);
		when(step.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(step.getZ()).thenReturn(-75);
		when(step.getRenderable()).thenReturn(original);
		when(tile.getGroundObject()).thenReturn(step);
		tiles(tile);
		doReturn(source()).when(client).loadModelData(11942);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, false, true, true));
		assertArrayEquals(new int[] {-2, -2, -2}, colours);
		verify(created.get(0)).setZ(-75);
		verify(fittedCopies.get(0)).light(ModelData.DEFAULT_AMBIENT + 20, ModelData.DEFAULT_CONTRAST,
			ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, false, true, true));
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, true, false, false));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(created.get(0)).setActive(false);
	}

	@Test
	public void sceneryWithUnexpectedActionsOrUnavailableDefinitionsIsProtected()
	{
		Tile tile = wall(0, 1, 64);
		when(tile.getWallObject()).thenReturn(null);
		GroundObject rubble = mock(GroundObject.class);
		when(rubble.getId()).thenReturn(ObjectID.BARROWS_BRICKS_PILE_1);
		when(rubble.getConfig()).thenReturn(22);
		when(rubble.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(rubble.getRenderable()).thenReturn(original);
		when(tile.getGroundObject()).thenReturn(rubble);
		tiles(tile);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, false, false, true));
		net.runelite.api.ObjectComposition definition = mock(net.runelite.api.ObjectComposition.class);
		when(definition.getActions()).thenReturn(new String[] {"Search"});
		when(client.getObjectDefinition(ObjectID.BARROWS_BRICKS_PILE_1)).thenReturn(definition);
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, false, false, true));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		assertTrue(created.isEmpty());
	}

	@Test
	public void variantsStayStableThroughTicksAndClearRebuildsEvenWithSharedOriginals()
	{
		tiles(wall(0, 1, 64), wall(0, 1, 192));
		doReturn(source()).when(client).loadModelData(anyInt());
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, true, true, false));
		verify(client).loadModelData(11909);
		verify(client).loadModelData(11912);
		assertEquals(2, created.size());
		assertFalse(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, true, true, false));
		replacements.clear();
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS, true, true, false));
		verify(client, times(2)).loadModelData(11909);
		verify(client, times(2)).loadModelData(11912);
	}

	@Test
	public void coxFrontSurfaceMustMatchOriginalEdgeDespiteIdenticalWholeMeshBounds()
	{
		float[] originalX = {-64, -64, -64, -64, 64, 64};
		when(original.getVerticesCount()).thenReturn(6);
		when(original.getVerticesX()).thenReturn(originalX);
		when(original.getVerticesY()).thenReturn(new float[] {-250, -250, 0, 0, 160, 160});
		when(original.getVerticesZ()).thenReturn(new float[] {-64, 64, -64, 64, -64, 64});
		ModelData data = source();
		doAnswer(invocation -> {
			ModelData copy = mock(ModelData.class); fittedCopies.add(copy);
			when(copy.cloneVertices()).thenReturn(copy); when(copy.cloneColors()).thenReturn(copy);
			when(copy.getVerticesCount()).thenReturn(6);
			when(copy.getVerticesX()).thenReturn(new float[] {0, 0, 0, 0, -64, 64});
			when(copy.getVerticesY()).thenReturn(new float[] {-400, -400, 0, 0, 0, 0});
			when(copy.getVerticesZ()).thenReturn(new float[] {-64, 64, -64, 64, -64, 64});
			when(copy.getFaceCount()).thenReturn(2);
			when(copy.getFaceIndices1()).thenReturn(new int[] {0, 0});
			when(copy.getFaceIndices2()).thenReturn(new int[] {1, 3});
			when(copy.getFaceIndices3()).thenReturn(new int[] {3, 2});
			Model lit = mock(Model.class);
			when(lit.getVerticesCount()).thenReturn(6);
			when(lit.getVerticesX()).thenAnswer(call -> copy.getVerticesX());
			when(lit.getVerticesY()).thenAnswer(call -> copy.getVerticesY());
			when(lit.getVerticesZ()).thenAnswer(call -> copy.getVerticesZ());
			when(copy.light(anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(lit);
			return copy;
		}).when(data).shallowCopy();
		tiles(wall(0, 1, 64)); when(client.loadModelData(32435)).thenReturn(data);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.CHAMBERS_OF_XERIC));
		assertEquals(-64, fittedCopies.get(0).getVerticesX()[0], .01);
		assertEquals(0, fittedCopies.get(0).getVerticesY()[2], .01);
		assertArrayEquals(new float[] {-64, -64, -64, -64, 64, 64}, originalX, 0);
		assertEquals(-72, fittedCopies.get(0).getVerticesZ()[0], 0);
		assertEquals(72, fittedCopies.get(0).getVerticesZ()[1], 0);
		verify(created.get(0)).setRadius(73);
	}

	@Test
	public void bakedCoxCornersMustNotApplyPlacementRotationAgainAtRenderTime()
	{
		for (int quarter = 0; quarter < 4; quarter++)
		{
			replacements.clear(); created.clear(); originalQuarter(quarter); colours = new int[] {10, -1, -2};
			when(original.getFaceColors3()).thenReturn(colours);
			Tile tile = wall(0, 1, 64); when(tile.getWallObject()).thenReturn(null);
			GameObject corner = mock(GameObject.class);
			when(corner.getId()).thenReturn(ObjectID.BARROWS_CRYPT_PURPLE);
			when(corner.getConfig()).thenReturn(9 | quarter << 6);
			when(corner.getOrientation()).thenReturn(quarter * 512);
			// As documented by GameObject: lighting has already baked the placement rotation.
			when(corner.getModelOrientation()).thenReturn(0);
			when(corner.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
			when(corner.getRenderable()).thenReturn(original);
			when(tile.getGameObjects()).thenReturn(new GameObject[] {corner}); tiles(tile);
			doReturn(source()).when(client).loadModelData(32432);
			assertTrue(replacements.refresh(client, scene, BarrowsTheme.CHAMBERS_OF_XERIC));
			verify(created.get(0)).setOrientation(0);
		}
	}

	@Test
	public void coxSurfaceCoordinatesComposeOnceWithEveryWallMaskAndCryptPalette()
	{
		for (int id : new int[] {ObjectID.BARROWS_CRYPT, ObjectID.BARROWS_CRYPT_PURPLE})
		{
			for (int quarter = 0; quarter < 4; quarter++)
			{
				replacements.clear(); created.clear(); fittedCopies.clear();
				float[] x = {-64,-54,-64,-54}, z = {54,64,54,64};
				for (int i = 0; i < x.length; i++)
				{
					for (int q = 0; q < quarter; q++) { float oldX = x[i]; x[i] = z[i]; z[i] = -oldX; }
				}
				when(original.getVerticesX()).thenReturn(x); when(original.getVerticesZ()).thenReturn(z);
				Tile tile = wall(1, 16 << quarter, 64);
				when(tile.getWallObject().getId()).thenReturn(id);
				when(tile.getWallObject().getConfig()).thenReturn(1 | quarter << 6);
				tiles(tile);
				doReturn(source(new float[] {-64,0,-64,0}, new float[] {-400,-400,0,0}, new float[] {0,64,0,64})).when(client).loadModelData(32439);
				assertTrue(replacements.refresh(client, scene, BarrowsTheme.CHAMBERS_OF_XERIC));
				int rotation = quarter;
				float[] expectedX = {-77,51,77,-51}, expectedZ = {51,77,-51,-77};
				assertEquals(expectedX[rotation], fittedCopies.get(0).getVerticesX()[0], 0.1f);
				assertEquals(expectedZ[rotation], fittedCopies.get(0).getVerticesZ()[0], 0.1f);
				verify(fittedCopies.get(0)).light(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST * 2,
					ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
				verify(created.get(0)).setOrientation(0);
			}
		}
	}

	@Test
	public void cornerNativeFacingOffsetsComposeWithAllFourPlacementRotations()
	{
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			if (!theme.supportsWallModels()) { continue; }
			for (int id : new int[] {ObjectID.BARROWS_CRYPT, ObjectID.BARROWS_CRYPT_INNER_PURPLE})
			{
				for (int quarter = 0; quarter < 4; quarter++)
				{
					replacements.clear(); created.clear(); fittedCopies.clear(); originalQuarter(quarter);
					Tile tile = wall(0, 1, 64);
					when(tile.getWallObject()).thenReturn(null);
					GameObject corner = mock(GameObject.class);
					when(corner.getId()).thenReturn(id);
					when(corner.getConfig()).thenReturn(9 | quarter << 6);
					when(corner.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
					when(corner.getRenderable()).thenReturn(original);
					when(tile.getGameObjects()).thenReturn(new GameObject[] {corner});
					tiles(tile);
					doReturn(source()).when(client).loadModelData(anyInt());
					assertTrue(replacements.refresh(client, scene, theme));
					int nativeQuarter = WallModelSources.extraRotation(theme, id, 9) / 512;
					int rotation = (quarter + nativeQuarter) & 3;
					float extent = theme == BarrowsTheme.CHAMBERS_OF_XERIC ? 72 : 64;
					assertEquals(rotation < 2 ? -extent : extent, fittedCopies.get(0).getVerticesX()[0], 0.1f);
					assertEquals(rotation == 0 || rotation == 3 ? -extent : extent, fittedCopies.get(0).getVerticesZ()[0], 0.1f);
					verify(fittedCopies.get(0)).light(ModelData.DEFAULT_AMBIENT + (theme == BarrowsTheme.ZANARIS ? 20 : 0),
						ModelData.DEFAULT_CONTRAST * 2, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
					verify(created.get(0)).setOrientation(0);
				}
			}
		}
	}

	@Test
	public void switchingToUnavailableThemeModelsRemovesOldModelsAndRestoresFaces()
	{
		tiles(wall(0, 1, 64));
		doReturn(source()).when(client).loadModelData(11909);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.INFERNO));
		verify(created.get(0)).setActive(false);
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		assertEquals(1, created.size());
	}

	@Test
	public void straightWallsUseSofterLightingForEveryThemeAndQuarterTurn()
	{
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			for (int quarter = 0; quarter < 4; quarter++)
			{
				replacements.clear(); created.clear(); fittedCopies.clear(); originalQuarter(quarter);
				Tile tile = wall(0, 1 << quarter, 64);
				when(tile.getWallObject().getConfig()).thenReturn(quarter << 6);
				tiles(tile); doReturn(source()).when(client).loadModelData(anyInt());
				assertTrue(replacements.refresh(client, scene, theme));
				verify(fittedCopies.get(0)).light(ModelData.DEFAULT_AMBIENT + (theme == BarrowsTheme.ZANARIS ? 20 : 0),
					ModelData.DEFAULT_CONTRAST * 2, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
				verify(created.get(0)).setOrientation(0);
			}
		}
	}

	@Test
	public void everyThemeRegistersItsOwnWallsWithSofterLightingAndCleansUp()
	{
		tiles(wall(0, 1, 64));
		doReturn(source()).when(client).loadModelData(anyInt());
		RuneLiteObject previous = null;
		for (BarrowsTheme theme : BarrowsTheme.values())
		{
			assertTrue(replacements.refresh(client, scene, theme));
			if (previous != null) { verify(previous).setActive(false); }
			previous = created.get(created.size() - 1);
			WallModelSources.Source selected = WallModelSources.select(theme, ObjectID.BARROWS_CRYPT, 0,
				new net.runelite.api.coords.WorldPoint(3550, 9690, 0), false);
			verify(client).loadModelData(selected.modelId);
			verify(fittedCopies.get(fittedCopies.size() - 1)).light(selected.ambient(), selected.contrast() * 2,
				ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
			assertArrayEquals(new int[] {-2, -2, -2}, colours);
			assertFalse(replacements.refresh(client, scene, theme));
		}
		assertTrue(replacements.clear());
		verify(previous).setActive(false);
		assertArrayEquals(new int[] {10, -1, -2}, colours);
	}

	private void observer(int localX, int plane)
	{
		net.runelite.api.Player player = mock(net.runelite.api.Player.class);
		net.runelite.api.WorldView view = mock(net.runelite.api.WorldView.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(localX, 64));
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(view.getPlane()).thenReturn(plane);
	}

	@Test
	public void shortCornerReportShowsSourceFacingAndRegistrationWithoutChangingTheScene()
	{
		observer(64, 0);
		Tile tile = wall(0, 1, 64); when(tile.getWallObject()).thenReturn(null);
		GameObject corner = mock(GameObject.class);
		when(corner.getId()).thenReturn(ObjectID.BARROWS_CRYPT);
		when(corner.getConfig()).thenReturn(9 | 64);
		when(corner.getOrientation()).thenReturn(512);
		when(corner.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(corner.getRenderable()).thenReturn(original);
		when(tile.getGameObjects()).thenReturn(new GameObject[] {corner}); tiles(tile);
		doReturn(source()).when(client).loadModelData(11890);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		clearInvocations(client, created.get(0));
		String report = replacements.cornerReport(client, scene, BarrowsTheme.ZANARIS, true);
		assertTrue(report.contains("Wall corner report v3"));
		assertTrue(report.contains("#20728 @ 3550,9690,0 type=9 slot=0 sourceModel=11890"));
		assertTrue(report.contains("placementRot=512 extraRot=1024 bakedRot=1536 renderRot=0"));
		assertTrue(report.contains("objectRot=512"));
		assertTrue(report.contains("registered group; original faces hidden"));
		assertTrue(report.contains("original: x=-64..64, y=-250..160, z=-64..64"));
		assertArrayEquals(new int[] {-2, -2, -2}, colours);
		verify(client, never()).loadModelData(anyInt()); verify(client, never()).createRuneLiteObject();
		verify(created.get(0), never()).setActive(anyBoolean());
		assertEquals(1, created.size());
	}

	@Test
	public void shortCornerReportFiltersStraightAndDistantPartsAndWorksWithReplacementOff()
	{
		observer(64, 0);
		tiles(wall(0, 1, 64), wall(1, 16, 192), wall(1, 16, 1600));
		String report = replacements.cornerReport(client, scene, BarrowsTheme.ZANARIS, false);
		assertEquals(1, report.split("sourceModel=", -1).length - 1);
		assertTrue(report.contains("type=1")); assertTrue(report.contains("disabled; original retained"));
		assertTrue(report.contains("masks=16/0"));
		assertArrayEquals(new int[] {10, -1, -2}, colours);
		verify(client, never()).loadModelData(anyInt()); verify(client, never()).createRuneLiteObject();
		when(client.getLocalPlayer()).thenReturn(null);
		assertEquals("Log in to inspect wall corners.", replacements.cornerReport(client, scene, BarrowsTheme.ZANARIS, false));
	}

	@Test
	public void shortCornerReportCapsOutputAndExplainsEmptyResults()
	{
		observer(64, 0);
		Tile[] nearby = new Tile[20];
		for (int i = 0; i < nearby.length; i++) { nearby[i] = wall(1, 16, 64 + i * 10); }
		tiles(nearby);
		String report = replacements.cornerReport(client, scene, BarrowsTheme.ZANARIS, false);
		assertEquals(12, report.split("sourceModel=", -1).length - 1);
		assertTrue(report.contains("8 farther parts omitted"));
		observer(64, 3);
		assertTrue(replacements.cornerReport(client, scene, BarrowsTheme.ZANARIS, false).contains("No supported static corners nearby"));
	}

	@Test
	public void themeSwitchRemovesOldObjectsAndUsesNewSource()
	{
		tiles(wall(0, 1, 64));
		doReturn(source()).when(client).loadModelData(11909);
		doReturn(source()).when(client).loadModelData(32435);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.ZANARIS));
		RuneLiteObject previous = created.get(0);
		assertTrue(replacements.refresh(client, scene, BarrowsTheme.CHAMBERS_OF_XERIC));
		verify(previous).setActive(false);
		verify(client).loadModelData(32435);
		assertArrayEquals(new int[] {-2, -2, -2}, colours);
		replacements.clear();
		assertArrayEquals(new int[] {10, -1, -2}, colours);
	}
}
