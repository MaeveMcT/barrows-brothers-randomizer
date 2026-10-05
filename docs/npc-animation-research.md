# NPC disguise animation research

## Scope and runtime constraints

**Barrows Brothers Randomizer** retains the NPC-disguise feature from the former Barrows Themes plugin. All scenery/texture features and diagnostic action recording have been removed. Production uses public live-client interfaces only: no reflection, disk-cache access, process execution, input injection or dynamic loading. Offline research tools live outside `src/`; no cache assets or keys are distributed.

The random pool is every file ID in config archive **9**. Definitions include static rocks, invisible helpers, statue variants and model-less morphs, not just animated combat monsters. Neither NPC identity/name/size nor a successful transform proves visual suitability. The pool must remain unfiltered; failed choices retain the original without rerolling.

A disguise is a visual `RuneLiteObject`, not a new `NPC`. Original brothers retain combat, menus, collision, clickboxes and overheads. GPU geometry suppression happens after picking through `RenderCallback.drawObject`; scene-admission callbacks must not reject disguised brothers.

## Primary sources

Inspected RuneLite cache revision `4d7df3fd871fea1331cb32ddc6e14f286de8f16d`:

- [NpcLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/NpcLoader.java), [NpcDefinition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/NpcDefinition.java): optional native movement metadata and morph/model definitions, but no universal attack field.
- [NPCComposition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/NPCComposition.java), [Client](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Client.java), [RuneLiteObject](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/RuneLiteObject.java): supported model loading, detached transforms and visual objects.
- [SequenceLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/SequenceLoader.java), [FrameLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/FrameLoader.java), [FramemapLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/FramemapLoader.java): classic transform-type/group-label layouts.
- [ModelDefinition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/ModelDefinition.java): missing skin labels can be skipped silently; a nonthrowing transform is not compatibility proof.
- [AnimationID](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/gameval/AnimationID.java): named candidate action sequences.

## Native movement and static fallback

Production decodes native idle/walk/turn/run/crawl fields through `Client.getIndexConfig().loadData(9, id)`, because the live composition interface does not expose them. Unknown tags stop safely; malformed/truncated metadata, unavailable animation assets, invalid frames, failed transforms and definitions without a native gait retain static/native fallback. Current morph children supply their models and animation definitions.

A private copied-cache audit compared all **15 movement fields** against RuneLite for **16,576 definitions**: zero mismatches/unsupported tags; **1,830** definitions lacked native movement metadata. Snapshot idx2 SHA-256: `8eea5056e4005bdf40483f6f0037f3da82c78a92f762f769165068c67a8e8cff`. This is not proof about every asset, later revisions or a particular static disguise. The user's `15343 → 15345` selection was Ghost Jenkins' ship/no-op variant with no gaits, not a demonstrated decoder bug.

## Animation policy

- **Auto** (default): borrows brother actions only with full classic frame-map evidence; otherwise retains native gait/static fallback. Shared gait IDs alone are only a hint.
- **Native only:** native gait only, never borrowed brother actions.
- **Force:** explicitly tries brother actions before native fallback.

There are no explicit native attack mappings, brother-attack classifiers or custom action overrides. NPC definitions have no universal attack-animation field; unsupported brother actions leave native gait/static playback in place.

Rig evidence compares every required classic frame's full transform-type/group-label layout against the selected native gait. Layouts can match across different skeleton IDs. Malformed, incomplete and Maya/weighted combinations are not automatically borrowed. Model skin labels/anatomy remain unverified even when layouts match. Playback consumes a typed compatible/incompatible/unknown classification; diagnostic wording never authorizes borrowing.

Rig metadata is prepared outside rendering, bounded by caches and at most **24 metadata reads per game tick** across disguises. Native gait uses its own clock; borrowed actions use the real brother's action/frame. Action-change notifications invalidate prepared compatibility evidence. Option changes preserve selection, registration and gait clock.

## Remaining checks

Verify the renamed plugin's six-brother scope, targeting, overhead alignment, movement/actions, cleanup and no-reroll option updates in-game. Test oversized/static/model-less choices without filtering or rerolling. Offline commands: [../tools/README.md](../tools/README.md).
