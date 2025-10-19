package game.entities;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

// Represents a bullet fired by the player.
public class Bullet {
    private PApplet sketch;
    public PVector pos;
    public float speed = 500;
    public int radius = 5;
    public boolean active = true;
    public float startY; // Startposition für Fade-Berechnung
    public float maxReach; // Maximale Reichweite (80% des Bildschirms)

    public Bullet(PApplet sketch, float x, float y) {
        this.sketch = sketch;
        this.pos = new PVector(x, y);
        this.startY = y;
        this.maxReach = sketch.height * 0.75f; // Erreicht nur 75% der Bildschirmhöhe
    }

    public void reset(float x, float y) {
        this.pos.set(x, y);
        this.active = true;
        this.startY = y;
        this.maxReach = sketch.height * 0.75f;
    }

    public void update(float delta_time) {
        pos.y -= speed * delta_time;
        
        // Deaktiviere Bullet wenn es obere 25% erreicht
        if (pos.y < sketch.height * 0.25f) {
            active = false;
        }
    }
    
    public boolean shouldSpawnTrailParticle(int frameCount) {
        return frameCount % 5 == 0;
    }
    
    public float getTrailParticleX() {
        return pos.x + sketch.random(-1, 1);
    }
    
    public float getTrailParticleY() {
        return pos.y + 5;
    }

    public boolean isOffScreen() {
        return pos.y < -radius;
    }
    
    public float getAlpha() {
        // Fade-out in den oberen 30% der Reichweite
        float travelDistance = startY - pos.y;
        float fadeStartDistance = maxReach * 0.7f;
        
        if (travelDistance > fadeStartDistance) {
            // Berechne Fade (1.0 = voll sichtbar, 0.0 = unsichtbar)
            float fadeProgress = (travelDistance - fadeStartDistance) / (maxReach * 0.3f);
            return PApplet.constrain(1.0f - fadeProgress, 0.0f, 1.0f);
        }
        return 1.0f; // Voll sichtbar
    }

    public void display(PGraphics pg, PImage bulletImg) {
        float alpha = getAlpha() * 255;
        
        if (bulletImg != null) {
            pg.pushStyle();
            pg.tint(255, alpha);
            pg.image(bulletImg, pos.x, pos.y);
            pg.popStyle();
        } else {
            pg.pushStyle();
            pg.fill(255, 0, 0, alpha);
            pg.noStroke();
            pg.rectMode(PApplet.CENTER);
            pg.rect(pos.x, pos.y, 5, 15);
            pg.popStyle();
        }
    }
}
