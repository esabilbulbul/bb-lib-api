package bb.reports;

import bb.app.account.AccountMisc;
import bb.app.account.ssoAccInvBalanceCore;
import bb.app.account.ssoUIBalanceItem;
import bb.app.inv.InventoryMisc;
import static bb.app.inv.InventoryOps.generateRowKey;
import static bb.app.inv.InventoryOps.generateRowKeyWSign;
import bb.app.obj.ssoBrand;
import Objects.ssoMerchant;
import bb.app.account.UserOps;
import bb.app.dict.DictionaryOps;
import bb.app.inv.InventoryOps;
import bb.app.report.invsearch.ssoProfitCore;
import bb.app.report.invsearch.ssoSTRCore;
import bb.app.report.invsearch.ssoUIBalanceReport;
import bb.app.report.invsearch.ssoUIBalanceReportAccount;
import bb.app.report.invsearch.ssoUIBalanceReportGeneric;
import bb.app.report.invsearch.ssoUIBalanceReportItem;
import bb.app.report.invsearch.ssoUIBalanceReportVendor;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemStats;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.util.Util;
import jaxesa.util.ssoDBRowLimits;

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

    public static ssoUIBalanceReport generate4SummaryByKeyword( EntityManager pem, 
                                                                boolean       pbCleanMemory,
                                                                BigInteger    pUserId,
                                                                String        pKeyword,
                                                                String        pFinancialYear,
                                                                int           pPageNumber) throws Exception
    {
        /*
            Sample Data output 
            Item           - Net - Received - Returned - Sold - Activity 

            - MODIVA 
                    - BULBULLER  - 5   - 15 ...       (Breaking into Branch Level = RAW DATA)
                    - DILEK      - 5   - 5  ...
        */
        try
        {
            //int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            //ArrayList<ssoUIBalanceReportGeneric>  balanceSheet = new ArrayList<ssoUIBalanceReportGeneric>();

            ssoUIBalanceReport report = new ssoUIBalanceReport();
            //ArrayList<ssoUIBalanceReportGeneric> vendorsSummary  = new ArrayList<ssoUIBalanceReportGeneric>();
            //ArrayList<ssoUIBalanceReportGeneric> itemsSummary    = new ArrayList<ssoUIBalanceReportGeneric>();
            //ArrayList<ssoUIBalanceReportGeneric> accountsSummary = new ArrayList<ssoUIBalanceReportGeneric>();

            ArrayList<String> vendorIds2Search = new ArrayList<String>();
            ArrayList<String> vendorNItemCodess2Search = new ArrayList<String>();

            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //  1. KEYWORD MATCHING (IF GIVEN)
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                if(pKeyword.trim().length()>0)
                {
                    // 1. Search in Brands
                    // 2. Search in ItemCodes (if not found in brands)
                    ArrayList<String> vendors = new ArrayList<String>();

                    //----------------------------------------------------------
                    //  SEARCH IN VENDORS
                    //----------------------------------------------------------
                    String sKeyword = pKeyword.trim().toLowerCase();
                    vendors = DictionaryOps.Vendor.getListOfVendors(pem, pUserId);
                    for(String vendorN: vendors)
                    {
                        String[] aVendors = vendorN.split("-");

                        String sBrandId   = aVendors[0];
                        String sBrandName = aVendors[1];

                        sBrandName = sBrandName.toLowerCase();

                        if( (sBrandName.indexOf(sKeyword.toLowerCase())>=0) ||
                            (Util.Str.SIMIL(sBrandName, sKeyword)>=75)
                          )
                        {
                            //add to list 
                            vendorIds2Search.add(sBrandId);
                        }
                    }

                    ArrayList<String> vendorItemCodes = new ArrayList<String>();
                    //----------------------------------------------------------
                    //  SEARCH IN ITEMCODES
                    //----------------------------------------------------------
                    if(vendorIds2Search.size()==0)
                    {
                        vendorItemCodes = DictionaryOps.Vendor.getListOfItemCodes(pem, pUserId);

                        for(String vendorItemCodesN: vendorItemCodes)
                        {
                            String[] aVendorItemCodes = vendorItemCodesN.split(":");

                            String sBrandId   = aVendorItemCodes[0].trim();
                            String sItemCodes = aVendorItemCodes[1].trim();

                            String[] aItemCodes = sItemCodes.toLowerCase().split(",");

                            String sItemCodeListMatched = "";
                            for(String itemCodeN: aItemCodes)
                            {
                                if( (itemCodeN.indexOf(sKeyword.toLowerCase())>=0) ||
                                    (Util.Str.SIMIL(itemCodeN, sKeyword)>=85)
                                  )
                                {
                                    //add to list 
                                    if(sItemCodeListMatched.trim().length()>0)
                                        sItemCodeListMatched += ",";

                                    sItemCodeListMatched += itemCodeN;
                                }
                            }

                            if(sItemCodeListMatched.trim().length()>0)
                                vendorNItemCodess2Search.add(sBrandId + ":" + sItemCodeListMatched);
                        }
                    }
                }

            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //  2. DATA FETCHING
            //  
            //  a. 1st level fetch vendors (based on vendor or item code)
            //  b. 2nd level fetch the accounts 
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                if(pKeyword.trim().length()>0)
                {
                    if(vendorIds2Search.size()>0)
                    {
                        // Based on Vendors is being searched 
                        report.vendors = generateVendorsSummary(pem, pUserId, pFinancialYear, pPageNumber, vendorIds2Search);// SEARCH THRU VENDOR ONLY
                    }
                    else
                    {
                        // Based on Item Code is being searched 
                        report.vendors = generateItemsSummary(pem, pUserId, pFinancialYear, vendorNItemCodess2Search, pPageNumber);// SEARCH THRU VENDOR + ITEM CODE
                    }
                }
                else
                {
                    // DEFAULT SEARCH - NO KEYWORD 
                    report.vendors = generateVendorsSummary(pem, pUserId, pFinancialYear, pPageNumber, vendorIds2Search);// // SEARCH THRU ALL (NO FILTER)
                }

                if(report.vendors.size()>0)
                {
                    String sVendorList = "" ;
                    for(ssoUIBalanceReportVendor vendorN: report.vendors)
                    {
                        if(sVendorList.trim().length()>0)
                            sVendorList += ",";

                        sVendorList += vendorN.brandId;
                    }

                    //itemsSummary = generateItemsSummary(pem, pUserId, pFinancialYear, sVendorList);

                    report.accounts = generateAccountSummary(pem, pUserId, pFinancialYear, sVendorList);
                }


            return report;
        }
        catch(Exception e)
        {
            throw e;
        }

    }

    public static String generateKey4BrandLevel(String pUserId, String pBrandId)
    {
        return pUserId + "-" + pBrandId;
    }

    public static String generateKey4ItemLevel(String pUserId, String pBrandId, String pItemCode)
    {
        return pUserId + "-" + pBrandId + "-" + pItemCode;
    }

    public static String generateKey4AccountLevel(String pUserId, String pBrandId, String pAccId)
    {
        return pUserId + "-" + pBrandId + "-" + pAccId;
    }

    public static ArrayList<ssoUIBalanceItem> generate4SummaryByBrand(EntityManager pem, 
                                                                      boolean       pbCleanMemoryData,
                                                                      BigInteger    pUserId,
                                                                      BigInteger    pBrandId,
                                                                      int           pStmtYear) throws Exception
    {
        try
        {
            if(pbCleanMemoryData==true)
                pem.flush();

            ArrayList<ssoUIBalanceItem>  balanceSheet = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user 
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            // on Cache 
            branches = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);

            balanceSheet = generateLines4Brand(pem, pbCleanMemoryData, pUserId, branches, pBrandId, "", pStmtYear);

            return balanceSheet;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // Vendor 
    public static ArrayList<ssoUIBalanceReportVendor> generateVendorsSummary(  EntityManager           pem,
                                                                                BigInteger             pUserId,
                                                                                String                 pFinancialYear,
                                                                                int                    pPageIndex,
                                                                                ArrayList<String>      pVendorIdsMatched2Keyword) throws Exception
    {
        ArrayList<ssoUIBalanceReportVendor> vendorBalances = new ArrayList<ssoUIBalanceReportVendor>();
        BigDecimal bdRevolvingQuantity = new BigDecimal(BigInteger.ZERO);
        
        int iRowPerPage = 50;//DEFAULT
        int iPageIndex  = pPageIndex;
        //int iOffset     = iPageIndex *  iRowPerPage;//skip first N records
        //int iLimit      = (iPageIndex+1) * iRowPerPage;//50 per page
        
        ssoDBRowLimits rowLimits = new ssoDBRowLimits();
        rowLimits = Util.Database.calculateRowLimits(iRowPerPage, pPageIndex);
        int iOffset     = rowLimits.offset;
        int iLimit      = rowLimits.limit;

        try
        {
            int iCurrentYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            int iRequestYear = Integer.parseInt(pFinancialYear);
            boolean bArchive = false;
            
            if(iCurrentYear!=iRequestYear)
            {
                bArchive = true;
            }

            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            String sQuery = "SELECT " +
                            //"   ACC.PROFILENAME, " +
                            "	VND.UID AS BRAND_ID," +
                            "	VND.BRAND, ";
            
            if(bArchive==false)
            {
                sQuery  +=  "	SUM((STT.QUANTITY_ENTERED + STT.LAST_EOD_QUANTITY_ENTERED)) AS Q_ENTERED," +
                            "	SUM((STT.QUANTITY_RETURNED + STT.LAST_EOD_QUANTITY_RETURNED)) AS Q_RETURNED," +
                            "	SUM((STT.QUANTITY_CR_SOLD + STT.LAST_EOD_QUANTITY_CR_SOLD)) AS Q_CR_SOLD," +
                            "	SUM((STT.QUANTITY_CR_REFUND + STT.LAST_EOD_QUANTITY_CR_REFUND)) AS Q_CR_REFUND," +
                            "   SUM(STT.QUANTITY_ADJ_PLUS + STT.LAST_EOD_QUANTITY_ADJ_PLUS - STT.QUANTITY_ADJ_MINUS - STT.LAST_EOD_QUANTITY_ADJ_MINUS ) AS Q_ADJ_NET," +
                            "   SUM((STT.GROSS_TOTAL_ENTERED + STT.LAST_EOD_GROSS_TOTAL_ENTERED) - (STT.GROSS_TOTAL_RETURNED + STT.LAST_EOD_GROSS_TOTAL_RETURNED)) AS NET_PAID," + 
                            "   SUM((STT.GROSS_TOTAL_CR_SOLD + STT.LAST_EOD_GROSS_TOTAL_CR_SOLD) - (STT.GROSS_TOTAL_CR_REFUND + STT.LAST_EOD_GROSS_TOTAL_CR_REFUND)) AS NET_CR_SOLD,";
            }
            else
            {
                sQuery  +=  "	SUM(STT.LAST_EOD_QUANTITY_ENTERED) AS Q_ENTERED," +
                            "	SUM(STT.LAST_EOD_QUANTITY_RETURNED) AS Q_RETURNED," +
                            "	SUM(STT.LAST_EOD_QUANTITY_CR_SOLD) AS Q_CR_SOLD," +
                            "	SUM(STT.LAST_EOD_QUANTITY_CR_REFUND) AS Q_CR_REFUND," +
                            "   SUM(STT.LAST_EOD_QUANTITY_ADJ_PLUS - STT.LAST_EOD_QUANTITY_ADJ_MINUS ) AS Q_ADJ_NET," +
                            "   SUM(STT.LAST_EOD_GROSS_TOTAL_ENTERED - STT.LAST_EOD_GROSS_TOTAL_RETURNED) AS NET_PAID," + 
                            "   SUM(STT.LAST_EOD_GROSS_TOTAL_CR_SOLD - STT.LAST_EOD_GROSS_TOTAL_CR_REFUND) AS NET_CR_SOLD,";
            }
                sQuery  +=  "   IF(STT.LAST_ENTRY_DATE=0, STT.INSERTDATE, STT.LAST_ENTRY_DATE) AS LAST_INV_ACTIVITY," +
                            "   STT.LAST_SALES_DATE AS LAST_SALE_ACTIVITY, " +

                            "   QUARTER_SUMMARY_SALES AS QUARTER_SUMMARY_SALES," + //i.e. {"TRY":{"q1":{"t":200,"q":2},"q*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},"EUR":{"q1":{"t":5.4,"q":2},"q*":{"t":5.4,"q":2}},"INR":{"q1":{"t":485.02,"q":2},"q*":{"t":485.02,"q":2}},"SGD":{"q1":{"t":7.6,"q":2},"q*":{"t":7.6,"q":2}},"AUD":{"q1":{"t":8.94,"q":2},"q*":{"t":8.94,"q":2}}}
                            "   QUARTER_SUMMARY_BILLS AS QUARTER_SUMMARY_BILLS," + //i.e. {"q1":{"q":22.00,"t":2378.00},"q2":{"q":23.00,"t":3421.00},"q*":{"q":45.00,"t":5799.00}}
                            "   QUARTER_SUMMARY_PAYMENTS AS QUARTER_SUMMARY_PAYMENTS," +//i.e. {"TRY":{},"USD":{},"EUR":{},"INR":{},"SGD":{},"AUD":{}}

                            "   SEASON_SUMMARY_SALES AS SEASON_SUMMARY_SALES," + //i.e. {"TRY":{"winter":{"q":2,"t":200},"*":{"q":2,"t":200}},"USD":{"winter":{"q":2,"t":5.62},"*":{"q":2,"t":5.62}},"EUR":{"winter":{"q":2,"t":5.4},"*":{"q":2,"t":5.4}},"INR":{"winter":{"q":2,"t":485.02},"*":{"q":2,"t":485.02}},"SGD":{"winter":{"q":2,"t":7.6},"*":{"q":2,"t":7.6}},"AUD":{"winter":{"q":2,"t":8.94},"*":{"q":2,"t":8.94}}}
                            "   SEASON_SUMMARY_BILLS AS SEASON_SUMMARY_BILLS," + //i.e. {"winter":{"q":22.00,"t":2378.00},"summer":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}}
                            "   SEASON_SUMMARY_PAYMENTS AS SEASON_SUMMARY_PAYMENTS "; //i.e. {"TRY":{"q":0,"t":0},"USD":{"q":0,"t":0},"EUR":{"q":0,"t":0},"INR":{"q":0,"t":0},"SGD":{"q":0,"t":0},"AUD":{"q":0,"t":0}}

                if(bArchive==false)
                {
                    sQuery +=   "FROM ss_acc_inv_vendor_stats STT ";
                }
                else
                {
                    sQuery +=   "FROM ss_acc_inv_vendor_archive STT ";
                }

                            
                sQuery +=   "INNER JOIN ss_usr_accounts ACC ON ACC.UID = STT.ACCOUNT_ID " +
                            "INNER JOIN ss_acc_inv_vendors VND ON VND.USER_ID = ACC.USER_ID AND VND.UID = STT.VENDOR_ID " +
                            "WHERE " +
                            "VND.STAT = 1 " +
                            "AND " +
                            "STT.STAT = 1 " +
                            "AND " +
                            "ACC.STAT = 1 " +
                            "AND " +
                            "VND.USER_ID = ? " +
                            "AND " + 
                            "STT.FINANCIAL_YEAR = ? ";
                            
                            if(pVendorIdsMatched2Keyword.size()>0)
                            {
            sQuery +=           " AND VND.UID IN (";
                                
                                int iCnt = 0;
                                for(String vendorN:pVendorIdsMatched2Keyword)
                                {
                                    if(iCnt>0)
            sQuery +=                   ",";

            sQuery +=               "?";
                                    iCnt++;
                                }

            sQuery +=           ") ";

                            }

            sQuery +=       "GROUP BY VND.UID " + 
                            "ORDER BY VND.BRAND ASC " + 
                            "LIMIT ? OFFSET ?";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            stmt.SetParameter(index++, pUserId       , "USER_ID");
            stmt.SetParameter(index++, pFinancialYear, "FINANCIAL_YEAR");

            for(String vendorIdN:pVendorIdsMatched2Keyword)
            {
                stmt.SetParameter(index++, new BigInteger(vendorIdN)        , "BRAND_ID");
            }
            
            stmt.SetParameter(index++, iLimit        , "LIMIT");
            stmt.SetParameter(index++, iOffset       , "OFFSET");
            
            int iStartIndex = iPageIndex * iRowPerPage;

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoUIBalanceReportVendor balanceN = new ssoUIBalanceReportVendor();

                balanceN.brandId   = Util.Database.getValString(rs.get(i), "BRAND_ID");
                balanceN.name      = Util.Database.getValString(rs.get(i), "BRAND");

                balanceN.key       = generateKey4BrandLevel(pUserId.toString(), balanceN.brandId); //pUserId + "-" + balanceN.brandId;
                balanceN.parentKey = "";

                balanceN.quantity.received  = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_ENTERED"));
                balanceN.quantity.returned  = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_RETURNED"));
                balanceN.quantity.sold      = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_CR_SOLD"));
                balanceN.quantity.refund    = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_CR_REFUND"));
                balanceN.quantity.adjNet    = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_ADJ_NET"));

                balanceN.balance.payments  = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_PAID"));
                balanceN.balance.sold      = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_CR_SOLD"));

                balanceN.lastEntryDate = Util.Database.getValString(rs.get(i), "LAST_INV_ACTIVITY");
                balanceN.lastSalesDate = Util.Database.getValString(rs.get(i), "LAST_SALE_ACTIVITY");

                String sQuarterSummarySales     = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_SALES");
                String sQuarterSummaryBills     = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_BILLS");
                String sQuarterSummaryPayments  = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_PAYMENTS");

                String sSeasonSummarySales      = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_SALES");
                String sSeasonSummaryBills      = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_BILLS");
                String sSeasonSummaryPayments   = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_PAYMENTS");

                //String sSTROverAll  = "";
                //String sSTR2Quarter = "";
                //String sSTR2Season = "";
                if(balanceN.brandId.equals("46852181")==true)
                    balanceN.brandId = balanceN.brandId;

                // STR always calculated in base currency (TRY)
                // Profit should be calculated both base and USD 
                // Seasons starts from past year 10th month to current year 10th month
                // Quarters are Qs of that current year
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                balanceN.str  = calculateSTR(sSeasonSummarySales, sSeasonSummaryBills, "TRY", "*", "*").str;
                ssoProfitCore profitBase = new ssoProfitCore();
                ssoProfitCore profitUSD = new ssoProfitCore();

                balanceN.profitBase  = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "TRY", "*", "*");
                balanceN.profitUSD   = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "USD", "*", "*");

                balanceN.STRQuarters.q1            = calculateSTR(sQuarterSummarySales, sQuarterSummaryBills, "TRY", "q1", "*");
                balanceN.STRQuarters.q1.profitBase = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "TRY", "q1", "q1");
                balanceN.STRQuarters.q1.profitUSD  = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "USD", "q1", "q1");
                        
                balanceN.STRQuarters.q2 = calculateSTR(sQuarterSummarySales, sQuarterSummaryBills, "TRY", "q2", "*");
                balanceN.STRQuarters.q2.profitBase = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "TRY", "q2", "q2");
                balanceN.STRQuarters.q2.profitUSD  = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "USD", "q2", "q2");
                
                balanceN.STRQuarters.q3 = calculateSTR(sQuarterSummarySales, sQuarterSummaryBills, "TRY", "q3", "*");
                balanceN.STRQuarters.q3.profitBase = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "TRY", "q3", "q3");
                balanceN.STRQuarters.q3.profitUSD  = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "USD", "q3", "q3");
                
                balanceN.STRQuarters.q4 = calculateSTR(sQuarterSummarySales, sQuarterSummaryBills, "TRY", "q4", "*");
                balanceN.STRQuarters.q4.profitBase = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "TRY", "q4", "q4");
                balanceN.STRQuarters.q4.profitUSD  = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "USD", "q4", "q4");
                
                balanceN.STRQuarters.all = calculateSTR(sQuarterSummarySales, sQuarterSummaryBills, "TRY", "*", "*");
                balanceN.STRQuarters.all.profitBase = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "TRY", "*", "*");
                balanceN.STRQuarters.all.profitUSD  = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "USD", "*", "*");

                balanceN.STRSeasons.winter = calculateSTR(sSeasonSummarySales, sSeasonSummaryBills, "TRY", "winter", "*");
                balanceN.STRSeasons.winter.profitBase = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "TRY", "winter", "winter");
                balanceN.STRSeasons.winter.profitUSD  = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "USD", "winter", "winter");
                
                balanceN.STRSeasons.summer = calculateSTR(sSeasonSummarySales, sSeasonSummaryBills, "TRY", "summer", "*");
                balanceN.STRSeasons.summer.profitBase = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "TRY", "summer", "summer");
                balanceN.STRSeasons.summer.profitUSD  = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "USD", "summer", "summer");

                balanceN.STRSeasons.all    = calculateSTR(sSeasonSummarySales, sSeasonSummaryBills, "TRY", "*", "*");
                balanceN.STRSeasons.all.profitBase = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "TRY", "*", "*");
                balanceN.STRSeasons.all.profitUSD  = calculateProfit(sSeasonSummarySales, sSeasonSummaryPayments, "USD", "*", "*");

                //sSTR2Quarter = calculateSTR(sQuarterSummarySales, sQuarterSummaryBills, "TRY");
                vendorBalances.add(balanceN);
            }

            return vendorBalances;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // STR (Sell Thru Rate) = AllQ (Sold / Bought=Bills) *STR is always in local currency for Turkey it is TRY
    // pQuarterSummarySales = {"TRY":{"q1":{"t":200,"q":2},"*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},"EUR":{"q1":{"t":5.4,"q":2},"q*":{"t":5.4,"q":2}},"INR":{"q1":{"t":485.02,"q":2},"q*":{"t":485.02,"q":2}},"SGD":{"q1":{"t":7.6,"q":2},"q*":{"t":7.6,"q":2}},"AUD":{"q1":{"t":8.94,"q":2},"q*":{"t":8.94,"q":2}}}
    // pQuarterSummaryBills = {"q1":{"q":22.00,"t":2378.00},"q2":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}}
    // 
    // pSeasonsSummarySales = "TRY":{"winter":{"q":1,"ts":200,"tc":100},"*":{"q":1,"ts":200,"tc":100}}
    // pSeasonsSummaryBills = "winter":{"q":9.00,"t":1210.00},"*":{"q":9.00,"t":1210.00}
    // 
    // QUARTERs are calculated 1 thru 4 of current year 
    // SEASONs are calculated from past years q4 thru q3 of the current year 
    // QSTR / SSTR 
    //
    // STR = Cost of Total Sold / Total Bought
    // STR comparison must be on the same currency. Bill summary is always calculated based on core currency while 
    // sales can be calculated with multiple currencies 
    //
    // pTerm 
    //    * = All Year
    //    q = quarter
    //    winter/summer = season
    public static ssoSTRCore calculateSTR(String pSummarySales, String pSummaryBills, String pCurrencyCode, String pTermKey1, String pTermKey2) throws Exception
    {
        ssoSTRCore str = new ssoSTRCore();

        try
        {
            // For example; 
            // For Sales 
            // get TRY-> q* -> t(otal)
            // For Bills
            // get q* -> t(otal)
            // STR = Sales.q*.t / Bills.q*.t
            String sTotalQuantitySales  = "0";
            String sTotalSalesCost = "0";
            String sTotalQuantityBills  = "0";
            String sTotalBillsCost = "0";
            String sTermQuantityBills  = "0";
            String sTermBillsCost = "0";

            //String KEY_ALL_TOTAL = "*";

            Gson gson = new Gson();

            // Sales Summary (all quarters total)
            // {"TRY":{"q1":{"t":200,"q":2},"*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},
            // OR
            // {"winter":{"q":22.00,"t":2378.00},"summer":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}}
            //-----------------------------------------------------------------
            Map<String, Map<String, Map<String, String>>> mSales = gson.fromJson(pSummarySales, Map.class);
            if(mSales!=null)
            {
                if(mSales.get(pCurrencyCode)!=null)
                {
                    if(mSales.get(pCurrencyCode).get(pTermKey1)!=null)
                    {
                        sTotalQuantitySales = String.valueOf(mSales.get(pCurrencyCode).get(pTermKey1).get("q"));
                        sTotalSalesCost    = String.valueOf(mSales.get(pCurrencyCode).get(pTermKey1).get("tc"));//total cost
                    }
                }
            }

            // Bills Summary (all quarters total)
            // {"q1":{"q":22.00,"t":2378.00},"q2":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}} 
            // or 
            // {"winter":{"q":22.00,"t":2378.00},"summer":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}} 
            //-----------------------------------------------------------------
            Map<String, Map<String, String>> mBills = gson.fromJson(pSummaryBills, Map.class);
            if(mBills!=null)
            {
                if(mBills.get(pTermKey2)!=null)
                {
                    sTotalQuantityBills  = String.valueOf(mBills.get(pTermKey2).get("q"));
                    sTotalBillsCost = String.valueOf(mBills.get(pTermKey2).get("t"));
                }
                
                if(mBills.get(pTermKey1)!=null)
                {
                    sTermQuantityBills  = String.valueOf(mBills.get(pTermKey2).get("q"));
                    sTermBillsCost      = String.valueOf(mBills.get(pTermKey2).get("t"));
                }
            }
            

            BigDecimal bdTotalSales = new BigDecimal(sTotalSalesCost);
            BigDecimal bdTotalBills = new BigDecimal(sTotalBillsCost);
            BigDecimal bdSTR = new BigDecimal(BigInteger.ZERO);

            // sales / bills and divide by 2. 
            // because when you sell the same amount of bills you just covered the cost. you at least need 
            // to be twice more because you are selling with profit 
            if(bdTotalBills.compareTo(BigDecimal.ZERO)!=0)
            {
                bdSTR = bdTotalSales.divide(bdTotalBills, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal.valueOf(100));
            }

            str.cSold   = sTotalSalesCost;
            str.cBought = sTermBillsCost;

            str.qSold   = sTotalQuantitySales;
            str.qBought = sTermQuantityBills;

            str.str     = bdSTR.toString();

            return str;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // summary payment = {"TRY":{"q1":{"t":200,"q":2},"*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},"EUR":{"q1":{"t":5.4,"q":2},"q*":{"t":5.4,"q":2}},"INR":{"q1":{"t":485.02,"q":2},"q*":{"t":485.02,"q":2}},"SGD":{"q1":{"t":7.6,"q":2},"q*":{"t":7.6,"q":2}},"AUD":{"q1":{"t":8.94,"q":2},"q*":{"t":8.94,"q":2}}}
    // pQuarterSummarySales = "TRY":{"winter":{"q":1,"ts":200,"tc":100},"*":{"q":1,"ts":200,"tc":100}} // this is summary related also available in quarter format
    public static ssoProfitCore calculateProfit(String pSummarySales, String pSummaryPayments, String pCurrencyCode, String pTermKey1, String pTermKey2) throws Exception
    {
        ssoProfitCore profit = new ssoProfitCore();

        try
        {
            // For example; 
            // For Sales 
            // get TRY-> q* -> t(otal)
            // For Bills
            // get q* -> t(otal)
            // STR = Sales.q*.t / Bills.q*.t
            String sSalesQuantity  = "0";
            String sSalesTotal     = "0";
            String sPaymentsQuantity  = "0";
            String sPaymentsTotal     = "0";

            //String KEY_ALL_TOTAL = "*";

            Gson gson = new Gson();

            // Sales Summary (all quarters total)
            // {"TRY":{"q1":{"t":200,"q":2},"*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},
            // OR
            // {"winter":{"q":22.00,"t":2378.00},"summer":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}}
            //-----------------------------------------------------------------
            Map<String, Map<String, Map<String, String>>> mSales = gson.fromJson(pSummarySales, Map.class);
            if(mSales!=null)
            {
                if(mSales.get(pCurrencyCode)!=null)
                {
                    if(mSales.get(pCurrencyCode).get(pTermKey1)!=null)
                    {
                        sSalesQuantity = String.valueOf(mSales.get(pCurrencyCode).get(pTermKey1).get("q"));
                        sSalesTotal         = String.valueOf(mSales.get(pCurrencyCode).get(pTermKey1).get("ts"));//total cost
                    }
                }
            }

            // Payments Summary (all quarters total)
            // {"TRY":{"q1":{"t":200,"q":2},"*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},
            // OR
            // {"winter":{"q":22.00,"t":2378.00},"summer":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}}
            //-----------------------------------------------------------------
            Map<String, Map<String, Map<String, String>>> mPayments = gson.fromJson(pSummaryPayments, Map.class);
            if(mPayments!=null)
            {
                if(mPayments.get(pCurrencyCode)!=null)
                {
                    if(mPayments.get(pCurrencyCode).get(pTermKey2)!=null)
                    {
                        sPaymentsQuantity = String.valueOf(mPayments.get(pCurrencyCode).get(pTermKey2).get("q"));
                        sPaymentsTotal    = String.valueOf(mPayments.get(pCurrencyCode).get(pTermKey2).get("t"));//total cost
                    }
                }
            }

            BigDecimal bdTotalSales   = new BigDecimal(sSalesTotal);
            BigDecimal bdTotalPayment = new BigDecimal(sPaymentsTotal).abs();
            BigDecimal bdProfitRate = new BigDecimal(BigInteger.ZERO);
            BigDecimal bdProfitTotal = new BigDecimal(BigInteger.ZERO);
            
            // sales / bills and divide by 2. 
            // because when you sell the same amount of bills you just covered the cost. you at least need 
            // to be twice more because you are selling with profit 
            if(bdTotalPayment.compareTo(BigDecimal.ZERO)!=0)
            {
                bdProfitRate  = bdTotalSales.subtract(bdTotalPayment).divide(bdTotalPayment, 2, RoundingMode.HALF_EVEN).subtract(BigDecimal.ONE);// (sales - payments / payments) - 1
                bdProfitTotal = bdTotalSales.subtract(bdTotalPayment);
            }
            
            profit.rate  = bdProfitRate.toString();
            profit.total = bdProfitTotal.toString();
            
            return profit;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // Vendor -> Item Code 
    public static ArrayList<ssoUIBalanceReportVendor> generateItemsSummary( EntityManager          pem,
                                                                            BigInteger             pUserId,
                                                                            String                 pFinancialYear,
                                                                            ArrayList<String>      paVendorNItemCodeList,
                                                                            int                    piPageNumber) throws Exception
    {
        ArrayList<ssoUIBalanceReportVendor> itemBalances = new ArrayList<ssoUIBalanceReportVendor>();
        BigDecimal bdRevolvingQuantity = new BigDecimal(BigInteger.ZERO);

        ssoDBRowLimits rowLimits = new ssoDBRowLimits();
        rowLimits = Util.Database.calculateRowLimits(50, piPageNumber);
        int iOffset     = rowLimits.offset;
        int iLimit      = rowLimits.limit;

        try
        {
            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            String sQuery = "SELECT " +
                            "	VND.UID AS BRAND_ID," +
                            "	VND.BRAND, " +
                            "    STT.ITEM_CODE," +
                            //"    PRC.ENTRY_PRICE," +
                            //"    PRC.SALE_PRICE," +
                            "	SUM((STT.QUANTITY_ENTERED + STT.LAST_EOD_QUANTITY_ENTERED)) AS Q_ENTERED," +
                            "	SUM((STT.QUANTITY_RETURNED + STT.LAST_EOD_QUANTITY_RETURNED)) AS Q_RETURNED," +
                            "	SUM((STT.QUANTITY_CR_SOLD + STT.LAST_EOD_QUANTITY_CR_SOLD)) AS Q_CR_SOLD," +
                            "	SUM((STT.QUANTITY_CR_REFUND + STT.LAST_EOD_QUANTITY_CR_REFUND)) AS Q_CR_REFUND," +
                            "    SUM(STT.QUANTITY_ADJ_PLUS + STT.LAST_EOD_QUANTITY_ADJ_PLUS - STT.QUANTITY_ADJ_MINUS - STT.LAST_EOD_QUANTITY_ADJ_MINUS ) AS Q_ADJ_NET," +
                            "    SUM(STT.GROSS_TOTAL_ENTERED - STT.GROSS_TOTAL_RETURNED) AS NET_PAID," +
                            "    SUM(STT.GROSS_TOTAL_CR_SOLD - STT.GROSS_TOTAL_CR_REFUND) AS NET_CR_SOLD, " +
                            "    IF(STT.LAST_ENTRY_DATE=0, STT.INSERTDATE, STT.LAST_ENTRY_DATE) AS LAST_INV_ACTIVITY," +
                            "    STT.LAST_SALES_DATE AS LAST_SALE_ACTIVITY " +
                            "FROM ss_acc_inv_item_stats STT " +
                            //"INNER JOIN ss_acc_inv_item_price PRC ON PRC.UID = STT.PRICE_ID " +
                            "INNER JOIN ss_usr_accounts ACC ON ACC.UID = STT.ACCOUNT_ID " +
                            "INNER JOIN ss_acc_inv_vendors VND ON VND.USER_ID = ACC.USER_ID " +
                            "WHERE " +
                            "VND.STAT = 1 " +
                            "AND " +
                            "STT.STAT = 1 " +
                            "AND " +
                            "VND.USER_ID = ? " +
                            "AND " +  
                            "STT.FINANCIAL_YEAR = ? ";

                            if(paVendorNItemCodeList.size()>0)
                            {
                                
                                // ADDING VENDOR IDS 
                                //----------------------------------------------
            sQuery +=           " AND VND.UID IN (";
                                
                                int iCnt = 0;
                                for(String vendorNItemCodeN:paVendorNItemCodeList)
                                {
                                    if(iCnt>0)
            sQuery +=                   ",";

            sQuery +=               "?";
                                    iCnt++;
                                }

            sQuery +=           ") ";

                                // ADDING ITEM CODES
                                //----------------------------------------------
            sQuery +=           " AND STT.ITEM_CODE IN (";
                                
                                iCnt = 0;
                                for(String vendorNItemCodeN:paVendorNItemCodeList)
                                {
                                    String[] aVendorNItemCodes = vendorNItemCodeN.split(":");

                                    String sVendorId  = aVendorNItemCodes[0];
                                    String sItemCodes = aVendorNItemCodes[1];
                                    String[] aItemCodes = sItemCodes.split(",");

                                    for(String sItemCodeN: aItemCodes)
                                    {
                                        if(iCnt>0)
            sQuery +=                       ",";

            sQuery +=                   "?";
                                        iCnt++;
                                    }
                                }

            sQuery +=           ") ";

                            }// end of array 

            sQuery +=       "GROUP BY VND.UID, STT.ITEM_CODE " + 
                            "ORDER BY VND.BRAND, STT.ITEM_CODE ASC " +
                            "LIMIT ? OFFSET ?";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            stmt.SetParameter(index++, pUserId       , "USER_ID");
            stmt.SetParameter(index++, pFinancialYear, "FINANCIAL_YEAR");

            // FIRST SET VENDOR IDS 
            //-----------------------------------------------------------------
            for(String vendorNItemCodeN:paVendorNItemCodeList)
            {
                String[] aVendorNItemCodes = vendorNItemCodeN.split(":");

                String sVendorId  = aVendorNItemCodes[0];
                String sItemCodes = aVendorNItemCodes[1];

                stmt.SetParameter(index++, new BigInteger(sVendorId), "VENDOR_ID");
                
            }

            // 2nd SET ITEM CODES
            //-----------------------------------------------------------------
            for(String vendorNItemCodeN:paVendorNItemCodeList)
            {
                String[] aVendorNItemCodes = vendorNItemCodeN.split(":");

                String sVendorId  = aVendorNItemCodes[0];
                String sItemCodes = aVendorNItemCodes[1];
                String[] aItemCodes = sItemCodes.split(",");

                for(String sItemCodeN: aItemCodes)
                {
                    stmt.SetParameter(index++, sItemCodeN, "ITEM_CODE");
                }

            }

            stmt.SetParameter(index++, rowLimits.limit , "LIMIT");
            stmt.SetParameter(index++, rowLimits.offset, "OFFSET");

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoUIBalanceReportVendor balanceN = new ssoUIBalanceReportVendor();

                balanceN.brandId   = Util.Database.getValString(rs.get(i), "BRAND_ID"); 
                //balanceN.brandName = Util.Database.getValString(rs.get(i), "BRAND");
                balanceN.itemCode  = Util.Database.getValString(rs.get(i), "ITEM_CODE"); 
                balanceN.name      = Util.Database.getValString(rs.get(i), "BRAND") + " / " + balanceN.itemCode;
                //balanceN.entryPrice= Util.Database.getValString(rs.get(i), "ENTRY_PRICE"); 
                //balanceN.salePrice = Util.Database.getValString(rs.get(i), "SALE_PRICE"); 

                //balanceN.key       = generateKey4ItemLevel(pUserId.toString(), balanceN.brandId, balanceN.itemCode);
                //balanceN.parentKey = generateKey4BrandLevel(pUserId.toString(), balanceN.brandId);
                balanceN.key       = generateKey4BrandLevel(pUserId.toString(), balanceN.brandId); //pUserId + "-" + balanceN.brandId;
                balanceN.parentKey = "";

                balanceN.quantity.received   = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_ENTERED"));
                balanceN.quantity.returned   = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_RETURNED"));
                balanceN.quantity.sold       = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_CR_SOLD"));
                balanceN.quantity.refund     = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_CR_REFUND"));
                balanceN.quantity.adjNet     = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_ADJ_NET"));

                balanceN.balance.payments  = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_PAID"));
                balanceN.balance.sold      = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_CR_SOLD"));

                balanceN.lastEntryDate = Util.Database.getValString(rs.get(i), "LAST_INV_ACTIVITY");
                balanceN.lastSalesDate = Util.Database.getValString(rs.get(i), "LAST_SALE_ACTIVITY");

                itemBalances.add(balanceN);
            }

            return itemBalances;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // Account -> Vendor -> ItemCode
    public static ArrayList<ssoUIBalanceReportAccount> generateAccountSummary(  EntityManager          pem,
                                                                                BigInteger             pUserId,
                                                                                String                 pFinancialYear,
                                                                                String                 pVendorList) throws Exception
    {
        ArrayList<ssoUIBalanceReportAccount> itemBalances = new ArrayList<ssoUIBalanceReportAccount>();
        
        try
        {
            int iCurrentYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            int iRequestYear = Integer.parseInt(pFinancialYear);
            boolean bArchive = false;
            
            if(iCurrentYear!=iRequestYear)
            {
                bArchive = true;
            }
            
            String  sQuery = "SELECT " +
                            "	STT.ACCOUNT_ID," +
                            "   ACC.PROFILENAME," +
                            "	VND.UID AS BRAND_ID," +
                            "	VND.BRAND, ";
                            //"    STT.ITEM_CODE," +

                if(bArchive==false)
                {
                    sQuery  +=  "   SUM((STT.QUANTITY_ENTERED + STT.LAST_EOD_QUANTITY_ENTERED)) AS Q_ENTERED," +
                                "   SUM((STT.QUANTITY_RETURNED + STT.LAST_EOD_QUANTITY_RETURNED)) AS Q_RETURNED," +
                                "   SUM((STT.QUANTITY_CR_SOLD + STT.LAST_EOD_QUANTITY_CR_SOLD)) AS Q_CR_SOLD," +
                                "   SUM((STT.QUANTITY_CR_REFUND + STT.LAST_EOD_QUANTITY_CR_REFUND)) AS Q_CR_REFUND," +
                                "   SUM(STT.QUANTITY_ADJ_PLUS + STT.LAST_EOD_QUANTITY_ADJ_PLUS - STT.QUANTITY_ADJ_MINUS - STT.LAST_EOD_QUANTITY_ADJ_MINUS ) AS Q_ADJ_NET," +
                                "   SUM((STT.GROSS_TOTAL_ENTERED + STT.LAST_EOD_GROSS_TOTAL_ENTERED) - (STT.GROSS_TOTAL_RETURNED + STT.LAST_EOD_GROSS_TOTAL_RETURNED)) AS NET_PAID," + 
                                "   SUM((STT.GROSS_TOTAL_CR_SOLD + STT.LAST_EOD_GROSS_TOTAL_CR_SOLD) - (STT.GROSS_TOTAL_CR_REFUND + STT.LAST_EOD_GROSS_TOTAL_CR_REFUND)) AS NET_CR_SOLD,";
                }
                else
                {
                    sQuery  +=  "   SUM(STT.LAST_EOD_QUANTITY_ENTERED) AS Q_ENTERED," +
                                "   SUM(STT.LAST_EOD_QUANTITY_RETURNED) AS Q_RETURNED," +
                                "   SUM(STT.LAST_EOD_QUANTITY_CR_SOLD) AS Q_CR_SOLD," +
                                "   SUM(STT.LAST_EOD_QUANTITY_CR_REFUND) AS Q_CR_REFUND," +
                                "   SUM(STT.LAST_EOD_QUANTITY_ADJ_PLUS - STT.LAST_EOD_QUANTITY_ADJ_MINUS ) AS Q_ADJ_NET," +
                                "   SUM(STT.LAST_EOD_GROSS_TOTAL_ENTERED - STT.LAST_EOD_GROSS_TOTAL_RETURNED) AS NET_PAID," + 
                                "   SUM(STT.LAST_EOD_GROSS_TOTAL_CR_SOLD - STT.LAST_EOD_GROSS_TOTAL_CR_REFUND) AS NET_CR_SOLD,";
                }
                    
                sQuery  +=  "    IF(STT.LAST_ENTRY_DATE=0, STT.INSERTDATE, STT.LAST_ENTRY_DATE) AS LAST_INV_ACTIVITY," +
                            "    STT.LAST_SALES_DATE AS LAST_SALE_ACTIVITY, " +

                            "   STT.QUARTER_SUMMARY_SALES AS QUARTER_SUMMARY_SALES," + //i.e. {"TRY":{"q1":{"t":200,"q":2},"q*":{"t":200,"q":2}},"USD":{"q1":{"t":5.62,"q":2},"q*":{"t":5.62,"q":2}},"EUR":{"q1":{"t":5.4,"q":2},"q*":{"t":5.4,"q":2}},"INR":{"q1":{"t":485.02,"q":2},"q*":{"t":485.02,"q":2}},"SGD":{"q1":{"t":7.6,"q":2},"q*":{"t":7.6,"q":2}},"AUD":{"q1":{"t":8.94,"q":2},"q*":{"t":8.94,"q":2}}}
                            "   STT.QUARTER_SUMMARY_BILLS AS QUARTER_SUMMARY_BILLS," + //i.e. {"q1":{"q":22.00,"t":2378.00},"q2":{"q":23.00,"t":3421.00},"q*":{"q":45.00,"t":5799.00}}
                            "   STT.QUARTER_SUMMARY_PAYMENTS AS QUARTER_SUMMARY_PAYMENTS," +//i.e. {"TRY":{},"USD":{},"EUR":{},"INR":{},"SGD":{},"AUD":{}}

                            "   STT.SEASON_SUMMARY_SALES AS SEASON_SUMMARY_SALES," + //i.e. {"TRY":{"winter":{"q":2,"t":200},"*":{"q":2,"t":200}},"USD":{"winter":{"q":2,"t":5.62},"*":{"q":2,"t":5.62}},"EUR":{"winter":{"q":2,"t":5.4},"*":{"q":2,"t":5.4}},"INR":{"winter":{"q":2,"t":485.02},"*":{"q":2,"t":485.02}},"SGD":{"winter":{"q":2,"t":7.6},"*":{"q":2,"t":7.6}},"AUD":{"winter":{"q":2,"t":8.94},"*":{"q":2,"t":8.94}}}
                            "   STT.SEASON_SUMMARY_BILLS AS SEASON_SUMMARY_BILLS," + //i.e. {"winter":{"q":22.00,"t":2378.00},"summer":{"q":23.00,"t":3421.00},"*":{"q":45.00,"t":5799.00}}
                            "   STT.SEASON_SUMMARY_PAYMENTS AS SEASON_SUMMARY_PAYMENTS "; //i.e. {"TRY":{"q":0,"t":0},"USD":{"q":0,"t":0},"EUR":{"q":0,"t":0},"INR":{"q":0,"t":0},"SGD":{"q":0,"t":0},"AUD":{"q":0,"t":0}}

            if(bArchive==false)
            {
                sQuery +=   "FROM ss_acc_inv_vendor_stats STT ";
            }
            else
            {
                sQuery +=   "FROM ss_acc_inv_vendor_archive STT ";
            }

                            //"INNER JOIN ss_acc_inv_item_price PRC ON PRC.UID = STT.PRICE_ID " +
            sQuery +=       "INNER JOIN ss_usr_accounts ACC ON ACC.UID = STT.ACCOUNT_ID " +
                            "INNER JOIN ss_acc_inv_vendors VND ON VND.USER_ID = ACC.USER_ID AND VND.UID = STT.VENDOR_ID " +
                            "WHERE " +
                            "VND.STAT = 1 " +
                            "AND " +
                            "STT.STAT = 1 " +
                            "AND " +
                            "VND.USER_ID = ? " +
                            "AND ";

                            if(pVendorList.trim().length()>0)
                            {
            sQuery +=           "VND.UID IN ( " + pVendorList + ") " + 
                                "AND ";
                            }

            sQuery +=       "STT.FINANCIAL_YEAR = ? " +
                            "GROUP BY STT.ACCOUNT_ID, VND.UID " +
                            "ORDER BY STT.ACCOUNT_ID, VND.BRAND ASC";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            stmt.SetParameter(index++, pUserId       , "USER_ID");
            stmt.SetParameter(index++, pFinancialYear, "FINANCIAL_YEAR");

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoUIBalanceReportAccount balanceN = new ssoUIBalanceReportAccount();

                balanceN.name   = Util.Database.getValString(rs.get(i), "PROFILENAME");
                balanceN.accId     = Util.Database.getValString(rs.get(i), "ACCOUNT_ID");
                balanceN.brandId   = Util.Database.getValString(rs.get(i), "BRAND_ID"); 
                //balanceN.brandName = Util.Database.getValString(rs.get(i), "BRAND");
                //balanceN.itemCode  = Util.Database.getValString(rs.get(i), "ITEM_CODE"); 
                balanceN.entryPrice= Util.Database.getValString(rs.get(i), "ENTRY_PRICE"); 
                balanceN.salePrice = Util.Database.getValString(rs.get(i), "SALE_PRICE"); 

                String sQuarterSummarySales     = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_SALES");
                String sQuarterSummaryBills     = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_BILLS");
                String sQuarterSummaryPayments  = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_PAYMENTS");

                String sSeasonSummarySales      = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_SALES");
                String sSeasonSummaryBills      = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_BILLS");
                String sSeasonSummaryPayments   = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_PAYMENTS");

                balanceN.str  = calculateSTR(sSeasonSummarySales, sSeasonSummaryBills, "TRY", "*", "*").str;
                ssoProfitCore profitBase = new ssoProfitCore();
                ssoProfitCore profitUSD = new ssoProfitCore();

                balanceN.profitBase  = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "TRY", "*", "*");
                balanceN.profitUSD   = calculateProfit(sQuarterSummarySales, sQuarterSummaryPayments, "USD", "*", "*");

                
                balanceN.key       = generateKey4AccountLevel(pUserId.toString(), balanceN.brandId, balanceN.accId);
                balanceN.parentKey = generateKey4BrandLevel(pUserId.toString(), balanceN.brandId);//to tie under the brand

                balanceN.quantity.received   = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_ENTERED"));
                balanceN.quantity.returned   = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_RETURNED"));
                balanceN.quantity.sold       = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_CR_SOLD"));
                balanceN.quantity.refund     = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_CR_REFUND"));
                balanceN.quantity.adjNet     = new BigDecimal(Util.Database.getValString(rs.get(i), "Q_ADJ_NET"));

                balanceN.balance.payments  = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_PAID"));
                balanceN.balance.sold      = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_CR_SOLD"));

                balanceN.lastEntryDate = Util.Database.getValString(rs.get(i), "LAST_INV_ACTIVITY");
                balanceN.lastSalesDate = Util.Database.getValString(rs.get(i), "LAST_SALE_ACTIVITY");

                itemBalances.add(balanceN);
            }

            return itemBalances;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoUIBalanceItem> generateLines4Brand(  EntityManager          pem,
                                                                    boolean                pbCleanMemory,
                                                                    BigInteger             pUserId,
                                                                    ArrayList<ssoMerchant> pBranches,
                                                                    BigInteger             pBrandId,
                                                                    String                 pBrandName,
                                                                    int                    pStmtYear) throws Exception
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
                                                                                        pbCleanMemory,
                                                                                        pUserId,
                                                                                        pBranches,
                                                                                        pBrandId,
                                                                                        pBrandName,
                                                                                        pStmtYear);

            // LEVEL 1 (Item Code Level)
            // PARENT_KEY = <empty> + <brandId> + <itemcode> + "a/u" (u = user for summary level a = a for account)
            // ON CACHE
            //----------------------------------------------------------
            ArrayList<ssoUIBalanceItem>  invSheetLevel1Summary = new ArrayList<ssoUIBalanceItem>();
            invSheetLevel1Summary = ssReportSearchInventory.generateLEVEL1_Summary(pUserId, balanceSheetLevel1RawData );

            ArrayList<ssoUIBalanceItem>  invSheetLevel1BranchDets = new ArrayList<ssoUIBalanceItem>();
            invSheetLevel1BranchDets = ssReportSearchInventory.generateLEVEL1_BranchDets(balanceSheetLevel1RawData);

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
                                                              boolean       pbCleanMemory,
                                                              BigInteger    pUserId,
                                                              BigInteger    pAccId,//depending on target type use User or Account
                                                              String        pTargetType,//User or Account (branch)
                                                              BigInteger    pBrandId,//vendor
                                                              String        pItemCode,
                                                              int           pPageNumber
                                                              ) throws Exception
    {
        try
        {
            ArrayList<ssoUIBalanceItem>  balanceSheetAll = new ArrayList<ssoUIBalanceItem>();
            ArrayList<ssoUIBalanceItem>  balanceSheetItems = new ArrayList<ssoUIBalanceItem>();
            ArrayList<ssoUIBalanceItem>  balanceSheetOptions = new ArrayList<ssoUIBalanceItem>();

            // Get branches / accounts linked to the user
            ArrayList<ssoMerchant> branches = new ArrayList<ssoMerchant>();
            // on Cache

            boolean bItemCode = false;
            if(pItemCode.trim().length()>0)
                bItemCode = true;

            balanceSheetItems = getInventory4Items( pem, 
                                                    pUserId, 
                                                    pAccId, 
                                                    pTargetType, 
                                                    pBrandId, 
                                                    pItemCode, 
                                                    bItemCode,
                                                    pbCleanMemory, 
                                                    pPageNumber);

            balanceSheetOptions = getInventory4ItemsOptions(pem, 
                                                            pUserId, 
                                                            pAccId, 
                                                            pTargetType, 
                                                            pBrandId, 
                                                            pItemCode, 
                                                            bItemCode, 
                                                            pbCleanMemory, 
                                                            pPageNumber);

            balanceSheetAll.addAll(balanceSheetItems);
            balanceSheetAll.addAll(balanceSheetOptions);
            
            return balanceSheetAll;
/*            
            // CALCULATE FOR EACH BRANCH LEVEL
            //------------------------------------------------------------------
            ArrayList<ssoUIBalanceItem>  balanceSheetLevelRawData = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevelRawData = ssReportSearchInventory.generateRawData(pem,
                                                                                pbCleanMemory,
                                                                                branches,
                                                                                pBrandId,
                                                                                "",
                                                                                pItemCode,
                                                                                pPageNumber);

            // CALCULATE THE FINAL THRU THE RAW DATA
            // SUM(BRANCHES)
            //------------------------------------------------------------------
            ArrayList<ssoUIBalanceItem>  balanceSheetLevel1 = new ArrayList<ssoUIBalanceItem>();
            balanceSheetLevel1 = ssReportSearchInventory.generateLEVELOptions( balanceSheetLevelRawData );

            // LEVEL 3 = BRANCH LEVEL
            // ON CACHE
            //----------------------------------------------------------
            // CALCULATE BOTTOM LINE FOR branches thru LEVEL 2 DATA (options)
            ArrayList<ssoUIBalanceItem>  balanceSheetLevel2 = new ArrayList<ssoUIBalanceItem>();
            if (pTargetType.equals("U")==true)
            {

                balanceSheetLevel2 = ssReportSearchInventory.generateLEVEL3_Branches(pem,
                                                                                     branches,
                                                                                     balanceSheetLevel1);
            }
            
            balanceSheet.addAll(balanceSheetLevel1);
            balanceSheet.addAll(balanceSheetLevel2);

            return balanceSheet;
*/
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVELOptions(ArrayList<ssoUIBalanceItem> pBalanceSheetLevel2RawData)
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
                //if (sBrandNOption.equals(optionN.dets.option)==true)
                if (sBrandNOption.equals(optionN.name)==true)
                {
                    bFound = true;
                }
            }

            if (bFound==false)
            {
                //aUniqueBrandNOptions.add(optionN.key);
                //aUniqueBrandNOptions.add(optionN.dets.option);
                aUniqueBrandNOptions.add(optionN.name);
            }
        }

        // Calculate Totals 
        for(String sBrandNOptionN:aUniqueBrandNOptions)
        {
            ssoUIBalanceItem balanceItemTotals = new ssoUIBalanceItem();

            for (ssoUIBalanceItem balanceItemN:pBalanceSheetLevel2RawData)
            {
                //if (sBrandNOptionN.equals(balanceItemN.key)==true)//sum for the same options
                //if(sBrandNOptionN.equals(balanceItemN.dets.option)==true)
                if(sBrandNOptionN.equals(balanceItemN.name)==true)
                {
                    balanceItemTotals.account = balanceItemN.account;
                    balanceItemTotals.aid     = balanceItemN.aid;
                    balanceItemTotals.key             = balanceItemN.key;
                    balanceItemTotals.parentKey       = balanceItemN.parentKey;
                    
                    balanceItemTotals.level   = balanceItemN.level;
                    balanceItemTotals.name    = balanceItemN.name;
                    balanceItemTotals.lastActivity = balanceItemN.lastActivity;
                    
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
    public static ArrayList<ssoUIBalanceItem> generateLEVEL1_Summary(BigInteger pUserId, ArrayList<ssoUIBalanceItem> pInvSheetLevel1RawData)
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
                if (sBrandNItemCodeN.equals(sKey2Search)==true) //BRAND ID + ITEMCODE
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

                    balanceItemTotals.priceId = balanceItemN.priceId;
                    balanceItemTotals.priceEntry = balanceItemN.priceEntry;
                    balanceItemTotals.priceSale  = balanceItemN.priceSale;
                    balanceItemTotals.discount   = balanceItemN.discount;

                    balanceItemTotals.dets = balanceItemN.dets;

                    // QUANTITY
                    balanceItemTotals.quantity.received = balanceItemTotals.quantity.received.add(balanceItemN.quantity.received);
                    balanceItemTotals.quantity.returned = balanceItemTotals.quantity.returned.add(balanceItemN.quantity.returned);
                    balanceItemTotals.quantity.sold     = balanceItemTotals.quantity.sold.add(balanceItemN.quantity.sold);
                    balanceItemTotals.quantity.refund   = balanceItemTotals.quantity.refund.add(balanceItemN.quantity.refund);
                    balanceItemTotals.quantity.netSold  = balanceItemTotals.quantity.sold.subtract(balanceItemTotals.quantity.refund);

                    balanceItemTotals.quantity.adjMinus = balanceItemTotals.quantity.adjMinus.add(balanceItemN.quantity.adjMinus);
                    balanceItemTotals.quantity.adjPlus  = balanceItemTotals.quantity.adjPlus.add(balanceItemN.quantity.adjPlus);
                    
                    balanceItemTotals.quantity.revolving= balanceItemTotals.quantity.revolving.add(balanceItemN.quantity.revolving);
                    balanceItemTotals.quantity.net      = balanceItemTotals.quantity.received.
                                                          subtract(balanceItemTotals.quantity.returned).
                                                          subtract(balanceItemTotals.quantity.sold).
                                                          add(balanceItemTotals.quantity.refund).
                                                          subtract(balanceItemTotals.quantity.adjMinus).
                                                          add(balanceItemTotals.quantity.adjPlus).
                                                          add(balanceItemTotals.quantity.revolving);
                    
                    // BALANCE
                    balanceItemTotals.balance.received = balanceItemTotals.balance.received.add(balanceItemN.balance.received);
                    balanceItemTotals.balance.returned = balanceItemTotals.balance.returned.add(balanceItemN.balance.returned);
                    balanceItemTotals.balance.sold     = balanceItemTotals.balance.sold.add(balanceItemN.balance.sold);
                    balanceItemTotals.balance.revolving= balanceItemTotals.balance.revolving.add(balanceItemN.balance.revolving);
                    
                    balanceItemTotals.balance.net      = balanceItemTotals.balance.received.subtract(balanceItemTotals.balance.returned).subtract(balanceItemTotals.balance.sold).add(balanceItemTotals.balance.revolving);
                    
                    // REVOLVING = FOR BACKUP OFFLINE
                    
                }

            }// end of roll thru raw sum

            balanceItemTotals.key     = generateRowKey(pUserId.toString() , 
                                                       balanceItemTotals.dets.brandId, 
                                                       balanceItemTotals.dets.itemCode, 
                                                       "U");//U for User

            balanceSheetLevel1.add(balanceItemTotals);

        }// end of thru unique brand + itemcodes 

        return balanceSheetLevel1;
    }

    public static ArrayList<ssoUIBalanceItem> generateLEVEL1_RawData(EntityManager          pem, 
                                                                     boolean                pbCleanMemory,
                                                                     BigInteger             pUserId,
                                                                     ArrayList<ssoMerchant> pBranches,
                                                                     BigInteger             pBrandId,
                                                                     String                 pBrandName,
                                                                     int                    pStmtYear) throws Exception
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevelRaw = new ArrayList<ssoUIBalanceItem>();

        for (ssoMerchant accN:pBranches)//26.12.2021
        {
            if(pbCleanMemory==true)
                pem.flush();

            ArrayList<ssoAccInvBalanceCore> itemBalances = new ArrayList<ssoAccInvBalanceCore>();
            itemBalances = AccountMisc.calculateAccountBalance4Brand(   pem, 
                                                                        accN.Id, 
                                                                        accN.name,
                                                                        pBrandId,
                                                                        pStmtYear);
            //Adding to Balance Sheet
            for(ssoAccInvBalanceCore ItemN:itemBalances)
            {
                ssoUIBalanceItem newItem = new ssoUIBalanceItem();

                newItem.level = 1;
                newItem.dets.itemCode   = ItemN.ItemCode;
                newItem.dets.itemCodeId = ItemN.ItemCodeId;

                newItem.account        = accN.name;
                newItem.aid            = accN.Id;
                
                newItem.dets.brandId   = ItemN.BrandId.toString();
                newItem.dets.brandName = Util.Str.wordNormalize(ItemN.Brandname);
                newItem.name = ItemN.Brandname + " / " + ItemN.ItemCode;

                // PARENT KEY WILL POINT TO SUMMARY LEVEL AND THIS RAW DATA WILL BE ADDED AS BRANCH DETS
                newItem.key       = generateRowKey(accN.Id.toString() , pBrandId.toString(), ItemN.ItemCode, "A"); //A for Account
                newItem.parentKey = generateRowKey(pUserId.toString() , pBrandId.toString(), ItemN.ItemCode, "U"); //U for User

                newItem.successRate     = ItemN.successRate;
                newItem.priceId         = ItemN.priceId;
                newItem.priceEntry      = ItemN.priceEntry;
                newItem.priceSale       = ItemN.priceSale;
                newItem.discount        = ItemN.discount;

                newItem.velocityStartup = ItemN.velocityStartup;
                newItem.velocityOverall = ItemN.velocityOverall;

                // INV - TOTAL NET
                //--------------------------
                newItem.quantity.received = ItemN.stats.inv.quantity.received;
                newItem.quantity.returned = ItemN.stats.inv.quantity.sent;
                newItem.quantity.sold     = ItemN.stats.inv.quantity.sold;
                newItem.quantity.refund   = ItemN.stats.inv.quantity.refund;
                newItem.quantity.adjPlus  = ItemN.stats.inv.quantity.adjPlus;
                newItem.quantity.adjMinus = ItemN.stats.inv.quantity.adjMinus;
                newItem.quantity.net      = ItemN.stats.inv.quantity.balance;
                //newItem.quantity.revolving= ItemN.stats.revolving.quantity.balance;
                //newItem.quantity.balanceManualOverwrite= ItemN.stats.revolving.quantity.balanceManualOverwrite;
                
                // YTD = CURRENT + EOD
                //--------------------------
                /*
                newItem.quantity.received = ItemN.stats.ytd.quantity.received;
                newItem.quantity.returned = ItemN.stats.ytd.quantity.sent;
                newItem.quantity.sold     = ItemN.stats.ytd.quantity.sold;
                newItem.quantity.refund   = ItemN.stats.ytd.quantity.refund;
                newItem.quantity.adjPlus  = ItemN.stats.ytd.quantity.adjPlus;
                newItem.quantity.adjMinus = ItemN.stats.ytd.quantity.adjMinus;
                newItem.quantity.net      = ItemN.stats.ytd.quantity.balance;
                newItem.quantity.revolving= ItemN.stats.revolving.quantity.balance;
                newItem.quantity.balanceManualOverwrite= ItemN.stats.revolving.quantity.balanceManualOverwrite;
                */

                // Revolving - Quantity 
                newItem.revolving.quantity.received = ItemN.stats.revolving.quantity.received;
                newItem.revolving.quantity.sent     = ItemN.stats.revolving.quantity.sent;
                newItem.revolving.quantity.sold     = ItemN.stats.revolving.quantity.sold;
                newItem.revolving.quantity.refund   = ItemN.stats.revolving.quantity.refund;
                newItem.revolving.quantity.adjPlus  = ItemN.stats.revolving.quantity.adjPlus;
                newItem.revolving.quantity.adjMinus = ItemN.stats.revolving.quantity.adjMinus;
                newItem.revolving.quantity.balance  = ItemN.stats.revolving.quantity.balance;
                newItem.revolving.quantity.balanceManualOverwrite  = ItemN.stats.revolving.quantity.balanceManualOverwrite;
                
                // Revolving - Base / List Price
                newItem.revolving.base.received = ItemN.stats.revolving.base.received;
                newItem.revolving.base.sent     = ItemN.stats.revolving.base.sent;
                newItem.revolving.base.sold     = ItemN.stats.revolving.base.sold;
                newItem.revolving.base.refund   = ItemN.stats.revolving.base.refund;
                newItem.revolving.base.adjPlus  = ItemN.stats.revolving.base.adjPlus;
                newItem.revolving.base.adjMinus = ItemN.stats.revolving.base.adjMinus;

                // Revolving - Discount
                newItem.revolving.discount.received = ItemN.stats.revolving.discount.received;
                newItem.revolving.discount.sent     = ItemN.stats.revolving.discount.sent;
                newItem.revolving.discount.sold     = ItemN.stats.revolving.discount.sold;
                newItem.revolving.discount.refund   = ItemN.stats.revolving.discount.refund;
                newItem.revolving.discount.adjPlus  = ItemN.stats.revolving.discount.adjPlus;
                newItem.revolving.discount.adjMinus = ItemN.stats.revolving.discount.adjMinus;

                // Revolving - Surcharge
                newItem.revolving.surcharge.received = ItemN.stats.revolving.surcharge.received;
                newItem.revolving.surcharge.sent     = ItemN.stats.revolving.surcharge.sent;
                newItem.revolving.surcharge.sold     = ItemN.stats.revolving.surcharge.sold;
                newItem.revolving.surcharge.refund   = ItemN.stats.revolving.surcharge.refund;
                newItem.revolving.surcharge.adjPlus  = ItemN.stats.revolving.surcharge.adjPlus;
                newItem.revolving.surcharge.adjMinus = ItemN.stats.revolving.surcharge.adjMinus;

                // Revolving - Tax
                newItem.revolving.tax.received = ItemN.stats.revolving.tax.received;
                newItem.revolving.tax.sent     = ItemN.stats.revolving.tax.sent;
                newItem.revolving.tax.sold     = ItemN.stats.revolving.tax.sold;
                newItem.revolving.tax.refund   = ItemN.stats.revolving.tax.refund;
                newItem.revolving.tax.adjPlus  = ItemN.stats.revolving.tax.adjPlus;
                newItem.revolving.tax.adjMinus = ItemN.stats.revolving.tax.adjMinus;

                // Revolving - Expense
                newItem.revolving.expense.received = ItemN.stats.revolving.expense.received;
                newItem.revolving.expense.sent     = ItemN.stats.revolving.expense.sent;
                newItem.revolving.expense.sold     = ItemN.stats.revolving.expense.sold;
                newItem.revolving.expense.refund   = ItemN.stats.revolving.expense.refund;
                newItem.revolving.expense.adjPlus  = ItemN.stats.revolving.expense.adjPlus;
                newItem.revolving.expense.adjMinus = ItemN.stats.revolving.expense.adjMinus;

                // Revolving - Gross
                newItem.revolving.gross.received = ItemN.stats.revolving.gross.received;
                newItem.revolving.gross.sent     = ItemN.stats.revolving.gross.sent;
                newItem.revolving.gross.sold     = ItemN.stats.revolving.gross.sold;
                newItem.revolving.gross.refund   = ItemN.stats.revolving.gross.refund;
                newItem.revolving.gross.adjPlus  = ItemN.stats.revolving.gross.adjPlus;
                newItem.revolving.gross.adjMinus = ItemN.stats.revolving.gross.adjMinus;
                
                //newItem.balance  = ItemN.balance;
                newItem.lastActivity = ItemN.lastActivity;

                balanceSheetLevelRaw.add(newItem);//add balance for each item
            }

        }

        return balanceSheetLevelRaw;
    }

    // IMPORTANT: 
    // Here you can either get it for USER or Specified ACCOUNT. NOT Multiple account is supported
    public static ArrayList<ssoUIBalanceItem> getInventory4ItemsOptions(    EntityManager               pem, 
                                                                            BigInteger                  pUserId,
                                                                            BigInteger                  pAccountId,
                                                                            String                      pTargetType,//either account level or user level calculation
                                                                            BigInteger                  pVendorId, //pBrandId,
                                                                            String                      pItemCode,
                                                                            boolean                     pbSpecificItemCode,
                                                                            boolean                     pbCleanMemory,
                                                                            int                         pPageNumber) throws Exception
    {
        String sLastRunText = "";
        
        ArrayList<ssoUIBalanceItem> brandItemOptionBalances = new ArrayList<ssoUIBalanceItem>();

        try
        {
            ArrayList<ssoReportItemsOptionsFolder> aITEMSOPTIONSDATA = new ArrayList<ssoReportItemsOptionsFolder>();
            aITEMSOPTIONSDATA = collectData4ItemOptions(pem, 
                                                        pUserId, 
                                                        pAccountId, 
                                                        pTargetType, 
                                                        pVendorId, 
                                                        pItemCode, 
                                                        pbSpecificItemCode, 
                                                        pbCleanMemory, 
                                                        pPageNumber);

            ArrayList<String> aOptGroups = new ArrayList<String>();
            ArrayList<String> aOptDistinctGroups = new ArrayList<String>();
            for(ssoReportItemsOptionsFolder optN: aITEMSOPTIONSDATA)
                aOptGroups.add(optN.name);

            aOptDistinctGroups = Util.Arrays.distinctString(aOptGroups);

            //for(int i=0;i<aItemsOptionsData.size();i++)
            // For instance; Black, Red, ... (OptGroup)
            for(String itemCodeNoptN: aOptDistinctGroups)//ItemCodeNOptGroup
            {

                // Collect Options for OptN (from each account)
                //--------------------------------------------------------------
                ArrayList<ssoReportItemsOptionsFolder> aOPTGROUPNQUANTITIES = new ArrayList<ssoReportItemsOptionsFolder>();
                for(ssoReportItemsOptionsFolder groupQuantityN: aITEMSOPTIONSDATA)
                {
                    if(groupQuantityN.name.equals(itemCodeNoptN)==true)
                    {
                        aOPTGROUPNQUANTITIES.add(groupQuantityN);
                    }
                }

                // Sort quantities of OptN for each Account
                //
                // (Quantity1 = {"M": 1, "L":2,...} Quantity2 ... 
                // (each belongs to different account / branch 
                // For instance; 
                // Acc1: Black - {"M": 1, "L":2,...} 
                // Acc2: Black - {"S": 4, "M": 3, "L":2,...} 
                //--------------------------------------------------------------
                ArrayList<ssoReportQuantityCore> accQuantities = new ArrayList<ssoReportQuantityCore>();
                ArrayList<ssoReportQuantityCore> allOptSumQuantities = new ArrayList<ssoReportQuantityCore>();
                for(ssoReportItemsOptionsFolder accQUANTITYN:aOPTGROUPNQUANTITIES)
                {
                    sLastRunText = "itemcode: " + accQUANTITYN.itemCode + "-" + accQUANTITYN.optGroup + "- all > " + accQUANTITYN.optAll + " = entered > " + accQUANTITYN.optEntered;

                    String sOptionUID            = accQUANTITYN.optUID;
                    String sOptionsAll           = accQUANTITYN.optAll;
                    JsonObject jsOptionsEntered  = Util.JSON.toJsonObject(accQUANTITYN.optEntered);
                    JsonObject jsOptionsReturned = Util.JSON.toJsonObject(accQUANTITYN.optReturned);
                    JsonObject jsOptionsSold     = Util.JSON.toJsonObject(accQUANTITYN.optSold);
                    JsonObject jsOptionsAdjPlus  = Util.JSON.toJsonObject(accQUANTITYN.optAdjPlus);
                    JsonObject jsOptionsAdjMinus = Util.JSON.toJsonObject(accQUANTITYN.optAdjMinus);
                    JsonObject jsOptionsNetAll   = Util.JSON.toJsonObject(accQUANTITYN.optAll);

                    // combine each json
                    // Run in a loop for json for each option add one item 
                    // KEYS = Options (M, L, XL...)
                    JsonObject jsOptAll = Util.JSON.toJsonObject(sOptionsAll);
                    Set<String> hKeys = Util.JSON.keys(jsOptAll);
                    ArrayList<String> aKeys = new ArrayList<>(hKeys);
                    if(aKeys.size()==0)
                        aKeys.add("");

                    // Adding Options
                    for(String optKeyN:aKeys)
                    {
                        ssoReportQuantityCore qCoreN = new ssoReportQuantityCore();

                        String sQuantityEntered  = Util.JSON.getValue(jsOptionsEntered,  optKeyN);
                        String sQuantityReturned = Util.JSON.getValue(jsOptionsReturned, optKeyN);
                        String sQuantitySold     = Util.JSON.getValue(jsOptionsSold,     optKeyN);
                        String sQuantityAdjPlus  = Util.JSON.getValue(jsOptionsAdjPlus,  optKeyN);
                        String sQuantityAdjMinus = Util.JSON.getValue(jsOptionsAdjMinus, optKeyN);
                        String sQuantityNetAll   = Util.JSON.getValue(jsOptionsNetAll, optKeyN);

                        boolean bQuantityEnteredSkip  = false;
                        boolean bQuantityReturnedSkip = false;
                        boolean bQuantitySoldSkip     = false;
                        boolean bQuantityAdjPlusSkip  = false;
                        boolean bQuantityAdjMinusSkip = false;

                        if (sQuantityEntered.trim().length()==0)
                        {
                            sQuantityEntered = "0";
                            bQuantityEnteredSkip = true;
                        }
                        else if(new BigDecimal(sQuantityEntered).compareTo(BigDecimal.ZERO)<0)
                        {
                            sQuantityEntered = "0";
                            bQuantityEnteredSkip = true;
                        }

                        if (sQuantityReturned.trim().length()==0) 
                        {
                            sQuantityReturned = "0";
                            bQuantityReturnedSkip = true;
                        }
                        else if(new BigDecimal(sQuantityReturned).compareTo(BigDecimal.ZERO)<0)
                        {
                            sQuantityReturned = "0";
                            bQuantityReturnedSkip = true;
                        }


                        if (sQuantitySold.trim().length()==0)
                        {
                            sQuantitySold = "0";
                            bQuantitySoldSkip = true;
                        }
                        else if(new BigDecimal(sQuantitySold).compareTo(BigDecimal.ZERO)<0)
                        {
                            sQuantitySold = "0";
                            bQuantitySoldSkip = true;
                        }


                        if (sQuantityAdjPlus.trim().length()==0)
                        {
                            sQuantityAdjPlus = "0";
                            bQuantityAdjPlusSkip = true;
                        }
                        else if(new BigDecimal(sQuantityAdjPlus).compareTo(BigDecimal.ZERO)<0)
                        {
                            sQuantityAdjPlus = "0";
                            bQuantityAdjPlusSkip = true;
                        }


                        if (sQuantityAdjMinus.trim().length()==0)
                        {
                            sQuantityAdjMinus = "0";
                            bQuantityAdjMinusSkip = true;
                        }
                        else if(new BigDecimal(sQuantityAdjMinus).compareTo(BigDecimal.ZERO)<0)
                        {
                            sQuantityAdjMinus = "0";
                            bQuantityAdjMinusSkip = true;
                        }
                        else if(new BigDecimal(sQuantityNetAll).compareTo(BigDecimal.ZERO)<0)
                        {
                            sQuantityNetAll = "0";
                        }
                        
                        qCoreN.uid  = sOptionUID;
                        qCoreN.name = optKeyN;

                        if(bQuantityEnteredSkip==false)
                            qCoreN.received = new BigDecimal(sQuantityEntered);

                        if(bQuantityReturnedSkip==false)
                            qCoreN.sent     = new BigDecimal(sQuantityReturned);

                        if(bQuantitySoldSkip==false)
                            qCoreN.sold     = new BigDecimal(sQuantitySold);

                        if(bQuantityAdjPlusSkip==false)
                            qCoreN.adjPlus  = new BigDecimal(sQuantityAdjPlus);

                        if(bQuantityAdjMinusSkip==false)
                            qCoreN.adjMinus = new BigDecimal(sQuantityAdjMinus);

                        qCoreN.netInventory = new BigDecimal(sQuantityNetAll);

                        accQuantities.add(qCoreN);
                    }

                    ArrayList<String> aSortedOptKeys = new ArrayList<String>();
                    ArrayList<String> aSortedDistinctOptKeys = new ArrayList<String>();
                    for(ssoReportQuantityCore optQuantityN: accQuantities)
                    {
                        aSortedOptKeys.add(optQuantityN.name);
                    }

                    aSortedDistinctOptKeys = Util.Arrays.distinctString(aSortedOptKeys);

                    // Get Sum Quantities for Each Option Key (sum of accounts)
                    //----------------------------------------------------------
                    for(String optKeyN: aSortedDistinctOptKeys)
                    {
                        ssoReportQuantityCore optNQuantityTotals = new ssoReportQuantityCore();
                        
                        for(ssoReportQuantityCore quantityN: accQuantities)
                        {
                            if(optKeyN.equals(quantityN.name)==true)
                            {
                                optNQuantityTotals.uid  = quantityN.uid;
                                optNQuantityTotals.name = quantityN.name;
                                optNQuantityTotals.received = optNQuantityTotals.received.add(quantityN.received);
                                optNQuantityTotals.sent     = optNQuantityTotals.sent.add(quantityN.sent);
                                optNQuantityTotals.sold     = optNQuantityTotals.sold.add(quantityN.sold);
                                optNQuantityTotals.refund   = optNQuantityTotals.refund.add(quantityN.refund);
                                optNQuantityTotals.adjPlus  = optNQuantityTotals.adjPlus.add(quantityN.adjPlus);
                                optNQuantityTotals.adjMinus = optNQuantityTotals.adjMinus.add(quantityN.adjMinus);
                                optNQuantityTotals.netInventory = optNQuantityTotals.netInventory.add(quantityN.netInventory);
                            }
                        }
                        
                        allOptSumQuantities.add(optNQuantityTotals);
                    }

                }//end of each acc

                // Create Option ROW
                //----------------------------------------------------------
                String [] aItemCodeNOptGroup = itemCodeNoptN.split("\\|");
                String sItemCode = aItemCodeNOptGroup[0].trim();
                String sOptGroup = aItemCodeNOptGroup[1].trim();

                for(ssoReportQuantityCore optNsumQuantity: allOptSumQuantities)
                {

                    ssoUIBalanceItem newUIRow = new ssoUIBalanceItem();

                    //newUIRow.key               = pUserId + "-" + pAccountId + "-" + pVendorId + "-" + sItemCode + "-" + optNsumQuantity.name + "-" + sOptGroup;
                    newUIRow.key               = pUserId + "-" + pAccountId + "-" + pVendorId + "-" + sItemCode + "-" + optNsumQuantity.uid + "-" + sOptGroup + "-" + optNsumQuantity.name;
                    newUIRow.parentKey         = pUserId + "-" + pAccountId + "-" + pVendorId + "-" + sItemCode;// + "-" + optNsumQuantity.name;

                    newUIRow.name              = sOptGroup + " - " + optNsumQuantity.name;
                    newUIRow.quantity.received = optNsumQuantity.received;
                    newUIRow.quantity.returned = optNsumQuantity.sent;
                    newUIRow.quantity.sold     = optNsumQuantity.sold;
                    newUIRow.quantity.refund   = optNsumQuantity.refund;
                    newUIRow.quantity.adjPlus  = optNsumQuantity.adjPlus;
                    newUIRow.quantity.adjMinus = optNsumQuantity.adjMinus;
                    newUIRow.quantity.net      = optNsumQuantity.netInventory;
                    brandItemOptionBalances.add(newUIRow);
                }

            }

            return brandItemOptionBalances;
        }
        catch(Exception e)
        {
            throw new Exception(sLastRunText + " => " + e.getMessage() );
        }

    }

    public static ArrayList<ssoReportItemsOptionsFolder> collectData4ItemOptions(   EntityManager               pem, 
                                                                                    BigInteger                  pUserId,
                                                                                    BigInteger                  pAccountId,
                                                                                    String                      pTargetType,//either account level or user level calculation
                                                                                    BigInteger                  pVendorId, //pBrandId,
                                                                                    String                      pItemCode,
                                                                                    boolean                     pbSpecificItemCode,
                                                                                    boolean                     pbCleanMemory,
                                                                                    int                         pPageNumber) throws Exception
    {
        ArrayList<ssoReportItemsOptionsFolder> itemsOptions = new ArrayList<ssoReportItemsOptionsFolder>();

        try
        {
            if(pbCleanMemory==true)
                pem.flush();

            int iRowPerPage = 10;//100;//DEFAULT
            int iPageIndex  = pPageNumber;
            //int iOffset     = iPageIndex *  iRowPerPage;//skip first N records
            //int iLimit      = (iPageIndex+1) * iRowPerPage;//50 per page

            ssoDBRowLimits rowLimits = new ssoDBRowLimits();
            rowLimits = Util.Database.calculateRowLimits(iRowPerPage, pPageNumber);
            int iOffset     = rowLimits.offset;
            int iLimit      = rowLimits.limit;

            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            // This query added under SsAccInvVendorStats as it is on cache
            // If something added new this table will also be affected and cache will be reset
            // In other words this methods works on cahce this way
            String sQuery = "SELECT " +
                            "	T.ACC_ID," +
                            "	T.BRAND," +
                            "   T.ITEM_CODE," +
                            "   T.OPT_UID," +
                            "   T.OPT_GROUP," +
                            "	T.QNT_ENTERED," +
                            "   T.QNT_RETURNED," +
                            "   T.QNT_CR_SOLD," +
                            "   T.QNT_ADJ_PLUS, " +
                            "   T.QNT_ADJ_MINUS, " +
                            //"   INV_JSON_MERGE(INV_JSON_MERGE(INV_JSON_MERGE(INV_JSON_MERGE(T.QNT_ENTERED, T.QNT_RETURNED, '-'), T.QNT_CR_SOLD, '-'), T.QNT_ADJ_PLUS, '+'), T.QNT_ADJ_MINUS, '-') AS QNT_ALL," +
                            "   T.QNT_ALL, " +
                            "   T.LASTUPDATE " +
                            "FROM " + 
                            "(" + 
                            "	SELECT " +
                            "       BRND.BRAND, " +
                            "       STT.ACCOUNT_ID AS ACC_ID," +
                            "	    ITM.ITEM_CODE, " +
                            "       STT.UID AS OPT_UID, " +
                            "	    TRIM(STT.OPTION_GROUP) AS OPT_GROUP, " +
                            "       INV_JSON_MERGE(STT.LAST_EOD_OPTIONS_ENTERED, INV_JSON_MERGE(STT.OPTIONS_ENTERED, '{}', '+'), '+') AS QNT_ENTERED, " +
                            "	    INV_JSON_MERGE(STT.LAST_EOD_OPTIONS_RETURNED, INV_JSON_MERGE(STT.OPTIONS_RETURNED, '{}', '+'),'+') AS QNT_RETURNED, " +
                            "	    INV_JSON_MERGE(STT.LAST_EOD_OPTIONS_CR_SOLD, INV_JSON_MERGE(STT.OPTIONS_CR_SOLD, '{}', '+'),'+') AS QNT_CR_SOLD, " +
                            "       INV_JSON_MERGE(STT.LAST_EOD_OPTIONS_ADJ_PLUS, INV_JSON_MERGE(STT.OPTIONS_ADJ_PLUS, '{}', '+'),'+') AS QNT_ADJ_PLUS, " +
                            "       INV_JSON_MERGE(STT.LAST_EOD_OPTIONS_ADJ_MINUS, INV_JSON_MERGE(STT.OPTIONS_ADJ_MINUS, '{}', '+'),'+') AS QNT_ADJ_MINUS," +
                            "       FN_INV_CALC_NET_QUANTITY_4_OPTION_N_CORE(IFNULL(STT.OPTIONS_ENTERED,'{}'), IFNULL(STT.OPTIONS_RETURNED,'{}'), IFNULL(STT.OPTIONS_CR_SOLD,'{}'), IFNULL(STT.OPTIONS_CR_REFUND,'{}'), IFNULL(STT.OPTIONS_DN_SOLD,'{}'), IFNULL(STT.OPTIONS_DN_REFUND,'{}'), IFNULL(STT.OPTIONS_ADJ_PLUS,'{}'), IFNULL(STT.OPTIONS_ADJ_MINUS,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_ENTERED,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_RETURNED,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_CR_SOLD,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_CR_REFUND,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_DN_SOLD,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_DN_REFUND,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_ADJ_PLUS,'{}'), IFNULL(STT.LAST_EOD_OPTIONS_ADJ_MINUS,'{}')) AS QNT_ALL," + 
                            "	    STT.LASTUPDATE " +
                            "	FROM ss_acc_inv_option_stats STT " +
                            "	INNER JOIN ss_acc_inv_vendors BRND ON STT.VENDOR_ID = BRND.UID " +
                            "	INNER JOIN ss_acc_inv_item_stats ITM ON ITM.UID = STT.ITEM_CODE_ID " +
                            "	WHERE " +
                            "	STT.STAT = 1 " +
                            "	AND " +
                            "	ITM.STAT = 1 " +
                            "	AND " +
                            "	STT.VENDOR_ID = ? ";

            if(pTargetType.equals("U")==false)//if not user
            {
                sQuery +=   "	AND " +
                            "	STT.ACCOUNT_ID = ?";
            }
            
            if(pItemCode.trim().length()>0)
            {
                sQuery +=   "	AND " +
                            "	ITM.ITEM_CODE = ?";            
            }

            sQuery +=       ") T " +
                            "GROUP BY T.ACC_ID, T.ITEM_CODE, T.OPT_GROUP  " +
                            "ORDER BY CONCAT(ITEM_CODE, '-', OPT_GROUP) ASC " +
                            "LIMIT ? OFFSET ? ";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            
            stmt.SetParameter(index++, pVendorId           , "VENDOR_ID");

            if(pTargetType.equals("U")==false)//if not user
                stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            
            if(pItemCode.trim().length()>0)
                stmt.SetParameter(index++, pItemCode          , "ITEM_CODE");

            stmt.SetParameter(index++, iLimit              , "LIMIT");
            stmt.SetParameter(index++, iOffset             , "OFFSET");

            List<List<RowColumn>> rs = stmt.getResultList();
            if (rs.size()>0)
            {
                for (int i=0;i<rs.size();i++)
                {
                    ssoReportItemsOptionsFolder itemOptsN = new ssoReportItemsOptionsFolder();

                    String sBrand        = "";//Util.Database.getValString(rs.get(i), "BRAND");
                    itemOptsN.optUID       = Util.Database.getValString(rs.get(i), "OPT_UID");
                    itemOptsN.itemCode     = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                    itemOptsN.optGroup     = Util.Database.getValString(rs.get(i), "OPT_GROUP");//Option Group
                    itemOptsN.name         = itemOptsN.itemCode.trim() + " | " + itemOptsN.optGroup.trim();

                    itemOptsN.lastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");

                    itemOptsN.optAll      = Util.Database.getValString(rs.get(i), "QNT_ALL");// DON'T NORMALIZE
                    itemOptsN.optEntered  = Util.Database.getValString(rs.get(i), "QNT_ENTERED");
                    itemOptsN.optReturned = Util.Database.getValString(rs.get(i), "QNT_RETURNED");
                    itemOptsN.optSold     = Util.Database.getValString(rs.get(i), "QNT_CR_SOLD");
                    //itemOptsN.optRefund   = Util.Database.getValString(rs.get(i), "QNT_ADJ_PLUS");
                    itemOptsN.optAdjPlus  = Util.Database.getValString(rs.get(i), "QNT_ADJ_PLUS");
                    itemOptsN.optAdjMinus = Util.Database.getValString(rs.get(i), "QNT_ADJ_MINUS");
                    
                    itemsOptions.add(itemOptsN);
                    /*
                    JsonObject jsOptionsEntered  = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_ENTERED")));// THIS IS JSON
                    JsonObject jsOptionsReturned = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_RETURNED")));// THIS IS JSON
                    JsonObject jsOptionsSold     = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_CR_SOLD")));// THIS IS JSON
                    JsonObject jsOptionsAdjPlus  = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_ADJ_PLUS")));// THIS IS JSON
                    JsonObject jsOptionsAdjMinus = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_ADJ_MINUS")));// THIS IS JSON
                    
                    // combine each json
                    // Run in a loop for json for each option add one item 
                    JsonObject jsOptAll = Util.JSON.toJsonObject(sOptionsAll);
                    Set<String> hKeys = Util.JSON.keys(jsOptAll);
                    ArrayList<String> aKeys = new ArrayList<>(hKeys);
                    if(aKeys.size()==0)
                        aKeys.add("");

                    //brandItemOptionBalances.add(newParentBalance);
                    */
                }
            }

            return itemsOptions;
            
        }
        catch(Exception e)
        {
            throw e;
        }

    }//collectData4ItemOptions

    // IMPORTANT: 
    // Here you can either get it for USER or Specified ACCOUNT. NOT Multiple account is supported
    public static ArrayList<ssoUIBalanceItem> getInventory4Items(    EntityManager               pem, 
                                                                    BigInteger                  pUserId,
                                                                    BigInteger                  pAccountId,
                                                                    String                      pTargetType,//either account level or user level calculation
                                                                    BigInteger                  pVendorId, //pBrandId,
                                                                    String                      pItemCode,
                                                                    boolean                     pbSpecificItemCode,
                                                                    boolean                     pbCleanMemory,
                                                                    int                         pPageNumber) throws Exception
    {
        ArrayList<ssoUIBalanceItem> brandItemOptionBalances = new ArrayList<ssoUIBalanceItem>();

        try
        {
            if(pbCleanMemory==true)
                pem.flush();

            int iRowPerPage = 10;//100;//DEFAULT
            int iPageIndex  = pPageNumber;
            //int iOffset     = iPageIndex *  iRowPerPage;//skip first N records
            //int iLimit      = (iPageIndex+1) * iRowPerPage;//50 per page

            ssoDBRowLimits rowLimits = new ssoDBRowLimits();
            rowLimits = Util.Database.calculateRowLimits(iRowPerPage, pPageNumber);
            int iOffset     = rowLimits.offset;
            int iLimit      = rowLimits.limit;

            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            // This query added under SsAccInvVendorStats as it is on cache
            // If something added new this table will also be affected and cache will be reset
            // In other words this methods works on cahce this way
            String sQuery = "SELECT "
                                //+ "T.USER_ID, "
                                + "T.ACC_ID, "
                                + "T.ITEM_CODE, "
                                + "SUM(T.QNT_ENTERED) AS QNT_ENTERED, "
                                + "SUM(T.QNT_RETURNED) AS QNT_RETURNED, "
                                + "SUM(T.QNT_SOLD) AS QNT_SOLD, "
                                + "SUM(T.QNT_ADJ_PLUS) AS QNT_ADJ_PLUS, "
                                + "SUM(T.QNT_ADJ_MINUS) AS QNT_ADJ_MINUS, "
                                + "T.QNT_NET, "
                                + "T.Q_SUMMARY_SALES, " 
                                + "T.Q_SUMMARY_BILLS, " 
                                + "T.S_SUMMARY_SALES, " 
                                + "T.S_SUMMARY_BILLS, " 
                                + "T.SELL_SPEED_STAR, " 
                                + "T.LASTUPDATE "
                        + "FROM "
                        + "( "
                            + "SELECT "
                                //+ "ACC.USER_ID, "
                                + "STT.ACCOUNT_ID AS ACC_ID, "
                                + "BRND.BRAND, "
                                + "BRND.UID, "
                                + "STT.ITEM_CODE, "
                                //+ "STT.LAST_EOD_QUANTITY_ENTERED + STT.QUANTITY_ENTERED AS QNT_ENTERED, "
                                + "STT.TOTAL_NET_QUANTITY_ENTERED AS QNT_ENTERED, "
                                //+ "STT.LAST_EOD_QUANTITY_RETURNED + STT.QUANTITY_RETURNED AS QNT_RETURNED, "
                                + "STT.TOTAL_NET_QUANTITY_RETURNED AS QNT_RETURNED, "
                                //+ "STT.LAST_EOD_QUANTITY_CR_SOLD + STT.QUANTITY_CR_SOLD - (STT.QUANTITY_CR_REFUND + STT.LAST_EOD_QUANTITY_CR_REFUND) AS QNT_CR_SOLD, "
                                + "STT.TOTAL_NET_QUANTITY_CR_SOLD - STT.TOTAL_NET_QUANTITY_CR_REFUND + STT.TOTAL_NET_QUANTITY_DN_SOLD - STT.TOTAL_NET_QUANTITY_DN_REFUND AS QNT_SOLD, "
                                //+ "(STT.LAST_EOD_QUANTITY_ADJ_PLUS + STT.QUANTITY_ADJ_PLUS) AS QNT_ADJ_PLUS, "
                                + "STT.TOTAL_NET_QUANTITY_ADJ_PLUS AS QNT_ADJ_PLUS, "
                                //+ "(STT.LAST_EOD_QUANTITY_ADJ_MINUS + STT.QUANTITY_ADJ_PLUS) AS QNT_ADJ_MINUS, "
                                + "STT.TOTAL_NET_QUANTITY_ADJ_MINUS AS QNT_ADJ_MINUS, "
                                + "STT.TOTAL_NET_QUANTITY_INV AS QNT_NET, "

                                + "STT.QUARTER_SUMMARY_SALES AS Q_SUMMARY_SALES, " 
                                + "STT.QUARTER_SUMMARY_BILLS AS Q_SUMMARY_BILLS, " 
                                + "STT.SEASON_SUMMARY_SALES AS S_SUMMARY_SALES, " 
                                + "STT.SEASON_SUMMARY_BILLS AS S_SUMMARY_BILLS, " 
                                + "STT.SPD_STAR AS SELL_SPEED_STAR, " 
                                + "STT.LASTUPDATE "
                                + "FROM ss_acc_inv_item_stats STT "
                                //+ "INNER JOIN ss_usr_accounts ACC ON ACC.UID = STT.ACCOUNT_ID "
                                + "INNER JOIN ss_acc_inv_vendors BRND ON STT.VENDOR_ID = BRND.UID "
                                + "WHERE "
                                + "STT.STAT = 1 " 
                                + "AND " 
                                + "STT.VENDOR_ID = ? ";

            if(pTargetType.equals("U")==false)//if not user
            {
                sQuery +=         "AND " + 
                                  "STT.ACCOUNT_ID = ? ";
            }

            if(pbSpecificItemCode==true)
            {
                sQuery +=         "AND " + 
                                  "STT.ITEM_CODE = ? ";
            }

            sQuery +=   ") T "
                        + "GROUP BY T.ITEM_CODE "
                        + "ORDER BY T.SELL_SPEED_STAR DESC, T.ITEM_CODE ASC "
                        + "LIMIT ? OFFSET ?";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            
            stmt.SetParameter(index++, pVendorId           , "VENDOR_ID");

            if(pTargetType.equals("U")==false)//if not user
                stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");

            if(pbSpecificItemCode==true)
                stmt.SetParameter(index++, pItemCode           , "ITEM_CODE");

            stmt.SetParameter(index++, iLimit              , "LIMIT");
            stmt.SetParameter(index++, iOffset             , "OFFSET");

            List<List<RowColumn>> rs = stmt.getResultList();
            if (rs.size()>0)
            {
                for (int i=0;i<rs.size();i++)
                {

                    String sBrand        = "";//Util.Database.getValString(rs.get(i), "BRAND");
                    String sItemCode     = Util.Database.getValString(rs.get(i), "ITEM_CODE");//pItemCode;

                    String sItemQEntered     = Util.Database.getValString(rs.get(i), "QNT_ENTERED");
                    String sItemQReturned    = Util.Database.getValString(rs.get(i), "QNT_RETURNED");
                    String sItemQSold        = Util.Database.getValString(rs.get(i), "QNT_SOLD");//CR + DN
                    String sItemQAdjPlus     = Util.Database.getValString(rs.get(i), "QNT_ADJ_PLUS");
                    String sItemQAdjMinus    = Util.Database.getValString(rs.get(i), "QNT_ADJ_MINUS");
                    String sItemQNet         = Util.Database.getValString(rs.get(i), "QNT_NET");

                    String sQuarterSummarySales    = Util.Database.getValString(rs.get(i), "Q_SUMMARY_SALES");
                    String sQuarterSummaryBills    = Util.Database.getValString(rs.get(i), "Q_SUMMARY_BILLS");
                    String sSeasonsSummarySales    = Util.Database.getValString(rs.get(i), "S_SUMMARY_SALES");
                    String sSeasonSummaryBills     = Util.Database.getValString(rs.get(i), "S_SUMMARY_BILLS");

                    String sLastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");
                    //newBalance.Option    = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT"));
                    //String sOptionsAll      = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPTS_ALL"));// THIS IS JSON

                    // CREATING PARENT ROW (ITEM)
                    //----------------------------------------------------------------
                    ssoUIBalanceItem newParentBalance =  new ssoUIBalanceItem();

                    newParentBalance.speed = Util.Database.getValString(rs.get(i), "SELL_SPEED_STAR");

                    newParentBalance.key          = pUserId + "-" + pAccountId + "-" + pVendorId + "-" + sItemCode;
                    newParentBalance.parentKey    = "";//no parent
                    newParentBalance.aid          = pAccountId;
                    newParentBalance.name         = sItemCode;
                    newParentBalance.lastActivity = sLastActivity;

                    newParentBalance.quantity.received = new BigDecimal(sItemQEntered);
                    newParentBalance.quantity.returned = new BigDecimal(sItemQReturned);
                    newParentBalance.quantity.sold     = new BigDecimal(sItemQSold);
                    newParentBalance.quantity.adjPlus  = new BigDecimal(sItemQAdjPlus);
                    newParentBalance.quantity.adjMinus = new BigDecimal(sItemQAdjMinus);
                    newParentBalance.quantity.net      = new BigDecimal(sItemQNet);

                    newParentBalance.str  = calculateSTR(sSeasonsSummarySales, sSeasonSummaryBills, "TRY", "*", "*").str;

                    //newParentBalance.OptionUID = "";
                    //newParentBalance.Option    = "";

                    brandItemOptionBalances.add(newParentBalance);

                }
            }

            return brandItemOptionBalances;
            
        }
        catch(Exception e)
        {
            throw e;
        }

    }
    
    public static ArrayList<ssoUIBalanceItem> generateRawData(  EntityManager               pem, 
                                                                boolean                     pbCleanMemory,
                                                                ArrayList<ssoMerchant>      pBranches,
                                                                BigInteger                        pBrandId,
                                                                String                      pBrandName,
                                                                String                      pItemCode,
                                                                int                         pPageNumber) throws Exception
    {
        ArrayList<ssoUIBalanceItem>  balanceSheetLevelBranches = new ArrayList<ssoUIBalanceItem>();

        for(ssoMerchant branchN:pBranches)
        {

            // LEVEL 2 (Option Level)
            // PARENT_KEY = bRAND + Itemcode
            //------------------------------------------------------

            //3.2 calculate balance for brand-option(s) of accId
            ArrayList<ssoAccInvBalanceCore> optionBalances = new ArrayList<ssoAccInvBalanceCore>();
            optionBalances = AccountMisc.calculateOptionsBalance4Account( pem, 
                                                                          pbCleanMemory,
                                                                          branchN.Id,
                                                                          branchN.name,//FOR NOW WE USE id instead oad pAccName,
                                                                          pBrandId,//vendor Id
                                                                          pPageNumber
                                                                        );
            
            boolean bParentRow = false;
            for(ssoAccInvBalanceCore optN:optionBalances)
            {
                if ( (pItemCode.trim().toLowerCase().equals(optN.ItemCode.trim().toLowerCase())==true) || 
                     (pItemCode.trim().length()==0) 
                   )
                {
                    ssoUIBalanceItem newItem = new ssoUIBalanceItem();
                    
                    bParentRow = false;
                    if(optN.OptionUID.trim().length()==0)
                        bParentRow = true;

                    newItem.level = 2;
                    newItem.aid  = branchN.Id;
                    newItem.account = branchN.name;
                    
                    if(pItemCode.trim().length()!=0)
                    {
                        newItem.name = optN.Option;
                    }
                    else
                    {
                        newItem.name = optN.ItemCode + " | " + optN.Option;
                    }

                    newItem.dets.brandId    = pBrandId.toString();
                    newItem.dets.itemCode   = optN.ItemCode;
                    if(bParentRow==false)
                        newItem.dets.optionId   = new BigInteger(optN.OptionUID);
                    else
                        newItem.dets.optionId   = BigInteger.ZERO;
                    
                    newItem.dets.option     = optN.Option;
                    newItem.dets.brandName  = Util.Str.wordNormalize(pBrandName);

                    //newItem.key = generateRowKey(newItem.aid, pBrandId, pBrandName, pBrandName)
                    //newItem.parentKey

                    // keys not need to be generated 
                    newItem.key  = generateRowKeyWSign( ROW_KEY_SEPERATOR_SIGN, 
                                                        newItem.aid.toString(),
                                                        newItem.dets.brandId, 
                                                        newItem.dets.itemCode, 
                                                        newItem.dets.optionId.toString(),
                                                        newItem.dets.option,
                                                        "U");//U= User A=Account
                    //newItem.parentKey = generateRowKey(pBrandName, optN.ItemCode, "", "");//Long.toString(accN.id);

                    newItem.quantity.received = optN.stats.ytd.quantity.received;
                    newItem.quantity.returned = optN.stats.ytd.quantity.sent;
                    newItem.quantity.sold     = optN.stats.ytd.quantity.sold;
                    newItem.quantity.refund   = optN.stats.ytd.quantity.refund;
                    newItem.quantity.adjPlus  = optN.stats.ytd.quantity.adjPlus;
                    newItem.quantity.adjMinus = optN.stats.ytd.quantity.adjMinus;
                    newItem.quantity.net      = optN.stats.ytd.quantity.balance;

                    //newItem.balance  = optN.balance;
                    newItem.lastActivity = optN.lastActivity;

                    balanceSheetLevelBranches.add(newItem);//add balance of each item
                }
            }

        }

        return balanceSheetLevelBranches;

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
                                                        itemN.aid.toString(),
                                                        itemN.dets.brandId, 
                                                        itemN.dets.itemCode, 
                                                        itemN.dets.optionId.toString(),
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
