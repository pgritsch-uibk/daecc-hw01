package at.ac.uibk.daecc;

import jFaaS.invokers.HTTPGETInvoker;
import jFaaS.utils.PairResult;
import org.apache.commons.cli.*;

import java.util.Map;
import java.util.function.Supplier;

public class Main {

    private static CommandLine parseCli(String[] args) {
        Options options = new Options();
        Option problem = new Option("p", "problem", true, "problem to solve (either nqueens or downup)");
        problem.setRequired(true);
        options.addOption(problem);

        options.addOption(new Option("n", true, "number of queens (only required for nqueens)"));
        options.addOption(new Option("k", true, "number of iterations (always required)"));

        options.addOption(new Option("filename", true, "filename (only required for updown)"));
        options.addOption(new Option("dbucket", true, "download bucket (only required for updown)"));
        options.addOption(new Option("ubucket", true, "upload bucket (only required for updown)"));

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {

        final CommandLine cmd = parseCli(args);
        final var invoker = new HTTPGETInvoker();
        final int k = Integer.parseInt(cmd.getOptionValue("k"));

        if (cmd.getOptionValue("problem").toLowerCase().equals("nqueens")) {
            for (int i = 0; i < k; i++) {
                final var result = invoker.invokeFunction("https://nqueens-h7vi6qx2ba-lm.a.run.app",
                        Map.of("num_queens", Integer.parseInt(cmd.getOptionValue("n")),
                                "from", 0,
                                "to", 100000000));
                System.out.println(result.getResult() + ", rtt:" + result.getRTT());
            }
        } else if (cmd.getOptionValue("problem").toLowerCase().equals("downup")) {
            for (int i = 0; i < k; i++) {
                final var result =
                        invoker.invokeFunction("https://europe-central2-macro-plate-365118.cloudfunctions.net/downUp",
                                Map.of("source_bucket", cmd.getOptionValue("dbucket"),
                                        "target_bucket", cmd.getOptionValue("ubucket"),
                                        "filename", cmd.getOptionValue("filename")));

                System.out.println(result.getResult() + ", rtt:" + result.getRTT());
            }
        }
    }
}