# Cache-backed material research and first curated mappings

Inspected 2026-10-04. These are **reviewable colour/material mappings**, not claims of identical scenery or wall-image texture swaps. This document records the original three cache-sampled themes and historical model experiments. The six additional artistic material palettes are documented separately in [theme-palettes.md](theme-palettes.md), and their subsequently implemented cache-backed wall geometry in [theme-models.md](theme-models.md).

## Primary sources and method

- A private working copy of the locally downloaded Jagex OSRS cache was decoded with the local RuneLite cache library (`cache-1.12.33-SNAPSHOT.jar`). The original cache was not edited; no game models/images or XTEA keys are included in this plugin.
- Snapshot fingerprint: `main_file_cache.idx2` SHA-256 `8eea5056e4005bdf40483f6f0037f3da82c78a92f762f769165068c67a8e8cff`. This fingerprints the object/config index file, not every cache asset. Locally cached assets may differ from later game revisions.
- Reproducible extraction: [`../tools/Study.java`](../tools/Study.java), instructions in [`../tools/README.md`](../tools/README.md). It decodes region placements, floor definitions and referenced models; applies object recolour/retexture tables before collecting material frequencies; reports unavailable models explicitly.
- RuneLite primary implementation references: [RegionLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/region/RegionLoader.java), [ObjectManager](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/ObjectManager.java), [ModelLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/ModelLoader.java), [ObjectID gameval names](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/gameval/ObjectID.java).
- Runtime constraints: [Mesh](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/Mesh.java) exposes an existing face-texture array, not a setter to attach one; [SceneUploader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-client/src/main/java/net/runelite/client/plugins/gpu/SceneUploader.java) uses model face colours, texture IDs and texture coordinates. RGB floor samples are converted with RuneLite's [JagexColor](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/JagexColor.java).

## Findings

### Barrows and the missing tomb-floor details

Region `14231` contains the lower maze on plane `0` and brother tombs on plane `3` in this snapshot. `BARROWS_CRYPT` (`20728`) and `BARROWS_CRYPT_PURPLE` (`20731`) reference models `6618/6619/6620`. All these referenced models were available and decoded; no textured faces were found. Brick relief is model geometry with coloured faces, not a brick-wall image.

The tomb map has the following previously unselected type-22 ground decorations:

| Constant | ID | Model | Placements, plane 3 | Packed material colour |
|---|---:|---:|---:|---:|
| `BARROWS_DUGUPSOIL_1_PURPLE` | 20761 | 1124 | 22 | 64018 |
| `BARROWS_DUGUPSOIL_2_PURPLE` | 20762 | 1139 | 209 | 64018 |
| `BARROWS_DUGUPSOIL_3_PURPLE` | 20763 | 1032 | 15 | 64018 |

All three have no configured animation and no image textures in the decoded models. Their brown counterparts `20750/20751/20752` occur on plane `0`. All six are now eligible for theming. These are a cache-backed candidate for the reported missed floor rubble; confirmation of the particular live pieces still requires in-game review.

### Zanaris

Regions `9540/9541/9797` contain `FAIRY_WALL_MUSH_GREEN/RED` and their variants (`12009..12012`). Models `11909/11911/11910/11912` were available and decoded; they have **no image-textured faces**. The green wall's major colour is packed HSL `24524` (102 faces in model 11909); mushroom stems include `10326`, and green details include `30156` and `21714`.

Zanaris ground underlay `142` has RGB `#50A0B8`, with a darker underlay `143` at `#005868` (zero-based cache definition IDs). The first preset uses the main ground's hue/saturation, retaining Barrows floor gradients. RGB conversion and lighting mean this is not a pixel-identical terrain reproduction.

A textured overlay exists in part of region `9541`, but is not evidence of a Zanaris *wall* texture. It is deliberately not substituted for walls.

### Chambers of Xeric

Regions `13137/13138` contain `RAIDS_WALL_1/2` (`29781/29782`). Their models `32435/32439/32432/32437` were available and decoded, without image textures. Major packed colours are `28950` (stone), `29194` (darker stone/detail), and `28830` (lighter stone). Ground underlay `102` has RGB `#111E1A`; the first preset uses its hue/saturation with a modest brightness reduction.

### Inferno

Region `9043` contains `INFERNO_WALL_EDGE_LARGE_01` (`30319`, model `33064`), `INFERNO_FLOOR_WALL_LARGE_CORNER_01` (`30315`, model `33072`) and `INFERNO_FLOOR_SAND_STRAIGHT_01` (`30288`, model `33098`). These models were available and decoded, without image textures. Cooled wall edges include greys `8/12`; the corner includes heated colours `949/272`; the floor model uses grey `12`. The first preset uses cooled greys for main stone and brighter heated accents for relief, retaining original geometry.

## First curated role mappings

These **role assignments are design choices**, backed by the sampled target colours above. They do not transfer mushroom shapes, CoX crystals or lava geometry into Barrows.

| Barrows role | Brown reference HSL | Purple reference HSL | Zanaris target | CoX target | Inferno target |
|---|---:|---:|---:|---:|---:|
| Main stone / floor detail | 10762 | 64018 | 24524 | 28950 | 8 |
| Secondary stone / trim | 10638 | 63894 | 10326 | 29194 | 12 |
| Relief / accent | 10518 | 63774 | 30156 | 28830 | 949 |
| Pale detail | 10394 | same purple accent family | 21714 | 28830 | 272 |

Runtime matching uses packed hue/saturation groups because lighting changes the low seven bits. Lightness is rescaled from each source role's reference to the target material, with clamping; gradients remain but appearance is not identical to the original scene. Unknown material groups retain the previous theme palette fallback. Black and renderer sentinels remain unchanged.

Manual texture overrides take precedence on texture-capable faces. No texture IDs were guessed or labelled as authentic wall materials. Material mapping does not replace geometry, change collision, edit texture pixels globally or attach missing texture arrays. The separate experimental model-replacement mode below loads existing game assets through the public client API, not dynamic code loading.

## Experimental wall-model replacement

A further offline decode using [`../tools/WallModels.java`](../tools/WallModels.java) established the following source definitions and models. Model IDs have no gameval constant class in this RuneLite API; object selection uses `ObjectID` constants.

| Theme | Source definitions | Straight | Diagonal | Corner |
|---|---|---:|---:|---:|
| Zanaris | `FAIRY_WALL_MUSH_GREEN` / `FAIRY_WALL` | 11909 | 11889 | 11890 |
| CoX | `RAIDS_WALL_1` | 32435 | 32439 | 32432 |
| Inferno (initial mapping) | `INFERNO_WALL_EDGE_LARGE_01` / `INFERNO_WALL_CORNER_LARGE_01` | 33064 | 33064, rotated 45 degrees | 33074 |

The subsequent variation, scenery and calibrated corner mappings below supersede the initial single-model selections.

Barrows `BARROWS_CRYPT` and its purple variant map placement types `0/1/9` to `6618/6619/6620`; `BARROWS_CRYPT_INNER` and its purple variant use `6621`, type `9`. These four models span approximately `-64..73` horizontally, `-250..160` vertically, depending on the shape. Source straight models have very different heights: Zanaris `-240..160`, CoX `-399..0`, Inferno `-434..-59`. Copying them at native scale would not preserve the original wall bounds.

`WallModelReplacements` therefore:

- selects the six crypt wall IDs (main/inner/missing-brick, brown/purple), and optionally explicitly allowlisted static small steps, rocks, rubble and soil; not interactive furniture, doors or chest;
- loads model data with `Client.loadModelData`, shallow-copies it, clones vertices and colours, rotates and fits to the original model-space bounding box;
- reproduces CoX's definition recolour `29574 -> 29194` and Zanaris's ambient lighting offset `+20`; other selected source definitions have default lighting and no model mirroring;
- stages visual-only `RuneLiteObject` instances at the exact original local position, plane, height and **model** render orientation (`GameObject.getModelOrientation()`, not `getOrientation()`); the original scene/collision objects are never removed;
- treats shared `faceColors3` arrays as one atomic group, including aliases on different `Model` instances, and protects arrays referenced by excluded static scenery anywhere in the loaded scene;
- hides eligible original faces with renderer sentinel `-2` only after every replacement in the group is activated, then invalidates GPU zones. RuneLite GPU's `SceneUploader` explicitly skips faces with this sentinel;
- retries unavailable source assets without respawning ready objects every tick, and removes replacements/restores captured arrays on reset before undoing material edits.

This is an **opt-in prototype**, off by default. Fitted bounds are not proof of identical wall surfaces or clear doorway geometry. Animated renderables are not resolved or replaced. Native diagonal Zanaris/CoX pieces are used; Inferno diagonals are a design adaptation, not an authentic diagonal asset. Temporary render-object capacity, roof/culling behaviour and per-frame GPU upload cost need in-game checks. Inspector counts describe registrations, not verified rendered walls.

Public API references: [RuneLiteObject](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/RuneLiteObject.java), [ModelData](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/ModelData.java), [WallObject](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/WallObject.java). No cache meshes, images or keys are distributed.

## Follow-up source mappings and corner calibration

A further private cache-copy study retained the same idx2 fingerprint. All selected source definitions had animation `-1`, default model scale `128/128/128`, no mirroring and contrast `0`. [`../tools/WallModels.java`](../tools/WallModels.java) now also prints animation, footprint and scale metadata.

Straight-wall variants are chosen by `(templateX * 31 + templateY * 17 + templatePlane * 13) mod variantCount`, not per-frame randomness:

| Theme | Straight model pool | Source definitions |
|---|---|---|
| Zanaris | 11909, 11910, 11911, 11912 | `FAIRY_WALL_MUSH_GREEN/RED` and their `_VAR` definitions |
| CoX | 32435, 32437 | `RAIDS_WALL_1/2` |
| Inferno | 33064, 33076, 33077, 33065, 33067, 33066 | `INFERNO_WALL_EDGE_LARGE_01..06` |

The historical scenery-model experiment used these **adapted** sources, fitted to original target bounds. It is now parked: the configuration item is removed and production ignores saved values. These mappings remain as research notes, not active scenery replacements:

| Target role | Zanaris/fairy sources | CoX sources | Inferno sources |
|---|---|---|---|
| Straight small steps | mushroom-ring edge 11942 (`FAIRY_MUSHROOM_RING_EDGE`) | entrance stairs 32384 (`RAIDS_ENTRANCE_STEPS`) | low floor edge 33060 |
| Corner small steps | mushroom-ring corner 11941 | wall corner 32432, flattened into step bounds | floor-wall corner 33072 |
| Brick piles | long red mushrooms 11985 (`FAIRY_MUSHROOMS_LONG_RED`) | floor rocks 32474/32501/32459/32454 | floor rocks 33051/33057 |
| Soil/rocks/rockslides | mossy ground rocks 12125/12126 (`FAIRY2_CENTAUR_AREA_ROCKS1/2`) | same floor-rock pool | same floor-rock pool |

Zanaris/fairy sources use ambient `+20`. Red mushrooms reproduce definition recolours `7442/16454/7265 -> 227/957/127`. Fairy ground rocks reproduce `43/39/35/30 -> 6705/6699/6695/6819`. CoX wall sources alone receive `29574 -> 29194`; CoX floor rocks and stairs have no such definition recolour. Inferno sources retain their colours. Example native bounds: mushroom-ring edge `-58..26,-27..0,-62..60`; CoX entrance stairs `-255..256,-606..0,-64..64`; Inferno floor edge `-128..128,-63..147,-128..128`. They are not native Barrows-sized steps; fitting is a deliberate visual adaptation.

For corner facing, [`../tools/CornerFacing.java`](../tools/CornerFacing.java) compares normalized horizontal mesh slices at 20%, 40% and 60% of model height on a 32×32 grid. Scores are summed bidirectional nearest-point squared distances; lower suggests closer occupied surfaces, **not correct in-game rendering**. The resulting targeted choices are:

| Original | Source | Native rotation offset | Score before / after |
|---|---:|---:|---:|
| Main corner 6620, Zanaris | 11890 | 1024 | 289.34 / 242.76 |
| Inner corner 6621, Zanaris | 11890 | 0 | 232.83 / unchanged |
| Main/inner corner, CoX | 32432 | 0 | already lowest sampled rotation |
| Main corner 6620, Inferno | 33063 (`INFERNO_WALL_CORNER_LARGE_02`) | 512 | old source 33074 at zero: 419.24 / new: 181.47 |
| Inner corner 6621, Inferno | 33074 | 512 | 484.75 / 164.80 |

Native offsets are added to baked placement rotation; original game-object **model** render orientation is retained separately. The first implementation mistakenly used object orientation (`getOrientation()`) here; the live-corner follow-up below corrects that double rotation. Nonzero actual model orientations are still preserved. These mesh-based adjustments have tests for all four placement rotations, but need comparison against the reported live corners. The inspector now reports template placement, type, config rotation and wall/render orientation to support that comparison.

NPC randomization now obtains every existing file ID in config archive 9 through `Client.getIndexConfig().getFileIds(9)`. Native NPC animation metadata is decoded through `loadData(9, selectedOrResolvedId)`, following RuneLite's [NpcLoader](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/cache/src/main/java/net/runelite/cache/definitions/loaders/NpcLoader.java) tag format. The public `NPCComposition` API does not expose native movement-sequence getters. Known idle/walk/run/turn tags are supported; unknown tags stop decoding and malformed data falls back safely. Attack sequences are not a universal field of NPC definitions, so arbitrary native attack animation cannot be inferred from these tags. No NPC IDs are filtered out of the random pool for missing sequences or buggy models.

The private research cache copy was removed after inspection; no meshes, config dumps or keys are distributed.

## Live CoX corner follow-up

**Historical v2 attempt:** the user subsequently confirmed persistent gaps in both opposite corners. The diagonal half-turn below has been undone because it reversed the visible front. The current v3 surface-fitting experiment is unfinished; its failed checks and correct rotation/orientation semantics are recorded in [agent-handoff.md](agent-handoff.md).

The user's Karil tomb comparison at template `3554,9686,3` showed separated, zigzagging replacements where original brickwork joined continuously. Nearby main/inner corners (`20731/20732`, type 9) had placement/object rotations `1536`, also incorrectly reported/applied as render rotation. RuneLite's [GameObject](https://github.com/runelite/runelite/blob/4d7df3fd871fea1331cb32ddc6e14f286de8f16d/runelite-api/src/main/java/net/runelite/api/GameObject.java) distinguishes `getOrientation()` from `getModelOrientation()`: static object models are usually oriented before lighting, with zero model render rotation. The owner now preserves the latter, avoiding a second application of placement rotation. This applies to all themed type-9 corners; genuine nonzero model render rotations are retained.

A new private cache-copy comparison of Barrows diagonal `6619` against CoX `32439` produced surface-distance scores at `0/512/1024/1536` of `1910.47/1210.92/542.22/1218.31`. CoX diagonals now receive a native half-turn (`1024`), composed once with the wall-mask rotation. Corner assets/offsets, original geometry, collision and bounding-box fitting are unchanged. The snapshot idx2 fingerprint was unchanged. Reproduce with `CornerFacing CACHE_COPY 6619 32439`; no meshes or keys are distributed. The private copy was removed after inspection.

Tests first failed on the repeated corner rotation and on a synthetic opposite-corner diagonal profile with identical bounds. They now cover actual model orientation versus object orientation, all four CoX diagonal masks and both crypt palettes. Report **v2** distinguishes `objectRot` from actual `renderRot`; full inspection labels them separately too. These checks establish transform/mapping corrections, not that all visible gaps are closed. Repeat the same on/off screenshot comparison in-game before claiming the visual problem fixed.

## Review and remaining work

- Review the original three cache-sampled profiles and six new artistic palettes in-game, especially brightness, Inferno accent frequency, coffin/stair detail and soil/rubble coverage.
- Inspect nearby original scenery from the sidebar to identify remaining IDs or dynamic/shared models. Inspection temporarily restores original arrays and reapplies the theme within the same client-thread task.
- If actual image-textured walls are desired on these untextured models, investigate a supported model/renderer integration separately. A texture-ID remap alone cannot add the missing arrays or turn Barrows brick geometry into mushroom geometry.
- Review experimental model replacements with GPU: all nine themes, both crypt planes, corners/diagonals, doorway clearance, roof removal, FPS, and repeated theme changes/reloads.
- Builds and tests verify mapping/fitting logic, atomic staging and cleanup, not game rendering. The user's CoX comparison established a visual alignment failure; the subsequent transform corrections and remaining themes still require in-game verification.
