package org.smackers.smack.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.smackers.smack.Activator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class SmackPreferences
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public SmackPreferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("SMACKd Options");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(new DirectoryFieldEditor(	PreferenceConstants.LLVM_BIN, 
											"&LLVM bin Path:",
											getFieldEditorParent()));
		
		addField(new DirectoryFieldEditor(	PreferenceConstants.SMACK_BIN,
											"&SMACK bin Path:",
											getFieldEditorParent()));

		addField(new DirectoryFieldEditor(	PreferenceConstants.BOOGIE_BIN,
											"&Boogie bin Path:",
											getFieldEditorParent()));
		
		addField(new DirectoryFieldEditor(	PreferenceConstants.CORRAL_BIN,
											"Corral bin Path:",
											getFieldEditorParent()));
		
		addField(new DirectoryFieldEditor(	PreferenceConstants.MONO_BIN,
											"Mono bin Path:",
											getFieldEditorParent()));
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}