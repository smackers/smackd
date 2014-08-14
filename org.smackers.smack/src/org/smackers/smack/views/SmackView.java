package org.smackers.smack.views;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.internal.views.markers.ExtendedMarkersView;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;

@SuppressWarnings("restriction")
public abstract class SmackView extends MarkerSupportView {
	
	private static final String[] allSmackAnnotationTypes = {
		"org.smackers.smack.markers.smackAssertionFailedAnnotation",
		"org.smackers.smack.markers.smackAssertionTraceAnnotation",
		"org.smackers.smack.markers.smackUnreachableAnnotation"
	};
	
	private List<String> mySmackAnnotationTypes;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.views.markers.ExtendedMarkersView#setFocus()
	 * 
	 * This is overridden to allow for the deactivation of smackd markers other than
	 *   the ones displayed by this view
	 */
	@Override
	public void setFocus() {
		System.out.println("Active: "+this.getTitle());
		
		AnnotationPreferenceLookup apl = new AnnotationPreferenceLookup();
		IPreferenceStore ps = EditorsPlugin.getDefault().getPreferenceStore();
		for(String curAnnType : allSmackAnnotationTypes) {
			AnnotationPreference ap = apl.getAnnotationPreference(curAnnType);
			if(mySmackAnnotationTypes.contains(curAnnType)) {
				// If current annotation is one registered for this SmackView, then display it
				ps.setValue(ap.getTextPreferenceKey(), true);
				ps.setValue(ap.getHighlightPreferenceKey(), true);
				ps.setValue(ap.getOverviewRulerPreferenceKey(), true);
				ps.setValue(ap.getVerticalRulerPreferenceKey(), true);
			} else {
				// Otherwise, hide it
				ps.setValue(ap.getTextPreferenceKey(), false);
				ps.setValue(ap.getHighlightPreferenceKey(), false);
				ps.setValue(ap.getOverviewRulerPreferenceKey(), false);
				ps.setValue(ap.getVerticalRulerPreferenceKey(), false);	
			}
		}
		super.setFocus();
	}

	/**
	 * Creates a new SmackView for the given contentGeneratorId.
	 * <p>
	 * The mySmackAnnotationTypes parameter is used to enable annotation
	 * display for the listed types during setFocus()
	 *
	 * @param contentGeneratorId the id of a markerContentGenerator defined in an extension of the markerSupport extension.
	 * @param mySmackAnnotationTypes a list of AnnotationTypes that should be visible when this view is selected 
	 */
	public SmackView(String contentGeneratorId, String[] mySmackAnnotationTypes) {
		super(contentGeneratorId);
		this.mySmackAnnotationTypes = new ArrayList<String>(Arrays.asList(mySmackAnnotationTypes));
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
