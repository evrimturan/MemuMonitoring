package monitoring;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.net.URL;
import java.net.HttpURLConnection;

public class Main {

    //private static String MemucPath = "\"D:\\Memu\\MEmu\\memuc.exe\"";
    private static String MemucPath = "\"C:\\Program Files\\Microvirt\\MEmu\\memuc.exe\"";
    //TODO Program Files or Program Files (x86)
    private static Set<String> tryLaterDevices = new HashSet<>();
    //TODO Need to decided what to do with tryLaterDevices

    private static int tryStopCount = 0;
    private static int tryStartCount = 0;
    private static int operationControl = 0;

    private static OutputStream output = new OutputStream() {
        private StringBuilder string = new StringBuilder();
        @Override
        public void write(int b) {
            this.string.append((char) b );
        }
        //Netbeans IDE automatically overrides this toString()
        public String toString(){
            return this.string.toString();
        }
    };

    private static Connection connection;
    private static Statement statement;

    private static Set<String> devices;

    private static String whereClause;
    private static String query;
    private static HashMap<String, String> androidIdIndex = new HashMap<>();


    public static void main(String[] args) {

        whereClause = "";
        //devices = readFile();
        devices = new HashSet<>();
        devices.add("19");
        //TODO this list can be got using memuc listvms

        for(String device : devices) {
            //Done by using Android ID. There's been an error while using IMEI
            //TODO This numbers will be used to retrieve Android ID by running adb command to retrieve data from database and will be used to restart the devices
            String androidIdCommand = MemucPath + " -i " + device + " adb shell settings get secure android_id";
            String cmdOutput = runcmd(androidIdCommand);
            String resultLines = produceOutput(cmdOutput, androidIdCommand);
            String lines[] = resultLines.split("\\r?\\n");

            String androidId = lines[lines.length-1];

            //test
            androidId = "188a38763f697b7c";

            System.out.println("AndoridId " + androidId);
            //TODO Need to figure out what exactly the String out is to create the exact Where Clause string
            if(whereClause.length() == 0) {
                whereClause += "where guid = " + androidId;
            }
            else {
                whereClause += " or guid = " + androidId;
            }
            //TODO HashMap key Android ID, value String device
            androidIdIndex.put(androidId, device); // Need to get androidId from out variable
        }

        //TODO The string of Where Clause of SQL query will be written here
        //TODO Select Clause should contain just guid and time and FROM Clause is campaignUp.device
        //TODO The string of the whole query will be written here

        query = "select guid, time from campaignUp.device " + whereClause + ";";

        System.out.println("Query " + query);


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://193.117.153.196:3306/campaignUp","root","root");
            //TODO locolhost must be changed to actual ip address of the server that this program is running on
            statement = connection.createStatement();

            //test
            System.out.println("Statement: " + statement.toString());

        }
        catch(Exception e) {
            System.out.println("DB Message: " + e.getMessage());
            System.out.println("DB StackTrace: " + e.getStackTrace());
        }

        System.out.println("Query " +query);




        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                //test
                System.out.println("Timer Run");
                retrieveData(connection, statement, query);

            }
        };

        Timer timer = new Timer("Timer");

        long delay = 1000;
        long period = 1000 * 60 * 1;

        timer.schedule(timerTask, delay, period);


        try {
            connection.close();
        }
        catch (Exception e) {
            System.out.println("Connection close message " + e.getMessage());
            System.out.println("Connection close stack trace " + e.getStackTrace());
        }

    }


    public static void retrieveData(Connection con, Statement stmt, String query) {
        //TODO SQL query will be written here and logic of monitoring will be implemented
        //TODO The name of method can be changed
        //test
        System.out.println("Retrieve Data");
        try {

            //test
            System.out.println("There is an error on executeQuery()");
            ResultSet resultSet = stmt.executeQuery(query);
            //test
            System.out.println("Result Set: " + resultSet);
            while(resultSet.next()) {
                //test
                System.out.println("Result Set");
                String dbGuid = resultSet.getString(1);
                String dbTime = resultSet.getString(2);

                //test
                System.out.println("dbGuid: " + dbGuid + " dbTime: " + dbTime);

                long nowLong = Calendar.getInstance().getTimeInMillis();
                long dbTimeLong = Long.parseLong(dbTime);

                String index = androidIdIndex.get(dbGuid);

                //test
                index = "19";

                String runningCommand = MemucPath + " isvmrunning -i" + index;
                String cmdOutput = runcmd(runningCommand);
                String isRunning = produceOutput(cmdOutput, runningCommand);

                //test
                isRunning = "Not Running";

                if((nowLong - dbTimeLong >= 1000 * 60 * 5)) {
                    //TODO if it is running restart it, but if it is not running start it
                    if(isRunning.equals("Running")) {
                        restartDevices(index);
                    }
                    else if(isRunning.equals("Not Running")) {
                        startDevice(index);
                    }
                }
            }
        }
        catch (Exception e) {
            System.out.println("Retrieve Data Massage " + e.getMessage());
            System.out.println("Retrieve Data StackTrace " + e.getStackTrace());
        }



        //TODO if there are 5 minute difference between time stamp of the device and now, the index of device will be found using androidIDIndex HashMap
        //TODO The devices will be restarted
    }

    public static void startDevice(String device) {
        Logs.appendLog("Device "+ device +" starts up.");
        String command = MemucPath + " start -i "+ device;
        String output = runcmd(command);
        boolean check = checkOutput(output, "SUCCESS: start vm finished.", command);
        if(check){
            tryStartCount = 0;
            Logs.appendLog("Device "+ device +" started successfully.");
        }else if(tryStartCount == 10 && operationControl == 0) {
            tryStartCount = 0;
            Logs.appendLog("Device "+ device +" failed to start. Once the other devices are turned on, it will be tried again.");
            //TODO notifyAdmins() method will be added
        }else{
            tryStartCount ++;
            Logs.appendLog("Device "+ device +" failed to start. Count = "+tryStartCount);
            startDevice(device);
        }
    }

    public static void stopDevice(String device) {
        Logs.appendLog("Device "+ device +" stopping.");
        String command = MemucPath+" stop -i "+ device;
        String output = runcmd(command);
        boolean check = checkOutput(output, "SUCCESS: stop vm finished.", command);
        if(check){
            tryStopCount = 0;
            Logs.appendLog("Device "+ device +" stopped successfully.");
            startDevice(device);
        }else if(tryStopCount == 10 && operationControl == 0){
            tryStopCount = 0;
            Logs.appendLog("Device "+ device +" could not be stopped. Once the other devices are turned on, it will be tried again.");
            //TODO notifyAdmins() method will be implemented
        }else{
            tryStopCount ++;
            Logs.appendLog("Device "+ device +" could not be stopped. Trying again. Count = "+tryStopCount);
            stopDevice(device);
        }
    }

    private static String runcmd(String _command){
        String[] command =
                {
                        "cmd",
                };
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            new Thread(new SyncPipe(p.getErrorStream(), output)).start();
            new Thread(new SyncPipe(p.getInputStream(), output)).start();
            PrintWriter stdin = new PrintWriter(p.getOutputStream());
            //stdin.println("cd C:\\Program Files\\Microvirt\\MEmu");
            stdin.println(_command);
            stdin.close();
            p.waitFor();
            System.out.println(output.toString());


        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    private static boolean checkOutput(String output,String check, String command){
        int i = output.lastIndexOf(command)+command.length();
        int j = output.lastIndexOf(System.getProperty("user.dir")+">");
        String _output = output.substring(i,j);
        return _output.contains(check);
    }

    private static String produceOutput(String output, String command){
        int i = output.lastIndexOf(command)+command.length();
        int j = output.lastIndexOf(System.getProperty("user.dir")+">");
        return output.substring(i,j);
    }

    private static void restartDevices(Set<String> Devices) {
        tryStartCount = 0;
        tryStopCount = 0;
        /*
        Devices.stream().forEach(d ->
        {
            restartDevices(d);
            /*try {
                Thread.sleep(60000*3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });


         */

        for(String device : devices) {
            restartDevices(device);
        }


            //TODO Need to decide to use which strategy. The code snipped above or advance for loop like for(String device : Set<String> Devices)
    }

    private static void restartDevices(String device){
        Logs.appendLog( "Device "+ device +" restarting." );
        stopDevice(device);
        startDevice(device);
    }

    private static Set<String> readFile() {
        BufferedReader reader;
        Set<String> Devices = new HashSet<>();

        operationControl = 0;
        Logs.appendLog("Devices reading.");
        try {
            reader = new BufferedReader(new FileReader(
                    System.getProperty("user.dir")+"/Devices.txt"));

            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                Devices.add(line);
                // read next line
                line = reader.readLine();
            }
            Logs.appendLog(Devices.size()+" devices will be started.");
            reader.close();
            return Devices;
        }catch (Exception ex){
            Logs.appendLog(ex.getMessage());
            Logs.appendLog(Arrays.toString(ex.getStackTrace()));
        }
        return Devices;
    }

    public static void notifyAdmins() {
        //TODO notifying the admins by email or whatsapp notification will be implemented here
    }

    /*public static void sendHttpRequest(String guid) {
        try {
            URL url = new URL ("locolhost:8080/monitoring");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            String jsonInputString = "{\"guid\": " + "\"" + guid + "\""  + "}";


            try(OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }




        }
        catch (Exception e) {
            System.out.println("HTTP Request: " +e.getMessage() );
            System.out.println("HTTP Request: " +e.getStackTrace());
        }

    } */


}
