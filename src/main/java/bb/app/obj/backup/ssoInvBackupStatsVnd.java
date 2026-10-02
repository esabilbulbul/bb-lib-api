/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.backup;

import bb.app.obj.stats.ssoInvState;
import bb.app.obj.stats.ssoInvStats;
import bb.app.obj.stats.ssoInvSummary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class ssoInvBackupStatsVnd 
{
    //public String vendorName = "";
    //public Map<String, ssoInvState> revolving  = new HashMap();//vendor, stats
    //public Map<String, Map<String, ssoInvStats>> archieve   = new HashMap<>();// vendor, year, stats
    public String vendor = "";
    public String insertdate = "";
    public ssoInvState  revolving = new ssoInvState();
    public ssoInvState  eod   = new ssoInvState();
    public ssoInvSummary summary = new ssoInvSummary();

}


