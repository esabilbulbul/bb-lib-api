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
public class ssoQueryInvUpdateItem 
{
    public String query = "";
    public BigInteger userAccId   = BigInteger.ZERO;
    public BigInteger chgAccId   = BigInteger.ZERO;
    public BigInteger brandId = BigInteger.ZERO;
    public String itemCode = "";
    public BigDecimal oldVal = new BigDecimal(BigInteger.ZERO);
    public BigDecimal newVal = new BigDecimal(BigInteger.ZERO);
    public BigDecimal diffVal = new BigDecimal(BigInteger.ZERO);
}
