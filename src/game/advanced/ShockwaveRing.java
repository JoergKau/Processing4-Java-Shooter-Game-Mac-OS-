package game.advanced;

import processing.core.PApplet;
import java.util.ArrayList;

// Represents a shockwave ring as part of a supernova effect.
public class ShockwaveRing {
    private PApplet sketch;
    private float sizeScale;
    public float delay;      // Verzögerung bevor Ring erscheint
    public float age;        // Alter des Rings
    public float radius;     // Aktueller Radius
    public float maxRadius;  // Maximaler Radius
    public int ringIndex;    // Index des Rings (0-4)
    public boolean active;   // Ist Ring sichtbar?
    public ArrayList<Float> particleAngles;  // 30-50 Partikel entlang des Rings
    public ArrayList<Float> particleOffsets; // Kleine Offset-Variation
    
    public ShockwaveRing(PApplet sketch, float delay, int index, float sizeScale) {
        this.sketch = sketch;
        this.sizeScale = sizeScale;
        this.delay = delay;
        this.age = -delay;
        this.radius = 0;
        this.maxRadius = sketch.random(100, 200) * sizeScale; // Skalierte max Radius
        this.ringIndex = index;
        this.active = false;
        
        // Erstelle Partikel entlang des Rings
        particleAngles = new ArrayList<>();
        particleOffsets = new ArrayList<>();
        int numParticles = (int) (sketch.random(30, 50) * sizeScale);
        for (int i = 0; i < numParticles; i++) {
            particleAngles.add((PApplet.TWO_PI / numParticles) * i + sketch.random(-0.2f, 0.2f));
            particleOffsets.add(sketch.random(-3, 3)); // Kleine Offset-Variation
        }
    }
    
    public void update(float delta_time, float lifeRatio) {
        age += delta_time;
        
        if (age >= 0 && !active) {
            active = true;
        }
        
        if (active) {
            // Langsame, kontinuierliche Expansion
            radius += sketch.random(20, 40) * delta_time * sizeScale;
            
            // Update Partikel-Offsets (leichte Bewegung)
            for (int i = 0; i < particleOffsets.size(); i++) {
                particleOffsets.set(i, particleOffsets.get(i) + sketch.random(-0.5f, 0.5f));
            }
        }
    }
    
    public float getAlpha(float supernovaAlpha) {
        if (!active) return 0;
        
        float waveLife = radius / maxRadius;
        float alpha = 1.0f;
        
        // Fade in
        if (waveLife < 0.1f) {
            alpha = waveLife / 0.1f;
        } 
        // Fade out
        else if (waveLife > 0.6f) {
            alpha = 1.0f - ((waveLife - 0.6f) / 0.4f);
        }
        
        return alpha * supernovaAlpha;
    }
}
