package org.lip6.scheduler;

import java.util.List;

/**
 * A TaskSchedule represents the assignment of a starting time s<sub>k</sub> to
 * a task. <br/>
 * The task and the resource are used to render correctly the solution in the
 * web interface.
 * 
 * @author davide
 *
 */
public class TaskSchedule implements Cloneable {
	private final Task task;
	private final int startingTime;
	private final List<Integer> resources;

	public TaskSchedule(Task task, int startingTime, List<Integer> resources) {
		if (!(task instanceof Task)) {
			throw new IllegalArgumentException("Argument is not a task");
		}

		this.task = task;
		this.startingTime = startingTime;
		this.resources = resources;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new TaskSchedule((Task) task.clone(), startingTime, resources);
	}

	public Task getTask() {
		return task;
	}

	public int getStartingTime() {
		return startingTime;
	}

	public List<Integer> getResource() {
		return resources;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((task == null) ? 0 : task.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaskSchedule other = (TaskSchedule) obj;
		if (task == null) {
			if (other.task != null)
				return false;
		} else if (!task.equals(other.task))
			return false;
		return true;
	}

}