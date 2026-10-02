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
public class ssoBillUpdate
{
    // THIS MAY BE CANCELED 
    public ssoBillChangeUnit taxRate        = new ssoBillChangeUnit();
    public ssoBillChangeUnit DiscountRate   = new ssoBillChangeUnit();
    public ssoBillChangeUnit totalDiscount  = new ssoBillChangeUnit();
    public ssoBillChangeUnit totalB4Tax     = new ssoBillChangeUnit();
    public ssoBillChangeUnit totalTax       = new ssoBillChangeUnit();
    public ssoBillChangeUnit totalSurcharge = new ssoBillChangeUnit();
    public ssoBillChangeUnit totalNet       = new ssoBillChangeUnit();

    public ArrayList<ssoBillUpdateLine> lines = new ArrayList<ssoBillUpdateLine>();

}
