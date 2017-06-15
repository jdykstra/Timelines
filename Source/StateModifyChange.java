//	StateModifyChange.java - Change event for when states are modified.

import java.util.*;

public class StateModifyChange extends StateChangeEvent {

	// 	Constants ------------------------------------------------------------------------
	
	//	Instance variables----------------------------------------------------------------
	protected TLState iNewValue;			//	Holder of values, NOT the state itself
	
	//	Constructor---------------------------------------------------------------------
	//	Constructor, for when there is a set of affected states.
	public StateModifyChange(Object source, Set affectedStates, TLState newValue){
		super(source, affectedStates);
		iNewValue = newValue;
	}


	//	Constructor, for when there is a single affected state.
	public StateModifyChange(Object source, TLState affectedState, TLState newValue){
		super(source, affectedState);
		iNewValue = newValue;
	}

	
	public TLState getNewValue()			{	return iNewValue;		}
}