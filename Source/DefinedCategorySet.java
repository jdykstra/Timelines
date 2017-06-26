//	DefinedCategorySet.java - Represent the set of all categories defined in a document.

////////////////////////	??	Need to decide how to maintain iIndexedCategories.

import java.io.*;
import java.util.*;

public class DefinedCategorySet extends HashSet implements Serializable {
	
	// 	Constants ----------------------------------------------------------------------
	protected static final int PORTABLE_STREAM_VERSION = 1;
	protected static final Set EMPTY_SET = new HashSet();

	//	Instance variables ----------------------------------------------------------------
	//	This list is used only while we are reading or writing DefinedCategorySets and their related
	//	MemberSet objects to or from a file.  The List contains the same Category objects as we do
	//	(with the exception of the Macintosh default category, see below).  Indexes into the list are 
	//	used in the external representation of MemberSet objects.
	protected List iIndexedCategories;
	
	//	iSharedMemberSets is used to manage a collection of MemberSet objects which are shared between all
	//	data objects having the same DefinedCategorySet.  Keys into the map are Sets of Category objects;  the 
	//	corresponding value is the shared MemberSet containing the same Category objects.
	protected Map iSharedMemberSets = new HashMap();
	

	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	//	Create an empty instance of this class.
	public DefinedCategorySet(){
		//	Nothing needed.
	}
	

	//	Create an instance of this class from a Macintosh byte stream.
	public DefinedCategorySet(MacInputStream is)
							throws FileFormatError, IOException {
		
		//	Create the iIndexedCategories List.
		iIndexedCategories = new ArrayList();
		
		//	Read byte stream version number.
		Debug.assertOnError(is.readShort() == 1);  //	Verify version
		int categoryCount = is.readInt();
		
		//	The Mac version of Timelines had the concept of a "default category" that contained
		//	any state that was not a member of any other state.  In this version of the app, we
		//	don't have a default category;  instead, we always display states that do not have any
		//	categories in their MemberSet set.
		//	The defined categories are normally stored as a set.  However, we
		//	also build a list of them, because other parts of the Macintosh file format
		//	refer to categories by index.  We represent the default category in this list as null
		//	??	The List isn't needed (or valid) after the bytestream
		//	??	read is completed.  Should we provide a method to
		//	??	discard it?
		for (int i = 0; i < categoryCount; i++){
			CategoryFromMac macCat = new CategoryFromMac(is);
			Category cat = new Category(macCat);
			if (macCat.isDefault())
				iIndexedCategories.add(null);
			else {
				this.add(cat);
				iIndexedCategories.add(cat);
			}
		}
	}
	
	
	//	Create an instance of this class from a portable byte stream.
	public DefinedCategorySet(DataInputStream is)
							throws FileFormatError, IOException {

		//	Create the iIndexedCategories List.
		iIndexedCategories = new ArrayList();
		
		//	Read byte stream version number.
		int version = is.readShort();
		switch (version){
		
			case PORTABLE_STREAM_VERSION:
				int categoryCount = is.readInt();
				
				//	Besides building the master Set, we build a List
				//	containing the same elements.  The Member instances are
				//	stored in the byte stream as indexes into this List.
				//	??	The List isn't needed (or valid) after the bytestream
				//	??	read is completed.  Should we provide a method to
				//	??	discard it?
				for (int i = 0; i < categoryCount; i++){
					Category cat = new Category(is);
					this.add(cat);
					iIndexedCategories.add(cat);
				}
				break;
				
			default:
				throw new FileFormatError("Unsupported file version (" + version + ")");
		}
	}
	
	
	//	Write an instance to a DataOutputStream.
	public void writeTo(DataOutputStream os) 
							throws IOException {
							
		//	Create the iIndexedCategories List as we write out the Categories.
		iIndexedCategories = new ArrayList();
		
		os.writeShort(PORTABLE_STREAM_VERSION);
		int count = this.size();
		os.writeInt(count);
		Iterator iter = this.iterator();
		while (iter.hasNext()){
			Category cat = (Category) iter.next();
			cat.writeTo(os);
			iIndexedCategories.add(cat);
		}
	}


	//	Return a shared MemberSet object, given a Set containing Category objects.
	public MemberSet getSharedMemberSet(Set value){
	
		//	First try to find a cached shared value.
		MemberSet sharedMS = (MemberSet) iSharedMemberSets.get(value);
		
		//	If a shared MemberSet with this value doesn't already exist, create it and save it in the map.
		if (sharedMS == null){
			sharedMS = new MemberSet();
			sharedMS.iValue = value;
			iSharedMemberSets.put(value, sharedMS);
		}
		return sharedMS;
	}
	
	
	//	Return the shared MemberSet object, corresponding to the empty set.
	public MemberSet getSharedMemberSet(){
		return getSharedMemberSet(EMPTY_SET);
	}
	
	
	//	Return a shared MemberSet object, from the next bytes in a Macintosh byte stream.
	public MemberSet getSharedMemberSet(MacInputStream is)
							throws FileFormatError, IOException {
		Set tempValue = new HashSet();

		//	The shown categories are stored as a bit map.  Translate that into
		//	our representation.  Ignore the default category used on Macintosh.
		long bitmap = is.readSwappedUnsignedMacInt();
		int listSize = iIndexedCategories.size();
		for (int i = 0; i < listSize; i++){
			if (((1 << i) & bitmap) != 0){
				Category cat = (Category)iIndexedCategories.get(i);
				if (cat != null)
					Debug.assertOnError( tempValue.add(cat));
			}
		}
		return getSharedMemberSet(tempValue);
	}
	
	
	//	Return a shared MemberSet object, from the next bytes in a portable byte stream.
	public MemberSet getSharedMemberSet(DataInputStream is)
							throws FileFormatError, IOException {
		Set tempValue = new HashSet();

		//	Read byte stream version number.
		int version = is.readShort();
		switch (version){
		
			case PORTABLE_STREAM_VERSION:
				int memberCount = is.readInt();
				for (int i = 0; i < memberCount; i++){
					int memberIndex = is.readInt();
					tempValue.add(iIndexedCategories.get(memberIndex));
				}
				break;
			
			default:
				throw new FileFormatError("Unsupported file version (" + version + ")");
		}
		return getSharedMemberSet(tempValue);
	}
		
		
	//	Member classes -----------------------------------------------------------------
	//	This member class represents a (perhaps proper) subset  of the
	//	Categories defined in a document.
	//	??	For now, we'll use a Set for each instance.  In the future, we can switch
	//	??	to shared instances to save time and memory.
	public class MemberSet extends Object  implements Serializable {
	
		protected Set iValue = new HashSet();
		

		//	Return the DefinedCategorySet this is associated with.
		public DefinedCategorySet getDefinedCategories(){
			return DefinedCategorySet.this;
		}
		
		
		//	Write an instance to a DataOutputStream.
		public void writeTo(DataOutputStream os) 
							throws IOException {
			os.writeShort(PORTABLE_STREAM_VERSION);
			int count = iValue.size();
			os.writeInt(count);
			Iterator iter = iValue.iterator();
			while (iter.hasNext()){
				Category cat = (Category) iter.next();
				int memberIndex = iIndexedCategories.indexOf(cat);
				Debug.assertOnError(memberIndex >= 0);
				os.writeInt(memberIndex);
			}
		}
	
	
		//	Return the Categories that are in this MemberSet set, as a Set.
		public Set getAsSet(){
			return Collections.unmodifiableSet(iValue);
		}
		
		
		//	Remove a Category from this MemberSet.
		public MemberSet remove(Category cat){
			Set tempValue = new HashSet(iValue);
			tempValue.remove(cat);
			return getSharedMemberSet(tempValue);
		}
		
		
		//	Add a Category to this MemberSet.
		public MemberSet add(Category cat){
			Set tempValue = new HashSet(iValue);
			tempValue.add(cat);
			return getSharedMemberSet(tempValue);
		}
	}
}
