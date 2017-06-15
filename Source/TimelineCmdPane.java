//	TimelineCmdPane.java - A timeline display that also implements commands.

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class TimelineCmdPane extends TimelinePane {

	// 	Constants------------------------------------------------------------------------


	//	Instance variables----------------------------------------------------------------
	protected Selection iSelection;				//	Data objects in current selection.
	protected boolean iMouseIsDown = false;
	

	//	Constructor---------------------------------------------------------------------
	public TimelineCmdPane(TLDocument itsDoc, TimePositionMapping tpm){
		super(itsDoc, tpm);
		
		iSelection = new Selection(this);
		enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
	}
	
	
      //	Override of Swing processMouseEvent().
      protected void processMouseEvent(MouseEvent e){
		if (e.getID()==e.MOUSE_PRESSED) {
		
			iMouseIsDown = true;
			Point mdLocation = new Point(e.getX(), e.getY());
						
			DisplayedState o = this.pointToDisplayedObject(mdLocation);
			if (o != null){
				if (!e.isShiftDown())
					this.clearSelection();
				iSelection.add(o);
				this.repaint(o.getXLocation(), o.getYLocation(), o.getWidth(), o.getHeight());
			}
			else {
				if (!e.isShiftDown())
					this.clearSelection();
			}
		}
		else if (e.getID()==e.MOUSE_RELEASED) {
			iMouseIsDown = false;
		}
      }
      
      
      //	Clear the selection, and update the screen accordingly.
	protected void clearSelection(){
	
		//	Iterate through the current selection, requesting a redraw of each one.
		//	??	Is there a race here if the redraws start before we actually clear the list?
		Iterator iter = iSelection.iterator();
		while (iter.hasNext()){
			DisplayedState o = (DisplayedState)iter.next();
			this.repaint(o.getXLocation(), o.getYLocation(), o.getWidth(), o.getHeight());
		}
		
		iSelection.clear();
	}


      //	Override of Swing processMouseMotionEvent().
	protected void processMouseMotionEvent(MouseEvent e){ 
		if (iMouseIsDown) {
			int tempX = e.getX();
			int tempY = e.getY();
		}
	}  
}