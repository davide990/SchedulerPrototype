package org.lip6.scheduler.algorithm;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.Task;

public class TabuListEntry {

	private final Task task;
	private final Plan plan;
	private int waitTurns;

	public TabuListEntry(Task task, Plan plan, int waitTurns) {
		this.task = task;
		this.plan = plan;
		this.waitTurns = waitTurns;
	}

	public int getWaitTurns() {
		return waitTurns;
	}

	public void setWaitTurns(int waitTurns) {
		this.waitTurns = waitTurns;
	}

	public Task getTask() {
		return task;
	}

	public Plan getPlan() {
		return plan;
	}

}
