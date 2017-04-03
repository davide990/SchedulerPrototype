package org.lip6.scheduler;

import java.util.Map;

public interface Plan {
	void addTask(Task t);

	Map<Integer, Executable> tasks();

	Executable getTask(int taskID);

	int getPriority();

	int getExecutionTime();

	int getNumberOfTasks();

	void updateTask(Task t);

	boolean isSchedulable();

	void setSchedulable(boolean schedulable);
}
