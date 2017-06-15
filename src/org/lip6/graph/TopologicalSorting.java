package org.lip6.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lip6.scheduler.ExecutableNode;

public class TopologicalSorting {

	/**
	 * Sort topologically a set of plans or tasks.
	 * 
	 * @param nodes
	 *            a set of plans or tasks
	 * @return a stack of pairs <b>(A,B)</b> where <b>A</b> is the ID of a plan,
	 *         and <b>B</b> is the frontiers which <b>A</b> belongs to according
	 *         to the precedences graph. The stack is sorted so that on top
	 *         there is always the next plan to schedule.
	 */
	public static Stack<ImmutablePair<Integer, Integer>> topologicalSort(final List<ExecutableNode> nodes) {
		Stack<ImmutablePair<Integer, Integer>> stack = new Stack<>();
		List<Integer> visitedPlans = new ArrayList<>();
		List<ExecutableNode> roots = new ArrayList<>();

		// Search for the nodes which have no predecessors
		for (ExecutableNode plan : nodes) {
			if (nodes.stream().filter(x -> x.getSuccessors().contains(plan.getID())).count() == 0) {
				roots.add(plan);
			}
		}

		// Starting from the found nodes, do the topological sorting
		for (ExecutableNode node : roots) {
			topologicalSortUtil(node, nodes, visitedPlans, stack, 0);
		}

		return stack;
	}

	/**
	 * Utility method for {@link topologicalSort}
	 * 
	 * @param node
	 *            a plan or a task
	 */
	private static void topologicalSortUtil(ExecutableNode node, final List<ExecutableNode> nodes,
			List<Integer> visited, Stack<ImmutablePair<Integer, Integer>> stack, int frontier) {
		// Mark the current node as visited.
		visited.add(node.getID());

		// Iterate for each successor in the current node
		for (Integer successor : node.getSuccessors()) {
			// Continue if the current node has been already visited
			if (visited.contains(successor)) {
				continue;
			}

			Optional<ExecutableNode> s = nodes.stream().filter(x -> x.getID() == successor).findFirst();
			if (s.isPresent()) {
				topologicalSortUtil(s.get(), nodes, visited, stack, frontier + 1);
			}

		}

		// Push current node to stack together with the frontier value
		stack.push(new ImmutablePair<Integer, Integer>(node.getID(), frontier));
	}
}
