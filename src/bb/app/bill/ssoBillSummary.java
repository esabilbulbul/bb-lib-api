/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 * 
 * Summary = Summary of Lines + Summary of Bottoms
 */
public class ssoBillSummary 
{
    public BigDecimal totalQuantity     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal totalGross        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal totalDiscount     = new BigDecimal(BigInteger.ZERO);// sum(line.discount) + sum(bottom.discount)
    public BigDecimal totalSurcharge    = new BigDecimal(BigInteger.ZERO);// sum(line.surcharge) + sum(bottom.surcharge)
    public BigDecimal totalNet          = new BigDecimal(BigInteger.ZERO);
}
