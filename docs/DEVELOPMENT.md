# Development

SVC Password Hide is a client-side UI add-on. Player installation and usage are
covered in the [README](../README.md).

## Build

Use Java 25. The Gradle wrapper downloads the required build dependencies.

```sh
./gradlew build
./gradlew build -Pminecraft_version=26.2
bash scripts/build-all.sh
```

The default target is Minecraft 26.1.2. Supported build targets are 26.1, 26.1.1,
26.1.2, 26.2, and 26.3. Artifacts are written to `build/libs/`. The matrix script
builds each target sequentially and collects installable JARs, documentation, and
test results in `dist/`.

The GitHub Actions workflow builds and tests the same five targets. Each target
produces a shared Fabric/Quilt JAR, a Forge JAR, and a NeoForge JAR.

To launch a Fabric development client with Simple Voice Chat:

```sh
./gradlew runClient -Pvoicechat_version=fabric-2.6.24+26.1.2
```

## Implementation

Minecraft versions starting with 26.1 use unobfuscated class names, allowing the
loader artifacts to share compiled UI code. Fabric Loom supplies the Minecraft
development classpath. Forge and NeoForge APIs are compile-only dependencies for
their entrypoints; no loader or Minecraft classes are bundled in the release JARs.
The Fabric/Quilt artifact contains native metadata for both loaders. This add-on
does not require an additional Fabric API dependency.

The public Simple Voice Chat API does not expose password-field customization.
Client-side mixins therefore target `CreateGroupScreen`, `EnterPasswordScreen`,
and the display/layout paths of `EditBox`.

Only the two marked password fields receive masking. Their stored values,
editing operations, clipboard operations, and network-facing getters remain
unchanged. Visibility state belongs to each dialog and resets during
initialization, including after a resize. Hidden fields also suppress password
narration, suggestions, and in-game IME composition overlays.

## Validation

Automated tests apply real Mixin transformations to Minecraft classes and to the
group screens from the published Simple Voice Chat Fabric 2.6.24 JAR. These group
screen sources are shared across loaders in the upstream project.

The transformed input field is also instantiated without a game window. Checks
cover masked text, cursor widths, selection, narration, and preservation of the
real password. There are four tests per Minecraft target, for 20 across the matrix.

These are not full gameplay tests for every loader/version combination. For a
manual check with a test password:

1. Create a group and confirm that typed and pasted text is masked immediately.
2. Toggle the eye button in both directions; check its hover and keyboard focus states.
3. Resize the window or reopen the dialog and confirm that visibility resets.
4. Join a protected group and confirm that the original password is accepted.
5. Edit, select, and delete a long password; reach the eye button using Tab.
6. Enable Minecraft narration and confirm that hidden passwords are not read aloud.

## Upstream references

- [Simple Voice Chat source](https://github.com/henkelmax/simple-voice-chat)
- [Voice Chat API events](https://voicechat.modrepo.de/de/maxhenkel/voicechat/api/events/package-summary.html)
- [Upstream supported versions](https://modrepo.de/minecraft/voicechat/wiki/supported_versions)

Simple Voice Chat source code is not distributed with this project.
