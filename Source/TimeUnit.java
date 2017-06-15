//	TimeUnit.java - Represent a time unit (hour, month, etc.)import java.util.Calendar;interface TimeUnit  {	//	Define names for the different time units.	//	These values are used in several places to index arrays, so they should not	//	be arbitrarily changed.	public static final int YEAR 		= 6;	public static final int MONTH 	= 5;	public static final int WEEK 		= 4;	public static final int DAY 		= 3;	public static final int HOUR		= 2;	public static final int MINUTE 	= 1;	public static final int SECOND	= 0;		public  static final String[] UNITNAMES = {"second",  "minute", "hour", "day", "week", "month", "year"};			//	Define max and min in semantic terms;  i.e., a year is larger than a month.	public static final int MAX_VALUE = YEAR;	public static final int MIN_VALUE = SECOND;}