//	CategoryChange.java - Change events for when categories are edited in any way.

import java.util.*;

public class CategoryChange extends javax.swing.event.ChangeEvent  {

	// 	Constants ------------------------------------------------------------------------
	public static final boolean ADDING = false;			//	Constants for constructor parameter
	public static final boolean DELETING = true;
	
	//	Instance variables----------------------------------------------------------------
	protected Category iAffectedCategory;			//	Category that was edited
	
	//	Constructor---------------------------------------------------------------------
	public CategoryChange(Object source, Category affectedCategory){
		super(source);
		iAffectedCategory = affectedCategory;
	}
}
