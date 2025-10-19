package game.effects;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.HashMap;

public class Explosion {
    private PApplet sketch;
    public PVector pos;
    public int currentFrame = 0;
    public float frameTime = 0;
    public float frameDuration = 0.05f;
    String size;
    public boolean active = true;
    public boolean finished = false;
    public float scale = 1.0f; // Optional: für spätere Skalierung

    public Explosion(PApplet sketch, float x, float y, String size) {
        this.sketch = sketch;
        this.pos = new PVector(x, y);
        this.size = size;
    }

    // Überladene reset-Methode für Skalierung (optional)
    public void reset(float x, float y, String size) {
        reset(x, y, size, 1.0f);
    }

    public void reset(float x, float y, String size, float scale) {
        this.pos.set(x, y);
        this.size = size;
        this.scale = scale;
        this.currentFrame = 0;
        this.frameTime = 0;
        this.active = true;
        this.finished = false;

        // Passe Frame-Duration basierend auf Explosionstyp an
        if (size.equals("ship")) {
            frameDuration = 0.04f; // Schnellere Animation für 18 Frames
        } else {
            frameDuration = 0.05f; // Standard für 9 Frames
        }
    }

    public void update(float delta_time, HashMap<String, ArrayList<PImage>> explosionAnims) {
        if (finished)
            return;

        frameTime += delta_time;
        if (frameTime >= frameDuration) {
            currentFrame++;
            frameTime = 0;

            ArrayList<PImage> anim = explosionAnims.get(size);
            if (anim == null || currentFrame >= anim.size()) {
                finished = true;
                active = false;
            }
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void display(PGraphics pg, HashMap<String, ArrayList<PImage>> explosionAnims) {
        if (!finished) {
            ArrayList<PImage> anim = explosionAnims.get(size);
            if (anim != null && currentFrame < anim.size()) {
                pg.pushMatrix();
                pg.translate(pos.x, pos.y);
                pg.scale(scale);
                pg.imageMode(PApplet.CENTER);
                pg.image(anim.get(currentFrame), 0, 0);
                pg.popMatrix();
            }
        }
    }
}
