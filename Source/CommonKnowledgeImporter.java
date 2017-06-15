//	CommonKnowledgeImporter.java - Import states from a Common Knowledge document.

import java.io.*;
import java.text.*;
import java.util.*;

class CommonKnowledgeImporter extends Object implements Importer {
	
	// 	Constants ----------------------------------------------------------------------
	
	//	Indexes (in field array) of the interesting fields in each record.
	protected static final int LABEL_FIELD = 0;
	protected static final int START_FIELD = 1;
	protected static final int END_FIELD = 2;
	protected static final int COMMENTS_FIELD = 3;
	
	protected static final int FIELD_COUNT = COMMENTS_FIELD + 1;

	//	The format of dates, as emitted by Common Knowledge.
	public static final String TIME_PATTERN = "EEE, MMM dd, yyyy @ KK:mm aa";
	protected static final DateFormat DATE_FORMATTER = new SimpleDateFormat(TIME_PATTERN, Locale.getDefault());
	
	//	A distinguised millisecond value, for indicating unknown times.
	public static final int UNKNOWN = 0;

	//	Instance variables ----------------------------------------------------------------


	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	public CommonKnowledgeImporter(){
		//	Nothing needed.
	}
	
	
	//	Parse the input file into an Set of TLStates
	public Set importFromFile(TLDocument doc, File file) throws IOException {
		DefinedCategorySet definedCategories = doc.getDefinedCategories();
		TabDelimitedFileParser parser = new TabDelimitedFileParser(file, FIELD_COUNT);
		Set newStates = new HashSet();
		
		//	Iterate through all records in the input file.
		String[] fields = null;
		while ((fields = parser.parseNextLine()) != null){
			
			//	Parse the beginning datestamp.  If it can't be parsed, this is probably a 
			//	repeating event.  Dump it to the log, and go to the next one.
			long start = parseDate(fields[START_FIELD]);
			if (start == UNKNOWN){
				System.err.println(fields);
				continue;
			}
			
			//	Parse the ending datestamp.  Common Knowledge sometimes emits events that start at
			//	(say) 5:30PM, and "end" at 12:00AM of the same day.  Kludge around these.
			long end = parseDate(fields[END_FIELD]);
			if (end == UNKNOWN || end < start)
				end = start;
				
			//	Create the state and its component objects.
			LabelInfo labelInfo = new LabelInfo(fields[LABEL_FIELD], fields[COMMENTS_FIELD], false);
			TLEvent startEvent = new TLEvent(start, start);
			TLEvent endEvent = new TLEvent(end, end);
			TLState state = new TLState(labelInfo, startEvent, endEvent, definedCategories.getSharedMemberSet());
			
			//	Add this state to the list and iterate for the next.
			newStates.add(state);
		}
		
		return newStates;
	}
	
	//	Parse a date as emitted by Common Knowledge.
	protected long parseDate(String date){
		long returnValue;
		try {
			returnValue = DATE_FORMATTER.parse(date).getTime();
		}
		catch (ParseException e){
			returnValue = UNKNOWN;
		}
		return returnValue;
	}
}

