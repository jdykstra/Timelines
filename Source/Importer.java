//	Importer.java - Import states from a foreign document.

import java.io.*;
import java.util.*;

interface Importer  {
	public Set importFromFile(TLDocument doc, File file) throws IOException;
}

