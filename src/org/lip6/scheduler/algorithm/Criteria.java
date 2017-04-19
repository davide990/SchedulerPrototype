package org.lip6.scheduler.algorithm;

import java.util.function.Function;

import org.lip6.scheduler.Plan;

public class Criteria {
	private final Function<Plan, Integer> criteriaFunc;
	private final float weight;

	public Criteria(Function<Plan, Integer> criteriaFunc, float weight) {
		this.criteriaFunc = criteriaFunc;
		this.weight = weight;
	}

	public Function<Plan, Integer> getCriteriaFunc() {
		return criteriaFunc;
	}

	public float getWeight() {
		return weight;
	}

}
