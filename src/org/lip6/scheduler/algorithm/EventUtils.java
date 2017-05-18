package org.lip6.scheduler.algorithm;

import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.lip6.scheduler.Task;

public class EventUtils {

	public static Optional<Event> getLastEventWhereTerminates(Task t, NavigableSet<Event> events) {

		List<Event> vv = events.stream().filter(x -> x.taskTerminatingHere().contains(t)).collect(Collectors.toList());
		OptionalInt max = vv.stream().mapToInt(x -> x.getTime()).max();

		if (max.isPresent()) {
			return events.stream().filter(x -> x.getTime() == max.getAsInt()).findFirst();
		}
		return Optional.empty();
	}

	public List<Event> executedAfter(int t, NavigableSet<Event> events) {
		return events.stream().filter(x -> x.getTime() > t).collect(Collectors.toList());
	}

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

	public static Optional<Event> getPreviousEvent(int t, int resID, boolean inclusive, NavigableSet<Event> events) {
		try {
			return Optional.of(events.headSet(Event.get(t, resID), inclusive).last());
		} catch (NoSuchElementException ex) {
			return Optional.empty();
		}
	}

	public static Optional<Event> getPreviousEvent(Event v, NavigableSet<Event> events) {
		try {
			return Optional.of(events.headSet(v).last());
		} catch (NoSuchElementException ex) {
			return Optional.empty();
		}
	}

}
