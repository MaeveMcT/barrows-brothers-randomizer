package com.barrowsbrothersrandomizer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;

/** Client-thread-only classic rig evidence. Prepared outside animate(); bounded loads and caches, no reflection. */
final class AnimationRigCache
{
	private static final int MAX_LOADS_PER_TICK = 24;
	private final Client client;
	private final Cache<Integer, Sequence> sequences = CacheBuilder.newBuilder().maximumSize(256).build();
	private final Cache<Long, Rig> frames = CacheBuilder.newBuilder().maximumSize(2048).build();
	private final Cache<Integer, Rig> skeletons = CacheBuilder.newBuilder().maximumSize(256).build();
	private final Cache<Long, Integer> misses = CacheBuilder.newBuilder().maximumSize(2048).build();
	private int tick = Integer.MIN_VALUE, loads;

	AnimationRigCache(Client client) { this.client = client; }

	String compare(int nativeSequence, int action)
	{
		Sequence own = sequence(nativeSequence), borrowed = sequence(action);
		if (!own.complete() || !borrowed.complete()) { return own.problem != null ? own.problem : borrowed.problem != null ? borrowed.problem : "Waiting for classic rig metadata"; }
		if (!own.rigs.containsAll(borrowed.rigs)) { return "Different classic frame-map layouts"; }
		return "Shared classic frame-map layout (model skin groups not verified)";
	}
	static boolean shared(String evidence) { return evidence.startsWith("Shared classic frame-map layout"); }

	private Sequence sequence(int id)
	{
		if (id < 0) { return Sequence.bad("No native sequence for rig comparison"); }
		Sequence entry = sequences.getIfPresent(id);
		if (entry == null)
		{
			// Current schema only. Old revisions have different sound/Maya opcode payloads.
			if (client.getRevision() < 226) { return Sequence.bad("Unsupported sequence schema/revision"); }
			byte[] bytes = load(2, 12, id); // CONFIGS / SEQUENCE
			if (bytes == null) { return Sequence.bad("Waiting for sequence #" + id); }
			entry = decode(bytes);
			sequences.put(id, entry);
		}
		if (entry.problem != null || entry.complete() || entry.lastAttempt == client.getTickCount()) { return entry; }
		entry.lastAttempt = client.getTickCount();
		while (entry.next < entry.frames.length)
		{
			int frame = entry.frames[entry.next];
			long key = frame & 0xffffffffL;
			Rig rig = frames.getIfPresent(key);
			if (rig == null)
			{
				byte[] bytes = load(0, frame >>> 16, frame & 65535); // ANIMATIONS
				if (bytes == null) { break; }
				if (bytes.length < 3) { entry.problem = "Malformed classic frame"; break; }
				int skeleton = (bytes[0] & 255) << 8 | bytes[1] & 255;
				rig = skeletons.getIfPresent(skeleton);
				if (rig == null)
				{
					byte[] map = load(1, skeleton, 0); // SKELETONS
					if (map == null) { break; }
					rig = Rig.decode(map);
					skeletons.put(skeleton, rig);
				}
				if (rig.problem == null && !validFrame(bytes, rig.types.length)) { rig = Rig.bad("Malformed classic frame"); }
				frames.put(key, rig);
			}
			if (rig.problem != null) { entry.problem = rig.problem; break; }
			entry.rigs.add(rig);
			entry.next++;
		}
		return entry;
	}

	private byte[] load(int index, int archive, int file)
	{
		int now = client.getTickCount();
		if (tick != now) { tick = now; loads = 0; }
		long key = ((long) index << 56) | ((long) archive << 24) | file;
		Integer missed = misses.getIfPresent(key);
		if (loads >= MAX_LOADS_PER_TICK || (missed != null && missed == now)) { return null; }
		loads++;
		try
		{
			IndexDataBase database = index == 2 ? client.getIndexConfig() : client.getIndex(index);
			byte[] bytes = database == null ? null : database.loadData(archive, file);
			if (bytes == null) { misses.put(key, now); }
			return bytes;
		}
		catch (RuntimeException ex) { misses.put(key, now); return null; }
	}

	static Sequence decode(byte[] bytes)
	{
		ByteBuffer data = ByteBuffer.wrap(bytes);
		int[] references = null;
		try
		{
			while (data.hasRemaining())
			{
				int opcode = u8(data);
				switch (opcode)
				{
					case 0: return references == null || references.length == 0 ? Sequence.bad("No classic sequence frames") : new Sequence(references);
					case 1:
						int count = u16(data);
						if (count > 4096) { return Sequence.bad("Oversized sequence metadata"); }
						skip(data, count * 2);
						references = new int[count];
						for (int i = 0; i < count; i++) { references[i] = u16(data); }
						for (int i = 0; i < count; i++) { references[i] |= u16(data) << 16; }
						break;
					case 2: case 6: case 7: skip(data, 2); break;
					case 3: skip(data, u8(data)); break;
					case 4: case 19: break;
					case 5: case 8: case 9: case 10: case 11: case 16: skip(data, 1); break;
					case 12: skip(data, u8(data) * 4); break;
					case 13: return Sequence.bad("Maya/weighted animation: automatic borrowing disabled");
					case 14: skip(data, u16(data) * 8); break; // frame ushort + modern sound's six bytes
					case 15: skip(data, 4); break;
					case 17: skip(data, u8(data)); break;
					case 18: while (data.get() != 0) { /* debug name */ } break;
					default: return Sequence.bad("Unsupported sequence opcode " + opcode);
				}
			}
		}
		catch (RuntimeException malformed) { return Sequence.bad("Malformed/truncated sequence metadata"); }
		return Sequence.bad("Missing sequence terminator");
	}

	private static boolean validFrame(byte[] bytes, int transforms)
	{
		try
		{
			int count = bytes[2] & 255;
			if (count > transforms || bytes.length < 3 + count) { return false; }
			ByteBuffer values = ByteBuffer.wrap(bytes); values.position(3 + count);
			for (int i = 0; i < count; i++)
			{
				int flags = bytes[3 + i] & 255;
				if ((flags & ~7) != 0) { return false; }
				for (int bit = 1; bit <= 4; bit <<= 1)
				{
					if ((flags & bit) != 0) { skip(values, (values.get(values.position()) & 128) == 0 ? 1 : 2); }
				}
			}
			return !values.hasRemaining();
		}
		catch (RuntimeException malformed) { return false; }
	}
	private static int u8(ByteBuffer data) { return data.get() & 255; }
	private static int u16(ByteBuffer data) { return data.getShort() & 65535; }
	private static void skip(ByteBuffer data, int count)
	{
		if (count > data.remaining()) { throw new IllegalArgumentException(); }
		data.position(data.position() + count);
	}
	void clear() { sequences.invalidateAll(); frames.invalidateAll(); skeletons.invalidateAll(); misses.invalidateAll(); tick = Integer.MIN_VALUE; }

	static final class Sequence
	{
		final int[] frames;
		final Set<Rig> rigs = new HashSet<>();
		String problem;
		int next, lastAttempt = Integer.MIN_VALUE;
		Sequence(int[] frames) { this.frames = frames; }
		static Sequence bad(String problem) { Sequence s = new Sequence(new int[0]); s.problem = problem; return s; }
		boolean complete() { return problem == null && next == frames.length && !rigs.isEmpty(); }
	}

	private static final class Rig
	{
		final int[] types;
		final int[][] labels;
		final String problem;
		Rig(int[] types, int[][] labels, String problem) { this.types = types; this.labels = labels; this.problem = problem; }
		static Rig bad(String problem) { return new Rig(new int[0], new int[0][], problem); }
		static Rig decode(byte[] bytes)
		{
			try
			{
				ByteBuffer data = ByteBuffer.wrap(bytes);
				int count = u8(data);
				int[] types = new int[count]; int[][] labels = new int[count][];
				for (int i = 0; i < count; i++)
				{
					types[i] = u8(data);
					if (types[i] > 3 && types[i] != 5) { return bad("Unsupported skeleton transform type"); }
				}
				int totalLabels = 0;
				for (int i = 0; i < count; i++)
				{
					int length = u8(data); totalLabels += length;
					if (totalLabels > 4096) { return bad("Oversized skeleton metadata"); }
					labels[i] = new int[length];
				}
				boolean geometry = false;
				for (int i = 0; i < count; i++)
				{
					for (int j = 0; j < labels[i].length; j++) { labels[i][j] = u8(data); }
					geometry |= types[i] >= 1 && types[i] <= 3 && labels[i].length > 0;
				}
				if (data.hasRemaining() && (data.remaining() != 2 || u16(data) != 0)) { return bad("Extended/weighted skeleton: automatic borrowing disabled"); }
				if (!geometry) { return bad("No geometric transforms in frame map"); }
				return new Rig(types, labels, null);
			}
			catch (RuntimeException malformed) { return bad("Malformed/truncated skeleton metadata"); }
		}
		@Override public int hashCode() { return 31 * Arrays.hashCode(types) + Arrays.deepHashCode(labels); }
		@Override public boolean equals(Object other)
		{
			if (!(other instanceof Rig)) { return false; }
			Rig rig = (Rig) other;
			return Arrays.equals(types, rig.types) && Arrays.deepEquals(labels, rig.labels);
		}
	}
}
