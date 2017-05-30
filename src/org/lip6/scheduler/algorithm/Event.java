package org.lip6.scheduler.algorithm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.lip6.scheduler.Task;

/**
 * Event class
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public class Event implements Cloneable, Comparable<Event> {

	/**
	 * @return The default comparator for the {@link Event} class. The
	 *         comparison is based on the time instant.
	 */
	public static Comparator<Event> getComparator() {
		return new Comparator<Event>() {
			@Override
			public int compare(Event o1, Event o2) {
				return Integer.compare(o1.getTime(), o2.getTime());
			}
		};
	}

	/**
	 * The time instant this event is related to.
	 */
	private final int time;
	/**
	 * The set of task that start exactly at {@link Event#getTime()}
	 */
	private Set<Task> starting;
	/**
	 * The set of task that terminate exactly at {@link Event#getTime()}
	 */
	private Set<Task> terminating;
	/**
	 * A map that contains the usage (value) of each resource(key).
	 */
	private Map<Integer, Integer> resourceUsage;

	private Event(int time) {
		this.time = time;
		starting = new HashSet<>();
		terminating = new HashSet<>();
		resourceUsage = new HashMap<>();
	}

	public static Event get(int time, Set<Integer> resourcesIDs) {
		if (time < 0) {
			throw new IllegalArgumentException("Time instant < 0");
		}

		Event e = new Event(time);
		for (Integer resID : resourcesIDs) {
			e.resourceUsage.put(resID, 0);
		}

		return e;
	}

	public Set<Task> taskStartingHere() {
		return starting;
	}

	public Set<Task> taskTerminatingHere() {
		return terminating;
	}

	public int getResourceCapacity(int resourceID) {
		return resourceUsage.getOrDefault(resourceID, 0);
	}

	public Map<Integer, Integer> resourceCapacity() {
		return resourceUsage;
	}

	public void increaseResourceUsage(int resourceID) {
		resourceUsage.put(resourceID, resourceUsage.getOrDefault(resourceID, 0) + 1);
	}

	public void setResourceCapacities(Map<Integer, Integer> capacities) {
		resourceUsage.clear();
		resourceUsage.putAll(capacities);
	}

	public void setResourceCapacity(int resourceID, int value) {
		resourceUsage.put(resourceID, value);
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
		return "e" + time + " [S=" + starting + ", C=" + terminating + ", "
				+ resourceUsage.values().stream().map(x -> Integer.toString(x)).collect(Collectors.joining(",")) + "]";
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

	@Override
	protected Object clone() throws CloneNotSupportedException {
		Event cloned = Event.get(getTime(), resourceUsage.keySet());
		cloned.starting = new HashSet<>(starting);
		cloned.terminating = new HashSet<>(terminating);
		cloned.resourceUsage = new HashMap<>(resourceUsage);
		return cloned;
	}

	@Override
	public int compareTo(Event o) {
		return Integer.compare(getTime(), o.time);
	}

}
