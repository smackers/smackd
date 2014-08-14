package org.smackers.smack.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.smackers.smack.Activator;

public class SmackJobReach extends SmackJob {
	
	public SmackJobReach(IFile file) {
		super("Getting SMACKd Reach", "SMACKd Reach Failed!", SmackJob.markerReach);
		setFile(file);
		
		//Prepare process
		setCmd("smackreach.py");
		setArgs( Arrays.asList(	"--smackd",
								"--verifier", "corral"));
		
		setMarkerIdsToClear(Arrays.asList(SmackJob.markerReach));
		
		setSuccessTitle("SMACKd Reach Success!");
		setSuccessMsg("No unreachable code was detected");
	}


	
	//Stores unreachable lines in ExecutionResult.Traces - nothing else of ER is used.
	//TODO support multiple source files.  Do IFile resolution here, or in ExecutionTrace
	protected ExecutionResult parseSmackOutput(IProject project, String output) throws ExecutionException {

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
			// Looks like 
			// {"filename": [[line1, col1], [line2, col2]], "filename2": [[line1, col1]]}
			
			Activator.getDefault().getLogger().write(Logger.SMACKD_ERR, jo.toString());
			
			
			//If json is empty object, all code is reachable.
			er.setVerificationPassed(jo.keySet().isEmpty());
			
			for(String filename : jo.keySet()) {
				JsonArray fileLines = jo.getJsonArray(filename);
				IFile traceFile = null;
				try {
					traceFile = getIFile(project, filename);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_ERR, e);
				}
				for(JsonValue lineColVal : fileLines) {
					//Convert jsonValue to jsonArray
					JsonArray lineCol = (JsonArray)lineColVal;
					int threadId = 1;
					String desc = "";
					ExecutionTrace trace = new ExecutionTrace(	threadId,
							filename,
							traceFile,
							lineCol.getInt(0),
							lineCol.getInt(1),
							desc);
					er.addTrace(trace);
				}
			}

		} catch (JsonException e) {
			Logger log = Activator.getDefault().getLogger();
			String msg = "SMACK did not return proper JSON output. Exception Message:\n\n" + e.getMessage();
			msg += "\n\nSMACK Ouput:\n" + output;
			log.write(Logger.SMACKD_ERR, msg);
					
			throw new ExecutionException(msg);
		}			
		return er;
	}
}
