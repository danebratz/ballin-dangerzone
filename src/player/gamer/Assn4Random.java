package player.gamer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import util.game.Game;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.match.Match;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class Assn4Random extends Gamer {

	private ArrayList<Match> prevMatches;
	private Match curMatch;
	private GdlProposition roleName;
	private ProverStateMachine proverMachine;
	
	
	public Assn4Random() {
		this.prevMatches = new ArrayList<Match>();
		curMatch = null;
		roleName = null;
	}
	
	@Override
	public boolean start(String matchId, GdlProposition roleName, Game game,
			int startClock, int playClock, long receptionTime)
			throws MetaGamingException {
		this.curMatch = new Match(matchId, startClock, playClock, game);
		this.roleName = roleName;
		this.proverMachine = new ProverStateMachine();
		proverMachine.initialize(this.curMatch.getGame().getRules());
		return true;
	}

	@Override
	public GdlSentence play(String matchId, List<GdlSentence> moves,
			long receptionTime) throws MoveSelectionException,
			TransitionDefinitionException, MoveDefinitionException {
		MachineState state = this.proverMachine.getInitialState();
		if(moves != null) {
			this.curMatch.appendMoves(moves);
		}
		if(moves.size() == 0){
			curMatch.appendState(state.getContents());
		} else {
			state = this.proverMachine.getNextState(new MachineState(curMatch.getMostRecentState()), Move.gdlToMoves(moves));
			this.curMatch.appendState(state.getContents());
		}
		Role role = new Role(this.roleName);
		List<Move> newMoves = this.proverMachine.getLegalMoves(state, role);
		Move move = newMoves.get((int) Math.floor(Math.random()*newMoves.size())); //in minimax, this is where we need to write the move finding function
		return move.getContents();
	}
	

	@Override
	public boolean stop(String matchId, List<GdlSentence> moves) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean ping() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abort(String matchId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Match getMatch(String matchId) {
		// TODO Auto-generated method stub
		return this.curMatch;
	}
	
	public Match getMatch() {
		return this.curMatch;
	}

}
