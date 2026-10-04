# Visual follow-ups

**Current WIP:** v2 rotations did not resolve CoX gaps. The v3 surface fitter is an unfinished experiment with five failing tests and two failing replay joins. Continue from [agent-handoff.md](agent-handoff.md); the older status below is historical where it describes the CoX half-turn.

Current status: nine material themes, optional cache-backed wall models for all nine themes, and random NPC disguises. Crypt scenery-model swaps are parked following the user's in-game feedback. New palettes, animation options and diagnostics have automated coverage but still need in-game review.

## Additional themes

Implemented: Prifddinas, Ancient Pyramid, Darkmeyer, Fremennik ice caves, Dorgesh-Kaan and Abyss. These six have artistic material palettes plus explicit cache-backed wall meshes, source recolours/lighting and calibrated corner offsets. Polar cave geometry is an adaptation for the ice theme; pyramid diagonals reuse a rotated wall. See [theme-palettes.md](theme-palettes.md) and [theme-models.md](theme-models.md).

Future themes are model-first: researched wall assets are required, not recolour-only presets or unrelated model fallbacks. The all-theme mapping regression test enforces explicit shape coverage.

Remaining: review all nine wall sets and palettes in tunnels/tombs, especially seams, corners, doorway clearance, roof cutaways, native lighting and FPS.

## Middle-wall variation

Implemented: **Vary middle wall models** (default on when wall replacement is used). Stable instance-template-position choices use four Zanaris mushroom walls, two CoX walls and six Inferno rock edges. Disabling the setting returns to the original single straight-wall source. Diagonals are not randomized.

Remaining: compare long wall runs, transitions, both crypt planes and all themes. Check for seams and repetitive patterns; position-stable variation must not reroll on ticks or inspector rebuilds.

## Corner facing

Implemented: source-specific native-facing adjustments for main/inner corner walls, calibrated against offline horizontal mesh slices. Zanaris main corners gain a half-turn, inner corners retain their facing; Inferno uses a different main-corner asset and quarter-turn offsets; CoX corners retain their sampled best facing; its diagonals now receive a cache-compared half-turn. All four placement rotations have regression coverage. The actual **model** render orientation is retained (`getModelOrientation()`), not the object orientation that incorrectly applied placement rotation a second time.

The user's CoX Karil on/off comparison showed large gaps and zigzagging sections. It led to the corrected object/model rotation distinction and source-specific diagonal half-turn; report v2 now separates object and render rotations. Remaining: recheck that same wall in-game. These are tested transform corrections, not yet confirmed fixes for all visible seams. The inspector now includes template coordinates, placement type/config rotation, wall orientation masks and game-object render orientation. For remaining mismatches, click the dedicated **Short wall-corner report** button beside the affected corner and send it with a screenshot, ideally comparing replacement on/off. It lists only the nearest static corners/diagonals (four tiles, at most 12 parts), including source model, placement/extra/baked/render rotations, wall masks, height, registration state and original/source/fitted bounds. It is read-only, works with replacement disabled and never loads assets or rebuilds the scene. Bounds/facing metadata do not establish correct alignment.

## Non-interactable small crypt steps

Parked: the former **Replace crypt scenery models (experimental)** covered `BARROWS_SKEWSTEPS`, `BARROWS_SKEWSTEPS_CORNER` and both purple variants. Sources are mushroom-ring edging, fitted CoX stairs/low corner rock, and Inferno low ledges/corners.

The toggle is removed and saved values are ignored. Small steps now retain their geometry and existing material treatment. Internal experimental mappings/tests are retained for possible future research; no scenery-model quality claims are made.

## Theme-specific scenery

Parked: the former scenery-model toggle replaced explicitly allowlisted static brick piles, soil, rocks and rockslides using fairy-area mushrooms/mossy ground rocks, CoX floor rocks and Inferno rubble. Sources have definition-specific recolours/lighting and position-stable variation where appropriate.

The user reported these models did not look good. Production scenery-model selection is disabled, including for previously enabled settings. Material/texture edits remain available; the wall-model feature is separate. No further scenery-model expansion is planned for now.

## Subsequent NPC requests

Implemented: choose uniformly from every available NPC-definition ID in the public client config index, without size/name/rig filters. Missing/model-less choices retain the original without rerolling. Current morph children are resolved where applicable.

Implemented: native idle/walk/run/crawl/turn metadata and independent gait timing. **Native only / Auto / Force brother actions** replace the old action checkbox, with one-time migration of saved enabled values to Force. Auto prefers mapped native attacks, otherwise requires full classic frame-map layout evidence or a reviewed allow. Matching gait IDs alone never authorize borrowing. Unknown/Maya/weighted combinations keep native gait/static fallback. Mode/override changes preserve selection, registration and gait clock.

Implemented: 14 explicit NPC definitions across five native-attack families (chickens, cows, bats, bears, giant rats), guarded by current cache-checked gaits and corroborated by full classic idle/attack layout matches. Attacks use their own clock and recognized brother attack cycles. Numeric native overrides and per-ID/action borrow allow/deny rules support reviewed additional cases; no automatic attack discovery or filtering of the NPC pool.

Remaining: in-game review of large/buggy models, native gait transitions, targeting/overhead alignment and cleanup. The original NPC remains in the scene for mouse picking; only its GPU geometry upload is suppressed while a disguise is registered.

## New NPC investigations

- Audited all 15 movement metadata fields in 16,576 local-cache NPC definitions against RuneLite: zero mismatches/unsupported tags; 1,830 definitions have no native movement metadata. This is not a diagnosis of a particular live static model.
- Implemented bounded session-only **Record NPC actions** using supported events. Records are review material, not automatically classified attacks or promoted mappings; clears on logout/disable.
- Expanded inspector output with source model/gait IDs, mode, rig evidence, detailed staging failures, and native/brother action fallback status.
- Remaining: capture actual static/distorted disguise IDs; verify Auto's visual quality, native attack timing/restarts and brother action classification in-game; review live observations before extending mappings. Model skin groups and weighted rigs remain outside automatic classic compatibility checks.

Primary-source findings, implemented policy, cache audit and next steps: [npc-animation-research.md](npc-animation-research.md).

Source provenance, fitting limitations and corner scores: [material-research.md](material-research.md).
