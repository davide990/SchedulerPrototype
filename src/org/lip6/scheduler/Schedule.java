package org.lip6.scheduler;

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Schedule implements Executable {
	private int WStart;
	private int WEnd;
	private final LinkedHashMap<Integer, PlanImpl> plans = new LinkedHashMap<>();

	private final static Logger logger = Logger.getLogger(PlanImpl.class.getName());

	public Schedule(int wStart, int wEnd) {
		WStart = wStart;
		WEnd = wEnd;
	}

	public static Schedule get(int WStart, int WEnd) {

		requireValidBounds(WStart, 0, Integer.MAX_VALUE);
		requireValidBounds(WEnd, WStart + 1, Integer.MAX_VALUE);

		Schedule schedule = new Schedule(WStart, WEnd);

		return schedule;
	}

	private static int requireValidBounds(int value, int min, int max) {
		if (value < min || value > max) {
			throw new IllegalArgumentException("Value " + value + " is not valid.");
		}
		return value;
	}

	public boolean isFeasible() {
		for (Plan p : plans.values()) {
			if (!p.isSchedulable()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void execute(String[] args) {
		logger.log(Level.FINEST, "Executing Schedule");
		plans.forEach((k, v) -> {
			logger.log(Level.FINEST, "Executing Plan [" + k + "]");
			v.execute(args);
		});

	}

}
