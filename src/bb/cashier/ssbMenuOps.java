/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.cashier;

import jaxesa.persistence.EntityManager;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;

/**
 *
 * @author Administrator
 */
public final class ssbMenuOps 
{
    public static void updateCashierMenu(EntityManager pem, long pAccId)
    {
        try
        {
            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_CSH_REGENERATE_MENU");

            SP.registerStoredProcedureParameter("P_ACC_ID"          , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("PB_START_OVER"     , Long.class         , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccId     , "P_ACC_ID");
            SP.SetParameter(Colindex++, "N"        , "PB_START_OVER");

            SP.execute();

            return;
        }
        catch(Exception e)
        {
        
        }
    }
}
