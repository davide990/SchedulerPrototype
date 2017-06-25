package org.lip6.scheduler.algorithm;

import java.util.Collections;
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
				return Integer.compare(o1.getTimeInstant(), o2.getTimeInstant());
			}
		};
	}

	/**
	 * The time instant this event is related to.
	 */
	private final int time;
	/**
	 * The set of task that start exactly at {@link Event#getTimeInstant()}
	 */
	private Set<Task> starting;
	/**
	 * The set of task that terminate exactly at {@link Event#getTimeInstant()}
	 */
	private Set<Task> terminating;
	/**
	 * A map that contains the usage (value) of each resource(key). <br/>
	 * The value represents how much resource is used between t<sub>e</sub> and
	 * next<sub>t<sub>e</sub></sub>
	 */
	private Map<Integer, Integer> resourceUsages;

	private Event(int time) {
		this.time = time;
		starting = new HashSet<>();
		terminating = new HashSet<>();
		resourceUsages = new HashMap<>();
	}

	public static Event get(int time, Set<Integer> resourcesIDs) {
		if (time < 0) {
			throw new IllegalArgumentException("Time instant < 0");
		}

		Event e = new Event(time);
		for (Integer resID : resourcesIDs) {
			e.resourceUsages.put(resID, 0);
		}

		return e;
	}

	/**
	 * The set of tasks starting exactly at this event's time instant
	 * 
	 * @return
	 */
	public Set<Task> taskStartingHere() {
		return starting;
	}

	/**
	 * The set of tasks that terminate exactly at this event's time instant
	 * 
	 * @return
	 */
	public Set<Task> taskTerminatingHere() {
		return terminating;
	}

	public int getResourceCapacity(int resourceID) {
		return resourceUsages.getOrDefault(resourceID, 0);
	}

	/**
	 * The map that contains the informations on the usage of the resource at
	 * this event's time instant. The <b>key</b> is the ID of a resource, the
	 * <b>value</b> is the usage of the resource.
	 * 
	 * @return a <b>read only</b> map.
	 */
	public Map<Integer, Integer> resourceUsages() {
		return Collections.unmodifiableMap(resourceUsages);
		// return resourceUsages;
	}

	/**
	 * Increase by 1 the usage of the specified resource.
	 * 
	 * @param resourceID
	 */
	public void increaseResourceUsage(int resourceID) {
		resourceUsages.put(resourceID, resourceUsages.getOrDefault(resourceID, 0) + 1);
	}

	public void setResourceCapacities(Map<Integer, Integer> capacities) {
		resourceUsages.clear();
		resourceUsages.putAll(capacities);
	}

	public void setResourceCapacity(int resourceID, int value) {
		resourceUsages.put(resourceID, value);
	}

	/**
	 * Remove all the tasks belonging to the plan with the specified ID from
	 * this event.
	 * 
	 * @param planID
	 */
	public void removePlan(int planID) {
		for (Task x : starting) {
			if (x.getPlanID() == planID) {
				x.getResourcesID().forEach(r -> resourceUsages.put(r, Math.max(0, resourceUsages.get(r) - 1)));
			}
		}

		starting.removeIf(x -> x.getPlanID() == planID);
		terminating.removeIf(x -> x.getPlanID() == planID);

	}

	/**
	 * Add the task t to the set of tasks starting exactly at this event's time
	 * instant.
	 * 
	 * @param t
	 */
	public void addToS(Task t) {
		starting.add(t);
	}

	/**
	 * Add the task t to the set of tasks terminating exactly at this event's
	 * time instant.
	 * 
	 * @param t
	 */
	public void addToC(Task t) {
		terminating.add(t);
	}

	/**
	 * Get the time instant related to this event.
	 * 
	 * @return
	 */
	public int getTimeInstant() {
		return time;
	}

	@Override
	public String toString() {
		return "e" + time + " [S=" + starting + ", C=" + terminating + ", "
				+ resourceUsages.values().stream().map(x -> Integer.toString(x)).collect(Collectors.joining(",")) + "]";
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
		Event cloned = Event.get(getTimeInstant(), resourceUsages.keySet());
		cloned.starting = new HashSet<>(starting);
		cloned.terminating = new HashSet<>(terminating);
		cloned.resourceUsages = new HashMap<>(resourceUsages);
		return cloned;
	}

	@Override
	public int compareTo(Event o) {
		return Integer.compare(getTimeInstant(), o.time);
	}

}
