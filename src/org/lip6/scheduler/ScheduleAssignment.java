package org.lip6.scheduler;

public class ScheduleAssignment {
	private final Task task;
	private final int startingTime;
	private final int resource;

	public ScheduleAssignment(Task task, int startingTime, int resource) {
		this.task = task;
		this.startingTime = startingTime;
		this.resource = resource;
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
}