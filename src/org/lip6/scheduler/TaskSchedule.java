package org.lip6.scheduler;

public class TaskSchedule implements Cloneable {
	private final Task task;
	private final int startingTime;
	private final int resource;

	public TaskSchedule(Task task, int startingTime, int resource) {
		if (!(task instanceof Task)) {
			throw new IllegalArgumentException("Argument is not a task");
		}

		this.task = task;
		this.startingTime = startingTime;
		this.resource = resource;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new TaskSchedule((Task)task.clone(), startingTime, resource);
	}

	public Task getTask() {
		return task;
	}

	public int getStartingTime() {
		return startingTime;
	}

	public int getResource() {
		return resource;
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