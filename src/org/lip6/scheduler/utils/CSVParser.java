package org.lip6.scheduler.utils;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.TaskFactory;

public class CSVParser {

	public static void main(String[] args) {
		Map<Integer, PlanImpl> p = null;
		try {
			p = parse("/home/davide/task_benchmark_papero.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}

		p.forEach((k, v) -> {
			System.out.println(v);
		});

	}

	public enum headers {
		taskID, planID, planPriority, resourceID, releaseTime, processingTime
	}

	public static Map<Integer, PlanImpl> parse(String fname) throws IOException {
		Reader in = new FileReader(fname);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().withHeader(headers.class).parse(in);

		Map<Integer, PlanImpl> plans = new HashMap<>();

		for (CSVRecord record : records) {
			int planID = Integer.parseInt(record.get("planID"));
			int planPriority = Integer.parseInt(record.get("planPriority"));
			plans.putIfAbsent(planID, PlanImpl.get(planID, planPriority));
			
			int taskID = Integer.parseInt(record.get("taskID"));
			int resourceID = Integer.parseInt(record.get("resourceID"));
			int releaseTime = Integer.parseInt(record.get("releaseTime"));
			int processingTime = Integer.parseInt(record.get("processingTime"));
			plans.get(planID).addTask(TaskFactory.getTask(taskID, planID, resourceID, releaseTime, processingTime));
		}

		return plans;
	}
}
