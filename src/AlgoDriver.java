import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class AlgoDriver {
    public static void main(String[] args) {
        AlgoInput al = null;
        
        try {
            File inputFile = new File("algorithms.txt");
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            al = new AlgoInput();
            saxParser.parse(inputFile, al);     
         } catch (Exception e) {
            e.printStackTrace();
         }
        
        ArrayList<Algorithm> allAlgos = al.algos;
        PrintWriter pWriter = null;
        
        try {
            File outputFile = new File("./output/5-9.txt");
            pWriter = new PrintWriter(new FileOutputStream(outputFile, true));
            
            //{"Greedy", "wfa", "cowpath", "dc", "harmonic", "hst"};
            
            int lineLength = allAlgos.get(0).getLineLength();
            int numServers = allAlgos.get(0).getNumServers();
            
            pWriter.println("Line Length: " + lineLength);
            pWriter.println("Number of Servers: " + numServers + "\n");
            pWriter.println("RequestSequence, Servers, opt_cost, greedy_cost, wfa_cost,"
                            + " cp_cost, dc_cost, harmonic_cost, hst_cost\n");
            
            for (Algorithm algo : allAlgos)  {
                algo.run();
                ArrayList<String> csvStrings = algo.getCSV();
                
                for (String s : csvStrings) {
                    pWriter.println(s);
                }
            }
            
        } catch (IOException e) {
            System.out.println("Error writing to file");
        } finally {
            if (pWriter != null)
                pWriter.close();
        }
    }
}
