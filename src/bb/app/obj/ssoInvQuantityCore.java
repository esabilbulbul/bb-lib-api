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
public class ssoInvQuantityCore 
{
    public BigDecimal   net      = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   received = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   returned = new BigDecimal(BigInteger.ZERO);
    
    public BigDecimal   adjPlus  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   adjMinus = new BigDecimal(BigInteger.ZERO);
    
    public BigDecimal   sold     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   revolving= new BigDecimal(BigInteger.ZERO);
}
