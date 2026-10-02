/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.reports;

import bb.app.account.AccountMisc;
import bb.app.account.ssoAccInvBalanceCore;
import bb.app.account.ssoUIBalanceItem;
import bb.app.inv.InventoryMisc;
import static bb.app.inv.InventoryOps.generateRowKey;
import bb.app.obj.ssoBrand;
import Objects.ssoMerchant;
import bb.app.account.UserOps;
import bb.app.dict.DictionaryOps;
import java.math.BigInteger;
import java.util.ArrayList;
import jaxesa.persistence.EntityManager;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class ssReportSearchBalances 
{
    /*
    NOTE: THIS METHOD RETURNS THE YTD BALANCE OF THE BRAND AND THE YTD QUANTITIS 
    
        1. FIND THE BRANDS/VENDORS MATCHED WITH KEYWORD
        2. FIND THE LIST OF ACCOUNTS / BRANCHES FOR THE USER
        3. PER ACCOUNT / BRANCH

        LEVEL 1: ROOT LEVEL(Brand Level) (SHOWS ONLY BRAND BALANCE) (This level will be calculated by level 2 data)
        LEVEL 2: PER ACCOUNT/BRANCH LEVEL 
        LEVEL 3: BREAK DOWN OF ACCCOUNT /BRANCH (option level)

        Sample Data output
        Item           - Net - Received - Returned - Sold - Activity
        MODIVA / 002   - 10,000  - 5,000  - 2,000  ... (Brand/Vendor Level) (LEVEL1)
            BULBULLER  - 5   - 15 ... (Branch Level) (LEVEL2)
            DILEK      - 5   - 5  ...                (LEVEL2)
    */
    public static ArrayList<ssoUIBalanceItem> generate( EntityManager pem, 
                                                        BigInteger    pUserId,
                                                        String        pKeyword,
                                                        boolean       pbCleanMemoryData) throws Exception
    {
        try
        {
            if(pbCleanMemoryData==true)
                pem.flush();

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user (on cache)
            ArrayList<ssoMerchant> aBranches = new ArrayList<ssoMerchant>();
            aBranches = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);

            // Step 1. (on cache)
            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            //aBrands = DictionaryOps.Vendor.findMatchedBrands4Account(pem, pUserId, pKeyword);
            aBrands = DictionaryOps.Vendor.findMatchedBrands4User(pem, pUserId, pKeyword);

            // 1. GET RAW DATA 1ST 
            // 2. Calculate Branch lines for Branch (LEVEL 2)
            // 3. Thru Branch line calculate Brands (LEVEL 1)
            ArrayList<ssoUIBalanceItem>  balanceSheetRawData = new ArrayList<ssoUIBalanceItem>();
            balanceSheetRawData = generateLEVELS_RawData(pem, pUserId, ThisYear, aBrands, aBranches, pbCleanMemoryData);
            for (ssoBrand brandN: aBrands)
            {
                //if (brandN.name.equals("ESHABIL")==true)
                //{

                    ArrayList<ssoUIBalanceItem>  balanceSheetSumLevel1 = new ArrayList<ssoUIBalanceItem>();
                    ArrayList<ssoUIBalanceItem>  balanceSheetSumLevel2 = new ArrayList<ssoUIBalanceItem>();

                    // CALCULATE LEVEL 1 - BRAND LEVEL TOTALS
                    //------------------------------------------------------

                        //1. Pre-Preration Of Data
                        ArrayList<ssoUIBalanceItem>  balanceSheets1 = new ArrayList<ssoUIBalanceItem>();
                        for (ssoUIBalanceItem balanceN: balanceSheetRawData)
                        {
                            String sBalanceVendorId = balanceN.parentKey;
                            BigInteger   sBalanceAccId    = balanceN.aid;

                            if (sBalanceVendorId.equals(brandN.Id.toString())==true)
                            {
                                balanceSheets1.add(balanceN);
                            }
                        }

                        //2. Sum Data -> Adding to the SHeet (FOR ALL BRANCHES)
                        //------------------------------------------------------
                        balanceSheetSumLevel1 = generateSheetTotal(pem, 
                                                                    pUserId, 
                                                                    BigInteger.valueOf(-1),//no account Id
                                                                    ThisYear, 
                                                                    brandN.name, 
                                                                    brandN.Id, 
                                                                    "",
                                                                    1,
                                                                    balanceSheets1);

                        balanceSheet.addAll(balanceSheetSumLevel1);

                    // CALCULATE LEVEL 2 - BRANCH LEVEL TOTALS
                    //------------------------------------------------------

                        //1. Pre-preperation of Data
                        //------------------------------------------------------
                        for (ssoMerchant accN:aBranches)
                        {

                            ArrayList<ssoUIBalanceItem>  balanceSheets2 = new ArrayList<ssoUIBalanceItem>();
                            for (ssoUIBalanceItem balanceN: balanceSheetRawData)
                            {
                                String sBalanceVendorId = balanceN.parentKey;
                                BigInteger   sBalanceAccId    = balanceN.aid;

                                if ( (sBalanceVendorId.equals(brandN.Id.toString())==true) && 
                                     (accN.Id.toString().equals(sBalanceAccId) == true)
                                   )
                                {
                                    balanceSheets2.add(balanceN);
                                }
                            }

                            //2. Add Data
                            balanceSheetSumLevel2 = generateSheetTotal(pem, 
                                                                        pUserId, 
                                                                        accN.Id,
                                                                        ThisYear, 
                                                                        accN.name, 
                                                                        brandN.Id, 
                                                                        brandN.Id.toString(),//parent key
                                                                        2,
                                                                        balanceSheets2);

                            balanceSheet.addAll(balanceSheetSumLevel2);

                        }// end of branches

                //}
            }//end of brands


            /*
            for (ssoBrand brandN: aBrands)
            {
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
                ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();

                // 1. First Calculate Branches
                // 2. Thru Branches calculate Brands
                balanceSheetLevel2 = generateLEVEL2_Branches(pem, pUserId, ThisYear, brandN, branches);

                // Calculate it thru the output of SheetLevel2 data
                balanceSheetLevel1 = generateLEVEL1_Brands(pem, pUserId, ThisYear, brandN, brandN.Id, balanceSheetLevel2);

                balanceSheet.addAll(balanceSheetLevel1);
                balanceSheet.addAll(balanceSheetLevel2);

            }
            */

            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    //calculate tots for  brand thru the sub totals
    //generateLEVEL1_Brands
    public static ArrayList<ssoUIBalanceItem> generateSheetTotal(     EntityManager                 pem,
                                                                       BigInteger                         pUserId,
                                                                       BigInteger                         pAccId,
                                                                       int                          pFiscalYear,
                                                                       String                       pName,
                                                                       BigInteger                         pBrandId,
                                                                       String                       pParentKey,
                                                                       int                          pLevel,
                                                                       ArrayList<ssoUIBalanceItem>  pBalanceSheets) throws Exception
    {
        int i = 0;

        try
        {
            ArrayList<ssoUIBalanceItem>  balanceSheetSum = new ArrayList<ssoUIBalanceItem>();

            // LEVEL 1 = TOTALS for Brand
            // TOTALS = SUM(BRANCH1..N)
            //----------------------------------------------------------
            ssoUIBalanceItem newItem = new ssoUIBalanceItem();
 
            newItem.level = pLevel;//1;
            newItem.name = pName;//brandN.name;
            if(pLevel==1)
                newItem.key  = pBrandId.toString(); //pBrandName;//brandN.name;
            else
                newItem.key  = pAccId + "-" + pBrandId.toString(); 
            newItem.parentKey = pParentKey;//"";
 
            for (ssoUIBalanceItem balanceN:pBalanceSheets)
            {
                //if (balanceN.parentKey.equals(Long.toString(pBrandId))==true)
                //{
                //if((pAccId==-1) || (pAccId==balanceN.aid))
                if(
                    (pAccId.compareTo(BigInteger.valueOf(-1))==0) || 
                    (pAccId.compareTo(balanceN.aid)==0)
                  )
                {
                    //if(pBrandId==46604235)
                    //    i = 0;

                    newItem.quantity.received = newItem.quantity.received.add(balanceN.quantity.received);
                    newItem.quantity.returned = newItem.quantity.returned.add(balanceN.quantity.returned);
                    newItem.quantity.sold     = newItem.quantity.sold.add(balanceN.quantity.sold);
                    newItem.quantity.refund   = newItem.quantity.sold.add(balanceN.quantity.refund);

                    newItem.quantity.revolving= newItem.quantity.revolving.add(balanceN.quantity.revolving);
                    //newItem.quantity = newItem.quantity.revolving.add(balanceN.quantity.revolving);

                    //newItem.quantity.received += balanceN.quantity.received;
                    //newItem.quantity.returned += balanceN.quantity.returned;
                    //newItem.quantity.sold     += balanceN.quantity.sold;

                    newItem.balance.received = newItem.balance.received.add(balanceN.balance.received);
                    newItem.balance.returned = newItem.balance.returned.add(balanceN.balance.returned);
                    newItem.balance.sold     = newItem.balance.sold.add(balanceN.balance.sold);
                    newItem.balance.payments = newItem.balance.payments.add(balanceN.balance.payments);
                    newItem.balance.revolving= newItem.balance.revolving.add(balanceN.balance.revolving);

                    newItem.lastActivity  = balanceN.lastActivity;
                }

            }

            newItem.quantity.net  = newItem.quantity.received.subtract(newItem.quantity.returned);
            newItem.balance.net       = newItem.balance.received.subtract(newItem.balance.returned).subtract(newItem.balance.payments);

            balanceSheetSum.add(newItem);//add balance of each item

            return balanceSheetSum;
        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static ArrayList<ssoUIBalanceItem> generateLEVELS_RawData(  EntityManager           pem,
                                                                        BigInteger                    pUserId,
                                                                        int                     pFiscalYear,
                                                                        ArrayList<ssoBrand>     pBrands,
                                                                        ArrayList<ssoMerchant>  paBranches,
                                                                        boolean                 pbCleanMemoryData) throws Exception
    {
        int i = 0;

        try
        {
            if(pbCleanMemoryData==true)
                pem.flush();

            ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();

            //Step 2.
            for(ssoMerchant accN: paBranches)
            {
                ArrayList<ssoAccInvBalanceCore> brandBalances = new ArrayList<ssoAccInvBalanceCore>(); 
                // On Cache
                brandBalances = AccountMisc.calculateBrandBalances( pem,
                                                                    accN.Id, 
                                                                    accN.name,
                                                                    pBrands,//brandN.name, 
                                                                    pFiscalYear);

                // LEVEL 2 = Totals for Branch
                //----------------------------------------------------------
                for(ssoAccInvBalanceCore balanceN:brandBalances)
                {
                    ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                    newItem.level = 2;
                    newItem.aid  = balanceN.AccountId;
                    newItem.name = balanceN.AccountName;//balanceN.Brandname;
                    newItem.key  = generateRowKey(balanceN.AccountName, balanceN.Brandname, "", "");
                    //newItem.parentKey = Long.toString(pBrand.Id);//brandN.name;
                    //if(balanceN.BrandId==46604235)
                    //    i = i;

                    newItem.parentKey = balanceN.BrandId.toString();//brandN.name;
                    newItem.quantity.received = balanceN.stats.ytd.quantity.received;
                    newItem.quantity.returned = balanceN.stats.ytd.quantity.sent;
                    newItem.quantity.sold     = balanceN.stats.ytd.quantity.sold;
                    newItem.quantity.refund   = balanceN.stats.ytd.quantity.refund;
                    newItem.quantity.net      = balanceN.stats.ytd.quantity.balance;

                    newItem.balance.received   = balanceN.stats.ytd.net.received;
                    newItem.balance.returned   = balanceN.stats.ytd.net.sent;
                    newItem.balance.sold       = balanceN.stats.ytd.net.sold;
                    newItem.balance.refund     = balanceN.stats.ytd.net.refund;
                    newItem.balance.net        = balanceN.stats.ytd.net.balance;

                    newItem.lastActivity  = balanceN.lastActivity;

                    balanceSheetLevel2.add(newItem);//add balance of each item
                }

            }// end of branches

            return balanceSheetLevel2;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

}
