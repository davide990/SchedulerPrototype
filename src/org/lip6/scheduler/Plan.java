package org.lip6.scheduler;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public interface Plan {
	void addTask(Task t);

	Collection<Task> getTasks();

	List<Integer> getSuccessors();

	void addSuccessor(int ID);

	void removeSuccessor(int ID);

	Task getTask(int taskID);

	int getID();

	int getPriority();

	int getInversePriority();

	float getScore();

	void setScore(float value);

	void setInversePriority(int maxPriority);

	int getExecutionTime();

	int getNumberOfTasks();

	boolean isSchedulable();

	void setSchedulable(boolean schedulable);
	
	static final Comparator<Plan> PLAN_COMPARATOR = new Comparator<Plan>() {
		@Override
		public int compare(Plan o1, Plan o2) {
			return Float.compare(o1.getScore(), o2.getScore());
		}
	};
}
