//	StateAddDeleteChange.java - Change event for when states are added or deleted.

import java.util.*;

public class StateAddDeleteChange extends StateChangeEvent {

	// 	Constants ------------------------------------------------------------------------
	public static final boolean ADDING = false;			//	Constants for constructor parameter
	public static final boolean DELETING = true;
	
	//	Instance variables----------------------------------------------------------------
	protected boolean iDeleting;				//	True if we're deleting;  false if we're adding
	
	//	Constructor---------------------------------------------------------------------
	//	Constructor, for when there is a set of affected states.
	public StateAddDeleteChange(Object source, Set affectedStates, boolean deleting){
		super(source, affectedStates);
		iDeleting = deleting;
	}


	//	Constructor, for when there is a single affected state.
	public StateAddDeleteChange(Object source, TLState affectedState, boolean deleting){
		super(source, affectedState);
		iDeleting = deleting;
	}


	public boolean isDeleting()				{	return iDeleting;		}
}