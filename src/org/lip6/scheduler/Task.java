package org.lip6.scheduler;

import java.util.Collections;
import java.util.List;

public class Task implements Executable, Cloneable {

	final int taskID;
	final int planID;
	final int resourceID;
	int releaseTime;
	final int processingTime;
	final List<Integer> predecessors;

	public Task(int taskID, int planID, int resourceID, int releaseTime, int processingTime,
			List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.resourceID = resourceID;
		this.releaseTime = releaseTime;
		this.processingTime = processingTime;
		this.predecessors = predecessors;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return TaskFactory.getTask(taskID, planID, resourceID, releaseTime, processingTime, predecessors);
	}

	@Override
	public void execute(String[] args) {
	}

	public int getResourceID() {
		return resourceID;
	}

	public int getTaskID() {
		return taskID;
	}

	public int getPlanID() {
		return planID;
	}

	public int getReleaseTime() {
		return releaseTime;
	}

	public int getDueDate() {
		return releaseTime + processingTime;
	}

	public void updateReleaseTime(int deltaReleaseTime) {
		this.releaseTime += releaseTime;
	}

	public int getProcessingTime() {
		return processingTime;
	}

	public List<Integer> getPredecessors() {
		return Collections.unmodifiableList(predecessors);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + planID;
		result = prime * result + taskID;
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
		Task other = (Task) obj;
		if (planID != other.planID)
			return false;
		if (taskID != other.taskID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Task [taskID=" + taskID + ", planID=" + planID + "]";
	}

}
