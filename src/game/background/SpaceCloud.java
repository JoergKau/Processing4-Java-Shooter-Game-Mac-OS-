package game.background;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import java.util.ArrayList;

// Represents a space cloud with organic shape and internal stars.
public class SpaceCloud {
    private PApplet sketch;
    public PVector pos;
    public PVector velocity;
    public float size;
    public int col;
    public int col2; // Secondary color for gradient
    public float alpha;
    public float rotation;
    public float rotationSpeed;
    public float pulsePhase;
    public ArrayList<PVector> blobOffsets; // Multiple blobs for organic shape
    public ArrayList<Float> blobSizes;
    public ArrayList<PVector> starPositions; // Internal stars
    public ArrayList<Float> starBrightness;

    public SpaceCloud(PApplet sketch) {
        this.sketch = sketch;
        size = sketch.random(150, 400);
        
        // Obere 2/3 des Bildschirms
        float upperLimit = sketch.height * 2.0f / 3.0f;
        
        // Spawn WEIT außerhalb des Bildschirms von verschiedenen Seiten
        int side = (int) sketch.random(3); // 0=oben, 1=rechts, 2=links
        
        if (side == 0) {
            // Von oben - außerhalb, fließt langsam nach unten
            pos = new PVector(sketch.random(sketch.width), -size - 100);
            velocity = new PVector(0, sketch.random(15, 30)); // Langsamer
            rotationSpeed = sketch.random(-0.1f, 0.1f);
        } else if (side == 1) {
            // Von rechts - außerhalb, fließt langsam nach links
            pos = new PVector(sketch.width + size + 100, sketch.random(-size - 100, -50));
            velocity = new PVector(sketch.random(-35, -20), sketch.random(10, 20)); // Langsamer
            rotationSpeed = sketch.random(-0.1f, 0.1f);
        } else {
            // Von links - außerhalb, fließt langsam nach rechts
            pos = new PVector(-size - 100, sketch.random(-size - 100, -50));
            velocity = new PVector(sketch.random(20, 35), sketch.random(10, 20)); // Langsamer
            rotationSpeed = sketch.random(-0.1f, 0.1f);
        }
        
        rotation = sketch.random(PApplet.TWO_PI);
        alpha = sketch.random(15, 40); // Slightly more visible
        pulsePhase = sketch.random(PApplet.TWO_PI);
        
        // Verschiedene Nebel-Farben mit Gradienten
        float colorChoice = sketch.random(1);
        if (colorChoice < 0.25f) {
            col = sketch.color(100, 150, 255); // Blau
            col2 = sketch.color(150, 100, 255); // zu Lila
        } else if (colorChoice < 0.5f) {
            col = sketch.color(150, 100, 255); // Lila
            col2 = sketch.color(255, 100, 150); // zu Pink
        } else if (colorChoice < 0.75f) {
            col = sketch.color(100, 255, 150); // Grün
            col2 = sketch.color(100, 200, 255); // zu Cyan
        } else {
            col = sketch.color(255, 150, 100); // Orange
            col2 = sketch.color(255, 100, 150); // zu Pink
        }
        
        // Create multiple blobs for organic shape
        blobOffsets = new ArrayList<>();
        blobSizes = new ArrayList<>();
        int numBlobs = (int) sketch.random(4, 8);
        for (int i = 0; i < numBlobs; i++) {
            float angle = sketch.random(PApplet.TWO_PI);
            float dist = sketch.random(size * 0.2f, size * 0.5f);
            blobOffsets.add(new PVector(sketch.cos(angle) * dist, sketch.sin(angle) * dist));
            blobSizes.add(sketch.random(size * 0.4f, size * 0.8f));
        }
        
        // Add internal stars
        starPositions = new ArrayList<>();
        starBrightness = new ArrayList<>();
        int numStars = (int) sketch.random(5, 15);
        for (int i = 0; i < numStars; i++) {
            float angle = sketch.random(PApplet.TWO_PI);
            float dist = sketch.random(size * 0.3f);
            starPositions.add(new PVector(sketch.cos(angle) * dist, sketch.sin(angle) * dist));
            starBrightness.add(sketch.random(PApplet.TWO_PI));
        }
    }

    public void update(float delta_time) {
        pos.x += velocity.x * delta_time;
        pos.y += velocity.y * delta_time;
        rotation += rotationSpeed * delta_time;
        pulsePhase += delta_time * 1.5f;
        
        // Update star brightness
        for (int i = 0; i < starBrightness.size(); i++) {
            starBrightness.set(i, starBrightness.get(i) + delta_time * sketch.random(2, 4));
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
        pg.rotate(rotation);
        
        pg.noStroke();
        
        // Pulsing effect
        float pulseIntensity = 0.7f + sketch.sin(pulsePhase) * 0.3f;
        
        // Draw multiple organic blobs
        for (int b = 0; b < blobOffsets.size(); b++) {
            PVector offset = blobOffsets.get(b);
            float blobSize = blobSizes.get(b);
            
            // Multiple layers per blob with gradient
            for (int i = 4; i > 0; i--) {
                float layerSize = blobSize * (i / 4.0f);
                float layerAlpha = alpha * (i / 4.0f) * pulseIntensity;
                
                if (layerSize > 1 && layerSize * 0.6f > 0) {
                    // Gradient between col and col2
                    float gradientMix = (float) i / 4.0f;
                    int blendedCol = sketch.lerpColor(col2, col, gradientMix);
                    
                    pg.fill(sketch.red(blendedCol), sketch.green(blendedCol), sketch.blue(blendedCol), layerAlpha);
                    pg.ellipse(offset.x, offset.y, layerSize, layerSize * 0.6f);
                }
            }
        }
        
        // Draw main central cloud with gradient
        for (int i = 5; i > 0; i--) {
            float layerSize = size * (i / 5.0f);
            float layerAlpha = alpha * (i / 5.0f) * pulseIntensity;
            
            if (layerSize > 1 && layerSize * 0.6f > 0) {
                // Gradient from outer (col2) to inner (col)
                float gradientMix = (float) i / 5.0f;
                int blendedCol = sketch.lerpColor(col2, col, gradientMix);
                
                pg.fill(sketch.red(blendedCol), sketch.green(blendedCol), sketch.blue(blendedCol), layerAlpha);
                pg.ellipse(0, 0, layerSize, layerSize * 0.6f);
            }
        }
        
        // Draw internal stars (bright spots)
        for (int i = 0; i < starPositions.size(); i++) {
            PVector starPos = starPositions.get(i);
            float brightness = (sketch.sin(starBrightness.get(i)) * 0.5f + 0.5f) * alpha * 3;
            
            // Star glow
            pg.fill(255, 255, 255, brightness * 0.5f);
            pg.ellipse(starPos.x, starPos.y, 8, 8);
            
            // Star core
            pg.fill(255, 255, 255, brightness);
            pg.ellipse(starPos.x, starPos.y, 3, 3);
        }
        
        // Add wispy tendrils
        pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), alpha * 0.3f * pulseIntensity);
        for (int i = 0; i < 3; i++) {
            float angle = (PApplet.TWO_PI / 3) * i + rotation * 0.5f;
            float tendrilLength = size * 0.8f;
            pg.ellipse(
                sketch.cos(angle) * tendrilLength * 0.5f,
                sketch.sin(angle) * tendrilLength * 0.5f,
                size * 0.3f,
                size * 0.15f
            );
        }
        
        pg.popMatrix();
        pg.popStyle();
    }
}
