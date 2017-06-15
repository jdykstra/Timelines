//	ProgressWindow.java - A generic progress window.

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.*;
import java.awt.BorderLayout;

public class ProgressWindow extends JDialog {

	//	Constants.
	static final int WINDOW_WIDTH = 300;
	static final int WINDOW_HEIGHT = 80;

	//	Instance variables.
	JProgressBar iProgressBar;
	
	
	public Insets getInsets() {
		return new Insets(40,30,20,30);
	}


	public ProgressWindow(String msg, int maxValue){
		//	??	If I tell JDialog that this window is modal, we loop in this.show() waiting
		//	??	for the user to close the window.  What's the proper way to do this?
		//	The cast helps the compiler resolve an ambiguity when it is trying to
		//	determine which constructor should be called.
		super((java.awt.Frame)null, msg, false);
		
		this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(screenSize.width/2 - WINDOW_WIDTH/2,
				  screenSize.height/2 - WINDOW_HEIGHT/2);
				  
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		iProgressBar = new JProgressBar();
		iProgressBar.setMinimum(0);
		iProgressBar.setMaximum(maxValue);
		iProgressBar.getAccessibleContext().setAccessibleName(msg);
		contentPane.add(iProgressBar, BorderLayout.CENTER);
	
		this.show();

	       //	??	If we add a cancel button to this window, we need to change the cursor.
	       this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}
	
	
	public void updateValue(int value){
		if (value != iProgressBar.getValue()){
			iProgressBar.setValue(value);
			
			//	The thread that we're reporting the progress of is probably tying up
			//	the CPU pretty well, so AWT's repaint thread doesn't execute very
			//	often.  Therefore, repaint this component immediately. 
			Rectangle r = this.getBounds();
			 r.x = 0;
			 r.y = 0;
			iProgressBar.paintImmediately(r);
		}
	}
	
	public void remove(){
		this.hide();
	       	this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

}