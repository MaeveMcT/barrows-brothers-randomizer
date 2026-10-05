# Barrows Brothers Randomizer

A RuneLite **GPU** plugin that gives each Barrows brother a random NPC disguise in the underground tunnels and tombs. Enabling the plugin enables the disguises—there is no separate randomization toggle.

## Behavior

- **Chance to randomize** (default **100%**): each brother gets one independent roll per spawn. **0%** keeps every original; **100%** always attempts a disguise. Changes affect future spawns, not brothers already tracked. Brothers present when the plugin is enabled also receive a roll.
- Successful chance rolls select uniformly from every available NPC-definition ID in the client's config archive. No size, name, combat, animation or rig filters are applied. Repeats, giant models, static/scenery-like definitions and malformed disguises are possible.
- Missing definitions, model-less morphs or unavailable models retain the original brother without rerolling. Ready disguises retain their selection through animation-setting changes.
- The original NPC still owns combat, movement, names, menus, collision, clickboxes and overhead UI. The replacement is a visual render object, not a new NPC actor. Only a successfully registered disguise suppresses the original's later GPU geometry upload; mouse picking is preserved.
- Native idle, walk, run, crawl and turn sequences are used where available. Unsupported/missing animations keep a static fallback. A small source-backed native-attack table covers five animal families; arbitrary NPC definitions have no universal attack-animation field.
- Disguises clean up on despawn, scene transitions, logout and plugin shutdown.

**Removed:** all wall/floor/scenery theming and model replacement, textures, the texture/inspection sidebar, and diagnostic action recording. Scenery is not read or edited by the randomization feature.

## Animation settings

- **Native only** (default): native gait and mapped native attacks; never borrows brother actions.
- **Auto:** prefers native attacks; otherwise requires full classic frame-map evidence or an explicit reviewed allow. Matching idle/walk IDs alone does not authorize borrowing.
- **Force brother actions:** tries the brother's current action/frame even on unknown rigs. This can produce distortion.
- **Mapped native NPC attacks** (default on): enables the explicitly researched mappings and numeric native overrides.
- **NPC action overrides:** advanced reviewed rules such as `1173:2075=5387; 42:*=deny; 42:2067=allow`. Exact actions beat wildcards; `42` is a syntax example, not a verified classification. Native-only never borrows.

Only NPC animation settings migrate from the interim Barrows Brothers Random NPC configuration (when present), otherwise the former Barrows Themes configuration, without overwriting settings already saved under the new plugin identity. Old theme, texture, randomization-toggle and recording settings do not control this plugin. Migration runs once, so resetting new settings does not reimport old values.

Source evidence, supported families and animation limitations: [docs/npc-animation-research.md](docs/npc-animation-research.md).

## Build and development

The repository directory and Gradle project are `barrows-brothers-randomizer`; the plugin class, package, display name and metadata use the same identity. Plugin bytecode targets Java 11.

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew clean build
```

The shared harness registration/reload ID is **`barrows-brothers-randomizer`**:

```sh
cd ../runelite-plugin-dev-client
./gradlew :barrows-brothers-randomizer:jar
```

**Restart an already-running harness once after this rename**, so it loads the new registration and stops the old plugin. Subsequent edits can use `reload barrows-brothers-randomizer`. Enable **Barrows Brothers Randomizer** and RuneLite **GPU**. The standalone test launcher is `com.barrowsbrothersrandomizer.BarrowsBrothersRandomizerLauncher`.

## In-game checks

Visit tombs and tunnels; verify the six brothers randomize while scenery stays original. Check targeting, overheads, native movement/action playback and static/missing-model fallbacks. Test 0%, 100% and an intermediate chance; confirm skipped brothers stay original across ticks. Change chance and animation options and confirm existing brothers do not reroll; respawn to apply the new chance. Despawn brothers, leave Barrows, log out/in, disable/re-enable and reload; confirm originals return without duplicate or stale render objects.

Unit tests and builds do not establish gameplay/rendering correctness. The renamed, brother-only plugin still needs in-game verification.
