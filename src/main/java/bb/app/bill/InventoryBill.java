/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import bb.app.dekonts.DekontSummaryYear;
import bb.app.global.ssoAppGlobal;
import bb.app.obj.ssoVendorItemStatsCore;
import bb.app.stats.ssStatsQuery;
import static bb.app.stmt.InventoryStatement.calculateStatementTotals;
import bb.app.vendor.VendorOps;
import entity.acc.SsAccInvVendorStats;
import entity.eod.SsEodInvTxnDets;
import entity.eod.SsEodInvTxns;
import entity.stmt.SsStmInvStatements;
import entity.txn.SsTxnInvBill;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jaxesa.api.callback.ssoCallbackParam;
import jaxesa.log.LogManager;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;
import org.json.simple.JSONObject;

/**
 *
 * @author Administrator
 */
public final class InventoryBill
{
    public static void deleteInvBillOnStatement( EntityManager               pem,
                                                String                      psUserId, 
                                                ArrayList<ssoCallbackParam> paParams,
                                                BigInteger                        pBillId) throws Exception
    {
        // update STAT = 0 to delete
        try
        {
            // update STAT = 0 to delete
            SsStmInvStatements stmtTable = new SsStmInvStatements();

            Query qry = pem.createNamedQuery("SsStmInvStatements.deleteBill", SsStmInvStatements.class);

            int index = 1;
            qry.SetParameter(index++, pBillId             , "BILL_ID");

            qry.executeUpdate();

        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static void deleteInvBillOnEOD(EntityManager               pem,
                                         String                      psUserId, 
                                         ArrayList<ssoCallbackParam> paParams,
                                         BigInteger                        pBillId) throws Exception
    {
        try
        {
            // update STAT = 0 to delete

            // DELETE EOD 
            // ---------------------------------------------------------------------------
            SsEodInvTxns eodTable = new SsEodInvTxns();

            Query qry = pem.createNamedQuery("SsEodInvTxns.deleteBill", SsEodInvTxns.class);

            int index = 1;
            qry.SetParameter(index++, pBillId             , "BILL_ID");

            qry.executeUpdate();

            // DELETE EOD - DETS
            // ---------------------------------------------------------------------------
            SsEodInvTxnDets eodDetsTable = new SsEodInvTxnDets();

            Query qry2 = pem.createNamedQuery("SsEodInvTxnDets.deleteBill", SsEodInvTxnDets.class);

            int index2 = 1;
            qry2.SetParameter(index2++, pBillId             , "BILL_ID");

            qry2.executeUpdate();

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void moveBill2EOD(EntityManager pem, BigInteger pBillId)
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_COL_INV_MOVE_TXN_2_EOD_4_BILL");

            SP.registerStoredProcedureParameter("P_BILL_ID"          , Long.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pBillId        , "P_BILL_ID");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            String s = e.getMessage();
        }
    }

    // Called @ UpdateBill 
    public static ArrayList<String> updateItemStats(EntityManager pem, 
                                                    BigInteger          pAccId,
                                                    BigInteger          pVendorId,
                                                    BigInteger          pBillId,
                                                    String        psNewDiscRate,//this value is not supported by UI as of March 28, 2024 but will be impletmented - EB
                                                    String        psNewTaxRate,
                                                    String        pjsBillChanges) throws Exception
    {
        ArrayList<String> aErrorCollection = new ArrayList<String>();

        ssoBillChanges billChanges = new ssoBillChanges();
        int[]           AffectedRows;

        String sPrevDiscRate = "";
        String sNewDiscRate  = "";

        String sPrevSurcharge = "";
        String sNewSurcharge  = "";

        String sPrevTaxRate = "";
        String sNewTaxRate  = psNewTaxRate;

        try
        {
            billChanges = collectBillChanges(psNewTaxRate, pjsBillChanges);

            // IMPORTANT:
            // This function is called by API Callback.
            // Therefore the bill has already been edited. 
            // Therefore we need to reverse it back to get the previous bill

            // Get Updated/New Bill (memwork)
            //------------------------------------------------------------------
            ssoBill newBill = new ssoBill();
            newBill = getCurrentBill(pem, pBillId);

            // Get Previous Bill (db)
            //------------------------------------------------------------------
            ssoBill prevBill = new ssoBill();
            //prevBill = getBillDeleted(pem, pBillId);// Bill Edit = Delete Current + Create New
            prevBill = getPrevBillAfterChanges(pem, newBill, billChanges);

            // Calculate per Item Surcharge Fees
            BigDecimal bdNewBottomExpensePerItem = new BigDecimal(BigInteger.ZERO); 
            bdNewBottomExpensePerItem = newBill.expense.divide(newBill.totQuantity, 5, RoundingMode.HALF_UP);

            BigDecimal bdPrevBottomExpensePerItem = new BigDecimal(BigInteger.ZERO); 
            bdPrevBottomExpensePerItem = prevBill.expense.divide(prevBill.totQuantity, 5, RoundingMode.HALF_UP);

            // GEN INIT QUERY
            //---------
            String sInitEditQuery    = "";
            //String sInitReverseQuery = "";// this is for the previous item code if the item code changed on the bill
            String sInitNewQuery = "";

            sInitEditQuery    = ssStatsQuery.generateUpdateItemStatsQuery4Change(prevBill.IsInvReturn, true, false, false);//txn type (return/sent) can't be changed
            //sInitReverseQuery = ssStatsQuery.generateUpdateItemStatsQuery4Reverse(prevBill.IsInvReturn);


            // SET QUERY 
            //----------
            Query qryItemCodeEdit    = pem.CreateNativeQuery(sInitEditQuery);
            //Query qryItemCodeReverse = pem.CreateNativeQuery(sInitReverseQuery);
            
            //qry.addBatch();
            //int iItemCodeChangeCounter = 0;
            
            ArrayList<Integer> aItemCodeChangeIndexes = new ArrayList<Integer>();
            
            boolean bItemCodeChangeDetected = false;
            for(int i=0;i<prevBill.lines.size();i++)
            {
                ssoBillLine lineNPrev = new ssoBillLine();
                ssoBillLine lineNNew  = new ssoBillLine();

                lineNPrev = prevBill.lines.get(i);
                lineNNew  = newBill.lines.get(i);//new bill

                boolean rc = false;
                rc = hasBillAttributesChanged(billChanges);
                if(rc==false)
                {
                    // attribs not changed. Check if the line among the changes
                    rc = isThisLineAmongChanges(billChanges, lineNPrev.Id);
                    if(rc==false)
                        continue;//No attributes nor line change detected so continue

                }
                else
                {
                    //if true, everything related to the bill changed.
                    //so roll thru each single line
                }

                // Set Parameters
                //----------------
                setBatchParams_ItemStats4Edit(  qryItemCodeEdit, 
                                                pAccId, 
                                                pVendorId,

                                                lineNPrev.itemCode,
                                                lineNNew.itemCode,

                                                prevBill.discountRate.toString(),
                                                newBill.discountRate.toString(), 

                                                //prevBill.surcharge.toString(),
                                                //newBill.surcharge.toString(),
                                                bdPrevBottomExpensePerItem.toString(),
                                                bdNewBottomExpensePerItem.toString(),

                                                prevBill.taxRate.toString(), 
                                                newBill.taxRate.toString(), 

                                                lineNPrev, 
                                                lineNNew);

                // add 2 batch
                //--------------------------------------------------
                qryItemCodeEdit.addBatch();

                if (lineNPrev.itemCode.toLowerCase().equals(lineNNew.itemCode.toLowerCase())==false)
                {
                    aItemCodeChangeIndexes.add(i);

                    bItemCodeChangeDetected = true;
                }

            }

            AffectedRows = qryItemCodeEdit.executeBatch();
            
            // Check if any of the prev item codes not exists
            // Normal circumstances the item code must exists as it is created in entry phase
            // if not exists then either deleted by hand or something else
            
            for(int j=0;j<AffectedRows.length;j++)
            {
                if(AffectedRows[j]==0)
                {
                    aErrorCollection.add("Item Code Not found either not created at the bill entry or deleted. Source @ cbInventoryBill_Update.updateItemStats");
                }
            }

            if (bItemCodeChangeDetected==true)
            {
                updateItemStatsOnItemCodeChange(pem, 
                                                aItemCodeChangeIndexes, 
                                                pAccId, 
                                                pVendorId, 
                                                prevBill, 
                                                newBill, 
                                                bdNewBottomExpensePerItem);
            }

            return aErrorCollection;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void updateItemStatsOnItemCodeChange(EntityManager            pem, 
                                                       ArrayList<Integer>       paItemCodeChangeIndexes,
                                                       BigInteger                     pAccId,
                                                       BigInteger                     pVendorId,
                                                       ssoBill                  poPrevBill,
                                                       ssoBill                  poNewBill,
                                                       BigDecimal               pbdNewBottomSurchargePerItem)
    {
        int[]           AffectedRows;
        int[]           AffectedRows2;

        String sInitUpdateQuery  = ssStatsQuery.generateUpdateItemStatsQuery4Change(poNewBill.IsInvReturn, false, false, false);
        String sInitCreateQuery  = ssStatsQuery.generateUpdateItemStatsQuery4New(poNewBill.txnType);//apply if the item code new

        try
        {

            Query qryItemCodeUpd     = pem.CreateNativeQuery(sInitUpdateQuery);
            Query qryItemCodeCrt     = pem.CreateNativeQuery(sInitCreateQuery);

            for(int i=0;i<paItemCodeChangeIndexes.size();i++)
            {
                int iChangeIndex = paItemCodeChangeIndexes.get(i);

                //ssoBillLine lineNPrev = new ssoBillLine();
                ssoBillLine lineNNew  = new ssoBillLine();

                //lineNPrev = poPrevBill.lines.get(iChangeIndex);
                lineNNew  = poNewBill.lines.get(iChangeIndex);//new bill

                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                // UPDATE 
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                setBatchParams_UpdateItemStatsOnItemCodeChange( qryItemCodeUpd,
                                                                pAccId,
                                                                pVendorId,
                                                                lineNNew.itemCode,
                                                                poNewBill.discountRate.toString(),
                                                                pbdNewBottomSurchargePerItem.toString(),//lineNPrev.surcharge.toString(), 
                                                                poNewBill.taxRate.toString(),
                                                                lineNNew);

                qryItemCodeUpd.addBatch();
            }

            AffectedRows = qryItemCodeUpd.executeBatch();

            int iCreateCounter = 0;
            for(int j=0;j<AffectedRows.length;j++)
            {
                if(AffectedRows[j]<=0)
                {
                    // Item Code Not Exists so create a new one
                    ssoBillLine lineNNew  = new ssoBillLine();

                    //lineNPrev = poPrevBill.lines.get(iChangeIndex);
                    lineNNew  = poNewBill.lines.get(j);//new bill

                    // CREATE
                    setBatchParams_CreateItemStatsOnItemCodeChange( qryItemCodeCrt,
                                                                    pAccId,
                                                                    pVendorId,
                                                                    lineNNew.itemCode,
                                                                    poNewBill.discountRate.toString(),
                                                                    pbdNewBottomSurchargePerItem.toString(),//lineNPrev.surcharge.toString(), 
                                                                    poNewBill.taxRate.toString(),
                                                                    lineNNew);

                    qryItemCodeCrt.addBatch();
                    iCreateCounter++;
                }

            }//for create

            if(iCreateCounter>0)
            {
                AffectedRows = qryItemCodeCrt.executeBatch();
                iCreateCounter = iCreateCounter;
            }

        }
        catch(Exception e)
        {
            String sMsg = e.getMessage();
        }
    }

    public static boolean hasBillAttributesChanged(ssoBillChanges pBillChanges)
    {
        if(pBillChanges.disc.newValue.equals(pBillChanges.disc.oldValue)==false)
            return true;

        if(pBillChanges.taxRate.newValue.equals(pBillChanges.taxRate.oldValue)==false)
            return true;
        
        if(pBillChanges.expense.newValue.equals(pBillChanges.expense.oldValue)==false)
            return true;
        
        return false;
    }
    
    public static boolean isThisLineAmongChanges(ssoBillChanges pBillChanges, BigInteger pLineId)
    {
        for(ssoBillChangeLineUnit lineN:pBillChanges.lines)
        {
            //if(lineN.Id==pLineId)
            if(lineN.Id.compareTo(pLineId)==0)
                return true;
        }

        return false;
    }

    public static void setBatchParams_UpdateItemStatsOnItemCodeChange(Query         pQry,
                                                                     BigInteger          pAccId,
                                                                     BigInteger          pVendorId,
                                                                     String        pItemCode,
                                                                     String        psBottomDiscRate,
                                                                     String        psBottomExpense,
                                                                     String        psTaxRate,
                                                                     ssoBillLine   poLine) throws Exception
    {
        ssoVendorItemStatsCore  oItemStats  = new ssoVendorItemStatsCore();

        try
        {

            oItemStats  = calcItemStats(poLine.quantity.toString(),
                                        poLine.EntryPrice.toString(),
                                        poLine.discountRate.toString(),
                                        poLine.surcharge.toString(),
                                        psBottomDiscRate,
                                        psBottomExpense,
                                        psTaxRate);

            int Colindex = 1;
            pQry.SetParameter(Colindex++, poLine.quantity            , "QUANTITY");        //QUANTITY++
            pQry.SetParameter(Colindex++, oItemStats.netTotal        , "NET_TOTAL");       //NET_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.discTotal       , "DISCOUNT_TOTAL");  //DISCOUNT_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.surchargeTotal  , "SURCHARGE_TOTAL"); //SURCHARGE_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.taxTotal        , "TAX_TOTAL");       //TAX_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.expenseTotal    , "EXPENSE_TOTAL");       //TAX_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.grossTotal      , "GROSS_TOTAL");     //GROSS_TOTAL++

            pQry.SetParameter(Colindex++, pAccId                     , "ACCOUNT_ID"); //
            pQry.SetParameter(Colindex++, pVendorId                  , "VENDOR_ID");  //
            pQry.SetParameter(Colindex++, pItemCode                  , "ITEM_CODE");  //
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void setBatchParams_CreateItemStatsOnItemCodeChange(Query         pQry,
                                                                     BigInteger          pAccId,
                                                                     BigInteger          pVendorId,
                                                                     String        pItemCode,
                                                                     String        psBottomDiscRate,
                                                                     String        psBottomSurcharge,
                                                                     String        psTaxRate,
                                                                     ssoBillLine   poLine) throws Exception
    {
        ssoVendorItemStatsCore  oItemStats  = new ssoVendorItemStatsCore();

        try
        {

            oItemStats  = calcItemStats(poLine.quantity.toString(),
                                        poLine.EntryPrice.toString(),
                                        poLine.discountRate.toString(),
                                        poLine.surcharge.toString(),
                                        psBottomDiscRate,
                                        psBottomSurcharge,
                                        psTaxRate);

            int Colindex = 1;
            pQry.SetParameter(Colindex++, -1                         , "PRICE_ID");        //QUANTITY++
            pQry.SetParameter(Colindex++, -1                         , "LAST_SALE_PRICE");       //NET_TOTAL++

            pQry.SetParameter(Colindex++, poLine.quantity            , "QUANTITY");        //QUANTITY++
            pQry.SetParameter(Colindex++, oItemStats.netTotal        , "NET_TOTAL");       //NET_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.discTotal       , "DISCOUNT_TOTAL");  //DISCOUNT_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.surchargeTotal  , "SURCHARGE_TOTAL"); //SURCHARGE_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.taxTotal        , "TAX_TOTAL");       //TAX_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.expenseTotal    , "EXPENSE_TOTAL");       //TAX_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.grossTotal      , "GROSS_TOTAL");     //GROSS_TOTAL++

            pQry.SetParameter(Colindex++, pAccId                     , "ACCOUNT_ID"); //
            pQry.SetParameter(Colindex++, pVendorId                  , "VENDOR_ID");  //
            pQry.SetParameter(Colindex++, pItemCode                  , "ITEM_CODE");  //
            pQry.SetParameter(Colindex++, -1                         , "CATEGORY_ID");  //
            //pQry.SetParameter(Colindex++, pItemCode                  , "ITEMCODE_ID");  //

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void setBatchParams_ItemStats4Reverse(Query         pQry,
                                                        long          pAccId,
                                                        long          pVendorId,
                                                        String        pItemCode,
                                                        String        psBottomDiscRate,
                                                        String        psBottomSurcharge,
                                                        String        psTaxRate,
                                                        ssoBillLine   poLine) throws Exception
    {
        ssoVendorItemStatsCore  oItemStats  = new ssoVendorItemStatsCore();

        try
        {

            oItemStats  = calcItemStats(poLine.quantity.toString(),
                                        poLine.EntryPrice.toString(),
                                        poLine.discountRate.toString(),
                                        poLine.surcharge.toString(),
                                        psBottomDiscRate,
                                        psBottomSurcharge,
                                        psTaxRate);

            int Colindex = 1;
            pQry.SetParameter(Colindex++, poLine.quantity            , "QUANTITY");        //QUANTITY++
            pQry.SetParameter(Colindex++, oItemStats.grossTotal      , "GROSS_TOTAL");     //GROSS_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.discTotal       , "DISCOUNT_TOTAL");  //DISCOUNT_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.surchargeTotal  , "SURCHARGE_TOTAL"); //SURCHARGE_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.taxTotal        , "TAX_TOTAL");       //TAX_TOTAL++
            pQry.SetParameter(Colindex++, oItemStats.netTotal        , "NET_TOTAL");       //NET_TOTAL++

            pQry.SetParameter(Colindex++, pAccId                     , "ACCOUNT_ID"); //
            pQry.SetParameter(Colindex++, pVendorId                  , "VENDOR_ID");  //
            pQry.SetParameter(Colindex++, pItemCode                  , "ITEM_CODE");  //
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // WHEN WE GOT HERE AT THE ORIGINAL API METHOD 
    // VENDOR STATS ARE ALREADY UPDATED BUT HERE THE LINES WILL BEING UPDATED
    public static void setBatchParams_ItemStats4Edit(Query         pQry,
                                                     BigInteger          pAccId,
                                                     BigInteger          pVendorId,

                                                     String        pPrevItemCode,
                                                     String        pNewItemCode,

                                                     String        psPrevBottomDiscRate,
                                                     String        psNewBottomDiscRate,

                                                     String        psPrevBottomExpense,
                                                     String        psNewBottomExpense,

                                                     String        psPrevTaxRate,
                                                     String        psNewTaxRate,

                                                     ssoBillLine   pPrevLine,
                                                     ssoBillLine   pNewLine) throws Exception
    {
        try
        {
            boolean bItemCodeChanged = false;
            if (pPrevItemCode.toLowerCase().equals(pNewItemCode.toLowerCase())==false)
                bItemCodeChanged = true;
            
            //ssoBillChangeLineUnit lineChangeN = new ssoBillChangeLineUnit();

            // ITEM CODE 
            //------------------------------------------------------------
            //lineChangeN.itemCode.oldValue =  pPrevLine.itemCode;
            //lineChangeN.itemCode.newValue =  pNewLine.itemCode;

            ssoVendorItemStatsCore  prevItemStats = new ssoVendorItemStatsCore();
            ssoVendorItemStatsCore  newItemStats  = new ssoVendorItemStatsCore();

            prevItemStats = calcItemStats(pPrevLine.quantity.toString(),
                                          pPrevLine.EntryPrice.toString(),
                                          pPrevLine.discountRate.toString(), 
                                          pPrevLine.surcharge.toString(),
                                          psPrevBottomDiscRate, 
                                          psPrevBottomExpense, 
                                          psPrevTaxRate);

            newItemStats  = calcItemStats(pNewLine.quantity.toString(),
                                          pNewLine.EntryPrice.toString(),
                                          pNewLine.discountRate.toString(),
                                          pNewLine.surcharge.toString(),
                                          psNewBottomDiscRate, 
                                          psNewBottomExpense, 
                                          psNewTaxRate);

            int Colindex = 1;

            if(bItemCodeChanged==false)
            {
                pQry.SetParameter(Colindex++, pNewLine.quantity            , "QUANTITY");        //QUANTITY++
                pQry.SetParameter(Colindex++, newItemStats.netTotal        , "NET_TOTAL");       //NET_TOTAL++
                pQry.SetParameter(Colindex++, newItemStats.discTotal       , "DISCOUNT_TOTAL");  //DISCOUNT_TOTAL++
                pQry.SetParameter(Colindex++, newItemStats.surchargeTotal  , "SURCHARGE_TOTAL"); //SURCHARGE_TOTAL++
                pQry.SetParameter(Colindex++, newItemStats.taxTotal        , "TAX_TOTAL");       //TAX_TOTAL++
                pQry.SetParameter(Colindex++, newItemStats.expenseTotal    , "EXPENSE_TOTAL");   //EXPENSE_TOTAL++
                pQry.SetParameter(Colindex++, newItemStats.grossTotal      , "GROSS_TOTAL");     //GROSS_TOTAL++
                
            }
            else
            {
                // If ItemCode has changed then these will affect the new Item Code 
                // The prev will be added to the fin adj (-)
                pQry.SetParameter(Colindex++, 0        , "QUANTITY");        //QUANTITY++
                pQry.SetParameter(Colindex++, 0        , "NET_TOTAL");       //NET_TOTAL++
                pQry.SetParameter(Colindex++, 0        , "DISCOUNT_TOTAL");  //DISCOUNT_TOTAL++
                pQry.SetParameter(Colindex++, 0        , "SURCHARGE_TOTAL"); //SURCHARGE_TOTAL++
                pQry.SetParameter(Colindex++, 0        , "TAX_TOTAL");       //TAX_TOTAL++
                pQry.SetParameter(Colindex++, 0        , "EXPENSE_TOTAL");   //EXPENSE_TOTAL++
                pQry.SetParameter(Colindex++, 0        , "GROSS_TOTAL");     //GROSS_TOTAL++
            }

            pQry.SetParameter(Colindex++, pPrevLine.quantity          , "QUANTITY_ADJ_TOTAL");     //QUANTITY_ADJ_TOTAL++
            pQry.SetParameter(Colindex++, prevItemStats.netTotal      , "FIN_ADJ_NET_TOTAL");      //FIN_ADJ_NET_TOTAL++
            pQry.SetParameter(Colindex++, prevItemStats.discTotal     , "FIN_ADJ_DISCOUNT_TOTAL"); //FIN_ADJ_DISCOUNT_TOTAL++
            pQry.SetParameter(Colindex++, prevItemStats.surchargeTotal, "FIN_ADJ_SURCHARGE_TOTAL");//FIN_ADJ_SURCHARGE_TOTAL++
            pQry.SetParameter(Colindex++, prevItemStats.taxTotal      , "FIN_ADJ_TAX_TOTAL");      //FIN_ADJ_TAX_TOTAL++
            pQry.SetParameter(Colindex++, prevItemStats.expenseTotal  , "FIN_ADJ_EXPENSE_TOTAL");  //FIN_ADJ_TAX_TOTAL++
            pQry.SetParameter(Colindex++, prevItemStats.grossTotal    , "FIN_ADJ_GROSS_TOTAL");    //FIN_ADJ_GROSS_TOTAL++

            pQry.SetParameter(Colindex++, pAccId       , "ACCOUNT_ID");
            pQry.SetParameter(Colindex++, pVendorId    , "VENDOR_ID");
            //pQry.SetParameter(Colindex++, pNewItemCode, "ITEM_CODE");
            pQry.SetParameter(Colindex++, pPrevItemCode, "ITEM_CODE");

            // No execution for batch
            //pQry.executeUpdate();
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoVendorItemStatsCore calcItemStats( String psQuantity,
                                                        String psEntryPrice,
                                                        String psLineDiscRate,
                                                        String psLineSurcharge,
                                                        String psBottomDiscRate,
                                                        //String psBottomSurcharge,
                                                        String psBottomSurchargePerItem,//EXPENSE
                                                        String psTaxRate) throws Exception
    {
        ssoVendorItemStatsCore itemStatsCore = new ssoVendorItemStatsCore();
        try
        {
            // This function calculates items stats based on one line
            //
            // Gross Total
            // Disc Total = (Line + Bottom)
            // Surcharge  = (Line + Bottom)
            // Tax
            // Net = (Gross - Disc_Total + SurchargeTotal) * (1 + Tax)
            BigDecimal bdQuantity        = new BigDecimal(psQuantity);
            BigDecimal bdEntryPrice      = new BigDecimal(psEntryPrice).multiply(bdQuantity);
            BigDecimal bdDiscRateLine    = new BigDecimal(psLineDiscRate);
            BigDecimal bdSurchargeLine   = new BigDecimal(psLineSurcharge).multiply(bdQuantity);

            BigDecimal bdDiscRateBottom  = new BigDecimal(psBottomDiscRate);
            BigDecimal bdExpense = new BigDecimal(psBottomSurchargePerItem).multiply(bdQuantity);//fixed tax

            BigDecimal bdTaxRate = new BigDecimal(psTaxRate);

            // Line Total
            BigDecimal bdLineDiscount      = bdEntryPrice.multiply(bdDiscRateLine);
            BigDecimal bdLineTotal         = bdEntryPrice.subtract(bdLineDiscount).add(bdSurchargeLine);

            BigDecimal bdBottomDiscount    = bdLineTotal.multiply(bdDiscRateBottom);
            //BigDecimal bdBottomTotalB4Tax  = bdLineTotal.subtract(bdBottomDiscount).add(bdSurchargeBottom);
            BigDecimal bdBottomTotalB4Tax  = bdLineTotal.subtract(bdBottomDiscount);

            BigDecimal bdTotalDiscount     = bdLineDiscount.add(bdBottomDiscount);
            //BigDecimal bdTotalSurcharge    = bdSurchargeLine.add(bdSurchargeBottom);
            BigDecimal bdTotalSurcharge    = bdSurchargeLine;

            BigDecimal bdTaxTotal          = bdBottomTotalB4Tax.multiply(bdTaxRate);
            BigDecimal bdGrossTotal        = bdBottomTotalB4Tax.add(bdTaxTotal).add(bdExpense);

            //results 
            itemStatsCore.netTotal       = bdEntryPrice;
            itemStatsCore.discTotal      = bdTotalDiscount;
            itemStatsCore.surchargeTotal = bdTotalSurcharge;
            //itemStatsCore.netTotal       = bdNetTotal;
            itemStatsCore.taxTotal       = bdTaxTotal;
            itemStatsCore.expenseTotal   = bdExpense;
            itemStatsCore.grossTotal     = bdGrossTotal;

            return itemStatsCore;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void mergeBill2Statement( EntityManager pem, 
                                            BigInteger          pAccId,
                                            BigInteger          pVendorId,
                                            BigInteger          pBillId, 
                                            boolean       pbRecalculateRevolving)
    {
        String sRecalcRevolving = "N";

        try
        {
            if (pbRecalculateRevolving==true)
                sRecalcRevolving = "Y";

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_STMT_MERGE_EOD_INTO_STMT_BY_ID");

            SP.registerStoredProcedureParameter("P_ACC_ID"           , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"        , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BILL_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_RECALC_REVOLVING" , String.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId          , "P_ACC_ID");
            SP.SetParameter(Colindex++, pVendorId       , "P_VENDOR_ID");
            SP.SetParameter(Colindex++, pBillId         , "P_BILL_ID");
            SP.SetParameter(Colindex++, sRecalcRevolving, "P_RECALC_REVOLVING");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            String s = e.getMessage();
        }
    }

    /*
        CHANGES Example;
        = {"46390263":{"prc":{"old":"155.00","new":"165.00"}},"46390264":{"qnty":{"old":"2","new":"3"}}}
        - 46390263 = bill line Id (dets.UID)
        - prc = price
        - old = the value before changed
        - new = the new value 
    */
    public static boolean updateBillNDetails(EntityManager pem, 
                                             BigInteger          pAccId, 
                                             BigInteger          pBrandId, 
                                             BigInteger          pBillId,
                                             BigDecimal    pbdNewDiscRate,
                                             BigDecimal    pbdNewTaxRate,
                                             BigDecimal    pbdExpense,
                                             String        pChangesSummary) throws Exception
    {
        try
        {
            ssoBillChanges billChanges = new ssoBillChanges();

            // 
            //------------------------------------------------------------------
            billChanges = collectBillChanges(pbdNewTaxRate.toString(), pChangesSummary);//only line changes here

            subtractPreviousBillAndAddNew4VendorStats(pem, 
                                                      pAccId, 
                                                      pBrandId, 
                                                      pBillId, 
                                                      pbdNewDiscRate,
                                                      pbdNewTaxRate,
                                                      pbdExpense,
                                                      billChanges);

            // update Bill (db)
            //------------------------------------------------------------------
            commitChangesOnBill( pem, 
                                 pAccId, 
                                 pBrandId, 
                                 pBillId, 
                                 pbdNewDiscRate.toString(),
                                 pbdNewTaxRate.toString(),
                                 pbdExpense.toString(),
                                 billChanges);

            // append pending operations (since this has works to do by callbacks)
            //------------------------------------------------------------------
            VendorOps.addWork4PendingOperations(pem, pAccId, pBrandId, pBillId, VendorOps.PENDING_OP_CODE_UPDATE_BILL);//Update Bill

            resetMemoryTables4BillUpdate(pem, pAccId);

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean subtractPreviousBillAndAddNew4VendorStats(EntityManager  pem,
                                                                    BigInteger           pAccId, 
                                                                    BigInteger           pVendorId, 
                                                                    BigInteger           pBillId,
                                                                    BigDecimal     pbdNewDiscRate,
                                                                    BigDecimal     pbdNewTaxRate,
                                                                    BigDecimal     pbdNewExpense,
                                                                    ssoBillChanges pBillChanges) throws Exception
    {
        try
        {
            BigDecimal bdNewDiscRate = new BigDecimal(BigInteger.ZERO);
            BigDecimal bdNewTaxRate  = new BigDecimal(BigInteger.ZERO);

            bdNewDiscRate = pbdNewDiscRate.divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);
            bdNewTaxRate  = pbdNewTaxRate.divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN);

            // Get Current Bill (db)
            //------------------------------------------------------------------
            ssoBill prevBill = new ssoBill();
            prevBill = getCurrentBill(pem, pBillId);
            //prevBill.taxRate = prevBill.taxRate.divide(new BigDecimal(100));//incoming is decimal
            //prevBill.discountRate = prevBill.discountRate.divide(new BigDecimal(100));
            //prevBill.taxRate = prevBill.taxRate;//incoming is decimal

            //String sChanges1 = prevBill.lines.get(0).quantity.toString() + "-" + 
            //                  prevBill.lines.get(1).quantity.toString();

            // Get Updated Bill (memwork)
            //------------------------------------------------------------------
            ssoBill newBill = new ssoBill();
            newBill = getNewBillAfterChanges(pem, bdNewDiscRate, bdNewTaxRate, pbdNewExpense, prevBill, pBillChanges);
            // DON'T DIVIDE HERE BECAUSE HERE IT IS DERIVED FROM (PREVBILL)
            //newBill.taxRate = newBill.taxRate.divide(new BigDecimal(100));//incoming is decimal
            //newBill.discountRate = newBill.discountRate.divide(new BigDecimal(100));

            //String sChanges2 = newBill.lines.get(0).quantity.toString() + "-" + 
            //                  newBill.lines.get(1).quantity.toString();

            // update vendor stats (db)
            //------------------------------------------------------------------
            ssoBillTotals prevBillTotals = new ssoBillTotals();
            prevBillTotals = calculateStatementTotals(prevBill.lines,
                                                      prevBill.discountRate.toString(), 
                                                      prevBill.taxRate.toString(), 
                                                      prevBill.expense.toString());

            ssoBillTotals newBillTotals = new ssoBillTotals();
            newBillTotals = calculateStatementTotals(newBill.lines,
                                                     newBill.discountRate.toString(), 
                                                     newBill.taxRate.toString(), 
                                                     newBill.expense.toString());

            // Check if quantity or totals has changed, or both
            // if it did, then update vendor stats
            boolean bChangeDetected = false;

            BigDecimal bdDiffQuantity       =   new BigDecimal(BigInteger.ZERO);
            BigDecimal bdDiffGrossTotal     =   new BigDecimal(BigInteger.ZERO);
            BigDecimal bdDiffDiscountTotal  =   new BigDecimal(BigInteger.ZERO);
            BigDecimal bdDiffSurchargeTotal =   new BigDecimal(BigInteger.ZERO);
            BigDecimal bdDiffTaxTotal       =   new BigDecimal(BigInteger.ZERO);
            BigDecimal bdDiffNetTotal       =   new BigDecimal(BigInteger.ZERO);

            if( (prevBillTotals.Summary.totalQuantity.equals(newBillTotals.Summary.totalQuantity)!=true) ||
                (prevBillTotals.Summary.totalNet.equals(newBillTotals.Summary.totalNet)!=true) ||
                (prevBillTotals.Bottom.totalTax.equals(newBillTotals.Bottom.totalTax)!=true) ||
                (prevBillTotals.Summary.totalSurcharge.equals(newBillTotals.Summary.totalSurcharge)!=true) ||
                (prevBillTotals.Summary.totalDiscount.equals(newBillTotals.Summary.totalDiscount)!=true) ||
                (prevBillTotals.Summary.totalGross.equals(newBillTotals.Summary.totalGross)!=true) )
            {
                // Totals Changed 
                // 
                bChangeDetected = true;
                /*
                bdDiffQuantity       = currentBillTotals.Summary.totalQuantity.subtract(updatedBillTotals.Summary.totalQuantity);
                bdDiffGrossTotal     = currentBillTotals.Summary.totalGross.subtract(updatedBillTotals.Summary.totalGross);
                bdDiffDiscountTotal  = currentBillTotals.Bottom.totalDiscount.subtract(updatedBillTotals.Bottom.totalDiscount);
                bdDiffSurchargeTotal = currentBillTotals.Bottom.totalSurcharge.subtract(updatedBillTotals.Bottom.totalSurcharge);
                bdDiffTaxTotal       = currentBillTotals.Bottom.totalTax.subtract(updatedBillTotals.Bottom.totalTax);
                bdDiffNetTotal       = currentBillTotals.Bottom.totalNet.subtract(updatedBillTotals.Bottom.totalNet);
                */
            }

            // OLD BILL GOES TO FIN_ADJ_MINUS 
            // NEW BILL GOES TO DIRECT TOTALS 
            if(bChangeDetected == true)
            {
                // Update Stats 
                //-------------------------------------------------------------
                String sQuery = "UPDATE ss_acc_inv_vendor_stats SET ";

                // Check if it is RETURN transaction, then REVERSE UPDATE (ADJ (+))
                if (prevBill.IsInvReturn==false)
                {
                    // New Bill adds to current stats
                    //---------------------------------------------------------
                    sQuery += " QUANTITY_ENTERED        = QUANTITY_ENTERED + ?, ";
                    sQuery += " RAW_TOTAL_ENTERED       = RAW_TOTAL_ENTERED + ?,  ";
                    sQuery += " DISCOUNT_TOTAL_ENTERED  = DISCOUNT_TOTAL_ENTERED + ?, ";
                    sQuery += " SURCHARGE_TOTAL_ENTERED = SURCHARGE_TOTAL_ENTERED + ?, ";
                    sQuery += " TAX_TOTAL_ENTERED       = TAX_TOTAL_ENTERED + ?, ";
                    sQuery += " EXPENSE_TOTAL_ENTERED   = EXPENSE_TOTAL_ENTERED + ?, ";
                    sQuery += " GROSS_TOTAL_ENTERED     = GROSS_TOTAL_ENTERED + ?, ";

                    // Previous Bill adds to FINADJ_MINUS
                    //---------------------------------------------------------
                    sQuery += " QUANTITY_ADJ_MINUS            = QUANTITY_ADJ_MINUS + ?, ";
                    sQuery += " FIN_ADJ_MINUS_RAW_TOTAL       = FIN_ADJ_MINUS_RAW_TOTAL + ?,  ";
                    sQuery += " FIN_ADJ_MINUS_DISCOUNT_TOTAL  = FIN_ADJ_MINUS_DISCOUNT_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_MINUS_SURCHARGE_TOTAL = FIN_ADJ_MINUS_SURCHARGE_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_MINUS_TAX_TOTAL       = FIN_ADJ_MINUS_TAX_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_MINUS_EXPENSE_TOTAL   = FIN_ADJ_MINUS_EXPENSE_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_MINUS_GROSS_TOTAL     = FIN_ADJ_MINUS_GROSS_TOTAL + ? ";
                }
                else
                {
                    // New Bill Updates current stats
                    //---------------------------------------------------------
                    sQuery += " QUANTITY_RETURNED        = QUANTITY_RETURNED + ?, ";
                    sQuery += " RAW_TOTAL_RETURNED       = RAW_TOTAL_RETURNED + ?, ";
                    sQuery += " DISCOUNT_TOTAL_RETURNED  = DISCOUNT_TOTAL_RETURNED + ?, ";
                    sQuery += " SURCHARGE_TOTAL_RETURNED = SURCHARGE_TOTAL_RETURNED + ?, ";
                    sQuery += " TAX_TOTAL_RETURNED       = TAX_TOTAL_RETURNED + ?, ";
                    sQuery += " EXPENSE_TOTAL_RETURNED   = EXPENSE_TOTAL_RETURNED + ?, ";
                    sQuery += " GROSS_TOTAL_RETURNED     = GROSS_TOTAL_RETURNED + ?, ";

                    // Previous Bill Goes to FINADJ_MINUS
                    //---------------------------------------------------------
                    sQuery += " QUANTITY_ADJ_PLUS            = QUANTITY_ADJ_PLUS + ?, ";
                    sQuery += " FIN_ADJ_PLUS_RAW_TOTAL       = FIN_ADJ_PLUS_RAW_TOTAL + ?,  ";
                    sQuery += " FIN_ADJ_PLUS_DISCOUNT_TOTAL  = FIN_ADJ_PLUS_DISCOUNT_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_PLUS_SURCHARGE_TOTAL = FIN_ADJ_PLUS_SURCHARGE_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_PLUS_TAX_TOTAL       = FIN_ADJ_PLUS_TAX_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_PLUS_EXPENSE_TOTAL   = FIN_ADJ_PLUS_EXPENSE_TOTAL + ?, ";
                    sQuery += " FIN_ADJ_PLUS_GROSS_TOTAL     = FIN_ADJ_PLUS_GROSS_TOTAL + ? ";

                }

                sQuery += " WHERE ";
                sQuery += "  STAT = 1 ";
                sQuery += "  AND ";
                sQuery += "  ACCOUNT_ID = ? ";
                sQuery += "  AND ";
                sQuery += "  VENDOR_ID = ? ";

                Query qry = pem.CreateNativeQuery(sQuery);

                int index = 1;

                //new Bill
                qry.SetParameter(index++, newBillTotals.Summary.totalQuantity , "QUANTITY");
                qry.SetParameter(index++, newBillTotals.Summary.totalNet      , "RAW_TOTAL");
                qry.SetParameter(index++, newBillTotals.Summary.totalDiscount , "DISCOUNT_TOTAL");
                qry.SetParameter(index++, newBillTotals.Summary.totalSurcharge, "SURCHARGE_TOTAL");
                qry.SetParameter(index++, newBillTotals.Bottom.totalTax       , "TAX_TOTAL");
                qry.SetParameter(index++, newBillTotals.Summary.totalExpense  , "EXPENSE_TOTAL");
                qry.SetParameter(index++, newBillTotals.Summary.totalGross    , "GROSS_TOTAL");
                

                // old goes to fin adj
                qry.SetParameter(index++, prevBillTotals.Summary.totalQuantity , "FIN_ADJ_QUANTITY");
                qry.SetParameter(index++, prevBillTotals.Summary.totalNet      , "FIN_ADJ_RAW_TOTAL");
                qry.SetParameter(index++, prevBillTotals.Summary.totalDiscount , "FIN_ADJ_DISCOUNT_TOTAL");
                qry.SetParameter(index++, prevBillTotals.Summary.totalSurcharge, "FIN_ADJ_SURCHARGE_TOTAL");
                qry.SetParameter(index++, prevBillTotals.Bottom.totalTax       , "FIN_ADJ_TAX_TOTAL");
                qry.SetParameter(index++, prevBillTotals.Summary.totalExpense  , "FIN_ADJ_EXPENSE_TOTAL");
                qry.SetParameter(index++, prevBillTotals.Summary.totalGross    , "FIN_ADJ_GROSS_TOTAL");

                qry.SetParameter(index++, pAccId            , "ACCOUNT_ID");
                qry.SetParameter(index++, pVendorId         , "VENDOR_ID");

                qry.executeUpdate();

            }//CHANGE DETECTED

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBill getPrevBillAfterChanges(EntityManager  pem, 
                                                  ssoBill        poCurrentBill,
                                                  ssoBillChanges poBillChanges) throws Exception
    {
        ssoBill prevBill = new ssoBill();

        BigDecimal bdTotalQuantity = new BigDecimal(BigInteger.ZERO);

        try
        {
            prevBill.IsInvReturn = poCurrentBill.IsInvReturn;
            prevBill.discountRate= poCurrentBill.discountRate;
            prevBill.expense   = poCurrentBill.expense;
            prevBill.taxRate     = poCurrentBill.taxRate;
            prevBill.txnCode     = poCurrentBill.txnCode;
            prevBill.txnType     = poCurrentBill.txnType;

            //updatedBill = (ssoBill) poCurrentBill;// DONT
            for (ssoBillLine curlineN:poCurrentBill.lines)
            {
                boolean bChangeDetected = false;
                
                //Check if line is one of the changes
                ssoBillChangeLineUnit chngLineN = new ssoBillChangeLineUnit();
                for(ssoBillChangeLineUnit cLineN:poBillChanges.lines)
                {
                    //if(cLineN.Id==curlineN.Id)
                    if(cLineN.Id.compareTo(curlineN.Id)==0)
                    {
                        chngLineN = cLineN;
                        bChangeDetected = true;
                        break;
                    }
                }

                if(bChangeDetected==true)
                {
                    ssoBillLine newLine = new ssoBillLine();

                    newLine.discountRate= curlineN.discountRate;
                    newLine.EntryPrice  = curlineN.EntryPrice;
                    newLine.Id          = curlineN.Id;
                    newLine.bDeleted    = curlineN.bDeleted;
                    newLine.category    = curlineN.category;
                    newLine.date        = curlineN.date;
                    newLine.itemCode    = curlineN.itemCode;
                    newLine.options     = curlineN.options;
                    newLine.quantity    = curlineN.quantity;
                    newLine.quantityType= curlineN.quantityType;
                    newLine.salesPrice  = curlineN.salesPrice;
                    newLine.surcharge   = curlineN.surcharge;
                    //newLine.taxRate     = curlineN.taxRate;

                    if(chngLineN.quantity.newValue.length()>0)//change detected
                        newLine.quantity = new BigDecimal(chngLineN.quantity.oldValue);

                    if(chngLineN.discount.newValue.length()>0)//change detected
                        newLine.discountRate = new BigDecimal(chngLineN.discount.oldValue).divide(new BigDecimal(100));

                    if(chngLineN.surcharge.newValue.length()>0)//change detected
                        newLine.surcharge = new BigDecimal(chngLineN.surcharge.oldValue);

                    //if(chngLineN.taxRate.newValue.length()>0)//change detected
                    //    newLine.taxRate = new BigDecimal(chngLineN.taxRate.newValue).divide(new BigDecimal(100));;

                    if(chngLineN.priceEntry.newValue.length()>0)//change detected
                        newLine.EntryPrice = new BigDecimal(chngLineN.priceEntry.oldValue);

                    if(chngLineN.priceSales.newValue.length()>0)//change detected
                        newLine.salesPrice = new BigDecimal(chngLineN.priceSales.oldValue);

                    if(chngLineN.itemCode.newValue.length()>0)//change detected
                        newLine.itemCode = chngLineN.itemCode.oldValue;
                    
                    bdTotalQuantity = bdTotalQuantity.add(newLine.quantity);
                    
                    prevBill.lines.add(newLine);
                }
                else
                {
                    //use as same
                    bdTotalQuantity = bdTotalQuantity.add(curlineN.quantity);
                    prevBill.lines.add(curlineN);
                }

            }

            prevBill.totQuantity = bdTotalQuantity;

            return prevBill;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBill getNewBillAfterChanges(EntityManager  pem, 
                                                 BigDecimal     pbdDiscRate,
                                                 BigDecimal     pbdTaxRate,
                                                 BigDecimal     pbdExpense,
                                                 ssoBill        poCurrentBill,
                                                 ssoBillChanges poBillChanges) throws Exception
    {
        ssoBill updatedBill = new ssoBill();

        try
        {
            updatedBill.IsInvReturn = poCurrentBill.IsInvReturn;
            updatedBill.discountRate= pbdDiscRate;//poCurrentBill.discountRate;
            updatedBill.expense     = pbdExpense;//poCurrentBill.expense;
            updatedBill.taxRate     = pbdTaxRate;//poCurrentBill.taxRate;
            updatedBill.txnCode     = poCurrentBill.txnCode;

            //updatedBill = (ssoBill) poCurrentBill;// DONT
            for (ssoBillLine curlineN:poCurrentBill.lines)
            {
                boolean bChangeDetected = false;
                
                //Check if line is one of the changes
                ssoBillChangeLineUnit chngLineN = new ssoBillChangeLineUnit();
                for(ssoBillChangeLineUnit cLineN:poBillChanges.lines)
                {
                    //if(cLineN.Id==curlineN.Id)
                    if(cLineN.Id.equals(curlineN.Id)==true)
                    {
                        chngLineN = cLineN;
                        bChangeDetected = true;
                        break;
                    }
                }

                if(bChangeDetected==true)
                {
                    ssoBillLine newLine = new ssoBillLine();
                    
                    newLine.discountRate= curlineN.discountRate;
                    newLine.EntryPrice  = curlineN.EntryPrice;
                    newLine.Id          = curlineN.Id;
                    newLine.bDeleted    = curlineN.bDeleted;
                    newLine.category    = curlineN.category;
                    newLine.date        = curlineN.date;
                    newLine.itemCode    = curlineN.itemCode;
                    newLine.options     = curlineN.options;
                    newLine.quantity    = curlineN.quantity;
                    newLine.quantityType= curlineN.quantityType;
                    newLine.salesPrice  = curlineN.salesPrice;
                    newLine.surcharge   = curlineN.surcharge;
                    //newLine.taxRate     = curlineN.taxRate;

                    if(chngLineN.quantity.newValue.length()>0)//change detected
                        newLine.quantity = new BigDecimal(chngLineN.quantity.newValue);

                    if(chngLineN.discount.newValue.length()>0)//change detected
                        newLine.discountRate = new BigDecimal(chngLineN.discount.newValue).divide(new BigDecimal(100));

                    if(chngLineN.surcharge.newValue.length()>0)//change detected
                        newLine.surcharge = new BigDecimal(chngLineN.surcharge.newValue);

                    //if(chngLineN.taxRate.newValue.length()>0)//change detected
                    //    newLine.taxRate = new BigDecimal(chngLineN.taxRate.newValue).divide(new BigDecimal(100));;

                    if(chngLineN.priceEntry.newValue.length()>0)//change detected
                        newLine.EntryPrice = new BigDecimal(chngLineN.priceEntry.newValue);

                    if(chngLineN.priceSales.newValue.length()>0)//change detected
                        newLine.salesPrice = new BigDecimal(chngLineN.priceSales.newValue);

                    if(chngLineN.itemCode.newValue.length()>0)//change detected
                        newLine.itemCode = chngLineN.itemCode.newValue;

                    updatedBill.lines.add(newLine);
                }
                else
                {
                    //use as same
                    updatedBill.lines.add(curlineN);
                }
            }
            
            return updatedBill;
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBill getBillDeleted(EntityManager pem, 
                                         long          pBillId) throws Exception
    {
        ssoBill bill = new ssoBill();

        try
        {
            Query qry = pem.createNamedQuery("SsEodInvTxns.getBillDeleted", SsEodInvTxns.class);

            int index = 1;
            qry.SetParameter(index++, pBillId             , "BILL_ID");

            List<List<RowColumn>> rs = qry.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoBillLine lineN = new ssoBillLine();

                List<RowColumn> RowN = rs.get(i);

                if(i==0)
                {
                    String sDiscRate  = Util.Database.getValString(RowN, "BILL_DISC_RATE").toString();
                    bill.discountRate = new BigDecimal(sDiscRate).divide(new BigDecimal(100));

                    String sExpense = Util.Database.getValString(RowN, "BILL_SURCHARGE").toString();
                    bill.expense    = new BigDecimal(sExpense);

                    String sTaxRate   = Util.Database.getValString(RowN, "BILL_TAX_RATE").toString();
                    bill.taxRate      = new BigDecimal(sTaxRate).divide(new BigDecimal(100));;

                    String sIsReturn  = Util.Database.getValString(RowN, "IS_RETURN").toString();
                    if(sIsReturn.trim().toUpperCase().equals("Y")==true)
                        bill.IsInvReturn  = true;

                    String sTxnCode   = Util.Database.getValString(RowN, "TXN_CODE").toString();
                    bill.txnCode      = sTxnCode;

                }

                lineN.Id = new BigInteger(Util.Database.getValString(RowN, "UID").toString());

                String sItemCode  = Util.Database.getValString(RowN, "ITEM_CODE").toString();
                lineN.itemCode    = sItemCode;

                String sQuantity  = Util.Database.getValString(RowN, "QUANTITY").toString();
                lineN.quantity    = new BigDecimal(sQuantity);

                String sDiscRate  = Util.Database.getValString(RowN, "DISCOUNT_RATE").toString();
                lineN.discountRate= new BigDecimal(sDiscRate).divide(new BigDecimal(100));;

                //String sTaxRate   = Util.Database.getValString(RowN, "BILL_TAX_RATE").toString();
                //lineN.taxRate     = new BigDecimal(sTaxRate).divide(new BigDecimal(100));;

                String sSurcharge = Util.Database.getValString(RowN, "SURCHARGE").toString();
                lineN.surcharge   = new BigDecimal(sSurcharge);

                String sEntryPrice= Util.Database.getValString(RowN, "PRICE_ENTRY").toString();
                lineN.EntryPrice  = new BigDecimal(sEntryPrice);

                String sSalePrice = Util.Database.getValString(RowN, "SALES_PRICE").toString();
                lineN.salesPrice  = new BigDecimal(sSalePrice);

                bill.lines.add(lineN);
            }

            return bill;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // !!!! WARNING !!!!
    // 1. subtractPreviousBillAndAddNew4VendorStats (Update Bill)
    //    updateItemStats
    // Therefore a change after these two must be tested
    // public static ssoBill getCurrentBill(EntityManager pem, 
    public static ssoBill getCurrentBill(EntityManager pem, BigInteger          pBillId) throws Exception
    {
        ssoBill bill = new ssoBill();
        BigDecimal bdTotalQuantity = new BigDecimal(BigInteger.ZERO);

        try
        {
            Query qry = pem.createNamedQuery("SsInvBill.getBill", SsTxnInvBill.class);

            int index = 1;
            qry.SetParameter(index++, pBillId             , "BILL_ID");

            List<List<RowColumn>> rs = qry.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoBillLine lineN = new ssoBillLine();

                List<RowColumn> RowN = rs.get(i);

                if(i==0)
                {
                    String sDiscRate  = Util.Database.getValString(RowN, "BILL_DISC_RATE").toString();
                    bill.discountRate = new BigDecimal(sDiscRate).divide(new BigDecimal(100));

                    String sTaxRate   = Util.Database.getValString(RowN, "BILL_TAX_RATE").toString();
                    bill.taxRate      = new BigDecimal(sTaxRate).divide(new BigDecimal(100));;

                    String sExpense = Util.Database.getValString(RowN, "BILL_EXPENSE").toString();
                    bill.expense    = new BigDecimal(sExpense);

                    String sIsReturn  = Util.Database.getValString(RowN, "IS_RETURN").toString();
                    if(sIsReturn.trim().toUpperCase().equals("Y")==true)
                        bill.IsInvReturn  = true;

                    String sTxnCode   = Util.Database.getValString(RowN, "TXN_CODE").toString();
                    bill.txnCode      = sTxnCode;
                    
                    bill.txnType      = Util.Database.getValString(RowN, "TXN_TYPE").toString();
                }

                lineN.Id = new BigInteger(Util.Database.getValString(RowN, "UID").toString());

                String sItemCode  = Util.Database.getValString(RowN, "ITEM_CODE").toString();
                lineN.itemCode    = sItemCode;

                String sQuantity  = Util.Database.getValString(RowN, "QUANTITY").toString();
                lineN.quantity    = new BigDecimal(sQuantity);

                String sDiscRate  = Util.Database.getValString(RowN, "DISCOUNT_RATE").toString();
                lineN.discountRate= new BigDecimal(sDiscRate).divide(new BigDecimal(100));;

                //String sTaxRate   = Util.Database.getValString(RowN, "BILL_TAX_RATE").toString();
                //lineN.taxRate     = new BigDecimal(sTaxRate).divide(new BigDecimal(100));;

                String sSurcharge = Util.Database.getValString(RowN, "SURCHARGE").toString();
                lineN.surcharge   = new BigDecimal(sSurcharge);

                String sEntryPrice= Util.Database.getValString(RowN, "PRICE_ENTRY").toString();
                lineN.EntryPrice  = new BigDecimal(sEntryPrice);

                String sSalePrice = Util.Database.getValString(RowN, "SALES_PRICE").toString();
                lineN.salesPrice  = new BigDecimal(sSalePrice);

                bdTotalQuantity = bdTotalQuantity.add(lineN.quantity);
                
                bill.lines.add(lineN);
            }
            
            bill.totQuantity = bdTotalQuantity;

            return bill;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBillChanges collectBillChanges(String psNewTaxRate,
                                                    String pChangesSummary) throws Exception
    {
        try
        {
            String SUMMARY_CODE_ITEM_CODE   = "c";
            String SUMMARY_CODE_PRICE_ENTRY = "p";
            String SUMMARY_CODE_PRICE_SALES = "m";
            String SUMMARY_CODE_QUANTITY    = "q";
            String SUMMARY_CODE_SURCHARGE   = "s";
            String SUMMARY_CODE_DISCOUNT    = "d";
            String SUMMARY_CODE_DELETE_LINE = "x";//dont change this because also used in isLineDeleted Function
            String SUMMARY_CODE_TAX_RATE    = "t";
            String SUMMARY_CODE_EXPENSE     = "e";
            
            ssoBillChanges billChanges = new ssoBillChanges();
            
            JSONObject jsBillLineChanges = Util.JSON.parseJSON(pChangesSummary);
            for(Iterator iterator = jsBillLineChanges.keySet().iterator(); iterator.hasNext();)
            {
                ssoBillChangeLineUnit lineNChanges = new ssoBillChangeLineUnit();

                String sBillLineId = (String)iterator.next();
                
                BigInteger lBillLineId = new BigInteger(sBillLineId);
                String sChanges    = (String)jsBillLineChanges.get(sBillLineId).toString();

                JSONObject jsChanges = Util.JSON.parseJSON(sChanges);

                ssoBillChangeUnit chngItemCode  = new ssoBillChangeUnit();
                ssoBillChangeUnit chngPrice     = new ssoBillChangeUnit();
                ssoBillChangeUnit chngQuantity  = new ssoBillChangeUnit();
                ssoBillChangeUnit chngSurcharge = new ssoBillChangeUnit();
                ssoBillChangeUnit chngDiscount  = new ssoBillChangeUnit();
                ssoBillChangeUnit chngExpense   = new ssoBillChangeUnit();
                ssoBillChangeUnit chngDelete    = new ssoBillChangeUnit();

                lineNChanges.Id        = lBillLineId;
                lineNChanges.itemCode  = getChangeValues(SUMMARY_CODE_ITEM_CODE, jsChanges);
                
                lineNChanges.priceEntry= getChangeValues(SUMMARY_CODE_PRICE_ENTRY, jsChanges);
                lineNChanges.priceSales= getChangeValues(SUMMARY_CODE_PRICE_SALES, jsChanges);
                
                lineNChanges.quantity  = getChangeValues(SUMMARY_CODE_QUANTITY, jsChanges);
                lineNChanges.surcharge = getChangeValues(SUMMARY_CODE_SURCHARGE, jsChanges);
                
                lineNChanges.discount = getChangeValues(SUMMARY_CODE_DISCOUNT, jsChanges);
                lineNChanges.taxRate   = getChangeValues(SUMMARY_CODE_TAX_RATE, jsChanges);
                
                lineNChanges.bDeleted  = isLineDeleted(SUMMARY_CODE_DELETE_LINE, jsChanges);
                
                billChanges.lines.add(lineNChanges);
                
            }
            
            return billChanges;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void resetMemoryTables4BillUpdate(EntityManager  pem, 
                                                    BigInteger     pAccId) throws Exception
    {
        try
        {
            // CLEAN CACHE - TXN BILL
            // ------------------------------------------------------
            ArrayList<ssoCacheSplitKey> aCacheSplitKeys_Bill = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey newKey1 = new ssoCacheSplitKey();
            newKey1.column = "ACCOUNT_ID";
            newKey1.value  = pAccId;
            aCacheSplitKeys_Bill.add(newKey1);

            // Flushes all related memories for the entity
            // clean cache
            pem.flush(SsTxnInvBill.class, aCacheSplitKeys_Bill);//cleans all related 

            // CLEAN CACHE - ACC - VENDOR STATS
            // ------------------------------------------------------
            ArrayList<ssoCacheSplitKey> aCacheSplitKeys_AccVendor = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey newKeyA = new ssoCacheSplitKey();
            newKey1.column = "ACCOUNT_ID";
            newKey1.value  = pAccId;
            aCacheSplitKeys_Bill.add(newKey1);

            // Flushes all related memories for the entity
            // clean cache
            pem.flush(SsAccInvVendorStats.class, aCacheSplitKeys_AccVendor);//cleans all related

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean commitChangesOnBill( EntityManager  pem, 
                                               BigInteger           pAccId, 
                                               BigInteger           pBrandId, 
                                               BigInteger           pBillId, 
                                               String         psNewDiscRate,//bottom
                                               String         psNewTaxRate,
                                               String         psExpense,
                                               ssoBillChanges poBillChanges) throws Exception
    {
        try
        {
            ArrayList<String> aAllUpdateQueries = new ArrayList<String>();

            // Calculate Total Quantity
            BigDecimal bdNewTotalQuantity = new BigDecimal(BigInteger.ZERO);
            BigDecimal bdOldTotalQuantity = new BigDecimal(BigInteger.ZERO);
            for(ssoBillChangeLineUnit lineNChange:poBillChanges.lines)
            {
                if(lineNChange.quantity.newValue.trim().length()>0)
                    bdNewTotalQuantity = bdNewTotalQuantity.add(new BigDecimal(lineNChange.quantity.newValue));
                
                if(lineNChange.quantity.oldValue.trim().length()>0)
                    bdOldTotalQuantity = bdOldTotalQuantity.add(new BigDecimal(lineNChange.quantity.oldValue));
            }

            String sNewTotalQuantity = "";
            String sOldTotalQuantity = "";

            if(bdNewTotalQuantity.compareTo(BigDecimal.ZERO)!=0)//NOT TRUE
                sNewTotalQuantity = bdNewTotalQuantity.toString();

            if(bdOldTotalQuantity.compareTo(BigDecimal.ZERO)!=0)//NOT TRUE
                sOldTotalQuantity = bdOldTotalQuantity.toString();

            // Prepare Update Queries 
            //-----------------------------------------------------------------
            for(ssoBillChangeLineUnit lineNChange:poBillChanges.lines)
            {
                 String sUpdQuery = prepareUpdateQuery( pBillId,
                                                        lineNChange.Id, //lBillLineId, 
                                                        lineNChange.bDeleted,//bDeleteLine,
                                                        lineNChange.itemCode.newValue,//chngItemCode.newValue, 
                                                        lineNChange.priceEntry.newValue,//chngPrice.newValue, 
                                                        lineNChange.quantity.newValue,//chngQuantity.newValue, 
                                                        lineNChange.discount.newValue, //chngDiscount.newValue,
                                                        lineNChange.surcharge.newValue,//chngSurcharge.newValue
                                                        sNewTotalQuantity,
                                                        psNewDiscRate,
                                                        psNewTaxRate
                                                        );

                //sAllLinesUpdQuery += sUpdQuery + ";";
                aAllUpdateQueries.add(sUpdQuery);
            }

            boolean rc = executeBillLineUpdates(pem, aAllUpdateQueries);
            if (rc==true)
            {
                if (! ( (aAllUpdateQueries.size()==0) && (psNewTaxRate.equals("")==true) ))
                {
                    // THIS ALSO DELETES EOD AND STMT RECORDS
                    recalculateNUpdateBillTotals(pem, 
                                                 pAccId, 
                                                 pBillId,
                                                 psNewDiscRate, 
                                                 psNewTaxRate,
                                                 psExpense);
                }
            }

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static boolean executeBillLineUpdates(EntityManager pem, ArrayList<String> paAllUpdQueries) throws Exception
    {
        try
        {
            if(paAllUpdQueries.size()==0)
                return true;

            String sInitQuery = paAllUpdQueries.get(0);

            Query stmtQry = pem.CreateNativeQuery(sInitQuery);
            stmtQry.addBatch();
            
            for(int i=1; i<paAllUpdQueries.size(); i++)
            {
                stmtQry.addBatch(paAllUpdQueries.get(i));
                //int iAffectedRowNum = stmtQry.executeUpdate();
            }

            int [] iAffectedRowCounts = stmtQry.executeBatch();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void recalculateNUpdateBillTotals(EntityManager  pem, 
                                                    BigInteger           pAccId, 
                                                    BigInteger           pBillId, 
                                                    String         psNewBottomDiscRate,
                                                    String         psNewTaxRate,
                                                    String         psNewExpense) throws Exception
    {
        BigDecimal dTaxRate = new BigDecimal(BigInteger.ZERO);
        String sContinueWSameTaxRate = "Y";
        
        BigDecimal dExpense = new BigDecimal(BigInteger.ZERO);
        String sContinueWSameExpense = "Y";

        BigDecimal dBottomDiscRate = new BigDecimal(BigInteger.ZERO);
        String sContinueWSameBottomDiscRate = "Y";

        try
        {
            if(psNewTaxRate.equals("")==false)
            {
                //dTaxRate = new BigDecimal(psNewTaxRate).divide(new BigDecimal(100));
                dTaxRate = new BigDecimal(psNewTaxRate);
                sContinueWSameTaxRate = "N";//TAX RATE HAS CHANGED
            }

            if(psNewBottomDiscRate.equals("")==false)
            {
                //dTaxRate = new BigDecimal(psNewTaxRate).divide(new BigDecimal(100));
                dBottomDiscRate = new BigDecimal(psNewBottomDiscRate);
                sContinueWSameBottomDiscRate = "N";//TAX RATE HAS CHANGED
            }
            
            if(psNewExpense.equals("")==false)
            {
                dExpense = new BigDecimal(psNewExpense);
                sContinueWSameExpense = "N";
            }

            // THIS ALSO DELETES EOD AND STMT RECORDS
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_RECALCULATE_BILL");

            SP.registerStoredProcedureParameter("P_ACC_ID"           , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BILL_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_SAME_TAX_RATE"    , String.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TAX_RATE"         , BigDecimal.class   , ParameterMode.IN);

            SP.registerStoredProcedureParameter("P_SAME_EXPENSE_RATE", String.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_EXPENSE"          , BigDecimal.class   , ParameterMode.IN);

            SP.registerStoredProcedureParameter("P_SAME_BTM_DISC_RATE", String.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BTM_DISC_RATE"     , BigDecimal.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId                         , "P_ACC_ID");
            SP.SetParameter(Colindex++, pBillId                        , "P_BILL_ID");
            SP.SetParameter(Colindex++, sContinueWSameTaxRate          , "P_SAME_TAX_RATE");
            SP.SetParameter(Colindex++, dTaxRate                       , "P_TAX_RATE");

            SP.SetParameter(Colindex++, sContinueWSameExpense          , "P_SAME_EXPENSE_RATE");
            SP.SetParameter(Colindex++, dExpense                       , "P_EXPENSE");

            SP.SetParameter(Colindex++, sContinueWSameBottomDiscRate   , "P_SAME_BTM_DISC_RATE");
            SP.SetParameter(Colindex++, dBottomDiscRate                , "P_BTM_DISC_RATE");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static String prepareUpdateQuery(BigInteger    pBillId, 
                                            BigInteger    pBillLineId, 
                                            boolean pbDeleteLine,
                                            String  pItemCode, 
                                            String  pPrice,
                                            String  pQuantity,
                                            String  pDiscountRate,
                                            String  pSurcharge,
                                            String  psNewTotalQuantity,
                                            String  psNewBottomDiscRate,
                                            String  psNewTaxRate)
    {
        String sStmt = "";
        int iTotalChangeNum = 0;
        int iChangeCounter  = 0;

        // Calculate Total Changes
        if (pItemCode.length()!=0)
            iTotalChangeNum++;

        if (pPrice.length()!=0)
            iTotalChangeNum++;

        if (pQuantity.length()!=0)
            iTotalChangeNum++;

        if (pDiscountRate.length()!=0)
            iTotalChangeNum++;

        if (pSurcharge.length()!=0)
            iTotalChangeNum++;

        if (psNewTotalQuantity.length()!=0)
            iTotalChangeNum++;

        if (psNewBottomDiscRate.length()!=0)
            iTotalChangeNum++;

        if (psNewTaxRate.length()!=0)
            iTotalChangeNum++;

        sStmt = "UPDATE ss_txn_inv_bill_dets SET ";

        if (pbDeleteLine==false)
        {
            
            if (psNewTotalQuantity.length()!=0)
            {
                iChangeCounter++;

                sStmt += " INFO_BILL_TOTAL_QUANTITY = " + psNewTotalQuantity;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (psNewBottomDiscRate.length()!=0)
            {
                iChangeCounter++;

                sStmt += " INFO_BILL_BOTTOM_DISC_RATE = " + psNewBottomDiscRate;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (psNewTaxRate.length()!=0)
            {
                iChangeCounter++;

                sStmt += " TAX_RATE = " + psNewTaxRate;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (pItemCode.length()!=0)
            {
                iChangeCounter++;

                sStmt += " ITEM_CODE = " + Util.Str.QUOTE(pItemCode);

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (pPrice.length()!=0)
            { 
                iChangeCounter++;

                sStmt += " PRICE_ENTRY = " + pPrice;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (pQuantity.length()!=0)
            {
                iChangeCounter++;

                sStmt += " QUANTITY = " + pQuantity ;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (pDiscountRate.length()!=0)
            {
                iChangeCounter++;

                sStmt += " DISCOUNT_RATE = " + pDiscountRate;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

            if (pSurcharge.length()!=0)
            {
                iChangeCounter++;

                sStmt += " SURCHARGE = " + pSurcharge;

                if (iChangeCounter!=iTotalChangeNum)
                    sStmt += ",";
            }

        }
        else
        {
            // LINE DELETE
            sStmt += " STAT = 0";
        }

        sStmt += ", EOD_DONE = 'N', "
                + " HAS_EDIT = 'Y', "
                + " EDIT_DATE = CAST(DATE_FORMAT(NOW(), \"%Y%m%d%H%i%s000\") AS UNSIGNED INTEGER) ";
        //sStmt += ", UPDATE_FLAG = 'Y', UPDATE_DATETIME = CAST(DATE_FORMAT(NOW(), \"%Y%m%d%H%i%s000\") AS UNSIGNED INTEGER) ";
        sStmt += " WHERE STAT = 1 AND  UID = " + pBillLineId;

        return sStmt;
    }

    public static boolean isLineDeleted(String pChangeCode, JSONObject pjsChanges)
    {
        String SUMMARY_CODE_DELETE_LINE = "x";

        try
        {
            if (pjsChanges.get(pChangeCode)!=null)
            {
                String sDeleted     = (String)pjsChanges.get(pChangeCode).toString();

                if (sDeleted.equals("true")==true)
                    return true;
                else
                    return false;
                
            }

            return false;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public static ssoBillChangeUnit getChangeValues(String pChangeCode, JSONObject pjsChanges)
    {
        ssoBillChangeUnit chng = new ssoBillChangeUnit();

        try
        {

            String SUMMARY_CODE_NEW_VALUE  = "n";
            String SUMMARY_CODE_OLD_VALUE  = "o";

            if (pjsChanges.get(pChangeCode)!=null)
            {
                String sChanges     = (String)pjsChanges.get(pChangeCode).toString();

                JSONObject jsValues = Util.JSON.parseJSON(sChanges);

                chng.newValue     = (String)jsValues.get(SUMMARY_CODE_NEW_VALUE).toString();
                chng.oldValue     = (String)jsValues.get(SUMMARY_CODE_OLD_VALUE).toString();

            }
            
            return chng;
        }
        catch(Exception e)
        {
            return chng;
        }
    }

}
