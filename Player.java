import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

import java.util.EnumMap;

public class Player extends PhysicsEntity implements StateMachine
{
    
    private static final int MOVEMENT_SPEED = 4;
    private static final int JUMP_SPEED = 12;
    private static final int MAX_BOUNCE = 8;

    private int acceleration = 1;
    
    boolean isJumping;
    private int baseGravity;
    private int heavyGravity;

    private int currentBounce;
    private long timeSinceLastKeyPress;
    
    private int airFrames;
    private int notWallSlidingFrames;
    
    private final BaseState idleState = new IdleState();
    private final BaseState walkState = new WalkState();
    private final BaseState jumpState = new JumpState();
    private final BaseState fallState = new FallState();
    private final BaseState wallslideState = new WallslideState();
    private final BaseState hitState = new HitState();
    private final StateManager stateManager = new StateManager(this);

    private final AnimationManager animationManager = new AnimationManager(this, "images/sprites/player1/");
        
    public Player(int x, int y) {
        super(x, y);
        isJumping = false;
        baseGravity = getGravity();
        heavyGravity = baseGravity * 3;
        stateManager.changeState(State.FALL);
    }
    
    public void act()
    {   
        handleState();
        checkWinCondition();
        if (isAtEdge()) getWorld().removeObject(this);
    }
    
    public EnumMap<State, BaseState> getStates() {
        EnumMap<State, BaseState> states = new EnumMap(State.class);
        states.put(State.IDLE, idleState);
        states.put(State.WALK, walkState);
        states.put(State.JUMP, jumpState);
        states.put(State.FALL, fallState);
        states.put(State.WALLSLIDE, wallslideState);
        states.put(State.HIT, hitState);
        return states;
    }
    
    public void handleState() {
        //printState();
        stateManager.handleInput();
        stateManager.handleProcess();
    }
    
    public void printState() {
        getWorld().showText("direction: " + stateManager.getCurrentState(), getWorld().getWidth()/4 * 3, 70);
    }
    
    public void handleHorizontalMovement() {
        if(Greenfoot.isKeyDown("left")) {
            acceleration = 1;
            setVelocityX(getVelocityX() - acceleration - currentBounce);
        }else if(Greenfoot.isKeyDown("right")) {
            acceleration = 1;
            setVelocityX(getVelocityX() + acceleration - currentBounce);
        } else{
            acceleration = 0;
            setVelocityX(-currentBounce);
        }
        
        if(getVelocityX() > 0) {
            setDirectionX(Direction.RIGHT);
        } else if (getVelocityX() < 0) {
            setDirectionX(Direction.LEFT);
        }
        
        if(getVelocityX() < -MOVEMENT_SPEED) {
            setVelocityX(-MOVEMENT_SPEED - currentBounce);
        } else if (getVelocityX() > MOVEMENT_SPEED) {
            setVelocityX(MOVEMENT_SPEED - currentBounce);
        }
        
        if(currentBounce < 0) {
            currentBounce += 2;
        } else if (currentBounce > 0) {
            currentBounce -= 2;
        }
    }
    
    private Enemy getStompableEnemy() {
        
        Enemy enemy;
        
        int imageWidth = getImage().getWidth();
        int imageHeight = getImage().getHeight();
        
        if(getDirectionY() == Direction.DOWN) {

            enemy = (Enemy) getOneObjectAtOffset(imageWidth / -2 + 4, imageHeight / 2 + getVelocityY(), Enemy.class);
            
            if(enemy != null) {
                return enemy;
            }
            
            enemy = (Enemy) getOneObjectAtOffset(imageWidth / 2 - 4, imageHeight / 2 + getVelocityY(), Enemy.class);
    
            if(enemy != null) {
                return enemy;
            } 
        } else {
            enemy = (Enemy) getOneObjectAtOffset(imageWidth / -2 + 4, imageHeight / -2 + getVelocityY(), Enemy.class);
            
            if(enemy != null) {
                return enemy;
            }
            
            enemy = (Enemy) getOneObjectAtOffset(imageWidth / 2 - 4, imageHeight / -2 + getVelocityY(), Enemy.class);
    
            if(enemy != null) {
                return enemy;
            } 
        }
        
        return null;
    }
    
    private void handleEnemies() {
        
        Enemy enemy;
        
        enemy = getStompableEnemy();
        String state = "SpecialState";
        
        if(enemy != null && enemy.isKillable() && !enemy.getState().contains(state)) {
            enemy.destroy();
            stateManager.changeState(State.JUMP);
            return;
        } 
        
        enemy = (Enemy) getOneIntersectingObject(Enemy.class);    
            
        if(enemy != null  && enemy.isAlive() && !enemy.getState().contains(state)) {
            stateManager.changeState(State.HIT);
        }
    }
    
    private void checkWinCondition() {
        Goal roomGoal = (Goal) getOneIntersectingObject(Goal.class);
        if(roomGoal != null) {
            LevelWorld world = (LevelWorld) getWorld();
            world.nextLevel();
        }
    }
    
    private class IdleState implements BaseState {
        
        public void enter() {
            animationManager.changeSprite(State.IDLE);
            setVelocityY(0);
        }
        
        public State input() {
            
            handleHorizontalMovement();
            
            if (Greenfoot.isKeyDown("up")) {
                return State.JUMP;
            }
            
            return State.NULL;
        }
        
        
        public State process() {
            
            handleGravity();
            
            if(!isGrounded()) {
                airFrames++;
            } else {
                airFrames = 0;
            }
            
            if(airFrames > 3) {
                return State.FALL;
            }
            
            if(getVelocityX() != 0) {
                return State.WALK;
            }

            handleEnemies();
            
            handleCollisions();
            setMovement();
            
            animationManager.nextSprite();
            
            return State.NULL;
        }
        
        public void exit() {
            handleCollisions();
            setMovement();
        }
        
    }
    
    private class WalkState implements BaseState {
        
        public void enter() {
            animationManager.changeSprite(State.WALK);
        }
        
        public State input() {
            handleHorizontalMovement();
            
            if (Greenfoot.isKeyDown("up")) {
                return State.JUMP;
            }
            
            return State.NULL;
        }
        
        
        public State process() {
            
            handleGravity();
            
            if(!isGrounded()) {
                airFrames++;
            } else {
                airFrames = 0;
            }
            
            if(airFrames > 3) {
                return State.FALL;
            }
            
            
            if(terrainIsDown()) {
                if(getVelocityX() == 0) {
                    return State.IDLE;
                }
            } 
            
            handleEnemies();
            
            handleCollisions();
            setMovement();
            
            animationManager.nextSprite();
            
            return State.NULL;
            
        }
        
        public void exit() {
            handleCollisions();
            setMovement();
        }
        
    }
    
    private class JumpState implements BaseState {
        
        public void enter() {
            
            animationManager.changeSprite(State.JUMP);

            if(getDirectionY() == Direction.UP) {
                setVelocityY(JUMP_SPEED);
            } else {
                setVelocityY(-JUMP_SPEED);
            }
            airFrames = 0;
        }
        
        public State input() {
            
            handleHorizontalMovement();
            
            if(!Greenfoot.isKeyDown("up")) {
                setGravity(heavyGravity);
            }
            return State.NULL;
        }
        
        public State process() {
            
            handleGravity();
            
            if((terrainIsLeft() || terrainIsRight()) && airFrames > 5) {
                return State.WALLSLIDE;
            }
            
            if(isFalling()) {
                return State.FALL;
            }
            
            airFrames++;
            
            handleEnemies();
            
            handleCollisions();
            setMovement();
            
            animationManager.nextSprite();
            
            return State.NULL;
        }
        
        public void exit() {
            handleCollisions();
            setMovement();
        }
        
    }
    
    private class FallState implements BaseState {
        
        public void enter() {
            animationManager.changeSprite(State.FALL);
        }
        
        public State input() {
            handleHorizontalMovement();
            return State.NULL;
        }
        
        public State process() {
            
            handleGravity();
            
            if((terrainIsLeft() || terrainIsRight()) && airFrames > 5) {
                return State.WALLSLIDE;
            }
            
            if(isGrounded()) {
                setGravity(baseGravity);
                return State.IDLE;
            }
            
            airFrames++;
            
            handleEnemies();
            
            handleCollisions();
            setMovement();
            
            animationManager.nextSprite();
            
            return State.NULL;
        }
        
        public void exit() {
            handleCollisions();
            setMovement();
        }
        
    }
    
    private class WallslideState implements BaseState {
        
        public void enter() {
            animationManager.changeSprite(State.WALLSLIDE);
            airFrames = 0;
        }
        
        public State input() {
            
            if (Greenfoot.isKeyDown("up")) {
                if(getDirectionX() == Direction.RIGHT) {
                currentBounce = MAX_BOUNCE;
                }else if(getDirectionX() == Direction.LEFT) {
                    currentBounce = -MAX_BOUNCE;
                }
                return State.JUMP;
            }
            
            if((Greenfoot.isKeyDown("left") && getDirectionX() == Direction.RIGHT) ||
               (Greenfoot.isKeyDown("right") && getDirectionX() == Direction.LEFT)) {
                return State.FALL;
            }
            
            return State.NULL;
        }
        
        public State process() {
            
            handleGravity();
            
            if(getDirectionY() == Direction.UP) {
                setVelocityY(-1);
            } else {
                setVelocityY(1);
            }
            
            if(terrainIsDown()) {
                return State.IDLE;
            }
            
            if(!isOnWall()){
                return State.FALL;
            }
            
            handleEnemies();
            
            handleCollisions();
            setMovement();
            
            animationManager.nextSprite();
            
            return State.NULL;
        }
        
        public void exit() {
            handleCollisions();
            setMovement();
        }
        
    }
    
    private class HitState implements BaseState {
        
        public void enter() {
            animationManager.changeSprite(State.HIT);
            if(getDirectionY() == Direction.UP) {
                setVelocityY(3);
            } else {
                setVelocityY(-3);
            }
            
            if(getDirectionX() == Direction.LEFT) {
                setVelocityX(3);
            } else {
                setVelocityX(-3);                
            }
            
            airFrames = 0;
        }
        
        public State input() {
            return State.NULL;
        }
        
        public State process() {
            
            setVelocityY(getVelocityY() + 1);
            
            setRotation(getRotation() + airFrames);
            
            setMovement();
            
            animationManager.nextSprite();
            
            airFrames++;
            
            return State.NULL;
        }
        
        public void exit() {
            
        }
        
    }
}
