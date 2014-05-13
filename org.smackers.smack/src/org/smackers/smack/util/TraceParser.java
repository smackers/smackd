package org.smackers.smack.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.json.Json; 
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class TraceParser {
	
	//Currently only supports a single source file, which must be passed in.
	//TODO support multiple source files.  Do IFile resolution here, or in ExecutionTrace
	public static ExecutionResult parseSmackOutput(IProject project, String output) {
		
		ExecutionResult er = new ExecutionResult();
				

		
//		if(output.substring(0,"smackd json\n".length()).equals("smackd json")) {
			// Here, throw away line[0], treat the rest as JSON
			//String jsonString = output.substring("smackd json\n".length());
			// Turn it back to input stream
			//TODO Fix this - when old boogie parsing can go away,
			//     convert this to using the original inputstream
			InputStream jsonInput = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
			
			JsonReader jr = Json.createReader(jsonInput);
			
			JsonObject jo = jr.readObject();
			jr.close();
			
			er.setVerifier(jo.getString("verifier"));
			er.setVerificationPassed(jo.getBoolean("passed?"));
			er.setThreadCount(jo.getInt("threadCount"));
			
			JsonObject jsonFailsAt = jo.getJsonObject("failsAt");
			String failsAtFileName = jsonFailsAt.getString("file");
			IFile failsAtFile = null;
			try {
				failsAtFile = findFileRecursively(project, failsAtFileName);
				//if(failsAtFile == null) {
					//perhaps it is an include (non relative URL, not directly part of IProject)
				//	IWorkspace workspace = ResourcesPlugin.getWorkspace();
				//	IWorkspaceRoot workspaceRoot = workspace.getRoot();
				//	try {
				//		IPath newPath = new Path(failsAtFileName);
				//		IFile[] failsAtFiles = workspaceRoot.
				//		failsAtFile = failsAtFiles[0];
				//	} catch (URISyntaxException e) {
				//		// TODO Auto-generated catch block
				//		e.printStackTrace();
				//	} 
				//}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ExecutionTrace failsAt = new ExecutionTrace(-1, failsAtFileName,
														failsAtFile,
														jsonFailsAt.getInt("line"),
														jsonFailsAt.getInt("column"),
														jsonFailsAt.getString("description"));
			er.setFailsAt(failsAt);
			
			JsonArray jsonTraces = jo.getJsonArray("traces");
			for(int x = 0; x < jsonTraces.size(); x++) {
				JsonObject jsonTrace = jsonTraces.getJsonObject(x);
				String traceFileName = jsonTrace.getString("file");
				IFile traceFile = null;
				try {
					traceFile = findFileRecursively(project, traceFileName);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ExecutionTrace trace = new ExecutionTrace(	jsonTrace.getInt("threadid"),
															traceFileName,
															traceFile,
															jsonTrace.getInt("line"),
															jsonTrace.getInt("column"),
															jsonTrace.getString("description"));
				er.addTrace(trace);
			}
//		} else {
			//Here, do old boogie parsing

//			er.setVerifier("boogie");
//			er.setThreadCount(1);
			
			
//			String[] lines = output.split(System.getProperty("line.separator"));
			//NOTE: Does not currently support parenthesis in filename
//			String desc;
//			int lineNo;
//			int callOrder = 0;
//			boolean smackRan = false;
//			for (int i=0; i < lines.length; i++) {
//				String line = lines[i];
//				switch(line.substring(0,Math. min(line.length(),13))) {
//				case "":
//					break;
//				case "SMACK verifie":
//					smackRan = true;
//					break;
//				case "Finished with":
					// If this line contains ", 0 errors", verification passed
//					er.setVerificationPassed(line.contains(", 0 errors"));
//					break;
//				default:
//					if(smackRan) {
						//TODO Get all Smack markers, clear them
//						int idx1stPar = line.indexOf('(');
//						String fileName = line.substring(0, idx1stPar);
//						int idxLineNoComma = line.indexOf(',', idx1stPar);
//						int idxClosePar = line.indexOf(')', idxLineNoComma);
//						lineNo = Integer.parseInt(line.substring(idx1stPar + 1, idxLineNoComma));
//						if(line.length() > idxClosePar + 3){
//							desc = line.substring(idxClosePar + 3);
//						} else {
//							desc = "";
//						}
//						er.addTrace(new ExecutionTrace(-1, fileName, file, lineNo, 1, desc));
						//try{
						//	highlight(file, lineNo, desc, callOrder++);
						//}
						//catch (Exception e) {
						//	e.printStackTrace();
						//}
//					}
//				}
//			}
//		}
		return er;
	}
	
	private static IFile findFileRecursively(IContainer project, String name) throws CoreException {
		IResource[] rs = project.members();
	    for (IResource r : rs) {
	        if (r instanceof IContainer) {
	            IFile file = findFileRecursively((IContainer)r, name);
	            if(file != null) {
	                return file;
	            }
	        } else if (r instanceof IFile) {
	        	URI uri = r.getLocationURI();
	        	IPath relPath = r.getFullPath();
	        	String osRelPath = relPath.toOSString();
	        	IPath fullPath = r.getRawLocation();
	        	String osFullPath = fullPath.toOSString();
	        	if(osFullPath.equals(name) || osRelPath.equals(name)) {
	        		return (IFile) r;	
	        	}

	        }
	    }

	    return null;
	}
}