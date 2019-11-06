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

    public static DekontSummary calculateSummary(EntityManager pem, int pBankCode, int pYear, int pMonth)
    {
        DekontSummary summary = new DekontSummary();
        
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BNK_CODE"  , Integer.class      , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_YEAR"      , Integer.class      , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MONTH_NO"  , Integer.class      , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBankCode      , "P_BNK_CODE");
            SP.SetParameter(Colindex++, pYear          , "P_YEAR");
            SP.SetParameter(Colindex++, pMonth         , "P_MONTH_NO");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();
            String sMonthNo = "";
            String sType = "";
            String sSum  = "";
            String sCnt  = "";
            String sMonthName = "";
            String sYear = "";

            ArrayList<DekontSummaryRec> SumRows = new ArrayList<DekontSummaryRec>();

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

            summary.baseYear = Util.DateTime.GetDateTime_s().substring(0,4);

            summary.targetMonth = Util.DateTime.GetDateTime_s().substring(4,6);

            summary.rows  = SumRows;
            summary.banks = calculateSummaryBankSubtotals(pem, summary.baseYear, -1);
            summary.years = calculateSummaryYears(pem, summary.baseYear);

            summary.Qdays = calculateSummaryQuarterDays(pem, "-1");//ALL
            summary.current.Qdays = calculateSummaryQuarterDays(pem, summary.baseYear);

            summary.Qweeks          =  calculateSummaryQuarterWeeks(pem, "-1");//ALL
            summary.current.Qweeks  =  calculateSummaryQuarterWeeks(pem, summary.baseYear);

            summary.weeks         = calculateSummaryWeeks(pem, "-1", "-1");
            summary.current.weeks = calculateSummaryWeeks(pem, summary.baseYear, "-1");

            summary.currentMonth.days = calculateSummaryDays(pem, "-1", summary.targetMonth);

            summary.currentMonth.weeks = calculateSummaryWeeksOfMonth(pem, "-1", summary.targetMonth);

            summary.overall =  calculateSummaryOverall(pem);

            summary.useRates = calculateSummaryUseRates(pem);

            return summary;
        }
        catch(Exception e)
        {
            return summary;
        }
    }

    public static DekontSummaryUseRates calculateSummaryUseRates(EntityManager pem)
    {
        DekontSummaryUseRates rates = new DekontSummaryUseRates();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_USE_RATES");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");

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

    public static ArrayList<DekontSummaryYear> calculateSummaryOverall(EntityManager pem)
    {
        ArrayList<DekontSummaryYear> overall = new ArrayList<DekontSummaryYear>();
        
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_OVERALL");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");

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
    
    public static ArrayList<DekontSummaryTots> calculateSummaryBankSubtotals(EntityManager pem,String pBaseYear, int pMonth)
    {
        ArrayList<DekontSummaryTots> bankCodes = new ArrayList<DekontSummaryTots>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_BY_BANK");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_MONTH"     , String.class  , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");
            SP.SetParameter(Colindex++, pMonth         , "P_MONTH");

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

    public static ArrayList<DekontSummaryYear> calculateSummaryYears(EntityManager pem,String pBaseYear)
    {
        ArrayList<DekontSummaryYear> years = new ArrayList<DekontSummaryYear>(); 

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_BY_YEARS");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");

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

    public static ArrayList<DekontSummaryQuarterDay> calculateSummaryQuarterDays(EntityManager pem,String pBaseYear)
    {
        ArrayList<DekontSummaryQuarterDay> quarters = new ArrayList<DekontSummaryQuarterDay>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");

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

    public static ArrayList<DekontSummaryQuarterWeek> calculateSummaryQuarterWeeks(EntityManager pem,String pBaseYear)
    {
        ArrayList<DekontSummaryQuarterWeek> quarters = new ArrayList<DekontSummaryQuarterWeek>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS_BY_QUARTER");

            SP.registerStoredProcedureParameter("P_MRC_ID"    , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
            SP.SetParameter(Colindex++, pBaseYear      , "P_BASE_YEAR");

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

    public static ArrayList<DekontSummaryDay> calculateSummaryTargetMonthDayAverages(EntityManager pem, String pBaseYear, String pBaseMonth)
    {
        ArrayList<DekontSummaryDay> days = new ArrayList<DekontSummaryDay>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_MONTH_DAYS_AVG");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
    
    public static ArrayList<DekontSummaryDay> calculateSummaryDays(EntityManager pem,String pBaseYear, String pBaseMonth)
    {
        ArrayList<DekontSummaryDay> days = new ArrayList<DekontSummaryDay>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_DAYS");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
    
    public static ArrayList<DekontSummaryWeek> calculateSummaryWeeks(EntityManager pem,String pBaseYear, String pBaseMonth)
    {
        ArrayList<DekontSummaryWeek> quarters = new ArrayList<DekontSummaryWeek>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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

    
    public static ArrayList<DekontSummaryWeek> calculateSummaryWeeksOfMonth(EntityManager pem,String pBaseYear, String pBaseMonth)
    {
        ArrayList<DekontSummaryWeek> quarters = new ArrayList<DekontSummaryWeek>();

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_BB_MRC_CALC_SUMMARY_WEEKS_OF_MONTH");

            SP.registerStoredProcedureParameter("P_MRC_ID"     , Long.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_YEAR"  , String.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BASE_MONTH" , String.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, -1             , "P_MRC_ID");
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
