package game.managers;

import game.Sketch;
import game.entities.*;
import java.util.Random;

/**
 * Manages game levels, phases, and enemy spawning logic.
 */
public class LevelManager {
    private final Sketch sketch;
    private final Random random;
    
    // Phase tracking
    private int currentPhase = 1;
    private float phaseTimer = 0;
    private boolean phaseSummaryShown = false;
    
    // Phase durations (in seconds)
    private static final float PHASE_1_DURATION = 60.0f;
    private static final float PHASE_2_DURATION = 60.0f;
    private static final float PHASE_3_DURATION = 60.0f;
    
    // Spawn timers
    private long lastMobSpawnTime = 0;
    private long lastEnemyShipSpawnTime = 0;
    private long mobSpawnInterval = 1000; // milliseconds
    private long enemyShipSpawnInterval = 3000; // milliseconds
    
    // Spawn counts
    private int minMobs = 5;
    private int maxMobs = 15;
    private int minEnemyShips = 0;
    private int maxEnemyShips = 5;
    
    // Difficulty scaling
    private float difficultyMultiplier = 1.0f;
    
    public LevelManager(Sketch sketch) {
        this.sketch = sketch;
        this.random = new Random();
    }
    
    public void update(float deltaTime) {
        phaseTimer += deltaTime;
        
        // Update phase progression
        updatePhaseProgression();
        
        // Spawn enemies based on current phase
        spawnEnemies();
        
        // Update difficulty over time
        updateDifficulty();
    }
    
    private void updatePhaseProgression() {
        switch (currentPhase) {
            case 1:
                if (phaseTimer >= PHASE_1_DURATION && !phaseSummaryShown) {
                    triggerPhaseSummary(1);
                    advanceToPhase(2);
                }
                break;
            case 2:
                if (phaseTimer >= PHASE_2_DURATION && !phaseSummaryShown) {
                    triggerPhaseSummary(2);
                    advanceToPhase(3);
                }
                break;
            case 3:
                if (phaseTimer >= PHASE_3_DURATION && !phaseSummaryShown) {
                    triggerPhaseSummary(3);
                    advanceToPhase(4);
                }
                break;
            case 4:
                // Boss phase - handled separately
                break;
        }
    }
    
    private void spawnEnemies() {
        long currentTime = System.currentTimeMillis();
        
        // Spawn mobs (asteroids)
        if (currentTime - lastMobSpawnTime > mobSpawnInterval) {
            spawnMobs();
            lastMobSpawnTime = currentTime;
        }
        
        // Spawn enemy ships (phase 2+)
        if (currentPhase >= 2 && currentTime - lastEnemyShipSpawnTime > enemyShipSpawnInterval) {
            spawnEnemyShips();
            lastEnemyShipSpawnTime = currentTime;
        }
        
        // Spawn formations (phase 2+)
        if (currentPhase >= 2) {
            spawnFormations();
        }
        
        // Spawn boss (phase 4)
        if (currentPhase == 4) {
            spawnBoss();
        }
    }
    
    private void spawnMobs() {
        // Get current mob count from entity manager
        // int currentMobCount = sketch.getEntityManager().getEntitiesByType(Mob.class).size();
        // 
        // if (currentMobCount < minMobs) {
        //     int toSpawn = minMobs - currentMobCount;
        //     for (int i = 0; i < toSpawn; i++) {
        //         float x = random.nextFloat() * sketch.width;
        //         float y = -50;
        //         Mob mob = new Mob(sketch, x, y);
        //         sketch.getEntityManager().addEntity(mob);
        //     }
        // }
    }
    
    private void spawnEnemyShips() {
        // Get current enemy ship count
        // int currentShipCount = sketch.getEntityManager().getEntitiesByType(EnemyShip.class).size();
        // 
        // if (currentShipCount < maxEnemyShips) {
        //     float x = random.nextFloat() * sketch.width;
        //     float y = -50;
        //     EnemyShip ship = new EnemyShip(sketch, x, y);
        //     sketch.getEntityManager().addEntity(ship);
        // }
    }
    
    private void spawnFormations() {
        // Spawn enemy formations periodically
        // Implementation depends on your EnemyFormation class
    }
    
    private void spawnBoss() {
        // Spawn boss if not already spawned
        // if (sketch.boss == null) {
        //     sketch.boss = new Boss(sketch, sketch.width / 2, -100);
        //     sketch.getEntityManager().addEntity(sketch.boss);
        // }
    }
    
    private void updateDifficulty() {
        // Gradually increase difficulty over time
        difficultyMultiplier = 1.0f + (phaseTimer / 60.0f) * 0.1f; // 10% increase per minute
        
        // Adjust spawn rates based on difficulty
        mobSpawnInterval = (long) (1000 / difficultyMultiplier);
        enemyShipSpawnInterval = (long) (3000 / difficultyMultiplier);
    }
    
    private void advanceToPhase(int phase) {
        currentPhase = phase;
        phaseTimer = 0;
        phaseSummaryShown = false;
        
        // Adjust spawn parameters for new phase
        switch (phase) {
            case 2:
                minEnemyShips = 2;
                maxEnemyShips = 5;
                break;
            case 3:
                minEnemyShips = 3;
                maxEnemyShips = 8;
                maxMobs = 20;
                break;
            case 4:
                // Boss phase - reduce regular spawns
                maxMobs = 10;
                maxEnemyShips = 3;
                break;
        }
    }
    
    private void triggerPhaseSummary(int phase) {
        phaseSummaryShown = true;
        // Set game state to show summary
        // sketch.getGameManager().setGameState(GameManager.GameState.SUMMARY);
    }
    
    public void reset() {
        currentPhase = 1;
        phaseTimer = 0;
        phaseSummaryShown = false;
        difficultyMultiplier = 1.0f;
        lastMobSpawnTime = 0;
        lastEnemyShipSpawnTime = 0;
    }
    
    // Getters
    public int getCurrentPhase() {
        return currentPhase;
    }
    
    public float getPhaseTimer() {
        return phaseTimer;
    }
    
    public float getDifficultyMultiplier() {
        return difficultyMultiplier;
    }
}
