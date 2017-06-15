//	CustomAbstractDocument.java - Basic implementation for Swing app framework.
//	Code based on AbstractDocument from Swing 1.0.3.


import javax.swing.event.*;
import javax.swing.undo.*;


public abstract class CustomAbstractDocument extends Object {

	//	Instance variables.
	protected transient EventListenerList listenerList = new EventListenerList();


	//	Constructor.
	protected CustomAbstractDocument() {
		//	Nothing needed here.
	}


	//	Add and remove methods for UndoableEditListeners.
	//	This follows the design pattern specified in swing.event.EventListenerList.
	public void addUndoableEditListener(UndoableEditListener listener) {
		listenerList.add(UndoableEditListener.class, listener);
	}

	public void removeUndoableEditListener(UndoableEditListener listener) {
		listenerList.remove(UndoableEditListener.class, listener);
	}


	//	Fire method for UndoableEditEvents.
	//	This follows the design pattern specified in swing.event.EventListenerList.
	protected void fireUndoableEditUpdate(UndoableEdit edit) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		UndoableEditEvent e = null;
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==UndoableEditListener.class) {
				// Lazily create the event:
				if (e == null)
					e = new UndoableEditEvent(this, edit);
				((UndoableEditListener)listeners[i+1]).undoableEditHappened(e);
			}	       
		}
	}


	//	Add and remove methods for ChangeListeners.
	//	This follows the design pattern specified in swing.event.EventListenerList.
	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}


	//	Fire method for ChangeEvents.
	//	This more or less follows the design pattern specified in swing.event.EventListenerList.
	protected void fireChangeUpdate(ChangeEvent ev) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==ChangeListener.class) {
				((ChangeListener)listeners[i+1]).stateChanged(ev);
			}	       
		}
	}


}






	
