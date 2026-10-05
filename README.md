# Barrows Brothers Randomizer

Swaps Barrows brothers' NPC models for random NPC models when they spawn in the tombs and tunnels. Configure the chance per spawn and animation mode, with best-effort animation playback. Combat and interactions stay unchanged. Requires RuneLite **GPU**.

## Development

Build with JDK 21 (plugin bytecode targets Java 11):

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew clean build
```

Rebuild from the shared development harness:

```sh
cd ../runelite-plugin-dev-client
./gradlew :barrows-brothers-randomizer:jar
```

Then enter `reload barrows-brothers-randomizer` in the running harness terminal.
