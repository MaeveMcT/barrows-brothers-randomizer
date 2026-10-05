# Offline NPC animation research

These tools are outside `src/` and never packaged in the plugin. Always use a **private cache copy**, not a cache belonging to a running client. No keys are needed; do not distribute cache assets or config dumps.

Set `CACHE_CP` to RuneLite's cache-library JAR and runtime dependencies, and `API_CP` to the public RuneLite API JAR. Use JDK 21 to run these tools.

## Native movement audit

`NpcAnimationAudit.java` compares the production decoder's 15 movement fields against RuneLite's `NpcLoader` for every definition. Optional IDs print definition/model/gait summaries, not meshes.

```sh
mkdir -p /tmp/barrows-npc-audit-classes
javac -cp "$CACHE_CP:$API_CP" -d /tmp/barrows-npc-audit-classes \
  tools/NpcAnimationAudit.java \
  src/main/java/com/barrowsbrothersrandomizer/NativeNpcAnimations.java
java -cp "$CACHE_CP:$API_CP:/tmp/barrows-npc-audit-classes" \
  com.barrowsbrothersrandomizer.NpcAnimationAudit /path/to/cache-copy \
  1173 1174 2790 2791 2793 2804 2805 2806 2827 2834 2838 2839 2856 2859
```

Findings and limitations: [../docs/npc-animation-research.md](../docs/npc-animation-research.md). The former wall/material research tools and all scenery replacement code have been removed from this plugin.
