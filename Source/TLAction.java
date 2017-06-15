//	TLAction.java - A CustomAction that understands the app/document/window object hiearchy.

import javax.swing.Icon;

public abstract class TLAction extends CustomAction {

	//	Instance variables.

	
	//	Constructors.
	//	The different constructors are provided so that each subclass definition can clearly
	//	specify what level in the hiearchy provides its ActionManager.
	//	??	If all ActionManagers do is control enables, maybe the Actions who are
	//	??	always enabled shouldn't be associated with an ActionManager at all.  This
	//	??	would require another constructor with just the String parameter.
	//	??	Or maybe they should just be instances of AbstractAction.
	public TLAction(String name, Application app){
		super(name, app.getActionManager());
	}
	
	public TLAction(String name,  Icon icon, Application app){
		super(name, icon, app.getActionManager());
	}
	
	public TLAction(String name, TLDocument doc){
		super(name, doc.getActionManager());
	}
	
	public TLAction(String name, Icon icon, TLDocument doc){
		super(name, icon, doc.getActionManager());
	}
	
	public TLAction(String name, TLWindow win){
		super(name, win.getActionManager());
	}

	public TLAction(String name, Icon icon, TLWindow win){
		super(name, icon, win.getActionManager());
	}

	public TLAction(Application app){
		super(null, app.getActionManager());
	}
	
	public TLAction(TLDocument doc){
		super(null, doc.getActionManager());
	}
	
	public TLAction(TLWindow win){
		super(null, win.getActionManager());
	}

}