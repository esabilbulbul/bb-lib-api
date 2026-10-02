/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.stmt;

import bb.app.dict.DictionaryOps;
import bb.app.inv.InventoryOps;
import bb.app.bill.ssoBillLine;
import bb.app.bill.ssoBillTotals;
import static bb.app.inv.InventoryOps.updateBrandBalance;
import bb.app.txn.txnDefs;
import bb.app.vendor.VendorOps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvVendorStats;
import entity.txn.SsTxnInvBill;
import entity.txn.SsTxnInvBillDets;
import entity.dct.SsDctInvVendorSummary;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import jaxesa.persistence.EntityManager;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class InventoryStatement 
{
    /*
    
    public static String INV_NEW_ITEM      = "N";//New Entry
    public static String INV_RETURN_ITEM   = "R";//Return
    public static String INV_EXISTING_ITEM = "E";//Existing

    public static String INV_TXN_EFFECT_CREDIT = "C";// Means Vendor CREDITS US
    public static String INV_TXN_EFFECT_DEBIT  = "D";// Means We Credit Vendor
    */
    /*
    public static String getTransactionEffect(String pTxnCode)
    {
        String sTxnEffect = "";

        if (pTxnTypeOnServer.trim().toUpperCase().equals(INV_NEW_ITEM)==true)
        {
            sTxnEffect = txnDefs.INV_TXN_EFFECT_CREDIT;
        }
        else if (pTxnTypeOnServer.trim().toUpperCase().equals(INV_RETURN_ITEM)==true)
        {
            sTxnEffect = txnDefs.INV_TXN_EFFECT_CREDIT;
        }

        return sTxnEffect;
    }
    */
    /*
    public static String getTransactionType(String pTypeonUI)
    {
        String sTxnType = "";
        if (pTypeonUI.trim().toUpperCase().equals("N")==true)
            sTxnType = InventoryStatement.INV_NEW_ITEM;
        //else if (pTypeonUI.trim().toUpperCase().equals("E")==true)
            //sTxnType = InventoryStatement.INV_EXISTING_ITEM;//EXIST
        else if (pTypeonUI.trim().toUpperCase().equals("R")==true)
            sTxnType = InventoryStatement.INV_RETURN_ITEM;

        return sTxnType;
    }
    */
    public static BigInteger saveInventoryStatement(  EntityManager       pem,
                                                BigInteger                pUserId,
                                                BigInteger                pAccId,
                                                String              psLang,
                                                String              psCountry,
                                                long                psSessionId,
                                                String              psStmtRefNum,
                                                String              psTxnType,
                                                String              psTxnEffect,
                                                String              psTxnCode,
                                                int                 pPaymentTerm,
                                                String              pPaymentTermType,
                                                SsAccInvVendorStats pBrand,
                                                ssoBillTotals       pTotals,
                                                int                 psStmtDate,
                                                String              psDesc,
                                                long                pTxnDateTime) throws Exception
    {
        try
        {
            SsTxnInvBill invStmt = new SsTxnInvBill();

            invStmt.accountId       =   pAccId;
            invStmt.billRefNum      =   psStmtRefNum;
            invStmt.descr           =   psDesc;
            invStmt.txnType         =   psTxnType;
            invStmt.txnEffect       =   psTxnEffect;
            invStmt.txnCode         =   psTxnCode;//txnDefs.TXN_CODE_INVENTORY_RECEIVED;
            invStmt.vendorId        =   pBrand.vendorId;
            invStmt.txnDate         =   psStmtDate;
            invStmt.txnDatetime     =   pTxnDateTime;

            // Rates N Surcharges
            invStmt.taxRate         =   pTotals.Bottom.taxRate.multiply(new BigDecimal(100));
            invStmt.discountRate    =   pTotals.Bottom.DiscountRate.multiply(new BigDecimal(100));
            //invStmt.surcharge       =   pTotals.Bottom.expense;

            invStmt.paymentTerm     =   pPaymentTerm;
            invStmt.paymentTermType =   pPaymentTermType;

            invStmt.totalQuantity   =   pTotals.Lines.totalQuantity;
            invStmt.totalRaw        =   pTotals.Lines.totalRaw;

            invStmt.totalLineDiscount   = pTotals.Lines.totalDiscount;
            invStmt.totalBottomDiscount = pTotals.Bottom.discount;
            //invStmt.totalDiscount       = pTotals.Bottom.totalDiscount.add(pTotals.Lines.totalDiscount);

            invStmt.totalLineSurcharge   = pTotals.Lines.totalSurcharge;
            //invStmt.totalSurcharge       = pTotals.Bottom.totalSurcharge.add(pTotals.Lines.totalSurcharge);

            invStmt.totalB4tax      =   pTotals.Bottom.totalB4Tax;
            invStmt.totalTax        =   pTotals.Bottom.totalTax;
            //invStmt.expense         =   pTotals.Bottom.expense;
            invStmt.totalExpense    =   pTotals.Bottom.expense;
            invStmt.totalGross      =   pTotals.Bottom.totalGross;

            invStmt.uiSessionid     =   psSessionId;

            BigInteger lInvStmtUId = pem.persist(invStmt);

            return lInvStmtUId;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    //processBill
    public static BigInteger processStatement(  EntityManager  pem,
                                                BigInteger     pUserId,
                                                BigInteger     pAccId,
                                                boolean        pbTaxInPrice,
                                                BigDecimal     pbdTaxRate,
                                                String         psLang,
                                                String         psCountry,
                                                String         psSessionId,
                                                String         psTxnType,
                                                String         pjsStmt,
                                                long           pTxnDateTime) throws Exception
    {
        try
        {
            JsonObject jsoInvStmt = Util.JSON.toJsonObject(pjsStmt);

            //String sTxnType  = jsoInvStmt.get("txnType").toString().replace("\"", "").toUpperCase();
            String sTxnCode  = txnDefs.getTransactionCodeFromType(psTxnType);
            String sTxnType  = psTxnType;
            //String sTxnType  = InventoryStatement.getTransactionType(jsoInvStmt.get("txnType").toString().replace("\"", "").toUpperCase());
            String sTxnEffect= txnDefs.getTransactionEffect(sTxnCode);//txntype must be converted one
            String sBrand    = jsoInvStmt.get("brand").toString().replace("\"", "");
            String sBrandId  = jsoInvStmt.get("brandId").toString().replace("\"", "");
            String sStmtDate = jsoInvStmt.get("stmtDate").toString().replace("\"", "");
            String sDesc     = jsoInvStmt.get("desc").toString().replace("\"", "");

            String sbdBillTaxRate     = jsoInvStmt.get("tax").toString().replace("\"", "");
            BigDecimal bdBillTaxRate  = new BigDecimal(sbdBillTaxRate).divide(new BigDecimal(100));

            String sBillSurcharge     = jsoInvStmt.get("surcharge").toString().replace("\"", "");
            String sbdBillDiscountRate  = jsoInvStmt.get("discount").toString().replace("\"", "");//final discount
            BigDecimal bdBillDiscountRate = new BigDecimal(sbdBillDiscountRate).divide(new BigDecimal(100));

            String sPaymentTerm      = jsoInvStmt.get("paymentTerm").toString().replace("\"", "");
            int    iPaymentTerm      = Integer.parseInt(sPaymentTerm);
            String sPaymentTermType  = jsoInvStmt.get("paymentTermType").toString().replace("\"", "");

            String sStmtLines = jsoInvStmt.get("stmtLines").toString();
            String sStmtRefNum = "";

            JsonArray jsaStmtLines= Util.JSON.toArray(sStmtLines);

            ArrayList<ssoBillLine> invStatement = new ArrayList<ssoBillLine>();
            for (int j=0; j<jsaStmtLines.size();j++)
            {
                ssoBillLine newStmtLine = new ssoBillLine();

                JsonObject line = (JsonObject)jsaStmtLines.get(j);

                newStmtLine.itemCode    = line.get("itemCode").toString().replace("\"", "");
                newStmtLine.category    = line.get("category").toString().replace("\"", "");
                newStmtLine.quantity    = new BigDecimal(line.get("quantity").toString().replace("\"", ""));
                newStmtLine.quantityType= line.get("qType").toString().replace("\"", "");

                newStmtLine.EntryPrice  = new BigDecimal(line.get("entryPrice").toString().replace("\"", ""));
                newStmtLine.discountRate= new BigDecimal(line.get("discount").toString().replace("\"", ""));
                newStmtLine.discountRate= newStmtLine.discountRate.divide(new BigDecimal(100));

                //newStmtLine.taxRate     = new BigDecimal(line.get("tax").toString().replace("\"", ""));
                newStmtLine.salesPrice  = new BigDecimal(line.get("salesPrice").toString().replace("\"", ""));
                newStmtLine.surcharge   = new BigDecimal(line.get("surcharge").toString().replace("\"", ""));
                newStmtLine.options     = line.get("options").toString();

                invStatement.add(newStmtLine);
            }

            BigInteger lStmtUID = saveStatement(pem,
                                                pUserId, 
                                                pAccId,
                                                pbTaxInPrice,
                                                pbdTaxRate,
                                                psLang, 
                                                psCountry, 
                                                psSessionId, 
                                                invStatement, 
                                                sTxnCode,
                                                psTxnType,
                                                sBrand, 
                                                sBrandId, 
                                                sStmtDate, 
                                                sPaymentTerm, 
                                                sPaymentTermType, 
                                                sDesc, 
                                                sbdBillDiscountRate, 
                                                sBillSurcharge, 
                                                sbdBillTaxRate,
                                                pTxnDateTime);

            // Add2 pending Operations
            BigInteger biBrandId = new BigInteger(sBrandId);
            VendorOps.addWork4PendingOperations(pem, pAccId, biBrandId, lStmtUID, VendorOps.PENDING_OP_CODE_NEW_BILL);//Append Bill

            return lStmtUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static BigInteger saveStatement( EntityManager          pem,
                                            BigInteger             pUserId,
                                            BigInteger             pAccId,
                                            boolean                pbTaxInPrice,
                                            BigDecimal             pbdTaxRate,
                                            String                 psLang,
                                            String                 psCountry,
                                            String                 psSessionId,
                                            ArrayList<ssoBillLine> pinvStatement,
                                            String                 psTxnCode,
                                            String                 psTxnType,
                                            String                 psBrand,
                                            String                 psBrandId,
                                            String                 psStmtDate,
                                            String                 psPaymentTerm,
                                            String                 psPaymentTermType,
                                            String                 psDesc,
                                            String                 psbdBillDiscountRate,
                                            String                 psBillExpense,
                                            String                 psbdBillTaxRate,
                                            long                   pTxnDateTime 
                                            ) throws Exception
    {
        try
        {
            String sStmtRefNum = "";
            ssoBillTotals stmtTotals = new ssoBillTotals();
            String sTxnEffect= txnDefs.getTransactionEffect(psTxnCode);//txntype must be converted one
            int    iPaymentTerm      = 0; //Integer.parseInt(sPaymentTerm);

            String sBrandId = psBrandId;
            String sStmtDate= psStmtDate;
            String sTxnType = psTxnType;
            String sTxnCode = psTxnCode;
            String sDesc    = psDesc;
            //String sBillSurcharge = psBillSurcharge;
            String sBillExpense = psBillExpense;

            String sPaymentTerm     = psPaymentTerm;
            String sPaymentTermType = psPaymentTermType;
            BigDecimal bdBillTaxRate      = new BigDecimal(psbdBillTaxRate).divide(new BigDecimal(100));
            BigDecimal bdBillDiscountRate = new BigDecimal(psbdBillDiscountRate).divide(new BigDecimal(100));

            stmtTotals = calculateStatementTotals(  pinvStatement, 
                                                    //pbTaxInPrice,
                                                    //pbdTaxRate,
                                                    bdBillDiscountRate.toString(),
                                                    bdBillTaxRate.toString(),
                                                    sBillExpense);

            //SsDctInvVendorSummary vendorSummary = new SsDctInvVendorSummary();
            BigInteger lBrandID = new BigInteger(sBrandId);
            SsAccInvVendorStats   vendorStats   = new SsAccInvVendorStats();

            //vendorSummary = DictionaryOps.Vendor.getVendor(pem, pUserId, sBrand);

            //if (vendorSummary==null)
            //{
                // Exception - Vendor must be defined one
                //throw new Exception("Vendor is not defined");
            //}

            vendorStats = updateBrandBalance(   pem, 
                                                pAccId, 
                                                sTxnType,
                                                pAccId, 
                                                lBrandID,//vendorSummary.brandId, 
                                                stmtTotals);// CHANGE TO GET BRAND INFO FROM DICTIONARY

            BigInteger lStmtUID = saveInventoryStatement( pem, 
                                                    pUserId, 
                                                    pAccId, 
                                                    psLang, 
                                                    psCountry, 
                                                    Long.parseLong(psSessionId), 
                                                    sStmtRefNum,
                                                    sTxnType,
                                                    sTxnEffect,
                                                    sTxnCode,
                                                    iPaymentTerm,
                                                    sPaymentTermType,
                                                    vendorStats,
                                                    stmtTotals,
                                                    Integer.parseInt(sStmtDate),
                                                    sDesc,
                                                    pTxnDateTime);

            // Save Lines
            //------------------------------------------------------------------------------
            //ArrayList<SsTxnInvBillDets> invStmtLines = new ArrayList<SsTxnInvBillDets>();
            saveInventoryStatementLines(pem, 
                                        pinvStatement, 
                                        lStmtUID, 
                                        pUserId, 
                                        pAccId, 
                                        psLang, 
                                        psCountry, 
                                        psSessionId, 
                                        lBrandID, 
                                        sTxnType,
                                        sTxnCode, 
                                        sTxnEffect,
                                        stmtTotals.Summary.totalQuantity,
                                        bdBillTaxRate.multiply(new BigDecimal(100)).toString(), 
                                        bdBillDiscountRate.multiply(new BigDecimal(100)).toString(),
                                        sBillExpense,//
                                        sStmtDate,
                                        sDesc);

            
            
            // This is needed to use for bulk printing 
            InventoryOps.resetMemoryTables(pem, pAccId, lBrandID);

            return lStmtUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static int[] saveInventoryStatementLines( EntityManager           pem,
                                                    ArrayList<ssoBillLine>  pStmtLines,
                                                    BigInteger                    pStmtId,
                                                    BigInteger                    pUserId,
                                                    BigInteger                    pAccId,
                                                    String                  psLang,
                                                    String                  psCountry,
                                                    String                  psSessionId,
                                                    BigInteger                    pBrandId,
                                                    String                  psTxnType,
                                                    String                  psTxnCode,
                                                    String                  psTxnEffect,
                                                    BigDecimal              pbdTotalQuantity,
                                                    String                  psTaxRate,
                                                    String                  psBillDiscountRate,
                                                    String                  psBillSurcharge,
                                                    String                  psStmtDate,
                                                    String                  psDesc) throws Exception
    {
        ArrayList<SsTxnInvBillDets> aStmtLines = new ArrayList<SsTxnInvBillDets>();

        try
        {
            for(ssoBillLine lineN:pStmtLines)
            {
                SsTxnInvBillDets   invStmtLineN  = new SsTxnInvBillDets();
                invStmtLineN = prepareInventoryStatementLine(   pem, 
                                                                pStmtId, 
                                                                pUserId, 
                                                                pAccId, 
                                                                psLang, 
                                                                psCountry, 
                                                                psSessionId, 
                                                                pBrandId,//vendorSummary.brandId,
                                                                psTxnType, 
                                                                psTxnCode,
                                                                psTxnEffect,
                                                                lineN.itemCode, 
                                                                lineN.category, 
                                                                lineN.quantity.toString(),//Integer.toString(lineN.quantity),
                                                                lineN.quantityType, 
                                                                lineN.EntryPrice.toString(),
                                                                lineN.discountRate.multiply(new BigDecimal(100)).toString(),
                                                                lineN.surcharge.toString(),
                                                                pbdTotalQuantity.toString(),
                                                                psTaxRate,//sBillTaxRate, //no line tax rate anymore
                                                                psBillDiscountRate,//BILL BOTTOM DISCOUNT
                                                                psBillSurcharge,
                                                                lineN.salesPrice.toString(), 
                                                                lineN.options, 
                                                                psStmtDate,
                                                                psDesc);
                aStmtLines.add(invStmtLineN);
            }
            
            int[] lAffectedRowNums = pem.persistAll(aStmtLines, false);
            
            return lAffectedRowNums;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
            
    
    public static SsTxnInvBillDets prepareInventoryStatementLine(   EntityManager pem,
                                                                    BigInteger           pStmtId,
                                                                    BigInteger           pUserId,
                                                                    BigInteger           pAccId,
                                                                    String         psLang,
                                                                    String         psCountry,
                                                                    String         psSessionId,
                                                                    BigInteger           pBrandId,
                                                                    String         psTxnType,
                                                                    String         psTxnCode,
                                                                    String         psTxnEffect,
                                                                    String         psItemCode,
                                                                    String         psCategory,
                                                                    String         psQuantity,
                                                                    String         psQuantityType,
                                                                    String         psEntryPrice,
                                                                    String         psDiscount,
                                                                    String         psSurcharge,
                                                                    String         psTotalQuantity,
                                                                    String         psTaxRate,
                                                                    String         psBillBottomDiscountRate,//
                                                                    String         psBillBottomSurcharge,
                                                                    String         psSalesPrice,
                                                                    String         psOptions,
                                                                    String         psBillingDate,
                                                                    String         psDesc) throws Exception
    {

        try
        {

            SsTxnInvBillDets   invStmtLine  = new SsTxnInvBillDets();
            int  lQuantity = Integer.parseInt(psQuantity);

            //String sBillDate = psBillingDate.replace("-","");
            //sBillDate = sBillDate.replace(".","");

            long lBillDate = Long.parseLong(psBillingDate);
            BigDecimal  bdPriceEntry = new BigDecimal(psEntryPrice);
            BigDecimal  bdDiscountRate   = new BigDecimal(psDiscount);//COMES WITH DECIMAL
            //bdDiscountRate = bdDiscountRate.divide(new BigDecimal(100),2, RoundingMode.HALF_UP);
            BigDecimal  bdSurcharge  = new BigDecimal(psSurcharge);
            BigDecimal  bdTaxRate    = new BigDecimal(psTaxRate);// LINE NO TAX ANYMORE
            BigDecimal  bdPriceSale  = new BigDecimal(psSalesPrice);

            invStmtLine.billId      = pStmtId;
            invStmtLine.accountId   = pAccId;
            invStmtLine.vendorId    = pBrandId;
            invStmtLine.itemCode    = psItemCode;
            invStmtLine.itemCategory= psCategory;
            invStmtLine.txnType     = psTxnType;
            invStmtLine.txnCode     = psTxnCode;
            invStmtLine.txnEffect   = psTxnEffect;
            invStmtLine.quantity    = new BigDecimal(lQuantity);
            invStmtLine.priceEntry  = bdPriceEntry;
            invStmtLine.discountRate    = bdDiscountRate;

            // THESE 2 will be used for EOD process
            invStmtLine.infoBillBottomDiscRate = new BigDecimal(psBillBottomDiscountRate);
            invStmtLine.infoBillBottomSurcharge= new BigDecimal(psBillBottomSurcharge);
            invStmtLine.infoBillTotalQuantity  = new BigDecimal(psTotalQuantity);

            invStmtLine.taxRate     = bdTaxRate;
            invStmtLine.tax         = new BigDecimal(BigInteger.ZERO);//this value will be calculated at EOD

            invStmtLine.priceEnd    = bdPriceSale;
            invStmtLine.options     = psOptions;

            invStmtLine.surcharge   = bdSurcharge;

            //invStmtLine.totalGross     =  invStmtLine.priceEntry.multiply(invStmtLine.quantity);
            //invStmtLine.discount       =  invStmtLine.totalGross.multiply(invStmtLine.discountRate);
            //invStmtLine.totalSurcharge =  invStmtLine.surcharge.multiply(invStmtLine.quantity);
            //invStmtLine.totalNet       =  invStmtLine.totalGross.subtract(invStmtLine.discount).add(invStmtLine.totalSurcharge);

            /*
            invStmtLine.billTotal   = calculateStatementLineTotal(  invStmtLine.priceEntry, 
                                                                    invStmtLine.quantity, 
                                                                    invStmtLine.tax, 
                                                                    invStmtLine.discount);
            */
            //invTxn.stmtTotal   = bdTot1.add(bdStmtTax).subtract(bdStmtDiscount);
            invStmtLine.descr       = psDesc;
            invStmtLine.uiSessionid = Long.parseLong(psSessionId.substring(0,19));

            //Calculate Quantity Total

            //int iTotal = calculateTotalQuantity(psOptions);

            return invStmtLine;
            //long lInvStmtDetUId = pem.persist(invStmtLine);

            // get brand current balance
            // get item  current quantity

            //return lInvStmtDetUId;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // pBottomTaxRate = must come plain. For example; if it is 8% it is 8 NOT 0.08
    //
    // !!!!!!WARNING!!!!!!
    // This method is called by two sources
    // 1. New Inventory/Bill Entry (passes Decimal)
    // 2. Update Bill
    // Therefore becarefull when changing parameters passed.
    // Parameters must be passed decimal i.e. 0.1 
    // Taxrate, Surcharge, ...
    //
    // !!! BOTH PARAMETERS LINE RATES AS WELL AS BOTTOM RATES !!!!!
    //
    public static ssoBillTotals calculateStatementTotals( ArrayList<ssoBillLine>      pStatementLines,
                                                          //boolean                     pbTaxInPrice,
                                                          //BigDecimal                  pbdTaxRate,
                                                          String                      pBottomDiscountRate,// MUST BE DECIMAL
                                                          String                      pBottomTaxRate,//MUST BE DECIMAL
                                                          String                      pBottomExpense)//MUST BE DECIMAL
    {
        ssoBillTotals tots = new ssoBillTotals();

        for(ssoBillLine lineN:pStatementLines)
        {
            // Gross = Net - Disc + Surcharge + Tax
            //------------------------------------------------------------
            BigDecimal lineNet      = new BigDecimal(BigInteger.ZERO);
            BigDecimal lineDiscount = new BigDecimal(BigInteger.ZERO);
            BigDecimal lineSurcharge= new BigDecimal(BigInteger.ZERO);
            //BigDecimal lineB4Tax    = new BigDecimal(BigInteger.ZERO);
            BigDecimal lineGross    = new BigDecimal(BigInteger.ZERO);

            // Gross = Price x Quantity
            //lineGross    = lineN.EntryPrice.multiply(lineN.quantity);
            lineNet       = lineN.EntryPrice.multiply(lineN.quantity);
            // Discount = Gross x DiscountRate
            //lineDiscount  = lineNet.multiply(lineN.discountRate.divide(new BigDecimal(100)));
            lineDiscount  = lineNet.multiply(lineN.discountRate);// inv entry passes decimal so update bill also has to pass same
            // Surcharge
            lineSurcharge = lineN.surcharge.multiply(lineN.quantity);
            // Net = Gross - Discount + Surcharge
            lineGross     = lineNet.subtract(lineDiscount).add(lineSurcharge);

            tots.Lines.totalRaw       = tots.Lines.totalRaw.add(lineNet);
            tots.Lines.totalDiscount  = tots.Lines.totalDiscount.add(lineDiscount);
            tots.Lines.totalSurcharge = tots.Lines.totalSurcharge.add(lineSurcharge);
            tots.Lines.totalGross     = tots.Lines.totalGross.add(lineGross);
            //tots.Lines.totalQuantity  += lineN.quantity;
            tots.Lines.totalQuantity  = tots.Lines.totalQuantity.add(lineN.quantity);
        }

        // Init
        //BigDecimal bottomDiscountRate = new BigDecimal(pBottomDiscountRate).divide(new BigDecimal(100));
        BigDecimal bottomDiscountRate = new BigDecimal(pBottomDiscountRate);
        //BigDecimal bottomTaxRate  = new BigDecimal(pBottomTaxRate).divide(new BigDecimal(100));
        BigDecimal bottomTaxRate  = new BigDecimal(pBottomTaxRate);
        BigDecimal bottomTax  = new BigDecimal(BigInteger.ZERO);
        BigDecimal bottomExpense = new BigDecimal(pBottomExpense);

        BigDecimal bottomStart = new BigDecimal(BigInteger.ZERO);
        bottomStart = tots.Lines.totalGross;

        // Gross x Discount
        tots.Bottom.DiscountRate  = bottomDiscountRate;
        tots.Bottom.taxRate       = bottomTaxRate;
        tots.Bottom.discount      = bottomStart.multiply(bottomDiscountRate);
        //tots.Bottom.surcharge     = bottomExpense;

        tots.Bottom.totalB4Tax    = bottomStart.subtract(tots.Bottom.discount);//.add(bottomSurcharge);
        tots.Bottom.expense       = bottomExpense;//fixed tax
        // B4Tax = (Gross x Discount) 
        //if(pbTaxInPrice==false)
        //{
            // Net = B4Tax  x Tax + Surcharge (surcharge = fixed / lower tax/kdv => USUALLY TAX IS ZERO WHEN SURCHAGE IS USED) 
            bottomTax                 = tots.Bottom.totalB4Tax.multiply(bottomTaxRate);
            tots.Bottom.totalTax      = bottomTax;
            
            tots.Bottom.totalGross    = tots.Bottom.totalB4Tax.add(tots.Bottom.totalTax).add(bottomExpense);// bottomExpense = fixed tax
        /*
        }
        else
        {
            bottomTax                 = tots.Bottom.totalB4Tax.multiply(pbdTaxRate).divide(new BigDecimal(100).add(pbdTaxRate),RoundingMode.HALF_EVEN);
            tots.Bottom.totalTax      = bottomTax;
            tots.Bottom.totalGross    = tots.Bottom.totalB4Tax;//because price already included in price so final price = gross price as well as priceb4tax 
        }
        */
        //SUMMARY (Lines + Bottom) 
        //------------------------------------------------
        tots.Summary.totalQuantity  = tots.Lines.totalQuantity;
        tots.Summary.totalNet       = tots.Lines.totalRaw;
        tots.Summary.totalDiscount  = tots.Lines.totalDiscount.add(tots.Bottom.discount);
        tots.Summary.totalSurcharge = tots.Lines.totalSurcharge;//.add(tots.Bottom.surcharge);
        tots.Summary.totalExpense   = tots.Bottom.expense;
        tots.Summary.totalGross     = tots.Bottom.totalGross;

        return tots;
    }

    // NET = FINAL
    public static BigDecimal calculateStatementLineNet(BigDecimal pPriceEntry, 
                                                         int pQuantity, 
                                                         BigDecimal pTaxRate, 
                                                         BigDecimal pDiscount,
                                                         BigDecimal pSurcharge)
    {
        
        BigDecimal bdTot1         = pPriceEntry.multiply(new BigDecimal(pQuantity));
        BigDecimal bdStmtDiscount = bdTot1.multiply(pDiscount.divide(new BigDecimal(100)));
        BigDecimal bdStmtTax      = bdTot1.subtract(bdStmtDiscount).multiply(pTaxRate.divide(new BigDecimal(100)));

        bdTot1 = bdTot1.subtract(bdStmtDiscount).add(bdStmtTax).add(pSurcharge);

        return bdTot1;
    }

    // This includes ENTRY PRICE - DISCOUNT
    public static BigDecimal calculateStatementLineB4Tax(BigDecimal pPriceEntry, 
                                                         int pQuantity, 
                                                         BigDecimal pTaxRate, 
                                                         BigDecimal pDiscount)
    {
        
        BigDecimal bdTot1         = pPriceEntry.multiply(new BigDecimal(pQuantity));
        BigDecimal bdStmtDiscount = bdTot1.multiply(pDiscount.divide(new BigDecimal(100)));

        bdTot1 = bdTot1.subtract(bdStmtDiscount);

        return bdTot1;
    }

}
