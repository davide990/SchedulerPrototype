package org.lip6.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlanImpl implements Plan, Executable {

	private final int ID;
	private boolean schedulable;
	private final int priority;
	private int executionTime;
	private int startTime;
	private int endTime;

	final LinkedHashMap<Integer, Task> tasks = new LinkedHashMap<>();

	private final static Logger logger = Logger.getLogger(PlanImpl.class.getName());

	private PlanImpl(int ID, int priority) {
		this.ID = ID;
		this.priority = priority;
		this.executionTime = 0;
		this.schedulable = true;
		startTime = Integer.MAX_VALUE;
		endTime = Integer.MIN_VALUE;
	}

	public static PlanImpl get(int ID, int priority) {
		if (priority < 0) {
			throw new IllegalArgumentException("Priority must be >= 0.");
		}
		return new PlanImpl(ID, priority);
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public int getExecutionTime() {
		return executionTime;
	}

	@Override
	public int getNumberOfTasks() {
		return tasks.size();
	}

	@Override
	public void addTask(Task t) {
		Objects.requireNonNull(t);
		if (tasks.putIfAbsent(t.taskID, t) != null) {
			throw new IllegalArgumentException("Task is already in plan");
		}

		// Update the execution time of this plan
		if (t.getReleaseTime() <= startTime) {
			startTime = t.getReleaseTime();
		}
		if (t.getDueDate() >= endTime) {
			endTime = t.getDueDate();
		}
		executionTime = endTime - startTime;
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
	public Collection<Task> tasks() {
		return Collections.unmodifiableCollection(tasks.values());

	}

	@Override
	public boolean isSchedulable() {
		return schedulable;
	}

	@Override
	public void setSchedulable(boolean schedulable) {
		this.schedulable = schedulable;
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
