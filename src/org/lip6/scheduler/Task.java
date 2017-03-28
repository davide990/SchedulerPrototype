package org.lip6.scheduler;

public class Task implements Executable {

	final int taskID;
	final int planID;
	int releaseTime;
	final int processingTime;
	final int[] successors;

	public Task(int taskID, int planID, int releaseTime, int processingTime, int[] successors) {
		this.taskID = taskID;
		this.planID = planID;
		this.releaseTime = releaseTime;
		this.processingTime = processingTime;
		this.successors = successors;
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

	public void updateReleaseTime(int deltaReleaseTime) {
		this.releaseTime += releaseTime;
	}

	public int getProcessingTime() {
		return processingTime;
	}

}
