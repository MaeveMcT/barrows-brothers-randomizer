package com.barrowsbrothersrandomizer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BarrowsBrothersRandomizerLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BarrowsBrothersRandomizerPlugin.class);
		RuneLite.main(args);
	}
}
