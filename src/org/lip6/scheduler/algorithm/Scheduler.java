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
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.TreeList;
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
	 * A listener for the scheduler. When a feasible solution is found, the listener
	 * is invoked.
	 */
	Optional<SchedulerListener> listener;
	/**
	 * The set of <b>all</b> plans.
	 */
	private Set<Plan> plans;
	/**
	 * The IDs of all the resources employed by the plans.
	 */
	Set<Integer> resourcesIDs;
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
	TreeSet<Event> events;
	/**
	 * The maximum allowed capacity of each resource
	 */
	int maxResourceCapacity;
	int wStart;
	/**
	 * The start time instant of the temporal window
	 */
	int wEnd;

	/**
	 * If true, the optimal We for the scheduling is calculated. This is useful when
	 * the user doesn't know which value of We to choose.
	 */
	private boolean calculateOptimalWe;

	/**
	 * Constructor for the Scheduler class
	 */
	protected Scheduler() {
		listener = Optional.empty();
		plans = new HashSet<>();
		scheduledPlans = new HashSet<>();
		unscheduledPlans = new HashSet<>();
		resourcesIDs = new HashSet<>();
		events = new TreeSet<>(Event.getComparator());
		calculateOptimalWe = false;
	}

	public boolean isCalculateOptimalWe() {
		return calculateOptimalWe;
	}

	public void setCalculateOptimalWe(boolean calculateOptimalWe) {
		this.calculateOptimalWe = calculateOptimalWe;
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

	public void addPlans(Set<Plan> plans) {
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
	 * 2 Return the set of scheduled plans within the temporal window
	 * [W<sub>s</sub>,W<sub>e</sub>]
	 * 
	 * @return
	 */
	public Set<Plan> getScheduledPlans() {
		return Collections.unmodifiableSet(scheduledPlans);
	}

	/**
	 * Return the set of unscheduled plans within the temporal window
	 * [W<sub>s</sub>,W<sub>e</sub>]
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
	 * Get W<sub>s</sub>
	 * 
	 * @return
	 */
	public int getwStart() {
		return wStart;
	}

	/**
	 * Get W<sub>e</sub>
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
	 * <b>ALGORITHM 1</b>
	 *
	 * @return
	 */
	public Schedule buildSchedule() {
		// Create the empty schedules
		Schedule workingSolution = Schedule.get(wStart, wEnd);
		Schedule lastFeasibleSolution = Schedule.get(wStart, wEnd);

		if (plans.isEmpty()) {
			// System.err.println("Warning: No plan to schedule. Return an empty
			// solution.");
			return workingSolution;
		}

		// Create a copy of the set of plans to schedule
		// --
		// TreeList for fast add/remove operations! Why not an HashSet? Because
		// with Set collection the order of elements is not guarantee.
		List<Plan> plansInput = new TreeList<>(plans);

		// Sort the plans according to the precedences (if any), and also
		// according to their priority value
		plansInput = sortPlans(plansInput.stream().map(x -> (ExecutableNode) x).collect(Collectors.toList()));

		// Create a map containing, for each priority as key value, a list of
		// plans having each one the priority value as key. This is used later
		// to schedule plans that have the same priority value.
		Map<Integer, Integer> prioritiesCountMap = new HashMap<>();

		// Why a TreeList here? Because it provides better performance in
		// add/remove operations compared to ArrayList. Add/remove operations
		// takes
		// O(log n) time with a TreeList.
		Map<Integer, TreeList<Plan>> plansWithSamePriority = new HashMap<>();
		plansInput.forEach(x -> {
			if (!prioritiesCountMap.containsKey(x.getPriority())) {
				prioritiesCountMap.put(x.getPriority(), 1);
			} else {
				int newVal = prioritiesCountMap.get(x.getPriority()) + 1;
				prioritiesCountMap.put(x.getPriority(), newVal);

				if (newVal > 1) {
					if (!plansWithSamePriority.containsKey(x.getPriority())) {
						plansWithSamePriority.put(x.getPriority(), new TreeList<>());
					}
					plansWithSamePriority.get(x.getPriority()).add(x);
				}
			}
		});

		// Main loop. Iterate until there is some plan left to schedule
		while (!plansInput.isEmpty()) {
			// Get the next plan to sort
			Plan pk = plansInput.get(0);
			plansInput.remove(0);
			if (pk == null) {
				continue;
			}

			// System.err.println("Scheduling plan #" +
			// Integer.toString(pk.getID()));

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
				toSchedule.addAll(plansWithSamePriority.get(pk.getPriority()));
				plansInput.removeAll(toSchedule);

				// Schedule all the plans with the same priority
				List<Plan> unscheduled = schedulePlanSet(toSchedule, workingSolution, events, maxResourceCapacity);

				// Remove the unscheduled plans from the working solution, so
				// that is contains only the successfully scheduled plans
				for (Plan p : unscheduled) {
					List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
							.filter(x -> ((Task) x.getTask()).getPlanID() == p.getID()).collect(Collectors.toList());
					workingSolution.unSchedule(toRemove);
				}

				unscheduled.forEach(x -> prioritiesCountMap.remove(x.getPriority()));
				unscheduledPlans.addAll(unscheduled);
				plansWithSamePriority.remove(pk.getPriority());
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
	 * <b>ALGORITHM 3</b> Schedule a set of plans that have the same priority value.
	 * <br/>
	 * The plans are scheduled in order to minimize the idle time. An idle time
	 * occurs when between the accomplishment date of a task and the starting time
	 * of its successor, a task can be placed.
	 * 
	 * @param plans               the set of plans to schedule
	 * @param workingSolution     the working solution where to schedule the plans
	 * @param events              the set of events used to schedule the plans
	 * @param maxResourceCapacity the maximum allowed resource capacity (for each
	 *                            resource)
	 * @return the list of <b>unscheduled</b> plans
	 */
	public List<Plan> schedulePlanSet(final List<Plan> plans, Schedule workingSolution, TreeSet<Event> events,
			int maxResourceCapacity) {
		// The list of unscheduled plans.
		List<Plan> unscheduled = new ArrayList<>();
		// At each iteration of the algorithm, this var contains the value of
		// the plans which minimize the idle time
		Optional<Plan> bestPlan = Optional.empty();
		Optional<Plan> toDelete = Optional.empty();

		// Create a list that contains only the plans the itself are schedulable
		List<Plan> plansList = new ArrayList<>(plans);

		// Iterate until there is some plan left to schedule
		while (!plansList.isEmpty()) {
			int bestIdleTime = Integer.MAX_VALUE;
			bestPlan = Optional.empty();

			// Iterate each plan
			for (Plan p : plansList) {
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

				// If p has been scheduled
				if (scheduled) {
					// For each event e that contains a task of p in S(e),
					// calculate the difference t(e) - t(pred_e) where pred_e is
					// the predecessor of e
					int idleTime = 0;
					for (Event e : E) {
						Optional<Task> t = e.taskStartingHere().stream().filter(x -> x.getPlanID() == p.getID())
								.findFirst();
						Optional<Event> pred_e = EventUtils.getPreviousEvent(e, E, false);
						if (t.isPresent() && pred_e.isPresent()) {
							// Update the idle time value
							idleTime += e.getTime() - pred_e.get().getTime();
						}
					}

					// Update the best idle time value, that is, the minimum
					// value.
					if (idleTime < bestIdleTime) {
						bestPlan = Optional.of(p);
						bestIdleTime = idleTime;
						// System.err.println("Best idle time: " + bestIdleTime
						// + " for plan #" + p.getID());
					}
				} else {
					toDelete = Optional.of(p);
				}

			}

			// Schedule the plan with the minimum idle time
			if (bestPlan.isPresent()) {
				schedulePlan(bestPlan.get(), workingSolution, events, maxResourceCapacity);
				plansList.remove(bestPlan.get());
			} else {
				// Otherwise, just delete it from the set of plans

				plansList.remove(toDelete.get());
				unscheduled.add(toDelete.get());
			}
		}

		return unscheduled;
	}

	/**
	 * <b>ALGORITHM 2</b> Schedule the plan given as input into the
	 * 
	 * @param pk
	 * @param workingSolution
	 * @param lastFeasibleSolution
	 * @param events
	 * @param maxResourceCapacity
	 */
	private boolean schedulePlan(Plan pk, Schedule workingSolution, TreeSet<Event> events,
			final int maxResourceCapacity) {

		if (pk.hasSyncTask()) {
			if (!scheduleSyncTasks(maxResourceCapacity, workingSolution, pk.getSyncTasks(), events)) {
				pk.setSchedulable(false);
			}
		}

		// Loop each task t within the plan pk
		if (pk.isSchedulable()) {
			List<Task> remainingTasks = pk.getTasks().stream().filter(x -> !pk.getSyncTasks().contains(x))
					.collect(Collectors.toList());

			for (Task t : remainingTasks) {
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
	 * 
	 * @param maxResourceCapacity
	 * @param s
	 * @param t
	 * @param events
	 * @return
	 */
	private boolean scheduleSyncTasks(final int maxResourceCapacity, Schedule s, List<Task> t,
			NavigableSet<Event> events) {

		// Take the initial starting time as the maximum starting time available
		// for each task
		int sk = t.stream().mapToInt(x -> getInitialStartingTime(s.getWStart(), events, x)).max().getAsInt();
		Event e = getPreviousEvent(sk, events);
		if (!events.contains(e)) {
			events.add(e);
		}

		Event f = e;
		Event g = f;
		final Event lastEvent;
		try {
			lastEvent = EventUtils.getLastEvent(s.getWEnd(), events).get();
		} catch (NoSuchElementException ex) {
			throw new NoSuchElementException("No event for We found.");
		}

		int placedTasks = 0;
		// mi is used to search the event e such that between e and next_e all
		// the tasks can be scheduled. For this reason, its initial value is set
		// as the maximum processing time of the tasks.
		int mi = t.stream().mapToInt(x -> x.getProcessingTime()).max().getAsInt();
		while (mi > 0 && !f.equals(lastEvent)) {

			// For each tested event e, keep the number of schedulable tasks in
			// e
			placedTasks = 0;
			if (EventUtils.getNextEvent(f, events).isPresent()) {
				g = EventUtils.getNextEvent(f, events).get();
			} else {
				break;
			}

			// Do the capacity test *FOR EACH TASK*
			for (Task task : t) {
				int capacityAte = f.getResourceCapacity(task.getResourceID()) + task.getResourceUsage();
				if (capacityAte <= maxResourceCapacity
						&& checkConstraints(task, e.getTime(), s.getWStart(), s.getWEnd())) {
					placedTasks++;
				}
			}

			// Update the mi value
			mi = Math.max(0, mi - g.getTime() + f.getTime());

			if (placedTasks < t.size()) {
				// start event e is NOT FEASIBLE, since not all the tasks can be
				// scheduled in e
				e = g;
				f = g;
			} else {
				f = g;
			}
		}

		// The search for an event is terminated here, check if the last event
		// found is feasible
		final int te = e.getTime();
		Long feasibleTaskCount = t.stream().map(x -> checkConstraints(x, te, s.getWStart(), s.getWEnd())).filter(x -> x)
				.count();

		if (feasibleTaskCount < t.size()) {
			t.forEach(x -> events.forEach(ev -> ev.removePlan(x.getPlanID())));
			return false;
		}

		// Here the event is feasible, proceed to schedule the tasks and update
		// the events.

		// Add to schedule
		t.forEach(task -> s.addTask(te, task));

		// Add/Update event
		for (Task task : t) {
			e.addToS(task);
			if (e.getTime() + task.getProcessingTime() == f.getTime()) {
				f.addToC(task);
			} else if (e.getTime() + task.getProcessingTime() > f.getTime()) {
				Event newEvent = Event.get(e.getTime() + task.getProcessingTime(), resourcesIDs);
				newEvent.setResourceCapacities(f.resourceCapacity());
				f.addToC(task);
				events.add(newEvent);
				f = newEvent;
			} else {
				Event predf = EventUtils.getPreviousEvent(f, events).get();
				Event newEvent = Event.get(e.getTime() + task.getProcessingTime(), resourcesIDs);
				newEvent.setResourceCapacities(predf.resourceCapacity());
				newEvent.addToC(task);
				events.add(newEvent);
				f = newEvent;
			}

			// update the resource usage
			Event predf = EventUtils.getPreviousEvent(f, events).get();
			for (Event ev : events) {
				if (ev.getTime() >= e.getTime() && ev.getTime() <= predf.getTime()) {
					ev.increaseResourceUsage(task.getResourceID());
				}
			}
		}
		return true;
	}

	/**
	 * <b>ALGORITHM 4</b> Schedule the task t
	 * 
	 * @param maxResourceCapacity
	 * @param s
	 * @param t
	 * @param events
	 * @return
	 */
	private boolean scheduleTask(final int maxResourceCapacity, Schedule s, Task t, NavigableSet<Event> events) {
		int sk = getInitialStartingTime(s.getWStart(), events, t);
		Event e = getPreviousEvent(sk, events);
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
			// System.err.println("Error: no final event found.");
			return false;
		}

		while (mi > 0 && !f.equals(lastEvent)) {
			if (EventUtils.getNextEvent(f, events).isPresent()) {
				g = EventUtils.getNextEvent(f, events).get();
			} else {
				g = lastEvent;
			}

			// Do the capacity test
			// int capacityAte = f.getResourceCapacity(t.getResourceID()) + 1;
			int capacityAte = f.getResourceCapacity(t.getResourceID()) + t.getResourceUsage();
			if (capacityAte <= maxResourceCapacity && checkConstraints(t, e.getTime(), s.getWStart(), s.getWEnd())) {
				mi = Math.max(0, mi - g.getTime() + f.getTime());
				f = g;
			} else {
				// start event e is NOT FEASIBLE
				mi = t.getProcessingTime();
				e = g;
				f = g;
			}
		}

		if (!checkConstraints(t, e.getTime(), s.getWStart(), s.getWEnd())) {
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

	/**
	 * 
	 * @return
	 */
	public Event getEndEvent() {
		return events.stream().max(Event.getComparator()).get();
	}

	/**
	 * Get the latest event <i>e</i> that precedes <i>s<sub>k</sub></i> and contains
	 * in <i>C(e)</i> a task scheduled in the same resource as the task <i>t</i>
	 * given in input
	 * 
	 * @param t
	 * @param sk
	 * @param numResources
	 * @param events
	 * @return
	 */
	private Event getPreviousEvent(int sk, final NavigableSet<Event> events) {
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

	/**
	 * Calculate the initial starting time s<sub>k</sub> for a task t. It is the
	 * maximum between W<sub>s</sub>, r<sub>k</sub> and the latest completion time
	 * of the predecessors of t
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

	private static Comparator<Plan> PLAN_PRIORITY_COMPARATOR = new Comparator<Plan>() {
		@Override
		public int compare(Plan o1, Plan o2) {
			return -Integer.compare(o1.getPriority(), o2.getPriority());
		}
	};

	/**
	 * <b>ALGORITHM 5</b> Sort plans according to their topological order and then
	 * according to their priority value
	 * 
	 * @param plans the set of plans to sort.
	 * @return the sorted set of plans
	 */
	private List<Plan> sortPlans(final List<ExecutableNode> plans) {
		List<Plan> sortedPlanList = new TreeList<>();
		// Sort topologically the nodes. Each pair is: (left: plan ID, right:
		// frontier which the plan belongs to into the precedences graph)
		Stack<ImmutablePair<Integer, Integer>> topologicallySortedPlans = TopologicalSorting
				.topologicalSort(new ArrayList<>(plans));

		// Assemble the plans in a map where the key is the value of a frontier
		// in the precedence graph,
		// and the value is a list of plans which belongs to that frontier. The
		// plans in each list keeps the topological sort
		Map<Integer, List<Plan>> sortedByFrontiers = new HashMap<>();
		while (!topologicallySortedPlans.isEmpty()) {
			ImmutablePair<Integer, Integer> top = topologicallySortedPlans.pop();
			if (!sortedByFrontiers.containsKey(top.right)) {
				sortedByFrontiers.put(top.right, new ArrayList<Plan>());
			}
			sortedByFrontiers.get(top.right)
					.add((Plan) (plans.stream().filter(x -> x.getID() == top.left).findFirst().get()));
		}

		// If there is a frontier which has more than one plan, the order in
		// which they are scheduled doesn't matter. In this case, sort them by
		// taking into account the priority value of each plan
		for (Integer frontier : sortedByFrontiers.keySet()) {
			sortedByFrontiers.get(frontier).sort(PLAN_PRIORITY_COMPARATOR);
			sortedPlanList.addAll(sortedByFrontiers.get(frontier));
		}

		return sortedPlanList;
	}

	/**
	 * <b>ALGORITHM 5</b>
	 * 
	 * @param t
	 * @param s
	 */
	private boolean checkConstraints(final Task t, int startingTime, int Ws, int We) {
		// Check for the starting time to be inside the allowed boundaries
		try {
			// Checks for the starting time to be inside [rk,dk]
			Utils.requireValidBounds(startingTime, t.getReleaseTime(), t.getDueDate(),
					"for " + t.toString() + ": starting time " + startingTime + " not in [rk=" + t.getReleaseTime()
							+ ",dk=" + t.getDueDate() + "]");

			// Checks for the starting time to be inside the temporal window
			// [Ws,We]
			Utils.requireValidBounds(startingTime, Ws, We, "for " + t.toString() + ": starting time " + startingTime
					+ " not in window [" + Ws + "," + We + "]");

			// Checks for the accomplishment date to be inside the temporal
			// window [Ws,We]
			Utils.requireValidBounds(startingTime + t.getProcessingTime(), Ws, We,
					"for " + t.toString() + ", sk=" + Integer.toString(startingTime) + ",pk="
							+ Integer.toString(t.getProcessingTime()) + ": accomplishment date "
							+ Integer.toString(startingTime + t.getProcessingTime()) + " not in window [" + Ws + ","
							+ We + "]");
		} catch (IllegalArgumentException e) {
			// System.err.println(e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Check if the given task's predecessors are already scheduled in the given
	 * solution
	 * 
	 * @param s the solution to be checked
	 * @param t the task which precedences are to be checked
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
