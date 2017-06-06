package org.lip6.scheduler;

import java.util.List;

/**
 * Abstract class inherited from Task and Plan. This ensure that the topological
 * sorting algorithm can be executed on both plans and tasks.
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public abstract class ExecutableNode {

	public abstract List<Integer> getSuccessors();

	public abstract void addSuccessor(int ID);

	public abstract boolean hasSuccessor(int taskID);

	public abstract int getID();

	protected abstract Object clone() throws CloneNotSupportedException;
}
