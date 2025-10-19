package game.background;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

// Represents slow-moving dust particles in the background.
public class AmbientDust {
    private PApplet sketch;
    public PVector pos;
    public PVector velocity; // Sehr langsam: -10 bis 10 horizontal, 5-15 vertikal
    public float size;       // 3-8 Pixel (größer als Micro Debris)
    public int col;
    public float alpha;      // 30-80 (sehr transparent)
    public float rotation;
    public float rotationSpeed;
    
    public AmbientDust(PApplet sketch) {
        this.sketch = sketch;
        
        // Zufällige Position
        pos = new PVector(sketch.random(sketch.width), sketch.random(sketch.height));
        
        // Sehr langsame Bewegung
        velocity = new PVector(sketch.random(-10, 10), sketch.random(5, 15));
        
        // Größere Partikel als Micro Debris
        size = sketch.random(3, 8);
        
        // Dunkle, subtile Farben
        int brightness = (int) sketch.random(40, 80);
        alpha = sketch.random(30, 80); // Sehr transparent
        col = sketch.color(brightness, brightness, brightness, alpha);
        
        rotation = sketch.random(PApplet.TWO_PI);
        rotationSpeed = sketch.random(-1, 1);
    }
    
    public void update(float delta_time) {
        pos.x += velocity.x * delta_time;
        pos.y += velocity.y * delta_time;
        rotation += rotationSpeed * delta_time;
        
        // Wrap around
        if (pos.x < -size) pos.x = sketch.width + size;
        if (pos.x > sketch.width + size) pos.x = -size;
        if (pos.y > sketch.height + size) pos.y = -size;
    }
    
    public void display(PGraphics pg) {
        pg.pushStyle();
        pg.pushMatrix();
        pg.translate(pos.x, pos.y);
        pg.rotate(rotation);
        pg.noStroke();
        
        // Soft blur effect
        pg.fill(sketch.red(col), sketch.green(col), sketch.blue(col), alpha * 0.3f);
        pg.ellipse(0, 0, size * 1.5f, size * 1.5f);
        
        pg.fill(col);
        pg.ellipse(0, 0, size, size);
        
        pg.popMatrix();
        pg.popStyle();
    }
}
