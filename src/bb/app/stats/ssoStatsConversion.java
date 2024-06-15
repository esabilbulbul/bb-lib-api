/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.stats;

import java.math.BigDecimal;
import java.math.BigInteger;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public class ssoStatsConversion 
{
    private BigDecimal bdPriceWItmDisc    = new BigDecimal(BigInteger.ZERO); //gross total = after item disc
    private BigDecimal bdPriceWCntDisc    = new BigDecimal(BigInteger.ZERO);
    private BigDecimal bdItemDisc      = new BigDecimal(BigInteger.ZERO);

    
    public BigDecimal bdQuantity              = new BigDecimal(BigInteger.ZERO);
    public BigDecimal bdPriceTag              = new BigDecimal(BigInteger.ZERO);
    
    public BigDecimal bdPriceNet         = new BigDecimal(BigInteger.ZERO);// Price - Disc - Tax
    public BigDecimal bdTaxRate          = new BigDecimal(BigInteger.ZERO);
    public BigDecimal bdTax              = new BigDecimal(BigInteger.ZERO);
    public BigDecimal bdPriceGross       = new BigDecimal(BigInteger.ZERO);
    
    public BigDecimal bdCounterDiscRate  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal bdCounterDisc      = new BigDecimal(BigInteger.ZERO);
    public BigDecimal bdTotalDisc        = new BigDecimal(BigInteger.ZERO);
    

    public ssoStatsConversion(String  pQuantity,
                              String  pPriceTag,
                              String  pPriceAfterItemDisc,//after item discount
                              String  pTaxRate,
                              String  pCounterDiscRate,
                              boolean pbTaxInPrice)
    {
        bdQuantity              = new BigDecimal(pQuantity);
        bdTaxRate               = new BigDecimal(pTaxRate);

        bdPriceTag              = new BigDecimal(pPriceTag).multiply(bdQuantity);;
        bdPriceWItmDisc         = new BigDecimal(pPriceAfterItemDisc).multiply(bdQuantity); //gross total = after item disc
        bdItemDisc              = bdPriceTag.subtract(bdPriceWItmDisc);
        
        //----------------------------------------------------------------------
        // Calculation of Net and Gross
        // Sale Txn calculation of Net and Gross direction is different than 
        // inventory transaction
        // NET PRICE = PRICE TAG
        // GROSS = NET - ITM DISC - CNT DISC + TAX
        //----------------------------------------------------------------------

        bdPriceNet              = bdPriceTag;//without disc and tax

        bdCounterDiscRate       = new BigDecimal(pCounterDiscRate);
        bdCounterDisc           = bdPriceWItmDisc.multiply(bdCounterDiscRate);
        bdPriceWCntDisc         = bdPriceWItmDisc.subtract(bdCounterDisc);
        
        bdTotalDisc             = bdItemDisc.add(bdCounterDisc);
        
        bdTax                   = Util.Tax.calculate(bdPriceWCntDisc.toString(), 
                                                     bdTaxRate.toString(), 
                                                     pbTaxInPrice);
        
        bdPriceGross = bdPriceWCntDisc;
        /*
        if(pbTaxInPrice==true)
        {
            bdPriceGross = bdPriceWCntDisc.subtract(bdTax);
        }
        else
        {
            bdPriceGross = bdPriceWCntDisc;//tax will be adding on top
        }
        */
    }
}
