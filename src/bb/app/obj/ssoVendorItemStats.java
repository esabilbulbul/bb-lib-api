/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.obj;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class ssoVendorItemStats 
{
    //public String key = "";
    //public String parentKey = "";

    public String itemCode = "";
    public String OptGroups = new String();//delimited by ","

    public ssoStatsGroup current   = new ssoStatsGroup();
    public ssoStatsGroup eod       = new ssoStatsGroup();
    public ssoStatsGroup revolving = new ssoStatsGroup();

    public ssoVendorOptionStats optStats = new ssoVendorOptionStats();
}
