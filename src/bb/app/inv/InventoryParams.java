/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.inv;


import bb.app.obj.ssoCityCode;
import bb.app.obj.ssoCountyCode;
import bb.app.obj.ssoInvBrandItemCodes;
import bb.app.obj.ssoInvCategory;
import bb.app.obj.ssoMCC;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvVendorStats;
import entity.prm.SsPrmCountryCodes;
import entity.prm.SsPrmCountryPostcodes;
import entity.prm.SsPrmCountryStates;
import entity.dct.SsDctInvCategories;
import entity.dct.SsDctInvVendorSummary;
import entity.prm.SsPrmMcc;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoKeyField;
import jaxesa.util.Util;
import jaxesa.webapi.ssoAPIResponse;

/**
 *
 * @author Administrator
 */
public final class InventoryParams 
{

    public static ArrayList<SsPrmCountryCodes> getCountryCodes(EntityManager pem) throws Exception
    {
        try
        {
            // WORKS WITH CACHE
            
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
            // WORKS WITH CACHE 
            
            ArrayList<ssoMCC> MCCs = new ArrayList<ssoMCC>();

            Query stmtFamily = pem.createNamedQuery("SsPrmMcc.getMCCCodes", SsPrmMcc.class);

            List<List<RowColumn>> rs = stmtFamily.getResultList();

            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoMCC newMCC = new ssoMCC();

                newMCC.code = Util.Database.getVal(rowN, "MCC").toString();
                newMCC.lang = "en";
                newMCC.name = Util.Database.getVal(rowN, "MCC_NAME").toString();

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
            // WORKS WITH CACHE
            
            ArrayList<ssoCityCode> countyCodes = new ArrayList<ssoCityCode>();

            Query stmtFamily;
            stmtFamily = pem.createNamedQuery("SsPrmCountryStates.getCityCodes", SsPrmCountryStates.class);
            
            int ParIndex = 1;
            stmtFamily.SetParameter(ParIndex++, pCountryCode, "COUNTRY_CODE");

            List<List<RowColumn>> rs = stmtFamily.getResultList(null);

            for (List<RowColumn> rowN:rs)
            {
                ssoCityCode newCity = new ssoCityCode();

                newCity.code = Util.Database.getVal(rowN, "STATE_CODE").toString();
                newCity.lang = "en";
                newCity.name = Util.Database.getVal(rowN, "STATE_NAME").toString();

                countyCodes.add(newCity);

            }

            return countyCodes;
            /*
            ArrayList<ssoCityCode> countyCodes = new ArrayList<ssoCityCode>();

            Query stmtFamily;
            if (pCountryCode.toLowerCase().trim().equals("tr")==true)
            {
                stmtFamily = pem.createNamedQuery("SsPrmCountryStates.getCityCodes", SsPrmCountryStates.class);
            }
            else
            {
                stmtFamily = pem.createNamedQuery("SsPrmCountryPostcodes.getCityCodes", SsPrmCountryPostcodes.class);
            }

            int ParIndex = 1;
            stmtFamily.SetParameter(ParIndex++, pCountryCode, "COUNTRY_CODE");

            List<List<RowColumn>> rs = stmtFamily.getResultList();

            for (List<RowColumn> rowN:rs)
            {
                ssoCityCode newCity = new ssoCityCode();

                newCity.code = Util.Database.getVal(rowN, "STATE_CODE").toString();
                newCity.lang = "en";
                newCity.name = Util.Database.getVal(rowN, "STATE_NAME").toString();

                countyCodes.add(newCity);

            }

            return countyCodes;
            */
        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static ArrayList<ssoCountyCode> getCountyCodes(EntityManager pem, String pCountryCode, String pStateCode) throws Exception
    {
        ArrayList<ssoCountyCode> countyCodes = new ArrayList<ssoCountyCode>();
        
        try
        {
            // WORKS WITH CACHE

            String KEY_POSTCODE     = "pst_code";
            String KEY_PCODE_NAME   = "pst_name";
            
            String KEY_COUNTY_CODE  = "county_code";
            String KEY_COUNTY_NAME  = "county";
            
            String KEY_LAT          = "lat";
            String KEY_LON          = "lon";

            /*
            Query stmtFamily;
            stmtFamily = pem.createNamedQuery("SsPrmCountryPostcodes.getCountyCodes", SsPrmCountryPostcodes.class);
            
            int Colindex = 1;
            stmtFamily.SetParameter(Colindex++, pCountryCode , "COUNTRY_CODE");
            stmtFamily.SetParameter(Colindex++, pStateCode   , "STATE_CODE");

            List<List<RowColumn>> rs = stmtFamily.getResultList();
            */
            List<List<RowColumn>> rs = getPostCodesSourceData(pem, pCountryCode, pStateCode);
            
            if (rs.size()>0)
            {
                List<RowColumn> rowN = rs.get(0);

                String sStateCode = Util.Database.getVal(rowN, "STATE_CODE").toString();
                String sStateName = Util.Database.getVal(rowN, "STATE_NAME").toString();
                String sPostCodes = Util.Database.getVal(rowN, "POSTCODES").toString();
                JsonArray jsonArray = (JsonArray) Util.JSON.toArray(sPostCodes);

                for (int j=0; j<jsonArray.size();j++)
                {

                    JsonObject item = (JsonObject)jsonArray.get(j);

                    JsonElement jeCountyCode  = item.get("uid"); // UID = COUNTY CODE
                    JsonElement jeNeighborName = item.get(KEY_PCODE_NAME);//WARNING: not county name GET PLACE NAME
                    //JsonElement jeCountry   = item.get(KEY_PCODE_NAME);

                    ssoCountyCode newCounty = new ssoCountyCode();

                    newCounty.code  =   jeCountyCode.toString().replace("\"", "");
                    newCounty.name  =   jeNeighborName.toString().replace("\"", "");
                    newCounty.lang = "en";

                    countyCodes.add(newCounty);
                }

                /*
                ssoCountyCode newCounty = new ssoCountyCode();

                newCounty.code = Util.Database.getVal(rowN, "COUNTY_CODE").toString();
                newCounty.lang = "en";
                newCounty.name = Util.Database.getVal(rowN, "COUNTY_NAME").toString();

                countyCodes.add(newCounty);
                */

            }

            return countyCodes;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // pCityCode = STATE CODE
    public static ArrayList<SsPrmCountryPostcodes> getPostCodes(EntityManager pem, String pCountryCode, String pCityCode) throws Exception
    {
        // WORKS WITH CACHE
        
        ArrayList<SsPrmCountryPostcodes> pstCodes = new ArrayList<SsPrmCountryPostcodes>();

        try
        {
            String KEY_POSTCODE     = "pst_code";
            String KEY_PCODE_NAME   = "pst_name";
            String KEY_COUNTY       = "county";
            String KEY_LAT          = "lat";
            String KEY_LON          = "lon";
            
            List<List<RowColumn>> rs = getPostCodesSourceData(pem, pCountryCode, pCityCode);
            //for(int i=0; i<rs.size(); i++)
            if (rs.size()>0)
            {
                List<RowColumn> rowN = rs.get(0);

                String sStateCode = Util.Database.getVal(rowN, "STATE_CODE").toString();
                String sStateName = Util.Database.getVal(rowN, "STATE_NAME").toString();
                String sPostCodes = Util.Database.getVal(rowN, "POSTCODES").toString();
                JsonArray jsonArray = (JsonArray) Util.JSON.toArray(sPostCodes);

                for (int j=0; j<jsonArray.size();j++)
                {
                    JsonObject item = (JsonObject)jsonArray.get(j);

                    JsonElement jePostCode = item.get(KEY_POSTCODE);

                    if(jePostCode!=null)
                    {
                        SsPrmCountryPostcodes newPostCode = new SsPrmCountryPostcodes();

                        newPostCode.postCode    = jePostCode.toString().replace("\"", "");
                        newPostCode.placeName   = item.get(KEY_PCODE_NAME).toString().replace("\"", "");
                        newPostCode.countyName  = item.get(KEY_COUNTY).toString().replace("\"", "");
                        newPostCode.lat         = item.get(KEY_LAT).toString().replace("\"", "");
                        newPostCode.lon         = item.get(KEY_LON).toString().replace("\"", "");
                        newPostCode.stateCode   = sStateCode;
                        newPostCode.stateName   = sStateName;
                        
                        pstCodes.add(newPostCode);
                    }
                }
            }
            
            return pstCodes;

            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_GET_POSTCODES");

            SP.registerStoredProcedureParameter("P_COUNTRY_CODE"    , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CITY_CODE"       , String.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pCountryCode, "P_COUNTRY_CODE");
            SP.SetParameter(Colindex++, pCityCode   , "P_CITY_CODE");

            SP.execute();

            ArrayList<SsPrmCountryPostcodes> PostCodes =  (ArrayList<SsPrmCountryPostcodes>)SP.getResultList(SsPrmCountryPostcodes.class);

            return PostCodes;
            */
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static List<List<RowColumn>> getPostCodesSourceData(EntityManager pem, String pCountryCode, String pCityCode)
    {
        try
        {
            // WORKS WITH CACHE

            Query stmtFamily;
            stmtFamily = pem.createNamedQuery("SsPrmCountryPostcodes.getPostodes", SsPrmCountryPostcodes.class);

            int Colindex = 1;
            stmtFamily.SetParameter(Colindex++, pCountryCode , "COUNTRY_CODE");
            stmtFamily.SetParameter(Colindex++, pCityCode    , "STATE_CODE");

            ArrayList<ssoKeyField> keys = new ArrayList<ssoKeyField>();

            boolean rc = false;
            List<List<RowColumn>>  rs = stmtFamily.getResultList();

            return rs;
        }
        catch(Exception e)
        {
            return null;
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

    // This function returns the list of brands associated with the merchant
    public static ArrayList<ssoInvBrandItemCodes> getVendorItemCodeSummary(EntityManager pem, long pUserId) throws Exception
    {
        // WORKS WITH CACHE

        ArrayList<ssoInvBrandItemCodes> BrandList = new ArrayList<ssoInvBrandItemCodes>();

        try
        {
            Query stmtFamily = pem.createNamedQuery("SsDctInvItemCodes.getBrandNItemList", SsDctInvVendorSummary.class);
            int index = 1;
            stmtFamily.SetParameter(index++, pUserId, "USER_ID");
            //stmtFamily.SetParameter(index++, psFamilyCode , "FAMILY_CODE");
            List<List<RowColumn>> rs = stmtFamily.getResultList();

            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoInvBrandItemCodes newBrandItem = new ssoInvBrandItemCodes();
                newBrandItem.brandId   = Util.Database.getVal(rowN, "BRAND_ID").toString().trim();
                newBrandItem.brandName = Util.Database.getVal(rowN, "BRAND_NAME").toString().trim().toUpperCase();

                String sItemCodeList = Util.Database.getVal(rowN, "CODE_LIST").toString().trim().toUpperCase();

                //JsonObject jsoItemCodeList = Util.JSON.toJsonObject(sItemCodeList);
                JsonArray jsaItemCodeList = Util.JSON.toArray(sItemCodeList);

                newBrandItem.itemCodes = "[";
                if (jsaItemCodeList!=null)
                {
                    for(int j=0; j<jsaItemCodeList.size(); j++)
                    {
                        if (j!=0)
                            newBrandItem.itemCodes += ",";
                        
                        newBrandItem.itemCodes += jsaItemCodeList.get(j).toString();//Util.Str.QUOTE(jsaItemCodeList.get(j).toString());
                    }
                }

                newBrandItem.itemCodes += "]";

                /*
                newBrandItem.itemCodes = "[";
                while(oList.hasNext())
                {
                    newBrandItem.itemCodes += Util.Str.QUOTE(oList.next());
                }
                newBrandItem.itemCodes = "]";
                */

                BrandList.add(newBrandItem);
            }

            return BrandList;
            /*
            ArrayList<ssoKeyField> keys = new ArrayList<ssoKeyField>();

            ssoKeyField keyAccId = new ssoKeyField();
            keyAccId.ColumnName = "ACCOUNT_ID";
            keyAccId.Value      = pMrcId;
            keys.add(keyAccId);

            ArrayList<SsInvBrands> brands = new ArrayList<SsInvBrands>();
            brands = (ArrayList<SsInvBrands>)pem.findAll(SsInvBrands.class, keys);
            for (SsInvBrands brandN: brands)
            {
                ssoInvBrandItemCodes newBrand = new ssoInvBrandItemCodes();
                //newBrand.brandName = brandN.uid;
                //newBrand.name = brandN.brand;

                BrandList.add(newBrand); 
            }

            return BrandList;
            */
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    //This function returns categories associated with the merchant
    public static ArrayList<ssoInvCategory> getCategoryList(EntityManager pem, long pMrcId) throws Exception
    {
        
        ArrayList<ssoInvCategory> ctgList = new ArrayList<ssoInvCategory>();

        try
        {
            //ctg = pem.find(SsPrmInvCategories.class, pMrcId , true, "ACCOUNT_ID");
            //ctg = pem.find(SsPrmInvCategories.class, pMrcId);
            ArrayList<ssoKeyField> keys = new ArrayList<ssoKeyField>();

            ssoKeyField keyAccId = new ssoKeyField();
            keyAccId.ColumnName = "ACCOUNT_ID";
            keyAccId.Value      = pMrcId;
            keys.add(keyAccId);

            ArrayList<SsDctInvCategories> ctgs = new ArrayList<SsDctInvCategories>();
            ctgs = (ArrayList<SsDctInvCategories>)pem.findAll(SsDctInvCategories.class, keys);
            for (SsDctInvCategories ctgN: ctgs)
            {
                ssoInvCategory newCategory = new ssoInvCategory();
                newCategory.code = ctgN.uid;
                newCategory.name = ctgN.category.trim().toUpperCase();

                ctgList.add(newCategory);
            }

            return ctgList;

            //ctgList = (ArrayList<SsPrmInvCategories>)pem.findAll(SsPrmInvCategories.class);

            /*
            ArrayList<ssoInvCategory> Categories = new ArrayList<ssoInvCategory>();

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_MRC_GET_CATEGORIES");
            SP.registerStoredProcedureParameter("P_ACC_ID"    , Long.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pMrcId, "P_ACC_ID");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            for (List<RowColumn> RowN:rs)
            {
                ssoInvCategory newCategory = new ssoInvCategory();

                newCategory.name = Util.Database.getVal(RowN, "CATEGORY").toString();

                Categories.add(newCategory);
            }

            return Categories;
            */
        }
        catch(Exception e)
        {
            throw e;
        }
    }

}
