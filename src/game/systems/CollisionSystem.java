package game.systems;

import processing.core.PVector;
import game.utils.GameObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimized collision detection system using spatial partitioning.
 * Uses a grid-based approach to reduce the number of collision checks needed.
 */
public class CollisionSystem {
    private final int gridSize;
    private final int gridCols;
    private final int gridRows;
    private final int worldWidth;
    private final int worldHeight;
    private Map<Integer, List<GameObject>> spatialGrid;
    
    public CollisionSystem(int worldWidth, int worldHeight, int gridSize) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gridSize = gridSize;
        this.gridCols = (int) Math.ceil((float) worldWidth / gridSize);
        this.gridRows = (int) Math.ceil((float) worldHeight / gridSize);
        this.spatialGrid = new HashMap<>();
    }
    
    /**
     * Clear the spatial grid
     */
    public void clear() {
        spatialGrid.clear();
    }
    
    /**
     * Add an object to the spatial grid
     */
    public void addToGrid(GameObject obj) {
        int cellIndex = getCellIndex(obj.getPosition().x, obj.getPosition().y);
        if (cellIndex >= 0) {
            spatialGrid.computeIfAbsent(cellIndex, k -> new ArrayList<>()).add(obj);
        }
    }
    
    /**
     * Get all objects in the same cell as the given position
     */
    public List<GameObject> getObjectsInCell(float x, float y) {
        int cellIndex = getCellIndex(x, y);
        return spatialGrid.getOrDefault(cellIndex, new ArrayList<>());
    }
    
    /**
     * Get all objects in neighboring cells (including the current cell)
     */
    public List<GameObject> getObjectsInNeighborhood(float x, float y) {
        List<GameObject> neighbors = new ArrayList<>();
        int col = (int) (x / gridSize);
        int row = (int) (y / gridSize);
        
        // Check the 3x3 grid around the object
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (r >= 0 && r < gridRows && c >= 0 && c < gridCols) {
                    int cellIndex = r * gridCols + c;
                    List<GameObject> cellObjects = spatialGrid.get(cellIndex);
                    if (cellObjects != null) {
                        neighbors.addAll(cellObjects);
                    }
                }
            }
        }
        
        return neighbors;
    }
    
    /**
     * Check collision between two objects
     */
    public boolean checkCollision(GameObject obj1, GameObject obj2) {
        if (obj1 == obj2 || !obj1.isActive() || !obj2.isActive()) {
            return false;
        }
        
        return obj1.collidesWith(obj2);
    }
    
    /**
     * Check collisions between an object and a list of potential colliders
     */
    public List<GameObject> checkCollisions(GameObject obj, List<GameObject> potentialColliders) {
        List<GameObject> collisions = new ArrayList<>();
        
        for (GameObject other : potentialColliders) {
            if (checkCollision(obj, other)) {
                collisions.add(other);
            }
        }
        
        return collisions;
    }
    
    /**
     * Check if a circle collides with any object in the grid
     */
    public boolean checkCircleCollision(float x, float y, float radius, List<GameObject> objects) {
        for (GameObject obj : objects) {
            if (!obj.isActive()) continue;
            
            PVector objPos = obj.getPosition();
            float distance = PVector.dist(new PVector(x, y), objPos);
            
            if (distance < radius + obj.getRadius()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the cell index for a given position
     */
    private int getCellIndex(float x, float y) {
        int col = (int) (x / gridSize);
        int row = (int) (y / gridSize);
        
        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) {
            return -1;
        }
        
        return row * gridCols + col;
    }
    
    /**
     * Debug: Get the number of objects in each cell
     */
    public Map<Integer, Integer> getCellCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Map.Entry<Integer, List<GameObject>> entry : spatialGrid.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }
    
    /**
     * Get grid statistics for debugging
     */
    public String getStats() {
        int totalObjects = spatialGrid.values().stream()
            .mapToInt(List::size)
            .sum();
        int occupiedCells = spatialGrid.size();
        int totalCells = gridCols * gridRows;
        
        return String.format("Grid: %dx%d (%d cells), Objects: %d, Occupied: %d (%.1f%%)",
            gridCols, gridRows, totalCells, totalObjects, occupiedCells,
            (occupiedCells * 100.0f / totalCells));
    }
}
