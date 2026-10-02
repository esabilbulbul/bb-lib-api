/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.searchengine;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class ssoSEDocument 
{

    public BigInteger Id            = BigInteger.ZERO;//item Id
    public String itemCode          = "";
    
public int itemSegment          = 0;//Segment = Clothing, Electronics and so on

    public BigInteger userId        = BigInteger.ZERO;//+
    public BigInteger accountId     = BigInteger.ZERO;//+
    public BigInteger vendorId      = BigInteger.ZERO;//+
    public BigInteger partnerId     = BigInteger.ZERO;//partner's product n/a
    public BigInteger agreementId   = BigInteger.ZERO;//partner - host agreement Id (terms) n/a
    
    public BigInteger lastActivity      = BigInteger.ZERO;//+
    public BigInteger insertDate        = BigInteger.ZERO;//+
    public BigInteger lastUpdate        = BigInteger.ZERO;//+
    
    public String itemName  = "";//+
    public String itemTitle = "";

    public BigDecimal firstPriceTag = new BigDecimal(0);//
    public BigDecimal lastPriceTag  = new BigDecimal(0);//+
    public boolean isDiscount = false;

    public String options = "";

    public String brandName = "";//+
    public String brandNameUI = "";//+
    public boolean isBrandNameVisible = false;//For instance; BELGIN //+

    public String category = "";//+
    
    public List<String> features;//+
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public List<String> hashtags;//+
    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }

    public String description = "";//n/a 
    public String imageDefault = "";//+
    public String images = "";//+

    public BigDecimal quantity = new BigDecimal(0);//+
    public BigDecimal str = new BigDecimal(0);//+ sell thru rate

    public ArrayList<ssoSEDocSimilarItem> similars = new ArrayList<ssoSEDocSimilarItem>();//similar items //+
    public String offers = "";//n/a

    public String reviews = "";//n/a

}

