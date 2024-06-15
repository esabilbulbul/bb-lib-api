/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.payment;

import bb.app.account.ssoVendorPayment;
import bb.app.account.ssoVendorPaymentSummary;
import bb.app.dict.DictionaryOps;
import bb.app.obj.ssoMerchant;
import bb.app.vendor.VendorFinancial;
import bb.app.vendor.VendorOps;
import entity.dct.SsDctPymGroups;
import entity.mrc.SsMrcCashRegEod;
import entity.stmt.SsStmInvStatements;
import entity.txn.SsTxnInvVendorPayments;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class PaymentOps 
{

    public static void deletePayment(   EntityManager  pem, 
                                        long           pUserId,
                                        long           pAccId,
                                        long           pPaymentId,
                                        long           pPayment_AccId,
                                        long           pPayment_BrandId,
                                        String         pAmountPrincipal,
                                        String         pAmountInterest
                                        ) throws Exception
    {
        try
        {
            deletePaymentRecord(pem, 
                                pUserId, 
                                pAccId, 
                                pPaymentId, 
                                pPayment_AccId, 
                                pPayment_BrandId, 
                                pAmountPrincipal, 
                                pAmountInterest);
            
            // UPDATE VENDOR STATS
            //------------------------------------------------------------------
            VendorFinancial.updateBalanceByDelete(pem, 
                                                  pUserId,
                                                  pAccId,
                                                  pPayment_BrandId, 
                                                  pPaymentId, 
                                                  pAmountPrincipal,
                                                  pAmountInterest);//false = update payment
            
            resetPaymentRelatedMemory(pem, pPayment_AccId, pPayment_BrandId);
        }
        catch(Exception e)
        {
            throw e;
        }
    }//deletepayment
    
    public static void deletePaymentRecord( EntityManager  pem, 
                                            long           pUserId,
                                            long           pAccId,
                                            long           pPaymentId,
                                            long           pPayment_AccId,
                                            long           pPayment_BrandId,
                                            String         pAmountPrincipal,
                                            String         pAmountInterest
                                            ) throws Exception
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_INV_DELETE_PAYMENT");

            SP.registerStoredProcedureParameter("P_ACC_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_PAYMENT_ID"      , Long.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pPayment_AccId, "P_ACC_ID");
            SP.SetParameter(Colindex++, pPaymentId    , "P_PAYMENT_ID");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void removePaymentGroup(  EntityManager  pem, 
                                            long           pUserId,
                                            long           pPaymentGroupId
                                         ) throws Exception
    {
        try
        {
            SsStmInvStatements stmtTable = new SsStmInvStatements();

            Query qry = pem.createNamedQuery("SsDctPymGroups.deletePaymentGroup", SsDctPymGroups.class);

            int index = 1;
            qry.SetParameter(index++, pPaymentGroupId     , "PYM_GROUP_ID");
            qry.SetParameter(index++, pUserId             , "USER_ID");//Don't remove this cache will be resed thru this

            qry.executeUpdate();

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoVendorPayment getPaymentDetails(   EntityManager  pem, 
                                                        long           pUserId,
                                                        long           pAccId,
                                                        long           pPaymentId// accId for the payment. Main account might have branches. This is the Id of the branch
                                                    ) throws Exception
    {
        ssoVendorPayment pym = new ssoVendorPayment();

        try
        {
            Query stmt = pem.createNamedQuery("SsAccInvBrandsPayments.getPayment", SsTxnInvVendorPayments.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pPaymentId      , "UID");

            List<List<RowColumn>> rs = stmt.getResultList();
            if (rs.size()>0)
            {

                pym.vendId           = Util.Database.getValString(rs.get(0), "VENDOR_ID");
                pym.vendName         = Util.Database.getValString(rs.get(0), "VENDOR_DESC");
                pym.paymentType      = Util.Database.getValString(rs.get(0), "PAYMENT_TYPE");
                pym.amount_principal = Util.Database.getValString(rs.get(0), "AMOUNT_PRINCIPAL");
                pym.amount_interest  = Util.Database.getValString(rs.get(0), "AMOUNT_INTEREST");
                pym.installmentNumber= Util.Database.getValString(rs.get(0), "INSTALLMENT_NUMBER");
                pym.reference        = Util.Database.getValString(rs.get(0), "REFERENCE");
                pym.entryDate        = Util.Database.getValString(rs.get(0), "ENTRY_DATE");
                pym.dueDate          = Util.Database.getValString(rs.get(0), "DUE_DATE");
                pym.txnEffect        = Util.Database.getValString(rs.get(0), "TXN_EFFECT");
                pym.paymentGroup     = Util.Database.getValString(rs.get(0), "PAYMENT_GROUP");

                return pym;
            }

            return null;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void resetPaymentRelatedMemory(EntityManager  pem, long pAccId, long pVendorId)
    {
        try
        {
            
            // CLEAN CACHE - STATEMENT
            // ------------------------------------------------------
            ArrayList<ssoCacheSplitKey> aCacheSplitKeys_Stmt = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey newKey1 = new ssoCacheSplitKey();
            newKey1.column = "ACCOUNT_ID";
            newKey1.value  = pAccId;
            aCacheSplitKeys_Stmt.add(newKey1);

            ssoCacheSplitKey newKey2 = new ssoCacheSplitKey();
            newKey2.column = "VENDOR_ID";//BRAND ID
            newKey2.value  = pVendorId;
            aCacheSplitKeys_Stmt.add(newKey2);

            //aCacheSplitKeys = Misc.Cache.prepareSplitKeysWithColNames(runSet.entity.cache.SplitKeyColumns, runSet.params);

            // Flushes all related memories for the entity
            // clean cache
            pem.flush(SsStmInvStatements.class, aCacheSplitKeys_Stmt);//cleans all related 

            // CLEAN CACHE - PAYMENTS
            // ------------------------------------------------------
            ArrayList<ssoCacheSplitKey> aCacheSplitKeys_Pymnt = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey newKeyPymnt1 = new ssoCacheSplitKey();
            newKeyPymnt1.column = "ACCOUNT_ID";
            newKeyPymnt1.value  = pAccId;
            aCacheSplitKeys_Pymnt.add(newKeyPymnt1);

            pem.flush(SsTxnInvVendorPayments.class, aCacheSplitKeys_Pymnt);//cleans all related 

        }
        catch(Exception e)
        {
            
        }
    }

    public static ArrayList<ssoVendorPaymentSummary> getVendorPaymentSummary(   EntityManager  pem, 
                                                                                long           pUserId,
                                                                                long           pAccId,
                                                                                String         pKeyword) throws Exception
    {
        ArrayList<ssoVendorPaymentSummary> summary = new ArrayList<ssoVendorPaymentSummary>();

        //int TodaysDate = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 8));
        int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

        try
        {

            ArrayList<ssoMerchant> accs = new ArrayList<ssoMerchant>();

            accs = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);
            for (ssoMerchant accN: accs)
            {
                ArrayList<ssoVendorPaymentSummary> accNpayments = new ArrayList<ssoVendorPaymentSummary>();

                accNpayments = VendorOps.getHistorySummaryData(pem, accN.Id, ThisYear, false);

                summary.addAll(accNpayments);
            }

            return summary;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // Fetch Last 3 years of Payment History for the account
    public static ArrayList<ssoVendorPayment> getAccountPaymentHistory( EntityManager  pem, 
                                                                        long           pUserId,
                                                                        long           pAccId,
                                                                        long           pVendorId,
                                                                        long           pYear,
                                                                        boolean        pbCleanMemory,
                                                                        int            piStartRowIndex,
                                                                        boolean        pbFullRows) throws Exception
    {
        ArrayList<ssoVendorPayment> history = new ArrayList<ssoVendorPayment>();

        int TodaysDate = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 8));

        try
        {

            ArrayList<ssoMerchant> accs = new ArrayList<ssoMerchant>();

            accs = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);
            for (ssoMerchant accN: accs)
            {
                ArrayList<ssoVendorPayment> accNpayments = new ArrayList<ssoVendorPayment>();
                
                if (pAccId==accN.Id)
                {
                    accNpayments = VendorOps.getHistoryDetailedData(pem, 
                                                                    accN.Id, 
                                                                    pVendorId, 
                                                                    pYear, 
                                                                    pbCleanMemory,
                                                                    piStartRowIndex,
                                                                    pbFullRows);

                    history.addAll(accNpayments);
                }
            }

            return history;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

}
