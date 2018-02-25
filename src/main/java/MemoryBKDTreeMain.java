import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Main class to execute program.
 */
public class MemoryBKDTreeMain {


    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            if ("-h".equals(args[0]) || "--help".equals(args[0])) {
                printUsage();
                System.exit(0);
            }
        }
        if (args.length != 2 && args.length != 3) {
            System.out.println("The program has been called with incorrect parameters:");
            System.out.println();
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
        int docsPerLeaf = MemoryBKDTree.DEFAULT_DOCUMENTS_PER_LEAF;
        if (args.length == 3) {
            try {
                docsPerLeaf = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("The input for documents per leaf is not an integer: " + args[2]);
                System.exit(0);
            }
        }
        System.out.println("Program started, loading documents in memory....");
        long start = System.currentTimeMillis();
        Document[] documents = readDocuments(dataFile);
        long end = System.currentTimeMillis();
        int numberDocs = documents.length;
        System.out.println("A total of " + numberDocs + " documents have been loaded in memory in " +formatDouble(1e-3 * (end -start)) + " seconds");
        System.out.println();
        System.out.println( "building the index ...");
        start = System.currentTimeMillis();
        MemoryBKDTree tree = new MemoryBKDTree(documents, docsPerLeaf);
        end = System.currentTimeMillis();
        System.out.println("Index has been built in : " + formatDouble(1e-3 * (end -start)) + " seconds");
        System.out.println();
        System.out.println( "Executing queries...");
        System.out.println();

        int[] results = executeQueries(queryFile, tree);
        System.out.println(results[0] + " queries has been executed in " + formatDouble(1e-3 * results[2]) + " seconds ( " + formatDouble(results[0]/(1e-3 * results[2])) + " queries per second)");
        System.out.println("Total number of hits: " + results[1]);
        System.out.println();
        System.out.println("Bye!");
    }

    private static Document[] readDocuments(File file) throws IOException{
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<Document> documents = new ArrayList<>();
        String line;
        int count =0;
        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\\s+");
            if (data.length != 3) {
                throw new IllegalArgumentException("Index input data not properly formed: " + line);
            }
            documents.add(new Document(data[0], Double.parseDouble(data[2]), Double.parseDouble(data[1])));
            if (++count % 1e6 == 0) {
                System.out.print(new StringBuilder("\r  " + count + " documents loaded in memory"));
            }
        }
        reader.close();
        System.out.println();
        return documents.toArray(new Document[documents.size()]);
    }

    private static int[] executeQueries(File file, MemoryBKDTree tree) throws IOException{
        FileInputStream inputStream = new FileInputStream(file);
        int totalTime =0;
        int totalHits =0;
        int numberOfQueries =0;

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] data = line.split("\\s+");
            if (data.length != 4) {
                System.out.println("Skipping query because input data not properly formed: " + line);
                System.out.println();
                continue;
            }
            long start = System.currentTimeMillis();
            int hits = executeQuery(data, tree);
            long end = System.currentTimeMillis();
            System.out.println();
            System.out.println("Hits :" + hits);
            System.out.println("Query took :" + formatDouble(1e-3 * (end - start)) + " seconds");
            System.out.println();
            totalHits += hits;
            totalTime += end - start;
            numberOfQueries++;
        }
        reader.close();
        return new int[] {numberOfQueries, totalHits, totalTime};
    }

    private static int executeQuery(String[] query, MemoryBKDTree tree) {
        final double minLatitude = Double.parseDouble(query[0]);
        final double maxLatitude = Double.parseDouble(query[1]);
        final double minLongitude = Double.parseDouble(query[2]);
        final double maxLongitude = Double.parseDouble(query[3]);
        System.out.println("Executing query: " + query[0] + " " + query[1] + " " + query[2] + " " + query[3]);
        System.out.println();
        List<Document> answer =  tree.contains(new double[]{maxLongitude, maxLatitude}, new double[]{minLongitude, minLatitude});
        System.out.println(" Results");
        System.out.println(" --------------------------");
        if (answer.size() == 0) {
            System.out.println(" no results for this query!");
            return 0;
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
        return answer.size();
    }

    private static void printUsage() {
        System.out.println("usage: java -jar <jarfile>.jar /path/to/geo_points.csv /path/to/queries.csv [(optional)Number of documents per leaf]");
        System.out.println();
        System.out.println("       -h | --help                      :       display this help");
    }

    private static String formatDouble(double myDouble){
        NumberFormat numberFormatter = new DecimalFormat("####.000");
        return numberFormatter.format(myDouble);
    }
}