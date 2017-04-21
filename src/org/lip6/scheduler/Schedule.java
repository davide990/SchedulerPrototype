package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.lip6.scheduler.utils.Utils;

public class Schedule implements Executable, Cloneable {

	private final int numResources;
	private final int WStart;
	private final int WEnd;

	/**
	 * This map contains, for each resource (key), the last assigned task.
	 */
	private final Map<Integer, TaskSchedule> lastTaskForResource;

	/**
	 * This list contains the ID of the plans scheduled
	 */
	private final List<Integer> plans;

	/**
	 * The ordered set of task schedules
	 */
	private final List<TaskSchedule> schedule;

	private final static Logger logger = Logger.getLogger(Schedule.class.getName());

	private Schedule(int numResources, int wStart, int wEnd) {
		this.numResources = numResources;
		WStart = wStart;
		WEnd = wEnd;
		schedule = new ArrayList<>();
		plans = new ArrayList<>();
		lastTaskForResource = new HashMap<>();
	}

	public static Schedule get(int numResources, int WStart, int WEnd) {
		Utils.requireValidBounds(WStart, 0, Integer.MAX_VALUE);
		Utils.requireValidBounds(WEnd, WStart + 1, Integer.MAX_VALUE);
		Utils.requireValidBounds(numResources, 1, Integer.MAX_VALUE);
		return new Schedule(numResources, WStart, WEnd);
	}

	public int getWStart() {
		return WStart;
	}

	public int getWEnd() {
		return WEnd;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Schedule s = Schedule.get(numResources, WStart, WEnd);

		lastTaskForResource.forEach((k, v) -> {
			try {
				s.lastTaskForResource.put(k, (TaskSchedule) v.clone());

			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		});

		schedule.forEach(sc -> {
			try {
				s.schedule.add((TaskSchedule) sc.clone());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		});

		plans.forEach(p -> s.plans.add(p));

		return s;
	}

	public void add(int startingTime, Task t) {
		Objects.requireNonNull(t, "Task cannot be null");

		// Create a new task assignment for task t at starting time startingTime
		TaskSchedule s = new TaskSchedule(t, startingTime, t.getResourceID());
		schedule.add(s);

		// Keep the ID of the plan which contains t
		if (!plans.contains(t.getPlanID())) {
			plans.add(t.getPlanID());
		}

		// Keep the task t as the last task assigned for the resource at which
		// it refers.
		lastTaskForResource.put(t.getResourceID(), s);
	}

	/**
	 * Returns the due date of the last task allocated for the specified
	 * resource, or WStart if there's no task allocated for it.
	 * 
	 * @param resource
	 *            the ID of the resource
	 * @return
	 */
	public int getDueDateForLastTaskIn(int resource) {
		// If a task has been already scheduled for a given resource
		if (lastTaskForResource.containsKey(resource)) {
			// Get its due date
			TaskSchedule s = lastTaskForResource.get(resource);

			int dueDate = s.getStartingTime() + s.getTask().getProcessingTime();

			return dueDate;
			// return s.getStartingTime() + s.getTask().getProcessingTime();
		}

		return WStart;
	}

	public List<TaskSchedule> taskSchedules() {
		return Collections.unmodifiableList(schedule);
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

	@Override
	public String toString() {

		String s = "";
		for (int plan : plans()) {
			String tasks = schedule.stream().map(TaskSchedule::getTask).filter(x -> x.getPlanID() == plan)
					.map(x -> Integer.toString(x.getTaskID())).collect(Collectors.joining(","));
			s = s.concat("Plan #" + plan + ": {" + tasks + "}\n");
		}

		return s;
	}

}
