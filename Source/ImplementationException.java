//	ImplementationException.java - Exception to report something that shouldn't happen.

import java.io.*;

public class ImplementationException extends RuntimeException {

	protected String message;
	
	//	Constructors.
	public ImplementationException(){
		super();
	}
	
	
	public ImplementationException(Throwable e){
		super();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("An unexpected exception was encountered:  " + e.toString());
		e.printStackTrace(new PrintWriter(sw));
		message = sw.toString();
		System.err.println(message);
	}
	
	
	public ImplementationException(String msg){
		super();
		message = msg;
	}
	
	
	public String toString(){
		return message;
	}
	
}