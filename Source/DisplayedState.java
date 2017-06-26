//	DisplayedState.java - Represent a state as displayed on the screen.

//	??	Font size (and by implication body size) should be user-configurable, to adapt to various display
//	??	pixel densities.

import java.awt.*;
import java.util.*;

public class DisplayedState extends DisplayedObject {

	// 	Constants ------------------------------------------------------------------------
	//	Value for inBodyPart to return if the hit is not in any drag point, but is in the body.
	//	This value is also returned if the hit is in a non-embedded label.
	//	This must not overlap with those defined in TLState.  Constant arrays in SelectTracker.java
	//	depend upon the exact value.
	static final public int HIT_IN_BODY = 6;
	
	//	Values for inBodyPart to return if the hit was in two overlapping drag handles.
	//	These must not overlap with those defined in TLState.  Constant arrays in SelectTracker.java
	//	depend upon the exact values.
	static final public int T0_AND_T1 = 4;
	static final public int T2_AND_T3 = 5;

	//	Constants determining pane layout.
	static final public int BODY_HEIGHT = 15;					//	Height of the object body
	static final protected int LABEL_Y_POSITION =11;			//	Distance from top of body to label baseline
	static final protected int LABEL_BLANKING_Y_POSITION = 2;	// 	Distance from top of body to label blanking
	static final protected int LABEL_BLANKING_HEIGHT = 11;		//	Height of blanking rectangle
	static final protected int MIN_EMBEDDED_END_SPACE = 5;	//	Minimum space between embedded label and state ends
	static final protected int TRAILING_LABEL_GAP = 3;			//	Gap between body and trailing label
	static final protected int MAX_LABEL_REPEAT_SPACING = 400;//	Max distance between repeats of a given label
	static final protected int DRAG_HANDLE_SIZE = 5;			//	Size of drag handle square
	static final protected int DRAG_HANDLE_Y_POSITION = (BODY_HEIGHT - DRAG_HANDLE_SIZE)/2;
	static final protected int SELECTION_BORDER_WIDTH = 2;		//	Width of border around selected objects
	static final protected int COLOR_BORDER_WIDTH = 0;			//	Width of color border around body
	static final protected int HIT_TOLERANCE = 4;				//	+/- this when hit testing
	static final protected int MINIMUM_BODY_WIDTH = 4;			//	Make sure bodies are at least this wide
	
	//	Constants determining the font we use for the label.
	static final protected String FONT_NAME = "SansSerif";  
	static final protected int FONT_STYLE = java.awt.Font.PLAIN;
	static final protected int FONT_SIZE = 12;
	
	//	Constants determining pane colors.
	static final protected Color HILIGHT_COLOR = Color.red;		//	Color of selection hilight
	
	//	The color ramp we use for displaying approximate time periods.
	//	??	MAX_RAMP_COLOR made public for TLDocument creating Categories.
	static final protected int MAX_RAMP_VALUE = 110;
	static final protected int MIN_RAMP_VALUE = 200;
	static final protected int RAMP_STEP_COUNT = 10;
	static final protected int MIN_RAMP_INDEX = 0;
	static final protected int MAX_RAMP_INDEX = RAMP_STEP_COUNT - 1;
	static final protected int MIDDLE_RAMP_INDEX = (MAX_RAMP_INDEX - MIN_RAMP_INDEX) / 2;
	static final protected Color[] sRampColors;
	static final public Color MAX_RAMP_COLOR;
	static final protected Color MIDDLE_RAMP_COLOR;
	static {
		sRampColors = new Color[RAMP_STEP_COUNT];
		int stepDelta = (MAX_RAMP_VALUE - MIN_RAMP_VALUE)/(RAMP_STEP_COUNT -1);
		int stepValue = MIN_RAMP_VALUE;
		for (int i = 0; i < RAMP_STEP_COUNT; i++){
			sRampColors[i] = new Color(stepValue, stepValue, stepValue);
			stepValue += stepDelta;
		}
		MAX_RAMP_COLOR = sRampColors[MAX_RAMP_INDEX];
		MIDDLE_RAMP_COLOR = sRampColors[MIDDLE_RAMP_INDEX];
	}
	static protected final Color DEFAULT_BODY_COLOR = Category.DEFAULT_BODY_COLOR;	//	Used if state is not in any categories
	static protected final float INDEFINITE_ALPHA_VALUE = 0.5f;		//	Used only if DISPLAY_INDEFINITE_USING_ALPHA_CHANNEL is true
	static protected final float INDEFINITE_BRIGHTNESS_VALUE = 0.8f;	//	Used only if DISPLAY_INDEFINITE_USING_ALPHA_CHANNLE is false
	static final protected boolean DISPLAY_STATES_IN_COLOR = true;	//	Which flavor of display
													//	do we prefer this week?
	static final protected boolean DISPLAY_STATES_IN_WHITE = false;	//	
	static final protected boolean DISPLAY_LABELS_IN_BLACK = true;
	static final protected boolean DISPLAY_INDEFINITE_USING_ALPHA_CHANNEL = false;
	
	//	The font used for drawing labels
	static final public Font LABEL_FONT = new Font(FONT_NAME, FONT_STYLE, FONT_SIZE);
	static final protected FontMetrics LABEL_METRICS = Toolkit.getDefaultToolkit().getFontMetrics(LABEL_FONT);
	
	//	Miscellaneous constants.
	static final protected int FRACTION_SIZE = 32;				//	Fraction part of fixed-point numbers


	//	Instance variables----------------------------------------------------------------
	protected TLState iState;			//	The State object we're displaying
	
	//	Position of the object in pane coordinates.
	protected int iLocationY;
		
	//	Offset of the object from the nominal location specified by iLocationX and iLocationY.
	protected int[] iOffsetX;			//	Null if no X offset
	protected int iOffsetY;
		
	protected boolean iDimensionsCalculated = false;	//	Dimensions (widths) are valid
	
	//	Nominal (before offsets) position of each of the state's time parameters, in graphic coordinates.
	int iNominalX0, iNominalX1, iNominalX2, iNominalX3;

	//	Widths of the different regions.  Region 1 is the starting indefinite period, region 2 is the
	//	definite period, and region 3 is the ending indefinite period.
	protected int iRegion1Width, iRegion2Width, iRegion3Width;
	
	//	Starting X locations of each region.  Updated whenever the object location or offset changes.
	//	These are essentially caches of calculations done ahead of time to speed up drawing.
	protected int iRegion1Start, iRegion2Start, iRegion3Start, iLabelStart, iRegion3End;
	
	//	Starting Y location of the object.  Again, this is a cache of the calculation, to speed up drawing.
	protected int iYStart;
	
	//	Width of the text that makes up the label, and boolean flag indicating the form of the label.
	//	The number of times a label is repeated within a body, and the spacing between repeats.
	protected int iLabelWidth;
	protected boolean iEmbeddedLabel;
	protected boolean iRepeatingLabel;
	protected int iLabelRepeatSpacing; 			//	Spacing between repeats
	
	//	Total width of the object, including the label if it is not embedded.
	protected int iTotalWidth;
	
	//	Indicate whether this object is currently selected.
	protected boolean iSelected;
	
	//	Indicates whether drag handles should be displayed.
	protected boolean iShowDragHandles;
	
	
	//	Constructor---------------------------------------------------------------------
	public DisplayedState(TLState state){
		iState = state;
	}
	
	
	//	Accessors
	public TLState getState()			{ return iState;}
	public int getXLocation()			{  return (iOffsetX == null) ? iNominalX0 : iNominalX0 + iOffsetX[TLState.T0];}
	public int getYLocation()			{  return iLocationY + iOffsetY;}
	
	
	//	Called by TimelinePane to set our vertical location in the pane.
	public void setYLocation(int y){
		iLocationY = y;
		
		this.calculateYStarts();
	}
	
	
	//	Set an offset (possibly zero) for this object.  The X offsets are specified as
	//	an array of four integers, corresponding to the four time parameters of the state.
	public void setOffset(int[] x, int y){
		iOffsetX = x;
		iOffsetY = y;
		
		this.calculateYStarts();
		this.calculateXStarts();
	}


	//	Called when the TPM or our underlying data object has changed.
	public void calculateDimensions(TimePositionMapping tpm){
	
   		TLEvent startingEvent = iState.getStartingEvent();
    		TLEvent endingEvent = iState.getEndingEvent();
    		
    		//	Get the locations of each of the region boundaries.
    		iNominalX0 = tpm.timeToXPosition(startingEvent.getPeriodStart());
    		iNominalX1 = tpm.timeToXPosition(startingEvent.getPeriodEnd());
       		iNominalX2 = tpm.timeToXPosition(endingEvent.getPeriodStart());
       		iNominalX3 = tpm.timeToXPosition(endingEvent.getPeriodEnd());
       		
       		//	Make sure that the state is at least a minimal width.
       		if (iNominalX3 - iNominalX0 < MINIMUM_BODY_WIDTH){
       			iNominalX3 = iNominalX0 + MINIMUM_BODY_WIDTH;
       			iNominalX2 = iNominalX3;
       		}
       		
      		//	Calculate width of label.
      		//	??	External labels could be made smaller by wrapping them into two lines.
    		iLabelWidth = LABEL_METRICS.stringWidth(iState.getLabelInfo().getLabel());
    		
		this.calculateXStarts();
		this.calculateYStarts();

		iDimensionsCalculated = true;
	}
	
	
	//	Calculate the starting positions for each region and the label.
	protected void calculateXStarts(){
	
		//	Compute the position of each of the state's time parameters, taking into account offsets.
		iRegion1Start = iNominalX0;
		iRegion2Start = iNominalX1;
		iRegion3Start = iNominalX2;
		iRegion3End = iNominalX3;
		
		if (iOffsetX != null){
			iRegion1Start +=  iOffsetX[TLState.T0];
			iRegion2Start += iOffsetX[TLState.T1];
			iRegion3Start += iOffsetX[TLState.T2];
			iRegion3End += iOffsetX[TLState.T3];
		}
		
       		//	Calculate the widths of each region.
       		iRegion1Width = iRegion2Start - iRegion1Start;
       		iRegion2Width = iRegion3Start - iRegion2Start;
       		iRegion3Width = iRegion3End - iRegion3Start;
       		iTotalWidth = iRegion3End - iRegion1Start;
     		
    		//	Embed the label inside the state body if it fits.
    		iEmbeddedLabel = iTotalWidth >= (iLabelWidth + 2 * MIN_EMBEDDED_END_SPACE);
	    	if (iEmbeddedLabel){
	    		
	    		//	The repeat count is one more than the number of label + max spacing pairs that fit into the body.
	    		int repeatCount =  ((iTotalWidth - MAX_LABEL_REPEAT_SPACING) / 
	    								(iLabelWidth + MAX_LABEL_REPEAT_SPACING)) + 1;
	    		iLabelRepeatSpacing = (iTotalWidth - repeatCount * iLabelWidth) /  (repeatCount + 1);
	    		iLabelStart = iRegion1Start + iLabelRepeatSpacing;
	    		iRepeatingLabel = repeatCount > 1;
	    	}
	    	else {
	    		iLabelStart = iRegion3Start + iRegion3Width + TRAILING_LABEL_GAP;
     			iTotalWidth += iLabelWidth + TRAILING_LABEL_GAP + TRAILING_LABEL_GAP;
	    		iRepeatingLabel = false;
	    	}
	}
	
	
	//	Calculate the starting Y location of the entire object.
	protected void calculateYStarts(){
		iYStart = iLocationY + iOffsetY;
	}
	
	
	//	Returns the overall horizontal dimension of this state.
	public int getWidth(){
		if (Debug.sCurLevel > 0){
			Debug.assertOnError(iDimensionsCalculated);
		}
		return iTotalWidth;
	}


	//	Returns the overall vertical dimension of this state.
	public int getHeight(){
		if (Debug.sCurLevel > 0)
			Debug.assertOnError(iDimensionsCalculated);
		return BODY_HEIGHT;
	}


	 //	Draw a single object.
	 protected void draw(Graphics g, Rectangle clipRect){
	 
	 	//	If we're not visible, return immediately.  This could be coded more consisely as the intersection
	 	//	of two rectangles, but the code actually executed ends up the same.
	 	if ((iRegion1Start + iTotalWidth < clipRect.x) || (clipRect.x + clipRect.width < iRegion1Start) ||
	 			(iLocationY + iOffsetY + BODY_HEIGHT < clipRect.y) || 
	 			(clipRect.y + clipRect.height < iLocationY + iOffsetY))
	 		return;

       		//	Find the color of the first Category that this state is a member of.
       		//	??	This will obviously be enhanced when we decide how to display states that
       		//	??	are members of more than one category.
         		Color bodyColor = null;
         		Color indefiniteColor = null;
      		if (DISPLAY_STATES_IN_COLOR){
      			
      			//	??	Following line split due to compiler bug as of 6/99.
      			//	??	Set categories = iState.getCategories().getAsSet();
      			DefinedCategorySet.MemberSet MemberSet = iState.getCategories();
      			Set categories = MemberSet.getAsSet();
      			if (categories.size() > 0){
		       		Category cat = (Category)categories.iterator().next();
		         		bodyColor = cat.getColor();
		         	}
		       	else
		         		bodyColor = DEFAULT_BODY_COLOR;
		         	if (DISPLAY_INDEFINITE_USING_ALPHA_CHANNEL){
		         		float[] components = bodyColor.getRGBComponents(null);
		         		indefiniteColor = new Color(components[0], components[1], components[2], INDEFINITE_ALPHA_VALUE);
		         	}
		         	else {
		         		float[] hsb = Color.RGBtoHSB(bodyColor.getRed(), bodyColor.getGreen(), bodyColor.getBlue(), null);
		         		hsb[2] = hsb[2]*INDEFINITE_BRIGHTNESS_VALUE;
		         		indefiniteColor = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
		         	}
	         	}
	         	else {
	         		if (DISPLAY_STATES_IN_WHITE)
	         			bodyColor = Color.white;
	         		else
	         			bodyColor = MAX_RAMP_COLOR;
	         		indefiniteColor = MIDDLE_RAMP_COLOR;
	         	}

     		//	Draw starting indefinite zone.
       		if (iRegion1Width > 0){
       			g.setColor(indefiniteColor);
       			g.fillRect(iRegion1Start, iYStart , iRegion1Width, BODY_HEIGHT);
       		}

   		//	Draw the state body. 
 		g.setColor(bodyColor);
		g.fillRect(iRegion2Start, iYStart, iRegion2Width, BODY_HEIGHT);
    		
       		//	Draw ending indefinite zone.
       		if (iRegion3Width > 0){
       			g.setColor(indefiniteColor);
       			g.fillRect(iRegion3Start, iYStart, iRegion3Width, BODY_HEIGHT);
       		}

		//	Draw the color outline.
		//	??	Precompute bodyWidth outside the draw() routine.
		//	??	Is this doing anything???
		g.setColor(bodyColor);
		int bodyWidth = iRegion1Width + iRegion2Width + iRegion3Width;  
		for (int i = 0; i < COLOR_BORDER_WIDTH; i++)
			g.drawRect(iRegion1Start + i, iYStart + i, bodyWidth, BODY_HEIGHT);

		//	Draw the highlight.
   		if (iSelected){
			g.setColor(HILIGHT_COLOR);
			for (int i = 0; i < SELECTION_BORDER_WIDTH; i++)
    				g.drawRect(iRegion1Start + i, iYStart + i, iTotalWidth-SELECTION_BORDER_WIDTH, 
    											BODY_HEIGHT-SELECTION_BORDER_WIDTH);
    		}
    		
    		//	Draw the drag handles.
    		if (iShowDragHandles){
 			g.setColor(HILIGHT_COLOR);
     			g.fillRect(iRegion1Start, iYStart +DRAG_HANDLE_Y_POSITION, DRAG_HANDLE_SIZE, 
    											DRAG_HANDLE_SIZE);
     			g.fillRect(iRegion2Start - (DRAG_HANDLE_SIZE/2), iYStart +DRAG_HANDLE_Y_POSITION, DRAG_HANDLE_SIZE, 
    											DRAG_HANDLE_SIZE);
     			g.fillRect(iRegion3Start - (DRAG_HANDLE_SIZE/2), iYStart +DRAG_HANDLE_Y_POSITION, DRAG_HANDLE_SIZE, 
    											DRAG_HANDLE_SIZE);
    			g.fillRect(iRegion3End -DRAG_HANDLE_SIZE, iYStart +DRAG_HANDLE_Y_POSITION, DRAG_HANDLE_SIZE, 
    											DRAG_HANDLE_SIZE);
  		}
    		
    		//	Draw the label.  TimelinePane sets this pane's font at init time.
       		String label = iState.getLabelInfo().getLabel();
   		if (iEmbeddedLabel){
 		 	if (DISPLAY_LABELS_IN_BLACK)
 		 		g.setColor(Color.black);
 		 	else
 		 		g.setColor(Color.white);
		 }
		 else {
       			g.setColor(GridPane.BACKGROUND_COLOR);
      			g.fillRect(iLabelStart, iYStart + LABEL_BLANKING_Y_POSITION, iLabelWidth, LABEL_BLANKING_HEIGHT);
		 	g.setColor(Color.black);
		 }
		
		if (!iRepeatingLabel){
	    		g.drawString(label, iLabelStart, iYStart + LABEL_Y_POSITION);
		}
		else {
	    		int stepSize = iLabelRepeatSpacing + iLabelWidth;
	    		int x;
	    		if (iLabelStart < clipRect.x)
	    			x = ((clipRect.x - iLabelRepeatSpacing) / stepSize ) * stepSize + iLabelRepeatSpacing;
	    		else
	    			x = iLabelStart;
	    		int endClip=clipRect.x + clipRect.width;
	    		int endStateBody = iRegion3End - iLabelWidth;
	    		int endX = Math.min(endClip, endStateBody);
	    		do {
	    			g.drawString(label, (int)(x >> FRACTION_SIZE), iYStart + LABEL_Y_POSITION);
	    			x += stepSize;
	    		} while (x <= endX);
		}
	 }
	 
	 
	//	Update our state variable which controls whether drag handles are shown.
	public void showDragHandles(boolean b){
		iShowDragHandles = b;
	}


	 //	Make this object selected.
	 public void setSelected(){
	 	iSelected = true;
	 }
	 
	 
	 //	Make sure this object is not selected.
	 public void clearSelected(){
	 	iSelected = false;
	 }
	 
	 
	 //	Identify the part of this object which is under the provided point.
	 //	Returns a parameter identifier (defined in TLState) if we're over one of the divisions
	 //	between parts.  Otherwise, returns HIT_IN_BODY.  Throws an internal exception
	 //	if the point is not in this displayed state.
	 public int inBodyPart(int x, int y){
	 
		if (Debug.sCurLevel > 0){
			Debug.assertOnError(iDimensionsCalculated);
		}

	 	//	Check for hits within each of the object's handles.
	 	//	??	Right now, we don't make sure that the hit is within a drag handle.  In fact, our HIT_TOLERANCE
	 	//	??	constant is defined separately from the size of the handles.
	 	//	??	There's probably a simpler way to program this, but I haven't thought of it yet.
	 	boolean hitT0 = java.lang.Math.abs(iRegion1Start - x) < HIT_TOLERANCE;
	 	boolean hitT1 = java.lang.Math.abs(iRegion2Start - x) < HIT_TOLERANCE;
	 	boolean hitT2 = java.lang.Math.abs(iRegion3Start - x) < HIT_TOLERANCE;
	 	boolean hitT3 = java.lang.Math.abs(iRegion3End - x) < HIT_TOLERANCE;
	 	if (hitT0){
	 		if (hitT1)
	 			return T0_AND_T1;
	 		else
	 			return TLState.T0;
	 	}
	 	if (hitT3){
	 		if (hitT2)
	 			return T2_AND_T3;
	 		else
	 			return TLState.T3;
	 	}
	 	if (hitT1)
	 		return TLState.T1;
	 	if (hitT2)
	 		return TLState.T2;
	 			
	 	if (iRegion1Start < x && x < iRegion3End)
	 		return HIT_IN_BODY;
	 	
	 	//	Is the hit is within a non-embedded label?
	 	if (!iEmbeddedLabel && iRegion3End < x && x <  iRegion1Start + iTotalWidth)
	 		return HIT_IN_BODY;
	 	
	 	throw new ImplementationException("DisplayedState.inBodyPart() called with point outside it");
	 }
	 
	 
	 //	Override of Object.toString().
	 public String toString(){
	 	return "DisplayedState for " + iState.toString();
	 }
}