/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

/**
 *
 * @author esabil
 */
public class DekontSummaryRec 
{
    public String Year = "";
    public String MonthNo = "";//always
    public String MonthName= "";
    public String BankCode = "";//Usage conditional
    public String Type = "";//always - txn type (pesin, taksit ..)
    public String Sum = "";//always
    public String Count = "";//always
}
