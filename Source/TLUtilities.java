//	TLUtilities.java - Miscellaneous useful methods.

import java.text.DateFormat;
import java.util.*;

public class TLUtilities extends Object {
 
 	//	Constants---------------------------------------------------------------------------------------
 	//	A Comparator that sorts Lists containing TimePeriods into ascending order by the starting moment.
	//	These would be faster if we could just return the difference between the two values,
	//	but Comparator.compare() is defined to return an int, not a long.
 	public static final Comparator SORT_UP_BY_START_COMPARATOR = new Comparator(){
			public int compare(Object o1, Object o2){
				long i = ((TimePeriod)o1).getPeriodStart() - ((TimePeriod)o2).getPeriodStart();
				if (i < 0)
					return -1;
				else if (i == 0)
					return 0;
				else
					return 1;
			}
		};

 	//	A Comparator that sorts Lists containing TimePeriods into decending order by the ending moment.
	//	These would be faster if we could just return the difference between the two values,
	//	but Comparator.compare() is defined to return an int, not a long.
	public static final Comparator SORT_DOWN_BY_ENDING_COMPARATOR = new Comparator(){
			public int compare(Object o1, Object o2){
				long i = ((TimePeriod)o2).getPeriodEnd() - ((TimePeriod)o1).getPeriodEnd();
				if (i < 0)
					return -1;
				else if (i == 0)
					return 0;
				else
					return 1;
			}
		};	
		
 	//	A Comparator that sorts Lists containing DisplayStates into descending order by duration
 	//	of the associated TLState.
	//	These would be faster if we could just return the difference between the two values,
	//	but Comparator.compare() is defined to return an int, not a long.
	public static final Comparator SORT_DOWN_BY_DURATION_COMPARATOR = new Comparator(){
			public int compare(Object o1, Object o2){
				TLState s1 = ((DisplayedState)o1).getState();
				TLState s2 = ((DisplayedState)o2).getState();
				long i = s2.getDuration() - s1.getDuration();
				if (i < 0)
					return -1;
				else if (i == 0)
					return 0;
				else
					return 1;
			}
		};	
		
 	//	A Comparator that sorts Lists containing DisplayObjects into ascending order by their x position.
	//	These would be faster if we could just return the difference between the two values,
	//	but Comparator.compare() is defined to return an int, not a long.
 	public static final Comparator SORT_UP_BY_X_POSITION = new Comparator(){
			public int compare(Object o1, Object o2){
				return ((DisplayedObject)o1).getXLocation() - ((DisplayedObject)o2).getXLocation();
			}
		};
	
	
	//	Snap a millisecond value to the specified unit.
	//	Helper method for editing methods.
	//	WARNING - For performance reasons, this uses a statically-allocated object.
	//	WARNING - Thus, it is not thread-safe.
	protected static CustomGregorianCalendar sCalendar = new CustomGregorianCalendar();
	
	public static long snapMillisToUnit(long inValue, int unit){
		sCalendar.setTimeInMillis(inValue);
		sCalendar.roundToNearest(unit);
		return sCalendar.getTimeInMillis();
	}


	//	Static DateFormats used by the various format methods.
	protected static final DateFormat SIMPLE_DATE_FORMAT = 
				DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	protected static final DateFormat FULL_DATE_FORMAT = 
				DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);
	
	
	//	Convert a Calendar object into a string in a standard format.  Meant for debugging use, so 
	//	performance was not considered in  the design.
	public static String formatSimpleDate(Calendar c){
		return SIMPLE_DATE_FORMAT.format(c.getTime());
	}
 
	public static String formatFullDate(Calendar c){
		return FULL_DATE_FORMAT.format(c.getTime());
	}
 
 
	//	Convert a millisecond time/date value into a string in a standard format.  Meant for debugging use, so
	//	performance was not considered in  the design.
	public static String formatSimpleDate(long m){
		return SIMPLE_DATE_FORMAT.format(new Date(m));
	}
 
	public static String formatFullDate(long m){
		return FULL_DATE_FORMAT.format(new Date(m));
	}
 
 
 }