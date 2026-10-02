/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import bb.app.account.ssoAccInvBalanceCore;
import bb.app.txn.txnDefs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvOptionStats;
import entity.txn.SsTxnInvQUpdates;
import entity.txn.SsTxnInvQuantityAdj;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;
import org.json.JSONObject;

/**
 *
 * @author Administrator
 */
public final class InventoryUpdate 
{
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_ACC_ID   = "A";
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_BRAND_ID = "V";//VENDOR
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_ITM_CODE = "IC";

    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_ID   = "OI";//UI 
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_GROUP= "OG";//Siyah
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_CODE = "OC";//38

    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OLD_VAL  = "QO";//QUANTITY OLD
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_NEW_VAL  = "QN";//QUANTITY NEW

    public static boolean updateInventory4Account_ItemCodes(EntityManager pem, 
                                                            BigInteger          pUserAccountId, 
                                                            String        psChanges
                                                          ) throws Exception
    {
        try
        {
            ArrayList<ssoInvChangeItem> aChanges = new ArrayList<ssoInvChangeItem>();

            boolean rc = false;
            aChanges = collectChanges(psChanges, pUserAccountId);
            if (aChanges.size()>0)
            {
                BigInteger lAccId      = aChanges.get(0).AccId;
                BigInteger lVendorId   = aChanges.get(0).BrandId;

                // Save Txn 
                //rc = saveInvUpdateTxn(pem, lAccId, lVendorId, psChanges);
                //if(rc==true)
                //{
                    // Collect Sys Latest Values for Changes from UI
                    updateSysValuesOnChanges(pem, aChanges, pUserAccountId, lVendorId);

                    rc = updateItemStats2Changes(pem, pUserAccountId, aChanges);
                    if (rc==true)
                    {
                        BigDecimal bdChangeNumber = new BigDecimal(BigInteger.ZERO);
                        bdChangeNumber = calcItemTotalChangeNumber(aChanges);

                        String sItemCode = aChanges.get(0).ItemCode;

                        rc = updateVendorStats2Changes( pem, 
                                                        pUserAccountId, 
                                                        lVendorId, 
                                                        bdChangeNumber);
                        if(rc==true)
                        {
                            resetMemoryTables(pem, pUserAccountId, aChanges);

                            // Prepare Fixed Changes
                            String sChangesFixed = applyFixedChanges(aChanges);
                            
                            BigInteger lUID = saveInvUpdateTxn(pem, 
                                                                lAccId,
                                                                lVendorId,
                                                                bdChangeNumber,
                                                                psChanges,
                                                                sChangesFixed,
                                                                txnDefs.TXN_CODE_QUANTITY_ADJ_ITEM);
                        }
                    }

            }

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean addNewOptions4ItemCode(   EntityManager pem, 
                                                    BigInteger    pAccId,
                                                    BigInteger    pVendorId,
                                                    String        psItemCode,
                                                    String        psNewOptions//"[{"BLACK":{"B":-1}},{"GREEN":{"C":-1}}]"
                                                  ) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_CREATE_NEW_OPTION_STATS");
            SP.registerStoredProcedureParameter("P_ACC_ID"                              , BigInteger.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"                           , BigInteger.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_ITEM_CODE_ID"                        , BigInteger.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_ITEM_CODE"                           , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_GROUP"                               , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_JS_MERGED_OPTIONS_ENTERED"           , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_JS_MERGED_OPTIONS_RETURNED"          , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_JS_MERGED_OPTIONS_CR_SOLD"           , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_JS_MERGED_OPTIONS_CR_REFUND"         , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_JS_MERGED_OPTIONS_Q_ADJ_PLUS"        , String.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_JS_MERGED_OPTIONS_Q_ADJ_MINUS"       , String.class         , ParameterMode.IN);

            JsonArray jsaNewOptions = Util.JSON.toArray(psNewOptions);
            if(jsaNewOptions!=null)
            {
                for (int j=0; j<jsaNewOptions.size();j++)
                {
                    //ssoInvChangeItem changeN = new ssoInvChangeItem();

                    JsonObject jsGroupN = Util.JSON.toJsonObject(jsaNewOptions.get(j).toString());//{"BLACK":{"B":-1}}

                    Set<String> aGroups = Util.JSON.keys(jsGroupN);
                    for(String sGroup:aGroups)
                    {
                        //String sOptN = Util.JSON.getValue(jsGroupN, sGroup);
                        String sGroupOpts = jsGroupN.getAsJsonObject(sGroup).toString();
                        
                        int Colindex = 1;
                        SP.SetParameter(Colindex++, pAccId                  , "P_ACC_ID");
                        SP.SetParameter(Colindex++, pVendorId               , "P_VENDOR_ID");
                        SP.SetParameter(Colindex++, 0                       , "P_ITEM_CODE_ID");
                        SP.SetParameter(Colindex++, psItemCode              , "P_ITEM_CODE");
                        SP.SetParameter(Colindex++, sGroup                  , "P_GROUP");
                        SP.SetParameter(Colindex++, sGroupOpts              , "P_JS_MERGED_OPTIONS_ENTERED");//{"A": 3.0, "B": 2.0}
                        SP.SetParameter(Colindex++, "{}"                    , "P_JS_MERGED_OPTIONS_RETURNED");
                        SP.SetParameter(Colindex++, "{}"                    , "P_JS_MERGED_OPTIONS_CR_SOLD");
                        SP.SetParameter(Colindex++, "{}"                    , "P_JS_MERGED_OPTIONS_CR_REFUND");
                        SP.SetParameter(Colindex++, "{}"                    , "P_JS_MERGED_OPTIONS_Q_ADJ_PLUS");
                        SP.SetParameter(Colindex++, "{}"                    , "P_JS_MERGED_OPTIONS_Q_ADJ_MINUS");

                        SP.addBatch();
                    }

                }
                
                int[] AffectedRows = SP.executeBatch();
                
                AffectedRows = AffectedRows;

            }

            return true;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean updateInventory4Account_Options(  EntityManager pem, 
                                                            BigInteger    pUserAccountId, 
                                                            String        psChanges
                                                          ) throws Exception
    {
        try
        {
            ArrayList<ssoInvChangeItem> aChanges = new ArrayList<ssoInvChangeItem>();

            boolean rc = false;
            aChanges = collectChanges(psChanges, pUserAccountId);
            if (aChanges.size()>0)
            {
                BigInteger lAccId      = aChanges.get(0).AccId;
                BigInteger lVendorId   = aChanges.get(0).BrandId;
                
                // Collect Sys Latest Values for Changes from UI
                updateSysValuesOnChanges(pem, aChanges, pUserAccountId, lVendorId);
                
                // Save Txn
                //rc = saveInvUpdateTxn(pem, lAccId, lVendorId, psChanges);
                //if(rc==true)
                //{
                    rc = updateItemStats2Changes(pem, pUserAccountId, aChanges);
                    if (rc==true)
                    {
                        //Update Option Stats 
                        rc = updateOptionStats2Changes(pem, pUserAccountId, aChanges);
                        if (rc==true)
                        {
                            BigDecimal bdChangeNumber = new BigDecimal(BigInteger.ZERO);
                            bdChangeNumber = calcItemTotalChangeNumber(aChanges);
                            
                            // Prepare Fixed Changes
                            String sChangesFixed = applyFixedChanges(aChanges);
                            
                            String sItemCode = aChanges.get(0).ItemCode;

                            if(bdChangeNumber.compareTo(BigDecimal.ZERO)!=0)//IF ITEM QUANTITY CHANGED
                            {
                                rc = updateVendorStats2Changes( pem, 
                                                                pUserAccountId, 
                                                                lVendorId, 
                                                                bdChangeNumber);
                            }

                            if(rc==true)
                            {
                                resetMemoryTables(pem, pUserAccountId, aChanges);

                                BigInteger lUID = saveInvUpdateTxn( pem, 
                                                                    lAccId, 
                                                                    lVendorId, 
                                                                    bdChangeNumber, 
                                                                    psChanges,
                                                                    sChangesFixed,
                                                                    txnDefs.TXN_CODE_QUANTITY_ADJ_OPTION);

                                //Log Transaction 
                                /*
                                logInventoryAdjTransaction( pem, 
                                                            pUserAccountId, 
                                                            lAccId,
                                                            lVendorId,
                                                            sItemCode,
                                                            "EA",
                                                            bdChangeNumber,
                                                            psChanges );
                                */
                            }

                        }
                    }
                //}
            }

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    // Example;
    // [
    //   {"A":38482645,"V":46862346,"IC":"A001","OI":0,"OG":"","OC":"","QO":-4,"QN":2,"U":"ea"},
    //   {"A":38482645,"V":46862346,"IC":"A002","OI":0,"OG":"","OC":"","QO":-3,"QN":4,"U":"ea"},
    //   {"A":38482645,"V":46862346,"IC":"A001","OI":46862576,"OG":"","OC":"","QO":2,"QN":3,"U":"ea"},{"A":38482645,"V":46862346,"IC":"A002","OI":46862577,"OG":"","OC":"","QO":5,"QN":6,"U":"ea"},{"A":38482645,"V":46862346,"IC":"A002","OI":46862577,"OG":"","OC":"M","QO":4,"QN":5,"U":"ea"}]
    public static String applyFixedChanges(ArrayList<ssoInvChangeItem> paChanges)
    {
        ArrayList<String> aChanges = new ArrayList<String>();
        
        String sChanges = "[";
        int index = 0;
        for(ssoInvChangeItem changeN: paChanges)
        {
            if(changeN.bIgnore==false)
            {
                if(index!=0)
                    sChanges += ",";

                sChanges +=    "{" +
                                    Util.Str.QUOTE("A") + ":" + changeN.AccId + "," +   //AccountId
                                    Util.Str.QUOTE("V") + ":" + changeN.BrandId + "," +   //VendorId
                                    Util.Str.QUOTE("IC") + ":" + Util.Str.QUOTE(changeN.ItemCode) + "," +  //ItemCode
                                    Util.Str.QUOTE("OI") + ":" + changeN.OptId + "," +  //OptionId
                                    Util.Str.QUOTE("OG") + ":" + Util.Str.QUOTE(changeN.OptGroup) + "," +  //Option Group
                                    Util.Str.QUOTE("OC") + ":" + Util.Str.QUOTE(changeN.OptCode) + "," +  //Quantity Code
                                    Util.Str.QUOTE("QO") + ":" + changeN.oldValSys + "," +  //Quantity Old
                                    Util.Str.QUOTE("QN") + ":" + changeN.newVal + "," +  //Quantity New
                                    Util.Str.QUOTE("U") + ":" + Util.Str.QUOTE("ea") +           //Unit
                                    "}";

                //aChanges.add(sChange);
                index++;
            }
        }
        sChanges += "]";

        //String sChanges = Util.JSON.toString(aChanges);

        return sChanges;
    }

    public static void resetMemoryTables(EntityManager               pem, 
                                         BigInteger                  pUserAccountId, 
                                         ArrayList<ssoInvChangeItem> paChanges)
    {
        ArrayList<String> aListAccNBrandIds = new ArrayList<String>();
        aListAccNBrandIds = getDistinctAccNBrandIdList(paChanges);
        for (String PairN: aListAccNBrandIds)
        {
            String [] aPairParts = PairN.split("-");
            BigInteger lAccId    = new BigInteger(aPairParts[0]);
            BigInteger lVendorId = new BigInteger(aPairParts[1]);

            //Reset Memory Related Tables 
            resetMemoryTables(pem, pUserAccountId, lAccId, lVendorId);

        }

    }

    // Put txn with the same ItemCode under the same group 
    public static BigDecimal calcItemTotalChangeNumber(ArrayList<ssoInvChangeItem> paChanges)
    {
        BigDecimal totOldValUI  = new BigDecimal(BigInteger.ZERO);
        BigDecimal totOldValSys = new BigDecimal(BigInteger.ZERO);
        BigDecimal totNewVal    = new BigDecimal(BigInteger.ZERO);

        ssoInvChangeItem change = new ssoInvChangeItem();

        change.ItemCode = paChanges.get(0).ItemCode;
        change.BrandId  = paChanges.get(0).BrandId;
        change.AccId    = paChanges.get(0).AccId;        

        for (ssoInvChangeItem changeN: paChanges)
        {
            if(changeN.bIgnore==true)
                continue;

            if(changeN.OptId.compareTo(BigInteger.ZERO)==0)//IF ITEM QUANTITY CHANGED 
            {
                totOldValUI  = totOldValSys.add(changeN.oldValUI);
                totOldValSys = totOldValSys.add(changeN.oldValSys);
                totNewVal    = totNewVal.add(changeN.newVal);
            }
        }

        change.newVal    = totNewVal;
        change.oldValSys = totOldValSys;
        change.oldValUI  = totOldValUI;

        return totNewVal.subtract(totOldValSys);
    }

    /*
    public static long logInventoryAdjTransaction(EntityManager pem, 
                                                  long          pUserAccountId, 
                                                  long          pItemAccId,
                                                  long          pVendorId,
                                                  String        psItemCode,
                                                  String        psUnit,
                                                  BigDecimal    pbdQChangeNumber,
                                                  String        psChanges
                                                  ) throws Exception
    {
        try
        {
            SsTxnInvQuantityAdj txnAdj = new SsTxnInvQuantityAdj();

            txnAdj.txnType               = txnDefs.TXN_TYPE_QUANTITY_ADJ;
            txnAdj.txnCode               = txnDefs.TXN_CODE_QUANTITY_ADJ;
            txnAdj.accountId             = pItemAccId;
            txnAdj.vendorId              = pVendorId;
            //txnAdj.itemCode              = psItemCode;
            txnAdj.itemTotQuantityChange = pbdQChangeNumber;
            txnAdj.optionsQuantityChange = psChanges;

            long lUID = pem.persist(txnAdj);

            return lUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    */
    public static ArrayList<String> getDistinctAccNBrandIdList(ArrayList<ssoInvChangeItem> paChanges)
    {
        ArrayList<String> lstAccNBrands = new ArrayList<String>();

        for (ssoInvChangeItem itemN: paChanges)
        {
            BigInteger sAccId    = itemN.AccId;
            BigInteger sVendorId = itemN.BrandId;
            
            String sAccNVendor = sAccId + "-" + sVendorId;
            boolean bFound = false;
            for (String ValN: lstAccNBrands)
            {
                if (ValN.equals(sAccNVendor)==true)
                {
                    bFound = true;
                    break;
                }
            }
            
            if (bFound==false)
                lstAccNBrands.add(sAccNVendor);
            
        }

        return lstAccNBrands;
    }

    public static void resetMemoryTables(EntityManager pem, BigInteger pUserId, BigInteger pAccId, BigInteger pVendorId)
    {
        try
        {
            // ss_acc_inv_item_stats
            //--------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys1 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey Col1 = new ssoCacheSplitKey();
            Col1.column = "ACCOUNT_ID";
            Col1.value  = pAccId;
            keys1.add(Col1);

            ssoCacheSplitKey Col2 = new ssoCacheSplitKey();
            Col2.column = "VENDOR_ID";
            Col2.value  = pVendorId;
            keys1.add(Col2);
            pem.flush(SsAccInvItemStats.class, keys1);


            // ss_acc_inv_option_stats
            //--------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys2 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey ColA = new ssoCacheSplitKey();
            ColA.column = "ACCOUNT_ID";
            ColA.value  = pAccId;
            keys2.add(Col1);

            ssoCacheSplitKey ColB = new ssoCacheSplitKey();
            ColB.column = "VENDOR_ID";
            ColB.value  = pVendorId;
            keys2.add(ColB);
            pem.flush(SsAccInvOptionStats.class, keys2);

        }
        catch(Exception e)
        {
            String s = "";
            s = e.getMessage();
        }
    }

    public static boolean updateOptionStats2Changes(EntityManager               pem, 
                                                    BigInteger                  pUserAccountId, 
                                                    ArrayList<ssoInvChangeItem> paChanges) throws Exception
    {

        ArrayList<ssoQueryInvUpdateOption> aUpdQueries = new ArrayList<ssoQueryInvUpdateOption>();
        //ArrayList<ssoUpdateResultQuantityOption> aUpdResults = new ArrayList<ssoUpdateResultQuantityOption>();

        try
        {
            // Get All Current Values On System 

            int i=0;
            for (ssoInvChangeItem changeN:paChanges)
            {
                if(changeN.bIgnore==true)
                    continue;//value on sys and on coming (new) same / equal so skip
                
                if(changeN.OptId.compareTo(BigInteger.ZERO)>0)//if change is related to OPTION
                {
                    // Prep Update Query 
                    //---------------------------------------------------------
                    ssoQueryInvUpdateOption updQuery = new ssoQueryInvUpdateOption();

                    updQuery = prepareUpdateQuery4OptionStats(  pUserAccountId,
                                                                changeN.AccId, 
                                                                changeN.BrandId, 
                                                                changeN.ItemCode,
                                                                changeN.OptId,
                                                                changeN.OptGroup,
                                                                changeN.OptCode,
                                                                changeN.oldValSys,
                                                                changeN.newVal);

                    i++;
                    aUpdQueries.add(updQuery);
                    
                    //aUpdResults.stat = "0";// Not Valid
                    //aUpdResults
                }
            }

            if(aUpdQueries.size()>0)
            {
                String sInitQuery = aUpdQueries.get(0).query;

                Query stmtQry = pem.CreateNativeQuery(sInitQuery);
                //stmtQry.addBatch();

                for(int j=0; j<aUpdQueries.size(); j++)
                {
                    ssoQueryInvUpdateOption updN = new ssoQueryInvUpdateOption();
                    
                    updN = aUpdQueries.get(j);

                    int Colindex = 1;

                    
                    stmtQry.SetParameter(Colindex++, updN.optCode                         , "OPT_CODE"); 
                    stmtQry.SetParameter(Colindex++, updN.optUID                          , "OPT_UID"); 
                    stmtQry.SetParameter(Colindex++, updN.userAccId                        , "ACC_ID");
                    stmtQry.SetParameter(Colindex++, updN.brandId                         , "VENDOR_ID"); 
                    // SET BY USER
                    stmtQry.SetParameter(Colindex++, updN.chgAccId                        , "ACC_ID");
                    // SET ADJ PLUS
                    stmtQry.SetParameter(Colindex++, updN.newVal                          , "OPT_NEW_VAL"); 
                    stmtQry.SetParameter(Colindex++, updN.optCode                         , "OPT_CODE"); 
                    stmtQry.SetParameter(Colindex++, updN.newVal                          , "OPT_NEW_VAL"); 
                    // SET ADJ MINUS
                    stmtQry.SetParameter(Colindex++, updN.newVal                          , "OPT_NEW_VAL"); 
                    stmtQry.SetParameter(Colindex++, updN.optCode                         , "OPT_CODE"); 
                    stmtQry.SetParameter(Colindex++, updN.newVal                          , "OPT_NEW_VAL"); 
                    // WHERE
                    stmtQry.SetParameter(Colindex++, updN.newVal                         , "OPT_NEW_VAL"); 
                    
                    stmtQry.addBatch();
                    //stmtQry.addBatch(aUpdQueries.get(j));
                }

                int [] iAffectedRowNum = stmtQry.executeBatch();
                
                int ix = 0;
            }

            return true;            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoQueryInvUpdateOption prepareUpdateQuery4OptionStats(BigInteger       pUserAccId,
                                                                        BigInteger       pChgAccId, 
                                                                        BigInteger       pChgBrandId, 
                                                                        String           pChgItemCode, 
                                                                        BigInteger       pChgOptionId, 
                                                                        String           pOptionGroup,
                                                                        String           pOptionCode,
                                                                        BigDecimal       pChgOldVal, 
                                                                        BigDecimal       pChgNewVal)
    {
        ssoQueryInvUpdateOption updQuery = new ssoQueryInvUpdateOption();
        
        String sQuery     = "";
        String sInvEffect = "";

        // Siyah-38 => Group = Siyah + Code = 38
        String sOptGroup = "";
        String sOptCode  = "";

        sOptGroup = pOptionGroup;
        sOptCode  = pOptionCode;

        BigDecimal bdDiff = new BigDecimal(BigInteger.ZERO);

        bdDiff = pChgNewVal.subtract(pChgOldVal);

        if (pChgOldVal.compareTo(pChgNewVal) > 0)
            sInvEffect = "M"; //Deducted / Minus
        else
            sInvEffect = "P"; //Added / Plus

        
        
        //String sJSNetOptCalcStmt = "FN_INV_CALC_NET_QUANTITY_4_OPTION_N_CORE(IFNULL(OPTIONS_ENTERED,'{}'), IFNULL(OPTIONS_RETURNED,'{}'), IFNULL(OPTIONS_CR_SOLD,'{}'), IFNULL(OPTIONS_CR_REFUND,'{}'), IFNULL(OPTIONS_DN_SOLD,'{}'), IFNULL(OPTIONS_DN_REFUND,'{}'), IFNULL(OPTIONS_ADJ_PLUS,'{}'), IFNULL(OPTIONS_ADJ_MINUS,'{}'), IFNULL(LAST_EOD_OPTIONS_ENTERED,'{}'), IFNULL(LAST_EOD_OPTIONS_RETURNED,'{}'), IFNULL(LAST_EOD_OPTIONS_CR_SOLD,'{}'), IFNULL(LAST_EOD_OPTIONS_CR_REFUND,'{}'), IFNULL(LAST_EOD_OPTIONS_DN_SOLD,'{}'), IFNULL(LAST_EOD_OPTIONS_DN_REFUND,'{}'), IFNULL(LAST_EOD_OPTIONS_ADJ_PLUS,'{}'), IFNULL(LAST_EOD_OPTIONS_ADJ_MINUS,'{}'))";
        //String sExtractOptNStmt = "IFNULL(JSON_EXTRACT" + "(" + sJSNetOptCalcStmt + "," + "$" + "." + "'" + "?" + "'" + "), 0)"; // ?[1]: OPTCODE - ?[2]: New Value (CHECKS IF VALUE COMING DIFFERENT THAN CURRENT)
        
        sQuery = "UPDATE ss_acc_inv_option_stats T1 " + 
                 "JOIN " + 
                 "(" +
                    "SELECT " + 
                       "UID," + 
                       //@OPT_CODE 
                       "IFNULL(JSON_EXTRACT(FN_INV_CALC_NET_QUANTITY_4_OPTION_N_CORE(IFNULL(OPTIONS_ENTERED,'{}'), IFNULL(OPTIONS_RETURNED,'{}'), IFNULL(OPTIONS_CR_SOLD,'{}'), IFNULL(OPTIONS_CR_REFUND,'{}'), IFNULL(OPTIONS_DN_SOLD,'{}'), IFNULL(OPTIONS_DN_REFUND,'{}'), IFNULL(OPTIONS_ADJ_PLUS,'{}'), IFNULL(OPTIONS_ADJ_MINUS,'{}'), IFNULL(LAST_EOD_OPTIONS_ENTERED,'{}'), IFNULL(LAST_EOD_OPTIONS_RETURNED,'{}'), IFNULL(LAST_EOD_OPTIONS_CR_SOLD,'{}'), IFNULL(LAST_EOD_OPTIONS_CR_REFUND,'{}'), IFNULL(LAST_EOD_OPTIONS_DN_SOLD,'{}'), IFNULL(LAST_EOD_OPTIONS_DN_REFUND,'{}'), IFNULL(LAST_EOD_OPTIONS_ADJ_PLUS,'{}'), IFNULL(LAST_EOD_OPTIONS_ADJ_MINUS,'{}')), CONCAT('$.\"', ?, '\"')), 0) AS CUR_OPT_VAL " +
                    "FROM ss_acc_inv_option_stats " +
                    "WHERE " + 
                    // OPT UID
                    "UID = ? " +
                    "AND " + 
                    // ACC ID
                    "ACCOUNT_ID = ? " + 
                    "AND " +
                    // VENDOR ID
                    "VENDOR_ID = ? " +
                ") " +
                "T2 ON T1.UID = T2.UID " +
                "SET  " +
                "LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s000') AS UNSIGNED INTEGER) , " +
                // USER
                "BYUSER = ?, " +
                "OPTIONS_ADJ_PLUS = IF( " +
                                        // OPT NEW VAL
                                        "IF(T2.CUR_OPT_VAL<0, 0, T2.CUR_OPT_VAL) < ?, " +
                                        // OPT CODE + OPT NEW VAL
                                        "INV_JSON_MERGE(IF(OPTIONS_ADJ_PLUS='','{}', OPTIONS_ADJ_PLUS), JSON_OBJECT( ?, ? - IF(T2.CUR_OPT_VAL<0,0,T2.CUR_OPT_VAL) ), '+'), " +
                                        "OPTIONS_ADJ_PLUS " +
                                      "), " + 
                "OPTIONS_ADJ_MINUS = IF(" + 
                                        // OPT NEW VAL 
                                        "IF(T2.CUR_OPT_VAL<0, 0, T2.CUR_OPT_VAL) > ?, " + 
                                        // OPT CODE + OPT NEW VAL 
                                        "INV_JSON_MERGE(IF(OPTIONS_ADJ_MINUS='','{}', OPTIONS_ADJ_MINUS), JSON_OBJECT( ?, IF(T2.CUR_OPT_VAL<0,0,T2.CUR_OPT_VAL) - ? ), '+'), " +
                                        "OPTIONS_ADJ_MINUS" + 
                                       ") " + 
                "WHERE " + 
                //"UID" // UID UP THERE 
                //OPT NEW VAL
                "IF(T2.CUR_OPT_VAL<0,0,T2.CUR_OPT_VAL) <> ? " +
                "AND " +
                "STAT = 1";

        updQuery.query      = sQuery;
        updQuery.userAccId  = pUserAccId;
        updQuery.chgAccId   = pChgAccId;
        updQuery.brandId    = pChgBrandId;

        updQuery.optUID     = pChgOptionId;
        updQuery.optGroup   = pOptionGroup;
        updQuery.optCode    = pOptionCode;

        updQuery.newVal     = pChgNewVal;
        updQuery.oldVal     = pChgOldVal;
        updQuery.diffVal    = bdDiff;

        /*
        sQuery = "UPDATE ss_acc_inv_option_stats " +
                 " SET " + 
                 " LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s000') AS UNSIGNED INTEGER) ," + 
                 " BYUSER = " + Util.Str.SQUOTE(pUserAccId.toString()) + ",";

        if(sOptCode.equals("\"\"")==true)
            sOptCode = "";

        //if(sInvEffect.equals("P")==true)
        //{

            //String sOptUpdate = "{" + Util.Str.QUOTE(sOptCode) + ":" + bdDiff.abs().toString() + "}";
            String sOptUpdate = "{" + Util.Str.QUOTE(sOptCode) + ":" + "?" + "}";//? = NEW VALUE

            //sQuery += "OPTIONS_ADJ_PLUS = INV_JSON_MERGE(IF(OPTIONS_ADJ_PLUS='','{}', OPTIONS_ADJ_PLUS),'" + sOptUpdate + "', '+') ";

            // UPDATE IF CURRENT VALUE SMALLER THAN COMING VALUE OTHERWISE KEEP AS IS
            sQuery += "OPTIONS_ADJ_PLUS = IF(" + sExtractOptNStmt + " < " + "?" + "," + "INV_JSON_MERGE(IF(OPTIONS_ADJ_PLUS='','{}', OPTIONS_ADJ_PLUS),'" + sOptUpdate + "', '+'), OPTIONS_ADJ_PLUS), ";


        //}
        //else
        //{
            //String sOptUpdate = "{" + Util.Str.QUOTE(sOptCode) + ":" + bdDiff.abs().toString() + "}";

            //sQuery += "OPTIONS_ADJ_MINUS = INV_JSON_MERGE(IF(OPTIONS_ADJ_MINUS='','{}', OPTIONS_ADJ_MINUS),'" + sOptUpdate + "', '+') ";

            // UPDATE IF CURRENT VALUE BIGGER THAN COMING VALUE OTHERWISE KEEP AS IS
            sQuery += "OPTIONS_ADJ_MINUS = IF(" + sExtractOptNStmt + " > " + "?" + "," + "INV_JSON_MERGE(IF(OPTIONS_ADJ_MINUS='','{}', OPTIONS_ADJ_MINUS),'" + sOptUpdate + "', '+'), OPTIONS_ADJ_MINUS) ";


        //}

        //if (sOptGroup.trim().length()==0)//REMOVED 25.08.2024 EB
        //    sOptGroup = "''";

        sQuery +=" WHERE " + 
                    " UID = " + pChgOptionId + 
                    " AND " + 
                    " OPTION_GROUP = " + Util.Str.QUOTE(sOptGroup) + 
                    " AND " + 
                    " ACCOUNT_ID = " + pChgAccId +
                    " AND " +
                    " VENDOR_ID = " + pChgBrandId +
                    " AND " + 
                    //" ITEM_CODE = " + pChgItemCode+//ITEM CODE NOT EXIST IN OPTIONS TABLE 
                    sExtractOptNStmt + "<>" + "?" + // ? = new value coming
                    " AND " + 
                    " STAT = 1";
        */

        return updQuery;
    }

    public static boolean updateVendorStats2Changes(EntityManager   pem,
                                                    BigInteger            pAccountId,
                                                    BigInteger            pVendorId,
                                                    BigDecimal      pbdQuantityChangeVal) throws Exception
    {
        String sStmt = "UPDATE ss_acc_inv_vendor_stats SET ";
        try
        {
            if (pbdQuantityChangeVal.compareTo(BigDecimal.ZERO) < 0)
            {
                sStmt += " QUANTITY_ADJ_MINUS = QUANTITY_ADJ_MINUS + ? ";
            }
            else if (pbdQuantityChangeVal.compareTo(BigDecimal.ZERO) > 0)
            {
                sStmt += " QUANTITY_ADJ_PLUS = QUANTITY_ADJ_PLUS + ? ";
            }
            else
                return true;// no change no need update
            
            sStmt += " WHERE ";
            sStmt += "  STAT = 1 ";
            sStmt += "  AND ";
            sStmt += "  ACCOUNT_ID = ? ";
            sStmt += "  AND ";
            sStmt += "  VENDOR_ID =? ";
            
            Query qry = pem.CreateNativeQuery(sStmt);

            int index = 1;
            qry.SetParameter(index++, pbdQuantityChangeVal.abs()  , "QUANTITY");
            qry.SetParameter(index++, pAccountId            , "P_ACCOUNT_ID");
            qry.SetParameter(index++, pVendorId             , "P_VENDOR_ID");
            
            long lAffectedRow = qry.executeUpdate();
            
            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static BigInteger saveInvUpdateTxn(  EntityManager         pem, 
                                                BigInteger                  pAccountId,
                                                BigInteger                  pVendorId,
                                                BigDecimal                  pbdItemTotalQuantityChange,
                                                String                      psChanges,
                                                String                      psChangesFixed,
                                                String                      psTxnCode) throws Exception
    {
        SsTxnInvQuantityAdj txnQAdj = new SsTxnInvQuantityAdj();
        
        txnQAdj.accountId = pAccountId;
        txnQAdj.vendorId  = pVendorId;

        txnQAdj.txnType   = txnDefs.TXN_TYPE_QUANTITY_ADJ;
        txnQAdj.txnCode   = psTxnCode;//txnDefs.TXN_CODE_QUANTITY_ADJ_OPTION;
        txnQAdj.itemTotQuantityChange = pbdItemTotalQuantityChange;
        txnQAdj.optionsQuantityChangeUi  = psChanges;
        txnQAdj.optionsQuantityChangeSys = psChangesFixed;

        BigInteger lUID = pem.persist(txnQAdj);

        return lUID;
        /*
        ArrayList<SsTxnInvQUpdates> txnUpdates = new ArrayList<SsTxnInvQUpdates>();

        try
        {
            for(ssoInvChangeItem updN: paChanges)
            {
                SsTxnInvQUpdates txnN = new SsTxnInvQUpdates();

                txnN.accountId    = updN.AccId;
                txnN.vendorId     = updN.BrandId;
                txnN.newQuantity  = updN.newVal;
                txnN.oldQuantity  = updN.oldVal;
                txnN.txnCode      = txnDefs.TXN_CODE_INVENTORY_UPDATE;

                txnUpdates.add(txnN);
            }
            
            int [] iTxnUIDs = pem.persistAll(paChanges, false);
            
            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
        */
    }

    public static boolean updateItemStats2Changes(EntityManager               pem, 
                                                  BigInteger                        pUserAccountId,
                                                  ArrayList<ssoInvChangeItem> paChanges) throws Exception
    {
        
        ArrayList<ssoQueryInvUpdateItem> aUpdQueries = new ArrayList<ssoQueryInvUpdateItem>();

        try
        {

            int i=0;
            for (ssoInvChangeItem changeN:paChanges)
            {
                if(changeN.bIgnore==true)
                    continue;//value on sys and coming are same
                
                if(changeN.OptId.compareTo(BigInteger.ZERO)<=0)//if change is related to ITEM NO OPT ID
                {
                    // Prep Update Query 
                    //---------------------------------------------------------
                    ssoQueryInvUpdateItem qUpd = new ssoQueryInvUpdateItem();

                    qUpd = prepareUpdateQuery4ItemStats(pUserAccountId,
                                                        changeN.AccId, 
                                                        changeN.BrandId, 
                                                        changeN.ItemCode.replaceAll("\"", ""),
                                                        changeN.oldValSys,
                                                        changeN.newVal);

                    i++;
                    aUpdQueries.add(qUpd);
                }
            }

            if(aUpdQueries.size()>0)
            {
                String sInitQuery = aUpdQueries.get(0).query;

                Query stmtQry = pem.CreateNativeQuery(sInitQuery);
                //stmtQry.addBatch();

                for(int j=0; j<aUpdQueries.size(); j++)
                {
                    ssoQueryInvUpdateItem updN = new ssoQueryInvUpdateItem();

                    updN = aUpdQueries.get(j);

                    int Colindex = 1;
                    stmtQry.SetParameter(Colindex++, updN.userAccId                         , "USER_ID"); //SURCHARGE_TOTAL++
                    //stmtQry.SetParameter(Colindex++, updN.diffVal                           , "DIFFERENCE"); //SURCHARGE_TOTAL++

                    stmtQry.SetParameter(Colindex++, updN.newVal                            , "NEW_VAL"); //SURCHARGE_TOTAL++
                    stmtQry.SetParameter(Colindex++, updN.newVal                            , "NEW_VAL"); //SURCHARGE_TOTAL++
                    stmtQry.SetParameter(Colindex++, updN.newVal                            , "NEW_VAL"); //SURCHARGE_TOTAL++
                    stmtQry.SetParameter(Colindex++, updN.newVal                            , "NEW_VAL"); //SURCHARGE_TOTAL++
                    
                    stmtQry.SetParameter(Colindex++, updN.chgAccId                          , "CHG_ACC_ID"); //SURCHARGE_TOTAL++
                    stmtQry.SetParameter(Colindex++, updN.brandId                           , "VENDOR_ID"); //SURCHARGE_TOTAL++
                    stmtQry.SetParameter(Colindex++, updN.itemCode                          , "ITEM_CODE"); //SURCHARGE_TOTAL++
                    stmtQry.SetParameter(Colindex++, updN.newVal                            , "NEW_VAL"); //SURCHARGE_TOTAL++
                    stmtQry.addBatch();
                }

                int [] iAffectedRowNum = stmtQry.executeBatch();

                int ix = 0;
            }

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoQueryInvUpdateItem prepareUpdateQuery4ItemStats(   BigInteger       pUserAccId,
                                                                    BigInteger       pChgAccId, 
                                                                    BigInteger       pChgBrandId, 
                                                                    String           pChgItemCode,
                                                                    BigDecimal       pChgOldVal, 
                                                                    BigDecimal       pChgNewVal)
    {
        ssoQueryInvUpdateItem qUpd = new ssoQueryInvUpdateItem();

        String sQuery     = "";
        String sInvEffect = "";

        BigDecimal bdDiff = new BigDecimal(BigInteger.ZERO);

        bdDiff = pChgNewVal.subtract(pChgOldVal);

        if (pChgOldVal.compareTo(pChgNewVal) > 0) // IF OLD VALUE IS LARGER 
            sInvEffect = "M"; //Deducted / Minus
        else
            sInvEffect = "P"; //Added / Plus

        /*
        sQuery = "UPDATE ss_acc_inv_item_stats " +
                 " SET " + 
                 " LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s000') AS UNSIGNED INTEGER) ," + 
                 " BYUSER = " + Util.Str.SQUOTE(pUserAccId.toString()) + ",";

        if(sInvEffect.equals("P")==true)
            sQuery += " QUANTITY_ADJ_PLUS  = QUANTITY_ADJ_PLUS + " + bdDiff.abs().toString() ;
        else
            sQuery += " QUANTITY_ADJ_MINUS = QUANTITY_ADJ_MINUS +" + bdDiff.abs().toString() ;

        sQuery +=" WHERE " + 
                 " ACCOUNT_ID = " + pChgAccId +
                 " AND " +
                 " VENDOR_ID = " + pChgBrandId +
                 " AND " +
                 " ITEM_CODE = " + Util.Str.SQUOTE(pChgItemCode) +
                 " AND " + 
                 " TOTAL_NET_QUANTITY_INV <> " + pChgNewVal.toString() +
                 " AND " + 
                 " ITEM_CODE = " + Util.Str.SQUOTE(pChgItemCode)+
                 " AND " + 
                 " STAT = 1";
        */
        sQuery = "UPDATE ss_acc_inv_item_stats " +
                 " SET " + 
                 " LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s000') AS UNSIGNED INTEGER) ," + 
                 " BYUSER = ? ,";

        //if(sInvEffect.equals("P")==true)//? = new value
            sQuery += " QUANTITY_ADJ_PLUS  = IF(TOTAL_NET_QUANTITY_INV < ?, QUANTITY_ADJ_PLUS  + (? - TOTAL_NET_QUANTITY_INV), QUANTITY_ADJ_PLUS), ";//NEW VAL LARGER: update as difference betwen new and current value (dont use the coming one it might be old)
        //else
            sQuery += " QUANTITY_ADJ_MINUS = IF(TOTAL_NET_QUANTITY_INV > ?, QUANTITY_ADJ_MINUS + (TOTAL_NET_QUANTITY_INV - ?), QUANTITY_ADJ_MINUS) ";//OLD VAL LARGER: update as difference betwen new and current value (dont use the coming one it might be old)

        sQuery +=" WHERE " + 
                 " ACCOUNT_ID = ? " +
                 " AND " +
                 " VENDOR_ID = ? " +
                 " AND " +
                 " ITEM_CODE = ? " +
                 " AND " + 
                 " TOTAL_NET_QUANTITY_INV <> ? " + 
                 " AND " + 
                 " STAT = 1";

        qUpd.query      = sQuery;
        qUpd.userAccId  = pUserAccId;
        qUpd.chgAccId   = pChgAccId;
        qUpd.brandId    = pChgBrandId;
        qUpd.itemCode   = pChgItemCode;
        qUpd.newVal     = pChgNewVal;
        qUpd.oldVal     = pChgOldVal;
        qUpd.diffVal    = bdDiff;

        return qUpd;
    }

    public static ArrayList<ssoInvChangeItem> collectChanges(String psChanges, BigInteger pUserAccountId) throws Exception
    {
        ArrayList<ssoInvChangeItem> changes = new ArrayList<ssoInvChangeItem>();

        try
        {
            // STEP 1: COLLECT CHANGES FROM UI
            //------------------------------------------------------------------
            JsonArray jsAllChanges = Util.JSON.toArray(psChanges);
            if(jsAllChanges!=null)
            {
                for (int j=0; j<jsAllChanges.size();j++)
                {
                    ssoInvChangeItem changeN = new ssoInvChangeItem();

                    JsonObject jsChangeN = Util.JSON.toJsonObject(jsAllChanges.get(j).toString());

                    Set<String> aKeys = Util.JSON.keys(jsChangeN);
                    for(String sKey:aKeys)
                    {
                        String sVal = Util.JSON.getValue(jsChangeN, sKey);

                        if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_ACC_ID)==true)
                        {
                            changeN.AccId = new BigInteger(sVal);
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_BRAND_ID)==true)
                        {
                            changeN.BrandId = new BigInteger(sVal);
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_ITM_CODE)==true)
                        {
                            changeN.ItemCode = sVal.trim();
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_ID)==true)
                        {
                            changeN.OptId = BigInteger.valueOf(-1);
                            if(sVal.trim().length()>0)
                                changeN.OptId = new BigInteger(sVal);
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_GROUP)==true)
                        {
                            changeN.OptGroup = sVal.trim();
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_CODE)==true)
                        {
                            changeN.OptCode = sVal.trim();
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_OLD_VAL)==true)
                        {
                            changeN.oldValUI = new BigDecimal(sVal);
                        }
                        else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_NEW_VAL)==true)
                        {
                            changeN.newVal = new BigDecimal(sVal);
                        }

                    }//all the changes thru keys collected 

                    changes.add(changeN);
                }// all the rows
            }
            
            return changes;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void updateSysValuesOnChanges(EntityManager               pem,  
                                                ArrayList<ssoInvChangeItem> paChanges, 
                                                BigInteger                  pUserAccountId, 
                                                BigInteger                  pVendorId) throws Exception
    {
        try
        {
            String sQuery = "SELECT \n" +
                            "	 ITM.ITEM_CODE,\n" +
                            "    ITM.TOTAL_NET_QUANTITY_INV AS ITEM_NET_QUANTITY,\n" +
                            "    OPTION_GROUP AS OPT_GROUP,\n" +
                            "	 OPT.UID AS OPT_UID, \n" +
                            "    FN_INV_CALC_NET_QUANTITY_4_OPTION_N_CORE(IFNULL(OPT.OPTIONS_ENTERED,'{}'), IFNULL(OPT.OPTIONS_RETURNED,'{}'), IFNULL(OPT.OPTIONS_CR_SOLD,'{}'), IFNULL(OPT.OPTIONS_CR_REFUND,'{}'), IFNULL(OPT.OPTIONS_DN_SOLD,'{}'), IFNULL(OPT.OPTIONS_DN_REFUND,'{}'), IFNULL(OPT.OPTIONS_ADJ_PLUS,'{}'), IFNULL(OPT.OPTIONS_ADJ_MINUS,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_ENTERED,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_RETURNED,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_CR_SOLD,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_CR_REFUND,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_DN_SOLD,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_DN_REFUND,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_ADJ_PLUS,'{}'), IFNULL(OPT.LAST_EOD_OPTIONS_ADJ_MINUS,'{}')) AS OPT_NET_QUANTITY \n" +
                            "FROM ss_acc_inv_item_stats ITM \n" +
                            "INNER JOIN ss_acc_inv_option_stats OPT ON ITM.UID = OPT.ITEM_CODE_ID \n" +
                            "WHERE\n" +
                            "ITM.STAT = 1 " +
                            "AND " + 
                            "OPT.STAT = 1 " +
                            "AND " +
                            "ITM.ACCOUNT_ID = ? " +
                            "AND " +
                            "ITM.VENDOR_ID = ? " + 
                            "AND " + 
                            "ITM.ITEM_CODE IN " +
                            "(";

            int index = 0;
            for(ssoInvChangeItem changeN: paChanges)
            {
                if(index!=0)
                    sQuery += ",";

                sQuery += "?";
                
                index++;
            }
            sQuery += ")";

            Query stmtQry = pem.CreateNativeQuery(sQuery);

            int Colindex = 1;
            stmtQry.SetParameter(Colindex++, pUserAccountId   , "ACCOUNT_ID");
            stmtQry.SetParameter(Colindex++, pVendorId        , "VENDOR_ID");

            for(ssoInvChangeItem changeN: paChanges)
            {
                stmtQry.SetParameter(Colindex++, changeN.ItemCode        , "ITEM_CODE");
            }

            boolean bItemCodeChange = false;

            ArrayList<ssoCurVal4Change> curValsOnSys = new ArrayList<ssoCurVal4Change>();
            List<List<RowColumn>> rs = stmtQry.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                List<RowColumn> RowN = rs.get(i);

                ssoCurVal4Change curVal = new ssoCurVal4Change();

                curVal.itemCode        = Util.Database.getValString(RowN, "ITEM_CODE").toString();
                curVal.itemNetQuantity = new BigDecimal(Util.Database.getValString(RowN, "ITEM_NET_QUANTITY").toString());
                curVal.optGroup        = Util.Database.getValString(RowN, "OPT_GROUP").toString();
                curVal.optUID          = new BigInteger(Util.Database.getValString(RowN, "OPT_UID").toString());
                curVal.jsOptCodeQuantities   = Util.Database.getValString(RowN, "OPT_NET_QUANTITY").toString();

                curValsOnSys.add(curVal);

            }

            for(ssoInvChangeItem changeN:paChanges)//search arriving changes in sys currents
            {
                if(changeN.OptId.compareTo(BigInteger.ZERO)<=0)//if item code
                {
                    // change of item => search for ItemCode

                    for(ssoCurVal4Change sysValN:curValsOnSys)
                    {

                        if(sysValN.itemCode.trim().equals(changeN.ItemCode.trim()))
                        {
                            // Value Found
                            if(sysValN.itemNetQuantity.compareTo(BigDecimal.ZERO)>=0)
                                changeN.oldValSys = sysValN.itemNetQuantity;
                            else
                                changeN.oldValSys = BigDecimal.ZERO;

                            if(changeN.oldValSys.compareTo(changeN.newVal)==0)//check if value changed
                            {
                                // VALUE ON SYS NOT CHANGED 
                                changeN.bIgnore = true;
                            }
                            break;
                            
                        }
                    }

                }
                else
                {
                    // change of option => search for OptionId 
                    for(ssoCurVal4Change sysValN:curValsOnSys)
                    {
                        if((sysValN.optUID.compareTo(changeN.OptId))==0)
                        {
                            // Value Found 
                            //changeN.OptCode
                            JSONObject oQuantities = Util.JSON.toObject(sysValN.jsOptCodeQuantities);
                            String sOptQuantity = oQuantities.get(changeN.OptCode).toString();
                            if(sOptQuantity.trim().length()==0)
                                sOptQuantity = "0";

                            changeN.oldValSys = new BigDecimal(sOptQuantity);
                            if(changeN.oldValSys.compareTo(BigDecimal.ZERO)<0)
                                changeN.oldValSys = BigDecimal.ZERO;

                            if(changeN.oldValSys.compareTo(changeN.newVal)==0)//check if value changed
                            {
                                // VALUE ON SYS NOT CHANGED 
                                changeN.bIgnore = true;
                            }
                            break;
                        }
                        
                    }
                }
            }

          
        }
        catch(Exception e)
        {
            throw e;
        }
    }
}
