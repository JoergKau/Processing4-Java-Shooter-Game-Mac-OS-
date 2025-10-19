# Local JDK for this project

This directory is intentionally ignored by Git. Do NOT commit your JDK here.

Purpose
- Place a Processing 4 JDK copy here if you want the project to use a local JDK at `./jdk`.
- The project configuration (VSCode `.vscode/settings.json` and IntelliJ `.idea/jdk.table.xml`) is set up to reference a project-local JDK at `jdk/`.

Options

A) Copy the Processing 4 JDK into this folder (recommended for portability)

1. Locate your Processing 4 installation and its bundled JDK.
   - macOS (typical): `/Applications/Processing.app/Contents/app/resources/jdk`
   - Windows (typical): `C:\Program Files\Processing\resources\jdk` (path may vary)
   - Linux (typical): `/path/to/processing/resources/jdk` (path may vary)

2. Copy the JDK into this project's `jdk/` folder.

macOS / Linux (example):

```bash
# from project root
cp -a "/Applications/Processing.app/Contents/app/resources/jdk" ./jdk
```

Windows (PowerShell example):

```powershell
# from project root (PowerShell)
Copy-Item -Recurse -Force "C:\Program Files\Processing\resources\jdk" ./jdk
```

3. Verify the JDK is present and executable:

```bash
./jdk/bin/java -version
./jdk/bin/javac -version
```

Notes:
- This approach is simple and makes the project self-contained for your local machine (no additional IDE configuration required beyond setting the Project SDK to `./jdk`).
- Do not commit the JDK files — they are ignored by `.gitignore`.


B) Keep the JDK elsewhere and use a symlink or direct IDE reference (if you prefer not to copy binaries)

Option B1 — Create a symbolic link named `jdk` that points to your external JDK:

macOS / Linux:

```bash
# remove existing jdk/ folder if present (be careful!)
rm -rf ./jdk
# create a symlink to an external JDK location
ln -s "/path/to/processing/resources/jdk" ./jdk
```

Windows (Command Prompt as Administrator):

```cmd
rmdir /s /q jdk
mklink /J jdk "C:\Path\To\Processing\resources\jdk"
```

Option B2 — Leave the JDK at its external location and configure your IDE to point to that path (no symlink needed):
- In IntelliJ: File → Project Structure → SDKs → `+` → JDK → select the external JDK folder; then set Project SDK to that entry.
- In VSCode: set `java.jdt.ls.java.home` to the absolute path of the external JDK (or adjust `.vscode/settings.json` to point to `${workspaceFolder}/jdk` if using a symlink).


C) Environment variables / Command-line
- You can also run compile/run commands using the external JDK by referencing its `bin` explicitly:

```bash
/path/to/external/jdk/bin/javac -cp "lib/*" -d out <java-files>
/path/to/external/jdk/bin/java -cp "out:lib/*" game.Sketch
```


Why two options?
- Copying (Option A) makes the project fully usable offline and ensures consistent behavior for local testing. However, it duplicates binary files.
- Linking or referencing an external JDK (Option B) saves disk space and matches your system installation, but requires IDE or symlink setup.


Troubleshooting & Tips
- If IntelliJ still shows the old JDK, reopen the Project Structure dialog and re-select the SDK or re-open the project.
- If you get native library errors at runtime (related to Processing natives), ensure that native jars (natives) are present under `lib/` and that your runtime classpath includes them.
- If your clone includes an accidentally tracked `jdk/` directory in Git history and you need to remove it from the repository entirely, let me know — I can guide you through rewriting history (this requires a force-push and coordination with collaborators).


Contact / Next steps
- If you want, I can:
  - help you create a symlink for your platform, or
  - adjust IDE settings files so they reference an alternate path, or
  - scrub the JDK from Git history (advanced, needs coordination).
