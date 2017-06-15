//	CategoryDialog.java - Dialog for adding, deleting or renaming a category.

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

//	A dialog enabling the user to changes values associated with a given category. 

public class CategoryDialog extends TLFormDialog {

	//	Constants.
	protected static final String WINDOW_NAME = "Category:  ";
	protected static final int FIELD_TO_CHOOSER_SPACING = 20;
	
	//	Label fields.
	protected static final String NAME_LABEL = "Name|N|Name which identifies this category";
	protected static final int NAME_FIELD_WIDTH = 200;
	protected static final String COLOR_LABEL = "Color|C|Color used to display states which are in this cateogry";

	//	Instance variables.
	protected TLDocument iDoc;
	protected JColorChooser iColorChooser;
	
	//	Subclass used to return results to the dialog's creator.
	//	Public fields are used to return all values--it doesn't seem worth it to implement
	//	accessor methods.
	public class Values extends Object {
		public String iCategoryName;
		public Color iColor;
		
		public Values(Category cat){
			iCategoryName = cat.getLabelInfo().getLabel();
			iColor = cat.getColor();
		}

		public Values(){
			iCategoryName = null;
			iColor = null;
		}
	}
	
	
	//	Constructor.
	protected CategoryDialog(TLDocument doc, JFrame parentWindow, String windowIdentifier){
		super(parentWindow, WINDOW_NAME + windowIdentifier);
		iDoc = doc;
	}
	

	//	Build the contents of the dialog, filling in defaults as appropriate.
	public void buildFields(Values priorValues, boolean viewOnly){
	
		//	Create the dialog's fields.
		addTextField(NAME_LABEL, priorValues.iCategoryName, NAME_FIELD_WIDTH, !viewOnly);
		Color priorColor = (priorValues.iColor == null) ? Category.DEFAULT_BODY_COLOR : priorValues.iColor;
		iColorChooser = new JColorChooser(priorColor);
		iTopToBottomContainer.add(Box.createVerticalStrut(FIELD_TO_CHOOSER_SPACING));
		iTopToBottomContainer.add(iColorChooser);
	}
	
	
	//	Display the dialog and process user input.    If the user canceled out of the dialog, null is returned.
	public Values doUserInteraction(){
		
		//	Display the dialog and process user input.  If the user cancels (or clicks the close box on the window), 
		//	return null.
		int userButtonChoice = doUserInteractionAndValidation(iDoc.isContentLocked());
		if (userButtonChoice != JOptionPane.OK_OPTION)
			return null;
		
		//	Build a Values object from the values entered by the user, and return it.
		Values newValues = null;
		try 	{
			newValues = buildAttributesFromFields();
		} catch (ValidationError e){
			Application.gApp.reportUnexpectedException(e);
		}
		return newValues;

	}


	protected  void doSemanticValidation() throws ValidationError{
		//	Anthing goes, at the moment.
	}
	
	
	protected Values buildAttributesFromFields () throws ValidationError{
		Values newAttributes = new Values();
		newAttributes.iCategoryName = getStringValue(NAME_LABEL);
		newAttributes.iColor =  iColorChooser.getColor();
		return newAttributes;
	}
}

