# Agent handoff — wall alignment checkpoint

## Read first: this commit is WIP, not a verified wall fix

The user requested a local commit and handoff while a second wall-fitting investigation was in progress. This repository previously had **no commits**: the checkpoint includes the whole plugin, not just the latest wall changes. Do not publish/release it as a completed fix. Nothing was pushed.

Current checks:

- `./gradlew build`: **129 tests, 5 failures, 0 errors, 0 skips**; production/test compilation succeeded.
- Harness `:barrows-themes:jar`: passed. The harness JAR now contains the unfinished experiment; do not recommend reloading it as a verified fix.
- Latest `WallJoinReplay CACHE_COPY --surface`: all 12 front-surface proximity cases pass the 24-local-unit limit, but **two of eight neighbouring joins fail** their original-gap-plus-8-unit allowance.
- Last fully green pre-experiment state: **124 tests**, plugin/harness builds passed; its in-game **v2** rotations were confirmed loaded but **gaps and zigzags persisted**.
- The new **v3** surface-fitting implementation has **not been tested in-game**.

Failing tests in the checkpoint:

1. `WallModelReplacementsTest.cornerNativeFacingOffsetsComposeWithAllFourPlacementRotations`
2. `WallModelReplacementsTest.coxSurfaceCoordinatesComposeOnceWithEveryWallMaskAndCryptPalette`
3. `WallModelReplacementsTest.coxFrontSurfaceMustMatchOriginalEdgeDespiteIdenticalWholeMeshBounds`
4. `WallSurfaceFitterTest.triangularCornerPostUsesItsActualSkinNotTheUndergroundTileBacking`
5. `WallSurfaceFitterTest.unbendsNativeCornerToOriginalDiagonalForAllFourOrientationsWithoutReversingFaces`

Several assertions predate the later eight-unit endpoint overlap, shared grid anchors, relief taper and radius changes. Diagnose each failure: do not merely replace expected numbers until the replay/geometry contract is sound. The edge test initially had a Mockito restubbing artefact (calling the old `shallowCopy()` answer during restubbing); it now uses `doAnswer(...).when(data).shallowCopy()` correctly.

## Workspace and constraints

- Owning repository: `barrows-themes/` in `/Users/maeve/dev/runelite-plugins`.
- Shared independent harness: `runelite-plugin-dev-client/`; registered reload ID `barrows-themes`.
- Use JDK 21 to run Gradle; plugin bytecode stays Java 11.
- Public live-client APIs only in production. No reflection, on-disk cache access, subprocesses, input injection or dynamic loading in the plugin.
- Offline research lives in `tools/`, outside `src/`; reflective interface adapters in `WallJoinReplay.java` are **development-only**, never packaged in the plugin.
- Preserve original objects, collision, combat, picking and interactions. Visual replacement must clean up fully on disable/logout/reload.
- Preserve the entire NPC-definition pool; no filtering or rerolling buggy/static choices.
- Scenery-model swaps remain parked. Native wall assets are required for themes; never use unrelated fallback meshes.
- Builds/replays do not prove game rendering. Public replies require exact-text approval; no public replies or push are requested here.

## Rotation/orientation: the important distinction

RuneLite `GameObject` exposes two different orientations:

- `getOrientation()`: **logical object/placement orientation**. It is not the extra rotation to apply to an already-oriented model.
- `getModelOrientation()`: actual **model render orientation**, usually **zero for static objects**, because placement rotation is baked into their vertices before lighting.

Source: `runelite/runelite-api/src/main/java/net/runelite/api/GameObject.java`, particularly the Javadoc of `getModelOrientation()`.

The initial code rotated the detached replacement to the original baked placement, then called `RuneLiteObject.setOrientation(owner.getOrientation())`. For type-9 game objects this rotated it **twice**. The corrected production code uses `owner.getModelOrientation()` instead. Keep that correction; do not blanket-force zero either—genuine nonzero model render orientations must be preserved.

Other values:

- Config placement quarter-turn: `(config >>> 6 & 3) * 512`.
- Wall masks `1/2/4/8` and `16/32/64/128` map to `0/512/1024/1536` respectively.
- `WallModelSources.extraRotation(...)` is a source-specific native-facing adjustment, composed with baked placement rotation modulo 2048; it is not a substitute for render orientation.
- `WallModelFitter.fit(...)` rotates using `x'=x*cos+z*sin`, `z'=z*cos-x*sin`, then independently scales each axis to the original full vertex bounds.
- `SceneInspector` now labels object and model-render orientation separately.
- Short report v1 incorrectly called object orientation render rotation. v2 fixed that; v3 additionally reports fitting strategy.

The user verified v2 showed type-9 `placementRot=1536, bakedRot=1536, renderRot=0, objectRot=1536`, yet visible gaps remained. Thus the API rotation bug was real, but not the complete wall-alignment cause.

## Placement types were misleadingly called “corners”

For the Barrows assets involved:

| Type | Actual role | Original model(s) |
|---|---|---|
| 0 | Straight wall | 6618 |
| 1 | Small triangular diagonal-corner **post**, not a full diagonal wall run | 6619 |
| 9 | Full curved **diagonal wall** (main/inner variants), not simply an orthogonal L corner | 6620 / 6621 |

Main brown/purple object IDs are `20728/20731`; inner are `20729/20732`. Use gameval constants in production.

CoX sources:

- Straight: `32435`, `32437`.
- Type-1 source: `32439`.
- Type-9 source: `32432`.

The original type-9 skins run diagonally across the cell; CoX `32432` has an L-like native cross-section. Whole-mesh boxes are similar while the occupied surfaces differ. Some Barrows meshes include backing geometry down to approximately **y=+160**; full vertex bounds do not describe the visible wall skin. Original straight skins sit near the cell edge, while unadjusted CoX skins occupy a different depth inside the same box.

## The diagonal half-turn was a failed attempt

`CornerFacing.java` compared normalized, unsigned horizontal slices of `6619` and `32439`. It favoured 1024, with scores `1910.47 / 1210.92 / 542.22 / 1218.31` at four quarter-turns. We applied it in v2.

**Live review still failed.** The later replay checks face winding; the half-turn reversed the CoX diagonal's visible front. The checkpoint **undoes that half-turn**: CoX `WallSet` is back to diagonal/main/inner native offsets **0/0/0**. Do not reintroduce 1024 based on the unsigned distance score. Historical research text elsewhere describes that failed intermediate experiment; this handoff supersedes it.

Upper-face normal evidence from the cache:

- CoX `32435`: principally **-X**.
- CoX `32439`, `32432`: principally **-X,+Z**.
- Barrows `6620/6621`: likewise principally **-X,+Z**.
- `6619` has both backing-box faces and a small diagonal front; including all backing faces in a distance heuristic is misleading.

## Current unfinished surface fitter

New file: `src/main/java/com/barrowsthemes/WallSurfaceFitter.java`.

It is currently **wired into production for CoX crypt walls only**, including straights. Other themes and parked scenery retain `WallModelFitter` bounding-box fitting.

Current algorithm:

1. Read public vertex/face-index arrays; bound vertices/faces to 8192 and reject malformed data.
2. Unrotate the original baked mesh into canonical placement coordinates.
3. Select front-facing, mostly vertical faces, respecting intrinsic `Model.faceColors3 == -2` exclusions.
4. Build 9 height × 9 progress samples of the skin and a roof envelope. Progress/depth are `u=z,v=-x` for straight walls, or `u=(x+z)/2,v=(z-x)/2` for type 1/9.
5. Conform source progress/depth to the target skin; target wall floor is capped at y=0 rather than fitting underground backing down to +160.
6. Preserve limited local source relief, taper it at joins, and use an **eight-local-unit (1/16 tile) endpoint overlap**.
7. Use common cell-corner anchors: full diagonals run between canonical ±64 endpoints; type-1 posts sit at the canonical NW corner. Type-0/9 progress and end depths blend toward those shared anchors.
8. CoX native cell-edge vertices get endpoint fractions; vertices on both source edge conditions get fraction .5 to avoid assigning an ambiguous backing-corner vertex to one end. This last change did **not** fix the remaining replay joins.

Be cautious: this is nonlinear deformation of a native mesh, not just a pose change. It can fold/flatten faces or change roof/doorway clearance; preserve winding and evaluate the rendered shape. It is not established that this is the best final design.

Additional integration changes:

- `WallModelReplacements.fitted(...)` now receives theme, selects the CoX surface fitter, and keeps originals when fitting fails; no legacy-box fallback for failed CoX profiles.
- Fitted cache keys are now `long`, including source, baked rotation, placement type and fitting strategy. Type/strategy matter now; the old key was only source and rotation.
- Visual-object radius now takes the maximum of original and replacement bounds; fitting may extend slightly beyond the original box. The existing 256-unit safety limit remains.
- Short report is now **v3**, with `fit=front-surface profile` or `fit=bounds`.
- No original vertex/face-index arrays are modified; existing atomic face-array staging, excluded-scenery protection and lifecycle cleanup remain.

## User reproduction and screenshots

Theme: **Chambers of Xeric**, Karil's tomb, live/template plane **3**.

First on/off pair (before v2 corrections):

- On: `/Users/maeve/.runelite/screenshots/Spiky_Melon/2026-10-04_21-22-03.png`
- Off: `/Users/maeve/.runelite/screenshots/Spiky_Melon/2026-10-04_21-22-50.png`

v2, still gaps/zigzags:

- Corner near player `3555,9685,3`: `/Users/maeve/.runelite/screenshots/Spiky_Melon/2026-10-04_21-38-01.png`
- Opposite corner near `3546,9681,3`: `/Users/maeve/.runelite/screenshots/Spiky_Melon/2026-10-04_21-38-10.png`

First corner placements (all placement rotation 1536, actual model render rotation 0):

- Inner `20732 @ 3555,9686`, model 6621 / source 32432, height -2642, live y -256..18.
- Main `20731 @ 3556,9685`, 6620 / 32432, height -2630, y -258..6.
- Posts `20731 @ 3555,9687`, 6619 / 32439, height -2652, y -248..164; and `3556,9686`, height -2650, y -270..162.
- Main `3554,9687`, height -2658, y -251..10; post `3554,9688`, height -2662, y -250..166.

Opposite (placement rotation 512, actual render rotation 0):

- Main `20731 @ 3546,9680`, height -2774, y -252..6.
- Posts `20731 @ 3545,9680`, height -2772, y -256..172; and `3546,9679`, height -2766, y -253..174.
- Inner `20732 @ 3547,9679`, height -2766, y -246..1.

Do not ask for all the same reports again. The user has supplied a clear failing example and both viewing directions.

## Offline replay: the useful feedback loop

New development-only tool: `tools/WallJoinReplay.java`.

It loads original/source meshes from a **private cache copy**, reconstructs the recorded rotations, approximates live terrain-contoured y coordinates by scaling native y to the reported bounds, calls production fitting code through offline public-interface adapters, then checks front-facing horizontal sections at three heights. It has ten reported cases, two straight-source controls and eight neighbouring joins.

Limitations: this is **not exact captured live geometry**, a renderer or GPU-culling replay. Terrain deformation is approximated and the adapter does not reproduce scene-merged hidden faces. Bounds/skin proximity and a numerical join allowance cannot establish visual success.

Commands (set `CACHE_CP` to the cache-library runtime classpath and `API_CP` to RuneLite API JAR):

```sh
mkdir -p /tmp/barrows-join-classes
javac -cp "$CACHE_CP:$API_CP" -d /tmp/barrows-join-classes \
  tools/WallJoinReplay.java \
  src/main/java/com/barrowsthemes/WallModelFitter.java \
  src/main/java/com/barrowsthemes/WallSurfaceFitter.java
java -cp "$CACHE_CP:$API_CP:/tmp/barrows-join-classes" \
  com.barrowsthemes.WallJoinReplay /path/to/cache-copy           # old v2 box fit; expected RED
java -cp "$CACHE_CP:$API_CP:/tmp/barrows-join-classes" \
  com.barrowsthemes.WallJoinReplay /path/to/cache-copy --surface # current WIP; still RED
```

Latest surface output: maximum front misses approximately **12.6–21.3 units**; straight controls **17.8/18.5**. Remaining failed joins:

- `0/2`: inner `3555,9686` → post `3555,9687`: original gap **1.07**, fitted **13.53**.
- `7/6`: inner `3547,9679` → post `3546,9679`: original **1.06**, fitted **10.55**.

Both remaining failures involve **inner** diagonal walls meeting posts. Several main/post joins now measure zero. Check which visible front faces, endpoint mapping, nonlinear roof mapping or winding causes the inner asymmetry; do not keep globally tuning rotation/overlap.

The baseline v2 replay failed all first six reported cases: corner misses about **46–61 units**, and the half-turned posts had no correctly facing front section. An early test used the wrong front normal (+X,-Z); it was corrected to **-X,+Z** for these diagonal skins before these measurements. Current replay considers all front-facing section triangles, not only strictly vertical ones; that change did not alter the latest results.

Cache idx2 fingerprint remains `8eea5056e4005bdf40483f6f0037f3da82c78a92f762f769165068c67a8e8cff`. The private `/tmp/barrows-wall-joins` copy and debug profile images/classes were removed for this checkpoint. Regenerate from a copied cache; never read a running client's live cache in a research tool. `/tmp/barrows-material-study/classpath` may still supply local library paths, but do not rely on it being portable. No cache assets, keys or screenshots are committed.

## Other plugin work already present

- Nine explicit native wall sets: Zanaris, CoX, Inferno, Prifddinas, Ancient Pyramid, Darkmeyer, northern polar-cave adaptation for Fremennik ice caves, Dorgesh-Kaan, Abyss.
- Source recolours, ambient/contrast offsets, stable straight variants, native meshes separate from artistic role palettes; unsupported/protected scenery stays original.
- Crypt scenery replacement configuration removed; production forces scenery=false.
- Uniform once-per-spawn random NPC definition from all 16,576 available IDs, current morph resolution, missing/model-less fallback without reroll.
- Native gait playback and conservative Native only / Auto / Force brother actions, classic sequence/frame-map evidence cache, reviewed per-action overrides, explicit native attacks for five families/14 IDs.
- Optional bounded, session-only NPC action recording; no upload/persistence or automatic attack classification.
- Original brother remains for combat/picking; late GPU geometry filtering preserves clickboxes.
- Full offline movement decoder audit had zero mismatches in 15 fields; 1,830 definitions lack movement metadata.
- User's static Karil disguise `15343→15345` is Ghost Jenkins' ship/no-op variant with no gaits; not a demonstrated decoder failure. Pool remains unfiltered.
- Live observations corroborated brother actions Verac 2062, Torag 2068, Guthan 2080, Karil 2075. Bloodworm attack 2070 and skeleton attack 5485 were identified as potential future native mappings, **not implemented** in this checkpoint.

## Suggested next steps

1. Read this handoff and inspect the two remaining replay joins; tighten the repro rather than trusting box bounds or unsigned rotation scores.
2. Decide whether to complete the surface fitter or park it outside production and return to a green checkpoint. Keep the proven `getModelOrientation()` fix either way.
3. Reconcile the five failed tests with the intended anchor/overlap/radius contract; keep meaningful assertions for actual skins, winding, all four placements and cleanup.
4. Rerun replay, focused tests, plugin build and harness JAR. Only then ask for a v3 screenshot comparison at the supplied corners; explicitly report what remains unverified.
5. Review door clearance, roof cutaways, both planes, all orientations and source variation. Do not extend this nonlinear fitting to other themes without comparable geometry evidence.

Build commands:

```sh
cd /Users/maeve/dev/runelite-plugins/barrows-themes
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build
cd ../runelite-plugin-dev-client
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :barrows-themes:jar
```
