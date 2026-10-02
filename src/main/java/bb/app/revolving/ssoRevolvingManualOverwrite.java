/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.revolving;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoRevolvingManualOverwrite 
{
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // WARNING: DON'T CHANGE THE UNIT NAMES (quantity, gross, ...)
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public ssoRevolvingQuantityUnit quantity = new ssoRevolvingQuantityUnit();

    public ssoRevolvingAmountUnit gross = new ssoRevolvingAmountUnit();
    public ssoRevolvingAmountUnit discount  = new ssoRevolvingAmountUnit();
    public ssoRevolvingAmountUnit surcharge = new ssoRevolvingAmountUnit();
    public ssoRevolvingAmountUnit tax = new ssoRevolvingAmountUnit();
    public ssoRevolvingAmountUnit net = new ssoRevolvingAmountUnit();

    public BigDecimal finAdj_Minus = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdj_Plus  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal balance = new BigDecimal(BigInteger.ZERO);
}
