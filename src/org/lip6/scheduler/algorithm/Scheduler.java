package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lip6.graph.TopologicalSorting;
import org.lip6.scheduler.ExecutableNode;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskSchedule;
import org.lip6.scheduler.utils.CSVParser;
import org.lip6.scheduler.utils.Utils;

public class Scheduler {

	private static final int MAX_TABU_TURNS = 10;
	private static final int MAX_TABU_TRIES = 10;

	private static final Comparator<Plan> PLAN_COMPARATOR = new Comparator<Plan>() {
		@Override
		public int compare(Plan o1, Plan o2) {
			return Float.compare(o1.getScore(), o2.getScore());
		}
	};

	public static Schedule scheduleFromFile(int maxResourceCapacity, int WStart, int WEnd, List<Criteria> criterias,
			String filename) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(filename);
		} catch (IOException e) {
			System.err.println("Error while loading file: \"" + filename + "\"");
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		List<Plan> plans = new ArrayList<>(p.values());
		Schedule s = buildSchedule(maxResourceCapacity, plans, criterias, WStart, WEnd);

		return s;
	}

	public static Schedule schedule(int maxResourceCapacity, int WStart, int WEnd, List<Criteria> criterias,
			InputStream is) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(is);
		} catch (IOException | ParseException e) {
			System.err.println("Error while processing input stream.\n" + e);
			return null;
		}
		List<Plan> plans = new ArrayList<>(p.values());
		Schedule s = buildSchedule(maxResourceCapacity, plans, criterias, WStart, WEnd);
		return s;
	}

	public static Schedule schedule(int maxResourceCapacity, int WStart, int WEnd, List<Criteria> criterias,
			List<Plan> plans) {
		Schedule s = buildSchedule(maxResourceCapacity, plans, criterias, WStart, WEnd);
		return s;
	}

	/**
	 * ALGORITHM 1
	 * 
	 * @param maxResourceCapacity
	 * @param plans
	 * @param criterias
	 * @param wStart
	 * @param wEnd
	 * @return
	 */
	private static Schedule buildSchedule(final int maxResourceCapacity, List<Plan> plans, List<Criteria> criterias,
			int wStart, int wEnd) {

		// Get the number of resources employed by the set of plans
		int numResources = getNumberOfResources(plans);
		// Create the empty schedules
		Schedule workingSolution = Schedule.get(numResources, wStart, wEnd);
		Schedule lastFeasibleSolution = Schedule.get(numResources, wStart, wEnd);
		TreeSet<Event> events = new TreeSet<>(Event.getComparator());
		// Add the event for Ws
		events.add(Event.get(wStart, numResources));
		// Add the event for We
		events.add(Event.get(wEnd, numResources));

		Queue<Plan> scheduledPlans = new PriorityQueue<>(PLAN_COMPARATOR);
		Queue<Plan> unscheduledPlans = new PriorityQueue<>(PLAN_COMPARATOR);

		// Calculate the inverse priority for each plan
		int maxPriority = Collections.max(plans.stream().map(Plan::getPriority).collect(Collectors.toList()));
		plans.forEach(p -> p.setInversePriority(maxPriority));

		// Sort the plans so that the precedences are respected
		// List<ExecutableNode> pl = sortPlans(plans.stream().map(x ->
		// (ExecutableNode) x).collect(Collectors.toList()));
		// plans = pl.stream().map(x -> (Plan) x).collect(Collectors.toList());

		sortByPriorities(plans);

		// key -> priorità, value -> numero di piani che hanno tale priorità
		Map<Integer, Integer> prioritiesCountMap = new HashMap<>();

		plans.forEach(x -> {
			if (!prioritiesCountMap.containsKey(x.getPriority())) {
				prioritiesCountMap.put(x.getPriority(), 1);
			} else {
				prioritiesCountMap.put(x.getPriority(), prioritiesCountMap.get(x.getPriority()) + 1);
			}
		});

		System.err.println(
				"sorted -> " + plans.stream().map(x -> Integer.toString(x.getID())).collect(Collectors.joining(",")));

		// MAIN LOOP
		while (!plans.isEmpty()) {
			// Get the plan with the highest priority
			Plan pk = plans.get(0);
			plans.remove(0);
			if (pk == null) {
				continue;
			}

			if (prioritiesCountMap.get(pk.getPriority()) == 1) {
				boolean scheduled = SchedulePlan(pk, workingSolution, events, maxResourceCapacity);

				if (scheduled) {
					scheduledPlans.add(pk);
					try {
						lastFeasibleSolution = (Schedule) workingSolution.clone();
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					unscheduledPlans.add(pk);
				}
			} else {
				List<Plan> toSchedule = new ArrayList<>();
				toSchedule.add(pk);
				toSchedule.addAll(
						plans.stream().filter(x -> x.getPriority() == pk.getPriority()).collect(Collectors.toList()));
				plans.removeAll(toSchedule);

				// Handle here the plans that have the same priority
				SchedulePlansWithSamePriority(toSchedule, workingSolution, events, maxResourceCapacity);

				// ...
			}

		}

		events.forEach(x -> System.err.println(x));
		return lastFeasibleSolution;
	}

	/**
	 * Schedule multiple plans that have the same priority value.
	 * 
	 * @param plans
	 * @param workingSolution
	 * @param events
	 * @param maxResourceCapacity
	 * @return
	 */
	private static boolean SchedulePlansWithSamePriority(List<Plan> plans, Schedule workingSolution,
			TreeSet<Event> events, int maxResourceCapacity) {

		Map<Plan, TreeSet<Event>> validPlans = new HashMap<>();
		Map<Plan, List<Integer>> tasksSorted = new HashMap<>();

		// Generate "intermediate" solutions by scheduling each single plan into
		// the actual solution
		for (Plan p : plans) {
			Schedule S = null;
			TreeSet<Event> E = null;
			try {
				S = (Schedule) workingSolution.clone();
				E = (TreeSet<Event>) events.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			boolean scheduled = SchedulePlan(p, S, E, maxResourceCapacity);

			if (scheduled) {
				validPlans.put(p, E);

				// Keep the topologically sorted set of tasks for the plan p
				Collection<ExecutableNode> tasks = p.getTasks().stream().map(x -> (ExecutableNode) x)
						.collect(Collectors.toList());
				List<Integer> sortedTasks = TopologicalSorting.calculateTopologicalOrderScores(tasks).stream()
						.map(x -> x.left).collect(Collectors.toList());
				tasksSorted.put(p, sortedTasks);
			}
		}

		// Now assemble the final solution...

		return true;
	}

	/**
	 * Schedule the plan given as input into the
	 * 
	 * @param pk
	 * @param workingSolution
	 * @param lastFeasibleSolution
	 * @param events
	 * @param maxResourceCapacity
	 */
	private static boolean SchedulePlan(Plan pk, Schedule workingSolution, TreeSet<Event> events,
			int maxResourceCapacity) {

		// Loop each task t in the plan pk
		for (Task t : pk.getTasks()) {
			// Check precedence constraints
			if (!checkPrecedences(workingSolution, t)) {
				pk.setSchedulable(false);
				continue;
			}

			if (!scheduleTask(maxResourceCapacity, workingSolution, t, events)) {
				pk.setSchedulable(false);
				break;
			}
		}
		// At this point, each task of pk has been scheduled
		if (pk.isSchedulable()) {
			/*
			 * try { lastFeasibleSolution = (Schedule) workingSolution.clone();
			 * } catch (CloneNotSupportedException e) { e.printStackTrace(); }
			 */
			return true;
		} else {
			// pk is NOT schedulable: take all its tasks and remove them
			// from the solution
			List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
					.filter(x -> ((Task) x.getTask()).getPlanID() == pk.getID()).collect(Collectors.toList());
			workingSolution.unSchedule(toRemove);
			return false;
		}
	}

	/**
	 * Schedule the task t
	 * 
	 * @param maxResourceCapacity
	 * @param s
	 * @param t
	 * @param events
	 * @return
	 */
	private static boolean scheduleTask(final int maxResourceCapacity, Schedule s, Task t, NavigableSet<Event> events) {
		int sk = getInitialStartingTime(s.getWStart(), events, t);
		// Start event!
		Event e = EventUtils.getPreviousEvent(sk, t.getResourceID(), true, events).get();

		if (e.getTime() < sk && !events.contains(Event.get(sk, t.getResourceID()))) {
			e = Event.get(sk, t.getResourceID());

		}

		Event f = e;
		Event g = f;
		int mi = t.getProcessingTime();
		final Event lastEvent;
		try {
			lastEvent = EventUtils.getLastEvent(s.getWEnd(), events).get();
		} catch (NoSuchElementException ex) {
			System.err.println("Error: no final event found.");
			return false;
		}

		while (mi > 0 && !f.equals(lastEvent)) {
			if (EventUtils.getNextEvent(f, events).isPresent()) {
				g = EventUtils.getNextEvent(f, events).get();
			} else {
				g = lastEvent;
			}

			// Do the capacity test
			int capacityAte = f.getResourceCapacity(t.getResourceID()) + 1;
			if (capacityAte <= maxResourceCapacity && checkConstraints(t, e.getTime(), s)) {
				mi = Math.max(0, mi - g.getTime() + f.getTime());
				f = g;
			} else {
				// start event e is NOT FEASIBLE
				mi = t.getProcessingTime();
				e = g;
				f = g;
			}
		}

		if (!checkConstraints(t, e.getTime(), s)) {
			events.forEach(ev -> ev.removePlan(t.getPlanID()));
			return false;
		}

		// Add to schedule
		s.addTask(e.getTime(), t);

		// Add/Update event
		e.addToS(t);
		if (e.getTime() + t.getProcessingTime() == f.getTime()) {
			f.addToC(t);
		} else if (e.getTime() + t.getProcessingTime() > f.getTime()) {
			Event newEvent = Event.get(e.getTime() + t.getProcessingTime(), s.resources());
			newEvent.setResourceCapacities(f.resourceCapacity());
			f.addToC(t);
			events.add(newEvent);
			f = newEvent;
		} else {
			Event predf = EventUtils.getPreviousEvent(f, events).get();
			Event newEvent = Event.get(e.getTime() + t.getProcessingTime(), s.resources());
			newEvent.setResourceCapacities(predf.resourceCapacity());
			newEvent.addToC(t);
			events.add(newEvent);
			f = newEvent;
		}

		// update the resource usage
		Event predf = EventUtils.getPreviousEvent(f, events).get();
		for (Event ev : events) {
			if (ev.getTime() >= e.getTime() && ev.getTime() <= predf.getTime()) {
				ev.increaseResourceUsage(t.getResourceID());
			}
		}
		return true;
	}

	/**
	 * Calculate the initial starting time sk for a task t
	 * 
	 * @param Ws
	 * @param events
	 * @param t
	 * @return
	 */
	private static int getInitialStartingTime(int Ws, final NavigableSet<Event> events, Task t) {

		int maxTime = t.getReleaseTime();
		if (t.getID() == 1 && t.getPlanID() == 3) {
			System.err.println("");
		}

		for (Event event : events) {
			// search for the latest event that contains a predecessor of t
			Optional<Task> pr = event.taskTerminatingHere().stream()
					.filter(x -> x.getPlanID() == t.getPlanID() && t.getPredecessors().contains(x.getID())).findFirst();

			if (pr.isPresent()) {
				if (event.getTime() > maxTime) {
					maxTime = event.getTime();
				}
			}
		}

		return Math.max(maxTime, Ws);
	}

	/**
	 * For the given set of plans, return the number of distinct resource
	 * employed by the set of plans
	 * 
	 * @param plans
	 * @return
	 */
	private static int getNumberOfResources(final List<Plan> plans) {
		Set<Integer> res = new HashSet<>();
		for (Plan plan : plans) {
			res.addAll(plan.getTasks().stream().map(x -> x.getResourceID()).distinct().collect(Collectors.toList()));
		}
		return res.size();
	}

	/**
	 * Calculate the topological sorting for the input set of plans. This ensure
	 * that the precedences between the plans are respected. Also, the plans
	 * with the same priority are then sorted according to user-defined criteria
	 * 
	 * @param plans
	 * @return
	 */
	private static List<ExecutableNode> sortPlans(final List<ExecutableNode> plans) {
		List<ExecutableNode> sorted = new ArrayList<>(plans);

		Stack<ImmutablePair<Integer, Integer>> orderScore = TopologicalSorting.calculateTopologicalOrderScores(sorted);

		ExecutableNode source = plans.stream().min(new Comparator<ExecutableNode>() {
			@Override
			public int compare(ExecutableNode o1, ExecutableNode o2) {
				return Integer.compare(o1.getID(), o2.getID());
			}
		}).get();

		return TopologicalSorting.bellmanFord(source, sorted, orderScore);
	}

	private static void sortByPriorities(List<Plan> plans) {

		// TODO la modifica effettivamente?
		plans.stream().sorted(new Comparator<Plan>() {
			public int compare(Plan o1, Plan o2) {
				return -Integer.compare(o1.getPriority(), o2.getPriority());
			};
		});
	}

	/**
	 * ALGORITHM 3
	 * 
	 * @param t
	 * @param s
	 */
	private static boolean checkConstraints(Task t, int startingTime, Schedule s) {
		// Check for the starting time to be inside the allowed boundaries
		try {
			// Checks for the starting time to be inside [rk,dk]
			Utils.requireValidBounds(startingTime, t.getReleaseTime(), t.getDueDate(),
					"for " + t.toString() + ": starting time " + startingTime + " not in [rk=" + t.getReleaseTime()
							+ ",dk=" + t.getDueDate() + "]");

			// Checks for the starting time to be inside the temporal window
			// [Ws,We]
			Utils.requireValidBounds(startingTime, s.getWStart(), s.getWEnd(), "for " + t.toString()
					+ ": starting time " + startingTime + " not in window [" + s.getWStart() + "," + s.getWEnd() + "]");

			// Checks for the accomplishment date to be inside the temporal
			// window [Ws,We]
			Utils.requireValidBounds(startingTime + t.getProcessingTime(), s.getWStart(), s.getWEnd(),
					"for " + t.toString() + ", sk=" + Integer.toString(startingTime) + ",pk="
							+ Integer.toString(t.getProcessingTime()) + ": accomplishment date "
							+ Integer.toString(startingTime + t.getProcessingTime()) + " not in window ["
							+ s.getWStart() + "," + s.getWEnd() + "]");
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Check if the given task's predecessors are already scheduled in the given
	 * solution
	 * 
	 * @param s
	 *            the solution to be checked
	 * @param t
	 *            the task which predecessors are to be checked
	 * @return <b>true</b> if all the predecessors of t are scheduled in s,
	 *         <b>false</b> otherwise
	 */
	private static boolean checkPrecedences(Schedule s, Task t) {
		// From the list of the actually scheduled task, take those who are
		// predecessors of the task t
		List<Integer> scheduledPredecessors = s.taskSchedules().stream()
				.filter(x -> x.getTask().getPlanID() == t.getPlanID()).map(x -> x.getTask().getID())
				.collect(Collectors.toList());

		// Precedeces between tasks of the SAME plan
		for (Integer p : t.getPredecessors()) {
			if (!scheduledPredecessors.contains(p)) {
				return false;
			}
		}
		return true;
	}
}
