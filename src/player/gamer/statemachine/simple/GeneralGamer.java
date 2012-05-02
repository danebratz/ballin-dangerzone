package player.gamer.statemachine.simple;

import player.gamer.statemachine.StateMachineGamer;

import java.util.HashMap;
import java.util.List;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.MachineState;
import util.statemachine.Role;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class GeneralGamer extends StateMachineGamer {

	private HashMap<Integer, Integer> hashmap = new HashMap<Integer, Integer>();
	private int count = 0;
	
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}

	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		// Does nothing yet...
	}

	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();
		Role role = getRole();
		List<Move> moves = stateMachine.getLegalMoves(currentState, role);
		
		int maxIndex = 0;
		int maxValue = 0;
		for(int i=0; i<moves.size(); i++){
			int nextValue = getMinValue(currentState, role, moves.get(i));
			if(nextValue > maxValue){
				maxValue = nextValue;
				maxIndex = i;
			}
		}
		System.out.print("Count = ");
		System.out.println(count); //////////////
		return moves.get(maxIndex);
	}

	@Override
	public void stateMachineStop() {
		// Does nothing yet...
	}

	@Override
	public void stateMachineAbort() {
		// Does nothing yet...
	}
	
	private int getMaxValue(MachineState currentState, Role role) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		count ++; ////////
		StateMachine stateMachine = getStateMachine();
		if(stateMachine.isTerminal(currentState))
			return stateMachine.getGoal(currentState, role);
		
		List<Move> moves = stateMachine.getLegalMoves(currentState, role);
		
		int maxValue = 0;
		for(int i=0; i<moves.size(); i++){
			int nextValue = getMinValue(currentState, role, moves.get(i));
			if(nextValue > maxValue)
				maxValue = nextValue;
		}
		return maxValue;
	}
	
	private int getMinValue(MachineState currentState, Role role, Move move) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		count ++; //////////
		StateMachine stateMachine = getStateMachine();
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(currentState, role, move);
		
		int minValue = 100;
		for(int i=0; i<jointMoves.size(); i++){
			MachineState newState = stateMachine.getNextState(currentState, jointMoves.get(i));
			int hashcode = newState.hashCode();
			int nextValue;
			if(!hashmap.containsKey(hashcode)) {
				nextValue = getMaxValue(newState, role);
				hashmap.put(hashcode,nextValue);
			} else {
				nextValue = hashmap.get(hashcode);
			}
			if(nextValue < minValue)
				minValue = nextValue;
		}
		return minValue;
	}
}
