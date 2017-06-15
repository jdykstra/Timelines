//	AbstractDialog.java - Provide framework for creating modal dialogs.

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.*;

public abstract class AbstractDialog extends JComponent {
	
	// 	Constants ----------------------------------------------------------------------
    /** Bound property name for icon. */
    public static final String      ICON_PROPERTY = "icon";
    /** Bound property name for message. */
    public static final String      MESSAGE_PROPERTY = "message";
    /** Bounds property name for value. */
    public static final String      VALUE_PROPERTY = "value";
    /** Bounds property namer for option. */
    public static final String      OPTIONS_PROPERTY = "options";
    /** Bounds property name for initialValue. */
    public static final String      INITIAL_VALUE_PROPERTY = "initialValue";
    /** Bounds property name for type. */
    public static final String      MESSAGE_TYPE_PROPERTY = "messageType";
    /** Bound property name for optionType. */
    public static final String      OPTION_TYPE_PROPERTY = "optionType";
    /** Bound property name for selectionValues. */
    public static final String      SELECTION_VALUES_PROPERTY = "selectionValues";
    /** Bound property name for initialSelectionValue. */
    public static final String      INITIAL_SELECTION_VALUE_PROPERTY = "initialSelectionValue";
    /** Bound property name for inputValue. */
    public static final String      INPUT_VALUE_PROPERTY = "inputValue";
    /** Bound property name for wantsInput. */
    public static final String      WANTS_INPUT_PROPERTY = "wantsInput";



	//	Instance variables ----------------------------------------------------------------


	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------




    //	Creates and returns a new JDialog wrapping <code>this</code>
    //	centered on the <code>parentComponent</code> in the 
    //	<code>parentComponent</code>'s frame.
    //	<code>title</code> is the title of the returned dialog.
    //	Stolen from JDK 1.2 javax.swing.JOptionPane, with the following modification:
    //	The JDK code contains a workaround for a Solaris bug, which calls
    //	SwingUtilities.getRecycledModalDialog().  Unfortunately, this is not declared
    //	public, so the bug workaround has been deleted from this code.
    public JDialog createDialog(Component parentComponent, String title) {
        Frame         frame = JOptionPane.getFrameForComponent(parentComponent);
        final JDialog dialog;

        dialog = new JDialog(frame, title, true);
        Container             contentPane = dialog.getContentPane();

        contentPane.setLayout(new BorderLayout());
        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(parentComponent);
        dialog.addWindowListener(new WindowAdapter() {
            boolean gotFocus = false;
            public void windowClosing(WindowEvent we) {
                setValue(null);
            }
            public void windowActivated(WindowEvent we) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    selectInitialValue();
                    gotFocus = true;
                }
            }
        });
        addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if(dialog.isVisible() && event.getSource() == AbstractDialog.this &&
                   (event.getPropertyName().equals(VALUE_PROPERTY) ||
                    event.getPropertyName().equals(INPUT_VALUE_PROPERTY))) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        });
        return dialog;
    }
    
    
    //	Methods for manipulating the dialog's final value.  This is frequently the OK/Cancel
    //	indication, but may be something else.
    //	These methods come from javax.swing.JOptionPane.
    protected abstract void setValue(Object obj);
        
    protected abstract void selectInitialValue();
}

