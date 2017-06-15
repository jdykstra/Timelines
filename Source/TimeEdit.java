//	TimeEdit.java - Represent an edit in which one or more time parameters of the state are changed.

import java.util.*;
import java.util.List;

public class TimeEdit extends TLUndoableEdit {

	protected long[] iDelta;			//	Change in each parameter (millis)
	
	
	//	Constructor.
	public TimeEdit(Set affectedStates, long[] del){
		super(affectedStates);
		
		//	Use the delta array provided by our caller, without copying.
		iDelta = del;
	}
	
	
	//	Accessors.
	public long[] getDelta()			{		return iDelta;		}
}