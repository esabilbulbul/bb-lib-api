/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

/**
 *
 * @author Administrator
 */
public class DekontFields 
{
    public String bankCode;
    public String TxnType;
    public String Date = "";
    public String Release = "";
    public String Desc = "";
    public String Amount = "";
    public String Time = "";
    public String Balance = "";
    public String TraceNo = "";//REFCODE SAME THING
    public String refNo="";//only used for kuveyt
    public String MonthNo = "";
    
    
    public boolean bSkip;//only for kuveyt
}
