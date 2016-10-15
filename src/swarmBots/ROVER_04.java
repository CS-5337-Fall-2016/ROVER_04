package swarmBots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import enums.RoverToolType;
import enums.Science;
import enums.Terrain;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for
 * publishing their code examples
 */

public class ROVER_04 {

    BufferedReader in;
    PrintWriter out;
    String rovername;
    ScanMap scanMap;
    int sleepTime=150;
    String SERVER_ADDRESS = "localhost";
    static final int PORT_ADDRESS = 9537;
    public static Map<Coord, MapTile> globalMap;
    List<Coord> destinations;
    long trafficCounter;
    static final long walkerDelay = TimeUnit.MILLISECONDS.toMillis(1230);

   
    boolean goingSouth = false,traverseJackpot=Boolean.FALSE;
    boolean goingEast = false;
    boolean goingWest = false;
    boolean goingNorth = false;
    boolean goingHorizontal = false;
    boolean blocked = false;
    boolean blockedByRover = false;
   
  
    public ROVER_04() {
        // constructor
        System.out.println("ROVER_04 rover object constructed");
        rovername = "ROVER_04";
        SERVER_ADDRESS = "localhost";
        // this should be a safe but slow timer value
        sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        globalMap = new HashMap<>();
        destinations = new ArrayList<>();
    }
   
    public ROVER_04(String serverAddress) {
        // constructor
        System.out.println("ROVER_04 rover object constructed");
        rovername = "ROVER_04";
        SERVER_ADDRESS = serverAddress;
        sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        globalMap = new HashMap<>();
        destinations = new ArrayList<>();
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException, InterruptedException {

        // Make connection to SwarmServer and initialize streams
        Socket socket = null;
        try {
            socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS);

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
            Coord rovergroupStartPosition = null;
            Coord targetLocation = null;
           
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
                rovergroupStartPosition = extractLocationFromString(line);
               
            }
            System.out.println(rovername + " START_LOC " + rovergroupStartPosition);
           
           
            // **** Request TARGET_LOC Location from SwarmServer ****
            out.println("TARGET_LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + " check connection to server");
                line = "";
            }
            if (line.startsWith("TARGET_LOC")) {
                targetLocation = extractLocationFromString(line);
            }
            System.out.println(rovername + " TARGET_LOC " + targetLocation);
           
            // ******* destination *******
            // TODO: Sort destination depending on current Location

            SearchLogic search = new SearchLogic();
            
            // ******** define Communication
//          String url = "http://192.168.1.104:3000/api";
          String url = "http://localhost:3000/api";
          String corp_secret = "gz5YhL70a2";

          Communication com = new Communication(url, rovername, corp_secret);

          boolean beenToJackpot = false;
          boolean ranSweep = false;

          long startTime;
          long estimatedTime;
          long sleepTime2;

          // Get destinations from Sensor group. I am a driller!
          List<Coord> blockedDestinations = new ArrayList<>();


//          destinations.add(targetLocation);
          //TODO: implement sweep target location
          
          

          	Coord destination = null;


            boolean stuck = false; // just means it did not change locations between requests,
                                    // could be velocity limit or obstruction etc.
            blocked = false;
   
            String[] cardinals = new String[4];
            cardinals[0] = "N";
            cardinals[1] = "E";
            cardinals[2] = "S";
            cardinals[3] = "W";
   
            String currentDir = cardinals[0];
            Coord currentLoc = null;
            Coord previousLoc = null;
           
            String dir;
            /**
             *  ####  Rover controller process loop  ####
             */
            while (true) {
               
                currentLoc = getCurrentLoaction();
                   
                // after getting location set previous equal current to be able to check for stuckness and blocked later
                previousLoc = currentLoc;       
                           
               
                // tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
                MapTile[][] scanMapTiles =getScanMapTiles();
                int centerIndex = (scanMap.getEdgeSize() - 1)/2;
                updateglobalMap(currentLoc, scanMapTiles);
                System.out.println("post message: " + com.postScanMapTiles(currentLoc, scanMapTiles));
                if (trafficCounter % 5 == 0) {
                    updateglobalMap(com.getGlobalMap());

                }
                trafficCounter++;
               
               
                // ***** get TIMER remaining *****
                out.println("TIMER");
                line = in.readLine();
                if (line == null) {
                    System.out.println(rovername + " check connection to server");
                    line = "";
                }
                if (line.startsWith("TIMER")) {
                    String timeRemaining = line.substring(6);
                    System.out.println(rovername + " timeRemaining: " + timeRemaining);
}
               
                
                // ***** MOVING *****
                // try moving east 5 block if blocked
                if(blockedByRover)
                {
                    dir=generateRandomDirection();
                    setDirection(dir);
                    moveRover(scanMapTiles, centerIndex);
                   
                    blocked = Boolean.FALSE;
                    blockedByRover = Boolean.FALSE;
                    Thread.sleep(sleepTime);
                   
                }
                else if (blocked) {
                   
                        moveWhenBlocked(scanMapTiles, centerIndex);
                        Thread.sleep(sleepTime);
                       
                    for (int i = 0; i < 6 ; i++) {
                       
                        scanMapTiles=getScanMapTiles();
                        dir=generateRandomDirection();
                        setDirection(dir);
                        moveRover(scanMapTiles, centerIndex);
                        blocked = Boolean.FALSE;
                        blockedByRover = Boolean.FALSE;
                        Thread.sleep(sleepTime);
                        currentLoc=getCurrentLoaction();
                        scanMapTiles =getScanMapTiles();
                       
                        }
                    }else {
                       
               
                   
                    if(blocked==Boolean.FALSE && blockedByRover==Boolean.FALSE){
                        getTargetDirection(currentLoc, targetLocation);
                        moveRover(scanMapTiles,centerIndex);
                    }   
                    currentLoc=getCurrentLoaction();
                    scanMapTiles=getScanMapTiles();
                   
                    if(currentLoc.xpos==targetLocation.xpos && currentLoc.ypos==targetLocation.ypos)
                    {
                        if(!traverseJackpot)
                        {
                            gatherInJackpot(scanMapTiles,centerIndex);
                            traverseJackpot=Boolean.TRUE;
                        }
                        
                    }
                   
                    }
   
            // test for stuckness
                stuck = currentLoc.equals(previousLoc);
   
                //System.out.println("ROVER_04 stuck test " + stuck);
                System.out.println("ROVER_04 blocked test " + blocked);
   
                // TODO - logic to calculate where to move next
               
               
              
                // this is the Rovers HeartBeat, it regulates how fast the Rover cycles through the control loop
                Thread.sleep(sleepTime);
               
                System.out.println("ROVER_04 ------------ bottom process control --------------");
            }
       
        // This catch block closes the open socket connection to the server
        } catch (Exception e) {
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

    } // END of Rover main control loop
public void gatherInJackpot(MapTile[][] scanMapTiles, int centerIndex) throws Exception {
		
	Coord currentLocation = getCurrentLoaction();
		int i,j,xPos,yPos;
		
		String dir;
//		targetLocation=targetLocation;
		
		List<Coord> list= new ArrayList<Coord>();
		Boolean flag;
		for(i=0;i<7;i++){
			for(j=0;j<7;j++)
			{
				if((scanMapTiles[i][i].getTerrain() == Terrain.ROCK || scanMapTiles[i][j].getTerrain() == Terrain.SOIL || scanMapTiles[i][j].getTerrain() == Terrain.GRAVEL ))
				{
					if(i>3){
						xPos=(currentLocation.xpos+(i%4)+1);
					}
					else 
					if(i<3)
					{
						xPos=(currentLocation.xpos-(4-i)+1);
					}
					else
					{
						xPos=currentLocation.xpos;
					}
					if(j<3)
					{
						yPos=currentLocation.ypos-(4-j)+1;
					}
					else 
					if(j>3)
					{
						yPos=currentLocation.ypos+(j%4)+1;
					}
					else{
						yPos=currentLocation.ypos;
					}
					list.add(new Coord(xPos,yPos));	
				}
			}
		}
}
   
    // tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
    private void moveWhenBlocked(MapTile[][]scanMapTiles, int x) throws Exception {
        Boolean north,east,south,west;
        north=Boolean.FALSE;
        east=Boolean.FALSE;
        south=Boolean.FALSE;
        west=Boolean.FALSE;
       
        if(checkNorthDirection(scanMapTiles, x, x))
            north=Boolean.TRUE;
        if(checkEastDirection(scanMapTiles, x, x))
            east=Boolean.TRUE;
        if(checkSouthDirection(scanMapTiles, x, x))
            south=Boolean.TRUE;
        if(checkWestDirection(scanMapTiles, x, x))
            west=Boolean.TRUE;
       
        if(north)
            setDirection("N");
        else if(east)
            setDirection("E");
        else if(south)
            setDirection("S");
        else if(west)
            setDirection("W");
       
        moveRover(scanMapTiles, x);
           
       
    }

    private MapTile[][] getScanMapTiles() throws Exception {
        // ***** do a SCAN *****
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
                    if (!destinations.contains(coord) && !marked)
                        destinations.add(coord);
                }

                globalMap.put(coord, tile);
            }
        }
    }


    // ####################### Support Methods #############################
   
    private Coord getCurrentLoaction() throws Exception {
        String line;
        Coord currentLoc=null;
        out.println("LOC");
        line = in.readLine();
        if(line == null){
            System.out.println("ROVER_04 check connection to server");
            line = "";
        }
        if (line.startsWith("LOC")) {
            currentLoc = extractLocationFromString(line);
            System.out.println(rovername + " currentLoc at start: " + currentLoc);
        }
       
        return currentLoc;
       
    }

    private void clearReadLineBuffer() throws IOException{
        while(in.ready()){
            //System.out.println("ROVER_04 clearing readLine()");
            in.readLine();   
        }
    }
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
        //System.out.println("ROVER_04 incomming EQUIPMENT result - first readline: " + jsonEqListIn);
       
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
                System.out.println("ROVER_04 check connection to server");
                jsonScanMapIn = "";
            }
            StringBuilder jsonScanMap = new StringBuilder();
            System.out.println("ROVER_04 incomming SCAN result - first readline: " + jsonScanMapIn);
           
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
   
    public Boolean validateMapTile(MapTile map) {
       
        if ( map.getHasRover() == Boolean.TRUE){
            blockedByRover=Boolean.TRUE;
            blocked=Boolean.FALSE;
            return Boolean.FALSE;
        }
        if (map.getTerrain() == Terrain.SAND || map.getTerrain() == Terrain.NONE){
            blocked=Boolean.TRUE;
            blockedByRover=Boolean.FALSE;
            return Boolean.FALSE;
        }

        return Boolean.TRUE;

    }
   
    public String generateRandomDirection() {

        Random ran = new Random();
   
        int i =  ran.nextInt(1000)%4;
        if(i==0)
        {
            return "S";           
        }
        else if(i ==1)
        {
            return "N";
        }
        else if(i==2)
        {
            return "W";
        }else
        {
            return "E";
        }

    }
   
    public void setDirection(String dir)
    {
       
        if(dir.equals("S")){
            goingSouth=Boolean.TRUE;goingEast=Boolean.FALSE;goingWest=Boolean.FALSE;goingNorth=Boolean.FALSE;
        }else if(dir.equals("N")){
            goingSouth=Boolean.FALSE;goingEast=Boolean.FALSE;goingWest=Boolean.FALSE;goingNorth=Boolean.TRUE;
        }else if(dir.equals("W")){
            goingSouth=Boolean.FALSE;goingEast=Boolean.FALSE;goingWest=Boolean.TRUE;goingNorth=Boolean.FALSE;
        }else if(dir.equals("E")){
            goingSouth=Boolean.FALSE;goingEast=Boolean.TRUE;goingWest=Boolean.FALSE;goingNorth=Boolean.FALSE;
        }else
            generateRandomDirection();
       
    }
   
    public void getTargetDirection(Coord current,Coord target) throws Exception {

       
        MapTile[][] map = scanMap.getScanMap();
        int x = (scanMap.getEdgeSize() - 1) / 2;
        // S = y + 1; N = y - 1; E = x + 1; W = x - 1
        if (current.xpos == target.xpos
                && current.ypos == target.ypos) {
//            directionChecker();
        } else if ((current.xpos < target.xpos && current.ypos < target.ypos)) {
            if (goingHorizontal) {
                    goingSouth = Boolean.TRUE;
                    goingNorth = Boolean.FALSE;
                    goingEast = Boolean.FALSE;
                    goingWest = Boolean.FALSE;
                } else {
                    goingSouth = Boolean.FALSE;
                    goingNorth = Boolean.FALSE;
                    goingEast = Boolean.TRUE;
                    goingWest = Boolean.FALSE;
               
            }

        } else if ((current.xpos > target.xpos && current.ypos > target.ypos)) {
            if (goingHorizontal) {
                goingSouth = Boolean.FALSE;
                goingNorth = Boolean.TRUE;
                goingEast = Boolean.FALSE;
                goingWest = Boolean.FALSE;
            } else {
                goingSouth = Boolean.FALSE;
                goingNorth = Boolean.FALSE;
                goingEast = Boolean.FALSE;
                goingWest = Boolean.TRUE;
           
            }   
        }else if (current.xpos == target.xpos) {

            if (current.ypos < target.ypos) {
                    goingSouth = Boolean.TRUE;
                    goingNorth = Boolean.FALSE;
                    goingEast = Boolean.FALSE;
                    goingWest = Boolean.FALSE;
               
            } else {
                    goingSouth = Boolean.FALSE;
                    goingNorth = Boolean.TRUE;
                    goingEast = Boolean.FALSE;
                    goingWest = Boolean.FALSE;
            }
        } else if (current.ypos == target.ypos) {
            if (current.xpos < target.xpos) {
                    goingSouth = Boolean.FALSE;
                    goingNorth = Boolean.FALSE;
                    goingEast = Boolean.TRUE;
                    goingWest = Boolean.FALSE;
            } else {
                    goingSouth = Boolean.FALSE;
                    goingNorth = Boolean.FALSE;
                    goingEast = Boolean.FALSE;
                    goingWest = Boolean.TRUE;
                   
            }
        } else if (current.xpos > target.xpos) {
                goingSouth = Boolean.FALSE;
                goingNorth = Boolean.FALSE;
                goingEast = Boolean.FALSE;
                goingWest = Boolean.TRUE;
           
        } else {
                goingSouth = Boolean.FALSE;
                goingNorth = Boolean.FALSE;
                goingEast = Boolean.TRUE;
                goingWest = Boolean.FALSE;
        }

    }
   
    public void moveRover(MapTile[][] scanMapTiles,int centerIndex) throws Exception
    {
        out.println("GATHER");
        // tile S = y + 1; N = y - 1; E = x + 1; W = x - 1
        if (goingSouth) {
           
            goingHorizontal = Boolean.FALSE;
            if (!checkSouthDirection(scanMapTiles, centerIndex, centerIndex)) {
                blocked = true;
            } else {
                out.println("MOVE S");
            }
           
        }else if(goingEast)
        {
            goingHorizontal = Boolean.TRUE;
       
                if (!checkEastDirection(scanMapTiles, centerIndex, centerIndex)) {
                    blocked = true;
                } else {
                    out.println("MOVE E");
                }
            }else if (goingWest) {
            goingHorizontal = Boolean.TRUE;

            if (!checkWestDirection(scanMapTiles, centerIndex, centerIndex)) {
                blocked = true;
            } else {
                out.println("MOVE W");
            }

        }else {
            goingHorizontal = Boolean.FALSE;
            if (!checkNorthDirection(scanMapTiles,centerIndex,centerIndex)) {
                blocked = true;
            } else {
                out.println("MOVE N");
            }                   
        }
        Thread.sleep(sleepTime);
       
    }
    // checks for obstacle in North Direction
    Boolean checkNorthDirection(MapTile[][] map, int x, int y) {
        if (validateMapTile(map[x][y - 1])) // North
        {
            goingSouth = Boolean.FALSE;
            goingNorth = Boolean.TRUE;
            goingEast = Boolean.FALSE;
            goingWest = Boolean.FALSE;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    // checks for obstacle in East Direction
    Boolean checkEastDirection(MapTile[][] map, int x, int y) {
        if (validateMapTile(map[x + 1][y])) // East
        {
            goingSouth = Boolean.FALSE;
            goingNorth = Boolean.FALSE;
            goingEast = Boolean.TRUE;
            goingWest = Boolean.FALSE;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    // checks for obstacle in South Direction
    Boolean checkSouthDirection(MapTile[][] map, int x, int y) {
        if (validateMapTile(map[x][y + 1])) // South
        {
            goingSouth = Boolean.TRUE;
            goingNorth = Boolean.FALSE;
            goingEast = Boolean.FALSE;
            goingWest = Boolean.FALSE;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    // checks for obstacle in West Direction
    Boolean checkWestDirection(MapTile[][] map, int x, int y) {
        if (validateMapTile(map[x - 1][y])) // West
        {
            goingSouth = Boolean.FALSE;
            goingNorth = Boolean.FALSE;
            goingEast = Boolean.FALSE;
            goingWest = Boolean.TRUE;
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
   
   
    /**
     * Runs the client
     */
    public static void main(String[] args) throws Exception {
//        ROVER_04 client = new ROVER_04("192.168.1.106");
        ROVER_04 client = new ROVER_04();
        client.run();
    }
}