package game.entities;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

// Represents a bomb dropped by enemies, possibly tracking the player.
public class Bomb {
    private PApplet sketch;
    public PVector pos;
    public PVector vel;
    public int radius = 8;
    public float rotation = 0;
    public float rotationSpeed;
    public int col;
    public float speedMultiplier;
    public boolean isTracking; // Verfolgt den Spieler
    public float trackingStrength = 150f; // Stärke der Verfolgung

    public Bomb(PApplet sketch, float x, float y, float shipSizeRatio, PVector playerPos) {
        this.sketch = sketch;
        this.pos = new PVector(x, y);
        // Geschwindigkeit basierend auf Schiffsgröße
        // Kleinere Schiffe (0.85) -> langsamere Bomben
        // Größere Schiffe (1.4) -> schnellere Bomben
        speedMultiplier = 0.7f + (shipSizeRatio * 0.6f); // 0.7-1.3x Multiplikator
        
        // 30% Chance für tracking Bombe
        isTracking = sketch.random(1) < 0.3f;
        
        if (isTracking) {
            // Tracking-Bomben: Initiale Richtung zum Spieler
            PVector toPlayer = PVector.sub(playerPos, pos);
            toPlayer.normalize();
            float speed = sketch.random(150, 250) * speedMultiplier;
            this.vel = toPlayer.mult(speed);
            // Tracking-Bomben haben eine andere Farbe (mehr rötlich)
            this.col = sketch.color(255, sketch.random(0, 50), sketch.random(0, 50));
        } else {
            // Normal Bomben fallen nach unten mit leichter seitlicher Drift
            this.vel = new PVector(
                sketch.random(-30, 30) * speedMultiplier, 
                sketch.random(150, 250) * speedMultiplier
            );
            // Normale orange Farbe
            this.col = sketch.color(sketch.random(200, 255), sketch.random(50, 100), 0);
        }
        
        this.rotationSpeed = sketch.random(-5, 5) * speedMultiplier;
    }

    public void update(float delta_time, PVector playerPos, boolean playerIsDead) {
        if (isTracking && !playerIsDead) {
            // Berechne Richtung zum Spieler
            PVector toPlayer = PVector.sub(playerPos, pos);
            toPlayer.normalize();
            toPlayer.mult(trackingStrength * speedMultiplier * delta_time);
            
            // Sanfte Anpassung der Geschwindigkeit in Richtung Spieler
            vel.add(toPlayer);
            
            // Begrenze maximale Geschwindigkeit
            float maxSpeed = 300 * speedMultiplier;
            if (vel.mag() > maxSpeed) {
                vel.normalize();
                vel.mult(maxSpeed);
            }
        } else {
            // Normale Bomben: Leichte Beschleunigung nach unten (Schwerkraft-Effekt)
            vel.y += 50 * delta_time;
        }
        
        pos.x += vel.x * delta_time;
        pos.y += vel.y * delta_time;
        rotation += rotationSpeed * delta_time;
    }
    
    public boolean shouldSpawnTrailParticle(int frameCount) {
        int trailFrequency = isTracking ? 1 : 3;
        return frameCount % trailFrequency == 0;
    }
    
    public float getTrailParticleX() {
        return pos.x + sketch.random(-radius * 0.5f, radius * 0.5f);
    }
    
    public float getTrailParticleY() {
        return pos.y + sketch.random(-radius * 0.5f, radius * 0.5f);
    }
    
    public float getTrailParticleVelX() {
        return sketch.random(-20, 20);
    }
    
    public float getTrailParticleVelY() {
        return sketch.random(-30, 10);
    }
    
    public int getTrailParticleColor() {
        return isTracking ? 
            sketch.color(255, 50, 50, 200) : // Rot für Tracking
            sketch.color(255, 150, 0, 150);  // Orange für normale
    }

    public boolean isOffScreen() {
        return pos.y > sketch.height + radius || pos.x < -radius || pos.x > sketch.width + radius;
    }

    public void display(PGraphics pg) {
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        pg.rotate(rotation);
        
        pg.pushStyle();
        
        if (isTracking) {
            // Tracking-Bomben: Intensiverer roter Glow
            pg.noStroke();
            pg.fill(255, 0, 0, 150);
            pg.ellipse(0, 0, radius * 4, radius * 4);
            
            // Haupt-Bombe (rot)
            pg.fill(col);
            pg.ellipse(0, 0, radius * 2, radius * 2);
            
            // Heller Kern (weißlich-rot)
            pg.fill(255, 150, 150);
            pg.ellipse(0, 0, radius, radius);
            
            // Tracking-Indikator: Pulsierendes Kreuz
            float pulse = 1.0f + PApplet.sin(sketch.millis() * 0.01f) * 0.3f;
            pg.stroke(255, 50, 50);
            pg.strokeWeight(2);
            pg.line(-radius * 0.9f * pulse, 0, radius * 0.9f * pulse, 0);
            pg.line(0, -radius * 0.9f * pulse, 0, radius * 0.9f * pulse);
        } else {
            // Normale Bomben: Äußerer Glow
            pg.noStroke();
            pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), 100);
            pg.ellipse(0, 0, radius * 3, radius * 3);
            
            // Haupt-Bombe
            pg.fill(col);
            pg.ellipse(0, 0, radius * 2, radius * 2);
            
            // Heller Kern
            pg.fill(255, 200, 100);
            pg.ellipse(0, 0, radius, radius);
            
            // Kleine Markierung
            pg.stroke(255, 255, 0);
            pg.strokeWeight(2);
            pg.line(-radius * 0.7f, 0, radius * 0.7f, 0);
            pg.line(0, -radius * 0.7f, 0, radius * 0.7f);
        }
        
        pg.popStyle();
        pg.popMatrix();
    }
}
