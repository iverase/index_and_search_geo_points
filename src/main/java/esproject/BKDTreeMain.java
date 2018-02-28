package esproject;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Class to execute program.
 */
public class BKDTreeMain {

    /**
     * Entry point of the program.
     *
     * @param args Provided arguments. See usage.
     * @throws IOException if there is an error reading the input files.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            if ("-h".equals(args[0]) || "--help".equals(args[0])) {
                printUsage();
                System.exit(0);
            }
        }
        if (args.length != 2 && args.length != 3) {
            System.out.println("The program has been called with incorrect parameters:");
            printUsage();
            System.exit(0);
        }
        File dataFile = new File(args[0]);
        if (!dataFile.exists()) {
            System.out.println("The input data file does not exists: " + args[0]);
            System.exit(0);
        }
        File queryFile = new File(args[1]);
        if (!queryFile.exists()) {
            System.out.println("The input query file does not exists: " + args[1]);
            System.exit(0);
        }
        int docsPerLeaf = KDBTree.DEFAULT_DOCUMENTS_PER_LEAF;
        if (args.length == 3) {
            try {
                docsPerLeaf = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("The input for points per leaf is not an integer: " + args[2]);
                printUsage();
                System.exit(0);
            }
            if (docsPerLeaf < 2) {
                System.out.println("The input for points per leaf must be bigger than 1: " + args[2]);
                printUsage();
                System.exit(0);
            }
        }
        System.out.println("Program started, loading points in memory....");
        long start = System.currentTimeMillis();
        Document[] documents = readDocuments(dataFile);
        long end = System.currentTimeMillis();
        int numberDocs = documents.length;
        double timeLoadingDocuments = 1e-3 * (end -start);
        System.out.println("A total of " + numberDocs + " points have been loaded in memory in " +formatDouble(timeLoadingDocuments) + " seconds");
        System.out.println();

        System.out.println( "building the index ...");
        start = System.currentTimeMillis();
        BKDTree tree = new BKDTree(documents, docsPerLeaf);
        end = System.currentTimeMillis();
        double timeBuildingIndex = 1e-3 * (end -start);
        System.out.println("Index has been built in : " + formatDouble(timeBuildingIndex) + " seconds");
        System.out.println();
        System.out.println( "Executing queries...");
        System.out.println();

        int[] results = executeQueries(queryFile, tree);

        System.out.println("Summary");
        System.out.println("--------");
        System.out.println("Time spent loading " + numberDocs + " points into memory: " + formatDouble(timeLoadingDocuments));
        System.out.println("Time spent indexing the points: " + formatDouble(timeBuildingIndex));
        System.out.println(results[0] + " queries has been executed in " + formatDouble(1e-3 * results[2]) + " seconds (" + formatDouble(results[0]/(1e-3 * results[2])) + " queries per second)");
        System.out.println("Total number of hits: " + results[1]);
        System.out.println();
    }

    /**
     * Read the points file and return an array of {@link Document}.
     *
     * @param file the location of the documents file.
     * @return an array of {@link Document}.
     * @throws IOException if there is an error reading the file.
     */
    private static Document[] readDocuments(File file) throws IOException{
        ArrayList<Document> documents = new ArrayList<>();
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        int count =0;
        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\\s+");
            if (data.length != 3) {
                System.out.println("Index input data not properly formed: " + line);
                System.exit(0);
            }
            double longitude = -999;
            double latitude = -999;
            try {
                longitude = Double.parseDouble(data[2]);
                latitude = Double.parseDouble(data[1]);
            } catch (NumberFormatException e) {
                System.out.println("Index input data not properly formed, not a number: " + line);
                System.exit(0);
            }
            if (!BoundingBoxUtils.checkLongitude(longitude)) {
                System.out.println("Index input data not properly formed, longitude out of bounds: " + line);
                System.exit(0);
            }
            if (!BoundingBoxUtils.checkLatitude(latitude)) {
                System.out.println("Index input data not properly formed, latitude out of bounds: " + line);
                System.exit(0);
            }
            documents.add(new Document(data[0], longitude, latitude));
            if (++count % 1e6 == 0) {
                System.out.print(new StringBuilder("\r  " + (int)(count / 1e6) + " million points loaded in memory"));
            }
            //basic check to prevent out of memory errors. Documents should get at maximum 80% of
            //available heap
            if (Runtime.getRuntime().totalMemory() == Runtime.getRuntime().maxMemory()) {
                if (Runtime.getRuntime().freeMemory() < 0.2 * Runtime.getRuntime().maxMemory()) {
                    System.out.println();
                    System.out.println("No more memory for points, breaking early...");
                    break;
                }
            }
        }
        reader.close();
        System.out.println();
        return documents.toArray(new Document[documents.size()]);
    }

    /**
     * Reads the file containing the queries and execute them.
     *
     * @param file the location of the queries file.
     * @param tree the {@link BKDTree} to be queried.
     * @return an array containing the number of queries executed, the total hits and the total execution time
     * @throws IOException if there is an error reading the file.
     */
    private static int[] executeQueries(File file, BKDTree tree) throws IOException{
        FileInputStream inputStream = new FileInputStream(file);
        int totalTime =0;
        int totalHits =0;
        int numberOfQueries =0;

        List<Document> answerContainer = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\\s+");
            if (data.length != 4) {
                System.out.println("Skipping query because input data not properly formed: " + line);
                System.out.println();
                continue;
            }
            double[] upperPoint = new double[2];
            double[] lowerPoint= new double[2];
            try {
                upperPoint[0] = Double.parseDouble(data[3]);
                upperPoint[1] = Double.parseDouble(data[1]);
                lowerPoint[0] = Double.parseDouble(data[2]);
                lowerPoint[1] = Double.parseDouble(data[0]);
            } catch (NumberFormatException e) {
                System.out.println("Skipping query because input data not properly formed, not a number: " + line);
                System.out.println();
                continue;
            }
            if (!BoundingBoxUtils.checkBoundingBox(upperPoint, lowerPoint)) {
                System.out.println("Skipping query because input data not properly formed, not a valid bounding box: " + line);
                System.out.println();
                continue;
            }
            long start = System.currentTimeMillis();
            executeQuery(upperPoint, lowerPoint, tree, answerContainer);
            long end = System.currentTimeMillis();
            System.out.println();
            System.out.println("Hits: " + answerContainer.size());
            System.out.println("Query took " + formatDouble(1e-3 * (end - start)) + " seconds");
            System.out.println();
            totalHits += answerContainer.size();
            totalTime += end - start;
            numberOfQueries++;
            //clear answer
            answerContainer.clear();
        }
        reader.close();
        return new int[] {numberOfQueries, totalHits, totalTime};
    }

    /**
     * Executes one query with the input provided.
     *
     * @param upperPoint The left upper corner of the bounding box.
     * @param lowerPoint The right lower corner of the bounding box.
     * @param tree the {@link BKDTree} to be queried.
     * @param answer the list collector.
     */
    private static void executeQuery(double[] upperPoint, double[] lowerPoint, BKDTree tree, List<Document> answer) {
        System.out.println("Executing query: " + lowerPoint[1] + " " + upperPoint[1] + " " + lowerPoint[0] + " " + upperPoint[0]);
        System.out.println();

        tree.contains(upperPoint, lowerPoint, answer);

        System.out.println(" Results");
        System.out.println(" --------------------------");

        if (answer.size() == 0) {
            System.out.println(" no results for this query!");
            return;
        }

        for (int i =0 ; i < answer.size(); i++) {
            if ( i == 25) {
                System.out.print(" " + answer.get(i).data + " ......");
                break;
            } else {
                System.out.print(" " + answer.get(i).data);
            }
        }
        System.out.println();
    }

    /**
     * Prints the usage of this program.
     */
    private static void printUsage() {
        System.out.println();
        System.out.println("  usage: java -jar <jarfile>.jar /path/to/geo_points.csv /path/to/queries.csv [number of points per leaf]");
        System.out.println();
        System.out.println("       -h | --help                      :       display this help");
        System.out.println();
        System.out.println("  Format of 'geopoints.csv'; a string id, then latitude, then longitude, separated by one or more spaces");
        System.out.println("  Format of 'queries.csv'; minimum latitude, maximum latitude, minimum longitude and maximum longitude, all separated by one or more spaces");
        System.out.println("  The number of points per leaf is optional (default 1024). If provided it must be an integer bigger than one");
        System.out.println();
    }

    /**
     * Format double for output.
     *
     * @param aDouble the double to be formatted.
     * @return The number formatted as string.
     */
    private static String formatDouble(double aDouble){
        NumberFormat numberFormatter = new DecimalFormat("####0.000");
        return numberFormatter.format(aDouble);
    }

    private BKDTreeMain() {
        //no instances
    }
}