package org.lip6.scheduler;

import java.util.List;

public abstract class ExecutableNode {
	// void execute(String[] args);

	public abstract List<Integer> getSuccessors();

	public abstract void addSuccessor(int ID);

	public abstract boolean hasSuccessor(int taskID);

	public abstract int getID();

	protected abstract Object clone() throws CloneNotSupportedException;
}
