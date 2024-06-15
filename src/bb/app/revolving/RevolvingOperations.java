/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.revolving;

import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvVendorStats;
import entity.dct.SsDctInvVendorSummary;
import entity.stmt.SsStmInvStatements;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.util.Util;
import org.json.simple.JSONObject;

/**
 *
 * @author Administrator
 */
public final class RevolvingOperations 
{
    public static void updateRevolvingBalance(  EntityManager  pem,
                                                long           pUserId,
                                                long           pAccountId,
                                                long           pVendorId,
                                                String         pRevolvingYear,
                                                String         pNewRevolvingBalance) throws Exception
    {
        ssoRevolvingManualOverwrite manualRevolving = new ssoRevolvingManualOverwrite();
        String sManualOverWriteRevolving = "{}";

        try
        {
            //1. Fetch Existing
            //2. Update Existing
            //3  Commit to DB
            Query stmt = pem.createNamedQuery("SsAccInvBrands.getRevolving", SsAccInvVendorStats.class);

            int index = 1;
            stmt.SetParameter(index++, pAccountId , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId  , "VENDOR_ID");

            List<List<RowColumn>> rs = stmt.getResultList();

            //for(int i=0; i<rs.size(); i++)
            if(rs.size()>0)
            {
                List<RowColumn> rowN = rs.get(0);

                String sRevolvingOverwrites = Util.Database.getValString(rowN, "REVOLVING_MANUAL_OVERWRITE");

                if(sRevolvingOverwrites.trim().equals("{}")==false)
                {
                    manualRevolving.gross     = getRevolvingUnit_Amount(sRevolvingOverwrites, pRevolvingYear, "gross");
                    manualRevolving.discount  = getRevolvingUnit_Amount(sRevolvingOverwrites, pRevolvingYear, "discount");
                    manualRevolving.surcharge = getRevolvingUnit_Amount(sRevolvingOverwrites, pRevolvingYear, "surcharge");
                    manualRevolving.tax       = getRevolvingUnit_Amount(sRevolvingOverwrites, pRevolvingYear, "tax");
                    manualRevolving.net       = getRevolvingUnit_Amount(sRevolvingOverwrites, pRevolvingYear, "net");
                    manualRevolving.quantity  = getRevolvingUnit_Quantity(sRevolvingOverwrites, pRevolvingYear, "net");

                    String sfinAdj_Minus = getRevolvingUnit(sRevolvingOverwrites, pRevolvingYear, "finAdj_Minus");
                    String sfinAdj_Plus = getRevolvingUnit(sRevolvingOverwrites, pRevolvingYear, "finAdj_Plus");

                    manualRevolving.finAdj_Minus  = new BigDecimal(sfinAdj_Minus);
                    manualRevolving.finAdj_Plus   = new BigDecimal(sfinAdj_Plus);
                    manualRevolving.balance       = new BigDecimal(pNewRevolvingBalance);
                }

                sManualOverWriteRevolving = "{" + Util.Str.QUOTE(pRevolvingYear) + ":" + Util.JSON.Convert2JSON(manualRevolving) + "}";

            }

            setNewRevolvingValue(pem, pUserId, pAccountId, pVendorId, sManualOverWriteRevolving);

            regenerateRevolving4Stmt(pem, pUserId, pAccountId, pVendorId, pRevolvingYear);

            resetMemoryTablesOnStmtUpdate(pem, pUserId, pAccountId, pVendorId);

            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static void resetMemoryTablesOnStmtUpdate(EntityManager pem, 
                                                     long          pUserId, 
                                                     long          pAccId, 
                                                     long          pVendorId)
    {
        try
        {

            // SsStmInvStatements
            //------------------------------------------------------------------
            ArrayList<ssoCacheSplitKey> keys3 = new ArrayList<ssoCacheSplitKey>();
            ssoCacheSplitKey ColY1 = new ssoCacheSplitKey();
            ColY1.column = "ACCOUNT_ID";
            ColY1.value  = pAccId;
            keys3.add(ColY1);

            ssoCacheSplitKey ColY2 = new ssoCacheSplitKey();
            ColY2.column = "VENDOR_ID";
            ColY2.value  = pVendorId;
            keys3.add(ColY2);
            
            pem.flush(SsStmInvStatements.class, keys3);

        }
        catch(Exception e)
        {
            String s = e.getMessage();
        }
    }

    public static void regenerateRevolving4Stmt(EntityManager  pem,
                                                long           pUserId,
                                                long           pAccountId,
                                                long           pVendorId,
                                                String         pRevolvingYear) throws Exception
    {
        try
        {
            int iYear = Integer.parseInt(pRevolvingYear);

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_STMT_GENERATE_YEAR_BEGINING_RECORD");

            SP.registerStoredProcedureParameter("P_ACC_ID"           , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_VENDOR_ID"        , Long.class         , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_YEAR_YYYY"          , Integer.class      , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId      , "P_ACC_ID");
            SP.SetParameter(Colindex++, pVendorId       , "P_VENDOR_ID");
            SP.SetParameter(Colindex++, iYear           , "P_YEAR_YYYY");
            
            SP.execute();

            return;
        }
        catch(Exception e)
        {
            String s = e.getMessage();
            throw e;
        }
    }

    public static void setNewRevolvingValue(EntityManager  pem,
                                            long           pUserId,
                                            long           pAccountId,
                                            long           pVendorId,
                                            String         pRevolvingUpdate) throws Exception
    {
        try
        {
            Query stmt = pem.createNamedQuery("SsAccInvBrands.updateRevolving", SsAccInvVendorStats.class);

            int index = 1;
            stmt.SetParameter(index++, pRevolvingUpdate , "REVOLVING");
            stmt.SetParameter(index++, pAccountId       , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId        , "VENDOR_ID");
            
            stmt.executeUpdate();
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static String getRevolvingUnit(String pjsRevolving,
                                          String pYear, 
                                          String pUnitName)
    {
        String sUnitN = "";
        JSONObject jsoGroups = Util.JSON.parseJSON(pjsRevolving);
        for(Iterator iterator = jsoGroups.keySet().iterator(); iterator.hasNext();)
        {
            String sYearN = (String)iterator.next();
            if(sYearN.trim().equals(pYear)==true)
            {
                String sUnits = (String)jsoGroups.get(pYear).toString();

                JSONObject jsoUnits = Util.JSON.parseJSON(sUnits);
                if(jsoUnits!=null)
                {
                    sUnitN = (String)jsoUnits.get(pUnitName).toString();
                }
                return sUnitN;
            }
        }

        return "";
    }
    // YYYY : { balance: <val>, quantity: {received: returned: sold:}, surcharge:{ entered: returned: sold: } ...}
    // YYYY = pYear
    // balance / quantity ... = pGroupName
    // unit = {entered: returned: sold:}
    public static ssoRevolvingAmountUnit getRevolvingUnit_Amount(String pjsRevolving,
                                                                 String pYear, 
                                                                 String pUnitName)
    {
        ssoRevolvingAmountUnit revUnit = new ssoRevolvingAmountUnit();

        String sUnitN = getRevolvingUnit(pjsRevolving, pYear, pUnitName);
        if(sUnitN.trim().length()>0)
        {
            JSONObject jsoUnit = Util.JSON.parseJSON(sUnitN);

            //String sEntered  = jsoUnit.get("entered").toString();
            String sEntered  = jsoUnit.get("received").toString();
            String sReturned = jsoUnit.get("returned").toString();
            String sSold     = jsoUnit.get("sold").toString();

            revUnit.entered  = new BigDecimal(sEntered);
            revUnit.returned = new BigDecimal(sReturned);
            revUnit.sold     = new BigDecimal(sSold);

        }

        return revUnit;
    }

    public static ssoRevolvingQuantityUnit getRevolvingUnit_Quantity(String pjsRevolving,
                                                                     String pYear, 
                                                                     String pUnitName)
    {
        ssoRevolvingQuantityUnit revUnit = new ssoRevolvingQuantityUnit();

        String sUnitN = getRevolvingUnit(pjsRevolving, pYear, pUnitName);
        if(sUnitN.trim().length()>0)
        {
            JSONObject jsoUnit = Util.JSON.parseJSON(sUnitN);
            
            //String sEntered  = jsoUnit.get("entered").toString();
            String sEntered  = jsoUnit.get("received").toString();
            String sReturned = jsoUnit.get("returned").toString();
            String sSold     = jsoUnit.get("sold").toString();

            revUnit.entered  = Long.parseLong(sEntered);
            revUnit.returned = Long.parseLong(sReturned);
            revUnit.sold     = Long.parseLong(sSold);

        }

        return revUnit;
    }
}
