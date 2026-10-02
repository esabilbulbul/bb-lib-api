/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.dekonts.DekontEarningStats;
import bb.app.dekonts.DekontQuantityStats;
import bb.app.dekonts.DekontSummary;
import bb.app.dekonts.DekontSummaryDay;
import bb.app.dekonts.DekontSummaryQuarterDay;
import bb.app.dekonts.DekontSummaryQuarterWeek;
import bb.app.dekonts.DekontSummaryRec;
import bb.app.dekonts.DekontSummaryTots;
import bb.app.dekonts.DekontSummaryUseRates;
import bb.app.dekonts.DekontSummaryWeek;
import bb.app.dekonts.DekontSummaryYear;
import bb.app.dict.DictionaryOps;
import bb.app.obj.ssoBrand;
import bb.app.vendor.VendorOps;
import bb.app.bill.ssoBillLineShort;
import bb.app.bill.ssoBillShort;
import bb.app.dekonts.DekontAccDashboard;
import bb.app.dekonts.DekontActiveVendorsSummary;
import bb.app.dekonts.DekontEarning;
import bb.app.dekonts.DekontEarningCore;
import bb.app.dekonts.DekontIndexes;
import bb.app.dekonts.DekontInvSummary;
import bb.app.dekonts.DekontNews;
import bb.app.dekonts.DekontPaymentSummary;
import bb.app.dekonts.DekontQuantityStatCore;
import bb.app.dekonts.DekontSummaryNthDay;
import bb.app.dekonts.DekontSummaryQuarterMonth;
import static bb.app.inv.InventoryOps.getVendorBillQueueName;
import bb.app.obj.ssoMerchantPreferences;
import bb.app.settings.UXParams;
import bb.app.txn.txnDefs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvVendors;
import entity.acc.SsAccInvVendorStats;
import entity.acc.SsAccUsrAccountStats;
import entity.eod.SsEodInvTxnDets;
import entity.mrc.SsMrcCashRegEod;
import entity.stmt.SsStmInvStatements;
import entity.txn.SsTxnInvBill;
import entity.user.SsUsrAccounts;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.redis.ssoRedisLettuce;
import jaxesa.util.Util;
import jaxesa.util.ssoDBRowLimits;
import jaxesa.webapi.ssoAPIResponse;

/**
 *
 * @author esabil
 */
public final class AccountMisc 
{
/*
    public static String INV_TXN_TYPE_NEW_ENTRY = "N";//New Entry
    public static String INV_TXN_TYPE_RETURN    = "R";//REFUND
    public static String INV_TXN_TYPE_CR_SOLD      = "S";//Sales
    public static String INV_TXN_TYPE_FIN_ADJ   = "A";//Financial Adjustment
    
    public static String INV_TXN_EFFECT_CREDIT = "C";//ALACAK
    public static String INV_TXN_EFFECT_DEBIT  = "D";//BORC
    
    public static String gCMMN_TXN_CODE_GROUP_PURCHASE      = "001";//same as in js file UX
    public static String gCMMN_TXN_CODE_GROUP_ITEM          = "002";//same as in js file UX
    public static String gCMMN_TXN_CODE_GROUP_PAYMENT       = "003";//same as in js file UX
  */
    private static final Logger logger = Logger.getLogger(AccountMisc.class.getName());
    
    public static void testBBLIBAPI()
    {
        String s = "";
    }
    //public static SsMr

    //Date format should be 2019-08-23 
    //inDate = DDMMYYYY
    public static String formatDate(String psDate)
    {

        String sFormattedDate = "";
        //index0 = day index1= month index2 = year -> reverse the order

        int index = psDate.indexOf(".");
        if (index>=0)
        {
            String[] aDateParts = psDate.split("\\.");
            
            sFormattedDate = aDateParts[2] + "-" + aDateParts[1] + "-" + aDateParts[0];
        }
        else
        {
            index = psDate.indexOf("/");
            if (index>=0)
            {
                String[] aDateParts = psDate.split("/");
                sFormattedDate = aDateParts[2] + "-" + aDateParts[1] + "-" + aDateParts[0];
                
            }
        }

        return sFormattedDate;
    }

    //example input 18-03-2019
    public static String getMonthNumber(String psDate)
    {

        String sDate = psDate;
        //fields.Time = sDateTime[1];

        String[] sDateParts = sDate.split("\\.");

        //sColMonthNo = sDateParts[1];
        String sMonthNo = sDateParts[1];

        return sMonthNo;
    }

    public static ArrayList<DekontEarningStats> calculateSummary_YearEarningOnly(   EntityManager pem, 
                                                                                    BigInteger pAccountId, //merchant Id
                                                                                    String pMerchantName,
                                                                                    String pBaseCurrency, 
                                                                                    String pTargetCurrency, 
                                                                                    int pBankCode, 
                                                                                    int pYear, 
                                                                                    int pMonth,
                                                                                    int pYearEarningLength) throws Exception
    {
        ArrayList<DekontEarningStats> earnings = new ArrayList<DekontEarningStats>();

        try
        {
            String SCAN_DAY_BEGINNING = "20190101";//DON'T CHANGE this. Works sync with earnings-stats Worker
            int iDayBackLength = 90;//default;

            if (pYearEarningLength==-1)
            {
                long lRefDate = Long.parseLong(Util.DateTime.GetDateTime_s().substring(0, 8));
                iDayBackLength = Util.DateTime.getDifferenceInDays(SCAN_DAY_BEGINNING, Long.toString(lRefDate) );
            }
            else
                iDayBackLength = pYearEarningLength;

            ArrayList<DekontEarningStats> AllEarningStats = new ArrayList<DekontEarningStats>();
            ArrayList<DekontEarningStats> RetailEarningStats = new ArrayList<DekontEarningStats>();

            earnings.addAll(RetailEarningStats);
            earnings.addAll(AllEarningStats);

            return earnings;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static DekontSummary calculateSummary(EntityManager pem, 
                                                 BigInteger pAccountId, //merchant Id
                                                 String pMerchantName,
                                                 String pBaseCurrency, 
                                                 String pTargetCurrency, 
                                                 int pBankCode, 
                                                 int pYear, 
                                                 int pMonth,
                                                 int pYearEarningLength) throws Exception
    {
        DekontSummary summary = new DekontSummary();

        try
        {
            String sAccId = pAccountId.toString();

            Util.Tomcat.print2TomcatLog(logger, "starting", sAccId);

            summary.currency = pTargetCurrency;
            summary.baseYearDate = Util.DateTime.GetDateTime_s().substring(0,4);
            summary.lastYearDate = Integer.toString(Integer.parseInt(summary.baseYearDate) - 1);

            summary.targetMonth = Util.DateTime.GetDateTime_s().substring(4,6);

            // for now for testing here
            //------------------------------------------------------------------
            Util.Tomcat.print2TomcatLog(logger, "getUserAccountStats {}", sAccId);
            SsAccUsrAccountStats usrAccStats = new SsAccUsrAccountStats();
            usrAccStats = getUserAccountStats(pem, pAccountId);
            if(usrAccStats.salesSummaryYears==null)
                usrAccStats.salesSummaryYears = "{}";

            if(usrAccStats.salesSummaryWeeks==null)
                usrAccStats.salesSummaryWeeks = "{}";

            if(usrAccStats.salesSummaryDays==null)
                usrAccStats.salesSummaryDays = "{}";

            if(usrAccStats.salesSummaryUseRates==null)
                usrAccStats.salesSummaryUseRates = "{}";

            if(usrAccStats.salesSummaryRecords==null)
                usrAccStats.salesSummaryRecords = "{}";

            if(usrAccStats.salesSummaryQuarterWeekdayAvgs==null)
                usrAccStats.salesSummaryQuarterWeekdayAvgs = "{}";

            if(usrAccStats.salesSummaryQuarterMonthAvgs==null)
                usrAccStats.salesSummaryQuarterMonthAvgs = "{}";

            if(usrAccStats.salesSummaryMonths==null)
                usrAccStats.salesSummaryMonths = "{}";

            if(usrAccStats.salesSummaryMonthWeeks==null)
                usrAccStats.salesSummaryMonthWeeks = "{}";

            if(usrAccStats.salesSummaryMonthWeekdayAvgs==null)
                usrAccStats.salesSummaryMonthWeekdayAvgs = "{}";

            if(usrAccStats.salesSummaryMonthWeekdayAvgs==null)
                usrAccStats.salesSummaryMonthWeekdayAvgs = "{}";

            Util.Tomcat.print2TomcatLog(logger, "generateIndexes {}", sAccId);
            DekontIndexes indexes = new DekontIndexes();
            indexes = generateIndexes(pem, pAccountId, pMerchantName, pTargetCurrency);

            //DekontNews news = new DekontNews();
            Util.Tomcat.print2TomcatLog(logger, "generateNews {}", sAccId);
            summary.news = generateNews(pMerchantName, indexes);

            // Overall/Avg Summary regardless of the year
            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryRecords {}: " + usrAccStats.salesSummaryRecords, sAccId);
            summary.rows    = calculateSummaryRecords(usrAccStats.salesSummaryRecords, pAccountId, pBaseCurrency, pTargetCurrency, pBankCode, pYear, pMonth);
            //summary.banks   = calculateSummaryBankSubtotals(pem, pAccountId, pBaseCurrency, pTargetCurrency, summary.baseYearDate, -1);

            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryYears {}", sAccId);
            summary.overall = calculateSummaryYears(usrAccStats.salesSummaryYears, pAccountId, pBaseCurrency, pTargetCurrency);//SUMMARY_YEARS ++

            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryMonths {}", sAccId);
            summary.years   = calculateSummaryMonths(usrAccStats.salesSummaryMonths, pAccountId, pBaseCurrency, pTargetCurrency, summary.baseYearDate);//SUMMARY_MONTHS ay bazinda yillik performans ++

            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryWeeks starting", sAccId);
            summary.weeks   = calculateSummaryWeeks(usrAccStats.salesSummaryWeeks, pAccountId, pBaseCurrency, pTargetCurrency, "-1", "-1");//SUMMARY_WEEKS Hafta Bazinda Yillik Performans ++

            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryDays starting", sAccId);
            summary.days   = calculateSummaryDays(usrAccStats.salesSummaryDays, pAccountId, pBaseCurrency, pTargetCurrency, "-1", "-1");//SUMMARY_WEEKS Hafta Bazinda Yillik Performans ++

            //Monthly Performance
            //------------------------------------------------------------------
            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryDaysOfMonth starting", sAccId);
            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryDaysOfMonth > salesSummaryMonthDays: ", usrAccStats.salesSummaryMonthDays);
            summary.currentMonth.days    = calculateSummaryDaysOfMonth(usrAccStats.salesSummaryMonthDays, pAccountId, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);//Gun bazinda Aylik Performans ++
            summary.currentMonth.weeks   = calculateSummaryWeeksOfMonth(usrAccStats.salesSummaryMonthWeeks, pAccountId, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);//Haftalik Aylik Performans ++
            summary.currentMonth.dayAvgs = calculateSummaryTargetMonthWeekDayAverages(usrAccStats.salesSummaryMonthWeekdayAvgs, pAccountId, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);

            //Seasonal Performance (weeks)
            //------------------------------------------------------------------
            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryQuarterMonths starting", sAccId);
            summary.Qmonths           =  calculateSummaryQuarterMonths(usrAccStats.salesSummaryQuarterMonthAvgs, pAccountId, pBaseCurrency, pTargetCurrency,"-1");////Genel/Avg - Week Ciro Ortalamasi
            summary.thisYear.Qmonths  =  calculateSummaryQuarterMonths(usrAccStats.salesSummaryQuarterMonthAvgs, pAccountId, pBaseCurrency, pTargetCurrency,summary.baseYearDate);
            summary.lastYear.Qmonths  =  calculateSummaryQuarterMonths(usrAccStats.salesSummaryQuarterMonthAvgs, pAccountId, pBaseCurrency, pTargetCurrency,summary.lastYearDate);
            //summary.thisYear.Qmonths  =  calculateSummaryYearQuarterMonths(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Week Ciro Ortalamasi
            //summary.lastYear.Qmonths  =  calculateSummaryYearQuarterMonths(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Week Ciro Ortalamasi

            //summary.Qweeks           =  calculateSummaryQuarterWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency,"-1");////Genel/Avg - Week Ciro Ortalamasi
            //summary.thisYear.Qweeks  =  calculateSummaryQuarterWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Week Ciro Ortalamasi
            //summary.lastYear.Qweeks  =  calculateSummaryQuarterWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Week Ciro Ortalamasi

            // Seasonal - Avg + Year (now) + Year (past) summary
            //------------------------------------------------------------------
            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryQuarterWeekDays starting", sAccId);
            summary.Qdays          = calculateSummaryQuarterWeekDays(usrAccStats.salesSummaryQuarterWeekdayAvgs, pAccountId, pBaseCurrency, pTargetCurrency, "-1");//Genel/Avg - Gun Ciro Ortalamasi
            summary.thisYear.Qdays = calculateSummaryQuarterWeekDays(usrAccStats.salesSummaryQuarterWeekdayAvgs, pAccountId, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Gun Ciro Ortalamasi
            summary.lastYear.Qdays = calculateSummaryQuarterWeekDays(usrAccStats.salesSummaryQuarterWeekdayAvgs, pAccountId, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Gun Ciro Ortalamasi

            //summary.thisYear.weeks = calculateSummaryWeeks(pem, summary.baseYearDate, "-1");//ignored
            //summary.lastYear.weeks = calculateSummaryWeeks(pem, summary.lastYearDate, "-1");//ignored

            Util.Tomcat.print2TomcatLog(logger, "calculateSummaryUseRates starting", sAccId);
            summary.useRates12 = calculateSummaryUseRates(usrAccStats.salesSummaryUseRates , pAccountId, pBaseCurrency, pTargetCurrency, 12);//LAST 12 MONTHS
            summary.useRates24 = calculateSummaryUseRates(usrAccStats.salesSummaryUseRates, pAccountId, pBaseCurrency, pTargetCurrency, 24);//LAST 12 MONTHS

            //------------------------------------------------------------------
            //                      MARKET vs Retail
            //------------------------------------------------------------------
            //Earning Stats
            //------------------------------------------------------------------
            ArrayList<DekontEarning> RetailEarningStats = new ArrayList<DekontEarning>();
            ArrayList<DekontEarning> AllEarningStats = new ArrayList<DekontEarning>();

            //QuantityStats
            //------------------------------------------------------------------
            ArrayList<DekontQuantityStats> RetailQuantityStats = new ArrayList<DekontQuantityStats>();
            ArrayList<DekontQuantityStats> AllQuantityStats    = new ArrayList<DekontQuantityStats>();

            //DekontIndexes indexes = new DekontIndexes();
            //indexes = generateIndexes(pem, pAccountId, pMerchantName, pTargetCurrency);

            summary.quantities  = indexes.volumeIndex;
            summary.earnings    = indexes.yearEarningIndex;
            summary.changes     = indexes.yearEarningChanges;
            //summary.quantities.addAll(indexes.volumeIndex);
            //summary.earnings.addAll(indexes.yearEarningIndex);
            //summary.changes.addAll(indexes.yearEarningChanges);

            //summary.quantities.addAll(RetailQuantityStats);
            //summary.quantities.addAll(AllQuantityStats);
            Util.Tomcat.print2TomcatLog(logger, "getDashboardValues starting", sAccId);
            summary.dashboard = getDashboardValues(sAccId,
                                                   pTargetCurrency, 
                                                   pMerchantName, 
                                                   "", 
                                                   "", 
                                                   indexes, 
                                                   summary.news,
                                                   usrAccStats.salesSummaryYears,
                                                   usrAccStats.yearEarnings,
                                                   usrAccStats.volumeIndex,
                                                   usrAccStats.salesSummaryDays);

            Util.Tomcat.print2TomcatLog(logger, "calculateInventorySummary starting", sAccId);
            summary.dashboard.invSummary = calculateInventorySummary(usrAccStats.invSummary);

            Util.Tomcat.print2TomcatLog(logger, "calculatePaymentSummary starting", sAccId);
            summary.dashboard.pymSummary = calculatePaymentSummary(usrAccStats.paymentsSummary, pTargetCurrency);

            Util.Tomcat.print2TomcatLog(logger, "calculateActiveVendorsSummary starting", sAccId);
            summary.dashboard.vndSummary = calculateActiveVendorsSummary(usrAccStats.activeVendorsSummary);

            return summary;
        }
        catch(Exception e)
        {
            //return summary;
            Util.Tomcat.print2TomcatLog(logger, "calculateSummary exception: " + e.getMessage(), pAccountId.toString());
            throw e;
        }
    }

    // pYearEarnings = {"today":{"EUR":XXX,"USD":AA...},"yesterday":{"EUR":abc, "USD":123...}
    public static DekontAccDashboard getDashboardValues(String                          pAccId,  
                                                        String                          pCurrency,
                                                        String                          pMerchantName,
                                                        String                          pCity,
                                                        String                          pAddr,
                                                        DekontIndexes                   pIndexes,
                                                        DekontNews                      pNews,
                                                        String                          pSummaryYears, //{"USD":{"2017":203505.13,"2018":161094.55,
                                                        String                          pYearEarnings,
                                                        String                          pVolumeIndex,
                                                        String                          pFirstNthDays)
    {
        DekontAccDashboard dashBoard = new DekontAccDashboard();

        long lDateYYYYMMDD = Util.DateTime.GetDate();
        String sThisYear = Util.DateTime.getYear().toString();
        String sPrevYear = Util.DateTime.getYear(-1).toString();

        int iNthDay = Util.DateTime.getDayOfYear(Long.toString(lDateYYYYMMDD));
        String sNthDay = Integer.toString(iNthDay - 1);//always refer to 1 days before in case eod failures you wouldn't be affected

        dashBoard.name = pMerchantName;
        dashBoard.city = pCity;
        dashBoard.addr = "";
        dashBoard.logo = "";


        Gson gson = new Gson();
        
            //dashBoard.yeSalesChangePerc = pIndexes.yearEarningChanges.acc.get(pIndexes.yearEarningChanges.acc.size()-1).val;
            //dashBoard.yeSalesChangeVal  = pIndexes.yearEarningChanges.acc.get(pIndexes.yearEarningChanges.acc.size()-1).diff;
        //}

        //YTD SALES
        //----------------------------------------------------------------------
        Util.Tomcat.print2TomcatLog(logger, "getDashboardValues >YTD SALES ", pAccId);
        String sPrevYearRevenue = "";
        if(pSummaryYears.trim().equals("{}")==false)
        {
            if(pSummaryYears.trim().length()>0)
            {

                Map<String,Map<String, String>> mYears = gson.fromJson(pSummaryYears, Map.class);

                String sYear     = Integer.toString(Util.DateTime.getCurrentYear());

                // Current YTD Sales
                if(mYears!=null)
                {
                    if(mYears.get(pCurrency).get(sYear)!=null)
                    {
                        dashBoard.ytdSales = String.valueOf(mYears.get(pCurrency).get(sYear));
                    }
                    else
                        dashBoard.ytdSales = "0";

                    // HERE WILL BE USED FORWARD
                    if(mYears.get(pCurrency).get(sPrevYear)!=null)
                    {
                        sPrevYearRevenue = String.valueOf(mYears.get(pCurrency).get(sPrevYear));
                    }
                    else
                    {
                        sPrevYearRevenue = "0";
                    }
                }
                else
                {
                    sPrevYearRevenue = "0";
                    dashBoard.ytdSales = "0";
                }

                // Get Change / Perc / Value 
                // YTD CHANGES (1ST N DAYS) 
                //--------------------------------------------------------------
                Util.Tomcat.print2TomcatLog(logger, "getDashboardValues >YTD CHANGES " , pAccId);
                Map<String,Map<String, Map<String, String>>> mNthDays = gson.fromJson(pFirstNthDays, Map.class);
                if(mNthDays!=null)
                {
                    Util.Tomcat.print2TomcatLog(logger, "getDashboardValues >YTD CHANGES 1" , pAccId);
                    if(mNthDays.get(pCurrency).get(sThisYear)!=null)
                    {
                        Util.Tomcat.print2TomcatLog(logger, "getDashboardValues >YTD CHANGES 2" , pAccId);
                        if(mNthDays.get(pCurrency).get(sThisYear).get(sNthDay)!=null)
                        {

                            Util.Tomcat.print2TomcatLog(logger, "getDashboardValues >YTD CHANGES 3" , pAccId);
                            if(mNthDays.get(pCurrency).get(sPrevYear)!=null)
                            {
                                if(mNthDays.get(pCurrency).get(sPrevYear).get(sNthDay)!=null)
                                {
                                    Util.Tomcat.print2TomcatLog(logger, "getDashboardValues >YTD CHANGES 4" , pAccId);

                                    String sThisYearNthDayVal = String.valueOf(mNthDays.get(pCurrency).get(sThisYear).get(sNthDay)).replaceAll(",", "");
                                    String sPrevYearNthDayVal = String.valueOf(mNthDays.get(pCurrency).get(sPrevYear).get(sNthDay)).replaceAll(",", "");

                                    BigDecimal bdThisYear = new BigDecimal(sThisYearNthDayVal);
                                    BigDecimal bdPrevYear = new BigDecimal(sPrevYearNthDayVal);

                                    String sChangeVal  = Util.Mathx.calcDiff(sThisYearNthDayVal, sPrevYearNthDayVal).toString();
                                    String sChangePerc = Util.Mathx.calcDiv(sThisYearNthDayVal, sPrevYearNthDayVal, 10).toString();

                                    dashBoard.ytdSalesChangeVal = sChangeVal;
                                    dashBoard.ytdSalesChangePerc= sChangePerc;

                                }
                                else
                                {
                                    dashBoard.ytdSalesChangeVal = "0";
                                    dashBoard.ytdSalesChangePerc= "0";
                                }
                            }
                        }
                    }
                }
            }
        }

        // LAST EOD VALUES
        // YEAR EARNING SALES (COMPARE TO THE LAST YEAR REVENUE)
        //----------------------------------------------------------------------
        Util.Tomcat.print2TomcatLog(logger, "getDashboardValues > YEAR EARNING SALES ", pAccId);
        if(pYearEarnings!=null)
        {
            if(pYearEarnings.trim().equals("{}")==false)
            {
                //Gson gson = new Gson();
                Map<String,Map<String, String>> mYearEarnings = gson.fromJson(pYearEarnings, Map.class);
                if(mYearEarnings!=null)
                {
                    if(mYearEarnings.get("today").get(pCurrency)!=null)
                    {
                        String yeSalesAsOfToday = String.valueOf(mYearEarnings.get("today").get(pCurrency));
                        String yeSalesAsOfYesterday = String.valueOf(mYearEarnings.get("yesterday").get(pCurrency));

                        BigDecimal bdYESalesToday = new BigDecimal(yeSalesAsOfToday);
                        //BigDecimal bdYESalesYesterday = new BigDecimal(yeSalesAsOfYesterday);
                        dashBoard.yeSales          = bdYESalesToday.toString();

                        if(sPrevYearRevenue.equals("0")==false)
                        {
                            BigDecimal bdLastYearRevenue = new BigDecimal(sPrevYearRevenue);
                            BigDecimal bdYESalesDiff = bdYESalesToday.subtract(bdLastYearRevenue);
                            BigDecimal bdYESalesPerc = bdYESalesDiff.divide(bdLastYearRevenue, 10, RoundingMode.HALF_UP).multiply(new BigDecimal(100));

                            dashBoard.yeSalesChangeVal = bdYESalesDiff.toString();
                            dashBoard.yeSalesChangePerc= bdYESalesPerc.toString();
                        }
                        else
                        {
                            dashBoard.yeSalesChangeVal = "0";
                            dashBoard.yeSalesChangePerc= "0";
                        }
                    }
                    else
                        dashBoard.yeSales           = "0";
                }
            }
        }

        // Volume / Quantity Index
        //----------------------------------------------------------------------
        //if(pIndexes.volumeIndex.acc.size()>=1)
        //{
            //dashBoard.accVolumeChangePerc   = pIndexes.volumeIndex.acc.get(0).change.setScale(2, RoundingMode.HALF_UP).toString();
            //dashBoard.accVolumeChangeVal    = pIndexes.volumeIndex.acc.get(0).diff.setScale(2, RoundingMode.HALF_UP).toString();
        Util.Tomcat.print2TomcatLog(logger, "getDashboardValues > Volume Index ", pAccId);
        if(pVolumeIndex!=null)
        {
            Map<String,String> mVolumeIndex = gson.fromJson(pVolumeIndex, Map.class);
            if(mVolumeIndex!=null)
            {
                if(mVolumeIndex.get("today")!=null)
                {
                    String sVolumeIndexAsOfToday = String.valueOf(mVolumeIndex.get("today"));
                    String sVolumeIndexOfYesterday = String.valueOf(mVolumeIndex.get("yesterday"));

                    BigDecimal bdVolumeIndexToday = new BigDecimal(sVolumeIndexAsOfToday);
                    BigDecimal bdVolumeIndexYesterday = new BigDecimal(sVolumeIndexOfYesterday);
                    BigDecimal bdVolumeIndexDiff = bdVolumeIndexToday.subtract(bdVolumeIndexYesterday);
                    BigDecimal bdVolumeIndexPerc = bdVolumeIndexDiff.divide(new BigDecimal(100), 10, RoundingMode.HALF_UP);

                    dashBoard.accVolumeChangePerc = bdVolumeIndexPerc.toString();
                    dashBoard.accVolumeChangeVal  = bdVolumeIndexDiff.toString();
                }
            }
        }
        //}
        
        Util.Tomcat.print2TomcatLog(logger, "getDashboardValues > Last Fix ", pAccId);
        if(pIndexes.volumeIndex.market.size()>=1)
        {
            dashBoard.marketChangePerc = pIndexes.volumeIndex.market.get(0).change.setScale(2, RoundingMode.HALF_UP).toString();
            dashBoard.marketChangeVal  = pIndexes.volumeIndex.market.get(0).diff.setScale(2, RoundingMode.HALF_UP).toString();
        }

        return dashBoard;


    }

    public static DekontQuantityStatCore getLastQuantitiesValue(ArrayList<DekontQuantityStatCore> pEarnings, long pAccountId)
    {
        DekontQuantityStatCore YEStat = new DekontQuantityStatCore();

        for (DekontQuantityStatCore statN:pEarnings)
        {
            //if (statN.id==pAccountId)
            //    YEStat = statN;
        }
        
        return YEStat;
    }

    public static DekontEarningStats getLastEarningValue(ArrayList<DekontEarningStats> pEarnings, long pAccountId)
    {
        DekontEarningStats QStatN = new DekontEarningStats();

        for (DekontEarningStats statN:pEarnings)
        {
            if (statN.id==pAccountId)
                QStatN = statN;
        }

        return QStatN;
    }

    public static DekontNews generateNews(String pRetailName, DekontIndexes pIndexes)
    {
        DekontNews news = new DekontNews();
        
        news.lastUpdate = pIndexes.volumeIndex.lastUpdate;
        
        String sCurrentDate = Util.DateTime.GetDateTime_s("yyyyMMdd");
        
        // ACCOUNT vs MARKET 
        //--------------------------------------------------------
        DekontEarningCore accEarningChange = new DekontEarningCore();
        DekontEarningCore marketEarningChange = new DekontEarningCore();

        if(pIndexes.yearEarningChanges.acc.size()>0)
        {
            accEarningChange    = pIndexes.yearEarningChanges.acc.get(pIndexes.yearEarningChanges.acc.size()-1);
            if(accEarningChange.date.equals(sCurrentDate)==true)//todays date can't exists it will be valid after EOD
            {
                if(pIndexes.yearEarningChanges.acc.size()>=2)
                    accEarningChange    = pIndexes.yearEarningChanges.acc.get(pIndexes.yearEarningChanges.acc.size()-2);
            }
        }
        
        if(pIndexes.yearEarningChanges.market.size()>0)
        {
            marketEarningChange = pIndexes.yearEarningChanges.market.get(pIndexes.yearEarningChanges.market.size()-1);
            if(marketEarningChange.date.equals(sCurrentDate)==true)
            {
                if(pIndexes.yearEarningChanges.market.size()>=2)
                    marketEarningChange    = pIndexes.yearEarningChanges.market.get(pIndexes.yearEarningChanges.market.size()-2);
            }
        }

        news.accName    = pRetailName;
        
        BigDecimal bdAccYearEarningChange    = new BigDecimal(BigInteger.ZERO);
        BigDecimal bdMarketYearEarningChage  = new BigDecimal(BigInteger.ZERO);
        BigDecimal diffInYearEarningChange = new BigDecimal(BigInteger.ZERO);
        if(accEarningChange.val.trim().length()>0)
        {
            bdAccYearEarningChange    = new BigDecimal(accEarningChange.val);
            news.yoyAccount = bdAccYearEarningChange.setScale(2, RoundingMode.HALF_UP).toString();
        }
        
        if(marketEarningChange.val.trim().length()>0)
        {
            bdMarketYearEarningChage  = new BigDecimal(marketEarningChange.val);
            news.yoyMarket  = bdMarketYearEarningChage.setScale(2, RoundingMode.HALF_UP).toString();
        }
        
        if((accEarningChange.val.trim().length()>0) && (marketEarningChange.val.trim().length()>0))
        {
            diffInYearEarningChange   = new BigDecimal(BigInteger.ZERO);
            diffInYearEarningChange = bdAccYearEarningChange.subtract(bdMarketYearEarningChage);

            news.acc2market = diffInYearEarningChange.setScale(2, RoundingMode.HALF_UP).toString();

        }

        return news;
    }

    public static DekontIndexes generateIndexes(EntityManager   pem, 
                                                BigInteger            pAccountId,
                                                String          pMerchantName,
                                                String          pTargetCurrency) throws Exception
    {
        DekontIndexes indexes = new DekontIndexes();

        try
        {
            String stStmt = "";
            boolean bUSD = false;
            
            if(pTargetCurrency.toLowerCase().trim().equals("usd")==true)
                bUSD = true;

            // -1 STORED FOR MARKET
            /*
            stStmt = "SELECT " + 
                        "LAST_SMRY_UPDATE_DATE, " + 
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD, " + 
                        "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD, " + 
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_ORG_CURRENCY, " +
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_USD " +
                     "FROM ss_idx_acc_indexes " +
                     "WHERE " +
                     "STAT = 1 " + 
                     "AND " + 
                     "ACCOUNT_ID = ? " + //pAccountId + //ORDER BY UID DESC";
                     "LIMIT 1 ";

            //pem.cacheable("P_ACCOUNT_ID");
            Query newQuery = pem.CreateQuery(stStmt);
            int ParIndex = 1;
            newQuery.SetParameter(ParIndex++, pAccountId     , "P_ACCOUNT_ID");

            List<List<RowColumn>> rs =  newQuery.getResultList();
            if(rs.size()>0)
            {
            */
                ssoIndexCore accSummary = new ssoIndexCore();
                ssoIndexCore marketSummary = new ssoIndexCore();

                accSummary    = getAccIndexSummary(pem, pAccountId);
                marketSummary = getMarketIndexSummary(pem);

                //String sLastUpdate                      = Util.Database.getValString(rs.get(0), "LAST_SMRY_UPDATE_DATE");

                //String sYearswNormalizedUSD             = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD");//{"2018":132.12, "2019":... 

                //String sIdxDaysOfL12MwNormalizedUSD     = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD");//{"20180101":102.12, "20180102":... 
                //String sYearEarningL12MonthsOrgCurrency = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ORG_CURRENCY");
                //String sYearEarningL12MonthsUSD         = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_USD");

                // Prepare Quantity Stats (THIS IS ALWAYS CALCULATED IN USD) 
                //indexes.volumeIndex.acc = calculateQuantityStats(sLastUpdate, sIdxDaysOfL12MwNormalizedUSD, sYearswNormalizedUSD, pAccountId, pMerchantName);
                //if(accSummary.summaryDaysRevenueL12MAdjCurrency.trim().equals("{}")==false)//if there is a data
                //{
                    indexes.volumeIndex = calculateQuantityStats(accSummary.lastUpdate, 
                                                                 accSummary.summaryDaysRevenueL12MAdjCurrency, 
                                                                 marketSummary.summaryDaysRevenueL12MAdjCurrency,
                                                                 pAccountId, 
                                                                 pMerchantName);
                //}

                // Prepare Year Earning in Value Index (THIS IS EITHER BASE CURRENCY OR USD)
                //String sYearEarningL12Months = sYearEarningL12MonthsOrgCurrency;
                String sAccYearEarningL12Months    = accSummary.summaryDaysRevenueL12MOrgCurrency;
                String sMarketYearEarningL12Months = marketSummary.summaryDaysRevenueL12MOrgCurrency;
                if(bUSD==true)
                {
                    sAccYearEarningL12Months    = accSummary.summaryDaysRevenueL12MUSD ;//sYearEarningL12MonthsUSD;
                    sMarketYearEarningL12Months = marketSummary.summaryDaysRevenueL12MUSD ;//sYearEarningL12MonthsUSD;
                }

                if(sAccYearEarningL12Months.trim().equals("{}")==false)
                {
                    indexes.yearEarningIndex = calculateEarningStats(sAccYearEarningL12Months);
                                                                     //pAccountId, 
                                                                     //pMerchantName);
                    indexes.yearEarningIndex.lastUpdate = accSummary.lastUpdate;
                }

                // Prepare Year Earning Change 
                if( (sAccYearEarningL12Months.trim().equals("{}")==false) || (sMarketYearEarningL12Months.trim().equals("{}")==false) )
                {
                    indexes.yearEarningChanges = calculateEarningChangeStats(sAccYearEarningL12Months, sMarketYearEarningL12Months, pAccountId, pMerchantName);
                    indexes.yearEarningChanges.lastUpdate = accSummary.lastUpdate;
                }
            //}

            return indexes;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoIndexCore getAccIndexSummary(EntityManager   pem, 
                                                  BigInteger            pAccountId) throws Exception
    {
        ssoIndexCore smry = new ssoIndexCore();

        String stStmt = "";

        try
        {
            stStmt = "SELECT " + 
                        "IF(LASTUPDATE=1,INSERTDATE,LASTUPDATE) AS LASTUPDATE, " + 
                        "LAST_SMRY_UPDATE_DATE, " + 
                        "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD, " + 
                        "INDEX_SUMMARY_YEARS_REVENUE_ORG_CURRENCY, " + 
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD, " + 
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_ORG_CURRENCY, " +
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_USD " +
                     "FROM ss_idx_acc_indexes " +
                     "WHERE " +
                     "STAT = 1 " + 
                     "AND " + 
                     "ACCOUNT_ID = ? " + //pAccountId + //ORDER BY UID DESC";
                     "LIMIT 1 ";

            //pem.cacheable("P_ACCOUNT_ID");
            Query newQuery = pem.CreateQuery(stStmt);
            int ParIndex = 1;
            newQuery.SetParameter(ParIndex++, pAccountId     , "P_ACCOUNT_ID");

            List<List<RowColumn>> rs =  newQuery.getResultList();
            if(rs.size()>0)
            {
                smry.lastUpdate                         = Util.Database.getValString(rs.get(0), "LASTUPDATE");

                smry.summaryYearsRevenueAdjUSD          = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD");//{"2018":132.12, "2019":... 
                smry.summaryYearsRevenueOrgCurrency     = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_YEARS_REVENUE_ORG_CURRENCY");

                smry.summaryDaysRevenueL12MAdjCurrency  = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD");//{"20180101":102.12, "20180102":... 
                smry.summaryDaysRevenueL12MOrgCurrency  = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ORG_CURRENCY");
                smry.summaryDaysRevenueL12MUSD          = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_USD");

            }

            return smry;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static ssoIndexCore getMarketIndexSummary(EntityManager   pem) throws Exception
    {
        ssoIndexCore smry = new ssoIndexCore();

        String stStmt = "";

        try
        {
            stStmt = "SELECT " + 
                        "LAST_SMRY_UPDATE_DATE, " + 
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD, " + 
                        "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD, " + 
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_ORG_CURRENCY, " +
                        "INDEX_SUMMARY_DAYS_REVENUE_L12M_USD " +
                     "FROM ss_idx_market_indexes " +
                     "WHERE " +
                     "STAT = 1 " + 
                     "LIMIT 1 ";

            //pem.cacheable("P_ACCOUNT_ID");
            Query newQuery = pem.CreateQuery(stStmt);

            List<List<RowColumn>> rs =  newQuery.getResultList();
            if(rs.size()>0)
            {
                smry.lastUpdate                         = Util.Database.getValString(rs.get(0), "LAST_SMRY_UPDATE_DATE");

                smry.summaryYearsRevenueAdjUSD          = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD");//{"2018":132.12, "2019":... 

                smry.summaryDaysRevenueL12MAdjCurrency  = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD");//{"20180101":102.12, "20180102":... 
                smry.summaryDaysRevenueL12MOrgCurrency  = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ORG_CURRENCY");
                smry.summaryDaysRevenueL12MUSD          = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_USD");

            }

            return smry;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // THIS IS ALWAYS CALCULATED BASED ON USD NO OTHER CURRENCIES ACCEPTED
    public static DekontQuantityStats calculateQuantityStats(  //EntityManager pem, 
                                                                          String    psLastUpdate,
                                                                          String    psAccIdxDaysOfL12MwNormalizedUSD,
                                                                          String    psMarketIdxDaysOfL12MwNormalizedUSD,
                                                                          //String    psYearswNormalizedUSD,
                                                                          BigInteger      pAccountId,
                                                                          String    pMerchantName
                                                                       )
    {
        DekontQuantityStats stats = new DekontQuantityStats();
        stats.name = pMerchantName;
        stats.lastUpdate = psLastUpdate;
        try
        {
            /*
            String stStmt = "";

            // -1 STORED FOR MARKET
            stStmt = "SELECT LAST_SMRY_UPDATE_DATE, INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD, INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD " + 
                     "FROM ss_idx_acc_indexes " + 
                     "WHERE " +
                     "STAT = 1 AND ACCOUNT_ID = ? " + //pAccountId + //ORDER BY UID DESC";
                     "LIMIT 1 ";

            //pem.cacheable("P_ACCOUNT_ID");
            Query newQuery = pem.CreateQuery(stStmt);
            int ParIndex = 1;
            newQuery.SetParameter(ParIndex++, pAccountId     , "P_ACCOUNT_ID");

            List<List<RowColumn>> rs =  newQuery.getResultList();
            if(rs.size()>0)
            {
                String sLastUpdate                  = Util.Database.getValString(rs.get(0), "LAST_SMRY_UPDATE_DATE");
                String sIdxDaysOfL12MwNormalizedUSD = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_DAYS_REVENUE_L12M_ADJ_USD");//{"20180101":102.12, "20180102":... 
                String sYearswNormalizedUSD         = Util.Database.getValString(rs.get(0), "INDEX_SUMMARY_YEARS_REVENUE_ADJ_USD");//{"2018":132.12, "2019":... 
            */
                //Gson gson = new Gson();

                String sIdxDaysOfL12MwNormalizedUSD = psAccIdxDaysOfL12MwNormalizedUSD;
                //String sYearswNormalizedUSD = psYearswNormalizedUSD;

                Gson gson = new Gson();
                Map<String,String> mAccDaysL12MNormalizedEarnings = gson.fromJson(sIdxDaysOfL12MwNormalizedUSD, Map.class);//{"20180101":..., "20180102":....
                Map<String,String> mMarketDaysL12MNormalizedEarnings    = gson.fromJson(psMarketIdxDaysOfL12MwNormalizedUSD, Map.class);//{"20180101":..., "20180102":....

                if(mAccDaysL12MNormalizedEarnings!=null)
                {
                    int days = mAccDaysL12MNormalizedEarnings.size();
                    String[] dates = new String[days + 1];
                    String maxDateYYYYMMDD = "";
                    if(mAccDaysL12MNormalizedEarnings.size()>0)
                        maxDateYYYYMMDD = Collections.max(mAccDaysL12MNormalizedEarnings.keySet());
                    else
                        maxDateYYYYMMDD = Collections.max(mMarketDaysL12MNormalizedEarnings.keySet());//if new user

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                    LocalDate date = LocalDate.parse(maxDateYYYYMMDD, formatter);

                    //LocalDate today = LocalDate.now();
                    for (int i = 0; i <= days; i++) 
                    {
                        dates[i] = date.minusDays(i).format(formatter);
                    }

                    String sLastYearDate   = "";

                    String sAccCurrentYearVal = "";
                    String sAccLastYearVal    = "";
                    
                    String sMarketCurrentYearVal = "";
                    String sMarketLastYearVal    = "";

                    String sChange          = "";
                    BigDecimal bdAccCurrentVal = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdAccChangeVal  = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdAccChangePerc = new BigDecimal(BigInteger.ZERO);

                    BigDecimal bdMarketCurrentVal = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdMarketChangeVal  = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdMarketChangePerc = new BigDecimal(BigInteger.ZERO);

                    boolean bAdd      = true;
                    for (String sDateN: dates)
                    {
                        DekontQuantityStatCore accQntN = new DekontQuantityStatCore();
                        DekontQuantityStatCore marketQntN = new DekontQuantityStatCore();
                        
                        sLastYearDate = Util.DateTime.subtractYears(sDateN, days, "yyyyMMdd");
                        
                        bAdd = false;
                        try
                        {
                            if(mAccDaysL12MNormalizedEarnings.get(sDateN)!=null)
                            {
                                sAccCurrentYearVal = String.valueOf(mAccDaysL12MNormalizedEarnings.get(sDateN));

                                sAccLastYearVal = String.valueOf(mAccDaysL12MNormalizedEarnings.get(sLastYearDate));
                                if(sAccLastYearVal.trim().length()>0)
                                {
                                    // CALCULATE FOR ACCOUNT
                                    //------------------------------------------
                                    BigDecimal bdAccCurrentYearVal = new BigDecimal(sAccCurrentYearVal);
                                    BigDecimal bdAccPrevYearVal    = new BigDecimal(sAccLastYearVal);
                                    bdAccChangeVal   = bdAccCurrentYearVal.subtract(bdAccPrevYearVal);
                                    bdAccChangePerc  = bdAccChangeVal.divide(bdAccPrevYearVal, 4, RoundingMode.HALF_UP);
                                    bdAccChangePerc  = bdAccChangePerc.multiply(new BigDecimal(100));//max 4 decimal point

                                    bdAccCurrentVal    =   bdAccCurrentYearVal;

                                    bAdd = true;
                                }
                            }

                        }
                        catch(Exception e)
                        {
                            sAccCurrentYearVal = "0";
                        }

                        if(bAdd==true)
                        {
                            //qntN.id    = pAccountId;
                            //qntN.name  = pMerchantName;
                            accQntN.date  = sDateN;
                            accQntN.dayNo = Util.DateTime.getDayOfYear(sDateN + "0000000", "yyyyMMddHHmmssSSS") -1;
                            accQntN.value   = bdAccCurrentVal;
                            accQntN.diff    = bdAccChangeVal;
                            accQntN.change  = bdAccChangePerc;

                            stats.acc.add(accQntN);
                        }

                        bAdd = false;
                        try
                        {
                            if(mMarketDaysL12MNormalizedEarnings.get(sDateN)!=null)
                            {
                                // CALCULATE FOR MARKET
                                //------------------------------------------
                                sMarketCurrentYearVal = String.valueOf(Util.misc.ifNull(mMarketDaysL12MNormalizedEarnings.get(sDateN),""));

                                sMarketLastYearVal = String.valueOf(mMarketDaysL12MNormalizedEarnings.get(sLastYearDate));
                                if(sMarketLastYearVal.trim().length()>0)
                                {
                                    BigDecimal bdMarketCurrentYearVal = new BigDecimal(sMarketCurrentYearVal);
                                    BigDecimal bdMarketPrevYearVal    = new BigDecimal(sMarketLastYearVal);

                                    bdMarketChangeVal   = bdMarketCurrentYearVal.subtract(bdMarketPrevYearVal);
                                    bdMarketChangePerc  = bdMarketChangeVal.divide(bdMarketPrevYearVal, 4, RoundingMode.HALF_UP);
                                    bdMarketChangePerc  = bdMarketChangePerc.multiply(new BigDecimal(100));//max 4 decimal point

                                    bdMarketCurrentVal  =   bdMarketCurrentYearVal;

                                    bAdd = true;
                                }
                                else
                                    sMarketCurrentYearVal = "0";
                            }
                        }
                        catch(Exception e)
                        {
                            sMarketCurrentYearVal = "0";
                        }

                        if(bAdd==true)
                        {

                            marketQntN.date    = sDateN;
                            marketQntN.dayNo   = Util.DateTime.getDayOfYear(sDateN + "0000000", "yyyyMMddHHmmssS") -1;
                            marketQntN.value   = bdMarketCurrentVal;
                            marketQntN.diff    = bdMarketChangeVal;
                            marketQntN.change  = bdMarketChangePerc;

                            stats.market.add(marketQntN);

                        }
                    }

                }

                return stats;
                //---------------------------------------------------------------
                
                /*
                Map<String, String> mIdxDays  = gson.fromJson(sIdxDaysOfL12MwNormalizedUSD, Map.class);
                Map<String, String> mIdxYears = gson.fromJson(sYearswNormalizedUSD, Map.class);
                
                TreeMap<String, String> mIdsSortedDays = new TreeMap<>(mIdxDays);

                String sCompareYear = "";
                //String sCompareYearVal = "";
                BigDecimal bdCompareYearVal = new BigDecimal(BigInteger.ZERO);
                for(String sYYYYMMDD: mIdsSortedDays.keySet())
                {
                    DekontQuantityStats qntN = new DekontQuantityStats();

                    String sRefYearN = Integer.toString(Integer.parseInt(sYYYYMMDD.substring(0,4)) - 1);
                    // GET BASE YEAR VALUE
                    //----------------------------------------------------------
                    if(sCompareYear.trim().length()==0)
                    {
                        sCompareYear     = sRefYearN;//Integer.toString(Integer.parseInt(sYYYYMMDD.substring(0,4)) - 1);
                        bdCompareYearVal = new BigDecimal(String.valueOf(mIdxYears.get(sCompareYear)));
                    }
                    else if(sCompareYear.equals(sRefYearN)!=true)
                    {
                        // year changed
                        for(String sYearN: mIdxYears.keySet())
                        {
                            if(sYearN.equals(sRefYearN)==true)
                            {
                                sCompareYear     = sYearN;
                                bdCompareYearVal = new BigDecimal(String.valueOf(mIdxYears.get(sYearN)));
                                break;
                            }
                        }
                    }

                    // GET DAY VALUE 
                    //----------------------------------------------------------
                    int iDayNo = Util.DateTime.getDayOfYear(sYYYYMMDD + "0000000", "yyyyMMddHHmmssS") -1;
                    if(iDayNo % 7 == 0)//1 stat per week
                    {
                        qntN.id      = pAccountId;
                        qntN.name    = pMerchantName;
                        qntN.dayNo   = iDayNo;
                        qntN.date = sYYYYMMDD;
                        qntN.value   = new BigDecimal(String.valueOf(mIdsSortedDays.get(sYYYYMMDD)));
                        qntN.diff    = qntN.value.subtract(bdCompareYearVal);
                        qntN.change  = qntN.diff.divide(bdCompareYearVal, 3, RoundingMode.HALF_UP).multiply(new BigDecimal(100));//3 decimal digits

                        stats.add(qntN);
                    }
                }
                */
            
            //}

            /*
            List<SsMrcStatsQuantity> rs =  newQuery.getResultList(SsMrcStatsQuantity.class);

            double dLastScore = 0;
            double dChange = 0;

            int iDayNo = 0;
            for (SsMrcStatsQuantity statsN:rs)
            {
                DekontQuantityStats newStats = new DekontQuantityStats();

                long lRefDate    = statsN.referenceDate;

                newStats.id      = pAccountId;
                newStats.name    = pMerchantName;
                newStats.dayNo   = iDayNo;
                newStats.refDate = Long.toString(lRefDate);
                //newStats.diff    = 
                double dScore = statsN.qScore.doubleValue() * 100;
                newStats.value = dScore;
                
                newStats.diff = dScore - dLastScore;
                
                dChange = (dScore - dLastScore)/dLastScore;
                if (dLastScore==0)
                    newStats.change = 0;
                else
                    newStats.change = dChange;

                stats.add(newStats);

                dLastScore = dScore;
                iDayNo++;
            }
            */

        }
        catch(Exception e)
        {
            return stats;
        }
    }

    public static SsAccUsrAccountStats getUserAccountStats( EntityManager   pem, 
                                                            BigInteger            pAccountId
                                                          ) throws Exception
    {
        SsAccUsrAccountStats usrAccStats = new SsAccUsrAccountStats();

        try
        {
            String stStmt = "";
            stStmt = "SELECT " +
                        "SALES_SUMMARY_YEARS, " + 
                        "SALES_SUMMARY_MONTHS, " + 
                        "SALES_SUMMARY_WEEKS, " +
                        "SALES_SUMMARY_DAYS, " +
                        "SALES_SUMMARY_MONTH_DAYS, " +
                        "SALES_SUMMARY_MONTH_WEEKS, " +
                        "SALES_SUMMARY_MONTH_WEEKDAY_AVGS, " +
                        "SALES_SUMMARY_QUARTER_MONTH_AVGS, " +
                        "SALES_SUMMARY_QUARTER_WEEKDAY_AVGS, " +
                        "SALES_SUMMARY_RECORDS, " + 
                        "SALES_SUMMARY_USE_RATES, " +
                        "YEAR_EARNINGS, " +
                        "VOLUME_INDEX, " +

                        "INV_SUMMARY, " +
                        "PAYMENTS_SUMMARY, " + 
                        "ACTIVE_VENDORS_SUMMARY " +
                     "FROM ss_acc_usr_account_stats " + 
                     "WHERE " + 
                     "STAT = 1 " + 
                     "AND " +
                     "ACCOUNT_ID = ?";

            Query newQuery = pem.CreateQuery(stStmt);
            int index = 1;
            newQuery.SetParameter(index++, pAccountId               , "ACCOUNT_ID");

            List<List<RowColumn>> rs =  newQuery.getResultList();
            if(rs.size()>0)
            {
                usrAccStats.salesSummaryYears                   = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_YEARS");
                usrAccStats.salesSummaryMonths                  = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_MONTHS");
                usrAccStats.salesSummaryWeeks                   = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_WEEKS");
                usrAccStats.salesSummaryDays                    = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_DAYS");

                usrAccStats.salesSummaryMonthDays               = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_MONTH_DAYS");
                usrAccStats.salesSummaryMonthWeeks              = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_MONTH_WEEKS");
                usrAccStats.salesSummaryMonthWeekdayAvgs        = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_MONTH_WEEKDAY_AVGS");
                usrAccStats.salesSummaryQuarterMonthAvgs        = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_QUARTER_MONTH_AVGS");

                usrAccStats.salesSummaryQuarterWeekdayAvgs      = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_QUARTER_WEEKDAY_AVGS");

                usrAccStats.salesSummaryRecords                 = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_RECORDS");
                usrAccStats.salesSummaryUseRates                = Util.Database.getValString(rs.get(0), "SALES_SUMMARY_USE_RATES");

                usrAccStats.yearEarnings                        = Util.Database.getValString(rs.get(0), "YEAR_EARNINGS");
                usrAccStats.volumeIndex                         = Util.Database.getValString(rs.get(0), "VOLUME_INDEX");

                usrAccStats.invSummary                          = Util.Database.getValString(rs.get(0), "INV_SUMMARY");
                usrAccStats.paymentsSummary                     = Util.Database.getValString(rs.get(0), "PAYMENTS_SUMMARY");
                usrAccStats.activeVendorsSummary                = Util.Database.getValString(rs.get(0), "ACTIVE_VENDORS_SUMMARY");
            }

            return usrAccStats;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // THIS IS EITHER BASE CURRENCY OR USD DUE TO ITS LONG TIME TAKING CALCULATION PROCESS
    public static DekontEarning calculateEarningChangeStats(  //EntityManager   pem, 
                                                            String          pAccSalesSummaryMonthsDays,
                                                            String          pMarketSalesSummaryMonthsDays,
                                                            BigInteger            pAccountId,
                                                            String          pAccountName
                                                           )
    {
        try
        {
            DekontEarning earnings = new DekontEarning();

            Gson gson = new Gson();
            Map<String,String> mAccDaysL12MEarnings = gson.fromJson(pAccSalesSummaryMonthsDays, Map.class);//{"20180101":..., "20180102":....
            Map<String,String> mMarketDaysL12MEarnings = gson.fromJson(pMarketSalesSummaryMonthsDays, Map.class);//{"20180101":..., "20180102":....
            
            if(mAccDaysL12MEarnings!=null)
            {
                int days = mAccDaysL12MEarnings.size(); //last 365 days
                String[] dates = new String[days + 1];
                
                String maxDateYYYYMMDD = "";
                if(mAccDaysL12MEarnings.size()>0)
                    maxDateYYYYMMDD = Collections.max(mAccDaysL12MEarnings.keySet());
                else
                    maxDateYYYYMMDD = Collections.max(mMarketDaysL12MEarnings.keySet());//account has no info or new account
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate date = LocalDate.parse(maxDateYYYYMMDD, formatter);

                //LocalDate startDay = LocalDate.now().minusDays(days);
                LocalDate startDay = date.minusDays(days);
                for (int i = 0; i <= days; i++) 
                {
                    //dates[i] = today.minusDays(i).format(formatter);
                    dates[i] = startDay.plusDays(i).format(formatter);
                }

                String sAccCurrentYearVal = "";
                String sMarketCurrentYearVal = "";

                String sLastYearDate   = "";
                
                String sAccLastYearVal    = "";
                String sMarketLastYearVal    = "";

                String sChange          = "";
                
                BigDecimal bdAccChangeVal  = new BigDecimal(BigInteger.ZERO);
                BigDecimal bdAccChangePerc = new BigDecimal(BigInteger.ZERO);
                
                BigDecimal bdMarketChangeVal  = new BigDecimal(BigInteger.ZERO);
                BigDecimal bdMarketChangePerc = new BigDecimal(BigInteger.ZERO);
                
                boolean bAdd      = true;
                for (String sDateN: dates)
                {
                    DekontEarningCore accEarning = new DekontEarningCore();
                    DekontEarningCore marketEarning = new DekontEarningCore();

                    //earning.id    = pAccountId;
                    //earning.name  = pAccountName;
                    accEarning.date  = sDateN;
                    accEarning.dayNo = Util.DateTime.getDayOfYear(sDateN + "0000000", "yyyyMMddHHmmssSSS") -1;

                    marketEarning.date    = accEarning.date;
                    marketEarning.dayNo   = accEarning.dayNo;

                    sLastYearDate = Util.DateTime.subtractYears(sDateN, days, "yyyyMMdd");
                    
                    bAdd = false;
                    try
                    {
                        // ACCOUNT 
                        if(sDateN.equals("20250116")==true)
                            sDateN = sDateN;

                        
                        if(mAccDaysL12MEarnings.get(sDateN)!=null)
                        {
                            sAccCurrentYearVal    = String.valueOf(mAccDaysL12MEarnings.get(sDateN));

                            sAccLastYearVal = String.valueOf(mAccDaysL12MEarnings.get(sLastYearDate));
                            if(sAccLastYearVal.trim().length()>0)
                            {
                                // ACCOUNT CALCULATION
                                //----------------------------------------------
                                BigDecimal bdAccCurrentYear = new BigDecimal(sAccCurrentYearVal);
                                BigDecimal bdAccPrevYear    = new BigDecimal(sAccLastYearVal);
                                bdAccChangeVal   = bdAccCurrentYear.subtract(bdAccPrevYear);
                                bdAccChangePerc  = bdAccChangeVal.divide(bdAccPrevYear, 4, RoundingMode.HALF_UP);
                                bdAccChangePerc  = bdAccChangePerc.multiply(new BigDecimal(100));//max 4 decimal point

                                bAdd = true;
                            }
                        }
                    }
                    catch(Exception e)
                    {
                        sAccCurrentYearVal = "0";
                    }
                        
                    try
                    {
                        // MARKET 
                        if(mMarketDaysL12MEarnings.get(sDateN)!=null)
                        {
                            sMarketCurrentYearVal = String.valueOf(mMarketDaysL12MEarnings.get(sDateN));

                            // MARKET CALCULATION
                            //----------------------------------------------
                            sMarketLastYearVal = String.valueOf(mMarketDaysL12MEarnings.get(sLastYearDate));
                            if(sMarketLastYearVal.trim().length()>0)
                            {
                                BigDecimal bdMarketCurrentYear = new BigDecimal(sMarketCurrentYearVal);
                                BigDecimal bdMarketPrevYear    = new BigDecimal(sMarketLastYearVal);
                                bdMarketChangeVal   = bdMarketCurrentYear.subtract(bdMarketPrevYear);
                                bdMarketChangePerc  = bdMarketChangeVal.divide(bdMarketPrevYear, 4, RoundingMode.HALF_UP);
                                bdMarketChangePerc  = bdMarketChangePerc.multiply(new BigDecimal(100));//max 4 decimal point
                                
                                bAdd = true;
                            }
                            
                        }
                    }
                    catch(Exception e)
                    {
                        sMarketCurrentYearVal = "0";
                    }

                    //earning.val = sVal;
                    if(bAdd==true)
                    {
                        accEarning.diff    = bdAccChangeVal.toString();
                        accEarning.val     = bdAccChangePerc.toString();
                        marketEarning.val  = bdMarketChangePerc.toString();
                    }

                    earnings.acc.add(accEarning);
                    earnings.market.add(marketEarning);    
                }

            }

            return earnings;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    // THIS IS EITHER BASE CURRENCY OR USD DUE TO ITS LONG TIME TAKING CALCULATION PROCESS
    public static DekontEarning calculateEarningStats(  //EntityManager   pem, 
                                                        String          pAccSalesSummaryMonthsDays
                                                        //long            pAccountId,
                                                        //String          pAccountName
                                                    )
    {
        try
        {
            DekontEarning earnings = new DekontEarning();

            Gson gson = new Gson();
            Map<String,String> mAccDaysL12MEarnings = gson.fromJson(pAccSalesSummaryMonthsDays, Map.class);//{"20180101":..., "20180102":....

            if(mAccDaysL12MEarnings!=null)
            {
                int days = mAccDaysL12MEarnings.size();
                String[] dates = new String[days + 1];

                String maxDateYYYYMMDD = Collections.max(mAccDaysL12MEarnings.keySet());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate date = LocalDate.parse(maxDateYYYYMMDD, formatter);
                //LocalDate today = LocalDate.now();
                for (int i = 0; i <= days; i++)
                {
                    dates[i] = date.minusDays(i).format(formatter);
                }

                String sVal = "";
                for (String sDateN: dates)
                {
                    DekontEarningCore accEarning = new DekontEarningCore();

                    //earning.id    = pAccountId;
                    //earning.name  = pAccountName;
                    accEarning.date  = sDateN;
                    accEarning.dayNo = Util.DateTime.getDayOfYear(sDateN + "0000000", "yyyyMMddHHmmssSSS") -1;

                    try
                    {
                        if(mAccDaysL12MEarnings.get(sDateN)!=null)
                            sVal = String.valueOf(mAccDaysL12MEarnings.get(sDateN));
                        else
                            sVal = "0";
                    }
                    catch(Exception e)
                    {
                        sVal = "0";
                    }

                    //earning.val = sVal;
                    accEarning.ye  = sVal;

                    earnings.acc.add(accEarning);
                }

            }

            return earnings;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    //@Cacheable(type=CacheLoadTypes.LAZY, key="P_MRC_ID")
    // SAMPLE OUTPUT 
    public static ArrayList<DekontSummaryRec> calculateSummaryRecords(  //EntityManager pem, 
                                                                        String  pSummaryRecords,
                                                                        BigInteger    pMerchantId,
                                                                        String  pBaseCurrency, 
                                                                        String  pTargetCurrency, 
                                                                        int     pBankCode, 
                                                                        int     pYear, 
                                                                        int     pMonth) throws Exception
    {
        ArrayList<DekontSummaryRec> SumRows = new ArrayList<DekontSummaryRec>();
        String sAccId = pMerchantId.toString();

        try
        {
            String sYearsNMonths = pSummaryRecords;

            Util.Tomcat.print2TomcatLog(logger, "started > " + pTargetCurrency, sAccId);

            if(sYearsNMonths!=null)
            {
                if(sYearsNMonths.trim().equals("{}")==false)
                {
                    Gson gson = new Gson();
                    Map<String, Map<String, Map<String, Map<String, String>>>> mYearsNMonths = gson.fromJson(sYearsNMonths, Map.class);

                    DekontSummaryRec bottomSum = new DekontSummaryRec();

                    Util.Tomcat.print2TomcatLog(logger, "calculating years ", sAccId);

                    Map<String, ?> mYears = mYearsNMonths.get(pTargetCurrency);
                    Set<String> aYears = (mYears != null) ? mYears.keySet() : Collections.emptySet();

                    Util.Tomcat.print2TomcatLog(logger, "years > " + aYears.toString(), sAccId);

                    //Set<String> aYears = mYearsNMonths.get(pTargetCurrency).keySet();
                    for(String yearN: aYears)
                    {
                        //Set<String> aMonths = mYearsNMonths.get(pTargetCurrency).get(yearN).keySet();
                        Map<String, ?> mMonths = mYearsNMonths.get(pTargetCurrency).get(yearN);
                        Set<String> aMonths = (mMonths != null) ? mMonths.keySet() : Collections.emptySet();
                        Util.Tomcat.print2TomcatLog(logger, "months > " + aMonths.toString(), sAccId);

                        for(String monthN: aMonths)
                        {

                            DekontSummaryRec newSum = new DekontSummaryRec();

                            newSum.Year      = yearN;
                            if(monthN.equals("")==false)
                                newSum.MonthName = Util.DateTime.getMonthName(monthN);

                            newSum.MonthNo   = monthN;
                            newSum.Type      = "";// N/A
                            newSum.Sum         = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN).get("all"));
                            newSum.SumCash     = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN).get("cash"));
                            newSum.SumCard     = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN).get("card"));
                            newSum.SumWire     = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN).get("wire"));
                            newSum.SumInternet = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN).get("dlvry"));//kapida odeme
                            newSum.SumOther    = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN).get("ins"));//elden taksit

                            //newSum.Count   = sCnt;

                            if(yearN.equals("")==false)
                                SumRows.add(newSum);
                            else
                                bottomSum = newSum;
                        }
                    }

                    SumRows.add(bottomSum);
                }
            }

            return SumRows;
        }
        catch(Exception e)
        {
            Util.Tomcat.print2TomcatLog(logger, "exception > " + e.getMessage(), sAccId);
            throw e;
            //return SumRows;
        }
    }
    
    public static DekontPaymentSummary calculatePaymentSummary(String     pActiveVendorsSummary, String pTargetCurrency)
    {
        DekontPaymentSummary vendorsSummary = new DekontPaymentSummary();
        
        Gson gson = new Gson();
        Map<String, Map<String, String>> mSummaryVendors = gson.fromJson(pActiveVendorsSummary, Map.class);
        if(mSummaryVendors!=null)
        {
            int iThisYear    = Util.DateTime.getCurrentYear();
            String sThisYear = Integer.toString(iThisYear);
            String sLastYear = Integer.toString(iThisYear - 1);

            if(mSummaryVendors.get(pTargetCurrency)!=null)
            {
                if(mSummaryVendors.get(pTargetCurrency).get(sThisYear)!=null)
                {
                    vendorsSummary.thisYear = String.valueOf(mSummaryVendors.get(pTargetCurrency).get(sThisYear));
                }

                if(mSummaryVendors.get(pTargetCurrency).get(sLastYear)!=null)
                {
                    vendorsSummary.lastYear = String.valueOf(mSummaryVendors.get(pTargetCurrency).get(sLastYear));
                }
            }
        }
        
        return vendorsSummary;
    }
    
    // {"TRY":{"2024":33550.00,"2025":300.00},"USD":{"2024":33550.00,"2025":300.00}}
    public static DekontActiveVendorsSummary calculateActiveVendorsSummary(String     pPaymentSummary)
    {
        DekontActiveVendorsSummary pymSummary = new DekontActiveVendorsSummary();
        
        Gson gson = new Gson();
        Map<String, Map<String, String>> mSummaryPayments = gson.fromJson(pPaymentSummary, Map.class);
        if(mSummaryPayments!=null)
        {
            int iThisYear    = Util.DateTime.getCurrentYear();
            String sThisYear = Integer.toString(iThisYear);
            String sLastYear = Integer.toString(iThisYear - 1);

            if(mSummaryPayments.get(sThisYear)!=null)
            {
                pymSummary.thisYear = String.valueOf(mSummaryPayments.get(sThisYear));
            }

            if(mSummaryPayments.get(sLastYear)!=null)
            {
                pymSummary.lastYear = String.valueOf(mSummaryPayments.get(sLastYear));
            }
        }

        return pymSummary;
    }

    // Sample : {{"2024":{"002010":{"t":200.00,"q":1.00},"002000":{"t":189442.30,"q":285.00}},"2025":{"002000":{"t":100.00,"q":1.00}}}}
    public static DekontInvSummary calculateInventorySummary(String     pInvSummary)
    {
        DekontInvSummary invSummary = new DekontInvSummary();

        Gson gson = new Gson();
        Map<String, Map<String, Map<String,String>>> mSummaryInv = gson.fromJson(pInvSummary, Map.class);
        
        if(mSummaryInv!=null)
        {
            int iThisYear    = Util.DateTime.getCurrentYear();
            String sThisYear = Integer.toString(iThisYear);
            String sLastYear = Integer.toString(iThisYear - 1);

            if(mSummaryInv.get(sThisYear)!=null)
            {
                if(mSummaryInv.get(sThisYear).get(txnDefs.TXN_CODE_INVENTORY_RECEIVED)!=null)
                {
                    invSummary.thisYearReceived.amount   = String.valueOf(mSummaryInv.get(sThisYear).get(txnDefs.TXN_CODE_INVENTORY_RECEIVED).get("t"));//total amount
                    invSummary.thisYearReceived.quantity = String.valueOf(mSummaryInv.get(sThisYear).get(txnDefs.TXN_CODE_INVENTORY_RECEIVED).get("q"));//total amount
                }
            }
            
            if(mSummaryInv.get(sLastYear)!=null)
            {
                if(mSummaryInv.get(sLastYear).get(txnDefs.TXN_CODE_INVENTORY_RECEIVED)!=null)
                {
                    invSummary.lastYearReceived.amount     = String.valueOf(mSummaryInv.get(sLastYear).get(txnDefs.TXN_CODE_INVENTORY_RECEIVED).get("t"));//total amount
                    invSummary.lastYearReceived.quantity   = String.valueOf(mSummaryInv.get(sLastYear).get(txnDefs.TXN_CODE_INVENTORY_RECEIVED).get("q"));//total amount
                }
            }
            
            if(mSummaryInv.get(sThisYear)!=null)
            {
                if(mSummaryInv.get(sThisYear).get(txnDefs.TXN_CODE_INVENTORY_SENT)!=null)
                {
                    invSummary.thisYearSent.amount     = String.valueOf(mSummaryInv.get(sThisYear).get(txnDefs.TXN_CODE_INVENTORY_SENT).get("t"));//total amount
                    invSummary.thisYearSent.quantity   = String.valueOf(mSummaryInv.get(sThisYear).get(txnDefs.TXN_CODE_INVENTORY_SENT).get("q"));//total amount
                }
            }
            
            if(mSummaryInv.get(sLastYear)!=null)
            {
                if(mSummaryInv.get(sLastYear).get(txnDefs.TXN_CODE_INVENTORY_SENT)!=null)
                {
                    invSummary.lastYearSent.amount      = String.valueOf(mSummaryInv.get(sLastYear).get(txnDefs.TXN_CODE_INVENTORY_SENT).get("t"));//total amount
                    invSummary.lastYearSent.quantity    = String.valueOf(mSummaryInv.get(sLastYear).get(txnDefs.TXN_CODE_INVENTORY_SENT).get("q"));//total amount
                }
            }

        }

        return invSummary;
    }
    
    public static DekontSummaryUseRates calculateSummaryUseRates(//EntityManager  pem, 
                                                                 String         pYearMonthsRates,
                                                                 BigInteger           pAccountId,
                                                                 String         pBaseCurrency, 
                                                                 String         pTargetCurrency,
                                                                 int            pLastNMonth)
    {
        DekontSummaryUseRates rates = new DekontSummaryUseRates();

        try
        {
            //pem.cacheable("P_MRC_ID");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_USE_RATES");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_LAST_N_MONTH"     , Integer.class    , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pLastNMonth     , "P_LAST_N_MONTH");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {

               String sYearMonthsRates = Util.Database.getValString(rs.get(0), "SUMMARY");
               */
               String sYearMonthsRates = pYearMonthsRates;

               // LAST 12 and 24 MONTHS
               //---------------------------------------------------------------
               // Formatter for yyyyMM
               DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
               // Get the current date
               LocalDate currentDate = LocalDate.now();
               // List to store the last 12 months
               List<String> last12Months = new ArrayList<>();
               List<String> last24Months = new ArrayList<>();
               // Loop through the last 12 months
               for (int i = 0; i < 24; i++) 
               {
                   // Subtract months from the current date
                   LocalDate month = currentDate.minusMonths(i);
                   // Format and add to the list
                   if(i<12)
                       last12Months.add(month.format(formatter));
                   
                   if(i<24)
                       last24Months.add(month.format(formatter));
               }
               //---------------------------------------------------------------

               Gson gson = new Gson();
               Map<String, Map<String, Map<String,String>>> mSummaryYearMonthRates = gson.fromJson(sYearMonthsRates, Map.class);

               //for(String keyN: mSummary.keySet())
               //{
               if(mSummaryYearMonthRates!=null)
               {
                    String sCurrency = "TRY";//Rate currency doesnt matter


                    // Calculate Last 12M totals
                    BigDecimal bdTotalAll  = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalCard = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalCash = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalWire = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalDelivery = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalIns  = new BigDecimal(BigInteger.ZERO);

                    Map<String, Map<String,String>> mMonthRates = mSummaryYearMonthRates.get(sCurrency);
                    
                    //for(String sMonthN: last12Months)
                    String sMonthN = "";
                    for(int i=0;i<pLastNMonth;i++)
                    {
                        sMonthN = last24Months.get(i);

                        Map<String,String> mRates = mMonthRates.get(sMonthN);
                        
                        if(mRates!=null)
                        {
                            bdTotalAll      = bdTotalAll.add(new BigDecimal(String.valueOf(mRates.get("t_all"))));
                            bdTotalCard     = bdTotalCard.add(new BigDecimal(String.valueOf(mRates.get("t_crd"))));
                            bdTotalCash     = bdTotalCash.add(new BigDecimal(String.valueOf(mRates.get("t_csh"))));
                            bdTotalWire     = bdTotalWire.add(new BigDecimal(String.valueOf(mRates.get("t_wre"))));
                            bdTotalDelivery = bdTotalDelivery.add(new BigDecimal(String.valueOf(mRates.get("t_dlv"))));
                            bdTotalIns      = bdTotalIns.add(new BigDecimal(String.valueOf(mRates.get("t_ins"))));
                        }

                    }

                    rates.total             = bdTotalAll.toString();
                    rates.subtotal_card     = bdTotalCard.toString();
                    rates.subtotal_cash     = bdTotalCash.toString();
                    rates.subtotal_wire     = bdTotalWire.toString();
                    rates.subtotal_delivery = bdTotalDelivery.toString();
                    rates.subtotal_ret_ins  = bdTotalIns.toString();

                    rates.userate_card      = bdTotalCard.divide(bdTotalAll).multiply(new BigDecimal(100)).toString();
                    rates.userate_cash      = bdTotalCash.divide(bdTotalAll).multiply(new BigDecimal(100)).toString();
                    rates.userate_wire      = bdTotalWire.divide(bdTotalAll).multiply(new BigDecimal(100)).toString();
                    rates.userate_delivery  = bdTotalDelivery.divide(bdTotalAll).multiply(new BigDecimal(100)).toString();
                    rates.userate_ret_ins   = bdTotalIns.divide(bdTotalAll).multiply(new BigDecimal(100)).toString();//retail installment

                    return rates;
               }
               //}

            //}
            /*
            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryYear newYear = new DekontSummaryYear();

                rates.total             = Util.Database.getValString(RowN, "ALL_TOTAL");
                rates.subtotal_card     = Util.Database.getValString(RowN, "CARD_TOTAL");
                rates.subtotal_cash     = Util.Database.getValString(RowN, "CASH_TOTAL");
                rates.subtotal_wire     = Util.Database.getValString(RowN, "WIRE_TOTAL");
                rates.subtotal_delivery = Util.Database.getValString(RowN, "DLVR_TOTAL");
                rates.subtotal_ret_ins  = Util.Database.getValString(RowN, "INS_TOTAL");

                rates.userate_card     = Util.Database.getValString(RowN, "RATE_CARD");
                rates.userate_cash     = Util.Database.getValString(RowN, "RATE_CASH");
                rates.userate_wire     = Util.Database.getValString(RowN, "RATE_WIRE");
                rates.userate_delivery = Util.Database.getValString(RowN, "RATE_DELIVERY");
                rates.userate_ret_ins  = Util.Database.getValString(RowN, "RATE_RETAIL_INS");

                return rates;//only one row
            }
            */

            return rates;
        }
        catch(Exception e)
        {
            return rates;
        }
    }

    public static ArrayList<DekontSummaryYear> calculateSummaryYears( //EntityManager pem, 
                                                                      String pSummaryYears,
                                                                      BigInteger   pAccountId,
                                                                      String pBaseCurrency, 
                                                                      String pTargetCurrency) throws Exception
    {
        ArrayList<DekontSummaryYear> overall = new ArrayList<DekontSummaryYear>();
        String sAccId = pAccountId.toString();

        try
        {
            // Returns like
            // {"USD":{"2018":161094.55,"2019":150712.59,"2020":91501.59,"2021":155139.48,"2022":154840.96,"2023":97614.29,"2024":157309.41},"TRY":{"2018":778120.00,"2019":859061.00,"2020":635262.00,"2021":1333973.00,"2022":2627331.00,"2023":2434404.00,"2024":5179780.00}}
            //pem.cacheable("P_MRC_ID");
            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_OVERALL");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_YEARS");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_FROM_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TO_CURRENCY"  , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_FROM_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TO_CURRENCY");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sYears = Util.Database.getValString(rs.get(0), "YEARS");
            */
            Util.Tomcat.print2TomcatLog(logger, "started" + pSummaryYears, sAccId);

            String sYears = pSummaryYears;
            if(sYears!=null)
            {
                if(sYears.trim().equals("{}")==false)
                {

                    Gson gson = new Gson();
                    Map<String, Map<String, String>> mYears = gson.fromJson(sYears, Map.class);

                    for(String yearN: mYears.get(pTargetCurrency).keySet())
                    {
                        DekontSummaryYear newYear = new DekontSummaryYear();

                        //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                        newYear.year     = yearN;
                        newYear.Sum      = String.valueOf(mYears.get(pTargetCurrency).get(yearN));

                        overall.add(newYear);
                    }
                }
            }

            return overall;
        }
        catch(Exception e)
        {
            Util.Tomcat.print2TomcatLog(logger, "exception > " + e.getMessage(), sAccId);
            throw e;
        }
    }

    public static ArrayList<DekontSummaryTots> calculateSummaryBankSubtotals(EntityManager pem,
                                                                             long pAccountId,
                                                                             String pBaseCurrency, 
                                                                             String pTargetCurrency, 
                                                                             String pBaseYear, 
                                                                             int pMonth)
    {
        ArrayList<DekontSummaryTots> bankCodes = new ArrayList<DekontSummaryTots>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_BY_BANK");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MONTH"     , String.class  , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");
            SP.SetParameter(Colindex++, pMonth          , "P_MONTH");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryTots newBank = new DekontSummaryTots();
                
                newBank.Code      = Util.Database.getValString(RowN, "BANK_CODE");
                newBank.Name      = Util.Database.getValString(RowN, "NAME");
                newBank.Sum       = Util.Database.getValString(RowN, "SUM");
                newBank.Count     = Util.Database.getValString(RowN, "CNT");
                
                bankCodes.add(newBank);
            }

            return bankCodes;

        }
        catch(Exception e)
        {
            return bankCodes;
        }

    }

    public static ArrayList<DekontSummaryYear> calculateSummaryMonths(//EntityManager  pem,
                                                                     String         pSummaryMonths,
                                                                     BigInteger           pAccountId,
                                                                     String         pBaseCurrency, 
                                                                     String         pTargetCurrency, 
                                                                     String         pBaseYear) throws Exception
    {
        ArrayList<DekontSummaryYear> yearsNmonths = new ArrayList<DekontSummaryYear>(); 
        String sAccId = pAccountId.toString();

        try
        {

            // SAMPLE OUTPUT
            // 
            //pem.cacheable("P_MRC_ID");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_BY_MONTHS");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"        , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sYearsNMonths = Util.Database.getValString(rs.get(0), "MONTHS");
                */
            Util.Tomcat.print2TomcatLog(logger, "started", sAccId);

            String sYearsNMonths = pSummaryMonths;
            if(sYearsNMonths!=null)
            {
                if(sYearsNMonths.trim().equals("{}")==false)
                {
                    Gson gson = new Gson();
                    Map<String, Map<String, Map<String, String>>> mYearsNMonths = gson.fromJson(sYearsNMonths, Map.class);

                    for(String yearN: mYearsNMonths.get(pTargetCurrency).keySet())
                    {
                        for(String monthN: mYearsNMonths.get(pTargetCurrency).get(yearN).keySet())
                        {
                            DekontSummaryYear newYear = new DekontSummaryYear();

                            //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                            newYear.year     = yearN;
                            newYear.month    = monthN;
                            newYear.Sum      = String.valueOf(mYearsNMonths.get(pTargetCurrency).get(yearN).get(monthN));

                            yearsNmonths.add(newYear);
                        }
                    }
                }
            }

            return yearsNmonths;

        }
        catch(Exception e)
        {
            //return years;
            Util.Tomcat.print2TomcatLog(logger, "exception = " + e.getMessage(), sAccId);
            throw e;
        }
    }

    public static ArrayList<DekontSummaryQuarterDay> calculateSummaryQuarterWeekDays(//EntityManager pem, 
                                                                                 String pSummaryQuarterDays,
                                                                                 BigInteger   pAccountId,
                                                                                 String pBaseCurrency, 
                                                                                 String pTargetCurrency, 
                                                                                 String pBaseYear)
    {
        ArrayList<DekontSummaryQuarterDay> quarters = new ArrayList<DekontSummaryQuarterDay>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS_BY_QUARTER");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKDAYS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"        , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sQDays = Util.Database.getValString(rs.get(0), "Q_DAYS");
                */
            String sQDays = pSummaryQuarterDays;
            if(sQDays.trim().equals("{}")==false)
            {
                Gson gson = new Gson();
                Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> mQuartersNDays = gson.fromJson(sQDays, Map.class);

                for(String yearN: mQuartersNDays.get(pTargetCurrency).keySet())
                {
                    if((pBaseYear.equals(yearN)==true) || (pBaseYear.equals("-1")==true))
                    {
                        for(String quarterN: mQuartersNDays.get(pTargetCurrency).get(yearN).keySet())
                        {
                            for(String dayN: mQuartersNDays.get(pTargetCurrency).get(yearN).get(quarterN).keySet())
                            {
                                DekontSummaryQuarterDay newQuarter = new DekontSummaryQuarterDay();

                                newQuarter.year      = yearN;
                                newQuarter.quarter   = quarterN;
                                newQuarter.day       = dayN;

                                newQuarter.Sum       = String.valueOf(mQuartersNDays.get(pTargetCurrency).get(yearN).get(quarterN).get(dayN).get("sum")).replaceAll(",", "");

                                newQuarter.Count     = String.valueOf(mQuartersNDays.get(pTargetCurrency).get(yearN).get(quarterN).get(dayN).get("cnt")).replaceAll(",", "");
                                newQuarter.Avg       = String.valueOf(mQuartersNDays.get(pTargetCurrency).get(yearN).get(quarterN).get(dayN).get("avg")).replaceAll(",", "");

                                quarters.add(newQuarter);
                            }
                        }
                    }
                }
            }
            /*
            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryQuarterDay newQuarter = new DekontSummaryQuarterDay();

                newQuarter.quarter   = Util.Database.getValString(RowN, "Q");
                newQuarter.day       = Util.Database.getValString(RowN, "DAY");
                newQuarter.Sum       = Util.Database.getValString(RowN, "SUM");
                newQuarter.Count     = Util.Database.getValString(RowN, "CNT");
                newQuarter.Avg       = Util.Database.getValString(RowN, "AVG");

                quarters.add(newQuarter);
            }
            */

            return quarters;

        }
        catch(Exception e)
        {
            return quarters;
        }
    }

    public static ArrayList<DekontSummaryQuarterMonth> calculateSummaryQuarterMonths(//EntityManager pem, 
                                                                                     String pSummaryQuarterMonths,
                                                                                     BigInteger   pAccountId,                                                                       
                                                                                     String pBaseCurrency, 
                                                                                     String pTargetCurrency, 
                                                                                     String pBaseYear) throws Exception
    {
        ArrayList<DekontSummaryQuarterMonth> quarters = new ArrayList<DekontSummaryQuarterMonth>();
        int testCounter = 0;

        try
        {
            //pem.cacheable("P_MRC_ID");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_MONTHS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                
                String sYQMonths = Util.Database.getValString(rs.get(0), "Q_MONTHS");
                */
            String sYQMonths = pSummaryQuarterMonths;
            if(sYQMonths.trim().equals("{}")==false)
            {
                if(sYQMonths.trim().length()!=0)
                {
                    Gson gson = new Gson();
                    Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> mQuartersNMonths = gson.fromJson(sYQMonths, Map.class);

                    for(String yearN: mQuartersNMonths.get(pTargetCurrency).keySet())//2017, 2018....
                    {
                        if((pBaseYear.equals(yearN)==true) || (pBaseYear.equals("-1")==true))
                        {
                            for(String quarterN: mQuartersNMonths.get(pTargetCurrency).get(yearN).keySet())
                            {
                                for(String monthN: mQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).keySet())
                                {
                                    DekontSummaryQuarterMonth newMonth = new DekontSummaryQuarterMonth();

                                    //String sMonthVals = String.valueOf(mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN));

                                    //{"sum":"516,225.00","cnt":"31","avg":"16,652.42"}
                                    //Map<String, String> mKeyNValues = gson.fromJson(sMonthVals, Map.class);
                                    int iAvgMonthDays =30;
                                    if (monthN.trim().equals("2")==true)
                                        iAvgMonthDays = 28;//for february

                                    String sFormattedAvg = String.valueOf(mQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("avg")).replaceAll(",", "");
                                    BigDecimal bdAvgDayRevenue = new BigDecimal(sFormattedAvg);
                                    BigDecimal bdAvgSumMonthRevenue = bdAvgDayRevenue.multiply(new BigDecimal(iAvgMonthDays));//average month calculated of 30 days

                                    newMonth.year       = yearN;
                                    newMonth.quarter    = quarterN;
                                    newMonth.month      = monthN;
                                    newMonth.Count      = String.valueOf(mQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("cnt"));//Count of sum of all days in Q of years
                                    //newMonth.Sum        = String.valueOf(mQuartersNMonths.get(pTargetCurrency).get(quarterN).get(monthN).get("sum"));
                                    //if(pBaseYear.trim().equals("-1")==true)
                                    //{
                                    //    newMonth.Sum        = bdAvgSumMonthRevenue.toString();
                                    //}
                                    //else
                                    //{   // specific year
                                        newMonth.Sum        = String.valueOf(mQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("sum"));
                                    //}
                                    newMonth.Avg        = String.valueOf(mQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("avg"));//day average
                                    //newMonth.year     = yearN;
                                    //newMonth.day      = weekDayN;
                                    //newMonth.Avg      = String.valueOf(mYearNWeekDays.get(pTargetCurrency).get(yearN).get(weekDayN));

                                    quarters.add(newMonth);
                                }//month
                            }//quarter
                        }//year
                    }

                }

            }

            // Calculate For All 
            /*
            ArrayList<DekontSummaryQuarterMonth> quartersAllYears = new ArrayList<DekontSummaryQuarterMonth>();
            if(pBaseYear.trim().equals("-1")==true)
            {
                for(int q=1;q<=4;q++)
                {
                    for(int m=1;m<=3;m++)
                    {
                        BigDecimal bdTotalSum   = new BigDecimal(BigInteger.ZERO);
                        BigDecimal bdTotalCount = new BigDecimal(BigInteger.ZERO);

                        for(DekontSummaryQuarterMonth quarterN:quarters)
                        {
                            if(quarterN.quarter.equals("Q" + Integer.toString(q))==true)
                            {
                                if(quarterN.month.equals(Integer.toString(m))==true)
                                {
                                    String sSum = quarterN.Sum.replaceAll(",", "");
                                    String sCnt = quarterN.Count.replaceAll(",", "");

                                    bdTotalSum   = bdTotalSum.add(new BigDecimal(sSum));
                                    bdTotalCount = bdTotalCount.add(new BigDecimal(sCnt));
                                }
                            }
                        }
                        // RESHAPING FOR ALL YEARS
                        DekontSummaryQuarterMonth quarterN = new DekontSummaryQuarterMonth();
                        quarterN.quarter = "Q" + Integer.toString(q);
                        quarterN.month   = Integer.toString(m);
                        quarterN.year    = "-1";
                        quarterN.Sum     = bdTotalSum.toString();
                        quarterN.Count   = bdTotalCount.toString();
                        quarterN.Avg     = bdTotalSum.divide(bdTotalCount, 2, RoundingMode.HALF_UP).toString();
                        quartersAllYears.add(quarterN);

                    }
                }
            }

            ArrayList<DekontSummaryQuarterMonth> targetQuarters = new ArrayList<DekontSummaryQuarterMonth>();
            if(pBaseYear.trim().equals("-1")==true)
                targetQuarters = quartersAllYears;
            else
                targetQuarters = quarters;
            */
            ArrayList<DekontSummaryQuarterMonth> targetQuarters = new ArrayList<DekontSummaryQuarterMonth>();
            targetQuarters = quarters;

            // CALCULATE YEAR SHARE / PERCENTAGES FOR EACH MONTH
            for(DekontSummaryQuarterMonth targetMonthN:targetQuarters)
            {
                if(testCounter==108)
                    testCounter = testCounter;

                String sTargetYear  = targetMonthN.year;
                String sTargetMonth = targetMonthN.month;
                BigDecimal bdYearTotal = new BigDecimal(BigInteger.ZERO);

                // Calculate Year Total
                for(DekontSummaryQuarterMonth monthN:quarters)
                {
                    if((monthN.year==sTargetYear) || (sTargetYear.equals("-1")==true))
                    {
                        String sSum = monthN.Sum.replaceAll(",", "");
                        bdYearTotal = bdYearTotal.add(new BigDecimal(sSum));
                    }
                }

                // Calculate Percentage
                if(bdYearTotal.equals(BigInteger.ZERO)!=true)
                {
                    String sSum = targetMonthN.Sum.replaceAll(",", "");
                    if(bdYearTotal.compareTo(BigDecimal.ZERO)!=0)
                        targetMonthN.Perc = new BigDecimal(sSum).divide(bdYearTotal, 3, RoundingMode.HALF_UP).toString();
                    else
                        targetMonthN.Perc = "0";
                }
                else
                    targetMonthN.Perc = "0";
                
                testCounter++;
            }

            return targetQuarters;

        }
        catch(Exception e)
        {
            //return quarters;
            throw e;
        }
    }

    public static ArrayList<DekontSummaryQuarterMonth> calculateSummaryYearQuarterMonths(EntityManager pem, 
                                                                                    long pAccountId,                                                                       
                                                                                    String pBaseCurrency, 
                                                                                    String pTargetCurrency, 
                                                                                    String pBaseYear) throws Exception
    {
        ArrayList<DekontSummaryQuarterMonth> quarters = new ArrayList<DekontSummaryQuarterMonth>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_MONTHS_BY_YEAR_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sYQMonths = Util.Database.getValString(rs.get(0), "Y_Q_MONTHS");

                Gson gson = new Gson();
                Map<String, Map<String, Map<String, Map<String, Map<String, String>>>>> mYearNQuartersNMonths = gson.fromJson(sYQMonths, Map.class);

                for(String yearN: mYearNQuartersNMonths.get(pTargetCurrency).keySet())
                {
                    for(String quarterN: mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).keySet())
                    {
                        for(String monthN: mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).keySet())
                        {
                            DekontSummaryQuarterMonth newMonth = new DekontSummaryQuarterMonth();

                            //String sMonthVals = String.valueOf(mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN));

                            //{"sum":"516,225.00","cnt":"31","avg":"16,652.42"}
                            //Map<String, String> mKeyNValues = gson.fromJson(sMonthVals, Map.class);

                            newMonth.year       = yearN;
                            newMonth.quarter    = quarterN;
                            newMonth.month      = monthN;
                            newMonth.Count      = String.valueOf(mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("cnt"));
                            newMonth.Sum        = String.valueOf(mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("sum"));
                            newMonth.Avg        = String.valueOf(mYearNQuartersNMonths.get(pTargetCurrency).get(yearN).get(quarterN).get(monthN).get("avg"));
                            //newMonth.year     = yearN;
                            //newMonth.day      = weekDayN;
                            //newMonth.Avg      = String.valueOf(mYearNWeekDays.get(pTargetCurrency).get(yearN).get(weekDayN));

                            quarters.add(newMonth);
                        }
                    }
                }

            }
            
            /*
            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryQuarterWeek newQuarter = new DekontSummaryQuarterWeek();

                newQuarter.quarter   = Util.Database.getValString(RowN, "Q");
                newQuarter.week      = Util.Database.getValString(RowN, "WEEK");
                newQuarter.Sum       = Util.Database.getValString(RowN, "SUM");
                newQuarter.Count     = Util.Database.getValString(RowN, "CNT");
                newQuarter.Avg       = Util.Database.getValString(RowN, "AVG");

                quarters.add(newQuarter);
            }
            */

            return quarters;

        }
        catch(Exception e)
        {
            //return quarters;
            throw e;
        }
    }

    public static ArrayList<DekontSummaryQuarterWeek> calculateSummaryQuarterWeeks(EntityManager pem, 
                                                                                    long pAccountId,                                                                       
                                                                                    String pBaseCurrency, 
                                                                                   String pTargetCurrency, 
                                                                                   String pBaseYear)
    {
        ArrayList<DekontSummaryQuarterWeek> quarters = new ArrayList<DekontSummaryQuarterWeek>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryQuarterWeek newQuarter = new DekontSummaryQuarterWeek();

                newQuarter.quarter   = Util.Database.getValString(RowN, "Q");
                newQuarter.week      = Util.Database.getValString(RowN, "WEEK");
                newQuarter.Sum       = Util.Database.getValString(RowN, "SUM");
                newQuarter.Count     = Util.Database.getValString(RowN, "CNT");
                newQuarter.Avg       = Util.Database.getValString(RowN, "AVG");

                quarters.add(newQuarter);
            }

            return quarters;

        }
        catch(Exception e)
        {
            return quarters;
        }
    }

    public static ArrayList<DekontSummaryDay> calculateSummaryTargetMonthWeekDayAverages(//EntityManager pem, 
                                                                                         String pSummaryMonthWeekDayAvgs,
                                                                                         BigInteger   pAccountId,
                                                                                         String pBaseCurrency, 
                                                                                         String pTargetCurrency, 
                                                                                         String pBaseYear, 
                                                                                         String pBaseMonth) throws Exception
    {
        ArrayList<DekontSummaryDay> weekDays = new ArrayList<DekontSummaryDay>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_MONTH_DAYS_AVG");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_MONTH_WEEKDAYS_AVG");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency  , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency, "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");
            SP.SetParameter(Colindex++, pBaseMonth     , "P_BASE_MONTH");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sYearNWeekDays = Util.Database.getValString(rs.get(0), "WEEK_DAY_AVGS");
                */
            String sYearNWeekDays = pSummaryMonthWeekDayAvgs;
            if(sYearNWeekDays.trim().equals("{}")==false)
            {
                Gson gson = new Gson();
                Map<String, Map<String, Map<String, String>>> mYearNWeekDays = gson.fromJson(sYearNWeekDays, Map.class);

                for(String yearN: mYearNWeekDays.get(pTargetCurrency).keySet())
                {
                    for(String weekDayN: mYearNWeekDays.get(pTargetCurrency).get(yearN).keySet())
                    {
                        DekontSummaryDay newDay = new DekontSummaryDay();

                        //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                        newDay.year     = yearN;
                        newDay.day      = weekDayN;
                        newDay.Avg      = String.valueOf(mYearNWeekDays.get(pTargetCurrency).get(yearN).get(weekDayN));

                        weekDays.add(newDay);
                    }
                }
            }
            
            return weekDays;
        }
        catch(Exception e)
        {
            throw e;
            //return days;
        }
    }

    public static ArrayList<DekontSummaryDay> calculateSummaryDaysOfMonth(  //EntityManager pem, 
                                                                            String pSummaryMonthDays,
                                                                            BigInteger   pAccountId,
                                                                            String pBaseCurrency, 
                                                                            String pTargetCurrency, 
                                                                            String pBaseYear, 
                                                                            String pBaseMonth)
    {
        ArrayList<DekontSummaryDay> days = new ArrayList<DekontSummaryDay>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS_OF_MONTHS");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId     , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency  , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency, "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");
            SP.SetParameter(Colindex++, pBaseMonth     , "P_BASE_MONTH");

            SP.execute();
            
            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sYearNMonthsNWeeks = Util.Database.getValString(rs.get(0), "MONTH_DAYS");
                */
            String sYearNMonthsNWeeks = pSummaryMonthDays;
            if(sYearNMonthsNWeeks.trim().equals("{}")==false)
            {
                Gson gson = new Gson();
                Map<String, Map<String, Map<String, Map<String, String>>>> mYearsNMonthsNDays = gson.fromJson(sYearNMonthsNWeeks, Map.class);

                for(String yearN: mYearsNMonthsNDays.get(pTargetCurrency).keySet())
                {
                    for(String monthN: mYearsNMonthsNDays.get(pTargetCurrency).get(yearN).keySet())
                    {
                        for(String dayN: mYearsNMonthsNDays.get(pTargetCurrency).get(yearN).get(monthN).keySet())
                        {
                            int iMonthN       = Integer.parseInt(monthN);
                            int iTargetMonthN = Integer.parseInt(pBaseMonth);

                            if((iMonthN==iTargetMonthN) || (iTargetMonthN==-1))
                            {
                                DekontSummaryDay newDay = new DekontSummaryDay();

                                //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                                newDay.year     = yearN;
                                newDay.month    = monthN;
                                newDay.day      = dayN;
                                newDay.Sum      = String.valueOf(mYearsNMonthsNDays.get(pTargetCurrency).get(yearN).get(monthN).get(dayN));

                                days.add(newDay);
                            }
                        }
                    }
                }
            }

            return days;
        }
        catch(Exception e)
        {
            return days;
        }
    }

    public static ArrayList<DekontSummaryWeek> calculateSummaryWeeks(//EntityManager pem,
                                                                     String pSummaryWeeks,
                                                                     BigInteger   pAccountId,
                                                                     String pBaseCurrency, 
                                                                     String pTargetCurrency,
                                                                     String pBaseYear, 
                                                                     String pBaseMonth) throws Exception
    {
        ArrayList<DekontSummaryWeek> weeks = new ArrayList<DekontSummaryWeek>();

        try
        {
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId     , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency  , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency, "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");
            SP.SetParameter(Colindex++, pBaseMonth     , "P_BASE_MONTH");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
                String sYearNWeeks = Util.Database.getValString(rs.get(0), "WEEKS");
                */
            String sYearNWeeks = pSummaryWeeks;
                
            if(sYearNWeeks.trim().equals("{}")==false)
            {
                Gson gson = new Gson();
                Map<String, Map<String, Map<String, String>>> mYearsNWeeks = gson.fromJson(sYearNWeeks, Map.class);

                for(String yearN: mYearsNWeeks.get(pTargetCurrency).keySet())
                {
                    for(String weekN: mYearsNWeeks.get(pTargetCurrency).get(yearN).keySet())
                    {
                        DekontSummaryWeek newWeek = new DekontSummaryWeek();

                        //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                        newWeek.year     = yearN;
                        newWeek.weekNo   = weekN;
                        newWeek.Sum      = String.valueOf(mYearsNWeeks.get(pTargetCurrency).get(yearN).get(weekN));

                        weeks.add(newWeek);
                    }
                }

            }

            return weeks;
        }
        catch(Exception e)
        {
            //return quarters;
            throw e;
        }
    }

    // First N Days 
    public static ArrayList<DekontSummaryNthDay> calculateSummaryDays(//EntityManager pem,
                                                                        String pSummaryDays,
                                                                        BigInteger   pAccountId,
                                                                        String pBaseCurrency, 
                                                                        String pTargetCurrency,
                                                                        String pBaseYear, 
                                                                        String pBaseMonth) throws Exception
    {
        ArrayList<DekontSummaryNthDay> days = new ArrayList<DekontSummaryNthDay>();

        try
        {

            String sYearNDays = pSummaryDays;

            if(sYearNDays!=null)
            {
                if(sYearNDays.trim().equals("{}")==false)
                {
                    Gson gson = new Gson();
                    Map<String, Map<String, Map<String, String>>> mYearsNDays = gson.fromJson(sYearNDays, Map.class);
                    
                    if(mYearsNDays!=null)
                    {
                        for(String yearN: mYearsNDays.get(pTargetCurrency).keySet())
                        {
                            for(String dayN: mYearsNDays.get(pTargetCurrency).get(yearN).keySet())
                            {
                                DekontSummaryNthDay newDay = new DekontSummaryNthDay();

                                //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                                newDay.year     = yearN;
                                newDay.dayNo    = dayN;
                                newDay.Sum      = String.valueOf(mYearsNDays.get(pTargetCurrency).get(yearN).get(dayN));

                                days.add(newDay);
                            }
                        }
                    }

                }
            }

            return days;
        }
        catch(Exception e)
        {
            //return quarters;
            throw e;
        }
    }

    public static ArrayList<DekontSummaryWeek> calculateSummaryWeeksOfMonth(//EntityManager pem,
                                                                            String pSummaryMonthWeeks,
                                                                            BigInteger   pAccountId,
                                                                            String pBaseCurrency, 
                                                                            String pTargetCurrency, 
                                                                            String pBaseYear, 
                                                                            String pBaseMonth) throws Exception
    {
        ArrayList<DekontSummaryWeek> weeks = new ArrayList<DekontSummaryWeek>();

        try
        {
            //pem.cacheable("P_MRC_ID");
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS_OF_MONTH");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency  , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency, "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");
            SP.SetParameter(Colindex++, pBaseMonth     , "P_BASE_MONTH");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            if(rs.size()>0)
            {
            
                String sYearNMonthsNWeeks = Util.Database.getValString(rs.get(0), "MONTH_WEEKS");
            */
            String sYearNMonthsNWeeks = pSummaryMonthWeeks;
            if(sYearNMonthsNWeeks.trim().equals("{}")==false)
            {
                Gson gson = new Gson();
                Map<String, Map<String, Map<String, Map<String, String>>>> mYearsNMonthsNDays = gson.fromJson(sYearNMonthsNWeeks, Map.class);

                for(String yearN: mYearsNMonthsNDays.get(pTargetCurrency).keySet())
                {
                    for(String monthN: mYearsNMonthsNDays.get(pTargetCurrency).get(yearN).keySet())
                    {
                        for(String weekN: mYearsNMonthsNDays.get(pTargetCurrency).get(yearN).get(monthN).keySet())
                        {
                            DekontSummaryWeek newWeek = new DekontSummaryWeek();

                            //JsonObject jsoYears = Util.JSON.toJsonObject(sYears);

                            newWeek.year     = yearN;
                            //newWeek.    = monthN;
                            newWeek.weekNo   = weekN;
                            newWeek.Sum      = String.valueOf(mYearsNMonthsNDays.get(pTargetCurrency).get(yearN).get(monthN).get(weekN));

                            weeks.add(newWeek);
                        }
                    }
                }
            }

            return weeks;
        }
        catch(Exception e)
        {
            //return quarters;
            throw e;
        }
    }

    public static boolean updateEODAll(EntityManager               pem,
                                        BigInteger                        pUserId,
                                        BigInteger                        pMrcId,
                                        ArrayList<SsMrcCashRegEod>  paEODRows
                                        /*
                                        String                      pTxnDate,
                                        BigDecimal      pbdCashTotal,
                                        BigDecimal      pbdCardTotal,
                                        BigDecimal      pbdWireTotal,
                                        BigDecimal      pbdInternetTotal,
                                        BigDecimal      pbdOtherTotal
                                        */
                                        )
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_SUMMARY_UPDATE_EOD");//JUST A SIMPLE UPDATE TABLE QUERY
            SP.registerStoredProcedureParameter("P_USR_ACC_ID"  , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MRC_ID"      , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_EOD_DATE"    , String.class   , ParameterMode.IN);

            SP.registerStoredProcedureParameter("P_CASH_TOTAL"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CARD_TOTAL"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_WIRE_TOTAL"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_DLVR_TOTAL"   , String.class   , ParameterMode.IN);//KAPIDA ODEME
            SP.registerStoredProcedureParameter("P_INS_TOTAL" , String.class   , ParameterMode.IN);//ELDEN TAKSIT

            for(SsMrcCashRegEod eodUpdN:paEODRows)
            {
                //if(eodUpdN.txnDate.equals("20170903")==true)
                //    eodUpdN.txnDate = eodUpdN.txnDate;

                int Colindex = 1;
                SP.SetParameter(Colindex++, pUserId                 , "P_USR_ACC_ID");
                SP.SetParameter(Colindex++, pMrcId                  , "P_MRC_ID");
                SP.SetParameter(Colindex++, eodUpdN.txnDate         , "P_EOD_DATE");

                SP.SetParameter(Colindex++, eodUpdN.fnlCashTotal    , "P_AMOUNT");
                SP.SetParameter(Colindex++, eodUpdN.fnlCardTotal    , "P_AMOUNT");
                SP.SetParameter(Colindex++, eodUpdN.fnlWireTotal    , "P_AMOUNT");
                SP.SetParameter(Colindex++, eodUpdN.fnlDeliveryTotal, "P_AMOUNT");
                SP.SetParameter(Colindex++, eodUpdN.fnlRetInsTotal  , "P_AMOUNT");//elden taksit

                SP.addBatch();
                //SP.execute();
            }

            int[] AffectedRows = SP.executeBatch();

            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public static boolean isEODAdded(EntityManager pem, long pMrcId, String pTxnDate)
    {
        try
        {
            Query stmt = pem.createNamedQuery("SsMrcDataEod.isEodDone", SsMrcCashRegEod.class);
            int index = 1;
            stmt.SetParameter(index++, pMrcId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pTxnDate        , "TXN_DATE");

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                int iCount  = Integer.parseInt(Util.Database.getValString(rs.get(0), "CNT"));

                if (iCount>0)
                    return true;
                else
                    return false;
            }

            return false;
            /*
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_SUMMARY_IS_EOD_ADDED");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TXN_DATE"  , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pMrcId         , "P_MRC_ID");
            SP.SetParameter(Colindex++, pTxnDate       , "P_TXN_DATE");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            
            if (rs.size()>0)
            {
                int iCount  = Integer.parseInt(Util.Database.getValString(rs.get(0), "CNT"));
                
                if (iCount>0)
                    return true;
                else
                    return false;
            }
            
            return false;
            */
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public static ArrayList<ssoAccInvBalanceCore> calculateAccountBalance4Brand(EntityManager pem, 
                                                                                BigInteger    pAccountId,
                                                                                String        pAccountName,
                                                                                BigInteger    pBrandId,
                                                                                int           pStmtYear) throws Exception
    {
        ArrayList<ssoAccInvBalanceCore> brandItemBalances = new ArrayList<ssoAccInvBalanceCore>();
        BigDecimal bdRevolvingQuantity = new BigDecimal(BigInteger.ZERO);

        boolean bYearArchive = false;
        int iCurrentYear = Util.DateTime.getCurrentYear();
        
        if(iCurrentYear!=pStmtYear)
            bYearArchive = true;
        
        try
        {
            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            String sQueryName = "SsAccInvBrandItemCodes.findItemBalancesByAccIdNBrand";
            if(bYearArchive==true)
                sQueryName = "SsAccInvBrandItemCodes.findItemBalancesByAccIdNBrand_ARCHIVE";

            Query stmt = pem.createNamedQuery(sQueryName, SsAccInvItemStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId            , "VENDOR_ID");

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoAccInvBalanceCore newItemBalance = new ssoAccInvBalanceCore();

                newItemBalance.AccountId = pAccountId;
                newItemBalance.BrandId   = pBrandId;
                newItemBalance.Brandname = Util.Database.getValString(rs.get(i), "BRAND");
                newItemBalance.lastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");
                newItemBalance.ItemCodeId = new BigInteger(Util.Database.getValString(rs.get(i), "UID"));
                newItemBalance.ItemCode  = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                
                newItemBalance.priceEntry  = Util.Database.getValString(rs.get(i), "ENTRY_PRICE");
                newItemBalance.priceSale   = Util.Database.getValString(rs.get(i), "SALE_PRICE");
                newItemBalance.discount    = Util.Database.getValString(rs.get(i), "DISCOUNT");
                newItemBalance.priceId     = Util.Database.getValString(rs.get(i), "PRICE_ID");

                newItemBalance.AccountName = Util.Str.wordNormalize(pAccountName);

                newItemBalance.successRate     = Util.Database.getValString(rs.get(i), "SUCCESS_RATE");
                newItemBalance.velocityStartup = Util.Database.getValString(rs.get(i), "VELOCITY_STARTUP");
                newItemBalance.velocityOverall = Util.Database.getValString(rs.get(i), "VELOCITY_OVERALL");
                
                if(bYearArchive==true)
                {
                    newItemBalance.stats.ytd.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ENTERED")); 
                    newItemBalance.stats.ytd.quantity.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_RETURNED"));
                    newItemBalance.stats.ytd.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_CR_SOLD"));
                    newItemBalance.stats.ytd.quantity.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_CR_REFUND"));
                    newItemBalance.stats.ytd.quantity.netSales = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QNT_NET_CR_SOLD"));
                    newItemBalance.stats.ytd.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ADJ_PLUS"));
                    newItemBalance.stats.ytd.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ADJ_MINUS"));
                }
                else
                {
                    newItemBalance.stats.ytd.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_ENTERED")); 
                    newItemBalance.stats.ytd.quantity.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_RETURNED"));
                    newItemBalance.stats.ytd.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_CR_SOLD"));
                    newItemBalance.stats.ytd.quantity.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_CR_REFUND"));
                    newItemBalance.stats.ytd.quantity.netSales = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_NET_CR_SOLD"));
                    newItemBalance.stats.ytd.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_ADJ_PLUS"));
                    newItemBalance.stats.ytd.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "YTD_QNT_ADJ_MINUS"));
                }
                newItemBalance.stats.revolving.quantity.balance                = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_QNT"));
                newItemBalance.stats.revolving.quantity.balanceManualOverwrite = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_QNT_MNL"));

                if(newItemBalance.stats.ytd.quantity.balanceManualOverwrite.compareTo(BigDecimal.ZERO)>0)
                {
                    bdRevolvingQuantity = newItemBalance.stats.ytd.quantity.balanceManualOverwrite;
                    newItemBalance.stats.revolving.quantity.balance = bdRevolvingQuantity;//overwrite
                }
                else
                {
                    bdRevolvingQuantity = newItemBalance.stats.revolving.quantity.balance;
                }

                newItemBalance.stats.inv.quantity.received       = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_ENTERED"));
                newItemBalance.stats.inv.quantity.sent           = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_RETURNED"));
                newItemBalance.stats.inv.quantity.sold           = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_CR_SOLD"));
                newItemBalance.stats.inv.quantity.refund         = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_CR_REFUND"));
                newItemBalance.stats.inv.quantity.adjPlus        = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_ADJ_PLUS"));
                newItemBalance.stats.inv.quantity.adjMinus       = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_ADJ_MINUS"));
                newItemBalance.stats.inv.quantity.balance        = new BigDecimal(Util.Database.getValString(rs.get(i), "TOT_NET_QNT_INV"));
                
                newItemBalance.stats.revolving.quantity.received       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_QNTY_TOT_ENTERED"));
                newItemBalance.stats.revolving.quantity.sent           = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_QNTY_TOT_RETURNED"));
                newItemBalance.stats.revolving.quantity.sold           = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_QNTY_TOT_CR_SOLD"));
                newItemBalance.stats.revolving.quantity.refund         = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_QNTY_TOT_CR_REFUND"));
                newItemBalance.stats.revolving.quantity.adjPlus        = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_QUANTITY_ADJ_PLUS"));
                newItemBalance.stats.revolving.quantity.adjMinus       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_QUANTITY_ADJ_MINUS"));

                newItemBalance.stats.revolving.base.received            = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_TOT_ENTERED"));
                newItemBalance.stats.revolving.base.sent                = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_TOT_RETURNED"));
                newItemBalance.stats.revolving.base.sold                = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_TOT_CR_SOLD"));
                newItemBalance.stats.revolving.base.refund              = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_TOT_CR_REFUND"));
                newItemBalance.stats.revolving.base.adjPlus             = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_TOT_FIN_ADJ_PLUS"));
                newItemBalance.stats.revolving.base.adjMinus            = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_NET_TOT_FIN_ADJ_MINUS"));

                newItemBalance.stats.revolving.discount.received       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_DISC_TOT_ENTERED"));
                newItemBalance.stats.revolving.discount.sent           = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_DISC_TOT_RETURNED"));
                newItemBalance.stats.revolving.discount.sold           = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_DISC_TOT_CR_SOLD"));
                newItemBalance.stats.revolving.discount.refund         = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_DISC_TOT_CR_REFUND"));
                newItemBalance.stats.revolving.discount.adjPlus        = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_DISC_TOT_FIN_ADJ_PLUS"));
                newItemBalance.stats.revolving.discount.adjMinus       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_DISC_TOT_FIN_ADJ_MINUS"));

                newItemBalance.stats.revolving.surcharge.received      = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_SRCH_TOT_ENTERED"));
                newItemBalance.stats.revolving.surcharge.sent          = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_SRCH_TOT_RETURNED"));
                newItemBalance.stats.revolving.surcharge.sold          = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_SRCH_TOT_CR_SOLD"));
                newItemBalance.stats.revolving.surcharge.refund        = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_SRCH_TOT_CR_REFUND"));
                newItemBalance.stats.revolving.surcharge.adjPlus       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_SRCH_TOT_FIN_ADJ_PLUS"));
                newItemBalance.stats.revolving.surcharge.adjMinus      = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_SRCH_TOT_FIN_ADJ_PLUS"));

                newItemBalance.stats.revolving.tax.received            = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_TAX_TOT_ENTERED"));
                newItemBalance.stats.revolving.tax.sent                = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_TAX_TOT_RETURNED"));
                newItemBalance.stats.revolving.tax.sold                = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_TAX_TOT_CR_SOLD"));
                newItemBalance.stats.revolving.tax.refund              = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_TAX_TOT_CR_REFUND"));
                newItemBalance.stats.revolving.tax.adjPlus             = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_TAX_TOT_FIN_ADJ_PLUS"));
                newItemBalance.stats.revolving.tax.adjMinus            = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_TAX_TOT_FIN_ADJ_MINUS"));

                newItemBalance.stats.revolving.expense.received   = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_EXPENSE_TOT_ENTERED"));
                newItemBalance.stats.revolving.expense.sent       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_EXPENSE_TOT_RETURNED"));
                newItemBalance.stats.revolving.expense.adjPlus    = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_EXPENSE_TOT_FIN_ADJ_PLUS"));
                newItemBalance.stats.revolving.expense.adjMinus   = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_EXPENSE_TOT_FIN_ADJ_MINUS"));

                newItemBalance.stats.revolving.gross.received       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_GROSS_TOT_ENTERED"));
                newItemBalance.stats.revolving.gross.sent           = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_GROSS_TOT_RETURNED"));
                newItemBalance.stats.revolving.gross.sold           = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_GROSS_TOT_CR_SOLD"));
                newItemBalance.stats.revolving.gross.refund         = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_GROSS_TOT_CR_REFUND"));
                newItemBalance.stats.revolving.gross.adjPlus        = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_GROSS_TOT_FIN_ADJ_PLUS"));
                newItemBalance.stats.revolving.gross.adjMinus       = new BigDecimal(Util.Database.getValString(rs.get(i), "REV_GROSS_TOT_FIN_ADJ_MINUS"));

                newItemBalance.stats.ytd.quantity.balance  =    newItemBalance.stats.ytd.quantity.received.
                                                                subtract(newItemBalance.stats.ytd.quantity.sent).
                                                                subtract(newItemBalance.stats.ytd.quantity.sold).
                                                                subtract(newItemBalance.stats.ytd.quantity.adjMinus).
                                                                add(newItemBalance.stats.ytd.quantity.adjPlus).
                                                                add(bdRevolvingQuantity);

                brandItemBalances.add(newItemBalance);

            }

            return brandItemBalances;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoAccInvBalanceCore> calculateOptionsBalance4Account(  EntityManager pem,
                                                                                    boolean       pbCleanMemory,
                                                                                    BigInteger          pAccountId, 
                                                                                    String        pAccountName,
                                                                                    BigInteger          pVendorId,
                                                                                    int           pPageNumber
                                                                                 ) throws Exception
    {
        ArrayList<ssoAccInvBalanceCore> brandItemOptionBalances = new ArrayList<ssoAccInvBalanceCore>();

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
            Query stmt = pem.createNamedQuery("SsAccInvBrandItemCodes.findOptionBalancesByAccIdNBrand", SsAccInvItemStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId           , "VENDOR_ID");
            stmt.SetParameter(index++, iLimit              , "LIMIT");
            stmt.SetParameter(index++, iOffset             , "OFFSET");

            List<List<RowColumn>> rs = stmt.getResultList();
            if (rs.size()>0)
            {
                for (int i=0;i<rs.size();i++)
                {

                    //newBalance.Brandname = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "BRAND"));
                    String sOptUID       = Util.Database.getValString(rs.get(i), "OPT_UID");
                    String sOptGroup     = Util.Database.getValString(rs.get(i), "OPT_GROUP");
                    String sBrand        = "";//Util.Database.getValString(rs.get(i), "BRAND");
                    String sItemCode     = Util.Database.getValString(rs.get(i), "ITEM_CODE");//pItemCode;

                    String sItemQEntered     = Util.Database.getValString(rs.get(i), "ITM_Q_ENTERED");
                    String sItemQReturned    = Util.Database.getValString(rs.get(i), "ITM_Q_RETURNED");
                    String sItemQSold        = Util.Database.getValString(rs.get(i), "ITM_Q_CR_SOLD");
                    String sItemQAdjPlus     = Util.Database.getValString(rs.get(i), "ITM_Q_ADJ_PLUS");
                    String sItemQAdjMinus    = Util.Database.getValString(rs.get(i), "ITM_Q_ADJ_MINUS");

                    String sLastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");
                    //newBalance.Option    = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT"));
                    //String sOptionsAll      = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPTS_ALL"));// THIS IS JSON

                    // CREATING PARENT ROW (ITEM)
                    //----------------------------------------------------------------
                    ssoAccInvBalanceCore newParentBalance =  new ssoAccInvBalanceCore();
                    newParentBalance.AccountId    = pAccountId;
                    newParentBalance.ItemCode     = sItemCode;
                    newParentBalance.lastActivity = sLastActivity;

                    newParentBalance.stats.ytd.quantity.received = new BigDecimal(sItemQEntered);
                    newParentBalance.stats.ytd.quantity.sent     = new BigDecimal(sItemQReturned);
                    newParentBalance.stats.ytd.quantity.sold     = new BigDecimal(sItemQSold);
                    newParentBalance.stats.ytd.quantity.adjPlus  = new BigDecimal(sItemQAdjPlus);
                    newParentBalance.stats.ytd.quantity.adjMinus = new BigDecimal(sItemQAdjMinus);

                    newParentBalance.OptionUID = "";
                    newParentBalance.Option    = "";

                    brandItemOptionBalances.add(newParentBalance);

                    //JsonObject jsOptionsAdjPlus     = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_CR_SOLD")));// THIS IS JSON
                    //JsonObject jsOptionsAdjMinus    = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_CR_SOLD")));// THIS IS JSON

                    // ADDING CHILD ROW (OPTION)
                    //----------------------------------------------------------------
                    String sOptionsAll      = Util.Database.getValString(rs.get(i), "OPTS_ALL");// DON'T NORMALIZE
                    JsonObject jsOptionsEntered  = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT_QNT_ENTERED")));// THIS IS JSON
                    JsonObject jsOptionsReturned = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT_QNT_RETURNED")));// THIS IS JSON
                    JsonObject jsOptionsSold     = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT_QNT_CR_SOLD")));// THIS IS JSON
                    JsonObject jsOptionsAdjNet   = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT_Q_ADJ_NET")));// THIS IS JSON

                    // combine each json
                    // Run in a loop for json for each option add one item 
                    JsonObject jsOptAll = Util.JSON.toJsonObject(sOptionsAll);
                    Set<String> hKeys = Util.JSON.keys(jsOptAll);
                    ArrayList<String> aKeys = new ArrayList<>(hKeys);
                    if(aKeys.size()==0)
                        aKeys.add("");

                    // Adding Options
                    for(String sKey:aKeys)
                    {
                        ssoAccInvBalanceCore newBalance =  new ssoAccInvBalanceCore();

                        newBalance.AccountId    = pAccountId;
                        newBalance.ItemCode     = sItemCode;
                        newBalance.lastActivity = sLastActivity;

                        //-------------------------------------------------------
                        // THIS IS HOW WE FORM OPTION ON VISUAL
                        // Combination of Group and Option
                        // IF(TRIM(OPT.OPTION_1)='',
                        //                          OPT.OPTION_2, 
                        //                          CONCAT(OPT.OPTION_1, ' - ', OPT.OPTION_2)) AS OPT
                        //-------------------------------------------------------
                        newBalance.OptionUID = sOptUID;
                        if (sOptGroup.trim().length()==0)
                        {
                            //newBalance.Option    = Util.Str.wordNormalize(sKey);
                            newBalance.Option    = sKey;//DONT NORMALIZE WORD
                        }
                        else
                        {
                            //newBalance.Option    = sOptGroup + " - " + Util.Str.wordNormalize(sKey);
                            newBalance.Option    = sOptGroup + " - " + sKey;//DONT NORMALIZE
                        }

                        String sQuantityEntered  = Util.JSON.getValue(jsOptionsEntered,  sKey);
                        String sQuantityReturned = Util.JSON.getValue(jsOptionsReturned, sKey);
                        String sQuantitySold     = Util.JSON.getValue(jsOptionsSold,     sKey);
                        //String sQuantityAdjPlus  = Util.JSON.getValue(jsOptionsAdjPlus,  sKey);
                        //String sQuantityAdjMinus = Util.JSON.getValue(jsOptionsAdjMinus, sKey);
                        String sQuantityAdjNet = Util.JSON.getValue(jsOptionsAdjNet, sKey);

                        boolean bQuantityEnteredSkip  = false;
                        boolean bQuantityReturnedSkip = false;
                        boolean bQuantitySoldSkip     = false;
                        boolean bQuantityAdjSkip      = false;

                        if (sQuantityEntered.trim().length()==0)
                        {
                            sQuantityEntered = "-1";
                            bQuantityEnteredSkip = true;
                        }

                        if (sQuantityReturned.trim().length()==0)
                        {
                            sQuantityReturned = "-1";
                            bQuantityReturnedSkip = true;
                        }
                        
                        if (sQuantitySold.trim().length()==0)
                        {
                            sQuantitySold = "-1";
                            bQuantitySoldSkip = true;
                        }

                        /*
                        if (sQuantityAdjPlus.trim().length()==0)
                        {
                            sQuantityAdjPlus = "-1";
                            bQuantitySoldSkip = true;
                        }

                        if (sQuantityAdjMinus.trim().length()==0)
                        {
                            sQuantityAdjMinus = "-1";
                            bQuantitySoldSkip = true;
                        }
                        */
                        if (sQuantityAdjNet.trim().length()==0)
                        {
                            sQuantityAdjNet = "-1";
                            bQuantityAdjSkip = true;
                        }

                        //------------------------------------------------------

                        newBalance.stats.ytd.quantity.received = new BigDecimal(sQuantityEntered);
                        newBalance.stats.ytd.quantity.sent     = new BigDecimal(sQuantityReturned);
                        newBalance.stats.ytd.quantity.sold     = new BigDecimal(sQuantitySold);
                        newBalance.stats.ytd.quantity.adjPlus  = new BigDecimal(sQuantityAdjNet);
                        //newBalance.stats.ytd.quantity.adjPlus  = new BigDecimal(sQuantityAdjPlus);
                        //newBalance.stats.ytd.quantity.adjMinus = new BigDecimal(sQuantityAdjMinus);

                        brandItemOptionBalances.add(newBalance);
                    }

                }
            }

            return brandItemOptionBalances;

            /*
            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            Query stmt = pem.createNamedQuery("SsAccInvBrands.findOptionBalancesByAccIdNBrand", SsAccInvVendorStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId           , "BRAND_ID");
            stmt.SetParameter(index++, pItemCodeId         , "ITEM_CODE_ID");
            //stmt.SetParameter(index++, pYear               , "YEAR");

            List<List<RowColumn>> rs = stmt.getResultList();
            if (rs.size()>0)
            {
                for (int i=0;i<rs.size();i++)
                {
                    ssoAccInvBalanceCore newBalance =  new ssoAccInvBalanceCore();

                    newBalance.AccountId = pAccountId;
                    //newBalance.Brandname = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "BRAND"));
                    newBalance.Brandname = Util.Database.getValString(rs.get(i), "BRAND");
                    newBalance.ItemCode  = pItemCode;
                    newBalance.lastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");
                    newBalance.Option    = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT"));

                    newBalance.quantity.received.add(new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_ENTERED")));
                    newBalance.quantity.returned.add(new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_RETURNED")));
                    newBalance.quantity.sold.add(new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_CR_SOLD")));
                    //newBalance.quantity.received = Long.parseLong(Util.Database.getValString(rs.get(i), "QNT_ENTERED"));
                    //newBalance.quantity.returned = Long.parseLong(Util.Database.getValString(rs.get(i), "QNT_RETURNED"));
                    //newBalance.quantity.returned = Long.parseLong(Util.Database.getValString(rs.get(i), "QNT_CR_SOLD"));

                    brandItemOptionBalances.add(newBalance);
                }
            }

            return brandItemOptionBalances;
            */
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoAccInvBalanceCore> calculateBrandBalances(   EntityManager            pem,
                                                                            BigInteger                     pAccountId,
                                                                            String                   pAccountName,
                                                                            ArrayList<ssoBrand>      pBrands,
                                                                            long                     pYear) throws Exception
    {

        ArrayList<ssoAccInvBalanceCore> brandBalances = new ArrayList<ssoAccInvBalanceCore>();

        try
        {
            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 

            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // IMPORTANT: THIS COMES FROM CACHE
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            Query stmt = pem.createNamedQuery("SsAccInvBrands.findBrandBalancesByAccIdNBrand", SsAccInvVendorStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            //stmt.SetParameter(index++, pBrand.Id           , "VENDOR_ID");
            //stmt.SetParameter(index++, pYear               , "YEAR");

            List<List<RowColumn>> rs = stmt.getResultList();
            for (ssoBrand brandN:pBrands)
            {
                for(int i=0;i<rs.size();i++)
                {
                    ssoAccInvBalanceCore newItemBalance = new ssoAccInvBalanceCore();

                    String sVendorId = Util.Database.getValString(rs.get(i), "VENDOR_ID");
                    BigInteger   lVendorId = new BigInteger(sVendorId);

                    //if (lVendorId==brandN.Id)
                    if (lVendorId.compareTo(brandN.Id)==0)
                    {
                        newItemBalance.AccountId    = pAccountId;
                        newItemBalance.AccountName  = pAccountName;
                        newItemBalance.BrandId      = lVendorId;
                        newItemBalance.Brandname    = brandN.name;
                        newItemBalance.lastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");

                        newItemBalance.stats.revolving.net.balance = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE"));
                        newItemBalance.stats.revolving.quantity.balance = new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_REV_NET"));
                        newItemBalance.stats.ytd.net.balance       = new BigDecimal(Util.Database.getValString(rs.get(i), "BALANCE"));

                        newItemBalance.stats.ytd.net.received   = new BigDecimal(Util.Database.getValString(rs.get(i), "TOTAL_ENTERED"));//ytd = year to date 
                        newItemBalance.stats.ytd.net.sent       = new BigDecimal(Util.Database.getValString(rs.get(i), "TOTAL_RETURNED")); 
                        newItemBalance.stats.ytd.net.sold       = new BigDecimal(Util.Database.getValString(rs.get(i), "TOTAL_CR_SOLD")); 
                        newItemBalance.stats.ytd.net.paymentNet = new BigDecimal(Util.Database.getValString(rs.get(i), "TOTAL_PAYMENT"));

                        newItemBalance.stats.ytd.quantity.balance   = new BigDecimal(Util.Database.getValString(rs.get(i), "NET_QUANTITY"));
                        newItemBalance.stats.ytd.quantity.received  = new BigDecimal(Util.Database.getValString(rs.get(i), "QUANTITY_ENTERED"));
                        newItemBalance.stats.ytd.quantity.sent      = new BigDecimal(Util.Database.getValString(rs.get(i), "QUANTITY_RETURNED"));
                        newItemBalance.stats.ytd.quantity.sold      = new BigDecimal(Util.Database.getValString(rs.get(i), "QUANTITY_CR_SOLD"));
                        newItemBalance.stats.ytd.quantity.refund    = new BigDecimal(Util.Database.getValString(rs.get(i), "QUANTITY_CR_REFUND"));

                        brandBalances.add(newItemBalance);
                    }

                }

            }

            return brandBalances;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoAccStmtCore> getStatement4Brand( EntityManager pem, 
                                                                boolean       pbCleanMemory,
                                                                BigInteger    pUserId,
                                                                BigInteger    pAccId,
                                                                BigInteger    pBrandId,
                                                                int           pYear,
                                                                int           piStartRowIndex,
                                                                boolean       pbFullRows
                                                                ) throws Exception
    {
        //SELECT LOG.INSERTDATE, LOG.QUANTITY, LOG.AMOUNT, LOG.TXN_TYPE, LOG.REVOLVING_BALANCE, LOG.REVOLVING_QUANTITY, LOG.NEW_QUANTITY_ENTERED, LOG.NEW_QUANTITY_RETURNED, LOG.NEW_QUANTITY_CR_SOLD, LOG.NEW_BALANCE_ENTERED, LOG.NEW_BALANCE_RETURNED, LOG.NEW_BALANCE_CR_SOLD FROM ss_acc_inv_brands_log LOG WHERE STAT = 1 AND FINANCIAL_YEAR = 2021 AND BRAND = 'MODIVA'
        ArrayList<ssoAccStmtCore> brandStatement = new ArrayList<ssoAccStmtCore>();

        try
        {
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);
            else
                pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);

            pem.setRowStartIndex(piStartRowIndex);

            if(pbCleanMemory==true)
                pem.flush();//this will execute flush before executing the query ahead

            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 

            // CALL SP_EOD_INV_TXN_2_EOD(20210410); this will carry the transactions to eod tables
            // the statement will be read from eod tables as well as txn tables.
            // The order of the records will be in eod then txn table order.
            // In other words, read eod records to the end. At the end of eod table
            // read from txn tables. TXN table will only contain today's rows.
            // ss_acc_brand table will have last inventory entry data.
            // this way the system will know should need to read from txn  
            Query stmt = pem.createNamedQuery("SsStmInvStatements.getVendorStatement4Account", SsStmInvStatements.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId         , "VENDOR_ID");
            stmt.SetParameter(index++, pYear            , "STMT_YEAR");

            BigDecimal bdBalance = new BigDecimal(BigInteger.ZERO);
            //BigDecimal bdTotal_Credit = new BigDecimal(BigInteger.ZERO);
            //BigDecimal bdTotalDebit   = new BigDecimal(BigInteger.ZERO);

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoAccStmtCore newStmtLine = new ssoAccStmtCore();

                /*
                if (pAccId!=-1)
                    newStmtLine.accName = "";
                else
                    newStmtLine.accName = Util.Database.getValString(rs.get(i), "PROFILENAME");
                */

                newStmtLine.title = Util.Database.getValString(rs.get(i), "TITLE");
                newStmtLine.isRevolving =  Util.Database.getValString(rs.get(i), "IS_REVOLVING");
                newStmtLine.hasEdit     =  Util.Database.getValString(rs.get(i), "HAS_EDIT");
                newStmtLine.editDate    =  Util.Database.getValString(rs.get(i), "EDIT_DATE");

                if(newStmtLine.isRevolving.equals("Y")==true)
                {

                    newStmtLine.title = "Revolving";//from last year
                    //newStmtLine.title = "{" + 
                    //                    Util.Str.QUOTE("en") + ":" + Util.Str.QUOTE("Revolving") + "," + 
                    //                    Util.Str.QUOTE("tr") + ":" + Util.Str.QUOTE("Devir") + 
                    //                    "}";//from last year
                    newStmtLine.note  = "Year of " + Util.DateTime.GetDateTime_s("YYYY");

                    bdBalance = new BigDecimal(Util.Database.getValString(rs.get(i), "BALANCE"));
                }

                newStmtLine.txnCode    = Util.Database.getValString(rs.get(i), "TXN_CODE");

                if(newStmtLine.txnCode.length()>=3)
                {
                    String sOTC = newStmtLine.txnCode.trim().substring(2, 3);

                    //if(sOTC.equals(txnDefs.TXN_CODE_OTC_INVENTORY)==true)
                        newStmtLine.billId     = Util.Database.getValString(rs.get(i), "TXN_ID");//ss_eod_inv_txn.uid

                    //if(sOTC.equals(txnDefs.TXN_CODE_OTC_PAYMENT)==true)
                        newStmtLine.paymentId     = Util.Database.getValString(rs.get(i), "TXN_ID");//ss_txn_vendor_payments.id

                    /*
                    if(newStmtLine.txnCode.trim().substring(0, 3).equals(gCMMN_TXN_CODE_GROUP_ITEM)==true)//inventory
                        newStmtLine.billId     = Util.Database.getValString(rs.get(i), "ORG_UID");//ss_eod_inv_txn.uid

                    if(newStmtLine.txnCode.trim().substring(0, 3).equals(gCMMN_TXN_CODE_GROUP_PAYMENT)==true)//inventory
                        newStmtLine.paymentId     = Util.Database.getValString(rs.get(i), "TXN_PYM_ID");//ss_txn_vendor_payments.id
                    */
                }
                else
                {
                    newStmtLine.billId     = Util.Database.getValString(rs.get(i), "ORG_UID");//default
                }

                newStmtLine.accountId   = Util.Database.getValString(rs.get(i), "ACC_ID");//branch NAME
                newStmtLine.accountName = Util.Database.getValString(rs.get(i), "PROFILENAME");//branch NAME
                newStmtLine.name     = Util.Database.getValString(rs.get(i), "BRAND");//VENDOR NAME
                newStmtLine.stmtDate = Util.Database.getValString(rs.get(i), "TXN_DATE");
                newStmtLine.stmtTime = Util.Database.getValString(rs.get(i), "INSERTDATE");
                newStmtLine.releaseDate = Util.Database.getValString(rs.get(i), "RELEASE_DATE");
                //newStmtLine.quantity = Util.Database.getValString(rs.get(i), "QUANTITY_TOTAL");
                newStmtLine.quantity = "0";
                newStmtLine.amount_c   = Util.Database.getValString(rs.get(i), "AMOUNT_CREDIT");
                newStmtLine.amount_d   = Util.Database.getValString(rs.get(i), "AMOUNT_DEBIT");
                newStmtLine.txnEffect  = Util.Database.getValString(rs.get(i), "TXN_EFFECT");


                newStmtLine.txnName_EN = Util.Database.getValString(rs.get(i), "TXN_NAME_EN");
                newStmtLine.txnName_TR = Util.Database.getValString(rs.get(i), "TXN_NAME_TR");
                newStmtLine.txnName    = "{" + 
                                           "\"en\":" + Util.Str.QUOTE(newStmtLine.txnName_EN) + "," + 
                                           "\"tr\":" + Util.Str.QUOTE(newStmtLine.txnName_TR) + 
                                         "}";

                BigDecimal bdAmountCredit = new BigDecimal(newStmtLine.amount_c);
                BigDecimal bdAmountDebit = new BigDecimal(newStmtLine.amount_d);

                if(newStmtLine.txnEffect.toUpperCase().equals("D")==true)
                {
                    // DEBIT (+) = PAYMENTS / INV IN
                    bdBalance    = bdBalance.add(bdAmountDebit);
                    //bdTotalDebit = bdTotalDebit.subtract(bdAmountDebit);
                }
                else if(newStmtLine.txnEffect.toUpperCase().equals("C")==true)
                {
                    // CREDIT (-) = PAYMENTS / INV OUT 
                    bdBalance     = bdBalance.subtract(bdAmountCredit);
                    //bdTotal_Credit = bdTotal_Credit.subtract(bdAmountCredit);
                }
                else
                {
                    // JUST INVENTORY ENTRIES (NO CHANGE ON BALANCE)
                }
                
                newStmtLine.balance  = bdBalance.toString(); //Util.Database.getValString(rs.get(i), "BALANCE");
                //newStmtLine.amount_c = bdTotal_Credit.toString();
                //newStmtLine.amount_d = bdTotalDebit.toString();
                //newStmtLine.txnType  = Util.Database.getValString(rs.get(i), "TXN_TYPE");

                String revolvingBalance   = "";//Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE");
                String revolvingQuantity  = "";//Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY");

                String quantityEntered   = "";//Util.Database.getValString(rs.get(i), "NEW_QUANTITY_ENTERED");
                String quantityReturned  = "";//Util.Database.getValString(rs.get(i), "NEW_QUANTITY_RETURNED");
                String quantitySold      = "";//Util.Database.getValString(rs.get(i), "NEW_QUANTITY_CR_SOLD");

                //BigDecimal balanceSold      = new BigDecimal(Util.Database.getValString(rs.get(i), "NEW_BALANCE_CR_SOLD"));
                //BigDecimal balanceEntered   = new BigDecimal(Util.Database.getValString(rs.get(i), "DEBIT"));
                //BigDecimal balanceReturned  = new BigDecimal(Util.Database.getValString(rs.get(i), "CREDIT"));

                //BigDecimal balance = new BigDecimal(BigInteger.ZERO);
                //balance = balanceEntered.subtract(balanceReturned);

                //newStmtLine.sumCredit = balanceReturned.toString();
                //newStmtLine.sumDebit  = balanceEntered.toString();
                //newStmtLine.balance   = balance.toString();

                /*
                if (newStmtLine.txnType.equals(INV_TXN_TYPE_NEW_ENTRY)==true)
                {
                    newStmtLine.txnEffect = INV_TXN_EFFECT_DEBIT;//borc
                }
                else if (newStmtLine.txnType.equals(INV_TXN_TYPE_RETURN)==true)
                {
                    newStmtLine.txnEffect = INV_TXN_EFFECT_CREDIT;//alacak
                }
                else if (newStmtLine.txnType.equals(INV_TXN_TYPE_FIN_ADJ)==true)
                {
                    //if amount larger than 0 or minus 
                }
                else if (newStmtLine.txnType.equals(INV_TXN_TYPE_CR_SOLD)==true)
                {
                    // Shouldn't fall here
                }
                else 
                {
                    // Unknown 
                }
                */

                brandStatement.add(newStmtLine);
            }

            return brandStatement;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoVendorStatementBills> getStatementBills4Brand(   EntityManager pem, 
                                                                                BigInteger    pUserId,
                                                                                BigInteger    pAccId,
                                                                                BigInteger    pBrandId,
                                                                                int           pLastNYear
                                                                            ) throws Exception
    {
        //SELECT LOG.INSERTDATE, LOG.QUANTITY, LOG.AMOUNT, LOG.TXN_TYPE, LOG.REVOLVING_BALANCE, LOG.REVOLVING_QUANTITY, LOG.NEW_QUANTITY_ENTERED, LOG.NEW_QUANTITY_RETURNED, LOG.NEW_QUANTITY_CR_SOLD, LOG.NEW_BALANCE_ENTERED, LOG.NEW_BALANCE_RETURNED, LOG.NEW_BALANCE_CR_SOLD FROM ss_acc_inv_brands_log LOG WHERE STAT = 1 AND FINANCIAL_YEAR = 2021 AND BRAND = 'MODIVA'
        ArrayList<ssoAccStmtCore> brandStatement = new ArrayList<ssoAccStmtCore>();

        try
        {
            /*
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);
            else
                pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);

            pem.setRowStartIndex(piStartRowIndex);

            if(pbCleanMemory==true)
                pem.flush();//this will execute flush before executing the query ahead
            */
            pem.flush();

            ArrayList<ssoVendorStatementBills> bills = new ArrayList<ssoVendorStatementBills>();

            Query stmt = pem.createNamedQuery("SsStmInvStatements.getVendorStatementsLastNYear", SsStmInvStatements.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId         , "VENDOR_ID");
            stmt.SetParameter(index++, pLastNYear       , "STMT_YEAR");

            BigDecimal bdBalance = new BigDecimal(BigInteger.ZERO);
            //BigDecimal bdTotal_Credit = new BigDecimal(BigInteger.ZERO);
            //BigDecimal bdTotalDebit   = new BigDecimal(BigInteger.ZERO);

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoVendorStatementBills billN = new ssoVendorStatementBills();

                billN.txnCode   = Util.Database.getValString(rs.get(i), "TXN_CODE");

                if(billN.txnCode.substring(0,3).equals(txnDefs.TXN_CODE_PAYMENT_RECEIVED_CARD.substring(0,3))==true)
                    billN.uid       = Util.Database.getValString(rs.get(i), "TXN_ID");
                else
                    billN.uid       = Util.Database.getValString(rs.get(i), "TXN_EOD_ID");

                billN.txnType   = Util.Database.getValString(rs.get(i), "TXN_TYPE");

                billN.txnDate  = Util.Database.getValString(rs.get(i), "TXN_DATE");
                billN.txnEffect = Util.Database.getValString(rs.get(i), "TXN_EFFECT");
                billN.balance   = Util.Database.getValString(rs.get(i), "BALANCE");

                bills.add(billN);
            }

            return bills;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean recalculateVendorBalance(EntityManager    pem, 
                                                   BigInteger             pUserId,
                                                   BigInteger             pAccId,
                                                   BigInteger             pVendorId) throws Exception
    {
        try
        {
            //long lStartDateTime = Util.DateTime.GetDateTime_l("yyyy0101000000000");//year begining ?
            //long lEndDateTime   = Util.DateTime.GetDateTime_l();
            long lStartDateTime   = Util.DateTime.GetDateTime_l("yyyyMMdd");

            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_UPDATE_VENDOR_N_STATS");
            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_UPDATE_VENDOR_N_ITEM_STATS");// THIS CALCULATES REMAINING TIME FOR
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_RECALC_VENDOR_STATS");// THIS CALCULATES REMAINING TIME FOR

            SP.registerStoredProcedureParameter("P_ACC_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"       , Long.class         , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_BYUSER"          , String.class       , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_EOD_DATE"        , Long.class         , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_START_DATE"      , Long.class         , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_END_DATE"        , Long.class         , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_RECALC"          , String.class         , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_EOD"             , String.class         , ParameterMode.IN);// N

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId                      , "P_ACC_ID");
            SP.SetParameter(Colindex++, pVendorId                   , "P_VENDOR_ID");
            //SP.SetParameter(Colindex++, pUserId.toString()          , "P_BYUSER");
            //SP.SetParameter(Colindex++, lStartDateTime              , "P_EOD_DATE");
            //SP.SetParameter(Colindex++, "N"                       , "P_EOD");
            //SP.SetParameter(Colindex++, lEndDateTime              , "P_END_DATE");
            //SP.SetParameter(Colindex++, "Y"                       , "P_RECALC");

            SP.execute();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static ArrayList<ssoVendorSalesSummary> getVendorSalesSummaryByDate( EntityManager pem, 
                                                                                BigInteger          pUserId,
                                                                                BigInteger          pAccId,
                                                                                BigInteger          pVendorId,
                                                                                String              pItemCode,
                                                                                int                 pYear,
                                                                                int                 pPageNumber) throws Exception
    {
        ArrayList<ssoVendorSalesSummary> summary = new ArrayList<ssoVendorSalesSummary>();
        //long lLastTxnDate = pLastTxnDate;
        //if(pLastTxnDate==0)
        //    lLastTxnDate = 99999999;//YYYYMMDD

        int iRowPerPage = 10;//100;//DEFAULT
        int iPageIndex  = pPageNumber;
        //int iOffset     = iPageIndex *  iRowPerPage;//skip first N records
        //int iLimit      = (iPageIndex+1) * iRowPerPage;//50 per page

        ssoDBRowLimits rowLimits = new ssoDBRowLimits();
        rowLimits = Util.Database.calculateRowLimits(iRowPerPage, pPageNumber);
        int iOffset     = rowLimits.offset;
        int iLimit      = rowLimits.limit;

        try
        {
            String sQuery = "SELECT "
                                + "TXN_DATE, "
                                + "ROUND(TOTAL_QUANTITY_PURCHASE,2) AS TOTAL_QUANTITY_PURCHASE, "
                                + "ROUND(TOTAL_QUANTITY_REFUND,2) AS TOTAL_QUANTITY_REFUND, "
                                + "ROUND(TOTAL_QUANTITY_NET,2) AS TOTAL_QUANTITY_NET, "
                                + "TOTAL_NET_PRICE_TAG, "
                                + "TOTAL_NET_PRICE_GIVEN, "
                                + "TOTAL_NET_PRICE_SOLD, "
                                + "SUMMARY "
                         + "FROM ss_eod_vendor_sales_summary "
                         + "WHERE "
                         + "STAT = 1 "
                         + "AND "
                         + "ACCOUNT_ID = ? "
                         + "AND "
                         + "VENDOR_ID = ? ";

            if(pItemCode.trim().length()>0)
            {
                sQuery += "AND " +
                          "JSON_EXTRACT(SUMMARY, CONCAT('$.', ?)) IS NOT NULL ";
            }

            sQuery +=    "AND "
                         //+ "TXN_YEAR = ? "
                         //+ "AND "
                         //+ "TXN_DATE < ? "//towards older data
                         + "SUBSTR(TXN_DATE,1,4) = ? "
                         + "ORDER BY TXN_DATE DESC "
                         //+ "LIMIT 50";
                         + "LIMIT ? OFFSET ?";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId        , "VENDOR_ID");

            if(pItemCode.trim().length()>0)
                stmt.SetParameter(index++, pItemCode        , "ITEM_CODE");

            stmt.SetParameter(index++, pYear                , "YEAR");
            //stmt.SetParameter(index++, lLastTxnDate       , "TXN_DATE");

            stmt.SetParameter(index++, iLimit              , "LIMIT");
            stmt.SetParameter(index++, iOffset             , "OFFSET");

            BigDecimal bdBalance = new BigDecimal(BigInteger.ZERO);

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoVendorSalesSummary smryN = new ssoVendorSalesSummary();

                smryN.date      = Util.Database.getValString(rs.get(i), "TXN_DATE");
                smryN.key       = smryN.date;

                smryN.qPurchase = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY_PURCHASE");
                smryN.qRefund   = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY_REFUND");
                smryN.qNet      = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY_NET");

                smryN.tPriceTag     = Util.Database.getValString(rs.get(i), "TOTAL_NET_PRICE_TAG");
                smryN.tPriceGiven   = Util.Database.getValString(rs.get(i), "TOTAL_NET_PRICE_GIVEN");
                smryN.tPriceSold    = Util.Database.getValString(rs.get(i), "TOTAL_NET_PRICE_SOLD");

                BigDecimal bdTotPriceTagTotal   = new BigDecimal(smryN.tPriceTag);
                BigDecimal bdTotPriceGivenTotal = new BigDecimal(smryN.tPriceGiven);
                BigDecimal bdTotPriceSoldTotal  = new BigDecimal(smryN.tPriceSold);

                BigDecimal bdtTotItemDiscRate      = new BigDecimal(BigInteger.ZERO);
                BigDecimal bdtTotCounterDiscRate   = new BigDecimal(BigInteger.ZERO);
                if(bdTotPriceTagTotal.compareTo(BigDecimal.ZERO)>0)
                {
                    bdtTotItemDiscRate = bdTotPriceTagTotal.subtract(bdTotPriceGivenTotal).divide(bdTotPriceTagTotal, RoundingMode.HALF_EVEN);
                }
                
                if(bdTotPriceGivenTotal.compareTo(BigDecimal.ZERO)>0)
                {
                    bdtTotCounterDiscRate = bdTotPriceGivenTotal.subtract(bdTotPriceSoldTotal).divide(bdTotPriceGivenTotal, RoundingMode.HALF_EVEN);
                }

                if(bdtTotItemDiscRate.compareTo(BigDecimal.ZERO)>0)
                    smryN.itemDiscount    = bdtTotItemDiscRate.toString();
                
                if(bdtTotCounterDiscRate.compareTo(BigDecimal.ZERO)>0)
                    smryN.counterDiscount = bdtTotCounterDiscRate.toString();

                smryN.summary       = Util.Database.getValString(rs.get(i), "SUMMARY");
                summary.add(smryN);
                
                // Adding Subs
                //------------------------------------------------------------------
                JsonObject jsoSalesSummary = Util.JSON.toJsonObject(smryN.summary);
                
                Set<String> aKeys = Util.JSON.keys(jsoSalesSummary);
                for(String sItemCode:aKeys)
                {
                    if(sItemCode.trim().equals("*")==false)
                    {
                        ssoVendorSalesSummary smryNItemCode = new ssoVendorSalesSummary();

                        JsonObject jsoVals = jsoSalesSummary.getAsJsonObject(sItemCode);

                        smryNItemCode.key       = smryN.key + "-" + sItemCode;
                        smryNItemCode.parentKey = smryN.key;

                        smryNItemCode.date      = sItemCode;
                        smryNItemCode.qPurchase = jsoVals.get("qp").toString();
                        smryNItemCode.qRefund   = jsoVals.get("qr").toString();
                        smryNItemCode.qNet      = jsoVals.get("qn").toString();

                        String stPrıceTag     = jsoVals.get("ptn").toString();//price tag 
                        String stPriceGiven   = jsoVals.get("pgn").toString(); //price given net (by teller)
                        String stPriceSold    = jsoVals.get("psn").toString(); //price sold net  (on counter)

                        smryNItemCode.tPriceTag     = jsoVals.get("ptn").toString();//price tag 
                        smryNItemCode.tPriceGiven   = jsoVals.get("pgn").toString(); //price given net (by teller)
                        smryNItemCode.tPriceSold    = jsoVals.get("psn").toString(); //price sold net  (on counter)

                        BigDecimal bdtPriceTagTotal   = new BigDecimal(stPrıceTag);
                        BigDecimal bdtPriceGivenTotal = new BigDecimal(stPriceGiven);
                        BigDecimal bdtPriceSoldTotal   = new BigDecimal(stPriceSold);

                        BigDecimal bdtItemDiscRate   = new BigDecimal(BigInteger.ZERO);
                        bdtItemDiscRate = bdtPriceTagTotal.subtract(bdtPriceGivenTotal).divide(bdtPriceTagTotal, RoundingMode.HALF_EVEN);

                        BigDecimal bdtCounterDiscRate   = new BigDecimal(BigInteger.ZERO);
                        bdtCounterDiscRate = bdtPriceGivenTotal.subtract(bdtPriceSoldTotal).divide(bdtPriceGivenTotal, RoundingMode.HALF_EVEN);

                        if(bdtItemDiscRate.compareTo(BigDecimal.ZERO)>0)
                            smryNItemCode.itemDiscount    = bdtItemDiscRate.toString();

                        if(bdtCounterDiscRate.compareTo(BigDecimal.ZERO)>0)
                            smryNItemCode.counterDiscount = bdtCounterDiscRate.toString();

                        summary.add(smryNItemCode);
                    }
                }
            }

            return summary;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoVendorInfo getVendorProfile(EntityManager       pem, 
                                                 boolean             pbResetMemory,
                                                 BigInteger          pUserId,
                                                 BigInteger          pAccId,
                                                 BigInteger          pVendorId,
                                                 int                 piStmtYear) throws Exception
    {
        try
        {
            ssoVendorInfo vendorInfo = new ssoVendorInfo();

            if(pbResetMemory==true)
                pem.flush();

            boolean bYearArchive = false;
            int iCurrentYear = Util.DateTime.getCurrentYear();

            if(iCurrentYear!=piStmtYear)
                bYearArchive = true;

            String sQueryName = "SsAccInvBrands.getVendorPageParams";
            if(bYearArchive==true)
            {
                sQueryName = "SsAccInvBrands.getVendorPageParams_ARCHIVE";
            }

            Query stmt = pem.createNamedQuery(sQueryName, SsAccInvVendorStats.class);
            int index = 1;
            //stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");//VENDERS ARE GATHERED UNDER USER
            stmt.SetParameter(index++, pUserId          , "USER_ID");
            stmt.SetParameter(index++, pVendorId        , "VENDOR_ID");

            BigDecimal bdBalance = new BigDecimal(BigInteger.ZERO);

            
            List<List<RowColumn>> rs = stmt.getResultList();
            if(rs.size()>0)  
            {
                ssoAccStmtCore newStmtLine = new ssoAccStmtCore();

                vendorInfo.name                 = Util.Str.wordNormalize(Util.Database.getValString(rs.get(0), "BRAND"));
                vendorInfo.city                 = Util.Database.getValString(rs.get(0), "CITY");
                vendorInfo.revolvingBalance     = Util.Database.getValString(rs.get(0), "REVOLVING_BALANCE");

                vendorInfo.balance              = Util.Database.getValString(rs.get(0), "BALANCE");
                vendorInfo.ytdNetTotal          = Util.Database.getValString(rs.get(0), "YTD_NET_TOTAL");

                vendorInfo.categorySummary      = Util.Database.getValString(rs.get(0), "CATEGORY_SUMMARY");

                // YTDS + CUMS
                //-------------------------------------------------------------------------------------------
                if(bYearArchive==false)
                {
                    vendorInfo.ytdNetTaxEntered     = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_ENTERED");
                    vendorInfo.ytdNetTaxReturned    = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_RETURNED");
                    vendorInfo.ytdNetTaxSold        = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_CR_SOLD");
                    vendorInfo.ytdNetTaxRefund      = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_CR_REFUND");
                    vendorInfo.ytdNetTax            = Util.Database.getValString(rs.get(0), "YTD_NET_TAX");
                    vendorInfo.ytdNetTaxFinAdjPlus  = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_FIN_ADJ_PLUS");
                    vendorInfo.ytdNetTaxFinAdjMinus = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_FIN_ADJ_MINUS");

                    vendorInfo.itemsTaxEntered_Cumulative            = Util.Database.getValString(rs.get(0), "CUMULATIVE_TAX_ENTERED");
                    vendorInfo.itemsTaxReturned_Cumulative           = Util.Database.getValString(rs.get(0), "CUMULATIVE_TAX_RETURNED");
                    vendorInfo.itemsTaxSold_Cumulative               = Util.Database.getValString(rs.get(0), "CUMULATIVE_TAX_CR_SOLD");
                    vendorInfo.itemsTaxRefund_Cumulative             = Util.Database.getValString(rs.get(0), "CUMULATIVE_TAX_CR_REFUND");
                    vendorInfo.itemsTaxFinAdjPlus_Cumulative         = Util.Database.getValString(rs.get(0), "CUMULATIVE_FIN_ADJ_PLUS_TAX");
                    vendorInfo.itemsTaxFinAdjMinus_Cumulative        = Util.Database.getValString(rs.get(0), "CUMULATIVE_FIN_ADJ_MINUS_TAX");

                    // YTDS + CUMS
                    //-------------------------------------------------------------------------------------------
                    vendorInfo.ytdNetExpenseEntered     = Util.Database.getValString(rs.get(0), "YTD_NET_EXPENSE_ENTERED");
                    vendorInfo.ytdNetExpenseReturned    = Util.Database.getValString(rs.get(0), "YTD_NET_EXPENSE_RETURNED");
                    //vendorInfo.ytdNetTaxSold        = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_CR_SOLD");
                    //vendorInfo.ytdNetTaxRefund      = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_CR_REFUND");
                    vendorInfo.ytdNetExpense            = Util.Database.getValString(rs.get(0), "YTD_NET_EXPENSE");
                    vendorInfo.ytdNetExpenseFinAdjPlus  = Util.Database.getValString(rs.get(0), "YTD_NET_EXPENSE_FIN_ADJ_PLUS");
                    vendorInfo.ytdNetExpenseFinAdjMinus = Util.Database.getValString(rs.get(0), "YTD_NET_EXPENSE_FIN_ADJ_MINUS");

                    vendorInfo.itemsExpenseEntered_Cumulative        = Util.Database.getValString(rs.get(0), "CUMULATIVE_EXPENSE_ENTERED");
                    vendorInfo.itemsExpenseReturned_Cumulative       = Util.Database.getValString(rs.get(0), "CUMULATIVE_EXPENSE_RETURNED");
                    //vendorInfo.itemsExpenseSold_Cumulative           = Util.Database.getValString(rs.get(0), "CUMULATIVE_EXPENSE_CR_SOLD");
                    //vendorInfo.itemsExpenseRefund_Cumulative         = Util.Database.getValString(rs.get(0), "CUMULATIVE_EXPENSE_CR_REFUND");
                    vendorInfo.itemsExpenseFinAdjPlus_Cumulative     = Util.Database.getValString(rs.get(0), "CUMULATIVE_FIN_ADJ_PLUS_EXPENSE");
                    vendorInfo.itemsExpenseFinAdjMinus_Cumulative    = Util.Database.getValString(rs.get(0), "CUMULATIVE_FIN_ADJ_MINUS_EXPENSE");

                    // YTDS + CUMS
                    //-------------------------------------------------------------------------------------------
                    vendorInfo.itemsNetQuantityEntered_YTD          = Util.Database.getValString(rs.get(0), "YTD_QUANTITY_ENTERED");
                    vendorInfo.itemsNetQuantityReturned_YTD         = Util.Database.getValString(rs.get(0), "YTD_QUANTITY_RETURNED");
                    vendorInfo.itemsNetQuantitySold_YTD             = Util.Database.getValString(rs.get(0), "YTD_QUANTITY_CR_SOLD");
                    vendorInfo.itemsNetQuantityRefund_YTD       = Util.Database.getValString(rs.get(0), "YTD_QUANTITY_CR_REFUND");
                    vendorInfo.itemsNetQuantityFinAdjPlus_YTD       = Util.Database.getValString(rs.get(0), "YTD_Q_ADJ_PLUS");
                    vendorInfo.itemsNetQuantityFinAdjMinus_YTD       = Util.Database.getValString(rs.get(0), "YTD_Q_ADJ_MINUS");
                    vendorInfo.itemsNetQuantity_YTD             = Util.Database.getValString(rs.get(0), "YTD_NET_QUANTITY");

                    vendorInfo.itemsNetQuantityEntered_Cumulative   = Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_ENTERED");
                    vendorInfo.itemsNetQuantityReturned_Cumulative  = Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_RETURNED");
                    vendorInfo.itemsNetQuantitySold_Cumulative      = Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_CR_SOLD");
                    vendorInfo.itemsNetQuantityRefund_Cumulative= Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_CR_REFUND");
                    vendorInfo.itemsNetQuantityFinAdjPlus_Cumulative= Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_ADJ_PLUS");
                    vendorInfo.itemsNetQuantityFinAdjMinus_Cumulative= Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_ADJ_MINUS");
                    vendorInfo.itemsNetQuantity_Cumulative      = Util.Database.getValString(rs.get(0), "CUMULATIVE_QUANTITY_NET_TOTAL");

                    // YTDS + CUMS
                    //-------------------------------------------------------------------------------------------
                    //amount
                    vendorInfo.itemsReceivedAmount_YTD          = Util.Database.getValString(rs.get(0), "YTD_GROSS_ENTERED");
                    vendorInfo.itemsSentAmount_YTD              = Util.Database.getValString(rs.get(0), "YTD_GROSS_RETURNED");
                    vendorInfo.itemsFinAdjPlusNetAmount_YTD     = Util.Database.getValString(rs.get(0), "YTD_FIN_ADJ_PLUS_NET_TOTAL");
                    vendorInfo.itemsFinAdjMinusNetAmount_YTD    = Util.Database.getValString(rs.get(0), "YTD_FIN_ADJ_MINUS_NET_TOTAL");
                    vendorInfo.itemsSoldAmount_YTD              = Util.Database.getValString(rs.get(0), "YTD_RAW_CR_SOLD");
                    vendorInfo.itemsRefundAmount_YTD            = Util.Database.getValString(rs.get(0), "YTD_RAW_CR_REFUND");

                    vendorInfo.itemsReceivedAmount_Cumulative   = Util.Database.getValString(rs.get(0), "CUMULATIVE_GROSS_TOTAL_ENTERED");
                    vendorInfo.itemsSentAmount_Cumulative       = Util.Database.getValString(rs.get(0), "CUMULATIVE_GROSS_TOTAL_RETURNED");
                    vendorInfo.itemsSoldAmount_Cumulative       = Util.Database.getValString(rs.get(0), "CUMULATIVE_GROSS_TOTAL_CR_SOLD");
                    vendorInfo.itemsRefundAmount_Cumulative     = Util.Database.getValString(rs.get(0), "CUMULATIVE_GROSS_TOTAL_CR_REFUND");


                    // YTDS + CUMS
                    //-------------------------------------------------------------------------------------------
                    vendorInfo.itemsNetPaidAmount_YTD                  = Util.Database.getValString(rs.get(0), "YTD_NET_PAYMENT");

                    vendorInfo.itemsPaymentSentAmount_Cumulative       = Util.Database.getValString(rs.get(0), "CUMULATIVE_TOTAL_PAYMENT_SENT");
                    vendorInfo.itemsPaymentReceivedAmount_Cumulative   = Util.Database.getValString(rs.get(0), "CUMULATIVE_TOTAL_PAYMENT_RECEIVED");

                }
                else
                {
                    // ARCHIVE
                    vendorInfo.ytdNetTaxEntered     = Util.Database.getValString(rs.get(0), "LAST_EOD_TAX_TOTAL_ENTERED");
                    vendorInfo.ytdNetTaxReturned    = Util.Database.getValString(rs.get(0), "LAST_EOD_TAX_TOTAL_RETURNED");
                    vendorInfo.ytdNetTaxSold        = Util.Database.getValString(rs.get(0), "LAST_EOD_TAX_TOTAL_CR_SOLD");
                    vendorInfo.ytdNetTaxRefund      = Util.Database.getValString(rs.get(0), "LAST_EOD_TAX_TOTAL_REFUDN");
                    vendorInfo.ytdNetTax            = Util.Database.getValString(rs.get(0), "LAST_EOD_NET_TAX");
                    vendorInfo.ytdNetTaxFinAdjPlus  = Util.Database.getValString(rs.get(0), "LAST_EOD_FIN_ADJ_PLUS_TAX_TOTAL");
                    vendorInfo.ytdNetTaxFinAdjMinus = Util.Database.getValString(rs.get(0), "LAST_EOD_FIN_ADJ_MINUS_TAX_TOTAL");

                    vendorInfo.ytdNetExpenseEntered     = Util.Database.getValString(rs.get(0), "LAST_EOD_EXPENSE_TOTAL_ENTERED");
                    vendorInfo.ytdNetExpenseReturned    = Util.Database.getValString(rs.get(0), "LAST_EOD_EXPENSE_TOTAL_RETURNED");
                    //vendorInfo.ytdNetTaxSold        = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_CR_SOLD");
                    //vendorInfo.ytdNetTaxRefund      = Util.Database.getValString(rs.get(0), "YTD_NET_TAX_CR_REFUND");
                    vendorInfo.ytdNetExpense            = Util.Database.getValString(rs.get(0), "LAST_EOD_NET_EXPENSE");
                    vendorInfo.ytdNetExpenseFinAdjPlus  = Util.Database.getValString(rs.get(0), "LAST_EOD_FIN_ADJ_PLUS_EXPENSE_TOTAL");
                    vendorInfo.ytdNetExpenseFinAdjMinus = Util.Database.getValString(rs.get(0), "LAST_EOD_FIN_ADJ_MINUS_EXPENSE_TOTAL");

                    vendorInfo.itemsNetQuantityEntered_YTD          = Util.Database.getValString(rs.get(0), "LAST_EOD_QUANTITY_ENTERED");
                    vendorInfo.itemsNetQuantityReturned_YTD         = Util.Database.getValString(rs.get(0), "LAST_EOD_QUANTITY_RETURNED");
                    vendorInfo.itemsNetQuantitySold_YTD             = Util.Database.getValString(rs.get(0), "LAST_EOD_QUANTITY_CR_SOLD");
                    vendorInfo.itemsNetQuantityRefund_YTD       = Util.Database.getValString(rs.get(0), "LAST_EOD_QUANTITY_CR_REFUND");
                    vendorInfo.itemsNetQuantityFinAdjPlus_YTD       = Util.Database.getValString(rs.get(0), "LAST_EOD_QUANTITY_ADJ_PLUS");
                    vendorInfo.itemsNetQuantityFinAdjMinus_YTD       = Util.Database.getValString(rs.get(0), "LAST_EOD_QUANTITY_ADJ_MINUS");
                    vendorInfo.itemsNetQuantity_YTD             = Util.Database.getValString(rs.get(0), "LAST_EOD_NET_QUANTITY");

                    vendorInfo.itemsReceivedAmount_YTD          = Util.Database.getValString(rs.get(0), "LAST_EOD_GROSS_TOTAL_ENTERED");
                    vendorInfo.itemsSentAmount_YTD              = Util.Database.getValString(rs.get(0), "LAST_EOD_GROSS_TOTAL_RETURNED");
                    vendorInfo.itemsFinAdjPlusNetAmount_YTD     = Util.Database.getValString(rs.get(0), "LAST_EOD_FIN_ADJ_PLUS_GROSS_TOTAL");
                    vendorInfo.itemsFinAdjMinusNetAmount_YTD    = Util.Database.getValString(rs.get(0), "LAST_EOD_FIN_ADJ_MINUS_GROSS_TOTAL");
                    vendorInfo.itemsSoldAmount_YTD              = Util.Database.getValString(rs.get(0), "LAST_EOD_GROSS_TOTAL_CR_SOLD");
                    vendorInfo.itemsRefundAmount_YTD            = Util.Database.getValString(rs.get(0), "LAST_EOD_GROSS_TOTAL_CR_REFUND");

                    vendorInfo.itemsNetPaidAmount_YTD                  = Util.Database.getValString(rs.get(0), "LAST_EOD_GROSS_TOTAL_ENTERED");

                }

                //vendorInfo.itemsSent            = Util.Database.getValString(rs.get(0), "NET_TOTAL_RETURNED");
                //vendorInfo.itemsSold            = Util.Database.getValString(rs.get(0), "NET_TOTAL_CR_SOLD");
                //vendorInfo.itemsPaid            = Util.Database.getValString(rs.get(0), "CUMULATIVE_ENTERED");
                vendorInfo.allTimeVolume        = Util.Database.getValString(rs.get(0), "CUMULATIVE_ENTERED");
                vendorInfo.profitability        = Util.Database.getValString(rs.get(0), "PROFITABILITY");

            }

            return vendorInfo;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean updateBillOnJustOptions(EntityManager               pem,
                                                BigInteger                  pUserId,
                                                BigInteger                  pAccId,
                                                BigInteger                  pVendorId,
                                                BigInteger                  pBillId,
                                                String                      psOptions) throws Exception
    {
        ArrayList<ssoBillItemOption> opts = new ArrayList<ssoBillItemOption>();
        String KEY_NAME_BILL_LINE_ID = "lineid";
        String KEY_NAME_ITEM_CODE    = "itemcode";
        String KEY_NAME_OPT_GROUP    = "group";
        String KEY_NAME_OPT_CODE     = "option";
        String KEY_NAME_OPT_QUANTITY = "quantity";

        try
        {
            JsonArray jsaOptions = Util.JSON.toArray(psOptions);
            for (int j=0; j<jsaOptions.size();j++)
            {
                JsonObject item = (JsonObject)jsaOptions.get(j);

                ssoBillItemOption optN = new ssoBillItemOption();

                optN.lineId      = item.get(KEY_NAME_BILL_LINE_ID).getAsString();
                optN.itemCode    = item.get(KEY_NAME_ITEM_CODE).getAsString();
                optN.optGroup    = item.get(KEY_NAME_OPT_GROUP).getAsString();
                optN.optCode     = item.get(KEY_NAME_OPT_CODE).getAsString();
                optN.optQuantity = item.get(KEY_NAME_OPT_QUANTITY).getAsString();

                opts.add(optN);
                //JsonElement jePostCode  = item.get(KEY_POSTCODE);
            }

            // UNIQUE ITEM CODES
            Set<String> uniqueItemCodeLineIds = opts.stream().map(opt -> opt.lineId).collect(Collectors.toSet());

            Map<String, String> aItemOptions = new HashMap<>();
            for(String itemCodeLineIdN: uniqueItemCodeLineIds)
            {
                // itemCode -> OptGroups
                List<String> uniqueOptGroups = opts.stream()
                                                    .filter(o -> o.lineId.equals(itemCodeLineIdN))
                                                    .map(o -> o.optGroup)
                                                    .distinct()
                                                    .collect(Collectors.toList());

                String sOptGroupSummary = "{";
                int indexG = 0;
                for(String optGroupN: uniqueOptGroups)
                {
                    // itemCode -> OptGroups -> OptCodes
                    List<String> uniqueOptCodes = opts.stream()
                                                        .filter(o -> o.lineId.equals(itemCodeLineIdN))//opt code can't be empty but optgroup can
                                                        .filter(o -> optGroupN.equals(o.optGroup)
                                                                     && o.optCode  != null && !o.optCode.trim().isEmpty())
                                                        .map(o -> o.optCode)
                                                        .distinct()
                                                        .collect(Collectors.toList());

                    if(indexG>0)
                        sOptGroupSummary += ",";

                    String sCodeSummary = "{";
                    int indexC = 0;
                    String sQuantity = "";
                    for(String optCodeN: uniqueOptCodes)
                    {
                        //Get Quantity
                        //------------------------------------------------------

                        sQuantity = "";
                        for (JsonElement je : jsaOptions) 
                        {
                            JsonObject joEl = je.getAsJsonObject();

                            if( itemCodeLineIdN.equals(joEl.get(KEY_NAME_BILL_LINE_ID).getAsString()) &&
                                optGroupN.equals(joEl.get(KEY_NAME_OPT_GROUP).getAsString()) &&
                                optCodeN.equals(joEl.get(KEY_NAME_OPT_CODE).getAsString())
                              )
                            {
                                if(indexC>0)
                                    sCodeSummary += ",";

                                sQuantity = joEl.get(KEY_NAME_OPT_QUANTITY).getAsString();

                                sCodeSummary += Util.Str.QUOTE(optCodeN) + ":" + sQuantity;
                                indexC++;
                                break;
                            }

                        }//end of get quantity 

                    }//END OF Opt Codes {"M":1,"L":2}
                    sCodeSummary += "}";

                    sOptGroupSummary += Util.Str.QUOTE(optGroupN) + ":" + sCodeSummary;
                    indexG++;

                }//end of opt groups { "WHITE":{"M":1,"L":2}, "BLACK":{"M":1,"L":2} }
                sOptGroupSummary += "}";

                aItemOptions.put(itemCodeLineIdN, sOptGroupSummary);
            }
            
            //-------------------------------------------------------------------
            // UPDATE TXNs 
            //-------------------------------------------------------------------
            String sQuery = "UPDATE ss_txn_inv_bill_dets " + 
                            "SET " + 
                                "OPTIONS = ? " + 
                            "WHERE " + 
                            "BILL_ID = ? " + 
                            "AND " + 
                            "UID = ? ";

            Query stmt = pem.CreateNativeQuery(sQuery);
            for (Map.Entry<String, String> oItemN : aItemOptions.entrySet()) 
            {
                String sItemCodeLineId  = oItemN.getKey();
                String sItemOptions     = oItemN.getValue();

                int Colindex = 1;

                stmt.SetParameter(Colindex++, sItemOptions            , "OPTIONS");
                stmt.SetParameter(Colindex++, pBillId                 , "BILL_ID");
                stmt.SetParameter(Colindex++, sItemCodeLineId         , "LINE_ID");

                stmt.addBatch();
            }

            int[] AffectedRows = stmt.executeBatch();

            //-------------------------------------------------------------------
            // UPDATE TXNs 
            //-------------------------------------------------------------------
            String sQueryEOD = "UPDATE ss_eod_inv_txn_dets " + 
                            "SET " + 
                                "OPTIONS = ? " + 
                            "WHERE " + 
                            "BILL_ID = ? " + 
                            "AND " + 
                            "ORG_UID = ? ";

            Query stmtEOD = pem.CreateNativeQuery(sQueryEOD);
            for (Map.Entry<String, String> oItemN : aItemOptions.entrySet()) 
            {
                String sItemCodeLineId  = oItemN.getKey();
                String sItemOptions     = oItemN.getValue();

                int Colindex = 1;

                stmtEOD.SetParameter(Colindex++, sItemOptions            , "OPTIONS");
                stmtEOD.SetParameter(Colindex++, pBillId                 , "BILL_ID");
                stmtEOD.SetParameter(Colindex++, sItemCodeLineId         , "LINE_ID");

                stmtEOD.addBatch();
            }

            int[] AffectedRowsEOD = stmtEOD.executeBatch();

            // Resets
            resetMemoryTables4Bill(pem, pAccId, pVendorId);

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void resetMemoryTables4Bill(EntityManager pem, BigInteger pAccId, BigInteger pVendorId) throws Exception
    {
        ssoRedisLettuce lettuce = new ssoRedisLettuce();

        try
        {
            // ss_acc_inv_item_stats
            //--------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys1 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey Col1 = new ssoCacheSplitKey();
            Col1.column = "ACCOUNT_ID";
            Col1.value  = pAccId;
            keys1.add(Col1);

            pem.flush(SsTxnInvBill.class, keys1);

            // Update Queue of Barcode
            //--------------------------------------------------------------------

            lettuce = Util.Redis.getConnection("updateBillLineOptions");
            String sStorageKey = getVendorBillQueueName(pAccId, pVendorId);
            Util.Redis.JString.remove(lettuce, sStorageKey);

            Util.Redis.releaseConnection(lettuce);
        }
        catch(Exception e)
        {
            if(lettuce!=null)
                Util.Redis.releaseConnection(lettuce);

            throw e;
        }
    }

    public static ssoBillShort getBill( EntityManager       pem, 
                                        BigInteger          pUserId,
                                        BigInteger          pBillId) throws Exception
    {
        ssoBillShort Bill = new ssoBillShort();

        try
        {

            Query stmt = pem.createNamedQuery("SsEodInvTxnDets.getBill", SsEodInvTxnDets.class);
            int index = 1;
            stmt.SetParameter(index++, pBillId           , "BILL_ID");

            //stmt.SetParameter(index++, pBrand           , "BRAND");// BAD IF YOU ARE USING CACHE TABLE

            BigDecimal bdBalance = new BigDecimal(BigInteger.ZERO);

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoBillLineShort newBillLine = new ssoBillLineShort();

                if(i==0)
                {
                    // This part will be repeating same for each line there fore only once will be used
                    // SUMMARY LEVEL
                    //-------------------------------------------------------------------------
                    Bill.BillId         = pBillId.toString();
                    Bill.BillType       = Util.Database.getValString(rs.get(i), "TXN_TYPE");
                    Bill.totalQuantity  = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY");
                    Bill.totalLineDisc  = Util.Database.getValString(rs.get(i), "TOTAL_LINE_DISCOUNT");
                    Bill.totalDiscount  = Util.Database.getValString(rs.get(i), "TOTAL_DISCOUNT");
                    Bill.DiscountRate   = Util.Database.getValString(rs.get(i), "BOTTOM_DISCOUNT_RATE");
                    //Bill.Surcharge      = Util.Database.getValString(rs.get(i), "TOTAL_SURCHARGE");
                    Bill.totalLineSurcharge = Util.Database.getValString(rs.get(i), "TOTAL_LINE_SURCHARGE");
                    Bill.totalSurcharge     = Util.Database.getValString(rs.get(i), "TOTAL_SURCHARGE");
                    Bill.taxRate            = Util.Database.getValString(rs.get(i), "TAX_RATE");

                    Bill.totalNet       = Util.Database.getValString(rs.get(i), "TOTAL_NET");
                    Bill.totalB4Tax     = Util.Database.getValString(rs.get(i), "TOTAL_B4TAX");//AFTER DISCOUNT
                    Bill.totalTax       = Util.Database.getValString(rs.get(i), "TOTAL_TAX");
                    Bill.taxRate        = Util.Database.getValString(rs.get(i), "TAX_RATE");
                    Bill.totalExpense   = Util.Database.getValString(rs.get(i), "TOTAL_EXPENSE");//expense 
                    Bill.totalGross     = Util.Database.getValString(rs.get(i), "TOTAL_GROSS");
                    
                    Bill.descr     = Util.Database.getValString(rs.get(i), "DESCR");
                }

                newBillLine.UID        = Util.Database.getValString(rs.get(i), "ORG_UID");
                newBillLine.ItemCode   = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                newBillLine.Category   = Util.Database.getValString(rs.get(i), "ITEM_CATEGORY");
                newBillLine.Quantity   = Util.Database.getValString(rs.get(i), "QUANTITY");
                newBillLine.Unit       = Util.Database.getValString(rs.get(i), "UNIT");
                newBillLine.EntryPrice = Util.Database.getValString(rs.get(i), "PRICE_ENTRY");
                newBillLine.SalesPrice = Util.Database.getValString(rs.get(i), "PRICE_END");//PRICE TAG
                newBillLine.DiscountRate = Util.Database.getValString(rs.get(i), "LINE_DISCOUNT_RATE");
                newBillLine.Discount   = Util.Database.getValString(rs.get(i), "LINE_DISCOUNT");
                newBillLine.Surcharge  = Util.Database.getValString(rs.get(i), "LINE_SURCHARGE");
                newBillLine.Total      = Util.Database.getValString(rs.get(i), "LINE_NET");
                newBillLine.Options    = Util.Database.getValString(rs.get(i), "OPTIONS");

                Bill.Lines.add(newBillLine);
            }

            return Bill;
        }
        catch(Exception e)
        {
            String s = e.getMessage();
            throw e;
        }
    }

    public static void deleteBill(  EntityManager  pem, 
                                    BigInteger           pUserId,
                                    BigInteger           pAccId,
                                    BigInteger           pBrandId,
                                    BigInteger           pBillId) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_DELETE_BILL");

            SP.registerStoredProcedureParameter("P_ACC_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BILL_ID"         , Long.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId        , "P_ACC_ID");
            SP.SetParameter(Colindex++, pBillId       , "P_BILL_ID");

            SP.execute();

            // CLEAN CACHE
            ArrayList<ssoCacheSplitKey> aCacheSplitKeys = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey newKey1 = new ssoCacheSplitKey();
            newKey1.column = "ACCOUNT_ID";
            newKey1.value  = pAccId;
            aCacheSplitKeys.add(newKey1);

            ssoCacheSplitKey newKey2 = new ssoCacheSplitKey();
            newKey2.column = "VENDOR_ID";//BRAND ID
            newKey2.value  = pBrandId;
            aCacheSplitKeys.add(newKey2);

            //aCacheSplitKeys = Misc.Cache.prepareSplitKeysWithColNames(runSet.entity.cache.SplitKeyColumns, runSet.params);

            // Flushes all related memories for the entity
            // clean cache
            pem.flush(SsStmInvStatements.class, aCacheSplitKeys);//cleans all related 

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void deleteBillLineN( EntityManager  pem, 
                                        long           pUserId,
                                        long           pAccId,
                                        long           pBrandId,
                                        long           pBillId) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("");
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void updateNewBrand(  EntityManager  pem, 
                                        long           pUserId,
                                        long           pAccId,
                                        String         psBrand,
                                        String         psContactName,
                                        String         psPhoneCountryCode,
                                        String         psPhoneNumber,
                                        String         psTaxOrNationalId,
                                        String         psEmail,
                                        String         psCity,
                                        String         psAddress,
                                        String         psNotes
                                       ) throws Exception
    {
        try
        {
            if(psBrand.trim().length()!=0)
            {
                // getBrandCompanyDetails
                {
                    // update Company Details Fields
                    
                    // use query = "SsAccInvBrandDets.updateDets"
                   
                }
                
            }
        }
        catch(Exception e)
        {

        }
    }

    public static void updateBrandInfo( EntityManager  pem, 
                                        BigInteger           pUserId,
                                        BigInteger           pAccId,
                                        String         psBrand,
                                        String         psContactName,
                                        String         psPhoneCountryCode,
                                        String         psPhoneNumber,
                                        String         psTaxOrNationalId,
                                        String         psEmail,
                                        String         psCity,
                                        String         psAddress,
                                        String         psNotes) throws Exception
    {
        try
        {
            // STEPS 
            // 1. Update Details 
            // 2. Refresh/Clean memory parameters
            
            // STEP 1: DETAILS
            //-------------------------------------------------------------------
            String sPhoneAreaCode = "";
            String sPhoneNumber = "";
            String []aPhoneParts = psPhoneNumber.replace(")", "#").split("#");
            if(aPhoneParts.length>1)
            {
                sPhoneAreaCode = aPhoneParts[0].replace("(", "").replace(")", "").trim();
                sPhoneNumber   = aPhoneParts[1].replace("(", "").replace(")", "").replace("-", "").trim();
            }

            Query stmtBrandAcc = pem.createNamedQuery("SsAccInvBrandDets.updateDets", SsAccInvVendors.class);

            int index = 1;
            stmtBrandAcc.SetParameter(index++, psContactName     , "CONTACT_NAME");
            stmtBrandAcc.SetParameter(index++, psPhoneCountryCode, "PHONE_COUNTRY_CODE");
            stmtBrandAcc.SetParameter(index++, sPhoneAreaCode    , "PHONE_AREA_CODE");
            stmtBrandAcc.SetParameter(index++, sPhoneNumber      , "PHONE_NUMBER");
            stmtBrandAcc.SetParameter(index++, psTaxOrNationalId , "TAX_NO");
            stmtBrandAcc.SetParameter(index++, psEmail           , "EMAIL");
            stmtBrandAcc.SetParameter(index++, psCity            , "CITY");
            stmtBrandAcc.SetParameter(index++, psAddress         , "ADDRESS");
            stmtBrandAcc.SetParameter(index++, psNotes           , "NOTES");
            stmtBrandAcc.SetParameter(index++, ""                , "BYUSER");
            stmtBrandAcc.SetParameter(index++, psBrand           , "BRAND");
            stmtBrandAcc.SetParameter(index++, pUserId            , "USER_ID");

            stmtBrandAcc.executeUpdate();

            // STEP 2: PARAMETERS
            //-------------------------------------------------------------------
            VendorOps.cleanVendorSummary(pem, pUserId, pAccId);

            return ;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void resetBrand(    EntityManager        pem, 
                                            BigInteger           pUserId,
                                            BigInteger           pAccId,
                                            BigInteger           pBrandId ) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_ACC_RESET_VENDOR");
 
            SP.registerStoredProcedureParameter("P_ACC_ID"    , BigInteger.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VND_ID"    , BigInteger.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId            , "P_ACC_ID");
            SP.SetParameter(Colindex++, pBrandId          , "P_BRND_ID");

            SP.execute();
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static BigInteger registerNewBrand(    EntityManager  pem, 
                                            BigInteger           pUserId,
                                            BigInteger           pAccId,
                                            String         psBrand,
                                            String         psContactName,
                                            String         psPhoneCountryCode,
                                            String         psPhoneNumber,
                                            String         psTaxOrNationalId,
                                            String         psEmail,
                                            String         psCity,
                                            String         psAddress,
                                            String         psNotes
                                       ) throws Exception
    {
        boolean bParamSaved = false;

        try
        {
            SsAccInvVendors newBrandDetails = new SsAccInvVendors();

            String sPhoneCountryCode = "";
            String sPhoneAreaCode    = "";
            String sPhoneNumber      = "";

            String []aPhoneParts = psPhoneNumber.replace(")", "#").split("#");
            if(aPhoneParts.length>1)
            {
                sPhoneAreaCode = aPhoneParts[0].replace("(", "").replace(")", "").trim();
                sPhoneNumber   = aPhoneParts[1].replace("(", "").replace(")", "").replace("-", "").trim();
            }

            // STEPS 
            // 1. Create new Vendor
            // 2. Create vendor stats
            // 3. Add to Dictionary
            //

            // STEP 1 Create new Vendor
            //------------------------------------------------------------------
            SsAccInvVendors vendor = new SsAccInvVendors();
            vendor = VendorOps.createNewVendor( pem, 
                                                pUserId,
                                                pAccId,
                                                psBrand, 
                                                psContactName, 
                                                sPhoneCountryCode,
                                                sPhoneAreaCode,
                                                sPhoneNumber,
                                                psTaxOrNationalId, 
                                                psEmail, 
                                                psCity, 
                                                psAddress, 
                                                psNotes);

            // STEP 2 Create vendor stats (balance quantity keeper)
            //-------------------------------------------------------------------
            SsAccInvVendorStats brandAcc = new SsAccInvVendorStats();
            brandAcc = VendorOps.createVendorStats(pem, pAccId, vendor.uid);//account is created here (balance / quantity)

            // STEP 3 Add to Dictionary (1ST ADD)
            //------------------------------------------------------------------
            ssoAPIResponse rsp = new ssoAPIResponse();
            rsp = DictionaryOps.Vendor.add_VendorNItemCodes(    pem, 
                                                                pUserId,
                                                                pAccId, 
                                                                psBrand.toUpperCase().trim(), 
                                                                "",
                                                                vendor.uid,
                                                                BigInteger.valueOf(-1),
                                                                "0");
            
            
            bParamSaved = true;

            return vendor.uid;
        }
        catch(Exception e)
        {
            if(bParamSaved==true)
            {
                // Rollback the param here
                DictionaryOps.Vendor.delete_Brand(pem, pUserId, pAccId, psBrand.toUpperCase().trim());
            }

            throw e;
        }
    }

    public static void resetMemoryTables4Account(EntityManager pem, BigInteger pUserId) throws Exception
    {
        try
        {
            // ss_acc_inv_item_stats
            //--------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys1 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey Col1 = new ssoCacheSplitKey();
            Col1.column = "USER_ID";
            Col1.value  = pUserId;
            keys1.add(Col1);

            pem.flush(SsUsrAccounts.class, keys1);
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoMerchantPreferences getAccountSettings(EntityManager pem, BigInteger pUserId, BigInteger pAccountId) throws Exception
    {
        ssoMerchantPreferences mrcPref = new ssoMerchantPreferences();

        try
        {
            //pem.cacheable("P_USR_ID, P_ACC_ID");
            //StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_MRC_GET_MERCHANT_PREFERENCES");
            Query stmt = pem.createNamedQuery("SsUsrAccounts.getMerchantPreferences", SsUsrAccounts.class);
            int index = 1;
            stmt.SetParameter(index++, pUserId          , "USER_ID");
            stmt.SetParameter(index++, pAccountId       , "ACCOUNT_ID");

            //SP.registerStoredProcedureParameter("P_USR_ID"    , Long.class     , ParameterMode.IN);
            //SP.registerStoredProcedureParameter("P_ACC_ID"    , Long.class     , ParameterMode.IN);

            //int Colindex = 1;
            //SP.SetParameter(Colindex++, pUserId             , "P_USR_ID");
            //SP.SetParameter(Colindex++, pAccountId          , "P_ACC_ID");

            //SP.execute();

            //List<List<RowColumn>> rs =  SP.getResultList();
            List<List<RowColumn>> rs = stmt.getResultList();
            if (rs.size()>0)
            {
                List<RowColumn> RowN = rs.get(0);

                DekontSummaryYear newYear = new DekontSummaryYear();

                mrcPref.Id              = Long.parseLong(Util.Database.getValString(RowN, "UID").toString());
                mrcPref.version         = Integer.parseInt(Util.Database.getValString(RowN, "VERSION").toString());
                mrcPref.MerchantName    = Util.Database.getValString(RowN, "PROFILENAME");
                //mrcPref.MerchantName    = Util.Database.getValString(RowN, "PROFILE_NAME");
                mrcPref.CurrencyCode    = Util.Database.getValString(RowN, "CURRENCY_CODE");
                mrcPref.CurrencyName    = Util.Database.getValString(RowN, "CURRENCY_NAME");
                mrcPref.MCC             = Util.Database.getValString(RowN, "MCC");
                mrcPref.MCCName         = Util.Database.getValString(RowN, "MCC_NAME");
                mrcPref.naceCode        = Util.Database.getValString(RowN, "NACE_CODE");
                mrcPref.CountryCode     = Util.Database.getValString(RowN, "COUNTRY_CODE");
                mrcPref.CountryName     = Util.Database.getValString(RowN, "COUNTRY_NAME");
                mrcPref.StateCode       = Util.Database.getValString(RowN, "STATE_CODE");
                mrcPref.StateName       = Util.Database.getValString(RowN, "STATE_NAME");
                //mrcPref.CountyCode      = Util.Database.getValString(RowN, "COUNTY_CODE");
                mrcPref.PlaceNameUID    = Util.Database.getValString(RowN, "PLACE_NAME_UID");
                mrcPref.PlaceName       = Util.Database.getValString(RowN, "PLACE_NAME");
                mrcPref.email           = Util.Database.getValString(RowN, "EMAIL");
                mrcPref.profileName       = Util.Database.getValString(RowN, "PROFILENAME");
                mrcPref.displayName       = Util.Database.getValString(RowN, "DISPLAY_NAME");
                mrcPref.isTaxInSalesPrice = Util.Database.getValString(RowN, "IS_TAX_INC_PRICE");
                mrcPref.taxRate           = Util.Database.getValString(RowN, "TAX_RATE");
                mrcPref.insDiffRate       = Util.Database.getValString(RowN, "INS_DIFF_RATE");
                mrcPref.isActive          = Util.Database.getValString(RowN, "ACTIVATED");
                mrcPref.printerCode       = Util.Database.getValString(RowN, "PRINTER_CODE");
                mrcPref.printerName       = Util.Database.getValString(RowN, "PRINTER_NAME");
                mrcPref.labelWidth        = Util.Database.getValString(RowN, "WIDTH_MM");
                mrcPref.labelHeight       = Util.Database.getValString(RowN, "HEIGHT_MM");

                mrcPref.seperateEStore          = Util.Database.getValString(RowN, "SEPERATE_ESTORE");
                mrcPref.showBrandNames          = Util.Database.getValString(RowN, "SHOW_BRAND_NAMES_ON_ESTORE");
                mrcPref.lastSearchEngineUpdate  = Util.Database.getValString(RowN, "LAST_SEARCH_ENGINE_UPDATE");

            }
            else
            {
                return null;
            }

            return mrcPref;
        }
        catch(Exception e)
        {
            throw e;
        }
        /*
        SsMrcMerchants mrcPrefs = new SsMrcMerchants();
        
        try
        {
            mrcPrefs = pem.find(SsMrcMerchants.class, pMrcId);
            
            return mrcPrefs;
        }
        catch(Exception e)
        {
            throw e;
        }
        */
    }

}

