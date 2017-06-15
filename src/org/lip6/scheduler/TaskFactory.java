package org.lip6.scheduler;

import java.util.List;
import java.util.function.BiFunction;

public class TaskFactory {

	public static Task getTask(int taskID, int planID, String planName, int resourceID, int releaseTime, int dueDate,
			int processingTime, int planPriority, List<Integer> predecessors) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		if (releaseTime >= dueDate) {
			throw new IllegalArgumentException("Invalid release date/due date");
		}

		if (predecessors.contains(taskID)) {
			throw new IllegalArgumentException("Predecessor list can not contains the same ID of the task to create");
		}

		return new TaskImpl(taskID, planID, planName, resourceID, releaseTime, dueDate, processingTime, planPriority,
				predecessors);
	}

	public static Task getTask(int taskID, int planID, String planName, int resourceID, int releaseTime, int dueDate,
			int processingTime, int planPriority, List<Integer> predecessors,
			BiFunction<Integer, Integer, Integer> processingTimeFunction) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		if (releaseTime >= dueDate) {
			throw new IllegalArgumentException("Invalid release date/due date");
		}

		if (predecessors.contains(taskID)) {
			throw new IllegalArgumentException("Predecessor list can not contains the same ID of the task to create");
		}

		Task task = new TaskImpl(taskID, planID, planName, resourceID, releaseTime, dueDate, processingTime,
				planPriority, predecessors);

		task.setProcessingTimeFunction(processingTimeFunction);

		return task;

	}

}
