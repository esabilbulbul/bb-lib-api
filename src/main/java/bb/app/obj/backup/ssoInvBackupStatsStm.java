/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.backup;

import bb.app.bill.ssoBillShort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Administrator
 */
public class ssoInvBackupStatsStm 
{
    public ArrayList<ssoBillShort> stmt = new ArrayList<ssoBillShort>();//
    public Map<String, ArrayList<ssoBillShort>> archive = new HashMap<>();//vendor , stmts
}


