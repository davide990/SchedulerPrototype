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
import org.lip6.scheduler.TaskFactory;
import org.lip6.scheduler.utils.Utils;

public class Scheduler {

	public static void main(String[] args) {
		int WStart = 2;
		int WEnd = 15;
		List<PlanImpl> plans = new ArrayList<>();
		List<Function<PlanImpl, Integer>> criteria = new ArrayList<>();
		List<Float> weights = new ArrayList<>();

		PlanImpl p0 = PlanImpl.get(0, 5);
		PlanImpl p1 = PlanImpl.get(1, 9);
		PlanImpl p2 = PlanImpl.get(2, 2);

		p0.addTask(TaskFactory.getTask(0, 0, 0, 2, 5));
		p0.addTask(TaskFactory.getTask(1, 0, 0, 6, 8));
		p0.addTask(TaskFactory.getTask(2, 0, 0, 9, 14));

		p1.addTask(TaskFactory.getTask(3, 1, 0, 1, 3));
		p1.addTask(TaskFactory.getTask(4, 1, 0, 1, 6));

		p2.addTask(TaskFactory.getTask(5, 2, 0, 6, 9));
		p2.addTask(TaskFactory.getTask(6, 2, 0, 10, 15));

		plans.add(p1);
		plans.add(p2);
		plans.add(p0);

		criteria.add(PlanImpl::getPriority);
		weights.add(1f);
		criteria.add(PlanImpl::getNumberOfTasks);
		weights.add(0.1f);
		criteria.add(PlanImpl::getExecutionTime);
		weights.add(0.01f);

		System.out.println("SET OF PLANS:");
		plans.forEach(p -> {
			System.out.println("---> " + p.getID());
		});

		Schedule s = buildSchedule(plans, criteria, weights, WStart, WEnd);

		System.out.println("Scheduled plans: " + s.plans().size() + " of " + plans.size());
	}

	/**
	 * ALGORITHM 1
	 * 
	 * @param t
	 * @param s
	 */
	public static Schedule buildSchedule(List<PlanImpl> plans, List<Function<PlanImpl, Integer>> criteria,
			List<Float> weights, int WStart, int WEnd) {

		Schedule workingSolution = Schedule.get(1, WStart, WEnd);
		Schedule lastFeasibleSolution = Schedule.get(1, WStart, WEnd);

		Comparator<Plan> comparator = new Comparator<Plan>() {
			@Override
			public int compare(Plan o1, Plan o2) {
				return Float.compare(o1.getScore(), o2.getScore());
			}
		};

		// The sorted set P of plans.
		Queue<Plan> plansQueue = new PriorityQueue<>(comparator);
		Queue<Plan> scheduledPlans = new PriorityQueue<>(comparator);
		Queue<Plan> unscheduledPlans = new PriorityQueue<>(comparator);

		// Calculate the inverse priority for each plan
		int maxPriority = Collections.max(plans.stream().map(PlanImpl::getPriority).collect(Collectors.toList()));
		plans.forEach(p -> p.setInversePriority(maxPriority));

		// Calculate the plans score
		calculatePlanScore(plans, criteria, weights);

		// Once calculated the score, each plan is inserted in order to the
		// priority queue
		plans.forEach(p -> plansQueue.add(p));

		// MAIN LOOP
		while (!plansQueue.isEmpty()) {
			Plan pk = plansQueue.poll();
			for (Task t : pk.tasks()) {
				int st = scheduleTask(t, workingSolution);
				if (!checkConstraints(t, st, workingSolution)) {
					pk.setSchedulable(false);
				}
			}

			if (pk.isSchedulable()) {
				try {
					lastFeasibleSolution = (Schedule) workingSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}

				// push pk into the queue of scheduled plans
				scheduledPlans.add(pk);
			} else {
				try{
					workingSolution = (Schedule) lastFeasibleSolution.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				unscheduledPlans.add(pk);
			}

		}

		return lastFeasibleSolution;
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
	public static boolean checkConstraints(Task t, int startingTime, Schedule s) {

		// STEP 1: controlla che starting time sia dentro il bound giusto (cioè
		// dentro
		// [rk,dk]e che a sua volta s e s+p siano dentro W
		// int startingTime = Math.max(t.getReleaseTime(),
		// s.getDueDateForLastTaskIn(t.getResourceID()) + 1);
		try {
			Utils.requireValidBounds(startingTime, t.getReleaseTime(), t.getDueDate(),
					"for " + t.toString() + ": starting time " + startingTime + " not in [rk=" + t.getReleaseTime()
							+ ",dk=" + t.getDueDate() + "]");

			Utils.requireValidBounds(startingTime, s.getWStart(), s.getWEnd(), "for " + t.toString()
					+ ": starting time " + startingTime + " not in window [" + s.getWStart() + "," + s.getWEnd() + "]");
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
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
	 * ALGORITHM 2
	 * 
	 * @param t
	 * @param s
	 */
	public static int scheduleTask(Task t, Schedule s) {
		int startingTime = Math.max(s.getDueDateForLastTaskIn(t.getResourceID()) + 1, t.getReleaseTime());
		s.add(startingTime, t);
		return startingTime;
	}

}
