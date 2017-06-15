//	CategoryListDialog.java - Dialog for adding, deleting or renaming categories.

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

//	??	The current implementation changes the document's categories directly, without
//	??	going through the edit mechanism, and is therefore not undoable.

//	??	The dialogs popped up for Add and Edit need to also include notes and lock fields.
//	??	We also need to respect the lock field on existing categories.
//	??	So are we allowed to delete a category which is mentioned in locked states???

public class CategoryListDialog extends AbstractDialog {

	//	Constants.
	protected static final String WINDOW_NAME = "Modify Categories";
	
	
	//	Label fields.
	protected static final String ADD_LABEL = "Add...";
	protected static final String DELETE_LABEL = "Delete";
	protected static final String EDIT_LABEL = "Edit...";
	protected static final String CLOSE_LABEL = "Close";

	
	//	Class variables.
	protected static JDialog iDialog;
	protected static JList iListWidget;
	
	
	//	Instance variables.
	protected TLDocument iDoc;

	//	Display a form dialog showing the currently-defined categories, and buttons for adding, deleting and
	//	renaming categories.
	public static void doDialog(TLDocument doc, TLWindow parentWindow){
				
		//	Build the dialog.
		CategoryListDialog pane = new CategoryListDialog(doc);
		iDialog = pane.createDialog(parentWindow, WINDOW_NAME);
		
		//	Show the dialog, and block until it is dismissed.
		iDialog.show();
	}
	
	
	//	Constructor.
	protected CategoryListDialog(TLDocument doc){
		super();
		iDoc = doc;
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		//	Create the category scrolling list.
		Set cats = iDoc.getDefinedCategories();
		iListWidget = new JList(new ListListModel(new ArrayList(cats)));
		iListWidget.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane sp = new JScrollPane(iListWidget);
		sp.setBorder(new CompoundBorder(CustomButton.MARGIN_BORDER, sp.getBorder()));
		add(sp);
		
		//	Add a listener which changes Action enables as selections come and go.
		//	Call the listener in a kludgy way to initilize the Actions.
		iListWidget.getSelectionModel().addListSelectionListener(iSelectionListener);
		iSelectionListener.valueChanged(null);

		//	Create the command buttons.
		Container buttonPane = new Box(BoxLayout.X_AXIS);
		this.add(buttonPane);
		buttonPane.add(new CustomButton(ADD_LABEL, iAdd));
		buttonPane.add(new CustomButton(DELETE_LABEL, iDelete));
		buttonPane.add(new CustomButton(EDIT_LABEL, iEdit));
		buttonPane.add(new CustomButton(CLOSE_LABEL, iClose));
		
		//	Enable add command only if the document is unlocked.
		iAdd.setEnabled(!iDoc.isContentLocked());
	}
	
	
	//	A selection listener for the category list.
	ListSelectionListener iSelectionListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e){
			boolean selectionExists = !iListWidget.isSelectionEmpty();
			boolean docUnlocked = !iDoc.isContentLocked();
			iDelete.setEnabled(selectionExists && docUnlocked);
			iEdit.setEnabled(selectionExists && docUnlocked);
		}
	};
	
	
	//	Actions ------------------------------------------------------------------------
	protected Action iAdd = new AbstractAction(){
		public void actionPerformed(ActionEvent e){
			try {
				//	Ask the user for the category name.
				CategoryDialog dialog = new CategoryDialog(iDoc, null,"");
				CategoryDialog.Values priorValues =  dialog.new Values();
				dialog.buildFields(priorValues, iDoc.isContentLocked());
				CategoryDialog.Values newValues = dialog.doUserInteraction();
				if (newValues == null)
					return;

				//	Add the category to the document.
				//	??	We're hardwiring the notes and lock fields, until the dialog
				//	??	provides a way to specify them.
				//	??	This should be done by creating an edit object, and calling
				//	??	TLDocument.executeEdit(), not by calling the document
				//	??	editing methods directly.
				Category cat = new Category(newValues.iCategoryName, "", false, newValues.iColor);
				iDoc.editAddCategory(null, cat);

				//	Update the displayed list to reflect the edit.
				ListListModel model = (ListListModel)iListWidget.getModel();
				model.addElement(cat);				
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	protected Action iDelete = new AbstractAction(){
		public void actionPerformed(ActionEvent e){
			try {
				//	Get the selection and remove it from the document.
				//	??	This should be done by creating an edit object, and calling
				//	??	TLDocument.executeEdit(), not by calling the document
				//	??	editing methods directly.
				Category cat = (Category)iListWidget.getSelectedValue();
				iDoc.editDeleteCategory(null, cat);
				
				//	Update the displayed list to reflect the edit.
				ListListModel model = (ListListModel)iListWidget.getModel();
				model.removeElement(cat);				
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	protected Action iEdit = new AbstractAction(){
		public void actionPerformed(ActionEvent e){
			try {
				//	Ask the user for the new name.
				Category cat = (Category)iListWidget.getSelectedValue();
				CategoryDialog dialog = new CategoryDialog(iDoc, null, cat.getLabelInfo().getLabel());
				CategoryDialog.Values priorValues =  dialog.new Values(cat);
				dialog.buildFields(priorValues, iDoc.isContentLocked());
				CategoryDialog.Values newValues = dialog.doUserInteraction();
				if (newValues == null)
					return;
			
				//	Rename the category in the document.
				//	??	This should be done by creating an edit object, and calling
				//	??	TLDocument.executeEdit(), not by calling the document
				//	??	editing methods directly.
				iDoc.editRenameCategory(null, cat, newValues.iCategoryName);
				cat.setColor(newValues.iColor);
				
				//	Update the displayed list to reflect the edit.
				ListListModel model = (ListListModel)iListWidget.getModel();
				model.notifyElementChanged(cat);
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	protected Action iClose = new AbstractAction(){
		public void actionPerformed(ActionEvent e){
			try {
				iDialog.setVisible(false);
				iDialog.dispose();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	//	This method is declared in AbstractDialog as abstract, so we need to define it.
	//	However, since this dialog doesn't have a value, these are dummied off.
	protected void setValue(Object obj){
	}
        
	protected void selectInitialValue(){
	}

}

