# Model-first themes

The supported themes are **Zanaris**, **Chambers of Xeric** and **Inferno**. Enable **Theme walls and crypt scenery** and **Replace wall models (experimental)** to use their native wall assets. Floors, furniture and the parked crypt-scenery experiment remain material-only. The six additional themes have been removed rather than extending unverified generic corner deformation to them.

## Source evidence

Models were inspected in a private cache copy using [WallModels.java](../tools/WallModels.java). No cache assets or keys are distributed. The idx2 fingerprint was `8eea5056e4005bdf40483f6f0037f3da82c78a92f762f769165068c67a8e8cff`. Model IDs have no gameval constants; source object names use RuneLite's gameval definitions. Detailed provenance: [material-research.md](material-research.md).

| Theme | Source definitions | Straight pool | Type-1 post source | Type-9 main / inner source |
|---|---|---|---:|---|
| Zanaris | `FAIRY_WALL_MUSH_GREEN/RED` and variants; `FAIRY_WALL` | 11909, 11910, 11911, 11912 | 11889 | 11890 / 11890 |
| Chambers of Xeric | `RAIDS_WALL_1/2` | 32435, 32437 | 32439 | 32432 / 32432 |
| Inferno | `INFERNO_WALL_EDGE_LARGE_01..06` and native corners | 33064, 33076, 33077, 33065, 33067, 33066 | 33064, rotated 45° | 33063 / 33074 |

Straight variants use stable instance-template coordinates, never per-tick random choices. Disabling variation uses the first source. Inferno's post is a deliberate adaptation of an actual Inferno wall edge, not a native diagonal post.

Source models are untextured coloured geometry. Zanaris reproduces ambient offset `+20`; CoX reproduces wall recolour `29574 → 29194`. The retained source definitions use default contrast. Replacement crypt walls use a deliberate **2× lighting-contrast divisor** (`1536` rather than `768`) to reduce directional brightness variation without increasing ambient or changing the light vector. The same policy applies to straight walls, posts and main/inner corners in all three themes; geometry, facing and seams are untouched. Visual quality still needs an in-game check. Missing assets, unsupported shapes and protected shared arrays retain originals; no unrelated fallback meshes are used.

## Fitting and orientation

Original type 0 is a straight wall, type 1 a small triangular diagonal post, and type 9 a full curved diagonal wall. Native source meshes do not necessarily occupy the same surfaces despite similar whole-mesh bounds.

- Zanaris and Inferno retain bounding-box fitting and their existing source-specific native-facing adjustments.
- CoX uses horizontal front-surface profiles, shared cell anchors and eight-local-unit endpoint overlap. Height scales once to the original visible height, with floor capped at `y=0`; this avoids collapsing upper endpoint vertices onto a sampled roof. It preserves relative native roof relief, not the exact original roof envelope.
- Baked placement/native rotation is separate from actual render orientation. `GameObject.getModelOrientation()` is retained; `getOrientation()` would double-rotate typical static models.

| Theme | Native post offset | Native main offset | Native inner offset |
|---|---:|---:|---:|
| Zanaris | 0 | 1024 | 0 |
| Chambers of Xeric | 0 | 0 | 0 |
| Inferno | 256 | 512 | 512 |

The Zanaris/Inferno offsets came from offline unsigned slice comparisons, not comprehensive live verification. CoX's failed v2 post half-turn was removed because it reversed the visible front. The corrected CoX fitter passes the four-rotation replay; the user subsequently reported that its corners look better. Door clearance, cutaways, both planes and all variants still need review. Reproduction and limitations: [agent-handoff.md](agent-handoff.md).

## Short corner diagnostics

The sidebar's **Short wall-corner report** creates a bounded, read-only snapshot of nearby static corners/posts. Report v3 distinguishes object orientation from actual model render rotation and labels `fit=front-surface profile` (CoX) versus `fit=bounds`. It lists template positions, type/slot, source model, placement/extra/baked/render rotations, height, registration status and bounds. Disabled replacement still reports candidates; it never loads assets or rebuilds walls. Matching bounds or registration do not prove matching surfaces.

## In-game review

Compare all three themes in tunnels and tombs: seams, all four corner orientations, doorway clearance, roof cutaways, native lighting and FPS. Switch themes, toggle replacement off, disable/re-enable and reload. Confirm originals return without stale/invisible faces and interactions/collision remain unchanged.

Future themes must have researched native assets and comparable signed-skin/join evidence before enabling fitting. Arbitrary sparse meshes cannot follow every original curved skin using vertex deformation alone. Do not restore the removed themes as recolour-only presets or borrow unrelated geometry.
