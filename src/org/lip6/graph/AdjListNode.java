package org.lip6.graph;

import org.lip6.scheduler.Plan;

public class AdjListNode {
	private final Plan v;
	private final int weight;

	public AdjListNode(Plan _v, int _w) {
		v = _v;
		weight = _w;
	}

	public Plan getV() {
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