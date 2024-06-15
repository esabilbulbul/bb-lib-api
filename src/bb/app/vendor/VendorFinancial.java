/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.vendor;

import java.math.BigDecimal;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;

/**
 *
 * @author Administrator
 */
public final class VendorFinancial 
{
    public static void updateBalanceByNewPayment(EntityManager  pem,
                                                 long           pUserId,
                                                 long           pAccId,
                                                 long           pVendorId,
                                                 String         pAmountPrincipal,
                                                 String         pAmountInterest) throws Exception
    {
        try
        {
            String sAmountPrincipal= "0";
            String sAmountInterest = "0";

            if(pAmountPrincipal.trim().length()>0)
                sAmountPrincipal = pAmountPrincipal;

            if(pAmountInterest.trim().length()>0)
                sAmountInterest = pAmountInterest;

            BigDecimal bdNewAmountPrincipal = new BigDecimal(sAmountPrincipal);
            BigDecimal bdNewAmountInterest  = new BigDecimal(sAmountInterest);

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_COL_UPDATE_VENDOR_STATS_NEW_PAYMENT");

            SP.registerStoredProcedureParameter("P_ACC_ID"                  , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"               , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_NEW_AMOUNT_PRINCIPAL"    , BigDecimal.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_NEW_AMOUNT_INTEREST"     , BigDecimal.class   , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId                  , "P_ACC_ID");
            SP.SetParameter(Colindex++, pVendorId               , "P_VENDOR_ID");
            SP.SetParameter(Colindex++, bdNewAmountPrincipal    , "P_NEW_AMOUNT_PRINCIPAL");
            SP.SetParameter(Colindex++, bdNewAmountInterest     , "P_NEW_AMOUNT_INTEREST");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    // Delete or Update creates FinAdj +/-
    public static void updateBalanceByUpdate(EntityManager  pem,
                                             long           pUserId,
                                             long           pAccId,
                                             long           pVendorId,
                                             long           pPaymentId,
                                             String         pAmountPrincipal,
                                             String         pAmountInterest) throws Exception
    {
        createFinAdjRecord(pem,
                           pUserId,
                           pAccId,
                           pVendorId,
                           pPaymentId,
                           pAmountPrincipal,
                           pAmountInterest,
                           false);
    }
    
    public static void updateBalanceByDelete(EntityManager  pem,
                                             long           pUserId,
                                             long           pAccId,
                                             long           pVendorId,
                                             long           pPaymentId,
                                             String         pAmountPrincipal,
                                             String         pAmountInterest) throws Exception
    {
        createFinAdjRecord(pem,
                           pUserId,
                           pAccId,
                           pVendorId,
                           pPaymentId,
                           pAmountPrincipal,
                           pAmountInterest,
                           true);
    }
    
    public static void createFinAdjRecord(EntityManager  pem,
                                          long           pUserId,
                                          long           pAccId,
                                          long           pVendorId,
                                          long           pPaymentId,
                                          String         pAmountPrincipal,
                                          String         pAmountInterest,
                                          boolean        bDeleteTxn) throws Exception
    {
        try
        {
            String sAmountPrincipal= "0";
            String sAmountInterest = "0";
            
            if(pAmountPrincipal.trim().length()>0)
                sAmountPrincipal = pAmountPrincipal;

            if(pAmountInterest.trim().length()>0)
                sAmountInterest = pAmountInterest;

            BigDecimal bdNewAmountPrincipal = new BigDecimal(sAmountPrincipal);
            BigDecimal bdNewAmountInterest  = new BigDecimal(sAmountInterest);
            String sIsDeleteTxn = "N";
            if (bDeleteTxn==true)
                sIsDeleteTxn = "Y";

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_COL_UPDATE_VENDOR_STATS_FINADJ");

            SP.registerStoredProcedureParameter("P_ACC_ID"                  , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"               , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_OLD_PAYMENT_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_NEW_AMOUNT_PRINCIPAL"    , BigDecimal.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_NEW_AMOUNT_INTEREST"     , BigDecimal.class   , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_B_IS_DELETE_TXN"         , String.class       , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId                  , "P_ACC_ID");
            SP.SetParameter(Colindex++, pVendorId               , "P_VENDOR_ID");
            SP.SetParameter(Colindex++, pPaymentId              , "P_OLD_PAYMENT_ID");
            SP.SetParameter(Colindex++, bdNewAmountPrincipal    , "P_NEW_AMOUNT_PRINCIPAL");
            SP.SetParameter(Colindex++, bdNewAmountInterest     , "P_NEW_AMOUNT_INTEREST");
            SP.SetParameter(Colindex++, sIsDeleteTxn            , "P_B_IS_DELETE_TXN");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
}
