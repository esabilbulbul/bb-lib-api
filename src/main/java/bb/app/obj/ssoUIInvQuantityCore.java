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
public class ssoUIInvQuantityCore 
{
    public BigDecimal   net      = new BigDecimal(BigInteger.ZERO);//received - returned - sold + refund + adjPlus - adjMinus
    
    public BigDecimal   received = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   returned = new BigDecimal(BigInteger.ZERO);

    public BigDecimal   adjPlus  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   adjMinus = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   adjNet = new BigDecimal(BigInteger.ZERO);

    public BigDecimal   sold     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   refund   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal   netSold  = new BigDecimal(BigInteger.ZERO);

    public BigDecimal   revolving= new BigDecimal(BigInteger.ZERO);
    public BigDecimal   balanceManualOverwrite= new BigDecimal(BigInteger.ZERO);
    //public ssoUIInvStatsCore revolving = new ssoUIInvStatsCore();
}


