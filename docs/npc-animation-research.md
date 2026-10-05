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

- **Auto** (default): native attacks first; otherwise requires full classic frame-map evidence. Shared gait IDs alone are only a hint.
- **Native only:** native gait and mapped native attacks, never borrowed brother actions.
- **Force:** explicitly tries brother actions before native fallback.

Native attack mappings are always enabled. Custom action overrides and the mapped-native-attack toggle are no longer exposed or used by the plugin.

Rig evidence compares every required classic frame's full transform-type/group-label layout against the selected native gait. Layouts can match across different skeleton IDs. Malformed, incomplete and Maya/weighted combinations are not automatically borrowed. Model skin labels/anatomy remain unverified even when layouts match.

Rig metadata is prepared outside rendering, bounded by caches and at most **24 metadata reads per game tick** across disguises. Native gait/action playback has separate clocks. Action-change notifications are retained for real restarts; the removed recorder does not affect playback. Option changes preserve selection, registration and gait clock.

## Native attack families

Mappings are guarded by current cache-checked idle/walk families and corroborated by full classic idle/attack layouts, not universally discovered or comprehensively verified in-game.

| Family | Explicit definitions | Idle / walk | Attack |
|---|---|---|---|
| Chickens | 1173, 1174, 2804, 2805, 2806 | 5386 / 5385 | 5387 |
| Cows, not calves | 2790, 2791, 2793 | 5852 / 5848 | 5849 |
| Bats | 2827, 2834 | 4914 / 4913 | 4915 |
| Bears | 2838, 2839 | 4919 / 4923 | 4925 |
| Giant rats | 2856, 2859 | 4932 / 4931 | 4933 |

Native attacks trigger only on the explicitly recognized brother weapon/cast actions, not arbitrary hit/death events. They play once on their own clock and restart on genuine action changes/frame resets. Only the built-in researched mappings are used; previously saved numeric overrides do not apply.

Earlier live observations included brother attacks Verac **2062**, Torag **2068**, Guthan **2080**, Karil **2075**, and Ahrim **2079**, along with hit/death/other actions. Skeleton **5485** and bloodworm **2070** remain research candidates, not newly implemented mappings. Target presence and `AnimationChanged` alone are not attack classifiers. No diagnostic recording option remains.

## Remaining checks

Verify the renamed plugin's six-brother scope, targeting, overhead alignment, movement/actions, cleanup and no-reroll option updates in-game. Test oversized/static/model-less choices without filtering or rerolling. Extend native mappings only with reviewed definition/gait/action evidence. Offline commands: [../tools/README.md](../tools/README.md).
