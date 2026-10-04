package com.barrowsbrothersrandomizer;

import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObject;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NativeNpcAnimationTest
{
	private Client client;
	private IndexDataBase configs;
	private NPC brother;
	private Model base, animated;
	private Animation idle, walk;
	private RuneLiteObject object;

	@Before
	public void setup()
	{
		client = mock(Client.class);
		configs = mock(IndexDataBase.class);
		when(client.getIndexConfig()).thenReturn(configs);
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {13, 0, 100, 14, 0, 101, 0});
		brother = mock(NPC.class);
		when(brother.getIdlePoseAnimation()).thenReturn(0);
		when(brother.getWalkAnimation()).thenReturn(10);
		when(brother.getAnimation()).thenReturn(999);
		base = mock(Model.class); animated = mock(Model.class);
		idle = animation(100); walk = animation(101);
		when(client.loadAnimation(100)).thenReturn(idle);
		when(client.loadAnimation(101)).thenReturn(walk);
		when(client.applyTransformations(eq(base), any(Animation.class), anyInt(), isNull(), eq(0))).thenReturn(animated);
		object = new RuneLiteObject(client);
		object.setModel(base);
		object.setAnimationController(new NativeNpcAnimation(client, brother, 77777));
	}

	private Animation animation(int id)
	{
		Animation animation = mock(Animation.class);
		when(animation.getId()).thenReturn(id);
		when(animation.getNumFrames()).thenReturn(3);
		when(animation.getDuration()).thenReturn(3);
		when(animation.getFrameStep()).thenReturn(3);
		when(animation.getFrameLengths()).thenReturn(new int[] {2, 2, 2});
		return animation;
	}

	@Test
	public void playsSelectedNpcsIdleAndDoesNotBorrowBrothersAttack()
	{
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		object.tick(3);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 1, null, 0);
		verify(client, never()).loadAnimation(999);
		verify(brother, never()).setAnimation(anyInt());
		verify(brother, never()).setPoseAnimation(anyInt());
		verify(brother, never()).getModel();
		assertSame(base, object.getBaseModel());
	}

	@Test
	public void optionalBrotherActionsUseRealActionFramesThenResumeNativeIdle()
	{
		Animation action = animation(999);
		Model actionModel = mock(Model.class);
		when(client.loadAnimation(999)).thenReturn(action);
		when(brother.getAnimationFrame()).thenReturn(2);
		when(client.applyTransformations(base, action, 2, null, 0)).thenReturn(actionModel);
		object.setAnimationController(new NativeNpcAnimation(client, brother, 77777, true));
		assertSame(actionModel, object.getModel());
		when(brother.getAnimationFrame()).thenReturn(0);
		when(client.applyTransformations(base, action, 0, null, 0)).thenReturn(actionModel);
		assertSame(actionModel, object.getModel());
		verify(client).applyTransformations(base, action, 0, null, 0);
		when(brother.getAnimation()).thenReturn(-1);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		verify(brother, never()).setAnimation(anyInt());
	}

	@Test
	public void incompatibleBrotherActionsRetainNativeAnimationWithoutRepeatedExceptions()
	{
		Animation action = animation(999);
		when(client.loadAnimation(999)).thenReturn(action);
		when(client.applyTransformations(base, action, 0, null, 0)).thenThrow(new IllegalArgumentException("rig"));
		object.setAnimationController(new NativeNpcAnimation(client, brother, 77777, true));
		assertSame(animated, object.getModel()); assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, action, 0, null, 0);
	}

	@Test
	public void unavailableBrotherActionsRetryOncePerTickAndKeepNativeGait()
	{
		object.setAnimationController(new NativeNpcAnimation(client, brother, 77777, true));
		assertSame(animated, object.getModel()); assertSame(animated, object.getModel());
		verify(client).loadAnimation(999);
		when(client.getTickCount()).thenReturn(1);
		Animation action = animation(999);
		when(client.loadAnimation(999)).thenReturn(action);
		assertSame(animated, object.getModel());
		verify(client, times(2)).loadAnimation(999);
		verify(client).applyTransformations(base, action, 0, null, 0);
	}

	private NativeNpcAnimation configured(NpcAnimationMode mode, boolean attacks, String overrides)
	{
		NativeNpcAnimation controller = new NativeNpcAnimation(client, brother, 77777, new AnimationRigCache(client));
		controller.options(mode, attacks, new NpcActionOverrides(overrides));
		object.setAnimationController(controller);
		return controller;
	}

	@Test
	public void autoRequiresRigEvidenceEvenWhenGaitIdsMatchAndNeverDecodesRigsDuringRendering()
	{
		when(brother.getIdlePoseAnimation()).thenReturn(100);
		when(brother.getWalkAnimation()).thenReturn(101);
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO, false, "");
		controller.prepare();
		assertSame(animated, object.getModel());
		verify(client, never()).loadAnimation(999);
		assertTrue(controller.summary().contains("matching idle/walk IDs (hint only)"));
		when(client.getRevision()).thenReturn(240);
		when(client.getTickCount()).thenReturn(1);
		controller.prepare();
		clearInvocations(configs);
		object.getModel(); object.getModel();
		verify(configs, never()).loadData(eq(12), anyInt());
		verify(client, never()).getIndex(anyInt());
	}

	@Test
	public void autoBorrowsOnlyAfterAllClassicFrameMapsMatchAndDenyCanOverrideThatEvidence()
	{
		IndexDataBase frames = mock(IndexDataBase.class), maps = mock(IndexDataBase.class);
		when(client.getRevision()).thenReturn(240);
		when(client.getIndex(0)).thenReturn(frames); when(client.getIndex(1)).thenReturn(maps);
		when(configs.loadData(12, 100)).thenReturn(AnimationRigCacheTest.sequence(10, 1));
		when(configs.loadData(12, 999)).thenReturn(AnimationRigCacheTest.sequence(20, 1));
		when(frames.loadData(anyInt(), anyInt())).thenReturn(new byte[] {0, 7, 1, 1, 64});
		when(maps.loadData(7, 0)).thenReturn(new byte[] {1, 1, 1, 0});
		Animation action = animation(999);
		when(client.loadAnimation(999)).thenReturn(action);
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO, false, "");
		controller.prepare();
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, action, 0, null, 0);
		assertTrue(controller.summary().contains("Shared classic frame-map layout"));
		controller.options(NpcAnimationMode.AUTO, false, new NpcActionOverrides("77777:999=deny")); controller.prepare();
		clearInvocations(client);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		verify(client, never()).applyTransformations(eq(base), eq(action), anyInt(), isNull(), eq(0));
	}

	@Test
	public void reviewedAllowCanEnableAutoButNativeOnlyAndForceDenyStillRespectPolicy()
	{
		Animation action = animation(999); when(client.loadAnimation(999)).thenReturn(action);
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO, false, "77777:999=allow"); controller.prepare();
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, action, 0, null, 0);
		controller.options(NpcAnimationMode.NATIVE, false, new NpcActionOverrides("77777:999=allow"));
		clearInvocations(client); object.getModel();
		verify(client, never()).applyTransformations(eq(base), eq(action), anyInt(), isNull(), eq(0));
		controller.options(NpcAnimationMode.FORCE, false, new NpcActionOverrides("77777:*=deny"));
		object.getModel(); verify(client, never()).applyTransformations(eq(base), eq(action), anyInt(), isNull(), eq(0));
	}

	@Test
	public void explicitNativeAttackUsesItsOwnClockFinishesOnceAndRestartsOnTheNextBrotherCycle()
	{
		Animation nativeAttack = animation(500); when(client.loadAnimation(500)).thenReturn(nativeAttack);
		when(brother.getAnimationFrame()).thenReturn(2);
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO, true, "77777:999=500");
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, nativeAttack, 0, null, 0);
		object.tick(3); assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, nativeAttack, 1, null, 0);
		object.tick(100); clearInvocations(client); object.getModel();
		verify(client).applyTransformations(base, idle, 0, null, 0);
		assertTrue(controller.summary().contains("awaiting next attack cycle"));
		when(brother.getAnimationFrame()).thenReturn(0); object.getModel();
		verify(client).applyTransformations(base, nativeAttack, 0, null, 0);
		when(brother.getAnimation()).thenReturn(-1); object.getModel();
		verify(client, never()).loadAnimation(999);
	}

	@Test
	public void sourceBackedAnimalAttackWinsOverBorrowingInAutoAndFailureKeepsTheNativeGait()
	{
		when(brother.getId()).thenReturn(net.runelite.api.gameval.NpcID.BARROWS_KARIL);
		when(brother.getAnimation()).thenReturn(net.runelite.api.gameval.AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE);
		int cow = net.runelite.api.gameval.NpcID.COW;
		when(configs.loadData(9, cow)).thenReturn(NativeAttackMappingsTest.metadata(
			net.runelite.api.gameval.AnimationID.COW_JUST_READY_UPDATE, net.runelite.api.gameval.AnimationID.COW_UPDATE_WALK));
		Animation cowIdle = animation(net.runelite.api.gameval.AnimationID.COW_JUST_READY_UPDATE);
		Animation cowAttack = animation(net.runelite.api.gameval.AnimationID.COW_UPDATE_ATTACK);
		when(client.loadAnimation(cowIdle.getId())).thenReturn(cowIdle);
		when(client.loadAnimation(cowAttack.getId())).thenReturn(cowAttack);
		NativeNpcAnimation controller = new NativeNpcAnimation(client, brother, cow, new AnimationRigCache(client));
		controller.options(NpcAnimationMode.AUTO, true, new NpcActionOverrides("")); object.setAnimationController(controller);
		assertSame(animated, object.getModel()); verify(client).applyTransformations(base, cowAttack, 0, null, 0);
		when(client.applyTransformations(base, cowAttack, 0, null, 0)).thenThrow(new IllegalArgumentException("rig"));
		assertSame(animated, object.getModel()); assertSame(animated, object.getModel());
		verify(client, times(2)).applyTransformations(base, cowAttack, 0, null, 0);
		verify(client, never()).loadAnimation(net.runelite.api.gameval.AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE);
		verify(client, atLeastOnce()).applyTransformations(base, cowIdle, 0, null, 0);
	}

	@Test
	public void switchesBetweenNativeWalkAndIdleAndLoopsWithoutDespawning()
	{
		object.getModel();
		when(brother.getPoseAnimation()).thenReturn(10);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, walk, 0, null, 0);
		object.tick(100);
		assertSame(animated, object.getModel());
		when(brother.getPoseAnimation()).thenReturn(0);
		assertSame(animated, object.getModel());
		verify(client).loadAnimation(100); // Successful animations are reused after gait switches.
		verify(client, never()).removeRuneLiteObject(any());
	}

	@Test
	public void missingMetadataRetriesAtMostOncePerGameTick()
	{
		when(configs.loadData(9, 77777)).thenReturn(null);
		assertSame(base, object.getModel()); assertSame(base, object.getModel());
		verify(configs).loadData(9, 77777);
		when(client.getTickCount()).thenReturn(1);
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {13, 0, 100, 0});
		assertSame(animated, object.getModel());
		verify(configs, times(2)).loadData(9, 77777);
	}

	@Test
	public void missingAnimationRetriesWithoutRerollingOrHidingTheModel()
	{
		when(client.loadAnimation(100)).thenReturn(null);
		assertSame(base, object.getModel()); assertSame(base, object.getModel());
		verify(client).loadAnimation(100);
		when(client.getTickCount()).thenReturn(1);
		when(client.loadAnimation(100)).thenReturn(idle);
		assertSame(animated, object.getModel());
		verify(client, times(2)).loadAnimation(100);
	}

	@Test
	public void incompatibleIdleFallsBackButNativeWalkCanStillPlay()
	{
		when(client.applyTransformations(base, idle, 0, null, 0)).thenThrow(new IllegalArgumentException("rig"));
		assertSame(base, object.getModel()); assertSame(base, object.getModel());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		when(brother.getPoseAnimation()).thenReturn(10);
		assertSame(animated, object.getModel());
	}

	@Test
	public void diagnosticsDistinguishWaitingMetadataMissingGaitsAndUnsupportedTags()
	{
		NativeNpcAnimation controller = new NativeNpcAnimation(client, brother, 77777);
		when(configs.loadData(9, 77777)).thenReturn(null);
		controller.animate(base, null);
		assertTrue(controller.summary().contains("waiting for NPC definition bytes"));
		when(client.getTickCount()).thenReturn(1);
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {(byte) 200, 13, 0, 100, 0});
		controller.animate(base, null);
		assertTrue(controller.summary().contains("no matching native gait sequence"));
		assertTrue(controller.summary().contains("unsupported definition opcode 200"));
	}

	@Test
	public void malformedMetadataLeavesAStaticModel()
	{
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {13, 0});
		assertSame(base, object.getModel());
		verify(client, never()).loadAnimation(anyInt());
	}

	@Test
	public void mayaIdleUsesNativeTickPlayback()
	{
		when(idle.isMayaAnim()).thenReturn(true);
		when(idle.getFrameLengths()).thenReturn(null);
		object.tick(1);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 1, null, 0);
	}

	@Test
	public void consumesSharedAnimatedResultsWithoutCachingThem()
	{
		Model second = mock(Model.class);
		when(client.applyTransformations(base, idle, 0, null, 0)).thenReturn(animated, second);
		assertSame(animated, object.getModel());
		assertSame(second, object.getModel());
	}

	@Test
	public void actionEventsRestartEvenWhenTheBrotherActionIdAndFrameAreUnchanged()
	{
		Animation action = animation(500); when(client.loadAnimation(500)).thenReturn(action);
		NativeNpcAnimation controller = configured(NpcAnimationMode.NATIVE, true, "77777:999=500");
		object.getModel(); object.tick(100); object.getModel();
		controller.actionChanged(); clearInvocations(client); object.getModel();
		verify(client).applyTransformations(base, action, 0, null, 0);
	}

	@Test
	public void crawlerOnlyDefinitionsCanMoveAndAllDirectionalMetadataIsDecoded()
	{
		NativeNpcAnimations data = NativeNpcAnimations.decode(new byte[] {13, 0, 100,
			115, 0, 106, 0, 107, 0, 108, 0, 109, 117, 0, 110, 0, 111, 0, 112, 0, 113, 0});
		assertTrue(data.complete());
		assertArrayEquals(new int[] {100, -1, -1, -1, -1, -1, -1, 106, 107, 108, 109, 110, 111, 112, 113}, data.movement());
		when(brother.getPoseAnimation()).thenReturn(10);
		assertEquals(110, data.sequenceFor(brother));
		when(brother.getWalkRotateLeft()).thenReturn(10);
		assertEquals(112, data.sequenceFor(brother));
	}

	@Test
	public void decoderSkipsKnownPayloadsAndReadsLateRunAndDirectionalSequences()
	{
		NativeNpcAnimations data = NativeNpcAnimations.decode(new byte[] {
			1, 1, 0, 42, 2, 'x', 0, 13, 0, 100, 40, 1, 0, 1, 0, 2,
			17, 0, 101, 0, 103, 0, 104, 0, 105,
			102, 1, 0, 1, 1, 114, 0, 106, 0});
		when(brother.getPoseAnimation()).thenReturn(20);
		when(brother.getRunAnimation()).thenReturn(20);
		assertEquals(106, data.sequenceFor(brother));
		when(brother.getPoseAnimation()).thenReturn(30);
		when(brother.getWalkRotateLeft()).thenReturn(30);
		assertEquals(104, data.sequenceFor(brother));
	}

	@Test
	public void unknownOpcodesStopParsingRatherThanTreatingPayloadAsAnimationTags()
	{
		NativeNpcAnimations data = NativeNpcAnimations.decode(new byte[] {13, 0, 100, (byte) 200, 14, 0, 101, 0});
		assertEquals(100, data.sequenceFor(brother));
		when(brother.getPoseAnimation()).thenReturn(10);
		assertEquals(100, data.sequenceFor(brother)); // Unknown walk safely uses the known idle.
		assertEquals(-1, NativeNpcAnimations.decode(new byte[] {13, (byte) 255, (byte) 255, 0}).sequenceFor(brother));
	}
}
