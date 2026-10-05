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

	/** Exercise the same preparation-before-render ordering as the live disguise module. */
	private Model render()
	{
		((NativeNpcAnimation) object.getAnimationController()).prepare();
		return object.getModel();
	}

	private NativeNpcAnimation configured(NpcAnimationMode mode)
	{
		NativeNpcAnimation controller = new NativeNpcAnimation(client, brother, 77777, new AnimationRigCache(client));
		controller.options(mode);
		object.setAnimationController(controller);
		return controller;
	}

	private void matchingIdleRigAndDifferentWalkRig()
	{
		IndexDataBase frames = mock(IndexDataBase.class), maps = mock(IndexDataBase.class);
		when(client.getRevision()).thenReturn(240);
		when(client.getIndex(0)).thenReturn(frames); when(client.getIndex(1)).thenReturn(maps);
		when(configs.loadData(12, 100)).thenReturn(AnimationRigCacheTest.sequence(10, 1));
		when(configs.loadData(12, 101)).thenReturn(AnimationRigCacheTest.sequence(30, 1));
		when(configs.loadData(12, 999)).thenReturn(AnimationRigCacheTest.sequence(20, 1));
		when(frames.loadData(10, 0)).thenReturn(new byte[] {0, 7, 1, 1, 64});
		when(frames.loadData(20, 0)).thenReturn(new byte[] {0, 7, 1, 1, 64});
		when(frames.loadData(30, 0)).thenReturn(new byte[] {0, 8, 1, 1, 64});
		when(maps.loadData(7, 0)).thenReturn(new byte[] {1, 1, 1, 0});
		when(maps.loadData(8, 0)).thenReturn(new byte[] {1, 1, 1, 3});
		Animation action = animation(999);
		when(client.loadAnimation(999)).thenReturn(action);
	}

	@Test
	public void playsSelectedNpcsIdleAndDoesNotBorrowBrothersAttack()
	{
		assertSame(animated, render());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		object.tick(3);
		assertSame(animated, render());
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
		assertSame(actionModel, render());
		when(brother.getAnimationFrame()).thenReturn(0);
		when(client.applyTransformations(base, action, 0, null, 0)).thenReturn(actionModel);
		assertSame(actionModel, render());
		verify(client).applyTransformations(base, action, 0, null, 0);
		when(brother.getAnimation()).thenReturn(-1);
		assertSame(animated, render());
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
		assertSame(animated, render()); assertSame(animated, render());
		verify(client).applyTransformations(base, action, 0, null, 0);
	}

	@Test
	public void unavailableBrotherActionsRetryOncePerTickAndKeepNativeGait()
	{
		object.setAnimationController(new NativeNpcAnimation(client, brother, 77777, true));
		assertSame(animated, render()); assertSame(animated, render());
		verify(client).loadAnimation(999);
		when(client.getTickCount()).thenReturn(1);
		Animation action = animation(999);
		when(client.loadAnimation(999)).thenReturn(action);
		assertSame(animated, render());
		verify(client, times(2)).loadAnimation(999);
		verify(client).applyTransformations(base, action, 0, null, 0);
	}

	@Test
	public void autoRequiresRigEvidenceEvenWhenGaitIdsMatchAndNeverDiscoversAssetsDuringRendering()
	{
		when(brother.getIdlePoseAnimation()).thenReturn(100);
		when(brother.getWalkAnimation()).thenReturn(101);
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO);
		controller.prepare();
		assertSame(animated, object.getModel());
		verify(client, never()).loadAnimation(999);
		assertTrue(controller.summary().contains("matching idle/walk IDs (hint only)"));
		when(client.getRevision()).thenReturn(240);
		when(client.getTickCount()).thenReturn(1);
		controller.prepare();
		clearInvocations(configs, client);
		object.getModel(); object.getModel();
		verify(configs, never()).loadData(anyInt(), anyInt());
		verify(client, never()).loadAnimation(anyInt());
		verify(client, never()).getIndex(anyInt());
		verify(client, never()).getIndexConfig();
	}

	@Test
	public void autoBorrowsOnlyAfterAllClassicFrameMapsMatchAndNativeModeDisablesBorrowing()
	{
		matchingIdleRigAndDifferentWalkRig();
		Animation action = client.loadAnimation(999);
		clearInvocations(client);
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO);
		assertSame(animated, render());
		verify(client).applyTransformations(base, action, 0, null, 0);
		assertTrue(controller.summary().contains("Shared classic frame-map layout"));
		controller.options(NpcAnimationMode.NATIVE); controller.prepare();
		clearInvocations(client);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		verify(client, never()).applyTransformations(eq(base), eq(action), anyInt(), isNull(), eq(0));
	}

	@Test
	public void forceCanBorrowWithoutEvidenceButAutoAndNativeKeepNativeGait()
	{
		Animation action = animation(999); when(client.loadAnimation(999)).thenReturn(action);
		NativeNpcAnimation controller = configured(NpcAnimationMode.FORCE);
		assertSame(animated, render());
		verify(client).applyTransformations(base, action, 0, null, 0);
		for (NpcAnimationMode mode : new NpcAnimationMode[] {NpcAnimationMode.AUTO, NpcAnimationMode.NATIVE})
		{
			controller.options(mode); controller.prepare();
			clearInvocations(client); object.getModel();
			verify(client).applyTransformations(base, idle, 0, null, 0);
			verify(client, never()).applyTransformations(eq(base), eq(action), anyInt(), isNull(), eq(0));
		}
	}

	@Test
	public void formerlyMappedAnimalKeepsNativeGaitWithoutRigEvidence()
	{
		when(brother.getId()).thenReturn(net.runelite.api.gameval.NpcID.BARROWS_KARIL);
		when(brother.getAnimation()).thenReturn(net.runelite.api.gameval.AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE);
		int cow = net.runelite.api.gameval.NpcID.COW;
		when(configs.loadData(9, cow)).thenReturn(new byte[] {13, 0, 100, 14, 0, 101, 0});
		NativeNpcAnimation controller = new NativeNpcAnimation(client, brother, cow, new AnimationRigCache(client));
		controller.options(NpcAnimationMode.AUTO); controller.prepare(); object.setAnimationController(controller);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		verify(client, never()).loadAnimation(net.runelite.api.gameval.AnimationID.COW_UPDATE_ATTACK);
		verify(client, never()).loadAnimation(net.runelite.api.gameval.AnimationID.BARROWS_REPEATING_CROSSBOW_FIRE);
	}

	@Test
	public void switchesBetweenNativeWalkAndIdleAndLoopsWithoutDespawning()
	{
		render();
		when(brother.getPoseAnimation()).thenReturn(10);
		assertSame(animated, render());
		verify(client).applyTransformations(base, walk, 0, null, 0);
		object.tick(100);
		assertSame(animated, render());
		when(brother.getPoseAnimation()).thenReturn(0);
		assertSame(animated, render());
		verify(client).loadAnimation(100);
		verify(client, never()).removeRuneLiteObject(any());
	}

	@Test
	public void missingMetadataRetriesAtMostOncePerGameTick()
	{
		when(configs.loadData(9, 77777)).thenReturn(null);
		assertSame(base, render()); assertSame(base, render());
		verify(configs).loadData(9, 77777);
		when(client.getTickCount()).thenReturn(1);
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {13, 0, 100, 0});
		assertSame(animated, render());
		verify(configs, times(2)).loadData(9, 77777);
	}

	@Test
	public void missingAnimationRetriesWithoutRerollingOrHidingTheModel()
	{
		when(client.loadAnimation(100)).thenReturn(null);
		assertSame(base, render()); assertSame(base, render());
		verify(client).loadAnimation(100);
		when(client.getTickCount()).thenReturn(1);
		when(client.loadAnimation(100)).thenReturn(idle);
		assertSame(animated, render());
		verify(client, times(2)).loadAnimation(100);
	}

	@Test
	public void incompatibleIdleFallsBackButNativeWalkCanStillPlay()
	{
		when(client.applyTransformations(base, idle, 0, null, 0)).thenThrow(new IllegalArgumentException("rig"));
		assertSame(base, render()); assertSame(base, render());
		verify(client).applyTransformations(base, idle, 0, null, 0);
		when(brother.getPoseAnimation()).thenReturn(10);
		assertSame(animated, render());
	}

	@Test
	public void diagnosticsDistinguishWaitingMetadataMissingGaitsAndUnsupportedTags()
	{
		NativeNpcAnimation controller = new NativeNpcAnimation(client, brother, 77777);
		when(configs.loadData(9, 77777)).thenReturn(null);
		controller.prepare(); controller.animate(base, null);
		assertTrue(controller.summary().contains("waiting for NPC definition bytes"));
		when(client.getTickCount()).thenReturn(1);
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {(byte) 200, 13, 0, 100, 0});
		controller.prepare(); controller.animate(base, null);
		assertTrue(controller.summary().contains("no matching native gait sequence"));
		assertTrue(controller.summary().contains("unsupported definition opcode 200"));
	}

	@Test
	public void malformedMetadataLeavesAStaticModel()
	{
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {13, 0});
		assertSame(base, render());
		verify(client, never()).loadAnimation(anyInt());
	}

	@Test
	public void mayaIdleUsesNativeTickPlayback()
	{
		when(idle.isMayaAnim()).thenReturn(true);
		when(idle.getFrameLengths()).thenReturn(null);
		object.tick(1);
		assertSame(animated, render());
		verify(client).applyTransformations(base, idle, 1, null, 0);
	}

	@Test
	public void consumesSharedAnimatedResultsWithoutCachingThem()
	{
		Model second = mock(Model.class);
		when(client.applyTransformations(base, idle, 0, null, 0)).thenReturn(animated, second);
		assertSame(animated, render());
		assertSame(second, render());
	}

	@Test
	public void renderingBeforePreparationDoesNotDiscoverAssets()
	{
		assertSame(base, object.getModel()); assertSame(base, object.getModel());
		verify(configs, never()).loadData(anyInt(), anyInt());
		verify(client, never()).getIndexConfig();
		verify(client, never()).loadAnimation(anyInt());
		assertSame(animated, render());
	}

	@Test
	public void renderingDoesNotRetryMissingMetadataOrAnimationsEvenOnANewGameTick()
	{
		NativeNpcAnimation controller = configured(NpcAnimationMode.FORCE);
		when(configs.loadData(9, 77777)).thenReturn(null);
		controller.prepare();
		when(client.getTickCount()).thenReturn(1);
		when(configs.loadData(9, 77777)).thenReturn(new byte[] {13, 0, 100, 0});
		when(client.loadAnimation(100)).thenReturn(null);
		clearInvocations(configs, client);
		assertSame(base, object.getModel());
		verify(configs, never()).loadData(anyInt(), anyInt());
		verify(client, never()).loadAnimation(anyInt());
		controller.prepare();
		verify(configs).loadData(9, 77777);
		verify(client).loadAnimation(100); verify(client).loadAnimation(999);
		when(client.getTickCount()).thenReturn(2);
		clearInvocations(configs, client);
		object.getModel(); object.getModel();
		verify(configs, never()).loadData(anyInt(), anyInt());
		verify(client, never()).loadAnimation(anyInt());
		controller.prepare();
		verify(client).loadAnimation(100); verify(client).loadAnimation(999);
	}

	@Test
	public void autoEvidenceTracksGaitChangesWithinTheSameGameTick()
	{
		matchingIdleRigAndDifferentWalkRig();
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO);
		controller.prepare(); object.getModel();
		verify(client).applyTransformations(eq(base), argThat(a -> a.getId() == 999), eq(0), isNull(), eq(0));
		when(brother.getPoseAnimation()).thenReturn(10);
		clearInvocations(client, configs);
		assertSame(base, object.getModel());
		verify(client, never()).loadAnimation(anyInt());
		verify(client, never()).applyTransformations(any(), any(), anyInt(), any(), anyInt());
		controller.prepare();
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(base, walk, 0, null, 0);
		assertTrue(controller.summary().contains("Different classic frame-map layouts"));
		when(brother.getPoseAnimation()).thenReturn(0);
		assertSame(base, object.getModel());
		controller.prepare(); clearInvocations(client);
		assertSame(animated, object.getModel());
		verify(client).applyTransformations(eq(base), argThat(a -> a.getId() == 999), eq(0), isNull(), eq(0));
	}

	@Test
	public void actionNotificationInvalidatesEvidenceUntilPreparation()
	{
		matchingIdleRigAndDifferentWalkRig();
		NativeNpcAnimation controller = configured(NpcAnimationMode.AUTO);
		controller.prepare(); object.getModel();
		controller.actionChanged(); clearInvocations(client);
		object.getModel();
		verify(client).applyTransformations(base, idle, 0, null, 0);
		verify(client, never()).loadAnimation(anyInt());
		controller.prepare(); clearInvocations(client); object.getModel();
		verify(client).applyTransformations(eq(base), argThat(a -> a.getId() == 999), eq(0), isNull(), eq(0));
	}

	@Test
	public void changedBrotherActionWaitsForTickPreparation()
	{
		Animation first = animation(999), second = animation(998);
		when(client.loadAnimation(999)).thenReturn(first);
		NativeNpcAnimation controller = configured(NpcAnimationMode.FORCE);
		controller.prepare(); object.getModel();
		when(brother.getAnimation()).thenReturn(998);
		when(client.loadAnimation(998)).thenReturn(second);
		clearInvocations(client);
		object.getModel();
		verify(client).applyTransformations(base, idle, 0, null, 0);
		verify(client, never()).loadAnimation(998);
		controller.prepare(); object.getModel();
		verify(client).loadAnimation(998);
		verify(client).applyTransformations(eq(base), argThat(a -> a.getId() == 998), eq(0), isNull(), eq(0));
	}

	@Test
	public void optionChangesPreserveTheNativeGaitClock()
	{
		NativeNpcAnimation controller = configured(NpcAnimationMode.NATIVE);
		controller.prepare(); object.tick(3);
		for (NpcAnimationMode mode : NpcAnimationMode.values())
		{
			controller.options(mode); controller.prepare();
			clearInvocations(client); object.getModel();
			verify(client).applyTransformations(base, idle, 1, null, 0);
		}
		verify(client, never()).loadAnimation(100);
	}

	@Test
	public void switchingGaitsDoesNotRetryAMissingSequenceWithinTheSameTick()
	{
		when(client.loadAnimation(100)).thenReturn(null);
		NativeNpcAnimation controller = configured(NpcAnimationMode.NATIVE);
		controller.prepare();
		when(brother.getPoseAnimation()).thenReturn(10); controller.prepare();
		when(brother.getPoseAnimation()).thenReturn(0); controller.prepare(); controller.prepare();
		verify(client).loadAnimation(100);
		when(client.getTickCount()).thenReturn(1); controller.prepare();
		verify(client, times(2)).loadAnimation(100);
	}

	@Test
	public void switchingActionsDoesNotRetryMissingActionsWithinTheSameTick()
	{
		NativeNpcAnimation controller = configured(NpcAnimationMode.FORCE);
		controller.prepare();
		when(brother.getAnimation()).thenReturn(998); controller.prepare();
		when(brother.getAnimation()).thenReturn(999); controller.prepare(); controller.prepare();
		verify(client).loadAnimation(999); verify(client).loadAnimation(998);
		when(client.getTickCount()).thenReturn(1); controller.prepare();
		verify(client, times(2)).loadAnimation(999);
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
		assertEquals(100, data.sequenceFor(brother));
		assertEquals(-1, NativeNpcAnimations.decode(new byte[] {13, (byte) 255, (byte) 255, 0}).sequenceFor(brother));
	}
}
