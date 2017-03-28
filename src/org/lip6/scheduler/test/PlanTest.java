package org.lip6.scheduler.test;

import org.junit.Test;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class PlanTest {

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateTask() {
		PlanImpl plan = new PlanImpl(0);
		Task t1 = TaskFactory.getTask(0, 0, 0, 1, new int[] { 1, 2 }, (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

		Task t2 = TaskFactory.getTask(0, 0, 0, 1, new int[] { 1, 2 }, (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

		plan.addTask(t1);
		plan.addTask(t2);
	}

	@Test
	public void testInvalidTask() {

	}

	@Test
	public void testUpdateTask() {

	}
}
