package org.smackers.smack.views;


public class SmackReachView extends SmackView {

	public SmackReachView() {
		super("org.smackers.smack.markers.SmackdReachMarkerGenerator",
				new String[]{
					"org.smackers.smack.markers.smackUnreachableAnnotation"
				});
	}
	
}