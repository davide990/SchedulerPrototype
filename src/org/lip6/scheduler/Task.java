package org.lip6.scheduler;

import java.util.List;

public interface Task {

	int getResourceID();

	int getID();

	int getDueDate();

	int getPlanID();

	int getPlanPriority();

	int getReleaseTime();

	int getProcessingTime();

	List<Integer> getPredecessors();

	boolean hasPredecessor(int taskID);

	List<Integer> getSuccessors();

	boolean hasSuccessor(int taskID);

	void addSuccessor(int taskID);

	String toHTMLString();

	String toHTMLString(String textColor);
	
	Object clone() throws CloneNotSupportedException;
}