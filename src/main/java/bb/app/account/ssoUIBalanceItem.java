/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.obj.ssoUIInvBalanceCore;
import bb.app.obj.ssoUIInvQuantityCore;
import bb.app.obj.stats.ssoInvState;
import bb.app.obj.stats.ssoInvStateCore;
import bb.app.report.invsearch.ssoSTRCore;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoUIBalanceItem 
{

    public String account = "";//or branch name
    public BigInteger aid = BigInteger.ZERO;
    public String name = "";//item name
    public String  key = "";
    public String  parentKey = "";//
    public String successRate = "";
    public String velocityStartup = "";// VELOCITY_STARTUP (2 in last 2 weeks)  = star (+2) 
    public String velocityOverall = "";// VELOCITY_OVERALL (8 in last 10 weeks) = star (+2) (16 in last 30 weeks) = star (+1)

    public ssoUIInvQuantityCore quantity = new ssoUIInvQuantityCore();
    public ssoUIInvBalanceCore balance   = new ssoUIInvBalanceCore();//NET
    public ssoInvState revolving = new ssoInvState();//To be filled

    public String priceId = "";
    public String priceEntry = "";
    public String priceSale  = "";
    public String discount   = "";
    
    public String speed = "0";//default 9
    
    public String str = "";

    public int level=0;
    public String lastActivity = "";

    public ssoUIBalanceItemDets dets = new ssoUIBalanceItemDets();
}

