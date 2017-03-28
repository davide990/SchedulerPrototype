package org.lip6.scheduler;

import java.util.Map;

public interface Plan {
	void addTask(Task t);

	Map<Integer, Executable> tasks();

	Executable getTask(int taskID);

	void updateTask(Task t);
}
