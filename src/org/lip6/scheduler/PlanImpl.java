package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.lip6.graph.TopologicalSorting;

public class PlanImpl extends ExecutableNode implements Plan {

	private final int ID;
	private boolean schedulable;
	private final int priority;
	/**
	 * The <b>*maximum*</b> execution time required by this plan. It is the
	 * number of time units employed by all the tasks of this plans in his
	 * critical path.
	 */
	private int executionTime;

	/**
	 * The minimum release date between the tasks of this plan.
	 */
	private int startTime;
	/**
	 * The maximum due date between the tasks of this plan.
	 */
	private int endTime;
	private float planScore;
	private final String planName;
	final List<Task> tasks = new LinkedList<>();
	final List<Integer> successors;
	final List<Integer> syncTasks;

	/**
	 * Private constructor. Use the static factory method instead.
	 * 
	 * @param name
	 * @param ID
	 * @param priority
	 */
	private PlanImpl(String name, int ID, int priority) {
		this.planScore = -1;
		this.ID = ID;
		this.priority = priority;
		this.executionTime = 0;
		this.schedulable = true;
		successors = new ArrayList<>();
		syncTasks = new ArrayList<>();
		startTime = Integer.MAX_VALUE;
		endTime = Integer.MIN_VALUE;
		planName = name;
	}

	/**
	 * Static factory method for creating instances of PlanImpl
	 * 
	 * @param ID
	 * @param name
	 * @param priority
	 * @param successors
	 * @return
	 */
	public static PlanImpl get(int ID, String name, int priority, List<Integer> successors) {
		if (priority < 0) {
			throw new IllegalArgumentException("Priority must be >= 0.");
		}

		PlanImpl p = new PlanImpl(name, ID, priority);

		// Prevent adding duplicates. I could have used Set to prevent
		// duplicate, but Set is an unsorted data type in java.
		p.successors.addAll(successors.stream().distinct().collect(Collectors.toList()));
		return p;
	}

	/**
	 * Static factory method for creating instances of PlanImpl
	 * 
	 * @param ID
	 * @param name
	 * @param priority
	 * @param successors
	 * @return
	 */
	public static PlanImpl get(int ID, String name, int priority, List<Integer> successors, List<Integer> syncTasks) {
		if (priority < 0) {
			throw new IllegalArgumentException("Priority must be >= 0.");
		}

		PlanImpl p = new PlanImpl(name, ID, priority);

		// Prevent adding duplicates. I could have used Set to prevent
		// duplicate, but Set is an unsorted data type in java.
		p.successors.addAll(successors.stream().distinct().collect(Collectors.toList()));
		
		
		syncTasks.forEach(s->p.addSyncTask(s));
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
	public void addSyncTask(int ID) {
		if (!syncTasks.contains(ID)) {
			syncTasks.add(ID);
		}
	}

	@Override
	public boolean hasSyncTask() {
		return syncTasks.size() > 0;
	}

	@Override
	public List<Task> getSyncTasks() {
		return tasks.stream().filter(x -> syncTasks.contains(x.getID())).collect(Collectors.toList());
	}

	@Override
	public void addTask(Task t) {
		// Task can not be null!
		Objects.requireNonNull(t, "Task can not be null.");

		// Check if t is already inside this plan
		if (tasks.stream().filter(x -> x.getID() == t.getID()).findFirst().isPresent()) {
			throw new IllegalArgumentException("Task is already in plan");
		}

		// I'm placing the task in the right plan?
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

		// Sort the tasks topologically to ensure that the precedences are
		// respected.
		sortTasksTopologically();
	}

	/**
	 * Calculate the topological sorting for the tasks in this plan. This ensure
	 * that the precedences between tasks constraints are respected.
	 * 
	 * @param nodes
	 * @return
	 */
	private void sortTasksTopologically() {

		// Get the tasks as ExecutableNode (remember that the topological sort
		// works for both plans and tasks)
		List<ExecutableNode> nodes = tasks.stream().map(x -> (ExecutableNode) x).collect(Collectors.toList());

		// Get the sorted list of tasks IDs
		List<Integer> sortedIDs = TopologicalSorting.topologicalSort(nodes).stream().map(x -> x.left)
				.collect(Collectors.toList());

		// Reorder the tasks in the list according to the found topological
		// sorting
		tasks.clear();
		for (Integer id : sortedIDs) {
			tasks.add(nodes.stream().filter(x -> x.getID() == id).map(x -> (Task) x).findFirst().get());
		}
	}

	@Override
	public Task getTask(int taskID) throws NoSuchElementException {
		return tasks.stream().filter(x -> x.getID() == taskID).findFirst().get();
	}

	@Override
	public List<Task> getTasks() {
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
