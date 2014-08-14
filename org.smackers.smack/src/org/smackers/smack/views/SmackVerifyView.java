package org.smackers.smack.views;

public class SmackVerifyView extends SmackView {

	public SmackVerifyView() {
		super("org.smackers.smack.markers.SmackdVerifyMarkerGenerator",
				new String[]{
					"org.smackers.smack.markers.smackAssertionFailedAnnotation",
					"org.smackers.smack.markers.smackAssertionTraceAnnotation"
				});
	}
}