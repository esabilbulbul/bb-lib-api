/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import bb.app.bill.ssoBillLineTotals;
import bb.app.bill.ssoBillLineTotals;
import bb.app.bill.ssoBillSummary;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 * 
 * 
 * Statement = Lines + Bottom
 * 
 */
public class ssoBillTotals
{
    public ssoBillLineTotals Lines = new ssoBillLineTotals();
    public ssoBillBottomTotals Bottom  = new ssoBillBottomTotals();//
    public ssoBillSummary Summary = new ssoBillSummary();
    /*
    public BigDecimal totalTax = new BigDecimal(BigInteger.ZERO);// bill Tax
    public BigDecimal totalGross = new BigDecimal(BigInteger.ZERO);// sum(price) [CALCULATED THRU LINES]

    public BigDecimal totalLineNet   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal totalBillB4Tax = new BigDecimal(BigInteger.ZERO);// sum(price) - discount [CALCULATED THRU LINES]
    public BigDecimal totalBillNet   = new BigDecimal(BigInteger.ZERO);// sum(price) - discount + tax + surcharge (order)

    public BigDecimal totalLineSurcharge = new BigDecimal(BigInteger.ZERO);//each line surcharge
    public BigDecimal totalBillSurcharge = new BigDecimal(BigInteger.ZERO);//final surcharge 
    public BigDecimal totalSurcharge = new BigDecimal(BigInteger.ZERO);

    public BigDecimal totalLineDiscount = new BigDecimal(BigInteger.ZERO);//each line discount
    public BigDecimal totalBillDiscount = new BigDecimal(BigInteger.ZERO);//final discount
    public BigDecimal totalDiscount = new BigDecimal(BigInteger.ZERO);
    */
}
