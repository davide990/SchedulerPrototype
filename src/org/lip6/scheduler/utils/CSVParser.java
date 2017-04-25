package org.lip6.scheduler.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class CSVParser {

	private enum csvHeaders {
		taskID, planID, planPriority, resourceID, releaseTime, dueDate, processingTime, predecessors
	}

	public static Map<Integer, Plan> parse(InputStream inputStream) throws IOException, ParseException {
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().withHeader(csvHeaders.class)
				.parse(new InputStreamReader(inputStream));
		return parseRecords(records);
	}

	public static Map<Integer, Plan> parse(String fname) throws IOException, ParseException {
		// Create a new file reader and parse the CSV file
		Reader in = new FileReader(fname);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().withHeader(csvHeaders.class).parse(in);
		// Parse the records and return a map containing the plans
		return parseRecords(records);
	}

	private static Map<Integer, Plan> parseRecords(Iterable<CSVRecord> records) throws ParseException {
		Map<Integer, Plan> plans = new HashMap<>();

		// Iterate each record (line) of the CSV file
		for (CSVRecord record : records) {
			// Parse its attributes
			int planID = Integer.parseInt(record.get("planID"));
			int planPriority = Integer.parseInt(record.get("planPriority"));
			int taskID = Integer.parseInt(record.get("taskID"));
			int resourceID = Integer.parseInt(record.get("resourceID"));
			int releaseTime = Integer.parseInt(record.get("releaseTime"));
			int dueDate = Integer.parseInt(record.get("dueDate"));
			int processingTime = Integer.parseInt(record.get("processingTime"));
			List<ImmutablePair<Integer, Integer>> predecessors = parsePrecedences(record.get("predecessors"));

			// Put the plan ID to the map if it's not already in.
			plans.putIfAbsent(planID, PlanImpl.get(planID, planPriority));

			// Add the task to the plans' map
			plans.get(planID).addTask(TaskFactory.getTask(taskID, planID, resourceID, releaseTime, dueDate, processingTime,
					planPriority, predecessors));
		}

		return plans;

	}

	private static List<ImmutablePair<Integer, Integer>> parsePrecedences(String precedences) throws ParseException {
		List<ImmutablePair<Integer, Integer>> taskPredecessors = new ArrayList<>();

		// If the string is not empty, there is at least one precedence relation
		// specified in the CSV
		if (precedences.length() > 0) {
			// Split the string by using the specified separator character
			List<String> stpr = Arrays.asList(precedences.trim().split(";"));
			// Convert each string to an integer
			List<Integer> pr = stpr.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toList());

			// If the size of the list is not even, the precedence relations are
			// not well specified, so an exception is throwed
			if (pr.size() % 2 != 0) {
				throw new ParseException("Wrong precedence list.", 0);
			}

			// Create the list of (plan,task) pairs
			for (int i = 0; i < pr.size() - 1; i += 2) {
				taskPredecessors.add(new ImmutablePair<Integer, Integer>(pr.get(i), pr.get(i + 1)));
			}
		}
		return taskPredecessors;
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
