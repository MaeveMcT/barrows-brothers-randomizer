package com.barrowsbrothersrandomizer;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.loaders.NpcLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Store;

/** Offline decoder comparison; outside src and never packaged in the plugin. */
public class NpcAnimationAudit
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1) { throw new IllegalArgumentException("NpcAnimationAudit CACHE_COPY [NPC_ID...]"); }
		try (Store store = new Store(new File(args[0])))
		{
			store.load();
			Archive archive = store.getIndex(IndexType.CONFIGS).getArchive(ConfigType.NPC.getId());
			NpcLoader loader = new NpcLoader().configureForRevision(archive.getRevision());
			Map<String, Integer> stops = new TreeMap<>();
			int count = 0, mismatches = 0, noGaits = 0, noMovement = 0;
			for (FSFile file : archive.getFiles(store.getStorage().loadArchive(archive)).getFiles())
			{
				NativeNpcAnimations ours = NativeNpcAnimations.decode(file.getContents());
				NpcDefinition reference = loader.load(file.getFileId(), file.getContents());
				count++;
				if (!ours.complete()) { stops.merge(ours.note(), 1, Integer::sum); }
				int[] expected = {reference.standingAnimation, reference.walkingAnimation, reference.rotate180Animation,
					reference.rotateLeftAnimation, reference.rotateRightAnimation, reference.idleRotateLeftAnimation, reference.idleRotateRightAnimation,
					reference.runAnimation, reference.runRotate180Animation, reference.runRotateLeftAnimation, reference.runRotateRightAnimation,
					reference.crawlAnimation, reference.crawlRotate180Animation, reference.crawlRotateLeftAnimation, reference.crawlRotateRightAnimation};
				for (int i = 0; i < expected.length; i++) { expected[i] = normalize(expected[i]); }
				if (!Arrays.equals(ours.movement(), expected)) { mismatches++; }
				if (ours.idle() < 0 && ours.walk() < 0) { noGaits++; }
				if (Arrays.stream(ours.movement()).allMatch(id -> id < 0)) { noMovement++; }
				for (int i = 1; i < args.length; i++)
				{
					if (file.getFileId() == Integer.parseInt(args[i]))
					{
						System.out.println("NPC " + file.getFileId() + " " + reference.name + " " + ours.gaits()
							+ " models=" + Arrays.toString(reference.models) + "; " + ours.note());
					}
				}
			}
			System.out.println("definitions=" + count + " movement mismatches=" + mismatches + " no idle/walk=" + noGaits + " no movement=" + noMovement + " incomplete=" + stops);
		}
	}
	private static int normalize(int sequence) { return sequence == 65535 ? -1 : sequence; }
}
