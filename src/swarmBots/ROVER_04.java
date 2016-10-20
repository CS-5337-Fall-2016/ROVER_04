package swarmBots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import rover_logic.SearchLogic;
import supportTools.CommunicationHelper;
import common.Communication;
import common.Coord;
import common.MapTile;
import common.ScanMap;
import enums.RoverDriveType;
import enums.Science;
import enums.Terrain;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 */
//This Rover was learned and implemented using ROVER11 as an sample, For the implementation of A* was already available 
//at Search logic provided by our professor so understood the working of search logic & A* implemented as needed 
//using internet and previous work done by some ppl on github as sample
public class ROVER_04 {

    BufferedReader in;
    PrintWriter out;
    String rovername;
    ScanMap scanMap;
    int sleepTime=150;
    String SERVER_ADDRESS = "localhost";
    static final int PORT_ADDRESS = 9537;
    public static Map<Coord, MapTile> globalMap;
    List<Coord> dests;
    long trafficCounter;
    static final long walkerDelay = TimeUnit.MILLISECONDS.toMillis(1230);
    List<Coord> visited =new ArrayList<Coord>();
    List<Coord> pLocationList =new ArrayList<Coord>();
    int pLocationListCount=0,pLocationListSize=7;
   // ROVER_04 rover = new ROVER_04();
   
    boolean goingSouth = false,traverseJackpot=Boolean.FALSE;
    boolean goingEast = false;
    boolean goingWest = false;
    boolean goingNorth = false;
    boolean goingHorizontal = false;
    boolean blockedByRover = false;
    boolean blocked = false;
  
    public ROVER_04() {
        // constructor
    	rovername = "ROVER_04";
        System.out.println(rovername + "rover object constructed");
        
        SERVER_ADDRESS = "localhost";
        // this should be a safe but slow timer value
        sleepTime = 300; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        globalMap = new HashMap<>();
        dests = new ArrayList<>();
    }
   
    public ROVER_04(String serverAddress) {
        // constructor
    	rovername = "ROVER_04";
        System.out.println(rovername + "rover object constructed");
        SERVER_ADDRESS = serverAddress;
        sleepTime = 300; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        globalMap = new HashMap<>();
        dests = new ArrayList<>();
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException, InterruptedException {

        // Make connection to SwarmServer and initialize streams
        Socket socket = null;
        try {
            socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set Address & port here

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
          
            

            while (true) {
                String line = in.readLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(rovername); // This sets the name of this instance
                                            // of a swarmBot for identifying the
                                            // thread to the server
                    break;
                }
            }
   
         
            // ********* Rover logic setup *********
           
            String line = "";
            Coord cLoc = null;
            Coord pLoc = null;
            Coord roverStartPosition = null;
            Coord tLoc = null;
            Coord destination = null;
           
            /**
             *  Get initial values that won't change
             */
            // **** get equipment listing ****           
            ArrayList<String> equipment = new ArrayList<String>();
            equipment = getEquipment();
            System.out.println(rovername + " equipment list results " + equipment + "\n");
           
           
            // **** Request START_LOC Location from SwarmServer ****
            out.println("START_LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + " check connection to server");
                line = "";
            }
            if (line.startsWith("START_LOC")) {
            	roverStartPosition = extractLocationFromString(line);
               
            }
            System.out.println(rovername + " START_LOC " + roverStartPosition);
           
           
            // **** Request TARGET_LOC Location from SwarmServer ****
            out.println("TARGET_LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + " check connection to server");
                line = "";
            }
            if (line.startsWith("TARGET_LOC")) {
                tLoc = extractLocationFromString(line);
            }
            System.out.println(rovername + " TARGET_LOC " + tLoc);
           
            // ******* destination *******
            // TODO: Sort destination depending on current Location
            //A* Logic Implementation
            SearchLogic logicA = new SearchLogic();
            
            // ******** define Communication

          String url = "http://localhost:3000/api";
          String corp_secret = "gz5YhL70a2";

          Communication com = new Communication(url, rovername, corp_secret);

          boolean beenToJackpot = false;
          boolean ranSweep = false;

          long startTime;
          long estimatedTime;
          long sleepTime2;

          // Get destinations from Sensor group. I am a driller!
          List<Coord> Obstacleblock = new ArrayList<>();

          //destinations.add(tLoc);
          //TODO: implement   target location 
          	//Coord destination = null;
          
            boolean stuck = false; // just means it did not change locations between requests,
            blocked = false; // could be velocity limit or obstruction etc.
           
   
            String[] cardinals = new String[4];
            cardinals[0] = "N";
            cardinals[1] = "E";
            cardinals[2] = "S";
            cardinals[3] = "W";
   
            String currentDir = cardinals[0];
           
            String dir;
            
            /**
             *  ####  Rover controller process loop  ####
             */
            while (true) {
               
            	startTime = System.nanoTime();
            	cLoc = getCurrentLoaction();
                   
                // after getting location set previous equal current to be able to check for stuckness and blocked later
                pLoc = cLoc;       
                           
               
                // tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
                MapTile[][] scanMapTiles =getScanMapTiles();
                int centerIndex = (scanMap.getEdgeSize() - 1)/2;
                updateglobalMap(cLoc, scanMapTiles);
                System.out.println("post message: " + com.postScanMapTiles(cLoc, scanMapTiles));
                if (trafficCounter % 5 == 0) {
                    updateglobalMap(com.getGlobalMap());
                 // ********* get closest destination from current location everytime
                    if (!dests.isEmpty()) {
                        destination = getClosestDestination(cLoc);
                    }

                }
                trafficCounter++;
               
               
                // ***** get TIMER remaining *****
                out.println("TIMER");
                line = in.readLine();
                if (line == null) 
                {
                    System.out.println(rovername + " check connection to server");
                    line = "";
                }
                if (line.startsWith("TIMER")) 
                {
                    String timeRemaining = line.substring(6);
                    System.out.println(rovername + " timeRemaining: " + timeRemaining);
                    
                }
               
                
                // ***** MOVING *****
                // try to look for Jackpot
                if (!beenToJackpot){
                    destination = tLoc;
                }
                if (logicA.targetVisible(cLoc, tLoc)) 
                {
                    if (!beenToJackpot)
                    {
                        beenToJackpot = true;
                        JackPotDestinations(tLoc);
                    }
                }
//                if (logicA.getDistance(cLoc, destination) < 301) {
//                if (logicA.validateTile(globalMap.get(destination), RoverDriveType.WALKER)) {
//                    if (logicA.targetVisible(cLoc, tLoc)) {
//             	       scanMapTiles = scanMap.getScanMap();
//             	       out.println("Collecting sciences from the destination !!!");  
//             	       sweep(tLoc, scanMapTiles);             
//                    }
//                    else {
//                    	/** if no destination, get the closest destination **/
//                    	Coord dest = rover.getClosestDestination(cLoc);
//                    	if (logicA.targetVisible(cLoc, dest)) {
//                        	scanMapTiles = scanMap.getScanMap();
//                        	sweep(dest, scanMapTiles);  
//                        }
//                    }
//                 }
//             }
//             
//             boolean ans = isOccupied(cLoc);
//             if(ans) {
//             	//do something
//             	//double d = SearchLogic.getDistance(currentLoc,rover.getClosestDestination(currentLoc));
//             }
             else {
 }
                // IMPLEMENT A* ALG to make the rover move fast and short path
                if (destination == null) // no destination
                {
                    if (!dests.isEmpty())
                    {
                        destination = getClosestDestination(cLoc);
                    }
                }
                else {
                	List<String> positions = logicA.Astar(cLoc, tLoc, scanMapTiles, RoverDriveType.WALKER, globalMap);
                    
                    //System.out.println(rovername + " moves: " + positions.toString());
                    
                    System.out.println(rovername + "currentLoc: " + cLoc + ", destination: " + destination);

                
                if (!positions.isEmpty())
                {
                    out.println("MOVE " + positions.get(0));
                    // if rover is next to the target it uses greedy algo
                    if (logicA.targetVisible(cLoc, destination)) 
                    {
                    	System.out.println("Walking towards target destination");
                    	
                    	// validation to reach the target destination in A* algo
                    	if (logicA.validateTile(globalMap.get(destination), RoverDriveType.WALKER)) 
                    	{
                            System.out.println("Target can be reached no obstacle");
                    	} 
                    	else {
                    		System.out.println("Unable to reach target due to obstacles");
                    		Obstacleblock.add(destination); // Obstacles 
                    		dests.remove(destination); // remove the destination 
                            destination = getClosestDestination(cLoc); // Get a new close point near the destination
                            System.out.println("Obstacle blockfound. Switch target to new coord: " + destination);
                    	}
                    }
				}	
               
                else { // Reached Destination
                	if (cLoc.equals(destination))
                	{
                		System.out.println("Verifying Destination!!!!");
                		//Verify Destination
                        if (!dests.isEmpty())
                        {
                        	dests.remove(destination);
                         // create a new point close to destination to check if we actually are in destination 
                            destination = getClosestDestination(cLoc); 
                            System.out.println(rovername + " Moving to destination at: " + destination);
                        } else {
                            System.out.println("Final Destination Reached!!! Target Accomplished !!");
                        }

                    } else {

                    }
                }
            }
            // test for stuckness
                stuck = cLoc.equals(pLoc);
   
                System.out.println("ROVER_04 stuck test " + stuck);
   
                // TODO - logic to calculate where to move next
                // this is the Rovers HeartBeat, it regulates how fast the Rover cycles through the control loop
                estimatedTime = System.nanoTime() - startTime;
                sleepTime2 = walkerDelay - TimeUnit.NANOSECONDS.toMillis(estimatedTime);
                if (sleepTime2 > 0) {
                	Thread.sleep(sleepTime2);}
                System.out.println(rovername +" ------------ bottom process control --------------");
            }
       
        // This catch block closes the open socket connection to the server
        } catch	(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("ROVER_04 problem closing socket");
                }
            }
        }

    } 
    //*********** END of Rover main control loop****************
    //Funtions for the activites of the rover//
    //Get the destination for Pot of luck
private void JackPotDestinations(Coord potofluck) {
		// TODO Auto-generated method stub
	 int xp = potofluck.xpos-3; // X axis
     int yp = potofluck.ypos-3; // Y axis

     for (int i = 0 ; i < 7; i = i + 6)
     {
         for (int j = 0; j < 7; j = j + 6)
         {
             Coord coord = new Coord(xp + i, yp + j);
             dests.add(coord);
         }
}}


    private MapTile[][] getScanMapTiles() throws Exception {

        // gets the scanMap from the server based on the Rover current location
        doScan();
        // prints the scanMap to the Console output for debug purposes
        	scanMap.debugPrintMap();
         return scanMap.getScanMap();
         
    }	

    private void updateglobalMap(Coord currentLoc, MapTile[][] scanMapTiles) {
        int centerIndex = (scanMap.getEdgeSize() - 1) / 2;

        for (int row = 0; row < scanMapTiles.length; row++) {
            for (int col = 0; col < scanMapTiles[row].length; col++) {

                MapTile mapTile = scanMapTiles[col][row];

                int xp = currentLoc.xpos - centerIndex + col;
                int yp = currentLoc.ypos - centerIndex + row;
                Coord coord = new Coord(xp, yp);
                globalMap.put(coord, mapTile);
            }
        }
        MapTile currentMapTile = scanMapTiles[centerIndex][centerIndex].getCopyOfMapTile();
        currentMapTile.setHasRoverFalse();
        globalMap.put(currentLoc, currentMapTile);
    }
    //providing a point close to destination and calc the distance between target and destination
    private Coord getClosestDestination(Coord currentLoc) {
        double max = Double.MAX_VALUE;
        Coord target = null;

        for (Coord desitnation : dests) {
            double distance = SearchLogic.getDistance(currentLoc, desitnation);
            if (distance < max) {
                max = distance;
                target = desitnation;
            }
        }
        return target;
    }
    private Coord getCurrentLoaction() throws Exception {
        String line;
        Coord currentLoc=null;
        out.println("LOC");
        line = in.readLine();
        if(line == null){
            System.out.println(rovername + "check connection to server");
            line = "";
        }
        if (line.startsWith("LOC")) {
            currentLoc = extractLocationFromString(line);
            System.out.println(rovername + " currentLoc at start: " + currentLoc);
        }
       
        return currentLoc;
       
    }

 // get data from server and update field map
    private void updateglobalMap(JSONArray data) {

        for (Object o : data) {

            JSONObject jsonObj = (JSONObject) o;
            boolean marked = (jsonObj.get("g") != null) ? true : false;
            int x = (int) (long) jsonObj.get("x");
            int y = (int) (long) jsonObj.get("y");
            Coord coord = new Coord(x, y);

            // only bother to save if our globalMap doesn't contain the coordinate
            if (!globalMap.containsKey(coord)) {
                MapTile tile = CommunicationHelper.convertToMapTile(jsonObj);

                // if tile has science AND is not in sand
                if (tile.getScience() != Science.NONE && tile.getTerrain() != Terrain.SAND) {

                    // then add to the destination
                    if (!dests.contains(coord) && !marked)
                    	dests.add(coord);
                }

                globalMap.put(coord, tile);
            }
        }
    }


    // ####################### Support Methods #############################
   

       // method to retrieve a list of the rover's EQUIPMENT from the server
    private ArrayList<String> getEquipment() throws IOException {
        //System.out.println("ROVER_04 method getEquipment()");
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();
        out.println("EQUIPMENT");
       
        String jsonEqListIn = in.readLine(); //grabs the string that was returned first
        if(jsonEqListIn == null){
            jsonEqListIn = "";
        }
        StringBuilder jsonEqList = new StringBuilder();
        //System.out.println("ROVER_04 incoming EQUIPMENT result - first readline: " + jsonEqListIn);
       
        if(jsonEqListIn.startsWith("EQUIPMENT")){
            while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
                if(jsonEqListIn == null){
                    break;
                }
                //System.out.println("ROVER_04 incomming EQUIPMENT result: " + jsonEqListIn);
                jsonEqList.append(jsonEqListIn);
                jsonEqList.append("\n");
                //System.out.println("ROVER_04 doScan() bottom of while");
            }
        } else {
            // in case the server call gives unexpected results
            clearReadLineBuffer();
            return null; // server response did not start with "EQUIPMENT"
        }
       
        String jsonEqListString = jsonEqList.toString();       
        ArrayList<String> returnList;       
        returnList = gson.fromJson(jsonEqListString, new TypeToken<ArrayList<String>>(){}.getType());       
        //System.out.println("ROVER_04 returnList " + returnList);
       
        return returnList;
    }
   
    // sends a SCAN request to the server and puts the result in the scanMap array
        public void doScan() throws IOException {
            //System.out.println("ROVER_04 method doScan()");
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .enableComplexMapKeySerialization()
                    .create();
            out.println("SCAN");

            String jsonScanMapIn = in.readLine(); //grabs the string that was returned first
            if(jsonScanMapIn == null){
                System.out.println(rovername +" check connection to server");
                jsonScanMapIn = "";
            }
            StringBuilder jsonScanMap = new StringBuilder();
            System.out.println(rovername +"incomming SCAN result - first readline: " + jsonScanMapIn);
           
            if(jsonScanMapIn.startsWith("SCAN")){   
                while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
                    //System.out.println("ROVER_04 incomming SCAN result: " + jsonScanMapIn);
                    jsonScanMap.append(jsonScanMapIn);
                    jsonScanMap.append("\n");
                    //System.out.println("ROVER_04 doScan() bottom of while");
                }
            } else {
                // in case the server call gives unexpected results
                clearReadLineBuffer();
                return; // server response did not start with "SCAN"
            }
            //System.out.println("ROVER_04 finished scan while");

            String jsonScanMapString = jsonScanMap.toString();
            // debug print json object to a file
            //new MyWriter( jsonScanMapString, 0);  //gives a strange result - prints the \n instead of newline character in the file

            //System.out.println("ROVER_04 convert from json back to ScanMap class");
            // convert from the json string back to a ScanMap object
            scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);       
        }
       

   
    // this takes the server response string, parses out the x and x values and
    // returns a Coord object   
    public static Coord extractLocationFromString(String sStr) {
        int indexOf;
        indexOf = sStr.indexOf(" ");
        sStr = sStr.substring(indexOf +1);
        if (sStr.lastIndexOf(" ") != -1) {
            String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
            //System.out.println("extracted xStr " + xStr);

            String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
            //System.out.println("extracted yStr " + yStr);
            return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
        }
        return null;
    }

    private void clearReadLineBuffer() throws IOException{
        while(in.ready()){
            //System.out.println("ROVER_04 clearing readLine()");
            in.readLine();   
        }
    }
    /**
     * Runs the client
     */
    public static void main(String[] args) throws Exception {

        ROVER_04 client = new ROVER_04();
        
        client.run();
    }
}
