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
public class ssoMerchantPreferences 
{
    public long Id;//Account Id - User
    public long version;
    public String MerchantName;
    public String CurrencyCode;
    public String CurrencyName;
    public String CountryCode;
    public String CountryName;
    public String StateCode;
    public String StateName;
    public String CountyCode;
    public String CountyName;
    public String PlaceNameUID;
    public String PlaceName;
    public String MCC;
    public String MCCName;
    public String email;
    public String profileName;
    public String isTaxInSalesPrice;
    public String taxRate;
    public String insDiffRate;//if the payment not advance
    public String isActive;
    
    public ssoMerchantPreferences()
    {
        Id = -1;
        version = -1;
        MerchantName = "";
        CurrencyCode = "";
        CurrencyName = "";
        CountryCode  = "";
        CountryName  = "";
        StateCode    = "";
        StateName    = "";
        CountyCode   = "";
        CountyName   = "";
        isActive     = "";
    }
}
