# NPC disguise animation follow-ups

## Questions

1. Can an arbitrary selected NPC's own attack animations be identified and played?
2. Why do some selected IDs produce static/scenery-like models rather than recognizable animated NPCs?

The source findings below now have an implemented conservative animation policy, native-attack mappings, cache audit and observation tools. They are not a diagnosis of any particular live disguise: the user's affected selected IDs have not yet been captured, and the new behaviour still needs in-game verification.

## What the selected ID means

The random pool comes from file IDs in the live client's NPC config archive (archive 9). These are NPC **definitions**, not a filtered list of combat-capable, animated monsters. RuneLite's gameval list includes inactive rock forms, invisible/dummy helpers and statue variants, for example `HORROR_ROCKCRAB_INACTIVE`, `DESERT_TREASURE_INVISIBLE_NPC` and `LOTR_TERROR_DOG_STATUE`. Those names establish the existence of nonstandard definitions, not that any particular example caused the user's static display. Sources: [ConfigType.NPC](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/ConfigType.java), [NpcID](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/gameval/NpcID.java).

An NPC definition supplies model IDs, colour overrides, size/scaling and optional transforms. It can have no standing/walking sequence: RuneLite's decoded definition defaults those fields to `-1`. Therefore “genuine NPC definition” does not imply a humanoid, moving monster or even a model that can currently be shown. Sources: [NPCComposition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/NPCComposition.java), [NpcDefinition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/NpcDefinition.java).

## The disguise is not a new NPC actor

Our plugin loads those models, creates a lit detached base model and draws it as a `RuneLiteObject`. This is a visual render object with a model and optional animation controller, not a new `NPC`. The original Barrows brother still owns combat, movement, menus, clickbox and overhead UI. Sources: [RuneLiteObject](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/RuneLiteObject.java), plugin implementation [BrotherDisguises.java](../src/main/java/com/barrowsthemes/BrotherDisguises.java) and [BarrowsThemesPlugin.java](../src/main/java/com/barrowsthemes/BarrowsThemesPlugin.java).

Creating this visual object does not give it server-driven behaviour or cause the selected NPC to execute its usual attacks. Rendering models/animations must be supplied separately. Sources: [RuneLiteObjectController](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/RuneLiteObjectController.java), [AnimationController](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/AnimationController.java).

## Native attacks: current evidence and limits

RuneLite's examined NPC definition schema/loader contains idle, movement and turn sequence fields, but no universal `attackAnimation` field. Tags 13–17 encode idle/walk/turn sequences, and 114–117 encode run/crawl families. This is a limitation of this inspected schema, not proof that no specific NPC/script/parameter elsewhere contains useful attack information. Sources: [NpcLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/NpcLoader.java), [NpcDefinition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/NpcDefinition.java).

A real live actor exposes its current action sequence with `getAnimation()` and frame with `getAnimationFrame()`. `AnimationChanged` can report changes, but its documented examples include non-attack actions; it is not an attack classifier. Sources: [Actor](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Actor.java), [AnimationChanged](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/events/AnimationChanged.java).

Named gameval animation constants can supply candidates for known NPC families; adjacent IDs, matching rigs or similar names alone do not establish that an animation is the selected NPC's attack. Source: [AnimationID](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/gameval/AnimationID.java).

## Why a disguise can appear static

The implementation deliberately retains a static base model when:

- definition bytes or animation assets are not yet available;
- the decoded definition has no appropriate idle/movement sequence;
- an unsupported tag stops our best-effort decoder before relevant metadata;
- malformed/truncated data cannot be trusted;
- the frame is invalid, transformation returns no model, or animation application throws.

A valid requested sequence also does not prove visible movement: the model may be intrinsically static, the sequence may barely move it, or rig/skin data may not produce useful deformation. We need actual selected IDs and live observation before deciding which applies. Implementation sources: [NativeNpcAnimations.java](../src/main/java/com/barrowsthemes/NativeNpcAnimations.java), [NativeNpcAnimation.java](../src/main/java/com/barrowsthemes/NativeNpcAnimation.java); transformation-copy contract: [Client.applyTransformations](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Client.java).

## Diagnostics added

**Inspect nearby original scenery** now includes a random-brother section with selected/resolved NPC IDs, registration state and requested sequence/frame or static-fallback reason. It does not call original NPC/model `getModel()` or mutate their animations. Counts and sequence/frame status are diagnostic evidence, not proof of rendered animation.

The inspector now includes source model IDs, all primary gait IDs, animation mode, compatibility evidence, native/brother action playback, and specific model/registration failures. **Record NPC actions** optionally adds a bounded, session-only record of real NPC action changes anywhere in the world. No observations are automatically promoted to attack mappings.

The old **Use brother attack animations** checkbox is replaced by **NPC animation mode**. A saved enabled checkbox migrates once to **Force brother actions** unless a new mode is already saved. Mode/override changes update the existing controller without rerolling, re-registering or resetting its gait clock.

## Selective compatibility with brother actions

The user reports some selected IDs work well with brother attack animations. There is no direct `compatibleWithBarrows` or humanoid-rig flag in the examined NPC definition/API. The numeric NPC ID, combat level, name and tile size do not establish animation compatibility. Useful evidence lies in the definition's native sequences and underlying model skinning/animation frame maps, not the ID number itself. Sources: [NPCComposition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/NPCComposition.java), [NpcDefinition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/NpcDefinition.java).

**Cheap hint:** a selected definition sharing native idle/walk sequence IDs with the real brother is evidence of a shared animation convention. Those IDs are already decoded by this plugin, and the real brother exposes idle/walk getters. This is a conservative heuristic, not a compatibility proof: different idle/walk IDs can still use compatible rigs, and sharing a gait does not prove every attack uses a suitable transform layout. Sources: [NpcLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/NpcLoader.java), [Actor](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Actor.java).

**Stronger classic-animation hint:** compare the frame-map/skeleton ID and transform-type/group-label layout used by the selected model's native sequences with those required by the brother's current action. Classic sequences reference frame files; their headers reference frame maps containing transform types and label lists. Model skinning supplies vertex groups to which those labels apply. Matching this structure is much more informative than matching NPC size/name, but is still not a visual-quality guarantee. Classic models do not simply supply a universal skeleton ID that can be compared in isolation. Sources: [SequenceLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/SequenceLoader.java), [FrameLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/FrameLoader.java), [FramemapLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/FramemapLoader.java), [ModelDefinition](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/ModelDefinition.java).

An animation not throwing is insufficient evidence. RuneLite's cache-model animator skips group labels outside the model's group array, and existing groups can refer to anatomy that does not look appropriate under the borrowed transformation. Thus an incompatible combination can quietly remain static or deform rather than fail. Source: [ModelDefinition.animate](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/ModelDefinition.java).

The public `Animation`/`Model`/`Mesh` interfaces do not expose the necessary skeleton/frame-map or vertex-skin-group getters. A stronger detector would require extra metadata decoding through supported client cache-index APIs, rather than reflection into client internals. Maya/weighted animations require separate treatment and must not be classified by classic group-label rules alone. Sources: [Animation](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Animation.java), [Model](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Model.java), [Mesh](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Mesh.java), [ModelDefinition animayaGroups/animayaScales](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/ModelDefinition.java).

Implemented policy: **Native only / Auto / Force brother actions**. The complete random-ID pool remains unchanged. Native-only is the default; mapped native attacks are enabled by default.

- **Native only:** native gait and mapped native attacks; never borrows brother actions, even with an allow override.
- **Auto:** prefers a mapped native attack. Otherwise requires positive classic frame-map evidence or an explicit reviewed allow. Unknown/incomplete/Maya/weighted combinations keep native gait/static fallback. Shared idle/walk IDs appear as a hint but do not independently authorize borrowing.
- **Force:** tries the brother's current action/frame, including hit/death actions, before native fallback. Explicit deny/native-sequence overrides still apply.

[AnimationRigCache.java](../src/main/java/com/barrowsthemes/AnimationRigCache.java) uses public cache-index APIs: config archive 12 for sequences, index 0 for classic frames, index 1 for skeleton/frame maps. It compares every referenced action frame's full transform-type/group-label layout against those supported by the selected native gait. Matching layouts can have different skeleton IDs. Malformed frame payloads and extended/weighted skeletons are rejected. Rig metadata is prepared on client/game ticks, never inside `animate()`, with at most 24 rig-metadata cache reads per game tick across all disguises, bounded Guava caches, once-per-tick missing-asset retries and shutdown/logout cleanup. New action metadata may take several ticks to load; rendering falls back during that time.

This remains evidence, not a visual guarantee: model vertex-skin groups and anatomy are not verified. No nonthrowing transform is treated as proof. Native Maya gaits remain supported by RuneLite's playback API; only automatic borrowing of unknown Maya/weighted rigs is excluded. Verified user overrides or Force can explicitly try them.

### Reviewed action overrides

**NPC action overrides** accepts up to 128 semicolon-separated rules, keyed by resolved disguise definition and actual brother action:

```text
1173:2075=5387; 42:*=deny; 42:2067=allow
```

A numeric value plays that native sequence on its own clock; `allow` authorizes borrowing in Auto/Force; `deny` prohibits borrowing but does not disable a source-backed native attack. `*` matches any nonnegative action. Exact rules beat wildcards; the last duplicate wins; invalid entries are ignored and counted in the inspector. The example's `42` rules illustrate syntax, not a verified NPC classification. Numeric rules are explicit manual mappings and can classify additional brother actions; review them carefully. Disabling **Mapped native NPC attacks** also disables numeric native overrides.

### Initial native-attack families

[NativeAttackMappings.java](../src/main/java/com/barrowsthemes/NativeAttackMappings.java) contains 14 explicit definitions across five families, guarded by their current idle/walk metadata. Evidence is the named gameval attack plus cache-confirmed definition/gait family and full classic-layout corroboration; it is **not in-game verification**.

| Family | Explicit NPC IDs | Native idle / walk | Native attack |
|---|---|---|---|
| Chickens | 1173, 1174, 2804, 2805, 2806 | 5386 / 5385 | `LORE_CHICKEN_ATTACK` (5387) |
| Cows, not calves | 2790, 2791, 2793 | 5852 / 5848 | `COW_UPDATE_ATTACK` (5849) |
| Bats | 2827, 2834 | 4914 / 4913 | `BAT_REWORK_ATTACK` (4915) |
| Bears | 2838, 2839 | 4919 / 4923 | `BEAR_REWORK_ATTACK` (4925) |
| Giant rats | 2856, 2859 | 4932 / 4931 | `GIANT_RAT_UPDATE_ATTACK` (4933) |

The original old-family constants (`CHICKEN_ATTACK=55`, `COW_ATTACK=59`, etc.) are not used for these reworked definitions. Dogs/calves and unlisted variants are deliberately left unmapped, not filtered from selection.

Native attacks trigger only for the explicit brother weapon/cast action mapping in that class, not arbitrary hit/death/other actions. They play once on their own duration/frame clock, restart on real action changes or frame resets, and return to gait when finished or when the brother's action ends. They do not reuse the brother's frame number or attempt to reproduce combat mechanics. Known brother action candidates and native families still need live observation, including repeated same-ID attacks.

[NativeAttackStudy.java](../tools/NativeAttackStudy.java) confirmed full classic layout matches for all five idle/attack pairs above. It also corroborated the six brother idle/action pairs `813→729`, `2065→2067`, `813→2080`, `808→2075`, `808→2068`, `2061→2062`. This confirms layout structure, not that every candidate is exercised by the live combat server.

### Static-disguise decoder audit

[NpcAnimationAudit.java](../tools/NpcAnimationAudit.java) compared all 15 decoded idle/walk/turn/run/crawl fields against RuneLite's `NpcLoader` for every definition in a private local cache copy:

```text
definitions=16576 movement mismatches=0 no idle/walk=1830 no movement=1830 incomplete={}
```

The snapshot's idx2 SHA-256 is `8eea5056e4005bdf40483f6f0037f3da82c78a92f762f769165068c67a8e8cff`; it is not a fingerprint of every asset or a promise about later revisions. No unsupported definition tags were found in this snapshot. This rules out a decoder mismatch in these fields for this cache, not model/animation failures or the user's particular live examples. The 1,830 definitions without native movement are retained in the random pool.

Run/crawl directional metadata is now retained, and crawl/run can supply movement when a definition has no ordinary walk. Unknown tags still stop safely, with byte offset reported. Missing idle sequences remain static when standing rather than inventing an idle. The private cache copy was removed after research; no meshes, config dumps or keys are distributed.

### Real NPC action observations

Enable **Record NPC actions**, observe ordinary real NPC combat, then copy the inspector's action section. It records original/resolved IDs, action IDs, idle/walk IDs, event counts, whether an interacting actor was present, and last game tick. It holds at most 256 combinations and clears on logout, shutdown or disabling recording. Nothing is saved or uploaded. Target presence is contextual evidence, **not** an attack classifier; deaths, defence and specials must be reviewed separately before entering overrides.

## Next investigation

1. Capture inspector reports for a few static/scenery-like disguises, preferably after waiting several ticks. Compare selected ID versus resolved morph ID, definition metadata and reported fallback reason.
2. Re-run the decoder audit for changed cache revisions or inspector-reported unsupported tags. The current full snapshot has zero movement-field mismatches; do not attribute unexplained static rendering to parsing without live evidence.
3. Use the implemented recorder to verify the five mapped native families and brother attack classification in live combat. Distinguish attacks, defence, death and special actions; do not treat every `AnimationChanged` as an attack.
4. Extend the small source-backed mapping only after reviewing new observations. Check multi-attack bosses, shared-model skins and morph variants. Unknown IDs retain native gait/static fallback, without restricting the user's full random-ID pool.
5. Review related scripts/parameters where primary evidence exists. Do not guess attack IDs by offsets from idle/walk or claim universal coverage from a partial mapping.

No input injection, server actor spawning, reflection or dynamic code loading is needed or proposed.
