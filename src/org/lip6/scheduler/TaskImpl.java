package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskImpl extends ExecutableNode implements Cloneable, Task {

	final int taskID;
	final int planID;
	final String planName;
	final int resourceID;
	final int releaseTime;
	final int dueDate;
	final int processingTime;
	final int planPriority;
	final List<Integer> predecessors;
	final List<Integer> successors;

	TaskImpl(int taskID, int planID, int resourceID, int releaseTime, int dueDate, int processingTime, int planPriority,
			List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.resourceID = resourceID;
		this.releaseTime = releaseTime;
		this.dueDate = dueDate;
		this.processingTime = processingTime;
		this.planPriority = planPriority;
		this.predecessors = new ArrayList<>(predecessors);
		this.successors = new ArrayList<>();
		this.planName = "";
	}

	TaskImpl(int taskID, int planID, String planName, int resourceID, int releaseTime, int dueDate, int processingTime,
			int planPriority, List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.resourceID = resourceID;
		this.releaseTime = releaseTime;
		this.dueDate = dueDate;
		this.processingTime = processingTime;
		this.planPriority = planPriority;
		this.predecessors = new ArrayList<>(predecessors);
		this.successors = new ArrayList<>();
		this.planName = planName;
	}

	@Override
	public Object clone() {
		return TaskFactory.getTask(taskID, planID, planName, resourceID, releaseTime, dueDate, processingTime,
				planPriority, predecessors);
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
	public int getID() {
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
	 * @see org.lip6.scheduler.Task#getPlanName()
	 */
	@Override
	public String getPlanName() {
		return planName;
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
	public boolean hasPredecessor(int taskID) {
		return predecessors.contains(taskID);
	}

	@Override
	public List<Integer> getSuccessors() {
		return Collections.unmodifiableList(successors);
	}

	@Override
	public boolean hasSuccessor(int taskID) {
		return successors.contains(taskID);
	}

	@Override
	public void addSuccessor(int taskID) {
		if (!successors.contains(taskID)) {
			successors.add(taskID);
		}
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

	public String toString() {
		return "(Π" + Integer.toString(planID) + ",J" + Integer.toString(taskID) + ")";
	}

	@Override
	public String toHTMLString() {
		String sup = planName;
		if (planName.equals("")) {
			sup = Integer.toString(getPlanID());
		}

		return "<html><body><center><p>J<sup>" + sup + "</sup><sub style='position: relative; left: -.5em;'>" + getID()
				+ "</sub></p></center></body></html>";

	}

	@Override
	public String toHTMLString(String textColor) {
		return "<html><body><center><p style='color:" + textColor + ";'>J<sup style='position: relative; top: -.3em;'>"
				+ getPlanID() + "</sup><sub style='position: relative; left: -.5em;'>" + getID()
				+ "</sub></p></center></body></html>";

	}

}
