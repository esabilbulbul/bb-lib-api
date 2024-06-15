/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoAccStmtCore 
{
    public String stat = "";//For UI use
    public String accountId="";//user-account / branch
    public String accountName="";
    public String name = "";//vendor name

    public String billId = "";//if txn code is inventory
    public String paymentId = "";//if txn code is payment

    public String stmtDate = "";//bill date
    public String releaseDate = "";//valor tarihi
    public String title = "";

    public String txnType = "";// N = New R = Return S = Sale A = Financial Adjustment
    public String txnCode = "";
    public String txnName = "";//json
    public String txnName_EN = "";
    public String txnName_TR = "";
    public String txnEffect = "";
    public String txnDate = "";
    
    public String quantity = "";
    public String amount_c = "";//credit
    public String amount_d = "";//debit
    
    public String isRevolving = "";
    /*
    public String sumDebit = "";
    public String sumCredit = "";
    */
    public String balance = "";
    public String note = "";
    
    public ssoAccStmtCore()
    {
    }
}
