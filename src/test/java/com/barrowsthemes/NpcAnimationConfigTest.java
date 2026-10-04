package com.barrowsthemes;

import net.runelite.client.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NpcAnimationConfigTest
{
	@Test public void oldEnabledCheckboxMigratesToForceWithoutOverwritingAnExistingMode()
	{
		ConfigManager manager = mock(ConfigManager.class); String group = BarrowsThemesConfig.GROUP;
		when(manager.getConfiguration(group, "brotherAttackAnimations")).thenReturn("true");
		BarrowsThemesPlugin.migrateAnimationConfig(manager);
		verify(manager).setConfiguration(group, "npcAnimationMode", NpcAnimationMode.FORCE);
		verify(manager).unsetConfiguration(group, "brotherAttackAnimations");
		clearInvocations(manager); when(manager.getConfiguration(group, "npcAnimationMode")).thenReturn("NATIVE");
		BarrowsThemesPlugin.migrateAnimationConfig(manager);
		verify(manager, never()).setConfiguration(eq(group), eq("npcAnimationMode"), any(Object.class));
	}
	@Test public void defaultsKeepBorrowingOffButEnableOnlyTheSmallMappedNativeAttackSet()
	{
		BarrowsThemesConfig config = new BarrowsThemesConfig() { };
		assertEquals(NpcAnimationMode.NATIVE, config.npcAnimationMode());
		assertTrue(config.nativeNpcAttacks()); assertEquals("", config.npcActionOverrides()); assertFalse(config.recordNpcActions());
	}
}
