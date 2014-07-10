/**
 * 
 */
package org.smackers.smack.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.smackers.smack.Activator;
import org.smackers.smack.preferences.PreferenceConstants;
import org.smackers.smack.views.SmackMarkView;

/**
 * @author mcarter
 * 
 * Roughly, the 'controller', in an MVC pattern
 *
 */
public class Controller {
	
	//Get reference to logger
	private static Logger log = Activator.getDefault().getLogger();		
	
	//Verifies the file using the locally installed instance of smack
	public static void smackVerifyLocal(final IFile file) {
		Job addJob = new Job("Run Smack Locally") {
			protected Process smackProc;
			
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {		
				final IProject project = file.getProject();
				smackProc = execSmack(file.getRawLocation().toString());
				String smackOutput = collectSmackResults(smackProc);
				final ExecutionResult smackExecutionResult = 
						TraceParser.parseSmackOutput(project,smackOutput);
				
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						UpdateViews(project, smackExecutionResult);		
					}
				});				
				
				return Status.OK_STATUS;
			}
			
			@Override
			protected void canceling() {
				if(smackProc != null)
					smackProc.destroy();
			}
		};
		addJob.schedule();
	}
	
	//Verifies the file using the remote rise4fun server
	public static void smackVerifyRemote(final IFile file) {
		Job addJob = new Job("Run Smack Remotely") {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				final IProject project = file.getProject();
				String smackOutput = remoteExecSmack(file);
				final ExecutionResult smackExecutionResult = 
						TraceParser.parseRemoteSmackOutput(file,smackOutput);
				
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						UpdateViews(project, smackExecutionResult);		
					}
				});
				
				return Status.OK_STATUS;
			}
		};
		addJob.schedule();
	}
	
	//Sends file contents smack server, returns output as string
	public static String remoteExecSmack(IFile file) {
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
		String serverURL = preferenceStore.getString(PreferenceConstants.SMACK_SERVER);
		
		try {
			URL url = new URL(serverURL + "/run");
			log.write(Logger.SMACK_EXEC, "Executing SMACK Remotely:");
			log.write(Logger.SMACK_EXEC, "Remote Server URL: " + url.toExternalForm());
			HttpURLConnection conn = null;
			try {
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
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			}

		} catch (MalformedURLException e) {
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
		}
		
		return result;
	}

	
	private static ProcessBuilder buildSmackProcess(String filenameToVerify) {
		ProcessBuilder pb = new ProcessBuilder();
		
		//Get preference store
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

		//Get paths from preference store
		String smackbin  = preferenceStore.getString(PreferenceConstants.SMACK_BIN);
		String llvmbin   = preferenceStore.getString(PreferenceConstants.LLVM_BIN);
		String boogiebin = preferenceStore.getString(PreferenceConstants.BOOGIE_BIN);
		String corralbin = preferenceStore.getString(PreferenceConstants.CORRAL_BIN);
		String monobin = preferenceStore.getString(PreferenceConstants.MONO_BIN);

		//Set process command
		String cmd = "smack-verify.py";
		List<String> args = Arrays.asList(	smackbin + "/" + cmd,
											"--smackd",
											"--verifier", "corral",
											filenameToVerify);
		pb.command(args);
		
		//So we see error output on console
		pb.redirectErrorStream(true);
		
		//Set up environment
		Map<String,String> env = pb.environment();
		
		log.write(Logger.SMACK_ENV, "CONFIGURING PATH:");
		//Grab current system path, add what we need
		String path = System.getenv("PATH");
		path += ":" + llvmbin;
		path += ":" + smackbin;
		env.put("PATH", path);
		log.write(Logger.SMACK_ENV, "PATH: " + path);
		
		//Add the command aliases expected by smack-verify.py
		String boogieVar = monobin + "/mono " + boogiebin + "/Boogie.exe";
		String corralVar = monobin + "/mono " + corralbin + "/Debug/corral.exe";
		env.put("BOOGIE", boogieVar);
		env.put("CORRAL", corralVar);
		log.write(Logger.SMACK_ENV, "BOOGIE: " + boogieVar);
		log.write(Logger.SMACK_ENV, "CORRAL: " + corralVar);
		
		return pb;
	}
	
	private static String collectSmackResults(Process p) {
		StringBuilder result = new StringBuilder();

		try {
			InputStream is = p.getInputStream();
			InputStreamReader ir = new InputStreamReader(is);
			BufferedReader bri = new BufferedReader(ir);
			
			
			//If we don't wait till bri has output, the call to bri.read()
			//  blocks, and deadlocks the threads.
			while(!bri.ready()) {
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					
				}
			}
			
			
			int c;
			while ((c = bri.read()) != -1) {
				result.append((char)c);
			}
			
			p.waitFor();
			//TODO throw instead of handling here?  Let caller handle failed smack exec?
			//if(p.exitValue()!=0)
			//{
			//	MessageBox a = new MessageBox(Display.getDefault().getActiveShell());
			//	a.setMessage("smack-verify.py failed to terminate normally");
			//	a.open();
			//}
			
			//ParseSmackOutput(result);
			log.write(Logger.SMACK_OUTPUT, result.toString());
			return result.toString();
		}
		catch (Exception e) {
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			return "";
		}
	}

	//Executes SMACK, returns handle to smack process
	private static Process execSmack(String filename) {
		//Build new process
		ProcessBuilder pb = buildSmackProcess(filename);
		
		// Start process
		log.write(Logger.SMACK_EXEC, "Executing SMACK");
		String commandString = "";
		for(String s : pb.command())
			commandString += s + " ";
		log.write(Logger.SMACK_EXEC, "Calling: " + commandString);
		Process p;
		try {
			p = pb.start();
		} catch (IOException e) {
			log.write(Logger.SMACKD_ERR, "IOException raised when executing smack-verify.py");
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			return null;
		}
		return p;
	}
	
	public static void ClearSmackView() {
		//Clears currently displayed markers
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		try {
			IViewPart view = window.getActivePage().showView("org.smackers.smack.views.SmackMarkView");
			((SmackMarkView)view).deleteCurrentMarkers();
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void ClearProjectMarkers(IProject project) {
		//Clears any markers for project
		int depth = IResource.DEPTH_INFINITE;
		try {
			project.deleteMarkers("org.smackers.smack.markers.smackAssertionFailedMarker", true, depth);
			project.deleteMarkers("org.smackers.smack.markers.smackAssertionTraceMarker", true, depth);
		} catch (CoreException e) {
			// something went wrong
		}	
	}
	
	public static void UpdateViews(IProject project, ExecutionResult result) {
		if(!result.isVerificationPassed()) {
			ClearProjectMarkers(project);
			
			// And update with new markers
			ArrayList<ExecutionTrace> traces = result.getTraces();
			for(int x = 0; x < traces.size(); x++) {
				ExecutionTrace trace = traces.get(x);
				try {
					highlight(	trace.getIfile(), trace.getLine(),
								trace.getDescription(), x);
				} catch (Exception e) {
					Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
				}
			}
		}
	}
	
	private static void highlight(	IFile file, int lineNo,
			String desc, int callOrder) throws CoreException {
		IWorkbenchPage ap = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();		
		ITextEditor editor = (ITextEditor)IDE.openEditor(ap, file, true);
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		IRegion lineInfo = null;
		try {
			//IDocument lines are 0 based idx
			lineInfo = document.getLineInformation(lineNo - 1);
		} catch (BadLocationException e){
			
		}
		if(lineInfo != null) {
			final IMarker marker;
			if(callOrder == 0) {
				marker = file.createMarker("org.smackers.smack.markers.smackAssertionFailedMarker");
			} else {
				marker = file.createMarker("org.smackers.smack.markers.smackAssertionTraceMarker");
			}
			//final IMarker marker = file.createMarker(IMarker.TEXT);

			//IMarker lines are 1 based idx
			marker.setAttribute(IMarker.LINE_NUMBER, lineNo);
			marker.setAttribute(IMarker.CHAR_START, lineInfo.getOffset());
			marker.setAttribute(IMarker.CHAR_END, lineInfo.getLength() + lineInfo.getOffset());
			marker.setAttribute(IMarker.MESSAGE, desc);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute("callOrder", callOrder);
			//IDE.gotoMarker(editor, marker);
		}
	}	
}
