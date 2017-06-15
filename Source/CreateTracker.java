//	CreateTracker.java - Track drags made with the create tool.

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

class CreateTracker extends Tracker {
	
	// 	Constants ----------------------------------------------------------------------
	static final public int BODY_HEIGHT = 15;				//	Height of the object body
	static final protected int COLOR_BORDER_WIDTH = 0;		//	Width of color border around body


	//	Instance variables ----------------------------------------------------------------
	protected TLDocument iDoc;
	protected TimePositionMapping iTPM;
	protected TLWindow iWin;					//	Window containing the drag

	protected MouseEvent iStartingEvent;			//	What event started this drag
	protected Point iLogicalOffset = new Point();		//	Offset after snapping
	protected Point iFinalOffset;					//	Offset of location where mouse button was released
	protected int iSnappedAnchorX;				//	Anchor X snapped to grid

	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	public CreateTracker(TLDocument doc, TimePositionMapping tpm, TLWindow win, MouseEvent ev, int bodyPart, Component component, 
											Selection selection){
		super(ev.getPoint(), component, null);
		iDoc = doc;
		iTPM = tpm;
		iWin = win;
		iStartingEvent = ev;

		TLAction dragAction = new CreateDragAction(win);
		setAction(dragAction);
	}	


	//	beginDrag() is called when the mouse starts moving with its button down.
	//	anchorPosition is the position (relative to Component origin) where the mouse button went down.
	//	physicalOffset is the distance between the anchor and the mouse's current position in the Component.
	protected void beginDrag(Point anchorPosition, Point physicalOffset){
		//	Snap the anchor to the time grid.
		//	??	We're curently keeping the snapped anchor in our own 
		//	??	instance variable.  Would it be better to add a feature to
		//	??	Tracker that enables a subclass to modify the anchor,
		//	??	just like constrain()?
		int scale = iTPM.getScale();
		iSnappedAnchorX = iTPM.timeToXPosition(TLUtilities.snapMillisToUnit(
			iTPM.xPositionToTime(anchorPosition.x), scale));
	}
	
	
	//	constrain() is called each time a new mouse position is reported by AWT.
	//	The point it returns is used as the logical offset for subsequent subclass calls during this cycle.
	//	The default implementation of constrain() sets the logical offset to be the physical offset.
	protected Point constrain(Point anchorPosition, Point physicalOffset){
		
		//	Snap the X coordinate to the time grid.  Constrain the Y coordinate to 
		//	match the anchor (vertical constraint).
		int scale = iTPM.getScale();
		iLogicalOffset.x = iTPM.timeToXPosition(TLUtilities.snapMillisToUnit(
			iTPM.xPositionToTime(physicalOffset.x), scale));
		iLogicalOffset.y = 0;
		
		return iLogicalOffset;
	}
	
	
	//	drawFeedback() is called after constrain().  It is where the subclass should draw visual feedback
	//	to user.
	//	We use a split approach to drawing the feedback.  The body of the state is drawn here, on Swing's
	//	event dispatch thread.  If the body is made smaller by the user, we invalidate the revealed
	//	part of the background, so that it will be drawn by the window update thread.
	protected void drawFeedback(Point anchorPosition, Point logicalOffset, Point previousOffset, 
														Component component){
    		Graphics g = component.getGraphics();
    		
    		//	Initialize the color values.  These should match those used by DisplayedState.
    		Color bodyColor = Category.DEFAULT_BODY_COLOR;

		//	Draw the state body. 
		g.setColor(bodyColor);
		g.fillRect(iSnappedAnchorX, anchorPosition.y, logicalOffset.x, BODY_HEIGHT);

		//	Draw the color outline.
		//	??	Precompute bodyWidth outside the draw() routine.
		//	??	Is this doing anything???
		g.setColor(bodyColor);
		for (int i = 0; i < COLOR_BORDER_WIDTH; i++)
			g.drawRect(iSnappedAnchorX + i, anchorPosition.y + i, logicalOffset.x, BODY_HEIGHT);
		
		//	If the state body has shrunk, invalidate the revealed area so that it will
		//	be painted with the grid and background.
		if (previousOffset != null && logicalOffset.x < previousOffset.x)
			component.repaint(iSnappedAnchorX + logicalOffset.x,
				anchorPosition.y, 
				previousOffset.x - logicalOffset.x, BODY_HEIGHT);
	}


	//	endDrag() is called when the mouse button is released.
	//	logicalOffset is the distance between the anchor and the mouse's current position, as modified by
	//	constrain().
	protected void endDrag(Point anchorPosition, Point logicalOffset, Component component){
		//	Request a repaint of the space currently occupied by the
		//	state body we've drawn.
		component.repaint(iSnappedAnchorX, anchorPosition.y, logicalOffset.x, BODY_HEIGHT);

		iFinalOffset = logicalOffset;
		performAction();
	}
	
	
	//	Internal class implementing a TLAction for drags.
	//	This is used for managing enable/disable.
	class CreateDragAction extends TLAction {
	
		//	Constructor
		CreateDragAction(TLWindow win){
			super("CreateDragAction", win);
		}
	
		//	Called by the action manager when it wants the Action to update its enable state.
		//	??	Need to also take into account the state's locked value, as well as the document's.
		public void updateEnable(){
		}
		
		
		//	Called when the action should be peSrformed.
		//	Unlike most instances of actionPerformed() this one is called by our own code (above),
		//	rather than by Swing.
		//	At the moment, the ActionEvent passed in is unused (and is actually null).
		public void actionPerformed(ActionEvent ev) {
			try {
				super.actionPerformed(ev);
				
				//	Compute the starting and ending times of the state.
				int scale = iTPM.getScale();
				long start = TLUtilities.snapMillisToUnit(iTPM.xPositionToTime(iSnappedAnchorX), scale);
				long end = TLUtilities.snapMillisToUnit(
					iTPM.xPositionToTime(iSnappedAnchorX + iFinalOffset.x), scale);
				
				//	Create the state.  All fields begin with default values.
				LabelInfo labelInfo = new LabelInfo("Unlabeled", "", false);
				TLEvent startingEvent = new TLEvent(start, start);
				TLEvent endingEvent = new TLEvent(end, end);		
				TLState newState = new TLState(labelInfo, startingEvent, endingEvent, null);
				//	??	Lines below expanded from:
				//	??	newState.setCategories(iDoc.getDefinedCategories().new MemberSet());
				//	??	so they can be parsed by Source-Navigator 99R1.
				DefinedCategorySet dcs = iDoc.getDefinedCategories();
				newState.setCategories(dcs.new MemberSet());

				//	Create the Edit object, and pass it to the document.
				Set affectedStateSet = new HashSet();
				affectedStateSet.add(newState);
				AddDeleteStateEdit e = new AddDeleteStateEdit(affectedStateSet, false);
				iDoc.executeEdit(e);
				
				//	Start up the State Info dialog so the user can enter a
				//	meaningful label, and perhaps change other parameters.
				//	Create an edit for this change too, unless the user cancels
				//	out of the dialog.
				//	??	If the user cancels out, should we delete the state we
				//	??	just created?  Should the creation and edit be treated
				//	??	as a single step as far as undo is concerned?
				TLState editedState = StateAttributeDialog.doDialog(iDoc, iWin, newState);
				if (editedState != null){
					TLUndoableEdit edit = new StateEdit(newState, editedState);
					iDoc.executeEdit(edit);
				}
				
				//	Reset (as a convenience to the user), the cursor to the selection tool, unless the
				//	shift key has been held down.
				if (!iStartingEvent.isShiftDown())
					iWin.enableSelectTool();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	}
}

