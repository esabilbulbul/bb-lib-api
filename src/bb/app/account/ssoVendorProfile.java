/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.obj.ssoMerchant;
import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class ssoVendorProfile 
{
    public ArrayList<ssoUIBalanceItem>  inventory = new ArrayList<ssoUIBalanceItem>();
    public ssoVendorInfo                Info = new ssoVendorInfo();
    public ArrayList<ssoAccStmtCore>    Stmt = new ArrayList<ssoAccStmtCore>();
    public ArrayList<ssoVendorPayment>  payments = new ArrayList<ssoVendorPayment>();
    public ArrayList<ssoMerchant>       branches = new ArrayList<ssoMerchant>();
}

