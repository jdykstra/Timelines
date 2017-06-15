//	Tracker.java - Provide architecture for tracking direct-manipulation drags.

//	Subclasses of Tracker plug application-specific behavior into the architecture provided by this class.
//	Subclasses are provided with the information they need through method arguments,
//	so that they not need to access our instance variables.

//	This code is somewhat influenced by the design of the Tracker class in MacApp.

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public abstract class Tracker extends MouseInputAdapter {
	
	// 	Constants ----------------------------------------------------------------------
	protected static int DRAG_THRESHOLD = 10;		//	Distance from starting point needed to start a drag


	//	Instance variables ----------------------------------------------------------------
	protected Component iComponent;				//	Component within which dragging occurs
	protected CustomAction iAction;				//	Action performed by completion of drag.
	protected boolean iDragging;					//	A drag has begun
	protected Point iAnchorPosition;				//	Place where the mouse button went down
	protected Point iPhysicalOffset = new Point();	//	The mouse's current offset from the anchor
	protected Point iPreviousOffset = null;			//	Logical offset the last time drawFeedback() was called


	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	//	The contructor is expected to be called with the mouse button already down.
	//	anchor is the location of the mouse down event.
	//	component is the java.awt.Component within which the drag occurs.
	//	action is a CustomAction which can be performed (usually by the subclass' endDrag()) by calling
	//	performAction.
	public Tracker(Point anchor, Component component, CustomAction action){
		super();
		iAnchorPosition = anchor;
		iComponent = component;
		iAction = action;

		//	Register ourselves to get mouse button and motion events.
		iComponent.addMouseMotionListener(this);
		iComponent.addMouseListener(this);
	}
	
	
	//	Methods usually overridden by subclasses --------------------------------------------
	
	//	beginDrag() is called when the mouse starts moving with its button down.
	//	anchorPosition is the position (relative to Component origin) where the mouse button went down.
	//	physicalOffset is the distance between the anchor and the mouse's current position in the Component.
	protected void beginDrag(Point anchorPosition, Point physicalOffset){
	}
	
	
	//	constrain() is called each time a new mouse position is reported by AWT.
	//	The point it returns is used as the logical offset for subsequent subclass calls during this cycle.
	//	The default implementation of constrain() sets the logical offset to be the physical offset.
	protected Point constrain(Point anchorPosition, Point physicalOffset){
		return physicalOffset;
	}
	
	
	//	drawFeedback() is called after constrain().  It is where the subclass should draw visual feedback
	//	to user.
	//	previousOffset is the value of logicalOffset the last time that drawFeedback() was called.  The
	//	first time drawFeedback() is called, it is null.
	protected void drawFeedback(Point anchorPosition, Point logicalOffset, Point previousOffset, 
														Component component){
	}


	//	endDrag() is called when the mouse button is released.
	//	logicalOffset is the distance between the anchor and the mouse's current position, as modified by
	//	constrain().
	protected void endDrag(Point anchorPosition, Point logicalOffset, Component component){
	}
	
	
	//	Methods provided for use by subclasses -----------------------------------------------
	//	performAction() can be called by the subclass to cause the Action that was passed to the contructor
	//	(or SetAction()) to be executed.
	protected void performAction(){
		iAction.actionPerformed(null);
	}
	
	
	//	setAction() can be called by the subclass to specify a new Action.
	protected void setAction(CustomAction action){
		iAction = action;
	}
	
	
	//	Internal methods ------------------ --------------------------------------------

	//	Override of superclass's mouseDragged() method.
	public void mouseDragged(MouseEvent ev) {
		try {
			iPhysicalOffset.x = ev.getX() - iAnchorPosition.x;
			iPhysicalOffset.y = ev.getY() - iAnchorPosition.y;

			//	If we hadn't yet started a drag, see if one is allowed and we've exceeded the threshold.
			if (!iDragging){
				int distanceSquared = (iPhysicalOffset.x * iPhysicalOffset.x) + 
										(iPhysicalOffset.y * iPhysicalOffset.y);
				iDragging = distanceSquared > (DRAG_THRESHOLD * DRAG_THRESHOLD) &&
										iAction.isEnabled();
				
				//	If we just started a drag, tell our subclass.
				if (iDragging)
					beginDrag(iAnchorPosition, iPhysicalOffset);
					
			}
			
			//	Allow our subclass to constrain the logical offset of the mouse.
			Point logicalOffset = constrain(iAnchorPosition, iPhysicalOffset);
			
			//	Allow our subclass to draw visual feedback for the user, and the remember the 
			//	logical offset value for next time.  We copy each integer rather than just saving
			//	a reference to the Point, because the subclass may have passed us (as constrain()'s
			//	return value) a Point that it intends to reuse.
			drawFeedback(iAnchorPosition, logicalOffset, iPreviousOffset, iComponent);
			if (iPreviousOffset == null)
				iPreviousOffset = new Point();
			iPreviousOffset.x = logicalOffset.x;
			iPreviousOffset.y = logicalOffset.y;
		 }
		 catch (Throwable e){
		 	Application.reportUnexpectedException(e);
		 }
	}


	//	Override of superclass's mouseReleased() method.
	public void mouseReleased(MouseEvent ev) {
		try {
			//	Only do end-of-drag processing if we actually started a drag.
			if (iDragging){
				//	Allow our subclass to constrain the logical offset of the mouse.
				Point logicalOffset = constrain(iAnchorPosition, iPhysicalOffset);
				
				//	Tell our subclass that the drag has ended.
				endDrag(iAnchorPosition, logicalOffset, iComponent);
			}
			
	
			//	Remove the Action from the TLWindow's list of actions.
			if (iAction != null){
				ActionManager am = iAction.getActionManager();
				am.remove((CustomAction)iAction);
			}

			//	Remove ourselves from the Component's listener lists.  Since our client probably
			//	did not keep a reference to this object, it will be garbage-collected after we return
			//	and our "this" pointer goes out of scope.
			iComponent.removeMouseMotionListener(this);
			iComponent.removeMouseListener(this);
		 }
		 catch (Throwable e){
		 	Application.reportUnexpectedException(e);
		 }
	}
}

