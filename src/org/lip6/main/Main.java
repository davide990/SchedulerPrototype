package org.lip6.main;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.algorithm.Scheduler;
import org.lip6.scheduler.algorithm.SchedulerFactory;
import org.lip6.scheduler.utils.CSVParser;

public class Main {

	public static void main(String[] args) {

		// String filename = "/home/davide/scenario_5.csv";
		//String filename = "/home/davide/test_case_2.csv";
		 String filename = "/home/davide/paper_plans_nouveau.csv";
		int WStart = 1;
		int WEnd = 200;
		int maxResourceCapacity = 1;

		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd, filename);

		/*try {
			CSVParser.parseDeltaValues("/home/davide/paper_plans_nouveau_dt.csv", sc.getPlans());
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

		long startTime = System.currentTimeMillis();
		sc.buildSchedule();
		long endTime = System.currentTimeMillis();
		long result = endTime - startTime;

		
		/*Task t11 = sc.getPlans().stream().filter(x->x.getID()==1).findFirst().get().getTask(1);

		for (Integer t : t11.deltaValues().keySet()) {
			System.out.println("p(" + Integer.toString(t) + ") = " + Integer.toString(t11.getProcessingTime(t)));
		}*/
		
		
		System.err.println("---> " + Long.toString(result) + "ms");

	}

}
