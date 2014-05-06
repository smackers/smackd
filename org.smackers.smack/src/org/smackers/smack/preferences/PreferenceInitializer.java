package org.smackers.smack.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.smackers.smack.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.LLVM_BIN, "/usr/local/smack-project/llvm/install/bin");
		store.setDefault(PreferenceConstants.SMACK_BIN, "/usr/local/smack-project/smack/install/bin");
		store.setDefault(PreferenceConstants.BOOGIE_BIN, "/usr/local/smack-project/boogie/Binaries");
		store.setDefault(PreferenceConstants.CORRAL_BIN, "/usr/local/smack-project/corral/bin");
		store.setDefault(PreferenceConstants.MONO_BIN, "/usr/bin");
		store.setDefault(PreferenceConstants.DEBUG_MODE, true);
		store.setDefault(PreferenceConstants.LOG_FILE, "/tmp/smackd_log.log");
	}

}
