//	CustomToolBar.java - A Swing JToolBar with additional behavior.

import javax. swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class CustomToolBar extends JToolBar {
 
    /**
     * Add a new JToggleButton which dispatches the action.
     *
     * @param a the Action object to add as a new menu item
     */
    public JToggleButton addToggleButtonAction(Action a) {
        JToggleButton b = new JToggleButton((String)a.getValue(Action.NAME),
                                (Icon)a.getValue(Action.SMALL_ICON));
        b.setHorizontalTextPosition(JButton.CENTER);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setEnabled(a.isEnabled());
        b.addActionListener(a);
        add(b);
        
        //	Link the Action and the button via our own property change listener.
        //	??	JToolBar retains a registry of these relationships, supposedly for releasing
        //	??	memory, but I don't think it is needed.
        PropertyChangeListener actionPropertyChangeListener = 
            createActionChangeListener(b);
        a.addPropertyChangeListener(actionPropertyChangeListener);
       return b;
    }

    //		ActionChangedListener is generalized from code in javax.swing.JToolBar.
    protected PropertyChangeListener createActionChangeListener(JToggleButton b) {
        return new ActionChangedListener(b);
    }

    private class ActionChangedListener implements PropertyChangeListener {
        JToggleButton button;
        
        ActionChangedListener(JToggleButton b) {
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
	public void setTarget(JToggleButton b) {
	    this.button = b;
	}
    }

}