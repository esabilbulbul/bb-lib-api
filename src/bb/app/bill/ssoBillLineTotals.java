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
 */
public class ssoBillLineTotals 
{
    public BigDecimal totalQuantity  = new BigDecimal(BigInteger.ZERO);
    //public BigDecimal totalB4Tax     = new BigDecimal(BigInteger.ZERO);//sum of lines
    public BigDecimal totalGross     = new BigDecimal(BigInteger.ZERO);//sum of lines
    public BigDecimal totalDiscount  = new BigDecimal(BigInteger.ZERO);//sum of discount on lines
    public BigDecimal totalSurcharge = new BigDecimal(BigInteger.ZERO);//sum of surcharge on lines
    public BigDecimal totalNet       = new BigDecimal(BigInteger.ZERO);//sum of net total on lines
}


