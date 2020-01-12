/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
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
                                                 String pBaseCurrency, 
                                                 String pTargetCurrency, 
                                                 int pBankCode, 
                                                 int pYear, 
                                                 int pMonth)
    {
        DekontSummary summary = new DekontSummary();

        try
        {

            summary.baseYearDate = Util.DateTime.GetDateTime_s().substring(0,4);
            summary.lastYearDate = Integer.toString(Integer.parseInt(summary.baseYearDate) - 1);

            summary.targetMonth = Util.DateTime.GetDateTime_s().substring(4,6);

            // Overall/Avg Summary regardless of the year
            summary.rows  = calculateSummaryRecords(pem, pBaseCurrency, pTargetCurrency, pBankCode, pYear, pMonth);
            summary.banks = calculateSummaryBankSubtotals(pem, pBaseCurrency, pTargetCurrency, summary.baseYearDate, -1);
            summary.overall =  calculateSummaryOverall(pem, pBaseCurrency, pTargetCurrency);
            summary.years = calculateSummaryYears(pem, pBaseCurrency, pTargetCurrency, summary.baseYearDate);//ay bazinda yillik performans
            

            // Avg + Year (now) + Year (past) summary
            //------------------------------------------------------------------
            summary.Qdays          = calculateSummaryQuarterDays(pem, pBaseCurrency, pTargetCurrency, "-1");//Genel/Avg - Gun Ciro Ortalamasi
            summary.thisYear.Qdays = calculateSummaryQuarterDays(pem, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Gun Ciro Ortalamasi
            summary.lastYear.Qdays = calculateSummaryQuarterDays(pem, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Gun Ciro Ortalamasi

            //Mevsimlik Performanslar
            //------------------------------------------------------------------
            summary.Qweeks           =  calculateSummaryQuarterWeeks(pem, pBaseCurrency, pTargetCurrency,"-1");////Genel/Avg - Week Ciro Ortalamasi
            summary.thisYear.Qweeks  =  calculateSummaryQuarterWeeks(pem, pBaseCurrency, pTargetCurrency,summary.baseYearDate);// Mevsimlik - <This Year> -  Week Ciro Ortalamasi
            summary.lastYear.Qweeks  =  calculateSummaryQuarterWeeks(pem, pBaseCurrency, pTargetCurrency,summary.lastYearDate);// Mevsimlik - <Last Year> -  Week Ciro Ortalamasi

            summary.weeks          = calculateSummaryWeeks(pem, pBaseCurrency, pTargetCurrency, "-1", "-1");//Hafta Bazinda Yillik Performans
            //summary.thisYear.weeks = calculateSummaryWeeks(pem, summary.baseYearDate, "-1");//ignored
            //summary.lastYear.weeks = calculateSummaryWeeks(pem, summary.lastYearDate, "-1");//ignored

            //Aylik Performanslar
            //------------------------------------------------------------------
            summary.currentMonth.days = calculateSummaryDays(pem, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);//Gun bazinda Aylik Performans
            summary.currentMonth.weeks = calculateSummaryWeeksOfMonth(pem, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);//Haftalik Aylik Performans
            summary.currentMonth.dayAvgs = calculateSummaryTargetMonthDayAverages(pem, pBaseCurrency, pTargetCurrency, "-1", summary.targetMonth);
            
            summary.useRates = calculateSummaryUseRates(pem, pBaseCurrency, pTargetCurrency);

            return summary;
        }
        catch(Exception e)
        {
            return summary;
        }
    }

    public static ArrayList<DekontSummaryRec> calculateSummaryRecords(  EntityManager pem, 
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
            SP.SetParameter(Colindex++, -1                 , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1              , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1              , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1              , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1              , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1              , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
