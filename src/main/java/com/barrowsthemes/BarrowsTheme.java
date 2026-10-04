package com.barrowsthemes;

/** Area-inspired fallback palettes; detailed material-role palettes live in CuratedMaterials. */
public enum BarrowsTheme
{
	ZANARIS("Zanaris", 28, 3, 38, 2, 100),
	CHAMBERS_OF_XERIC("Chambers of Xeric", 6, 2, 8, 1, 90),
	INFERNO("Inferno", 0, 0, 4, 6, 65),
	PRIFDDINAS("Prifddinas", 41, 1, 48, 2, 100),
	ANCIENT_PYRAMID("Ancient Pyramid", 6, 3, 8, 4, 95),
	DARKMEYER("Darkmeyer", 56, 1, 0, 1, 75),
	FREMENNIK_ICE_CAVES("Fremennik ice caves", 33, 2, 36, 1, 100),
	DORGESH_KAAN("Dorgesh-Kaan", 8, 3, 10, 2, 85),
	ABYSS("Abyss", 59, 4, 61, 5, 80);

	final int floorHue;
	final int floorSaturation;
	final int wallHue;
	final int wallSaturation;
	final int brightnessPercent;
	private final String label;

	BarrowsTheme(String label, int floorHue, int floorSaturation, int wallHue, int wallSaturation, int brightnessPercent)
	{
		this.label = label;
		this.floorHue = floorHue;
		this.floorSaturation = floorSaturation;
		this.wallHue = wallHue;
		this.wallSaturation = wallSaturation;
		this.brightnessPercent = brightnessPercent;
	}

	boolean supportsWallModels()
	{
		return WallModelSources.hasWalls(this);
	}

	@Override
	public String toString() { return label; }
}
