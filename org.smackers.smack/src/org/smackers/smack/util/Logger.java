package org.smackers.smack.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import org.smackers.smack.Activator;
import org.smackers.smack.preferences.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;

public class Logger {
	
	//For now we will check preferences page every time.
	
	//In the future, the logger should be switched to enable/disable 
	//  based on event driven notification from preferences page.
	
	
	
	public static final String SMACK_OUTPUT = "SMACK OUT";
	public static final String SMACK_EXEC = "SMACK EXEC";
	public static final String SMACK_ENV = "SMACK ENV";
	
	String msgIdent;
	int widthLimit;
	
	public Logger() {
		int msgIdentQty = 5;
		msgIdent = "";
		for(int x = 0; x < msgIdentQty; x++) 
			msgIdent += "\t";
		widthLimit = 40;
	}
	
	void write(String category, String message) {
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		boolean debug    = preferenceStore.getBoolean(PreferenceConstants.DEBUG_MODE);
		String logfile   = preferenceStore.getString(PreferenceConstants.LOG_FILE);
		if(debug) {
			//Line header
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			StringBuilder sbLog = new StringBuilder(df.format(new java.util.Date()) + "\t" + category + "\t");
			// Keep message indented properly
			int sinceNewLine = 0;
			for(int x = 0; x < message.length(); x++) {
				if(message.charAt(x) == '\n') {
					//This is a user provided newline.
					//  Indent properly
					sbLog.append("\n" + msgIdent);
					sinceNewLine = 0;
				} else {
					sbLog.append(message.charAt(x));
					sinceNewLine++;
					if(sinceNewLine > widthLimit) {
						sbLog.append("\n" + msgIdent + "\t");
						sinceNewLine = 0;
					}
				}
			}
			sbLog.append('\n');
			
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
			    out.println(sbLog.toString());
			    out.close();
			} catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
		}
	}
	
	
	

}
