package game.utils;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * Base class for all game objects.
 * Provides common functionality for position, velocity, rendering, and lifecycle management.
 */
public abstract class GameObject {
    protected PApplet sketch;
    protected PVector position;
    protected PVector velocity;
    protected boolean active = true;
    protected boolean visible = true;
    protected int renderLayer = 0; // For sorting render order
    
    public GameObject(PApplet sketch) {
        this.sketch = sketch;
        this.position = new PVector();
        this.velocity = new PVector();
    }
    
    public GameObject(PApplet sketch, float x, float y) {
        this.sketch = sketch;
        this.position = new PVector(x, y);
        this.velocity = new PVector();
    }
    
    /**
     * Update the game object's state
     */
    public abstract void update();
    
    /**
     * Render the game object
     */
    public abstract void render();
    
    /**
     * Reset the object to its initial state (for object pooling)
     */
    public void reset() {
        position.set(0, 0);
        velocity.set(0, 0);
        active = true;
        visible = true;
    }
    
    /**
     * Check if this object collides with another
     */
    public boolean collidesWith(GameObject other) {
        float distance = PVector.dist(this.position, other.position);
        return distance < (this.getRadius() + other.getRadius());
    }
    
    /**
     * Get the collision radius of this object
     */
    public abstract float getRadius();
    
    // Getters and setters
    public PVector getPosition() {
        return position;
    }
    
    public void setPosition(PVector pos) {
        position.set(pos);
    }
    
    public PVector getVelocity() {
        return velocity;
    }
    
    public void setVelocity(float x, float y) {
        velocity.set(x, y);
    }
    
    public void setVelocity(PVector vel) {
        velocity.set(vel);
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public int getRenderLayer() {
        return renderLayer;
    }
    
    public void setRenderLayer(int layer) {
        this.renderLayer = layer;
    }
    
    /**
     * Check if object is out of bounds
     */
    public boolean isOutOfBounds() {
        return position.x < -100 || position.x > sketch.width + 100 ||
               position.y < -100 || position.y > sketch.height + 100;
    }
}
