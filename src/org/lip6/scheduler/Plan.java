package org.lip6.scheduler;

import java.util.Map;

public interface Plan {
	void addTask(TaskImpl t);

	Map<Integer, Task> tasks();
}
