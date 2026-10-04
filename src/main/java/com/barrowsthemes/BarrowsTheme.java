package com.barrowsthemes;

/** Area-inspired fallback palettes; detailed material-role palettes live in CuratedMaterials. */
public enum BarrowsTheme
{
	ZANARIS("Zanaris", 28, 3, 38, 2, 100),
	CHAMBERS_OF_XERIC("Chambers of Xeric", 6, 2, 8, 1, 90),
	INFERNO("Inferno", 0, 0, 4, 6, 65);

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
