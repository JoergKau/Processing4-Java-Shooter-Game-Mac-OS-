package game.powerups;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import java.util.HashMap;

// Represents a collectible power-up (shield or gun) in the game.
public class PowerUp {
    private PApplet sketch;
    public PVector pos;
    public float speed = 100;
    public int radius = 15;
    public String type;
    public float pulse = 0;

    public PowerUp(PApplet sketch, float x, float y) {
        this.sketch = sketch;
        this.pos = new PVector(x, y);
        type = sketch.random(1) > 0.5 ? "shield" : "gun";
    }

    public void update(float delta_time) {
        pos.y += speed * delta_time;
        pulse += delta_time * 5;
    }
    
    public boolean shouldSpawnAuraParticle(int frameCount) {
        return frameCount % 3 == 0;
    }
    
    public float getAuraParticleAngle() {
        return sketch.random(PApplet.TWO_PI);
    }
    
    public float getAuraParticleX(float angle) {
        float dist = radius + 10;
        return pos.x + sketch.cos(angle) * dist;
    }
    
    public float getAuraParticleY(float angle) {
        float dist = radius + 10;
        return pos.y + sketch.sin(angle) * dist;
    }
    
    public float getAuraParticleVelX(float angle) {
        return sketch.cos(angle) * sketch.random(20, 40);
    }
    
    public float getAuraParticleVelY(float angle) {
        return sketch.sin(angle) * sketch.random(20, 40);
    }
    
    public int getAuraParticleColor() {
        return type.equals("shield") ? 
            sketch.color(0, 255, 100, 150) : 
            sketch.color(255, 255, 0, 150);
    }

    public boolean isOffScreen() {
        return pos.y > sketch.height + radius;
    }

    public void display(PGraphics pg, HashMap<String, PImage> powerupImgs) {
        pg.pushStyle();

        // Pulsing glow effect
        float glowSize = 1.0f + sketch.sin(pulse) * 0.2f;
        pg.tint(255, 150);
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        pg.scale(glowSize);
        pg.image(powerupImgs.get(type), 0, 0);
        pg.popMatrix();

        // Main image
        pg.tint(255, 255);
        pg.image(powerupImgs.get(type), pos.x, pos.y);

        pg.popStyle();
    }
}
