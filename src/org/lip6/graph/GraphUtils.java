package org.lip6.graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.lip6.scheduler.ExecutableNode;
import org.lip6.scheduler.Plan;

public class GraphUtils {

	public static Map<Integer, LinkedList<AdjListNode>> getAdjacencyList(List<ExecutableNode> plans) {
		Map<Integer, LinkedList<AdjListNode>> adj = new HashMap<>();
		List<Integer> planIDs = plans.stream().map(x -> x.getID()).collect(Collectors.toList());
		for (Integer i : planIDs) {
			adj.put(i, new LinkedList<AdjListNode>());
			ExecutableNode pi = plans.stream().filter(x -> x.getID() == i).findFirst().get();

			for (Integer successor : pi.getSuccessors()) {
				Optional<ExecutableNode> ops = plans.stream().filter(x -> x.getID() == successor).findFirst();
				if (ops.isPresent()) {
					ExecutableNode ps = ops.get();
					AdjListNode node = new AdjListNode(ps, ((Plan)pi).getExecutionTime() + ((Plan)pi).getExecutionTime());

					if (!adj.get(i).contains(node)) {
						adj.get(i).add(node);// Add v to u's list
					}
				}

			}
		}

		return adj;
	}

	public static void printAdjacencyList(Map<Integer, LinkedList<AdjListNode>> adj) {
		for (Integer i : adj.keySet()) {
			System.err.println(Integer.toString(i) + "-> "
					+ adj.get(i).stream().map(x -> x.toString()).collect(Collectors.joining(",")));
		}
	}
}
