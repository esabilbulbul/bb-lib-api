/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.obj.ssoInvBalanceCore;
import bb.app.obj.ssoInvQuantityCore;

/**
 *
 * @author Administrator
 */
public class ssoAccInvBalanceCore 
{
    public long   AccountId;
    public String AccountName = "";
    public long BrandId;//vendor Id
    public String Brandname = "";
    public long ItemCodeId = 0;
    public String ItemCode  = "";
    public String successRate = "";
    public String velocityStartup = "";// VELOCITY_STARTUP (2 in last 2 weeks)  = star (+2) 
    public String velocityOverall = "";// VELOCITY_OVERALL (8 in last 10 weeks) = star (+2) (16 in last 30 weeks) = star (+1)
    public String Option    = "";
    public String OptionUID = "";
    public ssoInvQuantityCore quantity = new ssoInvQuantityCore();
    public ssoInvBalanceCore balance = new ssoInvBalanceCore();
    public String lastActivity = "";

    public String UIParentKey = "";//For root level it is empty. For the rest, it refers to the one level up
}
