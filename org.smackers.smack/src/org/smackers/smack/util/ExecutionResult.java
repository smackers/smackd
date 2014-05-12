/**
 * 
 */
package org.smackers.smack.util;

import java.util.ArrayList;

/**
 * @author mcarter
 * 
 * Collects trace information to be displayed
 * 
 * Consumed by xyz to update views
 *
 */
public class ExecutionResult {

	// Verifier used (boogie, corral)
	private String verifier;
	
	// Did the program fail or pass verification?
	private boolean verificationPassed;
	
	// The code location where verification failed
	//   (Trace.threadid need not be set, and isn't used)
	private ExecutionTrace failsAt;
	
	// The number of threads involved in the trace
	private int threadCount;
	
	// The ordered list of traces
	private ArrayList<ExecutionTrace> traces;
	
	ExecutionResult() {
		traces = new ArrayList<ExecutionTrace>();
	}

	public String getVerifier() {
		return verifier;
	}

	public void setVerifier(String verifier) {
		this.verifier = verifier;
	}

	public boolean isVerificationPassed() {
		return verificationPassed;
	}

	public void setVerificationPassed(boolean verificationPassed) {
		this.verificationPassed = verificationPassed;
	}

	public ExecutionTrace getFailsAt() {
		return failsAt;
	}

	public void setFailsAt(ExecutionTrace failsAt) {
		this.failsAt = failsAt;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public ArrayList<ExecutionTrace> getTraces() {
		return traces;
	}

	public void addTrace(ExecutionTrace trace) {
		this.traces.add(trace);
	}
}
