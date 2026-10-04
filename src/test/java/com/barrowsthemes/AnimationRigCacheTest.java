package com.barrowsthemes;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AnimationRigCacheTest
{
	private Client client;
	private IndexDataBase configs, frames, skeletons;
	private AnimationRigCache cache;
	@Before public void setup()
	{
		client = mock(Client.class); configs = mock(IndexDataBase.class);
		frames = mock(IndexDataBase.class); skeletons = mock(IndexDataBase.class);
		when(client.getRevision()).thenReturn(240);
		when(client.getIndexConfig()).thenReturn(configs);
		when(client.getIndex(0)).thenReturn(frames);
		when(client.getIndex(1)).thenReturn(skeletons);
		when(configs.loadData(12, 100)).thenReturn(sequence(10, 1));
		when(configs.loadData(12, 200)).thenReturn(sequence(20, 1));
		when(frames.loadData(10, 0)).thenReturn(new byte[] {0, 7, 1, 1, 64});
		when(frames.loadData(20, 0)).thenReturn(new byte[] {0, 8, 1, 1, 64});
		when(skeletons.loadData(7, 0)).thenReturn(new byte[] {1, 1, 1, 0});
		when(skeletons.loadData(8, 0)).thenReturn(new byte[] {1, 1, 1, 0});
		cache = new AnimationRigCache(client);
	}
	static byte[] sequence(int archive, int count)
	{
		try
		{
			ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
			out.writeByte(1); out.writeShort(count);
			for (int i = 0; i < count; i++) { out.writeShort(2); }
			for (int i = 0; i < count; i++) { out.writeShort(i); }
			for (int i = 0; i < count; i++) { out.writeShort(archive); }
			out.writeByte(0); return bytes.toByteArray();
		}
		catch (java.io.IOException impossible) { throw new AssertionError(impossible); }
	}
	@Test public void matchingLayoutsAcrossDifferentSkeletonIdsArePositiveEvidenceAndCached()
	{
		assertTrue(AnimationRigCache.shared(cache.compare(100, 200)));
		assertTrue(cache.compare(100, 200).contains("model skin groups not verified"));
		verify(frames).loadData(10, 0); verify(frames).loadData(20, 0);
		verify(skeletons).loadData(7, 0); verify(skeletons).loadData(8, 0);
		when(client.getTickCount()).thenReturn(1);
		assertTrue(AnimationRigCache.shared(cache.compare(100, 200)));
		verify(configs).loadData(12, 100); verify(configs).loadData(12, 200);
	}
	@Test public void mismatchedLabelsDoNotBecomeCompatibleJustBecauseTransformsWouldNotThrow()
	{
		when(skeletons.loadData(8, 0)).thenReturn(new byte[] {1, 1, 1, 3});
		assertEquals("Different classic frame-map layouts", cache.compare(100, 200));
	}
	@Test public void examinesEveryActionFrameNotJustTheFirst()
	{
		when(configs.loadData(12, 200)).thenReturn(sequence(20, 2));
		when(frames.loadData(20, 1)).thenReturn(new byte[] {0, 9, 1, 1, 64});
		when(skeletons.loadData(9, 0)).thenReturn(new byte[] {1, 2, 1, 0});
		assertFalse(AnimationRigCache.shared(cache.compare(100, 200)));
	}
	@Test public void mayaAndWeightedSkeletonsRemainUnknown()
	{
		when(configs.loadData(12, 200)).thenReturn(new byte[] {13, 0, 0, 0, 1, 0});
		assertTrue(cache.compare(100, 200).contains("Maya/weighted"));
		cache.clear(); when(configs.loadData(12, 200)).thenReturn(sequence(20, 1));
		when(skeletons.loadData(8, 0)).thenReturn(new byte[] {1, 1, 1, 0, 0, 1});
		assertTrue(cache.compare(100, 200).contains("weighted skeleton"));
	}
	@Test public void missingAssetsRetryOncePerGameTickAndClearReleasesCaches()
	{
		when(frames.loadData(20, 0)).thenReturn(null);
		assertFalse(AnimationRigCache.shared(cache.compare(100, 200)));
		cache.compare(100, 200); verify(frames).loadData(20, 0);
		when(client.getTickCount()).thenReturn(1);
		when(frames.loadData(20, 0)).thenReturn(new byte[] {0, 8, 1, 1, 64});
		assertTrue(AnimationRigCache.shared(cache.compare(100, 200)));
		verify(frames, times(2)).loadData(20, 0);
		cache.clear(); assertTrue(AnimationRigCache.shared(cache.compare(100, 200)));
		verify(configs, times(2)).loadData(12, 100);
	}
	@Test public void capsCacheReadsPerGameTickWhileEventuallyFinishingLongSequences()
	{
		AtomicInteger reads = new AtomicInteger();
		when(configs.loadData(anyInt(), anyInt())).thenAnswer(i -> { reads.incrementAndGet(); return sequence(10, 40); });
		when(frames.loadData(anyInt(), anyInt())).thenAnswer(i -> { reads.incrementAndGet(); return new byte[] {0, 7, 1, 1, 64}; });
		when(skeletons.loadData(anyInt(), anyInt())).thenAnswer(i -> { reads.incrementAndGet(); return new byte[] {1, 1, 1, 0}; });
		String evidence = "";
		for (int tick = 0; tick < 5; tick++)
		{
			when(client.getTickCount()).thenReturn(tick); reads.set(0);
			evidence = cache.compare(100, 200); cache.compare(100, 200);
			assertTrue("reads=" + reads, reads.get() <= 24);
		}
		assertTrue(AnimationRigCache.shared(evidence));
	}
	@Test public void unknownTransformTypesAreNotClassifiedAsClassicCompatibility()
	{
		when(skeletons.loadData(8, 0)).thenReturn(new byte[] {2, 1, 6, 1, 1, 0, 0});
		assertTrue(cache.compare(100, 200).contains("Unsupported skeleton transform type"));
	}
	@Test public void malformedUnknownAndOldMetadataNeverGrantsCompatibility()
	{
		assertNotNull(AnimationRigCache.decode(new byte[] {1, 0, 4}).problem);
		assertNotNull(AnimationRigCache.decode(new byte[] {(byte) 222, 0}).problem);
		when(frames.loadData(20, 0)).thenReturn(new byte[] {0, 8, 1, 7});
		assertTrue(cache.compare(100, 200).contains("Malformed"));
		cache.clear(); when(client.getRevision()).thenReturn(220);
		assertTrue(cache.compare(100, 200).contains("schema/revision"));
	}
}
