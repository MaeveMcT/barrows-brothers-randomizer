package com.barrowsthemes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.runelite.api.Model;
import net.runelite.api.SceneTileModel;
import net.runelite.api.SceneTilePaint;

/** Owns reversible edits; originals are captured once per scene object, never per tick. */
final class SceneEdits
{
	private final Set<Object> edited = Collections.newSetFromMap(new IdentityHashMap<>());
	private final List<Runnable> undo = new ArrayList<>();
	private final BarrowsTheme theme;
	private final boolean curated;

	SceneEdits() { this(BarrowsTheme.ZANARIS); }

	SceneEdits(BarrowsTheme theme) { this(theme, false); }

	SceneEdits(BarrowsTheme theme, boolean curated)
	{
		this.theme = theme;
		this.curated = curated;
	}

	boolean paint(SceneTilePaint paint, int texture)
	{
		if (paint == null || !edited.add(paint)) { return false; }
		int sw = paint.getSwColor(), se = paint.getSeColor(), nw = paint.getNwColor(), ne = paint.getNeColor();
		int originalTexture = paint.getTexture();
		undo.add(() -> {
			paint.setSwColor(sw); paint.setSeColor(se); paint.setNwColor(nw); paint.setNeColor(ne);
			paint.setTexture(originalTexture);
		});
		paint.setSwColor(floorColour(sw, originalTexture, texture));
		paint.setSeColor(floorColour(se, originalTexture, texture));
		paint.setNwColor(floorColour(nw, originalTexture, texture));
		paint.setNeColor(floorColour(ne, originalTexture, texture));
		paint.setTexture(texture);
		return true;
	}

	boolean floorModel(SceneTileModel model, int texture)
	{
		if (model == null || !edited.add(model)) { return false; }
		int[] textures = model.getTriangleTextureId();
		int[] originalTextures = textures == null ? null : textures.clone();
		int[][] colours = {model.getTriangleColorA(), model.getTriangleColorB(), model.getTriangleColorC()};
		for (int[] array : colours)
		{
			capture(array);
			for (int i = 0; i < array.length; i++)
			{
				// A null texture array cannot be attached via public API: leave these faces coloured.
				array[i] = floorColour(array[i], textures == null ? -1 : originalTextures[i], textures == null ? -1 : texture);
			}
		}
		if (textures != null)
		{
			undo.add(() -> System.arraycopy(originalTextures, 0, textures, 0, textures.length));
			java.util.Arrays.fill(textures, texture);
		}
		return true;
	}

	boolean wall(Model model) { return wall(model, -1); }

	boolean wall(Model model, int texture)
	{
		if (model == null || !edited.add(model)) { return false; }
		short[] textures = model.getFaceTextures();
		short[] originalTextures = textures == null ? null : textures.clone();
		for (int[] array : new int[][] {model.getFaceColors1(), model.getFaceColors2(), model.getFaceColors3()})
		{
			capture(array);
			for (int i = 0; i < array.length; i++)
			{
				boolean wasTextured = textures != null && originalTextures[i] >= 0;
				array[i] = texture >= 0 && wasTextured
					? ThemeColors.textureBrightness(array[i])
					: recolour(array[i], false, wasTextured);
			}
		}
		if (textures != null)
		{
			undo.add(() -> System.arraycopy(originalTextures, 0, textures, 0, textures.length));
			for (int i = 0; i < textures.length; i++)
			{
				// Only replace faces that already had texture coordinates/materials.
				textures[i] = texture >= 0 && originalTextures[i] >= 0 ? (short) texture : -1;
			}
		}
		return true;
	}

	private int floorColour(int colour, int originalTexture, int texture)
	{
		return texture >= 0 ? ThemeColors.textureBrightness(colour) : recolour(colour, true, originalTexture >= 0);
	}

	private int recolour(int colour, boolean floor, boolean originallyTextured)
	{
		return curated ? CuratedMaterials.recolour(theme, colour, floor, originallyTextured)
			: ThemeColors.recolour(colour, floor, originallyTextured, theme);
	}

	private void capture(int[] array)
	{
		int[] original = array.clone();
		undo.add(() -> System.arraycopy(original, 0, array, 0, array.length));
	}

	void restore()
	{
		for (int i = undo.size() - 1; i >= 0; i--) { undo.get(i).run(); }
		undo.clear();
		edited.clear();
	}
}
