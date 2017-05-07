package org.lip6.scheduler.algorithm;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.lip6.scheduler.Task;

public class Event {
	private final int time;
	private final int resourceID;
	private int residualResourceCapacity;
	private Set<Task> starting;
	private Set<Task> terminating;

	public static Comparator<Event> getComparator() {
		return new Comparator<Event>() {
			@Override
			public int compare(Event o1, Event o2) {
				return Integer.compare(o1.getTime(), o2.getTime());
			}
		};
	}

	private Event(int time, int resourceID) {
		this.time = time;
		this.resourceID = resourceID;
		residualResourceCapacity = 0;
		starting = new HashSet<>();
		terminating = new HashSet<>();
	}

	public static Event get(int time, int resourceID) {
		if (time < 0) {
			throw new IllegalArgumentException("Time instant < 0");
		}

		return new Event(time, resourceID);
	}

	public Set<Task> taskTerminatingHere() {
		return terminating;
	}

	public int getResourceCapacity() {
		return residualResourceCapacity;
	}

	public void removePlan(int planID) {
		starting.removeIf(x -> x.getPlanID() == planID);
		terminating.removeIf(x -> x.getPlanID() == planID);
	}

	public void increaseResourceUsage() {
		this.residualResourceCapacity++;
	}

	public void setResourceCapacity(int value) {
		this.residualResourceCapacity = value;
	}

	public void addToS(Task t) {
		starting.add(t);
	}

	public void addToC(Task t) {
		terminating.add(t);
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + resourceID;
		result = prime * result + time;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		if (resourceID != other.resourceID)
			return false;
		if (time != other.time)
			return false;
		return true;
	}

}
