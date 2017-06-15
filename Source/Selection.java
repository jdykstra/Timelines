//	Selection.java - Represent a selection containing one or more display objects.

//	Selection is a Set which stores the DisplayState's that are currently selected by the user.
//	To the standard Set behavior, it adds:
//		*  Updates the state of DisplayStates and requests a repaint of the relevant parts of the
//			timeline when objects are added to or removed from the selection.
//		*  Updates the offset state of the DisplayStates in the selection as it is dragged around
//			the screen.
//		*  Implements PropertyChangeListener so it can be notified in changes of the enable state of
//			DragAction defined in DragPane, and change the displayed drag handles accordingly.

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.List;


public class Selection extends HashSet implements PropertyChangeListener {

	// 	Constants ------------------------------------------------------------------------


	//	Instance variables ----------------------------------------------------------------
	protected TimelinePane iTLPane;			//	Reference to pane containing this selection.
	

	//	Trivial accessors -------------------------------------------------------------


	//	Constructor ---------------------------------------------------------------------
	public Selection(TimelinePane tlPane){
		super();
		iTLPane = tlPane;
	}
	
	
	//	Nested class defining a closure which can be subclassed and executed for all
	//	members of the selection.
	abstract class ForAllMemberClosure extends Object {
		abstract void doForEachMember(DisplayedState obj);
	}
	
	
	//	Method for use with subclasses of ForAllMemberClosure, above.
	protected void doForAllMembers(ForAllMemberClosure fam){
		Iterator iter = this.iterator();
		while (iter.hasNext()){
			DisplayedState obj = (DisplayedState)iter.next();
			fam.doForEachMember(obj);
		}
	}

	
	//	Adds the specified object to this Selection if it is not already present.
	//	return true if the Selection did not already contain the specified object.
	public boolean add(DisplayedState o) {
		o.setSelected();
		boolean retval = super.add(o);
		this.repaint();
		return retval;
	}
	
	
	//	Removes the given DataObj from this Selection if it is present.
	//	Returns true if the HashSet contained the specified element.
	public boolean remove(DisplayedState obj) {
		this.repaint();
		obj.clearSelected();
		obj.showDragHandles(false);
		return super.remove(obj);
	}
	
	
	//	Removes all DataObj's in a Set from this Selection if they are present.
	//	??	This is ugly and inefficient.  Maybe some rethinking is necessary.
	public void removeStates(Set states) {
		this.repaint();
		
		Iterator iter = this.iterator();
		while (iter.hasNext()){
			DisplayedState dobj = (DisplayedState)iter.next();
			if (states.contains(dobj.getState())){
				dobj.clearSelected();
				dobj.showDragHandles(false);
				iter.remove();
			}
		}
	}
	
	
	//	Empty out the selection.
	public void clear(){
		this.repaint();

		this.doForAllMembers(new ForAllMemberClosure(){
			void doForEachMember(DisplayedState obj){
				obj.clearSelected();	
				obj.showDragHandles(false);
			}
		});
		
		super.clear();
	}
	
	
	//	Report whether some object in this Selection is locked.
	public boolean isLocked(){
		Iterator iter = this.iterator();
		while (iter.hasNext()){
			DisplayedState obj = (DisplayedState)iter.next();
			if (obj.getState().getLabelInfo().isLocked()){
				return true;
			}
		}
		
		return false;
	}
	
	
	//	Set all objects in the selection to the given location offset.
	public void setOffset(final int[] x, final int y){
		this.repaint();

		this.doForAllMembers(new ForAllMemberClosure(){
			void doForEachMember(DisplayedState obj){
				obj.setOffset(x, y);
			}
		});

		this.repaint();
	}
	
	
	//	Request a repaint of all data objects in the selection.
	//	??	Note that many of the calls to this method actually only need a repaint of one displayed object.
	//	??	At the moment, requesting a repaint of all of them doesn't seem too bad, but it might be a
	//	??	problem for really big selections.
	//	??	Also at the moment, we're frequently calling this _before_ changing the selection, so
	//	??	repaint requests get made for every state that _used_ to be in the selection.  I think
	//	??	this has a race condition;  what if the repaint actually starts (on another thread) before
	//	??	we get around to modifying the selection state appropriately?
	public void repaint(){
		iTLPane.repaintDataObjects(this);
	}
	
	
	
	//	Internal class implementing TimePeriod, for use by getTimePeriod() below.
	class minMaxTimePeriod extends TimePeriod {

		protected long iStart = Long.MAX_VALUE;			//	Millis of starting moment
		protected long iEnd = Long.MIN_VALUE;				//	Millis of ending moment

		public long getPeriodStart() 			{ return iStart;}
		public long getPeriodEnd()				{ return iEnd;}
		public void setPeriodStart(long m)		{ iStart = m;}
		public void setPeriodEnd(long m)			{ iEnd = m;}
	}
	
	
	//	Return the TimePeriod which encloses all states in the selection.
	//	Throws an exception if the selection is empty.
	public TimePeriod getTimePeriod(){
		if (this.size() == 0)
			throw new ImplementationException("Selection is empty");
		
		final TimePeriod range = new minMaxTimePeriod();
		
		this.doForAllMembers(new ForAllMemberClosure(){
			void doForEachMember(DisplayedState obj){
				TLState state = obj.getState();
				long thisObjStart = state.getPeriodStart();
				if (thisObjStart < range.getPeriodStart())
					range.setPeriodStart(thisObjStart);
					
				long thisObjEnd = state.getPeriodEnd();
				if (thisObjEnd > range.getPeriodEnd())
					range.setPeriodEnd(thisObjEnd);
			}
		});
		
		return range;
	}


	//	Remove from the selection any states that are not part of the currently shown categories.
	public void removeUnshownCategories(final Set shownCategories){
		
		//	Iterate through all DisplayedStates in the selection.
		this.doForAllMembers(new ForAllMemberClosure(){
			void doForEachMember(DisplayedState obj){
				TLState state = obj.getState();
				
				//	Iterate through the Categories that this state is a memeber of.
				//	As soon as we find one that is part of the shown set, we go on
				//	to the next state.
				Iterator iter = state.getCategories().getAsSet().iterator();
				while (iter.hasNext()){
					if (shownCategories.contains((Category)iter.next()))
						return;
				}
				
				//	Since this (displayed) state is not a member of any shown cateogry, remove it
				//	from the selection.
				Selection.this.remove(obj);
			}
		});
	}
	
	
	//	Return a Set containing the TLStates corresponding to the selection.
	//	??	Was it really a good idea for Selection to store DisplayObjects rather than DataObjects?
	//	??	Is the reasoning that a selection is per-window, and DisplayObjects are, too?
	public Set getStates(){
		final Set stateSet = new HashSet();

		//	Iterate through all DisplayedStates in the selection.
		this.doForAllMembers(new ForAllMemberClosure(){
			void doForEachMember(DisplayedState obj){
				TLState state = obj.getState();
				stateSet.add(state);
			}
		});
		
		return stateSet;
	}
	
	
	//	This property change listener is called when SelectTracker.SelectDragAction changes
	//	its enabled state.  
	public void propertyChange(PropertyChangeEvent e) {
		
		//	The only property we're interested in is "enabled".
		String propertyName = e.getPropertyName();
		if (propertyName.equals("enabled")) {
			System.err.println("propertyChange() called on " + toString());///////////
			final boolean isEnabled = ((Boolean) e.getNewValue()).booleanValue();
			
			//	Update the drag handles on the selected objects according to the new
			//	enable state.
			this.doForAllMembers(new ForAllMemberClosure(){
				void doForEachMember(DisplayedState obj){
					obj.showDragHandles(isEnabled);
				}
			});
		} 
		
		//	Request a repaint of the states in their new form.
		this.repaint();
	}
	
	
	//	Override of Object.toString();
	public String toString(){
		return "Selection containing " + super.toString();
	}
}