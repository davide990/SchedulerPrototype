package org.lip6.scheduler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Task implements Executable {

	final int taskID;
	final int planID;
	int releaseTime;
	final int processingTime;
	final List<Integer> predecessors;

	public Task(int taskID, int planID, int releaseTime, int processingTime, List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.releaseTime = releaseTime;
		this.processingTime = processingTime;
		this.predecessors = predecessors;
	}

	@Override
	public void execute(String[] args) {
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
		result = prime * result + ((predecessors == null) ? 0 : predecessors.hashCode());
		result = prime * result + processingTime;
		result = prime * result + releaseTime;
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
		if (predecessors == null) {
			if (other.predecessors != null)
				return false;
		} else if (!predecessors.equals(other.predecessors))
			return false;
		if (processingTime != other.processingTime)
			return false;
		if (releaseTime != other.releaseTime)
			return false;
		if (taskID != other.taskID)
			return false;
		return true;
	}

}
