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
public class ssoBillLine 
{
    public String itemCode = "";
    public String category = "";
    public BigDecimal quantity = new BigDecimal(BigInteger.ZERO);
    public String quantityType;
    public BigDecimal EntryPrice;
    //public BigDecimal Discount;
    public BigDecimal discountRate;
    //public BigDecimal taxRate;
    public BigDecimal surcharge;
    //public BigDecimal tax;
    public BigDecimal salesPrice;
    public String options;
    public String date;

    public long Id;
    public boolean bDeleted;
}

