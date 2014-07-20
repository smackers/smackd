package org.smackers.smack.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.smackers.smack.Activator;
import org.smackers.smack.preferences.PreferenceConstants;

public abstract class SmackJob extends Job {
	
	//Marker types
	protected static final String markerFail  = "org.smackers.smack.markers.smackAssertionFailedMarker";
	protected static final String markerTrace = "org.smackers.smack.markers.smackAssertionTraceMarker";
	protected static final String markerReach = "org.smackers.smack.markers.smackUnreachableMarker";
	
	private String normalMarkerID;
	
	private List<String> markerIdsToClear;
	protected void setMarkerIdsToClear(List<String> markerIds) {
		markerIdsToClear = markerIds;
	}
	
	private String successTitle;
	protected void setSuccessTitle(String title) {
		successTitle = title;
	}
	
	private String successMsg;
	protected void setSuccessMsg(String msg) {
		successMsg= msg;
	}
	
	private String errTitle;
	
	protected ExecutionResult vr;
	
	//Get reference to logger
	protected static Logger log = Activator.getDefault().getLogger();
	
	private String cmd;
	protected void setCmd(String cmd) {
		this.cmd = cmd;
	}
	
	private List<String> args;
	protected void setArgs(List<String> args) {
		this.args = args;
	}
	
	//The file being verified (might be changed to project/build config in future)
	private IFile file;
	
	protected void setFile(IFile file) {
		this.file = file;
	}
	protected IFile getFile() {
		return this.file;
	}
	
	//Normal marker ID is the marker for which all but ExecutionResult.failsAt is used
	public SmackJob(String name, String errorTitle, String normalMarkerId) {
		super(name);
		errTitle = errorTitle;
		this.normalMarkerID = normalMarkerId;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		final IProject project = file.getProject();
		//Clear Verify markers
		int depth = IResource.DEPTH_INFINITE;
		try {
			for(String markerID : markerIdsToClear) {
				project.deleteMarkers(markerID, true, depth);
			}
		} catch (CoreException e) {
			// something went wrong
		}
		
		Exception e = new Exception();
		String errMsg = "";
		try {
			vr = execSmack(project, file);
		} catch(ExecutionException e1){
			e = e1;
			e1.printStackTrace();
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			errMsg = e.getMessage();
		} catch (ConnectException e1) {
			e = e1;
			errMsg = "Connection to remote server refused or timed out (" + e.getMessage() + ")";
		} catch (MalformedURLException e1) {
			e = e1;
			errMsg = "Invalid server URL format (" + e.getMessage() + ")";
		} catch (UnknownHostException e1) {
			e = e1;
			errMsg = "Unknown Host (DNS resolution failure - Check URL)";
		} catch (IOException e1) {
			e = e1;
			e1.printStackTrace();
			if(e.getMessage().contains("error=2, No such file or directory")) {
				errMsg = cmd + " was not found in the SMACK bin path.  Please check Smackd' preferences, and ensure SMACK is properly installed";
			} else {
				e = e1;
				errMsg = "Something went wrong:" + e.getMessage() + "\n" + e.getStackTrace();
			}
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
		}
		if(vr != null) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					updateView(project, vr);		
				}
			});	
		} else {
			e.printStackTrace();
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			final String finErrTitle = errTitle;
			final String finErrMsg = errMsg;
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					MessageDialog.openInformation(
							window.getShell(),
							finErrTitle, 
							finErrMsg);
				}
			});	
		}
		
		return Status.OK_STATUS;		
	}

	//Executes SMACK, returns handle to smack process
	protected ExecutionResult execSmack(IProject project, IFile file) throws IOException, ExecutionException {
		
		ProcessBuilder pb = buildSmackProcess(cmd, args, file.getRawLocation().toString());
		
		// Start process
		log.write(Logger.SMACK_EXEC, "Executing " + cmd);
		String commandString = "";
		for(String s : pb.command())
			commandString += s + " ";
		log.write(Logger.SMACK_EXEC, "Calling: " + commandString);
		Process smackProc = pb.start();
		return parseSmackOutput(project, collectLocalProcOutput(smackProc));
	}
	
	private ProcessBuilder buildSmackProcess(String cmd, List<String> args, String filename) {
		ProcessBuilder lpb = new ProcessBuilder();
		
		//Get preference store
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

		//Get paths from preference store
		String smackbin  = preferenceStore.getString(PreferenceConstants.SMACK_BIN);
		String llvmbin   = preferenceStore.getString(PreferenceConstants.LLVM_BIN);
		String boogiebin = preferenceStore.getString(PreferenceConstants.BOOGIE_BIN);
		String corralbin = preferenceStore.getString(PreferenceConstants.CORRAL_BIN);
		String monobin = preferenceStore.getString(PreferenceConstants.MONO_BIN);

		
		//Prepend SMACK_BIN to cmd, then prepend to args
		cmd = smackbin + "/" + cmd;
		// In case Arrays.asList() was used to construct args, convert to ArrayList	
		args = new ArrayList<String>(args);
		args.add(0,cmd);
		args.add(filename);

		lpb.command(args);
		
		//So we see error output on console
		lpb.redirectErrorStream(true);
		
		//Set up environment
		Map<String,String> env = lpb.environment();
		
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
		
		return lpb;
	}
	
	private String collectLocalProcOutput(Process p) throws ExecutionException {
		StringBuilder result = new StringBuilder();

		InputStream is = p.getInputStream();
		InputStreamReader ir = new InputStreamReader(is);
		BufferedReader bri = new BufferedReader(ir);
		
		
		
		try {
			//If we don't wait till bri has output, the call to bri.read()
			//  blocks, and deadlocks the threads.
			while(!bri.ready()) {
				Thread.sleep(100L);
			}
			
			int c;
			while ((c = bri.read()) != -1) {
				result.append((char)c);
			}
		} catch (IOException | InterruptedException e) {
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			e.printStackTrace();
		}
		
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
			e.printStackTrace();
		}
		
		
		log.write(Logger.SMACK_OUTPUT, result.toString());
		if(p.exitValue()!=0)
		{
			
			throw new ExecutionException("Smack terminated abnormally, with the following output:\n\n" + result.toString());
		}
		return result.toString();
	}
	
	protected abstract ExecutionResult parseSmackOutput(IProject project, String output) throws ExecutionException;
	
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
	
	protected static IFile getIFile(IProject project, String fileName) throws CoreException {
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
	
	public void updateView(IProject project, ExecutionResult result) {
		if(!result.isVerificationPassed()) {

			
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
								trace.getDescription(), callOrder++, normalMarkerID);
				} catch (Exception e) {
					Activator.getDefault().getLogger().writeStackTrace(Logger.SMACKD_TRACE, e);
				}
			}
		} else {
			final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			MessageDialog.openInformation(
					window.getShell(),
					successTitle,
					successMsg);
		}
	}	
	
	protected void highlight(	IFile file, int lineNo, int colNo,
			String desc, int callOrder, String markerType) throws CoreException {
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
			final IMarker marker = file.createMarker(markerType);

			//IMarker lines are 1 based idx
			marker.setAttribute(IMarker.LINE_NUMBER, lineNo);
			// Columns from smack are starting at 1
			marker.setAttribute(IMarker.CHAR_START, lineInfo.getOffset() + colNo - 1);
			marker.setAttribute(IMarker.CHAR_END, lineInfo.getLength() + lineInfo.getOffset());
			marker.setAttribute(IMarker.MESSAGE, desc);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute("callOrder", callOrder);
		}
	}
}
