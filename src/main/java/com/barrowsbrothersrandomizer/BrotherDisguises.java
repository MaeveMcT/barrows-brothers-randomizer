package com.barrowsbrothersrandomizer;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.gameval.NpcID;

/** Visual-only, per-spawn disguises. Never mutates NPC definitions or original models. */
final class BrotherDisguises
{
	// NPC definitions are files in config archive 9 (RuneLite cache ConfigType.NPC).
	// There is no corresponding config-archive constant in the public client API.
	private static final int NPC_ARCHIVE = 9;
	private int[] npcIds;
	private final Map<NPC, Disguise> disguises = new IdentityHashMap<>();
	private final IntUnaryOperator random;
	private AnimationRigCache rigs;

	BrotherDisguises() { this(bound -> ThreadLocalRandom.current().nextInt(bound)); }
	BrotherDisguises(IntUnaryOperator random) { this.random = random; }

	static boolean isBrother(int id)
	{
		switch (id)
		{
			case NpcID.BARROWS_AHRIM:
			case NpcID.BARROWS_DHAROK:
			case NpcID.BARROWS_GUTHAN:
			case NpcID.BARROWS_KARIL:
			case NpcID.BARROWS_TORAG:
			case NpcID.BARROWS_VERAC: return true;
			default: return false;
		}
	}

	void spawn(Client client, NPC npc, int chance)
	{
		if (!isBrother(npc.getId())) { return; }
		Disguise disguise = disguises.computeIfAbsent(npc, ignored -> new Disguise());
		if (!disguise.chanceRolled)
		{
			disguise.chanceRolled = true;
			int percent = Math.max(0, Math.min(100, chance));
			disguise.randomize = percent == 100 || (percent > 0 && random.applyAsInt(100) < percent);
		}
		if (!disguise.randomize || disguise.source >= 0) { return; }
		if (npcIds == null)
		{
			IndexDataBase configs = client.getIndexConfig();
			int[] ids = configs == null ? null : configs.getFileIds(NPC_ARCHIVE);
			if (ids == null || ids.length == 0) { return; }
			npcIds = ids.clone();
		}
		// Sample every available ID equally, without size, name, animation or rig filters.
		disguise.source = npcIds[random.applyAsInt(npcIds.length)];
	}

	/** Also discovers brothers already present when the plugin is enabled. */
	void refresh(Client client, List<NPC> eligible, NpcAnimationMode mode, int chance)
	{
		if (rigs == null) { rigs = new AnimationRigCache(client); }
		Set<NPC> present = Collections.newSetFromMap(new IdentityHashMap<>());
		present.addAll(eligible);
		for (NPC npc : eligible) { spawn(client, npc, chance); }
		Iterator<Map.Entry<NPC, Disguise>> iterator = disguises.entrySet().iterator();
		while (iterator.hasNext())
		{
			Map.Entry<NPC, Disguise> entry = iterator.next();
			if (!present.contains(entry.getKey()))
			{
				remove(entry.getValue());
				iterator.remove();
				continue;
			}
			Disguise disguise = entry.getValue();
			if (disguise.object != null)
			{
				disguise.controller.options(mode);
				disguise.controller.prepare();
				continue;
			}
			if (disguise.source < 0) { continue; }
			RuneLiteObject staged = null;
			try
			{
				LoadedModel model = load(client, disguise);
				if (model == null) { continue; } // Retry this same choice, never reroll on cache misses.
				staged = client.createRuneLiteObject();
				staged.setModel(model.model);
				NativeNpcAnimation controller = new NativeNpcAnimation(client, entry.getKey(), model.definitionId, rigs, mode);
				controller.prepare();
				staged.setAnimationController(controller);
				staged.setRadius(60);
				place(staged, entry.getKey());
				staged.setActive(true);
				if (!staged.isActive()) { staged.setActive(false); continue; }
				disguise.object = staged;
				disguise.controller = controller;
			}
			catch (RuntimeException ex)
			{
				if (staged != null) { deactivate(staged); }
			}
		}
	}

	private LoadedModel load(Client client, Disguise disguise)
	{
		NPCComposition definition = client.getNpcDefinition(disguise.source);
		if (definition == null) { return null; }
		int definitionId = disguise.source;
		if (definition.getConfigs() != null)
		{
			definition = definition.transform();
			if (definition == null) { return null; }
			definitionId = definition.getId();
		}
		int[] ids = definition.getModels();
		if (ids == null || ids.length == 0) { return null; }
		ModelData[] parts = new ModelData[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			parts[i] = client.loadModelData(ids[i]);
			if (parts[i] == null) { return null; }
		}
		ModelData data = (parts.length == 1 ? parts[0] : client.mergeModels(parts))
			.shallowCopy().cloneVertices().cloneColors();
		short[] from = definition.getColorToReplace();
		short[] to = definition.getColorToReplaceWith();
		if (from != null && to != null)
		{
			for (int i = 0; i < Math.min(from.length, to.length); i++) { data.recolor(from[i], to[i]); }
		}
		data.scale(definition.getWidthScale(), definition.getHeightScale(), definition.getWidthScale());
		data.translate(0, 0, 0); // Invalidate copied normals after scaling.
		Model model = data.light();
		if (model == null) { return null; }
		return new LoadedModel(model, definitionId);
	}

	void actionChanged(NPC npc)
	{
		Disguise disguise = disguises.get(npc);
		if (disguise != null && disguise.controller != null) { disguise.controller.actionChanged(); }
	}

	void move()
	{
		for (Map.Entry<NPC, Disguise> entry : disguises.entrySet())
		{
			Disguise disguise = entry.getValue();
			if (disguise.object == null) { continue; }
			try { disguise.controller.prepare(); place(disguise.object, entry.getKey()); }
			catch (RuntimeException ex)
			{
				remove(disguise);
			}
		}
	}

	private void place(RuneLiteObject object, NPC npc)
	{
		object.setLocation(npc.getLocalLocation(), npc.getWorldView().getPlane());
		object.setOrientation(npc.getCurrentOrientation());
	}

	boolean hides(NPC npc)
	{
		Disguise disguise = disguises.get(npc);
		return disguise != null && disguise.object != null && disguise.object.isActive();
	}

	void despawn(NPC npc)
	{
		Disguise disguise = disguises.remove(npc);
		if (disguise != null) { remove(disguise); }
	}

	void clear()
	{
		for (Disguise disguise : disguises.values()) { remove(disguise); }
		disguises.clear();
		npcIds = null;
		if (rigs != null) { rigs.clear(); rigs = null; }
	}

	private void remove(Disguise disguise)
	{
		if (disguise.object != null) { deactivate(disguise.object); disguise.object = null; }
	}

	private void deactivate(RuneLiteObject object)
	{
		try { object.setActive(false); }
		catch (RuntimeException ignored) { /* Continue cleanup of the remaining disguises. */ }
	}

	private static final class LoadedModel
	{
		final Model model;
		final int definitionId;
		LoadedModel(Model model, int definitionId) { this.model = model; this.definitionId = definitionId; }
	}

	private static final class Disguise
	{
		private boolean chanceRolled;
		private boolean randomize;
		private int source = -1;
		private NativeNpcAnimation controller;
		private RuneLiteObject object;
	}
}
