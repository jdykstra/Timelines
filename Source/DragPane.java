//	DragPane.java - Implement direct manipulation commands for the timeline display.

//	DragPane overlays the TimelinePane in the main timeline display area, so that it gets mouse clicks first.
//	It interprets those mouse clicks and mouse motion events into direct-manipulation commands.

//	??	Since the select tool and the create tool use (in general) different variables to
//	??	hold their state, it would be nice to somehow segrate the code into separate
//	??	classes.  What is a good way to do this?

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

public class DragPane extends JComponent implements ChangeListener {

	// 	Constants ------------------------------------------------------------------------
	
	//	Instance variables ----------------------------------------------------------------
	//	Significant objects we have permanent relationships with.
	protected TLDocument iDoc;
	protected TimePositionMapping iTPM;
	protected TLWindow iWin;					//	The window we're associated with
	protected TimelinePane iTLPane;
		
	protected Selection iSelection;				//	Display objects in current selection.


	//	Trivial accessors -------------------------------------------------------------
	public Selection getSelection()		{	return iSelection;	}


	//	Constructor ---------------------------------------------------------------------
	public DragPane(TLDocument itsDoc, TLWindow win, TimePositionMapping tpm, TimelinePane tlPane){
		iDoc = itsDoc;
		iWin = win;
		iTPM = tpm;
		iTLPane = tlPane;
		iSelection = new Selection(iTLPane);

		//	Set up our relationships with other object.
		iTPM.addChangeListener(this);

		//	Initialize our properties as a Swing component.
		this.setFont(DisplayedState.LABEL_FONT);
		this.updatePaneSize();

		//	Since we're not registering ourself as a mouse input listener, but
		//	simply overriding processMouseEvent(), we need to explicitly ask that
		//	we get mouse events.
		this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
	}
	
	
	//	Override of Swing standard method paintComponent().
	protected void paintComponent(Graphics g) {
		try {
			//	Draw the objects in the selection, so they'll appear on top of unselected objects.
			//	Note that the objects in the selection will also be drawn by TimelinePane  ("underneath"
			//	what we draw).  This is slightly inelegant, but doesn't appear to need fixing.
			//	??	It might be better for the code that knows how to draw objects to be in a common
			//	??	abstract superclass of both TimelinePane and DragPane.  It might then be possible
			//	??	for DragPane to not reference TimelinePane at all.
			long startTime = System.currentTimeMillis();
		 	iTLPane.drawDataObjects(g, iSelection);
			if (Debug.DISPLAY_TIMINGS)
				System.out.println((System.currentTimeMillis() - startTime) + " mS. to draw DragPane");
		 }
		 catch (Throwable e){
		 	Application.reportUnexpectedException(e);
		 }
	 }
	 

	//	Override of Swing processMouseEvent().
	protected void processMouseEvent(MouseEvent ev){
		try {
			switch (iWin.getCurrentCursorTool()){
				case TLWindow.SELECT_TOOL:
					processSelectToolMouseEvent(ev);
					break;
				case TLWindow.CREATE_TOOL:
					processCreateToolMouseEvent(ev);
					break;
			}
		 }
		 catch (Throwable e){
		 	Application.reportUnexpectedException(e);
		 }
  		super.processMouseEvent(ev);
    }
      
      
	//	Process a mouse event when the select tool is active.
	protected void processSelectToolMouseEvent(MouseEvent ev){
		if (ev.getID() == ev.MOUSE_PRESSED) {
		
			//	See if the mousedown was inside a displayed state.
			DisplayedState o = iTLPane.pointToDisplayedObject(
										new Point(ev.getX(), ev.getY()));
			if (o != null){
	
				//	Update the selection, and ask the action manager to update all Action enables.
				//	We need to call updateAllActionEnables() immediately after clearing the selection,
				//	to handle the case of a mousedown in an object that is already selected.  Clearing the
				//	selection will erase the drag handles, and we need to ensure that the action enables
				//	are kept in step.
				if (!ev.isShiftDown()){
					iSelection.clear();
					iWin.getActionManager().updateAllActionEnables();
				}
				if (ev.isShiftDown() && iSelection.contains(o))
				  iSelection.remove(o);
				else
				  iSelection.add(o);
				iWin.getActionManager().updateAllActionEnables();
				
				//	Determine which body part this mousedown was in.
				int bodyPart = o.inBodyPart(ev.getX(), ev.getY());

				//	If the document can be modified,
				//	create a Tracker to monitor the mouse and process any
				//	ensuing drag operation.  Note that we don't need to keep a
				//	reference to the Tracker--it just goes off and does its thing.
				//	The Tracker goes out of existance when the mouse button
				//	is released.
				if (!iDoc.isContentLocked())
					new SelectTracker(iDoc, iTPM, iWin, ev, bodyPart, this, iSelection);
			}
			else {
				//	Since the mouse-down was not in any state, clear the selection
				//	(unless the shift key is down).
				if (!ev.isShiftDown()){
					iSelection.clear();
					iWin.getActionManager().updateAllActionEnables();
				}
			}
		}
		else if (ev.getID() == ev.MOUSE_CLICKED){
		
			//	See if this is a double-click.
			if (ev.getClickCount() == 2){
				
				//	If the selection contains a single state, do a GetInfo command.
				if (iSelection.size() == 1)
					iWin.iGetInfoAction.actionPerformed(null);
			}
		}
	}

      
	//	Process a mouse event when the create tool is active.
	protected void processCreateToolMouseEvent(MouseEvent ev){
		//	Create a Tracker to monitor the mouse and process any
		//	ensuing drag operation.  Note that we don't need to keep a
		//	reference to the Tracker--it just goes off and does its thing.
		//	The Tracker goes out of existance when the mouse button
		//	is released.
		//	We don't need to check whether the document's content is locked,
		//	because the create tool is never enabled for a locked document.
		if (ev.getID() == ev.MOUSE_PRESSED)
			new CreateTracker(iDoc, iTPM, iWin, ev, 0, this, iSelection);
	}

      
	//	Implement the stateChanged() method of the ChangeListener interface.
	//	This is called when the TPM changes.
	public void stateChanged(ChangeEvent e){
		Object source = e.getSource();
		 if (source == iTPM)
			this.tpmStateChanged(e);
		else
			throw new ImplementationException("Received stateChanged event from unexpected source - " +
					source.toString());
	}
	
	
	//	Called when the scale of this window is changed.
	public void tpmStateChanged(ChangeEvent e){
	
		//	Tell Swing our new pane size.
		this.updatePaneSize();
	}


	//	Tell Swing what the current size of this pane is.
	protected void updatePaneSize(){
		
		//	Update the prefered and maximum size we report to our layout manager.
		//	It doesn't matter what height we specify,
		//	because OverlayLayout takes the biggest of the overlayed panes, so the value specified by
		//	TimelinePane overrides the value we specify.
		int paneWidth = iTPM.getTimelineWidth();
		this.setPreferredSize(new Dimension(paneWidth, 0));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		//	Ask the layout manager to update our size.
		this.revalidate();
	}
		
	
	

}