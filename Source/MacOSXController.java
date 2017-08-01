/**
 * Handle the application menu created by the runtime only on Macintosh.
 */

/**
 * @author jwd based on code by Alvin Alexander 
 * https://alvinalexander.com/blog/post/jfc-swing/handling-main-mac-menu-in-swing-application.
 * Many of the functions from com.apple.mrj are deprecated.
 * ??  Need to use reflection to see if the Mac classes are present.  The imports break
 * ??  portability.
 *
 */
import javax.swing.*;

import com.apple.mrj.MRJAboutHandler;
import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJPrefsHandler;
import com.apple.mrj.MRJQuitHandler;

public class MacOSXController 
implements MRJAboutHandler, MRJQuitHandler, MRJPrefsHandler{

	public MacOSXController(){
		MRJApplicationUtils.registerAboutHandler(this);
		MRJApplicationUtils.registerPrefsHandler(this);
		MRJApplicationUtils.registerQuitHandler(this);
	}
	
	public void handleAbout(){
		JOptionPane.showMessageDialog(null, 
                                  "Timelines " + Application.gApp.getVersion(), 
                                  "About Timelines", 
                                  JOptionPane.INFORMATION_MESSAGE);
	}

	public void handlePrefs() throws IllegalStateException{
		JOptionPane.showMessageDialog(null, 
                                  "Timelines doesn't have any preferences yet.", 
                                  "Timelines Preferences", 
                                  JOptionPane.INFORMATION_MESSAGE);
	}

	public void handleQuit() throws IllegalStateException{
		Application.gApp.doQuit();
	}
}
