package game.background;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import java.util.ArrayList;

// Represents a shooting star in the background.
// The shooting star has a position, velocity, length, life, and color.
// It also leaves a trail behind as it moves.

public class ShootingStar {
    private PApplet sketch;
    public PVector pos;
    public PVector vel;
    public float length;
    public float life;
    public float maxLife;
    public int col;
    public ArrayList<PVector> trail;
    public int trailLength;
    public float size; // Größenskalierung

    public ShootingStar(PApplet sketch) {
        this.sketch = sketch;
        
        // Größenvarianz: 1.0x bis 2.0x
        size = sketch.random(1.0f, 2.0f);
        
        // Obere 2/3 des Bildschirms
        float upperLimit = sketch.height * 2.0f / 3.0f;
        
        // Zufällige Startposition und Richtung (nur obere 2/3)
        int side = (int) sketch.random(3); // 0=oben, 1=rechts, 2=links (kein unten mehr)
        
        if (side == 0) { // Von oben
            pos = new PVector(sketch.random(sketch.width), -10);
            vel = new PVector(sketch.random(-200, 200), sketch.random(100, 300));
        } else if (side == 1) { // Von rechts
            pos = new PVector(sketch.width + 10, sketch.random(0, upperLimit));
            vel = new PVector(sketch.random(-300, -100), sketch.random(-100, 100));
        } else { // Von links
            pos = new PVector(-10, sketch.random(0, upperLimit));
            vel = new PVector(sketch.random(100, 300), sketch.random(-100, 100));
        }
        
        this.length = sketch.random(30, 80) * size;
        this.life = sketch.random(1.5f, 3.5f);
        this.maxLife = this.life;
        
        // Verschiedene Farben für Sternschnuppen
        float colorChoice = sketch.random(1);
        if (colorChoice < 0.4f) {
            col = sketch.color(200, 220, 255); // Bläulich-weiß
        } else if (colorChoice < 0.7f) {
            col = sketch.color(255, 240, 200); // Gelblich-weiß
        } else {
            col = sketch.color(255, 200, 150); // Orange-weiß
        }
        
        trail = new ArrayList<>();
        trailLength = (int) (sketch.random(8, 15) * size);
    }

    public void update(float delta_time) {
        // Update trail
        trail.add(0, pos.copy());
        if (trail.size() > trailLength) {
            trail.remove(trail.size() - 1);
        }
        
        pos.x += vel.x * delta_time;
        pos.y += vel.y * delta_time;
        life -= delta_time;
    }

    public boolean isDead() {
        return life <= 0 || pos.x < -100 || pos.x > sketch.width + 100 || pos.y < -100 || pos.y > sketch.height + 100;
    }

    public void display(PGraphics pg) {
        if (trail.size() < 2) return;
        
        float alpha = 255 * (life / maxLife);
        
        pg.pushStyle();
        pg.noFill();
        
        // Zeichne Trail mit abnehmender Dicke und Alpha (größenabhängig)
        for (int i = 0; i < trail.size() - 1; i++) {
            PVector p1 = trail.get(i);
            PVector p2 = trail.get(i + 1);
            
            float trailAlpha = alpha * (1.0f - (float) i / trail.size());
            float strokeW = sketch.map(i, 0, trail.size(), 3 * size, 0.5f * size);
            
            pg.stroke(sketch.red(col), sketch.green(col), sketch.blue(col), trailAlpha);
            pg.strokeWeight(strokeW);
            pg.line(p1.x, p1.y, p2.x, p2.y);
        }
        
        // Heller Kopf der Sternschnuppe (größenabhängig)
        if (!trail.isEmpty()) {
            PVector head = trail.get(0);
            pg.noStroke();
            
            // Glow
            pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), alpha * 0.3f);
            pg.ellipse(head.x, head.y, 12 * size, 12 * size);
            
            // Kern
            pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), alpha);
            pg.ellipse(head.x, head.y, 6 * size, 6 * size);
            
            // Heller Punkt
            pg.fill(255, 255, 255, alpha);
            pg.ellipse(head.x, head.y, 3 * size, 3 * size);
        }
        
        pg.popStyle();
    }
}
