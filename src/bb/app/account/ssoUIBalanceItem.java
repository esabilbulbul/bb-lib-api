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
public class ssoUIBalanceItem 
{

    public String account = "";//or branch name
    public long aid = 0;
    public String name = "";//item name
    public String  key = "";
    public String  parentKey = "";//
    public String successRate = "";
    public String velocityStartup = "";// VELOCITY_STARTUP (2 in last 2 weeks)  = star (+2) 
    public String velocityOverall = "";// VELOCITY_OVERALL (8 in last 10 weeks) = star (+2) (16 in last 30 weeks) = star (+1)

    public ssoInvQuantityCore quantity = new ssoInvQuantityCore();

    public ssoInvBalanceCore balance = new ssoInvBalanceCore();
    public int level=0;
    public String lastActivity = "";

    public ssoUIBalanceItemDets dets = new ssoUIBalanceItemDets();
}

