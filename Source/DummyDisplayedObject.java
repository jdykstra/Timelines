//	DummyDisplayedObject.java - Implementation of DisplayedObject used by TimelinePane.

public class DummyDisplayedObject extends DisplayedObject {

	protected int iX;
	
	//	Constructor.
	public DummyDisplayedObject(int x){
		iX = x;
	}
	
	
	//	Accessors.
	public int getXLocation()		{		return iX;		}


	//	Update our state variable which controls whether drag handles are shown.
	public void showDragHandles(boolean b){
	}

}