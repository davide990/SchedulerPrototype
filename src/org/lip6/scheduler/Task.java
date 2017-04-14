package org.lip6.scheduler;

import java.util.List;

public interface Task {

	int getResourceID();

	int getTaskID();

	int getDueDate();

	int getPlanID();

	int getReleaseTime();

	int getProcessingTime();

	List<Integer> getPredecessors();

	String toHTMLString();

	Object clone() throws CloneNotSupportedException;
}