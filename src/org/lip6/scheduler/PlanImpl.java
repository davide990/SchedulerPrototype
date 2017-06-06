package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lip6.graph.TopologicalSorting;

public class PlanImpl extends ExecutableNode implements Plan {

	private final int ID;
	private boolean schedulable;
	private final int priority;
	private int executionTime;
	private int startTime;
	private int endTime;
	private float planScore;
	private final String planName;
	final List<Task> tasks = new LinkedList<>();
	final List<Integer> successors;

	private PlanImpl(String name, int ID, int priority) {
		this.planScore = -1;
		this.ID = ID;
		this.priority = priority;
		this.executionTime = 0;
		this.schedulable = true;
		successors = new ArrayList<>();
		startTime = Integer.MAX_VALUE;
		endTime = Integer.MIN_VALUE;
		planName = name;
	}

	public static PlanImpl get(int ID, String name, int priority, List<Integer> successors) {
		if (priority < 0) {
			throw new IllegalArgumentException("Priority must be >= 0.");
		}

		PlanImpl p = new PlanImpl(name, ID, priority);
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
	public String getName() {
		return planName;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public float getScore() {
		return planScore;
	}

	@Override
	public void setScore(float planScore) {
		this.planScore = planScore;
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

		if (tasks.stream().filter(x -> x.getID() == task.getID()).findFirst().isPresent()) {
			throw new IllegalArgumentException("Task is already in plan");
		}

		if (t.getPlanID() != ID) {
			throw new IllegalArgumentException("Task ID is different from plan ID.");
		}

		tasks.add(t);

		// Update the execution time of this plan
		if (t.getReleaseTime() <= startTime) {
			startTime = t.getReleaseTime();
		}
		if (t.getDueDate() >= endTime) {
			endTime = t.getDueDate();
		}
		executionTime = endTime - startTime;

		// Sort the tasks topologically
		sortTasksTopologically();
	}

	/**
	 * Calculate the topological sorting for the tasks in this plan. This ensure
	 * that the precedences constraints are respected.
	 * 
	 * @param nodes
	 * @return
	 */
	private void sortTasksTopologically() {
		List<ExecutableNode> sorted = tasks.stream().map(x -> (ExecutableNode) x).collect(Collectors.toList());

		// Sort topologically the nodes
		Stack<ImmutablePair<Integer, Integer>> orderScore = TopologicalSorting.getPlansFrontiers(sorted);

		// Get the source node
		ExecutableNode source = sorted.stream().min(new Comparator<ExecutableNode>() {
			@Override
			public int compare(ExecutableNode o1, ExecutableNode o2) {
				return Integer.compare(o1.getID(), o2.getID());
			}
		}).get();

		// Execute the Bellman-Ford algorithm to get all the distances values
		// from to source node to each vertex
		List<Task> s = TopologicalSorting.bellmanFord(source, sorted, orderScore).stream().map(x -> (Task) x)
				.collect(Collectors.toList());
		Collections.reverse(s);

		tasks.clear();
		for (int i = 0; i < s.size(); i++) {
			tasks.add(s.get(i));
		}

	}

	@Override
	public Task getTask(int taskID) throws NoSuchElementException {
		return tasks.stream().filter(x -> x.getID() == taskID).findFirst().get();
	}

	@Override
	public Collection<Task> getTasks() {
		return tasks.stream().collect(Collectors.toList());

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
				+ getTasks().stream().map(Task::toString).collect(Collectors.joining("\n\t")) + "]";
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public boolean hasSuccessor(int planID) {
		return successors.contains(planID);
	}

}
