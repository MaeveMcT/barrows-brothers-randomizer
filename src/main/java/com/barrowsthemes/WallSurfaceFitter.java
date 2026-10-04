package com.barrowsthemes;

import java.util.Arrays;
import net.runelite.api.Mesh;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

/** Detached CoX wall deformation in front-surface coordinates, not whole-mesh bounding boxes. */
final class WallSurfaceFitter
{
	private static final int ROWS = 9, COLUMNS = 9, MAX_ELEMENTS = 8192;
	private static final float END_OVERLAP = 8;
	private WallSurfaceFitter() { }

	static boolean fit(ModelData source, Model original, int placementRotation, int type)
	{
		if (type != 0 && type != 1 && type != 9) { return false; }
		Geometry from = Geometry.read(source, 0, type), to = Geometry.read(original, -placementRotation, type);
		if (from == null || to == null) { return false; }
		Profile a = Profile.build(from, false), b = Profile.build(to, true);
		if (a == null || b == null) { return false; }
		float[] x = source.getVerticesX(), y = source.getVerticesY(), z = source.getVerticesZ();
		double angle = (placementRotation & 2047) * Math.PI / 1024.0, sin = Math.sin(angle), cos = Math.cos(angle);
		// All validation/profile construction finishes before changing any detached vertex.
		for (int i = 0; i < from.u.length; i++)
		{
			float sourceMin = a.sample(from.y[i], -1), sourceMax = a.sample(from.y[i], -2);
			float fraction = clamp((from.u[i] - sourceMin) / (sourceMax - sourceMin));
			// Native CoX endpoints sit on cell edges even though their surface depth changes with height.
			boolean start = type == 9 && from.z[i] <= -63.99f || type == 1 && from.x[i] <= -63.99f;
			boolean end = type == 9 && from.x[i] >= 63.99f || type == 1 && from.z[i] >= 63.99f;
			if (start && end) { fraction = .5f; }
			else if (start) { fraction = 0; }
			else if (end) { fraction = 1; }
			// A mid-height section's progress endpoints are not the roof's endpoints.
			// Scaling height once preserves native roof relief instead of collapsing higher
			// cell-edge vertices onto a sampled roof and opening upper wall/post joins.
			float height = to.minY + clamp((from.y[i] - from.minY) / (from.maxY - from.minY)) * (to.maxY - to.minY);
			float targetMin = type == 1 ? b.sample(height, -1) : -64;
			float targetMax = type == 1 ? b.sample(height, -2) : 64;
			float u = targetMin - END_OVERLAP + fraction * (targetMax - targetMin + 2 * END_OVERLAP);
			// Keep local rock relief, taper it at joins, and overlap by 1/16 of a tile.
			float reliefScale = Math.min(.35f, (targetMax - targetMin) / (sourceMax - sourceMin)) * 4 * fraction * (1 - fraction);
			// Type 9 is a full diagonal wall, type 1 is its tiny triangular corner post.
			// Their shared tile-corner anchors must agree even when original brick relief differs.
			float sharedV = type == 9 ? 0 : 64;
			float edgeWeight = type == 1 ? 0 : Math.min(1, Math.min(fraction, 1 - fraction) * 8);
			float v = sharedV + (b.spine(height, fraction) - sharedV) * edgeWeight
				+ (from.v[i] - a.spine(from.y[i], fraction)) * reliefScale;
			float localX = type == 0 ? -v : u - v, localZ = type == 0 ? u : u + v;
			x[i] = Math.round(localX * cos + localZ * sin);
			y[i] = Math.round(height);
			z[i] = Math.round(localZ * cos - localX * sin);
		}
		return true;
	}

	private static float clamp(float f) { return Math.max(0, Math.min(1, f)); }

	private static final class Geometry
	{
		float[] x, y, z, u, v;
		int[] a, b, c;
		boolean[] front;
		float minY, maxY;

		static Geometry read(Mesh<?> mesh, int rotation, int type)
		{
			int vertices = mesh.getVerticesCount(), faces = mesh.getFaceCount();
			float[] x = mesh.getVerticesX(), y = mesh.getVerticesY(), z = mesh.getVerticesZ();
			int[] a = mesh.getFaceIndices1(), b = mesh.getFaceIndices2(), c = mesh.getFaceIndices3();
			if (vertices < 3 || vertices > MAX_ELEMENTS || faces < 1 || faces > MAX_ELEMENTS
				|| x == null || y == null || z == null || x.length < vertices || y.length < vertices || z.length < vertices
				|| a == null || b == null || c == null || a.length < faces || b.length < faces || c.length < faces) { return null; }
			Geometry g = new Geometry();
			g.x = new float[vertices]; g.z = new float[vertices]; g.u = new float[vertices]; g.v = new float[vertices];
			g.y = y; g.a = a; g.b = b; g.c = c; g.front = new boolean[faces];
			g.minY = Float.POSITIVE_INFINITY; g.maxY = Float.NEGATIVE_INFINITY;
			double angle = (rotation & 2047) * Math.PI / 1024.0, sin = Math.sin(angle), cos = Math.cos(angle);
			for (int i = 0; i < vertices; i++)
			{
				if (!Float.isFinite(x[i]) || !Float.isFinite(y[i]) || !Float.isFinite(z[i]) || Math.abs(x[i]) > 4096 || Math.abs(y[i]) > 4096 || Math.abs(z[i]) > 4096) { return null; }
				g.x[i] = (float) (x[i] * cos + z[i] * sin); g.z[i] = (float) (z[i] * cos - x[i] * sin);
				g.u[i] = type == 0 ? g.z[i] : (g.x[i] + g.z[i]) * .5f;
				g.v[i] = type == 0 ? -g.x[i] : (g.z[i] - g.x[i]) * .5f;
				g.minY = Math.min(g.minY, y[i]); g.maxY = Math.max(g.maxY, y[i]);
			}
			// Barrows has underground backing geometry down to +160. It is not visible wall height.
			g.maxY = Math.min(0, g.maxY);
			if (g.maxY - g.minY < 1) { return null; }
			int[] colours = mesh instanceof Model ? ((Model) mesh).getFaceColors3() : null;
			for (int f = 0; f < faces; f++)
			{
				int p = a[f], q = b[f], r = c[f];
				if (p < 0 || q < 0 || r < 0 || p >= vertices || q >= vertices || r >= vertices) { return null; }
				if (colours != null && (f >= colours.length || colours[f] == -2)) { continue; }
				double ux = g.x[q] - g.x[p], uy = y[q] - y[p], uz = g.z[q] - g.z[p];
				double vx = g.x[r] - g.x[p], vy = y[r] - y[p], vz = g.z[r] - g.z[p];
				double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
				g.front[f] = (type == 0 ? -nx : -nx + nz) > .001 && Math.abs(ny) <= Math.hypot(nx, nz);
			}
			return g;
		}
	}

	private static final class Profile
	{
		float minY, maxY;
		final float[] min = new float[ROWS], max = new float[ROWS];
		final float[][] curve = new float[ROWS][COLUMNS];

		static Profile build(Geometry g, boolean outerSkin)
		{
			Profile p = new Profile();
			// Avoid isolated roof peaks and underground backing; clamp beyond the sampled skin.
			p.minY = g.minY + (g.maxY - g.minY) * .15f;
			p.maxY = g.minY + (g.maxY - g.minY) * .9f;
			float[] segments = new float[g.front.length * 4];
			boolean[] valid = new boolean[ROWS];
			for (int row = 0; row < ROWS; row++)
			{
				float height = p.minY + (p.maxY - p.minY) * row / (ROWS - 1);
				int count = 0;
				p.min[row] = Float.POSITIVE_INFINITY; p.max[row] = Float.NEGATIVE_INFINITY;
				for (int f = 0; f < g.front.length; f++)
				{
					if (!g.front[f]) { continue; }
					int start = count, first = g.a[f], second = g.b[f], third = g.c[f];
					count = edge(g, height, first, second, segments, count);
					count = edge(g, height, second, third, segments, count);
					if (count - start < 4) { count = edge(g, height, third, first, segments, count); }
					if (count - start != 4) { count = start; continue; }
					p.min[row] = Math.min(p.min[row], Math.min(segments[start], segments[start + 2]));
					p.max[row] = Math.max(p.max[row], Math.max(segments[start], segments[start + 2]));
				}
				if (p.max[row] - p.min[row] < 1) { continue; }
				Arrays.fill(p.curve[row], outerSkin ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY);
				for (int col = 0; col < COLUMNS; col++)
				{
					float u = p.min[row] + (p.max[row] - p.min[row]) * col / (COLUMNS - 1);
					for (int s = 0; s < count; s += 4)
					{
						float lo = Math.min(segments[s], segments[s + 2]), hi = Math.max(segments[s], segments[s + 2]);
						if (u < lo - .01f || u > hi + .01f) { continue; }
						float span = segments[s + 2] - segments[s];
						float v = Math.abs(span) < .001f ? Math.min(segments[s + 1], segments[s + 3])
							: segments[s + 1] + clamp((u - segments[s]) / span) * (segments[s + 3] - segments[s + 1]);
						p.curve[row][col] = outerSkin ? Math.max(p.curve[row][col], v) : Math.min(p.curve[row][col], v);
					}
				}
				valid[row] = true;
				for (float value : p.curve[row]) { if (!Float.isFinite(value)) { valid[row] = false; } }
			}
			for (int row = 0; row < ROWS; row++)
			{
				if (valid[row]) { continue; }
				int nearest = -1;
				for (int other = 0; other < ROWS; other++) { if (valid[other] && (nearest < 0 || Math.abs(other - row) < Math.abs(nearest - row))) { nearest = other; } }
				if (nearest < 0) { return null; }
				p.min[row] = p.min[nearest]; p.max[row] = p.max[nearest];
				System.arraycopy(p.curve[nearest], 0, p.curve[row], 0, COLUMNS);
			}
			return p;
		}

		private static int edge(Geometry g, float height, int a, int b, float[] segments, int offset)
		{
			if (g.y[a] == g.y[b] || height < Math.min(g.y[a], g.y[b]) || height > Math.max(g.y[a], g.y[b])) { return offset; }
			float t = (height - g.y[a]) / (g.y[b] - g.y[a]);
			segments[offset++] = g.u[a] + t * (g.u[b] - g.u[a]);
			segments[offset++] = g.v[a] + t * (g.v[b] - g.v[a]);
			return offset;
		}

		float sample(float height, int col)
		{
			float row = clamp((height - minY) / (maxY - minY)) * (ROWS - 1);
			int lo = (int) row, hi = Math.min(ROWS - 1, lo + 1);
			float a = col == -1 ? min[lo] : col == -2 ? max[lo] : curve[lo][col];
			float b = col == -1 ? min[hi] : col == -2 ? max[hi] : curve[hi][col];
			return a + (row - lo) * (b - a);
		}

		float spine(float height, float fraction)
		{
			float col = clamp(fraction) * (COLUMNS - 1);
			int lo = (int) col, hi = Math.min(COLUMNS - 1, lo + 1);
			return sample(height, lo) + (col - lo) * (sample(height, hi) - sample(height, lo));
		}
	}
}
