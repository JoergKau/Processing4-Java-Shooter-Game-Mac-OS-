// Represents a distant planet in the background.
// The planet has properties like position, velocity, size, color, rotation, and more.
// It can also have rings and a moon, with various characteristics for each planet type.
// The class is responsible for setting up the planet's movement, updating its position,
// and displaying it on the screen with atmospheric effects.

package game.background;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import java.util.ArrayList;

public class DistantPlanet {
    private PApplet sketch;
    public PVector pos;
    public PVector velocity; // Geschwindigkeitsvektor
    public float size;
    public int planetCol;
    public int shadowCol;
    public float rotation;
    public float rotationSpeed;
    public int depthLayer; // 0=weit, 1=mittel, 2=nah
    public float alpha; // Transparenz basierend auf Tiefe
    public boolean hasRings;
    public int ringCol;
    public boolean hasMoon;
    public float moonAngle;
    public float moonDistance;
    public float moonSize;
    public float moonSpeed;
    public float atmospherePulse;
    public int planetType; // 0=rocky, 1=gas, 2=ice
    public ArrayList<Float> craterX;
    public ArrayList<Float> craterY;
    public ArrayList<Float> craterSize;

    public DistantPlanet(PApplet sketch) {
        this.sketch = sketch;
        
        // Tiefenschicht bestimmen (0=weit, 1=mittel, 2=nah)
        float layerRoll = sketch.random(1);
        if (layerRoll < 0.4f) {
            depthLayer = 0; // 40% weit entfernt
        } else if (layerRoll < 0.75f) {
            depthLayer = 1; // 35% mittlere Distanz
        } else {
            depthLayer = 2; // 25% nah
        }
        
        // Größe und Geschwindigkeit basierend auf Tiefe
        if (depthLayer == 0) {
            // Weit entfernt: klein, langsam, transparent
            size = sketch.random(60, 120);
            float speedMult = 0.4f;
            alpha = sketch.random(80, 120); // Sehr transparent
            rotationSpeed = sketch.random(1) < 0.4f ? sketch.random(-0.15f, 0.15f) : 0;
            setupMovement(speedMult);
        } else if (depthLayer == 1) {
            // Mittlere Distanz: mittel, mittelschnell, halbtransparent
            size = sketch.random(100, 160);
            float speedMult = 0.7f;
            alpha = sketch.random(150, 200); // Halbtransparent
            rotationSpeed = sketch.random(1) < 0.6f ? sketch.random(-0.25f, 0.25f) : 0;
            setupMovement(speedMult);
        } else {
            // Nah: groß, schnell, fast opak
            size = sketch.random(140, 220);
            float speedMult = 1.0f;
            alpha = sketch.random(220, 255); // Fast opak
            rotationSpeed = sketch.random(1) < 0.7f ? sketch.random(-0.35f, 0.35f) : 0;
            setupMovement(speedMult);
        }
        
        rotation = sketch.random(PApplet.TWO_PI);
        atmospherePulse = sketch.random(PApplet.TWO_PI);
        
        // 40% chance for rings
        hasRings = sketch.random(1) < 0.4f;
        if (hasRings) {
            ringCol = sketch.color(
                sketch.red(planetCol) * 0.8f, 
                sketch.green(planetCol) * 0.8f, 
                sketch.blue(planetCol) * 0.8f
            );
        }
        
        // 30% chance for moon
        hasMoon = sketch.random(1) < 0.3f;
        if (hasMoon) {
            moonAngle = sketch.random(PApplet.TWO_PI);
            moonDistance = size * sketch.random(0.7f, 1.2f);
            moonSize = size * sketch.random(0.15f, 0.3f);
            moonSpeed = sketch.random(0.5f, 1.5f);
        }
        
        // Planet type
        planetType = (int) sketch.random(3);
        
        // Add craters for rocky planets
        if (planetType == 0) {
            craterX = new ArrayList<>();
            craterY = new ArrayList<>();
            craterSize = new ArrayList<>();
            int numCraters = (int) sketch.random(3, 8);
            for (int i = 0; i < numCraters; i++) {
                craterX.add(sketch.random(-size * 0.4f, size * 0.4f));
                craterY.add(sketch.random(-size * 0.4f, size * 0.4f));
                craterSize.add(sketch.random(size * 0.05f, size * 0.15f));
            }
        }
    }
    
    void setupMovement(float speedMult) {
        // Spawn außerhalb des Bildschirms von verschiedenen Seiten
        int side = (int) sketch.random(3); // 0=oben, 1=rechts, 2=links
        
        if (side == 0) {
            // Von oben
            pos = new PVector(sketch.random(sketch.width), -size - 100);
            velocity = new PVector(0, sketch.random(50, 80) * speedMult);
        } else if (side == 1) {
            // Von rechts
            pos = new PVector(sketch.width + size + 100, sketch.random(-size - 100, -50));
            velocity = new PVector(sketch.random(-100, -70) * speedMult, sketch.random(25, 40) * speedMult);
        } else {
            // Von links
            pos = new PVector(-size - 100, sketch.random(-size - 100, -50));
            velocity = new PVector(sketch.random(70, 100) * speedMult, sketch.random(25, 40) * speedMult);
        }
        
        // Verschiedene Planeten-Farben
        float colorChoice = sketch.random(1);
        if (colorChoice < 0.25f) {
            planetCol = sketch.color(200, 150, 100); // Braun/Mars
        } else if (colorChoice < 0.5f) {
            planetCol = sketch.color(100, 150, 200); // Blau/Erde
        } else if (colorChoice < 0.75f) {
            planetCol = sketch.color(220, 200, 150); // Gelb/Saturn
        } else {
            planetCol = sketch.color(150, 100, 150); // Lila/Exoplanet
        }
        
        shadowCol = sketch.color(0, 0, 0, 100);
    }

    public void update(float delta_time) {
        pos.x += velocity.x * delta_time;
        pos.y += velocity.y * delta_time;
        rotation += rotationSpeed * delta_time;
        atmospherePulse += delta_time * 2;
        
        if (hasMoon) {
            moonAngle += moonSpeed * delta_time;
        }
    }

    public boolean isOffScreen() {
        return pos.x < -size - 100 || pos.x > sketch.width + size + 100 || 
               pos.y > sketch.height + size + 100;
    }

    public void display(PGraphics pg) {
        pg.pushStyle();
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        
        pg.noStroke();
        
        // Draw rings BEHIND planet if present
        if (hasRings && size > 5) {
            pg.pushMatrix();
            pg.rotate(rotation * 0.3f);
            float ringSize1 = size * 1.8f;
            float ringSize2 = size * 2.0f;
            if (ringSize1 > 0 && size * 0.4f > 0) {
                pg.fill(sketch.red(ringCol), sketch.green(ringCol), sketch.blue(ringCol), alpha * 0.6f);
                pg.ellipse(0, 0, ringSize1, size * 0.4f);
            }
            if (ringSize2 > 0 && size * 0.5f > 0) {
                pg.fill(sketch.red(ringCol), sketch.green(ringCol), sketch.blue(ringCol), alpha * 0.3f);
                pg.ellipse(0, 0, ringSize2, size * 0.5f);
            }
            pg.popMatrix();
        }
        
        pg.rotate(rotation);
        
        // Pulsing atmospheric glow
        float pulseIntensity = 0.8f + sketch.sin(atmospherePulse) * 0.2f;
        pg.fill(sketch.red(planetCol), sketch.green(planetCol), sketch.blue(planetCol), alpha * 0.15f * pulseIntensity);
        pg.ellipse(0, 0, size * 1.5f, size * 1.5f);
        pg.fill(sketch.red(planetCol), sketch.green(planetCol), sketch.blue(planetCol), alpha * 0.2f * pulseIntensity);
        pg.ellipse(0, 0, size * 1.3f, size * 1.3f);
        
        // Planet base
        pg.fill(sketch.red(planetCol), sketch.green(planetCol), sketch.blue(planetCol), alpha);
        pg.ellipse(0, 0, size, size);
        
        // Surface details based on planet type
        if (planetType == 0 && craterX != null) {
            // Rocky planet - draw craters
            pg.fill(0, 0, 0, alpha * 0.3f);
            for (int i = 0; i < craterX.size(); i++) {
                pg.ellipse(craterX.get(i), craterY.get(i), craterSize.get(i), craterSize.get(i));
            }
        } else if (planetType == 1) {
            // Gas giant - draw bands
            pg.fill(sketch.red(planetCol) * 0.8f, sketch.green(planetCol) * 0.8f, sketch.blue(planetCol) * 0.8f, alpha * 0.4f);
            pg.ellipse(0, -size * 0.2f, size * 0.9f, size * 0.15f);
            pg.ellipse(0, size * 0.1f, size * 0.95f, size * 0.2f);
            pg.fill(sketch.red(planetCol) * 1.2f, sketch.green(planetCol) * 1.2f, sketch.blue(planetCol) * 1.2f, alpha * 0.3f);
            pg.ellipse(0, size * 0.3f, size * 0.85f, size * 0.12f);
        } else {
            // Ice planet - draw ice caps
            pg.fill(255, 255, 255, alpha * 0.6f);
            pg.ellipse(0, -size * 0.35f, size * 0.4f, size * 0.3f);
            pg.ellipse(0, size * 0.35f, size * 0.35f, size * 0.25f);
        }
        
        // Schatten (gibt 3D-Effekt)
        pg.fill(0, 0, 0, alpha * 0.4f);
        pg.arc(0, 0, size, size, -PApplet.HALF_PI, PApplet.HALF_PI);
        
        // Highlight
        pg.fill(255, 255, 255, alpha * 0.3f);
        pg.ellipse(-size * 0.2f, -size * 0.2f, size * 0.3f, size * 0.3f);
        
        pg.popMatrix();
        
        // Draw moon if present
        if (hasMoon) {
            float moonX = pos.x + sketch.cos(moonAngle) * moonDistance;
            float moonY = pos.y + sketch.sin(moonAngle) * moonDistance;
            
            pg.pushMatrix();
            pg.translate(moonX, moonY);
            
            // Moon glow
            pg.fill(180, 180, 180, alpha * 0.3f);
            pg.ellipse(0, 0, moonSize * 1.4f, moonSize * 1.4f);
            
            // Moon body
            pg.fill(200, 200, 200, alpha);
            pg.ellipse(0, 0, moonSize, moonSize);
            
            // Moon shadow
            pg.fill(0, 0, 0, alpha * 0.5f);
            pg.arc(0, 0, moonSize, moonSize, -PApplet.HALF_PI, PApplet.HALF_PI);
            
            pg.popMatrix();
        }
        
        pg.popStyle();
    }
}
