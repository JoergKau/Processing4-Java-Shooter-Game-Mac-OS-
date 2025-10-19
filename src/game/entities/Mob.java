package game.entities;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import java.util.ArrayList;

// Represents a meteor or asteroid object with position, speed, and rotation.
public class Mob {
    private PApplet sketch;
    public PVector pos;
    public PVector speed;
    public float rotation = 0;
    public float rotationSpeed;
    public int radius;
    public PImage img;

    public Mob(PApplet sketch, ArrayList<PImage> meteorImgs) {
        this.sketch = sketch;
        this.pos = new PVector();
        this.speed = new PVector();
        img = meteorImgs.get((int) sketch.random(meteorImgs.size()));
        radius = (int) (img.width / 2.0f);
        reset();
    }

    public void reset() {
        pos.set(sketch.random(radius, sketch.width - radius), -radius);
        speed.set(sketch.random(-100, 100), sketch.random(50, 150));

        // 50% der Asteroiden drehen sich, 50% bleiben statisch
        if (sketch.random(1) < 0.5f) {
            // Keine Rotation für statische Asteroiden
            rotationSpeed = 0;
        } else {
            // Unterschiedliche Rotationsgeschwindigkeiten für sich drehende Asteroiden
            // Zufällige Richtung: im Uhrzeigersinn (positiv) oder gegen den Uhrzeigersinn (negativ)
            float direction = sketch.random(1) < 0.5f ? -1 : 1;

            // Viel höhere Rotationsgeschwindigkeiten (in Radianten pro Sekunde)
            float baseSpeed = sketch.random(1.2f, 3.8f); // Grundgeschwindigkeit in Radianten
            float sizeMultiplier = radius / 25.0f; // Größere Asteroiden drehen sich langsamer

            rotationSpeed = direction * baseSpeed / Math.max(sizeMultiplier, 0.5f);
        }
    }

    public void update(float delta_time) {
        pos.x += speed.x * delta_time;
        pos.y += speed.y * delta_time;
        rotation += rotationSpeed * delta_time;
    }
    
    public boolean shouldSpawnDustParticle(int frameCount) {
        return frameCount % 2 == 0;
    }
    
    public float getDustParticleX() {
        float offsetX = sketch.random(-radius * 0.5f, radius * 0.5f);
        return pos.x + offsetX;
    }
    
    public float getDustParticleY() {
        float offsetY = sketch.random(-radius * 0.5f, radius * 0.5f);
        return pos.y + offsetY;
    }
    
    public float getDustParticleVelX() {
        return sketch.random(-30, 30);
    }
    
    public float getDustParticleVelY() {
        return sketch.random(10, 50);
    }
    
    public int getDustParticleColor() {
        return sketch.color(sketch.random(80, 120), sketch.random(60, 90), sketch.random(40, 70), 180);
    }
    
    public float getDustParticleSize() {
        float sizeMultiplier = radius / 20.0f;
        return sketch.random(2, 4) * sizeMultiplier;
    }
    
    public boolean shouldSpawnSecondaryDust() {
        return sketch.random(1) < 0.5f;
    }
    
    public float getSecondaryDustX() {
        return pos.x + sketch.random(-radius, radius);
    }
    
    public float getSecondaryDustY() {
        return pos.y + sketch.random(-radius, radius);
    }
    
    public float getSecondaryDustVelX() {
        return sketch.random(-20, 20);
    }
    
    public float getSecondaryDustVelY() {
        return sketch.random(5, 30);
    }
    
    public int getSecondaryDustColor() {
        return sketch.color(sketch.random(60, 100), sketch.random(50, 80), sketch.random(30, 60), 120);
    }
    
    public float getSecondaryDustSize() {
        float sizeMultiplier = radius / 20.0f;
        return sketch.random(1.5f, 3) * sizeMultiplier;
    }

    public boolean isOffScreen() {
        return pos.y > sketch.height + radius || pos.x < -radius || pos.x > sketch.width + radius;
    }

    public void display(PGraphics pg) {
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        pg.rotate(rotation); // Entferne sketch.radians() da rotation bereits in Radianten ist
        pg.image(img, 0, 0);
        pg.popMatrix();
    }
}
