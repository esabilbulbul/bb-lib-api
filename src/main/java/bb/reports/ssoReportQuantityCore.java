/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.reports;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoReportQuantityCore {

    //public String key  = "";//user Id or AccId
    public String name = "";
    public String uid  = "";

    // INV
    public BigDecimal received  = new BigDecimal(BigInteger.ZERO);//inv-received
    public BigDecimal sent      = new BigDecimal(BigInteger.ZERO);//inv-sent
    public BigDecimal netPurchasing = new BigDecimal(BigInteger.ZERO);

    // SALES
    public BigDecimal sold     = new BigDecimal(BigInteger.ZERO);
    public BigDecimal refund   = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netSales = new BigDecimal(BigInteger.ZERO);

    // ADJ
    public BigDecimal adjPlus  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal adjMinus = new BigDecimal(BigInteger.ZERO);
    public BigDecimal netAdj   = new BigDecimal(BigInteger.ZERO);

    public BigDecimal netInventory = new BigDecimal(BigInteger.ZERO);
}
