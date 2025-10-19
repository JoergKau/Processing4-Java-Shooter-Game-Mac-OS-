package game.managers;

import processing.core.PApplet;
import game.Sketch;
import game.entities.*;
import game.utils.GameObject;
import game.utils.ObjectPool;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class EntityManager {
    private final Sketch sketch;
    private final List<GameObject> gameObjects = new CopyOnWriteArrayList<>();
    private final List<GameObject> toAdd = new ArrayList<>();
    private final List<GameObject> toRemove = new ArrayList<>();
    
    // Object pools for performance
    private final Map<Class<?>, ObjectPool<?>> objectPools = new HashMap<>();
    
    public EntityManager(Sketch sketch) {
        this.sketch = sketch;
        initializeObjectPools();
    }
    
    private void initializeObjectPools() {
        // Register object pools for different entity types
        // Example: registerObjectPool(Bullet.class, () -> new Bullet(sketch, 0, 0), 50);
    }
    
    private <T extends GameObject> void registerObjectPool(Class<T> type, Supplier<T> supplier, int initialSize) {
        objectPools.put(type, new ObjectPool<>(supplier, initialSize));
    }
    
    @SuppressWarnings("unchecked")
    public <T extends GameObject> T createEntity(Class<T> type, Object... args) {
        try {
            ObjectPool<T> pool = (ObjectPool<T>) objectPools.get(type);
            T entity;
            
            if (pool != null) {
                entity = pool.obtain();
                if (entity != null) {
                    entity.reset();
                    toAdd.add(entity);
                    return entity;
                }
            }
            
            // If no pool exists or pool is empty, create new instance
            // This is a simplified version - you'd need to handle different constructors
            entity = type.getDeclaredConstructor(Sketch.class).newInstance(sketch);
            toAdd.add(entity);
            return entity;
        } catch (Exception e) {
            System.err.println("Error creating entity: " + e.getMessage());
            return null;
        }
    }
    
    public void addEntity(GameObject entity) {
        if (entity != null) {
            toAdd.add(entity);
        }
    }
    
    public void removeEntity(GameObject entity) {
        if (entity != null) {
            toRemove.add(entity);
        }
    }
    
    public void update() {
        // Process pending additions
        if (!toAdd.isEmpty()) {
            gameObjects.addAll(toAdd);
            toAdd.clear();
        }
        
        // Process pending removals
        if (!toRemove.isEmpty()) {
            gameObjects.removeAll(toRemove);
            toRemove.clear();
        }
        
        // Update all active game objects
        for (GameObject obj : gameObjects) {
            if (obj.isActive()) {
                obj.update();
            }
        }
    }
    
    public void render() {
        // Sort by render layer if needed
        // Collections.sort(gameObjects, Comparator.comparingInt(GameObject::getRenderLayer));
        
        // Render all active game objects
        for (GameObject obj : gameObjects) {
            if (obj.isVisible() && obj.isActive()) {
                obj.render();
            }
        }
    }
    
    public void clear() {
        gameObjects.clear();
        toAdd.clear();
        toRemove.clear();
    }
    
    @SuppressWarnings("unchecked")
    public <T extends GameObject> List<T> getEntitiesByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (type.isInstance(obj)) {
                result.add((T) obj);
            }
        }
        return result;
    }
    
    public void cleanup() {
        // Clean up object pools and resources
        objectPools.values().forEach(pool -> pool.clear());
        objectPools.clear();
        clear();
    }
}
