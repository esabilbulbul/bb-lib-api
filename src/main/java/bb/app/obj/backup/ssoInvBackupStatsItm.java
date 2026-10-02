/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.backup;

import bb.app.obj.stats.ssoInvState;
import bb.app.obj.stats.ssoInvSummary;

/**
 *
 * @author Administrator
 */
public class ssoInvBackupStatsItm {
    
    public String insertdate = "";
    public String vendor = "";
    public String itemCode = "";
    
    public String sellCounter = "";//SPDN_DAY_SELL_COUNTER
    public String sellThru = "";//SPDN_DAY_STR
    public String sellStar = "";//SPDN_DAY_STR
    
    public String lastEntryPrice = "";
    public String lastSalePrice = "";
    public String category = "";
    
    public String onMenu = "";
    public String onMenuImgUrl = "";
    public String onMenuIcon  = "";

    public String options = "";//only summary what exists (and not for archive)
    
    public ssoInvState  revolving = new ssoInvState();
    public ssoInvState  eod   = new ssoInvState();
    public ssoInvSummary summary = new ssoInvSummary();
}

