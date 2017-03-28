package org.lip6.scheduler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PlanImpl implements Plan {

	final LinkedHashMap<Integer, Task> tasks = new LinkedHashMap<>();

	@Override
	public void addTask(TaskImpl t) {
		Objects.requireNonNull(t);

		tasks.putIfAbsent(t.taskID, t);
	}

	@Override
	public Map<Integer, Task> tasks() {
		return Collections.unmodifiableMap(tasks);

	}

}
