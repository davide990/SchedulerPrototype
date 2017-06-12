package org.lip6.main;

import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.algorithm.Scheduler;
import org.lip6.scheduler.algorithm.SchedulerFactory;

public class Main {

	public static void main(String[] args) {
		String filename = "/home/davide/test_case_2.csv";
		//String filename = "/home/davide/paper_plans_nouveau.csv";
		int WStart = 2;
		int WEnd = 200;
		int maxResourceCapacity = 1;

		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd, filename);
		Schedule s = sc.buildSchedule();
		System.out.println("Scheduling:\n" + s);
		
		
	}

}
