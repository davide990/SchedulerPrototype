package org.lip6.scheduler.algorithm;

import java.util.function.Function;

import org.lip6.scheduler.PlanImpl;

public class Criteria {
	private final Function<PlanImpl, Integer> criteria;
	private final float weight;

	public Criteria(Function<PlanImpl, Integer> criteria, float weight) {
		this.criteria = criteria;
		this.weight = weight;
	}

	public Function<PlanImpl, Integer> getCriteria() {
		return criteria;
	}

	public float getWeight() {
		return weight;
	}

}
