package org.lip6.main;

import org.lip6.scheduler.algorithm.Scheduler;
import org.lip6.scheduler.algorithm.SchedulerFactory;

import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;
import etm.core.renderer.SimpleTextRenderer;
import etm.core.timer.DefaultTimer;
import etm.core.timer.SunHighResTimer;

public class Main {

	private static EtmMonitor monitor;

	public static void main(String[] args) {
		// configure measurement framework
		setup();
		
		EtmPoint point = monitor.createPoint("Scheduler:buildSchedule");

		//String filename = "/home/davide/test_case_2.csv";
		String filename = "/home/davide/paper_plans_nouveau.csv";
		int WStart = 1;
		int WEnd = 200;
		int maxResourceCapacity = 2;

		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd, filename);

	//	try {
			
			 long startTime = System.currentTimeMillis();

			sc.buildSchedule();
			 long endTime = System.currentTimeMillis();
			 long result = endTime - startTime; //Note, part might be backwards, I don't
			 
			 System.err.println("---> "+Long.toString(result));
/*
		} finally {
			point.collect();
		}*/
		// visualize results
		//monitor.render(new SimpleTextRenderer());

		// shutdown measurement framework
		//tearDown();
	}

	private static void setup() {
		BasicEtmConfigurator.configure(true, new DefaultTimer());
		monitor = EtmManager.getEtmMonitor();
		monitor.start();
	}

	private static void tearDown() {
		monitor.stop();
	}

}
