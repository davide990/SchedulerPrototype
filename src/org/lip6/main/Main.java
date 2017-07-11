package org.lip6.main;

import java.io.IOException;
import java.text.ParseException;

import org.lip6.scheduler.algorithm.Scheduler;
import org.lip6.scheduler.algorithm.SchedulerFactory;
import org.lip6.scheduler.utils.CSVParser;

/**
 * Usage (from command line):<br/>
 * ⇢Scheduler [PlanSetCSV] <br/>
 * ⇢Scheduler [Ws] [We] [PlanSetCSV]<br/>
 * ⇢Scheduler [Ws] [We] [MaxResCapacity] [PlanSetCSV]<br/>
 * ⇢Scheduler [Ws] [We] [MaxResCapacity] [PlanSetCSV]
 * [ProcessingTimeFunctionCSV]<br/>
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 *
 */
public class Main {

	public static void main(String[] args) {

		int WStart = 0;
		int WEnd = 180;
		int maxResourceCapacity = 1;
		String planSetFileName = "";
		String processingTimeFuncFileName = "";

		if (args.length == 1) {
			planSetFileName = args[0];
		} else if (args.length == 3) {
			WStart = Integer.parseInt(args[0]);
			WEnd = Integer.parseInt(args[1]);
			planSetFileName = args[2];
		} else if (args.length == 4) {
			WStart = Integer.parseInt(args[0]);
			WEnd = Integer.parseInt(args[1]);
			maxResourceCapacity = Integer.parseInt(args[2]);
			planSetFileName = args[3];
		} else if (args.length == 5) {
			WStart = Integer.parseInt(args[0]);
			WEnd = Integer.parseInt(args[1]);
			maxResourceCapacity = Integer.parseInt(args[2]);
			planSetFileName = args[3];
			processingTimeFuncFileName = args[4];
		} else {
			System.err.println(
					"Usage:\n⇢Scheduler [PlanSetCSV] \n⇢Scheduler [Ws] [We] [PlanSetCSV]\n⇢Scheduler [Ws] [We] [MaxResCapacity] [PlanSetCSV]\n⇢Scheduler [Ws] [We] [MaxResCapacity] [PlanSetCSV] [ProcessingTimeFunctionCSV]");
			return;
		}

		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd, planSetFileName);

		if (!processingTimeFuncFileName.equals("")) {
			try {
				CSVParser.parseDeltaValues("/home/davide/paper_plans_nouveau_dt.csv", sc.getPlans());
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}
		}

		long startTime = System.currentTimeMillis();
		sc.buildSchedule();
		long endTime = System.currentTimeMillis();
		long result = endTime - startTime;

		System.err.println("Execution time: #" + Long.toString(result) + "ms");
	}

}
