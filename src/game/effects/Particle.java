package game.effects;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

// Represents a single particle for explosion or effect.
public class Particle {
    private PApplet sketch;
    public PVector pos;
    public PVector vel;
    public float life;
    public float maxLife;
    public int col;
    public float size;
    public boolean noFriction; // Flag to disable friction for boss explosions

    public Particle(PApplet sketch, float x, float y) {
        this.sketch = sketch;
        this.pos = new PVector(x, y);
        this.vel = new PVector(0, 0);
        this.life = 1.0f;
        this.maxLife = 1.0f;
        this.size = 3;
        this.noFriction = false;
    }

    public void reset(float x, float y, float vx, float vy, int col) {
        this.pos.set(x, y);
        this.vel.set(vx, vy);
        this.col = col;
        this.life = sketch.random(1.0f, 2.5f); // Longer lifetime
        this.maxLife = this.life;
        this.size = sketch.random(2, 5); // Slightly larger
        this.noFriction = false; // Reset friction flag
    }

    public void update(float delta_time) {
        pos.x += vel.x * delta_time;
        pos.y += vel.y * delta_time;
        
        // Apply friction only if not disabled
        if (!noFriction) {
            vel.mult((float) Math.pow(0.95f, delta_time * 60)); // Frame-rate independent friction
        } else {
            // Very minimal friction for boss explosion particles (keeps them moving)
            vel.mult((float) Math.pow(0.99f, delta_time * 60)); // Only 1% friction
        }
        
        life -= delta_time;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void display(PGraphics pg) {
        float alpha;
        
        if (noFriction) {
            // For boss explosion particles: fade based on velocity (speed)
            float speed = vel.mag();
            float initialSpeed = 500; // Approximate average initial speed
            float speedRatio = Math.min(speed / initialSpeed, 1.0f);
            alpha = 255 * speedRatio; // Fade as speed decreases
            
            // Also consider life for very long-lasting particles
            float lifeRatio = life / maxLife;
            alpha = Math.min(alpha, 255 * lifeRatio);
        } else {
            // Normal particles: fade based on life
            alpha = 255 * (life / maxLife);
        }
        
        pg.noStroke();
        
        // Cache color components to avoid repeated red(), green(), blue() calls
        float r = sketch.red(col);
        float g = sketch.green(col);
        float b = sketch.blue(col);
        
        // Add glow effect for brighter particles
        float brightness = (r + g + b) / 3.0f;
        if (brightness > 150) {
            // Draw outer glow
            pg.fill(r, g, b, alpha * 0.3f);
            pg.ellipse(pos.x, pos.y, size * 2, size * 2);
        }
        
        // Draw main particle
        pg.fill(r, g, b, alpha);
        pg.ellipse(pos.x, pos.y, size, size);
    }
}
