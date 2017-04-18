package org.lip6.scheduler.algorithm;

import java.util.function.Function;

import org.lip6.scheduler.PlanImpl;

public class Criteria {
	private final Function<PlanImpl, Integer> criteriaFunc;
	private final float weight;

	public Criteria(Function<PlanImpl, Integer> criteriaFunc, float weight) {
		this.criteriaFunc = criteriaFunc;
		this.weight = weight;
	}

	public Function<PlanImpl, Integer> getCriteriaFunc() {
		return criteriaFunc;
	}

	public float getWeight() {
		return weight;
	}

}
