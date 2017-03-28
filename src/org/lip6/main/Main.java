package org.lip6.main;

import org.lip6.scheduler.Executable;
import org.lip6.scheduler.TaskFactory;

public class Main {

	public static void main(String[] args) {

		Executable t1 = TaskFactory.getTask(0, 0, 0, 0, new int[] { 1, 2 }, (String[] arg) -> {
			System.out.println("hello");
			return null;
		});

	}

}
