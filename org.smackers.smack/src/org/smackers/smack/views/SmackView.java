package org.smackers.smack.views;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.views.markers.ExtendedMarkersView;
import org.eclipse.ui.views.markers.MarkerSupportView;

@SuppressWarnings("restriction")
public abstract class SmackView extends MarkerSupportView {
	
	
	public SmackView(String contentGeneratorId) {
		super(contentGeneratorId);
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
	
	public void selectNextMarker() {
		changeMarkerSelection(1);
	}
	
	public void selectPreviousMarker() {
		changeMarkerSelection(-1);
	}
		
	private void changeMarkerSelection(int relativeIndex) {
		IMarker[] selMarkers = getSelectedMarkers();
		IMarker[] allMarkers = getCurrentMarkers();
		IMarker[] newSel = new IMarker[1];
		if(selMarkers.length == 0) {
			if(allMarkers.length == 0){
				return;
			}
			newSel[0] = allMarkers[0];
		} else {
			IMarker target = selMarkers[0];
			for(int x = 0; x < allMarkers.length; x++) {
				if(target.equals(allMarkers[x])) {
					//Make sure modulus gives positive number (for wrapping around to first/last marker)
					int index = (((x+relativeIndex) % allMarkers.length) + allMarkers.length) % allMarkers.length;
					newSel[0] = allMarkers[index];
				}
			}
		}
			
		//Uses method on internal ExtendedMarkersView to grab currently displayed markers.
		Method method;
		try {
			method = ExtendedMarkersView.class.getDeclaredMethod("setSelection",
					new Class[]{StructuredSelection.class, boolean.class});
			method.setAccessible(true);
		} catch (SecurityException e) {
			e.printStackTrace();
			return;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return;
		}
		try {
			method.invoke(this, new Object[]{new StructuredSelection(newSel), true});
			openMarkerInEditor(newSel[0], PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
