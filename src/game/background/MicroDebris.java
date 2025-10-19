package game.background;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

// Represents small debris particles in the background.
public class MicroDebris {
    private final PApplet sketch;
    public PVector pos;
    public PVector velocity;
    public float size;
    public int col;
    public int layer; // 0=sehr weit, 1=weit, 2=mittel
    public int type; // 0=grau, 1=braun, 2=bläulich
    public float rotation;
    public float rotationSpeed;
    public int shape; // 0=circle, 1=rect, 2=triangle
    
    public MicroDebris(PApplet sketch) {
        this.sketch = sketch;
        
        // Zufällige Tiefenschicht
        layer = (int) sketch.random(3);
        type = (int) sketch.random(3);
        shape = (int) sketch.random(3);
        
        // Position überall auf dem Bildschirm
        pos = new PVector(sketch.random(sketch.width), sketch.random(sketch.height));
        
        // Geschwindigkeit basierend auf Layer (Parallax)
        float baseSpeed = 10 + layer * 15; // 10, 25, 40
        velocity = new PVector(sketch.random(-baseSpeed, baseSpeed), sketch.random(baseSpeed * 0.5f, baseSpeed * 2));
        
        // Größe basierend auf Layer - GRÖSSER
        size = 1.5f + layer * 1.0f; // 1.5, 2.5, 3.5
        
        // Rotation für nicht-kreisförmige Partikel
        rotation = sketch.random(PApplet.TWO_PI);
        rotationSpeed = sketch.random(-2, 2);
        
        // Farbe - verschiedene Typen
        int brightness = 80 + layer * 30; // 80, 110, 140
        int alpha = 150 + layer * 50; // Alpha: 150, 200, 250
        
        if (type == 0) {
            // Grau
            col = sketch.color(brightness, brightness, brightness, alpha);
        } else if (type == 1) {
            // Braun (Asteroid-Staub)
            col = sketch.color(brightness * 0.8f, brightness * 0.6f, brightness * 0.4f, alpha);
        } else {
            // Bläulich (Weltraum-Eis)
            col = sketch.color(brightness * 0.7f, brightness * 0.8f, brightness, alpha);
        }
    }
    
    public void update(float delta_time) {
        pos.x += velocity.x * delta_time;
        pos.y += velocity.y * delta_time;
        rotation += rotationSpeed * delta_time;
        
        // Wrap around
        if (pos.x < 0) pos.x = sketch.width;
        if (pos.x > sketch.width) pos.x = 0;
        if (pos.y > sketch.height) pos.y = 0;
    }
    
    public void display(PGraphics pg) {
        pg.pushStyle();
        pg.noStroke();
        pg.fill(col);
        
        if (shape == 0) {
            // Circle
            pg.ellipse(pos.x, pos.y, size, size);
        } else if (shape == 1) {
            // Rectangle
            pg.pushMatrix();
            pg.translate(pos.x, pos.y);
            pg.rotate(rotation);
            pg.rectMode(PApplet.CENTER);
            pg.rect(0, 0, size, size);
            pg.popMatrix();
        } else {
            // Triangle
            pg.pushMatrix();
            pg.translate(pos.x, pos.y);
            pg.rotate(rotation);
            float h = size * 1.2f;
            pg.triangle(0, -h*0.5f, -size*0.5f, h*0.5f, size*0.5f, h*0.5f);
            pg.popMatrix();
        }
        
        pg.popStyle();
    }
}
