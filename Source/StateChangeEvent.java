//	StateChangeEvent.java - Base class for all document changes affecting states.

import java.util.*;
import javax.swing.undo.UndoableEdit;

public abstract class StateChangeEvent extends javax.swing.event.ChangeEvent {

	//	Instance variables----------------------------------------------------------------
	protected Set iAffectedStates;
	

	//	Constructor---------------------------------------------------------------------
	//	Constructor, for when there is a set of affected states.
	public StateChangeEvent(Object source, Set affectedStates){
		super(source);
		
		//	Make a copy of the affected states set, so that it remains valid for do and undo.
		iAffectedStates = new HashSet(affectedStates);
	}
	
	
	//	Constructor, for when there is a single affected state.
	public StateChangeEvent(Object source, TLState affectedState){
		super(source);
		iAffectedStates = new HashSet(1);
		iAffectedStates.add(affectedState);
	}
	
	
	//	Accessors.
	public Set getAffectedStates()		{		return iAffectedStates;			}
	
	public TLState getAffectedState(){
		Debug.assert(iAffectedStates.size() == 1);
		return (TLState)iAffectedStates.iterator().next();
	}
}