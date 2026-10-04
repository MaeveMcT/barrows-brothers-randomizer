# Agent handoff — Barrows Brothers Randomizer

## Current scope

The user requested removal of all theme/scenery/model-changing features except random NPC disguises for Barrows brothers, and the final name **Barrows Brothers Randomizer** (`barrows-brothers-randomizer`).

- Production contains only brother disguises, native gait/action playback, guarded native attacks, classic rig evidence and reviewed overrides.
- Removed all wall/floor/scenery edits and replacement code, material palettes, textures, sidebar/navigation/inspection UI, wall research tools and related tests/docs.
- Diagnostic action recording was removed in the preceding uncommitted cleanup and remains absent.
- Enabling the plugin enables disguises. The old `randomBrothers` checkbox is gone; saved false values do not disable the new plugin's sole feature.
- Each spawn selects uniformly from every available NPC-definition ID. Keep the pool unfiltered; missing/model-less/unsupported choices retain originals without rerolling.
- Original brothers retain combat, actions, collision, clickboxes and overheads. The renderer callback suppresses only successfully disguised NPCs' later GPU geometry upload, never scene admission.
- Scene transitions/logout/despawn/shutdown remove visual objects. Animation-option changes update controllers without rerolling or rebuilding ready disguises.

## Identity and migration

The existing independent Git checkout was moved from `barrows-themes/` to **`barrows-brothers-randomizer/`**. Its Gradle project name is **`barrows-brothers-randomizer`**; Java package is `com.barrowsbrothersrandomizer`; plugin/config classes are `BarrowsBrothersRandomizerPlugin` and `BarrowsBrothersRandomizerConfig`. Metadata and standalone launcher use the new identity.

New config group: `barrowsbrothersrandomizer`. Startup migrates only `npcAnimationMode`, `nativeNpcAttacks` and `npcActionOverrides` from interim group `barrowsbrothersrandomnpc` when it contains settings/a migration marker, otherwise legacy group `barrowsthemes`, without overwriting existing new settings. An interim reset to defaults must not resurrect older theme-plugin values. Legacy enabled `brotherAttackAnimations` maps to Force only when no explicit mode exists. The old recorder/attack-checkbox keys are cleared. An internal migration marker prevents repeated imports after users reset new settings. Theme/texture/random-toggle settings are never imported or consumed.

The shared harness explicitly registers reload ID/project **`barrows-brothers-randomizer`**, mapped to `../barrows-brothers-randomizer`, with the new plugin class. **Restart an already-running harness once**: its old launch-time registration/class name cannot load the renamed plugin with `reload barrows-themes`. Then enable Barrows Brothers Randomizer; later rebuild/reload using the new ID. The old instance must shut down so any old scenery edits are restored.

## Verification

- Focused new lifecycle/config-migration tests passed.
- Plugin `./gradlew clean build`: **61 tests, zero failures/errors/skips**.
- Harness `:barrows-brothers-randomizer:jar`: passed; assembled JAR contains no old theme/wall/texture classes.
- The renamed brother-only build still needs in-game verification: six-brother scope, original scenery, clicking/overheads, native animation modes, no-reroll option updates, missing/static models and cleanup.
- Keep Java 11 plugin bytecode; run Gradle with JDK 21.

Build commands:

```sh
cd /Users/maeve/dev/runelite-plugins/barrows-brothers-randomizer
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew clean build
cd ../runelite-plugin-dev-client
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew :barrows-brothers-randomizer:jar
```

## History and constraints

The previous wall/theme work was locally committed as **`ecc7f12`** after the user's positive lighting/corner feedback. The user requested local commits for recording removal, the brother-only rename/refactor and the corresponding harness registration. Nothing was pushed. The former wall implementations/research remain available in Git history, not active production.

NPC cache audit/mappings and limits: [npc-animation-research.md](npc-animation-research.md). Production must use supported live-client interfaces only; never add reflection, disk-cache access, subprocesses, input injection or dynamic code loading. Offline research remains in `tools/`, outside plugin source. No cache assets, keys or screenshots are distributed.

The native movement audit had zero mismatches across 15 fields in 16,576 local-cache definitions; 1,830 had no native gaits. Static/scenery-like selections are not automatically decoder failures. Keep Auto conservative for incomplete/Maya/weighted rig evidence; no successful transform is proof of anatomical compatibility. Reviewed animal attack families remain unchanged; no new attack mappings were inferred from the removed recorder.

Public replies require exact-text user approval; no public reply, push or release was requested.
