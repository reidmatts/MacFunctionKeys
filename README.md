# MacFunctionKeys

A client-side Fabric mod for macOS (Minecraft Java Edition `26.3`) that automatically switches the keyboard top row to standard `F1`–`F12` function keys while the Minecraft window is focused, and restores standard macOS media key behavior when unfocused or closed.

## Features

- **Automatic `F1`–`F12` Mode on Focus**: Uses JNA to interface with macOS `IOKit` (`IOHIDSystem` / `HIDFKeyMode`) and `ApplicationServices` (`CGEventTap`) so pressing `F1`–`F12` in Minecraft works directly without holding `Fn`.
- **Triple-Tap `Fn` Toggle**: Rapidly tap the `Fn` key **3 times** while in Minecraft to toggle between **Standard `F1`–`F12` Keys** and **macOS Media Keys** on the fly.
- **On-Screen HUD Toast**: Displays a 2.5-second animated toast in the top-right corner of the screen whenever the active mode is initialized or toggled.
- **Automatic Cleanup**: Restores your original macOS function key preference immediately on window blur (`Cmd+Tab`, clicking outside) or game shutdown.

## Requirements

- **OS**: macOS
- **Minecraft**: `26.3`
- **Fabric Loader**: `>=0.20.1`
- **Fabric API**: `>=0.165.0+26.3`
- **Java**: `25+`

## Building

```bash
./gradlew build
```

Compiled JAR artifacts are generated in `build/libs/`.

## License

Released under the [CC0-1.0 License](LICENSE).
