package org.lip6.scheduler.algorithm;

import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.Task;

public class Scheduler {

	/**
	 * ALGORITHM 1
	 * 
	 * @param t
	 * @param s
	 */
	public static void buildSchedule() {

	}

	/**
	 * ALGORITHM 2
	 * 
	 * @param t
	 * @param s
	 */
	public static boolean checkConstraints(Task t, Schedule s) {
		int startingTime = s.getDueDateForLastTaskIn(t.getResourceID()) + 1;
		
		//controlla che starting time sia dentro il bound giusto
		
		//controlla che tutti i predecessori siano in s
		
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
