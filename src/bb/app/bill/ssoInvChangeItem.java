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
public class ssoInvChangeItem 
{
    long AccId = 0;
    long BrandId = 0;
    String ItemCode="";
    long OptId = 0;
    String OptGroup = "";
    String OptCode = "";//opt code 
    BigDecimal oldVal = new BigDecimal(BigInteger.ZERO);
    BigDecimal newVal = new BigDecimal(BigInteger.ZERO);
}
