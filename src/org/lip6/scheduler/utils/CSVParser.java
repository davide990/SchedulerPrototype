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
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

/**
 * Utility class for (de)serialize CSV files describing the plans.
 * 
 * @author <a href="mailto:davide-andrea.guastella@lip6.fr">Davide Andrea
 *         Guastella</a>
 */
public class CSVParser {

	private enum csvHeaders {
		taskID, planID, planName, planPriority, resourceUsage, releaseTime, dueDate, processingTime, planSuccessors, lag, syncTasks, taskPredecessors
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
			String ru = record.get("resourceUsage");
			Map<Integer, Integer> resourceUsages = parseResourceUsages(ru);

			String lagStr = record.get("lag");

			int timeLag = 0;
			if (!lagStr.equals("")) {
				timeLag = Integer.parseInt(lagStr);
			}

			int releaseTime = Integer.parseInt(record.get("releaseTime"));
			int dueDate = Integer.parseInt(record.get("dueDate"));
			int processingTime = Integer.parseInt(record.get("processingTime"));
			List<Integer> planSuccessors = parseList(record.get("planSuccessors"));
			List<Integer> syncTasks = parseList(record.get("syncTasks"));
			List<Integer> taskPredecessors = parseList(record.get("taskPredecessors"));

			// Put the plan ID to the map if it's not already in.
			plans.putIfAbsent(planID, PlanImpl.get(planID, planName, planPriority, planSuccessors, syncTasks));

			// Add the task to the plans' map
			plans.get(planID).addTask(TaskFactory.getTask(taskID, planID, planName, resourceUsages, timeLag,
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
	 * Parse the resource usages string
	 * 
	 * @param list
	 */
	private static Map<Integer, Integer> parseResourceUsages(String list) {
		List<String> usagesParResource = Arrays.asList(list.trim().split(";")).stream().collect(Collectors.toList());
		Map<Integer, Integer> usageMap = new HashMap<>();

		for (String ru : usagesParResource) {
			try {
				// Try to parse the current string as 'f(x)'
				String[] parts = ru.split("[\\(\\)]");
				if (parts.length != 2) {
					throw new PatternSyntaxException(ru, ru, 0);
				}
				int res = Integer.parseInt(parts[0]);
				int usage = Integer.parseInt(parts[1]);

				usageMap.putIfAbsent(res, usage);
			} catch (PatternSyntaxException e) {
				// Resource usage string is not in the form f(x). Trying to
				// parse as normal number (so the number is parsed as the
				// resource ID, and usage is fixed to 1)
				try {
					int res = Integer.parseInt(ru);
					usageMap.putIfAbsent(res, 1);

				} catch (NumberFormatException ex) {
					throw new IllegalArgumentException("Malformed resource usage string.");
				}
			}
		}
		return usageMap;
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
				printer.printRecord(t.getID(), t.getPlanID(), p.getPriority(),
						t.getResourcesID().stream().map(r -> Integer.toString(r)).collect(Collectors.joining(";")),
						t.getReleaseTime(), t.getProcessingTime());
			}

		}
		fout.flush();
		fout.close();
		printer.close();
	}
}
