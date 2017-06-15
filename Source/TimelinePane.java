//	TimelinePane.java - Graphically display a timeline.

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;


import java.awt.event.*;

public class TimelinePane extends JComponent implements ChangeListener {

	// 	Constants------------------------------------------------------------------------



	//	Instance variables----------------------------------------------------------------
	protected TLDocument iDoc;					//	Document containing our data
	protected TLWindow iWindow;				//	The window that we're a part of
	protected TimePositionMapping iTPM;			//	Maps time to space on this pane
	protected Placer iPlacer;					//	Arranges the contents of this pane
	
	protected Map iDisplayMap;					//	Maps all DisplayedStates to their TLStates
	

	//	Constructor---------------------------------------------------------------------
	public TimelinePane(TLDocument itsDoc, TLWindow window, TimePositionMapping tpm){
		iDoc = itsDoc;
		iWindow = window;
		iTPM = tpm;
		
		//	Initialize this object.
		iDisplayMap = new HashMap();
		iPlacer = new Placer();
		
		//	Set up our relationships with other objects.
		iTPM.addChangeListener(this);
		iDoc.addChangeListener(this); 

		//	Initialize our properties as a Swing component.
		//	??	I think that the alignment values are only needed because of a bug
		//	??	in SizeRequirements.calculateAlignedPositions() in Swing 1.0.3.  
		this.setFont(DisplayedState.LABEL_FONT);
		this.setAlignmentX(LEFT_ALIGNMENT);
		this.setAlignmentY(TOP_ALIGNMENT);
		
		//	??	Temporary dummies...
		this.addToDisplayMap(iDoc.getStatesByStartList());
		this.computeObjectDimensions();
		iPlacer.assignToLevel(iDisplayMap.keySet());
		
		//	Compute the physical size of this pane.  
		//	This has to happen AFTER we create the first display map.
		this.updatePaneSize();
	}
	
	
    //	??	Following borrowed from SystemEventQueueUtilities for bug workaround in paintComponent() below.
    private static class SystemEventQueue 
    {
        private static Toolkit tk = null;

	// Return the AWT system event queue.  JDK1.2 applications 
	// and PlugIn enabled browsers allow direct access to the 
	// applet specific system event queue.
	//
	static EventQueue get() {
            if (tk == null) {
	        tk = Toolkit.getDefaultToolkit();
	    }
	    return tk.getSystemEventQueue();
	}

	static EventQueue get(JRootPane rootPane) {
	    return rootPane.getToolkit().getSystemEventQueue();
	}
    }


	//	Override of Swing standard method paintComponent().
	protected void paintComponent(Graphics g) {
		
		//	??	Workaround for Swing bug.  When the mouse button is held down in a scrollbar arrow,
		//	??	the scrollbar's value is updated every 100 mS.  If it takes longer than 100 mS to draw the
		//	??	scrolled view, the Timer events back up in the system event queue, and prevent the
		//	??	mouse-up from being recognized.  
		//	??	As a workaround, peek into the system event queue, and see if there is another scrollbar 
		//	??	update event in the queue.  If there is, just return without drawing.
		//	??	The event ID is the one used by RunnableEvent.
		//	??	THIS IS CURRENTLY DISABLED, BECAUSE IMPROVEMENTS IN DRAW SPEED MADE
		//	??	IT LESS NEEDED, AND BECAUSE THE REFRESH PROBLEMS IT CAUSES ARE
		//	??	INTRUSIVE.
		AWTEvent evt = SystemEventQueue.get().peekEvent(AWTEvent.RESERVED_ID_MAX + 1000);
		if (false && evt != null)
			return;
		
		long startTime = System.currentTimeMillis();
		this.drawDataObjects(g, iDisplayMap.keySet());
		if (Debug.DISPLAY_TIMINGS)
			System.out.println((System.currentTimeMillis() - startTime) + " mS. to paint TimelinePane");
	 }
	 
	 
	 //	Draw all data objects in a Collection.
	 protected void drawDataObjects(Graphics g, Collection c){
	 
	 	//	Iterate through all objects, and draw each one.
	        Rectangle clipRect = g.getClipBounds();	        
	 	Iterator iter = c.iterator();	 	while (iter.hasNext()){
	 		DisplayedState dObj = (DisplayedState)iter.next();
	        		dObj.draw(g, clipRect);
	 	}
	 }
	 
	 
	//	Request a repaint of all objects in a Collection.
	protected void repaintDataObjects(Collection c){

		//	Request a repaint of each object.
		Iterator iter = c.iterator();
		while (iter.hasNext()){
			DisplayedState o = (DisplayedState)iter.next();
			this.repaint(o.getXLocation(), o.getYLocation(), o.getWidth(), o.getHeight());
		}
	}
	 
	 
	 //	Create DisplayObjects for the provided DataObj's, and add them to the display list.
	 protected void addToDisplayMap(Collection dataObjectCollection){
	
		//	Once around for each element in the input list.
		Iterator iter = dataObjectCollection.iterator();
		while (iter.hasNext()){
			TLState state = (TLState) iter.next();

			//	See whether this state is included in the shown categories.
			//	If not, go on to the next one.
			if (! iWindow.isShown(state))
				continue;
			
			//	Create a DisplayedObject for this data object, and add it to the display list.
			DisplayedState dObj = new DisplayedState(state);
			iDisplayMap.put(dObj, state);
		}
	 }
	 
	 
	 //	Remove the specified data objects and their associated display objects from our map.
	 protected void removeFromDisplayMap(Collection dataObjectCollection){
	 	Iterator iter = dataObjectCollection.iterator();
	 	while (iter.hasNext()){
	 		DisplayedState dObj = getDisplayedStateFromState((TLState)iter.next());
	 		Debug.assert(iDisplayMap.remove(dObj) != null);
	 	}
	 }
	 
	 
	 //	Returns the displayed state representing the provided state.
	 protected DisplayedState getDisplayedStateFromState(TLState state){
	 
	 	//	This method is currently engineered under the assumption that this is a rare
	 	//	function.  Thus, we use a lot of memory and CPU time.  If this becomes something
	 	//	that is done frequently, we should accept the memory penalty of using iDisplayMap 
	 	//	to hold a bi-directional mapping.
	 	Set entrySet = iDisplayMap.entrySet();
	 	Iterator iter = entrySet.iterator();
	 	while (iter.hasNext()){
	 		Map.Entry entry = (Map.Entry)iter.next();
	 		if (entry.getValue().equals(state))
	 			return (DisplayedState) entry.getKey();
	 	}
	 	Debug.assert(false);					//	Could not find display object
	 	return null;
	 }
	
	
	//	Compute the dimensions of all objects in the display list.  This also sets their X position
	protected void computeObjectDimensions(){

		//	Once around for each element in the display list.
		Iterator displayListIter = iDisplayMap.keySet().iterator();
		while (displayListIter.hasNext()){
			
			//	Get the next DisplayedObject, and tell it to update its dimensions..
			DisplayedState dObj = (DisplayedState) displayListIter.next();
			dObj.calculateDimensions(iTPM);
		}
	}
	
	
	 //	Identify which DisplayedState contains the given point.  Return null if no object at that point.
	 public DisplayedState pointToDisplayedObject(Point p){
	 	return iPlacer.pointToDisplayedObject(p);
	 }
	 
	 
	//	Tell Swing what the current size of this pane is.
	protected void updatePaneSize(){
	
		//	Update the prefered and maximum size we report to our layout manager.
		//	??	Setting the maximum height every time is wasteful.
		int paneHeight = iPlacer.getMaximumYUsed();
		int paneWidth = iTPM.getTimelineWidth();
		this.setPreferredSize(new Dimension(paneWidth, paneHeight));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		//	Ask the layout manager to update our size.
		this.revalidate();
	}
	
	
	//	Implement the stateChanged() method of the ChangeListener interface.
	//	This is called when the document or the TPM changes.
	public void stateChanged(ChangeEvent e){
		Object source = e.getSource();
		if (source == iDoc)
			this.docStateChanged(e);
		else if (source == iTPM)
			this.tpmStateChanged(e);
		else
			throw new ImplementationException("Received stateChanged event from unexpected source - " +
					source.toString());
	}
	
	
	//	Handle ChangeEvent's from the document.
	//	The current approach is to rebuild everything, regardless of what changed.
	protected void docStateChanged(ChangeEvent ev){

		//	If the edit was adding or removing states, update our display map.
		if (ev instanceof StateAddDeleteChange){
			StateAddDeleteChange event = (StateAddDeleteChange)ev;
			Set affectedStates = event.getAffectedStates();
			if (event.isDeleting())
				this.removeFromDisplayMap(affectedStates);
			else
				this.addToDisplayMap(affectedStates);
		}
			
		//	Iterate through all DisplayedObjects in the display map, telling them to recalculate their dimensions.
		this.computeObjectDimensions();

		//	Assign all displayed objects to levels.
		iPlacer.forgetLevelAssignments();
		iPlacer.assignToLevel(iDisplayMap.keySet());
	
		//	Tell Swing our new pane size.
		this.updatePaneSize();
		
		//	Force a repaint of the window.
		//	??	Why isn't this needed for the TPM state change?
		this.repaint();
	}


	//	Called when the state of the TPM changes.  This is usually a scale change.
	//	??	This also happens when the window's time period changes.  In this case, the
	//	??	only thing we need to do is update the pane size, and the rest just gets in the
	//	??	user's way.  Is the distraction bad enough to justify changing this?
	protected void tpmStateChanged(ChangeEvent e){

		//	Iterate through all levels, telling all DisplayedObjects to recalculate their dimensions.
		this.computeObjectDimensions();

		//	Assign all displayed objects to levels.
		iPlacer.forgetLevelAssignments();
		iPlacer.assignToLevel(iDisplayMap.keySet());
	
		//	Tell Swing our new pane size.
		this.updatePaneSize();
	}
	
	
	//	Called by TLWindow when the set of shown categories changes.
	public void shownCategoriesChanged(){
	
		//	Rebuild the display
		iDisplayMap.clear();
		this.addToDisplayMap(iDoc.getStatesByStartList());
		this.computeObjectDimensions();
		iPlacer.forgetLevelAssignments();
		iPlacer.assignToLevel(iDisplayMap.keySet());
		this.updatePaneSize();
	
		//	Force a repaint of the window.
		this.repaint();
	}
	
}