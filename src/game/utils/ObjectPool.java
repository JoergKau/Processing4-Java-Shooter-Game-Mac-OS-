package game.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Generic object pool for efficient memory management.
 * Reduces garbage collection by reusing objects instead of creating new ones.
 * 
 * @param <T> The type of objects to pool
 */
public class ObjectPool<T> {
    private final List<T> available;
    private final List<T> inUse;
    private final Supplier<T> factory;
    private final int maxSize;
    
    public ObjectPool(Supplier<T> factory, int initialSize) {
        this(factory, initialSize, initialSize * 2);
    }
    
    public ObjectPool(Supplier<T> factory, int initialSize, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
        this.available = new ArrayList<>(initialSize);
        this.inUse = new ArrayList<>(initialSize);
        
        // Pre-populate the pool
        for (int i = 0; i < initialSize; i++) {
            available.add(factory.get());
        }
    }
    
    /**
     * Obtain an object from the pool
     * @return An object from the pool, or null if the pool is at max capacity
     */
    public T obtain() {
        T object;
        
        if (!available.isEmpty()) {
            // Reuse an existing object
            object = available.remove(available.size() - 1);
        } else if (inUse.size() < maxSize) {
            // Create a new object if we haven't hit the max size
            object = factory.get();
        } else {
            // Pool is at max capacity
            return null;
        }
        
        inUse.add(object);
        return object;
    }
    
    /**
     * Return an object to the pool for reuse
     * @param object The object to return
     */
    public void free(T object) {
        if (object == null) {
            return;
        }
        
        if (inUse.remove(object)) {
            available.add(object);
        }
    }
    
    /**
     * Free multiple objects at once
     * @param objects The objects to return to the pool
     */
    public void freeAll(List<T> objects) {
        for (T object : objects) {
            free(object);
        }
    }
    
    /**
     * Clear all objects from the pool
     */
    public void clear() {
        available.clear();
        inUse.clear();
    }
    
    /**
     * Get the number of available objects in the pool
     */
    public int getAvailableCount() {
        return available.size();
    }
    
    /**
     * Get the number of objects currently in use
     */
    public int getInUseCount() {
        return inUse.size();
    }
    
    /**
     * Get the total size of the pool
     */
    public int getTotalSize() {
        return available.size() + inUse.size();
    }
    
    /**
     * Check if the pool is at max capacity
     */
    public boolean isAtMaxCapacity() {
        return getTotalSize() >= maxSize;
    }
}
