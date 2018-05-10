import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class SortAlgos {

    public static void main(String[] args) {
        String inFileString = args[0];
        String outFileString = "./output/sorted/" + inFileString;
        System.out.println(inFileString);
        Scanner scnr = null;
        ArrayList<Algorithm> algos = new ArrayList<Algorithm>();
        Algorithm[] algoArr;
        PrintWriter pWriter = null;
        
        try {
            File inFile = new File("./output/" + inFileString);
            scnr = new Scanner(inFile);
            scnr.nextLine();
            scnr.nextLine();
            scnr.nextLine();
            scnr.nextLine();
            scnr.nextLine();
            while (scnr.hasNextLine()) {
                String algoString = scnr.nextLine();
                
                if (algoString.isEmpty())
                    continue;
                
                int sepIndex = findSeperator(algoString, 2, '}');
                
                String rsSeqsString = algoString.substring(0, sepIndex + 1);
                String costString = algoString.substring(sepIndex + 2, algoString.length());
                
                String[] costsArr = costString.split(",");
                
                Integer[] costs = new Integer[costsArr.length];
                
                //Account for extra comma
                for (int i = 0; i < costsArr.length; i++) {
                    costs[i] = Integer.parseInt(costsArr[i]);
                }
                
                int reqSepIndex = findSeperator(rsSeqsString, 1, '}');
                String reqString = rsSeqsString.substring(0, reqSepIndex + 1);
                String servString = rsSeqsString.substring(reqSepIndex + 2, rsSeqsString.length());
                
                Integer[] reqSeq = parseSequence(reqString);
                Integer[] servers = parseSequence(servString);
                
                algos.add(new Algorithm(reqSeq, servers, costs[0], costs[1], costs[2], costs[3],
                                costs[4], costs[5], costs[6]));
            }
            
            algoArr = algos.toArray(new Algorithm[algos.size()]);
            Arrays.sort(algoArr);
            
            File outFile = new File(outFileString);
            pWriter = new PrintWriter(outFile);
            
            pWriter.println("Line Length: " + algos.get(0).getLineLength());
            pWriter.println("Number of Servers: " + algos.get(0).getNumServers());
            pWriter.println("RequestSequence, Servers, opt_cost, greedy_cost, wfa_cost,"
                            + " cp_cost, dc_cost, harmonic_cost, hst_cost");
            
            for (Algorithm algo : algoArr) {
                pWriter.println(algo.getSortedCSV());
            }
            
        } catch(IOException e) {
            System.out.println("Failed");
            e.printStackTrace();
        } finally {
            if (scnr != null)
                scnr.close();
            if (pWriter != null)
                pWriter.close();
        }
        
    }

    private static Integer[] parseSequence(String sequence) {
        //Get rid of { }
        sequence = sequence.substring(1);
        sequence = sequence.substring(0, sequence.length() - 1);
        
        String[] nums = sequence.split(",");
        Integer[] costs = new Integer[nums.length];
        
        for (int i = 0; i < costs.length; i++) {
            costs[i] = Integer.parseInt(nums[i]);
        }
        
        return costs;
    }
    
    private static int findSeperator(String input, int count, char target) {
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == target)
                count--;
            
            if (count == 0)
                return i;
        }
        
        return -1;
    }
}
