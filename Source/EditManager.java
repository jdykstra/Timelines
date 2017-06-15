//	EditManager.java - Add functionality to Swing UndoManager class.

//	EditManager is the central coordinator for edits to the document.  Its principal responsibility
//	is to keep track of undoable edits, and provide the Undo and Redo commands.  Therefore, it
//	extends javax.swing.undo.UndoManager.  Behavior it adds to the Swing component is the ability
//	to update the state of menu items (including their labels) as the undo state changes.  Note
//	that the same sort of functionality has been suggested to Sun  in bug # 4141524.

//	See TLUndoableEdit for an overview of the editing mechanism.

import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.*;

public class EditManager extends UndoManager {

	//	Instance variables ----------------------------------------------------------------
	protected TLDocument iDoc;
	

	//	Trivial accessors -------------------------------------------------------------


	//	Constructor ---------------------------------------------------------------------
	public EditManager(TLDocument doc){
		iDoc = doc;
		
		//	Initialize the state of the Actions.
		this.updateUIState();
	}
	
	
	//	Called by other methods whenever the user interface state needs to be updated.
	protected void updateUIState(){
		//	??	Is it worth enhancing this implementation so that
		//	??	the Actions only get changed when our state really changes?
		iUndoAction.setEnabled(this.canUndo());
		iUndoAction.putValue(Action.NAME, this.getUndoPresentationName());
		iRedoAction.setEnabled(this.canRedo());
		iRedoAction.putValue(Action.NAME, this.getRedoPresentationName());
	}
	
	
	//	Execute a new edit just commanded by the user.
	public void executeEdit(TLUndoableEdit e) {
		
		//	Tell the Edit to execute.
		e.doEdit(iDoc);
	
		//	Increment the document's edit count.
		iDoc.incrementEditCount();

		//	Have our superclass add this edit to its list of undoable edits.
		super.addEdit(e);
		
		//	Update the enable state of the Undo and Redo commands.
		this.updateUIState();
	}


	//	Override undo()  to update the user interface.
	public void undo() throws CannotUndoException {
		
		//	Ask our superclass to call the edit's undo() method.
		super.undo();

		//	Decrement the document's edit count.
		iDoc.decrementEditCount();

		//	Update the enable state of the Undo and Redo commands.
		this.updateUIState();
	}


	//	Override redo()  to update the user interface.
	public void redo() throws CannotUndoException {

		//	Ask our superclass to call the edit's redo() method.
		super.redo();

		//	Increment the document's edit count.
		iDoc.incrementEditCount();

		//	Update the enable state of the Undo and Redo commands.
		this.updateUIState();
	}


	//	Command Actions ------------------------------------------------------------
	//	Undo and Redo are AbstractActions rather than TLActions because they do not use the ActionManager
	//	mechanism for enables.  Instead, their state is controlled by the EditManager.
	public Action iUndoAction = new AbstractAction("Undo")  {
	
		public void actionPerformed(ActionEvent ae) {			
			undo();
		}
	};


	//	Undo and Redo are AbstractActions rather than TLActions because they do not use the ActionManager
	//	mechanism for enables.  Instead, their state is controlled by the EditManager.
	public Action iRedoAction = new AbstractAction("Redo")  {
	
		public void actionPerformed(ActionEvent ae) {			
			redo();
		}
	};

}