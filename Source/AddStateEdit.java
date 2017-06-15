//	AddStateEdit.java - Represent an edit in which one or more states are added to the document.

import java.util.*;
import java.util.List;
import javax.swing.undo.*;

//	??	This can be generalized to handle state deletion, too.
public class AddStateEdit extends TLUndoableEdit {

	// 	Constants ------------------------------------------------------------------------
	protected static final String PRESENTATION_NAME = "State Insertion";
	
	
	//	Instance variables ----------------------------------------------------------------
	

	//	Constructor ---------------------------------------------------------------------
	public AddStateEdit(Set affectedStates){
		super(affectedStates);
	}
		
	
	//	doEdit() - Do the edit.  Normally called by the EditManager.
	public void doEdit(TLDocument doc){

		rememberDocument(doc);
		
		//	Set the create and modify timestamps on the states.
		Set affectedStates = this.getAffectedStates();
		Iterator iter = affectedStates.iterator();
		long now = System.currentTimeMillis();
		while (iter.hasNext()){
			TLState state = (TLState)iter.next();
			state.setCreateTime(now);
			state.setModifyTime(now);
		}
	
		//	Ask the document to add these states.
		iDoc.editAddStates(this, affectedStates);
	}
	
	
	//	undo() - Undo the edit.  Normally called by the EditManager.
	public void undo() throws CannotUndoException {
		super.undo();
		
		Set affectedStates = this.getAffectedStates();
		iDoc.editRemoveStates(this, affectedStates);
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