/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class ssoBill 
{
    public String txnCode = "";
    public boolean IsInvReturn = false;
    public BigDecimal discountRate;
    public BigDecimal taxRate;
    public BigDecimal surcharge;//bottom surcharge (total)
    public BigDecimal totQuantity;
    public ArrayList<ssoBillLine> lines = new ArrayList<ssoBillLine>();
    
}
