//	CustomSwingUtilities.java - Useful methods and constants for working with Swing.

import javax.swing.*;
import javax.swing.border.*;


class CustomSwingUtilities extends Object {

    public final static Border EMPTY_BORDER_5 = new EmptyBorder(5,5,5,5);
    public final static Border EMPTY_BORDER_10 = new EmptyBorder(10,10,10,10);
    public final static Border EMPTY_BORDER_15 = new EmptyBorder(15,15,15,15);

    public final static Border LOWERED_BORDER = new SoftBevelBorder(BevelBorder.LOWERED);
    public final static Border RAISED_BORDER = new SoftBevelBorder(BevelBorder.RAISED);
}