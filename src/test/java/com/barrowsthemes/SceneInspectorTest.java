package com.barrowsthemes;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SceneInspectorTest
{
	@Test
	public void reportsFloorDetailIdMaterialsAndSharedModelWithoutMutation()
	{
		Client client = mock(Client.class);
		WorldView view = mock(WorldView.class);
		Player player = mock(Player.class);
		Scene scene = mock(Scene.class);
		Tile tile = mock(Tile.class);
		when(client.getTopLevelWorldView()).thenReturn(view);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(view.getScene()).thenReturn(scene);
		when(scene.getBaseX()).thenReturn(3550);
		when(scene.getBaseY()).thenReturn(9690);
		when(scene.getTiles()).thenReturn(new Tile[][][] {{{tile}}});
		GameObject corner = mock(GameObject.class);
		when(corner.getId()).thenReturn(ObjectID.BARROWS_CRYPT_PURPLE);
		when(corner.getConfig()).thenReturn(9 | 3 << 6);
		when(corner.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(corner.getOrientation()).thenReturn(1536);
		when(corner.getModelOrientation()).thenReturn(0);
		when(tile.getGameObjects()).thenReturn(new GameObject[] {corner});
		when(tile.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		GroundObject ground = mock(GroundObject.class);
		when(ground.getId()).thenReturn(ObjectID.BARROWS_DUGUPSOIL_2_PURPLE);
		when(ground.getLocalLocation()).thenReturn(new LocalPoint(64, 64));
		when(ground.getConfig()).thenReturn(22 | 3 << 6);
		when(tile.getGroundObject()).thenReturn(ground);
		ObjectComposition definition = mock(ObjectComposition.class);
		when(client.getObjectDefinition(ObjectID.BARROWS_DUGUPSOIL_2_PURPLE)).thenReturn(definition);
		when(client.getObjectDefinition(ObjectID.BARROWS_CRYPT_PURPLE)).thenReturn(definition);
		when(definition.getName()).thenReturn("null");
		Model model = mock(Model.class);
		int[] colours = {64018, -1, ThemeColors.INVISIBLE};
		when(ground.getRenderable()).thenReturn(model);
		when(model.getFaceCount()).thenReturn(3);
		when(model.getFaceColors1()).thenReturn(colours);
		String report = SceneInspector.capture(client, candidate -> candidate == model);
		assertTrue(report.contains("Ground #20762"));
		assertTrue(report.contains("Selection: included"));
		assertTrue(report.contains("Template placement: 3550,9690,0"));
		assertTrue(report.contains("Placement type: 22; config rotation: 1536"));
		assertTrue(report.contains("Placement type: 9; config rotation: 1536; object orientation: 1536; render orientation: 0"));
		assertTrue(report.contains("shared with excluded scenery: skipped"));
		assertTrue(report.contains("Textured faces: {}"));
		assertTrue(report.contains("64000=1"));
		assertArrayEquals(new int[] {64018, -1, ThemeColors.INVISIBLE}, colours);
	}

	@Test
	public void materialSummaryIgnoresRenderSentinels()
	{
		assertEquals(Integer.valueOf(2), SceneInspector.materials(new int[] {64018, 64010, -1, -2, ThemeColors.INVISIBLE}).get(64000));
		assertEquals(1, SceneInspector.materials(new int[] {64018, -1, -2, ThemeColors.INVISIBLE}).size());
		assertEquals(Integer.valueOf(2), SceneInspector.textures(new short[] {-1, 3, 3}).get(3));
		assertTrue(SceneInspector.textures(null).isEmpty());
	}

	@Test
	public void unavailablePlayerReturnsHelpfulMessage()
	{
		assertEquals("Log in to inspect scenery.", SceneInspector.capture(mock(Client.class), model -> false));
	}
}
