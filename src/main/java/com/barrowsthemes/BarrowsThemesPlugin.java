package com.barrowsthemes;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.GameObject;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.Texture;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(name = "Barrows Themes", description = "Game-area-inspired themes for Barrows floors and scenery", tags = {"barrows", "zanaris", "theme"})
public class BarrowsThemesPlugin extends Plugin
{
	// Same underground region used by RuneLite's built-in Barrows plugin. Includes tombs.
	static final int CRYPT_REGION = 14231;
	@Inject private Client client;
	@Inject private RenderCallbackManager renderCallbacks;
	private volatile boolean active;
	private final BrotherDisguises disguises = new BrotherDisguises();
	private final NpcActionObservations actionObservations = new NpcActionObservations();
	private final RenderCallback disguiseListener = new RenderCallback()
	{
		// Never reject addEntity: that removes the original NPC's clickbox.
		// GPU calls drawObject at upload time, after the client's mouse-picking path.
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			if (!active || !(object instanceof GameObject)) { return true; }
			Renderable renderable = ((GameObject) object).getRenderable();
			return !(renderable instanceof NPC) || !disguises.hides((NPC) renderable);
		}
	};
	@Inject private ClientThread clientThread;
	@Inject private BarrowsThemesConfig config;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar clientToolbar;
	private NavigationButton navigation;
	private volatile TextureBrowserPanel texturePanel;
	private int wallTexture = -1;
	private SceneEdits edits = new SceneEdits();
	private final WallModelReplacements replacements = new WallModelReplacements();
	private final Set<Integer> zones = new HashSet<>();
	private final Map<Integer, Boolean> interactiveObjects = new HashMap<>();
	private final Set<Model> protectedModels = Collections.newSetFromMap(new IdentityHashMap<>());
	private Scene scene;

	@Provides
	BarrowsThemesConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(BarrowsThemesConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateThemeConfig(configManager);
		migrateAnimationConfig(configManager);
		active = true;
		renderCallbacks.register(disguiseListener);
		texturePanel = new TextureBrowserPanel(this::refreshTextures, this::inspectScenery, this::inspectCorners, (key, id) -> {
			if (active) { configManager.setConfiguration(BarrowsThemesConfig.GROUP, key, id); }
		});
		navigation = NavigationButton.builder().tooltip("Barrows textures").icon(TextureBrowserPanel.icon()).priority(6).panel(texturePanel).build();
		clientToolbar.addNavigation(navigation);
		clientThread.invokeLater(() -> { if (active) { apply(); refreshDisguises(); } });
	}

	static void migrateThemeConfig(ConfigManager manager)
	{
		if (manager == null) { return; }
		String saved = manager.getConfiguration(BarrowsThemesConfig.GROUP, "theme");
		if (saved == null) { return; }
		try { BarrowsTheme.valueOf(saved); }
		catch (IllegalArgumentException ex)
		{
			manager.setConfiguration(BarrowsThemesConfig.GROUP, "theme", BarrowsTheme.ZANARIS);
		}
	}

	static void migrateAnimationConfig(ConfigManager manager)
	{
		if (manager == null) { return; }
		String group = BarrowsThemesConfig.GROUP;
		if (manager.getConfiguration(group, "npcAnimationMode") == null
			&& Boolean.parseBoolean(manager.getConfiguration(group, "brotherAttackAnimations")))
		{
			manager.setConfiguration(group, "npcAnimationMode", NpcAnimationMode.FORCE);
		}
		manager.unsetConfiguration(group, "brotherAttackAnimations");
	}

	@Override
	protected void shutDown()
	{
		active = false;
		renderCallbacks.unregister(disguiseListener);
		clientToolbar.removeNavigation(navigation);
		navigation = null;
		texturePanel = null;
		clientThread.invoke(() -> { disguises.clear(); actionObservations.clear(); reset(true); });
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BarrowsThemesConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invokeLater(() -> { if (active) { if (!config.recordNpcActions()) { actionObservations.clear(); } reset(true); apply(); refreshDisguises(); } });
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			// Restore cached model arrays before another scene reuses them; no stale GPU invalidation.
			disguises.clear();
			if (event.getGameState() != GameState.LOADING) { actionObservations.clear(); }
			reset(false);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		apply();
		refreshDisguises();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (active && client.getGameState() == GameState.LOGGED_IN) { disguises.move(); }
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!active || client.getGameState() != GameState.LOGGED_IN || !(event.getActor() instanceof NPC)) { return; }
		NPC npc = (NPC) event.getActor();
		disguises.actionChanged(npc);
		if (config.recordNpcActions()) { actionObservations.record(npc, client.getTickCount()); }
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (active && config.randomBrothers() && eligibleBrother(event.getNpc())) { disguises.spawn(client, event.getNpc()); }
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event) { disguises.despawn(event.getNpc()); }

	private boolean eligibleBrother(NPC npc)
	{
		return BrotherDisguises.isBrother(npc.getId()) && npc.getWorldView() != null
			&& npc.getLocalLocation() != null
			&& WorldPoint.fromLocalInstance(npc.getWorldView().getScene(), npc.getLocalLocation(),
				npc.getWorldView().getPlane()).getRegionID() == CRYPT_REGION;
	}

	private void refreshDisguises()
	{
		if (!active || !config.randomBrothers() || client.getGameState() != GameState.LOGGED_IN
			|| client.getTopLevelWorldView() == null)
		{
			disguises.clear();
			return;
		}
		List<NPC> eligible = new ArrayList<>();
		for (NPC npc : client.getNpcs()) { if (eligibleBrother(npc)) { eligible.add(npc); } }
		disguises.refresh(client, eligible, config.npcAnimationMode(), config.nativeNpcAttacks(), config.npcActionOverrides());
	}

	private void apply()
	{
		if (!active || client.getGameState() != GameState.LOGGED_IN || client.getTopLevelWorldView() == null)
		{
			return;
		}
		Scene current = client.getTopLevelWorldView().getScene();
		boolean cryptLoaded = false;
		for (int region : current.getMapRegions())
		{
			if (region == CRYPT_REGION) { cryptLoaded = true; break; }
		}
		if (!cryptLoaded)
		{
			if (scene != null) { reset(scene == current); }
			return;
		}
		if (scene != current)
		{
			reset(false);
			scene = current;
			edits = new SceneEdits(config.theme(), config.curatedMaterials());
		}
		int texture = validTexture(config.floorTexture());
		wallTexture = validTexture(config.wallTexture());
		boolean changed = false;
		Tile[][][] tiles = scene.getTiles();
		protectSharedModels(tiles);
		int offset = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;
		for (Tile[][] plane : tiles)
		{
			for (int x = 0; x < plane.length; x++)
			{
				for (int y = 0; y < plane[x].length; y++)
				{
					Tile tile = plane[x][y];
					if (tile == null || !inCrypt(tile))
					{
						continue;
					}
					zones.add((((x + offset) >> 3) << 16) | ((y + offset) >> 3));
					if (config.floors())
					{
						changed |= edits.paint(tile.getSceneTilePaint(), texture);
						changed |= edits.floorModel(tile.getSceneTileModel(), texture);
					}
					WallObject wall = tile.getWallObject();
					if (config.walls() && wall != null && (!interactive(wall.getId()) || architecture(wall.getId())))
					{
						changed |= editWall(wall.getRenderable1());
						changed |= editWall(wall.getRenderable2());
					}
					if (config.walls())
					{
						for (GameObject object : tile.getGameObjects())
						{
							if (object != null && architecture(object.getId())) { changed |= editWall(object.getRenderable()); }
						}
						DecorativeObject decor = tile.getDecorativeObject();
						if (decor != null && architecture(decor.getId()))
						{
							changed |= editWall(decor.getRenderable()); changed |= editWall(decor.getRenderable2());
						}
						GroundObject ground = tile.getGroundObject();
						if (ground != null && architecture(ground.getId())) { changed |= editWall(ground.getRenderable()); }
					}
				}
			}
		}
		if (config.replaceWallModels() && config.walls() && config.theme().supportsWallModels())
		{
			// Crypt scenery-model swaps are parked, regardless of any previously saved setting.
			changed |= replacements.refresh(client, scene, config.theme(), true, config.varyWallModels(), false);
		}
		else { changed |= replacements.clear(); }
		// Shared wall models may appear in several zones; refresh all target zones only on change.
		if (changed) { invalidate(); }
	}

	private boolean inCrypt(Tile tile)
	{
		return WorldPoint.fromLocalInstance(scene, tile.getLocalLocation(), tile.getPlane()).getRegionID() == CRYPT_REGION;
	}

	private void protectSharedModels(Tile[][][] tiles)
	{
		protectedModels.clear();
		for (Tile[][] plane : tiles)
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null) { continue; }
					boolean crypt = inCrypt(tile);
					WallObject wall = tile.getWallObject();
					if (wall != null && (!crypt || (interactive(wall.getId()) && !architecture(wall.getId()))))
					{
						protect(wall.getRenderable1()); protect(wall.getRenderable2());
					}
					// Do not recolour a wall model also used by a chest, prop or other scenery.
					for (GameObject object : tile.getGameObjects())
					{
						if (object != null && (!crypt || !architecture(object.getId()))) { protect(object.getRenderable()); }
					}
					DecorativeObject decor = tile.getDecorativeObject();
					if (decor != null && (!crypt || !architecture(decor.getId()))) { protect(decor.getRenderable()); protect(decor.getRenderable2()); }
					GroundObject ground = tile.getGroundObject();
					if (ground != null && (!crypt || !architecture(ground.getId()))) { protect(ground.getRenderable()); }
				}
			}
		}
	}

	private void protect(Renderable renderable)
	{
		if (renderable instanceof Model) { protectedModels.add((Model) renderable); }
	}

	private boolean architecture(int id)
	{
		return BarrowsArchitecture.contains(id) && (!interactive(id) || BarrowsArchitecture.allowsInteractions(id));
	}

	private boolean interactive(int id)
	{
		return interactiveObjects.computeIfAbsent(id, this::hasActions);
	}

	private boolean hasActions(int id)
	{
		String[] actions = client.getObjectDefinition(id).getActions();
		if (actions != null)
		{
			for (String action : actions) { if (action != null) { return true; } }
		}
		return false;
	}

	private boolean editWall(Renderable renderable)
	{
		// Do not mutate animation/cached object-definition models obtained via getModel().
		return renderable instanceof Model && !protectedModels.contains(renderable) && edits.wall((Model) renderable, wallTexture);
	}

	private int validTexture(int id)
	{
		if (id < 0 || id > Short.MAX_VALUE || client.getTextureProvider() == null) { return -1; }
		Texture[] textures = client.getTextureProvider().getTextures();
		return textures != null && id < textures.length && textures[id] != null ? id : -1;
	}

	private void inspectScenery()
	{
		TextureBrowserPanel requestedPanel = texturePanel;
		clientThread.invokeLater(() -> {
			if (!active || requestedPanel != texturePanel) { return; }
			String report;
			String replacementSummary = !config.theme().supportsWallModels() ? "This theme uses material palettes only (original geometry)."
				: config.replaceWallModels() ? replacements.summary() : "Disabled";
			String disguiseSummary = config.randomBrothers() ? disguises.summary() : "Disabled";
			// Inspect originals rather than our edited arrays, and reapply within the same client task.
			reset(true);
			try
			{
				if (client.getGameState() != GameState.LOGGED_IN || client.getTopLevelWorldView() == null)
				{
					report = "Log in to inspect scenery.";
				}
				else
				{
					scene = client.getTopLevelWorldView().getScene();
					protectSharedModels(scene.getTiles());
					report = SceneInspector.capture(client, protectedModels::contains)
						+ "\nWall model replacements (before inspection):\n" + replacementSummary
						+ "\n\nRandom brother disguises:\n" + disguiseSummary
						+ "\n\n" + actionObservations.summary();
				}
			}
			finally { reset(false); apply(); }
			String snapshot = report;
			SwingUtilities.invokeLater(() -> {
				if (active && requestedPanel == texturePanel) { requestedPanel.showInspection(snapshot); }
			});
		});
	}

	private void inspectCorners()
	{
		TextureBrowserPanel requestedPanel = texturePanel;
		clientThread.invokeLater(() -> {
			if (!active || requestedPanel != texturePanel) { return; }
			String report;
			try
			{
				report = client.getGameState() != GameState.LOGGED_IN || client.getTopLevelWorldView() == null
					? "Log in to inspect wall corners."
					: replacements.cornerReport(client, client.getTopLevelWorldView().getScene(), config.theme(), config.walls() && config.replaceWallModels());
			}
			catch (RuntimeException ex) { report = "Corner report unavailable: " + ex.getClass().getSimpleName() + ". Try again after the scene loads."; }
			String snapshot = report;
			SwingUtilities.invokeLater(() -> {
				if (active && requestedPanel == texturePanel) { requestedPanel.showInspection(snapshot); }
			});
		});
	}

	private void refreshTextures()
	{
		TextureBrowserPanel requestedPanel = texturePanel;
		clientThread.invokeLater(() -> {
			if (!active || requestedPanel != texturePanel) { return; }
			List<TexturePreview> previews = client.getGameState() == GameState.LOGGED_IN
				? TexturePreview.capture(client.getTextureProvider()) : Collections.emptyList();
			SwingUtilities.invokeLater(() -> {
				if (active && requestedPanel == texturePanel) { requestedPanel.showTextures(previews); }
			});
		});
	}

	private void invalidate()
	{
		DrawCallbacks callbacks = client.getDrawCallbacks();
		if (scene == null || callbacks == null) { return; }
		for (int zone : zones) { callbacks.invalidateZone(scene, zone >>> 16, zone & 0xffff); }
	}

	private void reset(boolean refresh)
	{
		replacements.clear();
		edits.restore();
		if (refresh) { invalidate(); }
		zones.clear();
		interactiveObjects.clear();
		protectedModels.clear();
		scene = null;
	}
}
