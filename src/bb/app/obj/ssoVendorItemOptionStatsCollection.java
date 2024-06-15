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
public class ssoVendorItemOptionStatsCollection 
{
    public String vendorId = "";
    public String itemCode = "";
    public ArrayList<ssoItemOption> options = new ArrayList<ssoItemOption>();
    public String totalQuantity = "";
    public long categoryId = -1;
}
