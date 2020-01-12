/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.pages;

import entity.prm.SsPrmCities;
import entity.prm.SsPrmCityCodes;
import entity.prm.SsPrmCountryCodes;
import java.util.ArrayList;
import jaxesa.persistence.EntityManager;

/**
 *
 * @author esabil
 */
public final class ParamsMisc 
{
    public static ArrayList<SsPrmCountryCodes> getCountryCodes(EntityManager pem) throws Exception
    {
        try
        {
            ArrayList<SsPrmCountryCodes> CountryCodes = new ArrayList<SsPrmCountryCodes>();

            CountryCodes = (ArrayList<SsPrmCountryCodes>)pem.findAll(SsPrmCountryCodes.class);
            
            return CountryCodes;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<SsPrmCities> getCityCodes(EntityManager pem, String pCountryCode) throws Exception
    {
        try
        {
            ArrayList<SsPrmCities> CityCodes = new ArrayList<SsPrmCities>();

            CityCodes = (ArrayList<SsPrmCities>)pem.findAll(SsPrmCities.class);
            
            return CityCodes;
        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static void getMCCCodes(EntityManager pem) throws Exception
    {
        return ;
    }

    public static void getTownCodes(EntityManager pem, String pCountryCode, String pCityCode) throws Exception
    {
        return ;
    }
    
    public static void getCurrencyCodes(EntityManager pem) throws Exception
    {
        return ;
    }
}
