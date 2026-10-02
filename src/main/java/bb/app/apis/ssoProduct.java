/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.apis;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class ssoProduct {

    public String pageURL  = "";
    public ArrayList<ssoImage> itemImages = new ArrayList<ssoImage>();
    public ArrayList<ssoOption> options = new ArrayList<ssoOption>();

    public String oldPrice = "";//priceTag
    public String newPrice = "";
    
    public String quantity = "0";//quantity in inventory
    public String sectionKey = "";//new season , on sale , ...

    public ssoProduct(String pPageUrl, String pSection, String pOldPrice, String pNewPrice, String pQuantity, ArrayList<ssoImage> paImages)
    {
        pageURL = pPageUrl;
        sectionKey  = pSection;
        oldPrice = pOldPrice;
        newPrice = pNewPrice;
        quantity = pQuantity;
        
        itemImages = paImages;
    }


}


