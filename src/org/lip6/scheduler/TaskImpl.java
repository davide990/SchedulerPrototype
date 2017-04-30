package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class TaskImpl implements Executable, Cloneable, Task {

	final int taskID;
	final int planID;
	final int resourceID;
	final int releaseTime;
	final int dueDate;
	final int processingTime;
	final int planPriority;
	/**
	 * A map containing as key the ID of a plan and as value a list of tasks
	 * which precedes this task.
	 */
	// final List<ImmutablePair<Integer, Integer>> predecessors;
	final List<Integer> predecessors;

	TaskImpl(int taskID, int planID, int resourceID, int releaseTime, int dueDate, int processingTime, int planPriority,
			List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.resourceID = resourceID;
		this.releaseTime = releaseTime;
		this.dueDate = dueDate;
		this.processingTime = processingTime;
		this.planPriority = planPriority;
		// this.predecessors = new ArrayList<>(predecessors);
		this.predecessors = new ArrayList<>(predecessors);
	}

	@Override
	public Object clone() {
		return TaskFactory.getTask(taskID, planID, resourceID, releaseTime, dueDate, processingTime, planPriority,
				predecessors);
	}

	@Override
	public void execute(String[] args) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lip6.scheduler.Task#getResourceID()
	 */
	@Override
	public int getResourceID() {
		return resourceID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lip6.scheduler.Task#getTaskID()
	 */
	@Override
	public int getTaskID() {
		return taskID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lip6.scheduler.Task#getPlanID()
	 */
	@Override
	public int getPlanID() {
		return planID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lip6.scheduler.Task#getReleaseTime()
	 */
	@Override
	public int getReleaseTime() {
		return releaseTime;
	}

	@Override
	public int getDueDate() {
		return dueDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lip6.scheduler.Task#getProcessingTime()
	 */
	@Override
	public int getProcessingTime() {
		return processingTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.lip6.scheduler.Task#getPredecessors()
	 */
	@Override
	public List<Integer> getPredecessors() {
		return Collections.unmodifiableList(predecessors);
	}

	@Override
	public boolean hasPredecessor(int planID, int taskID) {
		return predecessors.contains(new ImmutablePair<>(planID, taskID));
	}

	@Override
	public int getPlanPriority() {
		return planPriority;
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
		TaskImpl other = (TaskImpl) obj;
		if (planID != other.planID)
			return false;
		if (taskID != other.taskID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Task [taskID=" + taskID + ", planID=" + planID + ", processingTime=" + processingTime + "]";
	}

	@Override
	public String toHTMLString() {
		return "<html><body><center><p>J<sup>" + getPlanID() + "</sup><sub style='position: relative; left: -.5em;'>"
				+ getTaskID() + "</sub></p></center></body></html>";

	}

	@Override
	public String toHTMLString(String textColor) {
		return "<html><body><center><p style='color:" + textColor + ";'>J<sup>" + getPlanID()
				+ "</sup><sub style='position: relative; left: -.5em;'>" + getTaskID()
				+ "</sub></p></center></body></html>";

	}

}
