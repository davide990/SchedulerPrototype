package org.lip6.scheduler.test;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.lip6.scheduler.TaskFactory;

public class TaskTest {
	// Quando creo un task,
	// la sua processing time deve essere > 0
	// la sua starting time deve essere > 0

	@Test(expected = IllegalArgumentException.class)
	public void testProcessingTime() {
		int processingTime = 1;
		int taskID = 0;
		int planID = 0;
		int releaseTime = -1;
		List<Integer> successors = Arrays.asList(1, 2);

		TaskFactory.getNonExecutableTask(taskID, planID, releaseTime, processingTime, successors);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReleaseTime() {
		int taskID = 0;
		int planID = 0;
		int releaseTime = 0;
		int processingTime = -1;
		List<Integer> successors = Arrays.asList(1, 2);
		TaskFactory.getNonExecutableTask(taskID, planID, releaseTime, processingTime, successors);
	}

}
