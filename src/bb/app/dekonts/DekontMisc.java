/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import entity.mrc.SsMrcEarnings;
import entity.mrc.SsMrcStatsEarnings;
import entity.mrc.SsMrcStatsQuantity;
import entity.user.SsUsrAccounts;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import jaxesa.log.LogManager;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.util.Util;

/**
 *
 * @author esabil
 */
public final class DekontMisc 
{
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

    public static DekontSummary calculateSummary(EntityManager pem, 
                                                 long pAccountId, //merchant Id
                                                 String pMerchantName,
                                                 String pBaseCurrency, 
                                                 String pTargetCurrency, 
                                                 int pBankCode, 
                                                 int pYear, 
                                                 int pMonth)
    {
        DekontSummary summary = new DekontSummary();

        try
        {
            summary.currency = pBaseCurrency;
            summary.baseYearDate = Util.DateTime.GetDateTime_s().substring(0,4);
            summary.lastYearDate = Integer.toString(Integer.parseInt(summary.baseYearDate) - 1);

            summary.targetMonth = Util.DateTime.GetDateTime_s().substring(4,6);

            // Overall/Avg Summary regardless of the year
            summary.rows  = calculateSummaryRecords(pem, pAccountId, pBaseCurrency, pTargetCurrency, pBankCode, pYear, pMonth);
            summary.banks = calculateSummaryBankSubtotals(pem, pAccountId, pBaseCurrency, pTargetCurrency, summary.baseYearDate, -1);
            summary.overall =  calculateSummaryOverall(pem, pAccountId, pBaseCurrency, pTargetCurrency);
            summary.years = calculateSummaryYears(pem, pAccountId, pBaseCurrency, pTargetCurrency, summary.baseYearDate);//ay bazinda yillik performans
            

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
            RetailEarningStats = calculateEarningStats(pem, pAccountId);//for test
            AllEarningStats    = calculateEarningStats(pem, -1);
            
            if (RetailEarningStats!=null)
                summary.earnings.addAll(RetailEarningStats);

            if (AllEarningStats!=null)
                summary.earnings.addAll(AllEarningStats);

            //QuantityStats
            //------------------------------------------------------------------
            ArrayList<DekontQuantityStats> RetailQuantityStats = new ArrayList<DekontQuantityStats>();
            ArrayList<DekontQuantityStats> AllQuantityStats    = new ArrayList<DekontQuantityStats>();
            RetailQuantityStats = calculateQuantityStats(pem, pAccountId);//for test
            AllQuantityStats    = calculateQuantityStats(pem, -1);

            summary.quantities.addAll(RetailQuantityStats);
            summary.quantities.addAll(AllQuantityStats);

            //Dashboard
            //------------------------------------------------------------------
            DecimalFormat df2 = new DecimalFormat("#.##");
            df2.setRoundingMode(RoundingMode.UP);
            
            DecimalFormat df3 = new DecimalFormat("#.###");
            df3.setRoundingMode(RoundingMode.UP);
            //Retail YEar Earning
            DekontEarningStats retailN = new DekontEarningStats();
            retailN = getLastEarningValue(summary.earnings, pAccountId);
            //retailN = RetailEarningStats.get(0);

            //RETAIL - YEAR EARNING
            summary.dashboard.yearearning_mybiz.id = 1;//Retail-YE
            summary.dashboard.yearearning_mybiz.Title = pMerchantName;
            summary.dashboard.yearearning_mybiz.Value = retailN.YearEarning;
            summary.dashboard.yearearning_mybiz.ChangeInValue = df2.format(retailN.diffYearEarning).toString();
            summary.dashboard.yearearning_mybiz.ChangeInPerc  = df2.format(Double.parseDouble(retailN.changeYearEarning)).toString();
            summary.dashboard.yearearning_mybiz.Change2Yesterday = df2.format(Double.parseDouble(retailN.changeYearEarning2Yesterday)).toString();
            summary.dashboard.yearearning_mybiz.dtime = retailN.dtime;

            //RETAIL - YEAR TO DATE
            summary.dashboard.ytd.id = 2;//Retail-YTD
            summary.dashboard.ytd.Title = pMerchantName + " - ytd";
            summary.dashboard.ytd.Value = retailN.YTDEarning;
            summary.dashboard.ytd.ChangeInValue = df2.format(retailN.diffYTD);
            summary.dashboard.ytd.ChangeInPerc  = df2.format(Double.parseDouble(retailN.changeYTDEarning)).toString();
            summary.dashboard.ytd.dtime = retailN.dtime;

            //MARKET YEAR EARNING
            DekontEarningStats market = new DekontEarningStats();
            market = getLastEarningValue(summary.earnings, -1);
            //market = AllEarningStats.get(0);

            summary.dashboard.yearearning_market.id = 3;//Market
            summary.dashboard.yearearning_market.Title = "Market";
            summary.dashboard.yearearning_market.Value = market.YearEarning;
            summary.dashboard.yearearning_market.ChangeInValue = df2.format(market.diffYearEarning).toString();
            summary.dashboard.yearearning_market.ChangeInPerc  = df2.format(Double.parseDouble(market.changeYearEarning)).toString();
            summary.dashboard.yearearning_market.Change2Yesterday = df2.format(Double.parseDouble(market.changeYearEarning2Yesterday)).toString();
            summary.dashboard.yearearning_market.dtime = market.dtime;

            //RETAIL QUANTITY 
            DekontQuantityStats retailQuantity = new DekontQuantityStats();
            retailQuantity = getLastQuantitiesValue(summary.quantities, pAccountId);
            //retailQuantity = RetailQuantityStats.get(0);

            summary.dashboard.quantity.id = 4;
            summary.dashboard.quantity.Title = "Quantity Index";
            summary.dashboard.quantity.Value = df2.format(retailQuantity.value).toString();
            summary.dashboard.quantity.ChangeInValue = df2.format(retailQuantity.diff).toString();//N/A
            summary.dashboard.quantity.ChangeInPerc = df2.format(retailQuantity.change).toString();
            summary.dashboard.quantity.dtime = retailQuantity.refDate;

            return summary;
        }
        catch(Exception e)
        {
            return summary;
        }
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
                                                                          long pAccountId
                                                                       )
    {
        ArrayList<DekontQuantityStats> stats = new ArrayList<DekontQuantityStats>();

        try
        {
            String stStmt = "";

            // -1 STORED FOR MARKET 
            stStmt = "SELECT * FROM ss_mrc_stats_quantity " + 
                     " WHERE " +
                     " STAT = 1 AND ACCOUNT_ID = " + pAccountId + //ORDER BY UID DESC";
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

            Query newQuery = pem.CreateQuery(stStmt);

            List<SsMrcStatsQuantity> rs =  newQuery.getResultList(SsMrcStatsQuantity.class);

            double dLastScore = 0;
            double dChange = 0;
            
            int iDayNo = 0;
            for (SsMrcStatsQuantity statsN:rs)
            {
                DekontQuantityStats newStats = new DekontQuantityStats();

                long lRefDate    = statsN.referenceDate;

                newStats.id      = pAccountId;
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
                                                                        long pAccountId
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
                                    " STAT = 1 AND ACCOUNT_ID = " + pAccountId + //ORDER BY UID DESC";
                                    " ORDER BY REFERENCE_DATE DESC " +
                                    " LIMIT 91 " + 
                     ") T " + 
                     "ORDER BY T.REFERENCE_DATE ASC";

            Query newQuery = pem.CreateQuery(stStmt);

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
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_USE_RATES");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
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
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_OVERALL");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_FROM_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TO_CURRENCY"  , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId              , "P_MRC_ID");
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
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"           , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_CURRENCY"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TARGET_CURRENCY"  , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"        , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId              , "P_MRC_ID");
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

    public static boolean updateEOD(EntityManager pem, String pMrcId, String pTxnDate, String pAmount)
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_SUMMARY_UPDATE_EOD");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_EOD_DATE"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_AMOUNT"    , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pTxnDate       , "P_EOD_DATE");
            SP.SetParameter(Colindex++, pAmount        , "P_AMOUNT");

            SP.execute();

            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    public static boolean isEODAdded(EntityManager pem, String pMrcId, String pTxnDate)
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_SUMMARY_IS_EOD_ADDED");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_TXN_DATE"  , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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

        }
        catch(Exception e)
        {
            return false;
        }
    }
}
