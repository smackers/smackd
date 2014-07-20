package org.smackers.smack.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.views.markers.MarkerViewHandler;
import org.smackers.smack.views.SmackView;



public class ChangeMarkerViewHandler extends MarkerViewHandler {
	
	/**
	 * The constructor.
	 */
	public ChangeMarkerViewHandler() {
	}

	
	@SuppressWarnings({ "restriction"})
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String eventCommandId = event.getCommand().getId();
		SmackView sendingSmackView = (SmackView)getView(event);
		
		if(eventCommandId.equals("org.smackers.smack.commands.selectNextMarkerCommand")) {
			sendingSmackView.selectNextMarker();
			sendingSmackView.setFocus();
		} else if(eventCommandId.equals("org.smackers.smack.commands.selectPreviousMarkerCommand")){
			sendingSmackView.selectPreviousMarker();
			sendingSmackView.setFocus();
		} else if(eventCommandId.equals("org.smackers.smack.commands.clearVerifyMarkersCommand") ||
				eventCommandId.equals("org.smackers.smack.commands.clearReachMarkersCommand")) {
			sendingSmackView.deleteCurrentMarkers();
		}

		return this;
	}

}
