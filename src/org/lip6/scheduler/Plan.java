package org.lip6.scheduler;

import java.util.Collection;

public interface Plan {
	void addTask(Task t);

	Collection<Task> tasks();

	Task getTask(int taskID);

	int getID();

	int getPriority();

	int getInversePriority();

	float getScore();

	void setScore(float value);

	void setInversePriority(int maxPriority);

	int getExecutionTime();

	int getNumberOfTasks();

	void updateTask(Task t);

	boolean isSchedulable();

	void setSchedulable(boolean schedulable);
}
