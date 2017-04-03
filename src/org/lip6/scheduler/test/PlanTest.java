package org.lip6.scheduler.test;

import java.util.Arrays;

import org.junit.Test;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class PlanTest {

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateTask() {
		PlanImpl plan = PlanImpl.get(0, 0);
		Task t1 = TaskFactory.getTask(0, 0, 0, 1, Arrays.asList(1, 2), (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

		Task t2 = TaskFactory.getTask(0, 0, 0, 1, Arrays.asList(1, 2), (String[] arg) -> {
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
