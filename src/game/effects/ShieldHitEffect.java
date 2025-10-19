package game.effects;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import java.util.ArrayList;

// Represents the visual effect when the shield is hit.
public class ShieldHitEffect {
    private PApplet sketch;
    public PVector pos;      // Position des Treffers auf dem Shield-Ring
    public float life;       // 0.4 Sekunden Dauer
    public float maxLife;
    public ArrayList<PVector> sparkPositions;  // 8-15 Funken
    public ArrayList<PVector> sparkVelocities; // Fliegen nach außen
    public ArrayList<Float> sparkSizes;
    public float ringRadius;
    public float maxRingRadius;
    
    public ShieldHitEffect(PApplet sketch, float x, float y) {
        this.sketch = sketch;
        pos = new PVector(x, y);
        maxLife = 0.4f; // Kurze Dauer
        life = maxLife;
        ringRadius = 0;
        maxRingRadius = 30;
        
        // Erstelle Funken
        sparkPositions = new ArrayList<>();
        sparkVelocities = new ArrayList<>();
        sparkSizes = new ArrayList<>();
        
        int numSparks = (int) sketch.random(8, 15);
        for (int i = 0; i < numSparks; i++) {
            sparkPositions.add(new PVector(0, 0));
            
            // Funken fliegen nach außen
            float angle = sketch.random(PApplet.TWO_PI);
            float speed = sketch.random(50, 150);
            sparkVelocities.add(new PVector(sketch.cos(angle) * speed, sketch.sin(angle) * speed));
            
            sparkSizes.add(sketch.random(2, 4));
        }
    }
    
    public void update(float delta_time) {
        life -= delta_time;
        
        // Expand ring
        ringRadius += 100 * delta_time;
        
        // Update sparks
        for (int i = 0; i < sparkPositions.size(); i++) {
            PVector sparkPos = sparkPositions.get(i);
            PVector vel = sparkVelocities.get(i);
            
            sparkPos.x += vel.x * delta_time;
            sparkPos.y += vel.y * delta_time;
            
            // Slow down (frame-rate independent)
            vel.mult((float) Math.pow(0.95f, delta_time * 60));
        }
    }
    
    public boolean isDead() {
        return life <= 0;
    }
    
    public void display(PGraphics pg) {
        pg.pushStyle();
        pg.noFill();
        
        float lifeRatio = life / maxLife;
        float alpha = lifeRatio * 255;
        
        // Expanding ring (impact wave)
        if (ringRadius < maxRingRadius) {
            pg.stroke(100, 200, 255, alpha * 0.8f);
            pg.strokeWeight(3);
            pg.ellipse(pos.x, pos.y, ringRadius * 2, ringRadius * 2);
            
            pg.stroke(150, 220, 255, alpha * 0.5f);
            pg.strokeWeight(1.5f);
            pg.ellipse(pos.x, pos.y, ringRadius * 2.2f, ringRadius * 2.2f);
        }
        
        // Draw sparks
        pg.noStroke();
        for (int i = 0; i < sparkPositions.size(); i++) {
            PVector sparkPos = sparkPositions.get(i);
            float sparkSize = sparkSizes.get(i);
            
            float sx = pos.x + sparkPos.x;
            float sy = pos.y + sparkPos.y;
            
            // Spark glow
            pg.fill(100, 200, 255, alpha * 0.6f);
            pg.ellipse(sx, sy, sparkSize * 2, sparkSize * 2);
            
            pg.fill(150, 220, 255, alpha);
            pg.ellipse(sx, sy, sparkSize, sparkSize);
            
            pg.fill(255, 255, 255, alpha);
            pg.ellipse(sx, sy, sparkSize * 0.5f, sparkSize * 0.5f);
        }
        
        pg.popStyle();
    }
}
