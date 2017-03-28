package org.lip6.main;

import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class Main {

	public static void main(String[] args) {

		Task t1 = TaskFactory.getTask(0, 0, 0, 1, new int[] { 1, 2 }, (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

		PlanImpl plan = new PlanImpl(0);
		plan.addTask(t1);

		plan.execute(args);

	}

}
