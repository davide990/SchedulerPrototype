package org.lip6.scheduler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PlanImpl implements Plan {

	final LinkedHashMap<Integer, Executable> tasks = new LinkedHashMap<>();

	@Override
	public void addTask(Task t) {
		Objects.requireNonNull(t);

		tasks.putIfAbsent(t.taskID, t);
	}

	@Override
	public Executable getTask(int taskID) {
		return tasks.getOrDefault(taskID, null);
	}

	@Override
	public void updateTask(Task t) {
		tasks.replace(t.taskID, t);
	}

	@Override
	public Map<Integer, Executable> tasks() {
		return Collections.unmodifiableMap(tasks);

	}

}
