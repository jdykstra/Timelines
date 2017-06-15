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

//	??	I suspect that putting these public class definitions into the same file is not legal, but CodeWarrior
//	??	doesn't have any trouble with it, so I'll leave it like this for now.

public class CategoryAddChange extends CategoryChange {
	public CategoryAddChange(Object source, Category affectedCategory){
		super(source, affectedCategory);
	}
}


public class CategoryDeleteChange extends CategoryChange {
	public CategoryDeleteChange(Object source, Category affectedCategory){
		super(source, affectedCategory);
	}
}


public class CategoryEditChange extends CategoryChange {
	public CategoryEditChange(Object source, Category affectedCategory){
		super(source, affectedCategory);
	}
}
