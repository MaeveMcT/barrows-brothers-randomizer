package com.barrowsthemes;

/** OSRS packed HSL, not RGB. Retains lighting and renderer sentinel values. */
final class ThemeColors
{
	static final int INVISIBLE = 12345678;

	private ThemeColors() { }

	static int recolour(int colour, boolean floor, boolean originallyTextured)
	{
		return recolour(colour, floor, originallyTextured, BarrowsTheme.ZANARIS);
	}

	static int recolour(int colour, boolean floor, boolean originallyTextured, BarrowsTheme theme)
	{
		if (colour < 0 || colour == INVISIBLE)
		{
			return colour;
		}
		int lightness = colour & 127;
		// Textured colours carry brightness rather than a material hue.
		if (originallyTextured)
		{
			lightness = Math.max(30, Math.min(100, lightness));
		}
		lightness = Math.max(2, lightness * theme.brightnessPercent / 100);
		return ((floor ? theme.floorHue : theme.wallHue) << 10)
			| ((floor ? theme.floorSaturation : theme.wallSaturation) << 7) | lightness;
	}

	static int textureBrightness(int colour)
	{
		return colour < 0 || colour == INVISIBLE ? colour : Math.max(2, Math.min(126, colour & 127));
	}
}
