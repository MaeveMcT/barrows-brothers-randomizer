package com.barrowsbrothersrandomizer;

import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Scene;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.events.ConfigChanged;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BarrowsBrothersRandomizerPluginTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private RenderCallbackManager renderCallbacks;
	@Mock private BarrowsBrothersRandomizerConfig config;
	@InjectMocks private BarrowsBrothersRandomizerPlugin plugin;
	private NPC brother;
	private Scene scene;
	private RuneFixture fixture;
	private RenderCallback listener;

	private static class RuneFixture
	{
		final net.runelite.api.RuneLiteObject object = mock(net.runelite.api.RuneLiteObject.class);
		final IndexDataBase index = mock(IndexDataBase.class);
	}

	@Before
	public void setup()
	{
		scene = mock(Scene.class);
		WorldView view = mock(WorldView.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(view.getScene()).thenReturn(scene); when(view.getPlane()).thenReturn(3);
		when(scene.getBaseX()).thenReturn(3550); when(scene.getBaseY()).thenReturn(9690);
		brother = mock(NPC.class);
		when(brother.getId()).thenReturn(NpcID.BARROWS_VERAC);
		when(brother.getLocalLocation()).thenReturn(new LocalPoint(64,64));
		when(brother.getWorldView()).thenReturn(view);
		when(client.getNpcs()).thenReturn(Collections.singletonList(brother));
		when(config.chanceToRandomize()).thenReturn(100);
		when(config.npcAnimationMode()).thenReturn(NpcAnimationMode.NATIVE);
		when(config.nativeNpcAttacks()).thenReturn(true); when(config.npcActionOverrides()).thenReturn("");
		fixture = new RuneFixture();
		when(client.getIndexConfig()).thenReturn(fixture.index);
		when(fixture.index.getFileIds(9)).thenReturn(new int[] {99999});
		NPCComposition definition = mock(NPCComposition.class);
		when(client.getNpcDefinition(99999)).thenReturn(definition);
		when(definition.getModels()).thenReturn(new int[] {123});
		when(definition.getWidthScale()).thenReturn(128); when(definition.getHeightScale()).thenReturn(128);
		ModelData source = mock(ModelData.class), copy = mock(ModelData.class);
		when(client.loadModelData(123)).thenReturn(source);
		when(source.shallowCopy()).thenReturn(copy);
		when(copy.cloneVertices()).thenReturn(copy); when(copy.cloneColors()).thenReturn(copy);
		when(copy.light()).thenReturn(mock(Model.class));
		when(client.createRuneLiteObject()).thenReturn(fixture.object);
		when(fixture.object.isActive()).thenReturn(true);
		doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(clientThread).invokeLater(any(Runnable.class));
		doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(clientThread).invoke(any(Runnable.class));
	}

	private void start()
	{
		plugin.startUp();
		ArgumentCaptor<RenderCallback> captor = ArgumentCaptor.forClass(RenderCallback.class);
		verify(renderCallbacks).register(captor.capture()); listener = captor.getValue();
	}

	@Test
	public void enablingDisguisesExistingBrothersWithoutTouchingScenery()
	{
		start();
		verify(fixture.object).setActive(true);
		verify(fixture.object).setLocation(brother.getLocalLocation(), 3);
		verify(scene, never()).getTiles();
		verify(client, never()).getObjectDefinition(anyInt());
		verify(client, never()).getTextureProvider();
		verify(brother, never()).getModel();
		assertTrue(listener.drawObject(scene, mock(WallObject.class)));
	}

	@Test
	public void admissionAndClickboxesRemainWhileOnlyDisguisedGeometryIsHidden()
	{
		start();
		RenderCallbackManager dispatcher = new RenderCallbackManager(); dispatcher.register(listener);
		GameObject actor = mock(GameObject.class); when(actor.getRenderable()).thenReturn(brother);
		assertTrue(dispatcher.addEntity(brother, false)); assertTrue(dispatcher.addEntity(brother, true));
		assertFalse(dispatcher.drawObject(scene, actor));
		NPC unrelated = mock(NPC.class); GameObject other = mock(GameObject.class);
		when(other.getRenderable()).thenReturn(unrelated);
		assertTrue(dispatcher.addEntity(unrelated, false)); assertTrue(dispatcher.drawObject(scene, other));
		plugin.shutDown();
		assertTrue(dispatcher.drawObject(scene, actor));
		verify(fixture.object).setActive(false); verify(renderCallbacks).unregister(listener);
	}

	@Test
	public void optionsUpdateInPlaceWithoutRerollingOrRebuilding()
	{
		start();
		when(config.npcAnimationMode()).thenReturn(NpcAnimationMode.AUTO);
		ConfigChanged event = new ConfigChanged(); event.setGroup(BarrowsBrothersRandomizerConfig.GROUP);
		plugin.onConfigChanged(event); plugin.onGameTick(new GameTick());
		verify(client, times(1)).createRuneLiteObject();
		verify(fixture.index, times(1)).getFileIds(9);
		verify(fixture.object, never()).setActive(false);
	}

	@Test
	public void leavingOrDespawningRestoresOriginalVisibility()
	{
		start();
		plugin.onNpcDespawned(new NpcDespawned(brother)); verify(fixture.object).setActive(false);
		GameObject actor = mock(GameObject.class); when(actor.getRenderable()).thenReturn(brother);
		assertTrue(listener.drawObject(scene, actor));
		plugin.onGameTick(new GameTick());
		GameStateChanged state = mock(GameStateChanged.class); when(state.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
		plugin.onGameStateChanged(state); verify(fixture.object, times(2)).setActive(false);
		assertTrue(listener.drawObject(scene, actor));
	}

	@Test
	public void brothersOutsideBarrowsAndUnrelatedNpcsAreNotDisguised()
	{
		when(scene.getBaseX()).thenReturn(3200); when(scene.getBaseY()).thenReturn(3200);
		start(); verify(client, never()).createRuneLiteObject();
		when(scene.getBaseX()).thenReturn(3550); when(scene.getBaseY()).thenReturn(9690);
		when(brother.getId()).thenReturn(42);
		plugin.onGameTick(new GameTick()); verify(client, never()).createRuneLiteObject();
	}

	@Test
	public void zeroChanceRetainsOriginalAndConfigChangesDoNotReroll()
	{
		when(config.chanceToRandomize()).thenReturn(0);
		start();
		when(config.chanceToRandomize()).thenReturn(100);
		ConfigChanged event = new ConfigChanged(); event.setGroup(BarrowsBrothersRandomizerConfig.GROUP);
		plugin.onConfigChanged(event); plugin.onGameTick(new GameTick());
		verify(client, never()).createRuneLiteObject();
		plugin.onNpcDespawned(new NpcDespawned(brother));
		plugin.onNpcSpawned(new net.runelite.api.events.NpcSpawned(brother));
		plugin.onGameTick(new GameTick());
		verify(client).createRuneLiteObject();
	}

	@Test
	public void unavailableDisguiseLeavesOriginalVisible()
	{
		when(client.loadModelData(123)).thenReturn(null);
		start();
		GameObject actor = mock(GameObject.class); when(actor.getRenderable()).thenReturn(brother);
		assertTrue(listener.drawObject(scene, actor)); verify(client, never()).createRuneLiteObject();
		verify(fixture.index, times(1)).getFileIds(9);
	}
}
