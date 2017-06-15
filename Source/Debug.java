//	Debug.java - Debugging tools and utilities.


import java.io.*;


public class Debug {

	//	Public constants.
	//	Debug levels;  primiarily for use in log() calls.
	public static final int INTERNAL_FAILURES = 0;	
	public static final int UNUSUAL_EVENT = 1;	
	public static final int USER_VISIBLE_EVENT = 2;
	public static final int MAJOR_PROCESSING_STEP = 3;	
	public static final int MINOR_PROCESSING_STEP = 4;
	public static final int DETAIL = 5;
	public static final int EXTREME_DETAIL = 6;
	
	//	Public variables.
	public static PrintStream stream = null;	//	Stream for debugging info.
	public static int sCurLevel;				//	Current debug sCurLevel

	//	Names of properties that control the debugging mode.
	protected final static String DEBUG_LEVEL_PROPERTY = "debug_level";
	protected final static String DEBUG_FILE_PROPERTY = "debug_file";
	
	//	The debug sCurLevel that applies if property DEBUG_LEVEL_PROPERTY is not defined.
	protected static final int DEFAULT_DEBUG_LEVEL = -1;
	
	//	Set DISPLAY_TIMINGS to true to put elapsed time messages into System.out.
	public static final boolean DISPLAY_TIMINGS = false;


	//	Make the constructor private, so that no one can instantiate this class.
	private Debug() {}


	//	Static initialization code.
	//	Open the debug stream, as determined by the compile-time options.
	static {
			
		//	Read the properties that control our operation.
		sCurLevel = Integer.getInteger(DEBUG_LEVEL_PROPERTY, DEFAULT_DEBUG_LEVEL).intValue();
		if (sCurLevel >= 0){
			String debugFilename = System.getProperty(DEBUG_FILE_PROPERTY);
			if (debugFilename != null){
				try {
					stream = new PrintStream(new BufferedOutputStream(
												new FileOutputStream(debugFilename)), true);
				}
				catch (IOException e){
					//	Ignore exceptions, leaving stream null.
					System.err.println("Could not open debugging stream because " + 
													e.toString());
				}
			}
			else {
				stream = System.err;
			}
		}
	}


	//	Include a message in the debugging log if the current debug sCurLevel is equal or greater than the specified sCurLevel.
	static void log(int minLevel, String msg){
		if (sCurLevel >= minLevel){
			stream.println(msg);
			if (stream != null){
				stream.flush();
			}
		}
	}


	//	Version of log that doesn't include a minimum level;  for backward compatibility only.
	static void log(String msg){
		log(MINOR_PROCESSING_STEP, msg);
	}


	public final static void assert(boolean b) {
		if (!b){
			log(UNUSUAL_EVENT, "Assertion failed");
			throw new Error("Assertion failed");
		}
	}


	public final static void assert(boolean b, String msg) {
		if (!b){
			log(UNUSUAL_EVENT, "Assertion failed:  " + msg);
			throw new Error(msg);
		}
	}
}