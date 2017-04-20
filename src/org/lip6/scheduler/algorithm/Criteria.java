package org.lip6.scheduler.algorithm;

import java.util.function.Function;

import org.lip6.scheduler.Plan;

public class Criteria {
	private final Function<Plan, Integer> criteriaFunc;
	private final String description;
	private float weight;

	public Criteria(Function<Plan, Integer> criteriaFunc, String description, float weight) {
		this.criteriaFunc = criteriaFunc;
		this.description = description;
		this.weight = weight;
	}

	public Criteria(Function<Plan, Integer> criteriaFunc, String description) {
		this.criteriaFunc = criteriaFunc;
		this.description = description;
		weight = 0;
	}

	public Function<Plan, Integer> getCriteriaFunc() {
		return criteriaFunc;
	}

	public String getDescription() {
		return description;
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

}
