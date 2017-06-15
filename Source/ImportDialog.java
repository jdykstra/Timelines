//	ImportDialog.java - Dialog for getting information for an import command.

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


public class ImportDialog extends FormDialog {

	//	Constants.
	protected static final String WINDOW_NAME = "Import";
	protected static final Date DEFAULT_START = new Date(1970, 0, 1);	//	Default for start field is 1/1/70.
	protected static final Date DEFAULT_END = new Date();				//	Default for end field is today
	
	//	Label fields.
	protected static final  String START = "Import events on or after|S|Exclude events before this date";
	protected static final  String END = "and before|E|Exclude events after this date";

	
	//	Class variables.
	//	These variables return values to our caller after the dialog is run.
	//	??	Is this a decent design?
	public static long importStart;			//	Beginning of date range for which events are imported
	public static long importEnd;			//	End of date range for which events are imported
	
	
	//	Instance variables.


	public static int doDialog(){
				
		//	Build the dialog.
		ImportDialog dialog = new ImportDialog(null, WINDOW_NAME);
		dialog.createFields();
		
		//	Display the dialog and process user input.   Return value indicating OK or Cancel.
		int okOrCancel =  dialog.doUserInteractionAndValidation(false);		
		if (okOrCancel == JOptionPane.OK_OPTION){
			try {
				dialog.getValuesFromDialog();
			}
			catch  (ValidationError e){
				Application.gApp.reportUnexpectedException(e);
			}
		}
		return okOrCancel;	
	}
	
	
	//	Constructor.
	protected ImportDialog(JFrame parentWindow, String name){
		super(parentWindow, name);
	}
	
	
	protected void createFields(){
		addTextField(START, 	DEFAULT_START, 		200, true);
		addTextField(END,  	DEFAULT_END, 			200, true);
	}

	
	//	Override of doSemanticValidation() defined in FormDialog.
	protected void doSemanticValidation() throws ValidationError {
		//	Just use the validation in getValuesFromDialog().
		getValuesFromDialog();
	}
	
	
	//	Put the field values provided by the user into class-static variables that can be 
	//	examined by our caller.
	protected void getValuesFromDialog()  throws ValidationError {
		checkDateOrder(START, END, true);
		importStart = getDateValue(START).getTime();
		importEnd = getDateValue(END).getTime();
	}
}

