//	TimePositionMapping.java - Represent the relationship between time and position on a timeline pane.

//	There are two elements to the time/position mapping:  the scaling factor, and the mapping period.
//	The scaling factor defines how many milleseconds map to each pixel, and the mapping period defines the
//	range of times over which the mapping is valid.
//	(Technically, only the starting time of the mapping period is actually used (to define the graphics origin),
//	but it is clearer to think about a period.)
//	
//	The mapping period computed as the document's time range, plus an optional "extra" range which is specified by the
//	window through one of the ensureIncludedInMappedTimePeriod() methods.  Once an extra range is specified
//	in this way, it remains in effect until a different one is requested, or the document's time range changes.
//	It is possible, although rare, for there not to be a mapping at all.  This occurs, for example, when a blank document
//	(which doesn't have a time range) is created.  However, as soon as a window is created to display this document,
//	the mapping period is created to cover the width of the window.
//
//	The document notifies us if its time range changes, by calling documentTimeRangeChanged().
//	The window notifies us if its scale changes, by calling setScale().  In either case, we notify the timeline,
//	grid and drag panes of the change through the callbacks they've registered via addChangeListener().

import java.util.*;
import javax.swing.event.*;


public class TimePositionMapping extends Object {

	// 	Constants------------------------------------------------------------------------
	public static final int SCALE_UNIT_SIZE = 20;	//	Width of the current scale unit.
	public static final long MILLIS_INCLUDING_FEBRUARY28 = (new Date(76, 2, 29)).getTime() -
											(new Date(76, 0, 1)).getTime();
	
	//	The number of extra units (of the current scale) which are allowed for as "margin" at the
	//	beginning and ending of the timeline.
	public static final int END_MARGIN = 3;

	//	A table that maps time units (as defined by the constants defined in TimeUnit) into
	//	the typical number of milleseconds in the unit.  N.B. These are typical values;  some of the
	//	units (such as month and year) have a varying size.
	protected static final long[] APPROX_MILLISECONDS_IN_UNIT = {	
		1000,							//	SECOND
		60 * 1000,						//	MINUTE
		60 * 60 * 1000,					//	HOUR
		24 * 60 * 60 * 1000,				//	DAY
		7 * 24 * 60 * 60 * 1000,				//	WEEK
		31L * 24 * 60 * 60 * 1000,			//	MONTH
		365L * 24 * 60 * 60 * 1000,			//	YEAR
	};

	//	Instance variables.
	protected TLDocument iDoc;					//	Document containing our data
	protected TLWindow iWindow;				//	Window we're contained in
	protected int iScale;						//	Current scale
	protected TimePeriod iMappedPeriod;			//	Time period covered by mapping, or null
	protected boolean iCyclicView;				//	Use cyclic form for view
	protected long[] iCyclicBoundaries;			//	Start of each year enclosing mapped period, in millis
	protected boolean [] iLeapYears;				//	For each entry in iCyclicBoundaries, is it a leap year?

	//	These two variables define the mapping from time (milliseconds) to horizontal position.
	//	iOriginMillis is really a cache of iMappedPeriod.getPeriodStart();
	//	The document notifies us, by calling updateTimePositionMapping(), whenever the
	//	document's time range changes.
	protected long iOriginMillis;				//	Moment represented by the origin
	protected long iMilliToPixelRatio;				//	Ratio of millis to screen pixels

	/**
	* Only one ChangeEvent is needed per TPM since the
	* event's only (read-only) state is the source property.  The source
	* of events generated here is always "this".
	*/
	protected transient ChangeEvent iChangeEvent = null;
	protected EventListenerList iListenerList = new EventListenerList();


	//	Constructor.
	public TimePositionMapping(TLDocument itsDoc, TLWindow itsWindow, int scale, boolean cyclic){
		iDoc = itsDoc;
		iWindow = itsWindow;
		iScale = scale;
		iCyclicView = cyclic;
		
		//	Initialize our period to be that covered by the document.  Note that this may be
		//	null if the document is empty.  
		iMappedPeriod = iDoc.getDocTimePeriod();

		//	Compute the mapping between time and space.
		computeMilliToPixel();
		if (iMappedPeriod != null)
			computeTimePositionMapping();
	}
	
	
	//	Trivial accessors.
	public boolean isCyclicView()			{	return iCyclicView;		}
	
	
	//	Called (by TLDocument via TLWindow) when the document time range changes.
	public void documentTimeRangeChanged(){
		
		//	Our mapped period is the cover of the document range, and the
		//	period currently visible in the window.
		ensureIncludedInMappedTimePeriod(iWindow.getVisiblePeriod());
	}
	
	
	//	Ensure that the passed time period is included in the mapped time period.
	public void ensureIncludedInMappedTimePeriod(TimePeriod tr){
		TimePeriod docRange = iDoc.getDocTimePeriod();
		TimePeriod newMapRange;
		if (docRange == null)
			newMapRange = tr;
		else
			newMapRange = docRange.cover(tr);
		if (!newMapRange.equals(iMappedPeriod)){
			iMappedPeriod = newMapRange;
			computeTimePositionMapping();
		}
	}
	
	
	//	This calculates iMilliToPixelRatio, which is part of the time/position mapping.
	protected void computeMilliToPixel(){
		//	Determine the typical number of milliseconds occupied by one scale unit.
	
		//	Note that some instances of this scale unit may
		//	be a different length, and thus will be slightly different in 
		//	size on the screen.
		long millisecondsInScaleUnit = APPROX_MILLISECONDS_IN_UNIT[iScale];
			
		//	Compute the number of milliseconds that must be represented by one pixel,
		//	in order for the scale unit to be (on average) SCALE_UNIT_SIZE wide.
		iMilliToPixelRatio = (millisecondsInScaleUnit / SCALE_UNIT_SIZE) + 1;
	}
	
	
	//	This must be called when any value that the mapping depends upon is changed.  At present,
	//	these are the TPM time range and the scale.
	protected void computeTimePositionMapping(){
	
		//	A mapped period must be determined before this method can function.
		//	Verify that this has been done.
		Debug.assertOnError(iMappedPeriod != null);
		
		//	Determine the typical number of milliseconds occupied by one scale unit.
		computeMilliToPixel();
				
		if (iCyclicView){
			
			//	Build the cyclic boundary array.  To make finding the appropriate entry as
			//	simple as possible, we build the array so that it "encloses" the mapped period.
			//	The first entry in the array is the start of the nearest leap year before the start of the 
			//	mapped period;  the last entry in the array is the start of the year _after_ the
			//	end of the mapped period.
			CustomGregorianCalendar cal = new CustomGregorianCalendar();
			
			//	Find the years in which the mapped period starts and ends.
			cal.setTimeInMillis(iMappedPeriod.getPeriodStart());
			cal.truncateToLower(Calendar.YEAR);
			int startingYear = cal.get(Calendar.YEAR);

			cal.clear();
			cal.setTimeInMillis(iMappedPeriod.getPeriodEnd());
			cal.truncateToLower(Calendar.YEAR);
			cal.roll(Calendar.YEAR, true);
			int endingYear = cal.get(Calendar.YEAR);
			
			//	Find the nearest year at or before the starting year of the mapped period
			//	which is a leap year.
			//	Compute the length of the array.
			while (!cal.isLeapYear(startingYear))
				--startingYear;
			int arraySize = endingYear - startingYear + 1;
			
			//	Allocate the arrays.
			iCyclicBoundaries = new long[arraySize];
			iLeapYears = new boolean[arraySize];
			
			//	Fill in the array.
			cal.clear();
			cal.set(startingYear, 0, 1);
			cal.truncateToLower(Calendar.YEAR);
			for (int i = 0; i < arraySize; i++){
				iCyclicBoundaries[i] = cal.getTimeInMillis();
				iLeapYears[i] = cal.isLeapYear(cal.get(Calendar.YEAR));
				cal.roll(Calendar.YEAR, true);
			}
			iOriginMillis = iCyclicBoundaries[0];
		}
		else {
			//	Compute the origin moment, as expressed in millis, as the lower bound of the
			//	TPM period, truncated down to the next lower whole scale unit.
			CustomGregorianCalendar temp = new CustomGregorianCalendar();
			long startTime = iMappedPeriod.getPeriodStart();
			long startWithMargin = startTime - END_MARGIN * APPROX_MILLISECONDS_IN_UNIT[iScale];
			temp.setTimeInMillis(startWithMargin);
			temp.truncateToLower(iScale);
			iOriginMillis = temp.getTimeInMillis();
		}
		
		//	Notify all panes that the mapping has changed.
		fireStateChanged();
	}
	
	
	//	Return the width of the timeline, in pixels.  This width is zero if the mapped period is not defined.
	public int getTimelineWidth(){
		if (iMappedPeriod != null){
			if (iCyclicView)
				return timeDeltaToXDelta(APPROX_MILLISECONDS_IN_UNIT[TimeUnit.YEAR] +
								APPROX_MILLISECONDS_IN_UNIT[TimeUnit.DAY]);
			else {
				long endTime = iMappedPeriod.getPeriodEnd();
				long endWithMargin = endTime + END_MARGIN * APPROX_MILLISECONDS_IN_UNIT[iScale];
				return timeToXPosition(endWithMargin);
			}
		}
		else
			return 0;
	}


	//	Translate a moment in time (expressed in millis) into a horizontal location in the pane 
	//	(expressed in drawing coordinates).
	public int timeToXPosition(long millis){
		long value;
		if (!iCyclicView){
			value = (millis - iOriginMillis)/iMilliToPixelRatio;
			if (Debug.sCurLevel > 0)
				Debug.assertOnError((value > 0) && (value < Integer.MAX_VALUE));
		}
		else {
			//	Search through boundary array, until we find the start of the year containing this time.
			//	??	Could do a binary search and/or cache the last value found.
			int i;
			for (i = 0;  i <  iCyclicBoundaries.length - 1; i++){
				if (iCyclicBoundaries[i] <= millis && millis < iCyclicBoundaries[i+1])
					break;
				Debug.assertOnError( i < iCyclicBoundaries.length - 1);
			}
			long millisSinceStartOfYear = millis - iCyclicBoundaries[i];
			
			//	Adjust for leap years.  The cyclic view is always a leap year.  If the time we're converting
			//	is not in a leap year, and it is after February 28, add in one day, so that, for example,
			//	July 1 in the source year converts to July 1 in the target yet.
			if (!iLeapYears[i] & millisSinceStartOfYear >= MILLIS_INCLUDING_FEBRUARY28)
				millisSinceStartOfYear += APPROX_MILLISECONDS_IN_UNIT[TimeUnit.DAY];
			
			//	Compute the final X offset in pixels.
			value = (millisSinceStartOfYear)/iMilliToPixelRatio;
		}
		
		return (int)value;
	}


	//	Translate a horizontal position in the pane (in drawing coordinates) into a moment of time
	//	(in millis).
	public long xPositionToTime(int x){
		return ((long)x * iMilliToPixelRatio) + iOriginMillis;
	}


	//	Translate a horizontal delta (in pixels) into a time delta (in millis).
	public long xDeltaToTimeDelta(int d){
		return (long)d * iMilliToPixelRatio;
	}


	//	Translate a time delta (in millis) into a hoirizontal delta (in pixels).
	public int timeDeltaToXDelta(long d){
		return (int)(d / iMilliToPixelRatio);
	}


	//	Get this pane's scale.
	public int getScale(){
		return iScale;
	}
	
	
	//	Change this pane's scale.
	public void setScale(int newScale){
		iScale = newScale;
		computeTimePositionMapping();
	}
	
	
	//	Change whether the timeline is displayed in cyclic form.
	public void setCyclicView(boolean cyclic){
		iCyclicView = cyclic;
		computeTimePositionMapping();
	}
	
	
	//	Return the time period supported by this mapping.
	public TimePeriod getMappedPeriod(){
		return iMappedPeriod;
	}
	
	
	    /**
     * Adds a ChangeListener.  The change listeners are run each
     * time any one of the Bounded Range model properties changes.
     *
     * @param l the ChangeListener to add
     * @see #removeChangeListener
     */
    public void addChangeListener(ChangeListener l) {
        iListenerList.add(ChangeListener.class, l);
    }
    

    /**
     * Removes a ChangeListener.
     *
     * @param l the ChangeListener to remove
     * @see #addChangeListener
     */
    public void removeChangeListener(ChangeListener l) {
        iListenerList.remove(ChangeListener.class, l);
    }


    /** 
     * Run each ChangeListeners stateChanged() method.
     * 
     * @see #setRangeProperties
     * @see EventListenerList
     */
    protected void fireStateChanged() 
    {
        Object[] listeners = iListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -=2 ) {
            if (listeners[i] == ChangeListener.class) {
                if (iChangeEvent == null) {
                    iChangeEvent = new ChangeEvent(this);
                }
                ((ChangeListener)listeners[i+1]).stateChanged(iChangeEvent);
            }          
        }
    }   

    

}