package com.barrowsthemes;

import net.runelite.api.JagexColor;

/** Detailed material-role palettes, not image textures. See docs/theme-palettes.md. */
final class CuratedMaterials
{
	// Original three profiles use cache samples. New profiles are designed RGB palettes,
	// not researched cache materials. Model geometry is never copied by this class.
	// Zanaris FAIRY_WALL_MUSH_GREEN: foliage, stem and green detail. Underlay 142 supplies ground.
	private static final Profile ZANARIS = new Profile(24524, 10326, 30156, 21714, rgb(0x50a0b8), 100);
	// CoX RAIDS_WALL_1/2 stone colours; underlay 102 supplies the dark cave ground.
	private static final Profile XERIC = new Profile(28950, 29194, 28830, 28830, rgb(0x111e1a), 90);
	// Inferno cooled wall-edge rock, brighter edge and heated corner rock; sand-floor model colour 12.
	private static final Profile INFERNO = new Profile(8, 12, 949, 272, 12, 65);
	// Artistic area-inspired palettes: main stone, trim, accents, pale detail, ground.
	private static final Profile PRIFDDINAS = designed(0xd3d0df, 0x9e8ab5, 0x76bda6, 0xefe7ff, 0x687f79, 100);
	private static final Profile PYRAMID = designed(0xc8aa6d, 0xa28145, 0x49a5a2, 0xe7ce87, 0x977841, 95);
	private static final Profile DARKMEYER = designed(0x696371, 0x46404e, 0x872c3c, 0xb9a6a5, 0x30252e, 75);
	private static final Profile ICE_CAVES = designed(0xb3d2dc, 0x7ca9bd, 0x72c7e4, 0xe5f5fa, 0x7196ad, 100);
	private static final Profile DORGESH_KAAN = designed(0x9a8053, 0x625343, 0xcfa450, 0x83965c, 0x59483a, 85);
	private static final Profile ABYSS = designed(0x934553, 0x623347, 0xbc5f73, 0xb98aab, 0x4d283f, 80);

	private CuratedMaterials() { }

	static int recolour(BarrowsTheme theme, int colour, boolean floor, boolean originallyTextured)
	{
		if (colour < 0 || colour == ThemeColors.INVISIBLE || colour == 0) { return colour; }
		Profile profile = profile(theme);
		if (floor)
		{
			// Preserve the original corner gradients; sample ground hue/saturation, not tile geometry.
			return (profile.ground & ~127) | Math.max(2, (colour & 127) * profile.floorBrightness / 100);
		}
		if (originallyTextured) { return ThemeColors.recolour(colour, false, true, theme); }
		// Match hue/saturation rather than exact lit colours. Brown tunnels and purple tombs share
		// material roles but have different reference lightness. Rescale to the sampled material.
		switch (colour & ~127)
		{
			case 10752: return material(colour, profile.main, 10);
			case 64000: return material(colour, profile.main, 18);
			case 10624: return material(colour, profile.trim, 14);
			case 63872: return material(colour, profile.trim, 22);
			case 10496: return material(colour, profile.detail, 22);
			case 63744: return material(colour, profile.detail, 30);
			case 10368: return material(colour, profile.pale, 26);
			default: return ThemeColors.recolour(colour, false, false, theme);
		}
	}

	private static int material(int colour, int target, int referenceLightness)
	{
		int lightness = Math.max(2, Math.min(126, (colour & 127) * (target & 127) / referenceLightness));
		return (target & ~127) | lightness;
	}

	private static int rgb(int colour) { return JagexColor.rgbToHSL(colour, 1.0) & 0xffff; }

	private static Profile designed(int main, int trim, int detail, int pale, int ground, int floorBrightness)
	{
		return new Profile(rgb(main), rgb(trim), rgb(detail), rgb(pale), rgb(ground), floorBrightness);
	}

	private static Profile profile(BarrowsTheme theme)
	{
		switch (theme)
		{
			case CHAMBERS_OF_XERIC: return XERIC;
			case INFERNO: return INFERNO;
			case PRIFDDINAS: return PRIFDDINAS;
			case ANCIENT_PYRAMID: return PYRAMID;
			case DARKMEYER: return DARKMEYER;
			case FREMENNIK_ICE_CAVES: return ICE_CAVES;
			case DORGESH_KAAN: return DORGESH_KAAN;
			case ABYSS: return ABYSS;
			case ZANARIS: return ZANARIS;
			default: throw new IllegalArgumentException("Missing material profile for " + theme);
		}
	}

	private static final class Profile
	{
		final int main, trim, detail, pale, ground, floorBrightness;
		Profile(int main, int trim, int detail, int pale, int ground, int floorBrightness)
		{
			this.main = main; this.trim = trim; this.detail = detail; this.pale = pale;
			this.ground = ground; this.floorBrightness = floorBrightness;
		}
	}
}
