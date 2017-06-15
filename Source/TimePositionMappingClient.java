//	TimePositionMappingClient.java - A Swing component who gets positioning info from a TimePositionMapping.

interface TimePositionMappingClient {

	//	Our TimePositionMapping calls this method when the mapping changes.
	public void notifyScaleChanged();

}