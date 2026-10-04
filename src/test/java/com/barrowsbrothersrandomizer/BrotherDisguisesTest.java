package com.barrowsbrothersrandomizer;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.IndexDataBase;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.NpcID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BrotherDisguisesTest
{
	private Client client;
	private NPC brother;
	private BrotherDisguises disguises;
	private final AtomicInteger picks = new AtomicInteger();
	private final List<RuneLiteObject> objects = new ArrayList<>();
	private ModelData original;
	private ModelData detached;

	@Before
	public void setup()
	{
		client = mock(Client.class);
		IndexDataBase configs = mock(IndexDataBase.class);
		when(client.getIndexConfig()).thenReturn(configs);
		when(configs.getFileIds(9)).thenReturn(new int[] {NpcID.CHICKEN, NpcID.COW});
		brother = mock(NPC.class);
		when(brother.getId()).thenReturn(NpcID.BARROWS_DHAROK);
		when(brother.getLocalLocation()).thenReturn(new LocalPoint(128, 256));
		WorldView view = mock(WorldView.class);
		when(view.getPlane()).thenReturn(3);
		when(brother.getWorldView()).thenReturn(view);
		when(brother.getCurrentOrientation()).thenReturn(512);
		disguises = new BrotherDisguises(bound -> picks.getAndIncrement() % bound);
		NPCComposition definition = mock(NPCComposition.class);
		when(client.getNpcDefinition(anyInt())).thenReturn(definition);
		when(definition.getModels()).thenReturn(new int[] {123});
		when(definition.getWidthScale()).thenReturn(128);
		when(definition.getHeightScale()).thenReturn(128);
		when(definition.getColorToReplace()).thenReturn(new short[] {10});
		when(definition.getColorToReplaceWith()).thenReturn(new short[] {20});
		original = mock(ModelData.class);
		detached = mock(ModelData.class);
		when(client.loadModelData(123)).thenReturn(original);
		when(original.shallowCopy()).thenReturn(detached);
		when(detached.cloneVertices()).thenReturn(detached);
		when(detached.cloneColors()).thenReturn(detached);
		when(detached.light()).thenReturn(mock(Model.class));
		when(client.createRuneLiteObject()).thenAnswer(invocation -> {
			RuneLiteObject object = mock(RuneLiteObject.class);
			when(object.isActive()).thenReturn(true);
			objects.add(object);
			return object;
		});
	}

	private void refresh() { disguises.refresh(client, Collections.singletonList(brother)); }

	@Test
	public void restrictsSelectionToTheSixActualBrothers()
	{
		int[] brothers = {NpcID.BARROWS_AHRIM, NpcID.BARROWS_DHAROK, NpcID.BARROWS_GUTHAN,
			NpcID.BARROWS_KARIL, NpcID.BARROWS_TORAG, NpcID.BARROWS_VERAC};
		for (int id : brothers) { assertTrue(BrotherDisguises.isBrother(id)); }
		assertFalse(BrotherDisguises.isBrother(NpcID.BARROWS_SKELETON_ARMED));
		assertFalse(BrotherDisguises.isBrother(NpcID.DT2_DHAROK_COMBAT));
	}

	@Test
	public void picksOnceAndDoesNotRespawnReadyDisguises()
	{
		disguises.spawn(client, brother);
		refresh(); refresh();
		assertEquals(1, picks.get());
		assertEquals(1, objects.size());
		assertTrue(disguises.hides(brother));
		verify(client).getNpcDefinition(NpcID.CHICKEN);
		verify(objects.get(0)).setLocation(new LocalPoint(128, 256), 3);
		verify(objects.get(0)).setOrientation(512);
		verify(objects.get(0)).setAnimationController(isA(NativeNpcAnimation.class));
		verify(brother, never()).getModel();
	}

	@Test
	public void rerollsOnDespawnAndNewSpawnEvenWhenIndexIsReused()
	{
		refresh();
		disguises.despawn(brother);
		assertFalse(disguises.hides(brother));
		verify(objects.get(0)).setActive(false);
		refresh();
		assertEquals(2, picks.get());
		verify(client).getNpcDefinition(NpcID.COW);
	}

	@Test
	public void missingAssetRetriesTheSameChoiceAndKeepsOriginalVisible()
	{
		when(client.loadModelData(123)).thenReturn(null);
		refresh(); refresh();
		assertFalse(disguises.hides(brother));
		assertEquals(1, picks.get());
		assertTrue(objects.isEmpty());
		when(client.loadModelData(123)).thenReturn(original);
		refresh();
		assertTrue(disguises.hides(brother));
		assertEquals(1, picks.get());
	}

	@Test
	public void registrationFailureDeactivatesStagingAndPreservesOriginal()
	{
		RuneLiteObject object = mock(RuneLiteObject.class);
		when(client.createRuneLiteObject()).thenReturn(object);
		doThrow(new IllegalStateException("full")).when(object).setActive(true);
		refresh();
		assertFalse(disguises.hides(brother));
		verify(object).setActive(false);
	}

	@Test
	public void copiesGeometryAndColoursBeforeChangingSource()
	{
		refresh();
		verify(original).shallowCopy();
		verify(original, never()).recolor(anyShort(), anyShort());
		verify(original, never()).scale(anyInt(), anyInt(), anyInt());
		verify(detached).cloneVertices();
		verify(detached).cloneColors();
		verify(detached).recolor((short) 10, (short) 20);
		verify(detached).scale(128, 128, 128);
	}

	@Test
	public void tracksMovementAndFacingWithoutNewRegistrations()
	{
		refresh();
		when(brother.getLocalLocation()).thenReturn(new LocalPoint(384, 512));
		when(brother.getCurrentOrientation()).thenReturn(1024);
		disguises.move();
		verify(objects.get(0)).setLocation(new LocalPoint(384, 512), 3);
		verify(objects.get(0)).setOrientation(1024);
		assertEquals(1, objects.size());
	}

	@Test
	public void reconciliationAndClearRemoveDisguises()
	{
		refresh();
		disguises.refresh(client, Collections.emptyList());
		assertFalse(disguises.hides(brother));
		verify(objects.get(0)).setActive(false);
		refresh();
		disguises.clear(); disguises.clear();
		assertFalse(disguises.hides(brother));
		verify(objects.get(1)).setActive(false);
	}

	@Test
	public void samplesEveryAvailableIdIncludingZeroAndSparseHighIdsWithoutFiltering()
	{
		IndexDataBase configs = client.getIndexConfig();
		int[] ids = {0, 90000, NpcID.BARROWS_DHAROK, 42};
		when(configs.getFileIds(9)).thenReturn(ids);
		for (int id : ids)
		{
			refresh();
			verify(client).getNpcDefinition(id);
			disguises.despawn(brother);
		}
		assertEquals(ids.length, picks.get());
		verify(configs).getFileIds(9);
	}

	@Test
	public void waitsForArchiveMetadataThenMakesOneChoice()
	{
		IndexDataBase configs = client.getIndexConfig();
		when(configs.getFileIds(9)).thenReturn(null);
		refresh(); refresh();
		assertEquals(0, picks.get());
		assertFalse(disguises.hides(brother));
		when(configs.getFileIds(9)).thenReturn(new int[] {77777});
		refresh(); refresh();
		verify(client).getNpcDefinition(77777);
		assertEquals(1, picks.get());
		assertTrue(disguises.hides(brother));
	}

	@Test
	public void changingBrotherAttackOptionDoesNotRerollOrRespawnTheDisguise()
	{
		refresh();
		disguises.refresh(client, Collections.singletonList(brother), true);
		disguises.refresh(client, Collections.singletonList(brother), true);
		disguises.refresh(client, Collections.singletonList(brother), false);
		assertEquals(1, picks.get());
		assertEquals(1, objects.size());
		verify(objects.get(0)).setAnimationController(isA(NativeNpcAnimation.class)); // Settings update the existing controller/clock.
		verify(objects.get(0)).setActive(true);
		verify(objects.get(0), never()).setActive(false);
	}

	@Test
	public void allModesAndOverridesPreserveSelectionAndRegistration()
	{
		for (NpcAnimationMode mode : NpcAnimationMode.values())
		{
			disguises.refresh(client, Collections.singletonList(brother), mode, true, NpcID.CHICKEN + ":*=deny");
			assertTrue(disguises.summary().contains("mode=" + mode));
		}
		assertEquals(1, picks.get()); assertEquals(1, objects.size());
		verify(objects.get(0)).setAnimationController(isA(NativeNpcAnimation.class));
		verify(objects.get(0)).setActive(true); verify(objects.get(0), never()).setActive(false);
		assertTrue(disguises.summary().contains("Models: [123]"));
	}

	@Test
	public void inspectorSummaryReportsSelectedIdWithoutResolvingModels()
	{
		refresh();
		String report = disguises.summary();
		assertTrue(report.contains("Available NPC definitions: 2"));
		assertTrue(report.contains("selected NPC #" + NpcID.CHICKEN));
		assertTrue(report.contains("render objects, not new NPC actors"));
		verify(brother, never()).getModel();
		verify(objects.get(0), never()).getModel();
	}

	@Test
	public void modelLessDefinitionKeepsTheOriginalWithoutRerolling()
	{
		NPCComposition empty = mock(NPCComposition.class);
		when(client.getNpcDefinition(NpcID.CHICKEN)).thenReturn(empty);
		refresh(); refresh();
		assertFalse(disguises.hides(brother));
		assertEquals(1, picks.get());
		assertTrue(objects.isEmpty());
		assertTrue(disguises.summary().contains("Resolved definition has no models"));
	}

	@Test
	public void resolvesTheChosenDefinitionsCurrentMorph()
	{
		NPCComposition morph = mock(NPCComposition.class);
		NPCComposition child = client.getNpcDefinition(NpcID.COW);
		when(morph.getConfigs()).thenReturn(new int[] {NpcID.COW, -1});
		when(morph.transform()).thenReturn(child);
		when(client.getNpcDefinition(NpcID.CHICKEN)).thenReturn(morph);
		refresh();
		assertTrue(disguises.hides(brother));
		verify(morph).transform();
		assertEquals(1, picks.get());
	}

	@Test
	public void cleanupFailureDoesNotPreventOtherCleanupOrRestoreVisibility()
	{
		NPC second = mock(NPC.class);
		when(second.getId()).thenReturn(NpcID.BARROWS_AHRIM);
		LocalPoint location = brother.getLocalLocation();
		WorldView view = brother.getWorldView();
		when(second.getLocalLocation()).thenReturn(location);
		when(second.getWorldView()).thenReturn(view);
		disguises.refresh(client, java.util.Arrays.asList(brother, second));
		doThrow(new IllegalStateException("cleanup")).when(objects.get(0)).setActive(false);
		disguises.clear();
		verify(objects.get(1)).setActive(false);
		assertFalse(disguises.hides(brother));
		assertFalse(disguises.hides(second));
	}
}
