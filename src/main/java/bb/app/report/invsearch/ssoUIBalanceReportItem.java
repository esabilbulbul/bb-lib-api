/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.report.invsearch;

import bb.app.obj.ssoUIInvBalanceCore;
import bb.app.obj.ssoUIInvQuantityCore;

/**
 *
 * @author Administrator
 */
public class ssoUIBalanceReportItem {
    
    public String key       = "";
    public String parentKey = "";

    public String accId     = "";
    public String brandId   = "";
    public String brandName = "";

    public String itemCode   = "";
    public String entryPrice = "";
    public String salePrice  = "";

    public ssoUIInvQuantityCore quantity = new ssoUIInvQuantityCore();
    public ssoUIInvBalanceCore  balance  = new ssoUIInvBalanceCore();
    //public String netPaid   = "";
    //public String netSold   = "";

    public String lastEntryDate = "";
    public String lastSalesDate = "";
}
