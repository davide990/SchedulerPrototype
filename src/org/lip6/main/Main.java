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

		 String filename = "/home/davide/test_case_2.csv";
		//String filename = "/home/davide/paper_plans_nouveau.csv";
		int WStart = 1;
		int WEnd = 200;
		int maxResourceCapacity = 1;

		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd, filename);
/*
		Map<Integer, Plan> pp = new HashMap<>();
		try {
			pp = CSVParser.parse(filename);
		} catch (IOException e) {
			System.err.println("Error while loading file: \"" + filename + "\"");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
			CSVParser.parseDeltaValues("/home/davide/paper_plans_nouveau_dt.csv", pp);
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		Task t11 = pp.get(1).getTask(1);
		
		
		for(Integer t : t11.deltaValues().keySet()){
			System.out.println("p("+Integer.toString(t)+") = "+Integer.toString(t11.getProcessingTime(t)));
		}*/
		
		long startTime = System.currentTimeMillis();
		sc.buildSchedule();
		long endTime = System.currentTimeMillis();
		long result = endTime - startTime;

		System.err.println("---> " + Long.toString(result) + "ms");

	}

}
