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
public class ssoUIBalanceReportVendor 
{

    public String key       = "";
    public String parentKey = "";

    public String accId     = "";

    public String brandId   = "";
    public String name      = "";//brand name

    public String str       = "";//Sell Thru Rate (inventory to sales rate)
    public ssoProfitCore profitBase = new ssoProfitCore();
    public ssoProfitCore profitUSD = new ssoProfitCore();

    public ssoSeasonSellThruRate STRSeasons = new ssoSeasonSellThruRate();
    public ssoQuarterSellThruRate STRQuarters = new ssoQuarterSellThruRate();

    public String itemCode  = "";

    public ssoUIInvQuantityCore quantity = new ssoUIInvQuantityCore();
    public ssoUIInvBalanceCore  balance  = new ssoUIInvBalanceCore();

    public String lastEntryDate = "";
    public String lastSalesDate = "";
}
