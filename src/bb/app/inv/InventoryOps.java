/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.inv;

import bb.app.obj.ssoInventoryParams;
import bb.app.obj.ssoItemOption;
import bb.app.obj.ssoBrand;
import bb.reports.ssReportSearchInventory;
import bb.app.account.ssoAccInvBalanceCore;
import bb.app.account.AccountMisc;
import bb.app.account.ssoAccStmtCore;
import bb.app.account.ssoUIBalanceItem;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import entity.inv.SsInvAccounting;
import entity.inv.SsInvItemsOptions;
import entity.mrc.SsMrcCashRegEod;
import entity.mrc.SsMrcDataPosTxn;
import entity.mrc.SsMrcMerchants;
import entity.prm.SsPrmCountryStates;
import entity.prm.SsPrmCountryCodes;
import entity.prm.SsPrmCountryPostcodes;
import java.util.ArrayList;
import jaxesa.util.Util;
import bb.app.inv.InventoryOps;
import bb.app.obj.ssoCityCode;
import bb.app.obj.ssoPostCode;
import bb.app.obj.ssoCountryCodes;
import bb.app.obj.ssoCountyCode;
import bb.app.obj.ssoInvBrandItemCodes;
import bb.app.obj.ssoInvCategory;
import bb.app.obj.ssoMCC;
import bb.app.obj.ssoMerchant;
import bb.app.obj.ssoMerchantPreferences;
import bb.app.obj.ssoPageParams;
import bb.app.account.UserOps;
import bb.app.dict.DictionaryOps;
import bb.app.obj.ssoItemOptionGroup;
import bb.app.obj.ssoItemOptionZipped;
import bb.app.obj.ssoPrintByBillData;
import bb.app.bill.ssoBillTotals;
import bb.app.stmt.InventoryStatement;
import bb.app.txn.txnDefs;
import bb.app.vendor.VendorOps;
import entity.acc.SsAccInvVendorStats;
import entity.inv.SsInvItemCodeQuantity;
import entity.inv.SsInvItemOptionsQuantity;
import entity.txn.SsTxnInvBillDets;
import entity.dct.SsDctInvCategories;
import entity.dct.SsDctInvVendorSummary;
import entity.prm.SsPrmMcc;
import entity.seo.SsPrmCatalogFamily;
import entity.test.TestCache;
import entity.user.SsUsrAccounts;
import java.math.BigDecimal;
import java.util.Iterator;
import jaxesa.persistence.ssoKeyField;
import jaxesa.api.callback.ssoCallbackParam;
import jaxesa.webapi.ssoMethodCallback;
import org.json.simple.JSONObject;
import redis.clients.jedis.Jedis;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvVendors;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvItemLog;
import entity.acc.SsAccInvOptionStats;
import entity.acc.SsAccInvOptionLog;
import entity.acc.SsAccInvItemPrice;
import entity.acc.SsAccInvVendorLog;
import entity.txn.SsTxnInvBill;
import java.math.BigInteger;
import java.util.Set;
import jaxesa.annotations.PathParam;
import jaxesa.webapi.ssoAPIResponse;


/**
 *
 * @author Administrator
 */
public final class InventoryOps
{
    public static int gINV_TXN_CACHE_TIMEOUT = 60 * 5;//5min 

    //public static String gTXNTYPE_KEY_NEW_ITEM      = "N";
    //public static String gTXNTYPE_KEY_EXISTING_ITEM = "E";
    //public static String gTXNTYPE_KEY_RETURN        = "R";

    public static long save2Inventory(EntityManager pem,
                                      long           pUserId,
                                      long           pAccId,
                                      String         psLang,
                                      String         psCountry,
                                      String         psSessionId,
                                      String         psTxnType,
                                      String         psBrand,
                                      String         psItemCode,
                                      String         psCategory,
                                      String         psQuantity,
                                      String         psQuantityType,
                                      String         psEntryPrice,
                                      String         psDiscount,
                                      String         psTax,
                                      String         psSalesPrice,
                                      String         psOptions,
                                      String         psBillingDate,
                                      String         psDesc) throws Exception
    {
        Jedis jedis = new Jedis();

        try
        {
            jedis = Util.Redis.getConnection();

            // Check if the same session request coming first. If not, continue with saving process
            boolean rc = isInvEntryExist(jedis, pAccId, psSessionId);
            long lInventoryTxnUID = 0;
            if (rc==false)
            {
                // Save Txn
                /*
                lInventoryTxnUID = saveInventoryTransaction(pem,
                                                            pUserId,
                                                            pAccId,
                                                            psLang,
                                                            psCountry,
                                                            psSessionId,
                                                            psTxnType,
                                                            psBrand,
                                                            psItemCode,
                                                            psCategory,
                                                            psQuantity,
                                                            psQuantityType,
                                                            psEntryPrice,
                                                            psDiscount,
                                                            psTax,
                                                            psSalesPrice,
                                                            psOptions,
                                                            psBillingDate,
                                                            psDesc);
                */
                
                saveTxn2Cache(jedis, pAccId, psSessionId);//in case the txn repeated in next transaction by mistake
            }
            else
            {
                lInventoryTxnUID = -1;//already exist
            }

            //readOptionsInfo(pem, pAccId, psBrand, psItemCode, psOptions);

            //save2InventoryOptions(pem, pAccId, lInventoryAccountingUID, psOptions);

            jedis.close();
            
            return lInventoryTxnUID;
        }
        catch(Exception e)
        {
            if (jedis!=null)
                jedis.close();
            
            throw e;
        }
    }

    public static SsInvItemOptionsQuantity readOptionsInfo( EntityManager pem, 
                                                                long pAccountId, 
                                                                String psBrand,
                                                                String psItemCode,
                                                                String psOption1,
                                                                String psOption2) throws Exception
    {
        SsInvItemOptionsQuantity quantity = new SsInvItemOptionsQuantity();

        try
        {
            ArrayList<ssoKeyField> keys = new ArrayList<ssoKeyField>();

            ssoKeyField KeyAccId = new ssoKeyField();
            KeyAccId.ColumnName = "ACCOUNT_ID";
            KeyAccId.Value      = pAccountId;
            keys.add(KeyAccId);

            ssoKeyField KeyBrand = new ssoKeyField();
            KeyBrand.ColumnName = "BRAND";
            KeyBrand.Value      = psBrand;
            keys.add(KeyBrand);

            ssoKeyField KeyItemCode = new ssoKeyField();
            KeyItemCode.ColumnName = "ITEM_CODE";
            KeyItemCode.Value      = psItemCode;
            keys.add(KeyItemCode);

            if (psOption1.trim().length()>0)
            {
                ssoKeyField KeyOption1 = new ssoKeyField();
                KeyOption1.ColumnName = "OPTION_1";
                KeyOption1.Value      = psItemCode;
                keys.add(KeyOption1);
            }

            if (psOption2.trim().length()>0)
            {
                ssoKeyField KeyOption2 = new ssoKeyField();
                KeyOption2.ColumnName = "OPTION_2";
                KeyOption2.Value      = psItemCode;
                keys.add(KeyOption2);
            }
            
            quantity = pem.find(SsInvItemOptionsQuantity.class, keys);

            return quantity;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static SsInvItemCodeQuantity readItemInfo(EntityManager pem, 
                                                         long pAccountId, 
                                                         String psBrand,
                                                         String psItemCode) throws Exception
    {
        SsInvItemCodeQuantity quantity = new SsInvItemCodeQuantity();

        try
        {
            ArrayList<ssoKeyField> keys = new ArrayList<ssoKeyField>();

            ssoKeyField KeyAccId = new ssoKeyField();
            KeyAccId.ColumnName = "ACCOUNT_ID";
            KeyAccId.Value      = pAccountId;
            keys.add(KeyAccId);

            ssoKeyField KeyBrand = new ssoKeyField();
            KeyBrand.ColumnName = "BRAND";
            KeyBrand.Value      = psBrand;
            keys.add(KeyBrand);

            ssoKeyField KeyItemCode = new ssoKeyField();
            KeyItemCode.ColumnName = "ITEM_CODE";
            KeyItemCode.Value      = psItemCode;
            keys.add(KeyItemCode);

            quantity = pem.find(SsInvItemCodeQuantity.class, keys);

            return quantity;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void save2InventoryOptions(   EntityManager  pem, 
                                                long           pAccId, 
                                                long           pItemTxnUID,
                                                String         psOptions) throws Exception
    {
        try
        {

            JSONObject jsoGroups = Util.JSON.parseJSON(psOptions);
            for(Iterator iterator = jsoGroups.keySet().iterator(); iterator.hasNext();)
            {
                String sGroupName = (String)iterator.next();
                String sGroupOpts = (String)jsoGroups.get(sGroupName).toString();
                
                JSONObject jsoOptions = Util.JSON.parseJSON(sGroupOpts);
                for(Iterator iterator2 = jsoOptions.keySet().iterator(); iterator2.hasNext();)
                {
                    String sOptName     = (String)iterator2.next();
                    String sOptQuantity = (String)jsoOptions.get(sOptName).toString();

                    SsInvItemsOptions invOpts = new SsInvItemsOptions();
                    invOpts.itemTxnId = pItemTxnUID;
                    invOpts.itemBaseId= -1;//this will always be -1 for shipshuk.biz. This will be filled by shipshuk.com
                    invOpts.option1   = sGroupName;
                    invOpts.option2   = sOptName;
                    invOpts.dets      = "{}";
                    invOpts.quantity  = Integer.parseInt(sOptQuantity);
                    
                    pem.persist(invOpts);
                }
            }

        }
        catch(Exception e)
        {
            throw e;
        }
    }


    // example = {"Group 1":{"green":18,"blue":-1}}
    public static ArrayList<ssoItemOption> getItemOptions(String psOptions)
    {
        ArrayList<ssoItemOption> options = new ArrayList<ssoItemOption>();
        
        int iTotal = 0;

        JsonObject jsOptions = Util.JSON.toJsonObject(psOptions);

        Set<String> groups = jsOptions.keySet();
        int iGroupNumber = groups.size();
        Iterator<String> grpIterator = groups.iterator();
        while(grpIterator.hasNext()==true)
        {

            String groupN = grpIterator.next();
            String sGroupName2BeUsed = groupN.trim().toLowerCase();
            if(iGroupNumber==1)
            {
                //determining if really group naming used
                if (groupN.toLowerCase().indexOf("group ")==0)
                    sGroupName2BeUsed = "";//default group name used and only one group means no need to use group naming
            }

            if(((JsonObject)jsOptions.get(groupN)).keySet().size()!=0)
            {
                Set<String> Options = ((JsonObject)jsOptions.get(groupN)).keySet();

                Iterator<String> optIterator = Options.iterator();
                while(optIterator.hasNext()==true)
                {
                    String optionN = optIterator.next();

                    String sOptionQuantity = ((JsonObject)jsOptions.get(groupN)).get(optionN).toString();

                    ssoItemOption opt = new ssoItemOption();
                    opt.groupName     = sGroupName2BeUsed;
                    opt.optionName    = optionN;
                    opt.quantity      = sOptionQuantity;
                    options.add(opt);

                }
            }
            
        }
        
        return options;

    }

    //it only stores there for 2 min. This is to make sure the transaction is not repeated for some malfunction 
    public static void saveTxn2Cache(Jedis          pJedis,
                                        long           pAccId,
                                        String         pSessionId) throws Exception
    {

        try
        {
            String sTxnKey = pAccId + '-' + pSessionId;
            
            Util.Redis.JString.set(pJedis, sTxnKey, "true", gINV_TXN_CACHE_TIMEOUT);
        }
        catch(Exception e)
        {
            throw e;
        }
    }
            

    //This checks the transaction if repeating request
    public static boolean isInvEntryExist(Jedis          pJedis,
                                          long           pAccId,
                                          String         pSessionId) throws Exception
    {
        int lCount = 0;

        try
        {
            String sTxnKey = pAccId + '-' + pSessionId;

            String sVal = Util.Redis.JString.get(pJedis, sTxnKey);

            if (sVal!=null)
            {
                return true;
            }

            return false;
            /*
            String sQuery = "SELECT COUNT(1) CNT FROM ss_inv_transactions WHERE STAT = 1 AND ACCOUNT_ID = ? AND UI_SESSIONID = ?";

            Query stmtQry = pem.CreateNativeQuery(sQuery);

            int ParIndex = 1;
            stmtQry.SetParameter(ParIndex++, pAccId     , "P_ACCOUNT_ID");
            stmtQry.SetParameter(ParIndex++, pSessionId , "P_SESSION_ID");

            List<List<RowColumn>> rs =  stmtQry.getResultList();

            if (rs.size()>0)
            {
                List<RowColumn> RowN = rs.get(0);

                lCount     = Integer.parseInt(Util.Database.getValString(RowN, "CNT"));
            }
            
            if (lCount>0)
                return true;
            else
                return false;//entry not exist
            */
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    /*
    PRINCIPLE: 
        Options table differs from brand and items table. The options may work with quantity 
        or without it. If the user didn’t select a quantity for the option then shipshuk will 
        ignore affecting the options quantity even though the previous entries for the same 
        options came with a given quantities. It basically dismisses the entry. 
    */
    public static boolean updateItemOptionsStats(    EntityManager            pem,
                                                     long                     pBillId,
                                                     SsAccInvItemStats        pBrandItem,
                                                     ArrayList<ssoItemOption> pOptions,
                                                     String                   pTxnType,
                                                     String                   pQuantity,
                                                     long                     pPrmCategoryId
                                                     ) throws Exception
    {
        SsAccInvOptionStats itemOption = new SsAccInvOptionStats();
        
        try
        {
            ArrayList<ssoItemOption> aOptions = new ArrayList<ssoItemOption>();
            boolean bNewItemOption = false;

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            // read option quantity
            // update it
            // add log 
            

            if(pOptions.size()==0)// NO OPTION ENTERED
            {
                //entry without options so then run it once for empty
                ssoItemOption noOption = new ssoItemOption();
                noOption.optionName = "";
                noOption.groupName  = "";
                //if ( (pTxnType.equals(InventoryStatement.INV_NEW_ITEM)==true) || 
                //     (pTxnType.equals(InventoryStatement.INV_EXISTING_ITEM)==true) )
                if ( (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true) || 
                     (pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true) )
                {
                    noOption.quantity   = pQuantity;//Integer.toString(pBrandItem.quantityEntered);
                }
                //else if (pTxnType.equals(InventoryStatement.INV_RETURN_ITEM)==true)
                else if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
                {
                    noOption.quantity   = pQuantity;//Integer.toString(pBrandItem.quantityReturned);
                }

                aOptions.add(noOption);
            }
            else
            {
                aOptions.addAll(pOptions);
            }

            //-----------------------------------------------------------------
            // COLLECT GROUP NAMES WITH OPTIONS
            //-----------------------------------------------------------------
            ArrayList<String> optGroups = new ArrayList<String>();
            ArrayList<ssoItemOptionZipped> optGroupsZipped = new ArrayList<ssoItemOptionZipped>();

            // Distincted
            optGroups = filterGroups(aOptions);
            for(String groupN: optGroups)
            {
                String sGroupJSON = prepareGroupOptionsasJSON(groupN, aOptions);

                ssoItemOptionZipped optGroupZip = new ssoItemOptionZipped();
                optGroupZip.groupName = groupN;
                optGroupZip.options = sGroupJSON;
                optGroupsZipped.add(optGroupZip);
            }

            //-----------------------------------------------------------------
            // SAVE 2 DB 
            //-----------------------------------------------------------------
            for(String groupN: optGroups)
            {
                groupN = groupN.toLowerCase();

                Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.findByItemCodeOptions", SsAccInvOptionStats.class);

                int index = 1;
                stmtFamily.SetParameter(index++, pBrandItem.accountId       , "ACCOUNT_ID");
                stmtFamily.SetParameter(index++, pBrandItem.vendorId        , "VENDOR_ID");
                stmtFamily.SetParameter(index++, pBrandItem.uid             , "ITEM_CODE_ID");//uid
                stmtFamily.SetParameter(index++, groupN.toLowerCase().trim(), "OPTION_GROUP");//uid

                List<SsAccInvOptionStats> rs = stmtFamily.getResultList(SsAccInvOptionStats.class);

                if(rs.size()>0)
                {
                    itemOption = rs.get(0);

                    bNewItemOption = false;
                }
                else
                {
                    bNewItemOption = true;
                }

                String sOptions = getGroupOptionsasJSON(groupN, optGroupsZipped);
                if (bNewItemOption == true)
                {
                    //----------------------------------------------------------
                    //  first time - new record
                    //----------------------------------------------------------
                    bNewItemOption = true;

                    itemOption.accountId        = pBrandItem.accountId;
                    itemOption.vendorId         = pBrandItem.vendorId;
                    itemOption.itemCodeId       = pBrandItem.uid;//uid == itemcode
                    itemOption.prmCategoryId    = pPrmCategoryId;
                    itemOption.optionGroup      = groupN;
                    itemOption.itemOptGroupHashMd5 = Util.crypto.md5.calculate(groupN.trim() + Long.toString(itemOption.itemCodeId).trim());

                    //if ( (pTxnType.equals(InventoryStatement.INV_NEW_ITEM)==true) || 
                    //     (pTxnType.equals(InventoryStatement.INV_EXISTING_ITEM)==true) )
                    if ( (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true) || 
                         (pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true) )
                    {
                        // NEW ENTRY or EXISTING ITEM
                        itemOption.optionsEntered  = sOptions;
                        itemOption.optionsReturned = "{}";
                        itemOption.optionsSold     = "{}";
                        itemOption.optionsRefund   = "{}";
                        
                        itemOption.optionsAdjMinus = "{}";
                        itemOption.optionsAdjPlus  = "{}";
                        
                        itemOption.optionsRevolvingEntered  = "{}";
                        itemOption.optionsRevolvingReturned = "{}";
                        itemOption.optionsRevolvingSold     = "{}";
                        itemOption.optionsRevolvingRefund   = "{}";
                        
                        itemOption.optionsRevolvingAdjMinus = "{}";
                        itemOption.optionsRevolvingAdjPlus  = "{}";

                    }
                    //else if (pTxnType.equals(InventoryStatement.INV_RETURN_ITEM)==true)
                    else if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
                    {
                        // RETURNING ITEM 
                        itemOption.optionsEntered   = "{}";
                        itemOption.optionsReturned  = sOptions;
                        itemOption.optionsReturned = "{}";
                        itemOption.optionsSold     = "{}";
                        itemOption.optionsRefund   = "{}";
                        itemOption.optionsAdjMinus = "{}";
                        itemOption.optionsAdjPlus  = "{}";

                        itemOption.optionsRevolvingEntered  = "{}";
                        itemOption.optionsRevolvingReturned = "{}";
                        itemOption.optionsRevolvingSold     = "{}";
                        itemOption.optionsRevolvingRefund   = "{}";
                        itemOption.optionsRevolvingAdjMinus = "{}";
                        itemOption.optionsRevolvingAdjPlus  = "{}";

                    }

                    pem.persist(itemOption);
                }
                else
                {
                    // UPDATE STATS
                    updateOptionStats(  pem, 
                                        pBrandItem.accountId, 
                                        pBrandItem.vendorId, 
                                        pBrandItem.uid,
                                        groupN,
                                        sOptions,
                                        pTxnType
                                        );
                    
                }

                // update here (THIS IS IF NOT A NEW ITEM -> IF NEW ITEM IT IS ADDED IN THE LINES ABOVE)
                //commit2DB_ItemOptionQuantity(pem, itemOption);
            }

            
            /*
            for(ssoItemOption optN:aOptions)
            {

                Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.findByAccIdNBrandIdNItemCodeNOptions", SsAccInvOptionStats.class);

                int index = 1;
                stmtFamily.SetParameter(index++, pBrandItem.accountId      , "ACCOUNT_ID");
                stmtFamily.SetParameter(index++, pBrandItem.vendorId       , "BRAND_ID");
                stmtFamily.SetParameter(index++, pBrandItem.uid            , "ITEM_CODE");//uid
                stmtFamily.SetParameter(index++, optN.groupName            , "OPTION_1");
                stmtFamily.SetParameter(index++, optN.optionName           , "OPTION_2");
                stmtFamily.SetParameter(index++, ThisYear                  , "FINANCIAL_YEAR");

                List<SsAccInvOptionStats> rs = stmtFamily.getResultList(SsAccInvOptionStats.class);

                if(rs.size()>0)
                {
                    itemOption = rs.get(0);

                    bNewItemOption = false;
                }
                else
                {
                    bNewItemOption = true;

                    itemOption.accountId = pBrandItem.accountId;
                    itemOption.vendorId   = pBrandItem.vendorId;
                    itemOption.itemCodeId= pBrandItem.uid;//uid == itemcode
                    itemOption.prmCategoryId  = pPrmCategoryId;
                    itemOption.optionGroup   = optN.groupName;
                    itemOption.option2   = optN.optionName;

                    if ((pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true) || (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true))
                    {
                        itemOption.quantityEntered  = Integer.parseInt(optN.quantity);
                    }

                    if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
                        itemOption.quantityReturned  = Integer.parseInt(optN.quantity);

                    itemOption.quantitySold     = 0;
                    itemOption.financialYear    = ThisYear;

                    pem.persist(itemOption);
                }
                
                if (bNewItemOption!=true)
                {
                    int iQuantity = Integer.parseInt(optN.quantity);
                    if (iQuantity!=-1)
                    {
                        // -1 means ignore quantity adding / subtracting
                        if ((pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true) || (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true))
                        {
                            // PREPARING ITEM
                            //--------------------------------------------------
                            if (itemOption.quantityEntered==-1)
                                itemOption.quantityEntered = iQuantity;
                            else
                                itemOption.quantityEntered += iQuantity;
                        }
                        else if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
                        {
                            // PREPARING ITEM
                            //--------------------------------------------------
                            if (itemOption.quantityReturned==-1)
                                itemOption.quantityReturned = iQuantity;
                            else
                                itemOption.quantityReturned += iQuantity;

                        }

                        // update here (THIS IS IF NOT A NEW ITEM -> IF NEW ITEM IT IS ADDED IN THE LINES ABOVE)
                        commit2DB_ItemOptionQuantity(pem, itemOption);

                    }

                }

            }// end of for
            */
            
            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static String getGroupOptionsasJSON(String pGroupName, ArrayList<ssoItemOptionZipped> paGroupOptsZipped)
    {
        for(ssoItemOptionZipped groupN: paGroupOptsZipped)
        {
            if (groupN.groupName.equals(pGroupName)==true)
            {
                return groupN.options;
            }
        }
        
        return "{}";
    }

    public static ArrayList<String> filterGroups(ArrayList<ssoItemOption> paOptions)
    {
        ArrayList<String> optGroups = new ArrayList<String> ();
        boolean bFound = true;

        for (ssoItemOption optN: paOptions)
        {
            bFound = false;
            //Check if found
            for(String groupN: optGroups)
            {
                if (groupN.equals(optN.groupName)==true)
                {
                    bFound = true;
                    break;
                }
            }

            if (bFound==false)
            {
                optGroups.add(optN.groupName);
            }
        }
        
        return optGroups;
    }

    public static String prepareGroupOptionsasJSON(String pGroup, ArrayList<ssoItemOption> paOptions)
    {
        ArrayList<ssoItemOption> groupOpts = new ArrayList<ssoItemOption>();

        for(ssoItemOption optionN: paOptions)
        {
            if(optionN.groupName.equals(pGroup)==true)
            {
                groupOpts.add(optionN);
            }
        }

        String sOptJSON = "{";
        int i =0;
        for(ssoItemOption optionN: groupOpts)
        {
            if (i>0)
                sOptJSON += ",";

            sOptJSON += "\"" + optionN.optionName + "\"" + ":" + Util.Str.ifEmpty(optionN.quantity,"-1");
            i++;
        }
        sOptJSON += "}";

        return sOptJSON;
    }

    public static long addItemPrice(EntityManager pem,
                                    long          pBillId,
                                    long          pAccId,
                                    long          pBrandId,
                                    String        pItemCode,
                                    String        pEntryPrice,
                                    String        pDiscount,
                                    String        pTax,
                                    String        pSalesPrice) throws Exception
    {
        SsAccInvItemPrice itemPrice = new SsAccInvItemPrice();

        try
        {
            itemPrice.accountId  = pAccId;
            itemPrice.billId     = pBillId;
            itemPrice.brandId    = pBrandId;
            itemPrice.itemCode   = pItemCode;
            itemPrice.entryPrice = new BigDecimal(pEntryPrice);
            itemPrice.discount   = new BigDecimal(pDiscount);
            itemPrice.tax        = new BigDecimal(pTax);
            itemPrice.salePrice  = new BigDecimal(pSalesPrice);
            
            long lPriceId = pem.persist(itemPrice);
            
            return lPriceId;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void updateOptionStats(EntityManager  pem,
                                         long           pAccId,
                                         long           pVendorId,
                                         long           pItemCodeId,
                                         String         pOptGroup,
                                         String         pOptions,
                                         String         pTxnType
                                         ) throws Exception
    {
        try
        {
            String sOptions = pOptions;
            
            if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true)
            {
                sOptions = "{" + Util.Str.QUOTE("received") + ":" + "{" + Util.Str.QUOTE(pOptGroup) + ":" + pOptions + "}" + "}";
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                sOptions = "{" + Util.Str.QUOTE("returned") + ":" + "{" + Util.Str.QUOTE(pOptGroup) + ":" + pOptions + "}" + "}";
            }

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_UPDATE_OPTION_N_STATS");

            SP.registerStoredProcedureParameter("ACCOUNT_ID"    , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("VENDOR_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("ITEM_CODE_ID"  , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("GROUP"         , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("OPTIONS"       , String.class     , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("TXN_TYPE"      , String.class     , ParameterMode.IN);

            int index = 1;
            SP.SetParameter(index++, pAccId         , "ACCOUNT_ID");
            SP.SetParameter(index++, pVendorId      , "VENDOR_ID");
            SP.SetParameter(index++, pItemCodeId    , "ITEM_CODE_ID");
            SP.SetParameter(index++, pOptGroup      , "GROUP");
            SP.SetParameter(index++, sOptions       , "OPTIONS");
            //SP.SetParameter(index++, pTxnType       , "TXN_TYPE");

            SP.execute();

            return ;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    /*
    public static void commit2DB_ItemOptionQuantity(EntityManager               pem,
                                                    SsAccInvOptionStats    pItemOption) throws Exception
    {
        try
        {
            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.updateStats", SsAccInvOptionStats.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pItemOption.optionsEntered     , "OPTIONS_ENTERED");
            stmtFamily.SetParameter(index++, pItemOption.optionsReturned    , "OPTIONS_RETURNED");
            stmtFamily.SetParameter(index++, pItemOption.optionsSold        , "OPTIONS_SOLD");
            stmtFamily.SetParameter(index++, pItemOption.accountId          , "BYUSER");
            stmtFamily.SetParameter(index++, pItemOption.uid                , "UID");

            stmtFamily.executeUpdate();
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    */
    /* 
    public static SsAccInvItemStats updateItemStats(EntityManager   pem,
                                                    long            pBillId,
                                                    String          pTxnType,
                                                    long            pAccountId, 
                                                    long            pBrandId,
                                                    String          pItemCode,
                                                    String          pQuantity,
                                                    long            pPrmCategoryId,
                                                    long            pPriceId,
                                                    String          pSalesPrice) throws Exception
    {
        try
        {
            boolean bNewBrandItem = false;
            int iOldQuantity = 0;

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            // 1. get curret quantity
            // 2. update it
            // 3. add log
            SsAccInvItemStats brndItem = new SsAccInvItemStats();

            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemCodes.findByAccIdNBrandNItemCode", SsAccInvItemStats.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pAccountId, "ACCOUNT_ID");
            stmtFamily.SetParameter(index++, pBrandId  , "VENDOR_ID");
            stmtFamily.SetParameter(index++, pItemCode , "ITEM_CODE");
            //stmtFamily.SetParameter(index++, ThisYear  , "FINANCIAL_YEAR");

            List<SsAccInvItemStats> rs = stmtFamily.getResultList(SsAccInvItemStats.class);

            if(rs.size()>0)
            {
                brndItem = rs.get(0);

                bNewBrandItem = false;
            }
            else
            {
                bNewBrandItem = true;

                // insert new with 0 quantity
                brndItem.accountId     = pAccountId;
                brndItem.vendorId      = pBrandId;
                brndItem.itemCode      = pItemCode;
                brndItem.prmCategoryId = pPrmCategoryId;
                brndItem.priceId       = pPriceId;
                brndItem.financialYear = ThisYear;
                brndItem.lastSalePrice = new BigDecimal(pSalesPrice);

                brndItem.quantitySold = new BigDecimal(0);
                if ((pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true) || (pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true))
                    brndItem.quantityEntered  = new BigDecimal(Integer.parseInt(pQuantity));
                else if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
                    brndItem.quantityReturned = new BigDecimal(Integer.parseInt(pQuantity));
                
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // CREATING NEW BRAND ITEM 
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                brndItem.uid = pem.persist(brndItem);
            }
            brndItem.priceId = pPriceId;//update the price info always

            if ((pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true) || (pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true))
            {
                if (bNewBrandItem!=true)
                {
                    brndItem.quantityEntered = brndItem.quantityEntered.add(new BigDecimal(Integer.parseInt(pQuantity)));
                    //brndItem.quantityEntered += new BigDecimal(Integer.parseInt(pQuantity));
                }
            }
            else if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                if (bNewBrandItem!=true)
                {
                    brndItem.quantityReturned = brndItem.quantityReturned.add(new BigDecimal(Integer.parseInt(pQuantity)));
                    //brndItem.quantityReturned += new BigDecimal(Integer.parseInt(pQuantity));
                }
            }

            brndItem.lastSalePrice = new BigDecimal(pSalesPrice);

            if (bNewBrandItem!=true)
            {
                // update ITEM
                commit2DB_BrandItemInfo(pem, brndItem);
            }

            return brndItem;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    */

    public static void commit2DB_BrandItemInfo(EntityManager          pem, 
                                               SsAccInvItemStats pBrandItem) throws Exception
    {
        try
        {
            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemCodes.updateItem", SsAccInvItemStats.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pBrandItem.quantityEntered             , "QUANTITY_ENTERED");
            stmtFamily.SetParameter(index++, pBrandItem.quantityReturned            , "QUANTITY_RETURNED");
            stmtFamily.SetParameter(index++, pBrandItem.priceId                     , "PRICE_ID");
            stmtFamily.SetParameter(index++, pBrandItem.lastSalePrice               , "LAST_SALE_PRICE");
            stmtFamily.SetParameter(index++, pBrandItem.accountId                   , "BYUSER");
            stmtFamily.SetParameter(index++, pBrandItem.uid                         , "UID");

            //stmtFamily.SetParameter(index++, pBrandItem.accountId                   , "ACCOUNT_ID");
            //stmtFamily.SetParameter(index++, pBrandItem.brandId                     , "BRAND_ID");
            //stmtFamily.SetParameter(index++, pBrandItem.itemCode                    , "ITEM_CODE");

            stmtFamily.executeUpdate();

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static SsAccInvVendorStats updateBrandBalance(   EntityManager       pem,
                                                            long                pTxnId,
                                                            String              pTxnType,                                                            
                                                            long                pAccountId,
                                                            long                pBrandId,
                                                            ssoBillTotals  pStmtTotals
                                                            ) throws Exception
    {
        boolean bNewStatsRecord4Branch = false;

        try
        {
            pem.flush();
            
            boolean bBrandAccExist = true;
            SsAccInvVendorStats brandAcc = new SsAccInvVendorStats();
            //SsAccInvBrandsLog brandLog = new SsAccInvBrandsLog();

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemCodes.findVendorStats", SsAccInvItemStats.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pAccountId, "ACCOUNT_ID");
            stmtFamily.SetParameter(index++, pBrandId  , "VENDOR_ID");
            //stmtFamily.SetParameter(index++, ThisYear  , "FINANCIAL_YEAR");

            //List<SsAccInvVendorStats> rs = stmtFamily.getResultList(SsAccInvVendorStats.class);
            /*
            if(rs.size()>0)
            {
                brandAcc = rs.get(0);
            }
            else
            {
                // If falls here, it means brand has defined but the stats are not created for vendor for this BRANCH
                bNewStatsRecord4Branch = true;
            }
            */

            List<List<RowColumn>> rs =  stmtFamily.getResultList();

            if (rs.size()>0)
            {
                List<RowColumn> RowN = rs.get(0);

                brandAcc.quantityEntered        = new BigDecimal(Util.Database.getValString(RowN, "QUANTITY_ENTERED"));
                brandAcc.netTotalEntered        = new BigDecimal(Util.Database.getValString(RowN, "NET_TOTAL_ENTERED"));
                brandAcc.discountTotalEntered   = new BigDecimal(Util.Database.getValString(RowN, "DISCOUNT_TOTAL_ENTERED"));
                brandAcc.surchargeTotalEntered  = new BigDecimal(Util.Database.getValString(RowN, "SURCHARGE_TOTAL_ENTERED"));
                brandAcc.taxTotalEntered        = new BigDecimal(Util.Database.getValString(RowN, "TAX_TOTAL_ENTERED"));
                brandAcc.grossTotalEntered      = new BigDecimal(Util.Database.getValString(RowN, "GROSS_TOTAL_ENTERED"));
                
                brandAcc.quantityReturned        = new BigDecimal(Util.Database.getValString(RowN, "QUANTITY_RETURNED"));
                brandAcc.netTotalReturned        = new BigDecimal(Util.Database.getValString(RowN, "NET_TOTAL_RETURNED"));
                brandAcc.discountTotalReturned   = new BigDecimal(Util.Database.getValString(RowN, "DISCOUNT_TOTAL_RETURNED"));
                brandAcc.surchargeTotalReturned  = new BigDecimal(Util.Database.getValString(RowN, "SURCHARGE_TOTAL_RETURNED"));
                brandAcc.taxTotalReturned        = new BigDecimal(Util.Database.getValString(RowN, "TAX_TOTAL_RETURNED"));
                brandAcc.grossTotalReturned      = new BigDecimal(Util.Database.getValString(RowN, "GROSS_TOTAL_RETURNED"));

            }

            brandAcc.vendorId   = pBrandId;
            brandAcc.accountId  = pAccountId;

            if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true)
            {
                brandAcc.quantityEntered        = brandAcc.quantityEntered.add(pStmtTotals.Summary.totalQuantity);
                //brandAcc.grossTotalEntered      = brandAcc.grossTotalEntered.add(pStmtTotals.Lines.totalGross);
                brandAcc.netTotalEntered        = brandAcc.netTotalEntered.add(pStmtTotals.Lines.totalNet);
                brandAcc.discountTotalEntered   = brandAcc.discountTotalEntered.add(pStmtTotals.Summary.totalDiscount);
                brandAcc.surchargeTotalEntered  = brandAcc.surchargeTotalEntered.add(pStmtTotals.Summary.totalSurcharge);
                brandAcc.taxTotalEntered        = brandAcc.taxTotalEntered.add(pStmtTotals.Bottom.totalTax);
                brandAcc.grossTotalEntered      = brandAcc.grossTotalEntered.add(pStmtTotals.Bottom.totalGross);

                //brandAcc.balanceFinAdjPlus      = new BigDecimal(BigInteger.ZERO);
                //brandAcc.balanceFinAdjMinus     = new BigDecimal(BigInteger.ZERO);
                
                // balance is auto-generated 22.03.2024
                //brandAcc.balance                = brandAcc.balance.add(pStmtTotals.Bottom.totalNet);
                //brandAcc.cumulativeNetTotalEntered   = brandAcc.revolvingNetTotalEntered.add(brandAcc.netTotalEntered);
            }
            else if (pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true)
            {
                // THIS WON'T AFFECT ANYTHING OTHER THAN QUANTITIES
                //--------------------------------------------------------
                // add up only to quantity 
                brandAcc.quantityEntered    = brandAcc.quantityEntered.add(pStmtTotals.Summary.totalQuantity);
                //brandAcc.quantityReturned   = new BigDecimal(BigInteger.ZERO);
                //brandAcc.quantitySold       = new BigDecimal(BigInteger.ZERO);
            }
            else if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                // deduct balance and quantity
                brandAcc.quantityReturned       = brandAcc.quantityReturned.add(pStmtTotals.Summary.totalQuantity);
                //brandAcc.grossTotalReturned     = brandAcc.grossTotalReturned.add(pStmtTotals.Lines.totalGross);
                brandAcc.netTotalReturned       = brandAcc.netTotalReturned.add(pStmtTotals.Lines.totalNet);
                brandAcc.discountTotalReturned  = brandAcc.discountTotalReturned.add(pStmtTotals.Summary.totalDiscount);
                brandAcc.surchargeTotalReturned = brandAcc.surchargeTotalReturned.add(pStmtTotals.Summary.totalSurcharge);
                brandAcc.taxTotalReturned       = brandAcc.taxTotalReturned.add(pStmtTotals.Bottom.totalTax);
                
                brandAcc.grossTotalReturned       = brandAcc.grossTotalReturned.add(pStmtTotals.Bottom.totalGross);

                //brandAcc.balanceFinAdjPlus      = new BigDecimal(BigInteger.ZERO);
                //brandAcc.balanceFinAdjMinus     = new BigDecimal(BigInteger.ZERO);

                // balance is auto-generated
                //brandAcc.balance                = brandAcc.balance.subtract(pStmtTotals.Bottom.totalNet);
                //brandAcc.cumulativeReturned     = brandAcc.cumulativeReturned.add(brandAcc.netTotalReturned);
                //brandAcc.cumulativeTotalReturned  = brandAcc.revolvingNetTotalReturned.add(brandAcc.netTotalReturned);
            }

            if (bNewStatsRecord4Branch==true)
                pem.persist(brandAcc);
            else
                VendorOps.update_VendorStats(pem, pTxnType, brandAcc);

            return brandAcc;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static void readTest(EntityManager pem)
    {

        try
        {

            // TEST 1
            //
            // em.find() should create cache in memory on redis
            // cache name = entity.prm.TestCache
            //-----------------------------------------------------------------
            TestCache tst = new TestCache();
            tst = pem.find(TestCache.class, 1);

            // TEST 2
            //
            // Named Query (TestCache.findSome) should create cache on  redis 
            // cache name = entity.prm.NMQ.TestCache.findSome
            //-----------------------------------------------------------------
            Query stmtFamily;
            stmtFamily = pem.createNamedQuery("TestCache.findSome", TestCache.class);

            int Colindex = 1;
            stmtFamily.SetParameter(Colindex++, "1" , "CODE1");
            stmtFamily.SetParameter(Colindex++, "2" , "CODE2");

            List<List<RowColumn>> rs = stmtFamily.getResultList();

        }
        catch(Exception e)
        {
            String s = "";
        }
    }

    public static void deleteTest(EntityManager pem)
    {
        try
        {
            // TEST 1
            //
            // em.remove() should delete the cache "entity.prm.TestCache" from memory
            //-----------------------------------------------------------------
            TestCache tst = new TestCache();
            tst.uid = 1L;
            int i = pem.remove(tst);

            // TEST 2
            //
            // em.remove() should delete the cache "entity.prm.TestCache" from memory
            //-----------------------------------------------------------------
            Query stmtFamily = pem.createNamedQuery("TestCache.deleteSome", TestCache.class);

            int index = 1;
            stmtFamily.SetParameter(index++, "1" , "CODE1");
            stmtFamily.SetParameter(index++, "2" , "CODE2");

            stmtFamily.executeUpdate();

        }
        catch(Exception e)
        {
            
        }

    }


    public static ssoInventoryParams getItemLastEntryInfo(EntityManager pem, long pMrcId, String pItemCode) throws Exception
    {
        ssoInventoryParams info = new ssoInventoryParams();

        try
        {
            String sQuery = "SELECT UID, ITEM_CATEGORY, PRICE_ENTRY, DISCOUNT, TAX FROM ss_inv_accounting " + 
                            "WHERE " + 
                            "ACCOUNT_ID = ? AND " + 
                            "ITEM_CODE = ?  " +
                            "ORDER BY UID DESC " +
                            "LIMIT 1";

            Query stmtQry = pem.CreateNativeQuery(sQuery);

            int ParIndex = 1;
            stmtQry.SetParameter(ParIndex++, pMrcId     , "P_ACCOUNT_ID");
            stmtQry.SetParameter(ParIndex++, pItemCode  , "P_ITEM_CODE");

            List<List<RowColumn>> rs =  stmtQry.getResultList();

            if (rs.size()>0)
            {
                List<RowColumn> RowN = rs.get(0);

                info.itemCode   = pItemCode;
                info.uid        = Integer.parseInt(Util.Database.getValString(RowN, "UID"));
                info.tax        = Util.Database.getValString(RowN, "TAX").toString();
                info.discount   = Util.Database.getValString(RowN, "DISCOUNT").toString();
                info.entryPrice = Util.Database.getValString(RowN, "PRICE_ENTRY").toString();
                info.category   = Util.Database.getValString(RowN, "ITEM_CATEGORY").toString();

            }
            
            return info;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoPrintByBillData> getPrintDataByBill( EntityManager   pem,
                                                                    long            pUserId,
                                                                    long            pAccId,
                                                                    String          pKeyword,
                                                                    boolean         pbExactMatch,
                                                                    boolean         bReset) throws Exception 
    {
        ArrayList<ssoPrintByBillData> printData = new ArrayList<ssoPrintByBillData>();
        Jedis  jedis = new Jedis();
        
        try
        {
            // This methods work cache oriented.
            // If data found on cache, continues from there
            // If not, fetch the data and restore on cache.
            // This methods works cordinated with new inventory bill method.
            // In case of new entry, the cache will be reset by that method
            //
            // Last 100 entry is stored 
            if(bReset==true)
                pem.flush();

            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            aBrands = DictionaryOps.Vendor.findMatchedBrands4Account(pem, pAccId, pKeyword);

            if(aBrands.size()>0)
                jedis = Util.Redis.getConnection();

            // Search for the ItemCodes matched in parameter table in ss_acc_inv_brand_item_codes table
            //-----------------------------------------------------------------
            ArrayList<ssoInvBrandItemCodes> itemsFound = new ArrayList<ssoInvBrandItemCodes>();
            for(ssoBrand brandN:aBrands)
            {
                if(pbExactMatch==true)
                {
                    if(brandN.name.toLowerCase().trim().equals(pKeyword.toLowerCase().trim())!=true)
                        continue;
                }
                
                String sStorageKey = "";// "ss_txn_inv_bill" +"printbybill" + accId + vendorId
                
                ArrayList<ssoPrintByBillData> brandNPrintData = new ArrayList<ssoPrintByBillData>();
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // 1. CHECK CACHE IF EXISTS 
                //--------------------------------------------------------------
                sStorageKey = getVendorBillQueueName(pAccId, brandN.Id);

                String sData = null;
                if(bReset!=true)
                {
                    sData = Util.Redis.JString.get(jedis, sStorageKey);
                }
                else//RESET = TRUE
                {
                    Util.Redis.JString.remove(jedis, sStorageKey);
                }

                if (sData==null)//not found on cache
                {
                    Query stmtPrintData = pem.createNamedQuery("SsInvBill.getPrintDataByBill", SsTxnInvBill.class);

                    int index = 1;
                    stmtPrintData.SetParameter(index++, pAccId          , "ACCOUNT_ID");
                    stmtPrintData.SetParameter(index++, brandN.Id       , "VENDOR_ID");

                    List<List<RowColumn>> rs = stmtPrintData.getResultList();
                    for(int i=0; i<rs.size(); i++)
                    {
                        ssoPrintByBillData dataN = new ssoPrintByBillData();

                        List<RowColumn> rowN = rs.get(i);

                        dataN.accountId     = Long.toString(pAccId);
                        dataN.insertdate    = Util.Database.getValString(rowN, "INSERTDATE").toString();
                        dataN.txnDate       = Util.Database.getValString(rowN, "TXN_DATE").toString();
                        dataN.totalAmount   = Util.Database.getValString(rowN, "TOTAL_NET").toString();
                        dataN.totalQuantity = Util.Database.getValString(rowN, "TOTAL_QUANTITY").toString();
                        dataN.subtotalsQuantity = Util.Database.getValString(rowN, "QUANTITY_SUBTOTALS").toString();
                        dataN.options       = Util.Database.getValString(rowN, "OPTIONS").toString();
                        dataN.prices        = Util.Database.getValString(rowN, "PRICES").toString();
                        dataN.year          = Util.Database.getValString(rowN, "YEAR").toString();
                        dataN.price1        = Util.Database.getValString(rowN, "PRICE_ENTRY").toString();
                        dataN.price2        = Util.Database.getValString(rowN, "PRICE_END").toString();
                        dataN.currency      = "TL";//for now
                        dataN.vendorId      = Util.Database.getValString(rowN, "BRAND_ID").toString();
                        dataN.vendorName    = Util.Database.getValString(rowN, "BRAND").toString();
                        
                        brandNPrintData.add(dataN);//this is for cache for each brand
                        printData.add(dataN);//this is to return
                    }

                    // SAVE 2 CACHE
                    //-----------------------------------------------------------------
                    Util.Redis.JObject.set(jedis, sStorageKey, brandNPrintData);

                }
                else
                {
                    // Data Found on Cache
                    JsonArray jsPrintData = Util.JSON.toArray(sData);
                    for(int i =0; i<jsPrintData.size(); i++)
                    {
                        ssoPrintByBillData dataN = new ssoPrintByBillData();
                        dataN = (ssoPrintByBillData)Util.JSON.toObject(jsPrintData.get(i).toString(), ssoPrintByBillData.class);
                        printData.add(dataN);
                    }
                }
                
            }
            
            Util.Redis.releaseConnection(jedis);
                    
            return printData;
        }
        catch(Exception e)
        {
            Util.Redis.releaseConnection(jedis);
            throw e;
        }
    }

    public static String getVendorBillQueueName(long pAccId, long pVendorId)
    {
        String sStorageKey = "ss_txn_inv_bill" + "." + "printbybill" + "." + pAccId + "." + pVendorId;

        return sStorageKey;
    }

    public static void cleanVendorBillQueue(long pAccId, long pVendorId)
    {
        Jedis  jedis = new Jedis();
        
        jedis = Util.Redis.getConnection();
        
        String sStorageKey = getVendorBillQueueName(pAccId, pVendorId);
        
        Util.Redis.JString.remove(jedis, sStorageKey);
        
        Util.Redis.releaseConnection(jedis);
    }

    // USER ID <> ACC ID (AccId under User Id)
    public static ArrayList<ssoInvBrandItemCodes> searchInventoryItems( EntityManager pem, 
                                                                        long pAccId, 
                                                                        String pKeyword) throws Exception
    {
        try
        {

            ArrayList<ssoInvBrandItemCodes> aItemsFound = new ArrayList<ssoInvBrandItemCodes>();

            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            aBrands = DictionaryOps.Vendor.findMatchedBrands4Account(pem, pAccId, pKeyword);

            // Search for the ItemCodes matched in parameter table in ss_acc_inv_brand_item_codes table
            //-----------------------------------------------------------------
            ArrayList<ssoInvBrandItemCodes> itemsFound = new ArrayList<ssoInvBrandItemCodes>();
            for(ssoBrand brandN:aBrands)
            {

                Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.findItemWithOptions", SsAccInvOptionStats.class);
                int index = 1;
                stmtFamily.SetParameter(index++, pAccId          , "ACCOUNT_ID");
                stmtFamily.SetParameter(index++, brandN.Id       , "BRAND_ID");

                List<List<RowColumn>> rs = stmtFamily.getResultList();
                for(int i=0; i<rs.size(); i++)
                { 
                    ssoInvBrandItemCodes itemN = new ssoInvBrandItemCodes();

                    List<RowColumn> rowN = rs.get(i);

                    itemN.brandName = Util.Database.getValString(rowN, "BRAND").toString();
                    itemN.itemCodes = Util.Database.getValString(rowN, "ITEM_CODE").toString();
                    itemN.refId     = Util.Database.getValString(rowN, "HASH_MD5").toString();
                    itemN.quantity  = Util.Database.getValString(rowN, "QUANTITY_ENTERED").toString();

                    itemN.options   = Util.Database.getValString(rowN, "OPTS").toString();

                    itemN.entryDate = Util.Database.getValString(rowN, "DT").toString();
                    /*
                    itemN.entryDate = itemN.entryDate.substring(2, 4) + "." + 
                                      itemN.entryDate.substring(4, 6) + "." + 
                                      itemN.entryDate.substring(6, 8) + ".";
                    */
                    //itemN.salePrice = Util.Database.getValString(rowN, "SALE_PRICE").toString();
                    itemN.entryPrice = new BigDecimal(Util.Database.getValString(rowN, "ENTRY_PRICE").toString()).setScale(2).toString();
                    itemN.salePrice  = new BigDecimal(Util.Database.getValString(rowN, "SALE_PRICE").toString()).setScale(2).toString();

                    aItemsFound.add(itemN);
                }

            }

            return aItemsFound;
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    static void add2ListByUniqueBrandId(ArrayList<Long> paList, long pNew)
    {
        boolean bFound = false;
        
        for(long valN:paList)
        {
            if (valN==pNew)
            {
                bFound = true;
                break;
            }
        }
        
        if (bFound==false)
            paList.add(pNew);
    }

    /*
        1. FIND THE BRANDS MATCHED WITH KEYWORD
        2. FIND THE LIST OF ACCOUNTS / BRANCHES FOR THE USER
        3. PER ACCOUNT / BRANCH
        3.1.      CALCULATE BRAND-ITEM SUM
        3.2.      CALCULATE BRAND-OPTION(s) SUM
        4. CALCULATE BRAND SUM FOR ALL ACCOUNTS TOTAL

        LEVEL 1: ROOT LEVEL(Brand Level) (SHOWS ONLY BRAND BALANCE) (This level will be calculated by level 2 data)
        LEVEL 2: PER OPTION - PER ACCOUNT/BRANCH LEVEL 
        LEVEL 3: BREAK DOWN OF ACCCOUNT /BRANCH (option level)

        Sample Data output
        Item           - Net - Received - Returned - Sold - Activity
        MODIVA / 002   - 10  - 20       - 2 ... (Brand level)
            Green  ...                          (Option Level)
                BULBULLER  - 5   - 15 ...       (Branch Level)
                DILEK      - 5   - 5  ...
            Blue   ...
                DILEK      - 5   - 5  ...
            Red    ...
            Purple ...
    */
    /*
    public static ArrayList<ssoUIBalanceItem> getInventoryBalanceByKeyword( EntityManager pem, 
                                                                            long          pUserId,
                                                                            String        pKeyword) throws Exception
    {
        
        try
        {
            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            // on Cache
            branches = UserOps.getListOfAccounts4User(pem, pUserId);

            // Step 1. (on cache)
            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            aBrands = DictionaryOps.Account.findMatchedBrandsForAccount(pem, pUserId, pKeyword);

            for (ssoBrand brandN: aBrands)
            {
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel3 = new ArrayList<ssoUIBalanceItem>();

                //Step 2.
                for(ssoMerchant accN: branches)
                {
                    // LEVEL 1 (Item Code Level)
                    // PARENT_KEY = <BrandName>
                    //----------------------------------------------------------


                    // 3. CALCULATION FOR EACH BRANCH / ACCOUNT
                    //-------------------------------------------

                    // on cache
                    //3.1 calculate item(s) balance for brand of accId (ON CACHE)
                    ArrayList<ssoAccInvBalanceCore> itemBalances = new ArrayList<ssoAccInvBalanceCore>();
                    balanceSheetLevel1 = ssReportSearchInventory.generateLEVEL1_Brands( pem,
                                                                                        accN.id,
                                                                                        accN.name,
                                                                                        brandN.name,
                                                                                        ThisYear);

                    // LEVEL 2 (Option Level)
                    // PARENT_KEY = bRAND + Itemcode
                    //------------------------------------------------------
                    balanceSheetLevel2 = ssReportSearchInventory.generateLEVEL2_Options(pem, 
                                                                                        accN.id, 
                                                                                        accN.name, 
                                                                                        brandN.name, 
                                                                                        ThisYear, 
                                                                                        balanceSheetLevel1);

                    
                    
                    // LEVEL 3 = BRANCH LEVEL
                    //----------------------------------------------------------
                    // CALCULATE BOTTOM LINE FOR branches BY LEVEL 2 DATA (options)
                    balanceSheetLevel3 = ssReportSearchInventory.generateLEVEL3_Branches(pem, 
                                                                                         accN.name, 
                                                                                         balanceSheetLevel2);
                    
                    // LEVEL 3 = BRANCH LEVEL
                    //----------------------------------------------------------
                    // CALCULATE BOTTOM LINE FOR branches BY LEVEL 2 DATA (options)


                }// end of account N loop

                balanceSheet.addAll(balanceSheetLevel1);
                balanceSheet.addAll(balanceSheetLevel2);
                balanceSheet.addAll(balanceSheetLevel3);//level 1

            }//end of brand N loop

            return balanceSheet;

            // Re-arrange the Data as following
            // L1. Sum per Item Code 
            // L2. Sum per Branch (Break-Down by Branch/AccId)
            // L3. Sum per Option (Break-Down by Branch + Option)

        }
        catch(Exception e)
        {
            throw e;
        }
    }
    */
    
    public static String[] getRowKeyParts(String pKey)
    {
        String [] aKeyParts = pKey.split("-");
        return aKeyParts;
    }
    
    public static String generateRowKey(String prm1, String prm2, String prm3, String prm4)
    {
        return prm1 + "-" + prm2 + "-" + prm3 + "-" + prm4;
    }

    public static String generateRowKeyWSign(String pSEPERATOR_SIGN, String prm1, String prm2, String prm3, String prm4, String prm5, String prm6)
    {
        return prm1 + pSEPERATOR_SIGN + prm2 + pSEPERATOR_SIGN + prm3 + pSEPERATOR_SIGN + prm4 + pSEPERATOR_SIGN + prm5 + pSEPERATOR_SIGN + prm6;
    }

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // WARNING
    // This methods run thru the cahce memory data so be careful when changing 
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    /*
        1. FIND THE BRANDS MATCHED WITH KEYWORD
        2. FIND THE LIST OF ACCOUNTS / BRANCHES FOR THE USER
        3. PER ACCOUNT / BRANCH

        LEVEL 1: ROOT LEVEL(Brand Level) (SHOWS ONLY BRAND BALANCE) (This level will be calculated by level 2 data)
        LEVEL 2: PER ACCOUNT/BRANCH LEVEL 
        LEVEL 3: BREAK DOWN OF ACCCOUNT /BRANCH (option level)

        Sample Data output
        Item           - Net - Received - Returned - Sold - Activity
        MODIVA / 002   - 10,000  - 5,000  - 2,000  ... (Brand Level)
            BULBULLER  - 5   - 15 ... (Branch Level)
            DILEK      - 5   - 5  ...
    */
    /*
    public static ArrayList<ssoUIBalanceItem> getBrandBalances( EntityManager pem, 
                                                                long          pUserId,
                                                                String        pKeyword) throws Exception
    {
        try
        {
            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user (on cache)
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            branches = UserOps.getListOfAccounts4User(pem, pUserId);

            // Step 1. (on cache)
            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            aBrands = DictionaryOps.Account.findMatchedBrandsForAccount(pem, pUserId, pKeyword);

            if (aBrands.size()>0)
            {
                for (ssoBrand brandN: aBrands)
                {
                    ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
                    ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
                    //ArrayList<ssoUIBalanceItem>  balanceSheetLevel3 = new ArrayList<ssoUIBalanceItem>();

                    //Step 2.
                    for(ssoMerchant accN: branches)
                    {
                        ArrayList<ssoAccInvBalanceCore> brandBalances = new ArrayList<ssoAccInvBalanceCore>(); 
                        // On Cache
                        brandBalances = AccountMisc.calculateBrandBalances( pem,
                                                                            accN.id, 
                                                                            accN.name,
                                                                            brandN.name, 
                                                                            ThisYear);

                        // LEVEL 2 = Totals for Branch
                        //----------------------------------------------------------
                        for(ssoAccInvBalanceCore balanceN:brandBalances)
                        {
                            ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                            newItem.level = 2;
                            newItem.name = balanceN.AccountName;//balanceN.Brandname;
                            newItem.key  = generateRowKey(balanceN.AccountName, balanceN.Brandname, "");
                            newItem.parentKey = brandN.name;
                            newItem.quantity = balanceN.quantity;
                            newItem.balance  = balanceN.balance;
                            newItem.lastActivity = balanceN.lastActivity;

                            balanceSheetLevel2.add(newItem);//add balance of each item
                        }

                    }// end of branches

                    // LEVEL 1 = TOTALS for Brand
                    // TOTALS = SUM(BRANCH1..N)
                    //----------------------------------------------------------
                    ssoUIBalanceItem newItem = new ssoUIBalanceItem();
                    newItem.level = 1;
                    newItem.name = brandN.name;
                    newItem.key  = brandN.name;
                    newItem.parentKey = "";
                    for (ssoUIBalanceItem balanceN:balanceSheetLevel2)
                    {
                        newItem.quantity.received += balanceN.quantity.received;
                        newItem.quantity.returned += balanceN.quantity.returned;
                        newItem.quantity.sold     += balanceN.quantity.sold;

                        newItem.balance.received = newItem.balance.received.add(balanceN.balance.received);
                        newItem.balance.returned = newItem.balance.returned.add(balanceN.balance.returned);
                        newItem.balance.sold     = newItem.balance.sold.add(balanceN.balance.sold);

                        newItem.lastActivity  = balanceN.lastActivity;

                    }

                    balanceSheetLevel1.add(newItem);//add balance of each item

                    balanceSheet.addAll(balanceSheetLevel1);
                    balanceSheet.addAll(balanceSheetLevel2);

                }//end for brands

            }
            else
            {
                // skip (no calculation for all brands allowed, this will be a report)
            }
            
            
            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }

    }
    */

}


