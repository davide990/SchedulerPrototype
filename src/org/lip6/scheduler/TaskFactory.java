package org.lip6.scheduler;

import java.util.function.Function;

public class TaskFactory {
	public static Task getSimpleTask(int taskID, int planID, int releaseTime, int processingTime) {

		if (processingTime <= 0)
			throw new IllegalArgumentException("Processing time must be >= 0.");

		if (releaseTime < 0)
			throw new IllegalArgumentException("Release time must be > 0.");

		return new TaskImpl(taskID, planID, releaseTime, processingTime) {
			@Override
			public void execute(String[] args) {
				super.execute(args);
			}
		};
	}

	public static Task getTask(int taskID, int planID, int releaseTime, int processingTime,
			Function<String[], Void> executionFunction) {

		if (processingTime <= 0)
			throw new IllegalArgumentException("Processing time must be >= 0.");

		if (releaseTime < 0)
			throw new IllegalArgumentException("Release time must be > 0.");

		return new TaskImpl(taskID, planID, releaseTime, processingTime) {
			@Override
			public void execute(String[] args) {
				executionFunction.apply(args);
			}
		};

	}
}
