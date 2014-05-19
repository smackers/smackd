/**
 * 
 */
package org.smackers.smack.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.smackers.smack.Activator;
import org.smackers.smack.preferences.PreferenceConstants;

/**
 * @author mcarter
 * 
 * Roughly, the 'controller', in an MVC pattern
 *
 */
public class Controller {
	
	//Sends file contents smack server, returns output as string
	public static String remoteExecSmack(String fileContents) {
		String result = "";
		return result;
	}

	//Executes SMACK, returns stdout as a single string
	public static String execSmack(String filename) {
		String result = "";
		//Grab the smack preference store, grab paths
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		Logger log = Activator.getDefault().getLogger();
		String smackbin  = preferenceStore.getString(PreferenceConstants.SMACK_BIN);
		String llvmbin   = preferenceStore.getString(PreferenceConstants.LLVM_BIN);
		String boogiebin = preferenceStore.getString(PreferenceConstants.BOOGIE_BIN);
		String corralbin = preferenceStore.getString(PreferenceConstants.CORRAL_BIN);
		String monobin = preferenceStore.getString(PreferenceConstants.MONO_BIN);
					
		String cmd = "smack-verify.py";

		try {
			//Build new process
			ProcessBuilder pb = new ProcessBuilder(	smackbin + "/" + cmd,
													"--smackd", "--verifier",
													"corral", filename);

			//So we see error output on console
			pb.redirectErrorStream(true);
			Map<String,String> env = pb.environment();
			log.write(Logger.SMACK_ENV, "CONFIGURING PATH:");
			//Grab current system path, add what we need
			String path = System.getenv("PATH");
			path += ":" + llvmbin;
			path += ":" + smackbin;
			env.put("PATH", path);
			
			log.write(Logger.SMACK_ENV, "PATH: " + path);
			//Add the command aliases expected by smack-verify.py
			//TODO User path builder instead, to handle trailing '/' on paths (see java.io.File, new File(baseDirFile,subdirStr))
			String boogieVar = monobin + "/mono " + boogiebin + "/Boogie.exe";
			env.put("BOOGIE", boogieVar);
			log.write(Logger.SMACK_ENV, "BOOGIE: " + boogieVar);
			String corralVar = monobin + "/mono " + corralbin + "/Debug/corral.exe";
			env.put("CORRAL", corralVar);
			log.write(Logger.SMACK_ENV, "CORRAL: " + corralVar);
			// Start process
			log.write(Logger.SMACK_EXEC, "Executing SMACK");
			Process p = pb.start();
			// Waaaait for it!
			p.waitFor();
			//TODO throw instead of handling here?  Let caller handle failed smack exec?
			if(p.exitValue()!=0)
			{
				MessageBox a = new MessageBox(Display.getCurrent().getActiveShell());
				a.setMessage("smack-verify.py failed to terminate normally");
				a.open();
			}
			
			BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String line;
			while ((line = bri.readLine()) != null) {
			    result+=line+"\n";
			    System.out.println(line);
			}
			//ParseSmackOutput(result);
			log.write(Logger.SMACK_OUTPUT, result);
			return result;
		}
		catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void UpdateViews(IProject project, ExecutionResult result) {
		if(!result.isVerificationPassed()) {
			//Clear old markers
			int depth = IResource.DEPTH_INFINITE;
			try {
				project.deleteMarkers("org.smackers.smack.markers.smackAssertionFailedMarker", true, depth);
				project.deleteMarkers("org.smackers.smack.markers.smackAssertionTraceMarker", true, depth);
			} catch (CoreException e) {
				// something went wrong
			}
			
			// And update with new markers
			ArrayList<ExecutionTrace> traces = result.getTraces();
			for(int x = 0; x < traces.size(); x++) {
				ExecutionTrace trace = traces.get(x);
				try {
					highlight(	trace.getIfile(), trace.getLine(),
								trace.getDescription(), x);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void highlight(	IFile file, int lineNo,
			String desc, int callOrder) throws CoreException {
		ITextEditor editor = (ITextEditor)IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
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
			System.out.println(lineNo);

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
