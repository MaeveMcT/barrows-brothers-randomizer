package com.barrowsthemes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-thread-only owner of reversible visual replacements. Never removes scene/collision objects. */
final class WallModelReplacements
{
	private static final Logger log = LoggerFactory.getLogger(WallModelReplacements.class);
	private final Map<int[], int[]> hidden = new IdentityHashMap<>();
	private final List<RuneLiteObject> objects = new ArrayList<>();
	private final Map<Integer, ModelData> sources = new HashMap<>();
	private final Map<Model, Map<Long, Model>> fitted = new IdentityHashMap<>();
	private final Set<Integer> pending = new HashSet<>();
	private final Map<Integer, Boolean> sceneryActions = new HashMap<>();
	private final Set<Model> previousBlocked = Collections.newSetFromMap(new IdentityHashMap<>());
	private List<Placement> previous = Collections.emptyList();

	boolean refresh(Client client, Scene scene, BarrowsTheme theme)
	{
		return refresh(client, scene, theme, true, false, false);
	}

	boolean refresh(Client client, Scene scene, BarrowsTheme theme, boolean walls, boolean varyWalls, boolean scenery)
	{
		Set<Model> blocked = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Placement> placements = collect(scene, theme, blocked, walls, varyWalls, scenery);
		for (Iterator<Placement> iterator = placements.iterator(); iterator.hasNext();)
		{
			Placement placement = iterator.next();
			if (!WallModelSources.isCryptWall(placement.owner.getId()) && hasSceneryActions(client, placement.owner.getId()))
			{
				blocked.add(placement.original);
				iterator.remove();
			}
		}
		Set<int[]> blockedArrays = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Model model : blocked) { if (model.getFaceColors3() != null) { blockedArrays.add(model.getFaceColors3()); } }
		Set<Integer> requiredSources = new HashSet<>();
		for (Placement placement : placements)
		{
			int[] colours = placement.original.getFaceColors3();
			if (colours != null && colours.length > 0 && !blockedArrays.contains(colours)) { requiredSources.add(placement.sourceId); }
		}
		pending.retainAll(requiredSources);
		boolean newlyAvailable = false;
		for (Iterator<Integer> iterator = pending.iterator(); iterator.hasNext();)
		{
			int id = iterator.next();
			ModelData data = client.loadModelData(id);
			if (data != null) { sources.put(id, data); iterator.remove(); newlyAvailable = true; }
		}
		if (samePlacements(placements, previous) && !newlyAvailable && sameBlocked(blocked)) { return false; }
		boolean changed = removeVisuals();
		previous = placements;
		previousBlocked.clear();
		previousBlocked.addAll(blocked);
		Map<int[], List<Placement>> groups = new IdentityHashMap<>();
		for (Placement placement : placements)
		{
			int[] colours = placement.original.getFaceColors3();
			if (colours != null && colours.length > 0) { groups.computeIfAbsent(colours, key -> new ArrayList<>()).add(placement); }
		}
		for (Map.Entry<int[], List<Placement>> group : groups.entrySet())
		{
			int[] originalColours = group.getKey();
			if (blockedArrays.contains(originalColours)) { continue; }
			List<RuneLiteObject> staged = new ArrayList<>();
			boolean ready = true;
			try
			{
				for (Placement placement : group.getValue())
				{
					Model replacement = fitted(client, placement, theme);
					int radius = replacement == null ? -1 : Math.max(WallModelFitter.radius(placement.original), WallModelFitter.radius(replacement));
					if (replacement == null || radius < 1 || radius > 256) { ready = false; break; }
					RuneLiteObject object = client.createRuneLiteObject();
					staged.add(object);
					object.setModel(replacement);
					object.setLocation(placement.location, placement.plane);
					object.setZ(placement.height);
					object.setOrientation(placement.renderOrientation);
					object.setRadius(radius);
				}
				// An original model may be shared by many walls. Hide it only when EVERY placement is ready.
				if (ready)
				{
					for (RuneLiteObject object : staged)
					{
						object.setActive(true);
						if (!object.isActive()) { throw new IllegalStateException("Visual object registration was rejected"); }
					}
					hidden.put(originalColours, originalColours.clone());
					Arrays.fill(originalColours, -2);
					objects.addAll(staged);
					changed = true;
				}
			}
			catch (RuntimeException exception)
			{
				ready = false;
				log.warn("Unable to prepare Barrows wall replacement; retaining the original", exception);
			}
			if (!ready) { for (RuneLiteObject object : staged) { deactivate(object); } }
		}
		return changed;
	}

	private boolean hasSceneryActions(Client client, int id)
	{
		Boolean cached = sceneryActions.get(id);
		if (cached != null) { return cached; }
		ObjectComposition definition = client.getObjectDefinition(id);
		if (definition == null) { return true; } // Unavailable metadata is not permission to replace.
		boolean interactive = false;
		String[] actions = definition.getActions();
		if (actions != null) { for (String action : actions) { interactive |= action != null; } }
		sceneryActions.put(id, interactive);
		return interactive;
	}

	private boolean sameBlocked(Set<Model> blocked)
	{
		return previousBlocked.size() == blocked.size() && previousBlocked.containsAll(blocked);
	}

	private static boolean surfaceFit(Placement placement, BarrowsTheme theme)
	{
		return theme == BarrowsTheme.CHAMBERS_OF_XERIC && WallModelSources.isCryptWall(placement.owner.getId());
	}

	private static long variant(Placement placement, BarrowsTheme theme)
	{
		return (long) placement.sourceId << 17 | (long) (placement.modelRotation & 2047) << 6
			| (placementConfig(placement.owner) & 31) << 1 | (surfaceFit(placement, theme) ? 1 : 0);
	}

	private Model fitted(Client client, Placement placement, BarrowsTheme theme)
	{
		long variant = variant(placement, theme);
		Map<Long, Model> variants = fitted.computeIfAbsent(placement.original, key -> new HashMap<>());
		if (variants.containsKey(variant)) { return variants.get(variant); }
		ModelData source = sources.get(placement.sourceId);
		if (source == null)
		{
			source = client.loadModelData(placement.sourceId);
			if (source == null) { pending.add(placement.sourceId); return null; }
			sources.put(placement.sourceId, source);
		}
		ModelData copy = source.shallowCopy().cloneVertices().cloneColors();
		// Invalidate cached normals using the supported transform API before fitting detached vertices.
		copy.translate(0, 0, 0);
		placement.source.recolor(copy);
		boolean fit = surfaceFit(placement, theme)
			? WallSurfaceFitter.fit(copy, placement.original, placement.modelRotation, placementConfig(placement.owner) & 31)
			: WallModelFitter.fit(copy, placement.original, placement.modelRotation);
		if (!fit) { return null; }
		int ambient = placement.source.ambient();
		Model model = copy.light(ambient, placement.source.contrast(), ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);
		if (model != null) { variants.put(variant, model); }
		return model;
	}

	private static List<Placement> collect(Scene scene, BarrowsTheme theme, Set<Model> blocked, boolean walls, boolean varyWalls, boolean scenery)
	{
		List<Placement> placements = new ArrayList<>();
		Set<TileObject> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null) { continue; }
					boolean crypt = WorldPoint.fromLocalInstance(scene, tile.getLocalLocation(), tile.getPlane()).getRegionID() == BarrowsThemesPlugin.CRYPT_REGION;
					WallObject wall = tile.getWallObject();
					if (wall != null && seen.add(wall))
					{
						int type = wall.getConfig() & 31;
						visit(placements, blocked, theme, wall, wall.getRenderable1(), 0, crypt, type, WallModelSources.wallRotation(wall.getOrientationA()), 0, scene, walls, varyWalls, scenery);
						visit(placements, blocked, theme, wall, wall.getRenderable2(), 1, crypt, type, WallModelSources.wallRotation(wall.getOrientationB()), 0, scene, walls, varyWalls, scenery);
					}
					for (GameObject object : tile.getGameObjects())
					{
						if (object != null && seen.add(object))
						{
							// getOrientation() includes logical placement rotation, already baked into static models.
							// Preserve only the extra rotation the renderer actually applies to the model.
							visit(placements, blocked, theme, object, object.getRenderable(), 0, crypt, object.getConfig() & 31,
								(object.getConfig() >>> 6 & 3) * 512, object.getModelOrientation(), scene, walls, varyWalls, scenery);
						}
					}
					DecorativeObject decor = tile.getDecorativeObject();
					if (decor != null && seen.add(decor))
					{
						visit(placements, blocked, theme, decor, decor.getRenderable(), 0, crypt, decor.getConfig() & 31, (decor.getConfig() >>> 6 & 3) * 512, 0, scene, walls, varyWalls, scenery);
						visit(placements, blocked, theme, decor, decor.getRenderable2(), 1, crypt, decor.getConfig() & 31, (decor.getConfig() >>> 6 & 3) * 512, 0, scene, walls, varyWalls, scenery);
					}
					GroundObject ground = tile.getGroundObject();
					if (ground != null && seen.add(ground))
					{
						visit(placements, blocked, theme, ground, ground.getRenderable(), 0, crypt, ground.getConfig() & 31, (ground.getConfig() >>> 6 & 3) * 512, 0, scene, walls, varyWalls, scenery);
					}
				}
			}
		}
		return placements;
	}

	private static void visit(List<Placement> placements, Set<Model> blocked, BarrowsTheme theme, TileObject owner,
		Renderable renderable, int slot, boolean crypt, int type, int rotation, int renderOrientation,
		Scene scene, boolean walls, boolean varyWalls, boolean scenery)
	{
		if (!(renderable instanceof Model)) { return; }
		Model original = (Model) renderable;
		LocalPoint location = owner.getLocalLocation();
		boolean selected = (walls && WallModelSources.isCryptWall(owner.getId()))
			|| (scenery && WallModelSources.isScenery(owner.getId()));
		WallModelSources.Source source = crypt && selected && location != null
			? WallModelSources.select(theme, owner.getId(), type,
				WorldPoint.fromLocalInstance(scene, location, owner.getPlane()), varyWalls) : null;
		if (source == null || rotation < 0)
		{
			blocked.add(original);
			return;
		}
		placements.add(new Placement(owner, slot, original, source, (rotation + WallModelSources.extraRotation(theme, owner.getId(), type)) & 2047,
			renderOrientation, location, owner.getPlane(), owner.getZ()));
	}

	private static boolean samePlacements(List<Placement> first, List<Placement> second)
	{
		if (first.size() != second.size()) { return false; }
		for (int i = 0; i < first.size(); i++) { if (!first.get(i).same(second.get(i))) { return false; } }
		return true;
	}

	private boolean removeVisuals()
	{
		boolean changed = !hidden.isEmpty();
		for (RuneLiteObject object : objects) { deactivate(object); }
		objects.clear();
		for (Map.Entry<int[], int[]> entry : hidden.entrySet())
		{
			System.arraycopy(entry.getValue(), 0, entry.getKey(), 0, entry.getValue().length);
		}
		hidden.clear();
		return changed;
	}

	private static void deactivate(RuneLiteObject object)
	{
		try { object.setActive(false); }
		catch (RuntimeException exception)
		{
			// A disappearing world view must not prevent restoring original model arrays.
			log.warn("Unable to remove a Barrows visual object during cleanup", exception);
		}
	}

	boolean clear()
	{
		boolean changed = removeVisuals();
		previous = Collections.emptyList(); previousBlocked.clear(); pending.clear(); sources.clear(); fitted.clear(); sceneryActions.clear();
		return changed;
	}

	String summary()
	{
		return objects.size() + "/" + previous.size() + " eligible static wall/scenery parts registered; "
			+ pending.size() + " source models awaiting cache. Other parts retain material themes.";
	}

	/** Short, read-only snapshot of nearby corners. No cache loads, resets or registration changes. */
	String cornerReport(Client client, Scene scene, BarrowsTheme theme, boolean enabled)
	{
		if (client.getTopLevelWorldView() == null || client.getLocalPlayer() == null || client.getLocalPlayer().getLocalLocation() == null) { return "Log in to inspect wall corners."; }
		LocalPoint player = client.getLocalPlayer().getLocalLocation();
		int plane = client.getTopLevelWorldView().getPlane();
		WorldPoint position = WorldPoint.fromLocalInstance(scene, player, plane);
		StringBuilder out = new StringBuilder("Wall corner report v3\nTheme: ").append(theme)
			.append("; replacement enabled=").append(enabled).append("\nPlayer template: ")
			.append(position.getX()).append(',').append(position.getY()).append(',').append(position.getPlane())
			.append("; live plane=").append(plane).append("\nNearest static corners/diagonals within 4 tiles (max 12):\n");
		Set<Model> blocked = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Placement> nearby = collect(scene, theme, blocked, true, false, false);
		nearby.removeIf(p -> p.plane != plane || (placementConfig(p.owner) & 31) != 9 && (placementConfig(p.owner) & 31) != 1
			|| Math.abs(p.location.getX() - player.getX()) > 512 || Math.abs(p.location.getY() - player.getY()) > 512);
		nearby.sort(java.util.Comparator.comparingInt(p -> Math.abs(p.location.getX() - player.getX()) + Math.abs(p.location.getY() - player.getY())));
		Set<int[]> protectedArrays = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Model model : blocked) { if (model.getFaceColors3() != null) { protectedArrays.add(model.getFaceColors3()); } }
		for (int i = 0; i < Math.min(12, nearby.size()); i++)
		{
			Placement p = nearby.get(i);
			int config = placementConfig(p.owner), type = config & 31;
			WorldPoint point = WorldPoint.fromLocalInstance(scene, p.location, p.plane);
			int[] faces = p.original.getFaceColors3();
			boolean registered = hidden.containsKey(faces) && previous.stream().anyMatch(p::same);
			String state = registered ? "registered group; original faces hidden"
				: hidden.containsKey(faces) ? "different replacement group active; original faces hidden"
				: !enabled ? "disabled; original retained" : protectedArrays.contains(faces) ? "shared with excluded scenery; original retained"
				: pending.contains(p.sourceId) ? "source pending; original retained" : "original retained/not registered";
			out.append("#").append(p.owner.getId()).append(" @ ").append(point.getX()).append(',').append(point.getY()).append(',').append(point.getPlane())
				.append(" type=").append(type).append(" slot=").append(p.slot).append(" sourceModel=").append(p.sourceId).append('\n')
				.append("  placementRot=").append((config >>> 6 & 3) * 512)
				.append(" extraRot=").append(WallModelSources.extraRotation(theme, p.owner.getId(), type))
				.append(" bakedRot=").append(p.modelRotation).append(" renderRot=").append(p.renderOrientation)
				.append(" height=").append(p.height);
			if (p.owner instanceof GameObject) { out.append(" objectRot=").append(((GameObject) p.owner).getOrientation()); }
			if (p.owner instanceof WallObject)
			{
				WallObject wall = (WallObject) p.owner;
				out.append(" masks=").append(wall.getOrientationA()).append('/').append(wall.getOrientationB());
			}
			Map<Long, Model> variants = fitted.get(p.original);
			Model replacement = variants == null ? null : variants.get(variant(p, theme));
			out.append("\n  ").append(state).append("; fit=").append(surfaceFit(p, theme) ? "front-surface profile" : "bounds").append("\n  original: ").append(WallModelFitter.boundsSummary(p.original))
				.append("\n  source: ").append(WallModelFitter.boundsSummary(sources.get(p.sourceId)))
				.append("; fitted: ").append(WallModelFitter.boundsSummary(replacement)).append('\n');
		}
		if (nearby.isEmpty()) { out.append("No supported static corners nearby. Stand beside the affected corner.\n"); }
		if (nearby.size() > 12) { out.append(nearby.size() - 12).append(" farther parts omitted.\n"); }
		return out.append("Bounds/registration do not prove alignment. Send this report with a screenshot; compare replacement on/off.\n").toString();
	}

	private static int placementConfig(TileObject owner)
	{
		if (owner instanceof WallObject) { return ((WallObject) owner).getConfig(); }
		if (owner instanceof GameObject) { return ((GameObject) owner).getConfig(); }
		if (owner instanceof DecorativeObject) { return ((DecorativeObject) owner).getConfig(); }
		return ((GroundObject) owner).getConfig();
	}

	private static final class Placement
	{
		final TileObject owner;
		final int slot, sourceId, modelRotation, renderOrientation, plane, height;
		final Model original;
		final WallModelSources.Source source;
		final LocalPoint location;

		Placement(TileObject owner, int slot, Model original, WallModelSources.Source source, int modelRotation, int renderOrientation, LocalPoint location, int plane, int height)
		{
			this.owner = owner; this.slot = slot; this.original = original; this.source = source; this.sourceId = source.modelId;
			this.modelRotation = modelRotation; this.renderOrientation = renderOrientation;
			this.location = location; this.plane = plane; this.height = height;
		}

		boolean same(Placement other)
		{
			return owner == other.owner && original == other.original && slot == other.slot && source == other.source
				&& modelRotation == other.modelRotation && renderOrientation == other.renderOrientation && plane == other.plane
				&& height == other.height && location.equals(other.location);
		}
	}
}
