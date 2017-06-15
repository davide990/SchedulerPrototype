package org.lip6.main;

import org.lip6.scheduler.algorithm.Scheduler;
import org.lip6.scheduler.algorithm.SchedulerFactory;

public class Main {

	public static void main(String[] args) {
		
		String filename = "/home/davide/test_case_2.csv";
		// String filename = "/home/davide/paper_plans_nouveau.csv";
		int WStart = 1;
		int WEnd = 200;
		int maxResourceCapacity = 1;

		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd, filename);
		sc.buildSchedule();

/*		Task a = TaskFactory.getTask(1, 1, "ciao", 1, 1, 10, 5, 10, new ArrayList<>(),
				(Integer timeInstant, Integer currentProcessingTime) -> {
					return Math.max(0, currentProcessingTime - 1);
				});*/

	}

}
