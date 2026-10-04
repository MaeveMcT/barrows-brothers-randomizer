import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Store;

/** Offline facing heuristic: compare normalized horizontal mesh slices, not game rendering. */
public class CornerFacing
{
	public static void main(String[] args) throws Exception
	{
		if (args.length < 3) { throw new IllegalArgumentException("CornerFacing CACHE_DIRECTORY TARGET_MODEL SOURCE_MODEL..."); }
		try (Store store = new Store(new File(args[0])))
		{
			store.load();
			ModelDefinition target = load(store, Integer.parseInt(args[1]));
			for (int i = 2; i < args.length; i++)
			{
				ModelDefinition source = load(store, Integer.parseInt(args[i]));
				double[] scores = new double[4];
				for (int rotation = 0; rotation < 4; rotation++)
				{
					for (double height : new double[] {.2, .4, .6})
					{
						boolean[] a = slice(target, 0, height), b = slice(source, rotation, height);
						scores[rotation] += distance(a, b) + distance(b, a);
					}
				}
				System.out.println(args[1] + " vs " + args[i] + " scores at 0/512/1024/1536=" + Arrays.toString(scores));
			}
		}
	}

	private static ModelDefinition load(Store store, int id) throws Exception
	{
		Archive archive = store.getIndex(IndexType.MODELS).getArchive(id);
		if (archive == null) { throw new IllegalArgumentException("Missing model " + id); }
		byte[] packed = store.getStorage().loadArchive(archive);
		if (packed == null) { throw new IllegalArgumentException("Unavailable model " + id); }
		return new ModelLoader().load(id, archive.getFiles(packed).getFiles().iterator().next().getContents());
	}

	private static double[] bounds(int[] vertices)
	{
		return new double[] {Arrays.stream(vertices).min().getAsInt(), Arrays.stream(vertices).max().getAsInt()};
	}

	private static boolean[] slice(ModelDefinition model, int rotation, double fraction)
	{
		double[] bx = bounds(model.vertexX), by = bounds(model.vertexY), bz = bounds(model.vertexZ);
		if (bx[1] == bx[0] || bz[1] == bz[0]) { throw new IllegalArgumentException("Flat horizontal model axis"); }
		double height = by[0] + (by[1] - by[0]) * fraction;
		boolean[] grid = new boolean[32 * 32];
		for (int face = 0; face < model.faceCount; face++)
		{
			int[] vertices = {model.faceIndices1[face], model.faceIndices2[face], model.faceIndices3[face]};
			List<double[]> points = new ArrayList<>();
			for (int edge = 0; edge < 3; edge++)
			{
				int p = vertices[edge], q = vertices[(edge + 1) % 3];
				int yp = model.vertexY[p], yq = model.vertexY[q];
				if (yp == yq || height < Math.min(yp, yq) || height > Math.max(yp, yq)) { continue; }
				double fractionAlong = (height - yp) / (yq - yp);
				double x = (model.vertexX[p] + fractionAlong * (model.vertexX[q] - model.vertexX[p]) - bx[0]) / (bx[1] - bx[0]);
				double z = (model.vertexZ[p] + fractionAlong * (model.vertexZ[q] - model.vertexZ[p]) - bz[0]) / (bz[1] - bz[0]);
				for (int quarter = 0; quarter < rotation; quarter++) { double oldX = x; x = z; z = 1 - oldX; }
				points.add(new double[] {x, z});
			}
			if (points.size() < 2) { continue; }
			double[] p = points.get(0), q = points.get(1);
			for (int sample = 0; sample <= 128; sample++)
			{
				double t = sample / 128.0;
				int x = (int) Math.round((p[0] + t * (q[0] - p[0])) * 31);
				int z = (int) Math.round((p[1] + t * (q[1] - p[1])) * 31);
				if (x >= 0 && x < 32 && z >= 0 && z < 32) { grid[x + z * 32] = true; }
			}
		}
		return grid;
	}

	private static double distance(boolean[] first, boolean[] second)
	{
		double sum = 0, count = 0;
		for (int i = 0; i < first.length; i++)
		{
			if (!first[i]) { continue; }
			count++;
			int x = i % 32, z = i / 32;
			double nearest = 100000;
			for (int j = 0; j < second.length; j++)
			{
				if (second[j]) { nearest = Math.min(nearest, Math.pow(x - j % 32, 2) + Math.pow(z - j / 32, 2)); }
			}
			sum += nearest;
		}
		return count == 0 ? 100000 : sum / count;
	}
}
