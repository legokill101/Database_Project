/* Group 1
* ISTE-330 Database Connectivity and access
*  Written By Jacob Feiner, Ian Ayala, Chris Bonsu, Vincent Ventutolo
 */

import com.mysql.fabric.xmlrpc.base.Array;

import java.util.ArrayList;
import java.util.*;

/* This is the main class that is responsible for executing any SQL queries that run against the database
* The reason that it is called server is that we hope to eventually have it running on a seprate machine from the frontend
* and comunicting with the front end over the network
 */
public class CapstoneTrackerDBInterface {

    /* method to Get a Get the info about a current version of the capstone from the database bassed on the capstoneID
     * In the CapstoneInfo object passed in and then stores that info in the capstoneInfo object passed in before reurning it
     */
    MySQLDatabase db;

    // default constructor to initialize database object
    public CapstoneTrackerDBInterface() {
        db = new MySQLDatabase();
    }

    // method to log in user
    public user_info Login(loginInfo info){
        try {
            // checks to see if an entry exists for the information provided
            String select = "SELECT username from Users WHERE username=? and password=sha(?)";
            ArrayList<String>params=new ArrayList<String>();
            params.add(info.getUserName());
            params.add(info.getPassword());
            db.connect();
            ArrayList<ArrayList<String>>result=db.getData(select,params);
            db.close();
            // if the results are not empty creates a user_info object, calls the GetUserInfo to fill it, and returns the object
            if (result.size()>0) {
                user_info user=new user_info();
                user.setUserName(info.getUserName());
                user=GetUserInfo(user);
                return user;
            }
            else{
                return null;
            }

        } catch (DLException dle) {
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e){}
            System.out.println("An error occured attempting to log in");
            return null;
        }
        catch (Exception e){
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e2){}
            System.out.println("An unxepcted error occured when attempting to log in");
            return null;
        }

    }


    // gets the info on the most recent capstone
    public CapstoneInfo GetCapstoneInfo(CapstoneInfo outObj) {
        // arraylist to hold data returned
        ArrayList<ArrayList<String>> Values;
        try {
            // sql statement to pull out data from database
            String sqlStatement = "select capstone_info.CapStoneId,`Date:`," +
                    "Plagerism_Score,Grade,DefenseDate,Title,Capstone_Version.Description,FileLocation,Name,`type`,status from" +
                    " Capstone_Info JOIN capstone_Version ON capstone_info.capstoneId=capstone_Version.capstoneId" +
                    " JOIN status_code ON status_code.SID=capstone_Version.status WHERE capstone_info.capstoneId=?" +
                    " AND Lattest_Date=`Date:`; ";
            // arraylist of paramaters for the folliwing method
            ArrayList<String> Params = new ArrayList<String>();
            Params.add(outObj.getCapstoneID());
            // connects to and runs the query against the database returns null if an error occurs connecting to or closing the dabatse
            db.connect();
            Values = db.getData(sqlStatement, Params);
            db.close();


        } catch (DLException dle) {
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e){}
            System.out.println("An error occured attempting to get the info on a capstone");
            return null;
        }
        try{
            // sets the lattest date in outobj
            outObj.setLatestDate(Values.get(0).get(1));
            // sets the plagerism score
            outObj.setPlagiarismScore(Values.get(0).get(2));
            // sets the grade
            outObj.setGrade(Values.get(0).get(3));
            // sets the defenseDAte
            outObj.setDefenseDate(Values.get(0).get(4));
            // object to map to the capstone version table
            CapstoneVersion outObj2=new CapstoneVersion(outObj.getCapstoneID(),outObj.getLatestDate());
            // adds outobj2 to outobj.
            outObj.addVersion(outObj2);
            // sets the rest of the paramater for object 2
            outObj2.setTitle(Values.get(0).get(5));
            outObj2.setDescription(Values.get(0).get(6));
            outObj2.setFilePath(Values.get(0).get(7));
            outObj2.setStatus(Values.get(0).get(8));
            outObj2.setType(Values.get(0).get(9));
            outObj2.setStatusCode(Values.get(0).get(10));
        }
        catch(Exception E){
            return null;
        }
        return outObj;
    }
    //gets all the versions of a paticular capstone project
    public CapstoneInfo GetCapstoneVersions(CapstoneInfo outObj) {
        // arraylist to hold data returned
        ArrayList<ArrayList<String>> Values;
        try {

            // sql statement to pull out data from database
            String sqlStatement = "Select CapstoneId, `Date:`,name,Title,Capstone_Version.description,`Type`,FileLocation " +
                    "FROM capstone_Version JOIN status_code on capstone_Version.Status=status_code.SID Where capstoneId=?;";
            // arraylist of paramaters for the folliwing method
            ArrayList<String> Params = new ArrayList<String>();
            Params.add(outObj.getCapstoneID());
            // connects to and runs the query against the database returns null if an error occurs connecting to or closing the dabatse
            db.connect();
            Values = db.getData(sqlStatement, Params);
            db.close();


        } catch (DLException dle) {
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e){}
            System.out.println("An error occured attempting to get the info on a capstone");
            return null;
        }
        try {
            // loops over each entry in the array lis and creates a corispond Capstonversion object and adds it to the capstone info object
            for (ArrayList<String> info : Values) {
                CapstoneVersion outObj2 = new CapstoneVersion(info.get(0), info.get(1));
                outObj2.setStatus(info.get(2));
                outObj2.setTitle(info.get(3));
                outObj2.setDescription(info.get(4));
                outObj2.setType(info.get(5));
                outObj2.setFilePath(info.get(6));
                outObj.addVersion(outObj2);
            }
        } catch (Exception E) {
            return null;
        }
        return outObj;
    }
    // updates the capstone info by inserts new capstone version and changing the lattest date in capstone_info table to
    // the date in the provided capstoneVersion Object
    public Boolean updateCapstone(CapstoneInfo inObj){
        try {
            // grabs the capstone version object that conatins much of the data, asside from capstoneID

            CapstoneVersion inObj2=inObj.GetVersions().get(0);

            ArrayList<String>Params=new ArrayList<String>();
            ArrayList<String>Params2=new ArrayList<String>();
            String insertStatement = "Insert INTO Capstone_Version Values(?,?,?,?,?,?,?);";
            String updateStatement="Update Capstone_Info Set Lattest_Date=? Where CapstoneID=?";
            //sets the paramaters useing inObj1 and inObj2
            Params.add(inObj2.getDate());
            Params.add(inObj2.getCapstoneID());
            Params.add(inObj2.getStatusCode());
            Params.add(inObj2.getTitle());
            Params.add(inObj2.getDescription());
            Params.add(inObj2.getType());
            Params.add(inObj2.getFilePath());
            Params2.add(inObj2.getDate());
            Params2.add(inObj.getCapstoneID());
            db.startTrans();
            db.connect();
            db.setData(insertStatement,Params);
            db.setData(updateStatement,Params2);
            db.endTrans();
            db.close();
            return true;
        }
        catch(DLException dle){
            System.out.println("an error has ocured when trying to update the capstone project");
            try {
                db.rollback();
                db.close();
            }
            catch (DLException dle2){
                System.out.println("you should not see this message and if you do that is really REALLY bad and you should contact the developers");
            }
        }
        return false;
    }


    //gets the date of the start of the students master, and the date of the start of the students cpastones
    public user_info GetStudentDates(user_info outObj){

        // arraylist to hold data returned
        ArrayList<ArrayList<String>> Values;
        ArrayList<ArrayList<String>> Value;
        try{
            String sqlStatement = "select startDate FROM StudentDetails JOIN Term ON StudentDetails.MasterStart=Term.term WHERE username=?;";
            String sqlStatementTwo = "Select startDate FROM StudentDetails JOIN Term ON StudentDetails.CapstoneStart=Term.term WHERE username=?;";

            ArrayList<String> Params = new ArrayList<String>();
            Params.add(outObj.getUsername() );

            // connects to and runs the query against the database returns null if an error occurs connecting to or closing the database
            db.connect();
            db.startTrans();
            Values = db.getData(sqlStatement, Params);
            Value = db.getData(sqlStatementTwo, Params);
            db.endTrans();
            db.close();

        }
        catch(DLException dle){
            System.out.println("An error occured attempting to get the info on a capstone");
            try {
                db.rollback();
                db.close();
            }
            catch (DLException dle2){
                System.out.println("you should not see this message and if you do that is really REALLY bad and you should contact the developers");
            }
            return null;
        }

        try{

            //
            outObj.setCapstoneStartDate(Values.get(0).get(0));
            outObj.setMasterStart(Value.get(0).get(1));

            return outObj;


        }
        catch(Exception e){
            System.out.println(e.getMessage());

        }
        return null;
    }
// gets the info about a students to fill the user info object
    public user_info GetUserInfo(user_info outObj){

        ArrayList<ArrayList<String>> Values;
        try{
            String sqlStatement = "SELECT UserType,Name,Email,`Phone Number`,Department FROM users WHERE Username=?;";

            //arraylist of parameters for the following method
            ArrayList<String> Params = new ArrayList<String>();
            Params.add(outObj.getUsername());
            db.connect();
            Values = db.getData(sqlStatement, Params);
            db.close();

        }catch(DLException dle){
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e){}
            return null;

        }
        try
        {
            //how to pull all fields from the user table
            outObj.setUserName(Values.get(0).get(1));
            outObj.setUserType(Values.get(0).get(2));
            outObj.setName(Values.get(0).get(3));
            outObj.setEmail(Values.get(0).get(4));
            outObj.setPhoneNumber(Values.get(0).get(5));
            outObj.setDepartment(Values.get(0).get(6));
            outObj= GetStudentDates(outObj);
            return outObj;



        }catch(Exception e){


        }
        return null;
    }

    public user_info GetCommitees(user_info outObj){

        ArrayList<ArrayList<String>> Values;
        try{
            // fills in the user part of the output object
            outObj= GetUserInfo(outObj);
            // then moves onto filling out the commitee part
            String sqlStatement = "Select * From Committe where Username=?";

            //arraylist of parameters for the following method
            ArrayList<String> Params = new ArrayList<String>();
            Params.add(outObj.getUsername());
            db.connect();
            Values = db.getData(sqlStatement, Params);
            db.close();

        }catch(DLException dle){
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e){}
            return null;

        }
        try
        {
            for(ArrayList<String> info : Values) {

                commitee_info outObj2= new commitee_info(info.get(1));
                outObj2.setCapStoneID(info.get(2));
                outObj2.setHasAccepted(info.get(3));
                outObj2.setHasDecline(info.get(4));
                outObj2.setPosition(info.get(5));
                outObj2.setTracking(info.get(6));
                outObj.addCommitees(outObj2);
            }
            return outObj;



        }catch(Exception e){


        }
        return null;

    }

    /* method to get a lisst of some pararmetns of mutliple , more specifically the capstoneID
    * the Title of the project, its current status, and the name of the author
    * I used whenever a list of multiple capstones is needed
    */
    private ArrayList<CapstoneInfo> GetMultipleCapstones(String sqlWhere, ArrayList<String>paramInfo) {
        // arraylist to store capstone objects to be created
        ArrayList<CapstoneInfo>capstones=new ArrayList<CapstoneInfo>();
        /*arraylist to store results of query, to be parrsed in seprate try block so that if a exception is thrown during
        * parsis it is distinct from one getting the data out
        */
        ArrayList<ArrayList<String>>results;
        // try catch for getting stuff from the database
        try {
            String sqlStatement="SELECT capstone_info.CapstoneID, capstone_version.Title, status_code.Name, users.Name,Capstone_Info.Lattest_date"+
                                " FROM users JOIN committe ON users.username=committe.username"+
                                " JOIN capstone_Info on committe.capstoneID=capstone_info.capstoneID"+
                                " JOIN capstone_version on capstone_info.capstoneID=capstone_version.CapstoneID"+
                                " JOIN status_code on capstone_version.status=status_code.SID"+sqlWhere;

            // Monster SQL statement to get info from database, its huge cause it traverses 5 tables
            db.connect();
            results=db.getData(sqlStatement,paramInfo);
            db.close();
        }
        catch (DLException dle){
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e){}
            System.out.println("An error has occured with the database when trying to get all the capstone projects");
            return null;
        }
        catch (Exception e){
            // closes the database if it is open since an exception occured
            try{ db.close(); }
            catch (Exception e2){}
            System.out.println("An unexpected error has occured when trying to get all the capstone projects");
            return null;
        }
        // try catch for parsing said stuff
        try{
            // for each loop to go over each row in the results
            for(ArrayList<String>r :results){
                // creates the capstoneInfo object and sets the capstoneID
                CapstoneInfo capInfo=new CapstoneInfo(r.get(0));
                // adds the name of the author
                capInfo.setAuthor(r.get(3));
                // creates a capstoneversion object with the date retrived and capstoneID
                CapstoneVersion capVer=new CapstoneVersion(r.get(0),r.get(4));
                // adds the current status of the project
                capVer.setStatus(r.get(2));
                // and its title
                capVer.setTitle(r.get(1));
                // and then adds capstone version to capstone info
                capInfo.addVersion(capVer);
                // and then ads cpastone info the the arraylist
                capstones.add(capInfo);
            }

        }
        catch(Exception E){
            System.out.println("an error has occured parsing the returned results");
            return null;
        }
        return capstones;
    }
    // gets a list of all capstones
    public ArrayList<CapstoneInfo>GetAllCapstones(){
        return GetMultipleCapstones(" WHERE users.userType='student' AND capstone_version.`Date:`=capstone_info.Lattest_Date;",new ArrayList<String>());
    }
    // gets all the capstones a faculty is a commitee member of
    public ArrayList<CapstoneInfo>GetCommiteeCapstones(user_info user){
        ArrayList<String> params=new ArrayList<String>();
        params.add(user.getUsername());
        return GetMultipleCapstones(" WHERE users.UserType='student' AND capstone_info.CapstoneID = " +
                "(SELECT capstoneID FROM committe WHERE username=? AND HasAccepted=1);",params);

    }
    // gets all the capstones a faculty is tracking
    public ArrayList<CapstoneInfo>GetTrackedCapstones(user_info user) {
        ArrayList<String> params = new ArrayList<String>();
        params.add(user.getUsername());
        return GetMultipleCapstones(" WHERE users.UserType='student' AND capstone_info.CapstoneID = " +
                "(SELECT capstoneID FROM committe WHERE username=? AND Tracking=1);", params);
    }



}