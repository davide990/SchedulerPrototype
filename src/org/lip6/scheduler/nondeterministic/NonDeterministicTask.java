package org.lip6.scheduler.nondeterministic;

import java.util.List;

import org.lip6.scheduler.TaskImpl;

public class NonDeterministicTask extends TaskImpl {

	// TODO ajouter les valeurs possibles et la distr de probabilité

	
	protected NonDeterministicTask(int taskID, int planID, String planName, int resourceID, int resourceUsage,
			int releaseTime, int dueDate, int processingTime, int planPriority, List<Integer> predecessors) {
		super(taskID, planID, planName, resourceID, resourceUsage, releaseTime, dueDate, processingTime, planPriority,
				predecessors);
	}

	NonDeterministicTask(int taskID, int planID, int resourceID, int resourceUsage, int releaseTime, int dueDate,
			int processingTime, int planPriority, List<Integer> predecessors) {
		super(taskID, planID, resourceID, resourceUsage, releaseTime, dueDate, processingTime, planPriority,
				predecessors);
	}

	@Override
	public int getProcessingTime() {

		// !!!!

		return super.getProcessingTime();
	}
}
