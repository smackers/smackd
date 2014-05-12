package org.smackers.smack.util;

import org.eclipse.core.resources.IFile;


//Right now, both fileName and ifile must be set.
//TODO in the future, move ifile resolution here?
public class ExecutionTrace {
	
	// The thread id
	private int threadId;
	
	// The file containing the instruction
	private String fileName;
	
	// The eclipse IFile
	private IFile ifile;
	
	// The line in file containing the instruction
	private int line;
	
	// The column in the line ... containing the instruction
	private int column;
	
	// The description associated with the instruction
	private String description;
	
	ExecutionTrace(	int threadId, String fileName, IFile ifile, 
					int line, int column, String description) {
		this.threadId = threadId;
		this.fileName = fileName;
		this.ifile = ifile;
		this.line = line;
		this.column = column;
		this.description = description;
	}

	public int getThreadId() {
		return threadId;
	}

	public String getFileName() {
		return fileName;
	}
	
	public IFile getIfile() {
		return ifile;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public String getDescription() {
		return description;
	}
	
}
