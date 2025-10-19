package game.managers;

import processing.core.PApplet;

public class InputHandler {
    private final PApplet sketch;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean spacePressed = false;

    public InputHandler(PApplet sketch) {
        this.sketch = sketch;
        setupKeyBindings();
    }

    private void setupKeyBindings() {
        sketch.registerMethod("keyEvent", this);
    }

    public void keyEvent(processing.event.KeyEvent event) {
        boolean pressed = (event.getAction() == processing.event.KeyEvent.PRESS);
        
        switch (event.getKeyCode()) {
            case PApplet.LEFT:
            case 'A':
            case 'a':
                leftPressed = pressed;
                break;
            case PApplet.RIGHT:
            case 'D':
            case 'd':
                rightPressed = pressed;
                break;
            case ' ':
                spacePressed = pressed;
                break;
        }
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public boolean isSpacePressed() {
        return spacePressed;
    }

    public void update() {
        // Handle continuous input or input buffering here if needed
    }
}
