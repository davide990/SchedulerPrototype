package org.lip6.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TaskFactory {

	public static TaskImpl getTask(int taskID, int planID, int resourceID, int releaseTime, int processingTime,
			int planPriority) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		return new TaskImpl(taskID, planID, resourceID, releaseTime, processingTime, planPriority, new ArrayList<>());
	}

	public static Task getTask(int taskID, int planID, int resourceID, int releaseTime, int processingTime,
			int planPriority, List<Integer> predecessors) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		return new TaskImpl(taskID, planID, resourceID, releaseTime, processingTime, planPriority, predecessors);
	}

	public static TaskImpl getTask(int taskID, int planID, int resourceID, int releaseTime, int processingTime,
			int planPriority, List<Integer> predecessors, Function<String[], Void> executionFunction) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		return new TaskImpl(taskID, planID, resourceID, releaseTime, processingTime, planPriority, predecessors) {
			@Override
			public void execute(String[] args) {
				executionFunction.apply(args);
			}
		};

	}
}
