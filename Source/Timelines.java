//	Timelines.java - Main application body for Timelines.

//	To Do:
//	X	Restructuring windows and menus.
//	X	Save file
//	X	Creating new states via command
//	X	Creating new states via dragging
//	X	Deleting states
//	X	Window time range extension
//	X	Go to command
//		Renaming events in main window
//	X	Showing selection after scale change
//	X	Categories!
//	X	Add creation and modification timestamps to states (or LabelInfo)
//		Improve updateAllActionEnables() mechanism re head comments in ActionManager.


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;


public class Timelines extends Application {

	//	Compile-time options.


	//	Constants.
	protected static final String APPLICATION_NAME = "Timelines";
	protected static final String APPLICATION_VERSION = "1.5";
	protected static final String[] FILE_EXTENSIONS = {"MTL", "TML"};
	protected static final FileFilter MAC_FILE_FILTER =
							new TLFileFilter(FILE_EXTENSIONS ,"Timeline files (*.mtl, *.tml)");

	//	Instance variables.
	protected List iDocuments;					//	Open documents


	//	Trivial accessors.
	public String getName()		{return APPLICATION_NAME;			}
	public String getVersion()		{return APPLICATION_VERSION;			}


	//	Entry point for executing application.
	public static void main(String[] args){

		// Set system properties so that we use the MacOS menu bar.  This does no harm on other platforms.
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", APPLICATION_NAME);

		gApp = new Timelines();
		gApp.run(args);
	}


	//	Constructor.
	private Timelines(){
		super();

		iDocuments = new ArrayList();

		//	Start all command enables in their proper states.
		iAM.updateAllActionEnables();
	}


	//	Main body of Timelines application.
	protected void run(String[] args){

	    this.checkRuntimeEnvironment();

		this.initializeWindowSystem();

		//	If any file names were provided to us as command arguments, open them.
		//	Otherwise, create a new empty document.
		if (args.length > 0){
			for (int i = 0; i < args.length; i++)
				openExistingFile(new File(args[i]));
			if (iDocuments.size() == 0)
				doQuit();
		}
		else
			createNewDocument();

		//	Output something to the console window during debugging, just so
		//	that we know whether the primary task has actually exited.
		Debug.log(Debug.DETAIL, "Primary task exited.");
	}


	//	Implement the quit command, available as both a menu command and by closing the window.
	public void doQuit(){

		//	Explicitly close each open document, which gives the user a chance to save changes.
		Iterator iter = iDocuments.iterator();
		while (iter.hasNext()){
			TLDocument doc = (TLDocument)iter.next();
			doc.close();
		}
		super.doQuit();
	}


	//	Open a document, provided as a File.
	protected void openExistingFile(File file){
		try {
			TLDocument doc = TLDocument.create(file);
			iDocuments.add(doc);
		}
		catch (Exception e){
			StringBuffer msg = new StringBuffer("\"");
			msg.append(file.toString());
			msg.append("\" could not be opened due to ");
			if (e instanceof FileFormatError)
				msg.append("unrecognized file contents.");
			else {
				msg.append(e.toString());
				StringWriter sw = new StringWriter();
				try {
					PrintWriter pw = new PrintWriter(sw.toString());
					e.printStackTrace(pw);
					msg.append(sw);
				} catch (FileNotFoundException e2){
					Debug.log(Debug.INTERNAL_FAILURES, "Exception while handling exception:  " + e2.toString());
				}
			}
			Debug.log(Debug.INTERNAL_FAILURES, msg.toString());
			JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
		}
	}


	//	Create a new, empty, document.
	protected void createNewDocument(){
		TLDocument doc = TLDocument.createNew();
		iDocuments.add(doc);
	}


	//	Close a specific document.
	public void closeDocument(CustomAbstractDocument doc){

		Debug.assertOnError(iDocuments.contains(doc));

		//	Ask the document to close itself, and remove it from our list of documents.
		((TLDocument)doc).close();
		iDocuments.remove(doc);

		//	If this was the last document open, quit the application.
		if (iDocuments.size() == 0)
			this.doQuit();
	}


	//	Open command Action.
	public  final TLAction iOpenCommandAction = new TLAction("Open...", this) {
		public void actionPerformed(ActionEvent e) {
			Thread t = new OpenCommandThread();
			t.start();
		}
	};


	//	Open command thread.
	//	??	To be pure in style, this should really be a Runnable.
	class OpenCommandThread extends Thread {

		public void run(){

			//	Display the file selection dialog on the EDT, since Swing dialogs must run
			//	here.  The actual file loading still happens in this background thread.
			final JFileChooser[] chooserRef = new JFileChooser[1];
			final boolean[] userChoseOK = new boolean[1];
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						JFileChooser fc = new JFileChooser();
						fc.addChoosableFileFilter(MAC_FILE_FILTER);
						chooserRef[0] = fc;
						userChoseOK[0] = fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;
					}
				});
			}
			catch (Exception e) {
				reportUnexpectedException(e);
				return;
			}

			//	If the user chose one, open it as a new document.
			if (userChoseOK[0]){
				try {
					File file = chooserRef[0].getSelectedFile();
					openExistingFile(file);
				}
				catch (Exception e1){
					reportUnexpectedException(e1);
					return;
				}
			}
		}
	}


	//	New command Action.
	public final TLAction iNewCommandAction = new TLAction("New", this) {
		public void actionPerformed(ActionEvent e) {
			createNewDocument();
		}
	};

}
