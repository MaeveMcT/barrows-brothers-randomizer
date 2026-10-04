package com.barrowsthemes;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;

/** Explicit, nearby, read-only inspection on the client thread; never calls dynamic getModel(). */
final class SceneInspector
{
	private SceneInspector() { }

	static String capture(Client client, Predicate<Model> protectedModel)
	{
		if (client.getLocalPlayer() == null || client.getTopLevelWorldView() == null) { return "Log in to inspect scenery."; }
		Scene scene = client.getTopLevelWorldView().getScene();
		int plane = client.getTopLevelWorldView().getPlane();
		WorldPoint position = WorldPoint.fromLocalInstance(scene, client.getLocalPlayer().getLocalLocation(), plane);
		StringBuilder report = new StringBuilder("Original materials within 8 tiles\n");
		report.append("Region ").append(position.getRegionID()).append(", live plane ").append(plane)
			.append(", template plane ").append(position.getPlane()).append('\n');
		report.append("Textures apply only to already-textured scenery faces.\n\n");
		Tile[][][] tiles = scene.getTiles();
		if (plane < 0 || plane >= tiles.length) { return report.toString(); }
		int px = client.getLocalPlayer().getLocalLocation().getSceneX();
		int py = client.getLocalPlayer().getLocalLocation().getSceneY();
		Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<Integer, Integer> floorTextures = new TreeMap<>();
		for (int x = Math.max(0, px - 8); x <= Math.min(tiles[plane].length - 1, px + 8); x++)
		{
			for (int y = Math.max(0, py - 8); y <= Math.min(tiles[plane][x].length - 1, py + 8); y++)
			{
				Tile tile = tiles[plane][x][y];
				if (tile == null) { continue; }
				int region = WorldPoint.fromLocalInstance(scene, tile.getLocalLocation(), tile.getPlane()).getRegionID();
				if (tile.getSceneTilePaint() != null) { floorTextures.merge(tile.getSceneTilePaint().getTexture(), 1, Integer::sum); }
				if (tile.getSceneTileModel() != null && tile.getSceneTileModel().getTriangleTextureId() != null)
				{
					for (int texture : tile.getSceneTileModel().getTriangleTextureId()) { floorTextures.merge(texture, 1, Integer::sum); }
				}
				WallObject wall = tile.getWallObject();
				if (wall != null && seen.add(wall)) { object(report, client, protectedModel, "Wall", wall, scene, region, wall.getRenderable1(), wall.getRenderable2()); }
				for (GameObject object : tile.getGameObjects())
				{
					if (object != null && seen.add(object)) { object(report, client, protectedModel, "Game", object, scene, region, object.getRenderable()); }
				}
				DecorativeObject decor = tile.getDecorativeObject();
				if (decor != null && seen.add(decor)) { object(report, client, protectedModel, "Decor", decor, scene, region, decor.getRenderable(), decor.getRenderable2()); }
				GroundObject ground = tile.getGroundObject();
				if (ground != null && seen.add(ground)) { object(report, client, protectedModel, "Ground", ground, scene, region, ground.getRenderable()); }
			}
		}
		report.append("Floor texture counts (-1 means colour): ").append(floorTextures).append('\n');
		return report.toString();
	}

	private static void object(StringBuilder out, Client client, Predicate<Model> protectedModel, String slot, TileObject owner, Scene scene, int region, Renderable... renderables)
	{
		int id = owner.getId();
		ObjectComposition definition = client.getObjectDefinition(id);
		boolean interactive = false;
		String[] actions = definition.getActions();
		if (actions != null) { for (String action : actions) { interactive |= action != null; } }
		boolean eligible = region == BarrowsThemesPlugin.CRYPT_REGION
			&& ((slot.equals("Wall") && !interactive)
			|| (BarrowsArchitecture.contains(id) && (!interactive || BarrowsArchitecture.allowsInteractions(id))));
		out.append(slot).append(" #").append(id).append(' ').append(definition.getName()).append('\n');
		if (owner.getLocalLocation() != null)
		{
			WorldPoint point = WorldPoint.fromLocalInstance(scene, owner.getLocalLocation(), owner.getPlane());
			out.append("  Template placement: ").append(point.getX()).append(',').append(point.getY()).append(',').append(point.getPlane()).append('\n');
		}
		int config = owner instanceof WallObject ? ((WallObject) owner).getConfig()
			: owner instanceof GameObject ? ((GameObject) owner).getConfig()
			: owner instanceof DecorativeObject ? ((DecorativeObject) owner).getConfig()
			: ((GroundObject) owner).getConfig();
		out.append("  Placement type: ").append(config & 31)
			.append("; config rotation: ").append((config >>> 6 & 3) * 512);
		if (owner instanceof WallObject)
		{
			WallObject wall = (WallObject) owner;
			out.append("; wall orientation masks: ").append(wall.getOrientationA()).append('/').append(wall.getOrientationB());
		}
		if (owner instanceof GameObject)
		{
			GameObject object = (GameObject) owner;
			out.append("; object orientation: ").append(object.getOrientation()).append("; render orientation: ").append(object.getModelOrientation());
		}
		out.append('\n');
		out.append("  Selection: ").append(eligible ? "included" : "excluded by region/slot/allowlist/actions").append('\n');
		out.append("  Actions: ").append(Arrays.toString(actions)).append('\n');
		if (definition.getImpostorIds() != null) { out.append("  Morph IDs: ").append(Arrays.toString(definition.getImpostorIds())).append('\n'); }
		for (Renderable renderable : renderables)
		{
			if (renderable == null) { continue; }
			if (!(renderable instanceof Model)) { out.append("  Dynamic/no static model: skipped\n"); continue; }
			Model model = (Model) renderable;
			out.append("  Static model: ").append(model.getFaceCount()).append(" faces")
				.append(protectedModel.test(model) ? "; shared with excluded scenery: skipped" : "").append('\n');
			out.append("  Textured faces: ").append(textures(model.getFaceTextures())).append('\n');
			out.append("  Lit material groups: ").append(materials(model.getFaceColors1())).append('\n');
		}
		out.append('\n');
	}

	static Map<Integer, Integer> textures(short[] textures)
	{
		Map<Integer, Integer> result = new TreeMap<>();
		if (textures != null) { for (short texture : textures) { if (texture >= 0) { result.merge((int) texture, 1, Integer::sum); } } }
		return result;
	}

	static Map<Integer, Integer> materials(int[] colours)
	{
		Map<Integer, Integer> result = new TreeMap<>();
		if (colours != null)
		{
			for (int colour : colours)
			{
				if (colour >= 0 && colour != ThemeColors.INVISIBLE) { result.merge(colour & ~127, 1, Integer::sum); }
			}
		}
		return result;
	}
}
