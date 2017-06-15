package org.lip6.scheduler;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Task {

	int getResourceID();

	int getID();

	int getDueDate();

	int getPlanID();

	String getPlanName();

	int getPlanPriority();

	int getReleaseTime();

	int getProcessingTime();

	/**
	 * Define the function used to modeling the evolution of the processing time
	 * along the time axis. This is motivated by the fact that the processing
	 * time of a task may vary depending on when it is executed.
	 * 
	 * @param func
	 *            a function that takes the current time instant as parameter,
	 *            and a void type as output
	 */
	void setProcessingTimeFunction(BiFunction<Integer, Integer, Integer> func);

	/**
	 * Update the processing time of this task according to the **evolution
	 * function**
	 * 
	 * @param currentTimeInstant
	 */
	void updateProcessingTime(int currentTimeInstant);

	List<Integer> getPredecessors();

	boolean hasPredecessor(int taskID);

	List<Integer> getSuccessors();

	boolean hasSuccessor(int taskID);

	void addSuccessor(int taskID);

	String toHTMLString();
	
	String toHTMLString(boolean printPlanName);

	String toHTMLString(String textColor);

	Object clone() throws CloneNotSupportedException;
}