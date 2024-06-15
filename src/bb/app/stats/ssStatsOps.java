/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.stats;


import bb.app.bill.InventoryBill;
import bb.app.obj.ssoItemOption;
import bb.app.obj.ssoSummaryItemCode;
import bb.app.obj.ssoVendorItemOptionStatsCollection;
import bb.app.obj.ssoVendorItemStatsCollection;
import bb.app.obj.ssoVendorItemStatsCore;
import bb.app.txn.txnDefs;
import entity.acc.SsAccInvItemPrice;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class ssStatsOps 
{
    public static void updateVendorStats4CashRegisterTxn(EntityManager                           pem,
                                                         String                                  pUserId,
                                                         String                                  pAccId,
                                                         boolean                                 pbTxnReturn,
                                                         String                                  pTaxRate,
                                                         boolean                                 bTaxInPrice,
                                                         String                                  pCounterDiscRate,
                                                         ArrayList<ssoVendorItemStatsCollection> paItemStats)
    {
        try
        {
            long lAccId = Long.parseLong(pAccId);
            
            ArrayList<String> aVendors = new ArrayList<String>();
            ArrayList<String> aVendorsDistinct = new ArrayList<String>();

            for(ssoVendorItemStatsCollection statN: paItemStats)
            {
                aVendors.add(statN.vendorId);
            }

            aVendorsDistinct = (ArrayList<String>)Util.Arrays.distinct(aVendors);

            String sQueryUpd = ssStatsQuery.generateUpdateVendorStatsQuery4CashRegister(pbTxnReturn);

            Query qryUpd = pem.CreateNativeQuery(sQueryUpd);

            for(String vendorIdN: aVendorsDistinct)
            {
                ssoStatsCashierTxnTotals vendorNTotals = new ssoStatsCashierTxnTotals();

                vendorNTotals = calculateTotals4Vendor(vendorIdN, 
                                                       pTaxRate, 
                                                       pCounterDiscRate, 
                                                       bTaxInPrice, 
                                                       paItemStats);

                // UPDATE VENDOR STATS 
                //------------------------------------------------------------------
                long lVendorId = Long.parseLong(vendorIdN);

                int Colindex = 1;
                qryUpd.SetParameter(Colindex++, vendorNTotals.quantity            , "QUANTITY");
                qryUpd.SetParameter(Colindex++, vendorNTotals.grossTotal          , "GROSS");//not price tag
                qryUpd.SetParameter(Colindex++, vendorNTotals.discTotal           , "DISCOUNT");//counter disc only
                //qryUpd.SetParameter(Colindex++, itemStatsCore.surchargeTotal , "SURCHARGE");
                qryUpd.SetParameter(Colindex++, vendorNTotals.taxTotal            , "TAX");
                qryUpd.SetParameter(Colindex++, vendorNTotals.netTotal            , "NET");

                qryUpd.SetParameter(Colindex++, lAccId                            , "ACCOUNT_ID");        //QUANTITY++
                qryUpd.SetParameter(Colindex++, lVendorId                         , "VENDOR_ID");     //GROSS_TOTAL++

                qryUpd.addBatch();
            }

            int [] iAffectedRowCounts = qryUpd.executeBatch();
            
            return ;
        }
        catch(Exception e)
        {
            
        }
    }

    public static ssoStatsCashierTxnTotals calculateTotals4Vendor(String                                    pVendorId, 
                                                                  String                                    pTaxRate,
                                                                  String                                    pCounterDiscRate,
                                                                  boolean                                   pbTaxInPrice,
                                                                  ArrayList<ssoVendorItemStatsCollection>   paItemStats)
    {
        ssoStatsCashierTxnTotals vendorTotals = new ssoStatsCashierTxnTotals();
        
        BigDecimal bdQuantityTotal = new BigDecimal(BigInteger.ZERO);
        BigDecimal bdGrossTotal    = new BigDecimal(BigInteger.ZERO);
        BigDecimal bdDiscTotal     = new BigDecimal(BigInteger.ZERO);
        BigDecimal bdTaxTotal      = new BigDecimal(BigInteger.ZERO);
        BigDecimal bdNetTotal      = new BigDecimal(BigInteger.ZERO);
        
        for (ssoVendorItemStatsCollection statN: paItemStats)
        {
            if(statN.vendorId.trim().equals(pVendorId)==true)
            {
                ssoStatsConversion statNConversions = new ssoStatsConversion(statN.quantity.toString(), 
                                                                             statN.priceTag.toString(), 
                                                                             statN.salesPrice.toString(),
                                                                             pTaxRate,
                                                                             pCounterDiscRate,
                                                                             pbTaxInPrice);

                bdQuantityTotal = bdQuantityTotal.add(statNConversions.bdQuantity);
                bdNetTotal      = bdNetTotal.add(statNConversions.bdPriceNet);
                bdDiscTotal     = bdDiscTotal.add(statNConversions.bdTotalDisc);//disc is calculated only for counter disc
                bdTaxTotal      = bdTaxTotal.add(statNConversions.bdTax);
                bdGrossTotal    = bdGrossTotal.add(statNConversions.bdPriceGross);//pricetag = price gross 
            }
        }

        vendorTotals.quantity   = bdQuantityTotal;
        vendorTotals.netTotal   = bdNetTotal;
        vendorTotals.discTotal  = bdDiscTotal;
        vendorTotals.taxTotal   = bdTaxTotal;
        vendorTotals.grossTotal = bdGrossTotal;

        return vendorTotals;
    }
    
    public static void updateItemStats4CashRegisterTxn(EntityManager                           pem,
                                                       String                                  pUserId,
                                                       String                                  pAccId,
                                                       String                                  pTaxRate,
                                                       boolean                                 bTaxInPrice,
                                                       boolean                                 pbTxnRefund,
                                                       String                                  pCounterDiscRate,
                                                       ArrayList<ssoVendorItemStatsCollection> paItemStats)
    {
        //boolean bTxnRefund = false;

        try
        {
            long lAccId    = Long.parseLong(pAccId);

            // First Update 
            // If not exists then insert
            //------------------------------------------------------------------
            String sInitQuery4Upd = ssStatsQuery.generateUpdateItemStatsQuery4CashRegister(pbTxnRefund);

            Query qryUpd = pem.CreateNativeQuery(sInitQuery4Upd);
            //qryUpd.addBatch();
            int index = 0;
            for(ssoVendorItemStatsCollection statN: paItemStats)
            {
                ssoVendorItemStatsCore  itemStatsCore = new ssoVendorItemStatsCore();

                // CALCULATE NET VALUES FOR THE ITEM
                // COUNTER DISC WILL BE SHARED AMONG ALL THE ITEMS ON THE CART
                //--------------------------------------------------------------
                /*
                BigDecimal bdQuantity         = new BigDecimal(statN.quantity);
                BigDecimal bdPriceSold        = new BigDecimal(statN.salesPrice); //gross total = after item disc
                BigDecimal bdPriceB4Tax       = new BigDecimal(BigInteger.ZERO);
                BigDecimal bdPriceFinal       = new BigDecimal(BigInteger.ZERO);
                BigDecimal bdTaxRate          = new BigDecimal(pTaxRate);
                BigDecimal bdTax              = new BigDecimal(BigInteger.ZERO);

                BigDecimal bdCounterDiscRate  = new BigDecimal(pCounterDiscRate);
                BigDecimal bdCounterDisc      = new BigDecimal(BigInteger.ZERO);

                bdCounterDisc        = bdPriceSold.multiply(bdCounterDiscRate);
                bdPriceB4Tax         = bdPriceSold.subtract(bdCounterDisc);

                bdTax = Util.Tax.calculate(bdPriceB4Tax.toString(), bdTaxRate.toString(), bTaxInPrice);

                if(bTaxInPrice==true)
                {
                    bdPriceFinal = bdPriceB4Tax.subtract(bdTax);
                }
                else
                {
                    bdPriceFinal = bdPriceB4Tax;
                }
                */
                ssoStatsConversion statNConversions = new ssoStatsConversion(statN.quantity.toString(), 
                                                                             statN.priceTag.toString(),
                                                                             statN.salesPrice.toString(), 
                                                                             pTaxRate,
                                                                             pCounterDiscRate,
                                                                             bTaxInPrice);

                long lVendorId = Long.parseLong(statN.vendorId);

                int Colindex = 1;
                qryUpd.SetParameter(Colindex++, statNConversions.bdQuantity              , "QUANTITY");
                qryUpd.SetParameter(Colindex++, statNConversions.bdPriceGross            , "GROSS");//not price tag
                qryUpd.SetParameter(Colindex++, statNConversions.bdTotalDisc             , "DISCOUNT");//counter disc only
                //qryUpd.SetParameter(Colindex++, itemStatsCore.surchargeTotal , "SURCHARGE");
                qryUpd.SetParameter(Colindex++, statNConversions.bdTax                   , "TAX");
                qryUpd.SetParameter(Colindex++, statNConversions.bdPriceNet              , "NET");

                qryUpd.SetParameter(Colindex++, lAccId                                   , "ACCOUNT_ID");        //QUANTITY++
                qryUpd.SetParameter(Colindex++, lVendorId                                , "VENDOR_ID");     //GROSS_TOTAL++
                //qry.SetParameter(Colindex++, statN.categoryId              , "PRM_CATEGORY_ID");  //DISCOUNT_TOTAL++
                qryUpd.SetParameter(Colindex++, statN.itemCode                           , "ITEM_CODE"); //SURCHARGE_TOTAL++

                qryUpd.addBatch();

                index++;
            }

            int [] iAffectedRowCounts = qryUpd.executeBatch();

            //------------------------------------------------------------------
            // IF AFFECTED ROW IS ZERO CREATE A NEW RECORD FOR ITEM STAT
            //------------------------------------------------------------------
            String sTxnType = "";
            if(pbTxnRefund==true)
                sTxnType = txnDefs.TXN_TYPE_REFUND;
            else
                sTxnType = txnDefs.TXN_TYPE_SALES;

            String sInitQuery4New = ssStatsQuery.generateUpdateItemStatsQuery4New(sTxnType);

            Query qryNew = pem.CreateNativeQuery(sInitQuery4New);

            int iCounter = 0;
            for(int i=0;i<iAffectedRowCounts.length;i++)
            {
                if(iAffectedRowCounts[i]==0)
                {
                    iCounter++;

                    ssoVendorItemStatsCollection statN = new ssoVendorItemStatsCollection();
                    statN = paItemStats.get(i);
                    long lVendorId = Long.parseLong(statN.vendorId);

                    // SET INSERT PARAMS 
                    //-------------------------------------------------------------
                    ssoStatsConversion statNConversions = new ssoStatsConversion(statN.quantity.toString(),
                                                                                 statN.priceTag.toString(),
                                                                                 statN.salesPrice.toString(),
                                                                                 pTaxRate,
                                                                                 pCounterDiscRate,
                                                                                 bTaxInPrice);

                    // set params 
                    int Colindex = 1;
                    qryNew.SetParameter(Colindex++, -1                              , "PRICE_ID");// unknown came with sales
                    qryNew.SetParameter(Colindex++, -1                              , "LAST_SALE_PRICE");
                    qryNew.SetParameter(Colindex++, statNConversions.bdQuantity     , "QUANTITY");
                    qryNew.SetParameter(Colindex++, statNConversions.bdPriceGross   , "GROSS");
                    qryNew.SetParameter(Colindex++, statNConversions.bdCounterDisc  , "DISCOUNT");
                    qryNew.SetParameter(Colindex++, 0                               , "SURCHARGE");
                    qryNew.SetParameter(Colindex++, statNConversions.bdTax          , "TAX");
                    qryNew.SetParameter(Colindex++, statNConversions.bdPriceNet     , "NET");

                    qryNew.SetParameter(Colindex++, pAccId                          , "ACCOUNT_ID");        //QUANTITY++
                    qryNew.SetParameter(Colindex++, lVendorId                       , "VENDOR_ID");     //GROSS_TOTAL++
                    qryNew.SetParameter(Colindex++, -1                              , "PRM_CATEGORY_ID");  //DISCOUNT_TOTAL++
                    qryNew.SetParameter(Colindex++, statN.itemCode                  , "ITEM_CODE"); //SURCHARGE_TOTAL++

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

            return ;
        }
        catch(Exception e)
        {
            
        }
    }

    // options = 
    public static void updateOptionStats4CashRegisterTxn(EntityManager                                 pem,
                                                         String                                        pUserId,
                                                         String                                        pAccId,
                                                         String                                        pTaxRate,
                                                         boolean                                       bTaxInPrice,
                                                         String                                        pCounterDiscRate,
                                                         boolean                                       pbTxnRefund,
                                                         ArrayList<ssoSummaryItemCode>                 paItemCodesLookup,
                                                         ArrayList<ssoVendorItemOptionStatsCollection> paOptionStats)
    {
        try
        {
            long lAccId = Long.parseLong(pAccId);

            ArrayList<String> aItemCodes = new ArrayList<String>();
            ArrayList<String> aItemCodesDistinct = new ArrayList<String>();

            //Item Codes
            aItemCodes = collectItemCodesWVendorId(paOptionStats);
            aItemCodesDistinct = (ArrayList<String>)Util.Arrays.distinct(aItemCodes);

            // Recollection
            ArrayList<ssoGroupNOptNQuantity> aoGroups = new ArrayList<ssoGroupNOptNQuantity>();

            for(String itemCodeNVendorId: aItemCodesDistinct)
            {
                String[] aItemCodeNVendorId = itemCodeNVendorId.split("-");
                String sVendorId = aItemCodeNVendorId[0];
                String sItemCode = aItemCodeNVendorId[1];

                ArrayList<String> aOptionGroups = new ArrayList<String>();
                ArrayList<String> aOptionGroupsDistinct = new ArrayList<String>();

                //Option Groups
                aOptionGroups = collectOptionGroups4Item(sVendorId, sItemCode, paOptionStats);
                aOptionGroupsDistinct = (ArrayList<String>)Util.Arrays.distinct(aOptionGroups);

                for(String groupN: aOptionGroupsDistinct)
                {
                    ArrayList<ssoOptNQuantity> aOptions = new ArrayList<ssoOptNQuantity>();
                    aOptions = collectOptions4Group(sVendorId, sItemCode, groupN, paOptionStats);

                    ssoGroupNOptNQuantity oNewGroup = new ssoGroupNOptNQuantity();
                    oNewGroup.group = groupN;
                    oNewGroup.itemCode = sItemCode;
                    oNewGroup.vendorId = sVendorId;
                    oNewGroup.options.addAll(aOptions);
                    aoGroups.add(oNewGroup);
                }
            }

            // Updates 
            String sQueryUpd = ssStatsQuery.generateUpdateOptionStatsQuery4CashRegister(pbTxnRefund);
            Query qryUpd = pem.CreateNativeQuery(sQueryUpd);

            for(ssoGroupNOptNQuantity oGroupN: aoGroups)
            {
                long lVendorId   = Long.parseLong(oGroupN.vendorId);
                long lItemCodeId = getItemCodeId(oGroupN.vendorId, oGroupN.itemCode, paItemCodesLookup);

                String sJSOptionQuantities = generateOptionGroupJSON(oGroupN.options);

                int Colindex = 1;
                qryUpd.SetParameter(Colindex++, sJSOptionQuantities             , "OPTIONS");// unknown came with sales
                qryUpd.SetParameter(Colindex++, lAccId                          , "ACCOUNT_ID");
                qryUpd.SetParameter(Colindex++, lVendorId                       , "VENDOR_ID");
                qryUpd.SetParameter(Colindex++, lItemCodeId                     , "ITEM_CODE_ID");
                qryUpd.SetParameter(Colindex++, oGroupN.group                   , "OPTION_GROUP");

                qryUpd.addBatch();
            }

            int [] iAffectedRowCounts = qryUpd.executeBatch();
            
            return ;
        }
        catch(Exception e)
        {
            
        }
    }

    public static long getItemCodeId(String                           pVendorId,
                                     String                           pItemCode,
                                     ArrayList<ssoSummaryItemCode>    paItemCodesLookup)
    {
        ssoSummaryItemCode optItemCodeSmry = new ssoSummaryItemCode();
        optItemCodeSmry = getItemCodeSummaryIdFromSummaryTable(pVendorId, pItemCode, paItemCodesLookup);
        
        return optItemCodeSmry.itemCodeId;
    }

    public static ssoSummaryItemCode getItemCodeSummaryIdFromSummaryTable(String                          pVendorId,
                                                                          String                          pItemCode, 
                                                                          ArrayList<ssoSummaryItemCode>   paItemCodeSummary)
    {
        for(ssoSummaryItemCode itmN: paItemCodeSummary)
        {
            if (itmN.itemCode.trim().toLowerCase().equals(pItemCode.trim().toLowerCase())==true)
                return itmN;
        }

        return null;
    }

    // {"L":[1,2..], "M": 1 ...} // L = > Option Name 1,2.. => Quantities
    public static String generateOptionGroupJSON(ArrayList<ssoOptNQuantity> pGroupOptions)
    {
        int index = 0;
        
        String jsText = "{";
        
        for(ssoOptNQuantity optN: pGroupOptions)
        {
            if(index!=0)
                jsText += ",";

            String jsOptText = "";

            ArrayList<String> aQuantities = new ArrayList<String>();

            // Collect Quantities
            for(ssoOptNQuantity quantityN: pGroupOptions)
            {
                if(quantityN.option.equals(optN.option)==true)
                {
                    aQuantities.addAll(quantityN.quantities);
                }
            }
            
            int j = 0;
            jsOptText += Util.Str.QUOTE(optN.option) + ":" + "[";
            for(String sQuantity: aQuantities)
            {
                if(j!=0)
                    jsOptText += ",";

                jsOptText += sQuantity;
                j++;
            }
            jsOptText += "]";

            jsText += jsOptText;
            index++;
        }

        jsText += "}";

        return jsText;
    }

    public static ArrayList<ssoOptNQuantity> collectOptions4Group(String                                           pVendorId,
                                                                  String                                           pItemCode,
                                                                  String                                           pOptionGroup,
                                                                  ArrayList<ssoVendorItemOptionStatsCollection>    paOptionStats)
    {
        ArrayList<String> oOptionNames = new ArrayList<String>();
        ArrayList<String> oOptionNamesDistinct = new ArrayList<String>();
        ArrayList<ssoOptNQuantity> aAllOptsQuantities = new ArrayList<ssoOptNQuantity>();
        
        // COLLECT OPTION NAMES
        oOptionNames = collectOptionNames(pVendorId, pItemCode, pOptionGroup, paOptionStats);
        oOptionNamesDistinct = (ArrayList<String>)Util.Arrays.distinct(oOptionNames);
        
        // COLLECT QUANTITIES FOR EACH OPTION
        for(String optNameN: oOptionNamesDistinct)
        {
            ArrayList<String> aQuantities = new ArrayList<String>();
            aQuantities = collectQuantities4Option( pVendorId,
                                                    pItemCode, 
                                                    pOptionGroup, 
                                                    optNameN, 
                                                    paOptionStats);
            
            ssoOptNQuantity optNQ = new ssoOptNQuantity();
            optNQ.option = optNameN;
            optNQ.quantities.addAll(aQuantities);
            aAllOptsQuantities.add(optNQ);
        }
        
        return aAllOptsQuantities;
    }

    public static ArrayList<String> collectOptionNames(String                                           pVendorId,
                                                       String                                           pItemCode,
                                                       String                                           pOptionGroup,
                                                       ArrayList<ssoVendorItemOptionStatsCollection>    paOptionStats)
    {
        ArrayList<String> oOptions = new ArrayList<String>();
        
        for(ssoVendorItemOptionStatsCollection statN: paOptionStats)
        {
            if(statN.vendorId.trim().toLowerCase().equals(pVendorId.trim().toLowerCase())==true)
            {
                if(statN.itemCode.trim().toLowerCase().equals(pItemCode.trim().toLowerCase())==true)
                {
                    for(ssoItemOption optN: statN.options)
                    {
                        if(optN.groupName.trim().toLowerCase().equals(pOptionGroup.trim().toLowerCase())==true)
                        {
                            oOptions.add(optN.optionName);
                        }
                    }
                }
            }
        }

        return oOptions;
    }

    public static ArrayList<String> collectQuantities4Option(String                                           pVendorId,
                                                             String                                           pItemCode,
                                                             String                                           pOptionGroup,
                                                             String                                           pOptionName,
                                                             ArrayList<ssoVendorItemOptionStatsCollection>    paOptionStats)
    {
        ArrayList<String> aQuantities = new ArrayList<String>();
        
        for(ssoVendorItemOptionStatsCollection statN: paOptionStats)
        {
            if(statN.vendorId.trim().toLowerCase().equals(pVendorId.trim().toLowerCase())==true)
            {
                if(statN.itemCode.trim().toLowerCase().equals(pItemCode.trim().toLowerCase())==true)
                {
                    for(ssoItemOption optN: statN.options)
                    {
                        if(optN.groupName.trim().toLowerCase().equals(pOptionGroup.trim().toLowerCase())==true)
                        {
                            if(optN.optionName.trim().toLowerCase().equals(pOptionName.trim().toLowerCase())==true)
                            {
                                aQuantities.add(optN.quantity);
                            }
                        }
                    }
                }
            }
        }
        
        return aQuantities;
    }

    public static ArrayList<String> collectItemCodesWVendorId(ArrayList<ssoVendorItemOptionStatsCollection>    paItemStats)
    {
        ArrayList<String> aItemCodes = new ArrayList<String>();

        for(ssoVendorItemOptionStatsCollection optionN:paItemStats)
        {
            String sItemCodeNVendorId = optionN.vendorId + "-" + optionN.itemCode;

            aItemCodes.add(sItemCodeNVendorId);
        }

        return aItemCodes;
    }


    public static ArrayList<String> collectOptionGroups4Item(String                                           pVendorId,
                                                             String                                           pItemCode,
                                                             ArrayList<ssoVendorItemOptionStatsCollection>    paOptionStats)
    {
        ArrayList<String> aOptionGroups = new ArrayList<String>();

        for(ssoVendorItemOptionStatsCollection itemN:paOptionStats)
        {
            if (itemN.vendorId.trim().equals(pVendorId)==true)
            {
                if (itemN.itemCode.trim().equals(pItemCode)==true)
                {
                    for(ssoItemOption optionN: itemN.options)
                    {
                        aOptionGroups.add(optionN.groupName);
                    }
                }
            }
        }

        return aOptionGroups;
    }
/*
    public static ArrayList<ssoOptNQuantity> collectOptions(String                                           pItemCode,
                                                            String                                           pOptGroup,
                                                            ArrayList<ssoVendorItemOptionStatsCollection>    paOptionStats)
    {
        ArrayList<ssoOptNQuantity> aOptions = new ArrayList<ssoOptNQuantity>();

        for(ssoVendorItemOptionStatsCollection itemN:paOptionStats)
        {
            if (itemN.itemCode.trim().toLowerCase().equals(pItemCode)==true)
            {
                for(ssoItemOption optionN: itemN.options)
                {
                    if (optionN.groupName.trim().toLowerCase().equals(pOptGroup)==true)
                    {
                        ssoOptNQuantity newOptNQ = new ssoOptNQuantity();
                        newOptNQ.option   = optionN.optionName;
                        newOptNQ.quantity = optionN.quantity;

                        aOptions.add(newOptNQ);
                    }
                }
            }
        }

        // Reorganize = { "L":1, "L":2,... } => { "L":[1,2...] }
        ArrayList<ssoOptNQuantity> aOptionsDistinct = new ArrayList<ssoOptNQuantity>();
        
        return aOptionsDistinct;
    }
*/
}
