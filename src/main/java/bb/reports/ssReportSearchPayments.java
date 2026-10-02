/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.reports;

import bb.app.account.ssoUIPaymentItem;
import bb.app.account.ssoVendorPaymentSummary;
import bb.app.dict.DictionaryOps;
import Objects.ssoMerchant;
import bb.app.settings.UXParams;
import entity.txn.SsTxnInvPayments;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.util.Util;
import jaxesa.util.ssoDBRowLimits;

/**
 *
 * @author Administrator
 */
public class ssReportSearchPayments 
{
    public static ArrayList<ssoUIPaymentItem> generate( EntityManager  pem, 
                                                        BigInteger     pUserId,
                                                        String         pKeyword,
                                                        long           pYear,
                                                        boolean        pbCleanMemory,
                                                        BigInteger     pVendorId,
                                                        BigInteger     pPaymentGroupId,
                                                        int            piPageNumber,
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

                reportN = generatePaymentsStatement4Account( pem, 
                                                            accN.Id, 
                                                            pKeyword, 
                                                            pYear, 
                                                            pbCleanMemory,
                                                            pVendorId,
                                                            pPaymentGroupId,
                                                            piPageNumber,
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

    public static ArrayList<ssoUIPaymentItem> generatePaymentsStatement4Account(    EntityManager  pem, 
                                                                                    BigInteger     pAccId,
                                                                                    String         pKeyword,
                                                                                    long           pYear,
                                                                                    boolean        pbCleanMemory,
                                                                                    BigInteger     pVendorId,
                                                                                    BigInteger     pPaymentGroupId,
                                                                                    int            piPageNumber,
                                                                                    boolean        pbFullRows) throws Exception
    {
        ArrayList<ssoUIPaymentItem> report = new ArrayList<ssoUIPaymentItem>();
        BigInteger MYSQL_BIGINT_UNSIGNED_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger biLastRowId = MYSQL_BIGINT_UNSIGNED_MAX;
        
        boolean bStmt4Vendor = false;
        boolean bAllPaymentGroups = false;

        try
        {

            if(pVendorId.compareTo(BigInteger.ZERO)>0)//LARGER THAN 0
                bStmt4Vendor = true;

            if(pPaymentGroupId.compareTo(BigInteger.ZERO)>0)//LARGER THAN 0
            {
                bAllPaymentGroups = true;
                pem.flush();//cache is all only others clean cache
            }

            //if(pbCleanMemory==true)
            //    pem.flush();

            int iRowPerPage = 10;//100;//DEFAULT
            int iPageIndex  = piPageNumber;
            //int iOffset     = iPageIndex *  iRowPerPage;//skip first N records
            //int iLimit      = (iPageIndex+1) * iRowPerPage;//50 per page

            ssoDBRowLimits rowLimits = new ssoDBRowLimits();
            rowLimits = Util.Database.calculateRowLimits(iRowPerPage, piPageNumber);
            int iOffset     = rowLimits.offset;
            int iLimit      = rowLimits.limit;

            String sQueryName = "SsAccInvBrandsPayments.getPaymentsSummary4Account";//default account
            if(bStmt4Vendor==true)
                sQueryName = "SsAccInvBrandsPayments.getPaymentsSummary4Vendor";

            Query stmt = pem.createNamedQuery(sQueryName, SsTxnInvPayments.class);
            int index = 1;
            if(bAllPaymentGroups==true)
                stmt.SetParameter(index++, pPaymentGroupId  , "PAYMENT_GROUP_ID");
            else
                stmt.SetParameter(index++, null             , "PAYMENT_GROUP_ID");

            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");

            if(bStmt4Vendor==true)
                stmt.SetParameter(index++, pVendorId , "VENDOR_ID");//yyyymmdd

            stmt.SetParameter(index++, pYear         , "YEAR");
            stmt.SetParameter(index++, iLimit        , "LIMIT");
            stmt.SetParameter(index++, iOffset       , "OFFSET");

            String sParentRowId = "";
            String lastKeyGroup = "";// grouped under vendor
            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoUIPaymentItem lineN = new ssoUIPaymentItem();

                lineN.userId                  = Util.Database.getValString(rs.get(i), "USER_ID");
                lineN.paymentId               = Util.Database.getValString(rs.get(i), "PYM_ID");
                lineN.vendorId                = Util.Database.getValString(rs.get(i), "BRAND_ID");
                lineN.vendorName              = Util.Database.getValString(rs.get(i), "BRAND");
                lineN.paymentGroupId          = Util.Database.getValString(rs.get(i), "PYM_GROUP_DCT_ID");

                if(i==173)
                    i=i;
                //String sPartialKeyword = "";
                //if(pKeyword.trim().length()>0)
                //    sPartialKeyword = lineN.vendorName.substring(0, pKeyword.trim().length()-1);
                int iIndexOfKeyword = lineN.vendorName.indexOf(pKeyword.trim());

                if ( (Util.Str.SIMIL(lineN.vendorName, pKeyword)>=75) 
                      || 
                     (pKeyword.trim().length()==0)
                      ||
                     (iIndexOfKeyword >= 0)
                    )
                {

                    lineN.year                    = Util.Database.getValString(rs.get(i), "FINANCIAL_YEAR");
                    lineN.entryDate               = Util.Database.getValString(rs.get(i), "TXN_DATETIME");//entry date
                    lineN.dueDate                 = Util.Database.getValString(rs.get(i), "DUE_DATE");//payment date

                    lineN.accountId               = pAccId.toString();
                    lineN.accountName             = Util.Database.getValString(rs.get(i), "PROFILENAME");

                    lineN.tot_quantity            = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY");
                    lineN.tot_amount_principal    = Util.Database.getValString(rs.get(i), "TOTAL_PRINCIPAL");
                    lineN.tot_amount_interest     = Util.Database.getValString(rs.get(i), "TOTAL_INTEREST");

                    if (lastKeyGroup.equals(lineN.vendorId)!=true)//vendor changed 
                    {
                        sParentRowId = lineN.paymentId;
                        // at each vendor payment group key has been changed  (VENDORS ARE GROUPED UNDER ACCOUNTS)
                        //lineN.key                     = lineN.accountId + "-" + lineN.vendorId + "-" + lineN.paymentId;//keep only vendor Id
                        lineN.key                     = lineN.userId + "-" + lineN.vendorId;// + "-" + lineN.paymentId;//keep only vendor Id
                    }
                    else
                    {
                        //lineN.key                     = lineN.accountId + "-" + lineN.vendorId + "-" + lineN.paymentId;
                        //lineN.parentKey               = lineN.accountId + "-" + lineN.vendorId + "-" + sParentRowId;
                        lineN.key                     = lineN.userId + "-" + lineN.vendorId + "-" + lineN.accountId;
                        lineN.parentKey               = lineN.userId + "-" + lineN.vendorId;
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
