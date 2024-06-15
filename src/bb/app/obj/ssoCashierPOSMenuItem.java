/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.obj;

/**
 *
 * @author Administrator
 */
public class ssoCashierPOSMenuItem 
{
    public String vendorId;
    public String vendorName;
    public String itemCode;
    public String itemCodeId;
    public String referenceId;//hash
    public String quantity;
    public String options;//json
    public String category;
    public String price;

    public String cSign;// Content signature = SGN(vendorId + itemCodeId + referenceId + accountId)
    public String vSign;// Visual signature = vendorname + itemCode + price

    public String imgURL;
    public String icon;
}
