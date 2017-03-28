package org.lip6.scheduler;

import java.util.function.Function;

public class TaskFactory {
	public static Task getSimpleTask(int taskID, int planID, int releaseTime, int processingTime, int[] successors) {

		if (processingTime <= 0)
			throw new IllegalArgumentException("Processing time must be >= 0.");

		if (releaseTime < 0)
			throw new IllegalArgumentException("Release time must be > 0.");

		return new Task(taskID, planID, releaseTime, processingTime, successors) {
			@Override
			public void execute(String[] args) {
				throw new UnsupportedOperationException("Not available");
			}
		};
	}

	public static Task getTask(int taskID, int planID, int releaseTime, int processingTime, int[] successors,
			Function<String[], Void> executionFunction) {

		if (processingTime <= 0)
			throw new IllegalArgumentException("Processing time must be >= 0.");

		if (releaseTime < 0)
			throw new IllegalArgumentException("Release time must be > 0.");

		return new Task(taskID, planID, releaseTime, processingTime, successors) {
			@Override
			public void execute(String[] args) {
				executionFunction.apply(args);
			}
		};

	}
}
