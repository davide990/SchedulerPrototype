package org.lip6.scheduler;

import java.util.List;
import java.util.Map;

public interface Task {

	List<Integer> getResourcesID();

	Map<Integer, Integer> getResourceUsages();

	int getResourceUsage(int resource);

	int getLag();

	int getID();

	int getDueDate();

	int getPlanID();

	String getPlanName();

	int getPlanPriority();

	int getReleaseTime();

	int getProcessingTime();

	int getProcessingTime(int t);

	/**
	 * A map containing a time instant as key and an integer as value. The value
	 * is the Δt to be added to the processing time. The map contains value
	 * <b>only</b> for the time range [r<sub>k</sub>,d<sub>k</sub>], that is,
	 * the area of validity of the task.
	 * 
	 * @return
	 */
	Map<Integer, Integer> deltaValues();

	/**
	 * Set the Δt to be added to the processing time of this task
	 * 
	 * @param values
	 *            A map containing a time instant as key and an integer as
	 *            value. The value is the Δt to be added to the processing time.
	 */
	void setDeltaValues(Map<Integer, Integer> values);

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