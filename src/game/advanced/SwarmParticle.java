package game.advanced;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

// Represents a single particle in a particle swarm.
public class SwarmParticle {
    private PApplet sketch;
    public PVector offset;
    public PVector pos;
    public float angle;
    public float distance;
    public float speed;
    public int col;
    public float particleSize;

    public SwarmParticle(PApplet sketch, PVector centerPos, int swarmCol, float swarmSize) {
        this.sketch = sketch;
        this.col = swarmCol;
        this.particleSize = swarmSize;
        angle = sketch.random(PApplet.TWO_PI);
        distance = sketch.random(20, 60) * swarmSize;
        speed = sketch.random(1, 3);
        offset = new PVector(sketch.cos(angle) * distance, sketch.sin(angle) * distance);
        pos = PVector.add(centerPos, offset);
    }

    public void update(float delta_time, PVector centerPos) {
        angle += speed * delta_time;
        offset.set(sketch.cos(angle) * distance, sketch.sin(angle) * distance);
        pos = PVector.add(centerPos, offset);
    }

    public void display(PGraphics pg, float alpha) {
        pg.pushStyle();
        pg.noStroke();
        
        // Glow (größenabhängig)
        pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), alpha * 0.3f);
        pg.ellipse(pos.x, pos.y, 8 * particleSize, 8 * particleSize);
        
        // Kern
        pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), alpha);
        pg.ellipse(pos.x, pos.y, 4 * particleSize, 4 * particleSize);
        
        // Heller Punkt
        pg.fill(255, 255, 255, alpha);
        pg.ellipse(pos.x, pos.y, 2 * particleSize, 2 * particleSize);
        
        pg.popStyle();
    }
}
