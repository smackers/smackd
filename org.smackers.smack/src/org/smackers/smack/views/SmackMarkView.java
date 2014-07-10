package org.smackers.smack.views;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.internal.views.markers.ExtendedMarkersView;
import org.eclipse.ui.views.markers.MarkerSupportView;

@SuppressWarnings("restriction")
public class SmackMarkView extends MarkerSupportView {

	public SmackMarkView() {
		super("org.smackers.smack.markers.SmackMarkerGenerator");
	}
	
	public void deleteCurrentMarkers(){
		IMarker[] curMarkers = getCurrentMarkers();
		for(IMarker curMark : curMarkers) {
			try {
				curMark.delete();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Get the current markers for the receiver.
	 * 
	 * @return
	 */
	private IMarker[] getCurrentMarkers() {
		//Uses method on internal ExtendedMarkersView to grab currently displayed markers.
		Method method;
		try {
			method = ExtendedMarkersView.class.getDeclaredMethod("getAllMarkers",
					new Class[0]);
			method.setAccessible(true);
		} catch (SecurityException e) {
			e.printStackTrace();
			return new IMarker[0];
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return new IMarker[0];
		}
		try {
			return (IMarker[]) method.invoke(this, new Object[0]);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return new IMarker[0];
	}
}