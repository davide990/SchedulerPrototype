package org.lip6.scheduler;

import java.util.List;
import java.util.function.Function;

public class TaskFactory {
	public static Task getNonExecutableTask(int taskID, int planID, int resourceID, int releaseTime, int processingTime,
			List<Integer> predecessors) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		return new Task(taskID, planID, resourceID, releaseTime, processingTime, predecessors);
	}

	public static Task getTask(int taskID, int planID, int resourceID, int releaseTime, int processingTime,
			List<Integer> predecessors, Function<String[], Void> executionFunction) {

		if (processingTime <= 0) {
			throw new IllegalArgumentException("Processing time must be >= 0.");
		}
		if (releaseTime < 0) {
			throw new IllegalArgumentException("Release time must be > 0.");
		}

		return new Task(taskID, planID, resourceID, releaseTime, processingTime, predecessors) {
			@Override
			public void execute(String[] args) {
				executionFunction.apply(args);
			}
		};

	}
}
