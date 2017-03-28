package org.lip6.main;

import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class Main {

	public static void main(String[] args) {

		Task t1 = TaskFactory.getTask(0, 0, 0, 0, (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

	}

}
