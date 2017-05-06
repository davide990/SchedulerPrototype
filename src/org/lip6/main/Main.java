package org.lip6.main;

import java.util.ArrayList;
import java.util.List;

import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Schedule;
import org.lip6.scheduler.TaskFactory;
import org.lip6.scheduler.algorithm.Criteria;
import org.lip6.scheduler.algorithm.Scheduler;

public class Main {

	public static void main(String[] args) {
		int WStart = 2;
		int WEnd = 15;
		int maxResourceCapacity = 1;
		List<Criteria> criterias = new ArrayList<>();
		criterias.add(new Criteria(Plan::getInversePriority, "Plan Priority", 1f));
		criterias.add(new Criteria(Plan::getNumberOfTasks, "Number of Tasks", 0.1f));
		criterias.add(new Criteria(Plan::getExecutionTime, "Estimated Execution Time", 0.01f));

		Schedule s = Scheduler.scheduleFromFile(maxResourceCapacity, WStart, WEnd, criterias,
				"/home/davide/paper_plans_nouveau.csv");

		System.out.println("Scheduling:\n" + s);
	}

	void main2(String[] args) {
		int WStart = 2;
		int WEnd = 8;
		int maxResourceCapacity = 1;
		List<Plan> plans = new ArrayList<>();
		List<Criteria> criterias = new ArrayList<>();

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

		// CRITERIA SETTING
		criterias.add(new Criteria(Plan::getPriority, "Plan Priority", 1f));
		criterias.add(new Criteria(Plan::getNumberOfTasks, "Number of Tasks", 0.1f));
		criterias.add(new Criteria(Plan::getExecutionTime, "Estimated Execution Time", 0.01f));

		System.out.println("SET OF PLANS:");
		plans.forEach(p -> {
			System.out.println("---> " + p.getID());
		});

		Schedule s = Scheduler.schedule(maxResourceCapacity, WStart, WEnd, criterias, plans);
		System.out.println("Scheduled plans: " + s.plans().size() + " of " + plans.size());

		System.out.println("Scheduling:\n" + s);
	}

}
