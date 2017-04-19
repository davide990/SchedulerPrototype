package org.lip6.scheduler;

import java.util.List;

public interface Task {

	int getResourceID();

	int getTaskID();

	int getDueDate();

	int getPlanID();
	
	int getPlanPriority();

	int getReleaseTime();

	int getProcessingTime();

	List<Integer> getPredecessors();

	String toHTMLString();
	
	String toHTMLString(String textColor);

	Object clone() throws CloneNotSupportedException;
}