import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class Algorithm implements Comparable {
    
    private Integer[] requestSequence;
    private Integer[] servers;

    private Flags requestFlag;
    private Flags serverFlag;
    
    //Used to keep track of which requests are matched up to which server
    private int[] state;
    private int optCost;
    
    //WFA weight
    private int eps;
    
    private int lineLength;
    
    private TreeSet<Integer> availableServers;
    
    //Array of the algorithms to run
    private boolean[] methods;
    
    private int numServers;
    
    private ArrayList<String> csvStrings;
    
    //These fields were used to sort the Algorithms based on optCost
    private Integer[] costs;
    private int[] optState;
    
    public Algorithm(Integer[] requestSequence, Integer[] servers, boolean[] methods,
                    Flags serverFlag, Flags requestFlag, int lineLength, int numServers, int eps) {
        
        if (requestSequence != null)
            this.requestSequence = deepCopy(requestSequence);
        if (servers != null)
            this.servers = deepCopy(servers);
        
        this.methods = methods.clone();
        
        this.requestFlag = requestFlag;
        this.serverFlag = serverFlag;
        
        optState = new int[numServers];
        this.state = new int[numServers];
        
        this.lineLength = lineLength;
        this.numServers = numServers;
        
        this.eps = eps;       
        
        this.csvStrings = new ArrayList<String>();
    }
    
    /**
     * This constructor was used to sort the algorithms based on their opt cost
     */
    public Algorithm(Integer[] reqSeq, Integer[] servSeq, Integer optCost, Integer greedyCost,
                    Integer wfaCost, Integer cpCost, Integer dcCost, Integer harmonicCost, 
                    Integer hstCost) {
        
        costs = new Integer[7];
        
        this.requestSequence = reqSeq;
        this.servers = servSeq; 
        
        this.optCost = optCost;
        
        costs[0] = optCost;
        costs[1] = greedyCost;
        costs[2] = wfaCost;
        costs[3] = cpCost;
        costs[4] = dcCost;
        costs[5] = harmonicCost;
        costs[6] = hstCost;
    }
   
    /**
     * Used for outputting the sorted algorithms based on opt cost
     * @return
     */
    public String getSortedCSV() {
        return sequenceToString(this.requestSequence) + "," + sequenceToString(this.servers) + 
                        "," + this.costs[0] + "," + this.costs[1] + "," + this.costs[2] + ","
                        + this.costs[3] + "," + this.costs[4] + "," + this.costs[5] + 
                        "," + this.costs[6];
    }
    /**
     * Calculate the optimal matchings
     * @param requestSequece
     * @param servers
     * @return
     */
    public int calcOPT(Integer[] requestSequence, Integer[] servers, boolean setOptValues) {
        TreeSet<Integer> availableServers = initServers(servers);
        int optCost = 0;
        int[] optState = new int[servers.length];
        
        
        int[] sortedRequests = new int[requestSequence.length];

                        
        for (int i = 0; i < requestSequence.length; i++) {
            sortedRequests[i] = requestSequence[i];
        }
        
        Arrays.sort(sortedRequests);
        
        /*
         * Optimal Algo
         * 
         */
        
        for (int i = 0; i < sortedRequests.length; i++) {
            if (availableServers.floor(sortedRequests[i]) == null) {
                //Get the closest higher server
                
                optState[i] = availableServers.higher(sortedRequests[i]);
                availableServers.remove(optState[i]);
                
                
            } else {
                //Else get the lowest possible server
                optState[i] = availableServers.pollFirst();
            }
        }
        
        for (int i = 0; i < optState.length; i++) {
            optCost += Math.abs(optState[i] - sortedRequests[i]);
        }
        
        if (setOptValues) {
            this.optCost = optCost;
            this.optState = optState;
        }
        
        return optCost;        
    }
    
    /**
     * Run the greedy algorithm on the request sequence
     * @return
     */
    public int greedy() {
        availableServers = initServers(this.servers);
                
        for (int i = 0; i < requestSequence.length; i++) {
            
            int req = requestSequence[i];
            

            Integer lower = availableServers.floor(req);
            Integer higher = availableServers.ceiling(req);
            
            if (lower != null && higher != null) {
                int server = (req - lower) > (higher - req) ? higher : lower;
                state[i] = server;
                availableServers.remove(server);
            }
            else if (lower != null) {
                state[i] = lower;
                availableServers.remove(lower);
            } 
            else {
                state[i] = higher;
                availableServers.remove(higher);
            }
            
        }
        
        return calcCost(this.state, this.requestSequence);       
    }
    
    /**
     * Run the WFA algorithm on the request sequence
     * @param g: the weighted value of the WFA function
     * @return
     */
    public int WFA(int g) {
        TreeSet<Integer> matchedServers = new TreeSet<Integer>();
        availableServers = initServers(this.servers);
        
        ArrayList<Integer> pastRequests = new ArrayList<Integer>();
                        
        for (int i = 0; i < requestSequence.length; i++) {
            pastRequests.add(requestSequence[i]);
            
            Integer[] servArr = availableServers.toArray(new Integer[availableServers.size()]);
            
            int min = Integer.MAX_VALUE;
            int server = -1;
            
            //Loop through the available servers, then apply the WFA formula to each
            for (int serv : servArr) {
                matchedServers.add(serv);
                
                Integer[] pReqs = pastRequests.toArray(new Integer[pastRequests.size()]);
                Integer[] matchedServs = matchedServers.toArray(new Integer[matchedServers.size()]);
                
                
                int val = g * calcOPT(pReqs, matchedServs, false) + Math.abs(serv
                                - requestSequence[i]);
                
                
                
                if (val < min) {
                    min = val;
                    server = serv;
                }
                
                matchedServers.remove(serv);
            }
            
            matchedServers.add(server);
            availableServers.remove(server);
            this.state[i] = server;
        }
        
        return calcCost(state, requestSequence);
    }
    
    /**
     * Run the cowpath algorithm
     * @param direction direction to start in
     * @return
     */
    public int cowPath(boolean direction) {
        availableServers = initServers(this.servers);
        
        for (int i = 0; i < requestSequence.length; i++) {
            int distance = 1;
            int curPos = requestSequence[i];
            
            walkPath(true, curPos, distance, i);
        }
        
        return calcCost(state, requestSequence);
    }
    
    /**
     * Recursive algorithm. Walk along the current path, and if the server isn't found,
     * reverse directions and double the distance
     * @param right true if right, false if going left
     * @param curPos current position on the lne
     * @param distance distance to travel
     * @param reqIndex index of the request
     */
    private void walkPath(boolean right, int curPos, int distance, int reqIndex) {
       
        //Increment curPos as you walk along the path, until you reach a server, end of the 
        //line, or the end of the distance

        
        if(checkPosition(curPos)) {
            state[reqIndex] = curPos;
            return;
        }
        
        if (availableServers.isEmpty())
            return;
        
        
        for (int i = 0; i < distance; i++) {
            curPos = right ? curPos + 1 : curPos - 1;
           
            
            if (curPos == lineLength + 1 || curPos == -1) {
                curPos = right ? curPos - 1 : curPos + 1;
                walkPath(!right, curPos, lineLength, reqIndex);
            }
            
            if(checkPosition(curPos)) {
                state[reqIndex] = curPos;
                return;
            }

        }

        
        walkPath(!right, curPos, distance * 2, reqIndex);
            
    }
    
    /**
     * Check the current position on the cow path
     * @param pos
     * @return
     */
    private boolean checkPosition(int pos) {
        if (availableServers.contains(pos)) {
            availableServers.remove(pos);
            return true; 
        }
        
        return false;
    }
    
    /**
     * Run the harmonic algorithm on the given request sequence and servers
     * @return
     */
    public int harmonic() {
        availableServers = initServers(this.servers);
        
        for (int i = 0; i < requestSequence.length; i++) {
            
            //Get the two closest servers to the left and right
            Integer left = availableServers.floor(requestSequence[i]);
            Integer right = availableServers.ceiling(requestSequence[i]);
            
            //Look for cases where the request is on the edge of the line
            if (left != null && right != null) {
                Integer chosenServer = calcHarmonicServer(requestSequence[i], left, right);
                state[i] = chosenServer;
                availableServers.remove(chosenServer);
                
            } else if (left != null) {
                state[i] = left;
                availableServers.remove(left);
                
            } else {
                state[i] = right;
                availableServers.remove(right);
            }
        }
        
        return calcCost(state, requestSequence);
    }
    
    /**
     * Run the double coverage algo on a given request sequence and servers
     * @return
     */
    public int doubleCoverage() {
        int[][] curServs = new int[servers.length][2];
        
        for (int i = 0; i < servers.length; i++) {
            curServs[i][0] = servers[i];
            curServs[i][1] = servers[i];
        }
        
        //The adjusted servers due to double coverage
        TreeSet<Integer> shadowServers = initServers(this.servers);
        
        
        for (int i = 0; i < requestSequence.length; i++) {
            
            Integer request = requestSequence[i];
            
            Integer right = shadowServers.ceiling(request);
            Integer left = shadowServers.floor(request);
            
            int server = -1;
            int cost = 0;
            
            //Check for edge cases when the request is on the edge of the line
            if (right != null && left != null) {
                server = (request - left) > (right - request) ? right : left;
                
                int index = findShadowServer(curServs, server);
                
                
                shadowServers.remove(server);
                curServs[index][1] = -1;
                
                cost = Math.abs(curServs[index][0] - request);
                
                //Adjust the shadow servers based on the cost of the chosen server

                if (left.equals(server) && !(right.equals(left))) {
                    shadowServers.remove(right);
                    shadowServers.add(right - cost);
                    
                    int rightIndex = findShadowServer(curServs, right);
                    curServs[rightIndex][1] = right - cost;
                    
                } else if (right.equals(server) && !(left.equals(right))) {
                    shadowServers.remove(left);
                    shadowServers.add(left + cost);
                    
                    int leftIndex = findShadowServer(curServs, left);
                    curServs[leftIndex][1] = left + cost;
                }
                
                state[i] = curServs[index][0];
                
            } else if (right != null) {
                shadowServers.remove(right);
                
                int rightIndex = findShadowServer(curServs, right);
                curServs[rightIndex][1] = -1;
                state[i] = curServs[rightIndex][0];
            } else {
                shadowServers.remove(left);

                try {
                    int leftIndex = findShadowServer(curServs, left);
                    curServs[leftIndex][1] = -1;
                    state[i] = curServs[leftIndex][0];
                }
                catch(Exception e) {
                    System.out.println(left);
                }
            }
        }
        
        return calcCost(state, requestSequence);
    }
    
    /**
     * Find actual server corresponding to the given shadow server
     * @param servArr
     * @param shadowServ
     * @return index of the actual server
     */
    private int findShadowServer(int[][] servArr, int shadowServ) {
        for (int i = 0; i < servArr.length; i++ ) {
            if (servArr[i][1] == shadowServ) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Calculate the server to use in the harmonic algo
     * @param request
     * @param left
     * @param right
     * @return
     */
    private Integer calcHarmonicServer(int request, Integer left, Integer right) {
        double d1 = request - left;
        double d2 = right - request;
        
        double probLeft = d1 / (d1 + d2);
        
        if (Math.random() <= probLeft) {
            return left;
        }
        
        return right;
    }

    /**
     * Initialize the available servers
     * @param servers
     * @return
     */
    private TreeSet<Integer> initServers(Integer[] servers) {
        TreeSet<Integer> retServers = new TreeSet<Integer>();
        
        for (int x : servers) {
            retServers.add(x);
        }
        
        return retServers;
    }
    
    /**
     * Calculate the cost of a given algorithm
     * @param state
     * @param reqSequence
     * @return
     */
    public int calcCost(int[] state, Integer[] reqSequence) {
        int cost = 0;
        
        for (int i = 0; i < state.length; i++) {
            cost += Math.abs(state[i] - reqSequence[i]);
        }
        
        return cost;
    }

    /**
     * Do a deep copy of a given Integer Algorithm
     * @param original
     * @return
     */
    public Integer[] deepCopy(Integer[] original) {
        Integer[] retArr = new Integer[original.length];
        
        for (int i = 0; i < original.length; i++) {
            int val = original[i];
            retArr[i] = val;
        }
        
        return retArr;
    }

    /**
     * Calculate which request sequences and server positions to use. Then run all of the required
     * algorithms on the requests/servers
     */
    public void run() {
        ArrayList<Integer[]> allRequests = new ArrayList<Integer[]>();
        ArrayList<Integer[]> allServers = new ArrayList<Integer[]>();
        
        
        if (requestFlag == Flags.fixed) {
            allRequests.add(requestSequence);
        } else if (requestFlag == Flags.all) {
            Integer[] root = new Integer[numServers];
            setToZero(root);
            allRequests.add(root);
            
            calcPermutations(root, root.length - 1, allRequests);
                        
        } else {
            Random randGend = new Random();
            Integer[] reqs = new Integer[numServers];
            
            for (int i = 0; i < numServers; i++) {
                reqs[i] = randGend.nextInt(lineLength);
            }
            
            allRequests.add(reqs);
        }
        
        if (serverFlag == Flags.fixed) {
            allServers.add(servers);
        } else if (serverFlag == Flags.all) {
            calcServers(allServers);
        } else {
            Random randGen = new Random();
            Integer[] servs = new Integer[numServers];
            
            for (int i = 0; i < numServers; i++) {
                servs[i] = randGen.nextInt(lineLength);
            }
            
            allServers.add(servs);
        }
                
        
        for (Integer[] reqSeq : allRequests) {
            this.requestSequence = reqSeq;
            
            for (Integer[] servs : allServers) {
                
                
                if (!isValidServs(servs))
                    continue;
                
                this.servers = servs;
                
                String tempString = "";
                
                tempString += sequenceToString(requestSequence) + ",";
                tempString += sequenceToString(servers) + ",";
                
                tempString += calcOPT(reqSeq, servers, false);
                                
                for (int i = 0; i < methods.length; i++) {
                    int cost = -1;
                    if (methods[i]) {
                        cost = runMethod(i);
                    }
                    
                    tempString += "," + cost;
                }
                
                tempString += "\n";    
                csvStrings.add(tempString);
            }
        }
        
    }

    /**
     * Run the algorithm corresponding to an index i
     * @param i
     * @return
     */
    private int runMethod(int i) {
        int cost = 0;
        
        switch(i) {
            case 0:
                cost = greedy();
                break;
            case 1:
                cost = WFA(this.eps);
                break;
            case 2:
                cost = cowPath(true);
                break;
            case 3:
                try {
                    cost = doubleCoverage();
                } catch (Exception e) {
                    cost = -5;
                    System.out.println("Error in DC.");
                }
                break;
            case 4:
                cost = harmonic();
                break;               
        }
        
        return cost;
    }
    
    /**
     * Calculate the permutations for the request sequences
     * @param root The root of the permutations that follow it
     * @param index Index of the array to change
     * @param allPerms ArrayList of all the permutations calculated
     */
    public void calcPermutations(Integer[] root, int index,
                    ArrayList<Integer[]> allPerms) {
                
        if (index == -1)
            return;
        
        calcPermutations(root, index - 1, allPerms);
        
        for (int i = 0; i < lineLength; i++) {
            Integer[] child = deepCopy(root);
            child[index] = i;
            
            if (!Arrays.deepEquals(child, root)) {
                allPerms.add(child);
                calcPermutations(child, index - 1, allPerms);
            }         
        }
    }
    
    /**
     * Calculate the combinations of the servers
     * @param arrL
     */
    public void calcServers(ArrayList<Integer[]> arrL) {
        
        Integer[] servs = new Integer[this.numServers];
        
        for (int i = 0; i < servs.length; i++) {
            servs[i] = i;
        }
        
        arrL.add(servs);
        
        int[] pointers = new int[this.numServers];
        boolean[] moving = new boolean[this.numServers];
        moving[moving.length - 1] = true;
        int numMoving = 1;
        
        for (int i = 0; i < pointers.length; i++) {
            pointers[i] = servs[i];
        }
        
        while (pointers[0] < this.lineLength - this.numServers + 1) {
            if (pointers[pointers.length - 1] == this.lineLength) {
                
                if (!moving[0] && pointers[pointers.length - numMoving] == this.lineLength - 
                                (numMoving - 1)) {
                    numMoving++;
                    moving[moving.length - numMoving] = true;
                    
                    for (int i = pointers.length - numMoving; i < pointers.length; i++) {
                        if (i == pointers.length - numMoving)
                            pointers[i]++;
                        else 
                            pointers[i] = pointers[i - 1] + 1;
                    }
                } else {
                    if (pointers[pointers.length - numMoving + 1] == this.lineLength - 
                                    (numMoving - 1 - 1)) {
                        pointers[pointers.length - numMoving]++;
                        
                        for (int i = pointers.length - numMoving + 1; i < pointers.length; i++) {
                            pointers[i] = pointers[i - 1] + 1;
                        }
                    } else {
                        for (int i = pointers.length - numMoving + 1; 
                                        i < pointers.length;i++) {
                            pointers[i]++;
                        }
                        pointers[pointers.length - 1] = pointers[pointers.length - 2] + 1;
                    }
                }
                
            } else {
                pointers[pointers.length - 1]++;
            }
            
            Integer[] newServs = new Integer[servs.length];
            
            for (int i = 0; i < servs.length; i++) {
                newServs[i] = pointers[i];
            }
            
            arrL.add(newServs);
        }
        
    }
    
    /**
     * Set an Integer array to zero
     * @param array
     */
    private void setToZero(Integer[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 0;
        }
    }
    
    /**
     * Get the CSV strings of an algorithm
     * @return
     */
    public ArrayList<String> getCSV() {
        return this.csvStrings;
    }
    
    /**
     * Return a string representation of an integer sequence
     * @param sequence
     * @return
     */
    private String sequenceToString(Integer[] sequence) {
        String retString = "{";
        
        for (int i = 0; i < sequence.length; i++) {
            retString += sequence[i];
            if (i != sequence.length - 1)
                retString += "-";
        }
        
        return retString + "}";
    }
    
    /**
     * Check to see if there are any repeat servers. Obsolete now that servers are combinations
     * @param servs
     * @return
     */
    private boolean isValidServs(Integer[] servs) {
        Set<Integer> checkSet = new TreeSet<Integer>();
        
        for (Integer serv : servs) {
            if (!checkSet.add(serv))
                return false;
        }
        
        return true;
    }
    
    public int getLineLength() {
        return this.lineLength;
    }
    
    public int getNumServers() {
        return this.numServers;
    }


    public int getOptCost() {
        return this.optCost;
    }


    /**
     * Compare two Algorithms based on optCost
     */
    @Override
    public int compareTo(Object obj) {
        if (obj instanceof Algorithm) {
            Algorithm algo = (Algorithm) obj;
            return this.optCost - algo.getOptCost();
        }
        
        //Should never be reached
        return 0;
    }
    
    
}
