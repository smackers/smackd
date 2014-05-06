package org.smackers.smack.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
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
import org.smackers.smack.preferences.PreferenceConstants;

import org.smackers.smack.Activator;

public class TraceParser {
	
	public static void parseSmackOutput(IFile file, String output) {
		//NOTE: Does not currently support parenthesis in filename
		String[] lines = output.split(System.getProperty("line.separator"));
		//String sourcefile, desc;
		String desc;
		//int lineNo, pos;
		int lineNo;
		int callOrder = 0;
		boolean smackRan = false;
		for (int i=0; i < lines.length; i++) {
			String line = lines[i];
			switch(line.substring(0,Math. min(line.length(),13))) {
			case "":
				break;
			case "SMACK verifie":
				smackRan = true;
				break;
			case "Finished with":
				break;
			default:
				if(smackRan) {
					//TODO Get all Smack markers, clear them
					int idx1stPar = line.indexOf('(');
					int idxLineNoComma = line.indexOf(',', idx1stPar);
					int idxClosePar = line.indexOf(')', idxLineNoComma);
					//sourcefile = line.substring(0,idx1stPar);
					lineNo = Integer.parseInt(line.substring(idx1stPar + 1, idxLineNoComma));
					//pos = Integer.parseInt(line.substring(idxLineNoComma + 1, idxClosePar));
					if(line.length() > idxClosePar + 3){
						desc = line.substring(idxClosePar + 3);
					} else {
						desc = "";
					}
					try{
						highlight(file, lineNo, desc, callOrder++);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
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
			ProcessBuilder pb = new ProcessBuilder(smackbin + "/" + cmd, filename);

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
			//TODO Can we assume mono in path?
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