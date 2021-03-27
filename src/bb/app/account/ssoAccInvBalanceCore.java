/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.inv.ssoInvBalanceCore;
import bb.app.inv.ssoInvQuantityCore;

/**
 *
 * @author Administrator
 */
public class ssoAccInvBalanceCore 
{
    public long   AccountId;
    public String AccountName = "";
    public String Brandname = "";
    public long ItemCodeId = 0;
    public String ItemCode  = "";
    public String Option    = "";
    public ssoInvQuantityCore quantity = new ssoInvQuantityCore();
    public ssoInvBalanceCore balance = new ssoInvBalanceCore();
    public String lastActivity = "";

    public String UIParentKey = "";//For root level it is empty. For the rest, it refers to the one level up
}
