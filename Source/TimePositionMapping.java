//	TimePositionMapping.java - Represent the relationship between time and position on a timeline pane.

//  A TPM converts between time and pixel counts for a particular window, which is primarily
//  a function of the window's scaling factor.  The scaling factor defines how many milleseconds 
//	map to each pixel, and the mapping period defines the range of times over which the mapping is valid.
//	(Technically, only the starting time of the mapping period is actually used (to define the graphics origin),
//	but it is clearer to think about a period.)

//  A TPM also keeps track of the amount of time in the 
//  window in variable iMappedPeriod, which starts out as the document's contained period, but which 
//  can be expanded with ensureIncludedInMappedTimePeriod() if the user scrolls or jumps past 
//  either end of that period.  Once an extra range is specified
//	in this way, it remains in effect until a different one is requested, or the document's time range changes.
//	It is possible, although rare, for there not to be a mapping at all.  This occurs, for example, when a blank document
//	(which doesn't have a time range) is created.  However, as soon as a window is created to display this document,
//	the mapping period is created to cover the width of the window.
//
//	The document notifies us if its time range changes, by calling documentTimeRangeChanged().
//	The window notifies us if its scale changes, by calling setScale().  In either case, we notify the timeline,
//	grid and drag panes of the change through the callbacks they've registered via addChangeListener().
//
//  Cyclic view shows a single year, with each state positioned according to its distance from the first moment 
//  of the year containing it.  Leap years are not handled specially.  A state that occurs after February 30 in a 
//  leap year, displayed in a non-leap-year year, will be shifted one calendar day later than the date in the 
//  state.  A state the occurs after February 30 in a non-leap-year year will be displayed in a leap-year year
//  one calendar day earlier.

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
		1000,									//	SECOND
		60 * 1000,								//	MINUTE
		60 * 60 * 1000,							//	HOUR
		24 * 60 * 60 * 1000,					//	DAY
		7 * 24 * 60 * 60 * 1000,				//	WEEK
		31L * 24 * 60 * 60 * 1000,				//	MONTH
		365L * 24 * 60 * 60 * 1000,				//	YEAR
	};

	//	Instance variables.
	protected TLDocument iDoc;					//	Document containing our data
	protected TLWindow iWindow;					//	Window we're contained in
	protected int iScale;						//	Current scale
	protected TimePeriod iMappedPeriod;			//	Time period covered by mapping, or null
												//  This is usually the period contained in the document,
												//  but can be extended at either end if the user scrolls
												//  past an end of the document.  See ensureIncludedInMappedTimePeriod()
	protected boolean iCyclicView;				//	Use cyclic form for view
	protected long[] iCyclicYearStarts;			//	Start of each year enclosing mapped period, in millis

	//	These two variables define the mapping from time (milliseconds) to horizontal position.
	//	iOriginMillis is really a cache of iMappedPeriod.getPeriodStart();
	//	The document notifies us, by calling updateTimePositionMapping(), whenever the
	//	document's time range changes.
	protected long iOriginMillis;				//	Moment represented by the origin
	protected long iMilliToPixelRatio;			//	Ratio of millis to screen pixels

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

		if (iMappedPeriod != null){
			CustomGregorianCalendar cal = new CustomGregorianCalendar();
			cal.setTimeInMillis(iMappedPeriod.getPeriodStart());
			Debug.log(Debug.DETAIL, "Document start: " + cal.toZonedDateTime().toString());
			cal.clear();
			cal.setTimeInMillis(iMappedPeriod.getPeriodEnd());
			Debug.log(Debug.DETAIL, "Document end: " + cal.toZonedDateTime().toString());
		}
		
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
    //  ??  This gets called to ensure iMappedPeriod includes space beyond the
   	//  ??  beginning or end of the document that's visible in the TimelinePane.
   	//  ??  But it seems like this function is needlessly obtuse.
	public void ensureIncludedInMappedTimePeriod(TimePeriod tr){
		TimePeriod docRange = iDoc.getDocTimePeriod();
		TimePeriod newMapRange;
		if (docRange == null)
			newMapRange = tr;
		else
			newMapRange = docRange.cover(tr);
		if (!newMapRange.equals(iMappedPeriod)){
			iMappedPeriod = newMapRange;
			CustomGregorianCalendar cal = new CustomGregorianCalendar();
			cal.setTimeInMillis(iMappedPeriod.getPeriodStart());
			Debug.log(Debug.DETAIL, "Document extended start: " + cal.toZonedDateTime().toString());
			cal.clear();
			cal.setTimeInMillis(iMappedPeriod.getPeriodEnd());
			Debug.log(Debug.DETAIL, "Document extended end: " + cal.toZonedDateTime().toString());

			computeTimePositionMapping();
		}
	}
	
	
	//	This calculates iMilliToPixelRatio, which is part of the time/position mapping.
	//  ??  This could be pre-computed.
	protected void  computeMilliToPixel(){
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
	//	these are the TPM time range, the scale and cyclic mode.
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
			TimePeriod docRange = iDoc.getDocTimePeriod();
			CustomGregorianCalendar cal = new CustomGregorianCalendar();
			
			//	Find the years in which the mapped period starts and ends.
			cal.setTimeInMillis(docRange.getPeriodStart());
			cal.truncateToLower(Calendar.YEAR);
			int startingYear = cal.get(Calendar.YEAR);
			Debug.log(Debug.DETAIL, "docRange start: " + cal.toZonedDateTime().toString());

			cal.clear();
			cal.setTimeInMillis(docRange.getPeriodEnd());
			cal.truncateToLower(Calendar.YEAR);
			cal.roll(Calendar.YEAR, true);
			Debug.log(Debug.DETAIL, "docRange end: " + cal.toZonedDateTime().toString());
			int endingYear = cal.get(Calendar.YEAR);
			
			//	Find the nearest year at or before the starting year of the mapped period
			//	which is a leap year.
			//	Compute the length of the array.
			while (!cal.isLeapYear(startingYear))
				--startingYear;
			int arraySize = endingYear - startingYear + 1;
			
			//	Allocate the arrays.
			iCyclicYearStarts = new long[arraySize];
			
			//	Fill in the array.
			cal.clear();
			cal.set(startingYear, 0, 1);
			cal.truncateToLower(Calendar.YEAR);
			for (int i = 0; i < arraySize; i++){
				iCyclicYearStarts[i] = cal.getTimeInMillis();
				cal.roll(Calendar.YEAR, true);
			}
			
			//  The origin of the window is one day before the first moment in the year specified by the user.
			//  ??  The year is hardwired for testing.
			cal.clear();
			cal.set(2022, 0, 1);   ///////////////////////////
			Debug.log(Debug.DETAIL, "Origin date: " + cal.toZonedDateTime().toString());
			iOriginMillis = cal.getTimeInMillis();
			iMappedPeriod = new ConcreteTimePeriod(cal.getTimeInMillis(), 
										cal.getTimeInMillis() + APPROX_MILLISECONDS_IN_UNIT[TimeUnit.YEAR]);
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
	//  Note that this may be extended past the actual period contained in this document, to show
	//  time before or after the document period visible in the TimelinePane.  See ensureIncludedInMappedTimePeriod().
	//  ??  This handling of time before or after the document period seems needlessly complex.
	public int getTimelineWidth(){
		if (iMappedPeriod != null){
			if (iCyclicView)
				return timeDeltaToXDelta(APPROX_MILLISECONDS_IN_UNIT[TimeUnit.YEAR]);
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
		if (!iCyclicView)
			value = (millis - iOriginMillis)/iMilliToPixelRatio;
		else {
			//	Search through boundary array, until we find the start of the year containing this time.
			//	??	Could do a binary search and/or cache the last value found.
			int i;
			for (i = 0;  i <  iCyclicYearStarts.length - 1; i++){
				if (iCyclicYearStarts[i] <= millis && millis < iCyclicYearStarts[i+1])
					break;
				Debug.assertOnError( i < iCyclicYearStarts.length - 1);
			}
			long millisSinceStartOfYear = millis - iCyclicYearStarts[i];
			
			//	Compute the final X offset in pixels.
			value = millisSinceStartOfYear/iMilliToPixelRatio;
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