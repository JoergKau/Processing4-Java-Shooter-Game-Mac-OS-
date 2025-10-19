# Processing4 Java Shooter — Version 1.0

A small 2D arcade shooter built with Processing and Java.

This repository contains the source code and required libraries to build and run a simple shooter game. This README is the authoritative, English-only, Version 1.0 documentation for this project. Other markdown documents in the repository are intentionally not included in the public documentation for this release.

## Quick overview

- Language: Java (uses Processing 4 libraries)
- Target: Desktop (macOS, Windows, Linux)
- Main entry point: `game.Sketch`

## What is included

- `src/` — Java source code (game logic, entities, managers)
- `lib/` — Third-party JARs required at runtime (Processing/JOGL natives included for macOS)
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

- The `lib/` folder contains platform-specific native JARs for Processing/JOGL. The provided jars are for macOS. If you run on Windows or Linux, replace the macOS native jars with the appropriate platform versions.
- If you see native library errors at runtime, verify you are using the correct native JARs for your OS and architecture.

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

Copyright (c) 2025 JoergKau

This project is released under the MIT License. See the `LICENSE` file (or the license section in this repository) for details.

---

Note: This README is intentionally short and is the single primary documentation file for Version 1.0. If you want additional developer guides or examples later, we can add them as separate files but they will not be published in this release.
