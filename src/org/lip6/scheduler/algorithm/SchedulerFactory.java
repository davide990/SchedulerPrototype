package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.utils.CSVParser;

public class SchedulerFactory {

	public static Schedule scheduleFromFile(int maxResourceCapacity, int WStart, int WEnd, String filename) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(filename);
		} catch (IOException e) {
			System.err.println("Error while loading file: \"" + filename + "\"");
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Set<Plan> plans = new HashSet<>(p.values());

		Scheduler scheduler = Scheduler.get(maxResourceCapacity, plans, WStart, WEnd);
		Schedule s = scheduler.buildSchedule();
		return s;
	}

	public static Schedule schedule(int maxResourceCapacity, int WStart, int WEnd, InputStream is) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(is);
		} catch (IOException | ParseException e) {
			System.err.println("Error while processing input stream.\n" + e);
			return null;
		}
		Set<Plan> plans = new HashSet<>(p.values());
		Scheduler scheduler = Scheduler.get(maxResourceCapacity, plans, WStart, WEnd);
		Schedule s = scheduler.buildSchedule();
		return s;
	}

	public static Schedule schedule(int maxResourceCapacity, int WStart, int WEnd, Set<Plan> plans) {
		Scheduler scheduler = Scheduler.get(maxResourceCapacity, plans, WStart, WEnd);
		Schedule s = scheduler.buildSchedule();
		return s;
	}
}
