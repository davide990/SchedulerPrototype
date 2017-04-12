package org.lip6.scheduler.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.TaskSchedule;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.utils.Utils;

public class Scheduler {

	/**
	 * ALGORITHM 1
	 * 
	 * @param t
	 * @param s
	 */
	public static void buildSchedule(List<PlanImpl> plans, List<Function<PlanImpl, Integer>> criteria,
			List<Float> weights) {

		// The sorted set P of plans.
		Queue<PlanImpl> plansQueue = new PriorityQueue<>(new Comparator<Plan>() {
			@Override
			public int compare(Plan o1, Plan o2) {
				return Float.compare(o1.getScore(), o2.getScore());
			}
		});

		// Calculate the inverse priority for each plan
		int maxPriority = Collections.max(plans.stream().map(PlanImpl::getPriority).collect(Collectors.toList()));
		plans.forEach(p -> p.setInversePriority(maxPriority));

		// Calculate the plans score
		calculatePlanScore(plans, criteria, weights);

		// Once calculated the score, each plan is inserted in order to the
		// priority queue
		plans.forEach(p -> plansQueue.add(p));

		// [TODO] MAIN LOOP

	}

	public static List<Float> calculatePlanScore(List<PlanImpl> plans, List<Float> weights) {
		List<Float> scores = new ArrayList<>();
		int maxPriority = Collections.max(plans.stream().map(PlanImpl::getPriority).collect(Collectors.toList()));

		for (PlanImpl plan : plans) {
			float score = 0;
			// inverse of priority
			score += (1 + maxPriority - plan.getPriority()) * weights.get(0);
			score += plan.getExecutionTime() * weights.get(1);
			score += plan.getNumberOfTasks() * weights.get(2);
			plan.setScore(score);
			scores.add(score);
		}

		return scores;
	}

	public static List<Float> calculatePlanScore(List<PlanImpl> plans, List<Function<PlanImpl, Integer>> criteria,
			List<Float> weights) {

		if (weights.size() != criteria.size()) {
			throw new IllegalArgumentException("Weights list must have the same size of criteria list.");
		}

		List<Float> scores = new ArrayList<>();
		for (PlanImpl plan : plans) {
			float score = 0;
			for (int i = 0; i < weights.size(); i++) {
				score += criteria.get(i).apply(plan) * weights.get(i);
			}
			scores.add(score);
		}

		return scores;
	}

	private static boolean schedulePlan(PlanImpl plan, Schedule s) {
		for (Task t : plan.tasks()) {
			if (checkConstraints(t, s)) {
				scheduleTask(t, s);
			} else {
				plan.setSchedulable(false);
			}
		}
		plan.setSchedulable(true);
		return plan.isSchedulable();
	}

	/**
	 * ALGORITHM 2
	 * 
	 * @param t
	 * @param s
	 */
	public static boolean checkConstraints(Task t, Schedule s) {

		// STEP 1: controlla che starting time sia dentro il bound giusto (cioè
		// dentro
		// [rk,dk]e che a sua volta s e s+p siano dentro W
		int startingTime = Math.max(t.getReleaseTime(), s.getDueDateForLastTaskIn(t.getResourceID()) + 1);
		try {
			Utils.requireValidBounds(startingTime, s.getWStart(), s.getWEnd(),
					"for task [" + t.toString() + "]: starting time" + startingTime + " not in [rk,dk]");

			Utils.requireValidBounds(startingTime, t.getReleaseTime(), t.getDueDate(),
					"for task [" + t.toString() + "]: starting time" + startingTime + "not in window ["
							+ t.getReleaseTime() + "," + t.getDueDate() + "]");
		} catch (IllegalArgumentException e) {
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
				return false;
		}

		return true;
	}

	/**
	 * ALGORITHM 3
	 * 
	 * @param t
	 * @param s
	 */
	public static void scheduleTask(Task t, Schedule s) {
		int startingTime = s.getDueDateForLastTaskIn(t.getResourceID()) + 1;
		s.add(startingTime, t);
	}

}
