package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.list.TreeList;
import org.lip6.scheduler.utils.Utils;

/**
 * A schedule represents a solution for the scheduling problem.
 * 
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 *
 */
public class Schedule implements Cloneable {
	/**
	 * The left bound W<sub>s</sub> of the time window W.
	 */
	private final int WStart;
	/**
	 * The right bound W<sub>e</sub> of the time window W.
	 */
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
	 * times.<br/>
	 * Recall that each TaskSchedule actually represents a starting time for a
	 * scheduled task, together with other useful informations used, for
	 * example, for rendering tasks in the web interface. <br/>
	 * Why a TreeSet? Because it provides guaranteed log(n) time cost for the
	 * basic operations (add, remove and contains). Also, it lets to <b>specify
	 * a comparator to keep the set sorted after insertion/removal
	 * operations</b> (with TreeList this is not possible).
	 */
	private final TreeList<TaskSchedule> schedule;

	private Schedule(int wStart, int wEnd) {
		// this.numResources = numResources;
		WStart = wStart;
		WEnd = wEnd;
		plans = new ArrayList<>();
		lastTaskForResource = new HashMap<>();
		schedule = new TreeList<>();
		/*
		 * schedule = new TreeSet<>(new Comparator<TaskSchedule>() {
		 * 
		 * @Override public int compare(TaskSchedule o1, TaskSchedule o2) {
		 * return Integer.compare(o1.getStartingTime(), o2.getStartingTime()); }
		 * });
		 */
	}

	/**
	 * Static factory method for Schedule class.
	 * 
	 * @param numResources
	 * @param WStart
	 * @param WEnd
	 * @return
	 */
	public static Schedule get(int WStart, int WEnd) {
		Utils.requireValidBounds(WStart, 0, Integer.MAX_VALUE, "Invalid value of WStart");
		Utils.requireValidBounds(WEnd, WStart + 1, Integer.MAX_VALUE, "Invalid value of WEnd");
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

		s.plans.addAll(new ArrayList<>(plans));
		return s;
	}

	/**
	 * Get the completion time
	 * (p<sup>k</sup><sub>i</sub>+s<sup>k</sup><sub>i</sub>) for the task
	 * J<sup>k</sup><sub>i</sub> specified in input.
	 */
	public int getCompletionTime(int planID, int taskID) {
		// Find the corresponding task assignment
		Optional<TaskSchedule> s = schedule.stream()
				.filter(x -> ((Task) x.getTask()).getPlanID() == planID && x.getTask().getID() == taskID).findFirst();

		// if the task has been scheduled, get its completion time
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
	 *            the starting time s<sup>k</sup><sub>i</sub> for the task t
	 * @param t
	 */
	public void addTask(int startingTime, final Task task) {
		Utils.requireValidBounds(startingTime, 0, Integer.MAX_VALUE, "Starting time can not be negative");
		Objects.requireNonNull(task, "Task cannot be null");

		// Create a new task assignment for task t at starting time startingTime
		TaskSchedule s = new TaskSchedule(task, startingTime, task.getResourcesID());
		if (!schedule.add(s)) {
			throw new RuntimeException("Cannot insert " + task + " to schedule.");
		}

		// Keep the ID of the plan which contains t
		if (!plans.contains(task.getPlanID())) {
			plans.add(task.getPlanID());
		}

		// Keep the task t as the last task assigned for the resource at which
		// it refers.
		for (Integer res : task.getResourcesID()) {
			lastTaskForResource.put(res, s);
		}
	}

	/**
	 * Returns the completion time of the last task allocated in the specified
	 * resource, or W<sub>s</sub> if there's no task allocated for it.
	 * 
	 * @param resource
	 *            the ID of the resource
	 * @return
	 */
	public int getCompletionTimeForLastTaskIn(int resource) {
		// If a task has been already scheduled for a given resource
		if (lastTaskForResource.containsKey(resource)) {
			TaskSchedule s = lastTaskForResource.get(resource);
			return s.getStartingTime() + ((Task) s.getTask()).getProcessingTime();
		}

		return WStart;
	}

	/**
	 * Return a read-only list containing the current schedules.
	 * 
	 * @return
	 */
	public List<TaskSchedule> taskSchedules() {
		return Collections.unmodifiableList(new TreeList<>(schedule));
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
				x.getResource().forEach(res -> lastTaskForResource.put(res, x));
			} else {
				int lastAccomplishmentDate = lastTaskForResource.get(x.getResource()).getStartingTime()
						+ ((Task) lastTaskForResource.get(x.getResource()).getTask()).getProcessingTime();
				if (x.getStartingTime() + ((Task) x.getTask()).getProcessingTime() >= lastAccomplishmentDate) {

					x.getResource().forEach(res -> lastTaskForResource.put(res, x));
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
