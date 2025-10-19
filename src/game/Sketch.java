// Main game class, now primarily responsible for setup and delegation to manager classes.
// Uses a component-based architecture for better organization and maintainability.

package game;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.core.PGraphics;

import java.util.*;
import java.awt.*;

import processing.sound.*;

// Import game packages
import game.entities.*;
import game.effects.*;
import game.background.*;
import game.managers.*;
import game.systems.*;
import game.utils.*;
import game.advanced.*;
import game.powerups.*;

public class Sketch extends PApplet {
    // Game constants
    final int FPS = 60;
    final int minMobs = 5; // Minimale Anzahl Mobs beim Start
    static final String POWERUP_SHIELD = "shield";
    static final String POWERUP_GUN = "gun";
    static final float POWERUP_DROP_CHANCE = 0.1f;
    static final float SCREEN_SHAKE_BULLET = 2f;
    static final float SCREEN_SHAKE_BULLET_DURATION = 0.05f;
    static final float SCREEN_SHAKE_DAMAGE = 10f;
    static final float SCREEN_SHAKE_DAMAGE_DURATION = 0.2f;
    static final float SCREEN_SHAKE_DEATH = 20f;
    static final float SCREEN_SHAKE_DEATH_DURATION = 0.5f;

    // Sound System
    private boolean soundEnabled = false;
    private SoundFile shootSound;
    private SoundFile explosionSound;
    private SoundFile enemyExplosionSound; // Spezielle Explosion für Enemy Ships
    private SoundFile asteroidExplosionSound; // Spezielle Explosion für Asteroiden
    private SoundFile powerupSound;
    private SoundFile hitSound;
    private SoundFile trackingBombSound;
    private SoundFile splitSound; // Sound für Ship Split
    private SoundFile supernovaSound; // Sound für Supernova
    private SoundFile laserSound; // Sound für normale Enemy Bullets
    private SoundFile controlSound; // Sound für Player Ship Destruction
    private SoundFile backgroundMusic;
    private SoundFile gameOverMusic; // Boss Theme für Game Over
    private float masterVolume = 0.7f;
    private final float musicVolume = 0.3f;

    // Game managers
    private GameManager gameManager;
    private AssetManager assetManager;
    private InputHandler inputHandler;
    private EntityManager entityManager;
    private UIManager uiManager;
    private SoundManager soundManager;

    // Game state
    enum GameState {
        MENU, RUNNING, SUMMARY, SECOND_SUMMARY, THIRD_SUMMARY, GAME_OVER
    }

    GameState gameState = GameState.MENU;

    public int score = 0;
    public boolean playerRespawning = false;
    public boolean playerDeathHandled = false; // Prevent multiple death calls
    public float respawnTimer = 0;
    public float respawnDelay = 2.0f; // 2 Sekunden Verzögerung
    public long lastMobSpawnTime = 0;
    public long mobSpawnInterval = 1000;

    // Game timer and statistics
    public float gameTimer = 0;
    public float summaryTriggerTime = 60.0f; // 1 minute in seconds (for testing)
    public int enemiesKilled = 0;
    public int mobsKilled = 0;
    public int shipsKilled = 0;
    public boolean summaryShown = false;
    public boolean formationModeActive = false;
    public boolean secondPhaseActive = false;
    public float secondPhaseTimer = 0.0f;
    public final float SECOND_PHASE_DURATION = 60.0f; // 1 minute for second phase
    public boolean secondSummaryShown = false;
    public boolean thirdPhaseActive = false;
    public float thirdPhaseTimer = 0.0f;
    public final float THIRD_PHASE_DURATION = 60.0f; // 1 minute for third phase
    public boolean thirdSummaryShown = false;
    public boolean fourthPhaseActive = false; // Boss fight phase
    public boolean bossDefeated = false; // Flag to trigger explosion only once
    public int enemySpawnCounter = 0; // Counter for formation spawning

    // Timing - Frame-rate independent movement
    public long last_time;
    public float delta_time; // Time since last frame in seconds
    public long time;
    static final float NANOS_TO_SECONDS = 1.0f / 1_000_000_000.0f;
    static final float MAX_DELTA_TIME = 0.1f; // Cap delta to prevent huge jumps

    // Game objects - optimized collections
    public Player player;
    public ArrayList<Mob> mobs = new ArrayList<>(20);
    public ArrayList<EnemyShip> enemyShips = new ArrayList<>(10);
    public ArrayList<EnemyFormation> enemyFormations = new ArrayList<>(5);
    public Boss boss = null; // Boss for Phase 4
    public ArrayList<Bullet> bullets = new ArrayList<>(50); // Changed from ConcurrentLinkedQueue for better performance
    public ArrayList<Bomb> bombs = new ArrayList<>(20);
    public ArrayList<PowerUp> powerups = new ArrayList<>(10);
    public ArrayList<Explosion> explosions = new ArrayList<>(15);
    public ArrayList<Particle> particles = new ArrayList<>(500);
    public ArrayList<ScorePopup> scorePopups = new ArrayList<>(20);

    // Object pools for memory efficiency - reduces garbage collection
    private final ObjectPool<Bullet> bulletPool = new ObjectPool<>(() -> new Bullet(this, 0, 0), 50);
    private final ObjectPool<Explosion> explosionPool = new ObjectPool<>(() -> new Explosion(this, 0, 0, "sm"), 20);
    private final ObjectPool<Particle> particlePool = new ObjectPool<>(() -> new Particle(this, 0, 0), 5000, 15000); // Increased pool size for boss explosions

    // Reusable PVectors to avoid allocations in hot paths
    private final PVector tempVector = new PVector();

    // PGraphics layers - separate rendering for game and UI
    PGraphics gameLayer;  // Main game content (only redrawn when needed)
    PGraphics hudLayer;   // UI overlay (score, lives, shield)

    // Screen shake
    public float shakeDuration = 0;
    public float shakeAmount = 0;
    public PVector shakeOffset = new PVector(0, 0);

    // Parallax starfield
    public ArrayList<Star> stars = new ArrayList<>(240); // 100+80+60
    public ArrayList<ShootingStar> shootingStars = new ArrayList<>(10);

    // Space effects
    public ArrayList<SpaceCloud> spaceClouds = new ArrayList<>(5);
    public ArrayList<DistantPlanet> distantPlanets = new ArrayList<>(5);
    public ArrayList<ParticleSwarm> particleSwarms = new ArrayList<>(10);
    public ArrayList<MicroDebris> microDebris = new ArrayList<>(300);
    public ArrayList<Supernova> supernovas = new ArrayList<>(3);
    public ArrayList<AmbientDust> ambientDust = new ArrayList<>(50);
    public ArrayList<ShieldHitEffect> shieldHits = new ArrayList<>(10);
    public float nextShootingStarTime = 999999; // Sehr hoher Wert, wird in setup() richtig gesetzt
    public float nextCloudTime = 999999; // Sehr hoher Wert, wird in setup() richtig gesetzt
    public float nextPlanetTime = 999999; // Sehr hoher Wert, wird in setup() richtig gesetzt
    public float nextSwarmTime = 999999; // Sehr hoher Wert, wird in setup() richtig gesetzt
    public float nextSupernovaTime = 999999; // Sehr hoher Wert, wird in setup() richtig gesetzt

    // Assets
    public PImage backgroundImg;
    public PImage playerImg;
    public PImage playerMiniImg;
    public PImage bulletImg;
    public ArrayList<PImage> meteorImgs = new ArrayList<>();
    public ArrayList<PImage> enemyShipImgs = new ArrayList<>();
    public PImage bossImg;
    HashMap<String, ArrayList<PImage>> explosionAnims = new HashMap<>();
    HashMap<String, PImage> powerupImgs = new HashMap<>();

    // Performance monitoring
    public boolean showFPS = true;
    public boolean showDebug = false;
    public boolean godMode = false; // God mode for testing
    public float currentFPS = 0;
    public int frameCounter = 0;
    public long lastFPSUpdate = 0;

    // Collision optimization - spatial grid
    private static final int GRID_SIZE = 100;
    private ArrayList<ArrayList<Mob>> spatialGrid;
    private int gridCols;
    private int gridRows;

    // Rendering optimization flags
    private boolean hudNeedsRedraw = true;
    private int lastScore = 0;
    private int lastLives = 0;
    private int lastShield = 0;
    private boolean hintsInitialized = false;

    // Fullscreen support
    private boolean isFullscreen = false;
    private final int WINDOWED_WIDTH = 600;  // Initial window width
    private final int WINDOWED_HEIGHT = 800; // Initial window height
    private int screenHeight; // Display height from hardware


    public static void main(String[] args) {
        PApplet.main("game.Sketch");
    }

    @Override
    public void settings() {
        // Start in windowed mode with custom aspect ratio (3:4)
        size(WINDOWED_WIDTH, WINDOWED_HEIGHT, P2D);
        pixelDensity(2);
        noSmooth();
    }

    @Override
    public void setup() {
        // Initialize managers first
        gameManager = new GameManager(this);
        assetManager = new AssetManager(this);
        inputHandler = new InputHandler(this);
        soundManager = new SoundManager(this);
        entityManager = new EntityManager(this);
        uiManager = new UIManager(this);

        // Set system properties for better X11 cleanup
        System.setProperty("jogl.disable.openglcore", "false");
        System.setProperty("jogl.disable.openglarbcontext", "false");

        // Get display height from hardware (available after size() is called)
        screenHeight = displayHeight;

        // Initialize PGraphics with P2D renderer
        gameLayer = createGraphics(width, height, P2D);
        hudLayer = createGraphics(width, height, P2D);

        // P2D-specific optimizations
        hint(ENABLE_STROKE_PURE);

        // Set up text rendering
        textAlign(LEFT, TOP);
        textSize(16);

        // Initialize spatial grid
        gridCols = (int) Math.ceil((float) width / GRID_SIZE);
        gridRows = (int) Math.ceil((float) height / GRID_SIZE);
        spatialGrid = new ArrayList<>(gridCols * gridRows);
        for (int i = 0; i < gridCols * gridRows; i++) {
            spatialGrid.add(new ArrayList<>(5));
        }

        // Initialize parallax starfield
        initStarfield();

        // Load assets and initialize game
        loadAssets();
        loadSounds(); // Sound-Loading hinzufügen

        // Initialize space effect timers BEFORE initGame
        float currentTime = millis() / 1000.0f;
        nextShootingStarTime = currentTime + random(2, 5);
        nextCloudTime = currentTime + random(3, 5); // Erste Cloud nach 3-5 Sekunden
        nextPlanetTime = currentTime + random(3, 5); // Erster Planet nach 3-5 Sekunden
        nextSwarmTime = currentTime + random(3, 7);
        nextSupernovaTime = currentTime + random(15, 30); // Erste Supernova nach 15-30 Sekunden

        initGame();

        // Initialize timing
        last_time = System.nanoTime();
        frameRate(FPS);
    }

    // Sound-Loading-Methode
    void loadSounds() {
        int loadedCount = 0;

        // Lade jede Sound-Datei einzeln mit individueller Fehlerbehandlung
        try {
            shootSound = new SoundFile(this, "resources/snd/pew.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            explosionSound = new SoundFile(this, "resources/snd/explosion.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            enemyExplosionSound = new SoundFile(this, "resources/snd/explosion.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            asteroidExplosionSound = new SoundFile(this, "resources/snd/expl6.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            powerupSound = new SoundFile(this, "resources/snd/pow4.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            hitSound = new SoundFile(this, "resources/snd/pow5.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            trackingBombSound = new SoundFile(this, "resources/snd/rumble1.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            splitSound = new SoundFile(this, "resources/snd/SpaceShip3.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            supernovaSound = new SoundFile(this, "resources/snd/space_ship.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            laserSound = new SoundFile(this, "resources/snd/laser.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            controlSound = new SoundFile(this, "resources/snd/control.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        try {
            gameOverMusic = new SoundFile(this, "resources/snd/BossTheme.wav");
            loadedCount++;
        } catch (Exception e) {
            // Failed to load sound file
        }

        // Try to load background music - first WAV, then OGG
        // Note: Processing Sound Library has limited OGG support
        try {
            // Try WAV first (better compatibility)
            backgroundMusic = new SoundFile(this, "resources/snd/tgfcoder-FrozenJam-SeamlessLoop.wav");
            loadedCount++;
        } catch (Exception e1) {
            try {
                // Fallback to OGG (may not work with Processing Sound Library)
                backgroundMusic = new SoundFile(this, "resources/snd/tgfcoder-FrozenJam-SeamlessLoop.ogg");
                loadedCount++;
            } catch (Exception e2) {
                // Failed to load background music
            }
        }

        // Sound aktivieren wenn mindestens eine Datei geladen wurde
        if (loadedCount > 0) {
            soundEnabled = true;

            // Hintergrundmusik starten wenn verfügbar
            if (backgroundMusic != null) {
                try {
                    backgroundMusic.loop();
                    backgroundMusic.amp(musicVolume * masterVolume);
                } catch (Exception e) {
                    // Failed to start background music
                }
            }
        } else {
            soundEnabled = false;
        }
    }

    // Sound-Hilfsmethoden
    void playSound(SoundFile sound, float volume) {
        if (soundEnabled && sound != null) {
            try {
                // Kein stop() - lasse mehrere Instanzen gleichzeitig laufen
                // Das verhindert NullPointerException und klingt natürlicher
                float sfxVolume = 0.8f;
                sound.amp(volume * sfxVolume * masterVolume);
                sound.play();
            } catch (Exception e) {
                // Ignore sound playback errors
            }
        }
    }

    void playShootSound() {
        playSound(shootSound, 0.3f);
    }

    void playExplosionSound() {
        playSound(explosionSound, 0.5f);
    }

    void playEnemyExplosionSound() {
        playSound(enemyExplosionSound, 0.5f);
    }

    void playAsteroidExplosionSound() {
        playSound(asteroidExplosionSound, 0.4f);
    }

    void playPowerupSound() {
        playSound(powerupSound, 0.5f);
    }

    void playHitSound() {
        playSound(hitSound, 0.4f);
    }

    void playTrackingBombSound() {
        playSound(trackingBombSound, 0.5f);
    }

    void playSplitSound() {
        playSound(splitSound, 0.6f);
    }

    void playSupernovaSound() {
        playSound(supernovaSound, 0.4f); // Leiser da im Hintergrund
    }

    void playLaserSound() {
        playSound(laserSound, 0.3f); // Leiser da häufig
    }

    void playControlSound() {
        // Play at boosted volume - bypass normal scaling for extra loudness
        if (soundEnabled && controlSound != null) {
            try {
                controlSound.amp(2.0f * masterVolume); // 2x boost for dramatic effect
                controlSound.play();
            } catch (Exception e) {
                // Ignore sound playback errors
            }
        }
    }

    void toggleMusic() {
        // Don't allow toggling during game over - only boss theme should play
        if (gameState == GameState.GAME_OVER) {
            return;
        }

        if (soundEnabled && backgroundMusic != null) {
            if (backgroundMusic.isPlaying()) {
                backgroundMusic.pause();
            } else {
                backgroundMusic.loop();
                backgroundMusic.amp(musicVolume * masterVolume);
            }
        }
    }

    void startGameOverMusic() {
        // ALWAYS stop background music when entering game over
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.stop();
        }

        // Start game over music if available
        if (soundEnabled && gameOverMusic != null) {
            // Starte Game Over Music (endlos) - MAXIMUM VOLUME for dramatic effect
            gameOverMusic.loop();
            gameOverMusic.amp(masterVolume); // Maximum volume for boss theme
        }
    }

    void stopGameOverMusic() {
        if (gameOverMusic != null && gameOverMusic.isPlaying()) {
            gameOverMusic.stop();
        }
    }

    void adjustVolume(float delta) {
        masterVolume = constrain(masterVolume + delta, 0.0f, 1.0f);
        if (soundEnabled && backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.amp(musicVolume * masterVolume);
        }
        hudNeedsRedraw = true;
    }

    void toggleFullscreen() {
        isFullscreen = !isFullscreen;

        // Store old dimensions for position scaling
        float oldWidth = width;
        float oldHeight = height;

        if (isFullscreen) {
            // src/game/Sketch.java
            // Use Java AWT to get usable screen dimensions, accounting for menu bars/docks
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            // src/game/Sketch.java
            // src/game/Sketch.java
            int usableHeight = gc.getBounds().height - insets.top - insets.bottom;
            int safetyMargin = 40; // Extra pixels to ensure it fits

            // Switch to fullscreen mode - use usable display height, calculate width based on initial aspect ratio
            float aspectRatio = (float) WINDOWED_WIDTH / (float) WINDOWED_HEIGHT; // 600/800 = 0.75 (3:4)
            int fullscreenHeight = usableHeight - safetyMargin;
            int fullscreenWidth = (int) (fullscreenHeight * aspectRatio);

            surface.setSize(fullscreenWidth, fullscreenHeight);

            // Move window to top-center of screen
            int centerX = (displayWidth - fullscreenWidth) / 2;
            surface.setLocation(centerX, insets.top); // Position below the menu bar
        } else {
            // Switch back to windowed mode
            surface.setSize(WINDOWED_WIDTH, WINDOWED_HEIGHT);
        }

        // Calculate scaling factors
        float scaleX = width / oldWidth;
        float scaleY = height / oldHeight;

        // Recreate graphics layers to match new window size
        gameLayer = createGraphics(width, height, P2D);
        hudLayer = createGraphics(width, height, P2D);

        // Re-apply hints
        gameLayer.beginDraw();
        gameLayer.hint(ENABLE_STROKE_PURE);
        gameLayer.endDraw();

        hudLayer.beginDraw();
        hudLayer.hint(ENABLE_STROKE_PURE);
        hudLayer.endDraw();

        // Reinitialize spatial grid for new dimensions
        gridCols = (int) Math.ceil((float) width / GRID_SIZE);
        gridRows = (int) Math.ceil((float) height / GRID_SIZE);
        spatialGrid.clear();
        for (int i = 0; i < gridCols * gridRows; i++) {
            spatialGrid.add(new ArrayList<>(5));
        }

        // Handle player position specially to avoid rounding errors
        // Player should always be at bottom center (same relative position as when spawned)
        if (!playerRespawning) {
            player.pos.x = width / 2.0f;
            player.pos.y = height - 60;  // Same offset as in Player constructor
        }

        // Scale positions of all other game objects
        for (Mob mob : mobs) {
            mob.pos.x *= scaleX;
            mob.pos.y *= scaleY;
        }

        for (EnemyShip ship : enemyShips) {
            ship.pos.x *= scaleX;
            ship.pos.y *= scaleY;
        }

        for (Bullet bullet : bullets) {
            bullet.pos.x *= scaleX;
            bullet.pos.y *= scaleY;
        }

        for (Bomb bomb : bombs) {
            bomb.pos.x *= scaleX;
            bomb.pos.y *= scaleY;
        }

        for (PowerUp powerup : powerups) {
            powerup.pos.x *= scaleX;
            powerup.pos.y *= scaleY;
        }

        for (Explosion explosion : explosions) {
            explosion.pos.x *= scaleX;
            explosion.pos.y *= scaleY;
        }

        for (Particle particle : particles) {
            particle.pos.x *= scaleX;
            particle.pos.y *= scaleY;
        }

        for (ScorePopup popup : scorePopups) {
            popup.pos.x *= scaleX;
            popup.pos.y *= scaleY;
        }

        // Scale background elements that have accessible position fields
        for (Star star : stars) {
            star.x *= scaleX;
            star.y *= scaleY;
        }

        // Shield hit effects need scaling
        for (ShieldHitEffect hit : shieldHits) {
            hit.pos.x *= scaleX;
            hit.pos.y *= scaleY;
        }

        // Note: Other background elements (clouds, planets, etc.) will naturally adjust
        // as they spawn and wrap around with the new screen dimensions

        hudNeedsRedraw = true;
    }

    void initStarfield() {
        // Create 3 layers of stars for parallax effect
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(this, 1)); // Slow layer
        }
        for (int i = 0; i < 80; i++) {
            stars.add(new Star(this, 2)); // Medium layer
        }
        for (int i = 0; i < 60; i++) {
            stars.add(new Star(this, 3)); // Fast layer
        }

        // Space clouds werden jetzt dynamisch gespawnt, nicht beim Start
    }

    // Robust image loader: returns placeholder if missing
    public PImage safeLoadImage(String path, int w, int h, int col) {
        PImage img = loadImage(path);
        if (img == null) {
            img = createPlaceholderImage(w, h, col);
        }
        return img;
    }

    void loadAssets() {
        // Load background image
        backgroundImg = safeLoadImage("resources/img/starfield.png", width, height, color(0));

        // Load player images
        playerImg = safeLoadImage("resources/img/playerShip1_orange.png", 50, 30, color(255, 165, 0));

        // Create mini player image
        playerMiniImg = playerImg.copy();
        playerMiniImg.resize(25, 19);

        // Pre-optimize images for P2D
        if (backgroundImg != null) {
            backgroundImg.filter(BLUR, 0);
        }
        playerImg.filter(BLUR, 0);
        playerMiniImg.filter(BLUR, 0);

        // Load meteor images
        String[] meteorFiles = {"meteorBrown_big1.png", "meteorBrown_med1.png",
                "meteorBrown_med3.png", "meteorBrown_small1.png",
                "meteorBrown_small2.png", "meteorBrown_tiny1.png"};
        for (String file : meteorFiles) {
            PImage img = safeLoadImage("resources/img/" + file, 20, 20, color(139, 69, 19));
            img.filter(BLUR, 0);
            meteorImgs.add(img);
        }

        // Load explosion animations
        explosionAnims.put("lg", new ArrayList<>());
        explosionAnims.put("sm", new ArrayList<>());
        explosionAnims.put("player", new ArrayList<>());
        explosionAnims.put("ship", new ArrayList<>()); // Neue Explosion für Raumschiffe

        for (int i = 0; i < 9; i++) {
            String filename = String.format("regularExplosion%02d.png", i);
            PImage img = safeLoadImage("resources/img/" + filename, 75, 75, color(255, 140, 0));
            PImage largeImg = img.copy();
            largeImg.resize(75, 75);
            largeImg.filter(BLUR, 0);
            explosionAnims.get("lg").add(largeImg);

            PImage smallImg = img.copy();
            smallImg.resize(40, 40);
            smallImg.filter(BLUR, 0);
            explosionAnims.get("sm").add(smallImg);

            filename = String.format("sonicExplosion%02d.png", i);
            img = loadImage("resources/img/" + filename);
            if (img != null) {
                img.resize(100, 100);
                img.filter(BLUR, 0);
                explosionAnims.get("player").add(img);
            } else if (!explosionAnims.get("lg").isEmpty() && i < explosionAnims.get("lg").size()) {
                explosionAnims.get("player").add(explosionAnims.get("lg").get(i));
            }
        }

        // Load space effects for ship explosions
        for (int i = 1; i <= 18; i++) {
            String filename = String.format("resources/Effects/spaceEffects_%03d.png", i);
            PImage effectImg = safeLoadImage(filename, 60, 60, color(100, 200, 255));
            effectImg.resize(60, 60);
            effectImg.filter(BLUR, 0);
            explosionAnims.get("ship").add(effectImg);
        }

        // Load bullet image
        bulletImg = safeLoadImage("resources/img/laserRed16.png", 10, 20, color(255, 0, 0));
        bulletImg.filter(BLUR, 0);

        // Load enemy ship images from Ships directory
        for (int i = 1; i <= 9; i++) {
            String filename = String.format("resources/Ships/spaceShips_%03d.png", i);
            PImage shipImg = safeLoadImage(filename, 50, 50, color(200, 50, 50));
            shipImg.filter(BLUR, 0);
            enemyShipImgs.add(shipImg);
        }

        // Load additional enemy ship images from Ships2 directory
        String[] ships2Files = {"Raumschiff1.png", "Raumschiff2.png", "Raumschiff3.png",
                "Raumschiff4.png", "Raumschiff5.png", "Raumschiff6.png",
                "enemy00.png", "enemy03.png", "enemy04.png"};
        for (String file : ships2Files) {
            String filename = "resources/Ships2/" + file;
            PImage shipImg = safeLoadImage(filename, 50, 50, color(200, 50, 50));
            shipImg.filter(BLUR, 0);
            enemyShipImgs.add(shipImg);
        }

        // Load boss image
        bossImg = safeLoadImage("resources/boss/boss-ship.png", 150, 150, color(255, 0, 0));
        bossImg.filter(BLUR, 0);

        // Load powerup images
        PImage shieldImg = safeLoadImage("resources/img/shield_gold.png", 30, 30, color(255, 255, 0));
        shieldImg.filter(BLUR, 0);
        powerupImgs.put(POWERUP_SHIELD, shieldImg);

        PImage gunImg = safeLoadImage("resources/img/bolt_gold.png", 30, 30, color(255, 255, 0));
        gunImg.filter(BLUR, 0);
        powerupImgs.put(POWERUP_GUN, gunImg);
    }

    public PImage createPlaceholderImage(int w, int h, int col) {
        PImage img = createImage(w, h, ARGB);
        img.loadPixels();
        Arrays.fill(img.pixels, col);
        img.updatePixels();
        return img;
    }

    void initGame() {
        // Stoppe Game Over Musik wenn neues Spiel startet
        stopGameOverMusic();

        // Starte normale Background Music wieder
        if (soundEnabled && backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.loop();
            backgroundMusic.amp(musicVolume * masterVolume);
        }

        player = new Player(this);
        mobs.clear();
        enemyShips.clear();
        enemyFormations.clear();
        bullets.clear();
        bombs.clear();
        powerups.clear();
        explosions.clear();
        particles.clear();
        scorePopups.clear();
        distantPlanets.clear();
        particleSwarms.clear();
        shootingStars.clear();
        supernovas.clear();

        // Initialize micro debris (Background-Staub)
        microDebris.clear();
        for (int i = 0; i < 300; i++) {
            microDebris.add(new MicroDebris(this));
        }

        // Initialize ambient dust (Vordergrund-Staub)
        ambientDust.clear();
        for (int i = 0; i < 50; i++) {
            ambientDust.add(new AmbientDust(this));
        }

        lastMobSpawnTime = millis();

        // Bei Neustart: Setze Timer neu, damit keine sofortigen Spawns passieren
        float currentTime = millis() / 1000.0f;
        if (nextCloudTime < currentTime) {
            nextCloudTime = currentTime + random(2, 4);
        }
        if (nextPlanetTime < currentTime) {
            nextPlanetTime = currentTime + random(2, 4);
        }
        if (nextSwarmTime < currentTime) {
            nextSwarmTime = currentTime + random(2, 6);
        }
        if (nextShootingStarTime < currentTime) {
            nextShootingStarTime = currentTime + random(2, 5);
        }
        if (nextSupernovaTime < currentTime) {
            nextSupernovaTime = currentTime + random(20, 40); // Alle 20-40 Sekunden
        }

        // Pre-allocate mobs and enemy ships
        mobs.ensureCapacity(minMobs + 5);
        for (int i = 0; i < minMobs; i++) {
            if (random(1) < 0.3) { // 30% chance for enemy ship
                spawnEnemyShip();
            } else {
                spawnMob();
            }
        }

        gameState = GameState.RUNNING;
        score = 0;
        lastScore = 0;
        lastLives = player.lives;
        lastShield = player.shield;
        hudNeedsRedraw = true;
        shakeAmount = 0;
        shakeDuration = 0;

        // Reset all game state for a fresh start
        resetGameState();
        clearAllGameObjects();
        resetPlayerState();
    }

    void spawnMob() {
        mobs.add(new Mob(this, meteorImgs));
    }

    void spawnEnemyShip() {
        // In second phase, only spawn a ship 50% of the time
        if (!secondPhaseActive || random(1) < 0.5f) {
            enemyShips.add(new EnemyShip(this, enemyShipImgs));
        }
    }

    void spawnEnemyFormation() {
        int shipCount;
        if (secondSummaryShown && !thirdSummaryShown) {
            // Phase 3: 1-3 ships only (reduced by 30%)
            shipCount = (int) random(1, 4); // 1-3 ships
        } else {
            // Phase 1 and 2: 3-6 ships
            shipCount = (int) random(3, 7); // 3-6 ships
        }
        int formationType = (int) random(4); // 0-3 formation types
        EnemyFormation formation = new EnemyFormation(this, shipCount, formationType, enemyShipImgs);

        // Enable Galaxian-style diving attacks in Phase 3
        if (secondSummaryShown && !thirdSummaryShown) {
            formation.useGalaxianPattern = true;
            formation.useRandomMovementPatterns = true; // Enable random movement patterns
            // Set shorter initial attack interval for Phase 3 (ships dive sooner)
            formation.attackTimer = random(0.5f, 1.5f); // Start diving after 0.5-1.5 seconds

            // Reduce bomb drops by additional 20% in Phase 3 (total 40% reduction)
            for (EnemyShip ship : formation.ships) {
                ship.bombInterval *= 1.25f; // Increase interval by 25% = 20% fewer bombs
                ship.nextBombTime = (millis() / 1000.0f) + ship.bombInterval;
            }
        }

        enemyFormations.add(formation);
    }

    void spawnBoss() {
        boss = new Boss(this, bossImg);
    }

    /**
     * Resets all game state variables to their initial values.
     * Called when starting a new game.
     */
    private void resetGameState() {
        // Timer and statistics
        gameTimer = 0;
        secondPhaseTimer = 0;
        thirdPhaseTimer = 0;
        enemiesKilled = 0;
        mobsKilled = 0;
        shipsKilled = 0;
        enemySpawnCounter = 0;

        // Phase flags - start with phase 1
        summaryShown = false;
        secondSummaryShown = false;
        thirdSummaryShown = false;
        formationModeActive = false;
        secondPhaseActive = false;
        thirdPhaseActive = false;
        fourthPhaseActive = false;

        // Boss state
        bossDefeated = false;
        boss = null;

        // Player state
        playerRespawning = false;
        playerDeathHandled = false;
        respawnTimer = 0;
    }

    /**
     * Clears all active game objects (enemies, projectiles, effects).
     * Called when starting a new game or transitioning between phases.
     */
    private void clearAllGameObjects() {
        mobs.clear();
        enemyShips.clear();
        enemyFormations.clear();
        bombs.clear();
        supernovas.clear();
        particleSwarms.clear();
    }

    /**
     * Resets player to initial state with full health and lives.
     */
    private void resetPlayerState() {
        player.lives = 3;
        player.shield = 100;
    }

    /**
     * Handles boss shooting behavior based on current attack pattern.
     * Boss has 3 attack types:
     * - Type 0: 3-way spread pattern (straight bombs)
     * - Type 1: 2 seeking missiles (tracking)
     * - Type 2: 5-way wide spread (faster bombs)
     */
    void performBossShoot() {
        float bossSize = 6.0f; // Boss is 6x normal ship size
        int attackType = boss.getAttackType();

        if (attackType == 0) {
            // Spread pattern attack (3 bombs)
            // Center bomb (straight down)
            Bomb centerBomb = new Bomb(this, boss.pos.x, boss.pos.y + boss.radius, bossSize, player.pos);
            centerBomb.vel.set(0, 200); // Straight down, faster
            centerBomb.isTracking = false;
            bombs.add(centerBomb);

            // Left bomb (angled left)
            Bomb leftBomb = new Bomb(this, boss.pos.x - 50, boss.pos.y + boss.radius, bossSize, player.pos);
            leftBomb.vel.set(-120, 200); // Angled left
            leftBomb.isTracking = false;
            bombs.add(leftBomb);

            // Right bomb (angled right)
            Bomb rightBomb = new Bomb(this, boss.pos.x + 50, boss.pos.y + boss.radius, bossSize, player.pos);
            rightBomb.vel.set(120, 200); // Angled right
            rightBomb.isTracking = false;
            bombs.add(rightBomb);
        } else if (attackType == 1) {
            // Seeking missile attack (2 tracking bombs)
            // Use normal ship size for speed calculation (not boss size)
            float normalShipSize = 1.0f; // Normal enemy ship size

            // Left seeking missile
            Bomb leftMissile = new Bomb(this, boss.pos.x - 60, boss.pos.y + boss.radius, normalShipSize, player.pos);
            leftMissile.isTracking = true; // Enable tracking
            leftMissile.trackingStrength = 150f; // Same as normal enemy tracking
            bombs.add(leftMissile);

            // Right seeking missile
            Bomb rightMissile = new Bomb(this, boss.pos.x + 60, boss.pos.y + boss.radius, normalShipSize, player.pos);
            rightMissile.isTracking = true; // Enable tracking
            rightMissile.trackingStrength = 150f; // Same as normal enemy tracking
            bombs.add(rightMissile);

            // Play tracking bomb sound for missiles
            playTrackingBombSound();
        } else if (attackType == 2) {
            // Special pattern: 5-way spread with faster bombs
            for (int i = 0; i < 5; i++) {
                float angle = -60 + (i * 30); // -60, -30, 0, 30, 60 degrees
                float angleRad = radians(angle);
                float xOffset = sin(angleRad) * 80;

                Bomb bomb = new Bomb(this, boss.pos.x + xOffset, boss.pos.y + boss.radius, bossSize, player.pos);
                bomb.vel.set(sin(angleRad) * 180, 250); // Faster and wider spread
                bomb.isTracking = false;
                bombs.add(bomb);
            }
        }

        // Play laser sound
        playLaserSound();
    }

    /**
     * Boss special attack: Circular burst of 8 tracking missiles.
     * Missiles spawn in a circle around the boss and track the player.
     */
    void performBossSpecialAttack() {
        int missileCount = 8;
        float normalShipSize = 1.0f;

        for (int i = 0; i < missileCount; i++) {
            float angle = (TWO_PI / missileCount) * i;
            float spawnRadius = boss.radius + 40;
            float spawnX = boss.pos.x + cos(angle) * spawnRadius;
            float spawnY = boss.pos.y + sin(angle) * spawnRadius;

            Bomb missile = new Bomb(this, spawnX, spawnY, normalShipSize, player.pos);
            missile.isTracking = true;
            missile.trackingStrength = 200f; // Stronger tracking than normal

            // Initial velocity in circle direction
            missile.vel.set(cos(angle) * 100, sin(angle) * 100);
            bombs.add(missile);
        }

        // Visual effect: Ring of particles
        for (int i = 0; i < 50; i++) {
            float angle = random(TWO_PI);
            float speed = random(200, 400);
            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x, boss.pos.y, cos(angle) * speed, sin(angle) * speed,
                        color(255, 100, 100, 220));
                p.size = random(3, 8);
                particles.add(p);
            }
        }

        // Play special sound
        playTrackingBombSound();
        playLaserSound();
    }

    void handleBossDefeat() {
        if (boss == null) return;

        // EPIC BOSS EXPLOSION - Multiple waves of destruction

        // INITIAL PHASE: BOSS DISSOLVING INTO ENERGY CLOUD
        // Boss breaks apart into expanding energy particles from all parts of the ship

        // Create particles from the entire boss body (dissolving effect)
        int particlesFromBody = 2000;
        for (int i = 0; i < particlesFromBody; i++) {
            // Spawn particles from random positions within boss radius
            float spawnAngle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float spawnX = boss.pos.x + cos(spawnAngle) * spawnDist;
            float spawnY = boss.pos.y + sin(spawnAngle) * spawnDist;

            // Particles explode outward from their spawn position
            float explodeAngle = random(TWO_PI);
            float speed = random(100, 500);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(spawnX, spawnY, cos(explodeAngle) * speed, sin(explodeAngle) * speed,
                        color(255, random(150, 220), random(0, 50), 220)); // Orange/yellow
                p.size = random(3, 8); // Larger particles for explosion effect
                p.noFriction = true; // Keep moving until fade out
                particles.add(p);
            }
        }

        // Central white-hot core explosion (reactor breach)
        for (int i = 0; i < 1000; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(300, 700);
                p.reset(boss.pos.x, boss.pos.y, cos(angle) * speed, sin(angle) * speed,
                        color(255, 255, 255, 250));
                p.size = random(4, 10);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Bright energy shockwave (expanding ring)
        for (int ring = 0; ring < 5; ring++) {
            float ringRadius = boss.radius * (0.3f + ring * 0.2f);
            int particlesInRing = 80 + ring * 20;
            for (int i = 0; i < particlesInRing; i++) {
                float angle = (TWO_PI / particlesInRing) * i + random(-0.1f, 0.1f);
                float spawnX = boss.pos.x + cos(angle) * ringRadius;
                float spawnY = boss.pos.y + sin(angle) * ringRadius;
                float speed = random(200, 600);

                Particle p = particlePool.obtain();
                if (p != null) {
                    p.reset(spawnX, spawnY, cos(angle) * speed, sin(angle) * speed,
                            color(255, random(200, 255), random(0, 100), 200)); // Yellow/orange
                    p.size = random(5, 12);
                    p.noFriction = true;
                    particles.add(p);
                }
            }
        }

        // Blue energy plasma (reactor core dissolving)
        for (int i = 0; i < 1200; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 0.7f);
            float spawnX = boss.pos.x + cos(angle) * spawnDist;
            float spawnY = boss.pos.y + sin(angle) * spawnDist;
            float speed = random(150, 550);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(spawnX, spawnY, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(180, 220), random(0, 80), 220)); // Orange/yellow reactor
                p.size = random(4, 9);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Cyan electric arcs (energy discharge)
        for (int i = 0; i < 800; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 0.5f);
            float speed = random(250, 650);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(0, 255, 255, 240));
                p.size = random(3, 7);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Orange/yellow fire explosion (hull burning)
        for (int i = 0; i < 1500; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(120, 480);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(150, 255), random(0, 80), 200));
                p.size = random(4, 10);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Purple plasma clouds (exotic energy)
        for (int i = 0; i < 900; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 0.8f);
            float speed = random(100, 450);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(100, 180), random(0, 50), 190)); // Orange/red plasma
                p.size = random(5, 11);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Red critical fragments (hull debris)
        for (int i = 0; i < 1000; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(180, 520);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(0, 80), 0, 210));
                p.size = random(3, 8);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Green toxic gas cloud (coolant/fuel)
        for (int i = 0; i < 600; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 0.6f);
            float speed = random(80, 350);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(100, 200), 255, random(100, 200), 170));
                p.size = random(6, 13);
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Dark smoke/debris cloud (structural collapse)
        for (int i = 0; i < 800; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(60, 300);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(40, 100), random(40, 100), random(40, 100), 190));
                p.size = random(5, 12);
                p.life *= 2.0f; // Smoke lasts twice as long
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // DEBRIS AND SHRAPNEL - Heavy metal fragments
        for (int i = 0; i < 1500; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(150, 600);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(120, 180), random(120, 180), random(120, 180), 230));
                p.size = random(2, 6); // Small sharp fragments
                p.life *= 2.5f; // Debris lasts longer
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // SPARKS - Bright welding-like sparks
        for (int i = 0; i < 2000; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(200, 800);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(200, 255), random(100, 200), 250));
                p.size = random(1, 4); // Small bright sparks
                p.life *= 1.8f; // Sparks last longer
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // HEAVY SHRAPNEL - Large spinning debris
        for (int i = 0; i < 800; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(100, 450);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(80, 140), random(80, 140), random(80, 140), 240));
                p.size = random(8, 18); // Large chunks
                p.life *= 3.0f; // Heavy debris lasts much longer
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // BURNING FRAGMENTS - Fire-covered debris
        for (int i = 0; i < 1200; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(120, 500);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(100, 200), 0, 220));
                p.size = random(4, 10);
                p.life *= 2.2f; // Burning debris lasts longer
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // THICK SMOKE CLOUDS - Billowing smoke
        for (int i = 0; i < 1000; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 1.2f);
            float speed = random(40, 200);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(30, 80), random(30, 80), random(30, 80), 180));
                p.size = random(10, 25); // Large smoke clouds
                p.life *= 3.5f; // Smoke lasts very long
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // GLOWING EMBERS - Hot metal pieces
        for (int i = 0; i < 1000; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(80, 400);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(80, 150), 0, 200));
                p.size = random(3, 7);
                p.life *= 2.8f; // Embers glow for a long time
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // METAL SHARDS - Sharp angular pieces
        for (int i = 0; i < 1500; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius);
            float speed = random(180, 700);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(150, 220), random(150, 220), random(150, 220), 240));
                p.size = random(2, 5); // Sharp small pieces
                p.life *= 2.0f; // Metal shards last longer
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // MULTI-STAGE EXPLOSION (Contra-style)

        // STAGE 1: Initial burst points across boss body (multiple explosion origins)
        int burstPoints = 12; // Multiple explosion points
        for (int point = 0; point < burstPoints; point++) {
            float burstAngle = (TWO_PI / burstPoints) * point + random(-0.3f, 0.3f);
            float burstDist = random(boss.radius * 0.3f, boss.radius * 0.9f);
            float burstX = boss.pos.x + cos(burstAngle) * burstDist;
            float burstY = boss.pos.y + sin(burstAngle) * burstDist;

            // Large colored particles bursting from each point
            for (int i = 0; i < 150; i++) {
                float angle = random(TWO_PI);
                float speed = random(200, 700);

                Particle p = particlePool.obtain();
                if (p != null) {
                    p.reset(burstX, burstY, cos(angle) * speed, sin(angle) * speed,
                            color(255, random(150, 220), random(0, 50), 240)); // Orange/yellow burst
                    p.size = random(8, 20); // Large colored particles
                    p.life *= 2.5f;
                    p.maxLife = p.life;
                    p.noFriction = true;
                    particles.add(p);
                }
            }
        }

        // STAGE 2: Boss parts scattering (wings, engines, hull sections)
        // Left wing section exploding
        float leftWingX = boss.pos.x - boss.radius * 0.6f;
        float leftWingY = boss.pos.y;
        for (int i = 0; i < 400; i++) {
            float angle = random(PI * 0.5f, PI * 1.5f); // Explode left
            float speed = random(250, 650);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(leftWingX, leftWingY, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(150, 200), 0, 230)); // Orange/yellow wing
                p.size = random(10, 25); // Large wing fragments
                p.life *= 3.0f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Right wing section exploding
        float rightWingX = boss.pos.x + boss.radius * 0.6f;
        float rightWingY = boss.pos.y;
        for (int i = 0; i < 400; i++) {
            float angle = random(-PI * 0.5f, PI * 0.5f); // Explode right
            float speed = random(250, 650);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(rightWingX, rightWingY, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(150, 200), 0, 230)); // Orange/yellow wing
                p.size = random(10, 25); // Large wing fragments
                p.life *= 3.0f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Top engine section exploding
        float topEngineX = boss.pos.x;
        float topEngineY = boss.pos.y - boss.radius * 0.5f;
        for (int i = 0; i < 350; i++) {
            float angle = random(-PI, 0); // Explode upward
            float speed = random(300, 700);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(topEngineX, topEngineY, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(180, 220), random(0, 80), 230)); // Orange/yellow engine
                p.size = random(12, 28); // Large engine parts
                p.life *= 3.2f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Bottom hull section exploding
        float bottomHullX = boss.pos.x;
        float bottomHullY = boss.pos.y + boss.radius * 0.5f;
        for (int i = 0; i < 350; i++) {
            float angle = random(0, PI); // Explode downward
            float speed = random(300, 700);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(bottomHullX, bottomHullY, cos(angle) * speed, sin(angle) * speed,
                        color(random(150, 220), random(150, 220), random(150, 220), 230));
                p.size = random(12, 28); // Large hull chunks
                p.life *= 3.2f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // STAGE 3: Massive colored particle bursts (final large-scale burst)
        // Giant orange/red burst
        for (int i = 0; i < 800; i++) {
            float angle = random(TWO_PI);
            float speed = random(350, 800);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x, boss.pos.y, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(100, 200), 0, 240));
                p.size = random(15, 35); // Very large particles
                p.life *= 2.8f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Giant yellow/white burst
        for (int i = 0; i < 600; i++) {
            float angle = random(TWO_PI);
            float speed = random(400, 850);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x, boss.pos.y, cos(angle) * speed, sin(angle) * speed,
                        color(255, 255, random(100, 200), 250)); // Bright yellow/white
                p.size = random(12, 30); // Very large particles
                p.life *= 2.5f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Giant blue/cyan burst
        for (int i = 0; i < 600; i++) {
            float angle = random(TWO_PI);
            float speed = random(380, 820);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x, boss.pos.y, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(200, 255), random(0, 100), 240)); // Yellow/orange
                p.size = random(12, 30); // Very large particles
                p.life *= 2.5f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Giant purple/magenta burst
        for (int i = 0; i < 500; i++) {
            float angle = random(TWO_PI);
            float speed = random(360, 800);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x, boss.pos.y, cos(angle) * speed, sin(angle) * speed,
                        color(255, random(100, 150), 0, 240)); // Deep orange/red
                p.size = random(12, 30); // Very large particles
                p.life *= 2.5f;
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Wave 4: Massive ship explosion effect
        spawnShipExplosion(boss.pos.x, boss.pos.y, 8.0f); // 8x normal explosion

        // Wave 4.5: LINGERING DEBRIS - Remains at explosion site and fades slowly
        // Large debris chunks that stay near the explosion center
        for (int i = 0; i < 500; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 1.5f);
            float speed = random(10, 80); // Very slow moving

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(100, 150), random(100, 150), random(100, 150), 220));
                p.size = random(8, 20); // Large debris
                p.life *= 5.0f; // Lasts much longer
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Glowing embers that drift slowly
        for (int i = 0; i < 400; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 1.2f);
            float speed = random(5, 50); // Very slow

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(255, random(100, 180), 0, 200));
                p.size = random(4, 12);
                p.life *= 6.0f; // Lasts very long
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Thick smoke that lingers
        for (int i = 0; i < 600; i++) {
            float angle = random(TWO_PI);
            float spawnDist = random(0, boss.radius * 1.3f);
            float speed = random(5, 40); // Very slow drift

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(boss.pos.x + cos(angle) * spawnDist,
                        boss.pos.y + sin(angle) * spawnDist,
                        cos(angle) * speed, sin(angle) * speed,
                        color(random(30, 70), random(30, 70), random(30, 70), 160));
                p.size = random(15, 35); // Very large smoke
                p.life *= 7.0f; // Lasts extremely long
                p.maxLife = p.life;
                p.noFriction = true;
                particles.add(p);
            }
        }

        // Wave 5: Create multiple supernovas for dramatic effect
        for (int i = 0; i < 3; i++) {
            float offsetX = random(-boss.radius * 0.5f, boss.radius * 0.5f);
            float offsetY = random(-boss.radius * 0.5f, boss.radius * 0.5f);
            Supernova sn = new Supernova(this);
            sn.pos.x = boss.pos.x + offsetX;
            sn.pos.y = boss.pos.y + offsetY;
            supernovas.add(sn);
        }

        // EXTREME screen shake - long duration
        addScreenShake(SCREEN_SHAKE_DEATH * 8, SCREEN_SHAKE_DEATH_DURATION * 4);

        // Play multiple explosion sounds for dramatic effect
        playEnemyExplosionSound();
        playExplosionSound();
        playSupernovaSound();
        playControlSound(); // Dramatic destruction sound

        // Add massive score
        score += 10000;
        addScorePopup(boss.pos.x, boss.pos.y, 10000);

        // Increment kill counter
        enemiesKilled++;
        shipsKilled++;

        // Clear boss
        boss = null;

        // Restart background spawning (planets, swarms, supernovas)
        fourthPhaseActive = false; // Allow background elements to spawn again

        // Trigger victory screen - set game state to GAME_OVER but keep bossDefeated = true
        gameState = GameState.GAME_OVER;
        
        // Start victory music (boss theme continues)
        startGameOverMusic();
        
        hudNeedsRedraw = true;
    }

    void performPlayerShoot() {
        // Sound-Integration: Schuss-Sound abspielen
        playShootSound();

        if (player.power == 1) {
            Bullet b = bulletPool.obtain();
            b.reset(player.pos.x, player.pos.y - 30);
            bullets.add(b);
        } else {
            Bullet b1 = bulletPool.obtain();
            b1.reset(player.pos.x - 20, player.pos.y - 20);
            bullets.add(b1);

            Bullet b2 = bulletPool.obtain();
            b2.reset(player.pos.x + 20, player.pos.y - 20);
            bullets.add(b2);
        }

        // Small recoil shake
        addScreenShake(2, 0.05f);
    }

    void performEnemyShipBombDrop(EnemyShip ship) {
        // Keine Bomben im Game Over State
        if (gameState == GameState.GAME_OVER) {
            return;
        }

        // Bombengeschwindigkeit basierend auf Schiffsgröße
        float sizeRatio = ship.radius / 20.0f; // Relativ zur Player-Größe
        Bomb bomb = new Bomb(this, ship.pos.x, ship.pos.y + ship.radius, sizeRatio, player.pos);
        bombs.add(bomb);

        // Sound-Effekte unterschiedlich für normale und Tracking-Bomben
        if (bomb.isTracking) {
            playTrackingBombSound(); // Rumble Sound für Tracking
        } else {
            playLaserSound(); // Laser Sound für normale Bomben
        }
    }

    void performEnemyShipSplit(EnemyShip ship) {
        // Verhindere mehrfache Splits
        ship.canSplit = false;

        // Spezieller Split-Partikel-Effekt
        spawnEnemyShipSplitEffect(ship);

        // Erstelle zwei kleinere Schiffe
        int newRadius = (int) (ship.radius * 0.7f); // 70% der ursprünglichen Größe

        // Linkes Schiff - verwendet dasselbe Bild wie das ursprüngliche Schiff
        EnemyShip leftShip = new EnemyShip(this, enemyShipImgs);
        leftShip.radius = newRadius;
        leftShip.pos.set(ship.pos.x - ship.radius * 0.5f, ship.pos.y);
        leftShip.speed.set(ship.speed.x - 60, ship.speed.y); // Nach links
        leftShip.movementPattern = (int) random(8);
        leftShip.isSplitChild = true; // WICHTIG: Kann sich nicht weiter teilen
        leftShip.canSplit = false; // Explizit deaktivieren
        leftShip.warpingIn = false; // Kein Warp-In
        leftShip.hasGlow = ship.hasGlow;
        leftShip.hasThrusters = ship.hasThrusters;
        leftShip.thrusterColor = ship.thrusterColor;
        // Verwende dasselbe Bild wie das ursprüngliche Schiff
        leftShip.img = ship.img;
        if (leftShip.img != null) {
            leftShip.scaledImg = leftShip.img.copy();
            leftShip.scaledImg.resize(newRadius * 2, newRadius * 2);
        }
        leftShip.maxHealth = newRadius;
        leftShip.health = newRadius;
        leftShip.wobble = random(TWO_PI);
        leftShip.glowPulse = random(TWO_PI);
        enemyShips.add(leftShip);

        // Rechtes Schiff - verwendet dasselbe Bild wie das ursprüngliche Schiff
        EnemyShip rightShip = new EnemyShip(this, enemyShipImgs);
        rightShip.radius = newRadius;
        rightShip.pos.set(ship.pos.x + ship.radius * 0.5f, ship.pos.y);
        rightShip.speed.set(ship.speed.x + 60, ship.speed.y); // Nach rechts
        rightShip.movementPattern = (int) random(8);
        rightShip.isSplitChild = true; // WICHTIG: Kann sich nicht weiter teilen
        rightShip.canSplit = false; // Explizit deaktivieren
        rightShip.warpingIn = false; // Kein Warp-In
        rightShip.hasGlow = ship.hasGlow;
        rightShip.hasThrusters = ship.hasThrusters;
        rightShip.thrusterColor = ship.thrusterColor;
        // Verwende dasselbe Bild wie das ursprüngliche Schiff
        rightShip.img = ship.img;
        if (rightShip.img != null) {
            rightShip.scaledImg = rightShip.img.copy();
            rightShip.scaledImg.resize(newRadius * 2, newRadius * 2);
        }
        rightShip.maxHealth = newRadius;
        rightShip.health = newRadius;
        rightShip.wobble = random(TWO_PI);
        rightShip.glowPulse = random(TWO_PI);
        enemyShips.add(rightShip);

        // Markiere ursprüngliches Schiff zum Entfernen (wird in nächstem Frame entfernt)
        ship.pos.y = height + 1000; // Bewege es aus dem Bildschirm
    }

    void spawnEnemyShipSplitEffect(EnemyShip ship) {
        // SEHR AUFFÄLLIGER Split-Effekt mit mehreren Schichten

        // 1. Innerer heller Blitz (weiß)
        for (int i = 0; i < 30; i++) {
            float angle = random(TWO_PI);
            float speed = random(200, 350);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(
                        ship.pos.x,
                        ship.pos.y,
                        cos(angle) * speed,
                        sin(angle) * speed,
                        color(255, 255, 255, 255) // Reines Weiß
                );
                particles.add(p);
            }
        }

        // 2. Mittlere Schicht (Cyan/Elektrisch)
        for (int i = 0; i < 50; i++) {
            float angle = (TWO_PI / 50) * i + random(-0.1f, 0.1f);
            float dist = ship.radius * 0.3f;
            float speed = random(180, 280);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(
                        ship.pos.x + cos(angle) * dist,
                        ship.pos.y + sin(angle) * dist,
                        cos(angle) * speed,
                        sin(angle) * speed,
                        color(100, 255, 255, 240) // Helles Cyan (elektrisch)
                );
                particles.add(p);
            }
        }

        // 3. Äußere Energiewelle (Gelb-Orange)
        for (int i = 0; i < 80; i++) {
            float angle = (TWO_PI / 80) * i;
            float dist = ship.radius * 0.7f;
            float speed = random(150, 250);

            Particle p = particlePool.obtain();
            if (p != null) {
                p.reset(
                        ship.pos.x + cos(angle) * dist,
                        ship.pos.y + sin(angle) * dist,
                        cos(angle) * speed,
                        sin(angle) * speed,
                        color(255, 200, 50, 220) // Gelb-orange
                );
                particles.add(p);
            }
        }

        // 4. Vertikale "Trennungs-Linie" Effekt
        for (int i = 0; i < 20; i++) {
            float yOffset = random(-ship.radius, ship.radius);

            // Links
            Particle pLeft = particlePool.obtain();
            if (pLeft != null) {
                pLeft.reset(
                        ship.pos.x,
                        ship.pos.y + yOffset,
                        -random(150, 250), // Nach links
                        random(-30, 30),
                        color(255, 100, 255, 230) // Magenta
                );
                particles.add(pLeft);
            }

            // Rechts
            Particle pRight = particlePool.obtain();
            if (pRight != null) {
                pRight.reset(
                        ship.pos.x,
                        ship.pos.y + yOffset,
                        random(150, 250), // Nach rechts
                        random(-30, 30),
                        color(255, 100, 255, 230) // Magenta
                );
                particles.add(pRight);
            }
        }

        // 5. Kleine Explosion am Zentrum
        Explosion e = explosionPool.obtain();
        e.reset(ship.pos.x, ship.pos.y, "sm");
        explosions.add(e);

        // Stärkerer Screen Shake
        addScreenShake(8, 0.3f);

        // Spezieller Split-Sound
        playSplitSound();
    }

    void spawnEnemyShipWarpInEffect(EnemyShip ship) {
        // Wirbel von Partikeln beim Spawn
        for (int i = 0; i < 40; i++) {
            float angle = i * TWO_PI / 40;
            float dist = ship.radius * 2;
            float speed = random(100, 200);
            Particle p = particlePool.obtain();
            p.reset(
                    ship.pos.x + cos(angle) * dist,
                    ship.pos.y + sin(angle) * dist,
                    cos(angle) * speed,
                    sin(angle) * speed,
                    color(100, 200, 255, 200) // Cyan Warp-Farbe
            );
            particles.add(p);
        }
    }

    void addScreenShake(float amount, float duration) {
        shakeAmount = amount;
        shakeDuration = duration;
    }

    /**
     * Spawns a burst of particles at the specified location.
     * Used for explosions and visual effects.
     *
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param count Number of particles to spawn
     * @param col   Color of the particles
     */
    void spawnParticles(float x, float y, int count, int col) {
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(50, 200);
                p.reset(x, y, cos(angle) * speed, sin(angle) * speed, col);
                particles.add(p);
            }
        }
    }

    /**
     * Spawns bright impact sparks when a bullet hits a target.
     * Creates a burst of white, yellow, and orange particles.
     */
    void spawnImpactSparks(float x, float y, int count) {
        count = count * 2; // Double the count for more visual impact
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(150, 400); // Schneller als normale Partikel

                // Verschiedene helle Farben für Funken-Effekt
                int sparkColor;
                float colorChoice = random(1);
                if (colorChoice < 0.4f) {
                    sparkColor = color(255, 255, 255); // Weiß
                } else if (colorChoice < 0.7f) {
                    sparkColor = color(255, 255, 100); // Gelb
                } else {
                    sparkColor = color(255, 200, 100); // Orange
                }

                p.reset(x, y, cos(angle) * speed, sin(angle) * speed, sparkColor);
                particles.add(p);
            }
        }
    }

    /**
     * Spawns electric-style sparks when a ship takes damage.
     * Creates cyan/blue/white particles for an electrical effect.
     */
    void spawnDamageSparks(float x, float y) {
        int count = 15;
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            float angle = random(TWO_PI);
            float speed = random(100, 250);

            // Elektrische Farben (Cyan/Blau/Weiß)
            int sparkColor;
            float colorChoice = random(1);
            if (colorChoice < 0.4f) {
                sparkColor = color(100, 200, 255); // Cyan
            } else if (colorChoice < 0.7f) {
                sparkColor = color(200, 220, 255); // Hellblau
            } else {
                sparkColor = color(255, 255, 255); // Weiß
            }

            p.reset(x, y, cos(angle) * speed, sin(angle) * speed, sparkColor);
            particles.add(p);
        }
    }

    /**
     * Spawns debris particles when an asteroid is destroyed.
     * Amount scales with asteroid size (40-70 particles).
     * Uses brown/gray colors for realistic asteroid debris.
     */
    void spawnMobDebris(float x, float y, float mobRadius) {
        int debrisCount = (int) (40 + mobRadius);

        for (int i = 0; i < debrisCount; i++) {
            Particle p = particlePool.obtain();
            float angle = random(TWO_PI);
            float speed = random(50, 250);

            // Verschiedene Trümmer-Farben (Braun/Grau/Dunkel)
            int debrisColor;
            float colorChoice = random(1);
            if (colorChoice < 0.4f) {
                debrisColor = color(139, 69, 19); // Braun (Asteroid)
            } else if (colorChoice < 0.7f) {
                debrisColor = color(random(80, 120), random(80, 120), random(80, 120)); // Grau
            } else {
                debrisColor = color(random(60, 90), random(40, 60), random(30, 50)); // Dunkelbraun
            }

            p.reset(x, y, cos(angle) * speed, sin(angle) * speed, debrisColor);
            particles.add(p);
        }
    }

    /**
     * Creates a complex ship explosion with multiple particle types.
     * Includes core explosion, energy particles, sparks, and debris.
     * Randomly varies intensity and color for visual variety.
     *
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param scale Size multiplier for the explosion
     */
    void spawnShipExplosion(float x, float y, float scale) {
        float intensity = random(0.8f, 1.2f) * scale;
        int explosionType = (int) random(3); // 3 different explosion color schemes

        // Zentrale helle Explosion - variiert in Anzahl und Farbe
        int coreCount = (int) (random(25, 35) * intensity);
        for (int i = 0; i < coreCount; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(100, 300) * intensity;

                // Verschiedene Farbvarianten
                int col;
                if (explosionType == 0) {
                    col = color(255, random(200, 255), random(100, 200)); // Orange-gelb
                } else if (explosionType == 1) {
                    col = color(255, random(150, 200), random(50, 150)); // Mehr orange
                } else {
                    col = color(255, random(220, 255), random(150, 255)); // Heller, gelblicher
                }

                p.reset(x, y, cos(angle) * speed, sin(angle) * speed, col);
                particles.add(p);
            }
        }

        // Energie-Partikel - Farbe variiert je nach Typ
        int energyCount = (int) (random(20, 30) * intensity);
        for (int i = 0; i < energyCount; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(150, 350) * intensity;

                int col;
                if (explosionType == 0) {
                    col = color(random(100, 200), random(150, 255), 255); // Cyan-blau
                } else if (explosionType == 1) {
                    col = color(random(150, 255), random(100, 200), 255); // Mehr violett
                } else {
                    col = color(100, random(200, 255), random(200, 255)); // Türkis
                }

                p.reset(x, y, cos(angle) * speed, sin(angle) * speed, col);
                particles.add(p);
            }
        }

        // Weiße Funken - zufällige Anzahl
        int sparkCount = (int) (random(15, 25) * intensity);
        for (int i = 0; i < sparkCount; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(200, 400) * intensity;
                int col = color(255, 255, random(200, 255)); // Leicht variierendes Weiß
                p.reset(x, y, cos(angle) * speed, sin(angle) * speed, col);
                particles.add(p);
            }
        }

        // Trümmer - manchmal mehr, manchmal weniger
        int debrisCount = (int) (random(10, 20) * intensity);
        for (int i = 0; i < debrisCount; i++) {
            Particle p = particlePool.obtain();
            if (p != null) {
                float angle = random(TWO_PI);
                float speed = random(50, 150) * intensity;
                int col = color(random(60, 140), random(60, 140), random(60, 140)); // Variierendes Grau
                p.reset(x, y, cos(angle) * speed, sin(angle) * speed, col);
                particles.add(p);
            }
        }

        // Gelegentlich extra Effekte
        if (random(1) < 0.3f) {
            // Bonus: Rote Explosions-Partikel (30% Chance)
            for (int i = 0; i < 10; i++) {
                Particle p = particlePool.obtain();
                if (p != null) {
                    float angle = random(TWO_PI);
                    float speed = random(100, 250);
                    int col = color(255, random(50, 100), 0); // Rot-orange
                    p.reset(x, y, cos(angle) * speed, sin(angle) * speed, col);
                    particles.add(p);
                }
            }
        }
    }

    void addScorePopup(float x, float y, int points) {
        scorePopups.add(new ScorePopup(this, x, y, points));
    }

    /**
     * Handles player death and respawn logic.
     * If player has lives remaining: Creates explosion and respawns after delay.
     * If last life lost: Transitions to GAME_OVER state with boss fade-out.
     */
    void handlePlayerDeath() {
        // Prevent multiple calls
        if (playerDeathHandled) {
            return;
        }
        playerDeathHandled = true;

        // Common setup for both cases
        playerRespawning = true;
        player.pos.set(-1000, -1000); // Hide player

        if (player.lives > 1) {
            // Spieler hat noch weitere Schiffe - große Explosion und Respawn nach 2 Sekunden
            // WICHTIG: Leben wird SOFORT reduziert für korrekte HUD-Anzeige
            player.lives--;
            respawnTimer = respawnDelay;

            // Große Explosion (wie vorher)
            explosions.add(new Explosion(this, player.pos.x, player.pos.y, "lg"));
            explosions.add(new Explosion(this, player.pos.x - 30, player.pos.y - 30, "sm"));
            explosions.add(new Explosion(this, player.pos.x + 30, player.pos.y + 30, "sm"));

            // Sound
            playExplosionSound();
            playControlSound();

            // Starker Screen Shake
            addScreenShake(SCREEN_SHAKE_DEATH * 1.5f, SCREEN_SHAKE_DEATH_DURATION * 1.5f);

            // Viele Partikel (150)
            spawnParticles(player.pos.x, player.pos.y, 100, color(255, 165, 0));
            spawnParticles(player.pos.x, player.pos.y, 50, color(255, 100, 0));
        } else {
            // Last life lost - transition to game over
            respawnTimer = 4.0f;

            // Set lives to 0
            player.lives = 0;

            // Set game state to GAME_OVER
            gameState = GameState.GAME_OVER;

            // Trigger boss fade out if active
            if (boss != null && boss.isActive()) {
                boss.setGameOver();
            }

            // Starte Game Over Musik (Boss Theme)
            startGameOverMusic();

            // Reset game state for background elements
            fourthPhaseActive = false; // Allow background elements to spawn again
            bossDefeated = false; // Reset boss defeated flag

            // Clear any existing explosions
            explosions.clear();

            // Clear all bullets and return them to the pool
            Iterator<Bullet> bulletIt = bullets.iterator();
            while (bulletIt.hasNext()) {
                Bullet b = bulletIt.next();
                b.active = false;
                bulletPool.free(b);
                bulletIt.remove();
            }

            // Clear all particles and return them to the pool
            Iterator<Particle> particleIt = particles.iterator();
            while (particleIt.hasNext()) {
                Particle p = particleIt.next();
                p.life = 0; // Mark particle for removal in next update
                particlePool.free(p);
                particleIt.remove();
            }
        }

        hudNeedsRedraw = true;
    }

    public void draw() {
        // Update timing
        time = System.nanoTime();
        delta_time = (time - last_time) * NANOS_TO_SECONDS;
        delta_time = Math.min(delta_time, MAX_DELTA_TIME);
        last_time = time;

        // Update FPS counter
        updateFPS();

        // Update screen shake
        updateScreenShake();

        if (gameState == GameState.RUNNING) {
            update();

            // Update game timer and check for summary trigger
            gameTimer += delta_time;

            // Update second phase timer if in second phase
            if (secondPhaseActive && !secondSummaryShown) {
                secondPhaseTimer += delta_time;
                // Check if second phase summary should be shown
                if (secondPhaseTimer >= SECOND_PHASE_DURATION) {
                    gameState = GameState.SECOND_SUMMARY;
                    secondSummaryShown = true;
                    hudNeedsRedraw = true;
                }
            }

            // Update third phase timer if in third phase
            if (thirdPhaseActive && !thirdSummaryShown) {
                thirdPhaseTimer += delta_time;
                // Check if third phase summary should be shown
                if (thirdPhaseTimer >= THIRD_PHASE_DURATION) {
                    gameState = GameState.THIRD_SUMMARY;
                    thirdSummaryShown = true;
                    hudNeedsRedraw = true;
                }
            }

            // Redraw HUD every second to update timer
            if ((int) gameTimer != (int) (gameTimer - delta_time)) {
                hudNeedsRedraw = true;
            }

            // Check for first summary screen
            if (!summaryShown && gameTimer >= summaryTriggerTime) {
                gameState = GameState.SUMMARY;
                summaryShown = true;
                hudNeedsRedraw = true;
            }
        } else if (gameState == GameState.SUMMARY || gameState == GameState.SECOND_SUMMARY || gameState == GameState.THIRD_SUMMARY) {
            // In summary state: game continues in background without collisions
            update();
        } else if (gameState == GameState.GAME_OVER) {
            // Im Game Over State: Spiel läuft komplett weiter im Hintergrund
            // Update background elements but skip game logic
            updateBackgroundOnly();
        }

        render();
    }

    /**
     * Updates only background elements during game over state
     */
    void updateBackgroundOnly() {
        // Update boss for fade-out effect if active
        if (boss != null && boss.isActive()) {
            boss.update(delta_time);
        }

        // Update parallax stars
        for (Star star : stars) {
            star.update(delta_time);
        }

        // Update shooting stars
        for (int i = shootingStars.size() - 1; i >= 0; i--) {
            ShootingStar ss = shootingStars.get(i);
            ss.update(delta_time);
            if (ss.isDead()) {
                shootingStars.remove(i);
            }
        }

        // Spawn new shooting stars randomly
        float shootingStarTime = millis() / 1000.0f;
        if (shootingStarTime >= nextShootingStarTime) {
            shootingStars.add(new ShootingStar(this));
            nextShootingStarTime = shootingStarTime + random(3, 8);
        }

        // Update space clouds
        for (int i = spaceClouds.size() - 1; i >= 0; i--) {
            SpaceCloud cloud = spaceClouds.get(i);
            cloud.update(delta_time);
            if (cloud.isOffScreen()) {
                spaceClouds.remove(i);
            }
        }

        // Spawn new space clouds
        if (shootingStarTime >= nextCloudTime) {
            spaceClouds.add(new SpaceCloud(this));
            nextCloudTime = shootingStarTime + random(12, 22);
        }

        // Update micro debris (Background-Staub)
        for (MicroDebris debris : microDebris) {
            debris.update(delta_time);
        }

        // Update ambient dust (Vordergrund-Staub)
        for (AmbientDust dust : ambientDust) {
            dust.update(delta_time);
        }

        // Update distant planets
        for (int i = distantPlanets.size() - 1; i >= 0; i--) {
            DistantPlanet planet = distantPlanets.get(i);
            planet.update(delta_time);
            if (planet.isOffScreen()) {
                distantPlanets.remove(i);
            }
        }

        // Spawn new planets
        if (shootingStarTime >= nextPlanetTime) {
            distantPlanets.add(new DistantPlanet(this));
            nextPlanetTime = shootingStarTime + random(20, 36);
        }

        // Update particle swarms
        for (int i = particleSwarms.size() - 1; i >= 0; i--) {
            ParticleSwarm swarm = particleSwarms.get(i);
            swarm.update(delta_time);
            if (swarm.isDead()) {
                particleSwarms.remove(i);
            }
        }

        // Spawn new swarms
        if (shootingStarTime >= nextSwarmTime) {
            particleSwarms.add(new ParticleSwarm(this));
            nextSwarmTime = shootingStarTime + random(5, 12);
        }

        // Update supernovas
        for (int i = supernovas.size() - 1; i >= 0; i--) {
            Supernova sn = supernovas.get(i);
            sn.update(delta_time);
            if (sn.isDead()) {
                supernovas.remove(i);
            }
        }

        // Spawn new supernovas
        if (shootingStarTime >= nextSupernovaTime) {
            supernovas.add(new Supernova(this));
            playSupernovaSound();
            nextSupernovaTime = shootingStarTime + random(20, 40);
        }
    }

    void updateScreenShake() {
        // Don't apply screen shake in GAME_OVER state
        if (gameState == GameState.GAME_OVER) {
            shakeAmount = 0;
            shakeOffset.set(0, 0);
            return;
        }

        if (shakeDuration > 0) {
            shakeDuration -= delta_time;
            if (shakeDuration <= 0) {
                shakeAmount = 0;
                shakeOffset.set(0, 0);
            } else {
                shakeOffset.set(
                        random(-shakeAmount, shakeAmount),
                        random(-shakeAmount, shakeAmount));
            }
        }
    }

    /**
     * Haupt-Update-Logik für alle Spielobjekte und das HUD.
     */
    void update() {
        // Update parallax stars
        for (Star star : stars) {
            star.update(delta_time);
        }

        // Update shooting stars
        for (int i = shootingStars.size() - 1; i >= 0; i--) {
            ShootingStar ss = shootingStars.get(i);
            ss.update(delta_time);
            if (ss.isDead()) {
                shootingStars.remove(i);
            }
        }

        // Spawn new shooting stars randomly
        float shootingStarTime = millis() / 1000.0f;

        if (shootingStarTime >= nextShootingStarTime) {
            shootingStars.add(new ShootingStar(this));
            nextShootingStarTime = shootingStarTime + random(3, 8); // Alle 3-8 Sekunden
        }

        // Update space clouds
        for (int i = spaceClouds.size() - 1; i >= 0; i--) {
            SpaceCloud cloud = spaceClouds.get(i);
            cloud.update(delta_time);
            if (cloud.isOffScreen()) {
                spaceClouds.remove(i);
            }
        }

        // Spawn new space clouds gradually (40% weniger als ursprünglich)
        if (shootingStarTime >= nextCloudTime) {
            spaceClouds.add(new SpaceCloud(this));
            nextCloudTime = shootingStarTime + random(12, 22); // Alle 12-22 Sekunden (10-18 * 1.2)
        }

        // Update micro debris (Background-Staub)
        for (MicroDebris debris : microDebris) {
            debris.update(delta_time);
        }

        // Update ambient dust (Vordergrund-Staub)
        for (AmbientDust dust : ambientDust) {
            dust.update(delta_time);
        }

        // Update shield hit effects
        for (int i = shieldHits.size() - 1; i >= 0; i--) {
            ShieldHitEffect hit = shieldHits.get(i);
            hit.update(delta_time);
            if (hit.isDead()) {
                shieldHits.remove(i);
            }
        }

        // Update distant planets
        for (int i = distantPlanets.size() - 1; i >= 0; i--) {
            DistantPlanet planet = distantPlanets.get(i);
            planet.update(delta_time);
            if (planet.isOffScreen()) {
                distantPlanets.remove(i);
            }
        }

        // Spawn new planets (not in Phase 4)
        if (!fourthPhaseActive && shootingStarTime >= nextPlanetTime) {
            distantPlanets.add(new DistantPlanet(this));
            nextPlanetTime = shootingStarTime + random(20, 36); // Alle 20-36 Sekunden (25-45 * 0.8)
        }

        // Update particle swarms
        for (int i = particleSwarms.size() - 1; i >= 0; i--) {
            ParticleSwarm swarm = particleSwarms.get(i);
            swarm.update(delta_time);
            if (swarm.isDead()) {
                particleSwarms.remove(i);
            }
        }

        // Spawn new swarms (not in Phase 4)
        if (!fourthPhaseActive && shootingStarTime >= nextSwarmTime) {
            particleSwarms.add(new ParticleSwarm(this));
            nextSwarmTime = shootingStarTime + random(5, 12); // Alle 5-12 Sekunden
        }

        // Update supernovas
        for (int i = supernovas.size() - 1; i >= 0; i--) {
            Supernova sn = supernovas.get(i);
            sn.update(delta_time);
            if (sn.isDead()) {
                supernovas.remove(i);
            }
        }

        // Spawn new supernovas (not in Phase 4)
        if (!fourthPhaseActive && shootingStarTime >= nextSupernovaTime) {
            supernovas.add(new Supernova(this));
            playSupernovaSound();
            nextSupernovaTime = shootingStarTime + random(20, 40); // Alle 20-40 Sekunden
        }

        // Handle player respawn timer - 2 second delay after death
        if (playerRespawning) {
            respawnTimer -= delta_time;
            if (respawnTimer <= 0) {
                playerRespawning = false;
                playerDeathHandled = false; // Reset death flag for next death

                // WICHTIG: Leben wurde bereits in handlePlayerDeath() reduziert

                if (player.lives > 0) {
                    // Respawn player mit neuem Schiff
                    player.pos.set(width / 2.0f, height - 60);  // Same position as initial spawn
                    player.shield = 100; // Voller Shield beim Respawn
                } else {
                    // Keine Leben mehr - Game Over
                    handlePlayerDeath();
                    startGameOverMusic();
                }
                hudNeedsRedraw = true;
            }
        }

        // Update player (nur wenn nicht respawning)
        if (!playerRespawning) {
            player.update(delta_time, inputHandler.isLeftPressed(), inputHandler.isRightPressed(), inputHandler.isSpacePressed());

            // Handle shooting
            if (player.shouldShoot(inputHandler.isSpacePressed())) {
                performPlayerShoot();
            }
        }

        // Update bullets efficiently using Iterator for safe removal
        Iterator<Bullet> bulletIt = bullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet b = bulletIt.next();
            if (b.active) {
                b.update(delta_time);

                // Spawn bullet trail particles (reduced)
                if (b.shouldSpawnTrailParticle(frameCounter)) {
                    Particle p = particlePool.obtain();
                    p.reset(b.getTrailParticleX(), b.getTrailParticleY(),
                            random(-5, 5), random(10, 30),
                            color(255, random(50, 150), 0, 120));
                    particles.add(p);
                }

                if (b.isOffScreen()) {
                    b.active = false;
                }
            }

            if (!b.active) {
                bulletIt.remove();
                bulletPool.free(b);
            }
        }

        // Update and cull mobs
        for (int i = mobs.size() - 1; i >= 0; i--) {
            Mob m = mobs.get(i);
            m.update(delta_time);

            // Spawn dust trail particles from asteroids
            if (m.shouldSpawnDustParticle(frameCounter)) {
                // Main dust trail
                Particle p = particlePool.obtain();
                p.reset(m.getDustParticleX(), m.getDustParticleY(),
                        m.getDustParticleVelX(), m.getDustParticleVelY(),
                        m.getDustParticleColor());
                p.size = m.getDustParticleSize();
                particles.add(p);

                // Additional smaller dust particles
                if (m.shouldSpawnSecondaryDust()) {
                    Particle p2 = particlePool.obtain();
                    p2.reset(m.getSecondaryDustX(), m.getSecondaryDustY(),
                            m.getSecondaryDustVelX(), m.getSecondaryDustVelY(),
                            m.getSecondaryDustColor());
                    p2.size = m.getSecondaryDustSize();
                    particles.add(p2);
                }
            }

            if (m.isOffScreen()) {
                mobs.remove(i);
            }
        }

        // Update and cull enemy ships
        for (int i = enemyShips.size() - 1; i >= 0; i--) {
            EnemyShip ship = enemyShips.get(i);

            // Spawn warp-in effect on first frame
            if (ship.needsWarpInEffect()) {
                spawnEnemyShipWarpInEffect(ship);
            }

            ship.update(delta_time, frameCount, player.pos);

            // Spawn warp particles
            if (ship.shouldSpawnWarpParticle(frameCount)) {
                float angle = ship.getWarpParticleAngle();
                float dist = ship.getWarpParticleDist();
                float speed = ship.getWarpParticleSpeed();
                Particle p = particlePool.obtain();
                p.reset(
                        ship.pos.x + cos(angle) * dist,
                        ship.pos.y + sin(angle) * dist,
                        cos(angle) * speed,
                        sin(angle) * speed,
                        ship.getWarpParticleColor()
                );
                particles.add(p);
            }

            // Spawn thruster particles
            if (ship.shouldSpawnThrusterParticle(frameCount)) {
                Particle p = particlePool.obtain();
                p.reset(ship.getThrusterParticleX(), ship.getThrusterParticleY(),
                        ship.getThrusterParticleVelX(), ship.getThrusterParticleVelY(),
                        ship.thrusterColor);
                p.size = random(4, 9); // Dicker = größere Partikel
                p.life *= 0.2f; // 80% kürzer = 20% der ursprünglichen Länge
                p.maxLife = p.life;
                particles.add(p);
            }

            // Handle bomb dropping
            if (ship.shouldDropBomb()) {
                performEnemyShipBombDrop(ship);
            }

            // Handle ship splitting
            if (ship.shouldPerformSplit()) {
                performEnemyShipSplit(ship);
            }

            if (ship.isOffScreen()) {
                enemyShips.remove(i);
            }
        }

        // Update enemy formations
        for (int i = enemyFormations.size() - 1; i >= 0; i--) {
            EnemyFormation formation = enemyFormations.get(i);
            formation.update(delta_time, player.pos);

            // Spawn warp-in effects for formation ships
            for (EnemyShip ship : formation.ships) {
                if (ship.needsWarpInEffect()) {
                    spawnEnemyShipWarpInEffect(ship);
                }

                // Handle bomb dropping from formation ships
                if (ship.shouldDropBomb()) {
                    performEnemyShipBombDrop(ship);
                }
            }

            if (formation.isOffScreen()) {
                enemyFormations.remove(i);
            }
        }

        // Update boss if active
        if (boss != null) {
            if (!boss.isDead()) {
                boss.update(delta_time);

                // Boss shooting
                if (boss.shouldShoot()) {
                    performBossShoot();
                }

                // Boss special attack
                if (boss.shouldDoSpecialAttack()) {
                    performBossSpecialAttack();
                }

                // Boss shield particle emission
                if (boss.shouldEmitShieldParticles()) {
                    int particleCount = boss.getShieldParticleCount();
                    for (int i = 0; i < particleCount; i++) {
                        float[] data = boss.getShieldParticleData();
                        Particle p = particlePool.obtain();
                        p.reset(data[0], data[1], data[2], data[3], color(100, 200, 255, 180));
                        p.size = random(2, 5);
                        p.life = random(0.3f, 0.8f); // Short life
                        p.maxLife = p.life;
                        p.noFriction = false; // Shield particles have friction
                        particles.add(p);
                    }
                }
            } else if (!bossDefeated && gameState != GameState.GAME_OVER) {
                // Check if boss just died (trigger explosion once), but not if game is already over
                bossDefeated = true;
                handleBossDefeat();
            }
        }

        // Spawn new mobs or enemy ships (NOT in Phase 4)
        if (!fourthPhaseActive) {
            long currentTime = millis();
            int totalEnemies = mobs.size() + enemyShips.size();

            // Calculate spawn interval (20% faster in Phase 3)
            long effectiveSpawnInterval = (secondSummaryShown && !thirdSummaryShown) ?
                    (long) (mobSpawnInterval * 0.83) : // 17% shorter interval = 20% faster spawn rate
                    mobSpawnInterval;

            // Phase 3: Count formations separately (not individual ships)
            int minEnemiesThreshold = minMobs;
            if (secondSummaryShown && !thirdSummaryShown) {
                // In Phase 3, count number of formations (not ships)
                totalEnemies = enemyFormations.size();
                minEnemiesThreshold = 3; // Keep 3 formations active (reduced by 30% from 5)
            }

            if (totalEnemies < minEnemiesThreshold && currentTime - lastMobSpawnTime > effectiveSpawnInterval) {
                enemySpawnCounter++;

                // Phase 3 (after second summary, before third summary): Only formations, no single ships or asteroids
                if (secondSummaryShown && !thirdSummaryShown) {
                    // 100% formations in Phase 3
                    spawnEnemyFormation();
                } else if (formationModeActive && enemySpawnCounter % 10 == 0) {
                    // Phase 2: Every 10th enemy spawn in formation mode
                    spawnEnemyFormation();
                } else if (random(1) < 0.3) {
                    // Phase 1 and 2: 30% chance for single enemy ship
                    spawnEnemyShip();
                } else {
                    // Phase 1 and 2: 70% chance for asteroids
                    spawnMob();
                }
                lastMobSpawnTime = currentTime;
            }
        }

        // Update bombs
        for (int i = bombs.size() - 1; i >= 0; i--) {
            Bomb bomb = bombs.get(i);
            bomb.update(delta_time, player.pos, player.isDead());

            // Spawn Trail-Partikel
            if (bomb.shouldSpawnTrailParticle(frameCounter)) {
                Particle p = particlePool.obtain();
                p.reset(
                        bomb.getTrailParticleX(),
                        bomb.getTrailParticleY(),
                        bomb.getTrailParticleVelX(),
                        bomb.getTrailParticleVelY(),
                        bomb.getTrailParticleColor()
                );
                particles.add(p);
            }

            if (bomb.isOffScreen()) {
                bombs.remove(i);
            }
        }

        // Update powerups
        for (int i = powerups.size() - 1; i >= 0; i--) {
            PowerUp p = powerups.get(i);
            p.update(delta_time);

            // Spawn Aura-Partikel
            if (p.shouldSpawnAuraParticle(frameCounter)) {
                float angle = p.getAuraParticleAngle();
                Particle particle = particlePool.obtain();
                particle.reset(
                        p.getAuraParticleX(angle),
                        p.getAuraParticleY(angle),
                        p.getAuraParticleVelX(angle),
                        p.getAuraParticleVelY(angle),
                        p.getAuraParticleColor()
                );
                particles.add(particle);
            }

            if (p.isOffScreen()) {
                powerups.remove(i);
            }
        }

        // Update explosions
        for (int i = explosions.size() - 1; i >= 0; i--) {
            Explosion e = explosions.get(i);
            e.update(delta_time, explosionAnims);
            if (e.isFinished()) {
                explosions.remove(i);
                explosionPool.free(e);
            }
        }

        // Update particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(delta_time);
            if (p.isDead()) {
                particles.remove(i);
                particlePool.free(p);
            }
        }

        // Update score popups
        for (int i = scorePopups.size() - 1; i >= 0; i--) {
            ScorePopup sp = scorePopups.get(i);
            sp.update(delta_time);
            if (sp.isDead()) {
                scorePopups.remove(i);
            }
        }

        checkCollisions();

        // Check if HUD needs redraw
        if (score != lastScore || player.lives != lastLives || player.shield != lastShield) {
            hudNeedsRedraw = true;
            lastScore = score;
            lastLives = player.lives;
            lastShield = player.shield;
        }
    }

    /**
     * Fast circle collision detection using squared distance.
     * Avoids expensive sqrt() call by comparing squared distances.
     *
     * @param a  Center of first circle
     * @param ra Radius of first circle
     * @param b  Center of second circle
     * @param rb Radius of second circle
     * @return true if circles overlap
     */
    public boolean circlesCollide(PVector a, float ra, PVector b, float rb) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float minDist = ra + rb;
        return dx * dx + dy * dy < minDist * minDist;
    }

    /**
     * Checks all collision interactions between game objects.
     * Uses spatial grid optimization for bullet-mob collisions.
     * Handles: bullets vs mobs, bullets vs ships, bullets vs boss,
     * player vs bombs, player vs powerups, player vs enemies.
     */
    void checkCollisions() {
        // No collisions during game over or summary screens
        if (gameState == GameState.GAME_OVER || gameState == GameState.SUMMARY ||
                gameState == GameState.SECOND_SUMMARY || gameState == GameState.THIRD_SUMMARY) {
            return;
        }

        // Build spatial grid for mobs
        for (ArrayList<Mob> cell : spatialGrid) {
            cell.clear();
        }

        for (Mob mob : mobs) {
            int gridX = (int) (mob.pos.x / GRID_SIZE);
            int gridY = (int) (mob.pos.y / GRID_SIZE);
            int index = gridY * gridCols + gridX;
            if (index >= 0 && index < spatialGrid.size()) {
                spatialGrid.get(index).add(mob);
            }
        }

        // Check bullet-mob collisions using spatial grid
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            if (!bullet.active) {
                bulletIter.remove();
                bulletPool.free(bullet);
                continue;
            }

            // Get grid cell for bullet
            int gridX = (int) (bullet.pos.x / GRID_SIZE);
            int gridY = (int) (bullet.pos.y / GRID_SIZE);

            // Check surrounding cells
            boolean hitMob = false;
            for (int dy = -1; dy <= 1 && !hitMob; dy++) {
                for (int dx = -1; dx <= 1 && !hitMob; dx++) {
                    int checkX = gridX + dx;
                    int checkY = gridY + dy;

                    if (checkX < 0 || checkX >= gridCols || checkY < 0 || checkY >= gridRows) {
                        continue;
                    }

                    int index = checkY * gridCols + checkX;
                    if (index < 0 || index >= spatialGrid.size()) {
                        continue;
                    }

                    ArrayList<Mob> cellMobs = spatialGrid.get(index);
                    for (int i = cellMobs.size() - 1; i >= 0 && !hitMob; i--) {
                        Mob mob = cellMobs.get(i);
                        if (circlesCollide(bullet.pos, bullet.radius, mob.pos, mob.radius)) {
                            bullet.active = false;
                            mobs.remove(mob);
                            cellMobs.remove(i);

                            // Sound-Integration: Asteroiden-Explosions-Sound abspielen
                            playAsteroidExplosionSound();

                            // Explosion abhängig von der Mob-Größe
                            String explosionSize = mob.radius > 30 ? "lg" : ("sm");
                            Explosion e = explosionPool.obtain();
                            e.reset(mob.pos.x, mob.pos.y, explosionSize);
                            explosions.add(e);

                            // Spawn debris particles (mehr und variabler)
                            spawnMobDebris(mob.pos.x, mob.pos.y, mob.radius);

                            // Spawn impact sparks (helle Funken beim Treffer)
                            spawnImpactSparks(bullet.pos.x, bullet.pos.y, 20);

                            // Screen shake
                            addScreenShake(SCREEN_SHAKE_BULLET, SCREEN_SHAKE_BULLET_DURATION);

                            int points = 50 - mob.radius;
                            score += points;

                            // Add score popup
                            addScorePopup(mob.pos.x, mob.pos.y, points);

                            // Random powerup drop
                            if (random(1) < POWERUP_DROP_CHANCE) {
                                powerups.add(new PowerUp(this, mob.pos.x, mob.pos.y));
                            }

                            // Increment kill counter
                            mobsKilled++;
                            enemiesKilled++;

                            hitMob = true;
                        }
                    }
                }
            }
        }

        // Check bullet-enemy ship collisions
        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext(); ) {
            Bullet bullet = it.next();
            if (!bullet.active) {
                it.remove();
                bulletPool.free(bullet);
                continue;
            }

            for (int i = enemyShips.size() - 1; i >= 0; i--) {
                EnemyShip ship = enemyShips.get(i);
                if (circlesCollide(bullet.pos, bullet.radius, ship.pos, ship.radius)) {
                    bullet.active = false;

                    // Reduziere Health
                    ship.health -= 10;

                    // Spawn Damage Sparks (elektrische Funken)
                    spawnDamageSparks(bullet.pos.x, bullet.pos.y);

                    // Wenn Health auf 0, zerstöre Schiff
                    if (ship.health <= 0) {
                        enemyShips.remove(i);

                        // Sound-Integration: Spezieller Enemy Explosions-Sound
                        playEnemyExplosionSound();

                        // Partikel-basierte Raumschiff-Explosion (größenabhängig)
                        float explosionScale = ship.radius / 20.0f; // Relativ zur Player-Größe
                        spawnShipExplosion(ship.pos.x, ship.pos.y, explosionScale);

                        // Spawn impact sparks (helle Funken beim Treffer)
                        spawnImpactSparks(bullet.pos.x, bullet.pos.y, 30);
                        // Screen shake (stärker bei größeren Schiffen)
                        addScreenShake(SCREEN_SHAKE_BULLET * 1.5f * explosionScale, SCREEN_SHAKE_BULLET_DURATION);

                        // Punkte basierend auf Schiffsgröße (85-140 Punkte)
                        int brightness = 85 + (ship.radius - 17) * 5; // 17->85, 28->140
                        score += brightness;

                        // Add score popup
                        addScorePopup(ship.pos.x, ship.pos.y, brightness);

                        // Increment kill counter
                        shipsKilled++;
                        enemiesKilled++;
                    }

                    // Höhere Powerup-Drop-Chance für Schiffe
                    if (random(1) < POWERUP_DROP_CHANCE * 2) {
                        powerups.add(new PowerUp(this, ship.pos.x, ship.pos.y));
                    }

                    break;
                }
            }
        }

        // Check bullet-boss collisions
        if (fourthPhaseActive && boss != null && !boss.isDead()) {
            for (Iterator<Bullet> it = bullets.iterator(); it.hasNext(); ) {
                Bullet bullet = it.next();
                if (!bullet.active) {
                    it.remove();
                    bulletPool.free(bullet);
                    continue;
                }

                if (circlesCollide(bullet.pos, bullet.radius, boss.pos, boss.radius)) {
                    bullet.active = false;

                    // Check if shield is active before damage
                    boolean shieldWasActive = boss.isShieldActive();

                    // Boss takes damage
                    boss.takeDamage(10);

                    // Spawn damage effects based on shield state
                    if (shieldWasActive) {
                        // SHIELD HIT - Energy absorption effect
                        // Large shield hit effect with expanding ring
                        ShieldHitEffect shieldHit = new ShieldHitEffect(this, bullet.pos.x, bullet.pos.y);
                        shieldHits.add(shieldHit);

                        // Cyan/blue energy particles radiating outward
                        for (int i = 0; i < 25; i++) {
                            Particle p = particlePool.obtain();
                            float angle = random(TWO_PI);
                            float speed = random(150, 300);
                            p.reset(bullet.pos.x, bullet.pos.y, cos(angle) * speed, sin(angle) * speed,
                                    color(100, 200, 255, 220));
                            p.life *= 0.6f; // Shorter life for shield particles
                            p.maxLife = p.life;
                            particles.add(p);
                        }

                        // Additional glowing particles
                        for (int i = 0; i < 10; i++) {
                            Particle p = particlePool.obtain();
                            float angle = random(TWO_PI);
                            float speed = random(50, 150);
                            p.reset(bullet.pos.x, bullet.pos.y, cos(angle) * speed, sin(angle) * speed,
                                    color(150, 220, 255, 180));
                            particles.add(p);
                        }

                        // Smaller screen shake for shield hit
                        addScreenShake(SCREEN_SHAKE_BULLET * 1.5f, SCREEN_SHAKE_BULLET_DURATION);

                        // Different sound for shield hit (use powerup sound for energy absorption)
                        playPowerupSound();
                    } else {
                        // DIRECT HIT - Damage to hull
                        // Red/orange damage sparks (electrical/fire)
                        spawnDamageSparks(bullet.pos.x, bullet.pos.y);

                        // Yellow/white impact sparks
                        spawnImpactSparks(bullet.pos.x, bullet.pos.y, 50);

                        // Additional red explosion particles for direct hit
                        for (int i = 0; i < 15; i++) {
                            Particle p = particlePool.obtain();
                            float angle = random(TWO_PI);
                            float speed = random(100, 250);
                            p.reset(bullet.pos.x, bullet.pos.y, cos(angle) * speed, sin(angle) * speed,
                                    color(255, random(100, 150), 0, 200));
                            particles.add(p);
                        }

                        // Stronger screen shake for direct hit
                        addScreenShake(SCREEN_SHAKE_BULLET * 2.5f, SCREEN_SHAKE_BULLET_DURATION);

                        // Normal hit sound
                        playHitSound();
                    }

                    // Small score for hitting boss
                    score += 10;
                    addScorePopup(bullet.pos.x, bullet.pos.y, 10);

                    hudNeedsRedraw = true;
                }
            }
        }

        // Check bullet-formation ship collisions
        for (Iterator<Bullet> it = bullets.iterator(); it.hasNext(); ) {
            Bullet bullet = it.next();
            if (!bullet.active) {
                it.remove();
                bulletPool.free(bullet);
                continue;
            }

            boolean hitFormationShip = false;
            for (EnemyFormation formation : enemyFormations) {
                for (int i = formation.ships.size() - 1; i >= 0; i--) {
                    EnemyShip ship = formation.ships.get(i);
                    if (circlesCollide(bullet.pos, bullet.radius, ship.pos, ship.radius)) {
                        bullet.active = false;

                        // Reduziere Health
                        ship.health -= 10;

                        // Spawn Damage Sparks
                        spawnDamageSparks(bullet.pos.x, bullet.pos.y);

                        // Wenn Health auf 0, zerstöre Schiff
                        if (ship.health <= 0) {
                            formation.removeShip(ship);

                            // Sound-Integration
                            playEnemyExplosionSound();

                            // Explosion
                            float explosionScale = ship.radius / 20.0f;
                            spawnShipExplosion(ship.pos.x, ship.pos.y, explosionScale);
                            spawnImpactSparks(bullet.pos.x, bullet.pos.y, 30);
                            addScreenShake(SCREEN_SHAKE_BULLET * 1.5f * explosionScale, SCREEN_SHAKE_BULLET_DURATION);

                            // Punkte
                            int brightness = 85 + (ship.radius - 17) * 5;
                            score += brightness;
                            addScorePopup(ship.pos.x, ship.pos.y, brightness);

                            // Increment kill counter
                            shipsKilled++;
                            enemiesKilled++;
                        }

                        // Powerup drop
                        if (random(1) < POWERUP_DROP_CHANCE * 2) {
                            powerups.add(new PowerUp(this, ship.pos.x, ship.pos.y));
                        }

                        hitFormationShip = true;
                        break;
                    }
                }
                if (hitFormationShip) break;
            }
        }

        // Check player-mob collisions - direct iteration (few mobs)
        if (!godMode) { // Skip player collisions in god mode
            for (Mob mob : mobs) {
                if (circlesCollide(player.pos, player.radius, mob.pos, mob.radius)) {
                    player.takeDamage(20);

                    // Shield Hit Effect - use temp vector to avoid allocation (performance)
                    tempVector.set(mob.pos).sub(player.pos).normalize().mult(player.radius).add(player.pos);
                    shieldHits.add(new ShieldHitEffect(this, tempVector.x, tempVector.y));

                    // Sound-Integration: Hit-Sound abspielen
                    playHitSound();

                    // Screen shake on damage
                    addScreenShake(SCREEN_SHAKE_DAMAGE, SCREEN_SHAKE_DAMAGE_DURATION);

                    // Spawn impact particles
                    spawnParticles(mob.pos.x, mob.pos.y, 10, color(255, 0, 0));

                    if (player.isDead()) {
                        handlePlayerDeath();
                    }
                }
            }
        }

        // Check player-enemy ship collisions
        if (!godMode) { // Skip player collisions in god mode
            for (EnemyShip ship : enemyShips) {
                if (circlesCollide(player.pos, player.radius, ship.pos, ship.radius)) {
                    player.takeDamage(30); // Mehr Schaden durch Schiffe

                    // Shield Hit Effect - use temp vector
                    tempVector.set(ship.pos).sub(player.pos).normalize().mult(player.radius).add(player.pos);
                    shieldHits.add(new ShieldHitEffect(this, tempVector.x, tempVector.y));

                    // Sound-Integration: Hit-Sound abspielen
                    playHitSound();

                    // Screen shake on damage
                    addScreenShake(SCREEN_SHAKE_DAMAGE * 1.5f, SCREEN_SHAKE_DAMAGE_DURATION);

                    // Spawn impact particles
                    spawnParticles(ship.pos.x, ship.pos.y, 15, color(255, 50, 0));

                    if (player.isDead()) {
                        handlePlayerDeath();
                    }
                }
            }
        }

        // Check player-formation ship collisions
        if (!godMode) { // Skip player collisions in god mode
            for (EnemyFormation formation : enemyFormations) {
                for (EnemyShip ship : formation.ships) {
                    if (circlesCollide(player.pos, player.radius, ship.pos, ship.radius)) {
                        player.takeDamage(30);

                        tempVector.set(ship.pos).sub(player.pos).normalize().mult(player.radius).add(player.pos);
                        shieldHits.add(new ShieldHitEffect(this, tempVector.x, tempVector.y));

                        playHitSound();
                        addScreenShake(SCREEN_SHAKE_DAMAGE * 1.5f, SCREEN_SHAKE_DAMAGE_DURATION);
                        spawnParticles(ship.pos.x, ship.pos.y, 15, color(255, 50, 0));

                        if (player.isDead()) {
                            handlePlayerDeath();
                        }
                    }
                }
            }
        }

        // Check player-bomb collisions
        if (!godMode) { // Skip player collisions in god mode
            for (int i = bombs.size() - 1; i >= 0; i--) {
                Bomb bomb = bombs.get(i);
                if (circlesCollide(player.pos, player.radius, bomb.pos, bomb.radius)) {
                    player.takeDamage(30); // Bomben machen mehr Schaden

                    // Shield Hit Effect - use temp vector
                    tempVector.set(bomb.pos).sub(player.pos).normalize().mult(player.radius).add(player.pos);
                    shieldHits.add(new ShieldHitEffect(this, tempVector.x, tempVector.y));

                    bombs.remove(i);

                    // Sound-Integration: Hit-Sound abspielen
                    playHitSound();

                    // Kleine Explosion
                    Explosion e = explosionPool.obtain();
                    e.reset(bomb.pos.x, bomb.pos.y, "sm");
                    explosions.add(e);

                    // Screen shake
                    addScreenShake(SCREEN_SHAKE_DAMAGE, SCREEN_SHAKE_DAMAGE_DURATION);

                    // Spawn impact particles
                    spawnParticles(bomb.pos.x, bomb.pos.y, 15, color(255, 150, 0));

                    if (player.isDead()) {
                        handlePlayerDeath();
                        startGameOverMusic();
                        explosions.add(new Explosion(this, player.pos.x, player.pos.y, "player"));
                        hudNeedsRedraw = true;
                        playExplosionSound();
                        addScreenShake(SCREEN_SHAKE_DEATH, SCREEN_SHAKE_DEATH_DURATION);
                        spawnParticles(player.pos.x, player.pos.y, 50, color(255, 165, 0));
                    }
                }
            }
        }

        // Check player-particle swarm collisions
        if (!godMode) { // Skip player collisions in god mode
            for (int i = particleSwarms.size() - 1; i >= 0; i--) {
                ParticleSwarm swarm = particleSwarms.get(i);
                if (circlesCollide(player.pos, player.radius, swarm.center, swarm.getRadius())) {
                    player.takeDamage(15); // Schaden durch Schwarm
                    particleSwarms.remove(i);

                    // Sound-Integration: Hit-Sound abspielen
                    playHitSound();

                    // Screen shake
                    addScreenShake(SCREEN_SHAKE_DAMAGE * 0.8f, SCREEN_SHAKE_DAMAGE_DURATION);
                    // Spawn impact particles
                    spawnParticles(swarm.center.x, swarm.center.y, 20, swarm.swarmCol);

                    if (player.isDead()) {
                        playExplosionSound();
                        addScreenShake(SCREEN_SHAKE_DEATH, SCREEN_SHAKE_DEATH_DURATION);
                        spawnParticles(player.pos.x, player.pos.y, 50, color(255, 165, 0));
                        handlePlayerDeath();
                    }
                }
            }

        }

        // Check player-powerup collisions
        for (int i = powerups.size() - 1; i >= 0; i--) {
            PowerUp p = powerups.get(i);
            if (circlesCollide(player.pos, player.radius, p.pos, p.radius)) {
                // Sound-Integration: Powerup-Sound abspielen
                playPowerupSound();

                if (p.type.equals(POWERUP_SHIELD)) {
                    player.gainShield((int) random(10, 30));
                    spawnParticles(p.pos.x, p.pos.y, 10, color(0, 255, 0));
                } else if (p.type.equals(POWERUP_GUN)) {
                    player.powerUp();
                    spawnParticles(p.pos.x, p.pos.y, 10, color(255, 255, 0));
                }
                powerups.remove(i);
                hudNeedsRedraw = true;
            }
        }
    }

    void render() {

        // Initialize hints on first render when GL context is ready
        if (!hintsInitialized) {
            gameLayer.beginDraw();
            gameLayer.hint(ENABLE_STROKE_PURE);
            gameLayer.endDraw();

            hudLayer.beginDraw();
            hudLayer.hint(ENABLE_STROKE_PURE);
            hudLayer.endDraw();

            hintsInitialized = true;
        }

        // Draw game layer with screen shake
        gameLayer.beginDraw();
        gameLayer.background(0);

        // Apply screen shake offset
        gameLayer.pushMatrix();
        gameLayer.translate(shakeOffset.x, shakeOffset.y);

        // Draw parallax starfield
        for (Star star : stars) {
            star.display(gameLayer);
        }

        // Draw space clouds (very far back)
        for (SpaceCloud cloud : spaceClouds) {
            cloud.display(gameLayer);
        }

        // Draw distant planets (far back)
        for (DistantPlanet planet : distantPlanets) {
            planet.display(gameLayer);
        }

        // Draw particle swarms (mid-background)
        for (ParticleSwarm swarm : particleSwarms) {
            swarm.display(gameLayer);
        }

        // Draw shooting stars (behind everything else)
        for (ShootingStar ss : shootingStars) {
            ss.display(gameLayer);
        }

        // Draw supernovas (very far back)
        for (Supernova sn : supernovas) {
            sn.display(gameLayer);
        }

        // Draw background image if available
        if (backgroundImg != null && backgroundImg.width > 0) {
            gameLayer.pushStyle();
            gameLayer.imageMode(CORNER);
            gameLayer.tint(255, 100); // Make it subtle

            float scaleX = (float) width / backgroundImg.width;
            float scaleY = (float) height / backgroundImg.height;
            float scale = Math.max(scaleX, scaleY);

            float imgWidth = backgroundImg.width * scale;
            float imgHeight = backgroundImg.height * scale;
            float x = (width - imgWidth) / 2;
            float y = (height - imgHeight) / 2;

            gameLayer.image(backgroundImg, x, y, imgWidth, imgHeight);
            gameLayer.popStyle();
        }

        // Draw micro debris (AFTER background image so it's visible)
        for (MicroDebris debris : microDebris) {
            debris.display(gameLayer);
        }

        // Draw ambient dust (Vordergrund - über allem)
        for (AmbientDust dust : ambientDust) {
            dust.display(gameLayer);
        }

        // Batch rendering - set mode once
        gameLayer.imageMode(CENTER);

        // Draw particles first (background layer)
        for (Particle p : particles) {
            p.display(gameLayer);
        }

        // Draw objects back to front
        for (PowerUp p : powerups) {
            p.display(gameLayer, powerupImgs);
        }

        for (Mob m : mobs) {
            m.display(gameLayer);
        }

        for (EnemyShip ship : enemyShips) {
            ship.display(gameLayer);
        }

        // Draw formation ships
        for (EnemyFormation formation : enemyFormations) {
            for (EnemyShip ship : formation.ships) {
                ship.display(gameLayer);
            }
        }

        // Draw boss if active
        if (boss != null && !boss.isDead()) {
            boss.display(gameLayer);
        }

        for (Bomb bomb : bombs) {
            bomb.display(gameLayer);
        }

        for (Bullet b : bullets) {
            b.display(gameLayer, bulletImg);
        }

        for (Explosion e : explosions) {
            e.display(gameLayer, explosionAnims);
        }

        // Always show player during boss explosion (Phase 4), otherwise normal visibility rules
        if (fourthPhaseActive || (!player.isDead() && gameState != GameState.GAME_OVER)) {
            player.display(gameLayer, playerImg, inputHandler.isLeftPressed(), inputHandler.isRightPressed());
        }

        // Draw score popups
        for (ScorePopup sp : scorePopups) {
            sp.display(gameLayer);
        }

        // Draw shield hit effects
        for (ShieldHitEffect hit : shieldHits) {
            hit.display(gameLayer);
        }

        gameLayer.popMatrix();
        gameLayer.endDraw();

        // Draw HUD layer only when needed - major performance optimization
        // HUD is cached and only redrawn when score/lives/shield changes
        if (hudNeedsRedraw || gameState == GameState.GAME_OVER || gameState == GameState.SUMMARY ||
                gameState == GameState.SECOND_SUMMARY || gameState == GameState.THIRD_SUMMARY) {
            hudLayer.beginDraw();
            hudLayer.clear();
            drawHUD();
            if (gameState == GameState.GAME_OVER) {
                drawGameOver();
            } else if (gameState == GameState.SUMMARY || gameState == GameState.SECOND_SUMMARY ||
                    gameState == GameState.THIRD_SUMMARY) {
                drawSummaryScreen();
            }
            hudLayer.endDraw();
            hudNeedsRedraw = false;
        }

        // Composite layers
        pushStyle();
        imageMode(CORNER);
        image(gameLayer, 0, 0);
        image(hudLayer, 0, 0);
        popStyle();
    }

    void updateFPS() {
        frameCounter++;
        if (millis() - lastFPSUpdate > 1000) {
            currentFPS = frameCounter * 1000f / (millis() - lastFPSUpdate);
            frameCounter = 0;
            lastFPSUpdate = millis();
            if (showFPS || showDebug) {
                hudNeedsRedraw = true;
            }
        }
    }

    void drawHUD() {
        hudLayer.fill(255);
        hudLayer.textSize(18);
        hudLayer.textAlign(LEFT, TOP);
        hudLayer.text("Score: " + score, 10, 10);

        // Show timer during running state
        if (gameState == GameState.RUNNING) {
            if (!summaryShown) {
                // Phase 1: Show countdown to first summary
                float timeRemaining = summaryTriggerTime - gameTimer;
                int minutes = (int) (timeRemaining / 60);
                int seconds = (int) (timeRemaining % 60);
                hudLayer.fill(255, 255, 100); // Yellow color for timer
                hudLayer.textSize(28);
                hudLayer.textAlign(CENTER, TOP);
                hudLayer.text(String.format("Phase 1: %d:%02d", minutes, seconds), width / 2.0f, 10);
                hudLayer.fill(255); // Reset to white
                hudLayer.textSize(20);
                hudLayer.textAlign(LEFT, TOP);
            } else if (secondPhaseActive && !secondSummaryShown) {
                // Phase 2: Show countdown to second summary
                float timeRemaining = SECOND_PHASE_DURATION - secondPhaseTimer;
                int minutes = (int) (timeRemaining / 60);
                int seconds = (int) (timeRemaining % 60);
                hudLayer.fill(100, 255, 255); // Cyan color for phase 2 timer
                hudLayer.textSize(28);
                hudLayer.textAlign(CENTER, TOP);
                hudLayer.text(String.format("Phase 2: %d:%02d", minutes, seconds), width / 2.0f, 10);
                hudLayer.fill(255); // Reset to white
                hudLayer.textSize(20);
                hudLayer.textAlign(LEFT, TOP);
            } else if (thirdPhaseActive && !thirdSummaryShown) {
                // Phase 3: Show countdown to third summary
                float timeRemaining = THIRD_PHASE_DURATION - thirdPhaseTimer;
                int minutes = (int) (timeRemaining / 60);
                int seconds = (int) (timeRemaining % 60);
                hudLayer.fill(255, 150, 255); // Magenta color for phase 3 timer
                hudLayer.textSize(28);
                hudLayer.textAlign(CENTER, TOP);
                hudLayer.text(String.format("Phase 3: %d:%02d", minutes, seconds), width / 2.0f, 10);
                hudLayer.fill(255); // Reset to white
                hudLayer.textSize(20);
                hudLayer.textAlign(LEFT, TOP);
            } else if (fourthPhaseActive) {
                // Phase 4: Boss fight - show elapsed time
                int minutes = (int) (gameTimer / 60);
                int seconds = (int) (gameTimer % 60);
                hudLayer.fill(255, 50, 50); // Red color for boss phase
                hudLayer.textSize(28);
                hudLayer.textAlign(CENTER, TOP);
                hudLayer.text(String.format("BOSS FIGHT: %d:%02d", minutes, seconds), width / 2.0f, 10);

                // Draw boss health bar below the phase text
                if (boss != null && !boss.isDead()) {
                    float barWidth = 400;
                    float barHeight = 20;
                    float barX = (width - barWidth) / 2;
                    float barY = 45; // Below the phase text
                    boss.drawHealthBar(hudLayer, barX, barY, barWidth, barHeight);
                }

                hudLayer.fill(255); // Reset to white
                hudLayer.textSize(20);
                hudLayer.textAlign(LEFT, TOP);
            }
        }

        if (showFPS) {
            hudLayer.text(String.format("FPS: %.1f", currentFPS), 10, 35);
        }

        if (godMode) {
            hudLayer.fill(255, 215, 0); // Gold color
            hudLayer.text("GOD MODE", 10, 60);
            hudLayer.fill(255); // Reset to white
        }

        if (showDebug) {
            int debugY = godMode ? 85 : 60; // Adjust position if god mode is shown
            hudLayer.text("Mobs: " + mobs.size(), 10, debugY);
            hudLayer.text("Ships: " + enemyShips.size(), 10, debugY + 25);
            hudLayer.text("Bullets: " + bullets.size(), 10, debugY + 50);
            hudLayer.text("Particles: " + particles.size(), 10, debugY + 75);
            hudLayer.text("Explosions: " + explosions.size(), 10, debugY + 100);
            hudLayer.text("Sound: " + (soundEnabled ? "On" : "Off"), 10, debugY + 125);
            hudLayer.text("Volume: " + nf(masterVolume, 0, 2), 10, debugY + 150);
            hudLayer.text("Fullscreen: " + (isFullscreen ? "On" : "Off") + " (G)", 10, debugY + 175);
        }

        drawShieldBar(hudLayer, width - 105, player.shield);

        hudLayer.imageMode(CENTER);
        for (int i = 0; i < player.lives; i++) {
            hudLayer.image(playerMiniImg, width - 27 - i * 25, 32);
        }
    }

    void drawShieldBar(PGraphics pg, float x, float val) {
        float w = 100;
        float h = 10;
        float y = 5;
        float maxVal = 100;
        float ratio = val / maxVal;

        // Draw background
        pg.noFill();
        pg.stroke(255);
        pg.rect(x, y, w, h);

        // Draw fill with color gradient based on health
        if (ratio > 0.6) {
            pg.fill(0, 255, 0);
        } else if (ratio > 0.3) {
            pg.fill(255, 255, 0);
        } else {
            pg.fill(255, 0, 0);
        }
        pg.noStroke();
        pg.rect(x, y, w * ratio, h);
    }

    void drawGameOver() {
        // Check if this is a victory (boss defeated) or regular game over
        boolean victory = bossDefeated;

        if (victory) {
            // Victory screen
            hudLayer.fill(255, 215, 0); // Gold color
            hudLayer.textSize(72);
            hudLayer.textAlign(CENTER, CENTER);
            hudLayer.text("VICTORY!", width / 2.0f, height / 2.0f - 120);

            hudLayer.fill(255, 255, 100);
            hudLayer.textSize(32);
            hudLayer.text("Boss Defeated!", width / 2.0f, height / 2.0f - 70);
        } else {
            // Regular game over
            hudLayer.fill(255);
            hudLayer.textSize(64);
            hudLayer.textAlign(CENTER, CENTER);
            hudLayer.text("GAME OVER", width / 2.0f, height / 2.0f - 120);
        }

        // Statistics box
        hudLayer.fill(50, 50, 100, 200);
        hudLayer.stroke(100, 200, 255);
        hudLayer.strokeWeight(2);
        hudLayer.rect(width / 2.0f - 250, height / 2.0f - 30, 500, 180, 10);

        // Final statistics
        hudLayer.fill(255);
        hudLayer.textSize(28);
        hudLayer.textAlign(CENTER, CENTER);
        hudLayer.text("FINAL STATISTICS", width / 2.0f, height / 2.0f);

        hudLayer.textSize(22);
        hudLayer.text("Total Score: " + score, width / 2.0f, height / 2.0f + 40);
        hudLayer.text("Enemies Killed: " + enemiesKilled, width / 2.0f, height / 2.0f + 70);
        hudLayer.text("Asteroids Destroyed: " + mobsKilled, width / 2.0f, height / 2.0f + 100);
        hudLayer.text("Ships Destroyed: " + shipsKilled, width / 2.0f, height / 2.0f + 130);

        // Restart prompt
        hudLayer.fill(200, 255, 200);
        hudLayer.textSize(24);
        hudLayer.text("Press 'R' to restart", width / 2.0f, height - 80);
    }

    void drawSummaryScreen() {
        // Semi-transparent background overlay
        hudLayer.fill(0, 0, 0, 200);
        hudLayer.noStroke();
        hudLayer.rect(0, 0, width, height);

        // Title and content based on which summary screen we're showing
        if (gameState == GameState.SUMMARY) {
            // First summary screen
            hudLayer.fill(100, 200, 255);
            hudLayer.textSize(48);
            hudLayer.textAlign(CENTER, CENTER);
            hudLayer.text("MILESTONE REACHED!", width / 2.0f, 100);

            // Statistics box
            hudLayer.fill(50, 50, 150, 200);
            hudLayer.stroke(100, 200, 255);
            hudLayer.strokeWeight(2);
            hudLayer.rect(width / 2.0f - 200, 150, 400, 200, 10);

            // Stats
            hudLayer.fill(255);
            hudLayer.textSize(24);
            hudLayer.text("Time: " + nf((int) gameTimer / 60, 1) + ":" + nf((int) gameTimer % 60, 2), width / 2.0f, 180);
            hudLayer.text("Asteroids Destroyed: " + mobsKilled, width / 2.0f, 220);
            hudLayer.text("Enemy Ships Destroyed: " + shipsKilled, width / 2.0f, 260);
            hudLayer.text("Total Score: " + score, width / 2.0f, 300);

            // Instructions
            hudLayer.fill(200, 255, 200);
            hudLayer.textSize(20);
            hudLayer.text("Phase 1 Complete! (1 minute)", width / 2.0f, 360);
            hudLayer.text("New Challenge: Enemy Formations!", width / 2.0f, 390);

            // Continue prompt
            hudLayer.fill(200, 255, 200);
            hudLayer.textSize(18);
            hudLayer.text("Press 'C' to continue to Phase 2 (1 minute)", width / 2.0f, height - 100);
        } else if (gameState == GameState.SECOND_SUMMARY) {
            // Second summary screen
            hudLayer.fill(255, 200, 100);
            hudLayer.textSize(48);
            hudLayer.textAlign(CENTER, CENTER);
            hudLayer.text("PHASE 2 COMPLETE!", width / 2.0f, 100);

            // Statistics box
            hudLayer.fill(80, 50, 100, 200);
            hudLayer.stroke(200, 100, 255);
            hudLayer.strokeWeight(2);
            hudLayer.rect(width / 2.0f - 200, 150, 400, 150, 10);

            // Stats
            hudLayer.fill(255);
            hudLayer.textSize(24);
            hudLayer.text("Phase 2 Complete! (1 minute)", width / 2.0f, 180);
            hudLayer.text("Enemy Ships Defeated: " + shipsKilled, width / 2.0f, 220);
            hudLayer.textSize(20);
            hudLayer.fill(255, 220, 150);
            hudLayer.text("Phase 3: Smaller formations (1-3 ships)", width / 2.0f, 260);

            // Continue prompt
            hudLayer.fill(255, 220, 150);
            hudLayer.textSize(18);
            hudLayer.text("Press 'C' to continue to Phase 3 (1 minute)", width / 2.0f, height - 100);
        } else if (gameState == GameState.THIRD_SUMMARY) {
            // Third summary screen
            hudLayer.fill(255, 100, 255);
            hudLayer.textSize(48);
            hudLayer.textAlign(CENTER, CENTER);
            hudLayer.text("PHASE 3 COMPLETE!", width / 2.0f, 100);

            // Statistics box
            hudLayer.fill(100, 20, 80, 200);
            hudLayer.stroke(255, 100, 255);
            hudLayer.strokeWeight(2);
            hudLayer.rect(width / 2.0f - 200, 150, 400, 200, 10);

            // Stats
            hudLayer.fill(255);
            hudLayer.textSize(24);
            hudLayer.text("Phase 3 Complete! (1 minute)", width / 2.0f, 180);
            hudLayer.text("Total Enemies Defeated: " + enemiesKilled, width / 2.0f, 220);
            hudLayer.text("Current Score: " + score, width / 2.0f, 260);

            // Boss warning
            hudLayer.fill(255, 50, 50);
            hudLayer.textSize(32);
            hudLayer.text("⚠ BOSS INCOMING ⚠", width / 2.0f, 320);

            // Continue prompt
            hudLayer.fill(255, 200, 200);
            hudLayer.textSize(18);
            hudLayer.text("Press 'C' to face the FINAL BOSS!", width / 2.0f, height - 100);
        }
    }

    @Override
    public void exit() {
        // Clean shutdown: stop sounds and dispose resources
        try {
            if (soundEnabled) {
                if (shootSound != null) shootSound.stop();
                if (explosionSound != null) explosionSound.stop();
                if (enemyExplosionSound != null) enemyExplosionSound.stop();
                if (asteroidExplosionSound != null) asteroidExplosionSound.stop();
                if (powerupSound != null) powerupSound.stop();
                if (hitSound != null) hitSound.stop();
                if (trackingBombSound != null) trackingBombSound.stop();
            }
        } catch (Exception e) {
            // Ignore any errors during cleanup
        }

        // Call parent exit to properly close window
        super.exit();
    }

    public void keyPressed(processing.event.KeyEvent event) {
        // No restart functionality in GAME_OVER state
        if (gameState == GameState.GAME_OVER) {
            return; // Ignore all input in GAME_OVER state
        }

        if (key == 'c' || key == 'C') {
            if (gameState == GameState.SUMMARY) {
                // Continue from first summary screen - activate formation mode and start second phase
                // Clear all existing enemy ships and formations for a fresh start
                enemyShips.clear();
                enemyFormations.clear();
                bombs.clear(); // Also clear any existing bombs

                gameState = GameState.RUNNING;
                secondPhaseActive = true;
                formationModeActive = true;
                secondPhaseTimer = 0; // Reset second phase timer
                hudNeedsRedraw = true;
            } else if (gameState == GameState.SECOND_SUMMARY) {
                // Continue from second summary screen - start Phase 3
                // Clear all existing enemies for a fresh start (formations only in Phase 3)
                enemyShips.clear();
                enemyFormations.clear();
                bombs.clear(); // Also clear any existing bombs
                mobs.clear(); // Clear all asteroids for Phase 3

                gameState = GameState.RUNNING;
                thirdPhaseActive = true;
                thirdPhaseTimer = 0; // Reset third phase timer
                hudNeedsRedraw = true;
            } else if (gameState == GameState.THIRD_SUMMARY) {
                // Continue from third summary screen - start Phase 4 (Boss Fight)
                // Clear all existing enemies and projectiles
                enemyShips.clear();
                enemyFormations.clear();
                bombs.clear();
                mobs.clear();
                powerups.clear(); // Also clear powerups for clean boss fight
                supernovas.clear(); // Clear supernovas for Phase 4
                particleSwarms.clear(); // Clear particle swarms for Phase 4

                // Ensure player has full lives and shield for boss fight
                player.lives = 3;
                player.shield = 100;

                // Spawn the boss
                spawnBoss();

                gameState = GameState.RUNNING;
                fourthPhaseActive = true;
                hudNeedsRedraw = true;
            }
        } else if (key == 'f' || key == 'F') {
            showFPS = !showFPS;
            hudNeedsRedraw = true;
        } else if (key == 'd' || key == 'D') {
            showDebug = !showDebug;
            hudNeedsRedraw = true;
        } else if (key == 'i' || key == 'I') {
            godMode = !godMode;
            hudNeedsRedraw = true;
        } else if (key == 'm' || key == 'M') {
            toggleMusic();
            hudNeedsRedraw = true;
        } else if (key == 'g' || key == 'G') {
            toggleFullscreen();
        } else if (key == '+' || key == '=') {
            adjustVolume(0.1f);
        } else if (key == '-' || key == '_') {
            adjustVolume(-0.1f);
        }

        // Delegate to InputHandler for movement and shooting keys
        inputHandler.keyEvent(event);
    }

    public void keyReleased(processing.event.KeyEvent event) {
        // Delegate to InputHandler
        inputHandler.keyEvent(event);
    }

    // Getters for managers
    public GameManager getGameManager() {
        return gameManager;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public InputHandler getInputHandler() {
        return inputHandler;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public UIManager getUIManager() {
        return uiManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    /**
     * Helper method to safely obtain a particle from the pool.
     * Returns null if the pool is exhausted (silently fails to prevent crashes).
     */
    private Particle safeObtainParticle() {
        return particlePool.obtain();
    }

    /**
     * Helper method to safely create and add a particle with null check.
     * Silently fails if pool is exhausted.
     */
    private void addParticleIfAvailable(float x, float y, float vx, float vy, int color) {
        Particle p = particlePool.obtain();
        if (p != null) {
            p.reset(x, y, vx, vy, color);
            particles.add(p);
        }
    }
}
