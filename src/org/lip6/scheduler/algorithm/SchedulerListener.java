package org.lip6.scheduler.algorithm;

import org.lip6.scheduler.Schedule;

/**
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public interface SchedulerListener {
	/**
	 * This method is invoked when a <b>feasible</b> solution has been generated
	 * by the scheduler.
	 * 
	 * @param solution
	 */
	void solutionGenerated(Schedule solution);
}
