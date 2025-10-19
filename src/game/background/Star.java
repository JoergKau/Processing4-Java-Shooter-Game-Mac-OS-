package game.background;

import processing.core.PApplet;
import processing.core.PGraphics;

// Represents a star in the background starfield.
public class Star {
    private PApplet sketch;
    public float x, y;
    public float speed;
    public int layer;
    public float size;
    public int col;

    public Star(PApplet sketch, int layer) {
        this.sketch = sketch;
        this.layer = layer;
        this.x = sketch.random(sketch.width);
        this.y = sketch.random(sketch.height);
        this.speed = layer * 20 + 10;
        this.size = layer * 0.5f + 0.5f;

        // Different brightness for different layers
        int brightness = 255 - layer * 40;
        this.col = sketch.color(brightness);
    }

    public void update(float delta_time) {
        y += speed * delta_time;
        if (y > sketch.height) {
            y = 0;
            x = sketch.random(sketch.width);
        }
    }

    public void display(PGraphics pg) {
        pg.noStroke();
        pg.fill(col);
        pg.ellipse(x, y, size, size);
    }
}
