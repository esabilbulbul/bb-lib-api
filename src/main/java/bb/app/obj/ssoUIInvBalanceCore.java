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
public class ssoUIInvBalanceCore 
{
    public BigDecimal net;//NET BALANCE = received - sent

    public BigDecimal revolving; // from previous year
    public BigDecimal received;
    public BigDecimal returned;

    public BigDecimal payments; 
    public BigDecimal sold;
    public BigDecimal refund;

    public ssoUIInvBalanceCore()
    {
        //net = new BigDecimal(BigInteger.ZERO);
        net  = new BigDecimal(BigInteger.ZERO);

        received = new BigDecimal(BigInteger.ZERO);
        returned = new BigDecimal(BigInteger.ZERO);
        sold = new BigDecimal(BigInteger.ZERO);
        payments = new BigDecimal(BigInteger.ZERO); 
        revolving = new BigDecimal(BigInteger.ZERO);
    }
}
