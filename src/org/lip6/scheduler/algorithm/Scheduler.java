package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

	private static final Comparator<Plan> PLAN_COMPARATOR = new Comparator<Plan>() {
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
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<Plan> plans = new ArrayList<>(p.values());
		Schedule s = buildSchedule(plans, criterias, WStart, WEnd);
		return s;
	}

	public static Schedule schedule(int WStart, int WEnd, List<Criteria> criterias, InputStream is) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(is);
		} catch (IOException | ParseException e) {
			System.err.println("Error while processing input stream.\n" + e);
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
	 * ALGORITHM 1 TODO MANCA IL NUMERO DI RISORSE
	 * 
	 * @param t
	 * @param s
	 */
	private static Schedule buildSchedule(List<Plan> plans, List<Criteria> criterias, int wStart, int wEnd) {
		Schedule workingSolution = Schedule.get(1, wStart, wEnd);
		Schedule lastFeasibleSolution = Schedule.get(1, wStart, wEnd);

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
				TabuListEntry e = tabuList.poll();
				if (e.getWaitTurns() > 0) {
					e.setWaitTurns(e.getWaitTurns() - 1);
					tabuList.add(e);
				} else {
					// Check precedence constraints
					if (!checkPrecedences(workingSolution, e.getTask())) {
						e.setWaitTurns(e.getWaitTurns() + 1);
						tabuList.add(e);
						continue;
					}

					int st = scheduleTask(e.getTask(), workingSolution);
					System.out.println("[TABU UPDATE]--> Task " + e.getTask().getResourceID() + " scheduled at "
							+ Integer.toString(st));
					if (!checkConstraints(e.getTask(), st, workingSolution)) {
						e.getPlan().setSchedulable(false);
					}

					if (e.getPlan().isSchedulable()) {
						try {
							lastFeasibleSolution = (Schedule) workingSolution.clone();
						} catch (CloneNotSupportedException ee) {
							ee.printStackTrace();
						}
						// push pk into the queue of scheduled plans
						scheduledPlans.add(e.getPlan());
					} else {
						List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
								.filter(x -> x.getTask().getPlanID() == e.getPlan().getID())
								.collect(Collectors.toList());
						workingSolution.unSchedule(toRemove);
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
			System.out.println("PLAN " + pk.getID());

			boolean hasTabuTask = false;

			// Loop each task t in the plan pk
			for (Task t : pk.tasks()) {

				// Check precedence constraints
				if (!checkPrecedences(workingSolution, t)) {
					tabuList.add(new TabuListEntry(t, pk, 1));
					hasTabuTask = true;
					continue;
				}
				// if satisfied -> proceed
				int st = scheduleTask(t, workingSolution);
				System.out.println("--> Task " + t.getResourceID() + " scheduled at " + Integer.toString(st));
				if (!checkConstraints(t, st, workingSolution)) {
					pk.setSchedulable(false);
				}
			}
			if (hasTabuTask) {
				continue; // ...because right now the plan cannot be checked,
							// since it has some task in the tabu list
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
				List<TaskSchedule> toRemove = workingSolution.taskSchedules().stream()
						.filter(x -> x.getTask().getPlanID() == pk.getID()).collect(Collectors.toList());
				workingSolution.unSchedule(toRemove);
				/*
				 * try { workingSolution = (Schedule)
				 * lastFeasibleSolution.clone(); } catch
				 * (CloneNotSupportedException e) { e.printStackTrace(); }
				 */
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

			// Checks for the accomplishment date to be inside the temporal
			// window [Ws,We]
			Utils.requireValidBounds(startingTime + t.getProcessingTime(), s.getWStart(), s.getWEnd(),
					"for " + t.toString() + ": due date " + startingTime + t.getProcessingTime() + " not in window ["
							+ s.getWStart() + "," + s.getWEnd() + "]");
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			return false;
		}

		// STEP 2: controlla che tutti i predecessori siano in s
		// Prendo gli altri task dello stesso piano di t, che sono stati già
		// schedulati
		// List<Task> scheduledTasksInSamePlan =
		// s.taskSchedules().stream().map(TaskSchedule::getTask)
		// .filter(sc -> sc.getPlanID() ==
		// t.getPlanID()).collect(Collectors.toList());
		// List<Integer> scheduledTaskID =
		// scheduledTasksInSamePlan.stream().map(Task::getTaskID)
		// .collect(Collectors.toList());

		// Per ogni predecessore del task, verifica che sia stato gia schedulato

		/*
		 * 
		 * 
		 * PRECEDENZA for (Integer p : t.getPredecessors()) { if
		 * (!scheduledTaskID.contains(p)) System.err.println("Error: for task "
		 * + t.getTaskID() + "[plan " + t.getPlanID() +
		 * "] schedule doesn't contains required predecessor " + p +
		 * " within the same plan."); return false; }
		 * 
		 * 
		 * 
		 * 
		 * 
		 */

		return true;
	}

	private static boolean checkPrecedences(Schedule s, Task t) {
		final List<ImmutablePair<Integer, Integer>> scheduledPlanTaskPairs = s.taskSchedules().stream()
				.map(x -> new ImmutablePair<>(x.getTask().getPlanID(), x.getTask().getTaskID()))
				.collect(Collectors.toList());

		for (ImmutablePair<Integer, Integer> pair : t.getPredecessors()) {
			if (!scheduledPlanTaskPairs.contains(pair)) {
				return false;
			}
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
		// int lastTaskScheduledDueDate =
		// s.getAccomplishmentForLastTaskIn(t.getResourceID());

		// In questo punto tutti i predecessori di t sono stati gia schedulati,
		// bisogna solo calcolare la giusta starting time

		// s -> triple (piano,task,sk)
		// t.predecessors

		// Prendo tutti gli schedule che sono anche predecessori di t
		List<TaskSchedule> f = s.taskSchedules().stream()
				.filter(x -> t.getPredecessors()
						.contains(new ImmutablePair<>(x.getTask().getPlanID(), x.getTask().getTaskID())))
				.collect(Collectors.toList());
		// Prendo la accomplishment date piu grande

		OptionalInt maxAccomplishmentDate = f.stream()
				.mapToInt(x -> x.getStartingTime() + x.getTask().getProcessingTime()).max();

		int md = -1;
		if (!maxAccomplishmentDate.isPresent()) {
			md = s.getAccomplishmentForLastTaskIn(t.getResourceID());
		} else {

			md = maxAccomplishmentDate.getAsInt();
			System.err.println("For task " + t + " starting time is MAX: " + Integer.toString(md));
		}

		int rk = t.getReleaseTime();
		int startingTime = Math.max(md, rk);
		// int startingTime = Math.max(lastTaskScheduledDueDate, rk);

		// Add the task to the schedule
		s.add(startingTime, t);

		return startingTime;
	}

}
