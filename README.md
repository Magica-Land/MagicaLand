<div align="center">

<img src="Resources/Icon/full-缩放.png" alt="Magical Land logo" width="45%">

# Magical Land · Appearance

Bring your own pony into Minecraft.

Minecraft Java 1.20.1 · Fabric · In development

English | [简体中文](README_ZH.md)

</div>

**Magical Land** is a fan mod inspired by *My Little Pony: Friendship Is Magic*. Mix pony styles, choose colors, draw cutie marks, and save your own character presets.

Appearance provides models, customization, animations, and appearance synchronization, and can be used on its own. Tribe abilities, achievements, and other gameplay features are being developed in the separate [Gameplay Addon](https://github.com/Magical-Land-Official/Magical-Land-Gameplay).

[Downloads](https://github.com/Magical-Land-Official/Magical-Land/releases) · [User and developer documentation (Chinese)](docs/README.md) · [Report an issue](https://github.com/Magical-Land-Official/Magical-Land/issues)

## Character customization

- **Styles**: Choose the front mane, back mane, tail, and eye shape separately. Each mane section and the tail can be mirrored independently.
- **Body and mane colors**: Choose a base color to automatically get matching highlight and shadow colors, or unlock those colors to adjust them yourself. Front mane, back mane, and tail colors can be linked or set separately.
- **Six-region dyeing**: The front mane, back mane, and tail each have six regions you can color independently. Create two-tone or multicolor streaks while keeping the original texture and shading.
- **Eye colors**: Adjust the two iris colors independently while keeping the original gradient. Expand the advanced options to also change the colors of the eye whites, pupils, and eyelashes.
- **Custom cutie marks**: Draw on a 12×12 pixel canvas with a brush, eraser, eyedropper, undo, and redo. Share one design between both sides or draw each side separately.
- **Character presets**: Switch, create, duplicate, rename, and delete presets to try new looks based on an existing character.

Ponies blink, occasionally twitch their ears, and look at nearby entities. Some actions also have matching expressions. Soft, flowing magical glows surround unicorn horns and levitating items, accompanied by a few star sparkles and gentle magic sounds. Levitating items lag and sway with your movements, leaving magical trails as you move.

During flight, winged characters flap their wings while wingless characters hover upright. Wingless unicorns are also surrounded by a magical glow. The ability to fly comes from vanilla Minecraft or a gameplay mod; see the [flight visuals guide (Chinese)](docs/flight-visuals.md).

<p align="center">
  <img src="https://github.com/user-attachments/assets/9d5d3c8d-fb6e-4066-bb05-3bfdbf14ad2d" alt="Twilight Sparkle pony model showcase" width="45%">
  <img src="https://github.com/user-attachments/assets/be96510c-c7bc-45e5-a75e-e2c09c7155fb" alt="Another view of Twilight Sparkle" width="45%">
</p>

*Early in-game screenshots showing the models and art style.*

## Installation and use

The current platform is **Minecraft Java 1.20.1 / Fabric**. Forge / NeoForge support may be considered later.

Current development versions: Appearance **0.3.5** (API **1.5**), paired with Gameplay **0.3.3** when using the addon. Addons can use the existing flight visuals without changing vanilla flight permissions.

Appearance requires:

- Fabric Loader **0.19.1 or later**;
- **Fabric API** for Minecraft 1.20.1;
- **GeckoLib 4.7 or later** for Minecraft 1.20.1; development currently uses 4.8.3.

See [Releases](https://github.com/Magical-Land-Official/Magical-Land/releases) for version files and installation notes. Place the Appearance JAR and its dependencies in your game's `mods` folder. When upgrading from the old all-in-one mod, replace it with the new Appearance mod.

Click **“Pony Custom”** at the bottom right of the game's main menu to open the standalone character editor. **F9** opens mod settings by default; the optional Mod Menu also provides an entry to Appearance's settings.

If you have hidden the main-menu button, change its visibility under **Settings → General** in Appearance's settings.

The preview on the left updates as you edit. Drag to rotate it; selecting a part automatically focuses the camera on it, and you can turn automatic focus off. The preview uses fixed lighting, and style thumbnails use fixed reference colors. See the [customization guide (Chinese)](docs/customization-ui.md) for details.

Edits stay in a draft. Select **“Save & apply”** to update your character and presets. Choosing to discard changes when leaving restores the previously saved state.

## Multiplayer and Gameplay

| Installation | Features |
| --- | --- |
| Appearance and its dependencies on the client | Local pony appearance and customization. |
| Appearance and its dependencies on both client and server | Multiplayer appearance, animation, and gaze synchronization. The server uses the same Appearance JAR. |
| A compatible Gameplay Addon also installed on both sides | Separate gameplay features; see the Gameplay repository. |

Seeing each other's custom appearances in multiplayer requires compatible appearance synchronization on the server. Gameplay uses Appearance's public API. The mods have independent versions; check their release notes for compatible combinations.

Gameplay's server rules can determine whether a pony displays a horn or wings, without changing saved appearance presets. Tribe selection and the option to allow cosmetic combinations are managed by Gameplay; Appearance alone keeps the player's original choices.

## Development and feedback

The project is still in development. You're welcome to try it and share feedback. See the [project task list (Chinese)](TODO.md) for known issues and future plans.

When reporting an issue, include the game and mod versions, whether Gameplay is installed, whether the issue occurs in singleplayer or multiplayer, steps to reproduce it, and relevant screenshots or logs. Check logs for personal information before posting them publicly.

## Get involved

We want to keep refining the models, hand-painted textures, animations, and customization experience. Contributions in Java development, modeling and animation, art, testing, and translation are welcome. You can also share ideas through [Issues](https://github.com/Magical-Land-Official/Magical-Land/issues).

Developer resources (Chinese): [Separate-repository guide](docs/module-split.md) · [Public API](docs/appearance-api.md)

- QQ: 2026010008
- Discord: mayhooves
- Email: w2026010008@outlook.com

This is an unofficial fan project. See [LICENSE.txt](LICENSE.txt) for the repository license and the relevant documentation for asset information. The [magic sound source records (Chinese)](docs/magic-sounds.md) are still being completed.
