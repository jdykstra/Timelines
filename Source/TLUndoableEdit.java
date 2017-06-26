//	TLUndoableEdit.java - Base class for all Timelines document edits.

import javax.swing.undo.*;
import java.util.*;
import java.util.List;

//	Overview of editing mechanism:
//	----------------------
//	Individual editing commands (frequently Actions) build subclasses of TLUndoableEdit, and then call
//	TLDocument.executeEdit(), which delegates to EditManager.executeEdit().  This method calls the
//	doEdit() method of the TLUndoableEdit subclass, which actually changes the documents datastructures,
//	usually through a call to TLDocument.editXXXXX().  This TLDocument method generates ChangeEvents
//	which are listened to by all UI components that need to track changes to the document's data.

//	Some edits affect a set of states, while others only affect a single state.  
//	This class has constructors and getAffectedStates()/getAffectedState() which efficiently
//	handle both cases.
public abstract class TLUndoableEdit extends AbstractUndoableEdit {

	protected TLDocument iDoc;
	protected Set iAffectedStates;
	
	
	//	Constructor, for when there is a set of affected states.
	public TLUndoableEdit(Set affectedStates){
		
		//	Make a copy of the affected states set, so that it remains valid for do and undo.
		iAffectedStates = new HashSet(affectedStates);
	}
	
	
	//	Constructor, for when there is a single affected state.
	public TLUndoableEdit(TLState affectedState){
		iAffectedStates = new HashSet(1);
		iAffectedStates.add(affectedState);
	}
	
	
	//	Remember the document which has been edited, so that we can find it again if we have
	//	to undo.  This is called by subclasses' doEdit() method.
	protected void rememberDocument(TLDocument doc){
		iDoc = doc;
	}
	
	
	//	The Swing base class for this class doesn't have a doEdit() method--it assumes that the
	//	operation has been done the first time by the same code that creates this object.  It
	//	seems cleaner to me to localize all implementations here, so I'm adding a doEdit() method.
	public void doEdit(TLDocument doc){
		throw new ImplementationException("This should be an abstract method");
	}
	
	
	//	Accessors.
	public Set getAffectedStates()		{		return iAffectedStates;			}
	
	public TLState getAffectedState(){
		Debug.assertOnError(iAffectedStates.size() == 1);
		return (TLState)iAffectedStates.iterator().next();
	}
}