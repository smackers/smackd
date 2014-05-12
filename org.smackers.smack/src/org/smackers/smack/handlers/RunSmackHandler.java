package org.smackers.smack.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.resources.IFile;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
//import org.eclipse.core.runtime.jobs.Job;

import org.smackers.smack.util.Controller;
import org.smackers.smack.util.ExecutionResult;
import org.smackers.smack.util.TraceParser;


/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class RunSmackHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public RunSmackHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		final IEditorInput input = HandlerUtil.getActiveEditorInput(event);
		//Job addJob = new Job("Add Markers") {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
//			protected IStatus run(IProgressMonitor monitor) {

					if (input instanceof IFileEditorInput) {
						IFileEditorInput fileInput = (IFileEditorInput) input;
						IFile file = fileInput.getFile();
						String smackOutput = Controller.execSmack(file.getRawLocation().toString());
						ExecutionResult smackExecutionResult = TraceParser.parseSmackOutput(file,smackOutput);
						Controller.UpdateViews(smackExecutionResult);
					} else {
						MessageDialog.openInformation(
								window.getShell(),
								"Test", 
								"Hello, Eclipse world");
					}
				return Status.OK_STATUS;
//			}
	//	};
		//addJob.schedule();
//		return this;
	}
}
