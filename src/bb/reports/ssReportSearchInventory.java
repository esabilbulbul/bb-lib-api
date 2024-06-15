package bb.reports;

import bb.app.account.AccountMisc;
import bb.app.account.ssoAccInvBalanceCore;
import bb.app.account.ssoUIBalanceItem;
import bb.app.inv.InventoryMisc;
import static bb.app.inv.InventoryOps.generateRowKey;
import static bb.app.inv.InventoryOps.generateRowKeyWSign;
import bb.app.obj.ssoBrand;
import bb.app.obj.ssoMerchant;
import bb.app.account.UserOps;
import bb.app.dict.DictionaryOps;
import bb.app.inv.InventoryOps;
import java.math.BigDecimal;
import java.util.ArrayList;
import jaxesa.persistence.EntityManager;
import jaxesa.util.Util;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 *  @author Administrator
 * 
 *  REPORT FORMAT
 * 
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
public final class ssReportSearchInventory
{
    public static String ROW_KEY_SEPERATOR_SIGN = ">";// DON'T USE - IT CAUSES CONFLICT WITH OPTIONGROUP + OPTION AS THEY ARE ALSO SEPERATED WITH -
    
    public static ArrayList<ssoUIBalanceItem> generate4SummaryByKeyword(EntityManager pem, 
                                                                        long          pUserId,
                                                                        String        pKeyword) throws Exception
    {
        /*
            1. FIND THE BRANDS MATCHED WITH KEYWORD
            2. FIND THE LIST OF ACCOUNTS / BRANCHES FOR THE USER
            3. PER ACCOUNT / BRANCH
            3.1.      CALCULATE BRAND-ITEM SUM

            Sample Data output 
            Item           - Net - Received - Returned - Sold - Activity
            MODIVA / 002   - 10  - 20       - 2 ... (Summary level)
                    BULBULLER  - 5   - 15 ...       (Breaking into Branch Level = RAW DATA)
                    DILEK      - 5   - 5  ...
        */
        try
        {
            //int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            // on Cache
            branches = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);

            // Step 1. (on cache)
            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            //aBrands = DictionaryOps.Vendor.findMatchedBrands4Account(pem, pUserId, pKeyword);
            aBrands = DictionaryOps.Vendor.findMatchedBrands4User(pem, pUserId, pKeyword);

            // Also find matched ITEMCODES here as returns of BRANDS

            for (ssoBrand brandN: aBrands)
            {
                ArrayList<ssoUIBalanceItem>  balanceSheet4Brand = new ArrayList<ssoUIBalanceItem>();
                
                balanceSheet4Brand = generateLines4Brand(pem, pUserId, branches, brandN.Id, brandN.name);
                
                balanceSheet.addAll(balanceSheet4Brand);
                //balanceSheet.addAll(balanceSheetLevel2);
                //balanceSheet.addAll(balanceSheetLevel3);
            }

            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static ArrayList<ssoUIBalanceItem> generate4SummaryByBrand(EntityManager pem, 
                                                                      long          pUserId,
                                                                      long          pBrandId) throws Exception
    {
        try
        {
            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            // on Cache
            branches = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);

            balanceSheet = generateLines4Brand(pem, pUserId, branches, pBrandId, "");

            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoUIBalanceItem> generateLines4Brand(  EntityManager        pem,
                                                                    long                   pUserId,
                                                                    ArrayList<ssoMerchant> pBranches,
                                                                    long                   pBrandId,
                                                                    String                 pBrandName) throws Exception
    {
        ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

        try
        {
            // LEVEL 1 (Item Code Level)
            // PARENT_KEY = <BrandName>
            // ON CACHE
            //----------------------------------------------------------
            ArrayList<ssoUIBalanceItem>  balanceSheetLevel1RawData = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel1RawData = ssReportSearchInventory.generateLEVEL1_RawData( pem,
                                                                                        pUserId,
                                                                                        pBranches,
                                                                                        pBrandId,
                                                                                        pBrandName);

            // LEVEL 1 (Item Code Level)
            // PARENT_KEY = <empty> + <brandId> + <itemcode> + "a/u" (u = user for summary level a = a for account)
            // ON CACHE
            //----------------------------------------------------------
            ArrayList<ssoUIBalanceItem>  invSheetLevel1Summary = new ArrayList<ssoUIBalanceItem>();
            invSheetLevel1Summary = ssReportSearchInventory.generateLEVEL1_Summary(pUserId, balanceSheetLevel1RawData );

            ArrayList<ssoUIBalanceItem>  invSheetLevel1BranchDets = new ArrayList<ssoUIBalanceItem>();
            invSheetLevel1BranchDets = ssReportSearchInventory.generateLEVEL1_BranchDets(balanceSheetLevel1RawData);

            // LEVEL 2 (Option Level)
            // PARENT_KEY = bRAND + Itemcode
            // ON CACHE
            //------------------------------------------------------
            /*
            ArrayList<ssoUIBalanceItem>  balanceSheetLevel2RawData = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel2RawData = ssReportSearchInventory.generateLEVEL2_RawData( pem,
                                                                                        branches,
                                                                                        brandN.Id,
                                                                                        brandN.name);


            ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel2 = ssReportSearchInventory.generateLEVEL2_Options( balanceSheetLevel2RawData );
            */

            // LEVEL 3 = BRANCH LEVEL
            // ON CACHE
            //----------------------------------------------------------
            // CALCULATE BOTTOM LINE FOR branches thru LEVEL 2 DATA (options)
            /*
            ArrayList<ssoUIBalanceItem>  balanceSheetLevel3 = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel3 = ssReportSearchInventory.generateLEVEL3_Branches(pem,
                                                                                 branches,
                                                                                 balanceSheetLevel2);
            */

            balanceSheet.addAll(invSheetLevel1Summary);
            balanceSheet.addAll(invSheetLevel1BranchDets);

            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoUIBalanceItem> generate4Items( EntityManager pem, 
                                                              long          pUserId,
                                                              long          pTargetId,
                                                              String        pTargetType,//User or Account (branch)
                                                              long          pBrandId,//vendor
                                                              String        pItemCode
                                                              ) throws Exception
    {
        try
        {
            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            // on Cache

            if (pTargetType.equals("U")==true)//user
            {
                // SEARCH with USER ID 
                branches = DictionaryOps.User.getListOfAccounts4User(pem, pTargetId, false);//accounts
            }
            else
            {
                // SEARCH WITH ACCOUNT ID
                ssoMerchant newAccount = new ssoMerchant();
                newAccount.Id = pTargetId;//acc Id
                branches.add(newAccount);
            }

            ArrayList<ssoUIBalanceItem>  balanceSheetLevel2RawData = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel2RawData = ssReportSearchInventory.generateLEVEL2_RawData( pem,
                                                                                        branches,
                                                                                        pBrandId,
                                                                                        "",
                                                                                        pItemCode);

            ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel1 = ssReportSearchInventory.generateLEVEL2_Options( balanceSheetLevel2RawData );

            // LEVEL 3 = BRANCH LEVEL
            // ON CACHE
            //----------------------------------------------------------
            // CALCULATE BOTTOM LINE FOR branches thru LEVEL 2 DATA (options)

            ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel2 = ssReportSearchInventory.generateLEVEL3_Branches(pem,
                                                                                 branches,
                                                                                 balanceSheetLevel1);

            balanceSheet.addAll(balanceSheetLevel1);
            balanceSheet.addAll(balanceSheetLevel2);


            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVEL2_Options(ArrayList<ssoUIBalanceItem> pBalanceSheetLevel2RawData)
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
        ArrayList<String> aUniqueBrandNOptions = new ArrayList<String>();

        //1. Get unique Brand + Item Code list
        for (ssoUIBalanceItem optionN:pBalanceSheetLevel2RawData)
        {
            boolean bFound = false;
            for (String sBrandNOption:aUniqueBrandNOptions)
            {
                //if (sBrandNOption.equals(optionN.key)==true)
                if (sBrandNOption.equals(optionN.dets.option)==true)
                {
                    bFound = true;
                }
            }

            if (bFound==false)
            {
                //aUniqueBrandNOptions.add(optionN.key);
                aUniqueBrandNOptions.add(optionN.dets.option);
            }
        }

        // Calculate Totals 
        for(String sBrandNOptionN:aUniqueBrandNOptions)
        {
            ssoUIBalanceItem balanceItemTotals = new ssoUIBalanceItem();

            for (ssoUIBalanceItem balanceItemN:pBalanceSheetLevel2RawData)
            {
                //if (sBrandNOptionN.equals(balanceItemN.key)==true)//sum for the same options
                if(sBrandNOptionN.equals(balanceItemN.dets.option)==true)
                {
                    balanceItemTotals.account = balanceItemN.account;
                    balanceItemTotals.aid     = balanceItemN.aid;
                    balanceItemTotals.key     = balanceItemN.key;
                    balanceItemTotals.level   = balanceItemN.level;
                    balanceItemTotals.name    = balanceItemN.name;
                    balanceItemTotals.lastActivity = balanceItemN.lastActivity;
                    balanceItemTotals.parentKey       = balanceItemN.parentKey;
                    balanceItemTotals.velocityOverall = balanceItemN.velocityOverall;
                    balanceItemTotals.velocityStartup = balanceItemN.velocityStartup;

                    balanceItemTotals.dets = balanceItemN.dets;

                    if (balanceItemN.quantity.received.longValue()>0)
                    {
                        balanceItemTotals.quantity.received = balanceItemTotals.quantity.received.add(balanceItemN.quantity.received);
                        balanceItemTotals.quantity.net      = balanceItemTotals.quantity.net.add(balanceItemTotals.quantity.received);
                    }
                    
                    if (balanceItemN.quantity.returned.longValue()>0)
                    {
                        balanceItemTotals.quantity.returned = balanceItemTotals.quantity.returned.add(balanceItemN.quantity.returned);
                        balanceItemTotals.quantity.net      = balanceItemTotals.quantity.net.subtract(balanceItemTotals.quantity.returned);
                    }
                    
                    if (balanceItemN.quantity.sold.longValue()>0)
                    {
                        balanceItemTotals.quantity.sold     = balanceItemTotals.quantity.sold.add(balanceItemN.quantity.sold);
                        balanceItemTotals.quantity.net     = balanceItemTotals.quantity.net.subtract(balanceItemTotals.quantity.sold);
                    }

                    if (balanceItemN.quantity.adjPlus.longValue()>0)
                    {
                        balanceItemTotals.quantity.adjPlus     = balanceItemTotals.quantity.adjPlus.add(balanceItemN.quantity.adjPlus);
                        balanceItemTotals.quantity.net         = balanceItemTotals.quantity.net.add(balanceItemTotals.quantity.adjPlus);
                    }

                    if (balanceItemN.quantity.adjMinus.longValue()>0)
                    {
                        balanceItemTotals.quantity.adjMinus     = balanceItemTotals.quantity.adjMinus.add(balanceItemN.quantity.adjMinus);
                        balanceItemTotals.quantity.net          = balanceItemTotals.quantity.net.subtract(balanceItemTotals.quantity.adjMinus);
                    }
                    
                    if (balanceItemN.quantity.revolving.longValue()>0)
                    {
                        balanceItemTotals.quantity.revolving= balanceItemTotals.quantity.revolving.add(balanceItemN.quantity.revolving);
                        balanceItemTotals.quantity.net      = balanceItemTotals.quantity.net.add(balanceItemTotals.quantity.revolving);
                    }

                    //balanceItemTotals.quantity.net      = balanceItemTotals.quantity.received.subtract(balanceItemTotals.quantity.returned).subtract(balanceItemTotals.quantity.sold).add(balanceItemTotals.quantity.revolving);

                }
            }//end of first row (option)
            
            balanceSheetLevel2.add(balanceItemTotals);
        }

        return balanceSheetLevel2;
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVEL1_BranchDets(ArrayList<ssoUIBalanceItem> pInvSheetLevel1RawData) throws Exception
    {
        try
        {
            ArrayList<ssoUIBalanceItem> sheetLevel1Dets = new ArrayList<ssoUIBalanceItem>();

            for(ssoUIBalanceItem ItemN: pInvSheetLevel1RawData)
            {
                ItemN.name = ItemN.account;
                
                sheetLevel1Dets.add(ItemN);
            }
            
            return sheetLevel1Dets;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // This generates Vendor + Itemcode by calculating all connected accounts
    public static ArrayList<ssoUIBalanceItem> generateLEVEL1_Summary(long pUserId, ArrayList<ssoUIBalanceItem> pInvSheetLevel1RawData)
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
        ArrayList<String> aUniqueBrandNItemCodes = new ArrayList<String>();

        //1. COLLECT unique Brand + Item Code list
        for (ssoUIBalanceItem itemN:pInvSheetLevel1RawData)
        {
            String sKey2Search = "";
            String[] aKeyParts = InventoryOps.getRowKeyParts(itemN.key);
            String sBrandId    = aKeyParts[1];
            String sItemCode   = aKeyParts[2];
            sKey2Search = sBrandId + "-" + sItemCode;

            boolean bFound = false;
            for (String sBrandNItemCode:aUniqueBrandNItemCodes)
            {

                //if (sBrandNItemCode.equals(itemN.key)==true)
                if (sBrandNItemCode.equals(sKey2Search)==true)
                {
                    bFound = true;
                }
            }

            if (bFound==false)
            {
                //aUniqueBrandNItemCodes.add(itemN.key);
                aUniqueBrandNItemCodes.add(sKey2Search);
            }
        }

        // Roll thru the unique Brand + ItemCode and 
        // CALCULATE TOTALS
        for(String sBrandNItemCodeN:aUniqueBrandNItemCodes)
        {
            ssoUIBalanceItem balanceItemTotals = new ssoUIBalanceItem();

            for (ssoUIBalanceItem balanceItemN:pInvSheetLevel1RawData)
            {
                String[] aKeyParts = InventoryOps.getRowKeyParts(balanceItemN.key);
                String sBrandId    = aKeyParts[1];
                String sItemCode   = aKeyParts[2];
                String sKey2Search = sBrandId + "-" + sItemCode;

                //if (sBrandNItemCodeN.equals(balanceItemN.key)==true)
                if (sBrandNItemCodeN.equals(sKey2Search)==true)
                {
                    balanceItemTotals.account = balanceItemN.account;
                    balanceItemTotals.aid     = balanceItemN.aid;
                    //balanceItemTotals.key     = balanceItemN.key;
                    balanceItemTotals.lastActivity = balanceItemN.lastActivity;
                    
                    balanceItemTotals.level   = balanceItemN.level;
                    balanceItemTotals.name    = balanceItemN.name;
                    balanceItemTotals.parentKey       = "";//balanceItemN.parentKey;//parent key must be empty for summary (root) level
                    balanceItemTotals.successRate     = balanceItemN.successRate;
                    balanceItemTotals.velocityOverall = balanceItemN.velocityOverall;
                    balanceItemTotals.velocityStartup = balanceItemN.velocityStartup;

                    balanceItemTotals.dets = balanceItemN.dets;

                    balanceItemTotals.quantity.received = balanceItemTotals.quantity.received.add(balanceItemN.quantity.received);
                    balanceItemTotals.quantity.returned = balanceItemTotals.quantity.returned.add(balanceItemN.quantity.returned);
                    balanceItemTotals.quantity.sold     = balanceItemTotals.quantity.sold.add(balanceItemN.quantity.sold);

                    balanceItemTotals.quantity.adjMinus = balanceItemTotals.quantity.adjMinus.add(balanceItemN.quantity.adjMinus);
                    balanceItemTotals.quantity.adjPlus  = balanceItemTotals.quantity.adjPlus.add(balanceItemN.quantity.adjPlus);

                    balanceItemTotals.quantity.revolving= balanceItemTotals.quantity.revolving.add(balanceItemN.quantity.revolving);
                    balanceItemTotals.quantity.net      = balanceItemTotals.quantity.received.
                                                          subtract(balanceItemTotals.quantity.returned).
                                                          subtract(balanceItemTotals.quantity.sold).
                                                          subtract(balanceItemTotals.quantity.adjMinus).
                                                          add(balanceItemTotals.quantity.adjPlus).
                                                          add(balanceItemTotals.quantity.revolving);

                    balanceItemTotals.balance.received = balanceItemTotals.balance.received.add(balanceItemN.balance.received);
                    balanceItemTotals.balance.returned = balanceItemTotals.balance.returned.add(balanceItemN.balance.returned);
                    balanceItemTotals.balance.sold     = balanceItemTotals.balance.sold.add(balanceItemN.balance.sold);
                    balanceItemTotals.balance.revolving= balanceItemTotals.balance.revolving.add(balanceItemN.balance.revolving);
                    
                    balanceItemTotals.balance.net      = balanceItemTotals.balance.received.subtract(balanceItemTotals.balance.returned).subtract(balanceItemTotals.balance.sold).add(balanceItemTotals.balance.revolving);

                }

            }// end of roll thru raw sum

            balanceItemTotals.key     = generateRowKey(Long.toString(pUserId) , 
                                                       balanceItemTotals.dets.brandId, 
                                                       balanceItemTotals.dets.itemCode, 
                                                       "U");//U for User

            balanceSheetLevel1.add(balanceItemTotals);

        }// end of thru unique brand + itemcodes 

        return balanceSheetLevel1;
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVEL1_RawData(EntityManager          pem, 
                                                                     long                   pUserId,
                                                                     ArrayList<ssoMerchant> pBranches,
                                                                     long                   pBrandId,
                                                                     String                 pBrandName) throws Exception
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevelRaw = new ArrayList<ssoUIBalanceItem>();

        for (ssoMerchant accN:pBranches)//26.12.2021
        {
            ArrayList<ssoAccInvBalanceCore> itemBalances = new ArrayList<ssoAccInvBalanceCore>();
            itemBalances = AccountMisc.calculateAccountBalance4Brand(   pem, 
                                                                        accN.Id, 
                                                                        accN.name,
                                                                        pBrandId);
            //Adding to Balance Sheet
            for(ssoAccInvBalanceCore ItemN:itemBalances)
            {
                ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                newItem.level = 1;
                newItem.dets.itemCode   = ItemN.ItemCode;
                newItem.dets.itemCodeId = ItemN.ItemCodeId;

                newItem.account        = accN.name;
                newItem.aid            = accN.Id;
                
                newItem.dets.brandId   = Long.toString(ItemN.BrandId);
                newItem.dets.brandName = Util.Str.wordNormalize(ItemN.Brandname);
                newItem.name = ItemN.Brandname + " / " + ItemN.ItemCode;

                // PARENT KEY WILL POINT TO SUMMARY LEVEL AND THIS RAW DATA WILL BE ADDED AS BRANCH DETS
                newItem.key       = generateRowKey(Long.toString(accN.Id) , Long.toString(pBrandId), ItemN.ItemCode, "A"); //A for Account
                newItem.parentKey = generateRowKey(Long.toString(pUserId) , Long.toString(pBrandId), ItemN.ItemCode, "U"); //U for User

                newItem.successRate     = ItemN.successRate;
                newItem.velocityStartup = ItemN.velocityStartup;
                newItem.velocityOverall = ItemN.velocityOverall;
                newItem.quantity = ItemN.quantity;
                newItem.balance  = ItemN.balance;
                newItem.lastActivity = ItemN.lastActivity;

                balanceSheetLevelRaw.add(newItem);//add balance of each item
            }

        }

        return balanceSheetLevelRaw;
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVEL2_RawData(EntityManager               pem, 
                                                                     ArrayList<ssoMerchant>      pBranches,
                                                                     long                        pBrandId,
                                                                     String                      pBrandName,
                                                                     String                      pItemCode) throws Exception
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();

        for(ssoMerchant branchN:pBranches)
        {

            // LEVEL 2 (Option Level)
            // PARENT_KEY = bRAND + Itemcode
            //------------------------------------------------------

            //3.2 calculate balance for brand-option(s) of accId
            ArrayList<ssoAccInvBalanceCore> optionBalances = new ArrayList<ssoAccInvBalanceCore>();
            optionBalances = AccountMisc.calculateOptionsBalance4Account( pem, 
                                                                          branchN.Id,
                                                                          branchN.name,//FOR NOW WE USE id instead oad pAccName,
                                                                          pBrandId//vendor Id
                                                                        );

            for(ssoAccInvBalanceCore optN:optionBalances)
            {
                if (pItemCode.trim().toLowerCase().equals(optN.ItemCode.trim().toLowerCase())==true)
                {
                    ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                    newItem.level = 2;
                    newItem.aid  = branchN.Id;
                    newItem.account = branchN.name;
                    newItem.name = optN.Option;
                    newItem.dets.brandId    = Long.toString(pBrandId);
                    newItem.dets.itemCode   = optN.ItemCode;
                    newItem.dets.optionId   = Long.parseLong(optN.OptionUID);
                    newItem.dets.option     = optN.Option;
                    newItem.dets.brandName  = Util.Str.wordNormalize(pBrandName);

                    //newItem.key = generateRowKey(newItem.aid, pBrandId, pBrandName, pBrandName)
                    //newItem.parentKey

                    // keys not need to be generated at raw level
                    newItem.key  = generateRowKeyWSign( ROW_KEY_SEPERATOR_SIGN, 
                                                        Long.toString(newItem.aid),
                                                        newItem.dets.brandId, 
                                                        newItem.dets.itemCode, 
                                                        Long.toString(newItem.dets.optionId),
                                                        newItem.dets.option,
                                                        "U");//U= User A=Account
                    //newItem.parentKey = generateRowKey(pBrandName, optN.ItemCode, "", "");//Long.toString(accN.id);

                    newItem.quantity = optN.quantity;
                    newItem.balance  = optN.balance;
                    newItem.lastActivity = optN.lastActivity;

                    balanceSheetLevel2.add(newItem);//add balance of each item
                }
            }

        }

        return balanceSheetLevel2;
        
        /*
        ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();

        for(ssoUIBalanceItem itemN:pBalanceSheetLevel1)
        {

            // LEVEL 2 (Option Level)
            // PARENT_KEY = bRAND + Itemcode
            //------------------------------------------------------

            //3.2 calculate balance for brand-option(s) of accId
            ArrayList<ssoAccInvBalanceCore> optionBalances = new ArrayList<ssoAccInvBalanceCore>();
            optionBalances = AccountMisc.calculateOptionsBalance4Account( pem, 
                                                                          pAccId,
                                                                          pAccName,
                                                                          pVendorId,
                                                                          itemN.dets.itemCodeId,//ItemN .ItemCodeId, 
                                                                          itemN.dets.itemCode, //ItemN.ItemCode, 
                                                                          pFiscalYear);

            for(ssoAccInvBalanceCore optN:optionBalances)
            {
                ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                newItem.level = 2;
                newItem.name = optN.Option;
                newItem.dets.itemCode   = optN.ItemCode;
                newItem.dets.option     = optN.Option;
                newItem.dets.brandName  = Util.Str.wordNormalize(optN.Brandname);

                newItem.key  = generateRowKey(optN.Brandname, optN.ItemCode, optN.Option);
                newItem.parentKey = generateRowKey(optN.Brandname, optN.ItemCode, "");//Long.toString(accN.id);
                newItem.quantity = optN.quantity;
                newItem.balance  = optN.balance;
                newItem.lastActivity = optN.lastActivity;

                balanceSheetLevel2.add(newItem);//add balance of each item
            }

        }

        return balanceSheetLevel2;
        */
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVEL3_Branches(  EntityManager               pem,
                                                                        ArrayList<ssoMerchant>      pBranches,
                                                                        ArrayList<ssoUIBalanceItem> pBalanceSheetLevel2) throws Exception
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevel3 = new ArrayList<ssoUIBalanceItem>();
        
        // LEVEL 3 = BRANCH LEVEL
        //----------------------------------------------------------
        // One for each option level (l2)
        // CALCULATE BOTTOM LINE FOR branches thru LEVEL 2 DATA (options)
        
        for(ssoMerchant branchN:pBranches)
        {
            
            for (ssoUIBalanceItem itemN:pBalanceSheetLevel2)
            {
                if (itemN.aid==branchN.Id)
                {
                    //itemN.aid
                    ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                    newItem.level = 2;
                    newItem.name = itemN.account;
                    //newItem.key  = itemN.account;
                    newItem.key  = generateRowKeyWSign( ROW_KEY_SEPERATOR_SIGN, 
                                                        Long.toString(itemN.aid),
                                                        itemN.dets.brandId, 
                                                        itemN.dets.itemCode, 
                                                        Long.toString(itemN.dets.optionId),
                                                        newItem.dets.option,
                                                        "A");

                    newItem.parentKey = itemN.key;
                    newItem.quantity = itemN.quantity;
                    newItem.balance  = itemN.balance;
                    newItem.lastActivity  = itemN.lastActivity;

                    balanceSheetLevel3.add(newItem);//add balance of each item
                }
            }

        }

        return balanceSheetLevel3;
    }

}
