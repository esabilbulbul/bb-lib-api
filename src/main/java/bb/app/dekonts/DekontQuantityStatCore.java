/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.dekonts;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class DekontQuantityStatCore
{
    public int    dayNo      = 0;    
    public String date       = "";
    public BigDecimal value  = new BigDecimal(BigInteger.ZERO);
    public BigDecimal diff   = new BigDecimal(BigInteger.ZERO);//dif in value
    public BigDecimal change = new BigDecimal(BigInteger.ZERO);//dif in perc
}

