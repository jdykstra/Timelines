//	TLDocument.java - Represent a Timelines document.//	TLDocument represents a timeline document currently open in the application.  Usually, it is associated//	with a file that contains the (possibly out of date) contents of the document.  However, there will be no//	associated file when the New command is used to create an untitled document.////	TLDocument is one part of the "object hierarchy" around which both the user interface and the internal//	structure of this application are built.  The three objects in the hiearchy are Application, Document, and//	Window.  See the class description for TLWindow for a full discussion.////	The document time range is the period from the earliest event in the document to the latest.  Usually, it//	directly relates to the boundaries of the timeline window's horizontal scrollbar, although there are endcase//	exceptions.  A document that contains no states has no time range, and getDocTimePeriod() will return null.//	Macintosh file format stored the time range along with the state list, but since this is really a cache, the//	design has been changed to compute it from scratch during file read.////	The Java-specific serialization support in this class and the other classes that make up a document were//	last tested in June 1999.  They are being left in the code in case they prove useful in the future, possibly//	for cut-and-paste.import java.awt.*;import java.awt.event.ActionEvent;import java.io.*;import java.util.*;import java.util.List;import javax.swing.*;import javax.swing.event.ChangeEvent;import javax.swing.undo.*;public class TLDocument extends CustomAbstractDocument implements Serializable {	// 	Constants ------------------------------------------------------------------------	protected static final String UNTITLED_DOCUMENT_NAME = "Untitled";	protected static final String LOCK_MENU_ITEM = "Lock Content";	protected static final String UNLOCK_MENU_ITEM = "Unlock Content";	protected static final boolean SAVE_IN_PORTABLE_FORMAT = true;	protected static final int PORTABLE_STREAM_VERSION = 7;	protected static final String TEMPORARY_FILE_PREFIX = "TIMELINE_";	protected static final int SAVE_BUFFER_SIZE = 10000;		//	Error messages.	protected static final String CONTENT_LOCK_ERROR = "the document's content is locked";		//	Instance variables ----------------------------------------------------------------	//	Significant objects we have permanent relationships with.	//	??	The current code only allows a single Window to be associated with this document.	//	??	This has been done to avoid lots of low-payback coding necessary to handle multiple windows.	//	??	However, the design should extend cleanly to the multiple window case.	protected transient TLWindow iWindow;		protected transient ActionManager iAM;	protected transient EditManager iEditManager;		//	The document contents, i.e., the states in the timeline.	protected List iStatesByStart;				//	All states, sorted by start time		//	Attributes of the document which are user-visible.	protected boolean iContentLocked;				//	Data content of document is locked	protected DefinedCategorySet iDefinedCategories;	//	Set of categories in document	protected WindowState iSavedWindowState;		//	Window position, etc.	//	Transient attributes of the document, and caches.	protected transient File iFile;					//	File containing document.  Null => none.	protected transient int iUnsavedEditCount;			//	Number of edits have been made, and not yet saved	protected transient ConcreteTimePeriod iDocTimeRange;	//	Time range included in the document.											//	Null if none (document empty)												//	State variables for the Find and Find Again commands.	protected transient String iSearchString;			//	The (uppercased) string we're searching for										//	Null => no existing search	protected transient int iSearchPosition;			//	index into iStatesByStart of starting point for search			//	Actions.	public transient TLAction iSaveCommandAction;	public transient TLAction iSaveAsCommandAction;	public transient TLAction iImportKNAction;	public transient TLAction iImportOutlookAction;	public transient TLAction iLockContentAction;	public transient TLAction iFindCommandAction;	public transient TLAction iFindAgainCommandAction;	public transient TLAction iFindAllCommandAction;	//	Trivial accessors -------------------------------------------------------------	public List getStatesByStartList()			{	return iStatesByStart;				}	public DefinedCategorySet getDefinedCategories()	{	return iDefinedCategories;			}	public boolean isContentLocked()				{	return iContentLocked;			}	public ConcreteTimePeriod getDocTimePeriod()	{	return iDocTimeRange;			}	public boolean areEditsUnsaved()				{	return iUnsavedEditCount > 0;		}	public ActionManager getActionManager()		{ 	return iAM;					}	public EditManager getEditManager()			{ 	return iEditManager;				}				//	Object creation, including file reading -----------------------------------------------		//	Static factory method that creates a TLDocument and all of its related objects 	//	from a Macintosh or persistent object  input stream, as indicted by the type of "is".	public static TLDocument create(File file) throws java.io.IOException, FileFormatError  {		long startTime = System.currentTimeMillis();		long startTotalMemory = Runtime.getRuntime().totalMemory();		long startUsedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();				//	Create the progress window.		ProgressWindow pw = new ProgressWindow("Opening " + file.toString() + "...", 100 );				//	First try reading the file as a portable byte stream.		TLDocument doc = null;		boolean successfulRead = false;		try {			DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));			doc = new TLDocument();			doc.fillInFromInputStream(is, pw);			is.close();			successfulRead = true;		} catch (FileFormatError e){			//	Ignore exception		} catch (IOException e){			throw e;		} catch (Exception e){			throw new ImplementationException("Unexpected exception while reading portable stream:  " + e.toString());		}				//	Next try reading the file as a saved object stream.  Note that we create new streams so that we start at the beginning		//	of the file again.		if (!successfulRead){			try {				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));				doc = (TLDocument)ois.readObject();				ois.close();				successfulRead = true;			} catch (StreamCorruptedException e){				//	Ignore exception			} catch (IOException e){				throw e;			} catch (Exception e){				throw new ImplementationException("Unexpected exception while reading object stream:  " + e.toString());			}		}				//	Finally, try reading the file as a saved object stream.		if (!successfulRead){			try {				doc = new TLDocument();				MacInputStream mis = new MacInputStream(new BufferedInputStream(new FileInputStream(file)));				doc.fillInFromMacInputStream(mis, pw);				mis.close();				successfulRead = true;			} catch (IOException e){				throw e;			} catch (Exception e){				throw new ImplementationException("Unexpected exception while reading Macintosh stream:  " + e.toString());			}		}						//	If we failed at all attempts, report an error to the user.		if (!successfulRead){			throw new FileFormatError("Could not recognize file format");		}				//	Initialize the instance variables that are not saved in the file.		doc.initializeTransientFields(file);				//	Close the progress window.		pw.remove();				long nowTime = System.currentTimeMillis();		long nowTotalMemory = Runtime.getRuntime().totalMemory();		long nowUsedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();		System.err.println("Open realtime = " +  (nowTime - startTime)/1000+			" seconds.");		System.err.println("Memory increment = " + (nowUsedMemory - startUsedMemory)/			1024 + " KB.");		if (nowTotalMemory != startTotalMemory){			System.err.println("Total memory started at " + startUsedMemory /					1024 + " KB.");			System.err.println("Total memory is now at " + nowTotalMemory /					1024 + " KB.");		}		return doc;	}			//	Static factory method that creates a new, empty, TLDocument and all of its related objects.	public static TLDocument createNew(){		TLDocument doc = new TLDocument();				//	Set the default values for an empty document.		doc.iStatesByStart = new ArrayList();		doc.iContentLocked = false;		doc.iDefinedCategories = new DefinedCategorySet();				//	Set up the default window information.		doc.iSavedWindowState = new WindowState(doc.iDefinedCategories);				//	Initialize transient fields, including those dependent upon the above initializations.		doc.initializeTransientFields(null);				return doc;	}			//	Create a TLDocument and all of its related objects from an Macintosh input stream.	protected void fillInFromMacInputStream(MacInputStream is, ProgressWindow pw) throws FileFormatError, IOException {			//	Read byte stream version number.		int version = is.readShort();		switch (version){					//	We only support byte stream version 6.			case 6:								//	Ignore the provided document time range;  we compute it from the state list.				is.readMacMoment();	//	Start time				is.readMacMoment();	//	End time				is.readMacBoolean();	//	Range valid				iContentLocked = is.readMacBoolean();								//	Read the master category list.				//	The Mac version of Timelines had the concept of a "default category" that contained				//	any state that was not a member of any other state.  In this version of the app, we				//	don't have a default category;  instead, we always display states that do not have any				//	categories in their MemberSet set.				//	The defined categories are normally stored as a set.  However, we				//	also build a list of them, because other parts of the Macintosh file format				//	refer to categories by index.  We put the default category in this list, but				//	not in the set used after the file is read in.				iDefinedCategories = new DefinedCategorySet(is);								//	Read the saved window state				iSavedWindowState = new WindowState(iDefinedCategories);											//	Read the window's initial position and size.  Unfortunately, the fields in MacApp's				//	Point object are in the opposite order of Java's Point.				int y = is.readShort();				iSavedWindowState.iWinPosition = new Point(is.readShort(), y);				y = is.readShort();				iSavedWindowState.iWinSize = new Dimension(is.readShort(), y);								//	Read the window resolution (what we call scale), and translate it to				//	the time unit values defined in interface TimeUnit..				iSavedWindowState.iResolution = MacInputStream.TIME_UNIT_TRANSLATOR[is.readByte()];								iSavedWindowState.iScrollPosition = is.readMacMoment();								//	Read in the shown categories.				iSavedWindowState.iShownCats = iDefinedCategories.getSharedMemberSet(is);				is.readMacBoolean();		//	Unused boolean				is.readMacBoolean();		//	Unused boolean				iSavedWindowState.iCyclicView = is.readMacBoolean();								//	Read in the events, and store them into a List.  When we read the				//	states below, we link them up to their events by the index.				int listSize = is.readInt();				ArrayList eventList = new ArrayList(listSize);				CustomGregorianCalendar tc = new CustomGregorianCalendar();				for (int i = 0; i < listSize; i++){					TLEvent evt = new TLEvent(is);					eventList.add(evt);									//	The Mac version did not define time values as boundaries between time periods,					//	and so was susceptible to endcase problems.  In particular, fix up time periods					//	that end at 23:59:59 so that they correctly specify the boundary at 00:00:00.					tc.setTimeInMillis(evt.getPeriodEnd());					if (tc.get(Calendar.HOUR_OF_DAY) == 23 && tc.get(Calendar.MINUTE) == 59 && 									tc.get(Calendar.SECOND) == 59){						tc.set(Calendar.MILLISECOND, 0);						tc.add(Calendar.SECOND, 1);						long newMilliValue = tc.getTimeInMillis();						if (evt.getDuration() == 0)							evt.setPeriodStart(newMilliValue);						evt.setPeriodEnd(newMilliValue);					}										//	Update the progress window.					pw.updateValue( (66  *  i) / listSize);				}				//	Read in the states, and sort them.				//	??	Is ArrayList the best implementation?  Is List the best model?				listSize = is.readInt();				iStatesByStart = new ArrayList(listSize);				for (int i = 0; i < listSize; i++){					TLState state = new TLState(is, eventList, iDefinedCategories);					iStatesByStart.add(state);					//	Update the progress window.					pw.updateValue( 67 + (33  *  i) / listSize);				}				pw.updateValue(100);								break;							default:				throw new FileFormatError("Unsupported file version (" + version + ")");		}	}			//	Create a TLDocument and all of its related objects from an input stream.	protected void fillInFromInputStream(DataInputStream is, ProgressWindow pw) 							throws FileFormatError, IOException {			//	Read byte stream version number.		int version = is.readShort();		switch (version){					//	We only support one byte stream format			case PORTABLE_STREAM_VERSION:								iContentLocked = is.readBoolean();								//	Read the master category list.				iDefinedCategories = new DefinedCategorySet(is);								//	Read the saved window state				iSavedWindowState = new WindowState(iDefinedCategories, is);															//	Read in the events, and store them into a List.  When we read the				//	states below, we link them up to their events by the index.				int listSize = is.readInt();				ArrayList eventList = new ArrayList(listSize);				for (int i = 0; i < listSize; i++){					TLEvent evt = new TLEvent(is);					eventList.add(evt);									//	Update the progress window.					pw.updateValue( (66  *  i) / listSize);				}				//	Read in the states, and sort them.				//	??	Is ArrayList the best implementation?  Is List the best model?				listSize = is.readInt();				Debug.assert(listSize * 2 == eventList.size());				iStatesByStart = new ArrayList(listSize);				int eventListIndex = 0;				for (int i = 0; i < listSize; i++){					TLState state = new TLState(is, (TLEvent)eventList.get(eventListIndex++), 									(TLEvent)eventList.get(eventListIndex++), iDefinedCategories);					iStatesByStart.add(state);					//	Update the progress window.					pw.updateValue( 67 + (33  *  i) / listSize);				}								//	Verify internal consistency of document data.				verifyDataConsistency();								pw.updateValue(100);								break;							default:				throw new FileFormatError("Unsupported file version (" + version + ")");		}	}			//	Write this instance to a portable byte stream.	public void writeTo(DataOutputStream os)						throws IOException {		os.writeShort(PORTABLE_STREAM_VERSION);				os.writeBoolean(iContentLocked);				//	write the master category list.		iDefinedCategories.writeTo(os);				//	Write the saved window state		iSavedWindowState.writeTo(os);											//	Write out all TLEvents, in the order they are referenced by the TLState list.		os.writeInt(iStatesByStart.size() * 2);		Iterator iter = iStatesByStart.iterator();		while (iter.hasNext()){			TLState state = (TLState)iter.next();			state.getStartingEvent().writeTo(os);			state.getEndingEvent().writeTo(os);		}				//	Write out the states.		os.writeInt(iStatesByStart.size());		iter = iStatesByStart.iterator();		while (iter.hasNext()){			TLState state = (TLState)iter.next();			state.writeTo(os);		}	}			//	This method is called immediately after a new TLDocument is filled in by reading a file.	//	It initializes instance variables that are not saved in the file.	protected void initializeTransientFields(File file){		//	Allocate miscellaneous transient objects owned by us.		iAM = new ActionManager();		Application.gApp.getActionManager().addChild(iAM);				iEditManager = new EditManager(this);		this.addUndoableEditListener(iEditManager);		resetEditCount();						//	Set up our relationship with other objects.		iFile = file;				//	Create the command Action objects.		this.createActions();				//	Make sure the state lists are sorted right.		//	If we just read a serialized stream, the sort is unnecessary, but the updating of the		//	document range is still needed.		this.sortStateListsAndUpdateDocumentRange();		//	Create a window to display this document.		iWindow = new TLWindow(this, iSavedWindowState);				//	Start all command enables in their proper states.		iAM.updateAllActionEnables();	}		//	Miscellaneous routines ---------------------------------------------------------		//	Increment the count of the number of edits that have been made to this document.	//	Each count corresponds to a single TLUndoableEdit and Change object.	public void incrementEditCount(){		iUnsavedEditCount++;		iAM.updateAllActionEnables();	}			//	Decrement the count of the number of edits that have been made to this document.	public void decrementEditCount(){		iUnsavedEditCount--;		Debug.assert(iUnsavedEditCount >= 0);		iAM.updateAllActionEnables();	}			//	Zero the count of the number of edits that have been made to this document.	public void resetEditCount(){		iUnsavedEditCount = 0;		iAM.updateAllActionEnables();	}			//	Ensure that our two state lists are sorted correctly, and update the document time range as necessary 	//	for it to include all of the states in the state list.	protected void sortStateListsAndUpdateDocumentRange(){			TimePeriod old = iDocTimeRange;		//	Handle end case of empty document.		if (iStatesByStart.size() == 0){			if (old != null){				iDocTimeRange = null;				iWindow.documentTimeRangeChanged();			}			return;		}				//	Do the sort.		Collections.sort(iStatesByStart, TLUtilities.SORT_UP_BY_START_COMPARATOR);				//	Extract the current time range.		TLState firstState = (TLState) iStatesByStart.get(0);		TLState lastState = (TLState) iStatesByStart.get(iStatesByStart.size()-1);		long beginning = firstState.getTimeParameter(TLState.T0);		long ending = lastState.getTimeParameter(TLState.T3);				//	If the current time range is different than it was, update our instance variable, 		//	and notify the window (assuming there is one).		if ((old == null) || (old.getPeriodStart() != beginning) || (old.getPeriodEnd() != ending)){			iDocTimeRange = new ConcreteTimePeriod(beginning, ending);			if (iWindow != null)				iWindow.documentTimeRangeChanged();		}	}			//	Return the name of this document.  This is generally the name of the file, but is "Untitled" if	//	there is no backing file.	public String getName(){		if (iFile != null){					//	Strip the extension off the filename before we return it.			//	??	On platforms without file extensions, this will remove parts of names that			//	??	happen to have periods in them.			String filename =  iFile.getName();			int i = filename.lastIndexOf('.');			if (i > 0 && i < filename.length()-1)				return filename.substring(0, i);			else				return filename;		}		else			return UNTITLED_DOCUMENT_NAME;					}	//	Save this document in serialized object format.	//	??	It would be nice to provide a progress bar for this operation.	//	??	At present, we do not cut back the undo list maintained by EditManager.  This means that	//	??	you can undo back through a save, but it also means that the edit list, and the objects that	//	??	they refer to, are never released.	protected void doSave(File userFile){		try {			//	Verify internal consistency of document data.			verifyDataConsistency();			//	Write this document's data to a temporary file.  We'll copy this to the file the user			//	specified once we're sure that we can save the whole document.			//	We put this file in the same directory as the user's file, rather than using the platform's			//	temporary file directory, under the assumption that, if anything goes wrong, the user is			//	more likely to find his/her data this way.			File tempFile = File.createTempFile(TEMPORARY_FILE_PREFIX, "", userFile.getAbsoluteFile().getParentFile());						//	Capture a snapshot of the window state.			iSavedWindowState = iWindow.getWindowState();						//	Save the document's data to the temporary file.			if (SAVE_IN_PORTABLE_FORMAT){				DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));				writeTo(os);				os.close();			}			else {				ObjectOutputStream oos;				oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)));				oos.writeObject(this);				oos.close();			}						//	Since we've successfully saved this document in the temporary file, 			//	now copy it on top of the user's file.			//	??	There is still a chance that we will leave the user's file partially written.			//	??	How does the Mac avoid putting user data in peril, while still preserving the			//	??	user's file's creation date, etc.?			InputStream is = new BufferedInputStream(new FileInputStream(tempFile));			OutputStream os = new BufferedOutputStream(new FileOutputStream(userFile));			byte[] buffer = new byte[SAVE_BUFFER_SIZE];			int count;			do {				count = is.read(buffer, 0, SAVE_BUFFER_SIZE);				os.write(buffer, 0, count);			} while (count == SAVE_BUFFER_SIZE);			is.close();			os.close();						tempFile.delete();			//	Reset the "document dirty" count.			resetEditCount();		}		catch (Exception e){			//	??	Need to handle exceptions smarter.			throw new ImplementationException(e);		}	}			//	Save this document under a new name.	protected void doSaveAs(){			//	Verify internal consistency of document data.		verifyDataConsistency();		//	Display the file selection dialog.		JFileChooser fc = new JFileChooser();		boolean userChoseOK = fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION;							if (userChoseOK){			try {				File file = fc.getSelectedFile();				if (file.exists()){					String message = "File \"" + file.getName() + "\" already exists.  Overwrite it?";					int response = JOptionPane.showConfirmDialog(null, message, "Overwrite Warning",						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);					if (response != JOptionPane.YES_OPTION)						return;				}				TLDocument.this.doSave(file);				iFile = file;				this.fireChangeUpdate(new DocumentNameChange(this));			}			catch (ImplementationException e1){				throw e1;			}			catch (Exception e1){				throw new ImplementationException(e1);			}		}	}			//	Import states into the document.	protected void doImport(Importer importer){			//	Display the file selection dialog.		JFileChooser fc = new JFileChooser();		boolean userChoseOK = fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;							if (userChoseOK){			try {				File file = fc.getSelectedFile();				Set newStates = importer.importFromFile(this, file);				if (newStates == null)					return;				AddDeleteStateEdit edit = new AddDeleteStateEdit(newStates, false);				executeEdit(edit);			}			catch (ImplementationException e1){				throw e1;			}			catch (Exception e1){				throw new ImplementationException(e1);			}		}	}			//	Search the document for the string contained in iSearchString, starting from iSearchPosition.  	//	If a match is found, scroll the window to show it.  If we don't find any, beep.	protected void searchForStringMatch(boolean findAll){		Debug.assert(iSearchString != null);		int listSize = iStatesByStart.size();		Collection matchStates = new ArrayList();		while (iSearchPosition < listSize && (findAll || matchStates.isEmpty())){			TLState state = (TLState)iStatesByStart.get(iSearchPosition++);			if (!iWindow.isShown(state))				continue;			String thisLabel = state.getLabelInfo().getLabel().toUpperCase();			if (thisLabel.indexOf(iSearchString) >= 0)				matchStates.add(state);		}				if (!matchStates.isEmpty())			iWindow.select(matchStates);		else {			forgetSearchPosition();			java.awt.Toolkit.getDefaultToolkit().beep();		}		iAM.updateAllActionEnables();	}			//	Clear out an existing search string and search position.  Used when the	//	document changes in a way that means an existing search cannot be continued.	protected void forgetSearchPosition(){		iSearchString = null;		iSearchPosition = 0;	}	//	Override of java.lang.Object.toString().	public String toString(){		String lineSeparator = System.getProperty("line.separator");		Date d1 = new Date(this.getDocTimePeriod().getPeriodStart());		Date d2 = new Date(this.getDocTimePeriod().getPeriodEnd());				StringBuffer b = new StringBuffer( "TLDocument[ DocStartTime=" +				d1.toString() + ", DocEndTime=" +				d2.toString() + ", " + lineSeparator + " DefinedCategories=" +				getDefinedCategories().toString() + ", " + lineSeparator + "States = ");		for (int i = 0; i < iStatesByStart.size(); i++){			TLState state = (TLState) iStatesByStart.get(i);			b.append((new Date(state.getPeriodStart())).toString());			b.append(", ");			b.append((new Date(state.getPeriodEnd())).toString());			b.append(", ");			b.append(state.getLabelInfo().getLabel());						b.append(lineSeparator);		}				return b.toString();	}			//	Request to close the specified window displaying this document.	public void closeWindow(TLWindow window){				//	The current implementation of TLDocument only supports a single window.		Debug.assert(window == iWindow);				//	Since the TLDocument currently supports only a single window, this is implicitly a request		//	to close the document.		//	Ask the application to close us.		Application.gApp.closeDocument(this);		//	Tell the window to close itself.		window.close();	}			//	Close this document.	//	??	Need to add a cancel option to this.	public void close(){		if (!this.areEditsUnsaved())			return;					int userReply = JOptionPane.showConfirmDialog(iWindow, "Do you want to save changes to " +			this.getName() + "?",  Application.gApp.getName(),  JOptionPane.YES_NO_OPTION);		if (userReply == JOptionPane.YES_OPTION){			if (iFile != null)				this.doSave(iFile);			else				this.doSaveAs();		}	}			//	Editing -----------------------------------------------------------------		//	Execute a new edit just commanded by the user.	//	??	Maybe the content-locked test should be enforced by preventing the user from doing anything	//	??	that causes an edit, rather than reporting the problem later.  This would reduce this to a	//	??	sanity check that throws an ImplementationException.	//	??	The sanity check should be in each individual edit operation, rather than here.	public void executeEdit(TLUndoableEdit e) throws UserError {		if (isContentLocked())			throw new UserError(CONTENT_LOCK_ERROR);		iEditManager.executeEdit(e);	}			//	Do processing that is needed for all user edits.  Called by each of the	//	individual edit processors.	//	??	Right now, we rebuild our transient data structures on every edit.  If we change this	//	??	in the future for efficiency, the update stuff will be moved elsewhere.	protected synchronized void editCommonProcessing(ChangeEvent ev){			//	Update our transient data structures to reflect the change.		this.sortStateListsAndUpdateDocumentRange();		//	Send an ChangeEvent to all listeners.		this.fireChangeUpdate(ev);	}			//	Add  a Set of TLStates to the document.	public void editAddStates(TLUndoableEdit edit, Set affectedStates){		Iterator iter = affectedStates.iterator();		while (iter.hasNext()){			TLState state = (TLState)iter.next();			Debug.assert(iStatesByStart.add(state));		}		forgetSearchPosition();		editCommonProcessing(new StateAddDeleteChange(this, affectedStates, StateAddDeleteChange.ADDING));	}			//	Remove a Set of TLStates from the document.	public void editRemoveStates(TLUndoableEdit edit, Set affectedStates){		Iterator iter = affectedStates.iterator();		while (iter.hasNext()){			TLState state = (TLState)iter.next();			int indexInList = iStatesByStart.indexOf(state);			Debug.assert(iStatesByStart.remove(state));						//	Fix up the search position to account for this change to the state list.			if (iSearchPosition > indexInList)				iSearchPosition--;		}					editCommonProcessing(new StateAddDeleteChange(this, affectedStates, StateAddDeleteChange.DELETING));	}			//	Change the value of a state.	public void editChangeState(TLUndoableEdit edit, TLState affectedState, TLState newValue){		affectedState.setAllFrom(newValue);		forgetSearchPosition();		editCommonProcessing(new StateModifyChange(this, affectedState, newValue));	}			//	Add a category.	//	Note that, at the moment, there are no Edit objects for category changes, so these methods	//	are called directly from the dialog.	public void editAddCategory(TLUndoableEdit edit, Category cat){		Debug.assert(!isContentLocked());		Debug.assert(getDefinedCategories().add(cat));		editCommonProcessing(new CategoryAddChange(this, cat));	}			//	Delete a category.	//	Note that, at the moment, there are no Edit objects for category changes, so these methods	//	are called directly from the dialog.	public void editDeleteCategory(TLUndoableEdit edit, Category cat){		Debug.assert(!isContentLocked());		//	Remove the category from all TLStates.		Iterator iter = iStatesByStart.iterator();		while (iter.hasNext()){			TLState state = (TLState)iter.next();			DefinedCategorySet.MemberSet categories = state.getCategories();			state.setCategories(categories.remove(cat));		}				//	Remove the category from the document itself.		Debug.assert(getDefinedCategories().remove(cat));		editCommonProcessing(new CategoryDeleteChange(this, cat));	}			//	Rename a category.	//	Note that, at the moment, there are no Edit objects for category changes, so these methods	//	are called directly from the dialog.	public void editRenameCategory(TLUndoableEdit edit, Category cat, String newName){		Debug.assert(!isContentLocked());		Debug.assert(getDefinedCategories().contains(cat));		cat.getLabelInfo().setLabel(newName);		editCommonProcessing(new CategoryEditChange(this, cat));	}				//	Command Actions ------------------------------------------------------------	//	??	The Save actions should perhaps be in TLWindow.		//	This method is called (after we're linked into the object hierarchy) 	//	to create the Actions.	protected void createActions(){		iSaveCommandAction = new TLAction("Save", this)  {					public void updateEnable(){				this.setEnabled(TLDocument.this.areEditsUnsaved());			}			public void actionPerformed(ActionEvent e) {				try {					if (iFile == null)						TLDocument.this.doSaveAs();					else						TLDocument.this.doSave(iFile);				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		iSaveAsCommandAction = new TLAction("Save As...", this)  {					public void updateEnable(){				this.setEnabled(true);			}			public void actionPerformed(ActionEvent e) {				try {					TLDocument.this.doSaveAs();				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		iImportKNAction = new TLAction("Import from Common Knowledge...", this)  {					public void updateEnable(){				this.setEnabled(!isContentLocked());			}			public void actionPerformed(ActionEvent e) {				try {					TLDocument.this.doImport(new CommonKnowledgeImporter());				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		iImportOutlookAction = new TLAction("Import from MS Outlook...", this)  {					public void updateEnable(){				this.setEnabled(!isContentLocked());			}			public void actionPerformed(ActionEvent e) {				try {					TLDocument.this.doImport(new MSOutlookImporter());				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		iLockContentAction = new TLAction(null, this)  {					public void updateEnable(){				this.setEnabled(true);			}			public void actionPerformed(ActionEvent e) {				try {					iContentLocked = ((AbstractButton)e.getSource()).isSelected();					iAM.updateAllActionEnables();				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		iFindCommandAction = new TLAction("Find...", this)  {					public void updateEnable(){				this.setEnabled(true);			}			public void actionPerformed(ActionEvent e) {				try {					//	We express our search position in the document as an index into iStatesByStart.					//	Reset the search anchor to the beginning of the document.					forgetSearchPosition();										//	Get the user input.					String userString = JOptionPane.showInputDialog(iWindow, "Find what?");					if (userString == null || userString.length() == 0)						return;					iSearchString = userString.toUpperCase();										//	Search the document, and update iSearchPosition.					searchForStringMatch(false);				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		//	??	Should the command-key for Find Again be F3?		iFindAgainCommandAction = new TLAction("Find Again...", this)  {					public void updateEnable(){				this.setEnabled(iSearchString != null);			}			public void actionPerformed(ActionEvent e) {				try {					//	Search the document, and update iSearchPosition.					searchForStringMatch(false);				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};		iFindAllCommandAction = new TLAction("Find All...", this)  {					public void updateEnable(){				this.setEnabled(true);			}			public void actionPerformed(ActionEvent e) {				try {					//	We express our search position in the document as an index into iStatesByStart.					//	Reset the search anchor to the beginning of the document.					forgetSearchPosition();										//	Get the user input.					String userString = JOptionPane.showInputDialog(iWindow, "Find what?");					if (userString == null || userString.length() == 0)						return;					iSearchString = userString.toUpperCase();										//	Search the document, and update iSearchPosition.					searchForStringMatch(true);				}				catch (Throwable ex){					Application.processExceptionInAction(ex);				}			}		};	}	//	Run consistency checks on the document's data, to catch data-corrupting bugs.	public void verifyDataConsistency(){		checkForDuplicateStates();	}			//	Inspect the document data for duplicated states.	protected void checkForDuplicateStates(){		if (iStatesByStart.isEmpty())			return;					//	Iterate through all states.		Iterator iter = iStatesByStart.iterator();		TLState stateM = (TLState)iter.next();		while (iter.hasNext()){			TLState stateN = (TLState)iter.next();			//	First check for matching labels, since that is so fast.  Then make sure			//	the time periods of the states match before declaring a duplicate.			if (stateM.getLabelInfo().getLabel().equals(stateN.getLabelInfo().getLabel()) &&					stateM.equalsTimePeriod(stateN)){				String msg = "State \"" + stateM.getLabelInfo().getLabel() + "\" is duplicated";				System.err.println(msg);				JOptionPane.showMessageDialog(null,  msg,                           "Possible data corruption", JOptionPane.ERROR_MESSAGE);            }			stateM = stateN;		}	}}//	An exception to throw if we can't recognize the file format.class FileFormatError extends Exception {	FileFormatError(String msg){		super(msg);	}}