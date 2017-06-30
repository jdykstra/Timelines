//	Application.java - Generic Application body.

//	Application is one part of the "object hierarchy" around which both the user interface and the internal
//	structure of this application are built.  The three objects in the hiearchy are Application, Document, and
//	Window.  See the class description for TLWindow for a full discussion.

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import javax.swing.*;


//	??	Applet support is not really done yet.


public abstract class Application extends Object {

	//	Compile-time options.
	static final boolean DEBUG_MODE = true;
	
	
	//	Constants.
	//	Path within JAR file of icon images.  When built with the CodeWarrior IDE, this is the path from
	//	the project to the directory holding the GIF files.
	public static final String ICON_PATH = "Icons/";


	//	Global variables.
	public static Application gApp;			//	The one instance of this class.

	//	Instance variables.
 	protected ActionManager iAM = new ActionManager();	//  ActionManager handling enables for this object
   	protected java.applet.Applet iApplet;		//	Non-null if running as an applet


	//	Trivial accessors.
	public ActionManager getActionManager()			{ return iAM;					}
	abstract public String getName();
	abstract public String getVersion();
	
	
	//	Check whether everything we need to execute is present in this JVM.
	//	Throws a runtime error if something essential is missing.
	protected void checkRuntimeEnvironment(){

	        //	If the JVM is too early a version to run Swing reliably, warn the user.
	        //	??	Should probably only make this check if it's a Sun JVM.
	        String vers = System.getProperty("java.version");
	        if (vers.compareTo("1.1.2") < 0) {
			JOptionPane.showMessageDialog(null, "This system is version " + vers +
	            	", but Swing is only guaranteed to work with 1.1.2 or later.");
	        }
	}
	
	
	//	Initialize the windowing system.
	protected void initializeWindowSystem(){
		
		//	Use the system look and feel.
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e){
			reportUnexpectedException(e);
		}
	}
	
	
	//	Create the user interface.
	/************************************************************************
	  *
	  *  Preserve this code, in case we want to create an MDI application in the future.
	  *
	static final int WINDOW_TITLE_HEIGHT = 26;	//	Height of Windows window title bar

	protected JFrame iAppFrame;			//	The application frame
	public int iAppFrameBottom;			//	Screen coords of bottom edge of application frame

	protected void createRootWindow(String mainWindowName){

		iAppFrame = new JFrame(mainWindowName);
		JOptionPane.setRootFrame(iAppFrame);
		iAppFrame.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				ActionListener al = new ExitCommandAction();
				al.actionPerformed(null);
			}
		});
		Container contentPane = iAppFrame.getContentPane();
		
		//	Create the menu bar and its action listeners.
		///////	this.createMenuBar();
		
		//	Position the root window at the top of the screen, and make it as wide as the screen,
		//	and as tall as the menu bar.
		//	Kludge - JFrame.setSize() apparently includes the size of the menu title bar, so
		//	we need to account for that in computing our desired height.
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		iAppFrame.setLocation(0, 0);
		iAppFrameBottom = WINDOW_TITLE_HEIGHT +
									 iAppFrame.getRootPane().getMinimumSize().height;
		iAppFrame.setSize(screenSize.width, iAppFrameBottom);

		iAppFrame.show();
	}
	****************************************************************************************/
	
	
	//	Clean up the user interface at application exit.
	protected void destroyUserInterface(){
	}
	
	
	//	Main body of application.
	abstract protected void run(String[] args);
	
	
	
	//	Return the Exit command action.  This enables windows to include a menu item
	//	that executes this action.
	public Action getExitCommandAction(){
		return iExitCommandAction;
	}
	
	
	//	Action for Exit command.
	protected final CustomAction iExitCommandAction = new CustomAction("Exit", iAM) {
	
		public void actionPerformed(ActionEvent e) {
			Application.this.doQuit();
		}
	};
	
	
	//	Implement the quit command, available as both a menu command and by closing the window.
	//	Default behavior is to immediately exit.  Subclasses will probably want to give the user the option
	//	to save the windows contents, and to keep running if there is more than one window open.
	public void doQuit(){
		destroyUserInterface();
		System.exit(0);
	}

	
	//	Request to close a specific document.
	public abstract void closeDocument(CustomAbstractDocument doc);
	
	
	//	Report to the user when an unexpected exception is encountered.
	//	This would be a great place for a debugger breakpoint.
	//	This is called both by the main application thread, and by the outermost condition
	//	handler of any threads we create.
	//	??	How can we use ThreadGroup.uncaughtException() to automatically catch 
	//	??	unexpected exceptions?
	public static void reportUnexpectedException(Throwable e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		System.err.println("Unexpected exception:");
		System.err.println(sw);
		JOptionPane.showMessageDialog(null,  sw.toString(),
                           "Unexpected internal error", JOptionPane.ERROR_MESSAGE);
    	}
	

	//	Find an image icon. 
	public ImageIcon loadImageIcon(String filename) {
		ClassLoader loader = this.getClass().getClassLoader();
		java.net.URL url = loader.getResource(ICON_PATH + filename);
		return new ImageIcon(url, filename);
	}
	
	
	//	This method is called if an exception is thrown in a Action's actionPerformed() method.
	//	This enables us to report these problems to the user, rather than having them eaten by Swing.
	//	In particular, we recognize UserError exceptions, and display them to the user.
	public static void processExceptionInAction(Throwable e){
		if (e instanceof UserError)
			((UserError)e).displayToUser();
		else 
			reportUnexpectedException(e);
	}
}
