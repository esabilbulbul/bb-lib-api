/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import bb.app.account.ssoAccInvBalanceCore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvOptionStats;
import entity.txn.SsTxnInvQuantityAdj;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Set;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class InventoryUpdate 
{
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_ACC_ID   = "A";
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_BRAND_ID = "B";
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_ITM_CODE = "I";
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_ID   = "OI";//UI 
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_GROUP= "OG";//Siyah
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_CODE = "OC";//38
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_OLD_VAL  = "OV";
    public static String g_INV_QNTY_CHANGE_ITEM_SIGN_NEW_VAL  = "NV";

    public static boolean updateInventory4Account( EntityManager pem, 
                                                   long          pUserAccountId, 
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
                rc = updateItemStats2Changes(pem, pUserAccountId, aChanges);
                if (rc==true)
                {
                    //Update Option Stats 
                    rc = updateOptionStats2Changes(pem, pUserAccountId, aChanges);
                    if (rc==true)
                    {
                        BigDecimal bdChangeNumber = new BigDecimal(BigInteger.ZERO);
                        bdChangeNumber = calcTotalChangeNumber(aChanges);

                        long lAccId      = aChanges.get(0).AccId;
                        long lVendorId   = aChanges.get(0).BrandId;
                        String sItemCode = aChanges.get(0).ItemCode;

                        rc = updateVendorStats2Changes( pem, 
                                                        pUserAccountId, 
                                                        lVendorId, 
                                                        bdChangeNumber);
                        if(rc==true)
                        {
                            resetMemoryTables(pem, pUserAccountId, aChanges);

                            //Log Transaction
                            logInventoryAdjTransaction( pem, 
                                                        pUserAccountId, 
                                                        lAccId,
                                                        lVendorId,
                                                        sItemCode,
                                                        "EA",
                                                        bdChangeNumber,
                                                        psChanges );
                        }

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
    
    public static void resetMemoryTables(EntityManager               pem, 
                                         long                        pUserAccountId, 
                                         ArrayList<ssoInvChangeItem> paChanges)
    {
        ArrayList<String> aListAccNBrandIds = new ArrayList<String>();
        aListAccNBrandIds = getDistinctAccNBrandIdList(paChanges);
        for (String PairN: aListAccNBrandIds)
        {
            String [] aPairParts = PairN.split("-");
            long lAccId    = Long.parseLong(aPairParts[0]);
            long lVendorId = Long.parseLong(aPairParts[1]);

            //Reset Memory Related Tables 
            resetMemoryTables(pem, pUserAccountId, lAccId, lVendorId);

        }

    }

    // Put txn with the same ItemCode under the same group 
    public static BigDecimal calcTotalChangeNumber(ArrayList<ssoInvChangeItem> paChanges)
    {

        BigDecimal totOldVal = new BigDecimal(BigInteger.ZERO);
        BigDecimal totNewVal = new BigDecimal(BigInteger.ZERO);

        ssoInvChangeItem change = new ssoInvChangeItem();

        change.ItemCode = paChanges.get(0).ItemCode;
        change.BrandId  = paChanges.get(0).BrandId;
        change.AccId    = paChanges.get(0).AccId;        
        
        for (ssoInvChangeItem changeN: paChanges)
        {
            totOldVal = totOldVal.add(changeN.oldVal);
            totNewVal = totNewVal.add(changeN.newVal);
        }

        change.newVal = totNewVal;
        change.oldVal = totOldVal;

        return totNewVal.subtract(totOldVal);
    }

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

            txnAdj.accountId             = pItemAccId;
            txnAdj.vendorId              = pVendorId;
            txnAdj.itemCode              = psItemCode;
            txnAdj.itemAddedQuantity     = pbdQChangeNumber;
            txnAdj.optionsQuantityChange = psChanges;
            
            long lUID = pem.persist(txnAdj);
            
            return lUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<String> getDistinctAccNBrandIdList(ArrayList<ssoInvChangeItem> paChanges)
    {
        ArrayList<String> lstAccNBrands = new ArrayList<String>();

        for (ssoInvChangeItem itemN: paChanges)
        {
            long sAccId    = itemN.AccId;
            long sVendorId = itemN.BrandId;
            
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

    public static void resetMemoryTables(EntityManager pem, long pUserId, long pAccId, long pVendorId)
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
                                                    long                        pUserAccountId, 
                                                    ArrayList<ssoInvChangeItem> paChanges) throws Exception
    {
        ArrayList<String> aUpdQueries = new ArrayList<String>();

        try
        {
            int i=0;
            for (ssoInvChangeItem changeN:paChanges)
            {
                // Prep Update Query 
                //---------------------------------------------------------
                String sQuery = prepareUpdateQuery4OptionStats( pUserAccountId,
                                                                changeN.AccId, 
                                                                changeN.BrandId, 
                                                                changeN.ItemCode,
                                                                changeN.OptId,
                                                                changeN.OptGroup,
                                                                changeN.OptCode,
                                                                changeN.oldVal,
                                                                changeN.newVal);

                i++;
                aUpdQueries.add(sQuery);
            }

            String sInitQuery = aUpdQueries.get(0);

            Query stmtQry = pem.CreateNativeQuery(sInitQuery);
            stmtQry.addBatch();

            for(int j=1; j<aUpdQueries.size(); j++)
            {
                stmtQry.addBatch(aUpdQueries.get(j));
            }

            int [] iAffectedRowNum = stmtQry.executeBatch();

            return true;            
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static String prepareUpdateQuery4OptionStats(long       pUserAccId,
                                                        long       pChgAccId, 
                                                        long       pChgBrandId, 
                                                        String     pChgItemCode, 
                                                        long       pChgOptionId, 
                                                        String     pOptionGroup,
                                                        String     pOptionCode,
                                                        BigDecimal pChgOldVal, 
                                                        BigDecimal pChgNewVal)
    {
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

        sQuery = "UPDATE ss_acc_inv_option_stats " +
                 " SET " + 
                 " LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s000') AS UNSIGNED INTEGER) ," + 
                 " BYUSER = " + Util.Str.SQUOTE(Long.toString(pUserAccId)) + ",";

        if(sOptCode.equals("\"\"")==true)
            sOptCode = "";

        //if(sOptGroup.equals("\"\"")==true)
        //    sOptGroup = "";

        if(sInvEffect.equals("P")==true)
        {
            
            String sOptUpdate = "{" + Util.Str.QUOTE(sOptCode) + ":" + bdDiff.abs().toString() + "}";
            
            sQuery += "OPTIONS_ADJ_PLUS = INV_JSON_MERGE(IF(OPTIONS_ADJ_PLUS='','{}', OPTIONS_ADJ_PLUS),'" + sOptUpdate + "', '+') ";
            
            /*
            sQuery += "OPTIONS_ADJ_PLUS = CASE " + 
                      "WHEN OPTIONS_ADJ_PLUS IS NULL OR OPTIONS_ADJ_PLUS = ''" + 
                      " THEN " + 
                            " JSON_SET('{}', " + 
                            " '$." + Util.Str.QUOTE(sOptCode) + "'," +  
                            " IFNULL(JSON_EXTRACT( '{}','$." + Util.Str.QUOTE(sOptCode) + "'), 0) + " +
                            bdDiff.toString() + ")" + 
                      " ELSE " + 
                            " JSON_SET(OPTIONS_ADJ_PLUS, " + 
                            " '$." + Util.Str.QUOTE(sOptCode) + "'," + 
                            " IFNULL(JSON_EXTRACT( OPTIONS_ADJ_PLUS," +  
                            " '$." + Util.Str.QUOTE(sOptCode) + "'), 0) + " + 
                            bdDiff.toString() + ")" + 
                      " END ";
            */
        }
        else
        {
            String sOptUpdate = "{" + Util.Str.QUOTE(sOptCode) + ":" + bdDiff.abs().toString() + "}";

            sQuery += "OPTIONS_ADJ_MINUS = INV_JSON_MERGE(IF(OPTIONS_ADJ_MINUS='','{}', OPTIONS_ADJ_MINUS),'" + sOptUpdate + "', '+') ";
            /*
            sQuery += "OPTIONS_ADJ_MINUS = CASE " + 
                      "WHEN OPTIONS_ADJ_MINUS IS NULL OR OPTIONS_ADJ_MINUS = ''" + 
                      " THEN " + 
                            " JSON_SET('{}', " + 
                            " '$." + Util.Str.QUOTE(sOptCode) + "'," +  
                            " IFNULL(JSON_EXTRACT( '{}','$." + Util.Str.QUOTE(sOptCode) + "'), 0) + " +
                            bdDiff.toString() + ")" + 
                      " ELSE " + 
                            " JSON_SET(OPTIONS_ADJ_MINUS, " + 
                            " '$." + Util.Str.QUOTE(sOptCode) + "'," + 
                            " IFNULL(JSON_EXTRACT( OPTIONS_ADJ_MINUS," +  
                            " '$." + Util.Str.QUOTE(sOptCode) + "'), 0) + " + 
                            bdDiff.toString() + ")" + 
                      " END ";
            */
        }
        
        if (sOptGroup.trim().length()==0)
            sOptGroup = "''";
        
        sQuery +=" WHERE " + 
                    " UID = " + pChgOptionId + 
                    " AND " + 
                    " OPTION_GROUP = " + Util.Str.QUOTE(sOptGroup) + 
                    " AND " + 
                    " ACCOUNT_ID = " + pChgAccId +
                    " AND " +
                    " VENDOR_ID = " + pChgBrandId +
                    //" AND " +
                    //" ITEM_CODE = " + pChgItemCode+//ITEM CODE NOT EXIST IN OPTIONS TABLE
                    " AND " + 
                    " STAT = 1";

        return sQuery;
    }

    public static boolean updateVendorStats2Changes(EntityManager   pem,
                                                    long            pAccountId,
                                                    long            pVendorId,
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

    public static boolean updateItemStats2Changes(EntityManager               pem, 
                                                  long                        pUserAccountId,
                                                  ArrayList<ssoInvChangeItem> paChanges) throws Exception
    {
        
        ArrayList<String> aUpdQueries = new ArrayList<String>();

        try
        {

            int i=0;
            for (ssoInvChangeItem changeN:paChanges)
            {

                // Prep Update Query 
                //---------------------------------------------------------
                String sQuery = prepareUpdateQuery4ItemStats(pUserAccountId,
                                                             changeN.AccId, 
                                                             changeN.BrandId, 
                                                             changeN.ItemCode.replaceAll("\"", ""),
                                                             changeN.oldVal,
                                                             changeN.newVal);

                i++;
                aUpdQueries.add(sQuery);
            }

            String sInitQuery = aUpdQueries.get(0);

            Query stmtQry = pem.CreateNativeQuery(sInitQuery);
            stmtQry.addBatch();

            for(int j=1; j<aUpdQueries.size(); j++)
            {
                stmtQry.addBatch(aUpdQueries.get(j));
            }

            int [] iAffectedRowNum = stmtQry.executeBatch();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static String prepareUpdateQuery4ItemStats(  long       pUserAccId,
                                                        long       pChgAccId, 
                                                        long       pChgBrandId, 
                                                        String     pChgItemCode, 
                                                        BigDecimal pChgOldVal, 
                                                        BigDecimal pChgNewVal)
    {
        String sQuery     = "";
        String sInvEffect = "";

        BigDecimal bdDiff = new BigDecimal(BigInteger.ZERO);

        bdDiff = pChgNewVal.subtract(pChgOldVal);

        if (pChgOldVal.compareTo(pChgNewVal) > 0)
            sInvEffect = "M"; //Deducted / Minus
        else
            sInvEffect = "P"; //Added / Plus

        sQuery = "UPDATE ss_acc_inv_item_stats " +
                 " SET " + 
                 " LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s000') AS UNSIGNED INTEGER) ," + 
                 " BYUSER = " + Util.Str.SQUOTE(Long.toString(pUserAccId)) + ",";

        if(sInvEffect.equals("P")==true)
            sQuery += " QUANTITY_ADJ_PLUS  = QUANTITY_ADJ_PLUS + " + bdDiff.abs().toString() ;
        else
            sQuery += " QUANTITY_ADJ_MINUS = QUANTITY_ADJ_MINUS +" + bdDiff.abs().toString() ;

        sQuery +=" WHERE " + 
                    " ACCOUNT_ID = " + pChgAccId +
                    " AND " +
                    " VENDOR_ID = " + pChgBrandId +
                    " AND " +
                    " ITEM_CODE = " + Util.Str.SQUOTE(pChgItemCode)+
                    " AND " + 
                    " STAT = 1";

        return sQuery;
    }

    public static ArrayList<ssoInvChangeItem> collectChanges(String psChanges, long pUserAccountId) throws Exception
    {
        ArrayList<ssoInvChangeItem> changes = new ArrayList<ssoInvChangeItem>();

        try
        {
            JsonArray jsAllChanges = Util.JSON.toArray(psChanges);
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
                        changeN.AccId = Long.parseLong(sVal);
                    }
                    else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_BRAND_ID)==true)
                    {
                        changeN.BrandId = Long.parseLong(sVal);
                    }
                    else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_ITM_CODE)==true)
                    {
                        changeN.ItemCode = sVal.trim();
                    }
                    else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_OPT_ID)==true)
                    {
                        changeN.OptId = Long.parseLong(sVal);
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
                        changeN.oldVal = new BigDecimal(sVal);
                    }
                    else if (sKey.equals(g_INV_QNTY_CHANGE_ITEM_SIGN_NEW_VAL)==true)
                    {
                        changeN.newVal = new BigDecimal(sVal);
                    }

                }//all the changes thru keys collected 
                
                changes.add(changeN);
            }// all the rows
            
            return changes;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

}
