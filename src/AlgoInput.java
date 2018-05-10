import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Servers come first -> requests
 * @author default
 *
 */

public class AlgoInput extends DefaultHandler {
    private boolean servers;
    private boolean requests;
    private boolean lengthOfLine;
    
    private int totalNum;
    private int length;
        
    private int eps;
    
    //Change to arraylists
    private ArrayList<Integer> requestSequence;
    private ArrayList<Integer> serverArr;
    
    // Used to check to see if the methods are present in the xml
    private final String[] methods = {"Greedy", "wfa", "cowpath", "dc", "harmonic", "hst"};
    private boolean[] methodArr;
    
    /*
     * serverFlags = {fixed, random, all}
     * requestFlags = {fixed, random, all}
     */
    private Flags serverFlag;
    private Flags requestFlag;
    
    public ArrayList<Algorithm> algos;
    
    public AlgoInput() {
        algos = new ArrayList<Algorithm>();
        methodArr = new boolean[6];
        
        servers = false;
        requests = false;
        
        totalNum = 0;
        
    }

    /**
     * Used on the start of an xml element. Only need to check for servers, requests, and the 
     * individual methods
     */
    @Override
    public void startElement(String uri, 
    String localName, String qName, Attributes attributes) throws SAXException {
       
        if (qName.equalsIgnoreCase("servers")) {
           
           //Get the server flags and parse them
           String flag = attributes.getValue("flag");

           serverFlag = checkFlags(flag);
           //Number of requests/servers
           totalNum = Integer.parseInt(attributes.getValue("num"));
           
           
           serverArr = new ArrayList<Integer>();

           servers = true;
           
       } else if (qName.equalsIgnoreCase("requests")) {
          
           //Parse the request flags
           String flag = attributes.getValue("flag");

           requestFlag = checkFlags(flag);
           
           requestSequence = new ArrayList<Integer>();
           requests = true;
           
       } else if (qName.equalsIgnoreCase("length")) {
           lengthOfLine = true;
       } else {
           //Check for all of the methods (Greedy, WFA, ...etc)
           for (int i = 0; i < methods.length; i++) {
               if (qName.equalsIgnoreCase(methods[i])) {
                   methodArr[i] = true;
                   
                   if (qName.equalsIgnoreCase("wfa")) {
                       this.eps = Integer.parseInt(attributes.getValue("eps"));
                   }
               }
           }
       }
       
    }

    /**
     * This event will only do something at the end of an algorithm tag. Adds the constructed 
     * algorithm to the list, and clears the fields
     */
    @Override
    public void endElement(String uri, 
    String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("algorithm")) {
            
            if (requestSequence.size() > totalNum || serverArr.size() > totalNum) {
                throw new SAXException("Request sequence or Server sequence did not match" +
                                " expected number.");
            }
        
            Integer[] reqs = requestSequence.toArray(new Integer[requestSequence.size()]);
            Integer[] servs = serverArr.toArray(new Integer[serverArr.size()]);
            
            algos.add(new Algorithm(reqs, servs, methodArr, 
                            serverFlag, requestFlag, this.length, this.totalNum, this.eps));
            
            requestSequence = null;
            serverArr = null;
            
            clearFlags(methodArr);
        }
    }

    /**
     * Parses the request sequence/servers
     */
    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
       
       if (servers) {
           String servString = new String(ch, start, length);
           
           if (this.serverFlag == Flags.fixed) {
               String[] servArr = servString.split(",");           
    
               for (String i : servArr) {
                   serverArr.add(Integer.parseInt(i.trim()));
               }
           }
           
           servers = false;
       } else if (requests) {
           String reqString = new String(ch, start, length);
           
           if (this.requestFlag == Flags.fixed) {
               String[] reqArr = reqString.split(",");
                          
               for (String i : reqArr) {
                   requestSequence.add(Integer.parseInt(i.trim()));
               }
           }
           
           requests = false;
       } else if (lengthOfLine) {
           String lenStr = new String(ch, start, length);
           this.length = Integer.parseInt(lenStr.trim());
           
           lengthOfLine = false;
       }
    }
    
    public void clearFlags(boolean[] flags) {
        for (int i = 0; i < flags.length; i++) {
            flags[i] = false;
        }
    }
    
    
    public Flags checkFlags(String value) {
        switch (value) {
            case "fixed":
                return Flags.fixed;
            case "random":
                return Flags.random;
            case "all":
                return Flags.all;
        }
        
        return null;
    }
}
