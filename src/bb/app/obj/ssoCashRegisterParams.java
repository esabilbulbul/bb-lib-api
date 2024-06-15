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
public class ssoCashRegisterParams
{
    public ArrayList<String> categoryItems = new ArrayList<String>();
    public ArrayList<ssoCashierPOSMenuItem> menuItems = new ArrayList<ssoCashierPOSMenuItem>();
    public ArrayList<ssoSalesConsultant> consultants = new ArrayList<ssoSalesConsultant>();//sales managers
    public ssoTaxSettings taxSet = new ssoTaxSettings();
}


