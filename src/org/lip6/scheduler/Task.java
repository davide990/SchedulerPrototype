package org.lip6.scheduler;

@FunctionalInterface
public interface Task {

	void execute(String[] args);

}
