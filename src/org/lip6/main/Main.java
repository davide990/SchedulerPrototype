package org.lip6.main;

import java.util.HashSet;
import java.util.Set;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.TaskFactory;
import org.lip6.scheduler.algorithm.Scheduler;
import org.lip6.scheduler.algorithm.SchedulerFactory;

public class Main {

	public static void main(String[] args) {
		int WStart = 1;
		int WEnd = 2000;
		int maxResourceCapacity = 1;
		Scheduler sc = SchedulerFactory.getFromFile(maxResourceCapacity, WStart, WEnd,
				"/home/davide/test_case_1.csv");
		Schedule s = sc.buildSchedule();
				//"/home/davide/paper_plans_nouveau.csv");
				

		System.out.println("Scheduling:\n" + s);
	}

	void main2(String[] args) {
		int WStart = 2;
		int WEnd = 8;
		int maxResourceCapacity = 1;
		Set<Plan> plans = new HashSet<>();

		PlanImpl p0 = PlanImpl.get(0, 5);
		PlanImpl p1 = PlanImpl.get(1, 9);
		PlanImpl p2 = PlanImpl.get(2, 2);

		p0.addTask(TaskFactory.getTask(0, 0, 0, 2, 4, 10, 5));
		p0.addTask(TaskFactory.getTask(1, 0, 0, 6, 4, 10, 8));
		p0.addTask(TaskFactory.getTask(2, 0, 0, 9, 4, 10, 14));

		p1.addTask(TaskFactory.getTask(3, 1, 0, 1, 4, 10, 3));
		p1.addTask(TaskFactory.getTask(4, 1, 0, 1, 4, 10, 6));

		p2.addTask(TaskFactory.getTask(5, 2, 0, 6, 4, 10, 9));
		p2.addTask(TaskFactory.getTask(6, 2, 0, 10, 4, 10, 15));

		plans.add(p1);
		plans.add(p2);
		plans.add(p0);

		System.out.println("SET OF PLANS:");
		plans.forEach(p -> {
			System.out.println("---> " + p.getID());
		});

		Scheduler sc = SchedulerFactory.get(maxResourceCapacity, WStart, WEnd, plans);
		Schedule s = sc.buildSchedule();
		System.out.println("Scheduling:\n" + s);
	}

}
