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
	private static final int NO_SOLUTION = -1;
	private double max_search_mobility; //use this in metagame computation of relative max for heuristics
	private long finishBy;
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}


	@Override
	public void stateMachineMetaGame(long timeout)// make sure to take into account the timeout
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		mobilityMetaGame(timeout);
	}
	
	/*
	 * performs a necessary metagame if the mobility family of heuristics is in use
	 */
	private void mobilityMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException{
		this.max_search_mobility = (double)monteCarloDepthSearch(timeout);
	}
	
	/*
	 * Performs a series of depth charges to randomly search the game tree to help to normalize to mobility heuristic values
	 */
	private int monteCarloDepthSearch(long timeout) throws TransitionDefinitionException, MoveDefinitionException {  
		long start = System.currentTimeMillis();
		finishBy = timeout - 1000;
        ArrayList<Integer> maxList = new ArrayList<Integer>();
        while(true) {
        	MachineState state = getStateMachine().getInitialState();
		    while(!getStateMachine().isTerminal(state)) {
		    	if (System.currentTimeMillis() > finishBy)
		    		break;
		    	state = getStateMachine().getNextState(state, getStateMachine().getRandomJointMove(state));
		    	int localMax = 0;
		    	for(int i = 0; i < getStateMachine().getRoles().size(); i++){
		    		int temp = 0;
		    		try{
		    			temp = getStateMachine().getLegalMoves(state, getStateMachine().getRoles().get(i)).size();
		    		} catch (MoveDefinitionException e){
		    			
		    		}
		    		if(temp >= localMax){
		    			localMax = temp;
		    		}
		    	}
            		maxList.add(localMax);
        	}
		    if (System.currentTimeMillis() > finishBy)
	    		break;
        }     
        return Collections.max(maxList);
    }    

	@Override
	public Move stateMachineSelectMove(long timeout)//make sure to take into account the timeout
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long start = System.currentTimeMillis();
		finishBy = timeout - 1000;
		StateMachine stateMachine = getStateMachine();
		//MachineState currentState = getCurrentState();
		Role role = getRole();
		List<Move> moves = stateMachine.getLegalMoves(getCurrentState(), role);
		
		int maxIndex = 0;
		int maxValue = 0;
		int j = 0;
		while(true){
			for(int i=0; i<moves.size(); i++){
				int nextValue = getMinValue(getCurrentState(), role, moves.get(i), Integer.MIN_VALUE, Integer.MAX_VALUE, j);
				if(nextValue > maxValue){
					maxValue = nextValue;
					maxIndex = i;
				}
				if(System.currentTimeMillis() > finishBy) break;
			}
			if(System.currentTimeMillis() > finishBy) break;
			j++;
		}
		//System.out.print("Count = ");
		//System.out.println(count); //////////////
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
		//count ++; ////////
		StateMachine stateMachine = getStateMachine();
		if(!expFn(currentState, level))
			return evalFn(currentState, role, level);
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
		//count ++; //////////
		StateMachine stateMachine = getStateMachine();
		List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(currentState, role, move);
		
		int minValue = GOAL_MAX;
		for(int i=0; i<jointMoves.size(); i++){
			MachineState newState = stateMachine.getNextState(currentState, jointMoves.get(i));
			int stateHashCode = newState.hashCode();
			int nextValue;
			if(!stateCodeMap.containsKey(stateHashCode)) {
				nextValue = getMaxValue(newState, role, alpha, beta, level-1);
				stateCodeMap.put(stateHashCode,nextValue);
			} else {
				nextValue = stateCodeMap.get(stateHashCode);
			}
			if(nextValue < minValue && nextValue != NO_SOLUTION)
				minValue = nextValue;
			if(minValue <= alpha) return minValue;
			if(minValue < beta) beta = minValue;
		}
		return minValue;
	}
	
	/*
	 * Serves as wrapper for specific heuristic implementations
	 */
	private int evalFn(MachineState state, Role role, int level) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if(getStateMachine().isTerminal(state)){
			//System.out.println("evalfn terminal state");
			return getStateMachine().getGoal(state, role);
		}
		if(level <= 0) return NO_SOLUTION;
		return mobility2(state, role);
	}
	
	/*
	 * As of now, simply does a depth check
	 */
	private boolean expFn(MachineState state, int level){
		if(getStateMachine().isTerminal(state)) {
			//System.out.println("expfn terminal state");
			return false;
		}
		//if(level > MAX_DEPTH) return false;
		if(System.currentTimeMillis() > finishBy) return false;
		else return true;
	}
	
	
	/*
	 * Simple Focus heuristic - depth of one
	 */
	private int focus1(MachineState state, Role role) throws MoveDefinitionException {
		List<Move> moveList = getStateMachine().getLegalMoves(state,role);
		double size = (double)moveList.size();
		size = this.max_search_mobility - size;
		int normalized = (int) ((size/this.max_search_mobility) * GOAL_MAX);
		if(normalized <= GOAL_MIN) return EVAL_FN_MAX;
		return normalized;
	}
	
	
	/*
	 * Simple mobility fn - depth of one
	 */
	private int mobility1(MachineState state, Role role) throws MoveDefinitionException{
		List<Move> moveList = getStateMachine().getLegalMoves(state,role);
		double size = (double)moveList.size();
		int normalized = (int) ((size/this.max_search_mobility) * GOAL_MAX);
		if(normalized >= EVAL_FN_MAX) return EVAL_FN_MAX;
		return normalized;
	}
	
	/*
	 * Simple mobility fn - depth of two
	 */
	private int mobility2(MachineState state, Role role) throws TransitionDefinitionException, MoveDefinitionException {
		StateMachine stateMachine = getStateMachine();
		List<List<Move>> moveList = stateMachine.getLegalJointMoves(state);
		int mobility = moveList.size();
		int count = 1;
		for(List<Move> moves:moveList) {
			MachineState next = stateMachine.getNextState(state, moves);
			try{
				mobility += stateMachine.getLegalMoves(next,role).size();
				count++;
			} catch(MoveDefinitionException e){}
		}
		return mobility/count; //average mobilities coming from a state - could do max or min too
	}
	
	/*
	 * 
	 */
	private int opponentMinMobility(MachineState state, Role role) throws MoveDefinitionException {
		List<Role> roles = getStateMachine().getRoles();
		List<Move> moveList = getStateMachine().getLegalMoves(state,role);
		ArrayList<Integer> mobilityArray = new ArrayList<Integer>();
		int mobilitySum = 0;
		for(Role opponentRole : roles) {
			int mobility = mobility1(state,opponentRole);//use either one of our mobility functions here
			mobilitySum += mobility;
			mobilityArray.add(mobility);
		}
		//return mobilitySum/mobilityArray.size();
		return Collections.min(mobilityArray);
	}
	
	/*
	 * 
	 */
	
	int opponentMaxMobility(MachineState state, Role role) throws MoveDefinitionException {
		List<Role> roles = getStateMachine().getRoles();
		List<Move> moveList = getStateMachine().getLegalMoves(state,role);
		ArrayList<Integer> mobilityArray = new ArrayList<Integer>();
		int mobilitySum = 0;
		for(Role opponentRole : roles) {
			int mobility = mobility1(state,opponentRole);//use either one of our mobility functions here
			mobilitySum += mobility;
			mobilityArray.add(mobility);
		}
		//return mobilitySum/mobilityArray.size();
		return Collections.max(mobilityArray);
	}
	
	/*
	 * 
	 */
	
	/*public int getMaxGoal(MachineState state, Role role) throws GoalDefinitionException	{
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
	}*/
	
	@Override
	public String getName() {
		return "ABPruner";
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}
