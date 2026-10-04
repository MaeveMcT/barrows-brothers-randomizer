package com.barrowsthemes;

import java.awt.image.BufferedImage;
import java.util.List;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TexturePreviewTest
{
	@Test
	public void copiesPixelsWithoutMutatingGameTextures()
	{
		int[] pixels = {0, 0x123456, 0xabcdef, 0xffffff};
		BufferedImage image = TexturePreview.image(pixels);
		assertEquals(0, image.getRGB(0, 0));
		assertEquals(0xff123456, image.getRGB(1, 0));
		assertEquals(0x123456, pixels[1]);
		pixels[1] = 0;
		assertEquals(0xff123456, image.getRGB(1, 0));
	}

	@Test
	public void rejectsMissingOrMalformedPixelArrays()
	{
		assertNull(TexturePreview.image(null));
		assertNull(TexturePreview.image(new int[0]));
		assertNull(TexturePreview.image(new int[3]));
		assertTrue(TexturePreview.capture(null).isEmpty());
	}

	@Test
	public void catalogKeepsActualIdsAndSkipsUnavailableTextures()
	{
		TextureProvider provider = mock(TextureProvider.class);
		when(provider.getTextures()).thenReturn(new Texture[] {null, mock(Texture.class), mock(Texture.class)});
		when(provider.load(1)).thenReturn(new int[] {0x123456});
		when(provider.load(2)).thenReturn(null);
		List<TexturePreview> previews = TexturePreview.capture(provider);
		assertEquals(1, previews.size());
		assertEquals(1, previews.get(0).id);
		verify(provider, never()).load(0);
	}
}
