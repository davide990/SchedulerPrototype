package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskSchedule;
import org.lip6.scheduler.utils.CSVParser;
import org.lip6.scheduler.utils.Utils;

public class Scheduler {

	private static Comparator<Plan> planComparator = new Comparator<Plan>() {
		@Override
		public int compare(Plan o1, Plan o2) {
			return -Float.compare(o1.getScore(), o2.getScore());
		}
	};

	public static Schedule scheduleFromFile(int WStart, int WEnd, List<Criteria> criterias, String filename) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(filename);
		} catch (IOException e) {
			System.err.println("Error while loading file: \"" + filename + "\"");
			return null;
		}
		List<Plan> plans = new ArrayList<>(p.values());
		Schedule s = buildSchedule(plans, criterias, WStart, WEnd);
		return s;
	}

	public static Schedule schedule(int WStart, int WEnd, List<Criteria> criterias, List<Plan> plans) {
		Schedule s = buildSchedule(plans, criterias, WStart, WEnd);
		return s;
	}

	/**
	 * ALGORITHM 1
	 * 
	 * @param t
	 * @param s
	 */

	private static Schedule buildSchedule(List<Plan> plans, List<Criteria> criterias, int wStart, int wEnd) {
		Schedule workingSolution = Schedule.get(1, wStart, wEnd);
		Schedule lastFeasibleSolution = Schedule.get(1, wStart, wEnd);

		// The sorted set P of plans.
		Queue<Plan> plansQueue = new PriorityQueue<>(planComparator);
		Queue<Plan> scheduledPlans = new PriorityQueue<>(planComparator);
		Queue<Plan> unscheduledPlans = new PriorityQueue<>(planComparator);

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
		while (!plansQueue.isEmpty()) {
			Plan pk = plansQueue.poll();
			System.out.println("PLAN " + pk.getID());

			for (Task t : pk.tasks()) {

				int st = scheduleTask(t, workingSolution);
				System.out.println("--> Task " + t.getResourceID() + " scheduled at " + Integer.toString(st));
				if (!checkConstraints(t, st, workingSolution)) {
					pk.setSchedulable(false);
				}
			}

			// At this point, each task has been scheduled
			if (pk.isSchedulable()) {
				try {
					lastFeasibleSolution = (Schedule) workingSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}

				// push pk into the queue of scheduled plans
				scheduledPlans.add(pk);
			} else {
				try {
					workingSolution = (Schedule) lastFeasibleSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				unscheduledPlans.add(pk);
			}

		}

		return lastFeasibleSolution;
	}

	private static List<Float> calculatePlanScore(List<Plan> plans, List<Criteria> criterias) {
		List<Float> scores = new ArrayList<>();
		for (Plan plan : plans) {
			float score = 0;
			for (int i = 0; i < criterias.size(); i++) {
				score += criterias.get(i).getCriteriaFunc().apply(plan) * criterias.get(i).getWeight();
			}
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

		// STEP 1: controlla che starting time sia dentro il bound giusto (cioè
		// dentro
		// [rk,dk]e che a sua volta s e s+p siano dentro W
		// int startingTime = Math.max(t.getReleaseTime(),
		// s.getDueDateForLastTaskIn(t.getResourceID()) + 1);
		try {
			// Utils.requireValidBounds(startingTime, t.getReleaseTime(),
			// t.getDueDate(),
			// "for " + t.toString() + ": starting time " + startingTime + " not
			// in [rk=" + t.getReleaseTime()
			// + ",dk=" + t.getDueDate() + "]");

			// Checks for the starting time to be inside the temporal window
			// [Ws,We]
			Utils.requireValidBounds(startingTime, s.getWStart(), s.getWEnd(), "for " + t.toString()
					+ ": starting time " + startingTime + " not in window [" + s.getWStart() + "," + s.getWEnd() + "]");
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return false;
		}

		// STEP 2: controlla che tutti i predecessori siano in s
		// Prendo gli altri task dello stesso piano di t, che sono stati già
		// schedulati
		List<Task> scheduledTasksInSamePlan = s.taskSchedules().stream().map(TaskSchedule::getTask)
				.filter(sc -> sc.getPlanID() == t.getPlanID()).collect(Collectors.toList());
		List<Integer> scheduledTaskID = scheduledTasksInSamePlan.stream().map(Task::getTaskID)
				.collect(Collectors.toList());

		for (Integer p : t.getPredecessors()) {
			if (!scheduledTaskID.contains(p))
				System.err.println("Error: for task " + t.getTaskID() + "[plan " + t.getPlanID()
						+ "] schedule doesn't contains required predecessor " + p + " within the same plan.");
			return false;
		}

		return true;
	}

	/**
	 * ALGORITHM 2
	 * 
	 * @param t
	 * @param s
	 */
	private static int scheduleTask(Task t, Schedule s) {
		// calculate the starting time for the task t
		int lastTaskScheduledDueDate = s.getDueDateForLastTaskIn(t.getResourceID());
		int rk = t.getReleaseTime();

		int startingTime = Math.max(lastTaskScheduledDueDate, rk);

		// Add the task to the schedule
		s.add(startingTime, t);

		return startingTime;
	}

}
