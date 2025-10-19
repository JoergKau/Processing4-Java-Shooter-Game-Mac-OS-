package game.managers;

import processing.core.PApplet;
import processing.sound.SoundFile;
import game.Sketch;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private final Sketch sketch;
    private final Map<String, SoundFile> sounds = new HashMap<>();
    private float masterVolume = 0.7f;
    private float musicVolume = 0.3f;
    private SoundFile currentMusic;
    
    public SoundManager(Sketch sketch) {
        this.sketch = sketch;
        initializeSounds();
    }
    
    private void initializeSounds() {
        // Load sound effects
        // loadSound("shoot", "shoot.wav");
        // loadSound("explosion", "explosion.wav");
        // loadSound("powerup", "powerup.wav");
        // loadSound("game_over", "game_over.wav");
        // 
        // // Load music
        // loadSound("bg_music", "background_music.mp3", true);
    }
    
    public void loadSound(String name, String filename) {
        loadSound(name, filename, false);
    }
    
    public void loadSound(String name, String filename, boolean isMusic) {
        try {
            SoundFile sound = new SoundFile(sketch, "data/sounds/" + filename);
            sound.amp(isMusic ? musicVolume * masterVolume : masterVolume);
            sounds.put(name, sound);
        } catch (Exception e) {
            System.err.println("Error loading sound " + filename + ": " + e.getMessage());
        }
    }
    
    public void playSound(String name) {
        playSound(name, 1.0f);
    }
    
    public void playSound(String name, float volume) {
        SoundFile sound = sounds.get(name);
        if (sound != null) {
            sound.amp(volume * masterVolume);
            sound.play();
        }
    }
    
    public void playMusic(String name) {
        playMusic(name, true);
    }
    
    public void playMusic(String name, boolean loop) {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        
        currentMusic = sounds.get(name);
        if (currentMusic != null) {
            currentMusic.amp(musicVolume * masterVolume);
            if (loop) {
                currentMusic.loop();
            } else {
                currentMusic.play();
            }
        }
    }
    
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
    }
    
    public void setMasterVolume(float volume) {
        this.masterVolume = PApplet.constrain(volume, 0, 1);
        updateVolumes();
    }
    
    public void setMusicVolume(float volume) {
        this.musicVolume = PApplet.constrain(volume, 0, 1);
        updateVolumes();
    }
    
    private void updateVolumes() {
        // Update all sound effects
        for (Map.Entry<String, SoundFile> entry : sounds.entrySet()) {
            if (entry.getValue() != currentMusic) {
                entry.getValue().amp(masterVolume);
            }
        }
        
        // Update music volume if playing
        if (currentMusic != null) {
            currentMusic.amp(musicVolume * masterVolume);
        }
    }
    
    public void pauseAll() {
        for (SoundFile sound : sounds.values()) {
            sound.pause();
        }
    }
    
    public void resumeAll() {
        for (SoundFile sound : sounds.values()) {
            sound.play();
        }
    }
    
    public void stopAll() {
        for (SoundFile sound : sounds.values()) {
            sound.stop();
        }
        currentMusic = null;
    }
    
    public void dispose() {
        stopAll();
        // SoundFile doesn't have a close() method, just stop all sounds
        sounds.clear();
    }
}
