//	TabDelimitedFileParser.java - Parse a tab-delimited text file.

import java.io.*;

class TabDelimitedFileParser extends Object {
	
	// 	Constants ----------------------------------------------------------------------


	//	Instance variables ----------------------------------------------------------------
	protected final BufferedReader iReader;
	
	//	iFieldCount is the maximum number of fields which will be returned by parseNextLine.
	protected final int iFieldCount;

	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	public TabDelimitedFileParser(File file, int fieldCount) throws FileNotFoundException {
		iReader = new BufferedReader(new FileReader(file));
		iFieldCount = fieldCount;
	}
	
	
	//	Read the next line, and return it parsed into an array of fieldCount Strings.  If the line contains
	//	more fields than fieldCount, those additional fields will be ignored.  If the line contains less fields,
	//	then the extra positions in the array will contain "".  If there are no more lines in the file, return null.
	public String[] parseNextLine() throws IOException {
		
		//	Read the next line, and return if we've hit end-of-file.
		String line = iReader.readLine();
		if (line == null)
			return null;
		
		//	Allocate the array and begin iterating through each field.
		String[] fieldArray = new String[iFieldCount];
		int nextFieldIndex = 0;
		int fieldStart = 0;
		while (fieldStart < line.length() && nextFieldIndex < iFieldCount){
			
			//	See if the field begins with a double-quote.  If it does, the field includes all text (including
			//	new-line characters) up to the next double-quote.
			if (line.charAt(fieldStart) == '\"'){
				
				String fieldUnderConstruction = new String();
				fieldStart++;								//  discard leading "
				int fieldEnd = fieldStart;
				do {
					//	Find the end of this field.
					while (fieldEnd < line.length() && line.charAt(fieldEnd) != '\"')
						fieldEnd++;
	
					//	Save this part of the field, and skip over it.
					fieldUnderConstruction = fieldUnderConstruction.concat(line.substring(fieldStart, fieldEnd));
					
					//	If we've reached the end of this line, read the next one.
					//	If we hit end-of-file, we return, ignoring whatever was parsed out up to now.
					while (fieldEnd >= line.length()) {
						line = iReader.readLine();
						if (line == null)
							return null;
						fieldStart = 0;
						fieldEnd = 0;
						fieldUnderConstruction = fieldUnderConstruction.concat("\n");
					}
				} while (line.charAt(fieldEnd) != '\"');
				
				//	Save this field in the array, and skip over the last part of the field, its terminating
				//	double-quote, and any other junk that preceeds the terminating tab.
				fieldArray[nextFieldIndex++] = fieldUnderConstruction;
				while (fieldEnd < line.length() && line.charAt(fieldEnd) != '\t')
					fieldEnd++;
				fieldStart = fieldEnd + 1;
			}
			else {
				//	Find the end of this field.
				int fieldEnd = fieldStart;
				while (fieldEnd < line.length() && line.charAt(fieldEnd) != '\t')
					fieldEnd++;
				
				//	Save this field in the array, and skip over the field and its delimiter.
				fieldArray[nextFieldIndex++] = line.substring(fieldStart, fieldEnd);
				fieldStart = fieldEnd + 1;
			}
		}
		
		//	Fill in any unused fields.
		while (nextFieldIndex < iFieldCount)
			fieldArray[nextFieldIndex++] = "";
		
		return fieldArray;
	}
}

