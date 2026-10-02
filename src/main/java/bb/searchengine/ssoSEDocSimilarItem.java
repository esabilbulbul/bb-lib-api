/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.searchengine;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @author Administrator
 */
public class ssoSEDocSimilarItem 
{
    public BigInteger Id = BigInteger.ZERO;
    public String itemCode = "";
    public BigDecimal price = BigDecimal.ZERO;
    public String name = "";//title
    
    public ssoSEDocSimilarItem(BigInteger pId, String pItemCode, BigDecimal pbdPrice, String pName)
    {
        Id = pId;
        itemCode = pItemCode;
        price = pbdPrice;
        name = pName;
    }
}

