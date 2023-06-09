import java.util.EnumMap;

public class StateManager  
{
    // instance variables - replace the example below with your own
    private PhysicsEntity entity;
    private EnumMap<State, BaseState> entityStates;
    
    private BaseState currentState;
    private State state;

    public StateManager(PhysicsEntity entity)
    {
        this.entity = entity;
        entityStates = entity.getStates();
    }

    public void handleInput() {
        State newState = currentState.input();
        if(newState != State.NULL) {
            changeState(newState);
        }
    }
    
    public void handleProcess() {
        State newState = currentState.process();

        if(newState != State.NULL) {
            changeState(newState);
        }
    }
    
    public void changeState(State newState) {
        if(currentState != null) {
            currentState.exit();
        }
        
        currentState = entityStates.get(newState);
        state = newState;
        currentState.enter();
    }
    
    public State getCurrentState() {
        return state;
    }
}
