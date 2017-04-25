package org.lip6.scheduler;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

public interface Task {

	int getResourceID();

	int getTaskID();

	int getDueDate();

	int getPlanID();

	int getPlanPriority();

	int getReleaseTime();

	int getProcessingTime();

	List<ImmutablePair<Integer, Integer>> getPredecessors();

	boolean hasPredecessor(int planID, int taskID);

	String toHTMLString();

	String toHTMLString(String textColor);

	Object clone() throws CloneNotSupportedException;
}