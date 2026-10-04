# Barrows Themes

> **Experimental:** CoX fitting passes automated geometry checks, and the user reports improved corners. Comprehensive in-game checks remain, especially roof/door clearance and the other themes. Continuation notes: [docs/agent-handoff.md](docs/agent-handoff.md).

Experimental RuneLite plugin targeting **RuneLite GPU**, with three themes for underground Barrows floors and static scenery: **Zanaris**, **Chambers of Xeric** and **Inferno**. **Theme material palettes** (default on) maps stone/detail roles using cache samples, not authentic image-texture presets. Optional wall-model replacement supplies each theme's native cache geometry; floors and furniture retain material treatment. Crypt scenery-model replacement is parked. Palette details: [docs/theme-palettes.md](docs/theme-palettes.md); cache evidence: [docs/material-research.md](docs/material-research.md).

## Current scope

- Targets underground region `14231`, including any brother tombs in that region, not surface Barrows.
- Resolves instance-template coordinates rather than assuming physical instance coordinates match the map.
- Recolours floors and static `WallObject` models, plus explicitly identified Barrows crypt scenery in game-object, decorative-object and ground-object slots: corners, brick/stone piles, rock pieces, dug-up soil ground decorations, all six sarcophagi and brother-tomb stairs. Coffin/stair actions are preserved; unrelated interactive objects remain excluded. Doors, chest, layout and collision are not intentionally changed. NPC visuals remain original unless random brother models are enabled.
- Optional model replacement covers eligible static straight, diagonal and corner crypt walls in tunnels and tombs, with stable middle-wall variants. Small crypt steps, rubble, rocks and soil keep their original geometry; their former model-replacement toggle has been removed and saved settings are ignored. Original scene objects, collision and actions remain intact. Interactive furniture, doors, chest and unsupported/shared models retain the material treatment.
- Invalidates changed GPU zones so their geometry can be uploaded again.
- Restores captured colours/textures on configuration changes, scene transitions and shutdown.
- Optional **Floor texture ID** and **Scenery texture ID** overrides use existing game textures. `-1` (default), out-of-range IDs and missing textures use recolouring. Scenery replacements affect only faces that originally had a texture, retaining their mapping and lighting. Untextured faces keep the palette.
- A **Barrows textures** sidebar lets you preview and select game textures without guessing IDs. Previews are detached copies; shared game texture pixels are never replaced globally.
- **Inspect nearby original scenery** reports nearby object IDs, placement/orientation metadata, static/dynamic renderables, selection eligibility, shared-model exclusions and original material/texture usage. It also reports selected/resolved random NPC IDs and animation/fallback status. Select/copy the report text to share it; nothing is uploaded.

Not yet implemented: exact scenery reproduction, attaching texture arrays to untextured models, furniture/floor model replacement, lighting/fog, tunnel-only boundaries, 117 HD compatibility. Triangle floor models without a texture array remain recoloured because the public API cannot attach that array. Animated/dynamic scenery is still skipped, including any coffin/stair variant represented by a dynamic object rather than a static model. Models visibly shared with excluded scenery are skipped to reduce shared-model side effects.

## Build and run

Requires a JDK capable of running Gradle; emits Java 11 bytecode.

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew clean build
```

Registered in the sibling development harness:

```sh
cd ../runelite-plugin-dev-client
./gradlew :barrows-themes:jar
# In a running harness terminal:
# reload barrows-themes
```

Enable **Barrows Themes** and RuneLite **GPU** in the client.

## Reviewing curated materials

1. Select a theme and enable **Theme material palettes** (default).
2. In the **Barrows textures** sidebar, click **Clear both texture overrides** if you previously chose manual textures.
3. Review walls, floor details, stones, coffins and stairs in the tunnels and all tombs. Zanaris, CoX and Inferno retain their sampled mappings. Saved selections of removed themes reset to Zanaris on startup.
4. Leave model replacement off and toggle curated materials off to compare against the old palette. Geometry, collision and actions remain unchanged in this material-only comparison.
5. For an unchanged piece, stand nearby and click **Inspect nearby original scenery**. The report shows original materials, not the plugin's modified colours. Dynamic renderables remain skipped and are not forced into static models.

The inspected target walls and Barrows walls are coloured geometry rather than image-textured walls. Material palettes use sampled role colours instead of inventing authentic texture IDs. See the research notes for the precise choices and remaining renderer/API limitations.

## Trying wall models

1. Reload the rebuilt plugin, enable **Walls / scenery**, and enable **Replace wall models (experimental)** (off by default). All three themes have their own wall-model sets.
2. Select Zanaris, Chambers of Xeric or Inferno. Zanaris uses mushroom straight walls and native diagonal/corner pieces; CoX uses its matching cave-wall shapes; Inferno uses rock edges/corners, with rotated edges for diagonals. **Vary middle wall models** (default on) selects stable alternate straight-wall models by instance-template position: four Zanaris mushroom variants, two CoX walls and six Inferno edges. Disable it for the original single straight model.
3. Native source colours and ambient offsets are retained; wall lighting uses a doubled contrast divisor to soften overly dark directional shading. Straight walls, posts and corners share this policy, with no changes to geometry or rotations. These are adapted meshes, not exact area reconstructions. Sources and caveats: [docs/theme-models.md](docs/theme-models.md).
4. Check straight walls, posts, inside/outside corners, doorway clearances and roof cutaways in the lower tunnels and every brother tomb. CoX uses front-surface fitting and shared endpoint anchors; Zanaris and Inferno retain bounds fitting. Placement/height and actual model render orientation are preserved, but this is not an exact copy of the original wall surfaces.
5. Check FPS and for missing walls. These are dynamic render objects, so rendering cost and temporary scene-object limits may differ from static walls. Disable the setting to restore originals if coverage is poor.
6. Switch themes, leave Barrows, log out/in, disable/re-enable and reload the plugin. Check that no replacement geometry remains and original walls return.
7. For a bad corner, stand beside it and click **Short wall-corner report**. This separate, read-only snapshot lists at most 12 nearby static corner/diagonal parts within four tiles, with theme, template coordinates, source model, placement/extra/baked/render rotations, wall masks, height, registration status and original/source/fitted bounds. Report v3 distinguishes object rotation from actual model render rotation and labels the fitting strategy. It works with replacement off, does not load assets or rebuild walls, and omits the long scenery/NPC listing. Copy it with a screenshot and compare replacement on/off; missing cached bounds are labelled unavailable.
8. Use **Inspect nearby original scenery** for the number of registered parts and assets awaiting cache. Registration counts do not prove those parts rendered correctly.

A wall's original faces are hidden only after replacements for all visible static references sharing its face-colour array are staged and activated. Arrays shared with excluded static scenery are not hidden; animated renderables remain skipped. Missing assets are retried through the client cache API. Replacement objects are removed before the captured original material arrays are restored.

These source models are also coloured geometry, not image-textured walls. The existing texture overrides cannot add image textures to their untextured faces. Other scenery continues to use the material theme and texture overrides where supported. The experimental crypt scenery-model swaps have been parked following in-game feedback; their toggle is removed and previously saved values are ignored.

Corner wall facings now distinguish main/inner pieces and apply source-specific native-facing offsets. These were calibrated against offline mesh slices, not verified against the particular misoriented corners reported in-game. The inspector now includes template position, placement type, config rotation and wall/render orientation; share those details for remaining mismatches.

## Trying random brother models

1. Reload the plugin and enable **Random brother models (experimental)** (off by default). This is independent of the theme and wall/floor toggles.
2. Each of the six Barrows brothers gets one random disguise when it spawns underground, in tombs or tunnels. Enabling the setting also disguises brothers already present. Choices are sampled uniformly from **every NPC definition ID available in the client's config archive**, not a curated pool or a guessed maximum ID. No size, name, combat, rig or debug-NPC filters are applied; repeats, enormous models and buggy disguises are possible. Definitions with no loadable model keep the original brother visible without rerolling.
3. Disguises follow the original NPC's position, height and facing. **Native animation sequences come from the selected NPC definition**, including idle, walk, run and directional turns where available. The brother's movement/pose state chooses the native gait, which runs on its own playback clock. A small source-backed native-attack table covers five animal families; other definitions keep native gait/idle or stay static unless Auto/Force or a reviewed override supplies an action. NPC definitions have no universal attack/hit/death field. The original NPC and its combat, name, menus, collision and overhead UI are retained; the replacement is visual-only. Large/small disguises do not redefine the original interaction footprint. Check clicking/targeting and overhead alignment in-game.
4. A missing cache model or failed registration leaves the original visible and retries the same choice. Theme/texture changes and scenery inspection do not reroll a ready disguise. A new spawn makes a fresh independent choice; disable/re-enable or a scene reload also discards current disguises.
5. Check all six brothers, repeated spawns, moving/facing, attacks/deaths, tunnel encounters and tombs. Check native idle/movement transitions and looping, and watch for distorted or giant disguise geometry. Disable the setting, leave Barrows, log out/in and reload the plugin; check that disguises disappear and originals return. Model rendering, interaction alignment and cleanup still require in-game verification.

6. Choose **NPC animation mode**: **Native only** (default) uses native gait/mapped attacks; **Auto** prefers native attacks and otherwise borrows only with classic frame-map evidence or a reviewed allow; **Force brother actions** tries the real brother's action/frame even on unknown rigs, including hit/death actions. Unknown/Maya/weighted combinations are not automatically borrowed. Matching idle/walk IDs alone is only a diagnostic hint. An old enabled attack checkbox migrates once to Force. Settings changes preserve the selected disguise, registration and gait clock.
7. **Mapped native NPC attacks** (default on) covers explicitly listed chickens, cows, bats, bears and giant rats with cache-checked current gait families. Attacks use their own timing, not the brother's frame number. Unknown IDs remain in the random pool and use gait/static fallback. This is not universal attack discovery; the mappings still need in-game verification.
8. **NPC action overrides** supports reviewed rules such as `1173:2075=5387; 42:*=deny; 42:2067=allow`: resolved disguise ID, actual brother action, then native sequence or borrow allow/deny. `42` is a syntax example, not a verified classification. Exact actions beat wildcards; invalid entries are counted in the inspector. Native-only never borrows, and deny rules apply even in Force. Turning mapped native attacks off also disables numeric native overrides.
9. For static/scenery-like or distorted disguises, copy the inspector's random-brother section: selected/resolved IDs, source model IDs, gait metadata, mode, compatibility evidence and detailed playback/fallback reasons. Sequence/frame status does not prove visible movement. A full cache audit found no decoder mismatches in 16,576 definitions, of which 1,830 had no native movement metadata; affected live IDs are still needed to diagnose particular cases.
10. Optionally enable **Record NPC actions**, observe ordinary real NPC combat (also outside Barrows), then copy the inspector's action section. It retains at most 256 combinations for this session, clears on logout/disable and uploads/saves nothing. Observed action changes and target presence do not classify attacks automatically. Review attacks vs defence/death/specials before adding overrides. Findings, exact mappings and caveats: [docs/npc-animation-research.md](docs/npc-animation-research.md).

Models are loaded through the public client cache API, with detached vertices/colours, definition recolours and scaling. Original NPC definitions and model arrays are never edited. A GPU draw callback suppresses only a successfully registered disguised brother's geometry upload, after mouse picking. The original NPC remains admitted to the scene with its original clickbox and overhead UI; entity-hiding callbacks must not be used here because rejecting scene admission removes clickboxes. Animation sequence metadata is read through the public config-index API using a bounded, best-effort decoder of known NPC-definition tags. Unknown tags stop parsing rather than scanning their payload as animation IDs; malformed or missing metadata safely leaves a static pose. Morph definitions use the resolved child's models and animation metadata. The public animation API transforms a temporary copy of the detached base model; animated results are consumed immediately rather than cached. Missing metadata/animation assets retry at most once per game tick. Invalid frames, null results or transformation errors fall back to the static pose; a sequence that throws is suppressed for that disguise's lifetime to avoid repeated render errors. Native gaits loop without despawning the disguise; mapped attacks play once per recognized action cycle. Classic compatibility metadata is decoded through public cache indexes outside rendering with bounded caches and a shared per-game-tick read budget. Matching frame-map layouts is evidence, not a model-skin/anatomy guarantee. Source-specific NPC lighting/retexture tables and universal native attack mappings are not provided by this prototype.

## Trying textures

1. Enable Barrows Themes, log in and open the **Barrows textures** sidebar (brick-pattern icon).
2. Click **Refresh game textures**. Select **Floors** or **Walls / scenery**, then click a thumbnail. Its game texture ID is saved to the corresponding override setting.
3. Enter Barrows and inspect the result. Enable the corresponding floor/scenery toggle if it is disabled.
4. Select **Use theme materials (no override)** to clear the selected target's override. Texture overrides are independent of the theme dropdown.

The browser lists available cache textures, not labelled Zanaris/CoX/Inferno materials. These are separate manual overrides, not the curated cache-colour mappings. Some scenery may remain coloured because it never had texture mapping in the original model.

## Visual follow-ups

Current work, parked scenery-model experiments and NPC animation investigations are tracked in [docs/follow-ups.md](docs/follow-ups.md).

## In-game verification needed

1. Visit the lower tunnels and chest room; confirm floors and static walls change under GPU.
2. Walk through doors and between chambers. Confirm doors, puzzles and chest still look/work normally, and NPCs retain their original appearance with random brother models disabled.
3. Visit each brother tomb and check coverage; establish whether a tunnel-only option needs coordinate/object-based boundaries.
4. Switch among all three themes and inspect corner pieces; confirm each theme replaces the previous wall set, and disabling replacement restores originals. Toggle floors/walls separately, disable/re-enable the plugin and reload it. Verify original materials return, including under GPU.
5. Leave Barrows, teleport, log out/in and toggle GPU. Check for stale colours, missing faces or unrelated scenery changes.
6. Try floor/scenery textures via the browser. Check mixed textured/coloured faces, corner pieces, stones, coffins and stairs, including their interactions. Clear overrides and disable/reload the plugin to verify original materials return.
7. Review material brightness/detail balance in all three themes. Check the newly selected soil/rubble details in the brother tombs and use the inspector for remaining unmatched objects.

The user reports the original Zanaris recolouring works in-game, with some corners missed. The curated mappings, expanded scenery coverage, inspector, texture browser, texture overrides and new wall-model replacements still need in-game verification. Build/tests do not verify rendering or gameplay.
