package org.lip6.scheduler;

import java.util.Collection;
import java.util.List;

public interface Plan {
	void addTask(Task t);

	Collection<Task> getTasks();

	List<Integer> getSuccessors();

	void addSuccessor(int ID);

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
