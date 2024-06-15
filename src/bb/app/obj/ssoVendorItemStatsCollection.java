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
public class ssoVendorItemStatsCollection 
{
    public String vendorId   = "";
    public String itemCode   = "";
    public BigDecimal quantity   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal entryPrice = new BigDecimal(BigInteger.ZERO);
    public BigDecimal salesPrice = new BigDecimal(BigInteger.ZERO);
    public BigDecimal priceTag   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal discountRate   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal surcharge      = new BigDecimal(BigInteger.ZERO);

    public String category = "";//name
    public long categoryId = 0;
    public long priceId    = 0;
}
