package com.barrowsthemes;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;

/** Detached previews: texture pixels are read/copied only on the client thread. */
final class TexturePreview
{
	final int id;
	final BufferedImage image;

	TexturePreview(int id, BufferedImage image) { this.id = id; this.image = image; }

	static List<TexturePreview> capture(TextureProvider provider)
	{
		if (provider == null || provider.getTextures() == null) { return Collections.emptyList(); }
		Texture[] textures = provider.getTextures();
		List<TexturePreview> previews = new ArrayList<>();
		for (int id = 0; id < textures.length && id <= Short.MAX_VALUE; id++)
		{
			if (textures[id] == null) { continue; }
			BufferedImage image = image(provider.load(id));
			if (image != null) { previews.add(new TexturePreview(id, image)); }
		}
		return previews;
	}

	static BufferedImage image(int[] pixels)
	{
		if (pixels == null || pixels.length == 0) { return null; }
		int size = (int) Math.sqrt(pixels.length);
		if (size * size != pixels.length) { return null; }
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		int[] copy = new int[pixels.length];
		for (int i = 0; i < pixels.length; i++) { copy[i] = pixels[i] == 0 ? 0 : 0xff000000 | pixels[i]; }
		image.setRGB(0, 0, size, size, copy, 0, size);
		return image;
	}
}
