// Represents an enemy spaceship with movement patterns and bomb-dropping behavior.
// The class handles the spaceship's appearance, movement, and interactions such as
// dropping bombs and splitting into smaller ships. It also manages the warp-in effect
// when the ship appears on the screen, and a glow effect for a subset of the ships.
// Additionally, the class includes thruster effects for propulsion and a shield
// mechanism that activates and deactivates over time.

package game.entities;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import java.util.ArrayList;

public class EnemyShip {
    private PApplet sketch;
    public PVector pos;
    public PVector speed;
    public int radius; // Variiert zwischen 12 und 20 (Player-Größe)
    public PImage img;
    public PImage scaledImg;
    public float wobble = 0;
    public float tilt = 0; // Leichte Neigung basierend auf Bewegungsrichtung
    public int movementPattern; // 0-6: verschiedene Bewegungsmuster
    public float patternTimer = 0; // Timer für Muster-spezifische Animationen
    public float nextBombTime; // Zeitpunkt für nächste Bombe
    public float bombInterval; // Intervall zwischen Bomben
    public boolean canSplit = false; // Kann sich teilen?
    public float splitTime = 0; // Zeitpunkt für Split
    public boolean isSplitChild = false; // Ist dies ein geteiltes Schiff?
    public boolean warpingIn = true; // Warp-In Effekt
    public float warpTime = 0; // Zeit seit Warp-Start
    public float warpDuration = 0.8f; // Dauer des Warp-Effekts
    public float glowPulse = 0; // Für pulsierenden Glow-Effekt
    public boolean hasGlow; // Nur 2/3 der Schiffe haben Glow
    public boolean hasThrusters; // 50% der Schiffe haben Thruster
    public boolean shieldActive = true; // Aktueller Shield-Status
    public float shieldTimer = 0; // Timer für Shield Ein/Aus
    public float shieldOnDuration = 3.0f; // Wie lange Shield an ist
    public float shieldOffDuration = 2.0f; // Wie lange Shield aus ist
    public int thrusterColor; // Farbe der Thruster
    public float thrusterFlicker = 0; // Flackern der Thruster
    public int health; // Gesundheit für Damage-Effekt
    public int maxHealth; // Maximale Gesundheit

    public EnemyShip(PApplet sketch, ArrayList<PImage> shipImages) {
        this.sketch = sketch;
        this.pos = new PVector();
        this.speed = new PVector();
        // Zufällige Größe: 84% bis 140% der Player-Größe (60-100% * 1.4)
        radius = (int) sketch.random(17, 29); // 17-28 (Player hat 20)
        if (shipImages != null && !shipImages.isEmpty()) {
            img = shipImages.get((int) sketch.random(shipImages.size()));
            // Skaliere das Bild basierend auf Radius
            scaledImg = img.copy();
            scaledImg.resize(radius * 2, radius * 2);
        }
        reset();
    }

    public void reset() {
        // Spawn well outside the screen (at least 100 pixels above)
        pos.set(sketch.random(radius, sketch.width - radius), -100 - radius);
        speed.set(sketch.random(-50, 50), sketch.random(80, 130));
        wobble = sketch.random(PApplet.TWO_PI);
        movementPattern = (int) sketch.random(8); // 8 verschiedene Bewegungsmuster (0-7)
        patternTimer = 0;
        bombInterval = sketch.random(1.5f, 4.0f); // Unregelmäßige Intervalle: 1.5-4 Sekunden
        nextBombTime = (sketch.millis() / 1000.0f) + bombInterval;
        
        // 30% Chance dass Schiff sich teilen kann (nur wenn groß genug und nicht bereits geteilt)
        if (!isSplitChild && radius >= 20) {
            canSplit = sketch.random(1) < 0.3f;
            if (canSplit) {
                splitTime = (sketch.millis() / 1000.0f) + sketch.random(2, 5); // Split nach 2-5 Sekunden
            }
        }
        
        // Warp-In Effekt zurücksetzen
        warpingIn = true;
        warpTime = 0;
        glowPulse = sketch.random(PApplet.TWO_PI); // Zufälliger Start für Puls-Variation
        
        // Gesundheit basierend auf Größe
        maxHealth = radius; // Größere Schiffe = mehr Health
        health = maxHealth;
        
        // Nur 2/3 der Schiffe haben Glow
        hasGlow = sketch.random(1) < 0.66f;
        
        // Zufällige Shield-Dauern (±30% Variation)
        shieldOnDuration = sketch.random(2.5f, 4.0f);
        shieldOffDuration = sketch.random(1.5f, 2.5f);
        shieldTimer = 0;
        shieldActive = true;
        
        // 50% der Schiffe haben Thruster
        hasThrusters = sketch.random(1) < 0.5f;
        
        // Zufällige Thruster-Farbe
        float thrusterChoice = sketch.random(1);
        if (thrusterChoice < 0.33f) {
            thrusterColor = sketch.color(255, 100, 50); // Orange
        } else if (thrusterChoice < 0.66f) {
            thrusterColor = sketch.color(100, 150, 255); // Blau
        } else {
            thrusterColor = sketch.color(150, 255, 100); // Grün
        }
        thrusterFlicker = sketch.random(PApplet.TWO_PI);
    }
    
    public boolean needsWarpInEffect() {
        return warpTime == 0 && warpingIn; // Nur beim ersten Frame
    }

    public void update(float delta_time, int frameCount, PVector playerPos) {
        // Warp-In Effekt
        if (warpingIn) {
            warpTime += delta_time;
            if (warpTime >= warpDuration) {
                warpingIn = false;
            }
        }
        
        // Basis-Bewegung
        pos.x += speed.x * delta_time;
        pos.y += speed.y * delta_time;
        wobble += delta_time * 2;
        glowPulse += delta_time * 3; // Pulsierender Glow
        thrusterFlicker += delta_time * 15; // Schnelles Flackern
        
        // Shield Ein/Aus Timer
        if (hasGlow) {
            shieldTimer += delta_time;
            if (shieldActive) {
                // Shield ist an
                if (shieldTimer >= shieldOnDuration) {
                    shieldActive = false;
                    shieldTimer = 0;
                    // Neue zufällige Off-Dauer
                    shieldOffDuration = sketch.random(1.5f, 2.5f);
                }
            } else {
                // Shield ist aus
                if (shieldTimer >= shieldOffDuration) {
                    shieldActive = true;
                    shieldTimer = 0;
                    // Neue zufällige On-Dauer
                    shieldOnDuration = sketch.random(2.5f, 4.0f);
                }
            }
        }
        
        // 7 verschiedene Bewegungsmuster für Abwechslung
        patternTimer += delta_time;
        
        switch (movementPattern) {
            case 0: // Gerade Bewegung mit leichtem Drift
                pos.x += sketch.sin(wobble) * 20 * delta_time;
                tilt = sketch.sin(wobble) * 3;
                break;
                
            case 1: // Sinuswellen-Bewegung (smooth)
                float sineMove = sketch.sin(wobble) * 50 * delta_time;
                pos.x += sineMove;
                tilt = sineMove * 2;
                break;
                
            case 2: // Zickzack-Bewegung (aggressiv)
                if (frameCount % 60 < 30) {
                    pos.x += 40 * delta_time;
                    tilt = 5;
                } else {
                    pos.x -= 40 * delta_time;
                    tilt = -5;
                }
                break;
                
            case 3: // Kreisförmige Bewegung (Spirale)
                float circleRadius = 30;
                pos.x += sketch.cos(patternTimer * 2) * circleRadius * delta_time;
                pos.y += sketch.sin(patternTimer * 2) * circleRadius * delta_time * 0.5f; // Langsamer vertikal
                tilt = sketch.cos(patternTimer * 2) * 8;
                break;
                
            case 4: // Aggressive Ausweichbewegung (zum Spieler hin/weg)
                if (patternTimer % 2 < 1) {
                    // Bewege dich zum Spieler
                    float dx = playerPos.x - pos.x;
                    pos.x += Math.signum(dx) * 60 * delta_time;
                    tilt = Math.signum(dx) * 6;
                } else {
                    // Bewege dich weg vom Spieler
                    float dx = playerPos.x - pos.x;
                    pos.x -= Math.signum(dx) * 40 * delta_time;
                    tilt = -Math.signum(dx) * 4;
                }
                break;
                
            case 5: // Wellenförmig mit Geschwindigkeitsänderung
                float wave = sketch.sin(wobble * 1.5f);
                pos.x += wave * 60 * delta_time;
                speed.y = 80 + wave * 30; // Geschwindigkeit variiert
                tilt = wave * 5;
                break;
                
            case 6: // Ruckartige "Dash" Bewegung
                if ((int)(patternTimer * 2) % 3 == 0) {
                    // Schneller Dash
                    pos.x += sketch.sin(patternTimer * 10) * 100 * delta_time;
                    tilt = sketch.sin(patternTimer * 10) * 10;
                } else {
                    // Langsame Drift
                    pos.x += sketch.sin(wobble) * 15 * delta_time;
                    tilt = sketch.sin(wobble) * 2;
                }
                break;
                
            case 7: // Starke horizontale Bewegung (Strafe-Pattern)
                // Langsamer vertikal, schnell horizontal
                speed.y = 50; // Sehr langsam nach unten
                float horizontalSpeed = 120;
                
                // Wechselt Richtung alle 1.5 Sekunden
                if ((int)(patternTimer / 1.5f) % 2 == 0) {
                    pos.x += horizontalSpeed * delta_time;
                    tilt = 8;
                } else {
                    pos.x -= horizontalSpeed * delta_time;
                    tilt = -8;
                }
                
                // Bounce an Bildschirmrändern
                if (pos.x < radius) {
                    pos.x = radius;
                    patternTimer += 0.75f; // Force direction change
                } else if (pos.x > sketch.width - radius) {
                    pos.x = sketch.width - radius;
                    patternTimer += 0.75f; // Force direction change
                }
                break;
        }
        
        // Tilt smoothing
        tilt *= 0.95f;
    }
    
    public boolean shouldDropBomb() {
        float currentTime = sketch.millis() / 1000.0f;
        if (currentTime >= nextBombTime && pos.y > 0 && pos.y < sketch.height - 100) {
            bombInterval = sketch.random(1.5f, 4.0f); // Neues zufälliges Intervall
            nextBombTime = currentTime + bombInterval;
            return true;
        }
        return false;
    }
    
    public boolean shouldPerformSplit() {
        float currentTime = sketch.millis() / 1000.0f;
        return canSplit && currentTime >= splitTime && !warpingIn;
    }
    
    public boolean shouldSpawnWarpParticle(int frameCount) {
        return warpingIn && frameCount % 2 == 0;
    }
    
    public float getWarpParticleAngle() {
        return sketch.random(PApplet.TWO_PI);
    }
    
    public float getWarpParticleDist() {
        return radius * 1.5f;
    }
    
    public float getWarpParticleSpeed() {
        return sketch.random(50, 100);
    }
    
    public int getWarpParticleColor() {
        return sketch.color(150, 220, 255, 150);
    }
    
    public boolean shouldSpawnThrusterParticle(int frameCount) {
        return hasThrusters && frameCount % 3 == 0;
    }
    
    public float getThrusterParticleX() {
        return pos.x + sketch.random(-2, 2);
    }
    
    public float getThrusterParticleY() {
        return pos.y - radius; // Oben = Heck (Schiff fliegt nach unten)
    }
    
    public float getThrusterParticleVelX() {
        return sketch.random(-15, 15);
    }
    
    public float getThrusterParticleVelY() {
        return sketch.random(-60, -20); // Kürzer = langsamere Geschwindigkeit
    }
    
    public boolean shouldDrawShieldParticles(int frameCount) {
        return hasGlow && shieldActive && frameCount % 3 == 0;
    }
    
    public float getShieldSize() {
        return radius * 3.0f;
    }

    public boolean isOffScreen() {
        return pos.y > sketch.height + radius || pos.x < -radius || pos.x > sketch.width + radius;
    }

    public void display(PGraphics pg) {
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        // Leichte Neigung basierend auf Bewegungsrichtung
        pg.rotate(sketch.radians(tilt));
        
        // Schutzschild-Effekt (nur vordere Hälfte, nur für 2/3 der Schiffe)
        // Schaltet sich zeitbasiert ein/aus (3 Sek an, 2 Sek aus)
        if (hasGlow && shieldActive) {
            float glowIntensity = 0.5f + sketch.sin(glowPulse) * 0.4f; // 0.1 - 0.9
            float shieldSize = radius * 3.0f; // Größer
            float shieldAlpha = 120 + glowIntensity * 100; // 120-220 (viel heller)
            
            pg.pushStyle();
            
            // Gefüllter Halbkreis als Basis (sehr transparent)
            pg.noStroke();
            pg.fill(100, 200, 255, shieldAlpha * 0.2f);
            pg.arc(0, 0, shieldSize, shieldSize, 0, PApplet.PI);
            
            // Äußerer heller Ring (dick)
            pg.noFill();
            pg.strokeWeight(3);
            pg.stroke(150, 220, 255, shieldAlpha);
            pg.arc(0, 0, shieldSize, shieldSize, 0, PApplet.PI);
            
            // Mittlerer Ring
            pg.strokeWeight(2);
            pg.stroke(200, 240, 255, shieldAlpha * 0.8f);
            pg.arc(0, 0, shieldSize * 0.85f, shieldSize * 0.85f, 0, PApplet.PI);
            
            // Innerer heller Kern-Ring
            pg.strokeWeight(2);
            pg.stroke(220, 250, 255, shieldAlpha * 0.6f);
            pg.arc(0, 0, shieldSize * 0.7f, shieldSize * 0.7f, 0, PApplet.PI);
            
            pg.popStyle();
        }
        
        // Zeichne Thruster als Partikel-System (hinter dem Schiff)
        if (hasThrusters) {
            // Unregelmäßiges Flackern
            float flicker1 = sketch.sin(thrusterFlicker);
            float flicker2 = sketch.sin(thrusterFlicker * 1.7f);
            float flickerIntensity = 0.5f + (flicker1 * 0.3f + flicker2 * 0.2f);
            
            pg.pushStyle();
            pg.noStroke();
            
            // Thruster-Positionen (am Heck = oben in lokalen Koordinaten, da Schiff nach unten zeigt)
            float leftX = -radius * 0.5f;
            float rightX = radius * 0.5f;
            float baseY = -radius * 0.8f; // Negativ = oben in lokalen Koordinaten = Heck des nach-unten-zeigenden Schiffs
            
            // Zeichne Partikel für jeden Thruster
            for (int thruster = 0; thruster < 2; thruster++) {
                float thrusterX = thruster == 0 ? leftX : rightX;
                
                // 5-8 Partikel pro Thruster
                int particleCount = (int) sketch.random(5, 9);
                for (int i = 0; i < particleCount; i++) {
                    // Zufällige Position entlang der Thruster-Flamme
                    float offsetX = sketch.random(-radius * 0.15f, radius * 0.15f);
                    float offsetY = sketch.random(-radius * 0.6f, 0); // Nach oben (Flamme entgegen Flugrichtung)
                    
                    // Größe variiert - größer am Anfang (baseY), kleiner am Ende (weiter oben)
                    float sizeMultiplier = 1.0f + (offsetY / (radius * 0.6f)) * 0.7f; // offsetY ist negativ, also invertiert
                    float particleSize = sketch.random(3, 8) * sizeMultiplier * flickerIntensity;
                    
                    // Alpha variiert - heller am Anfang, dunkler am Ende
                    float alphaMultiplier = 1.0f + (offsetY / (radius * 0.6f)) * 0.5f; // offsetY ist negativ, also invertiert
                    float particleAlpha = (150 + sketch.random(100)) * alphaMultiplier * flickerIntensity;
                    
                    // Farbe mit leichter Variation
                    int r = (int) sketch.constrain(sketch.red(thrusterColor) + sketch.random(-30, 30), 0, 255);
                    int g = (int) sketch.constrain(sketch.green(thrusterColor) + sketch.random(-30, 30), 0, 255);
                    int b = (int) sketch.constrain(sketch.blue(thrusterColor) + sketch.random(-30, 30), 0, 255);
                    
                    pg.fill(r, g, b, particleAlpha);
                    pg.ellipse(thrusterX + offsetX, baseY + offsetY, particleSize, particleSize);
                }
                
                // Heller Kern am Ursprung
                float coreAlpha = 200 * flickerIntensity;
                pg.fill(255, 255, 200, coreAlpha);
                pg.ellipse(thrusterX, baseY, radius * 0.25f, radius * 0.25f);
            }
            
            pg.popStyle();
        }
        
        if (scaledImg != null) {
            pg.image(scaledImg, 0, 0);
        } else {
            // Fallback: Zeichne ein einfaches Schiff
            pg.pushStyle();
            pg.fill(200, 50, 50);
            pg.noStroke();
            pg.triangle(-radius, radius, 0, -radius, radius, radius);
            pg.popStyle();
        }
        
        pg.popMatrix();
    }
}
