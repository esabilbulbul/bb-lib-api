/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class ssoBillChanges 
{
    public ssoBillChangeUnit taxRate    = new ssoBillChangeUnit();
    public ssoBillChangeUnit disc       = new ssoBillChangeUnit();
    public ssoBillChangeUnit surcharge  = new ssoBillChangeUnit();

    public ArrayList<ssoBillChangeLineUnit> lines = new ArrayList<ssoBillChangeLineUnit>();
}
