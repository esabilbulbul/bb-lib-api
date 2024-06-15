/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.cashier;

import bb.app.ui.ssoTableKeys;

/**
 *
 * @author Administrator
 */
public class ssoEODTxnRow 
{
    public ssoTableKeys tableKeys = new ssoTableKeys();

    public String Id = "";
    public String txnDate = "";
    public String txnCode = "";
    public String txnName = "";//json string
    public String txnTime = "";
    public String quantity = "";
    public String name = "";
    public String totalGross = "";
    public String discount     = "";
    public String discountCounter = "";
    public String totalFinal = "";//total of cart
    public String totalPayment = "";
    public String consultantId = "";
    public String consultant = "";
    public String note = "";
    public String paidBy = "";//paid methods
    public String phoneNumber = "";
    public String currencySign = "";
}
