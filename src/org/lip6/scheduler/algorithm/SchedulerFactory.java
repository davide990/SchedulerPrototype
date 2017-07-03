package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.list.TreeList;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.utils.CSVParser;

/**
 * Static factory for the Scheduler class.
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public class SchedulerFactory {

	public static Scheduler getFromFile(int maxResourceCapacity, int WStart, int WEnd, String filename) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(filename);
		} catch (IOException e) {
			System.err.println("Error while loading file: \"" + filename + "\"");
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		List<Plan> plans = new TreeList<>(p.values());
		return Scheduler.get(maxResourceCapacity, plans, WStart, WEnd);
	}

	public static Scheduler get(int maxResourceCapacity, int WStart, int WEnd, InputStream is) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(is);
		} catch (IOException | ParseException e) {
			System.err.println("Error while processing input stream.\n" + e);
			return null;
		}
		List<Plan> plans = new TreeList<>(p.values());

		return Scheduler.get(maxResourceCapacity, plans, WStart, WEnd);
	}

	public static Scheduler get(int maxResourceCapacity, int WStart, int WEnd, List<Plan> plans) {
		return Scheduler.get(maxResourceCapacity, plans, WStart, WEnd);
	}
}
