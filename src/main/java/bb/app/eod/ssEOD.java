/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.eod;

import bb.app.account.CachingOps_Account;
import static bb.app.account.CachingOps_Account.getAllKeys;
import bb.app.settings.UXParams;
import entity.mrc.SsMrcCashRegEod;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.cache.Caching;
import jaxesa.persistence.misc.NamedQueryTypes;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.redis.ssoRedisLettuce;
import jaxesa.util.Util;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Administrator
 */
public final class ssEOD 
{
    public static ArrayList<ssoEODDets> getEODHistory(EntityManager   pem,
                                                      BigInteger            pAccId,
                                                      boolean         pbInit,
                                                      boolean         pbFullRows,
                                                      int             piLastEODDate,
                                                      String          psKeyword,//Date or Amount
                                                      String          pYearFilter) throws Exception
    {
        ArrayList<ssoEODDets> eodHistory = new ArrayList<ssoEODDets>();
        //int iLastNDays = 365 * 3;//default 3 years
        int iLastEODDate = 99999999;//yyyymmdd
        int iMaxRow = 366;

        try
        {
            if(pbInit==true)
            {
                pem.flush();
            }
            
            if(piLastEODDate!=0)
                iLastEODDate = piLastEODDate; 
            /*
            if(psKeywordYYYYMMDD.trim().length()>0)
            {
                iLastNDays = 365 * 10;// 10 years with keyword
            }

            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);//max load 5 years
            else
                pem.setMaxRowNumber(iLastNDays);//last 5 years
            */

            //pem.setRowStartIndex(piStartRowIndex);
            /*
            if(psKeyword.trim().length()!=0)
            {
                if(pYearFilter.trim().length()==0)//all years selected
                {
                    iMaxRow = 366 * 7;//max 7 years
                }
            }
            */
            //memory data
            Query stmt = pem.createNamedQuery("SsMrcDataEod.getEODHistory", SsMrcCashRegEod.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId                        , "ACCOUNT_ID");
            stmt.SetParameter(index++, iLastEODDate                  , "P_TXN_DATE");
            stmt.SetParameter(index++, pYearFilter.trim()            , "P_TXN_YEAR_LENGTH");
            stmt.SetParameter(index++, pYearFilter                   , "P_TXN_YEAR");
            stmt.SetParameter(index++, iMaxRow                       , "P_MAX_ROW");

            boolean bAdd = true;
            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                if(psKeyword.trim().length()>0)
                    bAdd = false;//keyword match
                else
                    bAdd = true;//empty fetch all

                ssoEODDets eodN = new ssoEODDets();
                
                String sEodDate      = Util.Database.getValString(rs.get(i), "TXN_DATE");
                String sOverallTotal = Util.Database.getValString(rs.get(i), "FNL_ALL_TOTAL");
                
                if(sEodDate.trim().equals("20241216")==true)
                    sEodDate = sEodDate;
                
                //eodN.key             = pAccId + "-" + Util.Database.getValString(rs.get(i), "UID");
                eodN.key             = pAccId + "-" + sEodDate;
                eodN.parentKey       = "";

                if(psKeyword.trim().length()>0)
                {
                    boolean bNumericComparisonON = false;

                    if( (psKeyword.substring(0,1).equals(">")==true) ||
                        (psKeyword.substring(0,1).equals("<")==true) ||
                        (psKeyword.substring(0,1).equals("=")==true) )
                        bNumericComparisonON = true;

                    String sFormattedDate = sEodDate.substring(6, 8) + "." +//dd 
                                            sEodDate.substring(4, 6) + "." +//mm
                                            sEodDate.substring(0, 4);  //yyyy
                    //if(sEodDate.trim().equals(psKeywordYYYYMMDD.trim())!=true)
                    
                    if(bNumericComparisonON!=true)
                    {
                        if( (sFormattedDate.indexOf(psKeyword) >= 0) ||
                            (Util.Str.SIMIL(sEodDate, psKeyword.trim()) >= 85) ||
                            (Util.Str.SIMIL(sOverallTotal, psKeyword.trim()) >= 85)
                          )
                        {
                            bAdd = true;
                        }
                        else
                            bAdd = false;
                    }

                    if(bAdd==false)
                    {
                        if(psKeyword.trim().length()>0)
                        {
                            if(bNumericComparisonON==true)
                            {
                                String sComparison = psKeyword.substring(0,1);
                                String sFormattedKeyword =  psKeyword.substring(1).trim().replaceAll(",", "");
                                String sFormattedTotal   =  sOverallTotal.replaceAll(",", "");
                                if (Util.Str.isNumeric(sFormattedKeyword)==true)
                                {

                                    BigDecimal bdKeyword  = new BigDecimal(sFormattedKeyword);
                                    BigDecimal bdEODTotal = new BigDecimal(sOverallTotal);

                                    if( (sComparison.equals(">")==true) && (bdEODTotal.compareTo(bdKeyword)> 0))
                                    {
                                        bAdd = true;
                                    }
                                    else if( (sComparison.equals("<")==true) && (bdEODTotal.compareTo(bdKeyword)< 0))
                                    {
                                        bAdd = true;
                                    }
                                    else if( (sComparison.equals("=")==true) && (bdEODTotal.compareTo(bdKeyword)== 0))
                                    {
                                        bAdd = true;
                                    }
                                }
                            }
                        }
                    }
                }


                if(bAdd==true)
                {
                    String sSource       = Util.Database.getValString(rs.get(i), "SOURCE");

                    String sCashTotal    = Util.Database.getValString(rs.get(i), "FNL_CASH_TOTAL");
                    String sCardTotal    = Util.Database.getValString(rs.get(i), "FNL_CARD_TOTAL");
                    String sWireTotal    = Util.Database.getValString(rs.get(i), "FNL_WIRE_TOTAL");
                    String sOnlineTotal  = Util.Database.getValString(rs.get(i), "FNL_DELIVERY_TOTAL");
                    String sOtherTotal   = Util.Database.getValString(rs.get(i), "FNL_OTHER_TOTAL");

                    // this is a auto-generated column

                    eodN.eodDate    = sEodDate;
                    eodN.source     = sSource;
                    eodN.cashTot    = sCashTotal;
                    eodN.cardTot    = sCardTotal;
                    eodN.wireTot    = sWireTotal;
                    eodN.onlineTot  = sOnlineTotal;
                    eodN.otherTot   = sOtherTotal;

                    eodN.OverallTot = sOverallTotal;

                    eodHistory.add(eodN);
                }
            }

            return eodHistory;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    /*
    public static boolean addNewEOD(EntityManager   pem, 
                                    long            pOperationUserId,
                                    long            pMrcAccId,
                                    String          psDay,
                                    String          psMonth,
                                    String          psYear,
                                    String          psCashTotal,
                                    String          psCardTotal,
                                    String          psWireTotal,
                                    String          psInternetTotal,
                                    String          psOtherTotal
                                    ) throws Exception
    */
    public static boolean addNewEOD(EntityManager         pem, 
                                    ssoRedisLettuce       lettuce,
                                    BigInteger                  pOperationUserId,
                                    BigInteger                  pMrcAccId,
                                    ArrayList<ssoEODDets> paEODRecs
                                    ) throws Exception

    {
        ArrayList<SsMrcCashRegEod> aEODNewRecs = new ArrayList<SsMrcCashRegEod>();
        ArrayList<SsMrcCashRegEod> aEODExistingRecs = new ArrayList<SsMrcCashRegEod>();

        try
        {
            // Get All EODs to date
            ArrayList<String> aCurrentEODs = new ArrayList<String>();
            aCurrentEODs = getEODList(pem, pOperationUserId, pMrcAccId);
            for(ssoEODDets eodN: paEODRecs)
            {

                BigDecimal bdCashTotal     = new BigDecimal(eodN.cashTot.replaceAll(",", "").replaceAll("\\.", ""));
                BigDecimal bdCardTotal     = new BigDecimal(eodN.cardTot.replaceAll(",", "").replaceAll("\\.", ""));
                BigDecimal bdWireTotal     = new BigDecimal(eodN.wireTot.replaceAll(",", "").replaceAll("\\.", ""));
                BigDecimal bdInternetTotal = new BigDecimal(eodN.onlineTot.replaceAll(",", "").replaceAll("\\.", ""));
                BigDecimal bdOtherTotal    = new BigDecimal(eodN.otherTot.replaceAll(",", "").replaceAll("\\.", ""));

                //String sDate = psYear + "-" + psMonth + "-" + psDay;
                String sYear  = eodN.eodDate.substring(0,4);//YYYYMMDD
                String sMonth = eodN.eodDate.substring(4,6);
                String sDay   = eodN.eodDate.substring(6,8); 

                //String sDate = psYear + "-" + psMonth + "-" + psDay;

                String sDate = sYear + sMonth + sDay;

                if(sDate.equals("20201131")==true)
                     sDate = sDate;

                boolean bDateValid = Util.DateTime.isDateValidYYYYMMDD(sDate);
                if(bDateValid==false)
                {
                    continue;//ignore this record
                }

                SsMrcCashRegEod eodTxn = new SsMrcCashRegEod();
                eodTxn.stat        = 1;
                eodTxn.byuser      = pOperationUserId.toString();
                eodTxn.accountId   = pMrcAccId.toString();
                //cashTxn.txnAmount   = sTot;

                eodTxn.txnDesc     = "";
                eodTxn.txnDate     = Integer.parseInt(eodN.eodDate);
                eodTxn.txnMonthNo  = sMonth;
                //eodTxn.txnType     = "KASA";
                //cashTxn.txnAmount   = psTot;

                //eodTxn.sysCashTotal     = bdCashTotal;// CLOSED BECAUSE SYS is not calculated here. It is calculated by EOD. But Manual always take priority (the one in effect)
                eodTxn.fnlCashTotal     = bdCashTotal;// this might be overwritten by manual

                //eodTxn.sysCardTotal     = bdCardTotal;
                eodTxn.fnlCardTotal     = bdCardTotal;

                //eodTxn.sysWireTotal     = bdWireTotal;
                eodTxn.fnlWireTotal     = bdWireTotal;

                //eodTxn.sysInternetTotal = bdInternetTotal;
                eodTxn.fnlDeliveryTotal = bdInternetTotal;//kapida odeme

                //eodTxn.sysOtherTotal    = bdOtherTotal;
                eodTxn.fnlRetInsTotal    = bdOtherTotal;//elden taksit
                eodTxn.source           = "MNL";//NOT EOD SYS

                //boolean rc = bb.app.account.AccountMisc.isEODAdded(pem, pMrcAccId, sDate);
                boolean rc = isEODInList(sDate, aCurrentEODs);
                if (rc==false)
                {
                    //Calculate Last 12 months total (from txnDate)


                    //eodTxn.fnlAllTotal    = bdOverallTotal;// THIS IS GENERATED COLUMN
                    //long lUID = pem.persist(eodTxn);
                    aEODNewRecs.add(eodTxn);

                }
                else
                {
                    aEODExistingRecs.add(eodTxn);
                }

            }// end of for

            if (aEODNewRecs.size()>0)
            {
                int[] laAffectedRowNumber = pem.persistAll(aEODNewRecs, false);
            }

            // Already Exists so just update the numberss 
            boolean bUpdateSuccess = false;
            int iNumberOfRecords = aEODExistingRecs.size();
            if(iNumberOfRecords>0)
            {
                bUpdateSuccess = bb.app.account.AccountMisc.updateEODAll(   pem, 
                                                                            pOperationUserId,
                                                                            pMrcAccId, 
                                                                            aEODExistingRecs
                                                                            /*
                                                                            sDate, 
                                                                            bdCashTotal,
                                                                            bdCardTotal,
                                                                            bdWireTotal,
                                                                            bdInternetTotal,
                                                                            bdOtherTotal*/
                                                                            );
            }

            //--------------------------------------------------
            // RESET MEMORY / CACHE
            //--------------------------------------------------
            if((bUpdateSuccess==true) || (aEODNewRecs.size()>0))// New or Update
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
                ArrayList<String> aSummaryKeys = new ArrayList<String>();
                aSummaryKeys = CachingOps_Account.getSummaryKeys();// THIS IS NO LONGER NEEDED (DESIGN CHANGE - ALL DATA ON THE ROWS)
                
                
                ArrayList<String> aKeys2Delete = new ArrayList<String>();
                for (String sDBKey:aSummaryKeys)
                {
                    ArrayList<Object> keys = new ArrayList<Object>();
                    keys.add(pMrcAccId);
                    String sCacheDBKey = Caching.generateCacheKey(NamedQueryTypes.STORED_PROCEDURE,
                                                                  "",
                                                                  sDBKey,//"calculateSummaryOverall.SP_BB_MRC_CALC_SUMMARY_YEARSL",
                                                                  keys);
                    aKeys2Delete.add(sCacheDBKey);
                }

                // Also Delete MyStats (Account Home Page Cache Data)
                //--------------------------------------------------------------
                ArrayList<String> aCacheKeys = new ArrayList<String>();
                aCacheKeys = CachingOps_Account.getAllKeys(pOperationUserId, pMrcAccId);
                aKeys2Delete.addAll(aCacheKeys);

                // Convert
                //--------------------------------------------------------------
                String[] asKeys2Delete = aKeys2Delete.toArray(new String[0]);

                // Delete 
                //--------------------------------------------------------------
                Util.Redis.JString.remove(lettuce, asKeys2Delete);

            }

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<String> getEODList(EntityManager pem, BigInteger pUserId, BigInteger pAccId) throws Exception
    {
        ArrayList<String> aEODList = new ArrayList<String>();

        try
        {
            Query stmt = pem.CreateNativeQuery("SELECT TXN_DATE FROM ss_mrc_cashreg_eod WHERE STAT = 1 AND ACCOUNT_ID = ?");
            int index = 1;
            stmt.SetParameter(index++, pAccId      , "ACCOUNT_ID");

            boolean bAdd = true;
            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                bAdd = true;
                ssoEODDets eodN = new ssoEODDets();

                String sEODDate = Util.Database.getValString(rs.get(i), "TXN_DATE");

                aEODList.add(sEODDate);
            }

            return aEODList;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean isEODInList(String pEODDate, ArrayList<String> paList)
    {
        for(String sEODN: paList)
        {
            if(pEODDate.trim().equals(sEODN.trim())==true)
                return true;
        }
        
        return false;
    }
}
