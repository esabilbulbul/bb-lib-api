/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dict;

import bb.app.inv.InventoryParams;
import bb.app.obj.ssoBrand;
import bb.app.obj.ssoInvBrandItemCodes;
import bb.app.obj.ssoMerchant;
import bb.app.obj.ssoPaymentGroup;
import entity.dct.SsDctInvCategories;
import entity.dct.SsDctInvVendorSummary;
import entity.dct.SsDctPymGroups;
import entity.user.SsUsrAccounts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
public final class DictionaryOps 
{
    public static class Vendor
    {
        public static ArrayList<ssoBrand> findMatchedBrands4User(   EntityManager pem, 
                                                                    long          pUserId, 
                                                                    String        pKeyword) throws Exception
        {
            //ArrayList<ssoBrand> brands = new ArrayList<ssoBrand>();
            ArrayList<ssoBrand> brandsFiltered = new ArrayList<ssoBrand>();
            boolean rc = false;
            
            try
            {
                // Get branches / accounts linked to the user (on cache)
                ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
                branches = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);
                for(ssoMerchant branchN: branches)
                {
                    ArrayList<ssoBrand> brandsOfAccount = new ArrayList<ssoBrand>();
                    brandsOfAccount = findMatchedBrands4Account(pem, branchN.Id, pKeyword);

                    for (ssoBrand brandN: brandsOfAccount)
                    {
                        rc = isBrandIdUniqueInList(brandsFiltered, brandN.Id);
                        if (rc==true)
                        {
                            ssoBrand newBrand = new ssoBrand();
                            newBrand.accountId = branchN.Id;
                            newBrand.Id   = brandN.Id;
                            newBrand.name = brandN.name;
                            brandsFiltered.add(newBrand);
                        }
                    }
                    /*
                    if(brandsN.size()>0)
                        brands.addAll(brandsN);
                    */
                }
                        
                return brandsFiltered;
            }
            catch(Exception e)
            {
                throw e;
            }
        }

        public static ArrayList<ssoBrand> findMatchedBrands4Account(EntityManager pem, 
                                                                    long          pAccId, 
                                                                    String        pKeyword) throws Exception
        {
            try
            {
                ArrayList<SsDctInvVendorSummary> aBrands = new ArrayList<SsDctInvVendorSummary>();

                ArrayList<ssoBrand> aBrandsFiltered = new ArrayList<ssoBrand>();
                ArrayList<ssoInvBrandItemCodes> aItemsFound = new ArrayList<ssoInvBrandItemCodes>();

                ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();

                ssoKeyField keyAccId = new ssoKeyField();
                keyAccId.ColumnName = "ACCOUNT_ID";
                keyAccId.Value      = pAccId;
                criterias.add(keyAccId);

                boolean rc = false;

                aBrands = (ArrayList<SsDctInvVendorSummary>)pem.findAll(SsDctInvVendorSummary.class, criterias);

                for(SsDctInvVendorSummary brandN:aBrands)
                {
                    if (brandN.brandName.toLowerCase().indexOf(pKeyword.toLowerCase().trim())==0)
                    {
                        rc = isBrandIdUniqueInList(aBrandsFiltered, brandN.brandId);
                        if (rc==true)
                        {
                            ssoBrand newBrand = new ssoBrand();
                            newBrand.accountId = pAccId;
                            newBrand.Id   = brandN.brandId;
                            newBrand.name = brandN.brandName;
                            aBrandsFiltered.add(newBrand);
                        }
                    }
                }

                return aBrandsFiltered;
            }
            catch(Exception e)
            {
                throw e;
            }
        }

        static boolean isBrandIdUniqueInList(ArrayList<ssoBrand> paList, long pBrandId)
        {
            boolean bFound = false;

            for(ssoBrand brandN:paList)
            {
                if (brandN.Id==pBrandId)
                {
                    //bFound = true;
                    //break;
                    return false;
                }
            }

            return true;
            //if (bFound==false)
            //    paList.add(pNew);
        }

        public static SsDctInvVendorSummary getVendor(EntityManager  pem,
                                                     long            pUserId,
                                                     //long            pAccountId,
                                                     String          pBrand
                                                     ) throws Exception
        {
            try
            {
                SsDctInvVendorSummary NewItemCode = new SsDctInvVendorSummary();

                //NewItemCode.accountId = pAccountId;

                ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();
                ssoKeyField criteria1 = new ssoKeyField();
                criteria1.ColumnName = "USER_ID";
                criteria1.Value      = pUserId;
                criterias.add(criteria1);

                ssoKeyField criteria2 = new ssoKeyField();
                criteria2.ColumnName = "BRAND_NAME";
                criteria2.Value      = pBrand;
                criterias.add(criteria2);

                ssoKeyField criteria4 = new ssoKeyField();
                criteria4.ColumnName = "STAT";
                criteria4.Value      = 1;
                criterias.add(criteria4);

                List<SsDctInvVendorSummary> brands = new ArrayList<SsDctInvVendorSummary>();

                brands = (List<SsDctInvVendorSummary>)pem.findAll(SsDctInvVendorSummary.class, criterias);
                if (brands.size()>0)
                    return brands.get(0);
                else
                    return null;

            }
            catch(Exception e)
            {
                throw e;
            }
        }
        
        public static ssoAPIResponse add_VendorNItemCodes(      EntityManager   pem,
                                                                long            pUserId,
                                                                long            pAccountId,
                                                                String          pBrand,
                                                                String          pItemCode,
                                                                long            pBrandId,
                                                                long            pAccItemId,
                                                                String          pSalesPrice) throws Exception
        {
            long lBrandItemUID = 0;

            ssoAPIResponse rsp = new ssoAPIResponse();

            try
            {
                if(pBrand.trim().length()!=0)
                {
                    SsDctInvVendorSummary NewItemCode = new SsDctInvVendorSummary();

                    //NewItemCode = Vendor.getVendor(pem, pAccountId, pBrand);
                    NewItemCode = Vendor.getVendor(pem, pUserId, pBrand);
                    if(NewItemCode==null)
                    {
                        rsp.ResponseMsg = "new";

                        // New Item 
                        // IMPORTANT:
                        // FIRST TIME REGISTERY ACC_BRANDID and ACC_ITEM_ID
                        // will be empty. Because the accounting information
                        // will not be valid until balance taken place. 
                        // Balance will take place after the first stock /inventory
                        // Entry.
                        //------------------------------------------------------
                        NewItemCode = new SsDctInvVendorSummary();

                        NewItemCode.userId    = pUserId;
                        NewItemCode.accountId = pAccountId;
                        NewItemCode.brandName = pBrand;
                        //NewItemCode.itemCode  = pItemCode;
                        NewItemCode.itemCodeList =  "{" + Util.Str.QUOTE(pItemCode) + ":" + pSalesPrice + "\"}";;//default one
                        NewItemCode.brandId      = pBrandId;

                        lBrandItemUID = pem.persist(NewItemCode);
                    }
                    else
                    {
                        rsp.ResponseMsg = "existing";

                        lBrandItemUID = NewItemCode.uid;

                        // Existing Item -> Update Last Sales Price Info & Item List
                        //-----------------------------------------------------------
                        DictionaryOps.Vendor.add_ItemCode(pem, pUserId, pAccountId, pBrandId, pItemCode, pSalesPrice);
                        // pBrandId
                    }
                }

                rsp.Id = Long.toString(lBrandItemUID);
                rsp.Response = "ok";

                return rsp;
            }
            catch(Exception e)
            {
                throw e;
            }
        }

        public static boolean delete_ItemCode(  EntityManager  pem,
                                                long           pUserId,
                                                long           pAccountId,
                                                long           pBrandId,
                                                String         pItemCode) throws Exception
        {
            try
            {
                // WORKS WITH CACHE

                //String sItemCode = "$." + Util.Str.QUOTE(pItemCode); 

                Query stmtFamily = pem.createNamedQuery("SsDctInvItemCodes.deleteItemCode", SsDctInvVendorSummary.class);

                int ParIndex = 1;
                
                stmtFamily.SetParameter(ParIndex++, pItemCode  , "ITEM_CODE");
                stmtFamily.SetParameter(ParIndex++, pAccountId , "BYUSER");
                stmtFamily.SetParameter(ParIndex++, pUserId    , "USER_ID");
                stmtFamily.SetParameter(ParIndex++, pBrandId   , "BRAND_ID");

                stmtFamily.executeUpdate();

                return true;

            }
            catch(Exception e)
            {
                throw e;
            }
        }
        
        public static void add_ItemCode(    EntityManager   pem,
                                            long            pUserId,
                                            long            pAccountId,
                                            long            pBrandId,
                                            String          pItemCode,
                                            String          pSalesPrice) throws Exception
        {
            try
            {
                Query stmtBrandAcc = pem.createNamedQuery("SsDctInvItemCodes.addItemCode", SsDctInvVendorSummary.class);

                String sNewItemCode = "{" + Util.Str.QUOTE(pItemCode) + ":" + pSalesPrice + "}";
                int index = 1;
                stmtBrandAcc.SetParameter(index++, sNewItemCode     , "ITEM_CODE");// to be fixed
                stmtBrandAcc.SetParameter(index++, pAccountId       , "BYUSER");// WARNING: THIS CODE MUST BE FULLY CHANGED
                stmtBrandAcc.SetParameter(index++, pBrandId         , "BRAND_ID");
                //stmtBrandAcc.SetParameter(index++, pUserId          , "USER_ID");

                stmtBrandAcc.executeUpdate();

                return ;
            }
            catch(Exception e)
            {
                throw e;
            }
        }

        public static boolean delete_Brand( EntityManager  pem,
                                            long           pUserId,
                                            long           pAccountId,
                                            String         psBrandId) throws Exception
        {
            //WORKS WITH CACHE
            try
            {
                Query stmtFamily = pem.createNamedQuery("SsDctInvItemCodes.deleteBrand", SsDctInvVendorSummary.class);

                int index = 1;
                stmtFamily.SetParameter(index++, pAccountId, "BYUSER");
                stmtFamily.SetParameter(index++, pUserId   , "USER_ID");
                stmtFamily.SetParameter(index++, psBrandId , "BRAND_ID");

                stmtFamily.executeUpdate();

                return true;
            }
            catch(Exception e)
            {
                throw e;
            }
        }
        
        // check brand/vendor name
        public static boolean Check( EntityManager   pem,
                                     long            pUserId,
                                     String          pBrand) throws Exception
        {
            SsDctInvVendorSummary NewItemCode = new SsDctInvVendorSummary();

            try
            {
                NewItemCode = DictionaryOps.Vendor.getVendor(pem, pUserId, pBrand);
                if (NewItemCode==null)
                    return false;

                return true;
            }
            catch(Exception e)
            {
                throw e;
            }
        }
    }//end of vendor

    public static class User
    {
        // EMAIL Can be part of a branch account
        // or total new registration
        // An existing email can be used for a new branch
        public static boolean isEmailValid4Registration(  EntityManager pem, 
                                                          long          pUserId,
                                                          String        pEmail) throws Exception
        {
            ArrayList<ssoMerchant> accList = new ArrayList<ssoMerchant>();

            try
            {
                // STEP 1. Check if the same email used for main account (if this is adding new branch op)
                // STEP 2. Thoroughly search if email used for other users
                //------------------------------------------------------------------

                boolean bFound = false;
                if (pUserId!=-1)
                {
                    // STEP 1.
                    //----------------------------------------------------------
                    accList = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);
                    for(int i=0;i<accList.size();i++)
                    {
                        ssoMerchant accN = new ssoMerchant();
                        accN = accList.get(i);
                        
                        if(accN.email.toLowerCase().equals(pEmail.trim().toLowerCase())==true)
                        {
                            // the same email of user will be used for the branch 
                            bFound = true;
                            break;
                        }
                    }
                }

                if (bFound==true)
                    return true;//existing email will be used for the new branch
                
                // STEP 2: THOROUGH SEARCH
                //----------------------------------------------------------
                String stmt =   "SELECT COUNT(1) AS CNT FROM ss_usr_accounts\n" +
                                "WHERE\n" +
                                "STAT = 1\n" +
                                "AND\n" +
                                "EMAIL_CRC32_HASH = CRC32(?) \n" +
                                "AND\n" +
                                "EMAIL = ?";
                Query stmtQry = pem.CreateNativeQuery(stmt);

                int ParIndex = 1;
                stmtQry.SetParameter(ParIndex++, pEmail.trim().toLowerCase() , "EMAIL_CRC32_HASH");
                stmtQry.SetParameter(ParIndex++, pEmail.trim().toLowerCase() , "EMAIL");

                List<List<RowColumn>> rs =  stmtQry.getResultList();
                long lCnt = 0;
                if(rs.size()>0)
                {
                    List<RowColumn> rowN = rs.get(0);
                    
                    lCnt = Long.parseLong(Util.Database.getValString(rowN, "CNT").toString());
                }

                if(lCnt>0)
                    return false;//email used by someother user

                return true;
            }   
            catch(Exception e)
            {
                throw e;
            }
        }

        public static ArrayList<ssoMerchant> getListOfAccounts4User(EntityManager pem, 
                                                                    long pUserId,
                                                                    boolean pbResetCache) throws Exception
        {
            ArrayList<ssoMerchant> mrcList = new ArrayList<ssoMerchant>();

            try
            {
                if(pbResetCache==true)
                    pem.flush();

                Query stmtFamily = pem.createNamedQuery("SsUsrAccounts.findBranches", SsUsrAccounts.class);
                int index = 1;
                stmtFamily.SetParameter(index++, pUserId          , "USER_ID");
                stmtFamily.SetParameter(index++, pUserId          , "USER_ID");

                List<List<RowColumn>> rs = stmtFamily.getResultList();
                for(int i=0; i<rs.size(); i++)
                {
                    List<RowColumn> rowN = rs.get(i);

                    ssoMerchant mrcN = new ssoMerchant();

                    mrcN.isActivated = Util.Database.getValString(rowN, "ACTIVATED").toString();
                    mrcN.name        = Util.Database.getValString(rowN, "PROFILENAME").toString();
                    mrcN.Id          = Long.parseLong(Util.Database.getValString(rowN, "UID").toString());
                    mrcN.isDefault   = Util.Database.getValString(rowN, "ISDEFAULT").toString();

                    mrcN.email          = Util.Database.getValString(rowN, "EMAIL").toString();
                    mrcN.MCC            = Util.Database.getValString(rowN, "MCC").toString();
                    mrcN.MCCName        = Util.Database.getValString(rowN, "MCC_NAME").toString();

                    mrcN.CurrencyCode   = Util.Database.getValString(rowN, "CURRENCY_CODE").toString();
                    mrcN.CurrencyName   = Util.Database.getValString(rowN, "CURRENCY_NAME").toString();

                    mrcN.CountryCode    = Util.Database.getValString(rowN, "COUNTRY_CODE").toString();
                    mrcN.CountryName    = Util.Database.getValString(rowN, "COUNTRY_NAME").toString();

                    mrcN.StateCode      = Util.Database.getValString(rowN, "STATE_CODE").toString();
                    mrcN.StateName      = Util.Database.getValString(rowN, "STATE_NAME").toString();

                    mrcN.CountyCode     = Util.Database.getValString(rowN, "COUNTY_CODE").toString();
                    mrcN.placeNameUID   = Util.Database.getValString(rowN, "PLACE_NAME_UID").toString();
                    mrcN.placeName      = Util.Database.getValString(rowN, "PLACE_NAME").toString();

                    mrcN.isTaxIncluded  = Util.Database.getValString(rowN, "IS_TAX_INC_PRICE").toString();
                    mrcN.taxRate        = Util.Database.getValString(rowN, "TAX_RATE").toString();

                    mrcList.add(mrcN);
                }

                return mrcList;

            }
            catch(Exception e)
            {
                throw e;
            }
        }
        

    }// end of user

    public static class Account
    {
        public static long add_Category(    EntityManager  pem,
                                            long           pAccountId,
                                            long           pUserId,
                                            String         pCategory
                                       ) throws Exception
        {
            // Check if the category added before
            // if not, add new one
            long lCategoryUID = 0;

            try
            {
                if (pCategory.trim().length()!=0)
                {
                    SsDctInvCategories NewCategory = new SsDctInvCategories();

                    NewCategory.accountId = pAccountId;
                    NewCategory.userId    = pUserId;

                    ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();
                    ssoKeyField criteria1 = new ssoKeyField();
                    criteria1.ColumnName = "USER_ID";
                    criteria1.Value      = pUserId;
                    criterias.add(criteria1);

                    ssoKeyField criteria2 = new ssoKeyField();
                    criteria2.ColumnName = "CATEGORY";
                    criteria2.Value      = pCategory.trim().toUpperCase();
                    criterias.add(criteria2);

                    NewCategory = pem.find(SsDctInvCategories.class, criterias);
                    if (NewCategory==null)
                    {
                        NewCategory = new SsDctInvCategories();

                        //add New Category
                        NewCategory.category  = pCategory;
                        NewCategory.accountId = pAccountId;
                        NewCategory.userId    = pUserId;

                        lCategoryUID = pem.persist(NewCategory);
                    }
                    else
                        lCategoryUID = NewCategory.uid;

                }

                return lCategoryUID;
            }
            catch(Exception e)
            {
                throw e;
            }
        }    
        
        public static boolean delete_Category(  EntityManager  pem,
                                                long           pAccountId,
                                                String         pCategory) throws Exception
        {
            //WORKS WITH CACHE

            try
            {

                //deleteTest(pem);

                Query stmtFamily = pem.createNamedQuery("SsDctInvCategories.deleteCategory", SsDctInvCategories.class);

                int index = 1;
                stmtFamily.SetParameter(index++, pAccountId, "ACCOUNT_ID");
                stmtFamily.SetParameter(index++, pCategory , "CATEGORY");

                stmtFamily.executeUpdate();

                return true;

            }
            catch(Exception e)
            {
                throw e;
            }
        }

    }//end of account

    public static long getPaymentGroupIDByName(EntityManager  pem,
                                               long           pUserId,
                                               String         pName) throws Exception
    {
        try
        {
            ArrayList<ssoPaymentGroup> paGroups = new ArrayList<ssoPaymentGroup>();

            paGroups = getAllPaymentGroups(pem, pUserId);
            for(ssoPaymentGroup pymGrpN: paGroups)
            {
                if(pymGrpN.Name.trim().toLowerCase().equals(pName.trim().toLowerCase())==true)
                {
                    return pymGrpN.Id;
                }
            }

            return -1;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoPaymentGroup> getAllPaymentGroups(EntityManager  pem,
                                                                 long           pUserId) throws Exception
    {
        ArrayList<ssoPaymentGroup> paPymGroups = new ArrayList<ssoPaymentGroup>();

        try
        { 
                Query stmtFamily = pem.createNamedQuery("SsDctPymGroups.getPaymentGroups", SsDctPymGroups.class);
                int index = 1;
                stmtFamily.SetParameter(index++, pUserId          , "USER_ID");

                List<List<RowColumn>> rs = stmtFamily.getResultList();
                for(int i=0; i<rs.size(); i++)
                {
                    List<RowColumn> rowN = rs.get(i);

                    ssoPaymentGroup pymN = new ssoPaymentGroup();

                    pymN.Id      = Long.parseLong(Util.Database.getValString(rowN, "UID").toString());
                    pymN.Name    = Util.Str.wordNormalize(Util.Database.getValString(rowN, "PAYMENT_GROUP").toString());

                    if(pymN.Name.trim().length()>0)
                    {
                        paPymGroups.add(pymN);
                    }
                }

                return paPymGroups;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static long addNewPaymentGroup(EntityManager  pem,
                                          long           pUserId,
                                          long           pAccId,
                                          String         pName
                                          ) throws Exception
    {
        try
        {
            SsDctPymGroups newPymGroup = new SsDctPymGroups();

            newPymGroup.userId       = pUserId;
            newPymGroup.accountId    = pAccId;
            newPymGroup.paymentGroup = pName;
            
            long lUID = pem.persist(newPymGroup);
            
            return lUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }


}
