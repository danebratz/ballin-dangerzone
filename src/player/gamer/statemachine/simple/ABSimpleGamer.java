package player.gamer.statemachine.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import apps.player.detail.DetailPanel;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public final class ABSimpleGamer extends StateMachineGamer {

	private StateMachine stateMachine;
	private HashMap<Integer, Integer> stateCodeMap = new HashMap<Integer, Integer>();
	private int count = 0;
	private static final int MAX_DEPTH = 6;
	private static final int GOAL_MAX = 100;
	private static final int GOAL_MIN = 0;
	private static final int EVAL_FN_MAX = 80;
	private int max_search_mobility; //use this in metagame computation of relative max for heuristics
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}


	@Override
	public void stateMachineMetaGame(long timeout)// make sure to take into account the timeout
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		
	}

	@Override
	public Move stateMachineSelectMove(long timeout)//make sure to take into account the timeout
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		StateMachine stateMachine = getStateMachine();
		MachineState currentState = getCurrentState();
		Role role = getRole();
		List<Move> moves = stateMachine.getLegalMoves(currentState, role);
		
		int maxIndex = 0;
		int maxValue = 0;
		for(int i=0; i<moves.size(); i++){
			int nextValue = getMinValue(currentState, role, moves.get(i), Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}
	
	private int getMaxValue(MachineState currentState, Role role, int alpha, int beta, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		count ++; ////////
		StateMachine stateMachine = getStateMachine();
		if(stateMachine.isTerminal(currentState))
			return stateMachine.getGoal(currentState, role);
		if(!expFn(currentState, level))
			return evalFn(currentState, role);
		List<Move> moves = stateMachine.getLegalMoves(currentState, role);
		
		int maxValue = 0;
		int nextValue = 0;
		for(int i=0; i<moves.size(); i++){
			nextValue = getMinValue(currentState, role, moves.get(i), alpha, beta, level);
			if(nextValue > maxValue)
				maxValue = nextValue;
			if(maxValue >= beta) return maxValue;
			if(maxValue > alpha) alpha = maxValue;
		}
		return maxValue;
	}
	
	private int getMinValue(MachineState currentState, Role role, Move move, int alpha, int beta, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		count ++; //////////
		StateMachine stateMachine = getStateMachine();
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(currentState, role, move);
		
		int minValue = GOAL_MAX;
		for(int i=0; i<jointMoves.size(); i++){
			MachineState newState = stateMachine.getNextState(currentState, jointMoves.get(i));
			int stateHashCode = newState.hashCode();
			int nextValue;
			if(!stateCodeMap.containsKey(stateHashCode)) {
				nextValue = getMaxValue(newState, role, alpha, beta, level+1);
				stateCodeMap.put(stateHashCode,nextValue);
			} else {
				nextValue = stateCodeMap.get(stateHashCode);
			}
			if(nextValue < minValue)
				minValue = nextValue;
			if(minValue <= alpha) return minValue;
			if(minValue < beta) beta = minValue;
		}
		return minValue;
	}
	
	/*
	 * Serves as wrapper for specific heuristic implementations
	 */
	private int evalFn(MachineState state, Role role) throws MoveDefinitionException{
		return mobility1(state, role);
	}
	
	/*
	 * As of now, simply does a depth check
	 */
	private boolean expFn(MachineState state, int level){
		if(level > MAX_DEPTH) return false;
		else return true;
	}
	
	
	/*
	 * Simple mobility fn - depth of one
	 */
	private int mobility1(MachineState state, Role role) throws MoveDefinitionException{
		List<Move> moveList = getStateMachine().getLegalMoves(state,role);
		return moveList.size();
	}
	
	/*
	 * Simple mobility fn - depth of two
	 */
	private int mobility2(MachineState state, Role role) throws TransitionDefinitionException, MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();
		List<List<Move>> moveList = stateMachine.getLegalJointMoves(state);
		int mobility = moveList.size();
		for(List<Move> moves:moveList) {
			MachineState next = stateMachine.getNextState(state, moves);
			mobility += stateMachine.getLegalMoves(next,role).size();
		}
		return mobility;
	}
	
	/*
	 * 
	 */
	private int opponentMinMobility(MachineState state, Role role) {

		List<Role> roles = getStateMachine().getRoles();

		List<Move> moveList = getStateMachine().getLegalMoves(state,role);

		ArrayList<Integer> mobilityArray = new ArrayList<Integer>();

		int mobilitySum = 0;



		for(Role opponentRole : roles) {
			int mobility = evalfun(state,opponentRole);//use either one of our mobility functions here
			mobilitySum += mobility;
			mobilityArray.add(mobility);
		}
		//return mobilitySum/mobilityArray.size();
		return Collections.min(mobilityArray);
	}
	
	/*
	 * 
	 */
	
	int opponentMaxMobility(MachineState state, Role role) {
		List<Role> roles = getStateMachine().getRoles();
		List<Move> moveList = getStateMachine().getLegalMoves(state,role);
		ArrayList<Integer> mobilityArray = new ArrayList<Integer>();
		int mobilitySum = 0;
		for(Role opponentRole : roles) {
			int mobility = evalfun(state,opponentRole);//use either one of our mobility functions here
			mobilitySum += mobility;
			mobilityArray.add(mobility);
		}
		//return mobilitySum/mobilityArray.size();
		return Collections.max(mobilityArray);
	}
	
	/*
	 * 
	 */
	
	public int getMaxGoal(MachineState state, Role role) throws GoalDefinitionException	{
		Set<GdlSentence> results = prover.askAll(ProverQueryBuilder.getGoalQuery(role), ProverQueryBuilder.getContext(state));
		if (results.size() != 1){
			GamerLogger.logError("StateMachine", "Got goal results of size: " + results.size() + " when expecting size one.");
			throw new GoalDefinitionException(state, role);
		}
		try{
			ArrayList<Integer> maxGoalArray = new ArrayList<Integer>();
			Iterator<GdlSentence> it = results.iterator();
			while(it.hasNext()) {
				GdlRelation  relation = (GdlRelation) it.next();
				GdlConstant constant = (GdlConstant) relation.get(1);
				maxGoalArray.add(Integer.parseInt(constant.toString()));
			}
			return Collections.max(maxGoalArray);
		}
	catch (Exception e)	{
		throw new GoalDefinitionException(state, role);
		}
	}
	
	@Override
	public String getName() {
		return "ABPruner";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}
