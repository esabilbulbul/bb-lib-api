/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.pages;

import entity.prm.SsPrmCountryStates;
import entity.prm.SsPrmCountryCodes;
import entity.prm.SsPrmCountryPostcodes;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.util.Util;
import jaxesa.webapi.sso_APIResponse;

/**
 *
 * @author esabil
 * 
 * Country 
 *     - State / City
 *          - County
 *              - Suburb / Postcode
 * 
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

    public static ArrayList<ssoMCC> getMCCs(EntityManager pem) throws Exception
    {
        try
        {
            ArrayList<ssoMCC> MCCs = new ArrayList<ssoMCC>();
            
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_GET_MCC_CODES");

            //SP.registerStoredProcedureParameter("P_COUNTRY_CODE"    , String.class         , ParameterMode.IN);

            //int Colindex = 1;
            //SP.SetParameter(Colindex++, pCountryCode, "P_COUNTRY_CODE");

            SP.execute();
            
            List<List<RowColumn>> rs =  SP.getResultList();
            for (List<RowColumn> RowN:rs)
            {
                ssoMCC newMCC = new ssoMCC();

                newMCC.code = Util.Database.getVal(RowN, "MCC").toString();
                newMCC.lang = "en";
                newMCC.name = Util.Database.getVal(RowN, "MCC_NAME").toString();

                MCCs.add(newMCC);
            }

            return MCCs;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    // City / State
    public static ArrayList<ssoCityCode> getCityCodes(EntityManager pem, String pCountryCode) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_GET_CITY_CODES");

            SP.registerStoredProcedureParameter("P_COUNTRY_CODE"    , String.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pCountryCode, "P_COUNTRY_CODE");

            SP.execute();

            //ArrayList<SsPrmCountryStates> CityCodes =  (ArrayList<SsPrmCountryStates>)SP.getResultList(SsPrmCountryStates.class);
            //return CityCodes;

            ArrayList<ssoCityCode> countyCodes = new ArrayList<ssoCityCode>();

            List<List<RowColumn>> rs =  SP.getResultList();
            for (List<RowColumn> RowN:rs)
            {
                ssoCityCode newCity = new ssoCityCode();

                newCity.code = Util.Database.getVal(RowN, "STATE_CODE").toString();
                newCity.lang = "en";
                newCity.name = Util.Database.getVal(RowN, "STATE_NAME").toString();

                countyCodes.add(newCity);
            }

            return countyCodes;

/*
            ArrayList<SsPrmCities> CityCodes = new ArrayList<SsPrmCities>();

            CityCodes = (ArrayList<SsPrmCities>)pem.findAll(SsPrmCities.class);
            
            return CityCodes;
*/
        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static ArrayList<ssoCountyCode> getCountyCodes(EntityManager pem, String pCountryCode, String pStateCode) throws Exception
    {
        try
        {
            sso_APIResponse Rsp = new sso_APIResponse();
            
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_GET_COUNTIES");

            SP.registerStoredProcedureParameter("P_COUNTRY_CODE"    , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CITY_CODE"       , String.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pCountryCode , "P_COUNTRY_CODE");
            SP.SetParameter(Colindex++, pStateCode   , "P_CITY_CODE");

            SP.execute();

            ArrayList<ssoCountyCode> countyCodes = new ArrayList<ssoCountyCode>();

            List<List<RowColumn>> rs =  SP.getResultList();
            for (List<RowColumn> RowN:rs)
            {
                ssoCountyCode newCounty = new ssoCountyCode();

                newCounty.code = Util.Database.getVal(RowN, "COUNTY_CODE").toString();
                newCounty.lang = "en";
                newCounty.name = Util.Database.getVal(RowN, "COUNTY_NAME").toString();
                
                countyCodes.add(newCounty);
            }

            return countyCodes;

        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static ArrayList<SsPrmCountryPostcodes> getPostCodes(EntityManager pem, String pCountryCode, String pCityCode) throws Exception
    {
        try
        {
            sso_APIResponse Rsp = new sso_APIResponse();
            
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_GET_POSTCODES");

            SP.registerStoredProcedureParameter("P_COUNTRY_CODE"    , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CITY_CODE"       , String.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pCountryCode, "P_COUNTRY_CODE");
            SP.SetParameter(Colindex++, pCityCode   , "P_CITY_CODE");

            SP.execute();

            ArrayList<SsPrmCountryPostcodes> PostCodes =  (ArrayList<SsPrmCountryPostcodes>)SP.getResultList(SsPrmCountryPostcodes.class);

            return PostCodes;

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
