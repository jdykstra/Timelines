//	AddDeleteStateEdit.java - Represent an edit in which one or more states are added to the document.

import java.util.*;
import java.util.List;
import javax.swing.undo.*;

//	??	This can be generalized to handle state deletion, too.
public class AddDeleteStateEdit extends TLUndoableEdit {

	// 	Constants ------------------------------------------------------------------------
	protected static final String kInsertPresentationName = "state insertion";
	protected static final String kDeletePresentationName = "state deletion";
	
	
	//	Instance variables ----------------------------------------------------------------
	protected boolean iDeleting;				//	True if we're deleting;  false if we're adding
	

	//	Constructor ---------------------------------------------------------------------
	public AddDeleteStateEdit(Set affectedStates, boolean deleting){
		super(affectedStates);
		iDeleting = deleting;
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
		if (iDeleting)
			iDoc.editRemoveStates(this, affectedStates);
		else
			iDoc.editAddStates(this, affectedStates);
	}
	
	
	//	undo() - Undo the edit.  Normally called by the EditManager.
	public void undo() throws CannotUndoException {
		super.undo();
		
		Set affectedStates = this.getAffectedStates();
		if (iDeleting)
			iDoc.editAddStates(this, affectedStates);
		else
			iDoc.editRemoveStates(this, affectedStates);
	}
	
	
	//	redo() - Redo the edit.  Normally called by the EditManager.
	public void redo() throws CannotRedoException {
		super.redo();
		
		this.doEdit(iDoc);
	}
	
	
	//	Describe this operation in a form that ends up in the undo menu item.
	public String getPresentationName() {
		return (iDeleting) ? kDeletePresentationName : kInsertPresentationName;
	}
}