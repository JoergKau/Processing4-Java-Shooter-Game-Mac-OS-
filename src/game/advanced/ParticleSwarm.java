package game.advanced;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import java.util.ArrayList;

// Manages a group of particles for swarm effects.
public class ParticleSwarm {
    private PApplet sketch;
    public ArrayList<SwarmParticle> particles;
    public PVector center;
    public PVector velocity;
    public float life;
    public float maxLife;
    public int swarmCol;
    public float size; // Größenvarianz

    public ParticleSwarm(PApplet sketch) {
        this.sketch = sketch;
        particles = new ArrayList<>();
        
        // Größenvarianz: 1.0x bis 2.5x
        size = sketch.random(1.0f, 2.5f);
        
        // Spawn von verschiedenen Seiten
        int side = (int) sketch.random(4);
        if (side == 0) { // Oben
            center = new PVector(sketch.random(sketch.width), -50);
            velocity = new PVector(sketch.random(-30, 30), sketch.random(20, 50));
        } else if (side == 1) { // Rechts
            center = new PVector(sketch.width + 50, sketch.random(sketch.height * 0.6f));
            velocity = new PVector(sketch.random(-50, -20), sketch.random(-20, 20));
        } else if (side == 2) { // Links
            center = new PVector(-50, sketch.random(sketch.height * 0.6f));
            velocity = new PVector(sketch.random(20, 50), sketch.random(-20, 20));
        } else { // Diagonal
            center = new PVector(sketch.random(sketch.width), -50);
            velocity = new PVector(sketch.random(-40, 40), sketch.random(30, 60));
        }
        
        life = sketch.random(5, 10) * size; // Längere Lebensdauer für größere Schwärme
        maxLife = life;
        
        // Schwarm-Farben
        float colorChoice = sketch.random(1);
        if (colorChoice < 0.33f) {
            swarmCol = sketch.color(100, 200, 255); // Cyan
        } else if (colorChoice < 0.66f) {
            swarmCol = sketch.color(255, 200, 100); // Gold
        } else {
            swarmCol = sketch.color(200, 100, 255); // Lila
        }
        
        // Erstelle Partikel (größenabhängig)
        int count = (int) (sketch.random(15, 30) * size);
        for (int i = 0; i < count; i++) {
            particles.add(new SwarmParticle(sketch, center.copy(), swarmCol, size));
        }
    }

    public void update(float delta_time) {
        center.add(PVector.mult(velocity, delta_time));
        life -= delta_time;
        
        for (SwarmParticle p : particles) {
            p.update(delta_time, center);
        }
    }

    public boolean isDead() {
        return life <= 0 || center.x < -200 || center.x > sketch.width + 200 || 
               center.y < -200 || center.y > sketch.height + 200;
    }

    public float getRadius() {
        // Kollisionsradius basierend auf maximaler Partikel-Distanz
        return 60 * size;
    }

    public void display(PGraphics pg) {
        float alpha = 255 * (life / maxLife);
        for (SwarmParticle p : particles) {
            p.display(pg, alpha);
        }
    }
}
