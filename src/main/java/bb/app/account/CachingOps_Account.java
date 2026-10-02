/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.account;

import bb.app.dekonts.DekontSummary;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import jaxesa.persistence.cache.Caching;
import jaxesa.persistence.misc.NamedQueryTypes;
import jaxesa.redis.ssoRedisLettuce;
import jaxesa.util.Util;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Administrator
 */
public final class CachingOps_Account //account dashboard
{
    static String REDIS_KEY_4_ACC_HOME_PAGE = "acc.pg";
    static int    ACC_CACHE_EXPIRY_IN_SECONDS = 60 * 60 * 24;//1 day

    static String SUPPORTED_CURRENCIES = "\"AUD\", \"BRL\", \"CAD\", \"CHF\", \"CNY\", \"DKK\", \"EUR\", \"GBP\", \"ILS\", \"INR\", \"JPY\", \"NOK\", \"RUB\", \"SEK\", \"SGD\", \"TRY\", \"ZAR\", \"BTC\"";

    public static ArrayList<String> getKeysOnEODUpdate(BigInteger pUserId, BigInteger pAccId)
    {
        ArrayList<String> aDBKeys = new ArrayList<String>();
        /*
        aDBKeys.add("calculateSummaryOverall.SP_BB_MRC_CALC_SUMMARY_YEARS");
        aDBKeys.add("calculateSummaryYears.SP_BB_MRC_CALC_SUMMARY_BY_YEARS");
        aDBKeys.add("calculateSummaryRecords.SP_BB_MRC_CALC_SUMMARY");
        aDBKeys.add("calculateSummaryBankSubtotals.SP_BB_MRC_CALC_SUMMARY_BY_BANK");

        aDBKeys.add("calculateSummaryQuarterDays.SP_BB_MRC_CALC_SUMMARY_DAYS_BY_QUARTER");
        aDBKeys.add("calculateSummaryQuarterWeeks.SP_BB_MRC_CALC_SUMMARY_WEEKS_BY_QUARTER");
        aDBKeys.add("calculateSummaryWeeks.SP_BB_MRC_CALC_SUMMARY_WEEKS");
        aDBKeys.add("calculateSummaryDays.SP_BB_MRC_CALC_SUMMARY_DAYS_OF_MONTHS");
        aDBKeys.add("calculateSummaryWeeksOfMonth.SP_BB_MRC_CALC_SUMMARY_WEEKS_OF_MONTH");
        aDBKeys.add("calculateSummaryTargetMonthDayAverages.SP_BB_MRC_CALC_SUMMARY_MONTH_DAYS_AVG");
        aDBKeys.add("calculateSummaryUseRates.SP_BB_MRC_CALC_SUMMARY_USE_RATES");
        aDBKeys.add("calculateEarningStats.");
        aDBKeys.add("calculateQuantityStats.");
        */
        
        // SUMMARY KEYS
        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        ArrayList<String> aSummaryKeys = new ArrayList<String>();
        aSummaryKeys = CachingOps_Account.getSummaryKeys();
        aDBKeys.addAll(aSummaryKeys);

        ArrayList<String> aKeys2Delete = new ArrayList<String>();
        for (String sDBKey:aDBKeys)
        {
            ArrayList<Object> keys = new ArrayList<Object>();
            keys.add(pAccId);
            String sCacheDBKey = Caching.generateCacheKey(NamedQueryTypes.STORED_PROCEDURE,
                                                          "",
                                                          sDBKey,//"calculateSummaryOverall.SP_BB_MRC_CALC_SUMMARY_YEARSL",
                                                          keys);
            aKeys2Delete.add(sCacheDBKey);
        }

        // Also Delete MyStats (Account Home Page Cache Data)
        //--------------------------------------------------------------
        ArrayList<String> aCacheKeys = new ArrayList<String>();
        aCacheKeys = CachingOps_Account.getAllKeys(pUserId, pAccId);
        aKeys2Delete.addAll(aCacheKeys);
        
        return aKeys2Delete;
    }

    public static String generateKey(BigInteger pUserId, BigInteger pAccId, String pCurrency)
    {
        //String sKey = REDIS_KEY_4_ACC_HOME_PAGE + "." + pUserId + "." + pAccId;
        String sKey = REDIS_KEY_4_ACC_HOME_PAGE + "." + pAccId + "." + pCurrency;

        return sKey;
    }
    
    public static ArrayList<String> getAllKeys(BigInteger pUserId, BigInteger pAccId)
    {
        //String[] asCurriencies = SUPPORTED_CURRENCIES.split("\\\\");
        String[] asCurriencies = SUPPORTED_CURRENCIES.replaceAll("\\\"", "").split(",");
        String[] asKeys = new String[asCurriencies.length];
        
        int i =0;
        for(String sCurrency:asCurriencies)
        {
            asKeys[i++] = generateKey(pUserId, pAccId, sCurrency.trim());
        }

         ArrayList<String> list = new ArrayList<>(Arrays.asList(asKeys));

         return list;
    }

    public static ArrayList<String> getSummaryKeys()
    {
        ArrayList<String> aDBKeys = new ArrayList<String>();
        
        aDBKeys.add("calculateSummaryOverall.SP_BB_MRC_CALC_SUMMARY_YEARS");
        aDBKeys.add("calculateSummaryYears.SP_BB_MRC_CALC_SUMMARY_BY_YEARS");
        aDBKeys.add("calculateSummaryRecords.SP_BB_MRC_CALC_SUMMARY");
        aDBKeys.add("calculateSummaryBankSubtotals.SP_BB_MRC_CALC_SUMMARY_BY_BANK");

        aDBKeys.add("calculateSummaryQuarterDays.SP_BB_MRC_CALC_SUMMARY_DAYS_BY_QUARTER");
        aDBKeys.add("calculateSummaryQuarterWeeks.SP_BB_MRC_CALC_SUMMARY_WEEKS_BY_QUARTER");
        aDBKeys.add("calculateSummaryWeeks.SP_BB_MRC_CALC_SUMMARY_WEEKS");
        aDBKeys.add("calculateSummaryDays.SP_BB_MRC_CALC_SUMMARY_DAYS_OF_MONTHS");
        aDBKeys.add("calculateSummaryWeeksOfMonth.SP_BB_MRC_CALC_SUMMARY_WEEKS_OF_MONTH");
        aDBKeys.add("calculateSummaryTargetMonthDayAverages.SP_BB_MRC_CALC_SUMMARY_MONTH_DAYS_AVG");
        aDBKeys.add("calculateSummaryUseRates.SP_BB_MRC_CALC_SUMMARY_USE_RATES");
        aDBKeys.add("calculateEarningStats.");
        aDBKeys.add("calculateQuantityStats.");

        return aDBKeys;
    }

    public static void deleteAccPageData(ssoRedisLettuce lettuce, BigInteger pUserId, BigInteger pAccId)
    {
        ArrayList<String> aAllKeys = new ArrayList<String>();
        
        aAllKeys = getAllKeys(pUserId, pAccId);
        
        String[] asKeys = aAllKeys.toArray(new String[0]);

        Util.Redis.JString.remove(lettuce, asKeys);
    }

    public static void saveAccPageData(ssoRedisLettuce  lettuce, 
                                       BigInteger             pUserId, 
                                       BigInteger             pAccId,
                                       String           pCurrency,//default empty
                                       DekontSummary    pAccHomeData)
    {
        String sKey = generateKey(pUserId, pAccId, pCurrency);

        String sData = Util.JSON.toString(pAccHomeData);

        Util.Redis.JString.set(lettuce, sKey, sData, ACC_CACHE_EXPIRY_IN_SECONDS);
    }

    public static DekontSummary getAccPageData(ssoRedisLettuce   lettuce, 
                                               BigInteger              pUserId, 
                                               BigInteger              pAccId, 
                                               String            pCurrency)
    {
        DekontSummary accDashboardPageData = new DekontSummary();

        try
        {
            String sKey = generateKey(pUserId, pAccId, pCurrency);

            String sData = Util.Redis.JString.get(lettuce, sKey);

            if(sData!=null)
            {
                accDashboardPageData = (DekontSummary)Util.JSON.toObject(sData, DekontSummary.class);
                
                return accDashboardPageData;
            }
            else
            {
                return null;
            }
            
        }
        catch(Exception e)
        {
            return null;
        }
    }

}

