package org.lip6.scheduler;

import java.util.List;

public interface Plan {
	/**
	 * Add a task to this plan.<br/>
	 * <br/>
	 * 
	 * <b>NOTE</b>: each time a task is added, all the tasks are sorted
	 * topologically.
	 * 
	 * @param t
	 *            the task to be added
	 */
	void addTask(Task t);

	List<Task> getTasks();

	List<Integer> getSuccessors();

	void addSuccessor(int ID);
	
	void addSyncTask(int ID);
	
	boolean hasSyncTask();
	
	List<Task> getSyncTasks();

	void removeSuccessor(int ID);

	Task getTask(int taskID);

	int getID();

	String getName();

	int getPriority();

	float getScore();

	void setScore(float value);

	int getExecutionTime();

	int getNumberOfTasks();

	boolean isSchedulable();

	void setSchedulable(boolean schedulable);
}
