# Theme material palettes

The dropdown now contains nine themes. **Theme material palettes** is enabled by default and maps Barrows main stone, trim, relief and pale-detail roles separately; disabling it uses each theme's simpler hue/saturation fallback. Manual texture overrides still take precedence where supported.

## Provenance and scope

- Zanaris, Chambers of Xeric and Inferno retain the previously cache-sampled role colours. Evidence: [material-research.md](material-research.md).
- The six new presets below are **artistic area-inspired RGB palettes**, not sampled cache materials, exact recreations or authentic texture presets. RGB inputs are converted using RuneLite's `JagexColor.rgbToHSL(..., 1.0)` and applied through the existing lighting-preserving role mapping.
- Floor treatment uses the ground colour's hue/saturation and the brightness percentage, retaining original gradients rather than painting the exact RGB value.
- All nine themes now have explicit cache-backed wall geometry when **Replace wall models** is enabled. Replacement meshes use native source materials; these artistic palettes remain the treatment for original geometry. Sources and adaptations: [theme-models.md](theme-models.md).
- Crypt scenery-model swaps are parked: their configuration item is removed and any previously saved `replaceSceneryModels=true` is ignored. Small steps, soil, rocks and furniture keep their geometry and can still receive material/texture treatment.

## Designed palettes

| Theme | Main stone | Trim | Relief/accent | Pale detail | Ground inspiration | Floor brightness |
|---|---|---|---|---|---|---:|
| Prifddinas | `#D3D0DF` | `#9E8AB5` | `#76BDA6` | `#EFE7FF` | `#687F79` | 100% |
| Ancient Pyramid | `#C8AA6D` | `#A28145` | `#49A5A2` | `#E7CE87` | `#977841` | 95% |
| Darkmeyer | `#696371` | `#46404E` | `#872C3C` | `#B9A6A5` | `#30252E` | 75% |
| Fremennik ice caves | `#B3D2DC` | `#7CA9BD` | `#72C7E4` | `#E5F5FA` | `#7196AD` | 100% |
| Dorgesh-Kaan | `#9A8053` | `#625343` | `#CFA450` | `#83965C` | `#59483A` | 85% |
| Abyss | `#934553` | `#623347` | `#BC5F73` | `#B98AAB` | `#4D283F` | 80% |

The intended contrasts are pale lavender/mint, sandstone/turquoise, gothic stone/crimson, icy blue/frost, warm cave stone/amber, and red-purple organic colours. The palettes alone do not add geometry, frost effects, lights or fog; the separate wall-model option supplies themed meshes.

## Review in-game

Clear manual texture overrides, enable floors/walls and material palettes, and compare all nine themes in tunnels and tombs. Check brightness, role separation, gradient clipping, recognizability and remaining unmatched scenery. Switch among model-enabled themes and confirm each uses its own wall set; turn model replacement off to review these palettes on original geometry. Disable/re-enable and reload; check original materials return without stale faces. Builds/tests do not establish rendering quality.
