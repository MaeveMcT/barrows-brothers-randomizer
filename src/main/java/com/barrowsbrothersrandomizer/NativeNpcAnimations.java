package com.barrowsbrothersrandomizer;

import java.nio.ByteBuffer;
import net.runelite.api.NPC;

/** Best-effort sequence metadata from config archive 9; no meshes or cache files are opened. */
final class NativeNpcAnimations
{
	private static final NativeNpcAnimations EMPTY = new NativeNpcAnimations();
	private boolean complete;
	boolean complete() { return complete; }
	private NativeNpcAnimations() { }
	private int idle = -1, walk = -1, back = -1, left = -1, right = -1, turnLeft = -1, turnRight = -1, run = -1;
	private int runBack = -1, runLeft = -1, runRight = -1, crawl = -1, crawlBack = -1, crawlLeft = -1, crawlRight = -1;

	static NativeNpcAnimations decode(byte[] bytes)
	{
		NativeNpcAnimations result = new NativeNpcAnimations();
		ByteBuffer data = ByteBuffer.wrap(bytes);
		try
		{
			while (data.hasRemaining())
			{
				int opcode = u8(data);
				switch (opcode)
				{
					case 0: result.complete = true; return result;
					case 1: case 60: skip(data, u8(data) * 2); break;
					case 61: case 62: skip(data, u8(data) * 4); break;
					case 2: case 30: case 31: case 32: case 33: case 34: string(data); break;
					case 12: case 100: case 101: skip(data, 1); break;
					case 13: result.idle = sequence(data); break;
					case 14: result.walk = sequence(data); break;
					case 15: result.turnLeft = sequence(data); break;
					case 16: result.turnRight = sequence(data); break;
					case 17:
						result.walk = sequence(data); result.back = sequence(data);
						result.left = sequence(data); result.right = sequence(data); break;
					case 18: case 74: case 75: case 76: case 77: case 78: case 79:
					case 95: case 97: case 98: case 103: case 124: case 126: case 146: skip(data, 2); break;
					case 40: case 41: skip(data, u8(data) * 4); break;
					case 93: case 99: case 107: case 109: case 111: case 122: case 123:
					case 129: case 130: case 145: case 147: break;
					case 102:
						int mask = u8(data);
						for (int bit = 0; bit < 8; bit++)
						{
							if ((mask & 1 << bit) == 0) { continue; }
							skip(data, (data.get(data.position()) & 128) != 0 ? 4 : 2);
							skip(data, (data.get(data.position()) & 128) != 0 ? 2 : 1);
						}
						break;
					case 106: case 118:
						skip(data, opcode == 118 ? 6 : 4);
						skip(data, (u8(data) + 1) * 2); break;
					case 114: result.run = sequence(data); break;
					case 115:
						result.run = sequence(data); result.runBack = sequence(data);
						result.runLeft = sequence(data); result.runRight = sequence(data); break;
					case 116: result.crawl = sequence(data); break;
					case 117:
						result.crawl = sequence(data); result.crawlBack = sequence(data);
						result.crawlLeft = sequence(data); result.crawlRight = sequence(data); break;
					case 249:
						int count = u8(data);
						for (int i = 0; i < count; i++)
						{
							boolean text = u8(data) == 1;
							skip(data, 3);
							if (text) { string(data); } else { skip(data, 4); }
						}
						break;
					case 251: skip(data, 2); string(data); break;
					case 252: skip(data, 13); string(data); break;
					case 253: skip(data, 15); string(data); break;
					// Never scan unknown payload bytes as opcodes. Keep only the known prefix.
					default:
						return result;
				}
			}
		}
		catch (RuntimeException malformed) { return EMPTY; }
		return EMPTY; // Missing terminator/truncated metadata is not trusted.
	}

	int sequenceFor(NPC brother)
	{
		int pose = brother.getPoseAnimation();
		if (pose < 0 || pose == brother.getIdlePoseAnimation()) { return idle; }
		int moving = fallback(walk, fallback(crawl, fallback(run, idle)));
		if (pose == brother.getRunAnimation()) { return fallback(run, moving); }
		if (pose == brother.getIdleRotateLeft()) { return fallback(turnLeft, idle); }
		if (pose == brother.getIdleRotateRight()) { return fallback(turnRight, idle); }
		if (pose == brother.getWalkRotate180()) { return fallback(back, fallback(crawlBack, fallback(runBack, moving))); }
		if (pose == brother.getWalkRotateLeft()) { return fallback(left, fallback(crawlLeft, fallback(runLeft, moving))); }
		if (pose == brother.getWalkRotateRight()) { return fallback(right, fallback(crawlRight, fallback(runRight, moving))); }
		return moving;
	}

	private static int fallback(int preferred, int backup) { return preferred >= 0 ? preferred : backup; }
	private static int u8(ByteBuffer data) { return data.get() & 255; }
	private static int sequence(ByteBuffer data) { int id = data.getShort() & 65535; return id == 65535 ? -1 : id; }
	private static void string(ByteBuffer data) { while (data.get() != 0) { /* Skip a cache string. */ } }
	private static void skip(ByteBuffer data, int count)
	{
		if (count > data.remaining()) { throw new IllegalArgumentException("Truncated NPC metadata"); }
		data.position(data.position() + count);
	}
}
