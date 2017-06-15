//	TimelineLayeredPane.java - A LayeredPane that contains all of the timeline sub-panes.

//	This is just a LayeredPane whose Scrollable behavior is optimized for the timeline.

import java.awt.*;
import javax.swing.*;

class TimelineLayeredPane extends JLayeredPane implements Scrollable {
	
	// 	Constants ----------------------------------------------------------------------


	//	Instance variables ----------------------------------------------------------------


	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------


	
	//	In general, LayeredPane's getPreferredSize() returns exactly what we want:
	//	the size of the largest of the child panes, which is usually the timeline pane.  However,
	//	if the scrollable viewport's size is larger than the timeline, we want to
	//	make our preferred size be that of the viewport.  This then causes the GridPane to
	//	be enlarged to cover the viewport.
	public Dimension getPreferredSize(){
		Dimension layoutSize = super.getPreferredSize();
		JViewport vp = (JViewport)getParent();
		Dimension vpSize = vp.getSize();
		return new Dimension(Math.max(layoutSize.width, vpSize.width),
				Math.max(layoutSize.height, vpSize.height));
	}


	// 	Scrollable methods  ----------------------------------------------------------------------

	/**
	 * Returns the preferred size of the viewport for a view component.
	 * For example the preferredSize of a JList component is the size
	 * required to acommodate all of the cells in its list however the
	 * value of preferredScrollableViewportSize is the size required for
	 * JList.getVisibleRowCount() rows.   A component without any properties
	 * that would effect the viewport size should just return 
	 * getPreferredSize() here.
	 * 
	 * @return The preferredSize of a JViewport whose view is this Scrollable.
	 * @see JViewport#getPreferredSize
	 */
	public Dimension getPreferredScrollableViewportSize(){
		return getPreferredSize();
	}
	
	
	/**
	 * Components that display logical rows or columns should compute
	 * the scroll increment that will completely expose one new row
	 * or column, depending on the value of orientation.  Ideally, 
	 * components should handle a partially exposed row or column by 
	 * returning the distance required to completely expose the item.
	 * <p>
	 * Scrolling containers, like JScrollPane, will use this method
	 * each time the user requests a unit scroll.
	 * 
	 * @param visibleRect The view area visible within the viewport
	 * @param orientation Either SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
	 * @param direction Less than zero to scroll up/left, greater than zero for down/right.
	 * @return The "unit" increment for scrolling in the specified direction
	 * @see JScrollBar#setUnitIncrement
	 */
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		
		//	Determine which unit size and which edge of the visibleRect is
		//	relevant.
		int unitSize;
		int edge;
		switch (orientation){
			case SwingConstants.VERTICAL:
				unitSize = Placer.LEVEL_SPACING;
				edge = (direction < 0) ? visibleRect.y : visibleRect.y + visibleRect.height;
				break;
			case SwingConstants.HORIZONTAL:
			default:
				unitSize = TimePositionMapping.SCALE_UNIT_SIZE;
				edge = (direction < 0) ? visibleRect.x : visibleRect.x + visibleRect.width;
				break;
		}

		//	See if there is a partial unit revealed at the appropriate edge of
		//	the visibleRect.
		int partial;
		if (direction < 0)
			partial = edge % unitSize;
		else
			partial = unitSize - (edge % unitSize);
		
		//	If there is a partial unit revealed, return the amount necessary
		//	to fully show it.  Otherwise, return the amount necessary to
		//	show the next unit.
		return (partial != 0) ? partial : unitSize;
	}
	
	
	/**
	 * Components that display logical rows or columns should compute
	 * the scroll increment that will completely expose one block
	 * of rows or columns, depending on the value of orientation. 
	 * <p>
	 * Scrolling containers, like JScrollPane, will use this method
	 * each time the user requests a block scroll.
	 * 
	 * @param visibleRect The view area visible within the viewport
	 * @param orientation Either SwingConstants.VERTICAL or SwingConstants.HORIZONTAL.
	 * @param direction Less than zero to scroll up/left, greater than zero for down/right.
	 * @return The "block" increment for scrolling in the specified direction.
	 * @see JScrollBar#setBlockIncrement
	 */
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
		
		//	Compute the block size as the visible width/height of the pane.
		//	??	It might be nice to round this to the natural unit size, and also
		//	??	bump it so that any partial rows/columns become visible.
		int blockSize;
		switch (orientation){
			case SwingConstants.VERTICAL:
				blockSize = visibleRect.height;
				break;
			case SwingConstants.HORIZONTAL:
			default:
				blockSize = visibleRect.width;
				break;
		}
		return blockSize;
	} 
	
	
	/**
	 * Return true if a viewport should always force the width of this 
	 * Scrollable to match the width of the viewport.  For example a noraml 
	 * text view that supported line wrapping would return true here, since it
	 * would be undesirable for wrapped lines to disappear beyond the right
	 * edge of the viewport.  Note that returning true for a Scrollable
	 * whose ancestor is a JScrollPane effectively disables horizontal
	 * scrolling.
	 * <p>
	 * Scrolling containers, like JViewport, will use this method each 
	 * time they are validated.  
	 * 
	 * @return True if a viewport should force the Scrollables width to match its own.
	 */
	public boolean getScrollableTracksViewportWidth(){
		return false;
	}
	
	/**
	 * Return true if a viewport should always force the height of this 
	 * Scrollable to match the height of the viewport.  For example a 
	 * columnar text view that flowed text in left to right columns 
	 * could effectively disable vertical scrolling by returning
	 * true here.
	 * <p>
	 * Scrolling containers, like JViewport, will use this method each 
	 * time they are validated.  
	 * 
	 * @return True if a viewport should force the Scrollables height to match its own.
	 */
	public boolean getScrollableTracksViewportHeight(){
		return false;
	}
}

