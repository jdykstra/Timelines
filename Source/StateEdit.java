//	StateEdit.java - Represent an edit in which one or more parameters of a TLState are changed.

import java.util.*;
import java.util.List;
import javax.swing.undo.*;

public class StateEdit extends TLUndoableEdit {

	// 	Constants ------------------------------------------------------------------------
	protected static final String PRESENTATION_NAME = "state modification";
	
	
	//	Instance variables ----------------------------------------------------------------
	//	The TLStates referenced in iOldValue and INewValue are to be used for their
	//	values only.  Neither should be linked into the document!
	protected TLState iOldValue;			//	Values before edit
	protected TLState iNewValue;		//	Values after edit
	
	
	//	Constructor ---------------------------------------------------------------------
	public StateEdit(TLState affectedState, TLState newValue){
		super(affectedState);
		
		//	Save the old value of this state in a new object.
		//	Save the new value in the object that our called passed to us.
		iOldValue =new TLState(affectedState);
		iNewValue = newValue;
	}
	
	
	//	doEdit() - Do the edit.  Normally called by the EditManager.
	public void doEdit(TLDocument doc){
		rememberDocument(doc);
		TLState affectedState = this.getAffectedState();
		affectedState.setModifyTime(System.currentTimeMillis());
		iDoc.editChangeState(this, affectedState, iNewValue);
	}
	
	
	//	undo() - Undo the edit.  Normally called by the EditManager.
	public void undo() throws CannotUndoException {
		super.undo();
		
		TLState affectedState = this.getAffectedState();
		iDoc.editChangeState(this, affectedState, iOldValue);
	}
	
	
	//	redo() - Redo the edit.  Normally called by the EditManager.
	public void redo() throws CannotRedoException {
		super.redo();
		
		this.doEdit(iDoc);
	}
	
	
	//	Describe this operation in a form that ends up in the undo menu item.
	public String getPresentationName() {
		return PRESENTATION_NAME;
	}
}