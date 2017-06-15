//	GridPane.java - Draw the background grid for a timeline.

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.JComponent;
import javax.swing.event.*;


class GridPane extends JComponent implements ChangeListener {

	// 	Constants------------------------------------------------------------------------
	//	Constants determining pane layout.
	static final protected int HEADER_HEIGHT = 20;					//	Height of header pane
	static final protected int MAJOR_LEGEND_HEIGHT = 10;			//	Height of major legend in header
	static final protected int MAJOR_LEGEND_SPACING = 10;  		//	Distance from grid to start of text
	static final protected int MAJOR_LEGEND_Y_POSTION = 10;		//	Y coordinate of text baseline
	static final protected int MINOR_LEGEND_SPACING = 4;			//	Distance from grid to start of text
	static final protected int MINOR_LEGEND_Y_POSITION = 18;		// 	Y coordinate of text baseline

	//	Constants determining the font we use.
	static final protected String FONT_NAME = "SansSerif";
	static final protected int FONT_STYLE = java.awt.Font.PLAIN;
	static final protected int FONT_SIZE = 9;

	//	Useful colors that are not defined in java.awt.Color.
	static final protected Color DARK_CYAN = new Color(0, 128, 128);

	//	Constants determining pane colors.
	//	??	BACKGROUND_COLOR temporarily public for DisplayedState.java.
	static final public Color BACKGROUND_COLOR = new Color(220, 220, 220);	//	Color of the background
	static final protected Color MINOR_GRID_COLOR = Color.cyan;		//	Color of the grid minor divisions
	static final protected Color MAJOR_GRID_COLOR = DARK_CYAN;	//	Color of the grid major divisions
	
	//	Time/date formatters.
 	SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
 	SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMMM, yyyy");
 	SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMMMM");
 	SimpleDateFormat monthDayYearFormat = new SimpleDateFormat("EEEE, MMMMM d, yyyy");
 	SimpleDateFormat MONTH_DAY_FORMAT = new SimpleDateFormat("EEEE, MMMMM d");
 	SimpleDateFormat hourMonthYearFormat = new SimpleDateFormat("MMMMM, yyyy");
	
	//	Conversion tables.
	static final protected String[] MONTH_NAMES = {
		"January",	"February", "March", "April", "May", "June", "July",
		"August", "September", "October", "November", "December"
	};
	static final protected String[] MONTH_ABRIEVIATIONS = {
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul",
		"Aug", "Sep", "Oct", "Nov", "Dec"
	};
	static final protected String[] DAY_OF_WEEK_ABRIEVIATIONS = {
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
	};


	//	Instance variables----------------------------------------------------------------
	protected TLDocument iDoc;					//	Document containing our data
	protected TimePositionMapping iTPM;			//	Maps time to space on this pane
	protected boolean iIsHeaderPane;				//	This instance is for drawing headers


	//	Constructor---------------------------------------------------------------------
	//	If "header" is true, then this instance of GridPane is in the header part of the JScrollPane.
	public GridPane(TLDocument itsDoc, TimePositionMapping tpm, boolean header){
		iDoc = itsDoc;
		iTPM = tpm;
		iIsHeaderPane = header;
		
		//	Set up our relationships with other object.
		iTPM.addChangeListener(this);

		//	Initialize our properties as a Swing component.
		this.setOpaque(true);
		this.setBackground(BACKGROUND_COLOR);
		this.setFont(new Font(FONT_NAME, FONT_STYLE, FONT_SIZE));
		this.updatePaneSize();
	}


	//	Draw this component.
	 protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Dimension size = this.getSize();
		
		//	Fill the background.
		//	??	Can we skip this if we announce we're opaque?
		g.setColor(this.getBackground());
		g.fillRect(0, 0, size.width, size.height);
		
		//	Draw grid.
		long startTime = System.currentTimeMillis();
		this.drawGrid(g);
		if (Debug.DISPLAY_TIMINGS)
			System.out.println((System.currentTimeMillis() - startTime) + " mS. to paint GridPane");
	 }


	 //	Draw the time unit grid.
	 protected void drawGrid(Graphics g){
	 
	 	boolean isCyclicView = iTPM.isCyclicView();
	 	
		//	Start drawing the grid at the left side of the clip rectangle.
	        Rectangle clipRect = g.getClipBounds();
	 	CustomGregorianCalendar startOfGrid = new CustomGregorianCalendar();
	 	startOfGrid.setTimeInMillis(iTPM.xPositionToTime(clipRect.x));

	 	//	End drawing the grid just past the end of the clip rectangle.
	 	CustomGregorianCalendar endOfGrid = new CustomGregorianCalendar();
	 	endOfGrid.setTimeInMillis(iTPM.xPositionToTime(clipRect.x + clipRect.width));

	 	//	Draw the minor divisions of the grid.
	 	int scale = iTPM.getScale();
	 	this.drawMinorGridDivisions(g, scale, startOfGrid, endOfGrid);

	 	//	Only draw major divisions if there is a time unit larger than the current scale.
	 	if (scale < TimeUnit.MAX_VALUE){
	 		this.drawMajorGridDivisions(g, scale, startOfGrid, endOfGrid, isCyclicView);
	 	}
	 }
	 
	 
	 //	Array used by drawMinorGridDivisions() and drawMajorGridDivisions() to determine which 
	 //	Calendar field should be incremented to advance to the next grid line.
	 protected static final int[] FIELD_TO_INCREMENT = {
	 		Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, Calendar.DAY_OF_MONTH,
	 		Calendar.WEEK_OF_YEAR, Calendar.MONTH, Calendar.YEAR
	 };


	 //	Draw the minor divisions of the grid, along with their legend.
	 protected void drawMinorGridDivisions(Graphics g, int scale, CustomGregorianCalendar startOfGrid, 
	 				CustomGregorianCalendar endOfGrid){

	 	//	Align the grid with the boundary between two scale units.  
	 	CustomGregorianCalendar curMoment = new CustomGregorianCalendar(startOfGrid);
	 	curMoment.truncateToLower(scale);

	 	//	Once around this loop for each minor grid line.
		int height = this.getHeight();
		int startingY = (iIsHeaderPane) ? MAJOR_LEGEND_HEIGHT : 0;
		int fieldToIncrement = FIELD_TO_INCREMENT[scale];
	 	while (curMoment.before(endOfGrid)){

	 		//	Compute where the line is and draw it.
	 		int x = iTPM.timeToXPosition(curMoment.getTimeInMillis());
		 	g.setColor(MINOR_GRID_COLOR);
	 		g.drawLine(x, startingY, x, height);

	 		//	If we're doing header, draw appropriate legend.
	 		//	??	We should be using java.text.NumberFormat to handle the leading zeros.
		        if (iIsHeaderPane){
		 		int legendValue;
		 		String legendString = null;
		 		switch (scale){
					case TimeUnit.YEAR:
						legendValue = curMoment.get(Calendar.YEAR);
						legendString = Integer.toString(legendValue % 100);
						if (legendString.length() == 1)
							legendString = "0" + legendString;
						break;

					case TimeUnit.MONTH:
						legendValue = curMoment.get(Calendar.MONTH);
						legendString = MONTH_ABRIEVIATIONS[legendValue];
						break;

					case  TimeUnit.WEEK:
						legendString = Integer.toString(curMoment.get(Calendar.DATE));
						if (legendString.length() == 1)
							legendString = " " + legendString;
						break;

					case TimeUnit.DAY:
						legendValue = curMoment.get(Calendar.DAY_OF_MONTH);
						legendString = Integer.toString(legendValue);
						if (legendString.length() == 1)
							legendString = " " + legendString;
						break;

					case TimeUnit.HOUR:
						legendValue = curMoment.get(Calendar.HOUR);
						legendString = Integer.toString(legendValue);
						if (legendString.length() == 1)
							legendString = "0" + legendString;
						break;

					case TimeUnit.MINUTE:
						legendValue = curMoment.get(Calendar.MINUTE);
						legendString = Integer.toString(legendValue);
						if (legendString.length() == 1)
							legendString = "0" + legendString;
						break;

					case TimeUnit.SECOND:
						legendValue = curMoment.get(Calendar.SECOND);
						legendString = Integer.toString(legendValue);
						if (legendString.length() == 1)
							legendString = "0" + legendString;
						break;
			 	}
			 	g.setColor(Color.black);
			 	g.drawString(legendString, x + MINOR_LEGEND_SPACING, MINOR_LEGEND_Y_POSITION);
	 		}
	 		
	 		//	Advance to the time of the next line.
	 		curMoment.add(fieldToIncrement, 1);
	 	}
	 }


	 //	Draw the major divisions of the grid, along with their legend.
	 protected void drawMajorGridDivisions(Graphics g, int scale, CustomGregorianCalendar startOfGrid, 
	 								CustomGregorianCalendar endOfGrid, boolean isCyclicView){

	 	//	Align the grid with the boundary between two major division units.  
	 	CustomGregorianCalendar curMoment = new CustomGregorianCalendar(startOfGrid);
	 	curMoment.truncateToLower(scale + 1);

 		//	Delta between major divisions is one TimeUnit larger than scale.
 		int nextLargerUnit = scale + 1;
		int fieldToIncrement = FIELD_TO_INCREMENT[nextLargerUnit];

	 	//	Once around this loop for each major grid line.
	 	g.setColor(MAJOR_GRID_COLOR);
		int height = this.getHeight();
	 	while (curMoment.before(endOfGrid)){

	 		//	Compute where the line is and draw it.
	 		int x = iTPM.timeToXPosition(curMoment.getTimeInMillis());
	 		g.drawLine(x, 0, x, height);

	 		//	If we're doing header, draw appropriate legend.
		        if (iIsHeaderPane){
		 		SimpleDateFormat format = null;
		 		switch (nextLargerUnit){
					case TimeUnit.YEAR:
						format = yearFormat;
						break;
	
					case TimeUnit.MONTH:
					case TimeUnit.WEEK:
						format = (isCyclicView) ? MONTH_FORMAT : monthYearFormat;
						break;
											
					case TimeUnit.DAY:
						format = (isCyclicView) ? MONTH_DAY_FORMAT : monthDayYearFormat;
						break;
												
					case TimeUnit.HOUR:
						break;

					case TimeUnit.MINUTE:
					case TimeUnit.SECOND:
						break;
			 	}
			 	if (format != null){
				 	g.setColor(Color.black);
				 	String legendString = format.format(curMoment.getTime());
				 	g.drawString(legendString, x + MAJOR_LEGEND_SPACING, MAJOR_LEGEND_Y_POSTION);
				 }
	 		}

	 		//	Advance to the time of the next line.
	 		curMoment.add(fieldToIncrement, 1);
	 	}
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
		//	Always specify a pane height of HEADER_HEIGHT, even if this is the timeline pane.  This works
		//	because OverlayLayout takes the biggest of the overlayed panes, so the value specified by
		//	TimelinePane overrides the value we specify.
		int paneWidth = iTPM.getTimelineWidth();
		this.setPreferredSize(new Dimension(paneWidth, HEADER_HEIGHT));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		//	Ask the layout manager to update our size.
		this.revalidate();
	}
}