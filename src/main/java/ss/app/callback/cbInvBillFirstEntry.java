/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ss.app.callback;

import bb.app.bill.InventoryBill;
import bb.app.bill.ssoBillLine;
import bb.app.err.ErrCode;
import bb.app.inv.InventoryOps;
import static bb.app.inv.InventoryOps.filterGroups;
import static bb.app.inv.InventoryOps.getGroupOptionsasJSON;
import static bb.app.inv.InventoryOps.prepareGroupOptionsasJSON;
import bb.app.obj.ssoInvCategory;
import bb.app.obj.ssoItemOption;
import bb.app.obj.ssoItemOptionZipped;
import bb.app.obj.ssoVendorItemOptionStatsCollection;
import bb.app.obj.ssoVendorItemStatsCollection;
import bb.app.obj.ssoVendorItemStatsCore;
import bb.app.stats.ssStatsOps;
import bb.app.stats.ssStatsQuery;
import bb.app.txn.txnDefs;
import bb.cashier.ssbMenuOps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemPrice;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvOptionStats;
import entity.cashier.SsCshMenu;
import entity.dct.SsDctInvVendorSummary;
import entity.stmt.SsStmInvStatements;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import jaxesa.log.LogManager;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class cbInvBillFirstEntry 
{
    /*
        pjsaStmtLines = itemCode || category || quantity || entryPrice || discount || surcharge || options || salesPrice

    */
    //------------------------------------------------------------------
    // IMPORTANT: This function used by both 
    // - Callback worker
    // - File Importer worker
    // So be careful to change
    //------------------------------------------------------------------
    public static ArrayList<ErrCode> updateStats(   EntityManager     pem,
                                                    String            pBillId,
                                                    String            psUserId,
                                                    String            psAccId,
                                                    String            pTxnType,
                                                    String            pBrand,
                                                    String            pBrandId,
                                                    String            pBillStmtDate,
                                                    String            pDesc,
                                                    String            pBottomDiscRate,
                                                    String            pBottomSurcharge,
                                                    String            pTaxRate,
                                                    JsonArray         pjsaStmtLines)
    {
        ArrayList<ErrCode> aReports = new ArrayList<ErrCode>(); 

        String sBillId    = pBillId;
        try
        {

            BigInteger   lBillId = new BigInteger(sBillId);
            String sUserId       = psUserId;
            String sAccId        = psAccId;
            String sTxnType      = pTxnType;
            String sBrand        = pBrand;
            String sBrandId      = pBrandId;
            String sStmtDate     = pBillStmtDate;
            String sDesc         = pDesc;

            // LOG PURPOSE
            aReports.add(new ErrCode(0, "updateStats {"  + sBillId + "}"));

            String sBottomDiscRate  = pBottomDiscRate;
            BigDecimal bdBottomDiscRate = new BigDecimal(sBottomDiscRate);
            bdBottomDiscRate = bdBottomDiscRate.divide(new BigDecimal(100), 5, RoundingMode.HALF_DOWN);//decimal 5 points

            String sBottomSurcharge = pBottomSurcharge;

            //String sStmtDets  = (String)Util.Callback.getParamValue(aParams, "dts");
            String sTax       = pTaxRate;

            BigDecimal bdTaxRate = new BigDecimal(sTax);
            bdTaxRate = bdTaxRate.divide(new BigDecimal(100), 5, RoundingMode.HALF_DOWN);//decimal 5 points

            BigInteger lUserId  = new BigInteger(sUserId);
            BigInteger lAccId   = new BigInteger(sAccId);
            BigInteger lBrandId = new BigInteger(sBrandId);

            //JsonObject jsoInvStmt = Util.JSON.toJsonObject(sStmtDets);
            //String sStmtLines = jsoInvStmt.get("stmtLines").toString();
            String sStmtRefNum = "";

            //JsonArray jsaStmtLines= Util.JSON.toArray(sStmtLines);
            JsonArray jsaStmtLines = pjsaStmtLines;

            //SsDctInvVendorSummary vendorSummary = new SsDctInvVendorSummary();
            //vendorSummary = DictionaryOps.Vendor.getVendor(pem, lAccId, sBrand);
            //vendorSummary = DictionaryOps.Vendor.getVendor(pem, lUserId, sBrand);

            ArrayList<String> aCategories = new ArrayList<String>();
            ArrayList<SsAccInvItemPrice> aPrices    = new ArrayList<SsAccInvItemPrice>();
            ArrayList<ssoVendorItemStatsCollection> aItemStats = new ArrayList<ssoVendorItemStatsCollection>();
            ArrayList<ssoVendorItemOptionStatsCollection> aAllStmtOptionStats = new ArrayList<ssoVendorItemOptionStatsCollection>();

            BigDecimal bdTotalQuantity = new BigDecimal(BigInteger.ZERO);

            // LOG PURPOSE
            aReports.add(new ErrCode(0, "Retrieving Bill Lines - "  + sBillId));

            //Util.Arrays.distinctString(aCategories)
            ArrayList<ssoBillLine> invStatement = new ArrayList<ssoBillLine>();
            for (int j=0; j<jsaStmtLines.size();j++)
            {
                JsonObject line = (JsonObject)jsaStmtLines.get(j);

                String sItemCode   = line.get("itemCode").toString().replace("\"", "");
                String sCategory   = line.get("category").toString().replace("\"", "");
                String sQuantity   = line.get("quantity").toString().replace("\"", "");
                String sEntryPrice = line.get("entryPrice").toString().replace("\"", "");
                String sDiscount   = line.get("discount").toString().replace("\"", "");
                String sSurcharge  = line.get("surcharge").toString().replace("\"", "");
                //String sTax        = line.get("tax").toString().replace("\"", "");
                String sOptions    = line.get("options").getAsString();
                String sSalesPrice = line.get("salesPrice").toString().replace("\"", "");

                sBrand    = sBrand.trim();
                sCategory = sCategory.trim();
                sItemCode = sItemCode.trim();

                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1!!
                //
                // Steps to new inventory transaction
                // 1. Register Category Category (can be cachable)
                // 2. Read + Update/Write Brand Balance 
                // 3. Add Item Price Info
                // 4. Read + Update/Write Item Quantity
                // 5. Read + Update/Write Item Options
                // 6. Register Brand & Item Code 
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                // 1. Register New Category (if new)
                //------------------------------------------------------------------

                // collect category
                aCategories.add(sCategory);

                // 3. Add Item Price Info
                //------------------------------------------------------------------

                // collect price dets
                SsAccInvItemPrice priceN = new SsAccInvItemPrice();
                priceN.accountId  = lAccId;
                priceN.billId     = lBillId;
                priceN.brandId    = lBrandId;
                priceN.itemCode   = sItemCode;
                priceN.entryPrice = new BigDecimal(sEntryPrice);
                priceN.discount   = new BigDecimal(sDiscount);
                priceN.tax        = new BigDecimal(sTax);
                priceN.salePrice  = new BigDecimal(sSalesPrice);
                aPrices.add(priceN);

                // 4. Update Item (ITEM-STATS-UPDATE)
                //------------------------------------------------------------------
                
                // collect item dets
                ssoVendorItemStatsCollection itemStatN = new ssoVendorItemStatsCollection();
                itemStatN.itemCode    = sItemCode;

                int bExistingInArrayIndex = -1;
                for(int i=0;i<aItemStats.size();i++)
                {
                    ssoVendorItemStatsCollection statN = aItemStats.get(i);

                    if(statN.itemCode.toLowerCase().trim().equals(sItemCode.toLowerCase().trim())==true)
                    {
                        bExistingInArrayIndex = i;
                        break;
                    }
                }

                if(bExistingInArrayIndex!=-1)
                {
                    // The same Item Code entered multiple times IN THE STMT

                    //ONLY THE QUANTITY WILL BE ADDED THE REST WILL BE USED FROM THE LAST VALUE ADDED
                    itemStatN = aItemStats.get(bExistingInArrayIndex);

                    itemStatN.quantity    = itemStatN.quantity.add(new BigDecimal(sQuantity));

                }
                else
                { 
                    // NEW ENTRY IN THE STATMENT
                    itemStatN.quantity    = new BigDecimal(sQuantity);
                }

                // ALWAYS THE LAST ADDED VALUE WILL BE USED 
                itemStatN.surcharge   = new BigDecimal(sSurcharge);

                itemStatN.discountRate= new BigDecimal(sDiscount);
                itemStatN.discountRate = itemStatN.discountRate.divide(new BigDecimal(100));

                itemStatN.entryPrice  = new BigDecimal(sEntryPrice);
                itemStatN.salesPrice  = new BigDecimal(sSalesPrice);
                itemStatN.category    = sCategory;

                if(bExistingInArrayIndex==-1)
                    aItemStats.add(itemStatN);//add if new entry otherwise just update

                bdTotalQuantity = bdTotalQuantity.add(itemStatN.quantity);

                // 5. Update Item Options Quantity (OPT-STATS-UPDATE)
                //------------------------------------------------------------------

                //collect options
                ssoVendorItemOptionStatsCollection newOptStatsN = new ssoVendorItemOptionStatsCollection();

                //ArrayList<ssoItemOption> options = new ArrayList<ssoItemOption>();
                newOptStatsN.itemCode      = sItemCode;

                ArrayList<ssoItemOption> newOptionsX = new ArrayList<ssoItemOption>();
                ArrayList<ssoItemOption> newSortedOptions = new ArrayList<ssoItemOption>();
                newOptionsX      = InventoryOps.getItemOptions(sOptions);
                newSortedOptions = SortOptions(newOptionsX);

                // find Item Code Options (no find if first time adding)
                //----------------------------------------------------------
                int iItemOptionsIndex = -1;
                for(int i=0;i<aAllStmtOptionStats.size();i++)
                {
                    ssoVendorItemOptionStatsCollection libOptN = new ssoVendorItemOptionStatsCollection();
                    libOptN = aAllStmtOptionStats.get(i);
                    if(libOptN.itemCode.toLowerCase().trim().equals(sItemCode.toLowerCase().trim())==true)
                    {
                        iItemOptionsIndex = i;
                        break;
                    }
                }

                // Existing Opt (to manage multiple entry of same itemcode)

                // Check against multiple entries of option for the itemcode
                //---------------------------------------------------------- 
                if(iItemOptionsIndex!=-1)
                {
                    //item-options added before so just update the options and add quantity
                    //----------------------------------------------------------
                    ssoVendorItemOptionStatsCollection existingOptStats = aAllStmtOptionStats.get(iItemOptionsIndex);

                    ArrayList<ssoItemOption> tempOptions = new ArrayList<ssoItemOption>();
                    ArrayList<ssoItemOption> tempSortedOptions = new ArrayList<ssoItemOption>();
                    tempOptions.addAll(newSortedOptions);
                    tempOptions.addAll(existingOptStats.options);

                    tempSortedOptions = SortOptions(tempOptions);
                    existingOptStats.options = tempSortedOptions;

                    // also add quantity 
                    if((existingOptStats.totalQuantity.trim().equals("")==true) || (existingOptStats.totalQuantity.trim().equals("-1")==true))
                    {
                        //quantity ignored for existing item option entry 
                        if(!((sQuantity.trim().equals("")==true)||sQuantity.trim().equals("-1")==true))
                        {
                            //quantity not ignored for this entry so let this overwrites
                            
                            // existing ignored but this is not 
                            existingOptStats.totalQuantity  = sQuantity;
                        }
                        else
                        {
                            //quantity ignored for this entry too - so ignore do no change
                            
                            // existing ignored + this is ignored - so just skip
                        }
                    }
                    else
                    {
                       //existing not ignored 
                       if(!((sQuantity.trim().equals("")==true)||sQuantity.trim().equals("-1")==true))
                       {
                           //this is not ignored - so add up
                           existingOptStats.totalQuantity = new BigDecimal(existingOptStats.totalQuantity).add(new BigDecimal(sQuantity)).toString();
                       }
                       else
                       {
                           // this new is ignored 
                           //existing not ignored + this new is ignored - so just skip 
                       }
                    }

                }
                else
                {
                    // NO DOUBLE ENTRY OF ITEM CODE - FIRST TIME ADDING 
                    // newOptStatsN.options = InventoryOps.getItemOptions(sOptions);
                    newOptStatsN.options = newSortedOptions;//first time entry of itemcode
                    newOptStatsN.totalQuantity = sQuantity;
                    aAllStmtOptionStats.add(newOptStatsN);
                }

                //newOptStatsN.options = InventoryOps.getItemOptions(sOptions);
                //newOptStatsN.totalQuantity = sQuantity;
                //aAllStmtOptionStats.add(newOptStatsN);

            }//END OF for statement lines

            //------------------------------------------------------------------
            //------------------------------------------------------------------

            boolean bTxnReturn = false;

            if(txnDefs.TXN_TYPE_INVENTORY_SENT.equals(sTxnType.trim())==true)
            {
                bTxnReturn = true;
            }

            // LOGGING
            aReports.add(new ErrCode(0, "Registering Category - " + sBillId + " return: " + bTxnReturn));

            // Step 1: Registering Categories (Create new one if needed otherwise update)
            ArrayList<ssoInvCategory> aoCategories = new ArrayList<ssoInvCategory>();
            aoCategories = InventoryOps.registerCategories(pem, lUserId, lAccId, lBrandId, aCategories);

            // LOGGING
            aReports.add(new ErrCode(0, "Setting Category Items - " + sBillId));

            // Update Category Ids for Item Stats (Needed for Cash Register Menu)
            setCategoryOfItemStats(aoCategories, aItemStats);

            // Step 2: Registering Prices
            InventoryOps.registerItemPrices(pem, aPrices);

            // Calculate Total Quantity

            // Step 3: Register Item Stats (Create new if needed otherwise update)
            if(txnDefs.TXN_TYPE_INV_LOAD.equals(sTxnType.trim())==false)
            {
                // LOGGING
                aReports.add(new ErrCode(0, "registerItemStats - " + sBillId));

                registerItemStats(pem,
                                  lAccId,
                                  lBrandId, 
                                  bTxnReturn, 
                                  bdTotalQuantity.toString(),
                                  bdBottomDiscRate.toString(),
                                  sBottomSurcharge, 
                                  bdTaxRate.toString(),
                                  aItemStats,
                                  aPrices);
            }
            else // Existing Entry 
            {
                // LOGGING 
                aReports.add(new ErrCode(0, "registerItemStats4ExistingEntry - " + sBillId));

                // Update Item Stats 4 Only-Quantities
                registerItemStats4ExistingEntry(pem, 
                                                lAccId, 
                                                lBrandId, 
                                                aItemStats, 
                                                aPrices);
            }

            // LOGGING
            aReports.add(new ErrCode(0, "getCurrentSummaryItemCodes - " + sBillId));

            // get existing summary of item codes
            ArrayList<ssoSummaryItemCode> aCurItemCodeLookup = new ArrayList<ssoSummaryItemCode>();
            aCurItemCodeLookup = cbCallbackOps.getCurrentSummaryItemCodes(pem,
                                                                          lAccId,
                                                                          lBrandId);

            // LOGGING
            aReports.add(new ErrCode(0, "registerOptionStats - " + sBillId));

            // Step 4: Register Option Stats (Create new if needed otherwise update)
            registerOptionStats( pem,
                                 lAccId,
                                 lBrandId,
                                 sTxnType,
                                 "",
                                 sBottomDiscRate,
                                 sBottomSurcharge,
                                 sTax,
                                 aAllStmtOptionStats,
                                 aCurItemCodeLookup);

            // Step 5: Update Dictionary
            /*DictionaryOps.Vendor.add_VendorNItemCodes(  pem, 
                                                            lUserId,
                                                            lAccId, 
                                                            sBrand.toUpperCase().trim(), 
                                                            sItemCode,
                                                            lBrandId,
                                                            brndItem.uid,
                                                            sSalesPrice);*/

            // LOGGING
            aReports.add(new ErrCode(0, "updateCashierMenu - " + sBillId));

            // update cashier menu
            //-----------------------------------------------------------------
            ssbMenuOps.updateCashierMenu(pem, lAccId);

            // LOGGING
            aReports.add(new ErrCode(0, "moveBill2EOD - " + sBillId));

            // Move BILL to END OF DAY table (EOD)
            //-----------------------------------------------------------------
            InventoryBill.moveBill2EOD(pem, lBillId);

            // Merge into Statment table
            //-----------------------------------------------------------------
            // LOGGING
            aReports.add(new ErrCode(0, "mergeBill2Statement - " + sBillId));
            
            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            int txnYear  = Integer.parseInt(sStmtDate.substring(0, 4));

            boolean bRecalcRevolving = false;
            if(ThisYear!=txnYear)
                bRecalcRevolving = true;//entry or update for a previous year therefore revolving must be recalculated

            InventoryBill.mergeBill2Statement(pem, lAccId, lBrandId, lBillId, bRecalcRevolving);

            // FLUSH MEMORYS RELATED tables: ss_acc_item_stats 
            // LOGGING
            aReports.add(new ErrCode(0, "resetMemoryTables - " + sBillId));

            cbCallbackOps.resetMemoryTables(pem, lUserId, lAccId, lBrandId);

            return aReports;
        }
        catch(Exception e)
        {
            //String sErrMsg = "";
            //sErrMsg = e.getMessage();
            //LogManager.SysLog(-1, "New Inv/Bill Entry Callback Exception @ cbInventoryBill_FirstEntry : " + sErrMsg);
            aReports.add(new ErrCode(0, "Exception @ updateStats - " + sBillId + " details: " + e.getMessage()));
            
            return aReports;
        }
    }
    
    // This walk thru options and gathers the same options by only adding quantities
    public static ArrayList<ssoItemOption> SortOptions(ArrayList<ssoItemOption> paItemOptions)
    {
        ArrayList<ssoItemOption> aSortedOptions = new ArrayList<ssoItemOption>();
        
        for(ssoItemOption newOptN: paItemOptions)
        {
            String sNewOptPath1 = newOptN.groupName.trim().toLowerCase() + "." + newOptN.optionName.trim().toLowerCase();// BLACK -L
            String sNewOptPath2 = newOptN.optionName.trim().toLowerCase() + "." + newOptN.groupName.trim().toLowerCase();// L - BLACK
            
            boolean bFound = false;
            for(ssoItemOption sortedOptN: aSortedOptions)
            {
                String sSortedPathN = sortedOptN.groupName.trim().toLowerCase() + "." + sortedOptN.optionName.trim().toLowerCase();

                if((sNewOptPath1.equals(sSortedPathN)==true) || (sNewOptPath2.equals(sSortedPathN)==true) )// BLACK-L or L-BLACK 
                {
                    bFound = true;
                    // THIS OPTION ADDED BEFORE 

                    if(!((newOptN.quantity.trim().equals("-1")==true) || (newOptN.quantity.trim().equals("")==true)))
                    {
                        // just add quantity 
                        if((sortedOptN.quantity.trim().equals("-1")==true) || (sortedOptN.quantity.trim().equals("")==true))//added opt quantity ignored
                            sortedOptN.quantity = new BigDecimal(newOptN.quantity).toString();//use quantity of existing one as default
                        else
                            sortedOptN.quantity = (new BigDecimal(sortedOptN.quantity).add(new BigDecimal(newOptN.quantity))).toString();//add up
                    }
                    else
                    {
                        // new option added as to ignore the quantity - continue using the existing option
                    }
                    
                    break;
                }

            }

            if(bFound==false)
            {
                aSortedOptions.add(newOptN);
            }
        }

        return aSortedOptions;
    }

    public static void setCategoryOfItemStats(ArrayList<ssoInvCategory>               paCategories, 
                                               ArrayList<ssoVendorItemStatsCollection> paItemStats)
    {
        for(ssoVendorItemStatsCollection statN: paItemStats)
        {
            BigInteger lCategoryId = findCategoryId(paCategories, statN.category);
            
            statN.categoryId = lCategoryId;
        }
    }

    public static BigInteger findCategoryId(ArrayList<ssoInvCategory>               paCategories, String pCategoryName)
    {
        for(ssoInvCategory categoryN:paCategories)
        {
            if(categoryN.name.trim().toLowerCase().equals(pCategoryName.trim().toLowerCase())==true)
                return categoryN.uid;
        }

        return BigInteger.ZERO;
    }

    public static void registerItemStats(EntityManager                                 pem,
                                         BigInteger                                          pAccId,
                                         BigInteger                                          pVendorId,
                                         boolean                                       pbTxnReturn,
                                         String                                        psBillTotalQuantity,
                                         String                                        psBottomDiscRate,
                                         String                                        psBottomSurcharge,//EXPENSE
                                         String                                        psTaxRate,
                                         ArrayList<ssoVendorItemStatsCollection>       paItemStats,
                                         ArrayList<SsAccInvItemPrice>                  paPrices) throws Exception
    {
        try
        {
            // First update 
            // the ones NOT updated are NEW. INSERT for them
            boolean bUpdatePriceId    = true;
            boolean bUpdateCategoryId = true;
            if(pbTxnReturn==true)
            {
                bUpdatePriceId = false;
                bUpdateCategoryId = false;
            }

            String sInitQuery4Upd = ssStatsQuery.generateUpdateItemStatsQuery4Change(pbTxnReturn, false, bUpdatePriceId, bUpdateCategoryId);
            Query qryUpd = pem.CreateNativeQuery(sInitQuery4Upd);

            BigDecimal bdTotalQuantity = new BigDecimal(psBillTotalQuantity);
            BigDecimal bdPerItemBottomSurcharge = new BigDecimal(psBottomSurcharge).divide(bdTotalQuantity, 5, RoundingMode.HALF_UP);

            //qryUpd.addBatch();
            int index = 0;
            for(ssoVendorItemStatsCollection statN: paItemStats)
            {
                //BigDecimal quantityN = new BigDecimal(statN.quantity);
                BigDecimal quantityN = statN.quantity;

                ssoVendorItemStatsCore  itemStatsCore = new ssoVendorItemStatsCore();
                SsAccInvItemPrice priceN              = new SsAccInvItemPrice();

                priceN = paPrices.get(index);

                //BigDecimal bdStatDiscRate = new BigDecimal(statN.discountRate);
                BigDecimal bdStatDiscRate = statN.discountRate;
                //bdStatDiscRate = bdStatDiscRate.divide(new BigDecimal(100), 5, RoundingMode.HALF_DOWN);//decimal 5 points

                itemStatsCore = InventoryBill.calcItemStats(statN.quantity.toString(),
                                                            statN.entryPrice.toString(),
                                                            bdStatDiscRate.toString(),
                                                            statN.surcharge.toString(),
                                                            psBottomDiscRate,
                                                            bdPerItemBottomSurcharge.toString(),
                                                            psTaxRate);

                // Set Params
                int Colindex = 1;
                if(pbTxnReturn==false)
                {
                    if (bUpdatePriceId==true)
                        qryUpd.SetParameter(Colindex++, priceN.uid               , "PRICE_ID");

                    qryUpd.SetParameter(Colindex++, priceN.salePrice             , "LAST_SALE_PRICE");
                    qryUpd.SetParameter(Colindex++, priceN.entryPrice            , "LAST_ENTRY_PRICE");
                    qryUpd.SetParameter(Colindex++, statN.categoryId             , "PRM_CATEGORY_ID");  //DISCOUNT_TOTAL++
                }

                qryUpd.SetParameter(Colindex++, quantityN                    , "QUANTITY");
                qryUpd.SetParameter(Colindex++, itemStatsCore.netTotal       , "NET");
                qryUpd.SetParameter(Colindex++, itemStatsCore.discTotal      , "DISCOUNT");
                qryUpd.SetParameter(Colindex++, itemStatsCore.surchargeTotal , "SURCHARGE");
                qryUpd.SetParameter(Colindex++, itemStatsCore.taxTotal       , "TAX");
                qryUpd.SetParameter(Colindex++, itemStatsCore.expenseTotal   , "EXPENSE");
                qryUpd.SetParameter(Colindex++, itemStatsCore.grossTotal     , "GROSS");

                qryUpd.SetParameter(Colindex++, pAccId                       , "ACCOUNT_ID");        //QUANTITY++
                qryUpd.SetParameter(Colindex++, pVendorId                    , "VENDOR_ID");     //GROSS_TOTAL++
                qryUpd.SetParameter(Colindex++, statN.itemCode               , "ITEM_CODE"); //SURCHARGE_TOTAL++

                qryUpd.addBatch();

                index++;
            }

            int [] iAffectedRowCounts = qryUpd.executeBatch();

            // CHECK WHICH ONES ARE NOT AFFECTED. FOR THEM RUN INSERT BATCH
            //------------------------------------------------------------------
            String sTxnType = "";
            if(pbTxnReturn==true)
                sTxnType = txnDefs.TXN_TYPE_INVENTORY_SENT;//RETURN/SENT
            else
                sTxnType = txnDefs.TXN_TYPE_INVENTORY_RECEIVED;

            String sInitQuery4New = ssStatsQuery.generateUpdateItemStatsQuery4New(sTxnType);
            Query qryNew = pem.CreateNativeQuery(sInitQuery4New);

            int iCounter = 0;
            for(int i=0;i<iAffectedRowCounts.length;i++)
            {
                if(iAffectedRowCounts[i]==0)
                {
                    iCounter++;

                    ssoVendorItemStatsCollection statN = new ssoVendorItemStatsCollection();
                    SsAccInvItemPrice priceN = new SsAccInvItemPrice();

                    statN  = paItemStats.get(i);
                    priceN = paPrices.get(i);

                    // SET INSERT PARAMS 
                    //-------------------------------------------------------------
                    //BigDecimal quantityN = new BigDecimal(statN.quantity);
                    //BigDecimal bdStatDiscRate = new BigDecimal(statN.discountRate);
                    BigDecimal quantityN = statN.quantity;
                    BigDecimal bdStatDiscRate = statN.discountRate;

                    //bdStatDiscRate = bdStatDiscRate.divide(new BigDecimal(100), 5, RoundingMode.HALF_DOWN);//decimal 5 points

                    ssoVendorItemStatsCore  itemStatsCore = new ssoVendorItemStatsCore();

                    itemStatsCore = InventoryBill.calcItemStats(statN.quantity.toString(),
                                                                statN.entryPrice.toString(),
                                                                bdStatDiscRate.toString(),
                                                                statN.surcharge.toString(),
                                                                psBottomDiscRate, 
                                                                bdPerItemBottomSurcharge.toString(),
                                                                psTaxRate);
                    // set params 
                    int Colindex = 1;
                    qryNew.SetParameter(Colindex++, priceN.uid                   , "PRICE_ID");
                    qryNew.SetParameter(Colindex++, priceN.entryPrice            , "LAST_ENTRY_PRICE");
                    qryNew.SetParameter(Colindex++, priceN.salePrice             , "LAST_SALE_PRICE");
                    qryNew.SetParameter(Colindex++, quantityN                    , "QUANTITY");
                    qryNew.SetParameter(Colindex++, itemStatsCore.netTotal       , "NET");
                    qryNew.SetParameter(Colindex++, itemStatsCore.discTotal      , "DISCOUNT");
                    qryNew.SetParameter(Colindex++, itemStatsCore.surchargeTotal , "SURCHARGE");
                    qryNew.SetParameter(Colindex++, itemStatsCore.taxTotal       , "TAX");
                    qryNew.SetParameter(Colindex++, itemStatsCore.expenseTotal   , "EXPENSE");
                    qryNew.SetParameter(Colindex++, itemStatsCore.grossTotal     , "GROSS");

                    qryNew.SetParameter(Colindex++, pAccId                       , "ACCOUNT_ID");        //QUANTITY++
                    qryNew.SetParameter(Colindex++, pVendorId                    , "VENDOR_ID");     //GROSS_TOTAL++
                    qryNew.SetParameter(Colindex++, statN.itemCode               , "ITEM_CODE"); //SURCHARGE_TOTAL++
                    qryNew.SetParameter(Colindex++, statN.categoryId             , "PRM_CATEGORY_ID");  //DISCOUNT_TOTAL++

                    // add 2 batch
                    //--------------------------------------------------
                    qryNew.addBatch();
                }
            }
            
            int[] iAffectedRowCounts2;
            if(iCounter>0)
            {
                iAffectedRowCounts2 = qryNew.executeBatch();
                
                int i = 0;
            }
            
            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void registerItemStats4ExistingEntry(EntityManager                                 pem,
                                                       BigInteger                                          pAccId,
                                                       BigInteger                                          pVendorId,
                                                       ArrayList<ssoVendorItemStatsCollection>       paItemStats,
                                                       ArrayList<SsAccInvItemPrice>                  paPrices)
    {
        try
        {
            String sInitQuery4Upd = ssStatsQuery.generateUpdateItemStatsQuery4ExistingEntry();

            Query qryUpd = pem.CreateNativeQuery(sInitQuery4Upd);
            //qryUpd.addBatch();
            int index = 0;
            for(ssoVendorItemStatsCollection statN: paItemStats)
            {
                BigDecimal quantityN = statN.quantity;

                ssoVendorItemStatsCore  itemStatsCore = new ssoVendorItemStatsCore();
                SsAccInvItemPrice priceN              = new SsAccInvItemPrice();

                priceN = paPrices.get(index);

                int Colindex = 1;
                qryUpd.SetParameter(Colindex++, quantityN                    , "QUANTITY");
                qryUpd.SetParameter(Colindex++, priceN.uid                   , "PRICE_ID");
                    
                qryUpd.SetParameter(Colindex++, priceN.salePrice             , "LAST_SALE_PRICE");
                qryUpd.SetParameter(Colindex++, statN.categoryId             , "PRM_CATEGORY_ID");  //DISCOUNT_TOTAL++

                qryUpd.SetParameter(Colindex++, pAccId                       , "ACCOUNT_ID");        //QUANTITY++
                qryUpd.SetParameter(Colindex++, pVendorId                    , "VENDOR_ID");     //GROSS_TOTAL++
                qryUpd.SetParameter(Colindex++, statN.itemCode               , "ITEM_CODE"); //SURCHARGE_TOTAL++

                qryUpd.addBatch();
                
                index++;
            }

            int [] iAffectedRowCounts = qryUpd.executeBatch();

            // THE RECS NOT EXISTS SO CREATE NEW ONE
            //------------------------------------------------------------------

            String sInitQuery4New = ssStatsQuery.generateUpdateItemStatsQuery4ExistingEntry_NewRecord();

            Query qryNew = pem.CreateNativeQuery(sInitQuery4New);

            int iCounter = 0;
            for(int i=0;i<iAffectedRowCounts.length;i++)
            {
                if(iAffectedRowCounts[i]==0)
                {
                    iCounter++;

                    ssoVendorItemStatsCollection statN = new ssoVendorItemStatsCollection();
                    SsAccInvItemPrice priceN = new SsAccInvItemPrice();

                    statN  = paItemStats.get(i);
                    priceN = paPrices.get(i);

                    BigDecimal quantityN = statN.quantity;

                    int Colindex = 1;
                    qryNew.SetParameter(Colindex++, priceN.uid                   , "PRICE_ID");
                    qryNew.SetParameter(Colindex++, priceN.salePrice             , "LAST_SALE_PRICE");
                    qryNew.SetParameter(Colindex++, quantityN                    , "QUANTITY");
                    qryNew.SetParameter(Colindex++, pAccId                       , "ACCOUNT_ID");        //QUANTITY++
                    qryNew.SetParameter(Colindex++, pVendorId                    , "VENDOR_ID");     //GROSS_TOTAL++
                    qryNew.SetParameter(Colindex++, statN.itemCode               , "ITEM_CODE"); //SURCHARGE_TOTAL++

                    // add 2 batch
                    //--------------------------------------------------
                    qryNew.addBatch();
                }
            }
            
            int[] iAffectedRowCounts2;
            if(iCounter>0)
            {
                iAffectedRowCounts2 = qryNew.executeBatch();
                
                int i = 0;
            }
            
        }
        catch(Exception e)
        {
            String sMsg = e.getMessage();
            sMsg = sMsg;
        }
    }

    public static void registerOptionStats( EntityManager                                 pem,
                                            BigInteger                                          pAccId,
                                            BigInteger                                          pVendorId,
                                            String                                        pTxnType,
                                            //boolean                                       pbTxnType,
                                            String                                        psTotalItemQuantity,
                                            String                                        psBottomDiscRate,
                                            String                                        psBottomSurcharge,
                                            String                                        psTaxRate,
                                            ArrayList<ssoVendorItemOptionStatsCollection> paIncomingItemOptions,
                                            ArrayList<ssoSummaryItemCode>                 paCurItemCodeLookup) throws Exception
    {
        boolean bTxnReturn = false;
        String sOptions    = "";

        try
        {
            if(txnDefs.TXN_TYPE_INVENTORY_SENT.equals(pTxnType.trim())==true)
            {
                bTxnReturn = true;
            }

            ArrayList<SsAccInvOptionStats> aItemOptions = new ArrayList<SsAccInvOptionStats>();

            for(int i=0;i<paIncomingItemOptions.size();i++)
            {
                ssoVendorItemOptionStatsCollection optN = new ssoVendorItemOptionStatsCollection();
                optN = paIncomingItemOptions.get(i);

                ssoSummaryItemCode optItemCodeSmry = new ssoSummaryItemCode();
                optItemCodeSmry = ssStatsOps.getItemCodeSummaryIdFromSummaryTable(pVendorId.toString(), optN.itemCode, paCurItemCodeLookup);

                ArrayList<SsAccInvOptionStats> optNStats = new ArrayList<SsAccInvOptionStats>();

                optNStats = prepareOptionNStats(pem,
                                                pAccId,
                                                pVendorId,
                                                pTxnType,
                                                optN.itemCode,
                                                optItemCodeSmry.itemCodeId,
                                                psTotalItemQuantity,
                                                psBottomDiscRate,
                                                psBottomSurcharge,
                                                psTaxRate,
                                                optN);
                
                aItemOptions.addAll(optNStats);
            }

            //------------------------------------------------------------------
            // UPDATE STATS
            //------------------------------------------------------------------
            // 1st Update
            // 2nd Insert (if not exist / updated)

            String sQuery4Upd = ssStatsQuery.generateUpdateOptionStatsQuery4Update(bTxnReturn);
            Query qryUpd = pem.CreateNativeQuery(sQuery4Upd);

            for (SsAccInvOptionStats optionN: aItemOptions)
            {
                sOptions          = "";
                String sOptionsFormatted = "";

                /*
                if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true)
                {
                    sOptions = "{" + Util.Str.QUOTE("received") + ":" + "{" + Util.Str.QUOTE(optionN.optionGroup) + ":" + pOptions + "}" + "}";
                }
                else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
                {
                    sOptions = "{" + Util.Str.QUOTE("returned") + ":" + "{" + Util.Str.QUOTE(optionN.optionGroup) + ":" + pOptions + "}" + "}";
                }
                */
                if (bTxnReturn==false)
                    sOptions = optionN.optionsEntered;
                else
                    sOptions = optionN.optionsReturned;
                
                //sOptionsFormatted = generateOptionsValue(bTxnReturn, optionN.optionGroup, sOptions);

                // Set Params
                int Colindex = 1;
                qryUpd.SetParameter(Colindex++, sOptions                     , "OPTIONS");
                qryUpd.SetParameter(Colindex++, pAccId                       , "ACCOUNT_ID");
                qryUpd.SetParameter(Colindex++, pVendorId                    , "VENDOR_ID");
                qryUpd.SetParameter(Colindex++, optionN.itemCodeId           , "ITEM_CODE_ID");
                qryUpd.SetParameter(Colindex++, optionN.optionGroup          , "OPT_GROUP");

                //add batch
                qryUpd.addBatch();

            }//END OF UPDATE FOR

            int [] iAffectedRowCounts = qryUpd.executeBatch();

            //------------------------------------------------------------------
            // CREATE NEW OPTIONS
            //------------------------------------------------------------------
            String sQuery4New = ssStatsQuery.generateUpdateOptionStatsQuery4New(bTxnReturn);
            Query qryNew      = pem.CreateNativeQuery(sQuery4New);
            
            int iUpdCounter = 0;
            for(int i=0;i<iAffectedRowCounts.length;i++)
            {
                sOptions          = "";
                String sOptionsFormatted = "";

                int iAffectedRowNum = iAffectedRowCounts[i];
                SsAccInvOptionStats optN = aItemOptions.get(i);

                if (bTxnReturn==false)
                    sOptions = optN.optionsEntered;
                else
                    sOptions = optN.optionsReturned;

                //sOptionsFormatted = generateOptionsValue(bTxnReturn, optN.optionGroup, sOptions);

                if(iAffectedRowNum==0)
                {
                    // no record found create new one 
                    //----------------------------------------------------------
                    
                    // Set Params
                    int Colindex = 1;
                    qryNew.SetParameter(Colindex++, optN.optionGroup             , "OPT_GROUP");
                    qryNew.SetParameter(Colindex++, sOptions                     , "OPTIONS");
                    qryNew.SetParameter(Colindex++, pAccId                       , "ACCOUNT_ID");
                    qryNew.SetParameter(Colindex++, pVendorId                    , "VENDOR_ID");
                    qryNew.SetParameter(Colindex++, optN.itemCodeId              , "ITEM_CODE_ID");
                    qryNew.SetParameter(Colindex++, optN.prmCategoryId           , "PRM_CATEGORY_ID");

                    //add batch
                    qryNew.addBatch();

                    iUpdCounter++;
                }
                else if (iAffectedRowNum<0)
                {
                    // Fail add to log 
                    //----------------------------------------------------------

                }
            }

            if(iUpdCounter>0)
            {
                int [] iAffectedRowCounts2 = qryNew.executeBatch();

                int z = 0;
            }

        }
        catch(Exception e)
        {
            //LogManager.SysLog(-1, "Exception @registerOptionStats method @cbInventoryBill_FirstEntry : " + e.getMessage() + " #options: " + sOptions);
            throw e;
        }

    }

    public static ArrayList<SsAccInvOptionStats> prepareOptionNStats(   EntityManager                           pem,
                                                                        BigInteger                                    pAccId,
                                                                        BigInteger                                    pVendorId,
                                                                        String                                  pTxnType,
                                                                        String                                  pItemCode,
                                                                        BigInteger                                    pItemCodeId,
                                                                        String                                  psTotalItemQuantity,
                                                                        String                                  psBottomDiscRate,
                                                                        String                                  psBottomSurcharge,
                                                                        String                                  psTaxRate,
                                                                        ssoVendorItemOptionStatsCollection      paItemOptions) throws Exception
    {
        ArrayList<SsAccInvOptionStats> itemOptions = new ArrayList<SsAccInvOptionStats>();

        try
        {

            ArrayList<ssoItemOption> aOptions = new ArrayList<ssoItemOption>();
            boolean bNewItemOption = false;

            if(paItemOptions.options.size()==0)// NO OPTION ENTERED -> CREATE A DEFAULT OPTION 
            {
                //entry without options so then run it once for empty
                ssoItemOption noOption = new ssoItemOption();
                noOption.optionName = "";
                noOption.groupName  = "";

                if(pTxnType.equals(txnDefs.TXN_TYPE_INV_LOAD)==true)
                    noOption.quantity   = psTotalItemQuantity;
                else
                    noOption.quantity   = psTotalItemQuantity;

                aOptions.add(noOption);
            }
            else
            {
                aOptions.addAll(paItemOptions.options);
            }

            //-----------------------------------------------------------------
            // COLLECT GROUP NAMES WITH OPTIONS
            //-----------------------------------------------------------------
            ArrayList<String> optGroups = new ArrayList<String>();
            ArrayList<ssoItemOptionZipped> optGroupsZipped = new ArrayList<ssoItemOptionZipped>();

            optGroups = filterGroups(aOptions);
            for(String groupN: optGroups)
            {
                String sGroupJSON = prepareGroupOptionsasJSON(groupN, aOptions);

                ssoItemOptionZipped optGroupZip = new ssoItemOptionZipped();
                optGroupZip.groupName = groupN;
                optGroupZip.options   = sGroupJSON;
                optGroupsZipped.add(optGroupZip);
            }

            for(String groupN: optGroups)
            {
                SsAccInvOptionStats itemOptionN = new SsAccInvOptionStats();
                
                String sOptions = getGroupOptionsasJSON(groupN, optGroupsZipped);
                
                itemOptionN.accountId        = pAccId;
                itemOptionN.vendorId         = pVendorId;
                itemOptionN.itemCodeId       = pItemCodeId;//pBrandItem.uid;//uid == itemcode
                itemOptionN.prmCategoryId    = BigInteger.ZERO;//wont be set here
                itemOptionN.optionGroup      = groupN;
                itemOptionN.itemOptGroupHashMd5 = Util.crypto.md5.calculate(groupN.trim() + itemOptionN.itemCodeId.toString().trim());

                if ( (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true) || 
                     (pTxnType.equals(txnDefs.TXN_TYPE_INV_LOAD)==true) )
                {
                    // NEW ENTRY or EXISTING ITEM
                    itemOptionN.optionsEntered  = sOptions;
                    itemOptionN.optionsReturned = "{}";
                    itemOptionN.optionsCrSold     = "{}";
                    itemOptionN.optionsCrRefund   = "{}";

                    itemOptionN.optionsAdjMinus = "{}";
                    itemOptionN.optionsAdjPlus  = "{}";

                    itemOptionN.optionsRevolvingEntered  = "{}";
                    itemOptionN.optionsRevolvingReturned = "{}";
                    itemOptionN.optionsRevolvingCrSold     = "{}";
                    itemOptionN.optionsRevolvingCrRefund   = "{}";

                    itemOptionN.optionsRevolvingAdjMinus = "{}";
                    itemOptionN.optionsRevolvingAdjPlus  = "{}";

                }
                //else if (pTxnType.equals(InventoryStatement.INV_RETURN_ITEM)==true)
                else if (pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
                {
                    // RETURNING ITEM 
                    itemOptionN.optionsEntered   = "{}";
                    itemOptionN.optionsReturned  = sOptions;
                    itemOptionN.optionsCrSold     = "{}";
                    itemOptionN.optionsCrRefund   = "{}";
                    
                    itemOptionN.optionsAdjMinus = "{}";
                    itemOptionN.optionsAdjPlus  = "{}";

                    itemOptionN.optionsRevolvingEntered  = "{}";
                    itemOptionN.optionsRevolvingReturned = "{}";
                    itemOptionN.optionsRevolvingCrSold     = "{}";
                    itemOptionN.optionsRevolvingCrRefund   = "{}";
                    itemOptionN.optionsRevolvingAdjMinus = "{}";
                    itemOptionN.optionsRevolvingAdjPlus  = "{}";

                }

                itemOptions.add(itemOptionN);

            }//end of for

            return itemOptions;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    


}
