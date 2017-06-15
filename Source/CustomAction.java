//	CustomAction.java - Abstract base class for all actions in Timelines.

//	CustomAction extends the AbstractAction class from Swing with hooks into the ActionManager
//	infrastructure.  See that class for more description.
//	Most instances of CustomAction will be anonymous subclasses.  The enclosing class should be
//	the one whose instance variables the CustomAction instance needs access to.

import java.awt.event.*;
import javax.swing.AbstractAction;
import javax.swing.Icon;

public abstract class CustomAction extends AbstractAction {

	//	Instance variables.
	ActionManager iManagingAM;				//	ActionManager managing us
	
	
	//	Trivial accessors.
	public ActionManager getActionManager()		{	return iManagingAM;		}
	
	
	//	Constructors.
	//	??	I no longer think that it is a good idea for this constructor to add the Action to the
	//	??	ActionManager without the original creator of the Action knowing about it.
	public CustomAction(String name, ActionManager am){
		super(name);
		iManagingAM = am;
		iManagingAM.add(this);
	}
	
	//	??	I no longer think that it is a good idea for this constructor to add the Action to the
	//	??	ActionManager without the original creator of the Action knowing about it.
	public CustomAction(String name, Icon icon, ActionManager am){
		super(name, icon);
		iManagingAM = am;
		iManagingAM.add(this);
	}
	
	//	Most instances of CustomAction are created by initializers, which means they can't peek
	//	into parts of their enclosing object's state which are set up by the object's constructor.
	//	initializer() is a hook to handle this situation--it should be called by the constructor
	//	of any object that needs this behavior.
	//	The default implementation does nothing.
	public void initialize(){
	}

	
	//	Called by the action manager when it wants the Action to update its enable state.
	//	Most of the work is done by the subclasses' override of this method.
	//	The version provided in this base class always enables the Action.
	public void updateEnable(){
		this.setEnabled(true);
	}
	
	
	//	Called when the action should be performed.
	//	Most of the work is done by the subclasses' override of this method.
	//	Those overrides should first call super.actionPerformed() to take
	//	advantage of the sanity check provided here.
	public void actionPerformed(ActionEvent e){
		if (!this.isEnabled())
			throw new ImplementationException("Action performed when not enabled");
	}
}