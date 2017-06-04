/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dbconversion.transactionline;

/**
 *
 * @author reid
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

//import com.webapps.constants.WebConstants;
import com.opencsv.CSVReader;
import com.utilities.date_utilities.DateUtilities;
import com.utilities.constants.Constants;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 
 * This class represents a single line read from a text file with the following format:<br>
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
 * @author reid
 */
public class TransactionLine {
    
    /**
     * Path to the transaction file as a string.
     */
    private static final String FILE_PATH="/home/reid/Documents/Banking/DailyTransactions.txt";
    
    private Date tranDate;
    private String tranDateString;
    private String tranTimeString;
    private double tranAmount;
    private String tranAccount;
    private String tranNote;

    /**
     * 
     * @param tranDate Transaction Date as a Date including the time.
     * @param tranAmount Transaction Amount as a String.
     * @param tranAccount Transaction Account as a String.
     * @param tranNote Transaction Note as a String.
     * 
     */
    public TransactionLine(Date tranDate, String tranAmount, 
                           String tranAccount, String tranNote) {
    
        this.tranDate = tranDate;
        this.tranAmount = Double.parseDouble(tranAmount);
        this.tranAccount = tranAccount;
        this.tranNote = tranNote;
    }
    
    /**
     * 
     * @param tranDateString Transaction Date as a String mm/dd/yy.
     * @param tranTimeString Transaction Time as a String.
     * @param tranAmount Transaction Amount as a String.
     * @param tranAccount Transaction Account as a String.
     * @param tranNote Transaction Note as a String.
     * 
     */
    public TransactionLine(String tranDateString, String tranTimeString, String tranAmount, 
                           String tranAccount, String tranNote) {
    
        this.tranDateString = tranDateString;
        this.tranTimeString = tranTimeString;
        this.tranAmount = Double.parseDouble(tranAmount);
        this.tranAccount = tranAccount;
        this.tranNote = tranNote;
    }


    /**
     * 
     * @param writeFile true (write file) false (do not write file)
     * @return String representation of the line written to the file.
     * @throws IOException 
     * 
     */
    public String writeTransactionLine(boolean writeFile) throws IOException {
        
        StringBuilder sb = new StringBuilder();
        Locale loc = Locale.US;
//      SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mma", loc);
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy hh:mma", loc);
        String dtString = df.format(tranDate);
        
        String tranDate = dtString.substring(0,dtString.indexOf(" "));
        String tranTime = dtString.substring(dtString.indexOf(" ")+1).toLowerCase();
        
        String[] nextLine = { tranDate, tranTime, String.format("%1.2f", (double)this.tranAmount), this.tranAccount, this.tranNote };
        
        char delimiterChar = '\054';
        char quoteChar = '\000';
        String lineEnd = "\r\n";
        
        if(writeFile) {
            CSVWriter writer = new CSVWriter(new FileWriter(FILE_PATH, true),delimiterChar,quoteChar,lineEnd);
            writer.writeNext(nextLine);
            writer.flush();
            writer.close();
        }//if//
        for(int i=0; i < nextLine.length; i++) {
            sb.append(nextLine[i]);
            if(i+1 < nextLine.length)
               sb.append(",");
        }//for//
        return sb.toString();
    }
    
    /**
     * 
     * @return Transaction Date.
     * 
     */
    public Date getTranDate() {
        return tranDate;
    }
    
    /**
     * 
     * @return Transaction Date as a String.
     * 
     */
    public String getTranDateString() {
       return tranDateString; 
    }
    
    /**
     * 
     * @param tranDate Transaction Date.
     * 
     */
    public void setTranDate(Date tranDate) {
        this.tranDate = tranDate;
    }
    
    /**
     * 
     * @return Transaction Time String.
     * 
     */
    public String getTranTimeString() {
        return tranTimeString;
    }

    /**
     * 
     * @param tranTimeString Transaction Time String.
     * 
     */
    public void setTranTimeString(String tranTimeString) {
        this.tranTimeString = tranTimeString;
    }
    
    /**
     * 
     * @return Transaction Amount.
     * 
     */
    public double getTranAmount() {
        return tranAmount;
    }

    /**
     * 
     * @param tranAmount Transaction Amount.
     * 
     */
    public void setTranAmount(Float tranAmount) {
        this.tranAmount = tranAmount;
    }
 
    /**
     * 
     * @return Transaction Account Number.
     * 
     */
    public String getTranAccount() {
        return tranAccount;
    }

    /**
     * 
     * @param tranAccount Transaction Account Number.
     * 
     */
    public void setTranAccount(String tranAccount) {
        this.tranAccount = tranAccount;
    }

    /**
     * 
     * @return A string representing the transaction note.
     * 
     */
    public String getTranNote() {
        return tranNote;
    }

    /**
     * 
     * @param tranNote 
     * 
     */
    public void setTranNote(String tranNote) {
        this.tranNote = tranNote;
    }    
    
    public static void main(String[] args) {
        TransactionLine tl = new TransactionLine(new Date(), "12.34", "0876", "tran note");
        try {
           tl.writeTransactionLine(true);
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}//TransactionLine//
