/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.backup;

import bb.app.obj.stats.ssoInvState;
import bb.app.obj.stats.ssoInvStats;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class ssoInvBackupStatsInv {

    //public String vendorName = "";
    public Map<String, Map<String, ssoInvState>> revolving  = new HashMap<>();//vendor, itemcode, revolving 
    public Map<String, Map<String, Map<String,ssoInvStats>>> archieve   = new HashMap<>();//vendor, year, itemcode, stats 

}

