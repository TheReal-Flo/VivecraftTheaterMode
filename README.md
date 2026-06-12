# Vivecraft Theater Mode

Vivecraft Theater Mode brings back the GearVR Editions-style Theater view for Vivecraft.
Instead of floating menus and a standard VR layout, it recreates a more cinematic panel
in front of you, closer to the Theater mode shown in the GearVR editions era:
https://youtu.be/_rYfkDmSRKs?si=Hv8PWU4z2Hi2hOp-

This mod is made for players who want Vivecraft to feel more like a flat, front-facing
cinema screen while still keeping VR support and Vivecraft's normal menu flow.

## What It Does

- Adds a Theater play mode to Vivecraft
- Renders the game on a large front-facing screen
- Keeps the view comfortable and centered for seated-style play
- Preserves normal gameplay and menu interaction
- Includes controller-friendly GUI support

## Controller Mod Support

This mod is tested with [**Controlify**](https://modrinth.com/mod/controlify), so you can navigate
Theater menus with a controller—not just mouse and keyboard.

Other controller mods may work too, but Controlify is the one I have tested against.

## Notice on QuestCraft support

Right now, this won't work with the current QuestCraft version (`6.0.0`). This is mainly for two reasons:
- This mod is based of Vivecraft's Seated mode, which won't work without a mouse and keyboard or a gamepad, which both are not supported in QuestCraft.
- This mod is compiled agains a newer version of Vivecraft, which is not available for QuestCraft publicly at the moment.

Both of these might be fixed in the future through updates on QuestCrafts side. I'll remove this once that is the case.

## Requirements

- Minecraft `1.21.5`
- Fabric Loader
- Fabric API
- Vivecraft
- Available keyboard and mouse or a gamepad on a Controlify compatible environment (no PojavLauncher or QuestCraft)

## How To Use

1. Install the mod alongside Vivecraft.
2. Start the game and open Vivecraft's VR settings.
3. Use the play mode button until it reaches **Theater**.
4. Play normally in VR with the Theater view enabled.

The selected play mode is saved between launches.

## Custom Theater Environments

Theater mode now loads its 3D scene from a resource pack by default. Put the environment config at:

- `assets/vivecraft_theater_mode/theater/environment.json`

Then place your OBJ files and textures alongside it inside the same namespace. Resource packs can override that file, so users can install custom theater environments by enabling a pack in Minecraft's Resource Packs menu.

The JSON can reference multiple OBJ files, their textures, object transforms, the player spawn position, and the screen placement.
Use relative paths like `holograms/example.obj`, or explicit resource locations like `otherpack:theater/models/example.obj`.

Relative paths are resolved from the config file's folder, so this works:

Minimal shape:

```json
{
  "playerPosition": [0.0, 1.6, 0.0],
  "screen": {
    "position": [0.0, 1.4, -2.5],
    "rotation": [0.0, 0.0, 0.0],
    "size": [1.8, 1.0]
  },
  "objects": [
    {
      "model": "holograms/example.obj",
      "texture": "textures/example.png",
      "position": [0.0, 0.0, 0.0],
      "rotation": [0.0, 0.0, 0.0],
      "scale": [1.0, 1.0, 1.0]
    }
  ]
}
```

## Known issues

- Inventory sometimes seems to move items onto unintended slots, when using a gamepad. This can be fixed by closing and opening that inventory screen again.

## Notes

- This mod is a compatibility/add-on mod for Vivecraft, not a standalone VR replacement.
- If you use Controlify, make sure it is configured and actively controlling input when you want controller cursor behavior in Theater screens.

## Planned features
- [ ] Bringing back the OG theater environment
- [x] Functionality to add custom theater environments
- [ ] Multiplayer environments with splitscreen
