/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.obj;

import java.util.ArrayList;

/**
 *
 * @author esabil
 */
public class ssoPageParams 
{
    public String CountryCodeDefault;
    public ArrayList<ssoCountryCodes> CountryCodes;
    public ArrayList<ssoCityCode> Cities;//multiple cities will be sent
    public ArrayList<ssoMCC> MCCs;

    public ssoPageParams()
    {
        CountryCodeDefault = "";
        CountryCodes = new ArrayList<ssoCountryCodes>();
        MCCs = new ArrayList<ssoMCC>();

        Cities = new ArrayList<ssoCityCode>();
    }
}


