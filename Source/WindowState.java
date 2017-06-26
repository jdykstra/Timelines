//	WindowState.java - Structure to store state of timeline window, so it can be restored.import java.awt.Dimension;import java.awt.Point;import java.io.*;import java.util.Set;import java.util.Date;//	A window state struct is used to save the position, size and settings of//	a window, so that they can be restored later.class WindowState extends Object implements Serializable {	// 	Constants ----------------------------------------------------------------------	protected static final int PORTABLE_STREAM_VERSION = 1;	//	Default values used for new documents.	protected static final Point DEFAULT_WINDOW_POSITION = new Point(50, 50);	protected static final Dimension DEFAULT_WINDOW_SIZE = new Dimension(800, 500);	protected static final int DEFAULT_WINDOW_SCALE = TimeUnit.WEEK;	//	Instance variables ----------------------------------------------------------------	public Point iWinPosition;				//	Position of window;  screen coords	public Dimension iWinSize;				//	Size of window	public int iResolution;					//	Current resolution setting	public long iScrollPosition;				//	Current scroll position	public DefinedCategorySet.MemberSet iShownCats;	//	Shown categories	public boolean iCyclicView;				//	View in cyclic arrangement			// 	Constructor  --------------------------------------------------------------------	//	Create an instance of this class from default values.	public WindowState(DefinedCategorySet categories) {		iWinPosition = DEFAULT_WINDOW_POSITION;		iWinSize = DEFAULT_WINDOW_SIZE;		iResolution = DEFAULT_WINDOW_SCALE;		iScrollPosition = new Date().getTime();		iShownCats = categories.getSharedMemberSet();		iCyclicView = false;	}	//	Create an instance of this class from a portable byte stream.	public WindowState(DefinedCategorySet categories, DataInputStream is)							throws FileFormatError, IOException {		Debug.assertOnError(is.readShort() == PORTABLE_STREAM_VERSION);		iWinPosition = new Point(is.readInt(), is.readInt());		iWinSize = new Dimension(is.readInt(), is.readInt());		iResolution = is.readInt();		iScrollPosition = is.readLong();		iShownCats = categories.getSharedMemberSet(is);		iCyclicView = is.readBoolean();	}	//	Write an instance to a DataOutputStream.	public void writeTo(DataOutputStream os) 							throws IOException {		os.writeShort(PORTABLE_STREAM_VERSION);		os.writeInt(iWinPosition.x);		os.writeInt(iWinPosition.y);		os.writeInt(iWinSize.width);		os.writeInt(iWinSize.height);		os.writeInt(iResolution);		os.writeLong(iScrollPosition);		iShownCats.writeTo(os);		os.writeBoolean(iCyclicView);	}	public String toString(){		Date d = new Date(iScrollPosition);		return "WindowState[ " + 			iWinPosition.toString() + ", " +			iWinSize.toString() + ", " +			iResolution + ", " +			d.toString() + ", " +			iShownCats.toString() + ", " +			(iCyclicView ? "true" : "false") + "]";	}} 