package org.lip6.scheduler.algorithm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.lip6.scheduler.Task;

public class Event {
	private final int time;
	private Set<Task> starting;
	private Set<Task> terminating;

	private Map<Integer, Integer> resourceCapacity;

	public static Comparator<Event> getComparator() {
		return new Comparator<Event>() {
			@Override
			public int compare(Event o1, Event o2) {
				return Integer.compare(o1.getTime(), o2.getTime());
			}
		};
	}

	private Event(int time) {
		this.time = time;
		starting = new HashSet<>();
		terminating = new HashSet<>();
		resourceCapacity = new HashMap<>();
	}

	public static Event get(int time, int numResources) {
		if (time < 0) {
			throw new IllegalArgumentException("Time instant < 0");
		}

		Event e = new Event(time);
		for (int i = 1; i <= numResources; i++) {
			e.resourceCapacity.put(i, 0);
		}

		return e;
	}

	public Set<Task> taskTerminatingHere() {
		return terminating;
	}

	public int getResourceCapacity(int resourceID) {
		return resourceCapacity.getOrDefault(resourceID, 0);
	}

	public Map<Integer, Integer> resourceCapacity() {
		return resourceCapacity;
	}

	public void increaseResourceUsage(int resourceID) {
		resourceCapacity.put(resourceID, resourceCapacity.getOrDefault(resourceID, 0) + 1);
	}

	public void setResourceCapacities(Map<Integer, Integer> capacities){
		resourceCapacity.clear();
		resourceCapacity.putAll(capacities);
	}
	
	public void setResourceCapacity(int resourceID, int value) {
		resourceCapacity.put(resourceID, value);
	}

	public void removePlan(int planID) {
		starting.removeIf(x -> x.getPlanID() == planID);
		terminating.removeIf(x -> x.getPlanID() == planID);
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

	@Override
	public String toString() {
		return "e" + time + " [starting=" + starting + ", terminating=" + terminating + ", "
				+ resourceCapacity.values().stream().map(x -> Integer.toString(x)).collect(Collectors.joining(","))
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		if (time != other.time)
			return false;
		return true;
	}

}
