//	ActionManager.java - Manage Action states.

//	ActionManagers provide an infrastructure for updating the enable status of commands as
//	the application and document's state changes.  They utilize and build upon the Action 
//	interface defined in Swing.
//
//	There is an ActionManager in each level of the object hierarchy (app, document, window),
//	which manages Actions whose enable states depend upon the state of the associated object.
//
//	The updateAllActionEnables() method should be called whenever the state of the associated
//	object changes.  It calls the updateEnable() method of each Action, causing them to recompute
//	their update state as determined by the associated object's state.
//
//	??	This can lead to a tradeoff between clear code and performance.  For clear code, it's good 
//	??	to call updateAllActionEnables() immediately after changing state.  However, this might
//	??	lead to updateAllActionEnables() being called more than once for a single user-visible
//	??	operation.  One solution is to add to
//	??	ActionManager a flag indicating that the enables need to be re-evaluated.  This flag gets
//	??	set at each place where state is changed.  A low-priority task periodically (or when it
//	??	get's waked up?) does updateAllActionEnables() when the flag is set, and clears it.
//	
//	Provisions have been made in case updating all Action enables gets to be a performance hog.
//	In this case, lists of Actions which depend upon particular aspects of the associated object's
//	state will be kept.  When a particular aspect of the state is changed, updateActionEnables(List)
//	will be called with the appropriate list.
//
//	Update requests are propagated down the hierarchy;  i.e., if the state of
//	the document changes, the window ActionManager(s) get the update request too.

import java.util.*;
import java.util.List;

public class  ActionManager extends Object {

	//	Constants.
	
	
	//	Instance variables.
	protected List iChildAMs = new ArrayList();		//	Our children
	protected List iAllActions = new ArrayList();		//	All Actions managed by this object


	//	Add an ActionManager to our list of children.
	public void addChild(ActionManager am){
		iChildAMs.add(am);
	}
	
	
	//	Remove an ActionManager from our list of children.
	public void removeChild(ActionManager am){
		iChildAMs.remove(am);
	}

	
	//	Add an action to the list of all actions.
	//	Returns the Action passed to it, to save lines in calling code.
	public CustomAction add(CustomAction action){
		Debug.assertOnError(iAllActions.add(action));
		return action;
	}
	
	
	//	Remove an action from the list of all actions.
	public void remove(CustomAction action){
		Debug.assertOnError(iAllActions.remove(action));
	}
	
	
	//	Update the enable states of all Actions managed by this object.
	public void updateAllActionEnables(){
	
		long startTime = System.currentTimeMillis();
		
		//	Update all Actions we know about.
		this.updateActionEnables(iAllActions);

		//	Update all Actions of our children.
		Iterator iter = iChildAMs.iterator();
		while (iter.hasNext()){
			ActionManager am = (ActionManager)iter.next();
			am.updateAllActionEnables();
		}
		
		long runTime = System.currentTimeMillis() - startTime;
		if (runTime > 10)
			System.err.println("updateAllActionEnables() took " + runTime +
							" milliseconds.");
	}
	
	
	//	Update the enable states of Actions from the provided list.
	//	??	Parameter should be a Set rather than a List.
	//	??	Currently, this does nothing with child ActionManagers.
	//	??	Adding support will wait until we understand what the need is.
	public void updateActionEnables(List actionList){
		Iterator iter = actionList.iterator();
		while (iter.hasNext()){
			CustomAction action = (CustomAction)iter.next();
			action.updateEnable();
		}
	}
}