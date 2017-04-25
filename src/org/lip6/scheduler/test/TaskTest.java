package org.lip6.scheduler.test;

import org.junit.Test;
import org.lip6.scheduler.TaskFactory;

public class TaskTest {

	@Test(expected = IllegalArgumentException.class)
	public void testProcessingTime() {
		int processingTime = 1;
		int taskID = 0;
		int resourceID = 0;
		int planID = 0;
		int releaseTime = -1;
		int dueDate = 10;
		int planPriority = -1;
		// List<Integer> predecessors = Arrays.asList(1, 2);

		TaskFactory.getTask(taskID, planID, resourceID, releaseTime, dueDate, processingTime, planPriority, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReleaseTime() {
		int taskID = 0;
		int planID = 0;
		int resourceID = 0;
		int releaseTime = 0;
		int processingTime = -1;
		int planPriority = -1;
		int dueDate = 10;
		// List<Integer> successors = Arrays.asList(1, 2);
		TaskFactory.getTask(taskID, planID, resourceID, releaseTime, dueDate, processingTime, planPriority, null);
	}

}
