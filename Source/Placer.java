//	Placer.java - Arrange the contents of the timeline pane.

//	Placement only affects the vertical position of states.  Each DisplayedState computes its X location itself.  

import java.awt.Point;
import java.util.*;

public class Placer extends Object {

	// 	Constants------------------------------------------------------------------------
	//	Constants determining pane layout.
	static final public int LEVEL_SPACING = 25;	//	Distance between each level
	static final protected int TOP_MARGIN = 10;		//	Distrance from top of pane to first level


	//	Instance variables----------------------------------------------------------------
	//	These variables describe the extent and contents of the level lists.
	//	Each level list is sorted by the starting time of the data item.
	//	The level lists are sorted in iLevelList from top to bottom of the pane.  Since we don't put
	//	overlapping data objects in a given level, it is also true that element n ends before
	//	element n+1 begins.
	protected List iLevelList;					//	List of the levels;  each is also a List


	//	Constructor---------------------------------------------------------------------
	public Placer(){
		iLevelList = new ArrayList();
	}
	 
	 
	//	Assign all elements of the provided display list to a level, building the level lists as we go.
	public void assignToLevel(Set displayList){
	
	        long startTime = System.currentTimeMillis();
		
		//	To improve usability of the output display, try to place longer objects toward the top of the display.
		//	Do this by pre-sorting the display objects by width.
		List sortedDisplayList = new ArrayList(displayList);
		Collections.sort(sortedDisplayList, TLUtilities.SORT_DOWN_BY_DURATION_COMPARATOR);

		//	Once around for each element in the display list.
		Iterator sortedDisplayListIter = sortedDisplayList.iterator();
		while (sortedDisplayListIter.hasNext()){
			
			//	Get the next DisplayedObject.
			DisplayedState dObj = (DisplayedState) sortedDisplayListIter.next();
			
			//	Look through existing level lists for one that this will fit in.
			//	Once around for each existing level.
			boolean insertedIntoLevel = false;
			Iterator levelIter = iLevelList.iterator();
			int levelNumber = -1;
			while (!insertedIntoLevel && levelIter.hasNext()){
			
				//	Increment the level number first, so that we can use continue statement.
				levelNumber++;
			
				//	See if the current display object fits in this level.
				List curLevel = (List)levelIter.next();
				int searchResult = Collections.binarySearch(curLevel, dObj, TLUtilities.SORT_UP_BY_X_POSITION);
				
				//	If searchResult is positive, there was an exact match.  This is obviously a
				//	collision, so go on to the next level.
				if (searchResult >= 0)
					continue;
				
				//	Variable insertionPoint is where the data object would appear in this level list if
				//	it was inserted.  This calculation of insertionPoint undoes the "mangling" that 
				//	Collections.binarySearch() does to its result if it doesn't find a match.
				int insertionPoint = -(searchResult + 1);
				
				//	If insertionPoint is greater than zero, there is something in the level that starts before
				//	the current data object.  If it overlaps the new object, loop to check the next level.
				if (insertionPoint > 0){
					DisplayedState objectBefore = (DisplayedState) curLevel.get(insertionPoint - 1);
					if (objectBefore.getXLocation() + objectBefore.getWidth() >= dObj.getXLocation()){
						continue;
					}
				}
				
				//	If the size of the list is greater than insertionPoint, there is something in the
				//	level that starts after the current object.  If it overlaps the new object,
				//	loop to check the next level.
				if (curLevel.size() > insertionPoint){
					DisplayedState objectAfter = (DisplayedState) curLevel.get(insertionPoint);
					if (objectAfter.getXLocation() <= dObj.getXLocation() + dObj.getWidth()){
						continue;
					}
				}
				
				//	The current data object fits in this level, so put it in.  Since we use the
				//	insertion point that Collections.binarySearch() computed for us, the level list
				//	remains sorted.
				if (insertionPoint < curLevel.size()){
		 			curLevel.add(insertionPoint, dObj);
		 		}
		 		else {
		 			curLevel.add(dObj);
		 		}

				insertedIntoLevel = true;
			}
			
			//	If insertedIntoLevel is false, we didn't find an existing level that this data object would
			//	fit into.  Create a new one, and add the data object to it.
			//	??	Would it improve performance to specify an initialSize for the new level?
			if (!insertedIntoLevel){
				levelNumber++;
				List newLevel = new ArrayList();
				iLevelList.add(newLevel);
				newLevel.add(dObj);
			}
			
			//	Update the DisplayedObject with its y location.
			dObj.setYLocation(this.levelToYPosition(levelNumber));
		}
		if (Debug.DISPLAY_TIMINGS)
			System.out.println((System.currentTimeMillis() - startTime) + " mS. to place states");
	}


	//	Forget all assignments of objects to levels.
	public void forgetLevelAssignments(){
		Iterator levelIter = iLevelList.iterator();
		while (levelIter.hasNext()){
			List curLevel = (List)levelIter.next();
			curLevel.clear();
		}
	}
	
	
	 //	Return the level containing the provided DataObj.  Throws ImplementationException if DataObj not found.
	public int getLevel(DataObj o){
		
		//	Once around for each level in existence.
		Iterator iter = iLevelList.iterator();
		int levelNumber = 0;
		while (iter.hasNext()){
			List level = (List)iter.next();
			int searchResult = Collections.binarySearch(level, o, TLUtilities.SORT_UP_BY_START_COMPARATOR);
			if (searchResult >= 0)
				return levelNumber;
			levelNumber ++;	
		}
	 	
	 	throw new ImplementationException("Couldn't find DataObj");
	}
	 
	 
	 //	Translate a level index (zero-based) to a vertical location in the pane (expressed in
	 //	drawing coordinates).
	 protected int levelToYPosition(int level){
	 	return TOP_MARGIN + level * LEVEL_SPACING;
	 }
	 
	 
	 //	Translate a y position to a level index.  Returns -1 if the y position is between levels
	 //	or beyond the last occupied level.
	 protected int yPositionToLevelNum(int y){
	 	
	 	//	If we're above the first level of state (in the top margin), report no hit.
	 	int normalizedY = y - TOP_MARGIN;
	 	if (normalizedY < 0)
	 		return -1;
	 	
	 	int level = normalizedY / LEVEL_SPACING;
	 	
	 	//	If we're beyond the lowest occupied level, report no hit.
	 	if (level >= iLevelList.size())
	 		return -1;
	 	
	 	//	If we're between state bodies, report no hit.
	 	if (normalizedY - level * LEVEL_SPACING > DisplayedState.BODY_HEIGHT)
	 		return -1;
	 			 	
	 	return level;
	 }
	 
	 
	 //	Return the height  of the pane used by the current placements.
	 public int getMaximumYUsed(){
		return levelToYPosition(iLevelList.size());
	 }


	 //	Identify which DisplayedState contains the given point.  Return null if no object at that point.
	 public DisplayedState pointToDisplayedObject(Point p){
	 
		//	Determine which level the mouse-down is in.  
		//	If beyond all existing levels, return null.
		int levelNum = yPositionToLevelNum(p.y);
		if (levelNum < 0)
			return null;
		List level = (List)iLevelList.get(levelNum);
			
		//	Create a dummy "probe" DisplayedState corresponding to the x position of the mouse-down.
		//	??	This is a bit inelegant.  Among other warts, this is the only place DummyDisplayedObject
		//	??	is used.
		DisplayedObject probe = new DummyDisplayedObject(p.x);
		
		//	Do a binary search to determine where our probe would appear in the level list.
		int searchResult = Collections.binarySearch(level, probe, TLUtilities.SORT_UP_BY_X_POSITION);
		
		//	If we got a match from the binary search, which means our probe hit the very beginning of an object,
		//	return that object.
		if (searchResult >= 0)
			return (DisplayedState)level.get(searchResult);
		
		//	Variable insertionPoint is where the data object would appear in this level list if
		//	it was inserted.  This calculation of insertionPoint undoes the "mangling" that 
		//	Collections.binarySearch() does to its result if it doesn't find a match.
		int insertionPoint = -(searchResult + 1);
		
		//	If the probe is before the first object in the list, return null.
		if (insertionPoint == 0)
			return null;
		
		//	See whether the data object that starts before the probe point is large enough to contain it.  
		//	If so, we've got a hit.
		DisplayedState o = (DisplayedState)level.get(insertionPoint - 1);
		if (o.getXLocation() <= p.x && p.x <= o.getXLocation() + o.getWidth())
			return o;
		
		return null;
	 }
}	 
	 
