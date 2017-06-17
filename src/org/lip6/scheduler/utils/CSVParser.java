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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

public class CSVParser {

	private enum csvHeaders {
		taskID, planID, planName, planPriority, resourceID, resourceUsage, releaseTime, dueDate, processingTime, planSuccessors, syncTasks, taskPredecessors
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
			String planName = record.get("planName");
			int planPriority = Integer.parseInt(record.get("planPriority"));
			int taskID = Integer.parseInt(record.get("taskID"));
			int resourceID = Integer.parseInt(record.get("resourceID"));

			//Assign 1 as default resource usage
			String ru =record.get("resourceUsage");
			int resourceUsage = Integer.parseInt(ru.equals("") ? "1" : ru);
			
			int releaseTime = Integer.parseInt(record.get("releaseTime"));
			int dueDate = Integer.parseInt(record.get("dueDate"));
			int processingTime = Integer.parseInt(record.get("processingTime"));
			List<Integer> planSuccessors = parseList(record.get("planSuccessors"));
			List<Integer> syncTasks = parseList(record.get("syncTasks"));
			List<Integer> taskPredecessors = parseList(record.get("taskPredecessors"));

			// Put the plan ID to the map if it's not already in.
			plans.putIfAbsent(planID, PlanImpl.get(planID, planName, planPriority, planSuccessors, syncTasks));

			// Add the task to the plans' map
			plans.get(planID).addTask(TaskFactory.getTask(taskID, planID, planName, resourceID, resourceUsage,
					releaseTime, dueDate, processingTime, planPriority, taskPredecessors));
		}

		setTaskSuccessors(plans.values());
		return plans;
	}

	/**
	 * Set the successors for each task in the parsed plan set.
	 * 
	 * @param plans
	 */
	private static void setTaskSuccessors(Collection<Plan> plans) {
		// Iterate each plan
		for (Plan plan : plans) {
			for (Task task : plan.getTasks()) {
				List<Task> t = plan.getTasks().stream().filter(x -> x.getPredecessors().contains(task.getID()))
						.collect(Collectors.toList());
				t.forEach(tt -> task.addSuccessor(tt.getID()));
			}
		}
	}

	/**
	 * Parse a predecessors string, that is, a list of numbers separated by ';'
	 * 
	 * @param precedences
	 * @return
	 * @throws ParseException
	 */
	private static List<Integer> parseList(String precedences) throws ParseException {
		// If the string is not empty, there is at least one precedence relation
		// specified in the CSV
		if (precedences.length() > 0) {
			return Arrays.asList(precedences.trim().split(";")).stream().map(x -> Integer.parseInt(x))
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	public static void serialize(final List<Plan> plans, String fname) throws IOException {
		final FileWriter fout = new FileWriter(fname);
		final CSVPrinter printer = CSVFormat.EXCEL.withHeader(csvHeaders.class).print(fout);

		for (Plan p : plans) {
			for (Task t : p.getTasks()) {
				printer.printRecord(t.getID(), t.getPlanID(), p.getPriority(), t.getResourceID(), t.getReleaseTime(),
						t.getProcessingTime());
			}

		}
		fout.flush();
		fout.close();
		printer.close();
	}
}
