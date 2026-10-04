# Theme material palettes

The dropdown contains **Zanaris**, **Chambers of Xeric** and **Inferno**. **Theme material palettes** is enabled by default and maps Barrows main stone, trim, relief and pale-detail roles separately; disabling it uses each theme's simpler hue/saturation fallback. Manual texture overrides still take precedence where supported.

## Provenance and scope

These three profiles use privately inspected cache samples, not image-texture presets or exact area recreations. Evidence and source definitions: [material-research.md](material-research.md).

| Theme | Main | Trim | Relief | Pale detail | Ground | Floor brightness |
|---|---:|---:|---:|---:|---|---:|
| Zanaris | 24524 | 10326 | 30156 | 21714 | Underlay 142, RGB `#50A0B8` | 100% |
| Chambers of Xeric | 28950 | 29194 | 28830 | 28830 | Underlay 102, RGB `#111E1A` | 90% |
| Inferno | 8 | 12 | 949 | 272 | Sand-floor model colour 12 | 65% |

Material values are packed Jagex HSL. Wall lightness is rescaled from original material roles; floor hue/saturation and brightness retain the original gradients. Lighting means this is not a pixel-identical reproduction.

Optional wall replacements retain native source materials rather than receiving the Barrows role palette. Sources and fitting strategies: [theme-models.md](theme-models.md). Small steps, soil, rocks and furniture keep their original geometry; crypt scenery-model swaps remain parked.

The six additional themes and their artistic palettes/model mappings have been removed. A saved selection that is no longer supported resets to Zanaris on plugin startup; retained selections are preserved.

## Review in-game

Clear manual texture overrides, enable floors/walls and material palettes, and compare the three themes in tunnels and tombs. Check brightness, role separation, gradient clipping and remaining unmatched scenery. Switch themes with wall replacement enabled, turn replacement off to review original geometry, then disable/re-enable and reload. Check original materials return without stale faces. Builds/tests do not establish rendering quality.
