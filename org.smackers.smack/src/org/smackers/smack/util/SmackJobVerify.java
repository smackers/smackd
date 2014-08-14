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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.smackers.smack.Activator;

public class SmackJobVerify extends SmackJob {
	
	public SmackJobVerify(IFile file) {
		super("Getting SMACKd Verify", "SMACKd Verify Failed!", SmackJob.markerTrace);
		setFile(file);
		
		//Prepare process
		setCmd("smackverify.py");
		setArgs(Arrays.asList(	"--smackd",	"--verifier", "corral"));
		
		setMarkerIdsToClear(Arrays.asList(SmackJob.markerFail, SmackJob.markerTrace));
		setSuccessTitle("SMACKd Verify Success!");
		setSuccessMsg("Project verified successfully");
		
	}

	//Currently only supports a single source file, which must be passed in.
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
			
			
			Activator.getDefault().getLogger().write(Logger.SMACKD_ERR, jo.toString());
			
			er.setVerifier(jo.getString("verifier"));
			er.setVerificationPassed(jo.getBoolean("passed?"));
			if(!er.isVerificationPassed()){
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
						Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_ERR, e);
					}
					ExecutionTrace trace = new ExecutionTrace(	jsonTrace.getInt("threadid"),
							traceFileName,
							traceFile,
							jsonTrace.getInt("line"),
							jsonTrace.getInt("column"),
							jsonTrace.getString("description"));
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
