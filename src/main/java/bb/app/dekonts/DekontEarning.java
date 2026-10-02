/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.dekonts;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class DekontEarning 
{
    public long   id = 0;//merchant id
    public String name = "";
    public String lastUpdate = "";

    public ArrayList<DekontEarningCore> acc    = new ArrayList<DekontEarningCore>();
    public ArrayList<DekontEarningCore> market = new ArrayList<DekontEarningCore>();

}

