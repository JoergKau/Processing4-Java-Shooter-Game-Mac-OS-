package game.advanced;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import java.util.ArrayList;

// Represents a supernova explosion with shockwaves and debris.
public class Supernova {
    private PApplet sketch;
    public PVector pos;
    public float life;
    public float maxLife;
    public int coreColor;    // Helle Kernfarbe
    public int midColor;     // Mittlere Farbe
    public int outerColor;   // Dunkle Außenfarbe
    public ArrayList<ShockwaveRing> shockwaves; // 3-5 nacheinander erscheinende Wellen
    public ArrayList<PVector> debrisPositions;  // Turbulente Trümmer
    public ArrayList<PVector> debrisVelocities;
    public ArrayList<Float> debrisSizes;
    public ArrayList<Integer> debrisColors;
    public ArrayList<Float> debrisRotations;
    public float rotation;
    public float rotationSpeed;
    public float sizeScale; // Größen-Skalierung (0.5 - 1.0)
    
    public Supernova(PApplet sketch) {
        this.sketch = sketch;
        
        // Position nur im oberen 2/3 des Bildschirms
        pos = new PVector(sketch.random(sketch.width * 0.1f, sketch.width * 0.9f), 
                         sketch.random(sketch.height * 0.05f, sketch.height * 0.66f));
        
        maxLife = sketch.random(8, 15); // Längere Dauer für dramatischen Effekt
        life = maxLife;
        rotation = sketch.random(PApplet.TWO_PI);
        rotationSpeed = sketch.random(-0.2f, 0.2f);
        
        // Zufällige Größe: 50% bis 100% der vollen Größe
        sizeScale = sketch.random(0.5f, 1.0f);
        
        // Farbvarianten (realistischer)
        float colorChoice = sketch.random(1);
        if (colorChoice < 0.25f) {
            coreColor = sketch.color(255, 255, 255); // Weißer Kern
            midColor = sketch.color(255, 220, 150); // Gelb-orange
            outerColor = sketch.color(255, 150, 100); // Orange-rot
        } else if (colorChoice < 0.5f) {
            coreColor = sketch.color(255, 255, 240); // Hellgelb
            midColor = sketch.color(255, 180, 120); // Orange
            outerColor = sketch.color(200, 80, 80); // Dunkelrot
        } else if (colorChoice < 0.75f) {
            coreColor = sketch.color(220, 230, 255); // Hellblau
            midColor = sketch.color(150, 180, 255); // Blau
            outerColor = sketch.color(100, 120, 200); // Dunkelblau
        } else {
            coreColor = sketch.color(255, 220, 255); // Helles Pink
            midColor = sketch.color(220, 150, 255); // Pink-Lila
            outerColor = sketch.color(150, 100, 200); // Dunkellila
        }
        
        // Erstelle mehrere Schockwellen die nacheinander erscheinen
        shockwaves = new ArrayList<>();
        int numWaves = (int) sketch.random(3, 5);
        for (int i = 0; i < numWaves; i++) {
            float delay = i * sketch.random(0.8f, 1.5f); // Zeitverzögerung zwischen Wellen
            shockwaves.add(new ShockwaveRing(sketch, delay, i, sizeScale));
        }
        
        // Erstelle ungleichmäßige Trümmer-Partikel
        debrisPositions = new ArrayList<>();
        debrisVelocities = new ArrayList<>();
        debrisSizes = new ArrayList<>();
        debrisColors = new ArrayList<>();
        debrisRotations = new ArrayList<>();
        
        int numDebris = (int) (sketch.random(60, 100) * sizeScale);
        for (int i = 0; i < numDebris; i++) {
            debrisPositions.add(new PVector(0, 0));
            
            // Ungleichmäßige Geschwindigkeiten (turbulent)
            float angle = sketch.random(PApplet.TWO_PI);
            float speed = sketch.random(15, 80) * sizeScale; // Skalierte Expansion
            float turbulence = sketch.random(-20, 20) * sizeScale;
            debrisVelocities.add(new PVector(
                sketch.cos(angle) * speed + turbulence,
                sketch.sin(angle) * speed + turbulence
            ));
            
            debrisSizes.add(sketch.random(1.5f, 4) * sizeScale);
            debrisRotations.add(sketch.random(PApplet.TWO_PI));
            
            // Farbverteilung
            float colorRand = sketch.random(1);
            if (colorRand < 0.2f) {
                debrisColors.add(coreColor);
            } else if (colorRand < 0.6f) {
                debrisColors.add(midColor);
            } else {
                debrisColors.add(outerColor);
            }
        }
        
        // NEUER GROSSER PARTIKELRING - schnelle Expansion
        // Erstelle dichten Ring von Partikeln
        int ringParticles = (int) (sketch.random(80, 120) * sizeScale);
        for (int i = 0; i < ringParticles; i++) {
            debrisPositions.add(new PVector(0, 0));
            
            // Gleichmäßig verteilte Partikel im Ring
            float angle = (PApplet.TWO_PI / ringParticles) * i + sketch.random(-0.1f, 0.1f);
            float speed = sketch.random(120, 180) * sizeScale; // Skalierte SCHNELLE Expansion
            debrisVelocities.add(new PVector(sketch.cos(angle) * speed, sketch.sin(angle) * speed));
            
            debrisSizes.add(sketch.random(2, 4.5f) * sizeScale); // Skalierte Partikel
            debrisRotations.add(sketch.random(PApplet.TWO_PI));
            
            // Ring-Partikel sind heller
            debrisColors.add(sketch.random(1) < 0.5f ? coreColor : midColor);
        }
    }
    
    public void update(float delta_time) {
        life -= delta_time;
        rotation += rotationSpeed * delta_time;
        
        float lifeRatio = 1.0f - (life / maxLife);
        
        // Update Schockwellen
        for (ShockwaveRing wave : shockwaves) {
            wave.update(delta_time, lifeRatio);
        }
        
        // Update Trümmer mit Turbulenz
        for (int i = 0; i < debrisPositions.size(); i++) {
            PVector debrisPos = debrisPositions.get(i);
            PVector vel = debrisVelocities.get(i);
            
            debrisPos.x += vel.x * delta_time;
            debrisPos.y += vel.y * delta_time;
            
            // Turbulente Bewegung
            vel.x += sketch.random(-5, 5) * delta_time;
            vel.y += sketch.random(-5, 5) * delta_time;
            
            // Leichte Abbremsung (frame-rate independent)
            vel.mult((float) Math.pow(0.995f, delta_time * 60));
            
            // Update Rotation
            debrisRotations.set(i, debrisRotations.get(i) + sketch.random(-2, 2) * delta_time);
        }
    }
    
    public boolean isDead() {
        return life <= 0;
    }
    
    public void display(PGraphics pg) {
        pg.pushStyle();
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        pg.rotate(rotation);
        pg.noStroke();
        
        float lifeRatio = 1.0f - (life / maxLife);
        float alpha = 1.0f;
        
        // Langsames Fade out (nach 70% der Zeit)
        if (lifeRatio > 0.7f) {
            alpha = 1.0f - ((lifeRatio - 0.7f) / 0.3f);
        }
        
        // SEHR HELLES ZENTRUM (initialer Blitz)
        float centerBrightness = 1.0f;
        if (lifeRatio < 0.1f) {
            // Extrem hell am Anfang
            centerBrightness = 2.5f - (lifeRatio / 0.1f) * 1.5f;
        } else if (lifeRatio < 0.3f) {
            // Langsames Abdunkeln
            centerBrightness = 1.0f - ((lifeRatio - 0.1f) / 0.2f) * 0.7f;
        } else {
            // Schwaches Glühen bleibt
            centerBrightness = 0.3f * (1.0f - ((lifeRatio - 0.3f) / 0.7f));
        }
        
        // Zentraler Stern/Blitz (skaliert)
        if (centerBrightness > 0) {
            pg.fill(255, 255, 255, 250 * centerBrightness * alpha);
            pg.ellipse(0, 0, 40 * centerBrightness * sizeScale, 40 * centerBrightness * sizeScale);
            
            pg.fill(255, 255, 255, 180 * centerBrightness * alpha);
            pg.ellipse(0, 0, 60 * centerBrightness * sizeScale, 60 * centerBrightness * sizeScale);
            
            pg.fill(sketch.red(coreColor), sketch.green(coreColor), sketch.blue(coreColor), 120 * centerBrightness * alpha);
            pg.ellipse(0, 0, 80 * centerBrightness * sizeScale, 80 * centerBrightness * sizeScale);
        }
        
        // Zeichne Schockwellen-Ringe mit Partikeln
        for (ShockwaveRing wave : shockwaves) {
            if (wave.active && wave.radius > 5) { // Mindestgröße 5
                float waveAlpha = wave.getAlpha(alpha);
                if (waveAlpha > 0.01f) { // Mindest-Alpha
                    // Äußerer Ring (dunkel)
                    pg.noFill();
                    float outerSize = wave.radius * 2;
                    if (outerSize > 0) {
                        pg.stroke(sketch.red(outerColor), sketch.green(outerColor), sketch.blue(outerColor), 40 * waveAlpha);
                        pg.strokeWeight(3);
                        pg.ellipse(0, 0, outerSize, outerSize);
                    }
                    
                    // Mittlerer Ring
                    float midSize = wave.radius * 1.8f;
                    if (midSize > 0) {
                        pg.stroke(sketch.red(midColor), sketch.green(midColor), sketch.blue(midColor), 80 * waveAlpha);
                        pg.strokeWeight(2);
                        pg.ellipse(0, 0, midSize, midSize);
                    }
                    
                    // Innerer Ring (hell)
                    float innerSize = wave.radius * 1.6f;
                    if (innerSize > 0) {
                        pg.stroke(sketch.red(coreColor), sketch.green(coreColor), sketch.blue(coreColor), 120 * waveAlpha);
                        pg.strokeWeight(1.5f);
                        pg.ellipse(0, 0, innerSize, innerSize);
                    }
                    
                    // Zeichne Partikel entlang des Rings
                    pg.noStroke();
                    for (int i = 0; i < wave.particleAngles.size(); i++) {
                        float angle = wave.particleAngles.get(i);
                        float offset = wave.particleOffsets.get(i);
                        float particleRadius = wave.radius * 1.8f + offset;
                        
                        float px = sketch.cos(angle) * particleRadius;
                        float py = sketch.sin(angle) * particleRadius;
                        
                        // Partikel mit Glow
                        pg.fill(sketch.red(midColor), sketch.green(midColor), sketch.blue(midColor), 80 * waveAlpha);
                        pg.ellipse(px, py, 4, 4);
                        
                        pg.fill(sketch.red(coreColor), sketch.green(coreColor), sketch.blue(coreColor), 150 * waveAlpha);
                        pg.ellipse(px, py, 2.5f, 2.5f);
                        
                        pg.fill(255, 255, 255, 180 * waveAlpha);
                        pg.ellipse(px, py, 1.5f, 1.5f);
                    }
                }
            }
        }
        
        pg.noStroke();
        
        // Zeichne turbulente Trümmer-Partikel
        for (int i = 0; i < debrisPositions.size(); i++) {
            PVector debrisPos = debrisPositions.get(i);
            float debrisSize = debrisSizes.get(i);
            int debrisColor = debrisColors.get(i);
            float debrisRot = debrisRotations.get(i);
            
            // Partikel mit Glow und leichter Unschärfe
            pg.fill(sketch.red(debrisColor), sketch.green(debrisColor), sketch.blue(debrisColor), 60 * alpha);
            pg.ellipse(debrisPos.x, debrisPos.y, debrisSize * 2.5f, debrisSize * 2.5f);
            
            pg.fill(sketch.red(debrisColor), sketch.green(debrisColor), sketch.blue(debrisColor), 140 * alpha);
            pg.ellipse(debrisPos.x, debrisPos.y, debrisSize * 1.2f, debrisSize * 1.2f);
            
            // Heller Kern
            if (debrisColor == coreColor) {
                pg.fill(255, 255, 255, 180 * alpha);
                pg.ellipse(debrisPos.x, debrisPos.y, debrisSize * 0.4f, debrisSize * 0.4f);
            }
        }
        
        pg.popMatrix();
        pg.popStyle();
    }
}
