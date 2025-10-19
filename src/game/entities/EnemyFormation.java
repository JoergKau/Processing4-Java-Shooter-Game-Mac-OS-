// Represents a formation of enemy ships that move together and attack the player
// Each ship in the formation has slight individual movement while maintaining formation

package game.entities;

import processing.core.PApplet;
import processing.core.PVector;
import java.util.ArrayList;

public class EnemyFormation {
    private PApplet sketch;
    public ArrayList<EnemyShip> ships;
    public PVector centerPos;
    public PVector velocity;
    public int formationType; // 0=V-shape, 1=line, 2=diamond, 3=circle
    public float formationTimer = 0;
    public boolean isActive = true;
    public float wobblePhase = 0;
    
    // Formation behavior
    public float attackTimer = 0;
    private float attackInterval;
    private boolean inAttackMode = false;
    private PVector targetPos;
    
    // Galaxian-style diving attack (Phase 3)
    public boolean useGalaxianPattern = false;
    public boolean useRandomMovementPatterns = false; // Phase 3: random patterns instead of formation index
    private ArrayList<Integer> divingShipIndices = new ArrayList<>();
    private ArrayList<Float> diveAngles = new ArrayList<>();
    private ArrayList<Float> diveTimers = new ArrayList<>();
    private ArrayList<PVector> diveStartPos = new ArrayList<>();
    private ArrayList<Float> diveSpeedMultipliers = new ArrayList<>(); // Random speed variation
    
    public EnemyFormation(PApplet sketch, int shipCount, int formationType, ArrayList<processing.core.PImage> shipImages) {
        this.sketch = sketch;
        this.ships = new ArrayList<>();
        this.centerPos = new PVector();
        this.velocity = new PVector();
        this.formationType = formationType;
        this.targetPos = new PVector();
        
        // Random spawn position well outside the screen (at least 150 pixels above)
        centerPos.set(sketch.random(100, sketch.width - 100), -150);
        
        // Initial downward velocity
        velocity.set(sketch.random(-30, 30), sketch.random(40, 70));
        
        // Attack interval (will be overridden for Galaxian pattern)
        attackInterval = sketch.random(3, 6);
        
        // Create ships in formation
        createFormation(shipCount, shipImages);
    }
    
    private void createFormation(int shipCount, ArrayList<processing.core.PImage> shipImages) {
        // Select one ship type for the entire formation
        processing.core.PImage formationShipImage = null;
        if (shipImages != null && !shipImages.isEmpty()) {
            formationShipImage = shipImages.get((int) sketch.random(shipImages.size()));
        }
        
        for (int i = 0; i < shipCount; i++) {
            EnemyShip ship = new EnemyShip(sketch, shipImages);
            ship.isSplitChild = true; // Prevent splitting
            ship.canSplit = false;
            
            // Use the same ship image for all ships in formation
            if (formationShipImage != null) {
                ship.img = formationShipImage;
                ship.scaledImg = formationShipImage.copy();
                ship.scaledImg.resize(ship.radius * 2, ship.radius * 2);
            }
            
            // Reduce bomb drop rate by 20% (increase interval by 25%)
            ship.bombInterval = sketch.random(1.875f, 5.0f); // Was 1.5-4.0, now 1.875-5.0 (+25%)
            ship.nextBombTime = (sketch.millis() / 1000.0f) + ship.bombInterval;
            
            // Position ship in formation
            PVector offset = getFormationOffset(i, shipCount);
            ship.pos.set(centerPos.x + offset.x, centerPos.y + offset.y);
            
            // Assign movement pattern
            if (useRandomMovementPatterns) {
                // Phase 3: Random movement pattern (0-7)
                ship.movementPattern = (int) sketch.random(8);
            } else {
                // Phase 1 & 2: Use formation index for individual wobble
                ship.movementPattern = i;
            }
            
            ships.add(ship);
        }
    }
    
    private PVector getFormationOffset(int index, int total) {
        float spacing = 80; // Increased spacing for more room between ships
        PVector offset = new PVector();
        
        switch (formationType) {
            case 0: // V-shape
                int row = index / 2;
                int side = (index % 2 == 0) ? -1 : 1;
                offset.x = side * row * spacing;
                offset.y = row * spacing * 0.7f;
                break;
                
            case 1: // Horizontal line
                offset.x = (index - total / 2.0f) * spacing;
                offset.y = 0;
                break;
                
            case 2: // Diamond
                if (index == 0) {
                    offset.set(0, -spacing);
                } else if (index == total - 1) {
                    offset.set(0, spacing);
                } else {
                    int diamondSide = (index % 2 == 0) ? -1 : 1;
                    offset.x = diamondSide * spacing;
                    offset.y = 0;
                }
                break;
                
            case 3: // Circle
                float angle = index * PApplet.TWO_PI / total;
                offset.x = PApplet.cos(angle) * spacing;
                offset.y = PApplet.sin(angle) * spacing;
                break;
        }
        
        return offset;
    }
    
    public void update(float deltaTime, PVector playerPos) {
        formationTimer += deltaTime;
        wobblePhase += deltaTime * 2;
        attackTimer += deltaTime;
        
        // Galaxian-style diving attacks (Phase 3)
        if (useGalaxianPattern) {
            // Randomly select ships to dive
            if (attackTimer >= attackInterval && divingShipIndices.size() < 2) {
                int shipIndex = (int) sketch.random(ships.size());
                if (!divingShipIndices.contains(shipIndex)) {
                    divingShipIndices.add(shipIndex);
                    diveAngles.add(0f);
                    diveTimers.add(0f);
                    diveStartPos.add(ships.get(shipIndex).pos.copy());
                    // Random speed multiplier: 0.5 to 1.0 (additional 30% reduction from max)
                    // Base speed already reduced by 58%, now max is 70% of that (0.7)
                    diveSpeedMultipliers.add(sketch.random(0.5f, 0.7f));
                    attackTimer = 0;
                    attackInterval = sketch.random(1, 2.5f); // Reduced from 2-4 to 1-2.5 seconds
                }
            }
        } else {
            // Original attack mode (Phase 2)
            if (!inAttackMode && attackTimer >= attackInterval) {
                inAttackMode = true;
                targetPos.set(playerPos.x, playerPos.y);
                attackTimer = 0;
                attackInterval = sketch.random(4, 8);
            }
        }
        
        // Update center position
        if (inAttackMode) {
            // Move towards player horizontally, but keep moving downward
            PVector direction = PVector.sub(targetPos, centerPos);
            float distance = direction.mag();
            
            if (distance > 5) {
                direction.normalize();
                direction.mult(150); // Attack speed
                velocity.lerp(direction, 0.05f);
                // Ensure formation continues moving downward (never upward)
                if (velocity.y < 30) {
                    velocity.y = 30; // Minimum downward speed
                }
            } else {
                // Reached target, exit attack mode
                inAttackMode = false;
                velocity.set(sketch.random(-30, 30), sketch.random(40, 70));
            }
        } else {
            // Normal movement with slight direction changes
            if (sketch.random(1) < 0.02f) {
                velocity.x += sketch.random(-20, 20);
                velocity.x = PApplet.constrain(velocity.x, -80, 80);
            }
            // Ensure always moving downward
            if (velocity.y < 30) {
                velocity.y = sketch.random(40, 70);
            }
        }
        
        centerPos.x += velocity.x * deltaTime;
        centerPos.y += velocity.y * deltaTime;
        
        // Keep formation on screen horizontally
        if (centerPos.x < 100) {
            centerPos.x = 100;
            velocity.x = Math.abs(velocity.x);
        } else if (centerPos.x > sketch.width - 100) {
            centerPos.x = sketch.width - 100;
            velocity.x = -Math.abs(velocity.x);
        }
        
        // Update all ships in formation
        for (int i = 0; i < ships.size(); i++) {
            EnemyShip ship = ships.get(i);
            
            // Check if this ship is diving (Galaxian pattern)
            int diveIndex = divingShipIndices.indexOf(i);
            if (diveIndex >= 0) {
                // Update dive behavior
                updateDivingShip(i, diveIndex, deltaTime, playerPos);
            } else {
                // Normal formation behavior
                // Calculate target position in formation
                PVector offset = getFormationOffset(i, ships.size());
                
                // Add individual wobble (increased amplitude for more visible movement)
                float wobbleOffset = PApplet.sin(wobblePhase + i * 0.8f) * 12;
                offset.x += wobbleOffset;
                offset.y += PApplet.cos(wobblePhase * 0.7f + i * 0.5f) * 8;
                
                PVector targetShipPos = PVector.add(centerPos, offset);
                
                // Smoothly move ship to target position
                ship.pos.lerp(targetShipPos, 0.1f);
            }
            
            // Update ship's internal state
            ship.wobble += deltaTime * 3;
            ship.warpTime += deltaTime;
            if (ship.warpTime >= ship.warpDuration) {
                ship.warpingIn = false;
            }
            
            // Update shield timer
            ship.shieldTimer += deltaTime;
            if (ship.shieldActive && ship.shieldTimer >= ship.shieldOnDuration) {
                ship.shieldActive = false;
                ship.shieldTimer = 0;
            } else if (!ship.shieldActive && ship.shieldTimer >= ship.shieldOffDuration) {
                ship.shieldActive = true;
                ship.shieldTimer = 0;
            }
            
            // Update glow pulse
            ship.glowPulse += deltaTime * 2;
            
            // Update thruster flicker
            ship.thrusterFlicker += deltaTime * 10;
        }
        
        // Check if formation is off screen
        if (centerPos.y > sketch.height + 100) {
            isActive = false;
        }
    }
    
    private void updateDivingShip(int shipIndex, int diveIndex, float deltaTime, PVector playerPos) {
        EnemyShip ship = ships.get(shipIndex);
        
        // Update dive timer
        float diveTimer = diveTimers.get(diveIndex) + deltaTime;
        diveTimers.set(diveIndex, diveTimer);
        
        // Determine which side of formation the ship is on
        int totalShips = ships.size();
        boolean isLeftSide = shipIndex < totalShips / 2;
        
        // Galaxian-style curved dive path
        float angle = diveAngles.get(diveIndex);
        PVector startPos = diveStartPos.get(diveIndex);
        float speedMultiplier = diveSpeedMultipliers.get(diveIndex);
        
        // Diving phase (reduced by 40%: was 3 seconds, now 1.8 seconds)
        float diveDuration = 1.8f;
        float returnDuration = 1.8f;
        
        if (diveTimer < diveDuration) {
            // Curve down and to the side
            float progress = diveTimer / diveDuration;
            
            // Angle changes based on side (reduced by 40% + 30% = 58% total)
            // Now with random speed multiplier (additional 30% reduction from max)
            if (isLeftSide) {
                angle += deltaTime * 50 * speedMultiplier; // Curve left with random speed
            } else {
                angle -= deltaTime * 50 * speedMultiplier; // Curve right with random speed
            }
            diveAngles.set(diveIndex, angle);
            
            // Move in curved path with random speed multiplier
            float baseSpeed = 84 + progress * 42; // Base speed (was 200+100, then 120+60, now 84+42)
            float speed = baseSpeed * speedMultiplier; // Apply random multiplier (0.5-0.7)
            float radAngle = PApplet.radians(angle);
            ship.pos.x += PApplet.cos(radAngle) * speed * deltaTime;
            ship.pos.y += PApplet.sin(radAngle) * speed * deltaTime;
            
        } else {
            // Return to formation or exit screen
            if (diveTimer < diveDuration + returnDuration) {
                // Arc back up
                float returnProgress = (diveTimer - diveDuration) / returnDuration;
                
                // Curve back towards top with random speed multiplier
                if (isLeftSide) {
                    angle -= deltaTime * 63 * speedMultiplier; // Curve back right with random speed
                } else {
                    angle += deltaTime * 63 * speedMultiplier; // Curve back left with random speed
                }
                diveAngles.set(diveIndex, angle);
                
                float baseSpeed = 105; // Base return speed (was 250, then 150, now 105)
                float speed = baseSpeed * speedMultiplier; // Apply random multiplier (0.5-0.7)
                float radAngle = PApplet.radians(angle);
                ship.pos.x += PApplet.cos(radAngle) * speed * deltaTime;
                ship.pos.y += PApplet.sin(radAngle) * speed * deltaTime;
                
            } else {
                // Remove from diving list and return to formation
                divingShipIndices.remove(diveIndex);
                diveAngles.remove(diveIndex);
                diveTimers.remove(diveIndex);
                diveStartPos.remove(diveIndex);
                diveSpeedMultipliers.remove(diveIndex);
            }
        }
        
        // Remove ship if it goes off screen
        if (ship.pos.y > sketch.height + 50 || ship.pos.x < -50 || ship.pos.x > sketch.width + 50) {
            divingShipIndices.remove(diveIndex);
            diveAngles.remove(diveIndex);
            diveTimers.remove(diveIndex);
            diveStartPos.remove(diveIndex);
            diveSpeedMultipliers.remove(diveIndex);
        }
    }
    
    public boolean isOffScreen() {
        return !isActive || centerPos.y > sketch.height + 100;
    }
    
    public void removeShip(EnemyShip ship) {
        ships.remove(ship);
        if (ships.isEmpty()) {
            isActive = false;
        }
    }
    
    public boolean isEmpty() {
        return ships.isEmpty();
    }
}
