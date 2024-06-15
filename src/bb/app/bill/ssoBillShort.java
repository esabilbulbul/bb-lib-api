/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import bb.app.bill.ssoBillLine;
import bb.app.bill.ssoBillSummary;
import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class ssoBillShort 
{
    public String BillId                = "";

    public ArrayList<ssoBillLineShort> Lines = new ArrayList<ssoBillLineShort>();

    public String DiscountRate      = "";
    public String totalLineDisc     = "";
    public String totalDiscount     = "";

    public String Surcharge         = "";
    public String totalLineSurcharge= "";
    public String totalSurcharge    = "";

    public String taxRate           = "";
    public String totalGross        = "";
    public String totalB4Tax        = "";
    //public String TaxRate           = "";
    public String totalTax          = "";

    public String totalNet          = "";
}
