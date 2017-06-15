//	StateAttributeDialog.java - Dialog for setting all attributes of a state individually.

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class StateAttributeDialog extends TLFormDialog {

	//	Constants.
	protected static final String WINDOW_NAME = "Enter state information";
	
	//	Label fields.
	protected static final String LABEL = "Label|L|Identifying name of the state.  This will be displayed in the timeline view.";
	protected static final  String EARLIEST_START = "Starts at or after|S|When is the earliest the state could start?";
	protected static final  String LATEST_START = "Starts before|U|When is the latest the state could start?";
	protected static final  String EARLIEST_END = "Ends at or after|E|When is the earliest the state could end?";
	protected static final  String LATEST_END = "Ends before|X|When is the latest the state could end?";
	protected static final String NOTES = "Notes|N|Miscellaneous information about the state";
	protected static final  String CREATE = "Created| |When the state was entered into this document";
	protected static final  String MODIFY = "Modified| |When the state was last modified";

	
	//	Class variables.
	protected static Map sCategoryMenuMap;
	
	
	//	Instance variables.
	protected TLDocument iDoc;					//	Associated document

	//	Display a form dialog showing all parameters for a TLState object.  If a TLState is passed in, then its
	//	parameters are used to set the initial values of the dialog.  If
	//	null is passed in, defaults are used for the initial dialog values.
	//	If the user clicks OK, a new TLState is created and returned
	//	with the values entered by the user.  If the user cancels out of the dialog, null is returned.
	public static TLState doDialog(TLDocument doc, TLWindow parentWindow, TLState providedState){
				
		//	Build the dialog.
		StateAttributeDialog dialog = new StateAttributeDialog(doc, parentWindow, WINDOW_NAME);
		dialog.createFields(doc, providedState);
		
		//	Display the dialog and process user input.  If the user cancels (or clicks the close box on the window), 
		//	return null.
		int userButtonChoice = dialog.doUserInteractionAndValidation(doc.isContentLocked());
		if (userButtonChoice != JOptionPane.OK_OPTION)
			return null;
			
		//	Build a state from the field values provided by the user.
		TLState state;
		try {
			state = dialog.buildStateFromFields();
		}
		catch (ValidationError e){
			throw new ImplementationException(e);
		}

		return state;
	}
	
	
	//	Constructor.
	protected StateAttributeDialog(TLDocument doc, JFrame parentWindow, String name){
		super(parentWindow, name);
		iDoc = doc;
	}
	
	
	protected void createFields(final TLDocument doc, final TLState providedState){

		String labelParam = null;
		Date earliestStartParam = null;
		Date latesttStartParam = null;
		Date earliestEndParam = null;
		Date latesttEndParam = null;
		String notesParam = null;
		String createdParam = null;
		String modifiedParam = null;
		Boolean lockedParam = null;
		if (providedState != null){
			LabelInfo labelInfo = providedState.getLabelInfo();
			labelParam = labelInfo.getLabel();
			earliestStartParam = new Date(providedState.getTimeParameter(TLState.T0));
			latesttStartParam = new Date(providedState.getTimeParameter(TLState.T1));
			if (latesttStartParam.equals(earliestStartParam))
				latesttStartParam = null;
			earliestEndParam = new Date(providedState.getTimeParameter(TLState.T2));
			latesttEndParam = new Date(providedState.getTimeParameter(TLState.T3));
			if (latesttEndParam.equals(earliestEndParam))
				latesttEndParam = null;
			notesParam = labelInfo.getNotes();
			long time = providedState.getCreateTime();
			if (time == DataObj.UNKNOWN_TIMESTAMP)
				createdParam = "Unknown";
			else
				createdParam = (new Date(time)).toString();
			time = providedState.getModifyTime();
			if (time == DataObj.UNKNOWN_TIMESTAMP)
				modifiedParam = "Unknown";
			else
				modifiedParam = (new Date(time)).toString();
			lockedParam = new Boolean(labelInfo.isLocked());
		}
		
		//	Build the dialog.
		boolean editable = !doc.isContentLocked();
		addTextField(LABEL, 			labelParam, 			400, editable);
		addTextField(EARLIEST_START, 	earliestStartParam, 		200, editable);
		addTextField(LATEST_START, 	latesttStartParam, 		200, editable);
		addTextField(EARLIEST_END,  	earliestEndParam, 		200, editable);
		addTextField(LATEST_END,  		latesttEndParam, 		200, editable);
		addScrolledTextArea(NOTES,  	notesParam, 			400, editable);
		addTextField(CREATE,  		createdParam, 			200, editable);
		addTextField(MODIFY,  		modifiedParam, 			200, editable);
		
		//	Add the category popup list.
		JComponent catList = createCategoryList(doc, providedState);
		iLeftToRightContainer.add(catList);
	}

	
	//	Build the category list.
	protected static JComponent createCategoryList(TLDocument doc, TLState providedState){

		Box list = new Box(BoxLayout.Y_AXIS);
		
		//	Build the list model by iterating through all categories.
		//	??	The compiler current as of 6/7/99 went flakey when compiling the
		//	??	following statement.
		//	??	Set memberCategories = (providedState != null) ? 	providedState.getCategories().getAsSet() :  null;
		Set memberCategories = null;
		if (providedState != null)
			memberCategories = providedState.getCategories().getAsSet();
		
		sCategoryMenuMap = new HashMap();
		boolean editable = !doc.isContentLocked();
		Iterator iter = doc.getDefinedCategories().iterator();
		while (iter.hasNext()){
		
			//	Get the next category.
			Category cat = (Category)iter.next();
			LabelInfo li = cat.getLabelInfo();

			//	Each category is represented using a javax.swing.Box that contains
			//	a JCheckBox and a JLabel.
			Box box = new Box(BoxLayout.X_AXIS){
				public float getAlignmentX() {
					return Component.LEFT_ALIGNMENT;
				}
				public float getAlignmentY() {
					return Component.CENTER_ALIGNMENT;
				}
			};
			JCheckBox checkbox = new JCheckBox();
			checkbox.setAlignmentX(Component.CENTER_ALIGNMENT);
			checkbox.setAlignmentY(Component.CENTER_ALIGNMENT);
			checkbox.setEnabled(editable);
			box.add(checkbox);
			JLabel label = new JLabel(li.getLabel());
			label.setAlignmentX(Component.LEFT_ALIGNMENT);
			label.setAlignmentY(Component.CENTER_ALIGNMENT);
			label.setForeground(cat.getColor());
			box.add(label);
			list.add(box);
			
			//	Check the checkbox if the state is already a member of this
			//	category.
			if (memberCategories != null)
				checkbox.setSelected(memberCategories.contains(cat));
			
			//	Associate the checkbox with its corresponding Cateogry in sCategoryMenuMap.
			sCategoryMenuMap.put(checkbox, cat);
		}
		
		JScrollPane sp = new JScrollPane(list);
		sp.setBorder(new CompoundBorder(CustomButton.MARGIN_BORDER, sp.getBorder()));
		
		return sp;
	}
	

	//	Override of doSemanticValidation() defined in FormDialog.
	protected void doSemanticValidation() throws ValidationError {
		
		//	??	Validate entries by building (and discarding) the TLState.
		//	??	Once we're sure this is the way we want to do validation,
		//	??	it MIGHT be worth saving the TLState built here.
		buildStateFromFields();
	}
	
	
	//	Get the current settings of the category list.
	protected static Set getCategoryListSetting(){
	
		//	Iterate through all of the checkbox list items.
		Set newCategories = new HashSet();
		Iterator iter = sCategoryMenuMap.entrySet().iterator();
		while (iter.hasNext()){
		
			//	If this checkbox is checked, add its corresponding category
			//	to the set.
			Map.Entry entry = (Map.Entry)iter.next();
			JCheckBox cb = (JCheckBox)entry.getKey();
			if (cb.isSelected())
				newCategories.add(entry.getValue());
		}
		
		return newCategories;
	}
	
	
	//	Build a state from the field values provided by the user.
	protected TLState buildStateFromFields() throws ValidationError{
	
		//	Get the new parameter values.
		//	??	Change when lock is a real field.
		checkNotBlank(LABEL);
		String newLabel = getStringValue(LABEL);

		long newEarliestStart = getDateValue(EARLIEST_START).getTime();
		long newLatestStart;
		if (isEmptyOrBlank(LATEST_START))
			newLatestStart = newEarliestStart;
		else {
			checkDateOrder(EARLIEST_START, LATEST_START, true);
			newLatestStart = getDateValue(LATEST_START).getTime();
		}
		long newEarliestEnd = getDateValue(EARLIEST_END).getTime();
		long newLatestEnd;
		if (isEmptyOrBlank(LATEST_END))
			newLatestEnd = newEarliestEnd;
		else {
			checkDateOrder(EARLIEST_END,  LATEST_END, true);
			newLatestEnd = getDateValue(LATEST_END).getTime();
		}
		String newNotes = getStringValue(NOTES);
		boolean newLocked = false;
		DefinedCategorySet.MemberSet newCategories = iDoc.getDefinedCategories().getSharedMemberSet(getCategoryListSetting());
			
		//	Build a new TLState object, using the values the user entered.
		LabelInfo labelInfo = new LabelInfo(newLabel, newNotes, newLocked);
		TLEvent startingEvent = new TLEvent(newEarliestStart, newLatestStart);
		TLEvent endingEvent = new TLEvent(newEarliestEnd, newLatestEnd);		
		TLState newState = new TLState(labelInfo, startingEvent, endingEvent, null);
		newState.setCategories(newCategories);
		
		return newState;
	}
}

