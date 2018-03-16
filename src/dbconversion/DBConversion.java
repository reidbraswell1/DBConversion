/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dbconversion;

/**
 *
 * @author reid
 */

    //STEP 1. Import required packages
import java.sql.*;
import java.util.Date;
import com.opencsv.CSVReader;
import com.utilities.date_utilities.DateUtilities;
import dbconversion.transactionline.TransactionLine;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * 
 * Reads a text file in either of these formats:<br>
 * <code style="color:red">mm/dd/yy,hh:mmam[hh:mmpm],9999,999.99,Some Text</code><br>
 * <code style="color:red">mm/dd/yy,hh:mm,9999,999.99,Some Text</code><br>
 * 
 * <table>
 * <caption style="text-align:left;font-weight:bold;">Text File Format.</caption>
 * <tr>
 * <td>mm,dd,yy</td>
 * <td>corresponds to the standard month day and year</td>
 * </tr>
 * <tr>
 * <td>[hh:mmam] or [hh:mmpm] alt [hh:mm]</td>
 * <td>corresponds to the standard 12 hour format with am or pm indication or alternate 24 hour format.</td>
 * </tr>
 * <tr>
 * <td>9999</td>
 * <td>corresponds to a 4 digit account number.</td>
 * </tr>
 * <tr>
 * <td>999.99</td>
 * <td>corresponds to any dollar value ranging from 0.01 to 9999.99.</td>
 * </tr>
 * <tr>
 * <td>Some Text</td>
 * <td>is any text message.</td>
 * </tr> 
 * </table>
 * 
 * 2 tables will be built: 1. ACCOUNT_TRANSACTIONS and 2. TRANSACTION_DESCRIPTIONS
 * 
 * @author reid
 */
public class DBConversion {

   private static final int DATE=0;
   private static final int TIME=1;
   private static final int AMOUNT=2;
   private static final int ACCOUNT=3;
   private static final int NOTE=4;
   
   private static Boolean isHSQLDB = false;
   private static Boolean isMYSQL = false;
   private static Boolean isMARIADB = false;
   
   // JDBC driver name and database URL
   private static final String JDBC_DRIVER_HSQLDB = "org.hsqldb.jdbc.JDBCDriver"; 
   private static final String JDBC_DRIVER_MYSQL = "com.mysql.jdbc.Driver";
   private static final String JDBC_DRIVER_MARIADB = "org.mariadb.jdbc.Driver";   
   private static final String DB_URL_HSQLDB = "jdbc:hsqldb:hsql://192.168.1.51/xdb;shutdown=true";
   private static final String DB_URL_MYSQL = "jdbc:mysql://192.168.1.72/DAILY_TRANSACTIONS";
   private static final String DB_URL_MARIADB = "jdbc:mariadb://192.168.1.72/DAILY_TRANSACTIONS";
   private static String URL="";

   //  Database credentials
   private static String USER = "";
   private static String PASS = "";
   private static String filePath = "";
   
   private static final String MM_DD_YY_HH_MM_A = "MM/dd/yy hh:mmaa";
   private static final String MM_DD_YY_HH_MM = "MM/dd/yy HH:mm";
   
   // Tables //
   private static final String ACCOUNT_NUMBERS = "ACCOUNT_NUMBERS";
   private static final String ACCOUNT_TRANSACTIONS = "ACCOUNT_TRANSACTIONS";   
   private static final String TRANSACTION_DESCRIPTIONS = "TRANSACTION_DESCRIPTIONS";
      
   // Columns //
   private static final String ACCOUNT_NUMBERS_ID = ACCOUNT_NUMBERS.concat(".ID");
   private static final String ACCOUNT_NUMBERS_DESCRIPTION = "ACCOUNT_DESCRIPTION";
   private static final String TRANSACTION_DESCRIPTIONS_ID = TRANSACTION_DESCRIPTIONS.concat(".ID");   
   private static final String ACCOUNT_NUMBER = "ACCOUNT_NUMBER";
   private static final String TRANSACTION_TIMESTAMP = "TRANSACTION_TIMESTAMP";
   private static final String TRANSACTION_AMOUNT = "TRANSACTION_AMOUNT";
   private static final String TRANSACTION_ACCOUNT_ID = "TRANSACTION_ACCOUNT_ID";
   private static final String TRANSACTION_DESCRIPTION_ID = "TRANSACTION_DESCRIPTION_ID";
   private static final String TRANSACTION_DESCRIPTION = "TRANSACTION_DESCRIPTION";
   
   /**
    * Adds list of command line options.
    * 
    * @return Returns <code>Options</code> used on the command line.
    * 
    */
   private static Options buildOptions()
   {
       Option help   = new Option("help","help",false,"Help.");
       Option DBType = new Option("dbtype","dbtype",true,"Data Base Type HSQLDB, MYSQL or MARIADB.");
       Option URL    = new Option("url","database-url",true,"Database URL");
       Option u      = new Option("u","user-name",true,"User Name.");
       Option p      = new Option("p","password",true,"Password.");
       Option fp     = new Option("fp","file-path",true,"Text File Path");
       Options options = new Options();
       options.addOption(help);
       options.addOption(DBType);
       options.addOption(u);
       options.addOption(p);
       options.addOption(URL);
       options.addOption(fp);
       return options;
   }//buildOptions//
    
   /**
    * Parse command line options.
    * 
    * @param args Command line arguments.
    * 
    */
   private static void parseCommandLine(String[] args) 
   {
        Options options = buildOptions();
        String header = "DBConversion Header\n";
        String footer = "DBConversion Footer\n";
        HelpFormatter formatter = new HelpFormatter();
        
        CommandLineParser parser = new DefaultParser();
        try {
            if(!(args.length > 0))
            {
                throw new ParseException("Usage:");
            }
            CommandLine line = parser.parse( options, args);
            if(line.hasOption("help"))
            {
                formatter.printHelp("DBConversion", header, options,footer, true);
            }
            System.out.println("\nCommand line options entered:");
            if(line.hasOption("dbtype")) {
                System.out.println("dbtype=" + line.getOptionValue("dbtype"));
                if(line.getOptionValue("dbtype").equalsIgnoreCase("HSQLDB")) {
                    isHSQLDB=true;
                    isMYSQL=false;
                }//if//
                else if(line.getOptionValue("dbtype").equalsIgnoreCase("MYSQL")) {
                    isHSQLDB=false;
                    isMYSQL=true;
                }//if//
                else if(line.getOptionValue("dbtype").equalsIgnoreCase("MARIADB")) {
                    isHSQLDB=false;
                    isMYSQL=false;
                    isMARIADB=true;
                }
                else {
                    throw new ParseException("Usage:");
                }
            }//if//
            if(line.hasOption("fp")) {
                System.out.println("file-path=" + line.getOptionValue("fp"));
                filePath=line.getOptionValue("fp");
            }//if// 
            else
                throw new ParseException("Usage: file-path required");
            if(line.hasOption("u")) {
                System.out.println("u=" + line.getOptionValue("u"));
                USER = line.getOptionValue("u");
            }
            if(line.hasOption("p")) {
                System.out.println("p=" + line.getOptionValue("p"));
                PASS = line.getOptionValue("p");
            }
            if(line.hasOption("url")) {
                System.out.println("url=" + line.getOptionValue("url"));
                URL = line.getOptionValue("url");
            }
            String [] string = line.getArgs();
            for(int i = 1; i < string.length; i++)
            {
                System.out.println("Left over arguments " + i + " " + string[i]);
            }
        }
        catch (ParseException exe)
        {
            System.err.println(exe.getMessage());
            formatter.printHelp("DBConversion", header, options,footer, true);
            System.exit(1);
        }
    }//parseCommandLine//
    
   /**
    * 
    * Convenience method to obtain a database connection. 
    * 
    * @param driver Database driver.
    * @param url Database url.
    * @param user Database username.
    * @param pass Database password.
    * @return JDBC connection. 
    * 
    */
   public static Connection getDatabaseConnection(String driver, String url, String user, String pass) {

       Connection conn = null;
       Class cls = null;
       try {
            cls = Class.forName(driver);
            conn = DriverManager.getConnection(url, user, pass);
       }
       catch(ClassNotFoundException cne) {
           System.out.println("Exception Occurred Loading Driver Class-" + cne.getMessage());
           cne.printStackTrace();
       }
       catch(SQLException se) {
           System.out.println("Exception Occurred Connecting to Database-" + se.getMessage());
           se.printStackTrace();
       }
       return conn;
   }
   
   /**
    * 
    * @param conn
    * @param stmt
    * @throws SQLException 
    */
   private static void displayAccountNumbersTable(Connection conn, Statement stmt)
           throws SQLException 
   {
      //STEP 4: Execute a query
      System.out.println("Creating statement...");
      stmt = conn.createStatement();
      String sql;
      sql = "SELECT id, ACCOUNT_NUMBER, ACCOUNT_DESCRIPTION FROM ACCOUNT_NUMBERS";
      ResultSet rs = stmt.executeQuery(sql);

      //STEP 5: Extract data from result set
      if(rs != null) {
         System.out.println("---------------------------");
         System.out.println("ID    ActNbr      ActDesc  ");
         System.out.println("---------------------------");
      }
      while(rs.next()){
         //Retrieve by column name
         int id         = rs.getInt("id");
         String actNbr  = rs.getString("ACCOUNT_NUMBER");
         String actDesc = rs.getString("ACCOUNT_DESCRIPTION");

         //Display values
         System.out.println(id + "     " + actNbr + "        " + actDesc);
      }
      //STEP 6: Clean-up environment
      rs.close();
      stmt.close();      
   }//displayAccountNumbersTable//
   
   /**
    * 
    * 
    * @param conn
    * @throws SQLException 
    * 
    */
   private static void displayTransactionDescriptionsTable(Connection conn)
           throws SQLException 
   {
      //STEP 4: Execute a query
      String sql = null;
      Statement stmt1 = null;
      Statement stmt2 = null;
      System.out.println("Creating statement...");
      stmt1 = conn.createStatement();
      stmt2 = conn.createStatement();
      
      StringBuilder sb = new StringBuilder();
      sb.append("SELECT MAX(LENGTH(");
      sb.append(TRANSACTION_DESCRIPTION);
      sb.append(")) AS Max_Length_String FROM ");
      sb.append(TRANSACTION_DESCRIPTIONS);
      sql = sb.toString();
      ResultSet rs1 = stmt1.executeQuery(sql);
      
      sql = "SELECT ID, TRANSACTION_DESCRIPTION FROM TRANSACTION_DESCRIPTIONS";
      ResultSet rs2 = stmt2.executeQuery(sql);      

      //STEP 5: Extract data from result set
      int maxLen = 0;
         while(rs1.next()) {
            maxLen = rs1.getInt("Max_Length_String");
         }
         System.out.println("MaxLengthString = " + maxLen);
         if(maxLen > 0) {
            System.out.println(TRANSACTION_DESCRIPTIONS + " TABLE" );
            for(int k=0; k < maxLen; k++)
               System.out.print("-");
            System.out.println("");
            System.out.println("ID    TxnDesc");
            for(int k=0; k < maxLen; k++)
               System.out.print("-");
            System.out.println("");
         }//if//
         
       while(rs2.next()){
          //Retrieve by column name
          int id = rs2.getInt("id");
          String txnDesc  = rs2.getString("TRANSACTION_DESCRIPTION");
          System.out.printf(Locale.US,"%-4d  %-50s %n" ,id,txnDesc);
       }
      
      //STEP 6: Clean-up environment
      rs1.close();
      rs2.close();
      //rs2.close();
      stmt1.close();
      stmt2.close();
   }//displayAccountNumbersTable//

   /**
    * 
    * 
    * @param conn
    * @throws SQLException 
    * 
    */
   private static void displayAccountTransactionsTable(Connection conn)
           throws SQLException 
   {
      //STEP 4: Execute a query
      Statement stmt = null;
      System.out.println("Creating statement...");
      stmt = conn.createStatement();
      String sql;
      sql = "SELECT id, TRANSACTION_TIMESTAMP,"  +
                       "TRANSACTION_AMOUNT,"     +
                       "TRANSACTION_ACCOUNT_ID " +
                       "FROM ACCOUNT_TRANSACTIONS";
      ResultSet rs = stmt.executeQuery(sql);

      //STEP 5: Extract data from result set
      if(rs != null) {
         System.out.println("---------------------------------------------------");
         System.out.println("ID    TxnDate                    TxnAmt    TxnActID");
         System.out.println("---------------------------------------------------");
      }
      while(rs.next()){
         //Retrieve by column name
         int id             = rs.getInt("id");
         String txnDate     = rs.getString("TRANSACTION_TIMESTAMP");
         Timestamp ts       = rs.getTimestamp("TRANSACTION_TIMESTAMP");
         String txnAmt      = rs.getString("TRANSACTION_AMOUNT");
         BigDecimal txnAmt2 = rs.getBigDecimal("TRANSACTION_AMOUNT");
         int txnActID       = rs.getInt("TRANSACTION_ACCOUNT_ID");

         //Display values
         System.out.printf(Locale.US,"%-4d  %tY-%tm-%td %tk:%tM:%tS.%tL    %6.2f      %-4d %n" ,id,ts,ts,ts,ts,ts,ts,ts,txnAmt2,txnActID);
      }
      //STEP 6: Clean-up environment
      rs.close();
      stmt.close();
   }//displayAccountNumbersTable//

   /**
    * 
    * 
    * @param conn 
    * 
    */
   private static void truncateTables(Connection conn) {

       Statement stmt = null;
       
       try {
           stmt = conn.createStatement();
           if(isHSQLDB) {
              stmt.execute("SET DATABASE REFERENTIAL INTEGRITY FALSE;"); 
           }
           if(isMYSQL || isMARIADB) {
              stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
           }
           stmt.execute("TRUNCATE TABLE " + ACCOUNT_TRANSACTIONS);
           if(isHSQLDB) {
              stmt.execute("ALTER TABLE " + ACCOUNT_TRANSACTIONS + " ALTER COLUMN ID RESTART WITH 1"); 
           }
           if(isMYSQL || isMARIADB) {
              stmt.execute("ALTER TABLE " + ACCOUNT_TRANSACTIONS + " AUTO_INCREMENT = 1");
           }

           stmt.execute("TRUNCATE TABLE " + TRANSACTION_DESCRIPTIONS);
           if(isHSQLDB) {
              stmt.execute("ALTER TABLE " + TRANSACTION_DESCRIPTIONS + " ALTER COLUMN ID RESTART WITH 1");   
           }
           if(isMYSQL || isMARIADB) {
              stmt.execute("ALTER TABLE " + TRANSACTION_DESCRIPTIONS + " AUTO_INCREMENT = 1");
           }
           if(isHSQLDB) {
              stmt.execute("SET DATABASE REFERENTIAL INTEGRITY TRUE;"); 
           }
           if(isMYSQL || isMARIADB) {
              stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
           }
       }
       catch (SQLException se) {
           System.err.println("Error occurred creating statment or truncating table-" + se.getMessage());
           se.printStackTrace();
           System.exit(0);
       }//catch//
   }//truncateTables//

   /**
    * 
    * 
    * @param conn
    * @param descriptionList 
    * 
    */
   private static void addDescriptionsToDatabase(Connection conn, List<String> descriptionList)
           //throws SQLException 
   {
       PreparedStatement ps = null;
      
       StringBuffer sb = new StringBuffer();
       sb.append("INSERT INTO ");
       sb.append(TRANSACTION_DESCRIPTIONS);
       if(isHSQLDB) {
          sb.append(" COLUMN (" + TRANSACTION_DESCRIPTION + ") VALUES (?)");
       }
       if(isMYSQL || isMARIADB) {
          sb.append(" (" + TRANSACTION_DESCRIPTION + ") VALUES (?)");
       }
     
       System.out.println("String SQL = " + sb.toString());
     
       try {
           System.out.println("Creating Prepared Statment");
           ps = conn.prepareStatement(sb.toString());
           conn.setAutoCommit(false);
           for(int k=0; k < descriptionList.size(); k++) {
               ps.setString(1, descriptionList.get(k));
               ps.addBatch();
          //ps.executeBatch();
          //System.out.println(ps.toString());
          //ps.executeUpdate();
           }
           System.out.println("Executing Update");
    //    ps.executeUpdate();
          ps.executeBatch();
          conn.commit();
          ps.close();
       }//try//
      catch (SQLException se2) {
          System.err.println("SQL Error Occurred Updating Database-" + se2.getMessage());
          se2.printStackTrace();
          System.exit(0);
      }
   }//displayAccountNumbersTable//

   
   /**
    * 
    * 
    * @param conn
    * @return 
    * 
    */
   private static ForeignKeysMap loadForeignKeysMap(Connection conn)
   {

        ForeignKeysMap fkm = new ForeignKeysMap();
    
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        sb1.append("SELECT * FROM ");
        sb1.append(ACCOUNT_NUMBERS);
        String sql1 = sb1.toString();
    
        sb2.append("SELECT * FROM ");
        sb2.append(TRANSACTION_DESCRIPTIONS);
        String sql2 = sb2.toString();

        try {
           PreparedStatement ps1 = conn.prepareStatement(sql1);
           PreparedStatement ps2 = conn.prepareStatement(sql2);
          //System.out.println(ps1 + " " + ps2);
            ResultSet rs1 = ps1.executeQuery();
            ResultSet rs2 = ps2.executeQuery();
            Map<String,Integer> accountKeysMap = new HashMap();
            while(rs1.next()) {
                accountKeysMap.put(rs1.getString(ACCOUNT_NUMBER),
                                   rs1.getInt(ACCOUNT_NUMBERS_ID));
            }
            fkm.setAccountKeys(accountKeysMap);
            Map<String,Long> descriptionKeysMap = new HashMap();
            while(rs2.next()) {
                descriptionKeysMap.put(rs2.getString(TRANSACTION_DESCRIPTION),
                                       rs2.getLong(TRANSACTION_DESCRIPTIONS_ID));
            }
            fkm.setDescriptionKeys(descriptionKeysMap);
            fkm.setIsOk(true);
            rs1.close();
            rs2.close();
            ps1.close();
            ps2.close();
        }
        catch(SQLException se) {
            fkm.setIsOk(false);
            System.err.println("SQLException Occurred-" + se.getMessage());
            se.printStackTrace();
            System.exit(0);
        }//catch//
        return fkm;
    }

   /**
    * 
    * 
    * @param conn
    * @param tl
    * @return
    * 
    */
    private static ForeignKeys searchForForeignKeys(Connection conn, TransactionLine tl) {

    String tranAccount = tl.getTranAccount();
    String tranNote = tl.getTranNote();
    ForeignKeys fk = new ForeignKeys();
    
    StringBuilder sb1 = new StringBuilder();
    StringBuilder sb2 = new StringBuilder();
    sb1.append("SELECT ID FROM ");
    sb1.append(ACCOUNT_NUMBERS);
    sb1.append(" WHERE ");
    sb1.append(ACCOUNT_NUMBER);
    sb1.append(" = ?");
    String sql1 = sb1.toString();
    
    sb2.append("SELECT ID FROM ");
    sb2.append(TRANSACTION_DESCRIPTIONS);
    sb2.append(" WHERE ");
    sb2.append(TRANSACTION_DESCRIPTION);
    sb2.append(" = ?");
    String sql2 = sb2.toString();

    try {
        PreparedStatement ps1 = conn.prepareStatement(sql1);
        PreparedStatement ps2 = conn.prepareStatement(sql2);
        ps1.setString(1, tl.getTranAccount());
        ps2.setString(1, tl.getTranNote());
      //System.out.println(ps1 + " " + ps2);
        ResultSet rs1 = ps1.executeQuery();
        ResultSet rs2 = ps2.executeQuery();
        while(rs1.next()) {
            fk.setAccountID(rs1.getInt(ACCOUNT_NUMBERS_ID));
            //System.out.println("Result Set 1 ID = " + rs1.getString("ID"));
        }
        while(rs2.next()) {
            fk.setDescriptionID(rs2.getLong(TRANSACTION_DESCRIPTIONS_ID));
            //System.out.println("Result Set 2 ID = " + rs2.getString("ID"));
        }
        fk.setIsOk(true);
        rs1.close();
        rs2.close();
        ps1.close();
        ps2.close();
    }
    catch(SQLException se) {
        fk.setIsOk(false);
        System.err.println("SQLException Occurred-" + se.getMessage());
        se.printStackTrace();
        System.exit(0);
    }//catch//
    return fk;
}

   /**
    * 
    * @param conn
    * @param transList 
    */
   private static void addTransactionsToDatabase(Connection conn, List<TransactionLine> transList)
           //throws SQLException 
   {
       PreparedStatement ps = null;
      
       StringBuffer sb = new StringBuffer();
       sb.append("INSERT INTO ");
       sb.append(ACCOUNT_TRANSACTIONS);
       if(isHSQLDB) {
          sb.append(" COLUMN (");
       }
       if(isMYSQL || isMARIADB) {
          sb.append(" ("); 
       }
       sb.append(TRANSACTION_TIMESTAMP + ","); 
       sb.append(TRANSACTION_AMOUNT + ",");
       sb.append(TRANSACTION_ACCOUNT_ID + ",");
       sb.append(TRANSACTION_DESCRIPTION_ID + ")");
       sb.append(" VALUES (?,?,?,?)");
     
       System.out.println("String SQL = " + sb.toString());
     
       ForeignKeysMap fkm = loadForeignKeysMap(conn);
       Map<String,Integer> accountKeysMap = fkm.getAccountKeys();
       Map<String,Long> descriptionKeysMap = fkm.getDescriptionKeys();
       
       //Date dt = new Date();
       Timestamp ts = null;
       String dateString = null;
       String timeString = null;
       Date dt = null;
       try {
           System.out.println("Creating Prepared Statment");
           ps = conn.prepareStatement(sb.toString());
           conn.setAutoCommit(false);
           for(int k=0; k < transList.size(); k++) {
               TransactionLine tl = transList.get(k);
              // ForeignKeys fk = searchForForeignKeys(conn, tl);
               //System.out.println("Loop " + k + " " + tl.getTranDateString() + " " + tl.getTranAmount() + " " + tl.getTranNote());
               dateString = tl.getTranDateString();
               timeString = tl.getTranTimeString();
               if(timeString.toLowerCase().indexOf("am") > -1 ||
                  timeString.toLowerCase().indexOf("pm") > -1) {
                     dt = DateUtilities.getDate(dateString + " " + timeString, MM_DD_YY_HH_MM_A, Locale.US);
                     if(dt != null)
                         ts = new Timestamp(dt.getTime());
               }
               else {
                    dt = DateUtilities.getDate(dateString + " " + timeString, MM_DD_YY_HH_MM, Locale.US);
                    if(dt != null)
                        ts = new Timestamp(dt.getTime());
               }
               
               ps.setTimestamp(1, ts);
               ps.setDouble(2, tl.getTranAmount());
              // ps.setInt(3, fk.getAccountID());
               Integer accountKey = accountKeysMap.get(tl.getTranAccount());
               if(accountKey == null) {
                  System.out.println("Debug Null Account Key" + tl.getTranNote() + " " + tl.getTranDateString() + " " + tl.getTranAccount() + " " + tl.getTranAmount());
               }   
               ps.setInt(3, accountKey);
               //ps.setLong(4, fk.getDescriptionID());
               Long descriptionKey = descriptionKeysMap.get(tl.getTranNote());
               ps.setLong(4, descriptionKey);
               ps.addBatch();
               
          //ps.executeBatch();
          //System.out.println(ps.toString());
          //ps.executeUpdate();
           }
           System.out.println("Executing Update " + ACCOUNT_TRANSACTIONS);
    //    ps.executeUpdate();
          int[] batch = ps.executeBatch();
          conn.commit();
          ps.close();
       }//try//
      catch (BatchUpdateException b) {
          System.err.println("-----BatchUpdateException-----");
          System.err.println("SQLState:  " + b.getSQLState());
          System.err.println("Message:  " + b.getMessage());
          System.err.println("Vendor:  " + b.getErrorCode());
          System.err.println("Update counts:  ");
          int [] updateCounts = b.getUpdateCounts();
          for (int i = 0; i < updateCounts.length; i++) {
               if (updateCounts[i] >= 0) {
                    // Successfully executed; the number represents number of affected rows
                    System.err.print("Successfully executed: number of affected rows: " + i + " " + updateCounts[i] + "\n");
               } else if (updateCounts[i] == Statement.SUCCESS_NO_INFO) {
                    // Successfully executed; number of affected rows not available
                    System.err.print("Successfully executed: number of affected rows not available: "+updateCounts[i] + "\n");
               } else if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                   System.err.print("Failed to execute: "+updateCounts[i] + "\n");
                    // Failed to execute
               }              
          }
          System.err.println("");
          System.exit(0);
      }
      catch (SQLException se2) {
          System.err.println("SQL Error Occurred Updating Database-" + se2.getMessage());
          se2.printStackTrace();
          System.exit(0);
      }
   }//displayAccountNumbersTable//

   /**
    * 
    */
   private static class ForeignKeysMap {
       private Map<String,Integer> accountKeys;
       private Map<String,Long> descriptionKeys;
       private boolean isOk;

        /**
         * @return the accountKeys
         */
        public Map getAccountKeys() {
            return accountKeys;
        }

        /**
         * @param accountKeys the accountKeys to set
         */
        public void setAccountKeys(Map<String,Integer> accountKeys) {
            this.accountKeys = accountKeys;
        }

        /**
         * @return the descriptionKeys
         */
        public Map getDescriptionKeys() {
            return descriptionKeys;
        }

        /**
         * @param descriptionKeys the descriptionKeys to set
         */
        public void setDescriptionKeys(Map<String,Long> descriptionKeys) {
            this.descriptionKeys = descriptionKeys;
        }

        /**
         * @return the isOk
         */
        public boolean isIsOk() {
            return isOk;
        }

        /**
         * @param isOk the isOk to set
         */
        public void setIsOk(boolean isOk) {
            this.isOk = isOk;
        }
       
       
   }
   
   /**
    * 
    * 
    */
   private static class ForeignKeys {
       
       private int accountID;
       private long descriptionID;
       private boolean isOk;

       public ForeignKeys() {
           
       }
       
       /**
        * @return the accountID
        */
       private int getAccountID() {
           return accountID;
       }

       /**
        * @param accountID the accountID to set
        */
       private void setAccountID(int accountID) {
           this.accountID = accountID;
       }

       /**
        * @return the descriptionID
        */
       private long getDescriptionID() {
           return descriptionID;
       }

       /**
        * @param descriptionID the descriptionID to set
        */
       private void setDescriptionID(long descriptionID) {
           this.descriptionID = descriptionID;
       }

        /**
         * @return the isOk
         */
        public boolean isIsOk() {
            return isOk;
        }

        /**
         * @param isOk the isOk to set
         */
        public void setIsOk(boolean isOk) {
            this.isOk = isOk;
        }
   }//ForeignKeys//
   
   /**
    * 
    * @param args 
    * 
    */
   public static void main(String[] args) 
   {
       
       parseCommandLine(args);

       //isMYSQL = false;
       //isHSQLDB = true;
       Connection conn = null;
       if(isMYSQL) {
          conn = getDatabaseConnection(JDBC_DRIVER_MYSQL,URL,USER,PASS);
       }
       if(isHSQLDB) {
          conn = getDatabaseConnection(JDBC_DRIVER_HSQLDB,URL,USER,PASS);
       }
       if(isMARIADB) {
           conn = getDatabaseConnection(JDBC_DRIVER_MARIADB,URL,USER,PASS);
       }
       if(conn == null)
           System.exit(0);

       /*
       Class c = null;
      // Statement  stmt = null;
       List<String> descriptionList = new ArrayList();

       //CONNECTION //
       try {
           c = Class.forName(JDBC_DRIVER_HSQLDB);
           System.out.println("Connecting to database...");
           conn = DriverManager.getConnection(DB_URL_HSQLDB,USER,PASS);
       }
       catch (ClassNotFoundException ce) {
           System.out.println("Class not found exception occurred-" + ce.getMessage());
           ce.printStackTrace();
       }//catch//
       catch (SQLException se) {
           System.out.println("SQL Exception occurred-" + se.getMessage());
           se.printStackTrace();
       }//catch//
*/
       //READ TEXT FILE//
       List<String> descriptionList = new ArrayList();
       List<String> accountList = new ArrayList();
       List<TransactionLine> transList = new ArrayList();
       String[] s = null;
       try 
       {
           //CSVReader reader2 = new CSVReader(new FileReader(TransactionLine.FILE_PATH));
           CSVReader reader2 = new CSVReader(new FileReader(filePath));
           List<String[]>myList = reader2.readAll();
           for(int i = 0; i < myList.size(); i++) {
               s = myList.get(i);
               TransactionLine tl = new TransactionLine(s[DATE],s[TIME],s[AMOUNT],s[ACCOUNT],s[NOTE]);
               for(int j=0; j < s.length; j++)
                   // System.out.println(s[j]);
                   ;
               if(descriptionList.contains(tl.getTranNote().trim())) {
                    ;
               }
               else {
                   descriptionList.add(tl.getTranNote().trim());
               }
               if(accountList.contains(tl.getTranAccount().trim())) {
                    ;
               }
               else {
                   accountList.add(tl.getTranAccount().trim());
               }
               if(accountList.contains(tl)) {
                   ;
               }
               else {
                   transList.add(tl);
               }
               
           }//for//
       }//try//
       catch(Exception exe)
       {
           System.err.println(exe.getMessage());
           exe.printStackTrace();
           String tl = "";
           for(int q=0; q < s.length; q++) {
               tl = tl + s[q] + ",";
           }
           System.err.println("Exception Line = " + tl);
       }
       if(accountList != null && accountList.size() > 0) {
           Collections.sort(accountList);
           for(int k=0; k < accountList.size(); k++) {
               System.out.println(accountList.get(k));
           }//for//           
       }
       if(descriptionList != null && descriptionList.size() > 0) {
           Collections.sort(descriptionList,String.CASE_INSENSITIVE_ORDER);
           for(int k=0; k < descriptionList.size(); k++) {
               System.out.println(descriptionList.get(k));
           }//for//
           try {
               truncateTables(conn);
               addDescriptionsToDatabase(conn, descriptionList);
               displayTransactionDescriptionsTable(conn);
               if(transList != null && transList.size() > 0) {
                   addTransactionsToDatabase(conn, transList);
                   displayAccountTransactionsTable(conn);
               }
           }
           catch (SQLException se) {
               System.out.println("SQL Exception Occurred-" + se.getMessage());
               se.printStackTrace();
           }
       }//if//
       //     displayAccountNumbersTable(conn, stmt);
       //     displayTransactionDescriptionsTable(conn, stmt);
       //     displayAccountTransactionsTable(conn, stmt);

      //STEP 6: Clean Up
      try {
          conn.close();
      }
      catch (SQLException se) {
          System.err.println("Exception occurred closing connection-" + se.getMessage());
          se.printStackTrace();
      }
      finally{
      //finally block used to close resource
      /*
            try{
                if(stmt!=null)
                    stmt.close();
            }
            catch(SQLException se2){
            }// nothing we can do
*/
            try{
                if(conn!=null)
                    conn.close();
            }
            catch(SQLException se){
                se.printStackTrace();
            }
      }//end finally try
   System.out.println("Goodbye!");
   }//end main
}//DBConversion//
