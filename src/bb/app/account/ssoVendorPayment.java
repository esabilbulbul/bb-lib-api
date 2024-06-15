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
public class ssoVendorPayment 
{
    public String stat="";//for UI purpose only
    public String Id;//line Id / Payment Id
    public String accId;
    public String accName;//account(s) under the user in other words branch name
    public String vendId;//Brand
    public String vendName;//vendor name
    public String entryDate;
    public String writingDate;//writing of the check 
    public String dueDate;
    public String amount_principal;
    public String amount_interest;
    public String paymentType;
    public String paymentTypeName;//default EN
    public String paymentTypeName_TR;
    public String installmentNumber;
    public String sequence;//only for UX
    public String reference="";
    public String bank;
    public String txnEffect;
    public String paymentGroup;
}


