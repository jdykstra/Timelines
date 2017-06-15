//	TLWindow.java - A window that displays a timeline, along with its toolbar.

//	Object Hierarchy
//	------------
//	TLWindow is one part of the "object hierarchy" around which both the user interface and the internal
//	structure of this application are built.  The three objects in the hiearchy are Application, Document, and
//	Window.  There is only one Applilcation object, which can have multiple Document objects.  Each Document
//	has one or more Windows displaying its contents.
//
//	Closing
//	-----
//	The object hierarchy has conventions about closing.  Each object has a method closeXXX(), where XXX is the next
//	level in the hiearchy.  For example, TLDocument has a method closeWindow().  The parameter passed to this method
//	specifies which of the objects at the next level is to be closed.
//
//	The implementation of the closeXXX() method calls the close() method of the object to be closed, which is where
//	most of the work of closing the object occurs.  Finally, the closeXXX() method removes the closed object from
//	its list of objects.

import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

class TLWindow extends JFrame  implements ChangeListener {

	// 	Constants ------------------------------------------------------------------------
	public static final String ICON_PATH = "Icons/";
	
	//	Constants for the tool states that the cursor can be in (as controlled by radio buttons on the toolbar).
	public static final int SELECT_TOOL = 0;
	public static final int CREATE_TOOL = 1;
	protected static final int INITIAL_CURSOR_TOOL = SELECT_TOOL;
	
	//	Miscellaneous UI layout constants.
	//	Width of separator between sections of the toolbar.
	public static final int TOOLBAR_SEPARATOR_WIDTH = 15;	
	public static final Dimension TOOLBAR_SEPARATOR_DIMENSION = new Dimension(TOOLBAR_SEPARATOR_WIDTH, 0);
	
	//	Margin around command button icons in the toolbar.
	public static final int COMMAND_BUTTON_MARGIN_SIZE = 0;	
	public static final Insets COMMAND_BUTTON_MARGIN = new Insets(COMMAND_BUTTON_MARGIN_SIZE, COMMAND_BUTTON_MARGIN_SIZE,
											COMMAND_BUTTON_MARGIN_SIZE, COMMAND_BUTTON_MARGIN_SIZE);
	
	//	Margin around tool icons
	public static final int TOOL_MARGIN_SIZE = 1;		
	public static final Insets TOOL_MARGIN = new Insets(TOOL_MARGIN_SIZE, TOOL_MARGIN_SIZE,
											TOOL_MARGIN_SIZE, TOOL_MARGIN_SIZE);
	
	//	Instance variables ----------------------------------------------------------------
	protected TLDocument iDoc;						//	Document containing our data
	protected TimePositionMapping iTPM;				//	Time/Position mapping for data panes
	protected ActionManager iAM = new ActionManager();	//  ActionManager handling enables for this object
	protected JScrollPane iScrollPane;				//	The scroll pane
	protected JScrollBar iHScrollbar;					//	The horizontal scrollbar of the scroll pane
	protected JMenu iCategoryMenu;					//	The category menu
	protected TimelinePane iTLPane;					//	Pane displaying data
	protected DragPane iDragPane;					//	Pane used for dragging (and other commands)
	protected boolean iCyclicView;					//	Use cyclic form for view
	protected Set iShownCategories;					//	Categories currently shown
	protected Set iCategoryMenuItems;				//	All category menu items
	protected ButtonGroup iToolButtonGroup;			//	The button group for tools
	protected JToggleButton iSelectTool;				//	The select tool button
	protected int iCurrentCursorTool=INITIAL_CURSOR_TOOL;		//	SELECT_TOOL or CREATE_TOOL
	
	//	Trivial accessors -------------------------------------------------------------
	public TimelinePane getTimelinePane()					{  return iTLPane;				}
	public TimePositionMapping getTimePositionMapping()		{ return iTPM;					}
	public Selection getSelection()						{ return iDragPane.getSelection();	}
	public ActionManager getActionManager()				{ return iAM;					}
	public Set getShownCategories()						{ return iShownCategories;			}
	public int getCurrentCursorTool()					{  return iCurrentCursorTool;		}

	
	//	Constructor ---------------------------------------------------------------------
	//	initialState may be null.
	TLWindow(TLDocument itsDoc, WindowState initialState){
			
		//	Initialize the JFrame.
		//	Specify our listener for window events.
		super();
		addWindowListener(iWindowListener);
		
		//	Set up our relationship with other objects.
		iDoc = itsDoc;
		iDoc.getActionManager().addChild(iAM);
		iDoc.addChangeListener(this); 
		
		//	Set our window title.
		updateWindowTitle();
		
		//	The TimePositionMapping object sets the time scale for both the timeline and the
		//	header pane.
		iCyclicView = initialState.iCyclicView;
		iTPM = new TimePositionMapping(iDoc, this, initialState.iResolution, iCyclicView);
		
		//	Initialize the shown categories to be those in the saved window state.
		iShownCategories = new HashSet(initialState.iShownCats.getAsSet());
		
		//	Set our size and position on the screen according to the saved state.
		setLocation(initialState.iWinPosition);
		setSize(initialState.iWinSize);
		
		//	Create the menu bar and add it to this window.
		setJMenuBar(createMenuBar());
		
		//	Start building the panes that make up our contents.
		//	The scroll pane contains both the timeline display and its headers.
		//	setBackingStoreEnabled(true) should not be called on the scroll pane, because
		//	changes to the selection are not visible when AWT satisfies the repaint from
		//	the backing store.
		//	??	The scrollpane currently "runs away", due to flooding of the Swing
		//	??	event queue.  See JavaSoft bug # 4242634.
		iScrollPane = new JScrollPane();
		Container contentPane = getContentPane();
		contentPane.add(iScrollPane, BorderLayout.CENTER);
		iHScrollbar = iScrollPane.getHorizontalScrollBar();

		//	Build the timeline display from a layered pane containing the grid and the timeline.
		JLayeredPane lp = new TimelineLayeredPane();
		lp.setLayout(new OverlayLayout(lp));
		iScrollPane.setViewportView(lp);
		
		GridPane gp = new GridPane(iDoc, iTPM, false);
		setTopLeftAlignment(gp);
		lp.add(gp, new Integer(0));

		iTLPane = new TimelinePane(iDoc, this, iTPM);
		setTopLeftAlignment(iTLPane);
		lp.add(iTLPane, new Integer(1));
		
		iDragPane = new DragPane(iDoc, this, iTPM, iTLPane);
		setTopLeftAlignment(iDragPane);
		lp.add(iDragPane, new Integer(2));
		
		//	Create and add the pane which displays the timeline headers.  It's especially important
		//	to enable the backing store on the viewport for this header, because with this on we only 
		//	have to draw the revealed part during an incremental scroll.  This avoids slowing down
		//	the scroll due to all of the date computations necessary to draw the complete header.
		GridPane gp2 = new GridPane(iDoc, iTPM, true);
		iScrollPane.setColumnHeaderView(gp2);
		iScrollPane.getColumnHeader().setBackingStoreEnabled(true);
		
		//	Create and add the toolbar.  This should be done after creating the
		//	data pane, so that the Actions can find it.
		CustomToolBar toolbar = new CustomToolBar();
		contentPane.add(toolbar, BorderLayout.NORTH);
		
		//	Add the zoom controls to the toolbar.
		toolbar.addSeparator(TOOLBAR_SEPARATOR_DIMENSION);
		JButton smallerCmd = toolbar.add(iZoomOutAction);
		adjustCommandButtonAppearance(smallerCmd);
		JButton largerCmd = toolbar.add(iZoomInAction);
		adjustCommandButtonAppearance(largerCmd);
		
		//	Build the category menu and add it to the toolbar.
		JMenuBar cmb = new JMenuBar();
		cmb.setAlignmentY(0.5F);
		cmb.setBorder(BorderFactory.createRaisedBevelBorder());
		iCategoryMenu = new JMenu("Categories");
		fillInCategoryMenu(iCategoryMenu);
		cmb.add(iCategoryMenu);
		toolbar.addSeparator(TOOLBAR_SEPARATOR_DIMENSION);
		toolbar.add(cmb);
		
		//	Add the selection and create tool buttons to the toolbar.  Also add them to a ButtonGroup so that only
		//	one is selected at time.
		toolbar.addSeparator(TOOLBAR_SEPARATOR_DIMENSION);
		iSelectTool = toolbar.addToggleButtonAction(iSelectToolAction);
		adjustToolButtonAppearance(iSelectTool);
		JToggleButton createTool = toolbar.addToggleButtonAction(iCreateToolAction);
		adjustToolButtonAppearance(createTool);

		iToolButtonGroup = new ButtonGroup();
		iToolButtonGroup.add(iSelectTool);
		iToolButtonGroup.add(createTool);
		AbstractButton initialSelectedButton = (INITIAL_CURSOR_TOOL == SELECT_TOOL) ? iSelectTool : createTool;
		iToolButtonGroup.setSelected(initialSelectedButton.getModel(), true);
		
		//	Add the lock toggle button to the toolbar.
		JToggleButton lockButton = new JToggleButton(Application.gApp.loadImageIcon("Lock Open.GIF"),  iDoc.isContentLocked());
		lockButton.setSelectedIcon(Application.gApp.loadImageIcon("Lock Closed.GIF"));
		lockButton.addActionListener(iDoc.iLockContentAction);
		lockButton.setBorderPainted(false);
		toolbar.addSeparator(TOOLBAR_SEPARATOR_DIMENSION);
		toolbar.add(lockButton);
		
		//	Give the ActionManager a chance to initialize all command enables.
		iAM.updateAllActionEnables();
		
		//	Make ourselves visible.
		show();

		//	Set the scroll position from the saved window state.
		//	??	This must be called after the window is shown (see ensureScrollbarModelUpToDate()).
		//	??	If this is cleaned up, then this code may be moved earlier in the method.
		TimePeriod scrollPosition = new ConcreteTimePeriod(initialState.iScrollPosition,
															 initialState.iScrollPosition);
		setVisiblePeriod(scrollPosition);
	}
	
	
	//	Adjust the appearance of an AbstractButton that represents a command, per our visual design.
	protected AbstractButton adjustCommandButtonAppearance(AbstractButton button){
		button.setMargin(COMMAND_BUTTON_MARGIN);
		return button;
	}
	
	
	//	Adjust the appearance of an AbstractButton that represents a tool, per our visual design.
	protected AbstractButton adjustToolButtonAppearance(AbstractButton button) {
		button.setMargin(TOOL_MARGIN);
		return button;
	}
	
	
	//	Fill in the contents of the category menu.
	protected JMenu fillInCategoryMenu(JMenu categoryMenu){
		
		//	Build the first part of the menu, containing the "show all" commands.
		categoryMenu.add(iCategoryShowAll);
		categoryMenu.add(iCategoryShowNone);
		categoryMenu.add(new JSeparator());
		categoryMenu.add(iEditCategoriesAction);
		categoryMenu.add(new JSeparator());
		
		//	Enable the "show all" commands according to the current shown categories.
		Set definedCategories = iDoc.getDefinedCategories();
		iCategoryShowAll.setEnabled(!iShownCategories.equals(definedCategories));
		iCategoryShowNone.setEnabled(iShownCategories.size() > 0);

		//	Build the part of the menu that contains the individual category names as checkbox menu items.
		iCategoryMenuItems = new HashSet();
		Iterator iter = definedCategories.iterator();
		while (iter.hasNext()){
			Category cat = (Category)iter.next();
			LabelInfo li = cat.getLabelInfo();
			JMenuItem mi = new JCheckBoxMenuItem(li.getLabel());
			mi.setForeground(cat.getColor());
			mi.addActionListener(iCategoryItemAction);
			categoryMenu.add(mi);
			iCategoryMenuItems.add(mi);
			mi.setSelected(iShownCategories.contains(cat));
		}
		
		return categoryMenu;
	}
	
	
	//	Helper routine for constructor, which sets the alignment values of the provided component.
	//	The Alignment values are only relevant when the component is used in the data pane,
	//	because JLayeredPane uses OverlayLayout, which uses SizeRequirements in
	//	alignment mode.
	//	??	In fact, I think that the alignment values are only needed in the data pane
	//	??	because of a bug in SizeRequirements in Swing 1.0.3.  See the constructor
	//	??	in TimelinePane for details.
	protected void setTopLeftAlignment(JComponent c){
		c.setAlignmentX(LEFT_ALIGNMENT);
		c.setAlignmentY(TOP_ALIGNMENT);
	}
	
	
	//	Create the menu bar for the main application window.
	protected JMenuBar createMenuBar(){
	
		//	??	As of 10/1/98, the character code passed to KeyStroke.getKeyStroke()
		//	??	seems to have to be in upper-case, even though the keystroke is effectively
		//	??	lower-case (since the Shift modifier is not specified).
		JMenuBar mb = new JMenuBar();

		//	The File menu.
		JMenu fileMenu = new JMenu("File");
		mb.add(fileMenu);
		
		JMenuItem mi = fileMenu.add(((Timelines)Application.gApp).iNewCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('N', java.awt.Event.CTRL_MASK));
		
		mi = fileMenu.add(((Timelines)Application.gApp).iOpenCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('O', java.awt.Event.CTRL_MASK));
		
		mi = fileMenu.add(iDoc.iSaveCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('S', java.awt.Event.CTRL_MASK));
		
		mi = fileMenu.add(iDoc.iSaveAsCommandAction);

		fileMenu.add(new JSeparator());

		mi = fileMenu.add(iDoc.iImportKNAction);
		mi = fileMenu.add(iDoc.iImportOutlookAction);

		fileMenu.add(new JSeparator());

		mi = fileMenu.add(iCloseCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('W', java.awt.Event.CTRL_MASK));

		fileMenu.add(new JSeparator());

		mi = fileMenu.add(((Timelines)Application.gApp).getExitCommandAction());
		mi.setAccelerator(KeyStroke.getKeyStroke('Q', java.awt.Event.CTRL_MASK));

		//	The Edit menu.
		JMenu editMenu  = new JMenu("Edit");
		mb.add(editMenu);

		EditManager editManager = iDoc.getEditManager();
		mi = editMenu.add(editManager.iUndoAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('Z', java.awt.Event.CTRL_MASK));

		mi = editMenu.add(editManager.iRedoAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('Y', java.awt.Event.CTRL_MASK));

		editMenu.add(new JSeparator());

		mi = editMenu.add(iCutCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('X', java.awt.Event.CTRL_MASK));

		mi = editMenu.add(iCopyCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('C', java.awt.Event.CTRL_MASK));

		mi = editMenu.add(iPasteCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('V', java.awt.Event.CTRL_MASK));

		mi = editMenu.add(iClearCommandAction);

		editMenu.add(new JSeparator());

		mi = editMenu.add(iDoc.iFindCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('F', java.awt.Event.CTRL_MASK));

		mi = editMenu.add(iDoc.iFindAgainCommandAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('G', java.awt.Event.CTRL_MASK));

		mi = editMenu.add(iDoc.iFindAllCommandAction);

		editMenu.add(new JSeparator());
		
		mi = editMenu.add(iNewStateAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('N', java.awt.Event.CTRL_MASK));
		
		mi = editMenu.add(iGetInfoAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('I', java.awt.Event.CTRL_MASK));
		
		mi = editMenu.add(iEditCategoriesAction);

		//	The View Menu.
		JMenu viewMenu = new JMenu("View");
		mb.add(viewMenu);

		mi = viewMenu.add(iGotoAction);
		mi.setAccelerator(KeyStroke.getKeyStroke('J', java.awt.Event.CTRL_MASK));

		mi = viewMenu.add(new JCheckBoxMenuItem("Cyclic", iCyclicView));
		mi.addActionListener(iCyclicAction);
		
		return mb;
	}
	
	
	//	Ensures that the select tool is the currently-active tool.
	public void enableSelectTool(){
		iToolButtonGroup.setSelected(iSelectTool.getModel(), true);
		iSelectTool.repaint();
		iSelectToolAction.actionPerformed(null);
	}
	
	
	//	Called (by TLDocument) when the document time range changes.  
	//	This is a level of indirection so that the document doesn't have to know about TPM's.
	public void documentTimeRangeChanged(){
		iTPM.documentTimeRangeChanged();
	}
	
	
	//	Get the amount of time (in millis) visible at the current timeline pane width.
	public long getTimelineWidthInMillis(){
		return iTPM.xDeltaToTimeDelta(getHScrollWidth());
	}
	
	
	//	Scroll the Timeline pane to reveal the specified time period.
	//	If the period is larger than the window, center the window on the period.
	//	If the period is smaller than the window, center the period in the window.
	public void setVisiblePeriod(TimePeriod p){

		//	Determine the width of the scrollpane, in time units.  Also determine the
		//	"width" of the requested period.
		long paneWidthTime = iTPM.xDeltaToTimeDelta(getHScrollWidth());
		long requestWidthTime = p.getDuration();
		
		//	Compute a new value for the scroll position that aligns the centers of
		//	the request period and the timeline pane.
		long newPositionTime = p.getPeriodStart() - (paneWidthTime - requestWidthTime)/2;
		TimePeriod newVisiblePeriod = new ConcreteTimePeriod(newPositionTime, 
												newPositionTime + paneWidthTime);
														
		//	Make sure the TPM's mapped period includes the period we want to display.
		iTPM.ensureIncludedInMappedTimePeriod(newVisiblePeriod);
		
		//	Set the new scroll position.
		setHScrollPosition(iTPM.timeToXPosition(newPositionTime));
	}
	
	
	//	Get the time period currently revealed by the window's scroll position.
	public TimePeriod getVisiblePeriod(){

		//	Get the current edges of the visible area, in graphic coordinates.
		int leftEdge = getHScrollPosition();
		int rightEdge = leftEdge + getHScrollWidth();
		
		//	Convert each edge to time, and create a TimeRange to represent it.
		long leftTime = iTPM.xPositionToTime(leftEdge);
		long rightTime = iTPM.xPositionToTime(rightEdge);

		return new ConcreteTimePeriod(leftTime, rightTime);
	}
	
	
	//	Select the specified state (discarding any previous selection), and scroll
	//	the window to show it.
	public void select(TLState state){
		setVisiblePeriod(state);
		Selection selection = getSelection();
		selection.clear();
		DisplayedState dobj = iTLPane.getDisplayedStateFromState(state);
		selection.add(dobj);
	}
	
	
	//	Select the specified states (discarding any previous selection), and scroll
	//	the window to show them.  If the states cover a wider range than can be
	//	shown in the window, it is centered on the range.
	public void select(Collection states){
		ConcreteTimePeriod coveredPeriod = null;
		Selection selection = getSelection();
		selection.clear();
		Iterator iter = states.iterator();
		while (iter.hasNext()){
			TLState state = (TLState)iter.next();
			if (coveredPeriod == null)
			  coveredPeriod = new ConcreteTimePeriod(state);
			else
			  coveredPeriod.cover(state);
			DisplayedState dobj = iTLPane.getDisplayedStateFromState(state);
			selection.add(dobj);
		}
		if (coveredPeriod != null)
		  setVisiblePeriod(coveredPeriod);
	}


	 //	Report whether the provided TLState is currently shown.
	 public boolean isShown(TLState state){
		//	The loop is designed assuming that in general, the numbers of categories a state
		//	belongs to will be less than the number of shown categories.
		//	We special-case states that are members of zero categories, and always display them.
		Iterator iter = state.getCategories().getAsSet().iterator();
		boolean shown = !iter.hasNext();
		while (!shown && iter.hasNext())
			shown = iShownCategories.contains(iter.next());
		return shown;
	 }
	 
	 
	//	Update the window title.
	public void updateWindowTitle(){
		setTitle(Application.gApp.getName() + " - " + iDoc.getName());
	}
	
	
	//	Close this window.
	public void close(){
		dispose();
	}
	
	
	//	Set the "shown" state of all categories in the menu to either true or false.
	protected void setAllCategoryMenuItems(boolean show){
		Iterator iter = iCategoryMenuItems.iterator();
		while (iter.hasNext()){
			JCheckBoxMenuItem mi = (JCheckBoxMenuItem)iter.next();
			mi.setSelected(show);
		}
	}
	
	
	//	Return a WindowState object describing the current state of the window.
	public WindowState getWindowState(){
		WindowState ws = new WindowState(iDoc.getDefinedCategories());
		
		ws.iWinPosition = getLocation();
		ws.iWinSize = getSize();
		ws.iResolution = iTPM.getScale();
		ws.iScrollPosition = iTPM.xPositionToTime(getHScrollPosition() + 
						getHScrollWidth() / 2 );
		ws.iShownCats = iDoc.getDefinedCategories().getSharedMemberSet(iShownCategories);
		ws.iCyclicView = iCyclicView;
		
		return ws;
	}
	
	
	//	Encapsulation of horizontal scrolling -----------------------------------------------
	
	//	These methods hide the way that horizontal scrolling is implemented and controlled.  
	//	Using the Swing mechanisms for this leads to some kludges, and a better way may be found in the future.
	
	//	Get the current horizontal scroll position (the coordinate of the left edge of the viewport).
	protected int getHScrollPosition(){
		ensureScrollbarModelUpToDate();
		return iHScrollbar.getValue();
	}
	
	
	//	Set the current horizontal scroll position (the coordinate of the left edge of the viewport).
	protected void setHScrollPosition(int newValue){
		iHScrollbar.setValue(newValue);
	}

	
	//	Get the width of the viewport in graphic coordinates.
	protected int getHScrollWidth(){
		ensureScrollbarModelUpToDate();
		return iHScrollbar.getVisibleAmount();
	}
	
	
	//	Ensure that the horizontal scrollbar's model reflects the current window and TPM settings.
	//	This should only be called from other methods in this section that encapsulate the horizontal scrollbar.
	//	Since the model's values come from the sizes of some components of the timeline window,
	//	we must ensure that these components are valid, in the AWT sense.
	//	Since AWT (wisely) doen't try to layout components that are not shown, this method
	//	can only be called once this window is visible on the screen.
	//	??	This dependency is inelegant and problem-prone.  Is there any alternative?
	private void ensureScrollbarModelUpToDate(){
		Debug.assert(isShowing());
		if (!isValid())
			validate();
	}
	
	
	//	Change handling -------------------------------------------------------------------------------------------
	//	Implement the stateChanged() method of the ChangeListener interface.
	//	This is called when the document or the TPM changes.
	public void stateChanged(ChangeEvent e){
		Object source = e.getSource();
		if (source == iDoc)
			docStateChanged(e);
		else if (source == iTPM)
			;  //	No need to do anything when TPM changes.
		else
			throw new ImplementationException("Received stateChanged event from unexpected source - " +
					source.toString());
	}
	
	
	//	Handle ChangeEvent's from the document.
	protected void docStateChanged(ChangeEvent ev){

		//	If the edit changed the document's categories, rebuild our category menu.
		//	If the edit changed the document's name, update our window title.
		//	If the edit was removing states, update the selection.
		if (ev instanceof CategoryChange){
			iCategoryMenu.removeAll();
			fillInCategoryMenu(iCategoryMenu);
		}
		else if (ev instanceof DocumentNameChange){
			updateWindowTitle();
		}
		else if (ev instanceof StateAddDeleteChange){
			StateAddDeleteChange event = (StateAddDeleteChange)ev;
			Set affectedStates = event.getAffectedStates();

			//	If the edit deleted states, make sure that none of them are in the selection.
			if (event.isDeleting())
				getSelection().removeStates(affectedStates);
		}
	}


	//	Window event handling --------------------------------------------------------
	
	//	??	Need to add explicit hook into Application to handle window closing.  The override in Timeline
	//	??	should only quit the app if only one document is open.
	protected WindowListener iWindowListener = new WindowAdapter(){
		public void windowClosing(WindowEvent e){
			iDoc.closeWindow(TLWindow.this);
		}
	};
	
	
	//	Command Actions ------------------------------------------------------------

	protected TLAction iGotoAction = new TLAction("Goto...", this) {
	
		public void actionPerformed(ActionEvent ev) {
		
			try {
				//	Get the user input.
				//	??	Why is the command-line window flash through after this?
				String userInput = JOptionPane.showInputDialog(TLWindow.this, "Go to when?");
				if (userInput == null)
					return;
				
				//	Parse the user's input.
				//	??	If we specify a time and date formatter, it insists on getting a time.
				//	??	How do we enable the user to enter a time if necessary, but assume reasonable
				//	??	defaults if not provided?
				Date date;
				try {
					DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
					format.setLenient(true);
					date = format.parse(userInput);
				}
				catch (ParseException e){
					JOptionPane.showMessageDialog(null, "Date not recognized.", "Goto command",  JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//	Compute the time period displayed by the window when centered at the given date.
				long width = TLWindow.this.getTimelineWidthInMillis();
				long start = date.getTime() - width/2;
				long end = date.getTime() + width/2;
				TimePeriod period = new ConcreteTimePeriod(start, end);
				
				//	Now set the window's scroll position to the period computed above.
				TLWindow.this.setVisiblePeriod(period);
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};

	
	protected TLAction iCloseCommandAction = new TLAction("Close", this){
	
		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				//	??	Need code here.
				throw new ImplementationException("Not implemented yet");
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	protected TLAction iEditCategoriesAction = new TLAction("Categories...", this){
	
		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				CategoryListDialog.doDialog(iDoc, TLWindow.this);
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	protected TLAction iCutCommandAction = new TLAction("Cut", this){
	
		public void updateEnable(){
			setEnabled(!iDoc.isContentLocked() && iDragPane.getSelection().size() > 0);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				Set affectedStates = getSelection().getStates();
				AddDeleteStateEdit edit = new AddDeleteStateEdit(affectedStates, true);
				iDoc.executeEdit(edit);
				//	??	When clipboard is implemented, put the cut states into the clipboard.
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	protected TLAction iCopyCommandAction = new TLAction("Copy", this){
	
		public void updateEnable(){
			setEnabled(!iDoc.isContentLocked() && iDragPane.getSelection().size() > 0);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				//	??	Need code here.
				throw new ImplementationException("Not implemented yet");
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	protected TLAction iPasteCommandAction = new TLAction("Paste", this){
	
		public void updateEnable(){
			setEnabled(!iDoc.isContentLocked());
		}

		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				//	??	Need code here.
				throw new ImplementationException("Not implemented yet");
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	protected TLAction iClearCommandAction = new TLAction("Clear", this){
	
		public void updateEnable(){
			setEnabled(!iDoc.isContentLocked() && iDragPane.getSelection().size() > 0);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				Set affectedStates = getSelection().getStates();
				AddDeleteStateEdit edit = new AddDeleteStateEdit(affectedStates, true);
				iDoc.executeEdit(edit);
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	//	??	iZoomInAction and iZoomOutAction are almost identical except for
	//	??	 a couple of details.  Perhaps having a common superclass would be a 
	//	??	good idea.
	protected TLAction iZoomInAction = new TLAction(null, Application.gApp.loadImageIcon("Larger.gif"), this){
	
		public void updateEnable(){
			setEnabled(iTPM.getScale() > TimeUnit.MIN_VALUE);
		}

		public void actionPerformed(ActionEvent e){
			try {
				//	Let our superclass do its processing first.
				super.actionPerformed(e);
			
				//	If there is a selection, determine its time range.  If not,
				//	remember the previous scroll position of the window.
				Selection selection = getSelection();
				TimePeriod rangeToReveal;
				if (selection.size() > 0)
					rangeToReveal = selection.getTimePeriod();
				else
					rangeToReveal = TLWindow.this.getVisiblePeriod();
				
				//	Change the scale of the TimePositionMapping.
				iTPM.setScale(iTPM.getScale() - 1);
				
				//	Scroll to show the time period we saved above.
				TLWindow.this.setVisiblePeriod(rangeToReveal);
				
				//	Update all window enable states.  This is overkill for a scale change, but doesn't hurt.
				iAM.updateAllActionEnables();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	protected TLAction iZoomOutAction = new TLAction(null, Application.gApp.loadImageIcon("Smaller.gif"), this){
	
		public void updateEnable(){
			setEnabled(iTPM.getScale() < TimeUnit.MAX_VALUE);
		}

		public void actionPerformed(ActionEvent e){
			
			try {
				//	Let our superclass do its processing first.
				super.actionPerformed(e);
			
				//	If there is a selection, determine its time range.  If not,
				//	remember the previous scroll position of the window.
				Selection selection = getSelection();
				TimePeriod rangeToReveal;
				if (selection.size() > 0)
					rangeToReveal = selection.getTimePeriod();
				else
					rangeToReveal = TLWindow.this.getVisiblePeriod();
				
				//	Change the scale of the TimePositionMapping.
				iTPM.setScale(iTPM.getScale() + 1);
				
				//	Scroll to show the time period we saved above.
				TLWindow.this.setVisiblePeriod(rangeToReveal);
				
				//	Update all window enable states.  This is overkill for a scale change, but doesn't hurt.
				iAM.updateAllActionEnables();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	protected TLAction iSelectToolAction = new TLAction(null, Application.gApp.loadImageIcon("Select.gif"), this){
		public void actionPerformed(ActionEvent e){
			iCurrentCursorTool = SELECT_TOOL;
			//	??	We really want to explicitly specify the arrow cursor, rather than
			//	??	whatever the DEFAULT_CURSOR happens to be.  However, there doesn't
			//	??	seem to be a predefined cursor for this.
			iDragPane.setCursor(Cursor.getPredefinedCursor(DEFAULT_CURSOR));
		}
	};


	protected TLAction iCreateToolAction = new TLAction(null, Application.gApp.loadImageIcon("Create.gif"), this){

		public void updateEnable(){
			boolean enable = !iDoc.isContentLocked();
			this.setEnabled(enable);
			
			//	Make sure the select tool is selected if we are disabled.
			if (!enable)
				enableSelectTool();

		}

		public void actionPerformed(ActionEvent e){
			iDragPane.getSelection().clear();
			iCurrentCursorTool = CREATE_TOOL;
			iDragPane.setCursor(Cursor.getPredefinedCursor(MOVE_CURSOR));
		}
	};


	protected TLAction iCyclicAction = new TLAction(this){
	
		//	??	This method has no effect on the menu item, because the menu item is a checkbox
		//	??	created manually, not an Action added directly to the menu.  This could probably
		//	??	be fixed by creating a linkage object which listened for property changes on the
		//	??	TLAction, and changed the enable of the menu item appropriately.  See CustomToolBar
		//	??	for an example of how this is done.
		public void updateEnable(){
			setEnabled(!iDoc.isContentLocked());
		}

		public void actionPerformed(ActionEvent e){
			iCyclicView = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			iTPM.setCyclicView(iCyclicView);
		}
	};


	protected TLAction iNewStateAction = new TLAction("New State", this){
	
		public void updateEnable(){
			setEnabled(!iDoc.isContentLocked());
		}


		public void actionPerformed(ActionEvent ae) {
			try {
				super.actionPerformed(ae);
				
				TLState newState = StateAttributeDialog.doDialog(iDoc, TLWindow.this, null);
				if (newState != null){
					Set affectedStateSet = new HashSet();
					affectedStateSet.add(newState);
					
					AddDeleteStateEdit e = new AddDeleteStateEdit(affectedStateSet, false);
					iDoc.executeEdit(e);
				}
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	public TLAction iGetInfoAction = new TLAction("Get Info", this){
	
		public void updateEnable(){
			setEnabled(TLWindow.this.getSelection().size() == 1);
		}


		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				
				//	Get the state which is currently selected.
				//	??	Is it useful for this to handle multiple states in the selection?
				Iterator iter = TLWindow.this.getSelection().iterator();
				DisplayedState dobj = (DisplayedState)iter.next();
				TLState selectedState = dobj.getState();
				
				//	Display the state's info to the user.
				//	??	If this state's info cannot be changed (it or the document is locked),
				//	??	then we should indicate that to the user.  One way would be to only
				//	??	provide a cancel in the dialog box.  A better way would be to use a dialog
				//	??	format that showed the values, but not in editing boxes.
				TLState newValue =  StateAttributeDialog.doDialog(iDoc, TLWindow.this, selectedState);
				
				//	If the user clicked OK, tell the document that its values (may) have been modified.
				//	??	Would it be better to explicitly determine whether the values have been modified,
				//	??	before doing this?  This probably means manually checking each value for a change...
				if (newValue != null){
					TLUndoableEdit edit = new StateEdit(selectedState, newValue);
					iDoc.executeEdit(edit);
				}
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	//	Complete processing necessary when a category command is issued.
	protected void completeCategoryCommand(){

		//	Enable the "show all" commands according to the current shown categories.
		Set definedCategories = iDoc.getDefinedCategories();
		iCategoryShowAll.setEnabled(!iShownCategories.equals(definedCategories));
		iCategoryShowNone.setEnabled(iShownCategories.size() > 0);
		
		//	Rebuild the timeline with the new shown categories.
		getSelection().removeUnshownCategories(iShownCategories);
		iTLPane.shownCategoriesChanged();
	}


	//	This Action is associated with the items in the Category menu that correspond to categories. 
	Action iCategoryItemAction = new AbstractAction() {

		public void actionPerformed(ActionEvent e) {
			try {
				//	We figure out which menu item was selected by matching its Action's name to the
				//	category names.
				String name = e.	getActionCommand();
				Iterator iter = TLWindow.this.iDoc.getDefinedCategories().iterator();
				Category cat = null;
				while (iter.hasNext()){
					cat = (Category)iter.next();
					if (name.equals(cat.getLabelInfo().getLabel()))
						break;
					Debug.assert(iter.hasNext());
				}
				JCheckBoxMenuItem mi = (JCheckBoxMenuItem)e.getSource();
				if (mi.isSelected())
					iShownCategories.add(cat);
				else
					iShownCategories.remove(cat);
						
				//	The menu item state is updated by Swing.
				
				TLWindow.this.completeCategoryCommand();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};
	
	
	//	This action defines the "Show All" item in the Category menu.
	protected Action iCategoryShowAll = new AbstractAction("Show All"){
	
		public void actionPerformed(ActionEvent e) {
			try {
				iShownCategories.addAll(iDoc.getDefinedCategories());
				TLWindow.this.setAllCategoryMenuItems(true);
				TLWindow.this.completeCategoryCommand();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	//	This action defines the "Show None" item in the Category menu.
	protected Action iCategoryShowNone = new AbstractAction("Show None"){
	
		public void actionPerformed(ActionEvent e) {
			try {
				iShownCategories.clear();
				TLWindow.this.setAllCategoryMenuItems(false);
				TLWindow.this.completeCategoryCommand();
			}
			catch (Throwable ex){
				Application.processExceptionInAction(ex);
			}
		}
	};


	////	protected TLAction iOpenCommandAction = new TLAction(xxx){
	////	
	////		public void actionPerformed(ActionEvent e) {
	////			super.actionPerformed(e);
	////		}
	////	};


}