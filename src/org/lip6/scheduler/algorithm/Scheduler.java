package org.lip6.scheduler.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import org.lip6.scheduler.utils.Utils;

/**
 * Scheduler class.
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public class Scheduler {

	/**
	 * A listener for the scheduler. When a feasible solution is found, the
	 * listener is invoked.
	 */
	private Optional<SchedulerListener> listener;
	/**
	 * The set of <b>all</b> plans.
	 */
	private Set<Plan> plans;
	/**
	 * The IDs of all the resources employed by the plans.
	 */
	private Set<Integer> resourcesIDs;
	/**
	 * The set of scheduled plans.
	 */
	private Set<Plan> scheduledPlans;
	/**
	 * The set of unscheduled plans.
	 */
	private Set<Plan> unscheduledPlans;
	/**
	 * The set of events
	 */
	private TreeSet<Event> events;
	/**
	 * The maximum allowed capacity of each resource
	 */
	private int maxResourceCapacity;
	private int wStart;
	/**
	 * The start time instant of the temporal window
	 */
	private int wEnd;
	/**
	 * If true, plans are linked by precedence relations.
	 */
	private boolean hasPrecedences;
	/**
	 * If true, the optimal We for the scheduling is calculated. This is useful
	 * when the user doesn't know which value of We to choose.
	 */
	private boolean calculateOptimalWe;

	/**
	 * Constructor for the Scheduler class
	 */
	private Scheduler() {
		listener = Optional.empty();
		plans = new HashSet<>();
		scheduledPlans = new HashSet<>();
		unscheduledPlans = new HashSet<>();
		resourcesIDs = new HashSet<>();
		events = new TreeSet<>(Event.getComparator());
		hasPrecedences = true;
		calculateOptimalWe = false;
	}

	/**
	 * Static factory method for Scheduler. It returns a new instance of
	 * Scheduler class
	 * 
	 * @param maxResourceCapacity
	 *            the maximum capacity of all the resources
	 * @param plans
	 *            the set of plans to scheduler
	 * @param wStart
	 *            the starting time of the temporal window
	 * @param wEnd
	 *            the final time of the temporal window
	 * @param listener
	 *            a listener that is invoked each time a <b>feasible
	 *            solution</b> is constructed
	 * @return
	 */
	public static Scheduler get(final int maxResourceCapacity, Set<Plan> plans, int wStart, int wEnd,
			SchedulerListener listener) {
		Scheduler scheduler = get(maxResourceCapacity, plans, wStart, wEnd);
		scheduler.listener = Optional.of(listener);
		return scheduler;
	}

	public boolean hasPrecedences() {
		return hasPrecedences;
	}

	public void setHasPrecedences(boolean hasPrecedences) {
		this.hasPrecedences = hasPrecedences;
	}

	public boolean isCalculateOptimalWe() {
		return calculateOptimalWe;
	}

	public void setCalculateOptimalWe(boolean calculateOptimalWe) {
		this.calculateOptimalWe = calculateOptimalWe;
	}

	/**
	 * Static factory method for Scheduler. It returns a new instance of
	 * Scheduler class
	 * 
	 * @param maxResourceCapacity
	 *            the maximum capacity of all the resources
	 * @param plans
	 *            the set of plans to scheduler
	 * @param wStart
	 *            the starting time of the temporal window
	 * @param wEnd
	 *            the final time of the temporal window
	 * @return
	 */
	public static Scheduler get(final int maxResourceCapacity, Set<Plan> plans, int wStart, int wEnd) {
		// Check if the resource capacity value is > 0
		Utils.requireValidBounds(maxResourceCapacity, 1, Integer.MAX_VALUE,
				"Maximum resource capacity must be a positive integer value.");

		// Check if the temporal window is valid
		if (wEnd <= wStart) {
			throw new IllegalArgumentException(
					"Invalid temporal window [" + Integer.toString(wStart) + "," + Integer.toString(wEnd) + "]");
		}

		// Create a new scheduler
		Scheduler scheduler = new Scheduler();
		scheduler.maxResourceCapacity = maxResourceCapacity;
		scheduler.wStart = wStart;
		scheduler.wEnd = wEnd;
		scheduler.plans.addAll(plans);

		// retrieve the IDs of the resources
		for (Plan p : plans) {
			scheduler.resourcesIDs
					.addAll(p.getTasks().stream().map(x -> x.getResourceID()).collect(Collectors.toList()));
		}

		// Create two events for Ws and We
		scheduler.events.add(Event.get(wStart, scheduler.resourcesIDs));
		scheduler.events.add(Event.get(wEnd, scheduler.resourcesIDs));

		return scheduler;
	}

	/**
	 * Clear the result of the current scheduler
	 */
	public void clear() {
		plans.clear();
		resourcesIDs.clear();
		scheduledPlans.clear();
		unscheduledPlans.clear();
		events.clear();
	}

	public void addPlans(List<Plan> plans) {
		this.plans.addAll(plans);
		for (Plan p : plans) {
			resourcesIDs.addAll(p.getTasks().stream().map(x -> x.getResourceID()).collect(Collectors.toList()));
		}
	}

	/**
	 * Return the set of all plans assigned to this scheduler
	 * 
	 * @return
	 */
	public Set<Plan> getPlans() {
		return Collections.unmodifiableSet(plans);
	}

	/**
	 * Return the set of scheduled plans within the temporal window [Ws,We]
	 * 
	 * @return
	 */
	public Set<Plan> getScheduledPlans() {
		return Collections.unmodifiableSet(scheduledPlans);
	}

	/**
	 * Return the set of unscheduled plans within the temporal window [Ws,We]
	 * 
	 * @return
	 */
	public Set<Plan> getUnscheduledPlans() {
		return Collections.unmodifiableSet(unscheduledPlans);
	}

	/**
	 * Get the maximum allowed capacity of all the resources employed
	 * 
	 * @return
	 */
	public int getMaxResourceCapacity() {
		return maxResourceCapacity;
	}

	/**
	 * Get Ws
	 * 
	 * @return
	 */
	public int getwStart() {
		return wStart;
	}

	/**
	 * Get We
	 * 
	 * @return
	 */
	public int getwEnd() {
		return wEnd;
	}

	/**
	 * Set the temporal window this scheduler has to operate.
	 * 
	 * @param wStart
	 * @param wEnd
	 */
	public void setTemporalWindow(int wStart, int wEnd) {
		if (!events.stream().filter(x -> x.getTime() == this.wStart).findFirst().isPresent()) {
			events.add(Event.get(wStart, resourcesIDs));
		}

		if (!events.stream().filter(x -> x.getTime() == this.wEnd).findFirst().isPresent()) {
			events.add(Event.get(wEnd, resourcesIDs));
		}
	}

	/**
	 * ALGORITHM 1
	 *
	 * @return
	 */
	public Schedule buildSchedule() {
		// Create the empty schedules
		Schedule workingSolution = Schedule.get(wStart, wEnd);
		Schedule lastFeasibleSolution = Schedule.get(wStart, wEnd);

		if (plans.isEmpty()) {
			System.err.println("Warning: No plan to schedule. Return an empty solution.");
			return workingSolution;
		}
		// Create a copy of the set of plans to schedule
		List<Plan> plansInput = new ArrayList<>(plans);

		// Sort the plans so that the precedences are respected (if any)
		if (hasPrecedences) {
			plansInput = sortPlansByPrecedence(
					plansInput.stream().map(x -> (ExecutableNode) x).collect(Collectors.toList()));
		} else {
			// No precedences. Sort by priorities (decreasing)
			plansInput.sort(new Comparator<Plan>() {
				@Override
				public int compare(Plan o1, Plan o2) {
					return -Integer.compare(o1.getPriority(), o2.getPriority());
				}
			});
		}

		// Create a map for the priorities.This map will contains the number of
		// plans (as value) that have a specific
		// priority value (ask key).
		Map<Integer, Integer> prioritiesCountMap = new HashMap<>();
		plansInput.forEach(x -> {
			if (!prioritiesCountMap.containsKey(x.getPriority())) {
				prioritiesCountMap.put(x.getPriority(), 1);
			} else {
				prioritiesCountMap.put(x.getPriority(), prioritiesCountMap.get(x.getPriority()) + 1);
			}
		});

		// System.err.println("sorted -> "
		// + plansInput.stream().map(x ->
		// Integer.toString(x.getID())).collect(Collectors.joining(",")));

		// Main loop. Iterate until there is some plan left to schedule
		while (!plansInput.isEmpty()) {
			// Get the plan with the highest priority
			Plan pk = plansInput.get(0);
			plansInput.remove(0);
			if (pk == null) {
				continue;
			}

			// If pk is the only, in the plan set, to have its priority value,
			// then proceed by scheduling it
			if (prioritiesCountMap.get(pk.getPriority()) == 1) {
				// Schedule pk
				boolean scheduled = schedulePlan(pk, workingSolution, events, maxResourceCapacity);
				// If pk has been scheduled
				if (scheduled) {
					// Add to the set of scheduled plans
					scheduledPlans.add(pk);
					try {
						// Update the last feasible solution
						lastFeasibleSolution = (Schedule) workingSolution.clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					// If a listener has been registered, notify the last
					// feasible solution
					if (listener.isPresent()) {
						listener.get().solutionGenerated(lastFeasibleSolution);
					}
				} else {
					// If pk has not been scheduled, add to the set of
					// unscheduled plans
					unscheduledPlans.add(pk);
				}
				// Remove the key/value pair from the map of priorities
				prioritiesCountMap.remove(pk.getPriority());
			} else {
				// Get all the plans that have the same priority as the plan to
				// schedule
				List<Plan> toSchedule = new ArrayList<>();
				toSchedule.add(pk);
				toSchedule.addAll(plansInput.stream().filter(x -> x.getPriority() == pk.getPriority())
						.collect(Collectors.toList()));
				plansInput.removeAll(toSchedule);

				// Schedule all the plans with the same priority
				List<Plan> unscheduled = schedulePlansWithSamePriority(toSchedule, workingSolution, events,
						maxResourceCapacity);

				// Remove the unscheduled plans from the working solution, so
				// that is contains only the successfully scheduled plans
				for (Plan p : unscheduled) {
					List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
							.filter(x -> ((Task) x.getTask()).getPlanID() == p.getID()).collect(Collectors.toList());
					workingSolution.unSchedule(toRemove);
				}

				toSchedule.removeAll(unscheduled);
				unscheduled.forEach(x -> prioritiesCountMap.remove(x.getPriority()));
				unscheduledPlans.addAll(unscheduled);
				try {
					lastFeasibleSolution = (Schedule) workingSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				// If a listener has been registered, notify the last feasible
				// solution
				if (listener.isPresent()) {
					listener.get().solutionGenerated(lastFeasibleSolution);
				}
			}
		}

		// At the end, if user want to "reduce" the temporal window so that it
		// fits perfectly the scheduled events, the last event is set to the
		// last that contains *something*.
		if (calculateOptimalWe) {
			Event last = events.stream().max(Event.getComparator()).get();
			if (last.taskStartingHere().isEmpty() && last.taskTerminatingHere().isEmpty()) {
				events.remove(last);
			}
		}

		events.forEach(x -> System.err.println(x));
		return lastFeasibleSolution;
	}

	/**
	 * ALGORITHM 3 Schedule a set of plans that have the same priority value.
	 * 
	 * @param plans
	 *            the set of plans to schedule
	 * @param workingSolution
	 *            the working solution where to schedule the plans
	 * @param events
	 *            the set of events used to schedule the plans
	 * @param maxResourceCapacity
	 *            the maximum allowed resource capacity (for each resource)
	 * @return the list of <b>unscheduled</b> plans
	 */
	private List<Plan> schedulePlansWithSamePriority(List<Plan> plans, Schedule workingSolution, TreeSet<Event> events,
			int maxResourceCapacity) {
		List<Plan> unscheduled = new ArrayList<>();

		// keep the processing time of each task
		Map<Integer, Integer> processingTimes = new HashMap<>();

		// Create a list that contains only the plans the itself are schedulable
		Queue<Plan> validPlansList = new PriorityQueue<>(new Comparator<Plan>() {
			@Override
			public int compare(Plan o1, Plan o2) {
				return Integer.compare(o1.getID(), o2.getID());
			}
		});

		// Generate "intermediate" solutions by scheduling each single plan into
		// the working solution
		for (Plan p : plans) {
			Schedule S = null;
			TreeSet<Event> E = null;

			try {
				S = (Schedule) workingSolution.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			E = EventUtils.cloneSet(events);
			// Try to schedule the plan p
			boolean scheduled = schedulePlan(p, S, E, maxResourceCapacity);

			if (scheduled) {
				// Keep the plan p, since it generates itself a solution
				validPlansList.add(p);
			}
		}

		// Calculate the processing time for each task. This information is used
		// to calculate the idle time later
		for (Plan p : validPlansList) {
			for (Task t : p.getTasks()) {
				if (processingTimes.containsKey(t.getProcessingTime())) {
					processingTimes.put(t.getProcessingTime(), processingTimes.get(t.getProcessingTime()) + 1);
				} else {
					processingTimes.put(t.getProcessingTime(), 1);
				}
			}
		}

		// Iterate each valid plan
		while (!validPlansList.isEmpty()) {
			Optional<Plan> toInsert = Optional.empty();
			Optional<Plan> toDelete = Optional.empty();
			int bestDeadTime = Integer.MAX_VALUE;

			for (Plan p : validPlansList) {
				Schedule S = null;
				TreeSet<Event> E = null;

				try {
					S = (Schedule) workingSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				E = EventUtils.cloneSet(events);
				// Try to schedule the plan p
				boolean scheduled = schedulePlan(p, S, E, maxResourceCapacity);

				// Calculate the idle time for p
				int d = getIdleTime(p, E, processingTimes);
				System.err.println("idle time for plan #" + Integer.toString(p.getID()) + ": " + d);

				// Update the best idle time
				if (scheduled && d < bestDeadTime) {
					toInsert = Optional.of(p);
					bestDeadTime = d;
					System.err.println("Best idle time: " + bestDeadTime + " for plan #" + p.getID());
				} else {
					toDelete = Optional.of(p);
				}
			}

			// If a better plan has been found, schedule it
			if (toInsert.isPresent()) {
				System.err.println("Schedulign plan #" + toInsert.get().getID());
				schedulePlan(toInsert.get(), workingSolution, events, maxResourceCapacity);
				validPlansList.remove(toInsert.get());

				for (Task t : toInsert.get().getTasks()) {
					processingTimes.replace(t.getProcessingTime(), processingTimes.get(t.getProcessingTime()) - 1);
				}

			} else {
				// Otherwise, just delete it from the set of plans
				validPlansList.remove(toDelete.get());
				unscheduled.add(toDelete.get());
			}
		}

		return unscheduled;
	}

	/**
	 * For a given scheduled plan <b>p</b>, this method measures the idle time
	 * between each task and its predecessor on the same resource. An idle time
	 * occurs when between the accomplishment date of a task and the starting
	 * time of its successor, a task can be placed.
	 * 
	 * @param p
	 *            a scheduled plan
	 * @param events
	 *            the events list containing the tasks from <b>p</b>
	 * @param processingTimes
	 *            A map containing as key a processing time, and as value the
	 *            number of tasks, from a plan set, that have the processing
	 *            time specified as key
	 * @return the number of tasks that can be placed in the times between each
	 *         task of <b>p</b> and their predecessors.
	 */
	private int getIdleTime(final Plan p, final TreeSet<Event> events, Map<Integer, Integer> processingTimes) {
		return p.getTasks().stream().mapToInt(t -> getTaskIdleTime(t, events, processingTimes)).sum();
	}

	/**
	 * Calculate the idle time between the task given as parameter and its
	 * immediate predecessor allocated in the same resource.
	 * 
	 * @param t
	 * @param events
	 * @param taskProcessingTimes
	 * @return
	 */
	private Integer getTaskIdleTime(final Task t, final TreeSet<Event> events,
			Map<Integer, Integer> taskProcessingTimes) {
		Optional<Event> e = events.stream().filter(x -> x.taskStartingHere().contains(t)).findFirst();

		if (!e.isPresent()) {
			return 0;
		}
		// Find the previous event, previous to the event in which t starts,
		// that has a task allocated to the same resource of t, OR ELSE get
		// the previous event, that is the event corresponding to Ws time.
		Event previousEvent = events.stream()
				.filter(x -> x.getTime() < e.get().getTime() && x.getResourceCapacity(t.getResourceID()) > 0)
				.max(Event.getComparator()).orElseGet(() -> EventUtils.getPreviousEvent(e.get(), events, true).get());

		// calculate the size of the hole generated by task t
		int td = e.get().getTime() - previousEvent.getTime();

		// calculate the number of tasks that can be placed within the hole
		// generated by scheduling t into the actual solution
		int c = taskProcessingTimes.keySet().stream().filter(x -> x <= td).collect(Collectors.toList()).size();

		return c;
	}

	/**
	 * ALGORITHM 2 Schedule the plan given as input into the
	 * 
	 * @param pk
	 * @param workingSolution
	 * @param lastFeasibleSolution
	 * @param events
	 * @param maxResourceCapacity
	 */
	private boolean schedulePlan(Plan pk, Schedule workingSolution, TreeSet<Event> events, int maxResourceCapacity) {

		// Loop each task t in the plan pk
		for (Task t : pk.getTasks()) {
			// Check precedence constraints
			if (!checkPrecedences(workingSolution, t)) {
				pk.setSchedulable(false);
				break;
			}

			if (!scheduleTask(maxResourceCapacity, workingSolution, t, events)) {
				pk.setSchedulable(false);
				break;
			}
		}
		// At this point, each task of pk has been scheduled
		if (pk.isSchedulable()) {
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
	 * ALGORITHM 4 - Schedule the task t
	 * 
	 * @param maxResourceCapacity
	 * @param s
	 * @param t
	 * @param events
	 * @return
	 */
	private boolean scheduleTask(final int maxResourceCapacity, Schedule s, Task t, NavigableSet<Event> events) {
		int sk = getInitialStartingTime(s.getWStart(), events, t);

		if (t.getID() == 2 && t.getPlanID() == 6) {
			System.err.println();
		}

		// Start event!
		// Event e = EventUtils.getPreviousEvent(sk, numResources, true,
		// events).get();

		Event e = getPreviousEvent(t, sk, events);

		if (!events.contains(e)) {
			events.add(e);
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
			Event newEvent = Event.get(e.getTime() + t.getProcessingTime(), resourcesIDs);
			newEvent.setResourceCapacities(f.resourceCapacity());
			f.addToC(t);
			events.add(newEvent);
			f = newEvent;
		} else {
			Event predf = EventUtils.getPreviousEvent(f, events).get();
			Event newEvent = Event.get(e.getTime() + t.getProcessingTime(), resourcesIDs);
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

	public Event getEndEvent() {
		return events.stream().max(Event.getComparator()).get();
	}

	/**
	 * Get the latest event <i>e</i> that precedes <i>sk</i> and contains in
	 * <i>C(e)</i> a task scheduled in the same resource as the task <i>t</i>
	 * given in input
	 * 
	 * @param t
	 * @param sk
	 * @param numResources
	 * @param events
	 * @return
	 */
	private Event getPreviousEvent(final Task t, int sk, final NavigableSet<Event> events) {
		Optional<Event> ev = events.stream().filter(e -> e.getTime() == sk).findFirst();

		if (ev.isPresent()) {
			return ev.get();
		} else {
			Event event = Event.get(sk, resourcesIDs);

			Event predf = EventUtils.getPreviousEvent(event, events).get();
			event.setResourceCapacities(predf.resourceCapacity());

			return event;
		}
	}

	@Deprecated  
	@SuppressWarnings("unused")
	private Event getPreviousEventOld(final Task t, int sk, final NavigableSet<Event> events) {
		// Get the latest predecessor event
		Optional<Event> taskPredecessorEvent = events.stream()
				.filter(e -> e.taskTerminatingHere().stream().filter(c -> c.getPlanID() == t.getPlanID()).count() > 0)
				.max(Event.getComparator());

		// Get the latest event that contains a task scheduled on the same
		// resource as the task t to be scheduled, and its time instant is less
		// than sk
		Optional<Event> pred = events.stream().filter(x -> x.taskTerminatingHere().stream()
				.filter(y -> y.getResourceID() == t.getResourceID()).findFirst().isPresent() && x.getTime() < sk)
				.max(Event.getComparator());

		// If such event is not found, get the previous event to sk regardless
		// of the resources
		Event e = EventUtils.getPreviousEvent(sk, resourcesIDs, true, events).get();

		if (taskPredecessorEvent.isPresent()) {
			return taskPredecessorEvent.get();
		}
		return pred.orElse(e);

	}

	/**
	 * Calculate the initial starting time sk for a task t. It is the maximum
	 * between Ws, rk and the latest completion time of the predecessors of t
	 * 
	 * @param Ws
	 * @param events
	 * @param t
	 * @return
	 */
	private int getInitialStartingTime(int Ws, final NavigableSet<Event> events, Task t) {
		int maxTime = t.getReleaseTime();
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
	 * Calculate the topological sorting for the input set of plans or tasks.
	 * This ensure that the precedences constraints are respected. Also, the
	 * plans with the same priority are then sorted according to user-defined
	 * criteria
	 * 
	 * @param nodes
	 * @return
	 */
	private List<Plan> sortPlansByPrecedence(final List<ExecutableNode> nodes) {
		List<ExecutableNode> sorted = new ArrayList<>(nodes);

		// Sort topologically the nodes (left -> plan, right -> frontier)
		Stack<ImmutablePair<Integer, Integer>> orderScore = TopologicalSorting.getPlansFrontiers(sorted);
		List<Plan> output = new ArrayList<>();

		// Assemble the sorted list of plans
		while (!orderScore.isEmpty()) {
			int left = orderScore.pop().left;
			output.add(nodes.stream().map(x -> (Plan) x).filter(x -> x.getID() == left).findFirst().get());
		}
		return output;
	}

	/**
	 * ALGORITHM 5
	 * 
	 * @param t
	 * @param s
	 */
	private boolean checkConstraints(final Task t, int startingTime, final Schedule s) {
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
	private boolean checkPrecedences(final Schedule s, final Task t) {
		// From the list of the actually scheduled task, take those who are
		// predecessors of the task t
		List<Integer> scheduledPredecessors = s.taskSchedules().stream()
				.filter(x -> x.getTask().getPlanID() == t.getPlanID()).map(x -> x.getTask().getID())
				.collect(Collectors.toList());

		// Precedences between tasks of the SAME plan
		for (Integer p : t.getPredecessors()) {
			if (!scheduledPredecessors.contains(p)) {
				return false;
			}
		}
		return true;
	}
}
