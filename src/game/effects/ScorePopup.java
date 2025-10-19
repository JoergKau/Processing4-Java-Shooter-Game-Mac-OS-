package game.effects;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

// Represents a popup showing score after destroying an enemy.
public class ScorePopup {
    private PApplet sketch;
    public PVector pos;
    public float life;
    public int points;
    public float vel;

    public ScorePopup(PApplet sketch, float x, float y, int points) {
        this.sketch = sketch;
        this.pos = new PVector(x, y);
        this.points = points;
        this.life = 1.0f;
        this.vel = -50;
    }

    public void update(float delta_time) {
        pos.y += vel * delta_time;
        life -= delta_time;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void display(PGraphics pg) {
        float alpha = 255 * life;
        pg.fill(255, 255, 0, alpha);
        pg.textAlign(PApplet.CENTER, PApplet.CENTER);
        pg.textSize(20);
        pg.text("+" + points, pos.x, pos.y);
    }
}
