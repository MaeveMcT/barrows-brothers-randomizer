package com.barrowsbrothersrandomizer;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Renderable;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
	name = "Barrows Brothers Randomizer",
	description = "Give each Barrows brother a random NPC disguise while preserving combat and interactions",
	tags = {"barrows", "brothers", "random", "npc", "disguise"})
public class BarrowsBrothersRandomizerPlugin extends Plugin
{
	static final int CRYPT_REGION = 14231;
	private static final String LEGACY_GROUP = "barrowsthemes";
	private static final String INTERIM_GROUP = "barrowsbrothersrandomnpc";
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private RenderCallbackManager renderCallbacks;
	@Inject private BarrowsBrothersRandomizerConfig config;
	@Inject private ConfigManager configManager;
	private volatile boolean active;
	private final BrotherDisguises disguises = new BrotherDisguises();
	private final RenderCallback disguiseListener = new RenderCallback()
	{
		// Scene admission and mouse picking still use the original NPC. Suppress only
		// its later GPU geometry upload after a visual disguise is successfully active.
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			if (!active || !(object instanceof GameObject)) { return true; }
			Renderable renderable = ((GameObject) object).getRenderable();
			return !(renderable instanceof NPC) || !disguises.hides((NPC) renderable);
		}
	};

	@Provides
	BarrowsBrothersRandomizerConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(BarrowsBrothersRandomizerConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateConfig(configManager);
		active = true;
		renderCallbacks.register(disguiseListener);
		clientThread.invokeLater(() -> { if (active) { refreshDisguises(); } });
	}

	/** Carry over NPC settings only; never import the removed theme/texture/recording options. */
	static void migrateConfig(ConfigManager manager)
	{
		if (manager == null) { return; }
		String group = BarrowsBrothersRandomizerConfig.GROUP;
		manager.unsetConfiguration(group, "nativeNpcAttacks");
		manager.unsetConfiguration(group, "npcActionOverrides");
		if (Boolean.parseBoolean(manager.getConfiguration(group, "migratedLegacyNpcSettings"))) { return; }
		String[] keys = {"npcAnimationMode"};
		String sourceGroup = LEGACY_GROUP;
		if (Boolean.parseBoolean(manager.getConfiguration(INTERIM_GROUP, "migratedLegacyNpcSettings"))) { sourceGroup = INTERIM_GROUP; }
		for (String key : keys)
		{
			if (manager.getConfiguration(INTERIM_GROUP, key) != null) { sourceGroup = INTERIM_GROUP; break; }
		}
		for (String key : keys)
		{
			if (manager.getConfiguration(group, key) != null) { continue; }
			String saved = manager.getConfiguration(sourceGroup, key);
			if (saved != null) { manager.setConfiguration(group, key, saved); }
		}
		if (LEGACY_GROUP.equals(sourceGroup) && manager.getConfiguration(group, "npcAnimationMode") == null
			&& Boolean.parseBoolean(manager.getConfiguration(LEGACY_GROUP, "brotherAttackAnimations")))
		{
			manager.setConfiguration(group, "npcAnimationMode", NpcAnimationMode.FORCE);
		}
		manager.unsetConfiguration(LEGACY_GROUP, "brotherAttackAnimations");
		manager.unsetConfiguration(LEGACY_GROUP, "recordNpcActions");
		manager.setConfiguration(group, "migratedLegacyNpcSettings", true);
	}

	@Override
	protected void shutDown()
	{
		active = false;
		renderCallbacks.unregister(disguiseListener);
		clientThread.invoke(disguises::clear);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (BarrowsBrothersRandomizerConfig.GROUP.equals(event.getGroup()))
		{
			// Preserve per-spawn chance rolls and selections; update animation options in place.
			clientThread.invokeLater(() -> { if (active) { refreshDisguises(); } });
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN) { disguises.clear(); }
	}

	@Subscribe
	public void onGameTick(GameTick event) { refreshDisguises(); }

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (active && client.getGameState() == GameState.LOGGED_IN) { disguises.move(); }
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (active && client.getGameState() == GameState.LOGGED_IN && event.getActor() instanceof NPC)
		{
			disguises.actionChanged((NPC) event.getActor());
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (active && client.getGameState() == GameState.LOGGED_IN && eligibleBrother(event.getNpc()))
		{
			disguises.spawn(client, event.getNpc(), config.chanceToRandomize());
		}
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
		if (!active || client.getGameState() != GameState.LOGGED_IN || client.getTopLevelWorldView() == null)
		{
			disguises.clear();
			return;
		}
		List<NPC> eligible = new ArrayList<>();
		for (NPC npc : client.getNpcs()) { if (eligibleBrother(npc)) { eligible.add(npc); } }
		disguises.refresh(client, eligible, config.npcAnimationMode(), true, "", config.chanceToRandomize());
	}
}
