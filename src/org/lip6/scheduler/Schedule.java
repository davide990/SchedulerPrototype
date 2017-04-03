package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Schedule implements Executable {

	private final int numResources;
	private final int WStart;
	private final int WEnd;

	/**
	 * This map contains, for each resource (key), the last assigned task.
	 */
	private final Map<Integer, ScheduleAssignment> lastTaskForResource;
	private final List<Integer> plans;
	private final List<ScheduleAssignment> schedule;

	private final static Logger logger = Logger.getLogger(PlanImpl.class.getName());

	private Schedule(int numResources, int wStart, int wEnd) {
		this.numResources = numResources;
		WStart = wStart;
		WEnd = wEnd;
		schedule = new ArrayList<>();
		plans = new ArrayList<>();
		lastTaskForResource = new HashMap<>();
	}

	public static Schedule get(int numResources, int WStart, int WEnd) {
		requireValidBounds(WStart, 0, Integer.MAX_VALUE);
		requireValidBounds(WEnd, WStart + 1, Integer.MAX_VALUE);
		return new Schedule(numResources, WStart, WEnd);
	}

	public int getWStart() {
		return WStart;
	}

	public int getWEnd() {
		return WEnd;
	}

	private static int requireValidBounds(int value, int min, int max) {
		if (value < min || value > max) {
			throw new IllegalArgumentException("Value " + value + " is not valid.");
		}
		return value;
	}

	public void add(int resouceID, int startingTime, Task t) {
		Objects.requireNonNull(t, "Task cannot be null");

		ScheduleAssignment s = new ScheduleAssignment(t, startingTime, resouceID);
		schedule.add(s);
		if (!plans.contains(t.planID)) {
			plans.add(t.planID);
		}
		lastTaskForResource.put(resouceID, s);
	}

	/**
	 * Returns the due date of the last task allocated for the specified resource, or -1 if there's no task allocated for it.
	 * @param resource the ID of the resource
	 * @return
	 */
	public int getDueDateForLastTaskIn(int resource) {
		if (lastTaskForResource.containsKey(resource)) {
			ScheduleAssignment s = lastTaskForResource.get(resource);
			return s.getStartingTime() + s.getTask().getProcessingTime();
		}

		return -1;
	}

	public List<Integer> plans() {
		return Collections.unmodifiableList(plans);
	}

	public int resources() {
		return numResources;
	}

	public boolean isFeasible() {

		return true;
	}

	@Override
	public void execute(String[] args) {
		logger.log(Level.FINEST, "Executing Schedule");
		/*
		 * schedule.forEach((k, v) -> { logger.log(Level.FINEST,
		 * "Executing Plan [" + k + "]"); // v.execute(args); });
		 */
	}

}
