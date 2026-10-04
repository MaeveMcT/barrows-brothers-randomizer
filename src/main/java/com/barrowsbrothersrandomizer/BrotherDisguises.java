package com.barrowsbrothersrandomizer;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.gameval.NpcID;

/** Visual-only, per-spawn disguises. Never mutates NPC definitions or original models. */
@Slf4j
final class BrotherDisguises
{
	// NPC definitions are files in config archive 9 (RuneLite cache ConfigType.NPC).
	// There is no corresponding config-archive constant in the public client API.
	private static final int NPC_ARCHIVE = 9;
	private int[] npcIds;
	private final Map<NPC, Disguise> disguises = new IdentityHashMap<>();
	private final IntUnaryOperator random;
	private AnimationRigCache rigs;
	private String overrideText = "";
	private NpcActionOverrides overrides = new NpcActionOverrides("");

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

	void spawn(Client client, NPC npc)
	{
		if (!isBrother(npc.getId())) { return; }
		Disguise disguise = disguises.computeIfAbsent(npc, ignored -> new Disguise());
		if (disguise.source >= 0) { return; }
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
	void refresh(Client client, List<NPC> eligible) { refresh(client, eligible, false); }

	void refresh(Client client, List<NPC> eligible, boolean useBrotherActions)
	{
		refresh(client, eligible, useBrotherActions ? NpcAnimationMode.FORCE : NpcAnimationMode.NATIVE, false, "");
	}

	void refresh(Client client, List<NPC> eligible, NpcAnimationMode mode, boolean nativeAttacks, String rules)
	{
		if (rigs == null) { rigs = new AnimationRigCache(client); }
		String text = rules == null ? "" : rules;
		if (!text.equals(overrideText)) { overrideText = text; overrides = new NpcActionOverrides(text); }
		Set<NPC> present = Collections.newSetFromMap(new IdentityHashMap<>());
		present.addAll(eligible);
		for (NPC npc : eligible) { spawn(client, npc); }
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
				disguise.controller.options(mode, nativeAttacks, overrides);
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
				NativeNpcAnimation controller = new NativeNpcAnimation(client, entry.getKey(), model.definitionId, rigs);
				controller.options(mode, nativeAttacks, overrides);
				controller.prepare();
				staged.setAnimationController(controller);
				staged.setRadius(60);
				place(staged, entry.getKey());
				staged.setActive(true);
				if (!staged.isActive()) { disguise.failure = "Render-object registration rejected"; staged.setActive(false); continue; }
				disguise.object = staged;
				disguise.definitionId = model.definitionId;
				disguise.controller = controller;
				disguise.failure = "";
			}
			catch (RuntimeException ex)
			{
				if (staged != null) { deactivate(staged); }
				disguise.failure = "Model/registration failed: " + ex.getClass().getSimpleName();
				log.debug("Unable to stage brother disguise; retaining original NPC", ex);
			}
		}
	}

	private LoadedModel load(Client client, Disguise disguise)
	{
		NPCComposition definition = client.getNpcDefinition(disguise.source);
		if (definition == null) { disguise.failure = "NPC definition unavailable"; return null; }
		int definitionId = disguise.source;
		if (definition.getConfigs() != null)
		{
			definition = definition.transform();
			if (definition == null) { disguise.failure = "Morph has no active child"; return null; }
			definitionId = definition.getId();
		}
		int[] ids = definition.getModels();
		disguise.definitionId = definitionId;
		if (ids == null || ids.length == 0) { disguise.failure = "Resolved definition has no models"; return null; }
		disguise.models = ids.clone();
		ModelData[] parts = new ModelData[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			parts[i] = client.loadModelData(ids[i]);
			if (parts[i] == null) { disguise.failure = "Waiting for model #" + ids[i]; return null; }
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
		if (model == null) { disguise.failure = "Lighting returned no model"; return null; }
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
				log.debug("Unable to move brother disguise; retaining original NPC", ex);
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

	String summary()
	{
		StringBuilder out = new StringBuilder("Available NPC definitions: ");
		out.append(npcIds == null ? "awaiting archive metadata" : npcIds.length).append('\n');
		out.append(overrides.summary()).append('\n');
		if (disguises.isEmpty()) { return out.append("No eligible brothers currently tracked.\n").toString(); }
		for (Map.Entry<NPC, Disguise> entry : disguises.entrySet())
		{
			Disguise disguise = entry.getValue();
			out.append("Brother #").append(entry.getKey().getId()).append(" (index ").append(entry.getKey().getIndex())
				.append(") -> selected NPC #").append(disguise.source);
			if (disguise.object == null)
			{
				out.append("; resolved NPC #").append(disguise.definitionId).append("; no registered model: ")
					.append(disguise.failure).append("; original retained\n");
				continue;
			}
			out.append("; resolved NPC #").append(disguise.definitionId).append("; registered=").append(disguise.object.isActive()).append('\n');
			out.append("  Models: ").append(java.util.Arrays.toString(disguise.models)).append('\n');
			out.append("  ").append(disguise.controller.summary()).append('\n');
		}
		return out.append("Sequence/frame status does not prove visible movement. Disguises are render objects, not new NPC actors.\n").toString();
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
		catch (RuntimeException ex) { log.warn("Unable to remove brother disguise", ex); }
	}

	private static final class LoadedModel
	{
		final Model model;
		final int definitionId;
		LoadedModel(Model model, int definitionId) { this.model = model; this.definitionId = definitionId; }
	}

	private static final class Disguise
	{
		private int source = -1;
		private int definitionId = -1;
		private int[] models;
		private String failure = "Waiting for archive metadata";
		private NativeNpcAnimation controller;
		private RuneLiteObject object;
	}
}
