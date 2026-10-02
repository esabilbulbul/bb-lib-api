/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.estore;

import bb.app.report.invsearch.ssoUIBalanceReportVendor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemStats;
import entity.dct.SsDctInvCategories;
import entity.dct.SsDctInvFeatures;
import entity.dct.SsDctInvHashtags;
import entity.dct.SsDctInvVendorSummary;
import entity.txn.SsTxnInvBill;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;
import jaxesa.util.ssoDBRowLimits;
import org.json.JSONObject;

/**
 *
 * @author Administrator
 */
public class ssEStoreOps
{
    public static ArrayList<ssoEItemRow> searchByKeyword(EntityManager   pem,
                                                         BigInteger      pUserId,
                                                         BigInteger      pAccountId,
                                                         String          pKeyword,
                                                         int             pPageIndex,
                                                         boolean         pbReset) throws Exception
    {

        ArrayList<ssoSearchMatch> aMatches = new ArrayList<ssoSearchMatch>();
        ArrayList<ssoEItemRow> aRows       = new ArrayList<ssoEItemRow>();
        String sQuery = "";

        int iRowPerPage = 50;//DEFAULT
        int iPageIndex  = pPageIndex;
        //int iOffset     = iPageIndex *  iRowPerPage;//skip first N records
        //int iLimit      = (iPageIndex+1) * iRowPerPage;//50 per page

        try
        {
            ssoDBRowLimits rowLimits = new ssoDBRowLimits();
            rowLimits = Util.Database.calculateRowLimits(iRowPerPage, pPageIndex);
            int iOffset     = rowLimits.offset;
            int iLimit      = rowLimits.limit;

            // SEARCH WHICH VENDORS and ITEM CODES hit the criteria 
            aMatches = findMatches(pem, pUserId, pAccountId, pKeyword, pbReset);
            if(aMatches.size()>0)
            {
                sQuery = "SELECT ITM.UID, IF(ITM.ITEM_NAME_MANUAL='',ITM.ITEM_NAME, ITM.ITEM_NAME_MANUAL) AS ITEM_NAME, ITM.BYUSER, ITEM_CODE, VENDOR_ID, VND.BRAND AS VENDOR_NAME, PRM_CATEGORY_ID, CTG.CATEGORY, ITM.FEATURES_ID_MANUAL, FT.NAME AS FEATURE_NAME, FT.FEATURES, ITM.HASHTAGS_ID_MANUAL, KY.NAME AS HASHTAG_NAME, KY.HASHTAGS, PRICE_ID, LAST_ENTRY_PRICE, LAST_SALE_PRICE, IFNULL(IMG_URL_DEFAULT,'') AS IMG_URL_DEFAULT, JSON_MERGE_PRESERVE(IFNULL(IF(IMG_URLS='','[]',IMG_URLS),'[]'), IFNULL(IF(VID_URLS='','[]',VID_URLS),'[]')) AS IMG_URLS, IS_WEB_ACTIVE , ITM.INSERTDATE, ITM.LASTUPDATE, FN_INV_CALC_PRICE_AFTER_CAMPAIGN(0, ITM.ACCOUNT_ID, ITM.VENDOR_ID, ITM.ITEM_CODE, ITM.UID, ITM.PRM_CATEGORY_ID, CTG.CATEGORY, ITM.LAST_ENTRY_PRICE, ITM.LAST_SALE_PRICE) AS PRICE_FINAL " +
                         "FROM ss_acc_inv_item_stats ITM " +
                         "INNER JOIN ss_acc_inv_vendors VND ON VND.UID = ITM.VENDOR_ID " +
                         "LEFT JOIN ss_dct_inv_categories CTG ON CTG.UID = ITM.PRM_CATEGORY_ID " +
                         "LEFT JOIN ss_dct_inv_features FT ON FT.UID = ITM.FEATURES_ID_MANUAL " +
                         "LEFT JOIN ss_dct_inv_hashtags KY ON KY.UID = ITM.HASHTAGS_ID_MANUAL " +
                         "WHERE " +
                         "ITM.STAT = 1 " + 
                         "AND " +
                         "VND.USER_ID = ? " +
                         "AND ";

                // ADDING VENDOR IDs to the QUERY 
                //-------------------------------------------------------
                int iv = 0;
                for(ssoSearchMatch matchN: aMatches)
                {
                    if(matchN.type.equals("V")==true)
                    {
                        if(iv==0)
                        {
                            sQuery += "ITM.VENDOR_ID IN (";
                            sQuery += "?";
                        }
                        else
                        {
                            sQuery += ",";
                            sQuery += "?";
                        }

                        iv++;
                    }
                }

                if(iv>0)
                    sQuery += ") ";

                // ADDING ITEMCODES to the QUERY
                //-------------------------------------------------------
                int ic = 0;
                for(ssoSearchMatch matchN: aMatches)
                {
                    if(matchN.type.equals("I")==true)
                    {
                        if(ic==0)
                        {
                            sQuery += "ITEM_CODE IN (";
                            sQuery += "?";
                        }
                        else
                        {
                            sQuery += ",";
                            sQuery += "?";
                        }

                        ic++;
                    }
                }

                if(ic>0)
                    sQuery += ") ";

                sQuery += "ORDER BY IS_WEB_ACTIVE DESC ";
                sQuery += "LIMIT ? OFFSET ?";

                Query stmt = pem.CreateNativeQuery(sQuery);
                int index = 1;
                stmt.SetParameter(index++, pUserId       , "USER_ID");

                // SETTING VENDOR IDs
                //--------------------------------------
                for(ssoSearchMatch matchN: aMatches)
                {
                    if(matchN.type.equals("V")==true)
                    {
                        stmt.SetParameter(index++, matchN.uid, "VENDOR_ID");
                    }
                }

                // SETTING ITEM CODES 
                //-------------------------------------- 
                for(ssoSearchMatch matchN: aMatches)
                {
                    if(matchN.type.equals("I")==true)
                    {
                        stmt.SetParameter(index++, matchN.code, "ITEM_CODE");
                    }
                }

                stmt.SetParameter(index++, iLimit        , "LIMIT");
                stmt.SetParameter(index++, iOffset       , "OFFSET");

                int iStartIndex = iPageIndex * iRowPerPage;

                List<List<RowColumn>> rs = stmt.getResultList();
                for(int i=0;i<rs.size();i++)
                {
                    ssoEItemRow itemRowN = new ssoEItemRow();

                    itemRowN.itemId    = Util.Database.getValString(rs.get(i), "UID");
                    itemRowN.itemCode  = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                    itemRowN.itemName  = Util.Database.getValString(rs.get(i), "ITEM_NAME");
                    
                    itemRowN.user = Util.Database.getValString(rs.get(i), "BYUSER");
                    itemRowN.accountId   = Util.Database.getValString(rs.get(i), "ACCOUNT_ID");
                    itemRowN.vendorId    = Util.Database.getValString(rs.get(i), "VENDOR_ID");
                    itemRowN.vendorName  = Util.Database.getValString(rs.get(i), "VENDOR_NAME");
                    

                    itemRowN.categoryId = Util.Database.getValString(rs.get(i), "PRM_CATEGORY_ID");
                    itemRowN.category   = Util.Database.getValString(rs.get(i), "CATEGORY");

                    itemRowN.featureGroupId   = Util.Database.getValString(rs.get(i), "FEATURES_ID_MANUAL");
                    itemRowN.featureGroupName = Util.Database.getValString(rs.get(i), "FEATURE_NAME");
                    itemRowN.features         = Util.Database.getValString(rs.get(i), "FEATURES");

                    itemRowN.hashtagGroupId   = Util.Database.getValString(rs.get(i), "HASHTAGS_ID_MANUAL");
                    itemRowN.hashtagGroupName = Util.Database.getValString(rs.get(i), "HASHTAG_NAME");
                    itemRowN.hashtags         = Util.Database.getValString(rs.get(i), "HASHTAGS");

                    itemRowN.priceId          = Util.Database.getValString(rs.get(i), "PRICE_ID");
                    itemRowN.priceEntry       = Util.Database.getValString(rs.get(i), "LAST_ENTRY_PRICE");
                    itemRowN.priceTag         = Util.Database.getValString(rs.get(i), "PRICE_FINAL");

                    itemRowN.imgUrlDefault    = Util.Database.getValString(rs.get(i), "IMG_URL_DEFAULT");
                    itemRowN.imgUrls          = Util.Database.getValString(rs.get(i), "IMG_URLS");

                    itemRowN.isWebActive         = Util.Database.getValString(rs.get(i), "IS_WEB_ACTIVE");

                    itemRowN.insertDate         = Util.Database.getValString(rs.get(i), "INSERTDATE");
                    itemRowN.lastupdate         = Util.Database.getValString(rs.get(i), "LASTUPDATE");

                    aRows.add(itemRowN);
                }

            }// end of match iteration (if)

            return aRows;
        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static ArrayList<ssoSearchMatch> findMatches(EntityManager   pem,
                                                        BigInteger      pUserId,
                                                        BigInteger      pAccountId,
                                                        String          pKeyword,
                                                        boolean         pbReset) throws Exception
    {
        BigInteger biVendorId    = BigInteger.ZERO;
        String     sItemCodeList = "";
        String     sBrandName    = "";

        ArrayList<ssoSearchMatch> aMatches = new ArrayList<ssoSearchMatch>();

        try
        {
            Query stmt = pem.createNamedQuery("SsDctInvItemCodes.getBrandNItemList", SsDctInvVendorSummary.class);

            int index = 1;
            stmt.SetParameter(index++, pUserId         ,"USER_ID");

            List<List<RowColumn>> rs =  stmt.getResultList();
            long lCnt = 0;
            for(int i=0;i<rs.size();i++)
            {
                List<RowColumn> rowN = rs.get(i);

                biVendorId    = new BigInteger(Util.Database.getValString(rowN, "BRAND_ID").toString());
                sBrandName = Util.Database.getValString(rowN, "BRAND_NAME").toString();
                sItemCodeList = Util.Database.getValString(rowN, "CODE_LIST").toString();

                // 1. SEARCH IN BRAND NAME
                // 2. SEARCH IN CATEGORY
                // 3. SEARCH IN FEATURES
                // 4. SEARCH IN ITEM NAME

                // SEARCH IN BRAND NAME
                if(Util.Str.SIMIL(sBrandName.toLowerCase(), pKeyword.toLowerCase())>=75)
                {
                    ssoSearchMatch matchN = new ssoSearchMatch();

                    matchN.uid  = biVendorId;
                    matchN.code = sBrandName;
                    matchN.type = "V";
                    aMatches.add(matchN);
                }

                // SEARCH IN ITEM CODE
                if(sItemCodeList!=null)
                {
                    JsonArray jsaItemCodes = (JsonArray)Util.JSON.toArray(sItemCodeList);
                    for (int j=0; j<jsaItemCodes.size();j++)
                    {
                        String sItemCode = jsaItemCodes.get(j).getAsString();

                        //String sItemCode = itemCode.getAsString();

                        if(Util.Str.SIMIL(sItemCode.toLowerCase(), pKeyword.toLowerCase())>=80)
                        {
                            ssoSearchMatch matchN = new ssoSearchMatch();

                            matchN.code = sItemCode;
                            matchN.type = "I";//itemcode
                            aMatches.add(matchN);
                        }
                    }
                }

                //
            }

            return aMatches;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoHashTag createNewHashtagGroup(EntityManager   pem,
                                                BigInteger      pUserId,
                                                BigInteger      pAccountId,
                                                //BigInteger      pCategoryId,
                                                String          pGroup,
                                                String          pName,
                                                String          pHashTags) throws Exception
    {
        ssoHashTag oHashTag = new ssoHashTag();

        try
        {
            SsDctInvHashtags newHashTag = new SsDctInvHashtags();

            newHashTag.userId   = pUserId;
            newHashTag.name     = pName;
            newHashTag.hashtags = pHashTags;
            newHashTag.htGroup    = pGroup;

            BigInteger lHashTagGroupId =  pem.persist(newHashTag);

            //reset memory
            resetHashTagMemory(pem, pUserId);

            oHashTag.Id   = lHashTagGroupId;
            oHashTag.name = pName;

            return oHashTag;
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_WEB_CREATE_NEW_HASHTAG_GROUP");

            SP.registerStoredProcedureParameter("USER_ID"       , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("ACCOUNT_ID"    , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("GROUP_NAME"    , String.class           , ParameterMode.IN);
            SP.registerStoredProcedureParameter("HASH_TAGS"     , String.class           , ParameterMode.IN);

            int index = 1;
            SP.SetParameter(index++, pUserId         , "USER_ID");
            SP.SetParameter(index++, pAccountId      , "ACCOUNT_ID");
            SP.SetParameter(index++, pName           , "GROUP_NAME");
            SP.SetParameter(index++, pHashTags       , "HASH_TAGS");

            SP.execute();
            
            return true;
            */
        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static boolean updateNewHashtagGroup(EntityManager   pem,
                                                BigInteger      pUserId,
                                                BigInteger      pAccountId,
                                                BigInteger      pHashTagGroupId,
                                                //String          pName,
                                                String          pHashTags) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_WEB_UPDATE_HASHTAG_GROUP");

            //SP.registerStoredProcedureParameter("USER_ID"       , BigInteger.class       , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("ACCOUNT_ID"    , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("GROUP_ID"        , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("HASH_TAGS"       , String.class           , ParameterMode.IN);

            int index = 1;
            //SP.SetParameter(index++, pUserId         , "USER_ID");
            //SP.SetParameter(index++, pAccountId      , "ACCOUNT_ID");
            SP.SetParameter(index++, pHashTagGroupId , "GROUP_ID");
            SP.SetParameter(index++, pHashTags       , "HASH_TAGS");

            SP.execute();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }

    }
    
    public static void resetHashTagMemory(EntityManager   pem, BigInteger      pUserId) throws Exception
    {
        try
        {
            // SsDctInvHashtags
            //------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys1 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey ColY1 = new ssoCacheSplitKey();
            ColY1.column = "USER_ID";
            ColY1.value  = pUserId;
            keys1.add(ColY1);

            pem.flush(SsDctInvHashtags.class, keys1);
        }
        catch(Exception e)
        {
            throw e;
        }    
    }

    public static void resetFeatureMemory(EntityManager   pem, BigInteger      pUserId) throws Exception
    {
        try
        {
            // SsDctInvHashtags
            //------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys1 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey ColY1 = new ssoCacheSplitKey();
            ColY1.column = "USER_ID";
            ColY1.value  = pUserId;
            keys1.add(ColY1);

            pem.flush(SsDctInvFeatures.class, keys1);
        }
        catch(Exception e)
        {
            throw e;
        }    
    }

    public static ssoHashTag isHashTagGroupNameExists( EntityManager   pem,
                                                    BigInteger      pUserId,
                                                    //BigInteger      pAccountId,
                                                    //BigInteger      pCategoryId,
                                                    String          pName) throws Exception
    {
        ssoHashTag oHashTag = new ssoHashTag();
        
        try
        {
            Query stmtPrintData = pem.createNamedQuery("SsDctInvHashtags.getHashtags", SsDctInvHashtags.class);

            int index = 1;
            stmtPrintData.SetParameter(index++, pUserId          , "USER_ID");

            List<List<RowColumn>> rs = stmtPrintData.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);
                
                BigInteger biHashTagId = new BigInteger(Util.Database.getValString(rowN, "UID").toString());
                String sHashTagName = Util.Database.getValString(rowN, "NAME").toString();
                
                if(sHashTagName.trim().toLowerCase().equals(pName)==true)
                {
                    oHashTag.Id = biHashTagId;
                    oHashTag.name = sHashTagName;
                    
                    return oHashTag;
                }
            }

            return oHashTag;

        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static ArrayList<ssoHashTag> getHashtagGroups(   EntityManager   pem,
                                                            BigInteger      pUserId,
                                                            String          pGroup) throws Exception
    {
        ArrayList<ssoHashTag> aHashTag = new ArrayList<ssoHashTag>();
        
        try
        {
            Query stmtPrintData = pem.createNamedQuery("SsDctInvHashtags.getHashtagsByGroup", SsDctInvHashtags.class);

            int index = 1;
            stmtPrintData.SetParameter(index++, pUserId          , "USER_ID");
            stmtPrintData.SetParameter(index++, pGroup           , "HT_GROUP");

            List<List<RowColumn>> rs = stmtPrintData.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);
                
                BigInteger biHashTagId = new BigInteger(Util.Database.getValString(rowN, "UID").toString());
                String sHashTagName = Util.Database.getValString(rowN, "NAME").toString();
                
                ssoHashTag oHashTag = new ssoHashTag();

                oHashTag.Id = biHashTagId;
                oHashTag.name = sHashTagName;

                aHashTag.add(oHashTag);
            }
            
            return aHashTag;

        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static ssoHashTag createNewFeaturesGroup(EntityManager   pem,
                                                    BigInteger      pUserId,
                                                    BigInteger      pAccountId,
                                                    //BigInteger      pCategoryId,
                                                    String          pGroup,
                                                    String          pName,
                                                    String          pHashTags) throws Exception
    {
        ssoHashTag oFeature = new ssoHashTag();

        try
        {
            SsDctInvFeatures newFeature = new SsDctInvFeatures();

            newFeature.userId   = pUserId;
            newFeature.name     = pName;
            newFeature.features = pHashTags;
            newFeature.ftGroup  = pGroup;

            BigInteger lFeatureGroupId =  pem.persist(newFeature);

            //reset memory
            resetHashTagMemory(pem, pUserId);

            oFeature.Id   = lFeatureGroupId;
            oFeature.name = pName;

            return oFeature;
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_WEB_CREATE_NEW_HASHTAG_GROUP");

            SP.registerStoredProcedureParameter("USER_ID"       , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("ACCOUNT_ID"    , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("GROUP_NAME"    , String.class           , ParameterMode.IN);
            SP.registerStoredProcedureParameter("HASH_TAGS"     , String.class           , ParameterMode.IN);

            int index = 1;
            SP.SetParameter(index++, pUserId         , "USER_ID");
            SP.SetParameter(index++, pAccountId      , "ACCOUNT_ID");
            SP.SetParameter(index++, pName           , "GROUP_NAME");
            SP.SetParameter(index++, pHashTags       , "HASH_TAGS");

            SP.execute();
            
            return true;
            */
        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static boolean updateNewFeatureGroup(EntityManager   pem,
                                                BigInteger      pUserId,
                                                BigInteger      pAccountId,
                                                BigInteger      pHashTagGroupId,
                                                //String          pName,
                                                String          pHashTags) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_WEB_UPDATE_FEATURE_GROUP");

            //SP.registerStoredProcedureParameter("USER_ID"       , BigInteger.class       , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("ACCOUNT_ID"    , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("GROUP_ID"        , BigInteger.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("FEATURES"        , String.class           , ParameterMode.IN);

            int index = 1;
            //SP.SetParameter(index++, pUserId         , "USER_ID");
            //SP.SetParameter(index++, pAccountId      , "ACCOUNT_ID");
            SP.SetParameter(index++, pHashTagGroupId , "GROUP_ID");
            SP.SetParameter(index++, pHashTags       , "FEATURES");

            SP.execute();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static ssoHashTag isFeatureGroupNameExists(  EntityManager   pem,
                                                        BigInteger      pUserId,
                                                        //BigInteger      pAccountId,
                                                        //BigInteger      pCategoryId,
                                                        String          pName) throws Exception
    {
        ssoHashTag oHashTag = new ssoHashTag();

        try
        {
            Query stmtPrintData = pem.createNamedQuery("SsDctInvFeatures.getFeatures", SsDctInvFeatures.class);

            int index = 1;
            stmtPrintData.SetParameter(index++, pUserId          , "USER_ID");

            List<List<RowColumn>> rs = stmtPrintData.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                BigInteger biHashTagId = new BigInteger(Util.Database.getValString(rowN, "UID").toString());
                String sHashTagName = Util.Database.getValString(rowN, "NAME").toString();

                if(sHashTagName.trim().toLowerCase().equals(pName)==true)
                {
                    oHashTag.Id = biHashTagId;
                    oHashTag.name = sHashTagName;

                    return oHashTag;
                }
            }

            return oHashTag;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean updateCateogry(   EntityManager                    pem,
                                            BigInteger                       pUserId,
                                            ArrayList<bb.estore.ssoCategory> paChanges
                                            //BigInteger      pCategoryId,
                                            //String          pCategoryUI,
                                            //BigInteger      pDefaultHashtagId,
                                            //BigInteger      pDefaultFeaturesId
                                        ) throws Exception
    {

        try
        {
                Query stmt = pem.createNamedQuery("SsDctInvCategories.updateCategory", SsDctInvCategories.class);

                for(bb.estore.ssoCategory chgN: paChanges)
                {
                    int ParIndex = 1;

                    BigInteger biCategoryId = new BigInteger(chgN.Id);
                    BigInteger biHashTagId  = new BigInteger(chgN.hashTagId);
                    BigInteger biFeatureId  = new BigInteger(chgN.featureId);

                    stmt.SetParameter(ParIndex++, chgN.name     , "CATEGORY");
                    stmt.SetParameter(ParIndex++, chgN.nameUI   , "CATEGORY_UI");
                    stmt.SetParameter(ParIndex++, biHashTagId   , "HASHTAG_ID");
                    stmt.SetParameter(ParIndex++, biFeatureId   , "FEATURE_ID");
                    stmt.SetParameter(ParIndex++, biCategoryId  , "CATEGORY_ID");

                    stmt.addBatch();
                }

                int[] AffectedRows = stmt.executeBatch();

                return true;

        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static boolean updateItems(   EntityManager                    pem,
                                         BigInteger                       pUserId,
                                         BigInteger                       pAccId,
                                         ArrayList<bb.estore.ssoItem>     paChanges
                                            //BigInteger      pCategoryId,
                                            //String          pCategoryUI,
                                            //BigInteger      pDefaultHashtagId,
                                            //BigInteger      pDefaultFeaturesId
                                     ) throws Exception
    {
        
        try
        {
                Query stmt = pem.createNamedQuery("SsAccInvItemStats.updateItemEStoreSettings", SsAccInvItemStats.class);

                for(bb.estore.ssoItem chgN: paChanges)
                {
                    int ParIndex = 1;

                    BigInteger biItemId     = new BigInteger(chgN.Id);
                    BigInteger biVendorId   = new BigInteger(chgN.vendorId);
                    BigInteger biCategoryId = new BigInteger(chgN.categoryId);
                    BigInteger biHashTagId  = new BigInteger(chgN.hashTagId);
                    BigInteger biFeatureId  = new BigInteger(chgN.featureId);

                    stmt.SetParameter(ParIndex++, chgN.active   , "IS_WEB_ACTIVE");

                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");
                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");

                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");
                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");
                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");

                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");
                    stmt.SetParameter(ParIndex++, chgN.name     , "ITEM_NAME");

                    stmt.SetParameter(ParIndex++, biHashTagId   , "HASHTAGS_ID_MANUAL");
                    stmt.SetParameter(ParIndex++, biFeatureId   , "FEATURES_ID_MANUAL");
                    stmt.SetParameter(ParIndex++, biCategoryId  , "PRM_CATEGORY_ID");
                    stmt.SetParameter(ParIndex++, chgN.images   , "IMG_URLS");
                    stmt.SetParameter(ParIndex++, chgN.defaultImage   , "IMG_URL_DEFAULT");
                    stmt.SetParameter(ParIndex++, chgN.videos   , "VID_URLS");
                    stmt.SetParameter(ParIndex++, pAccId        , "ACCOUNT_ID");
                    stmt.SetParameter(ParIndex++, biItemId      , "UID");
                    stmt.SetParameter(ParIndex++, biVendorId    , "VENDOR_ID");

                    stmt.addBatch();
                }

                int[] AffectedRows = stmt.executeBatch();

                //resetItemsMemory(pem, pUserId, pAccId);//THIS WONT WORK BECAUSE VENDOR ID NEEDED 

                return true;

        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static void resetItemsMemory(EntityManager   pem, 
                                        BigInteger      pUserId, 
                                        BigInteger      pAccId) throws Exception
    {
        try
        {
            // SsDctInvHashtags
            //------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey Col1 = new ssoCacheSplitKey();
            Col1.column = "ACCOUNT_ID";
            Col1.value  = pUserId;
            keys.add(Col1);

            ssoCacheSplitKey Col2 = new ssoCacheSplitKey();
            Col2.column = "USER_ID";
            Col2.value  = pAccId;
            keys.add(Col2);

            pem.flush(SsAccInvItemStats.class, keys);
        }
        catch(Exception e)
        {
            throw e;
        }    
    }

    public static String autoGenerateItemName( EntityManager                    pem,
                                            BigInteger                       pUserId,
                                            BigInteger                       pAccId,
                                            BigInteger                       pItemUID
                                          ) throws Exception
    {
        String sItemName = "";

        try
        {
                String stQuery = "SELECT " +
                                 "FN_EST_GENERATE_ITEM_NAME( IF(IFNULL(CTG.CATEGORY_UI,'')='',CTG.CATEGORY, CTG.CATEGORY_UI), IFNULL(FN_EST_GET_FEATURES(STT.PRM_CATEGORY_ID),'[]'), IFNULL(FN_EST_GET_FEATURES(STT.FEATURES_ID_MANUAL),'[]')) AS ITEM_NAME  " +
                                 "FROM ss_acc_inv_item_stats STT " +
                                 "INNER JOIN ss_dct_inv_categories CTG ON CTG.UID = STT.PRM_CATEGORY_ID " +
                                 "WHERE " +
                                 "STT.UID = ? " + 
                                 "AND " + 
                                 "STT.STAT = 1 " + 
                                 "LIMIT 1";
                
                Query stmt = pem.CreateNativeQuery(stQuery);
                
                int index = 1;
                stmt.SetParameter(index++, pItemUID   , "ITEM_UID");
                List<List<RowColumn>> rs = stmt.getResultList();
                for(int i=0;i<rs.size();i++)
                {
                    ssoEItemRow itemRowN = new ssoEItemRow();

                    sItemName    = Util.Database.getValString(rs.get(i), "ITEM_NAME");

                }

                return sItemName;

        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

    public static String updateEStoreSettings(  EntityManager                    pem,
                                                BigInteger                       pUserId,
                                                BigInteger                       pAccId,
                                                String                           psbSeperateEStore,
                                                String                           psbShowBrandName,
                                                String                           psbResetSearchEngine
                                              ) throws Exception
    {
        String sItemName = "";

        try
        {
                String stQuery = "SP_WEB_UPDATE_ESTORE_SETTINGS";
                
                StoredProcedureQuery SP = pem.createStoredProcedureQuery(stQuery);

                SP.registerStoredProcedureParameter("P_USER_ID"             , BigInteger.class   , ParameterMode.IN);
                SP.registerStoredProcedureParameter("P_ACC_ID"              , BigInteger.class   , ParameterMode.IN);
                SP.registerStoredProcedureParameter("P_B_SEPERATE_ESTORE"   , String.class       , ParameterMode.IN);
                SP.registerStoredProcedureParameter("P_B_SHOW_BRAND_NAME"   , String.class       , ParameterMode.IN);

                int Colindex = 1;
                SP.SetParameter(Colindex++, pUserId               , "P_USER_ID");
                SP.SetParameter(Colindex++, pAccId                , "P_B_NEW_ROWS");
                SP.SetParameter(Colindex++, psbSeperateEStore     , "P_USER_ID");
                SP.SetParameter(Colindex++, psbShowBrandName      , "P_B_NEW_ROWS");

                SP.execute();
                
                if(psbResetSearchEngine.toLowerCase().equals("y")==true)
                {
                    // Write to Search Engine Worker Queue
                    
                }

                return sItemName;

        }
        catch(Exception e)
        {
            throw e;
        }
        
    }

}


