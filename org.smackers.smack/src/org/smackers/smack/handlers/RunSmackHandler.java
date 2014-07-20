package org.smackers.smack.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.smackers.smack.util.SmackJob;
import org.smackers.smack.util.SmackJobReach;
import org.smackers.smack.util.SmackJobVerify;
import org.smackers.smack.util.SmackJobVerifyRemote;


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
		String eventCommandId = event.getCommand().getId();
		
		if (input instanceof IFileEditorInput) {
			try {
				PlatformUI.getWorkbench().showPerspective("org.eclipse.cdt.ui.CPerspective", window);
			} catch (WorkbenchException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				if(eventCommandId.equals("org.smackers.smack.commands.runSmackVerifyCommand") ||
						eventCommandId.equals("org.smackers.smack.commands.runRemoteSmackVerifyCommand")) {
					window.getActivePage().showView("org.smackers.smack.views.SmackVerifyView");
				} else {
					window.getActivePage().showView("org.smackers.smack.views.SmackReachView");
				}
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			IFileEditorInput fileInput = (IFileEditorInput) input;
			IFile file = fileInput.getFile();
			
			SmackJob addJob;

			if(eventCommandId.equals("org.smackers.smack.commands.runSmackVerifyCommand")) {
				addJob = new SmackJobVerify(file);
			} else if(eventCommandId.equals("org.smackers.smack.commands.runRemoteSmackVerifyCommand")) {
				addJob = new SmackJobVerifyRemote(file);
			} else {
				addJob = new SmackJobReach(file);
			}
			addJob.setUser(true);
			addJob.schedule();

		} else {
			MessageDialog.openInformation(
					window.getShell(),
					"No Project", 
					"Switch focus to the editor containing the file to verify");
		}
		return this;
	}
}
