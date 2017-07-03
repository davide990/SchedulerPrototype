package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskImpl extends ExecutableNode implements Cloneable, Task {

	// Constant C and k are used by ludovic for calculating the processing
	// time...
	int C;
	final int k = 1;

	final int taskID;
	final int planID;
	final String planName;
	final int releaseTime;
	final int dueDate;
	final int processingTime;
	final int planPriority;
	final int timeLag;
	final List<Integer> predecessors;
	final List<Integer> successors;
	final Map<Integer, Integer> resourceUsages;
	final Map<Integer, Integer> delta;

	TaskImpl(int taskID, int planID, Map<Integer, Integer> resourceUsages, int timeLag, int releaseTime, int dueDate,
			int processingTime, int planPriority, List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.resourceUsages = resourceUsages;
		this.releaseTime = releaseTime;
		this.dueDate = dueDate;
		this.processingTime = processingTime;
		this.planPriority = planPriority;
		this.predecessors = new ArrayList<>(predecessors);
		this.successors = new ArrayList<>();
		this.timeLag = timeLag;
		this.planName = "";
		this.delta = new HashMap<>();
		for (int t = releaseTime; t <= dueDate; t++) {
			delta.put(t, 0);
		}
	}

	TaskImpl(int taskID, int planID, String planName, Map<Integer, Integer> resourceUsages, int timeLag,
			int releaseTime, int dueDate, int processingTime, int planPriority, List<Integer> predecessors) {
		this.taskID = taskID;
		this.planID = planID;
		this.resourceUsages = resourceUsages;
		this.releaseTime = releaseTime;
		this.dueDate = dueDate;
		this.processingTime = processingTime;
		this.planPriority = planPriority;
		this.predecessors = new ArrayList<>(predecessors);
		this.successors = new ArrayList<>();
		this.planName = planName;
		this.timeLag = timeLag;
		this.delta = new HashMap<>();
		for (int t = releaseTime; t <= dueDate; t++) {
			delta.put(t, 0);
		}
	}

	@Override
	public void setDeltaValues(Map<Integer, Integer> values) {
		for (Integer t : values.keySet()) {
			delta.put(t, values.get(t));
		}
	}

	@Override
	public Object clone() {
		return TaskFactory.getTask(taskID, planID, planName, resourceUsages, timeLag, releaseTime, dueDate,
				processingTime, planPriority, predecessors);
	}

	@Override
	public int getID() {
		return taskID;
	}

	@Override
	public Map<Integer, Integer> getResourceUsages() {
		return resourceUsages;
	}

	@Override
	public int getPlanID() {
		return planID;
	}

	@Override
	public String getPlanName() {
		return planName;
	}

	@Override
	public int getReleaseTime() {
		return releaseTime;
	}

	@Override
	public int getDueDate() {
		return dueDate;
	}

	@Override
	public int getProcessingTime() {
		return processingTime;
	}

	@Override
	public int getProcessingTime(int t) {
		return k * delta.get(t) + processingTime;
	}

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
		return toHTMLString(true);
	}

	@Override
	public String toHTMLString(boolean printPlanName) {
		if (printPlanName) {
			String sup = planName;
			if (planName.equals("")) {
				sup = Integer.toString(getPlanID());
				return "<html><body><center><p>J<sup>" + sup + "</sup><sub style='position: relative; left: -.5em;'>"
						+ getID() + "</sub></p></center></body></html>";
			}
		}

		return "<html><body><center><p>J<sup>" + getPlanID() + "</sup><sub style='position: relative; left: -.5em;'>"
				+ getID();
	}

	@Override
	public String toHTMLString(String textColor) {
		return "<html><body><center><p style='color:" + textColor + ";'>J<sup style='position: relative; top: -.3em;'>"
				+ getPlanID() + "</sup><sub style='position: relative; left: -.5em;'>" + getID()
				+ "</sub></p></center></body></html>";

	}

	@Override
	public List<Integer> getResourcesID() {
		return resourceUsages.keySet().stream().collect(Collectors.toList());
	}

	@Override
	public int getResourceUsage(int resource) {
		return resourceUsages.getOrDefault(resource, 1);
	}

	@Override
	public int getLag() {
		return timeLag;
	}

	@Override
	public Map<Integer, Integer> deltaValues() {
		return Collections.unmodifiableMap(delta);
	}

}
