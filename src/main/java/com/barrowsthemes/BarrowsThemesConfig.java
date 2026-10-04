package com.barrowsthemes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BarrowsThemesConfig.GROUP)
public interface BarrowsThemesConfig extends Config
{
	String GROUP = "barrowsthemes";

	@ConfigItem(keyName = "theme", name = "Theme", description = "Area-inspired materials and optional cache-backed wall models (not an exact area recreation)", position = -1)
	default BarrowsTheme theme() { return BarrowsTheme.ZANARIS; }

	@ConfigItem(keyName = "curatedMaterials", name = "Theme material palettes", position = 0,
		description = "Detailed material roles: cache-sampled for Zanaris/CoX/Inferno, artistic area-inspired colours for the six newer themes. Keeps geometry; manual textures override these mappings.")
	default boolean curatedMaterials() { return true; }

	@ConfigItem(keyName = "floors", name = "Theme floors", description = "Recolour underground Barrows floors", position = 1)
	default boolean floors() { return true; }

	@ConfigItem(keyName = "walls", name = "Theme walls and crypt scenery", description = "Recolour/retexture static walls, stones, coffins and stairs; interactions are preserved", position = 2)
	default boolean walls() { return true; }

	@ConfigItem(keyName = "replaceWallModels", name = "Replace wall models (experimental)", position = 3,
		description = "Fit visual-only themed cache models to static crypt walls/corners for all nine themes. Floors and furniture keep their geometry. Keeps collision; may affect GPU performance.")
	default boolean replaceWallModels() { return false; }

	@ConfigItem(keyName = "varyWallModels", name = "Vary middle wall models", position = 7,
		description = "Use stable, map-position-based variants for straight replacement walls. Does not change collision or reroll each tick.")
	default boolean varyWallModels() { return true; }

	@ConfigItem(keyName = "randomBrothers", name = "Random brother models (experimental)", position = 6,
		description = "Give each brother a random disguise from every available NPC definition ID on each spawn, including potentially buggy models. Visual only: names and combat stay unchanged; disguise size does not change the interaction footprint. Uses the selected NPC's native idle/movement animations where available; unsupported models stay static. Native attack IDs are not universally available.")
	default boolean randomBrothers() { return false; }

	@ConfigItem(keyName = "npcAnimationMode", name = "NPC animation mode", position = 8,
		description = "Native: native gait/mapped attacks only. Auto: prefer native attacks, otherwise borrow only with classic rig evidence or explicit allow. Force: try brother actions even on unknown rigs. No mode filters NPC IDs.")
	default NpcAnimationMode npcAnimationMode() { return NpcAnimationMode.NATIVE; }

	@ConfigItem(keyName = "nativeNpcAttacks", name = "Mapped native NPC attacks", position = 9,
		description = "Play source-backed animal-family attacks and reviewed overrides on recognized brother attacks, using native timing. Unknown IDs keep native gait. Force mode prefers brother actions.")
	default boolean nativeNpcAttacks() { return true; }

	@ConfigItem(keyName = "npcActionOverrides", name = "NPC action overrides", position = 10,
		description = "Reviewed rules: resolvedNpc:brotherAction=allow, deny, or nativeSequence; separate with semicolons. * matches any action. Example: 1173:2075=5387. Exact action beats wildcard. Native mode never borrows.")
	default String npcActionOverrides() { return ""; }

	@ConfigItem(keyName = "recordNpcActions", name = "Record NPC actions", position = 11,
		description = "Collect up to 256 NPC/action combinations during ordinary play for the inspector. Session-only, nothing uploaded. Actions are not automatically classified as attacks; clears on logout/disable.")
	default boolean recordNpcActions() { return false; }


	@ConfigItem(keyName = "floorTexture", name = "Floor texture ID (experimental)",
		description = "Existing game texture ID; -1 uses the selected colour palette. Overrides the floor palette, not an authentic area texture preset.", position = 4)
	default int floorTexture() { return -1; }

	@ConfigItem(keyName = "wallTexture", name = "Scenery texture ID (experimental)",
		description = "Replaces already-textured wall faces, preserving their UV mapping. Untextured faces retain the palette. -1 uses only recolouring.", position = 5)
	default int wallTexture() { return -1; }
}
