//	DisplayedObject.java - Abstract base class for all displayed objects.

public abstract class DisplayedObject extends Object {


	public abstract int getXLocation();

	//	Update our state variable which controls whether drag handles are shown.
	public abstract void showDragHandles(boolean b);


}