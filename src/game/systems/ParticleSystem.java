package game.systems;

import processing.core.PApplet;
import processing.core.PVector;
import game.effects.Particle;
import game.utils.ObjectPool;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages particle effects for explosions, trails, and other visual effects.
 */
public class ParticleSystem {
    private final PApplet sketch;
    private final List<Particle> activeParticles;
    private final ObjectPool<Particle> particlePool;
    private final int maxParticles;
    
    public ParticleSystem(PApplet sketch, int maxParticles) {
        this.sketch = sketch;
        this.maxParticles = maxParticles;
        this.activeParticles = new ArrayList<>(maxParticles);
        this.particlePool = new ObjectPool<>(() -> new Particle(sketch, 0, 0), maxParticles / 2, maxParticles);
    }
    
    /**
     * Update all active particles
     */
    public void update(float deltaTime) {
        // Update particles and remove dead ones
        activeParticles.removeIf(particle -> {
            particle.update(deltaTime);
            if (particle.isDead()) {
                particlePool.free(particle);
                return true;
            }
            return false;
        });
    }
    
    /**
     * Render all active particles
     */
    public void render() {
        sketch.pushStyle();
        for (Particle particle : activeParticles) {
            if (!particle.isDead()) {
                particle.display(sketch.g);
            }
        }
        sketch.popStyle();
    }
    
    /**
     * Create an explosion effect at the given position
     */
    public void createExplosion(float x, float y, int particleCount, int color) {
        createExplosion(x, y, particleCount, color, 2.0f, 5.0f);
    }
    
    /**
     * Create an explosion effect with custom parameters
     */
    public void createExplosion(float x, float y, int particleCount, int color, float minSpeed, float maxSpeed) {
        for (int i = 0; i < particleCount && activeParticles.size() < maxParticles; i++) {
            Particle particle = particlePool.obtain();
            if (particle != null) {
                // Random direction
                float angle = sketch.random(PApplet.TWO_PI);
                float speed = sketch.random(minSpeed, maxSpeed);
                
                // Use the reset method with all parameters
                particle.reset(
                    x, y,
                    PApplet.cos(angle) * speed,
                    PApplet.sin(angle) * speed,
                    color
                );
                
                activeParticles.add(particle);
            }
        }
    }
    
    /**
     * Create a trail effect behind a moving object
     */
    public void createTrail(float x, float y, int color, float size) {
        if (activeParticles.size() >= maxParticles) {
            return;
        }
        
        Particle particle = particlePool.obtain();
        if (particle != null) {
            particle.reset(x, y, 0, 0, color);
            particle.size = size;
            
            activeParticles.add(particle);
        }
    }
    
    /**
     * Create a burst effect in a specific direction
     */
    public void createDirectionalBurst(float x, float y, float angle, int particleCount, int color) {
        float spreadAngle = PApplet.PI / 4; // 45 degree spread
        
        for (int i = 0; i < particleCount && activeParticles.size() < maxParticles; i++) {
            Particle particle = particlePool.obtain();
            if (particle != null) {
                float particleAngle = angle + sketch.random(-spreadAngle, spreadAngle);
                float speed = sketch.random(2.0f, 6.0f);
                
                particle.reset(
                    x, y,
                    PApplet.cos(particleAngle) * speed,
                    PApplet.sin(particleAngle) * speed,
                    color
                );
                
                activeParticles.add(particle);
            }
        }
    }
    
    /**
     * Create a ring explosion effect
     */
    public void createRingExplosion(float x, float y, int particleCount, int color, float radius) {
        float angleStep = PApplet.TWO_PI / particleCount;
        
        for (int i = 0; i < particleCount && activeParticles.size() < maxParticles; i++) {
            Particle particle = particlePool.obtain();
            if (particle != null) {
                float angle = i * angleStep;
                float speed = 4.0f;
                
                particle.reset(
                    x + PApplet.cos(angle) * radius,
                    y + PApplet.sin(angle) * radius,
                    PApplet.cos(angle) * speed,
                    PApplet.sin(angle) * speed,
                    color
                );
                
                activeParticles.add(particle);
            }
        }
    }
    
    /**
     * Clear all particles
     */
    public void clear() {
        for (Particle particle : activeParticles) {
            particlePool.free(particle);
        }
        activeParticles.clear();
    }
    
    /**
     * Get the number of active particles
     */
    public int getActiveCount() {
        return activeParticles.size();
    }
    
    /**
     * Get particle pool statistics
     */
    public String getStats() {
        return String.format("Particles: %d/%d (Pool: %d available, %d in use)",
            activeParticles.size(), maxParticles,
            particlePool.getAvailableCount(), particlePool.getInUseCount());
    }
}
