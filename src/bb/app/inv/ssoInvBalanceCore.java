/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.inv;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoInvBalanceCore 
{
    public BigDecimal net;
    public BigDecimal received;
    public BigDecimal returned;
    public BigDecimal sold;

    public ssoInvBalanceCore()
    {
        net = new BigDecimal(BigInteger.ZERO);
        received = new BigDecimal(BigInteger.ZERO);
        returned = new BigDecimal(BigInteger.ZERO);
        sold = new BigDecimal(BigInteger.ZERO);
    }
}
