package com.barrowsbrothersrandomizer;

import java.util.HashMap;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NpcAnimationConfigTest
{
	private ConfigManager manager(Map<String, String> values)
	{
		ConfigManager manager = mock(ConfigManager.class);
		when(manager.getConfiguration(anyString(), anyString())).thenAnswer(call -> values.get(call.getArgument(0) + "." + call.getArgument(1)));
		doAnswer(call -> {
			Object value = call.getArgument(2);
			values.put(call.getArgument(0) + "." + call.getArgument(1), value instanceof Enum ? ((Enum<?>) value).name() : value.toString());
			return null;
		}).when(manager).setConfiguration(anyString(), anyString(), any(Object.class));
		doAnswer(call -> { values.put(call.getArgument(0) + "." + call.getArgument(1), call.getArgument(2)); return null; })
			.when(manager).setConfiguration(anyString(), anyString(), anyString());
		doAnswer(call -> { values.remove(call.getArgument(0) + "." + call.getArgument(1)); return null; })
			.when(manager).unsetConfiguration(anyString(), anyString());
		return manager;
	}

	@Test
	public void carriesOverOnlyNpcSettingsAndClearsObsoleteRecording()
	{
		Map<String, String> values = new HashMap<>();
		values.put("barrowsthemes.npcAnimationMode", "AUTO");
		values.put("barrowsthemes.nativeNpcAttacks", "false");
		values.put("barrowsthemes.npcActionOverrides", "1173:2075=5387");
		values.put("barrowsthemes.theme", "INFERNO");
		values.put("barrowsthemes.randomBrothers", "false");
		values.put("barrowsthemes.recordNpcActions", "true");
		BarrowsBrothersRandomizerPlugin.migrateConfig(manager(values));
		String group = BarrowsBrothersRandomizerConfig.GROUP + ".";
		assertEquals("AUTO", values.get(group + "npcAnimationMode"));
		assertFalse(values.containsKey(group + "nativeNpcAttacks"));
		assertFalse(values.containsKey(group + "npcActionOverrides"));
		assertFalse(values.containsKey(group + "theme"));
		assertFalse(values.containsKey(group + "randomBrothers"));
		assertFalse(values.containsKey("barrowsthemes.recordNpcActions"));
	}

	@Test
	public void preservesExistingNewSettingsAndDoesNotRepeatMigrationAfterReset()
	{
		Map<String, String> values = new HashMap<>();
		String mode = BarrowsBrothersRandomizerConfig.GROUP + ".npcAnimationMode";
		values.put(mode, "NATIVE"); values.put("barrowsthemes.npcAnimationMode", "FORCE");
		ConfigManager manager = manager(values);
		BarrowsBrothersRandomizerPlugin.migrateConfig(manager);
		assertEquals("NATIVE", values.get(mode));
		values.remove(mode);
		BarrowsBrothersRandomizerPlugin.migrateConfig(manager);
		assertFalse(values.containsKey(mode));
	}

	@Test
	public void oldEnabledCheckboxMigratesToForceButNeverOverridesAnExplicitMode()
	{
		for (String mode : new String[] {null, "NATIVE"})
		{
			Map<String, String> values = new HashMap<>();
			values.put("barrowsthemes.brotherAttackAnimations", "true");
			if (mode != null) { values.put("barrowsthemes.npcAnimationMode", mode); }
			BarrowsBrothersRandomizerPlugin.migrateConfig(manager(values));
			assertEquals(mode == null ? "FORCE" : mode, values.get(BarrowsBrothersRandomizerConfig.GROUP + ".npcAnimationMode"));
			assertFalse(values.containsKey("barrowsthemes.brotherAttackAnimations"));
		}
	}

	@Test
	public void interimNpcPluginSettingsTakePrecedenceOverOlderThemeSettings()
	{
		Map<String, String> values = new HashMap<>();
		values.put("barrowsthemes.npcAnimationMode", "FORCE");
		values.put("barrowsthemes.nativeNpcAttacks", "false");
		values.put("barrowsbrothersrandomnpc.npcAnimationMode", "AUTO");
		BarrowsBrothersRandomizerPlugin.migrateConfig(manager(values));
		assertEquals("AUTO", values.get(BarrowsBrothersRandomizerConfig.GROUP + ".npcAnimationMode"));
		assertFalse(values.containsKey(BarrowsBrothersRandomizerConfig.GROUP + ".nativeNpcAttacks"));
	}

	@Test
	public void resettingInterimSettingsDoesNotResurrectOldThemeValues()
	{
		Map<String, String> values = new HashMap<>();
		values.put("barrowsbrothersrandomnpc.migratedLegacyNpcSettings", "true");
		values.put("barrowsthemes.npcAnimationMode", "FORCE");
		values.put("barrowsthemes.brotherAttackAnimations", "true");
		BarrowsBrothersRandomizerPlugin.migrateConfig(manager(values));
		assertFalse(values.containsKey(BarrowsBrothersRandomizerConfig.GROUP + ".npcAnimationMode"));
	}

	@Test
	public void defaultsUseAutoAndExposeOnlyChanceAndAnimationMode()
	{
		BarrowsBrothersRandomizerConfig config = new BarrowsBrothersRandomizerConfig() { };
		assertEquals(NpcAnimationMode.AUTO, config.npcAnimationMode());
		assertEquals(100, config.chanceToRandomize());
		assertEquals(2, java.util.Arrays.stream(BarrowsBrothersRandomizerConfig.class.getDeclaredMethods())
			.filter(method -> method.isAnnotationPresent(net.runelite.client.config.ConfigItem.class)).count());
		BarrowsBrothersRandomizerPlugin.migrateConfig(null);
	}

	@Test
	public void removesRetiredSettingsEvenWhenMigrationAlreadyRan()
	{
		Map<String, String> values = new HashMap<>();
		String group = BarrowsBrothersRandomizerConfig.GROUP + ".";
		values.put(group + "migratedLegacyNpcSettings", "true");
		values.put(group + "npcAnimationMode", "NATIVE");
		values.put(group + "nativeNpcAttacks", "false");
		values.put(group + "npcActionOverrides", "1173:2075=deny");
		BarrowsBrothersRandomizerPlugin.migrateConfig(manager(values));
		assertEquals("NATIVE", values.get(group + "npcAnimationMode"));
		assertFalse(values.containsKey(group + "nativeNpcAttacks"));
		assertFalse(values.containsKey(group + "npcActionOverrides"));
	}
}
