package game.managers;

import processing.core.PApplet;
import processing.core.PImage;
import processing.sound.SoundFile;
import java.util.ArrayList;
import java.util.HashMap;

public class AssetManager {
    private final PApplet sketch;
    private final HashMap<String, PImage> images = new HashMap<>();
    private final HashMap<String, SoundFile> sounds = new HashMap<>();
    private final HashMap<String, ArrayList<PImage>> animations = new HashMap<>();

    public AssetManager(PApplet sketch) {
        this.sketch = sketch;
    }

    public void loadAllAssets() {
        // Load images
        loadImage("player", "player.png");
        loadImage("bullet", "bullet.png");
        // Add more image loading as needed

        // Load sounds
        loadSound("shoot", "shoot.wav");
        loadSound("explosion", "explosion.wav");
        // Add more sound loading as needed
    }

    public PImage getImage(String name) {
        return images.get(name);
    }

    public SoundFile getSound(String name) {
        return sounds.get(name);
    }

    public ArrayList<PImage> getAnimation(String name) {
        return animations.get(name);
    }

    private void loadImage(String name, String filename) {
        try {
            PImage img = sketch.loadImage("data/images/" + filename);
            if (img != null) {
                images.put(name, img);
            } else {
                System.err.println("Failed to load image: " + filename);
            }
        } catch (Exception e) {
            System.err.println("Error loading image " + filename + ": " + e.getMessage());
        }
    }

    private void loadSound(String name, String filename) {
        try {
            SoundFile sound = new SoundFile(sketch, "data/sounds/" + filename);
            sounds.put(name, sound);
        } catch (Exception e) {
            System.err.println("Error loading sound " + filename + ": " + e.getMessage());
        }
    }

    public void preloadAnimations() {
        // Initialize animation sequences here
        // Example:
        // ArrayList<PImage> explosionFrames = new ArrayList<>();
        // for (int i = 0; i < 10; i++) {
        //     explosionFrames.add(loadImage("explosion_" + i));
        // }
        // animations.put("explosion", explosionFrames);
    }
}
