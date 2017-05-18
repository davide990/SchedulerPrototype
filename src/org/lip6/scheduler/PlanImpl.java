package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlanImpl extends ExecutableNode implements Plan {

	private final int ID;
	private boolean schedulable;
	private final int priority;
	private int inversePriority;
	private int executionTime;
	private int startTime;
	private int endTime;
	private float planScore;

	final LinkedHashMap<Integer, Task> tasks = new LinkedHashMap<>();
	final List<Integer> successors;

	private final static Logger logger = Logger.getLogger(PlanImpl.class.getName());

	private PlanImpl(int ID, int priority) {
		this.planScore = -1;
		this.ID = ID;
		this.priority = priority;
		this.executionTime = 0;
		this.schedulable = true;
		successors = new ArrayList<>();
		startTime = Integer.MAX_VALUE;
		endTime = Integer.MIN_VALUE;
	}

	public static PlanImpl get(int ID, int priority) {
		if (priority < 0) {
			throw new IllegalArgumentException("Priority must be >= 0.");
		}
		return new PlanImpl(ID, priority);
	}

	public static PlanImpl get(int ID, int priority, List<Integer> successors) {
		if (priority < 0) {
			throw new IllegalArgumentException("Priority must be >= 0.");
		}

		PlanImpl p = new PlanImpl(ID, priority);
		// Prevent adding duplicates
		p.successors.addAll(successors.stream().distinct().collect(Collectors.toList()));
		return p;
	}

	@Override
	public List<Integer> getSuccessors() {
		return Collections.unmodifiableList(successors);
	}

	@Override
	public int getID() {
		return ID;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public int getInversePriority() {
		return inversePriority;
	}

	@Override
	public float getScore() {
		return planScore;
	}

	@Override
	public void setScore(float planScore) {
		this.planScore = planScore;
	}

	/**
	 * Set the inverse priority for this plan.
	 * 
	 * @param maxPriority
	 *            the highest priority of the plan contained in the same set
	 *            which contains this plan.
	 */
	@Override
	public void setInversePriority(int maxPriority) {
		inversePriority = 1 + maxPriority - getPriority();
	}

	@Override
	public void removeSuccessor(int ID) {
		if (successors.contains(ID)) {
			successors.remove(successors.indexOf(ID));
		}

	}

	@Override
	public void addSuccessor(int ID) {
		successors.add(ID);
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
	public void addTask(Task task) {

		if (!(task instanceof Task)) {
			throw new IllegalArgumentException("Argument is not a task");
		}

		Task t = (Task) task;

		Objects.requireNonNull(t);
		if (tasks.putIfAbsent(task.getID(), t) != null) {
			throw new IllegalArgumentException("Task is already in plan");
		}

		if (t.getPlanID() != ID) {
			throw new IllegalArgumentException("Task ID is different from plan ID.");
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
	public Task getTask(int taskID) {
		return tasks.getOrDefault(taskID, null);
	}

	@Override
	public void updateTask(ExecutableNode task) {
		if (!(task instanceof Task)) {
			throw new IllegalArgumentException("Argument is not a task");
		}
		Task t = (Task) task;
		tasks.replace(task.getID(), t);
	}

	@Override
	public Collection<Task> getTasks() {
		return tasks.values();

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
	public String toString() {
		return "Plan [ID=" + ID + ", tasks=[\n\t"
				+ tasks.values().stream().map(Task::toString).collect(Collectors.joining("\n\t")) + "]";
	}

	@Override
	public boolean hasSuccessor(int taskID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException("Not supported");
	}

}
