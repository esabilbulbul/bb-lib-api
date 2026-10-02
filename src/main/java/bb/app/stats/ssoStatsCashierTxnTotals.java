/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.stats;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoStatsCashierTxnTotals 
{
    public BigDecimal quantity   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal grossTotal = new BigDecimal(BigInteger.ZERO);
    public BigDecimal discTotal  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal taxTotal   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netTotal   = new BigDecimal(BigInteger.ZERO);
}
