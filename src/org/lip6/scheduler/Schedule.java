package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

import org.lip6.scheduler.utils.Utils;

public class Schedule implements Cloneable {

	//private final int numResources;
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
	 * The set of task schedules. These are ordered according to the starting
	 * times of the schedules.
	 */
	private final Queue<TaskSchedule> schedule;

	private Schedule(int wStart, int wEnd) {
		//this.numResources = numResources;
		WStart = wStart;
		WEnd = wEnd;
		plans = new ArrayList<>();
		lastTaskForResource = new HashMap<>();
		schedule = new PriorityQueue<>(new Comparator<TaskSchedule>() {
			@Override
			public int compare(TaskSchedule o1, TaskSchedule o2) {
				return Integer.compare(o1.getStartingTime(), o2.getStartingTime());
			}
		});
	}

	/**
	 * Get an istance of Schedule
	 * 
	 * @param numResources
	 * @param WStart
	 * @param WEnd
	 * @return
	 */
	public static Schedule get(int WStart, int WEnd) {
		Utils.requireValidBounds(WStart, 0, Integer.MAX_VALUE, "Invalid value of WStart");
		Utils.requireValidBounds(WEnd, WStart + 1, Integer.MAX_VALUE, "Invalid value of WEnd");
		//Utils.requireValidBounds(numResources, 1, Integer.MAX_VALUE, "Num. resource < 1");
		return new Schedule(WStart, WEnd);
	}

	public int getWStart() {
		return WStart;
	}

	public int getWEnd() {
		return WEnd;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Schedule s = Schedule.get(WStart, WEnd);

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

		s.plans.addAll(plans);
		return s;
	}

	/**
	 * Get the accomplishment time (proc. time + starting time) for the
	 * specified task
	 */
	public int getAccomplishmentTime(int planID, int taskID) {
		// Find the corresponding task assignment
		Optional<TaskSchedule> s = schedule.stream()
				.filter(x -> ((Task) x.getTask()).getPlanID() == planID && x.getTask().getID() == taskID).findFirst();

		// if the task has been scheduled, get its accomplishment time
		if (s.isPresent()) {
			return s.get().getStartingTime() + ((Task) s.get().getTask()).getProcessingTime();
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
	public void addTask(int startingTime, Task task) {
		Objects.requireNonNull(task, "Task cannot be null");

		if (!(task instanceof Task)) {
			throw new IllegalArgumentException("Argument is not a task");
		}

		Task t = (Task) task;

		if (startingTime <= 0) {
			throw new IllegalArgumentException("Starting time can not be <= 0");
		}

		// Create a new task assignment for task t at starting time startingTime
		TaskSchedule s = new TaskSchedule(task, startingTime, t.getResourceID());
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
			return s.getStartingTime() + ((Task) s.getTask()).getProcessingTime();
		}

		return WStart;
	}

	public List<TaskSchedule> taskSchedules() {
		return Collections.unmodifiableList(new ArrayList<>(schedule));
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
						+ ((Task) lastTaskForResource.get(x.getResource()).getTask()).getProcessingTime();
				if (x.getStartingTime() + ((Task) x.getTask()).getProcessingTime() >= lastAccomplishmentDate) {
					lastTaskForResource.put(x.getResource(), x);
				}
			}
		});
	}

	public List<Integer> plans() {
		return Collections.unmodifiableList(plans);
	}


	@Override
	public String toString() {
		String s = "";
		for (int plan : plans()) {
			String tasks = schedule.stream().map(TaskSchedule::getTask).filter(x -> ((Task) x).getPlanID() == plan)
					.map(x -> Integer.toString(x.getID())).collect(Collectors.joining(","));
			s = s.concat("Plan #" + plan + ": {" + tasks + "}\n");
		}

		return s;
	}

}
