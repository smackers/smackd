package org.smackers.smack.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import org.smackers.smack.Activator;
import org.smackers.smack.preferences.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class Logger {
	
	//For now we will check preferences page every time.
	
	//In the future, the logger should be switched to enable/disable 
	//  based on event driven notification from preferences page.
	
	public static final String SMACKD_TRACE = "SMACKD TRACE";
	public static final String SMACKD_ERR   = "SMACKD ERR";
	public static final String SMACKD_PARAM = "SMACKD PARAM";
	public static final String SMACK_OUTPUT = "SMACK OUT";
	public static final String SMACK_EXEC   = "SMACK EXEC";
	public static final String SMACK_ENV    = "SMACK ENV";
	
	String msgIdent;
	int widthLimit;
	MessageConsoleStream consoleOut;
	
	public Logger() {
		//Configure indent
		int msgIdentQty = 5;
		msgIdent = "";
		for(int x = 0; x < msgIdentQty; x++) 
			msgIdent += "\t";
		widthLimit = 40;
		
		//Get a console
		consoleOut = findConsole("SMACKD Console").newMessageStream();
	}
	
	/*
	 * Code from eclipse.org to get a console to print to
	 */
	private static MessageConsole findConsole(String name) {
		final ConsolePlugin plugin = ConsolePlugin.getDefault();
		final IConsoleManager conMan = plugin.getConsoleManager();
		final IConsole[] existing = conMan.getConsoles();
		for (final IConsole element : existing) {
			if (name.equals(element.getName())) {
				return (MessageConsole) element;
			}
		}
		// no console found, so create a new one
		final MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}
	
	void write(String category, String message) {
		final IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		boolean debug    = preferenceStore.getBoolean(PreferenceConstants.DEBUG_MODE);
		String logfile   = preferenceStore.getString(PreferenceConstants.LOG_FILE);
		
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
			if(debug) {
				//Print to log file
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)));
				out.println(sbLog.toString());
				out.close();
			}
			// And to the eclipse console
			consoleOut.println(sbLog.toString());
		} catch (IOException e) {
			//exception handling left as an exercise for the reader
		}
	}
	
	public void writeStackTrace(String category, Exception exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String stackTrace = sw.toString();
		write(category, stackTrace);
	}
}
