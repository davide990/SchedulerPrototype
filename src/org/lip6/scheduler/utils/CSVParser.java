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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.NotImplementedException;
import org.lip6.scheduler.Plan;
import org.lip6.scheduler.PlanImpl;
import org.lip6.scheduler.Task;
import org.lip6.scheduler.TaskFactory;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

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

	private enum distanceFunctionCsvHeaders {
		taskID, planID, function, dt
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
	 * Parse the delta values from a CSV file, and set for the set of plans
	 * given as input
	 * 
	 * @param fname
	 * @param plans
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void parseDeltaValues(String fname, List<Plan> plans) throws ParseException, IOException {
		Reader in = new FileReader(fname);
		parseDeltaValuesAux(in, plans);
	}
	
	public static void parseDeltaValues(InputStream is, List<Plan> plans) throws ParseException, IOException {
		parseDeltaValuesAux(new InputStreamReader(is), plans);
	}

	public static void parseDeltaValuesAux(Reader in, List<Plan> plans) throws ParseException, IOException {

		//Reader in = new FileReader(fname);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader()
				.withHeader(distanceFunctionCsvHeaders.class).parse(in);

		// Iterate each record (line) of the CSV file
		for (CSVRecord record : records) {
			// Parse its attributes
			int taskID = Integer.parseInt(record.get("taskID"));
			int planID = Integer.parseInt(record.get("planID"));

			Optional<Plan> planOpt = plans.stream().filter(p -> p.getID() == planID).findFirst();
			if (!planOpt.isPresent()) {
				throw new IllegalArgumentException("plan #" + Integer.toString(planID) + " not found.");
			}

			Plan plan = planOpt.get();

			try {
				plan.getTask(taskID);
			} catch (NoSuchElementException e) {
				throw new IllegalArgumentException(
						"task " + Integer.toString(taskID) + " in plan #" + Integer.toString(planID) + " not found.");
			}

			String function = record.get("function");
			String dt = record.get("dt");

			switch (function.toUpperCase()) {
			case "FIXED":
				setFixedFunctionValues(plan.getTask(taskID), dt);
				break;
			case "GAUSSIAN":
				setGaussianFunctionValues(plan.getTask(taskID), dt);
				break;
			case "USER":
				setUserDefinedFunctionValues(plan.getTask(taskID), dt);
				break;
			default:
				throw new IllegalArgumentException("not recognized: " + function);
			}

		}
	}

	/**
	 * 
	 * @param task
	 * @param valueField
	 * @throws PatternSyntaxException
	 */
	private static void setUserDefinedFunctionValues(Task task, String valueField) throws PatternSyntaxException {
		Map<Integer, Integer> valueMap = new HashMap<>();
		for (int i = task.getReleaseTime(); i <= task.getDueDate(); i++) {
			Expression e = new ExpressionBuilder(valueField).variables("pk", "t", "rk").build().setVariable("t", i)
					.setVariable("pk", task.getProcessingTime()).setVariable("rk", task.getReleaseTime());
			Double result = e.evaluate();
			valueMap.putIfAbsent(i, result.intValue());
		}

		task.setDeltaValues(valueMap);
	}

	/**
	 * 
	 * @param task
	 * @param valueField
	 * @throws PatternSyntaxException
	 */
	private static void setGaussianFunctionValues(Task task, String valueField) throws PatternSyntaxException {
		throw new NotImplementedException("Not implemented yet!");
		/*
		 * // String[] gaussianParameters = valueField.split("(\\,\\)"); // int
		 * sigma = Integer.parseInt(gaussianParameters[0]); // int c =
		 * Integer.parseInt(gaussianParameters[1]); Map<Integer, Integer>
		 * valueMap = new HashMap<>();
		 * 
		 * for (int i = task.getReleaseTime(); i <= task.getDueDate(); i++) {
		 * valueMap.putIfAbsent(i, 0); // TODO calcola gaussiana }
		 * 
		 * task.setDeltaValues(valueMap);
		 */
	}

	/**
	 * 
	 * @param task
	 * @param valueField
	 * @throws PatternSyntaxException
	 */
	private static void setFixedFunctionValues(Task task, String valueField) throws PatternSyntaxException {
		List<String> usagesParResource = Arrays.asList(valueField.trim().split(";")).stream()
				.collect(Collectors.toList());
		Map<Integer, Integer> valueMap = new HashMap<>();

		for (int i = task.getReleaseTime(); i <= task.getDueDate(); i++) {
			valueMap.putIfAbsent(i, 0);
		}

		for (String ru : usagesParResource) {
			if (ru.equals("")) {
				continue;
			}

			// Try to parse the current string as 't, v'
			String[] parts = ru.replaceAll("[\\(\\)]", "").split(",");

			if (parts.length != 2) {
				throw new PatternSyntaxException(ru, ru, 0);
			}
			int t = Integer.parseInt(parts[0]);
			int v = Integer.parseInt(parts[1]);
			valueMap.put(t, v);
		}
		task.setDeltaValues(valueMap);
		// TODO QUI va un task.setQUALCOSA(valueMap);
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
