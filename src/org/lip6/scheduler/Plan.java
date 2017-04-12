package org.lip6.scheduler;

import java.util.Collection;

public interface Plan {
	void addTask(Task t);

	Collection<Task> tasks();

	Executable getTask(int taskID);

	int getPriority();
	
	int getInversePriority();
	
	void setInversePriority(int maxPriority);

	int getExecutionTime();

	int getNumberOfTasks();

	void updateTask(Task t);

	boolean isSchedulable();

	void setSchedulable(boolean schedulable);
}
