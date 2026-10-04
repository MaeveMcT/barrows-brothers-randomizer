import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.SequenceDefinition;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.definitions.loaders.SequenceLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Store;

/** Offline classic layout corroboration for native attack families. No meshes/keys exported. */
public class NativeAttackStudy
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 3 || args.length % 2 != 1) { throw new IllegalArgumentException("NativeAttackStudy CACHE_COPY IDLE ATTACK [IDLE ATTACK...]"); }
		try (Store store = new Store(new File(args[0])))
		{
			store.load();
			Archive archive = store.getIndex(IndexType.CONFIGS).getArchive(ConfigType.SEQUENCE.getId());
			var files = archive.getFiles(store.getStorage().loadArchive(archive));
			SequenceLoader loader = new SequenceLoader().configureForRevision(archive.getRevision());
			for (int i = 1; i < args.length; i += 2)
			{
				int idle = Integer.parseInt(args[i]), attack = Integer.parseInt(args[i + 1]);
				SequenceDefinition a = loader.load(idle, files.findFile(idle).getContents());
				SequenceDefinition b = loader.load(attack, files.findFile(attack).getContents());
				Set<String> own = layouts(store, a), action = layouts(store, b);
				System.out.println(idle + " -> " + attack + " idle/attack layouts=" + own.size() + "/" + action.size()
					+ " shared=" + (!own.isEmpty() && !action.isEmpty() && own.containsAll(action)));
			}
		}
	}
	private static Set<String> layouts(Store store, SequenceDefinition sequence) throws Exception
	{
		if (sequence.animMayaID >= 0 || sequence.frameIDs == null) { throw new IllegalArgumentException("Not classic sequence " + sequence.getId()); }
		Set<Integer> maps = new HashSet<>();
		for (int frame : sequence.frameIDs)
		{
			Archive archive = store.getIndex(IndexType.ANIMATIONS).getArchive(frame >>> 16);
			byte[] packed = archive == null ? null : store.getStorage().loadArchive(archive);
			if (packed == null) { throw new IllegalArgumentException("Missing frame archive " + (frame >>> 16)); }
			byte[] bytes = archive.getFiles(packed).findFile(frame & 65535).getContents();
			maps.add((bytes[0] & 255) << 8 | bytes[1] & 255);
		}
		Set<String> layouts = new HashSet<>();
		for (int map : maps)
		{
			Archive archive = store.getIndex(IndexType.SKELETONS).getArchive(map);
			byte[] packed = archive == null ? null : store.getStorage().loadArchive(archive);
			if (packed == null) { throw new IllegalArgumentException("Missing skeleton " + map); }
			var definition = new FramemapLoader().load(map, archive.getFiles(packed).findFile(0).getContents());
			layouts.add(Arrays.toString(definition.types) + Arrays.deepToString(definition.frameMaps));
		}
		return layouts;
	}
}
