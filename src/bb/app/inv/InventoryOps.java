/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.inv;

import bb.app.account.ssoAccInvBalanceCore;
import bb.app.account.AccountMisc;
import bb.app.account.ssoUIBalanceItem;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import entity.inv.SsInvAccounting;
import entity.inv.SsInvItemsOptions;
import entity.mrc.SsMrcDataEod;
import entity.mrc.SsMrcDataPosTxn;
import entity.mrc.SsMrcMerchants;
import entity.prm.SsPrmCountryStates;
import entity.prm.SsPrmCountryCodes;
import entity.prm.SsPrmCountryPostcodes;
import java.util.ArrayList;
import jaxesa.util.Util;
import bb.app.inv.InventoryOps;
import bb.app.pages.ssoCityCode;
import bb.app.pages.ssoPostCode;
import bb.app.pages.ssoCountryCodes;
import bb.app.pages.ssoCountyCode;
import bb.app.pages.ssoInvBrandItemCodes;
import bb.app.pages.ssoInvCategory;
import bb.app.pages.ssoMCC;
import bb.app.pages.ssoMerchant;
import bb.app.pages.ssoMerchantPreferences;
import bb.app.pages.ssoPageParams;
import entity.acc.SsAccInvBrands;
import entity.inv.SsInvItemCodeQuantity;
import entity.inv.SsInvItemOptionsQuantity;
import entity.inv.SsInvTransactions;
import entity.prm.SsPrmInvCategories;
import entity.prm.SsPrmInvItemCodes;
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
import entity.acc.SsAccInvBrandItemCodes;
import entity.acc.SsAccInvBrandItemCodesLog;
import entity.acc.SsAccInvBrandItemOptions;
import entity.acc.SsAccInvBrandItemOptionsLog;
import entity.acc.SsAccInvBrandItemPrice;
import entity.acc.SsAccInvBrandsLog;
import java.math.BigInteger;
import java.util.Set;


/**
 *
 * @author Administrator
 */
public final class InventoryOps
{
    public static int gINV_TXN_CACHE_TIMEOUT = 60 * 5;//5min 

    public static String gTXNTYPE_KEY_NEW_ITEM      = "N";
    public static String gTXNTYPE_KEY_EXISTING_ITEM = "E";
    public static String gTXNTYPE_KEY_RETURN        = "R";

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
                
                lInventoryTxnUID = saveInventoryTransaction( pem,
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

    public static SsAccInvBrands readBrandInfo(EntityManager pem, long pAccountId, String pBrandName) throws Exception
    {
        SsAccInvBrands balance = new SsAccInvBrands();

        try
        {
            ArrayList<ssoKeyField> keys = new ArrayList<ssoKeyField>();

            ssoKeyField KeyUserId = new ssoKeyField();
            KeyUserId.ColumnName = "ACCOUNT_ID";
            KeyUserId.Value      = pAccountId;
            keys.add(KeyUserId);

            ssoKeyField KeyAccountId = new ssoKeyField();
            KeyAccountId.ColumnName = "BRAND";
            KeyAccountId.Value      = pBrandName;
            keys.add(KeyAccountId);

            balance = pem.find(SsAccInvBrands.class, keys);

            return balance;
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

    public static long saveInventoryTransaction(EntityManager pem,
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

        try
        {
            
            SsInvTransactions   invTxn  = new SsInvTransactions();

            int  lQuantity = Integer.parseInt(psQuantity);

            //String sBillDate = psBillingDate.replace("-","");
            //sBillDate = sBillDate.replace(".","");

            long lBillDate = Long.parseLong(psBillingDate);
            BigDecimal  bdPriceEntry = new BigDecimal(psEntryPrice);
            BigDecimal  bdDiscount   = new BigDecimal(psDiscount);
            BigDecimal  bdTax        = new BigDecimal(psTax);
            BigDecimal  bdPriceSale  = new BigDecimal(psSalesPrice);

            invTxn.accountId   = pAccId;
            invTxn.txnType     = psTxnType;
            invTxn.itemBrand   = psBrand;
            invTxn.itemCode    = psItemCode;
            invTxn.quantity    = lQuantity;
            invTxn.priceEntry  = bdPriceEntry;
            invTxn.discount    = bdDiscount;
            invTxn.tax         = bdTax;
            invTxn.priceSales  = bdPriceSale;
            invTxn.options     = psOptions;
            invTxn.stmtDate    = lBillDate;

            //BigDecimal bdTot1         = invTxn.priceEntry.multiply(new BigDecimal(invTxn.quantity));
            //BigDecimal bdStmtTax      = bdTot1.multiply(invTxn.tax.divide(new BigDecimal(100)));
            //BigDecimal bdStmtDiscount = bdTot1.multiply(invTxn.discount);
            invTxn.stmtTotal   = calculateStatementTotal(invTxn.priceEntry, invTxn.quantity, invTxn.tax, invTxn.discount);
            
            //invTxn.stmtTotal   = bdTot1.add(bdStmtTax).subtract(bdStmtDiscount);
            invTxn.descr       = psDesc;
            invTxn.uiSessionid = Long.parseLong(psSessionId.substring(0,19));

            //Calculate Quantity Total

            //int iTotal = calculateTotalQuantity(psOptions);

            long lInvAccUId = pem.persist(invTxn);

            // get brand current balance
            // get item  current quantity

            return lInvAccUId;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static BigDecimal calculateStatementTotal(BigDecimal pPriceEntry, 
                                                     int pQuantity, 
                                                     BigDecimal pTax, 
                                                     BigDecimal pDiscount)
    {
        
        BigDecimal bdTot1         = pPriceEntry.multiply(new BigDecimal(pQuantity));
        BigDecimal bdStmtDiscount = bdTot1.multiply(pDiscount.divide(new BigDecimal(100)));
        BigDecimal bdStmtTax      = bdTot1.subtract(bdStmtDiscount).multiply(pTax.divide(new BigDecimal(100)));

        bdTot1 = bdTot1.subtract(bdStmtDiscount).add(bdStmtTax);

        return bdTot1;
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
    
    public static ssoInventoryParams registerNewBrandNCodeNCategory(EntityManager  pem,
                                                                    long           pUserId,
                                                                    long           pAccountId,
                                                                    String         pBrandName,
                                                                    String         pItemCode,
                                                                    String         pCategory) throws Exception
    {
        String sItemCode     = "";
        String sCategoryCode = "";
        
        ssoInventoryParams InvParams = new ssoInventoryParams();
        
        try
        {

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_MRC_REGISTER_NEW_BRAND_N_CODE_N_CATEGORY");

            SP.registerStoredProcedureParameter("P_ACC_ID"        , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BRAND_NAME"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_ITEM_CODE"     , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CATEGORY"      , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId          , "P_ACC_ID");
            SP.SetParameter(Colindex++, pBrandName          , "P_BRAND_NAME");
            SP.SetParameter(Colindex++, pItemCode           , "P_ITEM_CODE");
            SP.SetParameter(Colindex++, pCategory           , "P_CATEGORY");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            if (rs.size()>0)
            {
                List<RowColumn> RowN = rs.get(0);

                sItemCode     = Util.Database.getValString(RowN, "UID_ITEM_CODE");
                sCategoryCode = Util.Database.getValString(RowN, "UID_CATEGORY_CODE");
                
                InvParams.itemUID = Long.parseLong(sItemCode);
                InvParams.categoryUID = Long.parseLong(sCategoryCode);

            }

            return InvParams;
            
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
    public static boolean updateItemOptionsQuantity( EntityManager            pem,
                                                     long                     pTxnId,
                                                     SsAccInvBrandItemCodes   pBrandItem,
                                                     ArrayList<ssoItemOption> pOptions,
                                                     String                   pTxnType,
                                                     String                   pQuantity,
                                                     long                     pPrmCategoryId
                                                     ) throws Exception
    {
        try
        {
            ArrayList<ssoItemOption> aOptions = new ArrayList<ssoItemOption>();
            boolean bNewItemOption = false;

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            // read option quantity
            // update it
            // add log 
            SsAccInvBrandItemOptions itemOption = new SsAccInvBrandItemOptions();

            if(pOptions.size()==0)
            {
                //entry without options so then run it once for empty
                ssoItemOption noOption = new ssoItemOption();
                noOption.optionName = "";
                noOption.groupName  = "";
                if ((pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true) || (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true))
                {
                    noOption.quantity   = pQuantity;//Integer.toString(pBrandItem.quantityEntered);
                }
                else if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
                {
                    noOption.quantity   = pQuantity;//Integer.toString(pBrandItem.quantityReturned);
                }   
                
                aOptions.add(noOption);
            }
            else
            {
                aOptions.addAll(pOptions);
            }

            for(ssoItemOption optN:aOptions)
            {

                Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.findByAccIdNBrandIdNItemCodeNOptions", SsAccInvBrandItemOptions.class);

                int index = 1;
                stmtFamily.SetParameter(index++, pBrandItem.accountId      , "ACCOUNT_ID");
                stmtFamily.SetParameter(index++, pBrandItem.brandId        , "BRAND_ID");
                stmtFamily.SetParameter(index++, pBrandItem.uid            , "ITEM_CODE");//uid
                stmtFamily.SetParameter(index++, optN.groupName            , "OPTION_1");
                stmtFamily.SetParameter(index++, optN.optionName           , "OPTION_2");
                stmtFamily.SetParameter(index++, ThisYear                  , "FINANCIAL_YEAR");

                List<SsAccInvBrandItemOptions> rs = stmtFamily.getResultList(SsAccInvBrandItemOptions.class);

                if(rs.size()>0)
                {
                    itemOption = rs.get(0);

                    bNewItemOption = false;
                }
                else
                {
                    bNewItemOption = true;

                    itemOption.accountId = pBrandItem.accountId;
                    itemOption.brandId   = pBrandItem.brandId;
                    itemOption.itemCodeId= pBrandItem.uid;//uid == itemcode
                    itemOption.prmCategoryId  = pPrmCategoryId;
                    itemOption.option1   = optN.groupName;
                    itemOption.option2   = optN.optionName;
                    
                    if ((pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true) || (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true))
                        itemOption.quantityEntered  = Integer.parseInt(optN.quantity);

                    if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
                        itemOption.quantityReturned  = Integer.parseInt(optN.quantity);

                    itemOption.quantitySold     = 0;
                    itemOption.financialYear    = ThisYear;

                    pem.persist(itemOption);
                }

                SsAccInvBrandItemOptionsLog brndItemOptLog = new SsAccInvBrandItemOptionsLog();
                brndItemOptLog.accountId          = itemOption.accountId;
                brndItemOptLog.itemCodeId         = itemOption.itemCodeId;
                brndItemOptLog.option1            = itemOption.option1;
                brndItemOptLog.option2            = itemOption.option2;
                brndItemOptLog.txnType            = pTxnType;
                brndItemOptLog.txnId              = pTxnId;
                brndItemOptLog.financialYear      = itemOption.financialYear;
                brndItemOptLog.revolvingQuantity  = itemOption.revolvingQuantity;
                if (bNewItemOption==true)
                {
                    brndItemOptLog.oldQuantityEntered = 0;
                    brndItemOptLog.oldQuantitySold    = 0;
                    brndItemOptLog.oldQuantityReturned= 0;
                }
                else
                {
                    brndItemOptLog.oldQuantityEntered = itemOption.quantityEntered;
                    brndItemOptLog.oldQuantitySold    = itemOption.quantitySold;
                    brndItemOptLog.oldQuantityReturned= itemOption.quantityReturned;
                }
                //brndItemOptLog.newQuantityEntered = itemOption.quantityEntered;
                //brndItemOptLog.newQuantitySold    = itemOption.quantitySold;

                //2. Update Quantity
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

                            // PREPARING LOG
                            //--------------------------------------------------
                            if (iQuantity!=-1)
                            {
                                if (itemOption.quantityEntered==-1)
                                    brndItemOptLog.newQuantityEntered = iQuantity;
                                else
                                    brndItemOptLog.newQuantityEntered = itemOption.quantityEntered;// + iQuantity;
                            }
                            else
                            {
                                //means ignored
                                brndItemOptLog.newQuantityEntered = itemOption.quantityEntered;
                            }
                            
                            brndItemOptLog.newQuantityReturned = itemOption.quantityReturned;
                            brndItemOptLog.newQuantitySold     = itemOption.quantitySold;
                        }
                        else if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
                        {
                            // PREPARING ITEM
                            //--------------------------------------------------
                            if (itemOption.quantityReturned==-1)
                                itemOption.quantityReturned = iQuantity;
                            else
                                itemOption.quantityReturned += iQuantity;

                            // PREPARING LOG
                            //--------------------------------------------------
                            // N / A
                            // No change if it is return 
                            if (iQuantity!=-1)
                            {
                                if (itemOption.quantityEntered==-1)
                                    brndItemOptLog.newQuantityReturned = iQuantity;
                                else
                                    brndItemOptLog.newQuantityReturned = itemOption.quantityReturned;// + iQuantity;
                            }
                            else
                            {
                                //means ignored
                                brndItemOptLog.newQuantityReturned = itemOption.quantityReturned;//keep same
                            }
                            
                            brndItemOptLog.newQuantityEntered  = itemOption.quantityEntered;
                            brndItemOptLog.newQuantitySold     = itemOption.quantitySold;

                        }

                        // update here (THIS IS IF NOT A NEW ITEM -> IF NEW ITEM IT IS ADDED IN THE LINES ABOVE)
                        commit2DB_ItemOptionQuantity(pem, itemOption);
                    }
                    else
                    {
                        //don't update the quantity - IGNORE
                        
                        // Only update the log - keep stats still
                        brndItemOptLog.newQuantityReturned= itemOption.quantityReturned;
                        brndItemOptLog.newQuantityEntered = itemOption.quantityEntered;
                        brndItemOptLog.newQuantitySold    = itemOption.quantitySold;

                    }
                }
                else
                {
                    brndItemOptLog.newQuantityReturned= itemOption.quantityReturned;
                    brndItemOptLog.newQuantityEntered = itemOption.quantityEntered;
                    brndItemOptLog.newQuantitySold    = itemOption.quantitySold;
                }
                
                // add Log
                pem.persist(brndItemOptLog);
                
            }// end of for
 
            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static long addItemPrice(EntityManager pem,
                                    long          pTxnId,
                                    long          pAccId,
                                    String        pEntryPrice,
                                    String        pDiscount,
                                    String        pTax,
                                    String        pSalesPrice) throws Exception
    {
        SsAccInvBrandItemPrice itemPrice = new SsAccInvBrandItemPrice();

        try
        {
            itemPrice.accountId  = pAccId;
            itemPrice.txnId      = pTxnId;
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

    public static void commit2DB_ItemOptionQuantity(EntityManager               pem,
                                                    SsAccInvBrandItemOptions    pItemOption) throws Exception
    {
        try
        {
            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.updateQuantity", SsAccInvBrandItemOptions.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pItemOption.quantityEntered    , "QUANTITY_ENTERED");
            stmtFamily.SetParameter(index++, pItemOption.quantityReturned   , "QUANTITY_RETURNED");
            stmtFamily.SetParameter(index++, pItemOption.quantitySold       , "QUANTITY_SOLD");
            stmtFamily.SetParameter(index++, pItemOption.accountId          , "BYUSER");
            stmtFamily.SetParameter(index++, pItemOption.uid                , "UID");

            stmtFamily.executeUpdate();

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static SsAccInvBrandItemCodes updateItemInfo(EntityManager   pem,
                                                        long            pTxnId,
                                                        String          pTxnType,
                                                        long            pAccountId, 
                                                        long            pBrandId,
                                                        String          pItemCode,
                                                        String          pQuantity,
                                                        long            pPrmCategoryId,
                                                        long            pPriceId) throws Exception
    {
        try
        {
            boolean bNewBrandItem = false;
            int iOldQuantity = 0;

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            // 1. get curret quantity 
            // 2. update it
            // 3. add log
            SsAccInvBrandItemCodes brndItem = new SsAccInvBrandItemCodes();

            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemCodes.findByAccIdNBrandNItemCode", SsAccInvBrandItemCodes.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pAccountId, "ACCOUNT_ID");
            stmtFamily.SetParameter(index++, pBrandId  , "BRAND_ID");
            stmtFamily.SetParameter(index++, pItemCode , "ITEM_CODE");
            stmtFamily.SetParameter(index++, ThisYear  , "FINANCIAL_YEAR");

            List<SsAccInvBrandItemCodes> rs = stmtFamily.getResultList(SsAccInvBrandItemCodes.class);

            if(rs.size()>0)
            {
                brndItem = rs.get(0);
                
                bNewBrandItem = false;
            }
            else
            {
                bNewBrandItem = true;
                
                // insert new with 0 quantity
                brndItem.accountId = pAccountId;
                brndItem.brandId   = pBrandId;
                brndItem.itemCode  = pItemCode;
                brndItem.prmCategoryId  = pPrmCategoryId;
                brndItem.priceId   = pPriceId;
                brndItem.financialYear = ThisYear;
                
                brndItem.quantitySold = 0;
                if ((pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true) || (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true))
                    brndItem.quantityEntered  = Integer.parseInt(pQuantity);
                else if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
                    brndItem.quantityReturned = Integer.parseInt(pQuantity);

                brndItem.uid = pem.persist(brndItem);
            }
            brndItem.priceId = pPriceId;//update the price info always

            SsAccInvBrandItemCodesLog brndItemLog = new SsAccInvBrandItemCodesLog();
            brndItemLog.accountId   = brndItem.accountId;
            brndItemLog.brandId     = brndItem.brandId;
            brndItemLog.itemCode    = brndItem.itemCode;
            brndItemLog.txnId       = pTxnId;
            brndItemLog.txnType     = pTxnType;
            brndItemLog.priceId     = pPriceId;
            brndItemLog.revolvingQuantity = brndItem.revolvingQuantity;

            if(bNewBrandItem==true)
            {
                brndItemLog.oldQuantityEntered  = 0;
                brndItemLog.oldQuantityReturned = 0;
                brndItemLog.oldQuantitySold     = 0;
            }
            else
            {
                brndItemLog.oldQuantityEntered  = brndItem.quantityEntered;
                brndItemLog.oldQuantityReturned = brndItem.quantityReturned;
                brndItemLog.oldQuantitySold     = brndItem.quantitySold;
            }

            //2. update if not new 
            //-----------------------------------------------------------------
            if ((pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true) || (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true))
            {
                if (bNewBrandItem!=true)
                    brndItem.quantityEntered += Integer.parseInt(pQuantity);

                brndItemLog.newQuantityEntered = brndItem.quantityEntered;
                brndItemLog.newQuantityReturned= brndItem.quantityReturned;
                brndItemLog.newQuantitySold    = brndItem.quantitySold;
            }
            else if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
            {
                //return 
                if (bNewBrandItem!=true)
                    brndItem.quantityReturned += Integer.parseInt(pQuantity);

                brndItemLog.newQuantityReturned = brndItem.quantityReturned;
                brndItemLog.newQuantityEntered  = brndItem.quantityEntered;
                brndItemLog.newQuantitySold     = brndItem.quantitySold;
            }
            
            if (bNewBrandItem!=true)
            {
                // update ITEM
                commit2DB_BrandItemInfo(pem, brndItem);
            }

            //3. add log
            //-----------------------------------------------------------------
            //brndItemLog.newQuantity = brndItem.quantity;

            brndItemLog.uid = pem.persist(brndItemLog);

            return brndItem;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void commit2DB_BrandItemInfo(EntityManager          pem, 
                                               SsAccInvBrandItemCodes pBrandItem) throws Exception
    {
        try
        {
            Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemCodes.updateItem", SsAccInvBrandItemCodes.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pBrandItem.quantityEntered             , "QUANTITY_ENTERED");
            stmtFamily.SetParameter(index++, pBrandItem.quantityReturned            , "QUANTITY_RETURNED");
            stmtFamily.SetParameter(index++, pBrandItem.priceId                     , "PRICE_ID");
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

    public static SsAccInvBrands updateBrandBalance(EntityManager   pem, 
                                                    long            pTxnId,
                                                    String          pTxnType,
                                                    long            pAccountId, 
                                                    String          pBrand,
                                                    String          pQuantity, 
                                                    String          pEntryPrice, 
                                                    String          pDiscount,
                                                    String          pTax) throws Exception
    {
        try
        {
            boolean bBrandAccExist = true;
            SsAccInvBrands brandAcc = new SsAccInvBrands();
            SsAccInvBrandsLog brandLog = new SsAccInvBrandsLog();

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            Query stmtFamily = pem.createNamedQuery("SsAccInvBrands.findByAccIdNBrand", SsAccInvBrands.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pAccountId, "ACCOUNT_ID");
            stmtFamily.SetParameter(index++, pBrand    , "BRAND");
            stmtFamily.SetParameter(index++, ThisYear  , "FINANCIAL_YEAR");

            List<SsAccInvBrands> rs = stmtFamily.getResultList(SsAccInvBrands.class);

            if(rs.size()>0)
            {
                brandAcc = rs.get(0);
            }
            else
            {
                // create new brand balance
                bBrandAccExist = false;
                brandAcc.financialYear = ThisYear;
            }

            // brand acount
            BigDecimal stmtTotal = new BigDecimal(BigInteger.ZERO);
            int iQuantity = Integer.parseInt(pQuantity);
            brandAcc.brand = pBrand;
            brandAcc.accountId = pAccountId;

            // brand account log
            brandLog.accountId = pAccountId;
            brandLog.brand     = pBrand;
            brandLog.txnId     = pTxnId;
            brandLog.txnType   = pTxnType;
            
            if (brandAcc.balanceEntered==null)
                brandAcc.balanceEntered = BigDecimal.ZERO;
            if (brandAcc.balanceReturned==null)
                brandAcc.balanceReturned = BigDecimal.ZERO;
            if (brandAcc.balanceSold==null)
                brandAcc.balanceSold = BigDecimal.ZERO;
            if (brandAcc.revolvingBalance==null)
                brandAcc.revolvingBalance = BigDecimal.ZERO;
            
            brandLog.financialYear      = brandAcc.financialYear;
            brandLog.revolvingQuantity  = brandAcc.revolvingQuantity;
            brandLog.revolvingBalance   = brandAcc.revolvingBalance;
            
            brandLog.oldBalanceEntered  = brandAcc.balanceEntered;
            brandLog.oldBalanceReturned = brandAcc.balanceReturned;
            brandLog.oldBalanceSold     = brandAcc.balanceSold;
            brandLog.oldQuantityEntered = brandAcc.quantityEntered;
            brandLog.oldQuantityReturned= brandAcc.quantityReturned;
            brandLog.oldQuantitySold    = brandAcc.quantitySold;
            
            stmtTotal = calculateStatementTotal(new BigDecimal(pEntryPrice), 
                                                iQuantity, 
                                                new BigDecimal(pTax), 
                                                new BigDecimal(pDiscount));

            if (pTxnType.equals(gTXNTYPE_KEY_NEW_ITEM)==true)
            {
                // add up to balance and quantity 
                brandAcc.quantityEntered += iQuantity;
                brandAcc.balanceEntered = brandAcc.balanceEntered.add(stmtTotal);
            }
            else if (pTxnType.equals(gTXNTYPE_KEY_EXISTING_ITEM)==true)
            {
                // add up only to quantity 
                brandAcc.quantityEntered += iQuantity;
            }
            else if (pTxnType.equals(gTXNTYPE_KEY_RETURN)==true)
            {
                // deduct balance and quantity
                brandAcc.quantityReturned += iQuantity;
                brandAcc.balanceReturned  = brandAcc.balanceReturned.add(stmtTotal);
            }

            if(bBrandAccExist==true)
                commit2DB_BrandAcc(pem, brandAcc);
            else
                brandAcc.uid = pem.persist(brandAcc);

            //if it is existing item should not affect the balance
            brandLog.newBalanceEntered  = brandAcc.balanceEntered;
            brandLog.newBalanceReturned = brandAcc.balanceReturned;
            brandLog.newBalanceSold     = brandAcc.balanceSold;

            //log
            brandLog.newQuantityEntered = brandAcc.quantityEntered;
            brandLog.newQuantityReturned= brandAcc.quantityReturned;
            brandLog.newQuantitySold    = brandAcc.quantitySold;

            long UIDLog = pem.persist(brandLog);
            
            return brandAcc;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean commit2DB_BrandAcc(EntityManager   pem, SsAccInvBrands pBrandAcc) throws Exception
    {
        try
        {
            Query stmtBrandAcc = pem.createNamedQuery("SsAccInvBrands.updateBrandAcc", SsAccInvBrands.class);

            int index = 1;
            stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityEntered  , "Q_ENTERED");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.balanceEntered   , "B_ENTERED");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityReturned , "Q_RETURNED");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.balanceReturned  , "B_RETURNED");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.quantitySold     , "Q_SOLD");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.balanceSold      , "B_SOLD");
            stmtBrandAcc.SetParameter(index++, Long.toString(pBrandAcc.accountId)      , "BY_USER");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.accountId        , "ACCOUNT_ID");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.brand            , "BRAND");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.financialYear    , "BRAND");

            stmtBrandAcc.executeUpdate();
            
            return true;
        
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static long addNewBrandItem(EntityManager  pem,
                                          long            pAccountId,
                                          String          pBrand,
                                          String          pItemCode,
                                          long            pAccBrandId,
                                          long            pAccItemId,
                                          String          pSalesPrice) throws Exception
    {
        long lBrandItemUID = 0 ;
        
        try
        {
            if(pBrand.trim().length()!=0)
            {
                SsPrmInvItemCodes NewItemCode = new SsPrmInvItemCodes();

                NewItemCode.accountId = pAccountId;

                ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();
                ssoKeyField criteria1 = new ssoKeyField();
                criteria1.ColumnName = "ACCOUNT_ID";
                criteria1.Value      = pAccountId;
                criterias.add(criteria1);

                ssoKeyField criteria2 = new ssoKeyField();
                criteria2.ColumnName = "BRAND_NAME";
                criteria2.Value      = pBrand;
                criterias.add(criteria2);

                ssoKeyField criteria3 = new ssoKeyField();
                criteria3.ColumnName = "ITEM_CODE";
                criteria3.Value      = pItemCode;
                criterias.add(criteria3);

                NewItemCode = pem.find(SsPrmInvItemCodes.class, criterias);
                if(NewItemCode==null)
                {
                    // New Item 
                    //------------------------------------------------------
                    NewItemCode = new SsPrmInvItemCodes();

                    NewItemCode.accountId = pAccountId;
                    NewItemCode.brandName = pBrand;
                    NewItemCode.itemCode  = pItemCode;
                    NewItemCode.accBrandId = pAccBrandId;
                    NewItemCode.accItemId  = pAccItemId;
                    NewItemCode.lastSalesPrice = new BigDecimal(pSalesPrice);

                    lBrandItemUID = pem.persist(NewItemCode);
                }
                else
                {
                    lBrandItemUID = NewItemCode.uid;

                    // Existing Item -> Update Last Sales Price Info
                    //------------------------------------------------------
                    // The following part is dismissed
                    
                    /*
                    Query stmtUpdItemCode = pem.createNamedQuery("SsPrmInvItemCodes.updateSalesPrice", SsPrmInvItemCodes.class);

                    int index = 1;
                    stmtUpdItemCode.SetParameter(index++, pAccountId   , "LAST_SALES_PRICE");
                    stmtUpdItemCode.SetParameter(index++, "bb-lib-api" , "BYUSER");
                    stmtUpdItemCode.SetParameter(index++, lBrandItemUID, "UID");

                    stmtUpdItemCode.executeUpdate();
                    */

                }
            }
            
            return lBrandItemUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static long addNewCategory(  EntityManager  pem,
                                        long           pAccountId,
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
                SsPrmInvCategories NewCategory = new SsPrmInvCategories();

                NewCategory.accountId = pAccountId;

                ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();
                ssoKeyField criteria1 = new ssoKeyField();
                criteria1.ColumnName = "ACCOUNT_ID";
                criteria1.Value      = pAccountId;
                criterias.add(criteria1);

                ssoKeyField criteria2 = new ssoKeyField();
                criteria2.ColumnName = "CATEGORY";
                criteria2.Value      = pCategory.trim().toUpperCase();
                criterias.add(criteria2);

                NewCategory = pem.find(SsPrmInvCategories.class, criterias);
                if (NewCategory==null)
                {
                    NewCategory = new SsPrmInvCategories();
                    
                    //add New Category
                    NewCategory.category  = pCategory;
                    NewCategory.accountId = pAccountId;

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

    public static boolean deleteCategory(   EntityManager  pem,
                                            long           pAccountId,
                                            String         pCategory) throws Exception
    {
        //WORKS WITH CACHE
        
        try
        {
            
            //deleteTest(pem);

            Query stmtFamily = pem.createNamedQuery("SsPrmInvCategories.deleteCategory", SsPrmInvCategories.class);

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

    public static boolean deleteBrand(  EntityManager  pem,
                                        long           pAccountId,
                                        String         pBrand) throws Exception
    {
        //WORKS WITH CACHE
        try
        {
            Query stmtFamily = pem.createNamedQuery("SsPrmInvItemCodes.deleteBrand", SsPrmInvItemCodes.class);

            int index = 1;
            stmtFamily.SetParameter(index++, pAccountId, "ACCOUNT_ID");
            stmtFamily.SetParameter(index++, pBrand    , "BRAND_NAME");

            stmtFamily.executeUpdate();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean deleteItemCode(   EntityManager  pem,
                                            long           pAccountId,
                                            String         pBrand,
                                            String         pItemCode) throws Exception
    {
        try
        {
            // WORKS WITH CACHE
            
            Query stmtFamily = pem.createNamedQuery("SsPrmInvItemCodes.deleteItemCode", SsPrmInvItemCodes.class);

            int ParIndex = 1;
            stmtFamily.SetParameter(ParIndex++, pAccountId, "ACCOUNT_ID");
            stmtFamily.SetParameter(ParIndex++, pBrand    , "BRAND_NAME");
            stmtFamily.SetParameter(ParIndex++, pItemCode , "ITEM_CODE");

            stmtFamily.executeUpdate();

            return true;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

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
            String KEY_COUNTY       = "county";
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

                    JsonElement jePostCode  = item.get(KEY_POSTCODE);
                    JsonElement jePCodeName = item.get(KEY_PCODE_NAME);

                    ssoCountyCode newCounty = new ssoCountyCode();

                    newCounty.code  =   jePostCode.toString().replace("\"", "");
                    newCounty.name  =   jePCodeName.toString().replace("\"", "");
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
    public static ArrayList<ssoInvBrandItemCodes> getBrandList(EntityManager pem, long pMrcId) throws Exception
    {
        // WORKS WITH CACHE

        ArrayList<ssoInvBrandItemCodes> BrandList = new ArrayList<ssoInvBrandItemCodes>();

        try
        {
            Query stmtFamily = pem.createNamedQuery("SsPrmInvItemCodes.getBrandNItemList", SsPrmInvItemCodes.class);
            int index = 1;
            stmtFamily.SetParameter(index++, pMrcId, "ACCOUNT_ID");
            //stmtFamily.SetParameter(index++, psFamilyCode , "FAMILY_CODE");
            List<List<RowColumn>> rs = stmtFamily.getResultList();

            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoInvBrandItemCodes newBrandItem = new ssoInvBrandItemCodes();
                
                newBrandItem.brandName = Util.Database.getVal(rowN, "BRAND_NAME").toString().trim().toUpperCase();
                newBrandItem.itemCodes = Util.Database.getVal(rowN, "CODE_LIST").toString();

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

            ArrayList<SsPrmInvCategories> ctgs = new ArrayList<SsPrmInvCategories>();
            ctgs = (ArrayList<SsPrmInvCategories>)pem.findAll(SsPrmInvCategories.class, keys);
            for (SsPrmInvCategories ctgN: ctgs)
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

    // USER ID <> ACC ID (AccId under User Id)
    public static ArrayList<ssoInvBrandItemCodes> searchInventoryItems(EntityManager pem, long pAccId, String pKeyword) throws Exception
    {
        try
        {
            ArrayList<SsPrmInvItemCodes> aPrmItemCodes = new ArrayList<SsPrmInvItemCodes>();
            ArrayList<ssoInvBrandItemCodes> aItemsFound = new ArrayList<ssoInvBrandItemCodes>();
            
            /*
            ArrayList<Long> aBrandIds = new ArrayList<Long>();
            ArrayList<ssoInvBrandItemCodes> aItemsFound = new ArrayList<ssoInvBrandItemCodes>();
            
            ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();

            ssoKeyField keyAccId = new ssoKeyField();
            keyAccId.ColumnName = "ACCOUNT_ID";
            keyAccId.Value      = pMrcId;
            criterias.add(keyAccId);

            aPrmItemCodes = (ArrayList<SsPrmInvItemCodes>)pem.findAll(SsPrmInvItemCodes.class, criterias);

            for(SsPrmInvItemCodes itemCodeN:aPrmItemCodes)
            {
                if (itemCodeN.brandName.toLowerCase().indexOf(pKeyword.toLowerCase().trim())>=0)
                {
                    //itemCodeN.accBrandId
                    add2ListByUniqueBrandId(aBrandIds, itemCodeN.accBrandId);
                }
            }
            */
            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            aBrands = InventoryMisc.findMatchedBrandsForAccount(pem, pAccId, pKeyword);

            // Search for the ItemCodes matched in parameter table in ss_acc_inv_brand_item_codes table
            //-----------------------------------------------------------------
            ArrayList<ssoInvBrandItemCodes> itemsFound = new ArrayList<ssoInvBrandItemCodes>();
            for(ssoBrand brandN:aBrands)
            {

                Query stmtFamily = pem.createNamedQuery("SsAccInvBrandItemOptions.findItemWithOptions", SsAccInvBrandItemOptions.class);
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

    public static ArrayList<ssoUIBalanceItem> getInventoryBalanceByKeyword( EntityManager pem, 
                                                                                long          pUserId,
                                                                                String        pKeyword) throws Exception
    {
        /*
            1. FIND THE BRANDS MATCHED WITH KEYWORD
            2. FIND THE LIST OF ACCOUNTS / BRANCHES FOR THE USER
            3. PER ACCOUNT / BRANCH
            3.1.      CALCULATE BRAND-ITEM SUM
            3.2.      CALCULATE BRAND-OPTION(s) SUM
            4. CALCULATE BRAND SUM FOR ALL ACCOUNTS TOTAL

            LEVEL 1: ROOT LEVEL(Brand Level) (SHOWS ONLY BRAND BALANCE) (This level will be calculated by level 2 data)
            LEVEL 2: PER ACCOUNT/BRANCH LEVEL 
            LEVEL 3: BREAK DOWN OF ACCCOUNT /BRANCH (option level)

            Sample Data output
            Item           - Net - Received - Returned - Sold - Activity
            MODIVA / 002   - 10  - 20       - 2 ...
                BULBULLER  - 5   - 15 ...
                    Green  ...
                    Blue   ...
                DILEK      - 5   - 5  ...
                    Red    ...
                    Purple ...
        */
        try
        {
            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            branches = AccountMisc.getListOfMerchants4User(pem, pUserId);

            // Step 1.
            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            aBrands = InventoryMisc.findMatchedBrandsForAccount(pem, pUserId, pKeyword);

            for (ssoBrand brandN: aBrands)
            {
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel3 = new ArrayList<ssoUIBalanceItem>();

                //Step 2.
                for(ssoMerchant accN: branches)
                {
                    // LEVEL 2 (Account Level)
                    // PARENT_KEY = <BrandName>
                    //----------------------------------------------------------


                    // 3. CALCULATION FOR EACH BRANCH / ACCOUNT
                    //-------------------------------------------

                    //3.1 calculate item(s) balance for brand of accId
                    ArrayList<ssoAccInvBalanceCore> itemBalances = new ArrayList<ssoAccInvBalanceCore>();
                    itemBalances = AccountMisc.calculateAccountBalance4Brand(   pem,
                                                                                accN.id, 
                                                                                accN.name,
                                                                                brandN.name, 
                                                                                ThisYear);
                    //Adding to Balance Sheet
                    for(ssoAccInvBalanceCore ItemN:itemBalances)
                    {
                        ssoUIBalanceItem newItem = new ssoUIBalanceItem();
                        
                        newItem.level = 2;
                        newItem.name = accN.name;
                        newItem.key  = Long.toString(ItemN.AccountId);
                        newItem.parentKey = Long.toString(brandN.Id);
                        newItem.quantity = ItemN.quantity;
                        newItem.balance  = ItemN.balance;
                        
                        balanceSheetLevel2.add(newItem);//add balance of each item
                    }
                    
                    //Calculating sub-level (Level 3)
                    for(ssoAccInvBalanceCore ItemN:itemBalances)
                    {

                        // LEVEL 3 (Option Level)
                        // PARENT_KEY = <Branch/Account>
                        //------------------------------------------------------
                        
                        //3.2 calculate balance for brand-option(s) of accId
                        ArrayList<ssoAccInvBalanceCore> optionBalances = new ArrayList<ssoAccInvBalanceCore>();
                        optionBalances = AccountMisc.calculateOptionsBalance4Account( pem, 
                                                                                     accN.id,
                                                                                     accN.name,
                                                                                     brandN.name, 
                                                                                     ItemN.ItemCodeId, 
                                                                                     ItemN.ItemCode, 
                                                                                     ThisYear);
                        //Add data to BalanceSheet (LEVEL 3)
                        //------------------------------------------------------
                        for(ssoAccInvBalanceCore optN:optionBalances)
                        {
                            ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                            newItem.level = 3;
                            newItem.name = optN.Option;
                            newItem.key  = optN.Option;
                            newItem.parentKey = Long.toString(accN.id);
                            newItem.quantity = optN.quantity;
                            newItem.balance  = optN.balance;

                            balanceSheetLevel3.add(newItem);//add balance of each item
                        }
                        
                    }

                }// end of account N loop

                // LEVEL 1 = ROOT LEVEL - BRAND LEVEL
                //----------------------------------------------------------
                // CALCULATE BOTTOM LINE FOR BRAND BY LEVEL 2 DATA
                ArrayList<ssoUIBalanceItem>  summary = new ArrayList<ssoUIBalanceItem>();
                ssoUIBalanceItem SumBalance = new ssoUIBalanceItem();
                SumBalance.name = brandN.name;
                SumBalance.parentKey = "";
                SumBalance.key  = Long.toString(brandN.Id);
                for(ssoUIBalanceItem balanceN:balanceSheetLevel2)
                {
                    if (balanceN.parentKey.equals(Long.toString(brandN.Id))==true)
                    {
                        SumBalance.quantity.net      += balanceN.quantity.net;
                        SumBalance.quantity.received += balanceN.quantity.received;
                        SumBalance.quantity.returned += balanceN.quantity.returned;
                        SumBalance.quantity.sold     += balanceN.quantity.sold;

                        SumBalance.balance.net.add(balanceN.balance.net);
                        SumBalance.balance.received.add(balanceN.balance.received);
                        SumBalance.balance.returned.add(balanceN.balance.returned);
                        SumBalance.balance.sold.add(balanceN.balance.sold);

                    }
                }
                
                balanceSheet.add(SumBalance);//level 1
                balanceSheet.addAll(balanceSheetLevel2);
                balanceSheet.addAll(balanceSheetLevel3);
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

}


