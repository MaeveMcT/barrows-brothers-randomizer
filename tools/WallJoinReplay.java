package com.barrowsthemes;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Store;

/** Offline-only geometry replay. Never packaged in the plugin; needs a private cache copy, no keys. */
public final class WallJoinReplay
{
	static final class Mesh
	{
		float[] x, y, z;
		int[] a, b, c;
		Mesh(ModelDefinition m)
		{
			x = floats(m.vertexX); y = floats(m.vertexY); z = floats(m.vertexZ);
			a = m.faceIndices1; b = m.faceIndices2; c = m.faceIndices3;
		}
		private static float[] floats(int[] in) { float[] out = new float[in.length]; for (int i = 0; i < in.length; i++) { out[i] = in[i]; } return out; }
		<T> T view(Class<T> api)
		{
			return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] {api}, (proxy, method, args) -> {
				switch (method.getName())
				{
					case "getVerticesCount": return x.length;
					case "getVerticesX": return x;
					case "getVerticesY": return y;
					case "getVerticesZ": return z;
					case "getFaceCount": return a.length;
					case "getFaceIndices1": return a;
					case "getFaceIndices2": return b;
					case "getFaceIndices3": return c;
					case "getFaceColors3": return new int[a.length];
					default: throw new UnsupportedOperationException(method.getName());
				}
			}));
		}
		void rotate(int rotation)
		{
			double angle = rotation * Math.PI / 1024.0, sin = Math.sin(angle), cos = Math.cos(angle);
			for (int i = 0; i < x.length; i++) { float oldX = x[i]; x[i] = (float) (oldX * cos + z[i] * sin); z[i] = (float) (z[i] * cos - oldX * sin); }
		}
		void yBounds(float lo, float hi)
		{
			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
			for (float v : y) { min = Math.min(min, v); max = Math.max(max, v); }
			for (int i = 0; i < y.length; i++) { y[i] = lo + (y[i] - min) / (max - min) * (hi - lo); }
		}
		List<double[]> section(double height, int quarter, int type)
		{
			List<double[]> out = new ArrayList<>();
			double angle = quarter * Math.PI / 2, nx = -Math.cos(angle) + (type == 0 ? 0 : Math.sin(angle)), nz = Math.sin(angle) + (type == 0 ? 0 : Math.cos(angle));
			for (int f = 0; f < a.length; f++)
			{
				int p = a[f], q = b[f], r = c[f];
				double ux = x[q] - x[p], uy = y[q] - y[p], uz = z[q] - z[p];
				double vx = x[r] - x[p], vy = y[r] - y[p], vz = z[r] - z[p];
				double fx = uy * vz - uz * vy, fy = uz * vx - ux * vz, fz = ux * vy - uy * vx;
				if (fx * nx + fz * nz <= 0) { continue; }
				int[] vertices = {p, q, r}; List<double[]> points = new ArrayList<>();
				for (int edge = 0; edge < 3; edge++)
				{
					int i = vertices[edge], j = vertices[(edge + 1) % 3];
					if (y[i] == y[j] || height < Math.min(y[i], y[j]) || height > Math.max(y[i], y[j])) { continue; }
					double t = (height - y[i]) / (y[j] - y[i]);
					points.add(new double[] {x[i] + t * (x[j] - x[i]), z[i] + t * (z[j] - z[i])});
				}
				if (points.size() >= 2) { double[] i = points.get(0), j = points.get(1); out.add(new double[] {i[0], i[1], j[0], j[1]}); }
			}
			return out;
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
	private static double distance(double x, double z, double[] s)
	{
		double dx = s[2] - s[0], dz = s[3] - s[1], length = dx * dx + dz * dz;
		double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((x - s[0]) * dx + (z - s[1]) * dz) / length));
		return Math.hypot(x - s[0] - t * dx, z - s[1] - t * dz);
	}
	private static int type(int[] c) { return c[0] == 6618 ? 0 : c[0] == 6619 ? 1 : 9; }
	private static List<double[]> worldSection(Mesh mesh, int[] c, double height)
	{
		List<double[]> segments = mesh.section(height - c[8], c[2], type(c));
		for (double[] s : segments) { s[0] += c[6] * 128; s[2] += c[6] * 128; s[1] += c[7] * 128; s[3] += c[7] * 128; }
		return segments;
	}
	private static double cross(double x, double z, double[] s) { return (s[2] - s[0]) * (z - s[1]) - (s[3] - s[1]) * (x - s[0]); }
	private static double separation(List<double[]> a, List<double[]> b)
	{
		double closest = Double.POSITIVE_INFINITY;
		for (double[] p : a) { for (double[] q : b)
		{
			boolean boxesOverlap = Math.max(Math.min(p[0], p[2]), Math.min(q[0], q[2])) <= Math.min(Math.max(p[0], p[2]), Math.max(q[0], q[2]))
				&& Math.max(Math.min(p[1], p[3]), Math.min(q[1], q[3])) <= Math.min(Math.max(p[1], p[3]), Math.max(q[1], q[3]));
			if (boxesOverlap && cross(q[0], q[1], p) * cross(q[2], q[3], p) <= 0 && cross(p[0], p[1], q) * cross(p[2], p[3], q) <= 0) { return 0; }
			closest = Math.min(closest, Math.min(Math.min(distance(p[0],p[1],q), distance(p[2],p[3],q)), Math.min(distance(q[0],q[1],p), distance(q[2],q[3],p))));
		} }
		return closest;
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length < 1 || args.length > 2) { throw new IllegalArgumentException("WallJoinReplay CACHE_COPY [--surface]"); }
		boolean surface = args.length == 2 && args[1].equals("--surface");
		// Captured Karil placements: model, source, quarter, old native offset, live y bounds, template x/z, height.
		int[][] cases = {{6621,32432,3,0,-256,18,3555,9686,-2642}, {6620,32432,3,0,-258,6,3556,9685,-2630},
			{6619,32439,3,1024,-248,164,3555,9687,-2652}, {6619,32439,3,1024,-270,162,3556,9686,-2650},
			{6620,32432,1,0,-252,6,3546,9680,-2774}, {6619,32439,1,1024,-256,172,3545,9680,-2772},
			{6619,32439,1,1024,-253,174,3546,9679,-2766}, {6621,32432,1,0,-246,1,3547,9679,-2766},
			{6620,32432,3,0,-251,10,3554,9687,-2658}, {6619,32439,3,1024,-250,166,3554,9688,-2662},
			// Straight-wall control: same native source family, not additional captured placements.
			{6618,32435,0,0,-250,160,0,0,0}, {6618,32437,0,0,-250,160,0,0,0}};
		Mesh[] originals = new Mesh[cases.length], replacements = new Mesh[cases.length];
		int failures = 0, index = 0;
		try (Store store = new Store(new File(args[0])))
		{
			store.load();
			for (int[] c : cases)
			{
				Mesh target = new Mesh(load(store, c[0])), source = new Mesh(load(store, c[1]));
				target.rotate(c[2] * 512); target.yBounds(c[4], c[5]);
				boolean fittedOk = surface ? WallSurfaceFitter.fit(source.view(ModelData.class), target.view(Model.class), c[2] * 512, type(c))
					: WallModelFitter.fit(source.view(ModelData.class), target.view(Model.class), c[2] * 512 + c[3]);
				if (!fittedOk) { throw new AssertionError("Fit failed for " + c[0] + "/" + c[1]); }
				double worst = 0;
				for (int height : new int[] {-180, -120, -60})
				{
					List<double[]> original = target.section(height, c[2], type(c)), fitted = source.section(height, c[2], type(c));
					if (original.isEmpty()) { throw new AssertionError("Missing original front section"); }
					for (double[] segment : original)
					{
						for (int endpoint = 0; endpoint < 4; endpoint += 2)
						{
							double nearest = Double.POSITIVE_INFINITY;
							for (double[] replacement : fitted) { nearest = Math.min(nearest, distance(segment[endpoint], segment[endpoint + 1], replacement)); }
							worst = Math.max(worst, nearest);
						}
					}
				}
				System.out.printf("model=%d source=%d quarter=%d maximum front-surface miss=%.2f local units (limit 24)%n", c[0], c[1], c[2], worst);
				if (worst > 24) { failures++; }
				originals[index] = target; replacements[index++] = source;
			}
			for (int[] pair : new int[][] {{0,3},{1,3},{0,2},{8,2},{8,9},{4,5},{4,6},{7,6}})
			{
				int i = pair[0], j = pair[1];
				double originalGap = 0, fittedGap = 0;
				for (int offset : new int[] {60,120,180})
				{
					double height = Math.min(cases[i][8], cases[j][8]) - offset;
					originalGap = Math.max(originalGap, separation(worldSection(originals[i],cases[i],height),worldSection(originals[j],cases[j],height)));
					fittedGap = Math.max(fittedGap, separation(worldSection(replacements[i],cases[i],height),worldSection(replacements[j],cases[j],height)));
				}
				System.out.printf("join=%d/%d original gap=%.2f fitted gap=%.2f local units%n",i,j,originalGap,fittedGap);
				if (fittedGap > originalGap + 8) { failures++; }
			}
		}
		if (failures > 0) { throw new AssertionError(failures + " replay checks failed (front miss >24 or join gap >original+8 local units)"); }
	}
}
