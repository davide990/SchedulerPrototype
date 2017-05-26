package org.lip6.graph;

import org.lip6.scheduler.ExecutableNode;

public class AdjListNode {
	private final ExecutableNode v;
	private final int weight;

	public AdjListNode(ExecutableNode _v, int _w) {
		v = _v;
		weight = _w;
	}

	public ExecutableNode getV() {
		return v;
	}

	public int getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return "[v=" + v.getID() + ", weight=" + weight + "]";
	}
}