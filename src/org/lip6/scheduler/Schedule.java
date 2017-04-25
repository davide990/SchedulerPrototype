package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
	 * List containing the IDs of the scheduled plans
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

	/**
	 * Get an istance of Schedule
	 * 
	 * @param numResources
	 * @param WStart
	 * @param WEnd
	 * @return
	 */
	public static Schedule get(int numResources, int WStart, int WEnd) {
		Utils.requireValidBounds(WStart, 0, Integer.MAX_VALUE, "Invalid value of WStart");
		Utils.requireValidBounds(WEnd, WStart + 1, Integer.MAX_VALUE, "Invalid value of WEnd");
		Utils.requireValidBounds(numResources, 1, Integer.MAX_VALUE, "Num. resource < 1");
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

	/**
	 * Get the accomplishment time (proc. time + starting time) for the
	 * specified task
	 */
	public int getAccomplishmentTime(int planID, int taskID) {
		// Find the corresponding task assignment
		Optional<TaskSchedule> s = schedule.stream()
				.filter(x -> x.getTask().getPlanID() == planID && x.getTask().getTaskID() == taskID).findFirst();

		// if the task has been scheduled, get its accomplishment time
		if (s.isPresent()) {
			return s.get().getStartingTime() + s.get().getTask().getProcessingTime();
		}

		// otherwise throw an exception
		throw new IllegalArgumentException(
				"task #" + Integer.toString(taskID) + " in plan #" + Integer.toString(planID) + " not found");
	}

	/**
	 * Add the task t to this schedule to the given starting time.
	 * 
	 * @param startingTime
	 * @param t
	 */
	public void add(int startingTime, Task t) {
		Objects.requireNonNull(t, "Task cannot be null");
		if (startingTime <= 0) {
			throw new IllegalArgumentException("Starting time can not be <= 0");
		}

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
	 * Returns the accomplishment date of the last task allocated for the given
	 * resource, or WStart if there's no task allocated for it.
	 * 
	 * @param resource
	 *            the ID of the resource
	 * @return
	 */
	public int getAccomplishmentForLastTaskIn(int resource) {
		// If a task has been already scheduled for a given resource
		if (lastTaskForResource.containsKey(resource)) {
			TaskSchedule s = lastTaskForResource.get(resource);
			return s.getStartingTime() + s.getTask().getProcessingTime();
		}

		return WStart;
	}

	public List<TaskSchedule> taskSchedules() {
		return Collections.unmodifiableList(schedule);
	}

	public void unSchedule(Collection<TaskSchedule> collection) {
		schedule.removeAll(collection);
		lastTaskForResource.values().removeAll(collection);

		// Since schedules are removed from the map that keeps the last
		// allocated task(value) for resource(key), this map has to be updated.
		// So, for each of the remaining task, rebuild the map containing the
		// latest allocated task for resource
		schedule.forEach(x -> {
			if (!lastTaskForResource.containsKey(x.getResource())) {
				lastTaskForResource.put(x.getResource(), x);
			} else {
				int lastAccomplishmentDate = lastTaskForResource.get(x.getResource()).getStartingTime()
						+ lastTaskForResource.get(x.getResource()).getTask().getProcessingTime();
				if (x.getStartingTime() + x.getTask().getProcessingTime() >= lastAccomplishmentDate) {
					lastTaskForResource.put(x.getResource(), x);
				}
			}
		});
	}

	public List<Integer> plans() {
		return Collections.unmodifiableList(plans);
	}

	public int resources() {
		return numResources;
	}

	public boolean isFeasible() {
		// TODO WRITE ME
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
