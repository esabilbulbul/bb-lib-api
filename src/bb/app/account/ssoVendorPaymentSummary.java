/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

/**
 *
 * @author Administrator
 */
public class ssoVendorPaymentSummary 
{
    public String Id;//payment Id

    public String accId;
    public String accName;//account(s) under the user in other words branch name

    public String year;//Financial Year
    public String entryDate;

    public String vendorId;//vendor Id
    public String vendorName;//vendor name

    public String tot_quantity;
    public String tot_amount_principal;
    public String tot_amount_interest;

    public String lastEntryDate;
}


