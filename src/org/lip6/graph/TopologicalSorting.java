package org.lip6.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lip6.scheduler.ExecutableNode;

public class TopologicalSorting {
	/**
	 * 
	 * returns a stack of pairs(A,B) where A is a plan and B the index of its
	 * frontier
	 * 
	 * @param plans
	 */
	public static Stack<ImmutablePair<Integer, Integer>> calculateTopologicalOrderScores(List<ExecutableNode> plans) {
		// Find the root node, that is, the node which doesn't appair as
		// successor of all the other nodes
		Optional<ExecutableNode> root = Optional.empty();
		for (ExecutableNode p : plans) {
			if (plans.stream().filter(x -> x.getSuccessors().contains(p.getID())).count() == 0) {
				root = Optional.of(p);
				break;
			}
		}

		// If such node is not found, throw an exception
		if (!root.isPresent()) {
			throw new IllegalArgumentException("Wrong plans precedences. No valid root node found.");
		}
		return topologicalSort(plans);
	}

	/**
	 * Topological sorting of the plans. It returns a stack of pairs
	 * <b>(A,B)</b> where <b>A</b> is the index of a plan, and <b>B</b> is the
	 * index of the frontier the plan A belongs to. The elements on the stack
	 * are topologically sorted.
	 * 
	 * @see Cormen Cormen et al.(2001), chapter 22.
	 */
	private static Stack<ImmutablePair<Integer, Integer>> topologicalSort(final List<ExecutableNode> plans) {
		Stack<ImmutablePair<Integer, Integer>> stack = new Stack<>();

		// Mark all the vertices as not visited
		Map<Integer, Boolean> visitedPlans = new HashMap<>();
		for (int i = 0; i < plans.size(); i++) {
			visitedPlans.put(plans.get(i).getID(), false);
		}

		// Call the recursive helper function to store Topological Sort starting
		// from all vertices one by one
		int frontierStartIndex = 0;
		for (int i = 0; i < plans.size(); i++) {
			if (visitedPlans.get(plans.get(i).getID()) == false) {
				topologicalSortUtil(plans.get(i), plans, visitedPlans, stack, frontierStartIndex);
			}
		}

		return stack;
	}

	private static void topologicalSortUtil(ExecutableNode plan, final List<ExecutableNode> plans,
			Map<Integer, Boolean> visited, Stack<ImmutablePair<Integer, Integer>> stack, int frontierIndex) {
		// Mark the current node as visited.
		visited.put(plan.getID(), true);

		frontierIndex++;
		for (Integer successor : plan.getSuccessors()) {
			if (!visited.get(successor)) {
				Optional<ExecutableNode> s = plans.stream().filter(x -> x.getID() == successor).findFirst();
				if (s.isPresent()) {
					topologicalSortUtil(s.get(), plans, visited, stack, frontierIndex);
				}
			}
		}
		// Push current vertex to stack which stores result
		stack.push(new ImmutablePair<Integer, Integer>(plan.getID(), frontierIndex));
	}

	// The function to find shortest paths from given vertex. It
	// uses recursive topologicalSortUtil() to get topological
	// sorting of given graph.
	// s -> source node/plan
	public static List<ExecutableNode> bellmanFord(ExecutableNode s, List<ExecutableNode> plans,
			final Stack<ImmutablePair<Integer, Integer>> topologicalOrder) {

		Map<Integer, Integer> predecessorMap = new HashMap<>();
		Map<Integer, Integer> planScores = new HashMap<>();
		Map<Integer, LinkedList<AdjListNode>> adj = GraphUtils.getAdjacencyList(plans);

		GraphUtils.printAdjacencyList(adj);

		Stack<ImmutablePair<Integer, Integer>> stack = new Stack<>();
		int frontierIndex = 0;

		// dist -> distances entre le noeud s et tous les autre noeuds
		Map<Integer, Integer> dist = new HashMap<>();
		Map<Integer, Boolean> visited = new HashMap<>();
		List<Integer> planIDs = plans.stream().map(x -> x.getID()).collect(Collectors.toList());

		for (Integer i : planIDs) {
			visited.put(i, false);
			predecessorMap.put(i, -1);
		}

		// Call the recursive helper function to store Topological
		// Sort starting from all vertices one by one
		for (Integer i : planIDs) {
			if (visited.get(i) == false) {

				Optional<ExecutableNode> p = plans.stream().filter(x -> x.getID() == i).findFirst();
				if (p.isPresent()) {
					topologicalSortUtil(p.get(), plans, visited, stack, frontierIndex);
				}
			}
		}
		// Initialize distances to all vertices as infinite and
		// distance to source as 0
		for (int i : planIDs) {
			dist.put(i, Integer.MIN_VALUE);
			planScores.put(i, 0);
		}
		dist.put(s.getID(), 0);

		// Process vertices in topological order
		while (!stack.empty()) {
			// Get the next vertex from topological order
			int u = (int) stack.pop().left;

			// Update distances of all adjacent vertices
			if (dist.get(u) != Integer.MIN_VALUE) {
				for (AdjListNode i : adj.get(u)) {
					if (dist.get(i.getV().getID()) < dist.get(u) + i.getWeight()) {
						dist.put(i.getV().getID(), dist.get(u) + i.getWeight());
						predecessorMap.put(i.getV().getID(), u);
					}
				}
			}
		}

		// Calculate the longest paths from the source node to every node in the
		// graph
		for (int i = planIDs.size() - 1; i >= 0; i--) {
			int plan = planIDs.get(i);
			Integer predecessor = null;
			int stepNum = 0;
			while ((predecessor = predecessorMap.get(plan)) != -1) {
				planScores.put(predecessor, Math.max(++stepNum, planScores.get(predecessor)));
				plan = predecessor;
			}
		}

		return sortPlansByDistance(plans, planScores, topologicalOrder);
	}

	/**
	 * Given a set of plans and its topological sorting, and a set of score for
	 * each plan (that is, the distance between each node and the source node),
	 * this algorithm returns a sorted set of plans according to the distances
	 * between their nodes and the source node
	 * 
	 * @param plans
	 *            the set of plans
	 * @param planScores
	 *            a map containing a plan ID as key, and as value the distance
	 *            between the plan and the source node
	 * @param topologicalOrder
	 *            the topological sorting for the set of plans
	 */
	private static List<ExecutableNode> sortPlansByDistance(List<ExecutableNode> plans,
			Map<Integer, Integer> planScores, final Stack<ImmutablePair<Integer, Integer>> topologicalOrder) {
		List<Integer> frontiers = topologicalOrder.stream().map(x -> x.right).sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());
		Stack<Integer> sortedPlans = new Stack<>();
		List<Integer> visitedFrontiers = new ArrayList<>();

		final Comparator<ImmutablePair<Integer, Integer>> c = new Comparator<ImmutablePair<Integer, Integer>>() {
			@Override
			public int compare(ImmutablePair<Integer, Integer> o1, ImmutablePair<Integer, Integer> o2) {
				return Integer.compare(o1.right, o2.right);
			}
		};

		for (Integer f : frontiers) {
			if (visitedFrontiers.contains(f)) {
				continue;
			}
			int count = Math.toIntExact(frontiers.stream().filter(x -> x == f).count());
			if (count == 1) {
				sortedPlans
						.push(topologicalOrder.stream().filter(x -> x.right == f).map(x -> x.left).findFirst().get());
			} else {
				List<ImmutablePair<Integer, Integer>> l = topologicalOrder.stream().filter(x -> x.right == f)
						.map(x -> new ImmutablePair<>(x.left, planScores.get(x.left))).collect(Collectors.toList());
				l.sort(c);
				for (int i = 0; i < l.size(); i++) {
					sortedPlans.push(l.get(i).left);
				}
			}
			visitedFrontiers.add(f);
		}

		List<ExecutableNode> sp = new ArrayList<>();
		while (!sortedPlans.empty()) {
			Integer id = sortedPlans.pop();
			sp.add(plans.stream().filter(x -> x.getID() == id).findFirst().get());
		}

		return sp;
	}
}
