package org.lip6.scheduler.algorithm;

import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.lip6.scheduler.Task;

/**
 * Utility methods for the Event class
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public class EventUtils {

	/**
	 * Get the latest event e which contains t in C(e).
	 * 
	 * @param t
	 * @param events
	 * @return
	 */
	public static Optional<Event> getLastEventWhereTerminates(Task t, NavigableSet<Event> events) {

		List<Event> vv = events.stream().filter(x -> x.taskTerminatingHere().contains(t)).collect(Collectors.toList());
		OptionalInt max = vv.stream().mapToInt(x -> x.getTime()).max();

		if (max.isPresent()) {
			return events.stream().filter(x -> x.getTime() == max.getAsInt()).findFirst();
		}
		return Optional.empty();
	}

	/**
	 * Get all the events which time instant t(e) is greater than t
	 * 
	 * @param t
	 * @param events
	 * @return
	 */
	public List<Event> executedAfter(int t, NavigableSet<Event> events) {
		return events.stream().filter(x -> x.getTime() > t).collect(Collectors.toList());
	}

	/**
	 * Get the last
	 * 
	 * @param We
	 * @param events
	 * @return
	 */
	public static Optional<Event> getLastEvent(int We, NavigableSet<Event> events) {
		return events.stream().filter(x -> x.getTime() == We).findFirst();
	}

	public static Optional<Event> getNextEvent(Event v, NavigableSet<Event> events) {
		try {
			return Optional.of(events.tailSet(v, false).first());

		} catch (NoSuchElementException ex) {
			return Optional.empty();
		}
	}

	public static Optional<Event> getNextEventForResource(int resourceID, Event v, NavigableSet<Event> events) {
		try {
			return events.stream()
					.filter(x -> x.getTime() > v.getTime() && x.taskStartingHere().stream()
							.filter(t -> t.getResourceID() == resourceID).findAny().isPresent())
					.min(Event.getComparator());
		} catch (NoSuchElementException ex) {
			return Optional.empty();
		}
	}

	public static Optional<Event> getPreviousEvent(int t, Set<Integer> resourcesIDs, boolean inclusive,
			NavigableSet<Event> events) {
		try {
			return Optional.of(events.headSet(Event.get(t, resourcesIDs), inclusive).last());
		} catch (NoSuchElementException ex) {
			return Optional.empty();
		}
	}

	public static Optional<Event> getPreviousEvent(Event v, NavigableSet<Event> events) {
		return getPreviousEvent(v, events, false);
	}

	public static Optional<Event> getPreviousEvent(Event v, NavigableSet<Event> events, boolean inclusive) {
		try {
			return Optional.of(events.headSet(v, inclusive).last());
		} catch (NoSuchElementException ex) {
			return Optional.empty();
		}
	}

	public static TreeSet<Event> cloneSet(TreeSet<Event> events) {
		TreeSet<Event> E = new TreeSet<>();
		for (Event x : events) {
			try {
				E.add((Event) x.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return E;
	}

}
