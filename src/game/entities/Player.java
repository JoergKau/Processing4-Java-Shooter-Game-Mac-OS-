package game.entities;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

// Represents the player’s spaceship, including movement, shooting, shield, and lives.
public class Player {
    private PApplet sketch;
    public PVector pos;
    public float speed = 300; // Pixels per second
    public int radius = 20;   // Collision radius
    public int shield = 100;  // 0-100, regenerates on powerup
    public int lives = 3;     // Number of ships remaining
    public int power = 1;     // 1 = single shot, 2 = double shot
    public long lastShot = 0;
    public int shootDelay = 250;
    public float thrusterFlicker = 0;
    public float thrusterIntensity = 1.0f;
    public float nextFlickerChange = 0;
    public float lightPulse = 0;
    public float wingLightPhase = 0;
    public float cockpitGlow = 0;

    public Player(PApplet sketch) {
        this.sketch = sketch;
        this.pos = new PVector(sketch.width / 2.0f, sketch.height - 60);
    }

    public void update(float delta_time, boolean leftPressed, boolean rightPressed, 
                       boolean spacePressed) {
        if (leftPressed) {
            pos.x -= speed * delta_time;
        }
        if (rightPressed) {
            pos.x += speed * delta_time;
        }

        // Update thruster flicker for animation - IRREGULAR
        thrusterFlicker += delta_time * sketch.random(15, 35); // Variable speed
        
        // Irregular intensity changes
        float currentTime = sketch.millis() / 1000.0f;
        if (currentTime >= nextFlickerChange) {
            thrusterIntensity = sketch.random(0.7f, 1.3f); // Random intensity
            nextFlickerChange = currentTime + sketch.random(0.05f, 0.2f); // Next change in 50-200ms
        }
        
        // Update light animations
        lightPulse += delta_time * 8; // Fast pulse for status lights
        wingLightPhase += delta_time * 3; // Slower for wing lights
        cockpitGlow += delta_time * 5; // Medium speed for cockpit
        
        // No trail particles for player ship

        pos.x = PApplet.constrain(pos.x, radius, sketch.width - radius);
        pos.y = PApplet.constrain(pos.y, radius, sketch.height - radius);
    }
    
    public boolean shouldShoot(boolean spacePressed) {
        if (spacePressed && sketch.millis() - lastShot > shootDelay) {
            lastShot = sketch.millis();
            return true;
        }
        return false;
    }

    public void takeDamage(int amount) {
        shield -= amount;
        // Nicht hier Leben reduzieren - das macht handlePlayerDeath()
    }

    public void gainShield(int amount) {
        shield = PApplet.min(shield + amount, 100);
    }

    public void powerUp() {
        power = 2;
    }

    public boolean isDead() {
        return shield <= 0; // Schiff ist zerstört wenn Shield auf 0
    }

    public void display(PGraphics pg, PImage playerImg, boolean leftPressed, boolean rightPressed) {
        pg.pushStyle();

        // Draw main thruster FIRST (so it appears behind the ship)
        if (leftPressed || rightPressed) {
            pg.noStroke();

            // Flickering thruster effect - BIGGER & IRREGULAR
            float baseFlicker = sketch.sin(thrusterFlicker) * 12;
            float irregularFlicker = sketch.random(-5, 5); // Additional random flicker
            float thrusterLength = (45 + baseFlicker + irregularFlicker) * thrusterIntensity;
            float startY = pos.y + 20; // Start at bottom of ship

            // Outermost glow layer (new)
            pg.fill(255, 100, 0, 120);
            pg.beginShape();
            pg.vertex(pos.x, startY);
            pg.vertex(pos.x - 14, startY + thrusterLength * 0.5f);
            pg.vertex(pos.x, startY + thrusterLength * 1.2f);
            pg.vertex(pos.x + 14, startY + thrusterLength * 0.5f);
            pg.endShape(PApplet.CLOSE);

            // Outer orange glow (diamond shape) - BIGGER
            pg.fill(255, 150, 0, 220);
            pg.beginShape();
            pg.vertex(pos.x, startY); // Top point
            pg.vertex(pos.x - 11, startY + thrusterLength * 0.4f); // Left middle (widest)
            pg.vertex(pos.x, startY + thrusterLength); // Bottom point
            pg.vertex(pos.x + 11, startY + thrusterLength * 0.4f); // Right middle (widest)
            pg.endShape(PApplet.CLOSE);

            // Middle orange layer (smaller diamond) - BIGGER
            pg.fill(255, 180, 50, 250);
            pg.beginShape();
            pg.vertex(pos.x, startY);
            pg.vertex(pos.x - 11, startY + thrusterLength * 0.4f);
            pg.vertex(pos.x, startY + thrusterLength * 0.85f);
            pg.vertex(pos.x + 11, startY + thrusterLength * 0.4f);
            pg.endShape(PApplet.CLOSE);

            // Inner bright yellow core - BIGGER
            pg.fill(255, 255, 150, 255);
            pg.beginShape();
            pg.vertex(pos.x, startY);
            pg.vertex(pos.x - 5, startY + thrusterLength * 0.35f);
            pg.vertex(pos.x, startY + thrusterLength * 0.7f);
            pg.vertex(pos.x + 5, startY + thrusterLength * 0.35f);
            pg.endShape(PApplet.CLOSE);

            // Innermost white hot core (new)
            pg.fill(255, 255, 255, 255);
            pg.beginShape();
            pg.vertex(pos.x, startY);
            pg.vertex(pos.x - 2, startY + thrusterLength * 0.25f);
            pg.vertex(pos.x, startY + thrusterLength * 0.5f);
            pg.vertex(pos.x + 2, startY + thrusterLength * 0.25f);
            pg.endShape(PApplet.CLOSE);
        }

        // Draw ship ON TOP of thruster
        pg.image(playerImg, pos.x, pos.y);
        
        // === SHIP ENHANCEMENTS ===
        
        // 1. Wing tip lights (navigation lights) - BIGGER
        float wingLightIntensity = 0.6f + sketch.sin(wingLightPhase) * 0.4f;
        
        // Left wing - yellow light (smaller)
        pg.fill(255, 255, 50, 220 * wingLightIntensity);
        pg.ellipse(pos.x - 22, pos.y, 6, 6);
        pg.fill(255, 255, 100, 160 * wingLightIntensity);
        pg.ellipse(pos.x - 22, pos.y, 10, 10);
        pg.fill(255, 255, 150, 80 * wingLightIntensity);
        pg.ellipse(pos.x - 22, pos.y, 14, 14);
        
        // Right wing - green light (smaller)
        pg.fill(50, 255, 50, 220 * wingLightIntensity);
        pg.ellipse(pos.x + 22, pos.y, 6, 6);
        pg.fill(100, 255, 100, 160 * wingLightIntensity);
        pg.ellipse(pos.x + 22, pos.y, 10, 10);
        pg.fill(150, 255, 150, 80 * wingLightIntensity);
        pg.ellipse(pos.x + 22, pos.y, 14, 14);
        
        // 2. Cockpit glow (cyan/blue) - BIGGER
        float cockpitIntensity = 0.7f + sketch.sin(cockpitGlow) * 0.3f;
        pg.fill(100, 200, 255, 200 * cockpitIntensity);
        pg.ellipse(pos.x, pos.y - 5, 14, 12);
        pg.fill(150, 220, 255, 140 * cockpitIntensity);
        pg.ellipse(pos.x, pos.y - 5, 22, 18);
        pg.fill(180, 230, 255, 70 * cockpitIntensity);
        pg.ellipse(pos.x, pos.y - 5, 30, 24);
        
        // 3. Status indicator lights (small blinking lights) - BIGGER
        float statusBlink = sketch.sin(lightPulse) * 0.5f + 0.5f;
        
        // Left status light (yellow)
        if (statusBlink > 0.5f) {
            pg.fill(255, 255, 100, 250);
            pg.ellipse(pos.x - 12, pos.y + 5, 5, 5);
            pg.fill(255, 255, 150, 180);
            pg.ellipse(pos.x - 12, pos.y + 5, 10, 10);
            pg.fill(255, 255, 200, 100);
            pg.ellipse(pos.x - 12, pos.y + 5, 16, 16);
        }
        
        // Right status light (yellow)
        if (statusBlink > 0.5f) {
            pg.fill(255, 255, 100, 250);
            pg.ellipse(pos.x + 12, pos.y + 5, 5, 5);
            pg.fill(255, 255, 150, 180);
            pg.ellipse(pos.x + 12, pos.y + 5, 10, 10);
            pg.fill(255, 255, 200, 100);
            pg.ellipse(pos.x + 12, pos.y + 5, 16, 16);
        }
        
        // 4. Shield indicator (if shield active) - BIGGER PULSING RINGS
        if (shield > 0) {
            float shieldAlpha = (shield / 100.0f) * 80; // Increased from 60
            float shieldPulse = sketch.sin(lightPulse * 2) * 0.3f + 0.7f; // Bigger pulse range
            
            // Shield color changes based on health
            int shieldColor;
            if (shield > 70) {
                shieldColor = sketch.color(100, 200, 255, shieldAlpha * shieldPulse); // Cyan
            } else if (shield > 30) {
                shieldColor = sketch.color(255, 200, 100, shieldAlpha * shieldPulse); // Orange
            } else {
                shieldColor = sketch.color(255, 100, 100, shieldAlpha * shieldPulse); // Red
            }
            
            // Outermost ring (new)
            pg.noFill();
            pg.stroke(sketch.red(shieldColor), sketch.green(shieldColor), sketch.blue(shieldColor), shieldAlpha * 0.3f * shieldPulse);
            pg.strokeWeight(1);
            pg.ellipse(pos.x, pos.y, radius * 4.5f, radius * 4.5f);
            
            // Outer ring
            pg.strokeWeight(2);
            pg.stroke(sketch.red(shieldColor), sketch.green(shieldColor), sketch.blue(shieldColor), shieldAlpha * 0.5f * shieldPulse);
            pg.ellipse(pos.x, pos.y, radius * 3.8f, radius * 3.8f);
            
            // Middle ring
            pg.strokeWeight(3);
            pg.stroke(shieldColor);
            pg.ellipse(pos.x, pos.y, radius * 3.2f, radius * 3.2f);
            
            // Inner bright ring
            pg.strokeWeight(2);
            pg.stroke(sketch.red(shieldColor), sketch.green(shieldColor), sketch.blue(shieldColor), shieldAlpha * 1.2f * shieldPulse);
            pg.ellipse(pos.x, pos.y, radius * 2.6f, radius * 2.6f);
        }
        
        // 5. Engine glow rings (when moving)
        if (leftPressed || rightPressed) {
            float engineGlow = sketch.sin(thrusterFlicker) * 0.3f + 0.7f;
            pg.noFill();
            pg.stroke(255, 150, 50, 100 * engineGlow);
            pg.strokeWeight(2);
            pg.ellipse(pos.x, pos.y + 18, 16, 8);
            pg.stroke(255, 200, 100, 60 * engineGlow);
            pg.strokeWeight(1);
            pg.ellipse(pos.x, pos.y + 18, 20, 10);
        }

        pg.popStyle();
    }
}
