/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.bill;

import java.math.BigDecimal;
import java.math.BigInteger;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;

/**
 *
 * @author Administrator
 */
public final class InventoryPayment 
{
    public static void movePayment2EOD(EntityManager               pem,
                                       String                      psUserId,
                                       String                      psPaymentId) throws Exception
    {
        long lPaymentId = Long.parseLong(psPaymentId);

        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_EOD_INV_MOVE_PYMNT_TXN_2_EOD_BY_ID");

            SP.registerStoredProcedureParameter("P_PAYMENT_ID"           , Long.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, lPaymentId                  , "P_PAYMENT_ID");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void movePayment2STMT(EntityManager               pem,
                                        long                        pAccId,
                                        long                        pVendorId,
                                        String                      psPaymentId
                                        ) throws Exception
                                        //boolean                     pbRecalcRevolving) throws Exception
    {
        long lPaymentId = Long.parseLong(psPaymentId);
        String sRecalcRevolving = "N";
        
        try
        {
            //if (pbRecalcRevolving==true)
            //   sRecalcRevolving = "Y";

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_STMT_MERGE_EOD_INTO_STMT_BY_ID");

            SP.registerStoredProcedureParameter("P_ACCOUNT_ID"           , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"            , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_PAYMENT_ID"           , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_RECALC_REVOLVING"     , String.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId                      , "P_ACCOUNT_ID");
            SP.SetParameter(Colindex++, pVendorId                   , "P_VENDOR_ID");
            SP.SetParameter(Colindex++, lPaymentId                  , "P_PAYMENT_ID");
            SP.SetParameter(Colindex++, sRecalcRevolving            , "P_RECALC_REVOLVING");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
}
