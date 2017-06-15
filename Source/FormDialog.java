//	FormDialog.java - Encapsulate code necessary to create and process a form-like dialog window.

import java.awt.Component;
import java.awt.Dimension;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.JTextComponent;


//	Each field can potentially have a label, an mnemonic character, and tooltip text.  To simplify 
//	the method declarations, these three items are packed into a single String, separated by '|'.

//	??	The InputVerifier class added in JDK 1.3 should probably be used to verify user input when the focus is changed,
//	??	rather than the existing doSemanticValidation() method which is invoked when the dialog is exited.

abstract class FormDialog extends Object {

	//	Public contants.
	
	// 	Constants ------------------------------------------------------------------------
	protected static final int TEXT_AREA_ROW_COUNT = 12;		//	Number of rows in a text area
	protected static final int TEXT_AREA_COLUMN_COUNT = 50;	//	Number of columns in a text area
	
	//	Redefine these constants so that all dialog clients don't have to import all of Swing.
	protected static final int OK_OPTION = JOptionPane.OK_OPTION;
	protected static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;

	//	We define our own pattern string for SimpleDateFormat, which puts the time before the date.
	public static final String TIME_PATTERN = "H:mm:ss 'on' MM/dd/yy";
	protected static final DateFormat DATE_FORMATTER = new SimpleDateFormat(TIME_PATTERN, Locale.getDefault());

	//	If a typed in time-and-date field includes only the date, interpret it as specifying the midnight 
	//	that starts the specified day.
	public static final String DEFAULT_TIME = "00:00:00";

	//	Instance variables ----------------------------------------------------------------
	//	The JFrame to be used to parent the dialog window.
	protected JFrame iFrame = null;
	protected String iWindowName;
	
	//	Three Box objects are used to organize the dialog's contents.  iDialogContents is the
	//	top-level container;  it ultimately is passed to JOptionPane.showMessageDialog().
	//	It contains (side-by-side) iLabelColumn and iFieldColumn.
	protected Box iTopToBottomContainer = new Box(BoxLayout.Y_AXIS);
	protected Box iLeftToRightContainer  = new Box(BoxLayout.X_AXIS);
	protected Box iLabelColumn  = new Box(BoxLayout.Y_AXIS);
	protected Box iFieldColumn  = new Box(BoxLayout.Y_AXIS);
	{
		iLeftToRightContainer.add(iLabelColumn);
		iLeftToRightContainer.add(iFieldColumn);
		iTopToBottomContainer.add(iLeftToRightContainer);
	}
	
	//	iFieldMap maps the label string to the Swing control for every field in the dialog.
	protected Map iFieldMap = new HashMap();
		
	
	//	Constructor ---------------------------------------------------------
	public FormDialog(JFrame frame, String windowName){
		iFrame = frame;
		iWindowName = windowName;
	}
	
	
	//	Add a editable text field.  This is a helper for addTextField().
	protected void addTextField( String packedLabel, Object initialValue, final int maxWidth, boolean editable){
	
		//	Create the field itself.
		//	The container that this gets added to uses BoxLayout, which by default sets the width
		//	of all components to the width of the largest one.  Override getPreferredSize() so that
		//	the value used to determine the widest component is that provided by our caller.  
		//	Override getMaximumSize() to prevent BoxLayout from setting all widths to that
		//	of the widest.
		final JTextField textField = new JTextField(""){
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = maxWidth;
				return size;
			}
			
			public Dimension getMaximumSize() {
				Dimension size = super.getMaximumSize();
				size.width = maxWidth;
				return size;
			}
		};
		textField.setAlignmentX(Component.LEFT_ALIGNMENT);
		textField.setEnabled(editable);

		//	Put the text field in a wrapper.  This provides space between the text fields
		//	which is painted in the background color.  If we just put an EmptyBorder on
		//	the JTextField, then the extra space is painted in the field's color (i.e. white),
		//	and there is no visual separation between adjacent fields.
		final JPanel textFieldWrapper = new JPanel();
		textFieldWrapper.setLayout(new BoxLayout(textFieldWrapper, BoxLayout.Y_AXIS));
		textFieldWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		textFieldWrapper.add(textField);
		textFieldWrapper.setBorder(CustomSwingUtilities.EMPTY_BORDER_5);
		
		//	Create the label and set its attributes.
		//	Override getMinimumSize() and getMaximumSize() so that we request a height equal to
		//	that of the text field.
		JLabel labelField = new JLabel(){
			public Dimension getMinimumSize() {
				Dimension ourSize = super.getPreferredSize();
				Dimension textfieldSize = textFieldWrapper.getPreferredSize();
				ourSize.height = textfieldSize.height;
				return ourSize;
			}
			
			public Dimension getMaximumSize() {
				return this.getMinimumSize();
			}
		};
		this.setLabelAttributes(labelField, packedLabel);
		labelField.setLabelFor(textField);
		
		//	Save the linkage between the packed label and the Swing control.
		iFieldMap.put(packedLabel, textField);

		//	Set the initial text, if any, into the field.
		if(initialValue != null){
			String initialText;
			if (initialValue instanceof String)
				initialText = (String)initialValue;
			else if (initialValue instanceof Date)
				initialText = DATE_FORMATTER.format(initialValue);
			else
				throw new ImplementationException("Unsupported type passed");

			textField.setText(initialText);
		}
				
		iLabelColumn.add(labelField);
		iFieldColumn.add(textFieldWrapper); 
	}
	
	
	//	Add a multi-line text area.  The label follows the syntax defined in the comments for this class.
	//	The following types (or their Class objects) may be passed as the value object:  String.
	public void addTextArea( String packedLabel, Object initialValue, final int maxWidth, boolean editable){
	
		//	Create the field itself.
		//	The container that this gets added to uses BoxLayout, which by default sets the width
		//	of all components to the width of the largest one.  Override getPreferredSize() so that
		//	the value used to determine the widest component is that provided by our caller.  
		//	Override getMaximumSize() to prevent BoxLayout from setting all widths to that
		//	of the widest.
		//	JTextArea seems to be confused (as of Swing 1.0.3)  in its use of getPreferredSize() and getMaximumSize().
		//	When the contents of the area are empty, getPreferredSize() returns a rectangle that includes the total
		//	space defined by the row count and column count passed to the constructor.  However, getMaximumSize() returns
		//	a height of just one row--the maximum is less than the preferred.  This confuses BoxLayout, which allocates space
		//	based on the preferred size, but then honors the maximum when actually setting component sizes.
		//	Override getMaximumSize() to fix this behavior.
		//	??	Check with later versions of Swing to see if this is still needed.
		//	??	If the user adds more than TEXT_AREA_ROW_COUNT lines of text, the JTextArea will grow, but
		//	??	the dialog does not.  The fix for this should probably to limit the number of lines the user can enter;  if
		//	??	unlimited lines are desired, a scrolling pane should be used.
		//	??	The border doesn't match those of text fields, at least in the Windows L&F.  This is probably because the
		//	??	default border was chosen to be different, in the assumption that JTextArea would be inside a scroller.
		final JTextArea textArea = new JTextArea(TEXT_AREA_ROW_COUNT, TEXT_AREA_COLUMN_COUNT){
			
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = maxWidth;
				return size;
			}
			
			public Dimension getMaximumSize() {
				Dimension size = super.getPreferredSize();
				size.width = maxWidth;
				return size;
			}
		};
		textArea.setLineWrap(true);
		textArea.setBorder(CustomSwingUtilities.LOWERED_BORDER);
		textArea.setEnabled(editable);

		//	Put the text field in a wrapper.  This provides space between the text fields
		//	which is painted in the background color.  If we just put an EmptyBorder on
		//	the JTextField, then the extra space is painted in the field's color (i.e. white),
		//	and there is no visual separation between adjacent fields.
		final JPanel textFieldWrapper = new JPanel();
		textFieldWrapper.setLayout(new BoxLayout(textFieldWrapper, BoxLayout.Y_AXIS));
		textFieldWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		textFieldWrapper.add(textArea);
		textFieldWrapper.setBorder(CustomSwingUtilities.EMPTY_BORDER_5);

		//	Create the label and set its attributes.
		JLabel labelField = new JLabel();
		this.setLabelAttributes(labelField, packedLabel);
		labelField.setLabelFor(textArea);
		labelField.setAlignmentY(Component.TOP_ALIGNMENT);
		
		//	Put a wrapper around the label, whose size is equal to the size of the text 
		//	area.  Set the label's Y alignment so that it appears at the top of this wrapper,
		//	level with the first line of the text area.
		Box labelWrapper = new Box(BoxLayout.X_AXIS){
			public Dimension getMinimumSize() {
				Dimension ourSize = super.getPreferredSize();
				Dimension textfieldSize = textArea.getPreferredSize();
				ourSize.height = textfieldSize.height;
				return ourSize;
			}
			
			public Dimension getMaximumSize() {
				return this.getMinimumSize();
			}
			
			public float getAlignmentX(){
				return Component.RIGHT_ALIGNMENT;
			}
		};
		labelWrapper.add(labelField);
		
		//	Save the linkage between the packed label and the Swing control.
		iFieldMap.put(packedLabel, textArea);

		//	Determine the type of the value, and its initial value, if any.
		//	Set the initial text, if any, into the field.
		if (initialValue != null){
			String initialText;
			if (initialValue instanceof String)
				initialText = (String)initialValue;
			else
				throw new ImplementationException("Unsupported type passed");

			textArea.setText(initialText);
		}

		//	Build the line on the dialog.
		iLabelColumn.add(labelWrapper);
		iFieldColumn.add(textFieldWrapper);
	}
	
	
	
	//	Add a multi-line text area, inside a scrollpane.  The label follows the syntax defined in the comments for this class.
	//	The following types (or their Class objects) may be passed as the value object:  String.
	public void addScrolledTextArea( String packedLabel, Object initialValue, final int maxWidth, boolean editable){
	
		////	if (!editable){
		////		this.addNoneditableTextField(packedLabel,  initialValue, maxWidth);
		////		return;
		////	}
		
		
		//	??	Flags were a parameter in an earlier version of this method.  Delete associated
		//	??	code if we don't find a use for them.
		int flags = 0;
		
		//	Create the field itself.
		//	The container that this gets added to uses BoxLayout, which by default sets the width
		//	of all components to the width of the largest one.  Override getPreferredSize() so that
		//	the value used to determine the widest component is that provided by our caller.  
		//	Override getMaximumSize() to prevent BoxLayout from setting all widths to that
		//	of the widest.
		//	JTextArea seems to be confused (as of Swing 1.0.3)  in its use of getPreferredSize() and getMaximumSize().
		//	When the contents of the area are empty, getPreferredSize() returns a rectangle that includes the total
		//	space defined by the row count and column count passed to the constructor.  However, getMaximumSize() returns
		//	a height of just one row--the maximum is less than the preferred.  This confuses BoxLayout, which allocates space
		//	based on the preferred size, but then honors the maximum when actually setting component sizes.
		//	Override getMaximumSize() to fix this behavior.
		//	??	Check with later versions of Swing to see if this is still needed.
		//	??	If the user adds more than TEXT_AREA_ROW_COUNT lines of text, the JTextArea will grow, but
		//	??	the dialog does not.  The fix for this should probably to limit the number of lines the user can enter;  if
		//	??	unlimited lines are desired, a scrolling pane should be used.
		//	??	The border doesn't match those of text fields, at least in the Windows L&F.
		final JTextArea textArea = new JTextArea(TEXT_AREA_ROW_COUNT, TEXT_AREA_COLUMN_COUNT){
			
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = maxWidth;
				return size;
			}
			
			public Dimension getMaximumSize() {
				return this.getPreferredSize();
			}
		};
		textArea.setLineWrap(true);
		textArea.setEnabled(editable);
		
		//	Put the JTextArea inside a JScrollPane.
		final JScrollPane textFieldScroller = new JScrollPane(textArea);
		textFieldScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
		textFieldScroller.setBorder(CustomSwingUtilities.EMPTY_BORDER_5);

		//	Create the label and set its attributes.
		JLabel labelField = new JLabel();
		this.setLabelAttributes(labelField, packedLabel);
		labelField.setLabelFor(textArea);
		labelField.setAlignmentY(Component.TOP_ALIGNMENT);
		
		//	Put a wrapper around the label, whose size is equal to the size of the text 
		//	area.  Set the label's Y alignment so that it appears at the top of this wrapper,
		//	level with the first line of the text area.
		Box labelWrapper = new Box(BoxLayout.X_AXIS){
			public Dimension getMinimumSize() {
				Dimension ourSize = super.getPreferredSize();
				Dimension textfieldSize = textArea.getPreferredSize();
				ourSize.height = textfieldSize.height;
				return ourSize;
			}
			
			public Dimension getMaximumSize() {
				return this.getMinimumSize();
			}

			public float getAlignmentX(){
				return Component.RIGHT_ALIGNMENT;
			}
		};
		labelWrapper.add(labelField);
		
		//	Save the linkage between the packed label and the Swing control.
		iFieldMap.put(packedLabel, textArea);

		//	Set the initial text, if any, into the field.
		if(initialValue != null){
			String initialText;
			if (initialValue instanceof String)
				initialText = (String)initialValue;
			else
				throw new ImplementationException("Unsupported type passed");

			textArea.setText(initialText);
		}

		//	Build the line on the dialog.
		iLabelColumn.add(labelWrapper);
		iFieldColumn.add(textFieldScroller);
	}
	
	
	//	Display the dialog to the user and handle interaction.  Returns JOptionPane.CANCEL_OPTION or
	//	JOptionPane.OK_OPTION.
	public int doUserInteractionAndValidation(boolean allowCancelOnly){

		//	Repeat the main body of this method until all fields validate, or the user cancels.
		boolean validationSuccessful = false;
		do {
			//	Show the form dialog.
			//	??	It would be nice if we could leave this dialog on the screen when presenting
			//	??	validation errors, but that would probably require that we build the dialog
			//	??	ourselves, rather than using JOptionPane.
			int response = JOptionPane.showConfirmDialog(iFrame, iTopToBottomContainer, iWindowName, 
					 (allowCancelOnly) ? JOptionPane.DEFAULT_OPTION : JOptionPane.OK_CANCEL_OPTION, 
					 JOptionPane.QUESTION_MESSAGE, null);
			if (allowCancelOnly)
				return JOptionPane.CANCEL_OPTION;
			if (response != JOptionPane.OK_OPTION)
				return response;
		
			try {
				doSemanticValidation();
				validationSuccessful = true;
			}
			catch (ValidationError error){
				showValidationError(error);
			}

		} while (!validationSuccessful);
		
		return JOptionPane.OK_OPTION;
	}
	
	
	//	Implemented by the subclass to validate all user-entered values.
	protected abstract void doSemanticValidation() throws ValidationError;
	
	
	//	checkNotBlank() verifies that a text field is not empty or blank.  It will be useful in semantic
	//	validation routines.
	protected void checkNotBlank(String packedLabel) throws ValidationError {
		if (isEmptyOrBlank(packedLabel))
			throw new ValidationError(packedLabel, "should not be blank");
	}
	
	
	//	Helper routine that tests whether the specified field is zero-length or just contains blanks.
	protected boolean isEmptyOrBlank(String packedLabel){
		
		JTextComponent component = (JTextComponent)iFieldMap.get(packedLabel);
		String rawText = component.getText();

		//	Only need to do blank check if the string is not empty.
		if (rawText.length() !=  0){
		
			//	See whether there are any non-wordbreak characters in the string.
			BreakIterator iter = BreakIterator.getWordInstance();
			iter.setText(rawText);
			for (int next = iter.first(); next < rawText.length();  next = iter.next())
				if (!Character.isWhitespace(rawText.charAt(next)))
 					return false;
		}
		
		return true;
	}
	
	
	//	Report a validation error by popping up a dialog box.
	protected void showValidationError(ValidationError error){
		
		//	Start building the message text.
		StringBuffer msg = new StringBuffer(" \"");
		
		//	Add the label text onto the message.
		LabelParser parser = new LabelParser(error.getPackedLabel());
		msg.append(parser.getNextValue());
		
		//	Complete the message with the error description.
		msg.append("\" ");
		msg.append(error.getErrorText());
		msg.append(".");
		
		//	Show the dialog.
		JOptionPane.showMessageDialog(iFrame, msg,  "Error", JOptionPane.ERROR_MESSAGE);
	}


	//	See the tooltip, mnemonic and label attributes of a JLabel, given a packed label string, as
	//	described by the global class comments.  Also set some other attributes that are the same
	//	for all JLabel's.
	protected void setLabelAttributes(JLabel label, String packedLabel){

		//	Parse the packed label into a label, mnemonic character, and tooltip text.
		LabelParser parser = new LabelParser(packedLabel);
		label.setText(parser.getNextValue() + ":  ");
		label.setDisplayedMnemonic(parser.getNextValue().charAt(0));
		label.setToolTipText(parser.getNextValue());

		//	Set other attributes which are the same for all JLabel's.
		label.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
	}
	
	
	//	Internal class representing a packed label field, as passed to the addXXXField() methods.
	protected class LabelParser extends Object {
	
		//	Constants.
		protected final char DELIMITER = '|';
		
		//	Instance variables.
		protected String iPackedValue;			//	Value passed to us
		protected int iParsePosition = 0;			//	Current position of parse
		
		//	Constructor.
		protected LabelParser(String value){
			iPackedValue = value;
		}
				
		//	Return the next item (delimited by '|') in the packed label.  Returns an empty string
		//	if the packed label has been exhausted.
		protected String getNextValue(){
			if (iParsePosition >= iPackedValue.length())
				return "";
				
			int end = iPackedValue.indexOf(DELIMITER, iParsePosition);
			if (end < 0)
				end = iPackedValue.length();
			
			String value = iPackedValue.substring(iParsePosition, end);
			iParsePosition = end + 1;
			return value;
		}
	}
	
	
	//	Get the contents of a text field or text area, returned as a String.
	protected String getStringValue(String packedLabel) throws ValidationError {
		JTextComponent component = (JTextComponent)iFieldMap.get(packedLabel);
		String rawText = component.getText();
		return rawText;
	}
	
	
	//	Get the contents of a text field (or text area), parsed into a Date.
	protected Date getDateValue(String packedLabel) throws ValidationError {

		//	We use a slightly kludgy way of determining whether the field typed
		//	in by the user lacks a time value:  We first try parsing the text
		//	just as entered by the user.  If that fails, we try appending the
		//	default time field.
		JTextComponent component = (JTextComponent)iFieldMap.get(packedLabel);
		String rawText = component.getText();
		Date returnValue;
		try {
			returnValue = DATE_FORMATTER.parse(rawText);
		}
		catch (ParseException e){
			try {
				returnValue = DATE_FORMATTER.parse(DEFAULT_TIME + " on " + rawText);
			}
			catch (ParseException e2){
				throw new ValidationError(packedLabel, " does not look like a Date" );
			}
		}

		return returnValue;
	}
	
		
	//	checkDateOrder() verifies that two Date fields are in the correct order.  It will be useful in
	//	semantic validation routines.  If allowIdenticalValues is true, it is legal for the two Dates to
	//	be the same.
	protected void checkDateOrder(String firstLabel, String secondLabel, boolean allowIdenticalValues)
					 throws ValidationError  {
		
		Date firstValue = this.getDateValue(firstLabel);
		Date secondValue = this.getDateValue(secondLabel);
		if (firstValue.getTime() > secondValue.getTime()){
		
			//	showValidationError() only knows how to insert one field label into its message.  
			//	Extract the second field name ourselves, to create a more informative message.
			StringBuffer msg = new StringBuffer("should be before \"");
			LabelParser parser = new LabelParser(secondLabel);
			msg.append(parser.getNextValue());
			msg.append("\"");

			throw new ValidationError(firstLabel, new String(msg));
		}
		
		//	If identical values are not allowed, make that check.
		if (!allowIdenticalValues && firstValue.getTime() == secondValue.getTime()){
		
			//	showValidationError() only knows how to insert one field label into its message.  
			//	Extract the second field name ourselves, to create a more informative message.
			StringBuffer msg = new StringBuffer("cannot be the same as \"");
			LabelParser parser = new LabelParser(secondLabel);
			msg.append(parser.getNextValue());
			msg.append("\"");

			throw new ValidationError(firstLabel, new String(msg));
		}
	}
	
	
	//	ValidationError's are thrown to indicate a validation error.
	class ValidationError extends Throwable {
	
		String iPackedLabel;
		String iErrorText;
	
		ValidationError(String packedLabel, String errorText){
			iPackedLabel = packedLabel;
			iErrorText = errorText;
		}
		
		String getPackedLabel()	{	return iPackedLabel;	}
		String getErrorText()		{	return iErrorText;	}
	}
}