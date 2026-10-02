/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.stats;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoInvStateCore // current, eod, ytd, revolving, cumulative
{
    public BigDecimal balance = new BigDecimal(BigInteger.ZERO);// received - returned - sold + refund + adjPlus - adjMinus
    public BigDecimal balanceManualOverwrite = new BigDecimal(BigInteger.ZERO);

    // INV
    public BigDecimal received  = new BigDecimal(BigInteger.ZERO);//inv-received
    public BigDecimal sent      = new BigDecimal(BigInteger.ZERO);//inv-sent
    public BigDecimal netInv = new BigDecimal(BigInteger.ZERO);

    // SALES
    public BigDecimal sold     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal refund   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netSales = new BigDecimal(BigInteger.ZERO);

    // ADJ
    public BigDecimal adjPlus  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal adjMinus = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netAdj   = new BigDecimal(BigInteger.ZERO);

    // Payment
    public BigDecimal paymentSent       = new BigDecimal(BigInteger.ZERO);
    public BigDecimal paymentReceived   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal paymentNet       = new BigDecimal(BigInteger.ZERO);
}


