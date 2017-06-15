//	SelectTracker.java - Track drags made with the select tool.

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class SelectTracker extends Tracker {

	// 	Constants ----------------------------------------------------------------------
	//	Intentionally illegal index into body parts.  Helps catch bugs.
	protected static final int ILLEGAL = -1;
	
	//	This array maps a part index into the index of the part which should be used as the anchor for
	//	a normal drag.  If the hit is in a single handle, use that handle.  If it is in two overlapping handles, 
	//	use the ends.  If it is in the state body, use the start of the state.
	protected static final int[] ANCHOR_PART = {TLState.T0, TLState.T1, TLState.T2, TLState.T3, 
											TLState.T0, TLState.T3, TLState.T0};
	
	//	This array maps a part index into the the index of the part that should be used as the anchor for
	//	a "special" drag.  
	protected static final int[] SPECIAL_ANCHOR_PART = {TLState.T1, TLState.T1, TLState.T2, TLState.T2, 
											TLState.T1, TLState.T2, ILLEGAL};
											
	//	This array maps a part index into a boolean indicating whether a "special" drag can be started this way.
	//	Special drags can be started at the state beginning or end, or in the overlapped beginning or end.
	protected static final boolean[] CAN_BE_SPECIAL = { true, false, false, true, true, true, false};
		
	//	Instance variables ----------------------------------------------------------------
	//	Significant objects we have permanent relationships with.
	protected TLDocument iDoc;
	protected TimePositionMapping iTPM;
	protected TLWindow iWin;					//	Window containing the drag

	protected int iDraggedPart;					//	Body part being dragged, per DisplayedState.inBodyPart()
	protected int iPairedPart;					//	Another body part being dragged, or NONE
	protected boolean iSpecialDragType = false;		//	The user clicked in either end of state with control key
	protected Selection iSelection;				//	Display objects in current selection.
	protected int iDragHandleInitialPosition;		//	Initial X position of object handle being dragged
	protected Point iLogicalOffset = new Point();		//	Offset after snapping
	protected int iFinalOffset;					//	Offset at end of drag
	
	//	Constraints on the on-going mouse drag operation.  Undefined after the drag is done.
	protected boolean iVerticalConstrain = false;
	protected boolean iUserContrain = false;
	protected int iLoDragLimit;					//	Lowest x coordinate that handle can be dragged to
	protected int iHiDragLimit;					//	Highest x coordinate that handle can be dragged to
	

	//	Trivial accessors -----------------------------------------------------------------
	
	
	// 	Constructor  --------------------------------------------------------------------
	public SelectTracker(TLDocument doc, TimePositionMapping tpm, TLWindow win, MouseEvent ev, int bodyPart, Component component, 
											Selection selection){
		super(ev.getPoint(), component, null);
		iDoc = doc;
		iTPM = tpm;
		iWin = win;
		iSelection = selection;
		iDraggedPart = bodyPart;
		
		//	Create the Action that is fired when the drag completes.
		//	The selection listens to change events from the drag action so that it can reflect that
		//	action's enable state in the sort of drag handles displayed to the user.		
		TLAction dragAction = new SelectDragAction(iWin);
		setAction(dragAction);
		dragAction.addPropertyChangeListener(iSelection);
		
		//	Do a special type of drag if the control key is down, and the mouse hit
		//	is in the beginning or end of the state.
		iSpecialDragType = ev.isControlDown() && CAN_BE_SPECIAL[iDraggedPart];
				
		//	Control snapping and contraints by the first TLState in the selection.
		DisplayedState dobj = (DisplayedState)iSelection.iterator().next();
		TLState state = dobj.getState();
		
		//	Remember the initial time of the handle we're dragging.  This will
		//	be used later to control snapping to the grid.
		long originalTime = state.getTimeParameter(ANCHOR_PART[iDraggedPart]);;
		iDragHandleInitialPosition = iTPM.timeToXPosition(originalTime);
	
		//	Constrain vertical movement, unless we're dragging the whole state.
		iVerticalConstrain = iDraggedPart != DisplayedState.HIT_IN_BODY;

		//	Compute the horizontal drag limits.
		//	??	Do we need to do this here, since we'll recompute the drag
		//	??	limits when the drag really starts?
		this.computeHorizontalDragLimits(ev.getPoint());
	}
	
	
	//	beginDrag() is called when the mouse starts moving with its button down.
	//	anchorPosition is the position (relative to Component origin) where the mouse button went down.
	//	physicalOffset is the distance between the anchor and the mouse's current position in the Component.
	protected void beginDrag(Point anchorPosition, Point physicalOffset){
		//	??	Should some or all of the processing done in the constructor
		//	??	be done here instead?
		
		//	If the drag is "special" (control key down), reset the anchor point
		//	as if the user dragged in the T2 or T3 handle.  This causes the handle to "jump" to the
		//	current mouse cursor position.
		//	??	Changing the anchor in this way is in some sense going behind the
		//	??	back of Tracker, but it works.
		//	??	I can't get this code to work with the next drag mechanism, but I'm going to leave
		//	??	it in in case I can figure it out later.
		if (iSpecialDragType){
			int simulatedAnchor = SPECIAL_ANCHOR_PART[iDraggedPart];
			DisplayedState dobj = (DisplayedState)iSelection.iterator().next();
			TLState state = dobj.getState();
			anchorPosition.x = iTPM.timeToXPosition(state.getTimeParameter(simulatedAnchor));

			//	Recompute the horizontal drag limits.  Since these are expressed as an offset,
			//	their value depends upon the value of the anchor point.
			this.computeHorizontalDragLimits(anchorPosition);
		}				
	}
	
	
	//	constrain() is called each time a new mouse position is reported by AWT.
	//	The point it returns is used as the logical offset for subsequent subclass calls during this cycle.
	protected Point constrain(Point anchorPosition, Point physicalOffset){
		
		//	Compute the new logical offset.				
		//	Snap the object handle being dragged to the minor grid.
		int scale = iTPM.getScale();
		int newLogicalOffsetX = iTPM.timeToXPosition(TLUtilities.snapMillisToUnit(
			iTPM.xPositionToTime(iDragHandleInitialPosition + physicalOffset.x), scale)) -
			iDragHandleInitialPosition;
		int newLogicalOffsetY = physicalOffset.y;
			
		//	Enforce time axis limits, if applicable.
		if (newLogicalOffsetX > iHiDragLimit)
			newLogicalOffsetX = iHiDragLimit;
	
		if (newLogicalOffsetX < iLoDragLimit)
			newLogicalOffsetX = iLoDragLimit;

		//	If we're dragging in the upper or lower slider of an event, or if the
		//	user asked for it, enforce Y-axis constraining.
		if (iVerticalConstrain || iUserContrain)
				newLogicalOffsetY = 0;

		//	Update the logical offset of the mouse position.
		iLogicalOffset.x = newLogicalOffsetX;
		iLogicalOffset.y = newLogicalOffsetY;
		return iLogicalOffset;
	}
	
	
	//	drawFeedback() is called after constrain().  It is where the subclass should draw visual feedback
	//	to user.
	protected void drawFeedback(Point anchorPosition, Point logicalOffset, Point previousOffset, 
														Component component){
		//	Move the selection on the screen.
		//	??	Should this array allocation be moved out of the drag loop?
		int offset[] = new int[TLState.PARAMETER_COUNT];
		boolean parameterAffected[] = determineAffectedParameters(logicalOffset.x);
		for (int i = 0; i < TLState.PARAMETER_COUNT; i++)
			if (parameterAffected[i])
				offset[i] = logicalOffset.x;
		iSelection.setOffset(offset, logicalOffset.y);
	}
	
	
	//	endDrag() is called when the mouse button is released.
	//	logicalOffset is the distance between the anchor and the mouse's current position, as modified by
	//	constrain().
	protected void endDrag(Point anchorPosition, Point logicalOffset, Component component){
		//	Clear the offsets in the display objects of the selection.
		iSelection.setOffset(null, 0);
		
		//	Save the final offset for use by the action.
		iFinalOffset = logicalOffset.x;
		
		//	Perform the action that implements the edit commanded by the drag just completed.
		performAction();
	}
	
	
	//	Set the horizontal drag constraints according to the position of the adjacent handle in the object.
	protected void computeHorizontalDragLimits(Point anchor){
		DisplayedState dobj = (DisplayedState)iSelection.iterator().next();
		TLState state = dobj.getState();
		long t0 = state.getTimeParameter(TLState.T0);
		long t1 = state.getTimeParameter(TLState.T1);
		long t2 = state.getTimeParameter(TLState.T2);
		long t3 = state.getTimeParameter(TLState.T3);
		switch (iDraggedPart){
			case TLState.T0:  
				iLoDragLimit = Integer.MIN_VALUE;
				iHiDragLimit = iTPM.timeToXPosition(Math.min(t1, t2)) - anchor.x;
				break;

			case TLState.T1:  
			case TLState.T2:  
				iLoDragLimit = iTPM.timeToXPosition(t0) - anchor.x;
				iHiDragLimit = iTPM.timeToXPosition(t3) -  anchor.x;
				break;

			case TLState.T3:  
				iLoDragLimit = iTPM.timeToXPosition(Math.max(t1, t2)) -  anchor.x;
				iHiDragLimit = Integer.MAX_VALUE;
				break;

			case DisplayedState.T0_AND_T1:  
				iLoDragLimit = Integer.MIN_VALUE;
				iHiDragLimit = iTPM.timeToXPosition(t3) -  anchor.x;
				break;

			case DisplayedState.T2_AND_T3:  
				iLoDragLimit = iTPM.timeToXPosition(t0) -  anchor.x;
				iHiDragLimit = Integer.MAX_VALUE;
				break;
				
			case DisplayedState.HIT_IN_BODY:
				iLoDragLimit = Integer.MIN_VALUE;
				iHiDragLimit = Integer.MAX_VALUE;
				break;
		}
	}


	//	Fill in an array of booleans indicating which parameters of the state are affected by the drag.
	//	This can be dependent upon the sign of the offset.
	protected boolean[] determineAffectedParameters(int logicalOffset){

		//	??	Is it worth moving the allocation of this array out of the drag loop, perhaps
		//	??	to avoid heap fragmentation?
		boolean parts[] = new boolean[TLState.PARAMETER_COUNT];
		switch (iDraggedPart){
			case TLState.T0:
			case TLState.T1:
			case TLState.T2:
			case TLState.T3:
				parts[iDraggedPart] = true;
				break;
				
			case DisplayedState.T0_AND_T1:
				if (iSpecialDragType){
					if (logicalOffset < 0)
						parts[TLState.T0] = true;
					else
						parts[TLState.T1] = true;
				}
				else {
					parts[TLState.T0] = true;
					parts[TLState.T1] = true;
				}
				break;

			case DisplayedState.T2_AND_T3:
				if (iSpecialDragType){
					if (logicalOffset >= 0)
						parts[TLState.T3] = true;
					else
						parts[TLState.T2] = true;
				}
				else {
					parts[TLState.T3] = true;
					parts[TLState.T2] = true;
				}
				break;

			case DisplayedState.HIT_IN_BODY:
				for (int i = 0; i < TLState.PARAMETER_COUNT; i++)
					parts[i] = true;
				break;
		}
		return parts;
	}
	

	//	Internal class implementing a TLAction for drags.
	//	This is used for managing enable/disable.
	class SelectDragAction extends TLAction {
	
		//	Constructor
		SelectDragAction(TLWindow win){
			super("SelectDrag", win);
		}
	
		//	Called by the action manager when it wants the Action to update its enable state.
		//	??	Need to also take into account the state's locked value, as well as the document's.
		public void updateEnable(){
			this.setEnabled(iSelection.size() == 1);
		}
		
		
		//	Called when the action should be peSrformed.
		//	Unlike most instances of actionPerformed() this one is called by our own code (above),
		//	rather than by Swing.
		//	At the moment, the ActionEvent passed in is unused (and is actually null).
		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
	
				//	Create a new TLState to store the updated state parameters.
				DisplayedState dobj = (DisplayedState)iSelection.iterator().next();
				TLState selectedState = dobj.getState();
				TLState newValueState = new TLState(selectedState);
				
				//	Compute the delta time from where the dragged handle ended up at.
				long deltaTime = iTPM.xDeltaToTimeDelta(iFinalOffset);
				
				//	Modify the values in the temporary TLState.
				//	If the user dragged in the state body, translate all four of its time
				//	parameters.
				//	Carefully choose the order in which we update the state parameters, so that the invariant
				//	relationships between the parameters are preserved when we're updating them all 
				//	(moving the state). 								
				boolean parameterAffected[] = determineAffectedParameters(iFinalOffset);
				if (deltaTime >= 0){
					for (int i = TLState.T3; i >= TLState.T0; i--)
						if (parameterAffected[i])
							newValueState.setTimeParameter(i, 
									newValueState.getTimeParameter(i) + deltaTime);
				}
				else {
					for (int i = TLState.T0; i <= TLState.T3; i++)
						if (parameterAffected[i])
							newValueState.setTimeParameter(i, 
									newValueState.getTimeParameter(i) + deltaTime);
				}

				//	Create the edit object and send it to the document.
				TLUndoableEdit edit = new StateEdit(selectedState, newValueState);
				iDoc.executeEdit(edit);
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	}
}

