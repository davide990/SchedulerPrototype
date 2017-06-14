package org.lip6.graph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.lip6.scheduler.ExecutableNode;
import org.lip6.scheduler.Plan;

public class GraphUtils {

	private static String OPEN_GRAPH = "digraph G { \n";
	private static String NODE = "{0} [label=\"{1}\"]; \n";
	private static String EDGE = "{0} -> {1}; \n";
	private static String CLOSE_GRAPH = "} \n";

	/**
	 * Write a DOT file containing the plans and their relations. <br/>
	 * To produce a pdf file using the produced dot file, execute (under
	 * UNIX):<br/>
	 * <br/>
	 * 
	 * {@code dot -Tpdf [filename.dot] -O}
	 * @param plans
	 * @param filename
	 */
	public static void graphToDot(final Collection<Plan> plans, String filename) {
		StringBuilder sb = new StringBuilder();
		Writer wr;
		try {
			format(sb, plans);
			wr = new FileWriter(filename);
			wr.write(sb.toString());
			wr.flush();
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Format a DOT string according to the plans precedences
	 * 
	 * @param sb
	 * @param plans
	 * @throws IOException
	 */
	private static void format(Appendable sb, final Collection<Plan> plans) throws IOException {
		sb.append(OPEN_GRAPH);
		// Render nodes
		for (Plan node : plans) {
			String label2 = Integer.toString(node.getID()) + " (&alpha; = " + Integer.toString(node.getPriority())
					+ ")";
			sb.append(MessageFormat.format(NODE, node.getID(), label2));

			// Render edges for node
			for (Integer targetEdge : node.getSuccessors()) {
				sb.append(MessageFormat.format(EDGE, node.getID(), targetEdge));
			}
		}
		sb.append(CLOSE_GRAPH);
	}

	
	public static Map<Integer, LinkedList<AdjListNode>> getAdjacencyList(final List<ExecutableNode> plans) {
		Map<Integer, LinkedList<AdjListNode>> adj = new HashMap<>();
		List<Integer> planIDs = plans.stream().map(x -> x.getID()).collect(Collectors.toList());
		for (Integer i : planIDs) {
			adj.put(i, new LinkedList<AdjListNode>());
			ExecutableNode pi = plans.stream().filter(x -> x.getID() == i).findFirst().get();

			for (Integer successor : pi.getSuccessors()) {
				Optional<ExecutableNode> ops = plans.stream().filter(x -> x.getID() == successor).findFirst();
				if (ops.isPresent()) {
					ExecutableNode ps = ops.get();
					AdjListNode node = new AdjListNode(ps,
							((Plan) pi).getExecutionTime() + ((Plan) pi).getExecutionTime());

					if (!adj.get(i).contains(node)) {
						adj.get(i).add(node);// Add v to u's list
					}
				}

			}
		}

		return adj;
	}

	public static void printAdjacencyList(Map<Integer, LinkedList<AdjListNode>> adj) {
		for (Integer i : adj.keySet()) {
			System.err.println(Integer.toString(i) + "-> "
					+ adj.get(i).stream().map(x -> x.toString()).collect(Collectors.joining(",")));
		}
	}
}
