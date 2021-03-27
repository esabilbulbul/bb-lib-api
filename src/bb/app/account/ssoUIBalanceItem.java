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
public class ssoUIBalanceItem 
{
    public String name = "";//item name
    public String  key = "";
    public String  parentKey = "";//
    public ssoInvQuantityCore quantity = new ssoInvQuantityCore();
    public ssoInvBalanceCore balance = new ssoInvBalanceCore();
    public int level=0;
    public String lastActivity = "";

}

