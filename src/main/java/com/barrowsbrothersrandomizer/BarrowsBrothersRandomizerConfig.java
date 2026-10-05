package com.barrowsbrothersrandomizer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BarrowsBrothersRandomizerConfig.GROUP)
public interface BarrowsBrothersRandomizerConfig extends Config
{
	String GROUP = "barrowsbrothersrandomizer";

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(keyName = "chanceToRandomize", name = "Chance to randomize", position = 0,
		description = "Chance per brother spawn to receive a random NPC disguise. 0% keeps originals; 100% always randomizes. Changes affect future spawns only.")
	default int chanceToRandomize() { return 100; }

	@ConfigItem(keyName = "npcAnimationMode", name = "NPC animation mode", position = 1,
		description = "Native: native gait/mapped attacks only. Auto: prefer native attacks, otherwise borrow only with classic rig evidence or explicit allow. Force: try brother actions even on unknown rigs. No mode filters NPC IDs.")
	default NpcAnimationMode npcAnimationMode() { return NpcAnimationMode.NATIVE; }

	@ConfigItem(keyName = "nativeNpcAttacks", name = "Mapped native NPC attacks", position = 2,
		description = "Play source-backed animal-family attacks and reviewed overrides on recognized brother attacks, using native timing. Unknown IDs keep native gait. Force mode prefers brother actions.")
	default boolean nativeNpcAttacks() { return true; }

	@ConfigItem(keyName = "npcActionOverrides", name = "NPC action overrides", position = 3,
		description = "Reviewed rules: resolvedNpc:brotherAction=allow, deny, or nativeSequence; separate with semicolons. * matches any action. Example: 1173:2075=5387. Exact action beats wildcard. Native mode never borrows.")
	default String npcActionOverrides() { return ""; }
}
