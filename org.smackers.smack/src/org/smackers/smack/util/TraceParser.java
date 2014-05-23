package org.smackers.smack.util;

import org.smackers.smack.Activator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.json.Json; 
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class TraceParser {
	
	
	public static ExecutionResult parseRemoteSmackOutput(IFile file, String output) {
		
		ExecutionResult er = new ExecutionResult();
		
		//Get smack output
		InputStream jsonInput = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
		JsonReader jr = Json.createReader(jsonInput);
		JsonObject jo = jr.readObject();
		jr.close();
		String smackOutputJson = jo.getJsonArray("Outputs").getJsonObject(0).getString("Value");
		
		String[] lines = smackOutputJson.split("\\r\\n");
		for(String line : lines) {
			if(line.startsWith("    input.c(")) {
				//This is a line for markup...
				int idx1stPar = line.indexOf('(');
				//String fileName = line.substring(0, idx1stPar);
				int idxLineNoComma = line.indexOf(',', idx1stPar);
				int idxClosePar = line.indexOf(')', idxLineNoComma);
				int lineNo = Integer.parseInt(line.substring(idx1stPar + 1, idxLineNoComma));
				String desc;
				if(line.length() > idxClosePar + 3){
					desc = line.substring(idxClosePar + 3);
				} else {
					desc = "";
				}
				er.addTrace(new ExecutionTrace(-1, file.getName(), file, lineNo, 1, desc));
			}
		}
		return er;
	}
	
	//Currently only supports a single source file, which must be passed in.
	//TODO support multiple source files.  Do IFile resolution here, or in ExecutionTrace
	public static ExecutionResult parseSmackOutput(IProject project, String output) {

		ExecutionResult er = new ExecutionResult();

		// Turn it back to input stream
		//TODO Fix this - when old boogie parsing can go away,
		//     convert this to using the original inputstream
		InputStream jsonInput = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
		JsonReader jr;

		try {
			jr = Json.createReader(jsonInput);


			JsonObject jo = jr.readObject();
			jr.close();

			er.setVerifier(jo.getString("verifier"));
			er.setVerificationPassed(jo.getBoolean("passed?"));
			er.setThreadCount(jo.getInt("threadCount"));

			JsonObject jsonFailsAt = jo.getJsonObject("failsAt");
			String failsAtFileName = jsonFailsAt.getString("file");
			IFile failsAtFile = null;
			try {
				failsAtFile = getIFile(project, failsAtFileName);
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
					traceFile = getIFile(project, traceFileName);
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

		} catch (JsonException e) {
			Logger log = Activator.getDefault().getLogger();
			log.write(Logger.SMACKD_ERR, 
					"SMACK did not return proper JSON output. Exception Message: " + e.getMessage());
			return new ExecutionResult();
		}			
		return er;
	}
	
	private static IFile findIFileInProjectRecursively(IContainer project, String name) throws CoreException {
		IResource[] rs = project.members();
	    for (IResource r : rs) {
	        if (r instanceof IContainer) {
	            IFile file = findIFileInProjectRecursively((IContainer)r, name);
	            if(file != null) {
	                return file;
	            }
	        } else if (r instanceof IFile) {
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
	
	private static IFile getIFile(IProject project, String fileName) throws CoreException {
		IFile ifile = null;
		ifile = findIFileInProjectRecursively(project, fileName);
		
		if(ifile == null) {
			//perhaps it is an include (non relative URL, not directly part of IProject)
			IPath newPath = new Path(fileName);
			IFolder folder = project.getFolder("Includes_" + newPath.removeLastSegments(1).toString().replace('/', '-') + "_(DeleteAfterVisualizing)");
			//ifile = project.getFile(newPath.lastSegment() + "_(DeleteThisLinkAtterVisualizing)");
			//ifile.createLink(newPath, IResource.NONE, null);
			folder.createLink(newPath.removeLastSegments(1), IResource.NONE, null);
			ifile = findIFileInProjectRecursively(project, fileName);
		}
		return ifile;
	}
}