/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.obj;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoVendorItemStatsCore 
{
    public String itemCode = "";
    public BigDecimal grossTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal discTotal      = new BigDecimal(BigInteger.ZERO);
    public BigDecimal surchargeTotal = new BigDecimal(BigInteger.ZERO);
    public BigDecimal taxTotal       = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netTotal       = new BigDecimal(BigInteger.ZERO);
}
