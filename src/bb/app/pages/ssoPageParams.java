/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.pages;

import java.util.ArrayList;

/**
 *
 * @author esabil
 */
public class ssoPageParams 
{
    public ArrayList<ssoCountryCodes> CountryCodes;
    public ArrayList<ssoCityCode> Cities;

    public ssoPageParams()
    {
        CountryCodes = new ArrayList<ssoCountryCodes>();
        Cities = new ArrayList<ssoCityCode>();
    }
}


