package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

		// Initialize the data structures
		Queue<TabuListEntry> tabuList = new LinkedList<>();
		// Queue<Plan> plansQueue = new PriorityQueue<>(PLAN_COMPARATOR);
		Queue<Plan> scheduledPlans = new PriorityQueue<>(PLAN_COMPARATOR);
		Queue<Plan> unscheduledPlans = new PriorityQueue<>(PLAN_COMPARATOR);

		// Calculate the inverse priority for each plan
		int maxPriority = Collections.max(plans.stream().map(Plan::getPriority).collect(Collectors.toList()));
		plans.forEach(p -> p.setInversePriority(maxPriority));

		// Sort the plans so that the precedences are respected
		plans = sortPlans(plans);

		System.err.println(
				"sorted -> " + plans.stream().map(x -> Integer.toString(x.getID())).collect(Collectors.joining(",")));

		// MAIN LOOP
		while (!plans.isEmpty() || !tabuList.isEmpty()) {
			// STEP 1 ------------------------------------------------------
			if (!tabuList.isEmpty()) {
				// Extract a tabu task from the list
				TabuListEntry e = tabuList.poll();
				if (e.getWaitTurns() > 0) {
					e.setWaitTurns(e.getWaitTurns() - 1);
					tabuList.add(e);
					continue;
				}

				// Here, a tabu list element has expired (turns=0). So, it can
				// be checked again
				System.err.println("Checking " + e);

				// Check the precedence constraint
				if (!checkPrecedences(workingSolution, e.getTask())) {
					if (e.getNumTries() >= MAX_TABU_TRIES) {
						// discard the plan, since the maximum number of tries
						// for this plan has been reached
						List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
								.filter(x -> x.getTask().getPlanID() == e.getPlan().getID())
								.collect(Collectors.toList());
						workingSolution.unSchedule(toRemove);
						// Push pk into the queue of unscheduled plans
						unscheduledPlans.add(e.getPlan());
					} else {
						// Re-insert in tabu list and increase the number of
						// tries done
						e.setWaitTurns(MAX_TABU_TURNS);
						e.increaseNumTries();
						tabuList.add(e);
						System.err.println("Reinserting " + e);
					}
				} else {
					// Precedence constraints are met here. Finally, the task
					// can be scheduled
					if (!scheduleWithEvents(maxResourceCapacity, workingSolution, e.getTask(), events)) {
						// the task is not schedulable. Discard the plan.
						List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
								.filter(x -> x.getTask().getPlanID() == e.getPlan().getID())
								.collect(Collectors.toList());
						workingSolution.unSchedule(toRemove);
						// Push pk into the queue of unscheduled plans
						unscheduledPlans.add(e.getPlan());
					}
				}
			}

			// STEP 2 ------------------------------------------------------
			// Get the highest scored plan from the sorted queue
			Plan pk = plans.get(0);
			plans.remove(0);
			if (pk == null) {
				continue;
			}
			boolean hasTabuTask = false;

			// Loop each task t in the plan pk
			for (Task t : pk.tasks()) {
				// Check precedence constraints
				if (!checkPrecedences(workingSolution, t)) {
					System.err.println("Adding " + t + " to tabu list");
					tabuList.add(new TabuListEntry(t, pk, MAX_TABU_TURNS));
					hasTabuTask = true;
					continue;
				}

				if (!scheduleWithEvents(maxResourceCapacity, workingSolution, t, events)) {
					pk.setSchedulable(false);
					break;
				}
			}

			if (hasTabuTask) {
				// Here the plan can not be checked if it's schedulable or not,
				// since it has one or more task pending in the tabu list. In
				// this case, the algorithm proceeds.
				continue;
			}

			// At this point, each task of pk has been scheduled
			if (pk.isSchedulable()) {
				try {
					lastFeasibleSolution = (Schedule) workingSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				// Push pk into the queue of scheduled plans
				scheduledPlans.add(pk);
			} else {
				// pk is NOT schedulable: take all its tasks and remove them
				// from the solution
				List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
						.filter(x -> x.getTask().getPlanID() == pk.getID()).collect(Collectors.toList());
				workingSolution.unSchedule(toRemove);
				// Push pk into the queue of unscheduled plans
				unscheduledPlans.add(pk);
			}
		}
		events.forEach(x -> System.err.println(x));
		return lastFeasibleSolution;
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
	private static boolean scheduleWithEvents(final int maxResourceCapacity, Schedule s, Task t,
			NavigableSet<Event> events) {
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

		if (t.getPlanID() == 4) {
			System.err.println();
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
		s.add(e.getTime(), t);

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
		if (t.getTaskID() == 1 && t.getPlanID() == 3) {
			System.err.println("");
		}

		for (Event event : events) {
			// search for the latest event that contains a predecessor of t
			Optional<Task> pr = event.taskTerminatingHere().stream()
					.filter(x -> x.getPlanID() == t.getPlanID() && t.getPredecessors().contains(x.getTaskID()))
					.findFirst();

			if (pr.isPresent()) {
				if (event.getTime() > maxTime) {
					maxTime = event.getTime();
				}
			}
		}

		return Math.max(maxTime, Ws);
	}

	/**
	 * ALGORITHM 2
	 * 
	 * @param maxResourceCapacity
	 *            the maximum capacity of the resources
	 * @param s
	 *            the solution where t has to be scheduled
	 * @param t
	 *            the task to be scheduled
	 * @param resUtilizationMap
	 *            the map of resources utilization
	 * @return <b>true</b> if <b>t</b> can be scheduled in <b>s</b>,
	 *         <b>false</b> otherwise
	 */
	@SuppressWarnings("unused")
	private static boolean tryScheduleTask(final int maxResourceCapacity, Schedule s, Task t,
			Map<Integer, Integer[]> resUtilizationMap) {
		// Take the scheduled predecessors of t (within the same plan)
		List<TaskSchedule> f = s.taskSchedules().stream().filter(
				x -> x.getTask().getPlanID() == t.getPlanID() && t.getPredecessors().contains(x.getTask().getTaskID()))
				.collect(Collectors.toList());

		// From the set of scheduled predecessors, take the maximum
		// accomplishment date
		OptionalInt maxPredecessorAccomplishmentDate = f.stream()
				.mapToInt(x -> x.getStartingTime() + x.getTask().getProcessingTime()).max();

		// Calculate the initial starting time
		int st = t.getReleaseTime();
		if (maxPredecessorAccomplishmentDate.isPresent()) {
			st = maxPredecessorAccomplishmentDate.getAsInt();
		}

		int resourceCapacity = maxResourceCapacity;
		int shifted_st = st;
		Integer[] res = resUtilizationMap.get(t.getResourceID());

		// Search a shift value k to add to the starting time
		for (int k = Math.max(st - s.getWStart(), 0); k < res.length - t.getProcessingTime(); k++) {
			// resource utilization at task's start
			int a = res[k];
			// resource utilization at task's end
			int b = res[k + t.getProcessingTime()];
			// Take the maximum between a and b
			int resUse = Math.max(a, b);
			// If the resource utilisation is minimum and the constraints are
			// satisfied, keep k
			if (resUse < resourceCapacity && resUse + 1 <= maxResourceCapacity
					&& checkConstraints(t, k + s.getWStart(), s)) {
				resourceCapacity = resUse;
				shifted_st = k;
			}
		}

		// calculate the final starting time
		st = shifted_st + s.getWStart();

		// If it doesn't satisfy the constraints, return false
		if (!checkConstraints(t, st, s)) {
			return false;
		}

		// If the task can be placed at the found starting time, but there the
		// utilization of the resource if over the maximum allowed, return false
		if (res[st - s.getWStart()] + 1 > maxResourceCapacity
				|| res[st - s.getWStart() + t.getProcessingTime()] + 1 > maxResourceCapacity) {
			return false;
		}

		// All the constraints are met, schedule the task
		s.add(st, t);

		// Update the resource utilization array
		for (int i = st - s.getWStart(); i < st - s.getWStart() + t.getProcessingTime(); i++) {
			res[i]++;
		}
		resUtilizationMap.put(t.getResourceID(), res);

		return true;
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
			res.addAll(plan.tasks().stream().map(x -> x.getResourceID()).distinct().collect(Collectors.toList()));
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
	private static List<Plan> sortPlans(final List<Plan> plans) {
		List<Plan> sorted = new ArrayList<>(plans);

		Stack<ImmutablePair<Integer, Integer>> orderScore = TopologicalSorting.calculateTopologicalOrderScores(sorted);

		Plan source = plans.stream().min(new Comparator<Plan>() {
			@Override
			public int compare(Plan o1, Plan o2) {
				return Integer.compare(o1.getID(), o2.getID());
			}
		}).get();

		return TopologicalSorting.bellmanFord(source, sorted, orderScore);
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
				.filter(x -> x.getTask().getPlanID() == t.getPlanID()).map(x -> x.getTask().getTaskID())
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
