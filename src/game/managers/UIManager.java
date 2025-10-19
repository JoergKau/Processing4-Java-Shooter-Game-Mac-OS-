package game.managers;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import game.Sketch;

public class UIManager {
    private final Sketch sketch;
    private PGraphics uiLayer;
    private boolean needsRedraw = true;
    private int lastScore = -1;
    private int lastLives = -1;
    private int lastShield = -1;
    
    // UI Elements
    private PImage healthIcon;
    private PImage shieldIcon;
    private PImage scoreIcon;
    
    public UIManager(Sketch sketch) {
        this.sketch = sketch;
        initializeUI();
    }
    
    private void initializeUI() {
        // Create a separate layer for UI elements
        uiLayer = sketch.createGraphics(sketch.width, sketch.height);
        
        // Load UI assets
        // healthIcon = sketch.getAssetManager().getImage("health_icon");
        // shieldIcon = sketch.getAssetManager().getImage("shield_icon");
        // scoreIcon = sketch.getAssetManager().getImage("score_icon");
    }
    
    public void update() {
        // Check if UI needs to be redrawn
        int currentScore = sketch.score;
        int currentLives = sketch.player != null ? sketch.player.lives : 0;
        int currentShield = sketch.player != null ? (int)sketch.player.shield : 0;
        
        if (currentScore != lastScore || currentLives != lastLives || currentShield != lastShield) {
            needsRedraw = true;
            lastScore = currentScore;
            lastLives = currentLives;
            lastShield = currentShield;
        }
    }
    
    public void render() {
        if (needsRedraw) {
            renderUI();
            needsRedraw = false;
        }
        sketch.image(uiLayer, 0, 0);
    }
    
    private void renderUI() {
        uiLayer.beginDraw();
        uiLayer.clear();
        
        // Draw HUD background
        uiLayer.noStroke();
        uiLayer.fill(0, 150);
        uiLayer.rect(0, 0, sketch.width, 40);
        
        // Draw score
        uiLayer.fill(255);
        uiLayer.textSize(20);
        uiLayer.textAlign(PApplet.LEFT, PApplet.CENTER);
        // if (scoreIcon != null) {
        //     uiLayer.image(scoreIcon, 10, 10, 20, 20);
        //     uiLayer.text(" " + lastScore, 35, 20);
        // } else {
            uiLayer.text("Score: " + lastScore, 10, 20);
        // }
        
        // Draw lives
        uiLayer.textAlign(PApplet.CENTER, PApplet.CENTER);
        // if (healthIcon != null) {
        //     uiLayer.image(healthIcon, sketch.width / 2 - 50, 10, 20, 20);
        //     uiLayer.text(" " + lastLives, sketch.width / 2 - 25, 20);
        // } else {
            uiLayer.text("Lives: " + lastLives, sketch.width / 2, 20);
        // }
        
        // Draw shield
        uiLayer.textAlign(PApplet.RIGHT, PApplet.CENTER);
        // if (shieldIcon != null) {
        //     uiLayer.image(shieldIcon, sketch.width - 80, 10, 20, 20);
        //     uiLayer.text(lastShield + "% ", sketch.width - 55, 20);
        // } else {
            uiLayer.text("Shield: " + lastShield + "%", sketch.width - 10, 20);
        // }
        
        // Draw game state specific UI
        switch (sketch.getGameManager().getGameState()) {
            case MENU:
                renderMenu();
                break;
            case GAME_OVER:
                renderGameOver();
                break;
            // Add other game states as needed
        }
        
        uiLayer.endDraw();
    }
    
    private void renderMenu() {
        uiLayer.textAlign(PApplet.CENTER, PApplet.CENTER);
        uiLayer.textSize(48);
        uiLayer.fill(255);
        uiLayer.text("SPACE SHOOTER", sketch.width / 2, sketch.height / 3);
        
        uiLayer.textSize(24);
        uiLayer.text("Press SPACE to Start", sketch.width / 2, sketch.height / 2);
        uiLayer.textSize(16);
        uiLayer.text("Use ARROW KEYS or A/D to move, SPACE to shoot", sketch.width / 2, sketch.height / 2 + 50);
    }
    
    private void renderGameOver() {
        uiLayer.fill(255, 0, 0, 200);
        uiLayer.rect(0, 0, sketch.width, sketch.height);
        
        uiLayer.textAlign(PApplet.CENTER, PApplet.CENTER);
        uiLayer.fill(255);
        uiLayer.textSize(48);
        uiLayer.text("GAME OVER", sketch.width / 2, sketch.height / 3);
        
        uiLayer.textSize(24);
        uiLayer.text("Final Score: " + lastScore, sketch.width / 2, sketch.height / 2);
        uiLayer.text("Press R to Restart", sketch.width / 2, sketch.height / 2 + 50);
    }
    
    public void resize(int width, int height) {
        if (uiLayer != null) {
            uiLayer.dispose();
        }
        uiLayer = sketch.createGraphics(width, height);
        needsRedraw = true;
    }
    
    public void dispose() {
        if (uiLayer != null) {
            uiLayer.dispose();
        }
    }
}
