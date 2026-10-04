package com.barrowsthemes;

import net.runelite.api.Mesh;
import net.runelite.api.Model;
import net.runelite.api.ModelData;

/** Fits a detached mesh to the original model-space bounds, without touching the original. */
final class WallModelFitter
{
	private WallModelFitter() { }

	static boolean fit(ModelData source, Model original, int rotation)
	{
		Bounds target = bounds(original);
		if (target == null || bounds(source) == null) { return false; }
		float[] x = source.getVerticesX(), y = source.getVerticesY(), z = source.getVerticesZ();
		double angle = (rotation & 2047) * Math.PI / 1024.0;
		double sin = Math.sin(angle), cos = Math.cos(angle);
		for (int i = 0; i < source.getVerticesCount(); i++)
		{
			float oldX = x[i], oldZ = z[i];
			x[i] = (float) (oldX * cos + oldZ * sin);
			z[i] = (float) (oldZ * cos - oldX * sin);
		}
		Bounds from = bounds(source);
		if (from == null || !canFit(from.xMin, from.xMax, target.xMin, target.xMax)
			|| !canFit(from.yMin, from.yMax, target.yMin, target.yMax)
			|| !canFit(from.zMin, from.zMax, target.zMin, target.zMax)) { return false; }
		for (int i = 0; i < source.getVerticesCount(); i++)
		{
			x[i] = axis(x[i], from.xMin, from.xMax, target.xMin, target.xMax);
			y[i] = axis(y[i], from.yMin, from.yMax, target.yMin, target.yMax);
			z[i] = axis(z[i], from.zMin, from.zMax, target.zMin, target.zMax);
		}
		return true;
	}

	static String boundsSummary(Mesh<?> mesh)
	{
		Bounds b = mesh == null ? null : bounds(mesh);
		return b == null ? "unavailable" : "x=" + Math.round(b.xMin) + ".." + Math.round(b.xMax)
			+ ", y=" + Math.round(b.yMin) + ".." + Math.round(b.yMax) + ", z=" + Math.round(b.zMin) + ".." + Math.round(b.zMax);
	}

	static int radius(Model model)
	{
		Bounds bounds = bounds(model);
		if (bounds == null) { return -1; }
		return (int) Math.ceil(Math.max(Math.max(Math.abs(bounds.xMin), Math.abs(bounds.xMax)),
			Math.max(Math.abs(bounds.zMin), Math.abs(bounds.zMax)))) + 1;
	}

	private static boolean canFit(float min, float max, float targetMin, float targetMax)
	{
		return max > min || targetMin == targetMax;
	}

	private static float axis(float value, float min, float max, float targetMin, float targetMax)
	{
		if (max == min || targetMin == targetMax) { return targetMin; }
		float fitted = targetMin + (value - min) / (max - min) * (targetMax - targetMin);
		return Math.max(targetMin, Math.min(targetMax, Math.round(fitted)));
	}

	private static Bounds bounds(Mesh<?> mesh)
	{
		int count = mesh.getVerticesCount();
		float[] x = mesh.getVerticesX(), y = mesh.getVerticesY(), z = mesh.getVerticesZ();
		if (count <= 0 || x == null || y == null || z == null || x.length < count || y.length < count || z.length < count) { return null; }
		Bounds bounds = new Bounds();
		for (int i = 0; i < count; i++)
		{
			if (!Float.isFinite(x[i]) || !Float.isFinite(y[i]) || !Float.isFinite(z[i])) { return null; }
			bounds.xMin = Math.min(bounds.xMin, x[i]); bounds.xMax = Math.max(bounds.xMax, x[i]);
			bounds.yMin = Math.min(bounds.yMin, y[i]); bounds.yMax = Math.max(bounds.yMax, y[i]);
			bounds.zMin = Math.min(bounds.zMin, z[i]); bounds.zMax = Math.max(bounds.zMax, z[i]);
		}
		return bounds;
	}

	private static final class Bounds
	{
		float xMin = Float.POSITIVE_INFINITY, yMin = Float.POSITIVE_INFINITY, zMin = Float.POSITIVE_INFINITY;
		float xMax = Float.NEGATIVE_INFINITY, yMax = Float.NEGATIVE_INFINITY, zMax = Float.NEGATIVE_INFINITY;
	}
}
