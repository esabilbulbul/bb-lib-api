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
 * Statement = Lines + Bottom
 * 
 * Line_Net => Discount => B4Tax => Tax => +Surcharge => Net
 * 
 */
public class ssoBillBottomTotals 
{
    public BigDecimal taxRate           = new BigDecimal(BigInteger.ZERO);

    public BigDecimal DiscountRate      = new BigDecimal(BigInteger.ZERO);
    //public BigDecimal totalDiscount     = new BigDecimal(BigInteger.ZERO);//sum of line-discount + bottom-discount
    public BigDecimal discount          = new BigDecimal(BigInteger.ZERO);
    public BigDecimal totalB4Tax        = new BigDecimal(BigInteger.ZERO);//sum of net total before tax
    //public BigDecimal totalSurcharge    = new BigDecimal(BigInteger.ZERO);//sum of line-surcharge + bottom-surcharge
    public BigDecimal surcharge         = new BigDecimal(BigInteger.ZERO);
    public BigDecimal totalTax          = new BigDecimal(BigInteger.ZERO);//sum of tax
    //public BigDecimal totalNet          = new BigDecimal(BigInteger.ZERO);//net total
    public BigDecimal totalGross        = new BigDecimal(BigInteger.ZERO);//net total
}
