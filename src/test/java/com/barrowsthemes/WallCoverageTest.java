package com.barrowsthemes;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.ui.ClientToolbar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WallCoverageTest
{
	@Mock private Client client;
	@Mock private RenderCallbackManager renderCallbacks;
	@Mock private ClientThread clientThread;
	@Mock private ClientToolbar clientToolbar;
	@Mock private BarrowsThemesConfig config;
	@InjectMocks private BarrowsThemesPlugin plugin;
	private Tile tile;
	private int[] colours;
	private Model model;
	private ObjectComposition definition;

	@Before
	public void setup()
	{
		WorldView view = mock(WorldView.class);
		Scene scene = mock(Scene.class);
		tile = mock(Tile.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(view.getScene()).thenReturn(scene);
		when(scene.getMapRegions()).thenReturn(new int[] {14231});
		when(scene.getBaseX()).thenReturn(3550);
		when(scene.getBaseY()).thenReturn(9690);
		when(scene.getTiles()).thenReturn(new Tile[][][] {{{tile}}});
		when(tile.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(config.walls()).thenReturn(true);
		when(config.theme()).thenReturn(BarrowsTheme.ZANARIS);
		when(config.floorTexture()).thenReturn(-1);
		when(config.wallTexture()).thenReturn(-1);
		model = mock(Model.class);
		colours = new int[] {12341};
		when(model.getFaceColors1()).thenReturn(colours);
		when(model.getFaceColors2()).thenReturn(new int[] {12342});
		when(model.getFaceColors3()).thenReturn(new int[] {12343});
		definition = mock(ObjectComposition.class);
		when(client.getObjectDefinition(ObjectID.BARROWS_SKEWSTEPS_CORNER)).thenReturn(definition);
		GameObject corner = mock(GameObject.class);
		when(corner.getId()).thenReturn(ObjectID.BARROWS_SKEWSTEPS_CORNER);
		when(corner.getRenderable()).thenReturn(model);
		when(tile.getGameObjects()).thenReturn(new GameObject[] {corner});
		plugin.startUp();
	}

	@Test
	public void themesBothDecorativeCornerModels()
	{
		when(tile.getGameObjects()).thenReturn(new GameObject[0]);
		DecorativeObject decor = mock(DecorativeObject.class);
		when(decor.getId()).thenReturn(ObjectID.BARROWS_SKEWSTEPS_CORNER);
		when(decor.getRenderable()).thenReturn(model);
		Model second = mock(Model.class);
		int[] secondColours = {12341};
		when(second.getFaceColors1()).thenReturn(secondColours);
		when(second.getFaceColors2()).thenReturn(new int[] {12342});
		when(second.getFaceColors3()).thenReturn(new int[] {12343});
		when(decor.getRenderable2()).thenReturn(second);
		when(tile.getDecorativeObject()).thenReturn(decor);
		plugin.onGameTick(new GameTick());
		assertNotEquals(12341, colours[0]);
		assertNotEquals(12341, secondColours[0]);
	}

	@Test
	public void leavesInteractiveCornerUnchanged()
	{
		when(definition.getActions()).thenReturn(new String[] {"Open"});
		plugin.onGameTick(new GameTick());
		assertEquals(12341, colours[0]);
	}

	@Test
	public void protectsCornerModelSharedWithUnrelatedProp()
	{
		GameObject prop = mock(GameObject.class);
		when(prop.getId()).thenReturn(ObjectID.BARROWS_STONE_CHEST_CLOSED);
		when(prop.getRenderable()).thenReturn(model);
		GameObject corner = tile.getGameObjects()[0];
		when(tile.getGameObjects()).thenReturn(new GameObject[] {corner, prop});
		plugin.onGameTick(new GameTick());
		assertEquals(12341, colours[0]);
	}

	@Test
	public void themesPreviouslyUnselectedTombFloorSoilDetails()
	{
		when(tile.getGameObjects()).thenReturn(new GameObject[0]);
		when(config.curatedMaterials()).thenReturn(true);
		int[] ids = {ObjectID.BARROWS_DUGUPSOIL_1_PURPLE, ObjectID.BARROWS_DUGUPSOIL_2_PURPLE, ObjectID.BARROWS_DUGUPSOIL_3_PURPLE};
		for (int id : ids)
		{
			Model detail = mock(Model.class);
			int[] colours = {64018};
			when(detail.getFaceColors1()).thenReturn(colours);
			when(detail.getFaceColors2()).thenReturn(new int[] {64018});
			when(detail.getFaceColors3()).thenReturn(new int[] {64018});
			GroundObject ground = mock(GroundObject.class);
			when(ground.getId()).thenReturn(id);
			when(ground.getRenderable()).thenReturn(detail);
			when(tile.getGroundObject()).thenReturn(ground);
			when(client.getObjectDefinition(id)).thenReturn(mock(ObjectComposition.class));
			plugin.onGameTick(new GameTick());
			assertEquals(24524, colours[0]);
		}
	}

	@Test
	public void themesStonesCoffinsAndStairsWithoutChangingActions()
	{
		int[] ids = {ObjectID.BARROWS_BRICKS_PILE_1, ObjectID.BARROW_AHRIM_SARCOPHAGUS, ObjectID.BARROWS_STAIRS_AHRIM};
		for (int id : ids)
		{
			int[] face = {12341};
			Model sceneryModel = mock(Model.class);
			when(sceneryModel.getFaceColors1()).thenReturn(face);
			when(sceneryModel.getFaceColors2()).thenReturn(new int[] {12342});
			when(sceneryModel.getFaceColors3()).thenReturn(new int[] {12343});
			ObjectComposition sceneryDefinition = mock(ObjectComposition.class);
			String[] actions = id == ObjectID.BARROWS_BRICKS_PILE_1 ? new String[0] : new String[] {"Search", "Climb-up"};
			when(sceneryDefinition.getActions()).thenReturn(actions);
			when(client.getObjectDefinition(id)).thenReturn(sceneryDefinition);
			GameObject object = mock(GameObject.class);
			when(object.getId()).thenReturn(id);
			when(object.getRenderable()).thenReturn(sceneryModel);
			when(tile.getGameObjects()).thenReturn(new GameObject[] {object});
			plugin.onGameTick(new GameTick());
			assertNotEquals("Scenery ID " + id + " must be themed", 12341, face[0]);
			assertSame(actions, sceneryDefinition.getActions());
		}
	}

	@Test
	public void appliesOnlyAvailableTextureIds()
	{
		TextureProvider provider = mock(TextureProvider.class);
		Texture[] textures = new Texture[8];
		textures[7] = mock(Texture.class);
		when(provider.getTextures()).thenReturn(textures);
		when(client.getTextureProvider()).thenReturn(provider);
		when(config.wallTexture()).thenReturn(7);
		short[] faceTextures = {3};
		when(model.getFaceTextures()).thenReturn(faceTextures);
		plugin.onGameTick(new GameTick());
		assertArrayEquals(new short[] {7}, faceTextures);
	}

	@Test
	public void invalidTextureFallsBackToPalette()
	{
		TextureProvider provider = mock(TextureProvider.class);
		when(provider.getTextures()).thenReturn(new Texture[8]);
		when(client.getTextureProvider()).thenReturn(provider);
		when(config.wallTexture()).thenReturn(999);
		short[] faceTextures = {3};
		when(model.getFaceTextures()).thenReturn(faceTextures);
		plugin.onGameTick(new GameTick());
		assertArrayEquals(new short[] {-1}, faceTextures);
	}

	@Test
	public void pluginRemovesReplacementObjectsBeforeRestoringOriginalMaterials()
	{
		when(config.replaceWallModels()).thenReturn(true);
		when(tile.getGameObjects()).thenReturn(new GameObject[0]);
		WallObject wall = mock(WallObject.class);
		when(tile.getWallObject()).thenReturn(wall);
		when(wall.getId()).thenReturn(ObjectID.BARROWS_CRYPT);
		when(wall.getOrientationA()).thenReturn(1);
		when(wall.getRenderable1()).thenReturn(model);
		when(wall.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(client.getObjectDefinition(ObjectID.BARROWS_CRYPT)).thenReturn(mock(ObjectComposition.class));
		when(model.getVerticesCount()).thenReturn(4);
		when(model.getVerticesX()).thenReturn(new float[] {-64, 64, -64, 64});
		when(model.getVerticesY()).thenReturn(new float[] {-250, -250, 160, 160});
		when(model.getVerticesZ()).thenReturn(new float[] {-64, 64, -64, 64});
		ModelData source = mock(ModelData.class), copy = mock(ModelData.class);
		when(client.loadModelData(11909)).thenReturn(source);
		when(source.shallowCopy()).thenReturn(copy);
		when(copy.cloneVertices()).thenReturn(copy);
		when(copy.cloneColors()).thenReturn(copy);
		when(copy.getVerticesCount()).thenReturn(4);
		when(copy.getVerticesX()).thenReturn(new float[] {-128, 128, -128, 128});
		when(copy.getVerticesY()).thenReturn(new float[] {-400, -400, 0, 0});
		when(copy.getVerticesZ()).thenReturn(new float[] {-128, 128, -128, 128});
		when(copy.light(anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mock(Model.class));
		RuneLiteObject replacement = mock(RuneLiteObject.class);
		when(replacement.isActive()).thenReturn(true);
		when(client.createRuneLiteObject()).thenReturn(replacement);
		doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
			.when(clientThread).invoke(any(Runnable.class));
		int[] originalColours = model.getFaceColors3();
		plugin.onGameTick(new GameTick());
		assertArrayEquals(new int[] {-2}, originalColours);
		verify(replacement).setActive(true);
		plugin.shutDown();
		verify(replacement).setActive(false);
		assertArrayEquals(new int[] {12343}, originalColours);
	}

	@Test
	public void removesNavigationAndRestoresMaterialsOnShutdown()
	{
		doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
			.when(clientThread).invoke(any(Runnable.class));
		plugin.onGameTick(new GameTick());
		assertNotEquals(12341, colours[0]);
		plugin.shutDown();
		assertEquals(12341, colours[0]);
		verify(clientToolbar).addNavigation(any());
		verify(clientToolbar).removeNavigation(any());
	}

	@Test
	public void disguisesRetainOverheadsAndCleanUpOnDisableAndShutdown()
	{
		when(config.randomBrothers()).thenReturn(true);
		IndexDataBase configs = mock(IndexDataBase.class);
		when(client.getIndexConfig()).thenReturn(configs);
		when(configs.getFileIds(9)).thenReturn(new int[] {99999});
		NPC brother = mock(NPC.class);
		when(brother.getId()).thenReturn(net.runelite.api.gameval.NpcID.BARROWS_VERAC);
		when(brother.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		WorldView view = client.getTopLevelWorldView();
		when(brother.getWorldView()).thenReturn(view);
		when(client.getNpcs()).thenReturn(java.util.Collections.singletonList(brother));
		NPCComposition sourceDefinition = mock(NPCComposition.class);
		when(client.getNpcDefinition(anyInt())).thenReturn(sourceDefinition);
		when(sourceDefinition.getModels()).thenReturn(new int[] {123});
		when(sourceDefinition.getWidthScale()).thenReturn(128);
		when(sourceDefinition.getHeightScale()).thenReturn(128);
		ModelData source = mock(ModelData.class), copy = mock(ModelData.class);
		when(client.loadModelData(123)).thenReturn(source);
		when(source.shallowCopy()).thenReturn(copy);
		when(copy.cloneVertices()).thenReturn(copy);
		when(copy.cloneColors()).thenReturn(copy);
		when(copy.light()).thenReturn(mock(Model.class));
		RuneLiteObject replacement = mock(RuneLiteObject.class);
		when(replacement.isActive()).thenReturn(true);
		when(client.createRuneLiteObject()).thenReturn(replacement);
		org.mockito.ArgumentCaptor<RenderCallback> captor =
			org.mockito.ArgumentCaptor.forClass(RenderCallback.class);
		verify(renderCallbacks).register(captor.capture());
		RenderCallback listener = captor.getValue();
		GameObject npcObject = mock(GameObject.class);
		when(npcObject.getRenderable()).thenReturn(brother);
		Scene scene = view.getScene();
		// Exercise RuneLite's actual dispatch: admission retains the NPC clickbox,
		// whereas drawObject filters only its later GPU geometry upload.
		RenderCallbackManager dispatcher = new RenderCallbackManager();
		dispatcher.register(listener);
		assertTrue(dispatcher.addEntity(brother, false));
		assertTrue(dispatcher.drawObject(scene, npcObject));
		plugin.onGameTick(new GameTick());
		assertTrue("Disguised brothers must remain in the scene for mouse picking", dispatcher.addEntity(brother, false));
		assertTrue(dispatcher.addEntity(brother, true));
		assertFalse(dispatcher.drawObject(scene, npcObject));
		GameObject unrelated = mock(GameObject.class);
		NPC otherNpc = mock(NPC.class);
		when(unrelated.getRenderable()).thenReturn(otherNpc);
		assertTrue(dispatcher.addEntity(otherNpc, false));
		assertTrue(dispatcher.drawObject(scene, unrelated));
		assertTrue(dispatcher.drawObject(scene, mock(WallObject.class)));
		when(config.randomBrothers()).thenReturn(false);
		plugin.onGameTick(new GameTick());
		assertTrue(dispatcher.drawObject(scene, npcObject));
		verify(replacement).setActive(false);
		when(config.randomBrothers()).thenReturn(true);
		plugin.onGameTick(new GameTick());
		doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; })
			.when(clientThread).invoke(any(Runnable.class));
		plugin.shutDown();
		assertTrue(dispatcher.addEntity(brother, false));
		assertTrue(dispatcher.drawObject(scene, npcObject));
		verify(renderCallbacks).unregister(listener);
		verify(replacement, times(2)).setActive(false);
	}

	@Test
	public void wallModelOptionStillOnlyRecoloursParkedSmallSteps()
	{
		when(config.theme()).thenReturn(BarrowsTheme.INFERNO);
		when(config.curatedMaterials()).thenReturn(true);
		when(config.replaceWallModels()).thenReturn(true);
		plugin.onGameTick(new GameTick());
		assertNotEquals(12341, colours[0]);
		verify(client, never()).createRuneLiteObject();
		verify(client, never()).loadModelData(anyInt());
	}

	@Test
	public void themesStaticCornerGameObject()
	{
		plugin.onGameTick(new GameTick());
		assertNotEquals("Architectural corner in a GameObject slot must be themed", 12341, colours[0]);
	}
}
