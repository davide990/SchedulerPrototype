package org.lip6.scheduler.test;

import java.util.Arrays;

import org.junit.Test;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.TaskImpl;
import org.lip6.scheduler.TaskFactory;

public class PlanTest {

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateTask() {
		PlanImpl plan = PlanImpl.get(0, 0);
		TaskImpl t1 = TaskFactory.getTask(0, 0, 0, 0, 1, Arrays.asList(1, 2), (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

		TaskImpl t2 = TaskFactory.getTask(0, 0, 0, 0, 1, Arrays.asList(1, 2), (String[] arg) -> {
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
