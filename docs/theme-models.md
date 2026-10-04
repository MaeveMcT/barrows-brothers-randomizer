# Model-first themes

**Checkpoint status:** the CoX surface-fitting experiment is unfinished (five failing tests, two failing replay joins). Current rotation/orientation findings and continuation instructions: [agent-handoff.md](agent-handoff.md).

All nine current themes now have explicit wall-model sets. Enable **Walls / scenery** and **Replace wall models (experimental)** to use them. This is real cache geometry fitted to Barrows, not another recolour of Barrows walls. Floors, furniture and the parked crypt-scenery experiment remain material-only.

## Source evidence

Inspected a private copy of the local Jagex cache with [WallModels.java](../tools/WallModels.java); no cache assets or keys are distributed. The idx2 fingerprint remains `8eea5056e4005bdf40483f6f0037f3da82c78a92f762f769165068c67a8e8cff`. All selected new models decoded successfully, with zero textured faces. Their definitions have animation `-1`, no mirroring or retexture tables, and scale `128/128/128`.

Definition names/IDs come from RuneLite's [ObjectID](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/gameval/ObjectID.java) and [ObjectID1](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/gameval/ObjectID1.java). Model/type, recolour and lighting metadata follows [ObjectLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/ObjectLoader.java); mesh availability and bounds follow [ModelLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/ModelLoader.java). Model IDs have no gameval constants.

| Theme | Source definitions | Straight pool | Diagonal | Corner |
|---|---|---|---|---|
| Prifddinas | `PRIF_HOUSE_WALL/2` (36247/36248) | 37493, 37497 | 37474 | 37500 |
| Ancient Pyramid | `FOUR_DIAMONDS_PYRAMID_WALL/CORNER` (6539/6540) | 6254 | 6254, rotated 45° | 6247 |
| Darkmeyer | `WALLKIT_DRAKAN01_DEFAULT01_DARKMEYER` / `...DAMAGED01_DARKMEYER` (61518/61520) | 60820, 60824 | 60821 | 60823 |
| Fremennik ice caves | `HUNTING_POLAR_CAVEWALL_FACE1` (19693) | 9963 | 9964 | 9965 |
| Dorgesh-Kaan | `DORGESH_GROUND_WALLS_INNER`, `DORGESH_FIRST_WALLS_INNER`, `DORGESH_GROUND_WALLS_INNER_WINDOW` (22898/22906/22902) | 23757, 23749, 23766 | 23758 | 23760 |
| Abyss | `RCU_ABYSSAL_WALL/BULGE` (26150/26153) | 7413, 7416 | 7414 | 7415 |

Zanaris, CoX and Inferno retain their existing cache-backed wall sets; see [material-research.md](material-research.md). Straight variants use stable instance-template coordinates, never per-tick random choices. Disabling variation uses the first source.

**Adaptations:** the ice theme uses northern polar-hunter cave geometry, not a verified reconstruction of a particular Fremennik dungeon. Prifddinas uses house walls, not giant city-boundary walls or new crystal effects. Pyramid diagonals reuse the actual pyramid wall with a rotation; this is not a native diagonal definition. Source shapes are fitted independently to original model-space bounds, so native proportions are deliberately changed. Large 3×2 Prifddinas city walls and 7×7 Duke ice chunks were rejected for this first pass.

## Native colours and lighting

Source meshes retain their own materials rather than receiving the artistic Barrows role palette. Definition recolours and ambient/contrast offsets are reproduced on detached copies:

| Theme | Ambient offset | Decoded contrast offset | Recolour table |
|---|---:|---:|---|
| Prifddinas | 20 | 250 | `7465/6435/5404/-22423/-22440/-22456 → 3350/3346/3470/7465/6435/5404` |
| Ancient Pyramid | 0 | 1000 | `10434 → 8526` |
| Darkmeyer | 10 | 2500 | `284/278/268/549/547/419 → 5404/5400/5392/173/169/167` |
| Polar ice | 30 | 750 | `7442/7446/7322/7326/7331/7335 → -29238/-29234/-31406/-31522/-27537/-22407` |
| Dorgesh-Kaan | 35 | 0 | none |
| Abyss | 0 | 0 | none |

Contrast is already multiplied by 25 by the cache loader; runtime adds these decoded offsets to `ModelData.DEFAULT_CONTRAST` once. Negative colour values are signed representations of packed unsigned HSL values. Native lighting metadata is not a guarantee of matching the source area's appearance in Barrows.

## Corner facing

[CornerFacing.java](../tools/CornerFacing.java) compared normalized horizontal slices against original models 6620 (main) and 6621 (inner). These quarter-turn offsets select the lowest sampled score, not verified visual correctness:

| Theme | Main corner offset | Inner corner offset |
|---|---:|---:|
| Prifddinas | 1024 | 0 |
| Ancient Pyramid | 512 | 0 |
| Darkmeyer | 1024 | 0 |
| Polar ice | 0 | 1024 |
| Dorgesh-Kaan | 0 | 1024 |
| Abyss | 0 | 1024 |

All offsets compose with the four original placement rotations; actual **model** render orientation remains separate. A live CoX follow-up corrected accidental double rotation of game-object corners by using `getModelOrientation()` instead of object `getOrientation()`. The v2 CoX diagonal half-turn failed live review and was undone: its unsigned slice heuristic ignored face winding. CoX currently uses the unfinished front-surface fitter rather than another rotation adjustment; see [agent-handoff.md](agent-handoff.md). Missing assets, unsupported shapes and protected shared arrays retain original geometry/material treatment. Existing atomic staging, retry and cleanup rules apply to every theme.

## Future theme rule

New themes must include researched, explicit wall geometry rather than ship as recolour-only presets. Record source definitions, supported shapes, native materials/lighting and any deliberate adaptations here. Never silently use another theme's model set. The all-theme mapping test requires every enum entry to supply its own shapes; adding a theme requires extending that test and its expected sources. Where suitable assets cannot be established, defer the theme instead of presenting recolours as model support.

## Short corner diagnostics

The sidebar's **Short wall-corner report** produces a separate, bounded snapshot of nearby static corner/diagonal replacements, without the full scenery/NPC listing. Stand beside the affected corner, copy the report, and pair it with a screenshot. Report v3 distinguishes object orientation from actual model render rotation and labels `fit=front-surface profile` (CoX) versus `fit=bounds`. It reports theme, template location, type/slot, source model, placement/extra/baked/render rotations, object rotation or wall masks, height, registration/protection status and original/native-source/fitted bounds. It reads existing geometry and caches only: no reset, new assets or replacement registration. Disabled replacement still reports candidate placements; uncached bounds are unavailable. Matching bounds do not prove matching corner surfaces.

## In-game review required

Compare all nine themes in both tunnels and tombs, including all four corner orientations, seams, diagonals, doorway clearance, roof cutaways, brightness and FPS. Switch themes with replacement enabled, toggle it off, disable/re-enable and reload. Confirm originals return without stale or invisible faces and interactions/collision are unchanged. Automated builds/tests establish mapping and lifecycle evidence only. The user's CoX comparison demonstrated an alignment failure that persisted with v2 rotations; the subsequent v3 surface-fitting experiment remains unverified in-game and has failing automated checks. The six new-theme meshes also remain unverified.
