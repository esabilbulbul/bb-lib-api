/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

import bb.app.bill.ssoBillBottomTotals;
import bb.app.dekonts.DekontEarningStats;
import bb.app.dekonts.DekontQuantityStats;
import bb.app.dekonts.DekontSummary;
import bb.app.dekonts.DekontSummaryDay;
import bb.app.dekonts.DekontSummaryQuarterDay;
import bb.app.dekonts.DekontSummaryQuarterWeek;
import bb.app.dekonts.DekontSummaryRec;
import bb.app.dekonts.DekontSummaryStats;
import bb.app.dekonts.DekontSummaryTots;
import bb.app.dekonts.DekontSummaryUseRates;
import bb.app.dekonts.DekontSummaryWeek;
import bb.app.dekonts.DekontSummaryYear;
import bb.app.dict.DictionaryOps;
import bb.app.inv.InventoryOps;
import bb.app.inv.InventoryParams;
import bb.app.obj.ssoBrand;
import bb.app.vendor.VendorOps;
import bb.app.obj.ssoBrandDets;
import bb.app.obj.ssoMerchant;
import bb.app.bill.ssoBillLine;
import bb.app.bill.ssoBillLineShort;
import bb.app.bill.ssoBillShort;
import bb.app.obj.ssoMerchantPreferences;
import bb.app.settings.UXParams;
import bb.app.txn.txnDefs;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvVendors;
import entity.acc.SsAccInvVendorStats;
import entity.acc.SsAccInvVendorLog;
import entity.eod.SsEodInvStmtDets;
import entity.mrc.SsMrcEarnings;
import entity.mrc.SsMrcStatsEarnings;
import entity.mrc.SsMrcStatsQuantity;
import entity.dct.SsDctInvVendorSummary;
import entity.eod.SsEodInvTxnDets;
import entity.mrc.SsMrcCashRegEod;
import entity.stmt.SsStmInvStatements;
import entity.user.SsUsrAccounts;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jaxesa.annotations.Cacheable;
import jaxesa.log.LogManager;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Misc;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.CacheLoadTypes;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.persistence.ssoKeyField;
import jaxesa.util.Util;
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
    public static String INV_TXN_TYPE_SOLD      = "S";//Sales
    public static String INV_TXN_TYPE_FIN_ADJ   = "A";//Financial Adjustment
    
    public static String INV_TXN_EFFECT_CREDIT = "C";//ALACAK
    public static String INV_TXN_EFFECT_DEBIT  = "D";//BORC
    
    public static String gCMMN_TXN_CODE_GROUP_PURCHASE      = "001";//same as in js file UX
    public static String gCMMN_TXN_CODE_GROUP_ITEM          = "002";//same as in js file UX
    public static String gCMMN_TXN_CODE_GROUP_PAYMENT       = "003";//same as in js file UX
  */
    
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
                                                                                    long pAccountId, //merchant Id
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

            RetailEarningStats = calculateEarningStats(pem, pAccountId, pMerchantName, iDayBackLength);//for test
            AllEarningStats    = calculateEarningStats(pem, -1, "", iDayBackLength);

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
                                                 long pAccountId, //merchant Id
                                                 String pMerchantName,
                                                 String pBaseCurrency, 
                                                 String pTargetCurrency, 
                                                 int pBankCode, 
                                                 int pYear, 
                                                 int pMonth,
                                                 int pYearEarningLength)
    {
        DekontSummary summary = new DekontSummary();

        try
        {
            summary.currency = pBaseCurrency;
            summary.baseYearDate = Util.DateTime.GetDateTime_s().substring(0,4);
            summary.lastYearDate = Integer.toString(Integer.parseInt(summary.baseYearDate) - 1);

            summary.targetMonth = Util.DateTime.GetDateTime_s().substring(4,6);

            // Overall/Avg Summary regardless of the year
            summary.rows    = calculateSummaryRecords(pem, pAccountId, pBaseCurrency, pTargetCurrency, pBankCode, pYear, pMonth);
            summary.banks   = calculateSummaryBankSubtotals(pem, pAccountId, pBaseCurrency, pTargetCurrency, summary.baseYearDate, -1);
            summary.overall = calculateSummaryOverall(pem, pAccountId, pBaseCurrency, pTargetCurrency);
            summary.years   = calculateSummaryYears(pem, pAccountId, pBaseCurrency, pTargetCurrency, summary.baseYearDate);//ay bazinda yillik performans
            

            // Avg + Year (now) + Year (past) summary
            //------------------------------------------------------------------
            summary.Qdays          = calculateSummaryQuarterDays(pem, pAccountId, pBaseCurrency, pTargetCurrency, "-1");//Genel/Avg - Gun Ciro Ortalamasi
            summary.thisYear.Qdays = calculateSummaryQuarterDays(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Gun Ciro Ortalamasi
            summary.lastYear.Qdays = calculateSummaryQuarterDays(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Gun Ciro Ortalamasi

            //Mevsimlik Performanslar
            //------------------------------------------------------------------
            summary.Qweeks           =  calculateSummaryQuarterWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency,"-1");////Genel/Avg - Week Ciro Ortalamasi
            summary.thisYear.Qweeks  =  calculateSummaryQuarterWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Week Ciro Ortalamasi
            summary.lastYear.Qweeks  =  calculateSummaryQuarterWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Week Ciro Ortalamasi

            summary.weeks          = calculateSummaryWeeks(pem, pAccountId, pBaseCurrency, pTargetCurrency, "-1", "-1");//Hafta Bazinda Yillik Performans
            //summary.thisYear.weeks = calculateSummaryWeeks(pem, summary.baseYearDate, "-1");//ignored
            //summary.lastYear.weeks = calculateSummaryWeeks(pem, summary.lastYearDate, "-1");//ignored

            //Aylik Performanslar
            //------------------------------------------------------------------
            summary.currentMonth.days = calculateSummaryDays(pem, pAccountId, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);//Gun bazinda Aylik Performans
            summary.currentMonth.weeks = calculateSummaryWeeksOfMonth(pem, pAccountId, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);//Haftalik Aylik Performans
            summary.currentMonth.dayAvgs = calculateSummaryTargetMonthDayAverages(pem, pAccountId, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);

            summary.useRates = calculateSummaryUseRates(pem, pAccountId, pBaseCurrency, pTargetCurrency);

            //earningStats
            //------------------------------------------------------------------
            ArrayList<DekontEarningStats> RetailEarningStats = new ArrayList<DekontEarningStats>();
            ArrayList<DekontEarningStats> AllEarningStats = new ArrayList<DekontEarningStats>();
            RetailEarningStats = calculateEarningStats(pem, pAccountId, pMerchantName, pYearEarningLength);//for test
            AllEarningStats    = calculateEarningStats(pem, -1, "", pYearEarningLength);

            if (RetailEarningStats!=null)
                summary.earnings.addAll(RetailEarningStats);

            if (AllEarningStats!=null)
                summary.earnings.addAll(AllEarningStats);

            //QuantityStats
            //------------------------------------------------------------------
            ArrayList<DekontQuantityStats> RetailQuantityStats = new ArrayList<DekontQuantityStats>();
            ArrayList<DekontQuantityStats> AllQuantityStats    = new ArrayList<DekontQuantityStats>();
            RetailQuantityStats = calculateQuantityStats(pem, pAccountId, pMerchantName);//for test
            AllQuantityStats    = calculateQuantityStats(pem, -1, "");

            summary.quantities.addAll(RetailQuantityStats);
            summary.quantities.addAll(AllQuantityStats);

            summary.dashboard = getDashboardValues(pMerchantName, pAccountId, summary.earnings, summary.quantities);

            return summary;
        }
        catch(Exception e)
        {
            return summary;
        }
    }

    public static DekontSummaryStats getDashboardValues(String pMerchantName, 
                                                        long pAccountId, //merchant Id
                                                        ArrayList<DekontEarningStats> pYearEarnings,
                                                        ArrayList<DekontQuantityStats> pQuantities
                                                       )
    {
        DekontSummaryStats dashboard = new DekontSummaryStats();

        //Dashboard
        //------------------------------------------------------------------
        DecimalFormat df2 = new DecimalFormat("#.##");
        df2.setRoundingMode(RoundingMode.UP);

        DecimalFormat df3 = new DecimalFormat("#.###");
        df3.setRoundingMode(RoundingMode.UP);
        //Retail YEar Earning
        DekontEarningStats retailN = new DekontEarningStats();
        retailN = getLastEarningValue(pYearEarnings, pAccountId);
        //retailN = RetailEarningStats.get(0);

        //RETAIL - YEAR EARNING
        dashboard.yearearning_mybiz.id = 1;//Retail-YE
        dashboard.yearearning_mybiz.Title = pMerchantName;
        dashboard.yearearning_mybiz.Value = retailN.YearEarning;
        dashboard.yearearning_mybiz.ChangeInValue = df2.format(retailN.diffYearEarning).toString();
        if (retailN.changeYearEarning.length()!=0)
            dashboard.yearearning_mybiz.ChangeInPerc  = df2.format(Double.parseDouble(retailN.changeYearEarning)).toString();
        if (retailN.changeYearEarning2Yesterday.length()!=0)
            dashboard.yearearning_mybiz.ChangeInPerc2Yesterday = df2.format(Double.parseDouble(retailN.changeYearEarning2Yesterday)).toString();
        
        dashboard.yearearning_mybiz.ChangeInValue2Yesterday = df2.format(retailN.diffYearEarning2Yesterday).toString();
        dashboard.yearearning_mybiz.dtime = retailN.dtime;

        //RETAIL - YEAR TO DATE
        dashboard.ytd.id = 2;//Retail-YTD
        dashboard.ytd.Title = pMerchantName + " - ytd";
        dashboard.ytd.Value = retailN.YTDEarning;
        dashboard.ytd.ChangeInValue = df2.format(retailN.diffYTD);
        if (retailN.changeYTDEarning.length()!=0)
            dashboard.ytd.ChangeInPerc  = df2.format(Double.parseDouble(retailN.changeYTDEarning)).toString();
        dashboard.ytd.dtime = retailN.dtime;

        //MARKET YEAR EARNING
        DekontEarningStats market = new DekontEarningStats();
        market = getLastEarningValue(pYearEarnings, -1);
        //market = AllEarningStats.get(0);

        dashboard.yearearning_market.id = 3;//Market
        dashboard.yearearning_market.Title = "Market";
        dashboard.yearearning_market.Value = market.YearEarning;
        dashboard.yearearning_market.ChangeInValue = df2.format(market.diffYearEarning).toString();
        if (market.changeYearEarning.length()!=0)
            dashboard.yearearning_market.ChangeInPerc  = df2.format(Double.parseDouble(market.changeYearEarning)).toString();
        //ChangeInValue2Yesterday
        if (market.changeYearEarning2Yesterday.length()!=0)
            dashboard.yearearning_market.ChangeInPerc2Yesterday  = df2.format(Double.parseDouble(market.changeYearEarning2Yesterday)).toString();
        dashboard.yearearning_market.ChangeInValue2Yesterday = df2.format(market.diffYearEarning2Yesterday).toString();
        dashboard.yearearning_market.dtime = market.dtime;

        //RETAIL QUANTITY 
        DekontQuantityStats retailQuantity = new DekontQuantityStats();
        retailQuantity = getLastQuantitiesValue(pQuantities, pAccountId);
        //retailQuantity = RetailQuantityStats.get(0);

        dashboard.quantity.id = 4;
        dashboard.quantity.Title = "Quantity Index";
        dashboard.quantity.Value = df2.format(retailQuantity.value).toString();
        dashboard.quantity.ChangeInValue = df2.format(retailQuantity.diff).toString();//N/A
        dashboard.quantity.ChangeInPerc = df2.format(retailQuantity.change).toString();
        dashboard.quantity.dtime = retailQuantity.refDate;

        return dashboard;
    }

    public static DekontQuantityStats getLastQuantitiesValue(ArrayList<DekontQuantityStats> pEarnings, long pAccountId)
    {
        DekontQuantityStats YEStat = new DekontQuantityStats();
        
        for (DekontQuantityStats statN:pEarnings)
        {
            if (statN.id==pAccountId)
                YEStat = statN;
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

    public static ArrayList<DekontQuantityStats> calculateQuantityStats(  EntityManager pem, 
                                                                          long pAccountId,
                                                                          String pMerchantName
                                                                       )
    {
        ArrayList<DekontQuantityStats> stats = new ArrayList<DekontQuantityStats>();

        try
        {
            String stStmt = "";

            // -1 STORED FOR MARKET 
            stStmt = "SELECT * FROM ss_mrc_stats_quantity " + 
                     " WHERE " +
                     " STAT = 1 AND ACCOUNT_ID = ? " + //pAccountId + //ORDER BY UID DESC";
                     " ORDER BY REFERENCE_DATE ASC ";
            

            /*
            }
            else
            {
                stStmt = "SELECT * FROM ss_mrc_stats_quantity " + 
                         " WHERE " +
                         " STAT = 1 " +
                         " ORDER BY REFERENCE_DATE ASC ";
            }
            */
            pem.cacheable("P_ACCOUNT_ID");
            Query newQuery = pem.CreateQuery(stStmt);
            int ParIndex = 1;
            newQuery.SetParameter(ParIndex++, pAccountId     , "P_ACCOUNT_ID");

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

            return stats;
        }
        catch(Exception e)
        {
            return stats;
        }
    }

    public static ArrayList<DekontEarningStats> calculateEarningStats(  EntityManager pem, 
                                                                        long pAccountId,
                                                                        String pAccountName,//pMerchantName
                                                                        int pYearEarningLength
                                                                    )
    {

        ArrayList<DekontEarningStats> earnings = new ArrayList<DekontEarningStats>();
        
        int iDayNo = 0;
        try
        {
            // IN DB ACCOUNT_ID = -1 stored for market value
            String stStmt = "";
            stStmt = "SELECT T.* FROM " +
                     "(" +
                        "SELECT * FROM ss_mrc_stats_earnings " + 
                                    " WHERE " +
                                    " STAT = 1 AND ACCOUNT_ID = ? " + //pAccountId + //ORDER BY UID DESC";
                                    " ORDER BY REFERENCE_DATE DESC " +
                                    " LIMIT ? " + //(pYearEarningLength + 1) + 
                     ") T " + 
                     "ORDER BY T.REFERENCE_DATE ASC";

            pem.cacheable("ACCOUNT_ID");

            Query newQuery = pem.CreateQuery(stStmt);
            int index = 1;
            newQuery.SetParameter(index++, pAccountId               , "ACCOUNT_ID");
            newQuery.SetParameter(index++, (pYearEarningLength + 1) , "LIMIT");

            List<SsMrcStatsEarnings> rs =  newQuery.getResultList(SsMrcStatsEarnings.class);

            /*
            BigDecimal prevYearEarning    = new BigDecimal(BigInteger.ZERO);
            BigDecimal prevYTDEarning     = new BigDecimal(BigInteger.ZERO);
            BigDecimal currentYearEarning = new BigDecimal(BigInteger.ZERO);
            BigDecimal currentYTDEarning  = new BigDecimal(BigInteger.ZERO);
            BigDecimal diffYearEarning = new BigDecimal(BigInteger.ZERO);
            BigDecimal diffYTDEarning  = new BigDecimal(BigInteger.ZERO);
            BigDecimal changeYearEarning = new BigDecimal(BigInteger.ZERO);
            BigDecimal changeYTDEarning  = new BigDecimal(BigInteger.ZERO);
            */
            //double avg_prevDayYearEarning    = 0;
            //double avg_prevDayYTDEarning     = 0;
            
            //double currentYearEarning = 0;
            //double currentYTDEarning  = 0;

            double totYearEarningDayAverage = 0;//this will be used as reference to calculate the market move / position
            double prevDayYearEarningDayAverage = 0;
            double prevDayYearEarning = 0;
            double prevDayYTDEarning  = 0;

            int ipastYear_YearEarningCount = 0;

            for (SsMrcStatsEarnings earningN:rs)
            {
                if (iDayNo==0)
                {
                    iDayNo++;
                    continue;
                }

                DekontEarningStats newEarning = new DekontEarningStats();

                long lRefDate       = earningN.referenceDate;
                int  iDayMercNumber = earningN.dailyAccountNumber;//number of merchants that have entry

                if (lRefDate==20191230)
                    lRefDate = lRefDate;

                //YEAR EARNING (DB)
                double currentYearEarning     = earningN.yearEarningSum.doubleValue();
                //int iYearEarningCount         = earningN.yearEarningCount;
                //double avg_CurrentYearEarning = earningN.yearEarningAvg.doubleValue();
                double pastYear_YearEarning   = earningN.priorYearEarningSum.doubleValue();
                //double avg_pastYear_YearEarning = earningN.priorYearEarningAvg.doubleValue();

                //YTD (DB)
                double currentYTDEarning      = earningN.yearToDateSum.doubleValue();
                //int iYTDEarningCount          = earningN.yearToDateCount;
                //double avg_CurrentYTDEarning  = earningN.yearToDateAvg.doubleValue();
                double pastYear_YTDEarning    = earningN.priorYtdSum.doubleValue();

                // Formula: Change in Earning = ( Current - Prev.Year ) / Prev. Year
                DecimalFormat df = new DecimalFormat("#.####");
                df.setRoundingMode(RoundingMode.CEILING);

                newEarning.id      = pAccountId;
                newEarning.name    = pAccountName;
                newEarning.dayNo   = iDayNo;
                newEarning.refDate = Long.toString(lRefDate);

                // Year Earning Difference
                double diffYearEarning    = currentYearEarning - pastYear_YearEarning;
                double diffYTDEarning     = currentYTDEarning  - pastYear_YTDEarning;
                double diffYearEarning2Yesterday    = currentYearEarning - prevDayYearEarning;
                
                String sChangeYearEarning = "";
                if (pastYear_YearEarning==0)
                    sChangeYearEarning = "0";
                else
                    sChangeYearEarning = df.format((diffYearEarning / pastYear_YearEarning) * 100);
                double changeYearEarning = Double.parseDouble(sChangeYearEarning);
                
                String sChange2Yesterday = "";
                if (prevDayYearEarning==0)
                    sChange2Yesterday = "0";
                else
                    sChange2Yesterday = df.format((diffYearEarning2Yesterday / prevDayYearEarning) * 100);
                double changeYearEarning2Yesterday = Double.parseDouble(sChange2Yesterday);

                String sChangeYTDEarning = "";
                if (pastYear_YTDEarning==0)
                    sChangeYTDEarning = "0";
                else
                    sChangeYTDEarning = df.format((diffYTDEarning / pastYear_YTDEarning) * 100);

                double changeYTDEarning  = Double.parseDouble(sChangeYTDEarning);

                newEarning.dtime = Long.toString(earningN.insertdate);
                newEarning.YearEarning = Double.toString(currentYearEarning);
                newEarning.YTDEarning  = Double.toString(currentYTDEarning);

                newEarning.diffYearEarning = diffYearEarning;
                newEarning.diffYTD         = diffYTDEarning;
                newEarning.changeYearEarning = Double.toString(changeYearEarning);
                newEarning.changeYTDEarning  = Double.toString(changeYTDEarning);
                
                newEarning.changeYearEarning2Yesterday = Double.toString(changeYearEarning2Yesterday);
                newEarning.diffYearEarning2Yesterday = diffYearEarning2Yesterday;

                earnings.add(newEarning);

                //PREV DAY YEAR
                prevDayYearEarning = earningN.yearEarningSum.doubleValue();
                prevDayYearEarningDayAverage = totYearEarningDayAverage;

                iDayNo++;
            }

            return earnings;
        }
        catch(Exception e)
        {
            return null;
        }
    }

    //@Cacheable(type=CacheLoadTypes.LAZY, key="P_MRC_ID")
    public static ArrayList<DekontSummaryRec> calculateSummaryRecords(  EntityManager pem, 
                                                                        long pMerchantId,
                                                                        String pBaseCurrency, 
                                                                        String pTargetCurrency, 
                                                                        int pBankCode, 
                                                                        int pYear, 
                                                                        int pMonth)
    {
        ArrayList<DekontSummaryRec> SumRows = new ArrayList<DekontSummaryRec>();

        try
        {
            pem.cacheable("P_MRC_ID");// THIS WILL CACHE THE RESULTSET
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY");

            SP.registerStoredProcedureParameter("P_MRC_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"   , String.class      , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY" , String.class      , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BNK_CODE"  , Integer.class      , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_YEAR"      , Integer.class      , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MONTH_NO"  , Integer.class      , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pMerchantId        , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency      , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency    , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBankCode          , "P_BNK_CODE");
            SP.SetParameter(Colindex++, pYear              , "P_YEAR");
            SP.SetParameter(Colindex++, pMonth             , "P_MONTH_NO");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            String sMonthNo = "";
            String sType = "";
            String sSum  = "";
            String sCnt  = "";
            String sMonthName = "";
            String sYear = "";

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryRec newSum = new DekontSummaryRec();

                sYear      = Util.Database.getValString(RowN, "TXN_YEAR");
                sMonthName = Util.Database.getValString(RowN, "TXN_MONTH_NAME");
                sMonthNo = Util.Database.getValString(RowN, "TXN_MONTH_NO");
                sType    = Util.Database.getValString(RowN, "TXN_TYPE");
                sSum     = Util.Database.getValString(RowN, "SUM");
                sCnt     = Util.Database.getValString(RowN, "CNT");

                newSum.Year    = sYear;
                newSum.MonthName = sMonthName;
                newSum.MonthNo = sMonthNo;
                newSum.Type    = sType;
                newSum.Sum     = sSum;
                newSum.Count   = sCnt;
                
                SumRows.add(newSum);
            }
            
            return SumRows;
        }
        catch(Exception e)
        {
            return SumRows;
        }
    }

    public static DekontSummaryUseRates calculateSummaryUseRates(EntityManager pem, 
                                                                 long pAccountId,
                                                                 String pBaseCurrency, 
                                                                 String pTargetCurrency)
    {
        DekontSummaryUseRates rates = new DekontSummaryUseRates();

        try
        {
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_USE_RATES");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryYear newYear = new DekontSummaryYear();
                
                rates.total             = Util.Database.getValString(RowN, "KASA");
                rates.subtotal_card     = Util.Database.getValString(RowN, "CARD");
                rates.subtotal_cash     = Util.Database.getValString(RowN, "CASH");

                return rates;//only one row
            }

            return rates;
        }
        catch(Exception e)
        {
            return rates;
        }
    }

    public static ArrayList<DekontSummaryYear> calculateSummaryOverall( EntityManager pem, 
                                                                        long pAccountId,
                                                                        String pBaseCurrency, 
                                                                        String pTargetCurrency)
    {
        ArrayList<DekontSummaryYear> overall = new ArrayList<DekontSummaryYear>();

        try
        {
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_OVERALL");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_FROM_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TO_CURRENCY"  , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_FROM_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TO_CURRENCY");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryYear newYear = new DekontSummaryYear();
                
                newYear.year      = Util.Database.getValString(RowN, "YEAR");
                newYear.Sum       = Util.Database.getValString(RowN, "SUM");
                
                
                overall.add(newYear);
            }

            return overall;
        }
        catch(Exception e)
        {
            return overall;
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
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_BY_BANK");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MONTH"     , String.class  , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId              , "P_MRC_ID");
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

    public static ArrayList<DekontSummaryYear> calculateSummaryYears(EntityManager pem,
                                                                     long pAccountId,
                                                                     String pBaseCurrency, 
                                                                     String pTargetCurrency, 
                                                                     String pBaseYear)
    {
        ArrayList<DekontSummaryYear> years = new ArrayList<DekontSummaryYear>(); 

        try
        {
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_BY_YEARS");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"        , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId              , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseCurrency   , "P_BASE_CURRENCY");
            SP.SetParameter(Colindex++, pTargetCurrency , "P_TARGET_CURRENCY");
            SP.SetParameter(Colindex++, pBaseYear       , "P_BASE_YEAR");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryYear newYear = new DekontSummaryYear();

                newYear.year      = Util.Database.getValString(RowN, "YEAR");
                newYear.month     = Util.Database.getValString(RowN, "MONTH");
                newYear.Sum       = Util.Database.getValString(RowN, "SUM");
                newYear.Count     = Util.Database.getValString(RowN, "CNT");

                years.add(newYear);
            }

            return years;

        }
        catch(Exception e)
        {
            return years;
        }
    }

    public static ArrayList<DekontSummaryQuarterDay> calculateSummaryQuarterDays(EntityManager pem, 
                                                                                 long pAccountId,
                                                                                 String pBaseCurrency, 
                                                                                 String pTargetCurrency, 
                                                                                 String pBaseYear)
    {
        ArrayList<DekontSummaryQuarterDay> quarters = new ArrayList<DekontSummaryQuarterDay>();

        try
        {
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS_BY_QUARTER");

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

            return quarters;

        }
        catch(Exception e)
        {
            return quarters;
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
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId              , "P_MRC_ID");
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

    public static ArrayList<DekontSummaryDay> calculateSummaryTargetMonthDayAverages(EntityManager pem, 
                                                                                     long pAccountId,
                                                                                     String pBaseCurrency, 
                                                                                     String pTargetCurrency, 
                                                                                     String pBaseYear, 
                                                                                     String pBaseMonth)
    {
        ArrayList<DekontSummaryDay> days = new ArrayList<DekontSummaryDay>();

        try
        {
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_MONTH_DAYS_AVG");

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

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryDay newDay = new DekontSummaryDay();

                newDay.year         = Util.Database.getValString(RowN, "YEAR");
                newDay.day          = Util.Database.getValString(RowN, "DAY");
                newDay.Avg          = Util.Database.getValString(RowN, "AVG");
                newDay.Sum          = Util.Database.getValString(RowN, "SUM");

                days.add(newDay);
            }

            return days;
        }
        catch(Exception e)
        {
            return days;
        }
    }
    
    public static ArrayList<DekontSummaryDay> calculateSummaryDays(EntityManager pem, 
                                                                   long pAccountId,
                                                                   String pBaseCurrency, 
                                                                   String pTargetCurrency, 
                                                                   String pBaseYear, 
                                                                   String pBaseMonth)
    {
        ArrayList<DekontSummaryDay> days = new ArrayList<DekontSummaryDay>();

        try
        {
            pem.cacheable("P_MRC_ID");
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS");
            
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

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryDay newDay = new DekontSummaryDay();

                newDay.year         = Util.Database.getValString(RowN, "YEAR");
                newDay.day          = Util.Database.getValString(RowN, "DAY");
                newDay.Sum          = Util.Database.getValString(RowN, "SUM");

                days.add(newDay);
            }

            return days;
        }
        catch(Exception e)
        {
            return days;
        }
    }

    public static ArrayList<DekontSummaryWeek> calculateSummaryWeeks(EntityManager pem,
                                                                     long pAccountId,
                                                                     String pBaseCurrency, 
                                                                     String pTargetCurrency,
                                                                     String pBaseYear, 
                                                                     String pBaseMonth)
    {
        ArrayList<DekontSummaryWeek> quarters = new ArrayList<DekontSummaryWeek>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS");

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

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryWeek newWeek = new DekontSummaryWeek();

                newWeek.year         = Util.Database.getValString(RowN, "YEAR");
                newWeek.weekNo       = Util.Database.getValString(RowN, "WEEK");
                newWeek.Sum          = Util.Database.getValString(RowN, "SUM");

                quarters.add(newWeek);
            }

            return quarters;
        }
        catch(Exception e)
        {
            return quarters;
        }
    }

    public static ArrayList<DekontSummaryWeek> calculateSummaryWeeksOfMonth(EntityManager pem,
                                                                            long pAccountId,
                                                                            String pBaseCurrency, 
                                                                            String pTargetCurrency, 
                                                                            String pBaseYear, 
                                                                            String pBaseMonth)
    {
        ArrayList<DekontSummaryWeek> quarters = new ArrayList<DekontSummaryWeek>();

        try
        {
            pem.cacheable("P_MRC_ID");
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

            for (List<RowColumn> RowN:rs)
            {
                DekontSummaryWeek newWeek = new DekontSummaryWeek();

                newWeek.year         = Util.Database.getValString(RowN, "YEAR");
                newWeek.weekNo       = Util.Database.getValString(RowN, "WEEK");
                newWeek.Sum          = Util.Database.getValString(RowN, "SUM");

                quarters.add(newWeek);
            }

            return quarters;
        }
        catch(Exception e)
        {
            return quarters;
        }
    }

    public static boolean updateEOD(EntityManager   pem, 
                                    long            pUserId,
                                    long            pMrcId, 
                                    String          pTxnDate,
                                    
                                    BigDecimal      pbdCashTotal,
                                    BigDecimal      pbdCardTotal,
                                    BigDecimal      pbdWireTotal,
                                    BigDecimal      pbdInternetTotal,
                                    BigDecimal      pbdOtherTotal)
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_SUMMARY_UPDATE_EOD");

            SP.registerStoredProcedureParameter("P_USR_ACC_ID"  , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MRC_ID"      , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_EOD_DATE"    , String.class   , ParameterMode.IN);

            SP.registerStoredProcedureParameter("P_CASH_TOTAL"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CARD_TOTAL"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_WIRE_TOTAL"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_INT_TOTAL"   , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_OTHER_TOTAL" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pUserId                 , "P_USR_ACC_ID");
            SP.SetParameter(Colindex++, pMrcId                  , "P_MRC_ID");
            SP.SetParameter(Colindex++, pTxnDate                , "P_EOD_DATE");

            SP.SetParameter(Colindex++, pbdCashTotal            , "P_AMOUNT");
            SP.SetParameter(Colindex++, pbdCardTotal            , "P_AMOUNT");
            SP.SetParameter(Colindex++, pbdWireTotal            , "P_AMOUNT");
            SP.SetParameter(Colindex++, pbdInternetTotal        , "P_AMOUNT");
            SP.SetParameter(Colindex++, pbdOtherTotal           , "P_AMOUNT");

            SP.execute();

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

    public static ArrayList<ssoAccInvBalanceCore> calculateAccountBalance4Brand(    EntityManager pem, 
                                                                                    long          pAccountId,
                                                                                    String        pAccountName,
                                                                                    long          pBrandId) throws Exception
    {
        ArrayList<ssoAccInvBalanceCore> brandItemBalances = new ArrayList<ssoAccInvBalanceCore>();

        try
        {
            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            Query stmt = pem.createNamedQuery("SsAccInvBrandItemCodes.findItemBalancesByAccIdNBrand", SsAccInvItemStats.class);
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
                newItemBalance.ItemCodeId = Long.parseLong(Util.Database.getValString(rs.get(i), "UID"));
                newItemBalance.ItemCode  = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                newItemBalance.AccountName = Util.Str.wordNormalize(pAccountName);

                newItemBalance.successRate     = Util.Database.getValString(rs.get(i), "SUCCESS_RATE");
                newItemBalance.velocityStartup = Util.Database.getValString(rs.get(i), "VELOCITY_STARTUP");
                newItemBalance.velocityOverall = Util.Database.getValString(rs.get(i), "VELOCITY_OVERALL");

                newItemBalance.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_ENTERED"));
                newItemBalance.quantity.returned = new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_RETURNED"));
                newItemBalance.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_SOLD"));
                newItemBalance.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_ADJ_PLUS"));
                newItemBalance.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_ADJ_MINUS"));

                newItemBalance.quantity.net      = newItemBalance.quantity.received.
                                                                                        subtract(newItemBalance.quantity.returned).
                                                                                        subtract(newItemBalance.quantity.sold).
                                                                                        subtract(newItemBalance.quantity.adjMinus).
                                                                                        add(newItemBalance.quantity.adjPlus).
                                                                                        add(newItemBalance.quantity.revolving);

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
                                                                                    long          pAccountId, 
                                                                                    String        pAccountName,
                                                                                    long          pVendorId
                                                                                 ) throws Exception
    {
        ArrayList<ssoAccInvBalanceCore> brandItemOptionBalances = new ArrayList<ssoAccInvBalanceCore>();

        try
        {
            // We keep this query on Brand level as it is cache. Otherwise , we could've kept it on SsAccInvBrandsItem
            // but it is too large to keep in cahce. 
            // This query added under SsAccInvVendorStats as it is on cache
            // If something added new this table will also be affected and cache will be reset
            // In other words this methods works on cahce this way
            Query stmt = pem.createNamedQuery("SsAccInvBrandItemCodes.findOptionBalancesByAccIdNBrand", SsAccInvItemStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId           , "VENDOR_ID");
            //stmt.SetParameter(index++, pItemCodeId         , "ITEM_CODE_ID");
            //stmt.SetParameter(index++, pYear               , "YEAR");

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
                    sItemCode = Util.Str.wordNormalize(sItemCode);
                    
                    String sLastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");
                    //newBalance.Option    = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPT"));
                    String sOptionsAll      = Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "OPTS_ALL"));// THIS IS JSON
                    JsonObject jsOptionsEntered  = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_ENTERED")));// THIS IS JSON
                    JsonObject jsOptionsReturned = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_RETURNED")));// THIS IS JSON
                    JsonObject jsOptionsSold     = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_SOLD")));// THIS IS JSON
                    JsonObject jsOptionsAdjPlus  = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_ADJ_PLUS")));// THIS IS JSON
                    JsonObject jsOptionsAdjMinus = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_ADJ_MINUS")));// THIS IS JSON

                    //JsonObject jsOptionsAdjPlus     = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_SOLD")));// THIS IS JSON
                    //JsonObject jsOptionsAdjMinus    = Util.JSON.toJsonObject(Util.Str.wordNormalize(Util.Database.getValString(rs.get(i), "QNT_SOLD")));// THIS IS JSON

                    // combine each json
                    // Run in a loop for json for each option add one item 
                    JsonObject jsOptAll = Util.JSON.toJsonObject(sOptionsAll);
                    Set<String> aKeys = Util.JSON.keys(jsOptAll);

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
                            newBalance.Option    = Util.Str.wordNormalize(sKey);
                        }
                        else
                        {
                            newBalance.Option    = sOptGroup + " - " + Util.Str.wordNormalize(sKey);
                        }

                        String sQuantityEntered  = Util.JSON.getValue(jsOptionsEntered,  sKey);
                        String sQuantityReturned = Util.JSON.getValue(jsOptionsReturned, sKey);
                        String sQuantitySold     = Util.JSON.getValue(jsOptionsSold,     sKey);
                        String sQuantityAdjPlus  = Util.JSON.getValue(jsOptionsAdjPlus,  sKey);
                        String sQuantityAdjMinus = Util.JSON.getValue(jsOptionsAdjMinus, sKey);

                        boolean bQuantityEnteredSkip  = false;
                        boolean bQuantityReturnedSkip = false;
                        boolean bQuantitySoldSkip     = false;

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

                        //------------------------------------------------------

                        newBalance.quantity.received = new BigDecimal(sQuantityEntered);
                        newBalance.quantity.returned = new BigDecimal(sQuantityReturned);
                        newBalance.quantity.sold     = new BigDecimal(sQuantitySold);
                        newBalance.quantity.adjPlus  = new BigDecimal(sQuantityAdjPlus);
                        newBalance.quantity.adjMinus = new BigDecimal(sQuantityAdjMinus);

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
                    newBalance.quantity.sold.add(new BigDecimal(Util.Database.getValString(rs.get(i), "QNT_SOLD")));
                    //newBalance.quantity.received = Long.parseLong(Util.Database.getValString(rs.get(i), "QNT_ENTERED"));
                    //newBalance.quantity.returned = Long.parseLong(Util.Database.getValString(rs.get(i), "QNT_RETURNED"));
                    //newBalance.quantity.returned = Long.parseLong(Util.Database.getValString(rs.get(i), "QNT_SOLD"));

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
                                                                            long                     pAccountId,
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
                    long   lVendorId = Long.parseLong(sVendorId);

                    if (lVendorId==brandN.Id)
                    {
                        newItemBalance.AccountId    = pAccountId;
                        newItemBalance.AccountName  = pAccountName;
                        newItemBalance.BrandId      = lVendorId;
                        newItemBalance.Brandname    = brandN.name;
                        newItemBalance.lastActivity = Util.Database.getValString(rs.get(i), "LASTUPDATE");

                        newItemBalance.balance.revolving = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE"));
                        newItemBalance.balance.received  = new BigDecimal(Util.Database.getValString(rs.get(i), "BALANCE_ENTERED")); 
                        newItemBalance.balance.returned  = new BigDecimal(Util.Database.getValString(rs.get(i), "BALANCE_RETURNED")); 
                        newItemBalance.balance.sold      = new BigDecimal(Util.Database.getValString(rs.get(i), "BALANCE_SOLD")); 

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
                                                                long          pUserId,
                                                                long          pAccId,
                                                                long          pBrandId,
                                                                int           pYear,
                                                                int           piStartRowIndex,
                                                                boolean       pbFullRows
                                                                ) throws Exception
    {
        //SELECT LOG.INSERTDATE, LOG.QUANTITY, LOG.AMOUNT, LOG.TXN_TYPE, LOG.REVOLVING_BALANCE, LOG.REVOLVING_QUANTITY, LOG.NEW_QUANTITY_ENTERED, LOG.NEW_QUANTITY_RETURNED, LOG.NEW_QUANTITY_SOLD, LOG.NEW_BALANCE_ENTERED, LOG.NEW_BALANCE_RETURNED, LOG.NEW_BALANCE_SOLD FROM ss_acc_inv_brands_log LOG WHERE STAT = 1 AND FINANCIAL_YEAR = 2021 AND BRAND = 'MODIVA'
        ArrayList<ssoAccStmtCore> brandStatement = new ArrayList<ssoAccStmtCore>();

        try
        {
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);
            else
                pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);

            pem.setRowStartIndex(piStartRowIndex);

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
                    // DEBIT (-) = PAYMENTS, REFUNDS
                    bdBalance    = bdBalance.subtract(bdAmountDebit);
                    //bdTotalDebit = bdTotalDebit.subtract(bdAmountDebit);
                }
                else if(newStmtLine.txnEffect.toUpperCase().equals("C")==true)
                {
                    // CREDIT (+) = BUYS
                    bdBalance     = bdBalance.add(bdAmountCredit);
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
                String quantitySold      = "";//Util.Database.getValString(rs.get(i), "NEW_QUANTITY_SOLD");

                //BigDecimal balanceSold      = new BigDecimal(Util.Database.getValString(rs.get(i), "NEW_BALANCE_SOLD"));
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
                else if (newStmtLine.txnType.equals(INV_TXN_TYPE_SOLD)==true)
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

    public static ssoVendorInfo getVendorProfile(EntityManager pem, 
                                                long          pUserId,
                                                long          pAccId,
                                                long          pVendorId) throws Exception
    {
        try
        {
            ssoVendorInfo vendorInfo = new ssoVendorInfo();

            Query stmt = pem.createNamedQuery("SsAccInvBrands.getVendorPageParams", SsAccInvVendorStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId        , "VENDOR_ID");

            BigDecimal bdBalance = new BigDecimal(BigInteger.ZERO);

            List<List<RowColumn>> rs = stmt.getResultList();
            if(rs.size()>0)  
            {
                ssoAccStmtCore newStmtLine = new ssoAccStmtCore();

                vendorInfo.name                 = Util.Str.wordNormalize(Util.Database.getValString(rs.get(0), "BRAND"));
                vendorInfo.city                 = Util.Database.getValString(rs.get(0), "CITY");
                vendorInfo.revolvingBalance     = Util.Database.getValString(rs.get(0), "REVOLVING_BALANCE");
                vendorInfo.itemsReceived        = Util.Database.getValString(rs.get(0), "NET_TOTAL_ENTERED");
                vendorInfo.itemsSent            = Util.Database.getValString(rs.get(0), "NET_TOTAL_RETURNED");
                vendorInfo.itemsSold            = Util.Database.getValString(rs.get(0), "NET_TOTAL_SOLD");
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

    public static ssoBillShort getBill( EntityManager pem, 
                                        long          pUserId,
                                        long          pBillId) throws Exception
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
                    Bill.BillId         = Long.toString(pBillId);
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
                    Bill.totalGross     = Util.Database.getValString(rs.get(i), "TOTAL_GROSS");

                }

                newBillLine.UID        = Util.Database.getValString(rs.get(i), "ORG_UID");
                newBillLine.ItemCode   = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                newBillLine.Quantity   = Util.Database.getValString(rs.get(i), "QUANTITY");
                newBillLine.Unit       = Util.Database.getValString(rs.get(i), "UNIT");
                newBillLine.EntryPrice = Util.Database.getValString(rs.get(i), "PRICE_ENTRY");
                newBillLine.DiscountRate = Util.Database.getValString(rs.get(i), "LINE_DISCOUNT_RATE");
                newBillLine.Discount   = Util.Database.getValString(rs.get(i), "LINE_DISCOUNT");
                newBillLine.Surcharge  = Util.Database.getValString(rs.get(i), "LINE_SURCHARGE");
                newBillLine.Total      = Util.Database.getValString(rs.get(i), "LINE_NET");

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
                                    long           pUserId,
                                    long           pAccId,
                                    long           pBrandId,
                                    long           pBillId) throws Exception
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
            stmtBrandAcc.SetParameter(index++, pAccId            , "ACC_ID");

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

    public static long registerNewBrand(    EntityManager  pem, 
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

            // STEP 3 Add to Dictionary
            //------------------------------------------------------------------
            ssoAPIResponse rsp = new ssoAPIResponse();
            rsp = DictionaryOps.Vendor.add_VendorNItemCodes(    pem, 
                                                                pUserId,
                                                                pAccId, 
                                                                psBrand.toUpperCase().trim(), 
                                                                "",
                                                                vendor.uid,
                                                                -1,
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

    public static void resetMemoryTables4Account(EntityManager pem, long pUserId) throws Exception
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
    
    public static ssoMerchantPreferences getAccountSettings(EntityManager pem, long pUserId, long pAccountId) throws Exception
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
                mrcPref.CountryCode     = Util.Database.getValString(RowN, "COUNTRY_CODE");
                mrcPref.CountryName     = Util.Database.getValString(RowN, "COUNTRY_NAME");
                mrcPref.StateCode       = Util.Database.getValString(RowN, "STATE_CODE");
                mrcPref.StateName       = Util.Database.getValString(RowN, "STATE_NAME");
                //mrcPref.CountyCode      = Util.Database.getValString(RowN, "COUNTY_CODE");
                mrcPref.PlaceNameUID    = Util.Database.getValString(RowN, "PLACE_NAME_UID");
                mrcPref.PlaceName       = Util.Database.getValString(RowN, "PLACE_NAME");
                mrcPref.email           = Util.Database.getValString(RowN, "EMAIL");
                mrcPref.profileName       = Util.Database.getValString(RowN, "PROFILENAME");
                mrcPref.isTaxInSalesPrice = Util.Database.getValString(RowN, "IS_TAX_INC_PRICE");
                mrcPref.taxRate           = Util.Database.getValString(RowN, "TAX_RATE");
                mrcPref.insDiffRate       = Util.Database.getValString(RowN, "INS_DIFF_RATE");
                mrcPref.isActive          = Util.Database.getValString(RowN, "ACTIVATED");
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

