package org.lip6.scheduler.graph;

import java.util.Objects;

import org.lip6.scheduler.Schedule;

public class GraphNode {

	private final GraphNode previous;
	private GraphNode successor;
	private final Schedule solution;
	private int cost;

	private GraphNode(GraphNode previous, Schedule solution, int cost) {
		this.previous = previous;
		this.solution = solution;
		this.cost = cost;
	}

	public static GraphNode get(GraphNode previous, Schedule solution, int cost) {
		Objects.requireNonNull(solution);

		if (cost < 0) {
			throw new IllegalArgumentException("Cost cannot be less than 0.");
		}

		GraphNode n = new GraphNode(previous, solution, cost);
		previous.setSuccessor(n);
		return n;
	}

	public void setSuccessor(GraphNode successor) {
		this.successor = successor;
	}

	public GraphNode getPrevious() {
		return previous;
	}

	public GraphNode getSuccessor() {
		return successor;
	}

	public Schedule getSolution() {
		return solution;
	}

	public int getCost() {
		return cost;
	}

}
