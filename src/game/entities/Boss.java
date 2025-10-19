// Boss enemy - final boss fight with advanced movement patterns and multiple attack phases
// Stays in the top 2/3 of the screen and uses a figure-8 movement pattern
// Has high health and shoots multiple projectiles

package game.entities;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.core.PGraphics;

public class Boss {
    public PVector pos;
    public PVector vel;
    public float radius;
    public int health;
    public int maxHealth;
    public boolean active;
    public PImage img;
    
    private PApplet sketch;
    private float moveTimer;
    private float shootTimer;
    private float shootInterval;
    private float moveSpeed;
    private float baseMoveSpeed; // Base movement speed
    private float speedVariation; // Current speed multiplier (0.6 - 1.4)
    private float speedChangeTimer; // Timer for changing speed
    private float speedChangeDuration; // How long to wait before changing speed
    // Movement pattern parameters for figure-8
    private float horizontalAmplitude; // How far left/right
    private float verticalAmplitude; // How far up/down
    private PVector centerPos; // Center point for movement pattern
    private float patternPhase; // Current phase in movement pattern (0-2PI)
    private float patternSpeed; // Speed of pattern movement
    
    // Game state
    private boolean isGameOver = false;
    private float fadeAlpha = 255f;
    private float fadeSpeed = 50f; // Pixels per second
    
    // Game over state
    private boolean isFadingOut = false;
    private float fadeOutAlpha = 255f;
    private float fadeOutSpeed = 0.8f; // How fast the boss fades out (1.0 = 1 second)
    
    // Shield system
    private boolean shieldActive;
    private float shieldTimer;
    private float shieldDuration;
    private float shieldCooldown;
    private float shieldCooldownTimer;
    private float shieldParticleTimer; // Timer for emitting shield particles
    private float shieldParticleInterval; // How often to emit particles
    // Special attack system
    private int attackCounter; // Counts attacks to trigger special patterns
    private float specialAttackCooldown; // Cooldown for special attacks
    private float specialAttackTimer; // Timer for special attack cooldown
    
    public Boss(PApplet sketch, PImage bossImg) {
        this.sketch = sketch;
        this.img = bossImg;
        
        // Boss is 6x bigger than normal ships (normal ships are ~50px, so boss is ~300px)
        this.radius = 150;
        
        // Start at top center of screen
        this.pos = new PVector(sketch.width / 2.0f, 150);
        this.centerPos = new PVector(sketch.width / 2.0f, sketch.height / 3.0f); // Center in top 1/3
        this.vel = new PVector(0, 0);
        // High health for boss fight
        this.maxHealth = 1000;
        this.health = maxHealth;
        this.active = true;
        
        // Movement pattern - figure-8
        // Movement parameters with variable speed
        this.baseMoveSpeed = 80; // base pixels per second
        this.moveSpeed = baseMoveSpeed;
        this.speedVariation = 1.0f; // Start at normal speed
        this.speedChangeTimer = 0;
        this.speedChangeDuration = sketch.random(2, 4); // Change speed every 2-4 seconds
        this.patternPhase = 0;
        this.patternSpeed = 0.5f; // radians per second (slower = smoother)
        
        // Figure-8 dimensions - stays in top 2/3 of screen
        this.horizontalAmplitude = sketch.width * 0.3f; // 30% of screen width on each side
        this.verticalAmplitude = sketch.height * 0.15f; // 15% of screen height up/down
        
        // Shooting parameters
        this.shootTimer = 0;
        this.shootInterval = 1.5f; // Shoot every 1.5 seconds
        this.moveTimer = 0;
        
        // Shield parameters
        this.shieldActive = false;
        this.shieldTimer = 0;
        this.shieldDuration = 10.0f; // Shield lasts 10 seconds
        this.shieldCooldown = 15.0f; // Shield cooldown 15 seconds
        this.shieldCooldownTimer = sketch.random(5, 10); // First shield after 5-10 seconds
        this.shieldParticleTimer = 0;
        this.shieldParticleInterval = 0.03f; // Emit particles every 0.03 seconds (~33 times per second)
        
        // Special attack system
        this.attackCounter = 0;
        this.specialAttackCooldown = 8.0f; // Special attack every 8 seconds
        this.specialAttackTimer = 4.0f; // First special attack after 4 seconds
    }
    
    public void setGameOver() {
        isFadingOut = true;
        fadeOutAlpha = 1.0f;
    }
    
    public void update(float dt) {
        if (!active) return;
        
        // Handle fade out if game is over
        if (isFadingOut) {
            fadeOutAlpha -= dt * fadeOutSpeed;
            if (fadeOutAlpha <= 0) {
                active = false;
                return;
            }
            return; // Don't update anything else while fading out
        }
        
        moveTimer += dt;
        shootTimer += dt;
        specialAttackTimer += dt;
        
        // Update shield system
        if (shieldActive) {
            shieldTimer += dt;
            shieldParticleTimer += dt;
            
            if (shieldTimer >= shieldDuration) {
                // Shield expires
                shieldActive = false;
                shieldTimer = 0;
                shieldCooldownTimer = 0;
            }
        } else {
            shieldCooldownTimer += dt;
            if (shieldCooldownTimer >= shieldCooldown) {
                // Activate shield
                shieldActive = true;
                shieldTimer = 0;
                shieldParticleTimer = 0;
            }
        }
        
        // Variable speed system - randomly change speed
        speedChangeTimer += dt;
        if (speedChangeTimer >= speedChangeDuration) {
            // Change speed to a new random value
            // Increase by 40% again:
            // Min speed: 0.96 * 1.4 = 1.344 (134% of base)
            // Max speed: 3.928 * 1.4 = 5.499 (550% of base)
            speedVariation = sketch.random(1.344f, 5.499f);
            moveSpeed = baseMoveSpeed * speedVariation;
            
            // Set next speed change time
            speedChangeDuration = sketch.random(2, 4);
            speedChangeTimer = 0;
        }
        
        // Update movement pattern phase
        patternPhase += patternSpeed * dt;
        if (patternPhase > PApplet.TWO_PI) {
            patternPhase -= PApplet.TWO_PI;
        }
        
        // Figure-8 (lemniscate) movement pattern
        // Parametric equations: x = a * sin(t), y = b * sin(t) * cos(t)
        // This creates a smooth figure-8 pattern
        float targetX = centerPos.x + horizontalAmplitude * PApplet.sin(patternPhase);
        float targetY = centerPos.y + verticalAmplitude * PApplet.sin(patternPhase) * PApplet.cos(patternPhase);
        
        // Smooth movement towards target position
        PVector target = new PVector(targetX, targetY);
        PVector direction = PVector.sub(target, pos);
        float distance = direction.mag();
        
        if (distance > 5) { // Only move if not at target
            direction.normalize();
            direction.mult(moveSpeed * dt);
            pos.add(direction);
        }
        
        // Ensure boss stays in top 2/3 of screen
        float maxY = sketch.height * 0.66f;
        float minY = radius + 50; // Stay below top edge
        pos.y = PApplet.constrain(pos.y, minY, maxY);
        
        // Keep boss on screen horizontally
        pos.x = PApplet.constrain(pos.x, radius + 20, sketch.width - radius - 20);
    }
    
    public boolean shouldShoot() {
        if (shootTimer >= shootInterval) {
            shootTimer = 0;
            attackCounter++;
            return true;
        }
        return false;
    }
    
    public boolean shouldDoSpecialAttack() {
        if (specialAttackTimer >= specialAttackCooldown) {
            specialAttackTimer = 0;
            return true;
        }
        return false;
    }
    
    public int getAttackType() {
        // Every 3rd attack is special
        if (attackCounter % 3 == 0) {
            return 2; // Special pattern
        }
        // Otherwise random between spread and seeking
        return (sketch.random(1) < 0.6f) ? 0 : 1; // 0 = spread, 1 = seeking
    }
    
    public void takeDamage(int damage) {
        // Don't take damage if already dead or fading out
        if (health <= 0 || isFadingOut) {
            return;
        }
        
        // Shield absorbs damage when active
        if (shieldActive) {
            // Shield reduces damage by 70%
            damage = (int) (damage * 0.3f);
        }
        
        health -= damage;
        if (health <= 0) {
            health = 0;
            if (!isFadingOut) {  // Only deactivate if not already fading out
                active = false;
            }
        }
    }
    
    public boolean isShieldActive() {
        return shieldActive;
    }
    
    public boolean shouldEmitShieldParticles() {
        if (shieldActive && shieldParticleTimer >= shieldParticleInterval) {
            shieldParticleTimer = 0;
            return true;
        }
        return false;
    }
    
    public int getShieldParticleCount() {
        // Random number of particles (8-20 per emission) - more particles
        return (int) sketch.random(8, 21);
    }
    
    public float[] getShieldParticleData() {
        // Returns: [x, y, vx, vy, speed]
        // Emit from random point on shield perimeter
        float angle = sketch.random(PApplet.TWO_PI);
        float emitRadius = radius + 20; // Emit from shield edge
        float x = pos.x + PApplet.cos(angle) * emitRadius;
        float y = pos.y + PApplet.sin(angle) * emitRadius;
        
        // Velocity pointing outward with random speed
        float speed = sketch.random(50, 200); // Random speed
        float vx = PApplet.cos(angle) * speed;
        float vy = PApplet.sin(angle) * speed;
        
        return new float[]{x, y, vx, vy, speed};
    }
    
    public boolean isDead() {
        return !active || health <= 0;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public float getHealthPercentage() {
        return (float) health / maxHealth;
    }
    
    public void display(PGraphics pg) {
        if (!active) return;
        
        pg.pushStyle();
        pg.imageMode(PApplet.CENTER);
        
        // Apply fade out effect
        if (isFadingOut) {
            pg.tint(255, fadeOutAlpha * 255);
        } else {
            pg.tint(255, 255); // Reset to fully opaque if not fading
        }
        
        // Draw boss image
        if (img != null) {
            // Scale image to 6x normal ship size
            float imgSize = radius * 2;
            pg.image(img, pos.x, pos.y, imgSize, imgSize);
        } else {
            // Fallback: draw red circle
            pg.fill(255, 0, 0);
            pg.noStroke();
            pg.circle(pos.x, pos.y, radius * 2);
        }
        
        // Reset tint after drawing the boss
        pg.tint(255, 255);
        
        // Draw shimmering glow shield effect when active
        if (shieldActive) {
            float time = sketch.millis() / 1000.0f;
            
            // Multiple layers of shimmering glow
            pg.noFill();
            pg.blendMode(PApplet.ADD); // Additive blending for glow effect
            
            // Pulsing animation
            float pulse = (PApplet.sin(time * 4) + 1) / 2; // 0-1, faster pulse
            float shimmer = (PApplet.sin(time * 8) + 1) / 2; // 0-1, shimmer effect
            
            // Cyan/blue energy color
            int baseColor = pg.color(100, 200, 255);
            
            // Outer glow aura (very soft, large radius)
            int auraAlpha = (int) (20 + 15 * pulse);
            pg.stroke(baseColor, auraAlpha);
            pg.strokeWeight(20);
            pg.circle(pos.x, pos.y, (radius + 50) * 2);
            
            // Second outer glow layer
            int outerAlpha = (int) (40 + 30 * pulse);
            pg.stroke(baseColor, outerAlpha);
            pg.strokeWeight(12);
            pg.circle(pos.x, pos.y, (radius + 35) * 2);
            
            // Middle glow layer
            int middleAlpha = (int) (60 + 40 * shimmer);
            pg.stroke(baseColor, middleAlpha);
            pg.strokeWeight(8);
            pg.circle(pos.x, pos.y, (radius + 22) * 2);
            
            // Inner glow layer (brightest)
            int innerAlpha = (int) (80 + 60 * pulse);
            pg.stroke(baseColor, innerAlpha);
            pg.strokeWeight(5);
            pg.circle(pos.x, pos.y, (radius + 12) * 2);
            
            // Core shield line (very bright)
            int coreAlpha = (int) (120 + 80 * shimmer);
            pg.stroke(baseColor, coreAlpha);
            pg.strokeWeight(3);
            pg.circle(pos.x, pos.y, (radius + 5) * 2);
            
            // Shimmering particles around the edge (more particles)
            pg.strokeWeight(4);
            for (int i = 0; i < 16; i++) {
                float angle = time * 1.5f + i * PApplet.TWO_PI / 16;
                float particleRadius = radius + 18 + PApplet.sin(time * 6 + i) * 8;
                float px = pos.x + PApplet.cos(angle) * particleRadius;
                float py = pos.y + PApplet.sin(angle) * particleRadius;
                int particleAlpha = (int) (120 + 120 * shimmer);
                pg.stroke(baseColor, particleAlpha);
                pg.point(px, py);
            }
            
            // Additional inner particles for more energy effect
            pg.strokeWeight(3);
            for (int i = 0; i < 12; i++) {
                float angle = -time * 2.0f + i * PApplet.TWO_PI / 12;
                float particleRadius = radius + 8 + PApplet.sin(time * 10 + i * 0.5f) * 4;
                float px = pos.x + PApplet.cos(angle) * particleRadius;
                float py = pos.y + PApplet.sin(angle) * particleRadius;
                int particleAlpha = (int) (100 + 100 * pulse);
                pg.stroke(baseColor, particleAlpha);
                pg.point(px, py);
            }
            
            pg.blendMode(PApplet.BLEND); // Reset blend mode
        }
        
        pg.popStyle();
        
        // Reset tint if it was applied
        if (isFadingOut) {
            pg.tint(255, 255);
        }
    }
    
    // Draw health bar at a specific position (called from HUD)
    public void drawHealthBar(PGraphics pg, float x, float y, float barWidth, float barHeight) {
        // Background
        pg.fill(50, 50, 50, 200);
        pg.noStroke();
        pg.rect(x, y, barWidth, barHeight, 5);
        
        // Health fill with color gradient
        float healthPercent = getHealthPercentage();
        int healthColor;
        if (healthPercent > 0.6f) {
            healthColor = pg.color(0, 255, 0); // Green
        } else if (healthPercent > 0.3f) {
            healthColor = pg.color(255, 255, 0); // Yellow
        } else {
            healthColor = pg.color(255, 0, 0); // Red
        }
        
        pg.fill(healthColor);
        pg.rect(x, y, barWidth * healthPercent, barHeight, 5);
        
        // Border
        pg.noFill();
        pg.stroke(255);
        pg.strokeWeight(2);
        pg.rect(x, y, barWidth, barHeight, 5);
        
        // Health text
        pg.fill(255);
        pg.textAlign(PApplet.CENTER, PApplet.CENTER);
        pg.textSize(14);
        pg.text(health + " / " + maxHealth, x + barWidth / 2, y + barHeight / 2);
    }
}
