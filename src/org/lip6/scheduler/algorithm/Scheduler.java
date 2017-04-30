package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.Queue;
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
			return -Float.compare(o1.getScore(), o2.getScore());
		}
	};

	public static Schedule scheduleFromFile(int maxResourceCapacity, int numResources, int WStart, int WEnd,
			List<Criteria> criterias, String filename) {
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
		Schedule s = buildSchedule(maxResourceCapacity, numResources, plans, criterias, WStart, WEnd);
		return s;
	}

	public static Schedule schedule(int maxResourceCapacity, int numResources, int WStart, int WEnd,
			List<Criteria> criterias, InputStream is) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(is);
		} catch (IOException | ParseException e) {
			System.err.println("Error while processing input stream.\n" + e);
			return null;
		}
		List<Plan> plans = new ArrayList<>(p.values());
		Schedule s = buildSchedule(maxResourceCapacity, numResources, plans, criterias, WStart, WEnd);
		return s;
	}

	public static Schedule schedule(int maxResourceCapacity, int numResources, int WStart, int WEnd,
			List<Criteria> criterias, List<Plan> plans) {
		Schedule s = buildSchedule(maxResourceCapacity, numResources, plans, criterias, WStart, WEnd);
		return s;
	}

	/**
	 * ALGORITHM 1 TODO MANCA IL NUMERO DI RISORSE
	 * 
	 * @param t
	 * @param s
	 */
	private static Schedule buildSchedule(final int maxResourceCapacity, final int numResources, List<Plan> plans,
			List<Criteria> criterias, int wStart, int wEnd) {
		Schedule workingSolution = Schedule.get(numResources, wStart, wEnd);
		Schedule lastFeasibleSolution = Schedule.get(numResources, wStart, wEnd);
		final int W = wEnd - wStart;

		Map<Integer, Integer[]> resUtilization = new HashMap<>();
		for (int i = 1; i <= numResources; i++) {
			Integer[] ru = new Integer[W + 1];
			Arrays.fill(ru, 0);
			resUtilization.put(i, ru);
		}

		// TABU LIST
		Queue<TabuListEntry> tabuList = new LinkedList<>();

		// The sorted set P of plans.
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
				.println("plan added: " + Integer.toString(p.getID()) + ", score:" + Float.toString(p.getScore())));

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
				// TURNI == 0 QUI
				// verifico la precedenza -> se va bene, ok
				// se non va bene, verifico il numero di tentativi fatti. Se
				// il numero di tries è maggiore di quello stabilito per
				// parametro, allora scarta il piano

				System.err.println("Checking " + e);

				// Check the precedence constraint
				if (!checkPrecedences(workingSolution, e.getTask())) {
					if (e.getNumTries() >= MAX_TABU_TRIES) {
						// discard the plans
						List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
								.filter(x -> x.getTask().getPlanID() == e.getPlan().getID())
								.collect(Collectors.toList());
						workingSolution.unSchedule(toRemove);
						// Push pk into the queue of unscheduled plans
						unscheduledPlans.add(e.getPlan());

						System.err.println("[TABU UPDATE] PLAN " + e.getTask().getPlanID()
								+ " Discarded -> max number of tabu turns for plan " + e.getTask().getTaskID());

					} else {
						// Re-insert in tabu list and increase the number of
						// tries done
						e.setWaitTurns(MAX_TABU_TURNS);
						e.increaseNumTries();
						tabuList.add(e);
						System.err.println("Reinserting " + e);
					}

				} else {
					// Precedence constraints are met here. Finally,
					// schedule
					// the task
					if (!tryScheduleTask(maxResourceCapacity, workingSolution, e.getTask(), resUtilization)) {
						System.err.println("I'M IN TABU FOR " + e.getTask());
						// pk is NOT schedulable: take all its tasks and remove
						// them
						// from the solution
						List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
								.filter(x -> x.getTask().getPlanID() == e.getPlan().getID())
								.collect(Collectors.toList());
						workingSolution.unSchedule(toRemove);
						// Push pk into the queue of unscheduled plans
						unscheduledPlans.add(e.getPlan());

					}
					/*
					 * int st = scheduleTask(e.getTask(), workingSolution); if
					 * (!checkConstraints(e.getTask(), st, workingSolution)) {
					 * System.err.println("Task " + e.getTask() +
					 * " DO NOT MEET CONSTRAINTS.");
					 * e.getPlan().setSchedulable(false); }
					 * 
					 * if (e.getPlan().isSchedulable()) { try {
					 * lastFeasibleSolution = (Schedule)
					 * workingSolution.clone(); } catch
					 * (CloneNotSupportedException ee) { ee.printStackTrace(); }
					 * // push pk into the queue of scheduled plans
					 * scheduledPlans.add(e.getPlan()); } else {
					 * List<TaskSchedule> toRemove =
					 * workingSolution.taskSchedules().stream() .filter(x ->
					 * x.getTask().getPlanID() == e.getPlan().getID())
					 * .collect(Collectors.toList());
					 * workingSolution.unSchedule(toRemove);
					 * unscheduledPlans.add(e.getPlan()); } }
					 */
				}
			}

			// STEP 2 ------------------------------------------------------
			// Get the highest scored plan from the sorted queue
			Plan pk = plansQueue.poll();
			if (pk == null) {
				continue;
			}
			System.out.println("------ >PLAN " + pk.getID());

			boolean hasTabuTask = false;

			// Loop each task t in the plan pk
			for (Task t : pk.tasks()) {
				System.out.println("------ >TASK " + t.getTaskID());
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
		System.err.println("FOR " + t + " st: " + Integer.toString(st));

		Integer[] res = resUtilizationMap.get(t.getResourceID());
		System.err.println("Resource #" + Integer.toString(t.getResourceID()) + "(before): " + Arrays.toString(res));

		if (t.getTaskID() == 2 && t.getPlanID() == 2) {
			System.err.println(".");
		}

		for (int k = st - s.getWStart(); k < res.length - t.getProcessingTime(); k++) {

			int a = res[k];
			int b = res[k + t.getProcessingTime()];

			int resUse = Math.max(a, b);
			System.err
					.println("k: " + Integer.toString(k) + " st: " + Integer.toString(st) + " r[" + Integer.toString(k)
							+ "]: " + Integer.toString(a) + " r[" + Integer.toString(k + t.getProcessingTime()) + "]: "
							+ Integer.toString(b) + " resUse: " + Integer.toString(resUse));

			if (resUse < resourceCapacity && resUse + 1 <= maxResourceCapacity
					&& checkConstraints(t, k + s.getWStart(), s)) {
				resourceCapacity = resUse;
				shifted_st = k;
				System.err.println("[BEST SLOT] For " + t + ", best slot = " + Integer.toString(shifted_st));
			}
		}

		// sposto la starting time
		st = shifted_st + s.getWStart();

		System.out.println("Plan #" + t.getPlanID() + ": " + t + " scheduled at " + Integer.toString(st));

		if (!checkConstraints(t, st, s)) {
			return false;
		}

		// Qui il task può essere schedulato, dunque entra in W/[rk,dk]. E'
		// necessario però verificare l'utilizzo della risosa poichè è possibile
		// che non sia stata trovata un posizione migliore per il task e nella
		// posizione attuale l'utilizzo della risorsa supera il limite imposto
		if (res[st - s.getWStart()] + 1 > maxResourceCapacity
				|| res[st - s.getWStart() + t.getProcessingTime()] + 1 > maxResourceCapacity) {
			return false;
		}

		// Se i vincoli sono soddisfatti, schedula il task
		s.add(st, t);

		for (int i = st - s.getWStart(); i < st - s.getWStart() + t.getProcessingTime(); i++) {
			res[i]++;
		}

		resUtilizationMap.put(t.getResourceID(), res);
		System.err.println("Resource #" + Integer.toString(t.getResourceID()) + "(after):  "
				+ Arrays.toString(resUtilizationMap.get(t.getResourceID())));
		return true;
	}

	/**
	 * Calculate the scores for the given plans, according to the input criteria
	 * 
	 * @param plans
	 * @param criterias
	 * @return
	 */
	private static List<Float> calculatePlanScore(List<Plan> plans, List<Criteria> criterias) {
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
		/*
		 * final List<ImmutablePair<Integer, Integer>> scheduledPlanTaskPairs =
		 * s.taskSchedules().stream() .map(x -> new
		 * ImmutablePair<>(x.getTask().getPlanID(), x.getTask().getTaskID()))
		 * .collect(Collectors.toList());
		 */

		List<Integer> scheduledPredecessors = s.taskSchedules().stream()
				.filter(x -> x.getTask().getPlanID() == t.getPlanID()).map(x -> x.getTask().getTaskID())
				.collect(Collectors.toList());

		// Precedeces between tasks of the SAME plan
		for (Integer p : t.getPredecessors()) {
			if (!scheduledPredecessors.contains(p)) {
				return false;
			}
		}
		// CHECK PRECEDENCES
		/*
		 * for (ImmutablePair<Integer, Integer> pair : t.getPredecessors()) { if
		 * (!scheduledPlanTaskPairs.contains(pair)) { return false; } }
		 */

		return true;
	}

	/**
	 * ALGORITHM 2
	 * 
	 * @param t
	 *            the task to be scheduled
	 * @param s
	 *            the solution where t has to be scheduled
	 */
	private static int scheduleTask(Task t, Schedule s) {
		/*
		 * Optional<Task> tt = s.taskSchedules().stream().map(x ->
		 * x.getTask()).filter(x -> x == t).findFirst(); if (tt.isPresent()) {
		 * return Integer.MAX_VALUE; // task already scheduled }
		 */

		// From the list of the actually scheduled task, take those who are
		// predecessors of the task t
		List<TaskSchedule> f = s.taskSchedules().stream()
				.filter(x -> t.getPredecessors()
						.contains(new ImmutablePair<>(x.getTask().getPlanID(), x.getTask().getTaskID())))
				.collect(Collectors.toList());

		// From the set of scheduled predecessors, take the maximum
		// accomplishment date
		OptionalInt maxPredecessorAccomplishmentDate = f.stream()
				.mapToInt(x -> x.getStartingTime() + x.getTask().getProcessingTime()).max();

		// If there is any predecessor scheduled, take its accomplishment date,
		// otherwise the accomplishment of the last scheduled task for the
		// resource demanded by the task t to be scheduled
		int md = 0;
		/*
		 * if (!maxPredecessorAccomplishmentDate.isPresent()) {
		 * 
		 * // ---Modifica qui --- // Parti da rk e scorri finchè non trovi una
		 * sk che soddisfa i // vincoli md =
		 * s.getAccomplishmentForLastTaskIn(t.getResourceID()); } else { md =
		 * maxPredecessorAccomplishmentDate.getAsInt(); }
		 */

		if (maxPredecessorAccomplishmentDate.isPresent()) {
			md = maxPredecessorAccomplishmentDate.getAsInt();
		}

		// Calculate the starting time for t
		int startingTime = Math.max(md, t.getReleaseTime());

		// Add the task to the schedule
		s.add(startingTime, t);

		// Return the starting time
		return startingTime;
	}

}
