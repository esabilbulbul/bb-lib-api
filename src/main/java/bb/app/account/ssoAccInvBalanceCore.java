/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.obj.ssoUIInvBalanceCore;
import bb.app.obj.ssoUIInvQuantityCore;
import bb.app.obj.stats.ssoInvStats;
import bb.app.obj.stats.ssoInvStateCore;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoAccInvBalanceCore 
{
    public BigInteger   AccountId;
    public String AccountName = "";
    public BigInteger BrandId;//vendor Id
    public String Brandname = "";
    public BigInteger ItemCodeId = BigInteger.ZERO;
    public String ItemCode  = "";
    public String priceId = "";
    public String priceEntry = "";
    public String priceSale  = "";
    public String discount   = "";
    public String successRate = "";
    public String velocityStartup = "";// VELOCITY_STARTUP (2 in last 2 weeks)  = star (+2) 
    public String velocityOverall = "";// VELOCITY_OVERALL (8 in last 10 weeks) = star (+2) (16 in last 30 weeks) = star (+1)
    public String Option    = "";
    public String OptionUID = "";

    //public ssoInvStatsCore quantity = new ssoInvStatsCore();
    //public ssoInvBalanceCore balance = new ssoInvBalanceCore();
    public ssoInvStats stats = new ssoInvStats();

    public String lastActivity = "";

    public String UIParentKey = "";//For root level it is empty. For the rest, it refers to the one level up
}
