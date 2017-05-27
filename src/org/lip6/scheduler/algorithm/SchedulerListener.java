package org.lip6.scheduler.algorithm;

import org.lip6.scheduler.Schedule;

public interface SchedulerListener {
	void solutionGenerated(Schedule solution);
}
