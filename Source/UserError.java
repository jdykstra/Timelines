//	UserError.java - Abstract base class for exceptions which report user errors.

import javax.swing.*;

class UserError extends Exception {
	
	// 	Constants ----------------------------------------------------------------------


	//	Instance variables ----------------------------------------------------------------
	protected String iMsg;					//	Message to be displayed to user
	

	//	Trivial accessors -----------------------------------------------------------------


	// 	Constructor  --------------------------------------------------------------------
	public UserError(String msg){
		iMsg  = msg;
	}
	
	
	//	Return a message to be inserted into the error message displayed to the user.
	//	??	Maybe don't need if displayToUser() works out.
	public String getMessage(){
		return iMsg;
	}
	
	
	//	Display this error to the user.
	public void displayToUser(){
		JOptionPane.showMessageDialog(null,  "The command could not be completed because " +
			iMsg + ".", "Problem in command", JOptionPane.ERROR_MESSAGE);
 	}
	
}

