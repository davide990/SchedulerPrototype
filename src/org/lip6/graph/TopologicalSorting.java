package org.lip6.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lip6.scheduler.ExecutableNode;

public class TopologicalSorting {

	/**
	 * Do a Breadth-first search to discover the frontiers value for each node
	 * in the precedence graph.
	 * 
	 * @param nodes
	 * @return a stack of pairs <b>(A,B)</b> where <b>A</b> is the ID of a plan,
	 *         and <b>B</b> is the frontiers which <b>A</b> belongs to according
	 *         to the precedences graph.
	 */
	@Deprecated
	private static Stack<ImmutablePair<Integer, Integer>> bfs(final Collection<ExecutableNode> nodes) {
		List<Integer> visited = new ArrayList<>();
		Stack<ImmutablePair<Integer, Integer>> frontiers = new Stack<>();
		Stack<ImmutablePair<Integer, Integer>> out = new Stack<>();

		// Iterate each node, and add to the frontier those who have no
		// predecessors (so, they are root nodes)
		for (ExecutableNode p : nodes) {
			if (nodes.stream().filter(x -> x.getSuccessors().contains(p.getID())).count() == 0) {
				frontiers.push(new ImmutablePair<Integer, Integer>(p.getID(), 0));
				out.push(new ImmutablePair<Integer, Integer>(p.getID(), 0));
			}
		}

		// iterate until there is a node not visited yet
		while (!frontiers.isEmpty()) {
			ImmutablePair<Integer, Integer> last = frontiers.pop();
			ExecutableNode v = nodes.stream().filter(x -> x.getID() == last.left).findFirst().get();

			if (visited.contains(v.getID())) {
				continue;
			}

			visited.add(v.getID());
			for (Integer succ : v.getSuccessors()) {
				if (!visited.contains(succ)) {
					frontiers.push(new ImmutablePair<Integer, Integer>(succ, last.right + 1));
					out.push(new ImmutablePair<Integer, Integer>(succ, last.right + 1));
				}
			}
		}

		return out;
	}

	/**
	 * returns a stack of pairs(A,B) where A is a plan and B the index of its
	 * frontier. The element on the stack are sorted according to the
	 * topological sorting of the plans/tasks.
	 * 
	 * @param nodes
	 *            a collection of plans <b>or</b> tasks
	 */
	@Deprecated
	public static Stack<ImmutablePair<Integer, Integer>> topologicalSort_old(final Collection<ExecutableNode> nodes) {
		// Check if exists at least one node which has no predecessor, otherwise
		// we have a cycle in the precedence graph
		Optional<ExecutableNode> root = Optional.empty();
		for (ExecutableNode p : nodes) {
			if (nodes.stream().filter(x -> x.getSuccessors().contains(p.getID())).count() == 0) {
				root = Optional.of(p);
				break;
			}
		}
		// If such node is not found, throw an exception (we have a cycle!)
		if (!root.isPresent()) {
			throw new IllegalArgumentException("Wrong plans precedences. Cycle found in precedences.");
		}

		// Do a BFS visit to get the frontier which each plan belongs to in the
		// precedences graph.
		// Stack<ImmutablePair<Integer, Integer>> frontiers = bfs(nodes);

		// Sort topologically the plans
		// Stack<Integer> topologicalSorting = doTopologicalSort(new
		// ArrayList<>(nodes));

		// Stack<ImmutablePair<Integer, Integer>> topologicalSorting =
		// doTopologicalSort(new ArrayList<>(nodes));

		// System.err.println(topologicalSorting.stream().map(x->Integer.toString(x)).collect(Collectors.joining(",")));

		// Prepare a stack containing the topologically sorted plans togheter
		// with the frontier they belongs to (the frontier is used when sorting
		// plans with the same priority value).
		Stack<ImmutablePair<Integer, Integer>> out = new Stack<>();

		/*
		 * for (Integer plan :
		 * topologicalSorting.stream().map(x->x.left).collect(Collectors.toList(
		 * ))) { int frontier = frontiers.stream().filter(x -> x.left ==
		 * plan).findFirst().get().right; out.push(new ImmutablePair<Integer,
		 * Integer>(plan, frontier)); }
		 */
		return out;
	}

	/**
	 * Topological sorting of the plans. It returns a stack of pairs
	 * <b>(A,B)</b> where <b>A</b> is the index of a plan, and <b>B</b> is the
	 * index of the frontier the plan A belongs to. The elements on the stack
	 * are topologically sorted.
	 * 
	 * @see Cormen Cormen et al.(2001), chapter 22.
	 */
	// private static Stack<Integer> doTopologicalSort(final
	// List<ExecutableNode> plans) {
	@Deprecated
	private static Stack<ImmutablePair<Integer, Integer>> doTopologicalSort_old(final List<ExecutableNode> plans) {
		Stack<Integer> stack = new Stack<>();

		Stack<ImmutablePair<Integer, Integer>> stackFrontier = new Stack<>();

		List<Integer> visitedPlans = new ArrayList<>();

		// Call the recursive helper function to store Topological Sort starting
		// from all vertices one by one
		for (int i = 0; i < plans.size(); i++) {
			if (!visitedPlans.contains(plans.get(i).getID())) {
				//topologicalSortUtil(plans.get(i), plans, visitedPlans, stack, 0);
			}
		}
		// return stack;
		return stackFrontier;
	}

	public static Stack<ImmutablePair<Integer, Integer>> topologicalSort(final List<ExecutableNode> plans) {
		Stack<ImmutablePair<Integer, Integer>> stack = new Stack<>();
		List<Integer> visitedPlans = new ArrayList<>();
		List<ExecutableNode> roots = new ArrayList<>();
		for (ExecutableNode plan : plans) {
			if (plans.stream().filter(x -> x.getSuccessors().contains(plan.getID())).count() == 0) {
				roots.add(plan);
			}
		}

		for (ExecutableNode node : roots) {
			topologicalSortUtil(node, plans, visitedPlans, stack, 0);
		}

		return stack;
	}

	/**
	 * Utility method for {@link topologicalSort}
	 * 
	 * @param plan
	 * @param plans
	 * @param visited
	 * @param stack
	 */
	private static void topologicalSortUtil(ExecutableNode plan, final List<ExecutableNode> plans,
			List<Integer> visited, Stack<ImmutablePair<Integer, Integer>> stack, int frontier) {
		// Mark the current node as visited.
		visited.add(plan.getID());

		for (Integer successor : plan.getSuccessors()) {
			if (visited.contains(successor)) {
				continue;
			}
			Optional<ExecutableNode> s = plans.stream().filter(x -> x.getID() == successor).findFirst();
			if (s.isPresent()) {
				topologicalSortUtil(s.get(), plans, visited, stack, frontier + 1);
			}

		}

		// Push current vertex to stack which stores result
		stack.push(new ImmutablePair<Integer, Integer>(plan.getID(), frontier));
	}
}
