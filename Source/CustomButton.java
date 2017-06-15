//	CustomButton.java - Add appearance and behavior to the Swing JButton class.

import java.awt.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;

class CustomButton extends JButton {
	
	// 	Constants ----------------------------------------------------------------------
	protected static final int MARGIN = 20;
	public static final Border MARGIN_BORDER = new EmptyBorder(MARGIN, MARGIN, MARGIN, MARGIN);
	
	
	//	Instance variables ----------------------------------------------------------------


	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	public CustomButton(String label, Action action){
		super(label);
		
		//	Arrange for this button to fire ActionEvents to the Action when it
		//	is pressed.
		this.addActionListener(action);
		
		//	Create a listener which catches changes to the action and updates
		//	the JButton appropriately.  Initialze the button's "enabled" status
		//	from the Action's.
		action.addPropertyChangeListener(new ActionChangedListener(this));
                setEnabled(action.isEnabled());
		
		//	Add a blank margin around the widget.
		setBorder(new CompoundBorder(MARGIN_BORDER, getBorder()));
	}


    //	This class definition is stolen from JDK 1.2 javax.swing.JToolBar.
    private class ActionChangedListener implements PropertyChangeListener {
        JButton button;
        
        ActionChangedListener(JButton b) {
            super();
            setTarget(b);
        }
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (e.getPropertyName().equals(Action.NAME)) {
                String text = (String) e.getNewValue();
                button.setText(text);
                button.repaint();
            } else if (propertyName.equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                button.setEnabled(enabledState.booleanValue());
                button.repaint();
            } else if (e.getPropertyName().equals(Action.SMALL_ICON)) {
                Icon icon = (Icon) e.getNewValue();
                button.setIcon(icon);
                button.invalidate();
                button.repaint();
            } 
        }
	public void setTarget(JButton b) {
	    this.button = b;
	}
    }

}

