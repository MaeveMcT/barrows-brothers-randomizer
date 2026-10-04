package com.barrowsthemes;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BarrowsThemesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BarrowsThemesPlugin.class);
		RuneLite.main(args);
	}
}
