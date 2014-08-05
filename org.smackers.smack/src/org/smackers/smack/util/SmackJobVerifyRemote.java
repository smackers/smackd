package org.smackers.smack.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.smackers.smack.Activator;
import org.smackers.smack.preferences.PreferenceConstants;

public class SmackJobVerifyRemote extends SmackJob {
	
	public SmackJobVerifyRemote(IFile file) {
		super("Getting SMACKd Verify Remote", "SMACKd Verify Remote Failed!", SmackJob.markerTrace);
		setFile(file);
		
		setMarkerIdsToClear(Arrays.asList(SmackJob.markerFail, SmackJob.markerTrace));
		
		setSuccessTitle("SMACKd Verify Remote Success!");
		setSuccessMsg("Project verified successfully");
	}

	//Sends file contents smack server, returns output as string
	@Override
	protected ExecutionResult execSmack(IProject project, IFile file) throws IOException, ConnectException, MalformedURLException {
		Logger log = Activator.getDefault().getLogger();
		
		//Get contents of input file as string
		Scanner s = null;
	    String contents = "";
		try {
			s = new Scanner(file.getContents()).useDelimiter("\\A");
			contents = s.hasNext() ? s.next() : "";
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e1);
		} finally {
			s.close();
		}
	    

		// Create json to submit			
		JsonObject request = Json.createObjectBuilder()
				.add("version", "1.0")
				.add("Source", contents)
				.build();
		
		String result = "";
		
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		final String serverURL = (preferenceStore.getString(PreferenceConstants.SMACK_SERVER)).trim();
		
		URL url = new URL(serverURL + "/run");
		log.write(Logger.SMACK_EXEC, "Executing SMACK Remotely:");
		log.write(Logger.SMACK_EXEC, "Remote Server URL: " + url.toExternalForm());
		HttpURLConnection conn = null;
		
		conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestMethod("POST");
		
		OutputStream os = conn.getOutputStream();
		String outReq = request.toString();
		log.write(Logger.SMACK_EXEC, "Posting data: \n" + outReq);
		os.write(outReq.getBytes("UTF-8"));
		os.flush();
		
		StringBuilder sb = new StringBuilder();  
		
		int HttpResult =conn.getResponseCode(); 
		
		if(HttpResult == HttpURLConnection.HTTP_OK){
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(),"utf-8"));  
			String line = null;  
			while ((line = br.readLine()) != null) {  
				sb.append(line + "\n");  
			}  
			br.close();  
			result = sb.toString();
			log.write(Logger.SMACK_OUTPUT, result);
		}else{
			log.write(Logger.SMACK_OUTPUT, "Got http error response: " + conn.getResponseMessage());  
		}  
		return parseSmackOutput(project, result);
	}
	
	protected ExecutionResult parseSmackOutput(IProject project, String output) {
		
		ExecutionResult er = new ExecutionResult();
		
		//Get smack output
		InputStream jsonInput = new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
		JsonReader jr = Json.createReader(jsonInput);
		JsonObject jo = jr.readObject();
		jr.close();
		String smackOutputJson = jo.getJsonArray("Outputs").getJsonObject(0).getString("Value");
		
		String filenameFilt = "[\\w#$~%.\\/-]+";
		Pattern outcomeFilt  = Pattern.compile("^Finished with (\\d+) verified, (\\d+) errors$");
		Pattern errorFilt    = Pattern.compile("^\\s+(" + filenameFilt + ")\\((\\d+),(\\d+)\\) : (error main: This assertion might not hold)$");
		Pattern traceFilt    = Pattern.compile("^\\s+(" + filenameFilt + ")\\((\\d+),(\\d+)\\) : (Trace Element: Error trace\\[\\d+\\])$");
		
		String[] lines = smackOutputJson.split("\\n");
		for(String line : lines) {
			line = line.replace("\r", "");
			Matcher traceMatcher = traceFilt.matcher(line);
			Matcher errorMatcher = errorFilt.matcher(line);
			Matcher outcomeMatcher = outcomeFilt.matcher(line);
			if(traceMatcher.matches()) {
				int lineNo = Integer.parseInt(traceMatcher.group(2));
				int colNo = Integer.parseInt(traceMatcher.group(3));
				String desc = traceMatcher.group(4);
				er.addTrace(new ExecutionTrace(-1, getFile().getName(), getFile(), lineNo, colNo, desc));
			} else if(errorMatcher.matches()) {
				int lineNo = Integer.parseInt(errorMatcher.group(2));
				int colNo = Integer.parseInt(errorMatcher.group(3));
				String desc = errorMatcher.group(4);
				er.setFailsAt(new ExecutionTrace(-1, getFile().getName(), getFile(), lineNo, colNo, desc));
			} else if(outcomeMatcher.matches()) {
				er.setThreadCount(1);
				er.setVerificationPassed(!outcomeMatcher.group(1).equals("0") && outcomeMatcher.group(2).equals("0"));
				er.setVerifier("boogie");
			}
		}
	
		return er;
	}
	
	public void updateView(IProject project, ExecutionResult result) {
		if(!result.isVerificationPassed()) {
			//Clear Verify markers
			int depth = IResource.DEPTH_INFINITE;
			try {
				project.deleteMarkers(markerFail, true, depth);
				project.deleteMarkers(markerTrace, true, depth);
			} catch (CoreException e) {
				// something went wrong
			}
			
			//Add FailsAt trace
			int callOrder = 0;
			if(result.getFailsAt() != null) {
				ExecutionTrace failTrace = result.getFailsAt();
				try {
					highlight(	failTrace.getIfile(), failTrace.getLine(), failTrace.getColumn(),
							failTrace.getDescription(), callOrder++, markerFail);
				} catch (CoreException e) {
					Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
				}
			}
			
			// And update with new markers
			ArrayList<ExecutionTrace> traces = result.getTraces();
			for(int x = 0; x < traces.size(); x++) {
				ExecutionTrace trace = traces.get(x);
				try {
					highlight(	trace.getIfile(), trace.getLine(), trace.getColumn(),
								trace.getDescription(), callOrder++, markerTrace);
				} catch (Exception e) {
					Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
				}
			}
		} else {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			MessageDialog.openInformation(
					window.getShell(),
					"SMACKd Verify Success!", 
					"Project verified successfully");
		}
	}

}
