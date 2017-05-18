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

	int getPriority();

	int getInversePriority();

	float getScore();

	void setScore(float value);

	void setInversePriority(int maxPriority);

	int getExecutionTime();

	int getNumberOfTasks();

	// void updateTask(ExecutableNode t);

	boolean isSchedulable();

	void setSchedulable(boolean schedulable);
}
