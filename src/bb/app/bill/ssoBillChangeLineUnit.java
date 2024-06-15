/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

/**
 *
 * @author Administrator
 */
public class ssoBillChangeLineUnit
{
    public long Id = 0;
    public ssoBillChangeUnit itemCode  = new ssoBillChangeUnit();
    public ssoBillChangeUnit priceEntry= new ssoBillChangeUnit();
    public ssoBillChangeUnit priceSales= new ssoBillChangeUnit();
    public ssoBillChangeUnit quantity  = new ssoBillChangeUnit();
    public ssoBillChangeUnit surcharge = new ssoBillChangeUnit();
    public ssoBillChangeUnit discount  = new ssoBillChangeUnit();
    public ssoBillChangeUnit taxRate   = new ssoBillChangeUnit();
    
    public boolean bDeleted = false;
}
