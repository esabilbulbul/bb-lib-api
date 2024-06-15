/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.obj;

/**
 *
 * @author esabil
 */
public class ssoMerchant 
{
    public String name;
    public long   Id;
    public String isDefault;
    public String isActivated;

    public String email;
    public String MCC;
    public String MCCName;

    public String CurrencyCode;
    public String CurrencyName;

    public String CountryCode;
    public String CountryName;

    public String StateCode;
    public String StateName;

    public String CountyCode;

    public String placeNameUID;
    public String placeName;
    public String isTaxIncluded;//in price
    public String taxRate;
    
    public ssoMerchant()
    {
        name = "";
        Id = 0;
        isDefault = "N";
    }
}
