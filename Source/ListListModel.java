//	ListListModel.java - Implementation of javax.swing.ListModel based on a List.

import java.util.*;
import javax.swing.*;

public class ListListModel extends DefaultListModel {


	//	Constructor.
	public ListListModel(List list){
		Iterator iter = list.iterator();
		while (iter.hasNext())
			addElement(iter.next());
	}
	
	
	//	Provide an API for clients to notify us that a list element has changed.
	public void notifyElementChanged(Object obj){
		int index = lastIndexOf(obj);
		fireContentsChanged(this, index, index);
	}
}