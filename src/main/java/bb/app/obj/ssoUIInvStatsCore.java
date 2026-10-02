/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoUIInvStatsCore 
{
    public BigDecimal quantityEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal quantityReturned    = new BigDecimal(BigInteger.ZERO);
    public BigDecimal quantitySold        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal quantityRefund      = new BigDecimal(BigInteger.ZERO);
    
    // Net (before surcharge, disc tax and expense)
    public BigDecimal netTotalEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netTotalReturned    = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netTotalSold        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netTotalRefund      = new BigDecimal(BigInteger.ZERO);

    // Discount
    public BigDecimal discTotalEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal discTotalReturned    = new BigDecimal(BigInteger.ZERO);
    public BigDecimal discTotalSold        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal discTotalRefund      = new BigDecimal(BigInteger.ZERO);

    // Surcharge
    public BigDecimal srchTotalEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal srchTotalReturned    = new BigDecimal(BigInteger.ZERO);
    public BigDecimal srchTotalSold        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal srchTotalRefund      = new BigDecimal(BigInteger.ZERO);

    // Tax
    public BigDecimal taxTotalEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal taxTotalReturned    = new BigDecimal(BigInteger.ZERO);
    public BigDecimal taxTotalSold        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal taxTotalRefund      = new BigDecimal(BigInteger.ZERO);

    // Expense
    public BigDecimal expenseTotalEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal expenseTotalReturned    = new BigDecimal(BigInteger.ZERO);

    // Gross
    public BigDecimal grossTotalEntered     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal grossTotalReturned    = new BigDecimal(BigInteger.ZERO);
    public BigDecimal grossTotalSold        = new BigDecimal(BigInteger.ZERO);
    public BigDecimal grossTotalRefund      = new BigDecimal(BigInteger.ZERO);

    // Q Adj 
    public BigDecimal quantityAdjPlus     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal quantityAdjMinus    = new BigDecimal(BigInteger.ZERO);

    // Fin Adj Net Total
    public BigDecimal finAdjPlusNetTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdjMinusNetTotal    = new BigDecimal(BigInteger.ZERO);

    // Fin Adj Disc Total
    public BigDecimal finAdjPlusDiscTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdjMinusDiscTotal    = new BigDecimal(BigInteger.ZERO);

    // Fin Adj Disc Total
    public BigDecimal finAdjPlusSrchTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdjMinusSrchTotal    = new BigDecimal(BigInteger.ZERO);
    
    // Fin Adj Expense Total
    public BigDecimal finAdjPlusTaxTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdjMinusTaxTotal    = new BigDecimal(BigInteger.ZERO);

    // Fin Adj Expense Total
    public BigDecimal finAdjPlusExpenseTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdjMinusExpenseTotal    = new BigDecimal(BigInteger.ZERO);

    // Fin Adj Expense Total
    public BigDecimal finAdjPlusGrossTotal     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal finAdjMinusGrossTotal    = new BigDecimal(BigInteger.ZERO);

    // Rev Q Net Totals
    public BigDecimal quantityNetTotal          = new BigDecimal(BigInteger.ZERO);
    public BigDecimal quantityNetTotalManual    = new BigDecimal(BigInteger.ZERO);
    
    // revolving year
    public String revolvingYear = "";
}
