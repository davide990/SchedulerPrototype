package org.lip6.scheduler.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.utils.CSVParser;
import org.lip6.scheduler.utils.Utils;

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

		Set<Plan> plans = new HashSet<>(p.values());

		return SchedulerFactory.get(maxResourceCapacity, plans, WStart, WEnd);
	}

	/**
	 * Static factory method for Scheduler. It returns a new instance of Scheduler
	 * class
	 * 
	 * @param maxResourceCapacity the maximum capacity of all the resources
	 * @param plans               the set of plans to scheduler
	 * @param wStart              the starting time of the temporal window
	 * @param wEnd                the final time of the temporal window
	 * @return
	 */
	public static Scheduler get(final int maxResourceCapacity, Set<Plan> plans, int wStart, int wEnd) {
		// Check if the resource capacity value is > 0
		Utils.requireValidBounds(maxResourceCapacity, 1, Integer.MAX_VALUE,
				"Maximum resource capacity must be a positive integer value.");

		// Check if the temporal window is valid
		if (wEnd <= wStart) {
			throw new IllegalArgumentException(
					"Invalid temporal window [" + Integer.toString(wStart) + "," + Integer.toString(wEnd) + "]");
		}

		// Create a new scheduler
		Scheduler scheduler = new Scheduler();
		scheduler.maxResourceCapacity = maxResourceCapacity;
		scheduler.wStart = wStart;
		scheduler.wEnd = wEnd;
		// scheduler.plans.addAll(plans);
		scheduler.addPlans(plans);

		// retrieve the IDs of the resources
		for (Plan p : plans) {
			scheduler.resourcesIDs
					.addAll(p.getTasks().stream().map(x -> x.getResourceID()).collect(Collectors.toList()));
		}

		// Create two events for Ws and We
		scheduler.events.add(Event.get(wStart, scheduler.resourcesIDs));
		scheduler.events.add(Event.get(wEnd, scheduler.resourcesIDs));

		return scheduler;
	}
	
	/**
	 * Static factory method for Scheduler. It returns a new instance of Scheduler
	 * class
	 * 
	 * @param maxResourceCapacity the maximum capacity of all the resources
	 * @param plans               the set of plans to scheduler
	 * @param wStart              the starting time of the temporal window
	 * @param wEnd                the final time of the temporal window
	 * @param listener            a listener that is invoked each time a <b>feasible
	 *                            solution</b> is constructed
	 * @return
	 */
	public static Scheduler get(final int maxResourceCapacity, Set<Plan> plans, int wStart, int wEnd,
			SchedulerListener listener) {
		Scheduler scheduler = SchedulerFactory.get(maxResourceCapacity, plans, wStart, wEnd);
		scheduler.listener = Optional.of(listener);
		return scheduler;
	}

	/**
	 * Unused?
	 * 
	 * @param maxResourceCapacity
	 * @param WStart
	 * @param WEnd
	 * @param is
	 * @return
	 */
	@Deprecated
	public static Scheduler get(int maxResourceCapacity, int WStart, int WEnd, InputStream is) {
		Map<Integer, Plan> p = null;
		try {
			p = CSVParser.parse(is);
		} catch (IOException | ParseException e) {
			System.err.println("Error while processing input stream.\n" + e);
			return null;
		}
		Set<Plan> plans = new HashSet<>(p.values());

		// !!!!!!!!!!!
		plans.forEach(x -> {
			x.getTasks().forEach(t -> {
				t.setProcessingTimeFunction(new BiFunction<Integer, Integer, Integer>() {
					@Override
					public Integer apply(Integer currentTimeInstant, Integer processingTime) {
						return Math.max(0, processingTime - 1);
					}
				});
			});
		});

		return SchedulerFactory.get(maxResourceCapacity, plans, WStart, WEnd);
	}

	
}
