# Offline material study (development only)

`Study.java` is a standalone research tool, outside `src/` and never included in the Plugin Hub JAR. It depends on RuneLite's cache library and its normal runtime dependencies, not the live client. The production plugin does not open/decode on-disk cache files, read XTEA keys, run processes or dynamically load code. Its NPC/sequence/frame-map metadata decoders only consume bytes provided by public live-client cache-index APIs.

Always point it at a **copy** of the Jagex cache, not a cache used by a running client. Keep cache contents and region keys outside this repository. The supplied study expects the local RuneLite `xtea.json` format: a JSON object mapping decimal region IDs to four integers. Do not publish that file.

With JDK 21 and `CACHE_CP` set to a classpath containing the RuneLite cache JAR and its runtime dependencies (Guava, Gson, SLF4J, Commons Compress/IO/Lang, and any dependencies required by your cache-library revision):

```sh
mkdir -p /tmp/barrows-study-classes
javac -cp "$CACHE_CP" -d /tmp/barrows-study-classes tools/Study.java
java -cp "$CACHE_CP:/tmp/barrows-study-classes" Study \
  /path/to/cache-copy /path/to/xtea.json \
  14231,9540,9541,9797,9043,13137,13138 > /tmp/barrows-material-report.json
```

The JSON report contains region floor definitions and placed object IDs, model references, post-definition material colours/textures, placement types/planes and missing-model lists. No keys are printed. Check both region errors and missing-model lists before drawing conclusions; an absent texture is not proof of an untextured model when model data is unavailable. This tool does not resolve every morph child, model placement-type choice, terrain blending or scene lighting; curated role matching is deliberately approximate.

## Wall model metadata

`WallModels.java` prints selected object model/type lists, recolour/retexture tables, ambient/contrast lighting, textured-face counts, mirroring flags, animation IDs, footprints, model scale and model bounds. It needs no XTEA keys and exports no meshes. Compile with the same cache classpath:

```sh
mkdir -p /tmp/barrows-study-classes
javac -cp "$CACHE_CP" -d /tmp/barrows-study-classes tools/WallModels.java
java -cp "$CACHE_CP:/tmp/barrows-study-classes" WallModels /path/to/cache-copy \
  20728 20729 20730 20731 20732 20764 12007 12009 29781 30319 30325
```

## Corner native-facing comparison

`CornerFacing.java` compares normalized horizontal mesh slices against a target model at all four quarter-turns. It needs no keys and exports no meshes. Lower distance scores suggest closer surfaces; the heuristic cannot establish correct live corner placement, culling or doorway clearances.

```sh
javac -cp "$CACHE_CP" -d /tmp/barrows-study-classes tools/CornerFacing.java
java -cp "$CACHE_CP:/tmp/barrows-study-classes" CornerFacing /path/to/cache-copy \
  6620 11890 32432 33063 33074
java -cp "$CACHE_CP:/tmp/barrows-study-classes" CornerFacing /path/to/cache-copy \
  6621 11890 32432 33063 33074
# CoX diagonal native-facing comparison (best sampled quarter-turn: 1024)
java -cp "$CACHE_CP:/tmp/barrows-study-classes" CornerFacing /path/to/cache-copy \
  6619 32439
```

## Wall-join replay (unfinished investigation)

`WallJoinReplay.java` checks captured Karil wall sections and neighbouring joins against production fitters using a private cache copy, no keys. Its reflective adapters implement public mesh interfaces **offline only**; it is outside `src/` and never packaged in the plugin. It approximates live y contouring from reported bounds, not an exact live mesh or GPU renderer replay. Default mode reconstructs the failed v2 box fit; `--surface` exercises the current experiment, which still fails two joins. Commands, failure evidence and next steps: [../docs/agent-handoff.md](../docs/agent-handoff.md#offline-replay-the-useful-feedback-loop).

## NPC movement decoder audit

`NpcAnimationAudit.java` compares the production decoder's 15 movement fields against RuneLite's cache `NpcLoader` for every definition. Optional IDs print definition/model/gait summaries, not meshes. Add the public RuneLite API JAR as `API_CP`:

```sh
javac -cp "$CACHE_CP:$API_CP" -d /tmp/barrows-study-classes \
  tools/NpcAnimationAudit.java src/main/java/com/barrowsthemes/NativeNpcAnimations.java
java -cp "$CACHE_CP:$API_CP:/tmp/barrows-study-classes" \
  com.barrowsthemes.NpcAnimationAudit /path/to/cache-copy \
  1173 1174 2790 2791 2793 2804 2805 2806 2827 2834 2838 2839 2856 2859
```

## Native attack layout corroboration

`NativeAttackStudy.java` compares all classic frame-map layouts used by each supplied idle/attack pair. It reports missing assets as errors instead of concluding compatibility. Matching layouts corroborate a named family mapping; they do not establish attack classification or visual correctness.

```sh
javac -cp "$CACHE_CP" -d /tmp/barrows-study-classes tools/NativeAttackStudy.java
java -cp "$CACHE_CP:/tmp/barrows-study-classes" NativeAttackStudy /path/to/cache-copy \
  5386 5387 5852 5849 4914 4915 4919 4925 4932 4933
```

NPC findings, mappings and limitations: [`../docs/npc-animation-research.md`](../docs/npc-animation-research.md).

Findings and chosen wall mappings: [`../docs/material-research.md`](../docs/material-research.md) and [`../docs/theme-models.md`](../docs/theme-models.md). New-theme metadata IDs: `36247 36248 6539 6540 61518 61520 19693 22898 22906 22902 26150 26153`. New corner models: `37500 6247 60823 9965 23760 7415`; compare each against both `6620` and `6621`.
