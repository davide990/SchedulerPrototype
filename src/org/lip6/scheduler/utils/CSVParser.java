package org.lip6.scheduler.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class CSVParser {

	private enum csvHeaders {
		taskID, planID, planPriority, resourceID, releaseTime, processingTime
	}

	public static Map<Integer, Plan> parse(InputStream inputStream) throws IOException {
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().withHeader(csvHeaders.class)
				.parse(new InputStreamReader(inputStream));

		Map<Integer, Plan> plans = new HashMap<>();

		for (CSVRecord record : records) {
			int planID = Integer.parseInt(record.get("planID"));
			int planPriority = Integer.parseInt(record.get("planPriority"));
			int taskID = Integer.parseInt(record.get("taskID"));
			int resourceID = Integer.parseInt(record.get("resourceID"));
			int releaseTime = Integer.parseInt(record.get("releaseTime"));
			int processingTime = Integer.parseInt(record.get("processingTime"));

			// Add the plan if it doesn't exists
			plans.putIfAbsent(planID, PlanImpl.get(planID, planPriority));

			// Add the task to the plan
			plans.get(planID).addTask(
					TaskFactory.getTask(taskID, planID, resourceID, releaseTime, processingTime, planPriority));
		}

		return plans;
	}

	public static Map<Integer, Plan> parse(String fname) throws IOException {
		Reader in = new FileReader(fname);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().withHeader(csvHeaders.class).parse(in);

		Map<Integer, Plan> plans = new HashMap<>();

		for (CSVRecord record : records) {
			int planID = Integer.parseInt(record.get("planID"));
			int planPriority = Integer.parseInt(record.get("planPriority"));
			int taskID = Integer.parseInt(record.get("taskID"));
			int resourceID = Integer.parseInt(record.get("resourceID"));
			int releaseTime = Integer.parseInt(record.get("releaseTime"));
			int processingTime = Integer.parseInt(record.get("processingTime"));

			// Add the plan if it doesn't exists
			plans.putIfAbsent(planID, PlanImpl.get(planID, planPriority));

			// Add the task to the plan
			plans.get(planID).addTask(
					TaskFactory.getTask(taskID, planID, resourceID, releaseTime, processingTime, planPriority));
		}

		return plans;
	}

	public static void serialize(List<Plan> plans, String fname) throws IOException {
		final FileWriter fout = new FileWriter(fname);
		final CSVPrinter printer = CSVFormat.EXCEL.withHeader(csvHeaders.class).print(fout);

		for (Plan p : plans) {
			for (Task t : p.tasks()) {
				printer.printRecord(t.getTaskID(), t.getPlanID(), p.getPriority(), t.getResourceID(),
						t.getReleaseTime(), t.getProcessingTime());
			}

		}
		fout.flush();
		fout.close();
		printer.close();
	}
}
