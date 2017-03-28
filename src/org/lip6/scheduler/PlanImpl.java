package org.lip6.scheduler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlanImpl implements Plan, Executable {

	private final int ID;

	final LinkedHashMap<Integer, Executable> tasks = new LinkedHashMap<>();

	private final static Logger logger = Logger.getLogger(PlanImpl.class.getName());

	public PlanImpl(int ID) {
		this.ID = ID;
	}

	@Override
	public void addTask(Task t) {
		Objects.requireNonNull(t);
		if (tasks.putIfAbsent(t.taskID, t) == null) {
			throw new IllegalArgumentException("Task is already in plan");
		}
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

	@Override
	public void execute(String[] args) {
		logger.log(Level.FINEST, "Executing plan [" + ID + "]");
		tasks.forEach((k, v) -> {
			logger.log(Level.FINEST, "Executing task [" + k + "]");
			v.execute(args);
		});
	}

}
