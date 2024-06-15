/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.reports;

import bb.app.account.ssoUIPaymentItem;
import bb.app.account.ssoVendorPaymentSummary;
import bb.app.dict.DictionaryOps;
import bb.app.obj.ssoMerchant;
import bb.app.settings.UXParams;
import entity.txn.SsTxnInvVendorPayments;
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
public class ssReportSearchPayments 
{
    public static ArrayList<ssoUIPaymentItem> generate( EntityManager  pem, 
                                                        long           pUserId,
                                                        String         pKeyword,
                                                        long           pThisYear,
                                                        boolean        pbCleanMemory,
                                                        int            piStartRowIndex,
                                                        boolean        pbFullRows) throws Exception
    {
        ArrayList<ssoUIPaymentItem> report = new ArrayList<ssoUIPaymentItem>();

        try
        {
            
            ArrayList<ssoMerchant> accs = new ArrayList<ssoMerchant>();

            accs = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);
            for (ssoMerchant accN: accs)
            {
                ArrayList<ssoUIPaymentItem> reportN= new ArrayList<ssoUIPaymentItem>();

                reportN = generate4Account( pem, 
                                            accN.Id, 
                                            pKeyword, 
                                            pThisYear, 
                                            pbCleanMemory,
                                            piStartRowIndex,
                                            pbFullRows);

                report.addAll(reportN);
            }
            
            return report;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoUIPaymentItem> generate4Account( EntityManager  pem, 
                                                                long           pAccId,
                                                                String         pKeyword,
                                                                long           pThisYear,
                                                                boolean        pbCleanMemory,
                                                                int            piStartRowIndex,
                                                                boolean        pbFullRows) throws Exception
    {
        ArrayList<ssoUIPaymentItem> report = new ArrayList<ssoUIPaymentItem>();

        try
        {
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);
            else
                pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);

            pem.setRowStartIndex(piStartRowIndex);

            if(pbCleanMemory==true)
                pem.flush();

            Query stmt = pem.createNamedQuery("SsAccInvBrandsPayments.getPaymentsSummary", SsTxnInvVendorPayments.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pThisYear        , "P_DATE");//START DATE
            stmt.SetParameter(index++, pThisYear - 1    , "P_DATE_MINUS_1");//START DATE
            stmt.SetParameter(index++, pThisYear - 2    , "P_DATE_MINUS_2");//START DATE

            String lastKeyGroup = "";// grouped under vendor
            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoUIPaymentItem lineN = new ssoUIPaymentItem();

                lineN.paymentId               = Util.Database.getValString(rs.get(i), "PYM_ID");
                lineN.vendorId                = Util.Database.getValString(rs.get(i), "BRAND_ID");
                lineN.vendorName              = Util.Database.getValString(rs.get(i), "BRAND");
                
                String sPartialKeyword = lineN.vendorName.substring(0, pKeyword.trim().length());
                
                if ( (Util.Str.SIMIL(lineN.vendorName, pKeyword)>=75) 
                      || 
                     (pKeyword.trim().length()==0)
                      ||
                     (sPartialKeyword.toLowerCase().trim().equals(pKeyword.toLowerCase().trim())==true)
                    )
                {

                    lineN.year                    = Util.Database.getValString(rs.get(i), "FINANCIAL_YEAR");
                    lineN.entryDate               = Util.Database.getValString(rs.get(i), "INSERTDATE");

                    lineN.accountId               = Long.toString(pAccId);
                    lineN.accountName             = Util.Database.getValString(rs.get(i), "PROFILENAME");

                    lineN.tot_quantity            = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY");
                    lineN.tot_amount_principal    = Util.Database.getValString(rs.get(i), "TOTAL_PRINCIPAL");
                    lineN.tot_amount_interest     = Util.Database.getValString(rs.get(i), "TOTAL_INTEREST");

                    if (lastKeyGroup.equals(lineN.vendorId)!=true)//vendor changed
                    {
                        // at each vendor payment group key has been changed
                        lineN.key                     = lineN.accountId + "-" + lineN.vendorId;//keep only vendor Id
                    }
                    else
                    {
                        lineN.key                     = lineN.accountId + "-" + lineN.vendorId + "-" + lineN.paymentId;
                        lineN.parentKey               = lineN.accountId + "-" + lineN.vendorId;
                    }

                    lastKeyGroup = lineN.vendorId;

                    report.add(lineN);
                }
            }

            return report;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
}
