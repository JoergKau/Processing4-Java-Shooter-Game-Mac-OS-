package game.managers;

import processing.core.PApplet;
import game.Sketch;

public class GameManager {
    private final Sketch sketch;
    private GameState gameState = GameState.MENU;
    private float gameTimer = 0;
    private float deltaTime = 0;
    private long lastUpdateTime = 0;

    public enum GameState {
        MENU, RUNNING, SUMMARY, SECOND_SUMMARY, THIRD_SUMMARY, GAME_OVER
    }

    public GameManager(Sketch sketch) {
        this.sketch = sketch;
        this.lastUpdateTime = System.nanoTime();
    }

    public void update() {
        long now = System.nanoTime();
        deltaTime = (now - lastUpdateTime) * 1e-9f; // Convert to seconds
        deltaTime = Math.min(deltaTime, 0.1f); // Cap delta time to prevent physics issues
        lastUpdateTime = now;
        
        if (gameState == GameState.RUNNING) {
            gameTimer += deltaTime;
        }
    }

    public void setGameState(GameState state) {
        this.gameState = state;
        // Reset relevant timers or states when changing game states
        if (state == GameState.RUNNING) {
            gameTimer = 0;
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public float getGameTimer() {
        return gameTimer;
    }

    public float getDeltaTime() {
        return deltaTime;
    }
}
