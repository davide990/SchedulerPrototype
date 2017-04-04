package org.lip6.scheduler.algorithm;

import java.util.List;
import java.util.stream.Collectors;

import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.ScheduleAssignment;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.utils.Utils;

public class Scheduler {

	/**
	 * ALGORITHM 1
	 * 
	 * @param t
	 * @param s
	 */
	public static void buildSchedule() {

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
		List<Task> scheduledTasksInSamePlan = s.assignments().stream().map(ScheduleAssignment::getTask)
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
