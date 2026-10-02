/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.bill;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoQueryInvUpdateOption 
{
    public String query = "";
    public BigInteger userAccId   = BigInteger.ZERO;// Account Id / Retail Account Id
    public BigInteger chgAccId   = BigInteger.ZERO;// Account Id who makes the update
    public BigInteger brandId = BigInteger.ZERO;

    public BigInteger optUID = BigInteger.ZERO;
    public String optGroup = "";
    public String optCode  = "";

    public BigDecimal oldVal = new BigDecimal(BigInteger.ZERO);
    public BigDecimal newVal = new BigDecimal(BigInteger.ZERO);
    public BigDecimal diffVal = new BigDecimal(BigInteger.ZERO);

}
