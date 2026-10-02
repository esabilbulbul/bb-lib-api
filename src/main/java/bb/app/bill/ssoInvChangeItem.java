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
    BigInteger AccId = BigInteger.ZERO;
    BigInteger BrandId = BigInteger.ZERO;
    String     ItemCode="";
    BigInteger OptId = BigInteger.ZERO;
    String     OptGroup = "";
    String     OptCode = "";//opt code 

    BigDecimal oldValUI  = new BigDecimal(BigInteger.ZERO);//On Client
    BigDecimal oldValSys = new BigDecimal(BigInteger.ZERO);//On Server
    BigDecimal newVal    = new BigDecimal(BigInteger.ZERO);
    
    public boolean bIgnore = false;//This flag is set when old Val on UI and on Sys is different. Server takes SYS as reference
}
