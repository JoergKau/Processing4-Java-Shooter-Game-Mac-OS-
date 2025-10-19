# Processing4 Java Shooter — Version 1.0

A small 2D arcade shooter built with Processing and Java.

This repository contains the source code and required libraries to build and run this shooter game. This README is the authoritative, English-only, Version 1.0 documentation for this project. Other markdown documents in the repository are intentionally not included in the public documentation for this release.

## Quick overview

- Language: Java (uses Processing 4 libraries)
- Target: Desktop (macOS, Windows, Linux)
- Main entry point: `game.Sketch`

## What is included

- `src/` — Java source code (game logic, entities, managers)
- `lib/` — Third-party JARs required at runtime (Processing JOGL and GLUE natives included for macOS)
- `resources/` — Images, sounds and other assets
- `jdk/README.md` — Notes about the bundled JDK (keep this file)

## Goals for Version 1.0

- Provide a working build and run experience using the included libraries
- Keep documentation concise and focused on installation and running
- Track only this README (and `jdk/README.md`) in the public-facing docs for this release

## Installation (minimum)

1. Ensure you have a Java 17 JDK. The project can use the Processing 4 bundled JDK (recommended for portability).
2. If you want to use the bundled JDK, copy or link the Processing JDK into the project root as `jdk/` (see `jdk/README.md`).
3. Make sure the `lib/` directory contains the required JARs. The repository includes macOS native JARs by default.
4. install Processing 4 from https://processing.org
5. This project is not using the Processing IDE but VS Code or IntelliJ.
6. Ensure Processing core library is added to the project (come along with the Processing app)

## Build (project-local JDK)

From the project root (macOS / Linux example):

```bash
# Compile all Java sources into out/
find src -name "*.java" -print0 | xargs -0 ./jdk/bin/javac -cp "lib/*" -d out
```

On Windows adjust the classpath separator (`;`) and the path to `jdk\bin\javac` accordingly.

## Run

From the project root (macOS / Linux example):

```bash
# Run the main class (game.Sketch)
./jdk/bin/java --enable-native-access=ALL-UNNAMED -cp "out:lib/*" game.Sketch
```

On Windows adapt the classpath separator and executable paths.

## Notes about native libraries

- The `lib/` folder contains platform-specific native JARs for Processing JOGL and GLUE. The provided jars are for macOS. If you run on Windows or Linux, replace the macOS native jars with the appropriate platform versions.
- For Mac OS this native libs are: "gluegen-rt-2.5.0-natives-macosx-universal.jar" and "jogl-all-2.5.0-natives-macosx-universal.jar". For Windows and Linux those file also come included in the Processing installation. 
- If you see native library errors at runtime, verify you are using the correct native JARs for your OS and architecture.

# Features — Version 1.0

- Classic 2D arcade shooter gameplay with player-controlled ship
- Smooth keyboard controls (left/right movement, shoot)
- Multiple enemy types with simple AI and varied behaviors
- Wave-based level progression with increasing difficulty
- Score system with combo multipliers and high-score tracking
- Power-ups and temporary upgrades (rate of fire, shields, multi-shot)
- HUD: score, lives, level, and FPS toggle
- Sound effects and background music with toggles
- Support for fullscreen and windowed modes
- Cross-platform: runs on macOS, Windows, and Linux with bundled libraries
- Modular codebase: clear separation of game logic, rendering and resource management


## Implementation / Engine Features (code-level)

- Modular manager-based architecture: `GameManager`, `AssetManager`, `InputHandler`, `EntityManager`, `UIManager`, `SoundManager`, `LevelManager` provide clear APIs and responsibilities.
- Spatial partitioning for collision detection: grid-based spatial hash reduces collision checks from O(n^2) to near O(n) and supports neighborhood queries.
- Object pooling across the engine: `ObjectPool` used for bullets, particles and other frequently-created objects to reduce GC pressure.
- Delta-time driven updates: `GameManager` provides delta time to keep movement and physics frame-rate independent.
- Entity system with type-safe queries: `EntityManager` supports creating/adding/removing entities, querying by type and object pooling integration.
- Particle system with pooling and configurable effects: explosions, trails, directional bursts and ring explosions with performance statistics.
- Asset management API: centralized asset loading and retrieval for images, sounds and animations with basic error recovery.
- Efficient UI rendering: UI is separated from game logic and supports selective redraws to improve rendering performance.
- Audio subsystem: centralized control for sound effects and music, master/music/effects volume controls, pause/resume and lifecycle management.
- Level & phase management: `LevelManager` handles spawning, phase timers and difficulty scaling per phase.
- Collision debug and metrics: collision system and particle system expose runtime stats for profiling and debugging.
- Testability & maintainability: modularized classes and clear interfaces make unit testing and mocking practical.

## Controls

- Left / Right arrows: move player
- Space: shoot
- C: continue from summary screens
- R: restart (disabled after final game over)
- F: toggle FPS display
- D: toggle debug info
- M: toggle music
- G: toggle fullscreen

## Author & License

Copyright (c) 2025 JoergKau + Windsurf 2.8.5

This project is released under the MIT License. See the `LICENSE` file (or the license section in this repository) for details.

---

Note: This README is intentionally short and is the single primary documentation file for Version 1.0. If you want additional developer guides or examples later, we can add them as separate files but they will not be published in this release.
