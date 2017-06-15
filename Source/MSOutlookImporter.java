//	MSOutlookImporter.java - Import states from a Microsoft Outlook export file.

//	The fields in the tab-delimited file are assumed to be in the order provided by Outlook 97 
//	as the default.

//	7/18/01		Trim leading and trailing blanks from labels and comments, and ignore the event
//				if its label is blank.

import java.io.*;
import java.text.*;
import java.util.*;

class MSOutlookImporter extends Object implements Importer {
	
	// 	Constants ----------------------------------------------------------------------
	protected static final int MAXIMUM_FIELDS = 17;		//	Maximum fields we use out of each line
	
	//	Indexes (in field array) of the interesting fields in each record.
	protected static final int LABEL_FIELD = 0;
	protected static final int START_DATE_FIELD = 1;
	protected static final int START_TIME_FIELD = 2;
	protected static final int END_DATE_FIELD = 3;
	protected static final int END_TIME_FIELD = 4;
	protected static final int COMMENTS_FIELD = 15;
	protected static final int LOCATION_FIELD = 16;
	
	//	The format of dates, as emitted by Outlook, and concatenated by parseDate().
	public static final String TIME_PATTERN = "MM/dd/yy @ hh:mm:ss aa";
	protected static final DateFormat DATE_FORMATTER = new SimpleDateFormat(TIME_PATTERN, Locale.getDefault());
	
	//	A distinguised millisecond value, for indicating unknown times.
	public static final int UNKNOWN = 0;

	//	Instance variables ----------------------------------------------------------------


	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	public MSOutlookImporter(){
		//	Nothing needed.
	}
	
	
	//	Parse the input file into an Set of TLStates.  Return null on error, which has already been reported
	//	to the user.
	public Set importFromFile(TLDocument doc, File file) throws IOException {
	
		//	Ask the user for a beginning and ending date.  All imported events completely outside this window will
		//	be discarded.
		int okOrCancel = ImportDialog.doDialog();
		if (okOrCancel == FormDialog.CANCEL_OPTION)
			return null;
		
		DefinedCategorySet definedCategories = doc.getDefinedCategories();
		TabDelimitedFileParser parser = new TabDelimitedFileParser(file, MAXIMUM_FIELDS);
		Set newStates = new HashSet();
		
		//	Iterate through all records in the input file.
		String[] fields = null;
		while ((fields = parser.parseNextLine()) != null){
			
			//	Parse the beginning datestamp.  If it can't be parsed, dump it to the log, and go to the next one.
			long start = parseDate(fields[START_DATE_FIELD], fields[START_TIME_FIELD]);
			if (start == UNKNOWN){
				System.err.println("Start timestamp is unparseable:  " + fields[LABEL_FIELD]);
				continue;
			}
			
			//	Parse the ending datestamp.  If it can't be parsed, dump it to the log, and go on to the next one.
			long end = parseDate(fields[END_DATE_FIELD], fields[END_TIME_FIELD]);
			if (end == UNKNOWN ){
				System.err.println("End timestamp is unparseable:  " + fields[LABEL_FIELD]);
				continue;
			}
			
			if (end < start){
				System.err.println("End is before start:  " + fields[LABEL_FIELD]);
				continue;
			}
			
			//	If the event (or some part of it), doesn't fall within the window specified by
			//	the user, ignore it.
			if (end < ImportDialog.importStart || start > ImportDialog.importEnd)
				continue;
				
			//	If the event's name is empty, ignore it.
			String trimmedLabel = fields[LABEL_FIELD].trim();
			if ("".equals(trimmedLabel))
				continue;
				
			//	Create the state and its component objects.
			//	??	Should I check for comments/locations fields that just contain whitespace?
			LabelInfo labelInfo = new LabelInfo(trimmedLabel, fields[COMMENTS_FIELD].trim() + fields[LOCATION_FIELD].trim(), false);
			TLEvent startEvent = new TLEvent(start, start);
			TLEvent endEvent = new TLEvent(end, end);
			TLState state = new TLState(labelInfo, startEvent, endEvent, definedCategories.getSharedMemberSet());
			
			//	Add this state to the list and iterate for the next.
			newStates.add(state);
		}
		
		return newStates;
	}
	
	//	Parse a date and time, as emitted by Outlook.
	protected long parseDate(String date, String time){
		long returnValue;
		try {
			returnValue = DATE_FORMATTER.parse(date + " @ " + time).getTime();
		}
		catch (ParseException e){
			returnValue = UNKNOWN;
		}
		return returnValue;
	}
	
	
}

