/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.eod;

import bb.app.settings.UXParams;
import entity.mrc.SsMrcCashRegEod;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class ssEOD 
{
    public static ArrayList<ssoEODDets> getEODHistory(EntityManager   pem,
                                                      long            pAccId,
                                                      boolean         pbInit,
                                                      boolean         pbFullRows,
                                                      int             piStartRowIndex,
                                                      String          psKeywordYYYYMMDD) throws Exception
    {
        ArrayList<ssoEODDets> eodHistory = new ArrayList<ssoEODDets>();
        int iLastNDays = 365 * 3;//default 3 years

        try
        {
            if(pbInit==true)
            {
                pem.flush();
            }
            
            if(psKeywordYYYYMMDD.trim().length()>0)
            {
                iLastNDays = 365 * 10;// 10 years
            }
            
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);//max load
            else
                pem.setMaxRowNumber(iLastNDays);//last 5 years
                //pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);

            pem.setRowStartIndex(piStartRowIndex);
            
            //memory data
            Query stmt = pem.createNamedQuery("SsMrcDataEod.getEODHistory", SsMrcCashRegEod.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId     , "ACCOUNT_ID");
            stmt.SetParameter(index++, iLastNDays , "LAST_N_DAY");
            //stmt.SetParameter(index++, psKeywordYYYYMMDD , "TXN_DATE");
            //stmt.SetParameter(index++, psKeywordYYYYMMDD , "TXN_DATE");//pointless because the data is memory
            //stmt.SetParameter(index++, iLastNDays , "LAST_N_DAY");//1 YEAR
            
            boolean bAdd = true;
            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                bAdd = true;
                ssoEODDets eodN = new ssoEODDets();

                eodN.key             = pAccId + "-" + Util.Database.getValString(rs.get(i), "UID");
                eodN.parentKey       = "";

                String sEodDate      = Util.Database.getValString(rs.get(i), "TXN_DATE");
                
                if(psKeywordYYYYMMDD.trim().length()>0)
                {
                    //if(sEodDate.trim().equals(psKeywordYYYYMMDD.trim())!=true)
                    if(sEodDate.trim().indexOf(psKeywordYYYYMMDD.trim())>=0)
                    {
                        bAdd = true;
                    }
                    else
                        bAdd = false;
                }
                

                if(bAdd==true)
                {
                    String sSource       = Util.Database.getValString(rs.get(i), "SOURCE");

                    String sCashTotal    = Util.Database.getValString(rs.get(i), "FNL_CASH_TOTAL");
                    String sCardTotal    = Util.Database.getValString(rs.get(i), "FNL_CARD_TOTAL");
                    String sWireTotal    = Util.Database.getValString(rs.get(i), "FNL_WIRE_TOTAL");
                    String sOnlineTotal  = Util.Database.getValString(rs.get(i), "FNL_INTERNET_TOTAL");
                    String sOtherTotal   = Util.Database.getValString(rs.get(i), "FNL_OTHER_TOTAL");

                    // this is a auto-generated column
                    String sOverallTotal = Util.Database.getValString(rs.get(i), "FNL_ALL_TOTAL");

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
    {
        try
        {
            BigDecimal bdCashTotal     = new BigDecimal(psCashTotal);
            BigDecimal bdCardTotal     = new BigDecimal(psCardTotal);
            BigDecimal bdWireTotal     = new BigDecimal(psWireTotal);
            BigDecimal bdInternetTotal = new BigDecimal(psInternetTotal);
            BigDecimal bdOtherTotal    = new BigDecimal(psOtherTotal);

            //String sDate = psYear + "-" + psMonth + "-" + psDay;
            String sYear  = psYear;
            String sMonth = psMonth;
            String sDay   = psDay;

            //String sDate = psYear + "-" + psMonth + "-" + psDay;
            String sDate = psYear + psMonth + psDay;
            
            boolean rc = bb.app.account.AccountMisc.isEODAdded(pem, pMrcAccId, sDate);
            if (rc==false)
            {
                //Calculate Last 12 months total (from txnDate)
                
                SsMrcCashRegEod eodTxn = new SsMrcCashRegEod();
                
                //New Insert
                eodTxn.stat        = 1;
                eodTxn.byuser      = Long.toString(pOperationUserId);
                eodTxn.accountId   = Long.toString(pMrcAccId);
                //cashTxn.txnAmount   = sTot;

                eodTxn.txnDesc     = "";
                eodTxn.txnDate     = sDate;
                eodTxn.txnMonthNo  = psMonth;
                //eodTxn.txnType     = "KASA";
                //cashTxn.txnAmount   = psTot;

                eodTxn.sysCashTotal     = bdCashTotal;
                eodTxn.fnlCashTotal     = bdCashTotal;// this might be overwritten by manual

                eodTxn.sysCardTotal     = bdCardTotal;
                eodTxn.fnlCardTotal     = bdCardTotal;

                eodTxn.sysWireTotal     = bdWireTotal;
                eodTxn.fnlWireTotal     = bdWireTotal;

                eodTxn.sysInternetTotal = bdInternetTotal;
                eodTxn.fnlInternetTotal = bdInternetTotal;

                eodTxn.sysOtherTotal    = bdOtherTotal;
                eodTxn.fnlOtherTotal    = bdOtherTotal;
                eodTxn.source           = "MNL";//NOT EOD SYS

                //eodTxn.fnlAllTotal    = bdOverallTotal;// THIS IS GENERATED COLUMN

                long lUID = pem.persist(eodTxn);
            }
            else
            {
                // Already Exists so just update the numberss
                rc = bb.app.account.AccountMisc.updateEOD(pem, 
                                                          pOperationUserId,
                                                          pMrcAccId, 
                                                          sDate, 
                                                          bdCashTotal,
                                                          bdCardTotal,
                                                          bdWireTotal,
                                                          bdInternetTotal,
                                                          bdOtherTotal);
            }
            
            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
}
