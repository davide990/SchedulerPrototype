package org.lip6.scheduler.algorithm;

import java.util.List;

public class Event {
	private final int time;
	private final int resourceID;
	private int residualResourceCapacity;
	private List<Integer> starting;
	private List<Integer> terminating;

	private Event(int time, int resourceID) {
		this.time = time;
		this.resourceID = resourceID;
	}

	public static Event get(int time, int resourceID) {
		if (time < 0) {
			throw new IllegalArgumentException("Time instant < 0");
		}

		return new Event(time, resourceID);
	}

	public int getResidualResourceCapacity() {
		return residualResourceCapacity;
	}

	public void setResidualResourceCapacity(int residualResourceCapacity) {
		this.residualResourceCapacity = residualResourceCapacity;
	}

	public List<Integer> getStarting() {
		return starting;
	}

	public void setStarting(List<Integer> starting) {
		this.starting = starting;
	}

	public List<Integer> getTerminating() {
		return terminating;
	}

	public void setTerminating(List<Integer> terminating) {
		this.terminating = terminating;
	}

	public int getTime() {
		return time;
	}

	public int getResourceID() {
		return resourceID;
	}

	@Override
	public String toString() {
		return "e" + Integer.toString(time) + " [resourceID=" + resourceID + ", b=" + residualResourceCapacity + ", S="
				+ starting + ", C=" + terminating + "]";
	}

}
