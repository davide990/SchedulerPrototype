package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
		// Create a map of array, where the key is the index of a resource, and
		// the value is an array indicating the utilization of the resource
		final int W = wEnd - wStart;
		Map<Integer, Integer[]> resUtilization = new HashMap<>();
		for (int i = 1; i <= numResources; i++) {
			Integer[] ru = new Integer[W + 1];
			Arrays.fill(ru, 0);
			resUtilization.put(i, ru);
		}

		// Initialize the data structures
		Queue<TabuListEntry> tabuList = new LinkedList<>();
		Queue<Plan> plansQueue = new PriorityQueue<>(PLAN_COMPARATOR);
		Queue<Plan> scheduledPlans = new PriorityQueue<>(PLAN_COMPARATOR);
		Queue<Plan> unscheduledPlans = new PriorityQueue<>(PLAN_COMPARATOR);

		// Calculate the inverse priority for each plan
		int maxPriority = Collections.max(plans.stream().map(Plan::getPriority).collect(Collectors.toList()));
		plans.forEach(p -> p.setInversePriority(maxPriority));

		// Calculate the plans score
		calculatePlanScore(plans, criterias);

		// Once calculated the score, each plan is inserted in order to the
		// priority queue
		plans.forEach(p -> plansQueue.add(p));
		plansQueue.forEach(p -> System.out
				.println("plan added: " + Integer.toString(p.getID()) + ", score: " + Float.toString(p.getScore())));

		// MAIN LOOP
		while (!plansQueue.isEmpty() || !tabuList.isEmpty()) {
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
					if (!tryScheduleTask(maxResourceCapacity, workingSolution, e.getTask(), resUtilization)) {
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
			Plan pk = plansQueue.poll();
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
				if (!tryScheduleTask(maxResourceCapacity, workingSolution, t, resUtilization)) {
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

		return lastFeasibleSolution;
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

		int st = t.getReleaseTime();
		if (maxPredecessorAccomplishmentDate.isPresent()) {
			st = maxPredecessorAccomplishmentDate.getAsInt();
		}

		int resourceCapacity = maxResourceCapacity;
		int shifted_st = st;
		Integer[] res = resUtilizationMap.get(t.getResourceID());

		for (int k = st - s.getWStart(); k < res.length - t.getProcessingTime(); k++) {
			int a = res[k]; // resource utilization at task's start
			int b = res[k + t.getProcessingTime()]; // resource utilization at
													// task's end
			int resUse = Math.max(a, b);
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
	 * 
	 * @param plans
	 */
	private static Stack<ImmutablePair<Integer, Integer>> calculateTopologicalOrderScores(List<Plan> plans) {
		// Find the root node, that is, the node which doesn't appair as
		// successor of all the other nodes
		Optional<Plan> root = Optional.empty();
		for (Plan p : plans) {
			if (plans.stream().filter(x -> x.successors().contains(p.getID())).count() == 0) {
				root = Optional.of(p);
				break;
			}
		}

		// If such node is not found, throw an exception
		if (!root.isPresent()) {
			throw new IllegalArgumentException("Wrong plans precedences. No valid root node found.");
		}
		return topologicalSort(plans);
		/*
		 * Stack<ImmutablePair<Integer, Integer>> sortedPlans =
		 * topologicalSort(plans); List<ImmutablePair<Integer, Integer>> scores
		 * = new ArrayList<>(); while (!sortedPlans.empty()) {
		 * scores.add(sortedPlans.pop()); } return scores;
		 */
	}

	/**
	 * Topological sorting of the plans. It returns a stack of pairs
	 * <b>(A,B)</b> where <b>A</b> is the index of a plan, and <b>B</b> is the
	 * index of the frontier the plan A belongs to.
	 * 
	 * @see Cormen Cormen et al.(2001), chapter 22.
	 */
	private static Stack<ImmutablePair<Integer, Integer>> topologicalSort(final List<Plan> plans) {
		Stack<ImmutablePair<Integer, Integer>> stack = new Stack<>();

		// Mark all the vertices as not visited
		Map<Integer, Boolean> visitedPlans = new HashMap<>();
		for (int i = 0; i < plans.size(); i++) {
			visitedPlans.put(plans.get(i).getID(), false);
		}

		// Call the recursive helper function to store Topological Sort starting
		// from all vertices one by one
		int frontierStartIndex = 0;
		for (int i = 0; i < plans.size(); i++) {
			if (visitedPlans.get(plans.get(i).getID()) == false) {
				topologicalSortUtil(plans.get(i), plans, visitedPlans, stack, frontierStartIndex);
			}
		}

		return stack;
	}

	static void topologicalSortUtil(Plan plan, final List<Plan> plans, Map<Integer, Boolean> visited,
			Stack<ImmutablePair<Integer, Integer>> stack, int frontierIndex) {
		// Mark the current node as visited.
		visited.put(plan.getID(), true);

		frontierIndex++;

		for (Integer successor : plan.successors()) {
			if (!visited.get(successor)) {
				Optional<Plan> s = plans.stream().filter(x -> x.getID() == successor).findFirst();
				if (s.isPresent()) {
					topologicalSortUtil(s.get(), plans, visited, stack, frontierIndex);
				}
			}
		}

		System.err.println("[TOPOLOGICAL SORTING] Pushing plan #" + Integer.toString(plan.getID()) + " with j: "
				+ Integer.toString(frontierIndex));

		// Push current vertex to stack which stores result
		stack.push(new ImmutablePair<Integer, Integer>(plan.getID(), frontierIndex));
	}

	/**
	 * Calculate the scores for the given plans, according to the input criteria
	 * 
	 * @param plans
	 * @param criterias
	 * @return
	 */
	private static List<Float> calculatePlanScore(List<Plan> plans, List<Criteria> criterias) {
		Stack<ImmutablePair<Integer, Integer>> orderScore = calculateTopologicalOrderScores(plans);

		List<Float> scores = new ArrayList<>();
		// Iterate each plan
		for (Plan plan : plans) {
			// Initialize the score at zero
			float score = 0;
			for (int i = 0; i < criterias.size(); i++) {
				// Apply the criteria function, using the weight specified, to
				// the current plan, and add the resulting value to the score
				score += criterias.get(i).getCriteriaFunc().apply(plan) * criterias.get(i).getWeight();
			}
			int toAdd = orderScore.stream().filter(x -> x.getLeft() == plan.getID()).mapToInt(x -> x.getRight()).sum();
			score += toAdd;

			// Set the score for the current plan
			plan.setScore(score);
			scores.add(score);
		}

		return scores;

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
