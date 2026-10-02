/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.vendor;

import bb.app.account.AccountMisc;
import bb.app.account.UserOps;
import bb.app.account.ssoVendorPayment;
import bb.app.account.ssoVendorPaymentSummary;
import bb.app.dict.DictionaryOps;
import bb.app.obj.ssoBrandDets;
import bb.app.obj.ssoInventoryParams;
import Objects.ssoMerchant;
import bb.app.obj.ssoVendorItemStats;
import bb.app.obj.ssoVendorOptionStats;
import bb.app.obj.ssoVendorStats;
import bb.app.payment.PaymentOps;
import static bb.app.payment.PaymentOps.deletePaymentRecord;
import static bb.app.payment.PaymentOps.resetPaymentRelatedMemory;
import bb.app.settings.UXParams;
import bb.app.txn.txnDefs;
import entity.acc.SsAccInvItemStats;
import entity.txn.SsTxnInvPayments;
import entity.acc.SsAccInvVendors;
import entity.acc.SsAccInvVendorStats;
import entity.dct.SsDctInvVendorSummary;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoCacheSplitKey;
import jaxesa.persistence.ssoKeyField;
import jaxesa.util.Util;
import org.json.simple.JSONObject;

/**
 *
 * @author Administrator
 */
public final class VendorOps
{
    public static String PENDING_OP_CODE_NEW_BILL     = "NB";
    public static String PENDING_OP_CODE_UPDATE_BILL  = "UB";
    public static String PENDING_OP_CODE_DELETE_BILL  = "DB";
    public static String PENDING_OP_CODE_APPEND2_BILL = "AB";

    public static SsAccInvVendors createNewVendor(  EntityManager  pem,
                                                    BigInteger           pUserId,
                                                    BigInteger           pAccId,
                                                    String         psBrand,
                                                    String         psContactName,
                                                    String         psPhoneCountryCode,
                                                    String         psPhoneAreaCode,
                                                    String         psPhoneNumber,
                                                    String         psTaxOrNationalId,
                                                    String         psEmail,
                                                    String         psCity,
                                                    String         psAddress,
                                                    String         psNotes
                                                   ) throws Exception
    {

        try
        {

            SsAccInvVendors brandDets = new SsAccInvVendors();

            brandDets.userId = pUserId;
            //brandDets.accId  = pAccId;//CLOSED BECAUSE VENDORS ARE AIMED TO GATHER UNDER USER ID
            brandDets.brand  = psBrand;
            brandDets.contactName = psContactName;
            brandDets.phoneCountryCode = psPhoneCountryCode;
            brandDets.phoneAreaCode    = psPhoneAreaCode;
            brandDets.phoneNumber = psPhoneNumber;
            brandDets.taxNo       = psTaxOrNationalId;
            brandDets.email       = psEmail;
            brandDets.city        = psCity;
            brandDets.address     = psAddress;
            brandDets.notes       = psNotes;

            BigInteger lUID = pem.persist(brandDets);
            brandDets.uid = lUID;

            copyVendor2Memory(pem, pUserId, lUID, psBrand);

            return brandDets;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void copyVendor2Memory(EntityManager  pem, 
                                         BigInteger           pUserId,
                                         BigInteger           pBrandId,
                                         String         pBrandName) throws Exception
    {
        try
        {
            String sQuery = "INSERT INTO ss_mem_vendors " +
                            "(" +
                            "INSERTDATE," + 
                            "USER_ID," + 
                            "BRAND_ID," + 
                            "BRAND_NAME" + 
                            ")" + 
                            "VALUES" +
                            "(" + 
                            "?," + 
                            "?," + 
                            "?," + 
                            "?" + 
                            ")";
            Query qQuery = pem.CreateNativeQuery(sQuery);
            int index = 1;

            long lInsertDateTime = Util.DateTime.GetDateTime_l();

            qQuery.SetParameter(index++, lInsertDateTime, "INSERTDATE");
            qQuery.SetParameter(index++, pUserId        , "USER_ID");
            qQuery.SetParameter(index++, pBrandId       , "BRAND_ID");
            qQuery.SetParameter(index++, pBrandName     , "BRAND_NAME");

            qQuery.executeUpdate();
            
            return;
        }
        catch(Exception e)
        {
            throw e;
        }
    }   

    public static void cleanVendorSummary(  EntityManager  pem, 
                                            BigInteger           pUserId,
                                            BigInteger           pAccId)
    {
        try
        {
            ArrayList<ssoCacheSplitKey> aCacheSplitKeys = new ArrayList<ssoCacheSplitKey>();

            ssoCacheSplitKey newKey = new ssoCacheSplitKey();
            newKey.column = "ACCOUNT_ID";
            newKey.value  = pAccId;
            aCacheSplitKeys.add(newKey);
            //aCacheSplitKeys = Misc.Cache.prepareSplitKeysWithColNames(runSet.entity.cache.SplitKeyColumns, runSet.params);

            // Flushes all related memories for the entity
            // clean cache
            pem.flush(SsDctInvVendorSummary.class, aCacheSplitKeys);//cleans all related 
        }
        catch(Exception e)
        {
            
        }
    }

    public static SsAccInvVendorStats getVendorStats(   EntityManager   pem,
                                                        BigInteger            pAccountId,
                                                        BigInteger            pBrandId,
                                                        boolean         pbCleanMemory) throws Exception
    {
        SsAccInvVendorStats brandAcc = new SsAccInvVendorStats();
        try
        {
            if(pbCleanMemory==true)
                pem.flush();

            //int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            Query stmt = pem.createNamedQuery("SsAccInvBrands.findByAccIdNBrand", SsAccInvVendorStats.class);

            int index = 1;
            stmt.SetParameter(index++, pAccountId , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId   , "BRAND_ID");

            List<SsAccInvVendorStats> rs = stmt.getResultList(SsAccInvVendorStats.class);

            if(rs.size()>0)
            {
                brandAcc = rs.get(0);
                
                return brandAcc;
            }
            
            return null;
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoVendorStats convertUIVendorStats(SsAccInvVendorStats poStats)
    {
        ssoVendorStats vendorStats = new ssoVendorStats();

        // CURRENT
        //------------------------------------------------------------------
        vendorStats.current.received.quantity   = poStats.quantityEntered.toString();
        vendorStats.current.received.gross      = poStats.grossTotalEntered.toString();
        vendorStats.current.received.discount   = poStats.discountTotalEntered.toString();
        vendorStats.current.received.surcharge  = poStats.surchargeTotalEntered.toString();
        vendorStats.current.received.tax        = poStats.taxTotalEntered.toString();
        vendorStats.current.received.expense    = poStats.expenseTotalEntered.toString();
        vendorStats.current.received.net        = poStats.rawTotalEntered.toString();
        //vendorStats.current.received.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.received.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.received.cumulative = poStats.cumulativeEntered.toString();

        vendorStats.current.sent.quantity   = poStats.quantityReturned.toString();
        vendorStats.current.sent.net        = poStats.rawTotalReturned.toString();
        vendorStats.current.sent.discount   = poStats.discountTotalReturned.toString();
        vendorStats.current.sent.surcharge  = poStats.surchargeTotalReturned.toString();
        vendorStats.current.sent.tax        = poStats.taxTotalReturned.toString();
        vendorStats.current.sent.expense    = poStats.expenseTotalReturned.toString();
        vendorStats.current.sent.gross      = poStats.grossTotalReturned.toString();
        //vendorStats.current.sent.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.sent.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.sent.cumulative = poStats.cumulativeReturned.toString();

        vendorStats.current.sold.quantity   = poStats.quantityCrSold.toString();
        vendorStats.current.sold.net        = poStats.rawTotalCrSold.toString();
        vendorStats.current.sold.discount   = poStats.discountTotalCrSold.toString();
        vendorStats.current.sold.surcharge  = poStats.surchargeTotalCrSold.toString();
        vendorStats.current.sold.tax        = poStats.taxTotalCrSold.toString();
        vendorStats.current.sold.expense    = poStats.expenseTotalCrSold.toString();
        vendorStats.current.sold.gross      = poStats.grossTotalCrSold.toString();
        //vendorStats.current.sold.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.sold.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.sold.cumulative = poStats.cumulativeSold.toString();

        vendorStats.current.refund.quantity   = poStats.quantityCrRefund.toString();
        vendorStats.current.refund.net        = poStats.rawTotalCrRefund.toString();
        vendorStats.current.refund.discount   = poStats.discountTotalCrRefund.toString();
        vendorStats.current.refund.surcharge  = poStats.surchargeTotalCrRefund.toString();
        vendorStats.current.refund.tax        = poStats.taxTotalCrRefund.toString();
        vendorStats.current.refund.expense    = poStats.expenseTotalCrRefund.toString();
        vendorStats.current.refund.gross      = poStats.grossTotalCrRefund.toString();
        //vendorStats.current.refund.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.refund.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.refund.cumulative = poStats.cumulativeRefund.toString();

        // adj (+)
        vendorStats.current.adjPlus.quantity   = poStats.quantityAdjPlus.toString();
        vendorStats.current.adjPlus.net        = poStats.finAdjPlusRawTotal.toString();
        vendorStats.current.adjPlus.discount   = poStats.finAdjPlusDiscountTotal.toString();
        vendorStats.current.adjPlus.surcharge  = poStats.finAdjPlusSurchargeTotal.toString();
        vendorStats.current.adjPlus.tax        = poStats.finAdjPlusTaxTotal.toString();
        vendorStats.current.adjPlus.expense    = poStats.finAdjPlusExpenseTotal.toString();
        vendorStats.current.adjPlus.gross      = poStats.finAdjPlusGrossTotal.toString();
        //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

        // adj (-)
        vendorStats.current.adjMinus.quantity   = poStats.quantityAdjMinus.toString();
        vendorStats.current.adjMinus.gross      = poStats.finAdjMinusGrossTotal.toString();
        vendorStats.current.adjMinus.discount   = poStats.finAdjMinusDiscountTotal.toString();
        vendorStats.current.adjMinus.surcharge  = poStats.finAdjMinusSurchargeTotal.toString();
        vendorStats.current.adjMinus.tax        = poStats.finAdjMinusTaxTotal.toString();
        vendorStats.current.adjMinus.expense    = poStats.finAdjMinusExpenseTotal.toString();
        vendorStats.current.adjMinus.net        = poStats.finAdjMinusRawTotal.toString();
        //vendorStats.current.adjMinus.cumulative = poStats.cumulativeRefund.toString();

        // EOD
        //------------------------------------------------------------------
        vendorStats.eod.received.quantity   = poStats.lastEodQuantityEntered.toString();
        vendorStats.eod.received.net        = poStats.lastEodRawTotalEntered.toString();
        vendorStats.eod.received.discount   = poStats.lastEodDiscountTotalEntered.toString();
        vendorStats.eod.received.surcharge  = poStats.lastEodSurchargeTotalEntered.toString();
        vendorStats.eod.received.tax        = poStats.lastEodTaxTotalEntered.toString();
        vendorStats.eod.received.expense    = poStats.lastEodExpenseTotalEntered.toString();
        vendorStats.eod.received.gross      = poStats.lastEodGrossTotalEntered.toString();
        //vendorStats.eod.received.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.received.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.received.cumulative = poStats.cumulativeEntered.toString();

        vendorStats.eod.sent.quantity   = poStats.lastEodQuantityReturned.toString();
        vendorStats.eod.sent.net        = poStats.lastEodRawTotalReturned.toString();
        vendorStats.eod.sent.discount   = poStats.lastEodDiscountTotalReturned.toString();
        vendorStats.eod.sent.surcharge  = poStats.lastEodSurchargeTotalReturned.toString();
        vendorStats.eod.sent.tax        = poStats.lastEodTaxTotalReturned.toString();
        vendorStats.eod.sent.expense    = poStats.lastEodExpenseTotalReturned.toString();
        vendorStats.eod.sent.gross      = poStats.lastEodGrossTotalReturned.toString();
        //vendorStats.eod.sent.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.sent.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.sent.cumulative = poStats.cumulativeReturned.toString();

        vendorStats.eod.sold.quantity   = poStats.lastEodQuantityCrSold.toString();
        vendorStats.eod.sold.net        = poStats.lastEodRawTotalCrSold.toString();
        vendorStats.eod.sold.discount   = poStats.lastEodDiscountTotalCrSold.toString();
        vendorStats.eod.sold.surcharge  = poStats.lastEodSurchargeTotalCrSold.toString();
        vendorStats.eod.sold.tax        = poStats.lastEodTaxTotalCrSold.toString();
        vendorStats.eod.sold.expense    = poStats.lastEodExpenseTotalCrSold.toString();
        vendorStats.eod.sold.gross      = poStats.lastEodGrossTotalCrSold.toString();
        //vendorStats.eod.sold.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.sold.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.sold.cumulative = poStats.cumulativeSold.toString();

        vendorStats.eod.refund.quantity   = poStats.lastEodQuantityCrRefund.toString();
        vendorStats.eod.refund.net        = poStats.lastEodRawTotalCrRefund.toString();
        vendorStats.eod.refund.discount   = poStats.lastEodDiscountTotalCrRefund.toString();
        vendorStats.eod.refund.surcharge  = poStats.lastEodSurchargeTotalCrRefund.toString();
        vendorStats.eod.refund.tax        = poStats.lastEodTaxTotalCrRefund.toString();
        vendorStats.eod.refund.expense    = poStats.lastEodExpenseTotalCrRefund.toString();
        vendorStats.eod.refund.gross      = poStats.lastEodGrossTotalCrRefund.toString();
        //vendorStats.eod.refund.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.refund.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.refund.cumulative = poStats.cumulativeRefund.toString();

        // adj (+)
        vendorStats.eod.adjPlus.quantity   = poStats.lastEodQuantityAdjPlus.toString();
        vendorStats.eod.adjPlus.net        = poStats.lastEodFinAdjPlusRawTotal.toString();
        vendorStats.eod.adjPlus.discount   = poStats.lastEodFinAdjPlusDiscountTotal.toString();
        vendorStats.eod.adjPlus.surcharge  = poStats.lastEodFinAdjPlusSurchargeTotal.toString();
        vendorStats.eod.adjPlus.tax        = poStats.lastEodFinAdjPlusTaxTotal.toString();
        vendorStats.eod.adjPlus.expense    = poStats.lastEodFinAdjPlusExpenseTotal.toString();
        vendorStats.eod.adjPlus.gross      = poStats.lastEodFinAdjPlusGrossTotal.toString();
        //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

        // adj (-)
        vendorStats.eod.adjMinus.quantity   = poStats.lastEodQuantityAdjMinus.toString();
        vendorStats.eod.adjMinus.net        = poStats.lastEodFinAdjMinusRawTotal.toString();
        vendorStats.eod.adjMinus.discount   = poStats.lastEodFinAdjMinusDiscountTotal.toString();
        vendorStats.eod.adjMinus.surcharge  = poStats.lastEodFinAdjMinusSurchargeTotal.toString();
        vendorStats.eod.adjMinus.tax        = poStats.lastEodFinAdjMinusTaxTotal.toString();
        vendorStats.eod.adjMinus.expense    = poStats.lastEodFinAdjMinusExpenseTotal.toString();
        vendorStats.eod.adjMinus.gross      = poStats.lastEodFinAdjMinusGrossTotal.toString();
        
        //vendorStats.current.adjMinus.cumulative = poStats.cumulativeRefund.toString();

        // REVOLVING
        //------------------------------------------------------------------
        vendorStats.revolving.received.quantity   = poStats.revolvingQuantityEntered.toString();
        vendorStats.revolving.received.net        = poStats.revolvingRawTotalEntered.toString();
        vendorStats.revolving.received.discount   = poStats.revolvingDiscountTotalEntered.toString();
        vendorStats.revolving.received.surcharge  = poStats.revolvingSurchargeTotalEntered.toString();
        vendorStats.revolving.received.tax        = poStats.revolvingTaxTotalEntered.toString();
        vendorStats.revolving.received.expense    = poStats.revolvingExpenseTotalEntered.toString();
        vendorStats.revolving.received.gross      = poStats.revolvingGrossTotalEntered.toString();
        //vendorStats.revolving.received.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.received.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.received.cumulative = poStats.cumulativeEntered.toString();

        vendorStats.revolving.sent.quantity   = poStats.revolvingQuantityReturned.toString();
        vendorStats.revolving.sent.net        = poStats.revolvingRawTotalReturned.toString();
        vendorStats.revolving.sent.discount   = poStats.revolvingDiscountTotalReturned.toString();
        vendorStats.revolving.sent.surcharge  = poStats.revolvingSurchargeTotalReturned.toString();
        vendorStats.revolving.sent.tax        = poStats.revolvingTaxTotalReturned.toString();
        vendorStats.revolving.sent.expense    = poStats.revolvingExpenseTotalReturned.toString();
        vendorStats.revolving.sent.gross      = poStats.revolvingGrossTotalReturned.toString();
        //vendorStats.revolving.sent.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.sent.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.sent.cumulative = poStats.cumulativeReturned.toString();

        vendorStats.revolving.sold.quantity   = poStats.revolvingQuantityCrSold.toString();
        vendorStats.revolving.sold.net        = poStats.revolvingRawTotalCrSold.toString();
        vendorStats.revolving.sold.discount   = poStats.revolvingDiscountTotalCrSold.toString();
        vendorStats.revolving.sold.surcharge  = poStats.revolvingSurchargeTotalCrSold.toString();
        vendorStats.revolving.sold.tax        = poStats.revolvingTaxTotalCrSold.toString();
        vendorStats.revolving.sold.expense    = poStats.revolvingExpenseTotalCrSold.toString();
        vendorStats.revolving.sold.gross      = poStats.revolvingGrossTotalCrSold.toString();
        //vendorStats.revolving.sold.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.sold.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.sold.cumulative = poStats.cumulativeSold.toString();

        vendorStats.revolving.refund.quantity   = poStats.revolvingQuantityCrRefund.toString();
        vendorStats.revolving.refund.net        = poStats.revolvingRawTotalCrRefund.toString();
        vendorStats.revolving.refund.discount   = poStats.revolvingDiscountTotalCrRefund.toString();
        vendorStats.revolving.refund.surcharge  = poStats.revolvingSurchargeTotalCrRefund.toString();
        vendorStats.revolving.refund.tax        = poStats.revolvingTaxTotalCrRefund.toString();
        vendorStats.revolving.refund.expense    = poStats.revolvingExpenseTotalCrRefund.toString();
        vendorStats.revolving.refund.gross      = poStats.revolvingGrossTotalCrRefund.toString();
        //vendorStats.revolving.refund.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.refund.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.refund.cumulative = poStats.cumulativeRefund.toString();

        // adj (+)
        vendorStats.revolving.adjPlus.quantity   = poStats.revolvingQuantityAdjPlus.toString();
        vendorStats.revolving.adjPlus.gross      = poStats.revolvingFinAdjPlusGrossTotal.toString();
        vendorStats.revolving.adjPlus.discount   = poStats.revolvingFinAdjPlusDiscountTotal.toString();
        vendorStats.revolving.adjPlus.surcharge  = poStats.revolvingFinAdjPlusSurchargeTotal.toString();
        vendorStats.revolving.adjPlus.tax        = poStats.revolvingFinAdjPlusTaxTotal.toString();
        vendorStats.revolving.adjPlus.expense    = poStats.revolvingFinAdjPlusExpenseTotal.toString();
        vendorStats.revolving.adjPlus.net        = poStats.revolvingFinAdjPlusRawTotal.toString();
        //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

        // adj (-)
        vendorStats.revolving.adjMinus.quantity   = poStats.lastEodQuantityAdjMinus.toString();
        vendorStats.revolving.adjMinus.gross      = poStats.lastEodFinAdjMinusGrossTotal.toString();
        vendorStats.revolving.adjMinus.discount   = poStats.lastEodFinAdjMinusDiscountTotal.toString();
        vendorStats.revolving.adjMinus.surcharge  = poStats.lastEodFinAdjMinusSurchargeTotal.toString();
        vendorStats.revolving.adjMinus.tax        = poStats.lastEodFinAdjMinusTaxTotal.toString();
        vendorStats.revolving.adjMinus.expense    = poStats.lastEodFinAdjMinusExpenseTotal.toString();
        vendorStats.revolving.adjMinus.net        = poStats.lastEodFinAdjMinusRawTotal.toString();
        //vendorStats.current.adjMinus.cumulative = poStats.cumulativeRefund.toString();

        return vendorStats;
    }

    public static ArrayList<ssoVendorItemStats> getItemStats4Vendor(EntityManager   pem,
                                                                    BigInteger            pAccountId,
                                                                    BigInteger            pBrandId,
                                                                    String          pItemCode,//if empty all items
                                                                    boolean         pbCleanMemory) throws Exception
    {
        

        ArrayList<ssoVendorItemStats> itemStats = new ArrayList<ssoVendorItemStats>();
        try
        {
            if(pbCleanMemory==true)
                pem.flush();

            Query stmt = pem.createNamedQuery("SsAccInvBrandItemCodes.getAllItemStats4Vendor", SsAccInvItemStats.class);

            int index = 1;
            stmt.SetParameter(index++, pAccountId , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId   , "VENDOR_ID");

            List<List<RowColumn>> rs =  stmt.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoVendorItemStats itemStatN = new ssoVendorItemStats();

                // Item Code
                itemStatN.itemCode = Util.Database.getValString(rowN, "ITEM_CODE").toString();

                //itemStatN.key = pAccountId + "-" + pBrandId + "-" + itemStatN.itemCode;

                //boolean bAdd = false;
                //if ( (itemStatN.itemCode.trim().equals(pItemCode)==true) || (pItemCode.trim().equals("")==true) )
                //    bAdd = true;
                boolean bAdd = true;//always add filtering will be done on client
                if(bAdd==true)
                {
                    BigDecimal bdTotalQReceived = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalQSent     = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalQSold     = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalQRefund   = new BigDecimal(BigInteger.ZERO);

                    BigDecimal bdTotalAmountReceived = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalAmountSent     = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalAmountSold     = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalAmountRefund   = new BigDecimal(BigInteger.ZERO);

                    BigDecimal bdTotalAmountAdjPlus  = new BigDecimal(BigInteger.ZERO);
                    BigDecimal bdTotalAmountAdjMinus = new BigDecimal(BigInteger.ZERO);

                    // Item Options
                    itemStatN.OptGroups = Util.Database.getValString(rowN, "OPT_GROUPS").toString();

                    // ITEM-CURRENT-RECEIVED
                    itemStatN.current.received.quantity  = Util.Database.getValString(rowN, "ITM_Q_RECEIVED").toString();
                    itemStatN.current.received.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.expense   = Util.Database.getValString(rowN, "ITM_EXPENSE_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_RECEIVED").toString();
                    //itemStatN.current.received.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.received.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-CURRENT-SENT
                    itemStatN.current.sent.quantity  = Util.Database.getValString(rowN, "ITM_Q_SENT").toString();
                    itemStatN.current.sent.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_SENT").toString();
                    itemStatN.current.sent.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_SENT").toString();
                    itemStatN.current.sent.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_SENT").toString();
                    itemStatN.current.sent.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_SENT").toString();
                    itemStatN.current.sent.expense   = Util.Database.getValString(rowN, "ITM_EXPENSE_TOTAL_SENT").toString();
                    itemStatN.current.sent.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_SENT").toString();
                    //itemStatN.current.sent.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.sent.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-CURRENT-SOLD
                    itemStatN.current.sold.quantity  = Util.Database.getValString(rowN, "ITM_Q_CR_SOLD").toString();
                    itemStatN.current.sold.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_CR_SOLD").toString();
                    itemStatN.current.sold.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_CR_SOLD").toString();
                    itemStatN.current.sold.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_CR_SOLD").toString();
                    itemStatN.current.sold.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_CR_SOLD").toString();
                    itemStatN.current.sold.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_CR_SOLD").toString();
                    //itemStatN.current.sold.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.sold.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-CURRENT-REFUND
                    itemStatN.current.refund.quantity  = Util.Database.getValString(rowN, "ITM_Q_CR_REFUND").toString();
                    itemStatN.current.refund.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_CR_REFUND").toString();
                    itemStatN.current.refund.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_CR_REFUND").toString();
                    itemStatN.current.refund.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_CR_REFUND").toString();
                    itemStatN.current.refund.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_CR_REFUND").toString();
                    itemStatN.current.refund.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_CR_REFUND").toString();
                    //itemStatN.current.refund.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.refund.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // current-adj (+)
                    itemStatN.current.adjPlus.quantity   = Util.Database.getValString(rowN, "QUANTITY_ADJ_PLUS").toString();
                    itemStatN.current.adjPlus.net        = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_NET_TOTAL").toString();
                    itemStatN.current.adjPlus.discount   = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_DISCOUNT_TOTAL").toString();
                    itemStatN.current.adjPlus.surcharge  = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_SURCHARGE_TOTAL").toString();
                    itemStatN.current.adjPlus.tax        = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.current.adjPlus.expense    = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_EXPENSE_TOTAL").toString();
                    itemStatN.current.adjPlus.gross      = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_GROSS_TOTAL").toString();
                    //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

                    // current-adj (-)
                    itemStatN.current.adjMinus.quantity   = Util.Database.getValString(rowN, "QUANTITY_ADJ_MINUS").toString();
                    itemStatN.current.adjMinus.net        = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_NET_TOTAL").toString();
                    itemStatN.current.adjMinus.discount   = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_DISCOUNT_TOTAL").toString();
                    itemStatN.current.adjMinus.surcharge  = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_SURCHARGE_TOTAL").toString();
                    itemStatN.current.adjMinus.tax        = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_TAX_TOTAL").toString();
                    itemStatN.current.adjMinus.expense    = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_EXPENSE_TOTAL").toString();
                    itemStatN.current.adjMinus.gross      = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_GROSS_TOTAL").toString();

                    //--------------------------------------------------------------------------------

                    // ITEM-EOD-RECEIVED
                    itemStatN.eod.received.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_RECEIVED").toString();
                    itemStatN.eod.received.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.expense   = Util.Database.getValString(rowN, "EOD_ITM_EXPENSE_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_RECEIVED").toString();
                    //itemStatN.eod.received.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.received.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-EOD-SENT
                    itemStatN.eod.sent.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_SENT").toString();
                    itemStatN.eod.sent.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_SENT").toString();
                    itemStatN.eod.sent.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_SENT").toString();
                    itemStatN.eod.sent.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_SENT").toString();
                    itemStatN.eod.sent.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_SENT").toString();
                    itemStatN.eod.sent.expense   = Util.Database.getValString(rowN, "EOD_ITM_EXPENSE_TOTAL_SENT").toString();
                    itemStatN.eod.sent.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_SENT").toString();
                    //itemStatN.eod.sent.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.sent.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-EOD-SOLD
                    itemStatN.eod.sold.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_CR_SOLD").toString();
                    itemStatN.eod.sold.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_CR_SOLD").toString();
                    itemStatN.eod.sold.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_CR_SOLD").toString();
                    itemStatN.eod.sold.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_CR_SOLD").toString();
                    itemStatN.eod.sold.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_CR_SOLD").toString();
                    itemStatN.eod.sold.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_CR_SOLD").toString();
                    //itemStatN.eod.sold.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.sold.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-EOD-REFUND
                    itemStatN.eod.refund.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_CR_REFUND").toString();
                    itemStatN.eod.refund.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_CR_REFUND").toString();
                    itemStatN.eod.refund.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_CR_REFUND").toString();
                    itemStatN.eod.refund.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_CR_REFUND").toString();
                    itemStatN.eod.refund.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_CR_REFUND").toString();
                    itemStatN.eod.refund.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_CR_REFUND").toString();
                    //itemStatN.eod.refund.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.refund.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // EOD-adj (+)
                    itemStatN.eod.adjPlus.quantity   = Util.Database.getValString(rowN, "LAST_EOD_QUANTITY_ADJ_PLUS").toString();
                    itemStatN.eod.adjPlus.net        = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_NET_TOTAL").toString();
                    itemStatN.eod.adjPlus.discount   = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_DISCOUNT_TOTAL").toString();
                    itemStatN.eod.adjPlus.surcharge  = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_SURCHARGE_TOTAL").toString();
                    itemStatN.eod.adjPlus.tax        = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.eod.adjPlus.gross      = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_GROSS_TOTAL").toString();
                    //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

                    // EOD-adj (-)
                    itemStatN.eod.adjMinus.quantity   = Util.Database.getValString(rowN, "LAST_EOD_QUANTITY_ADJ_MINUS").toString();
                    itemStatN.eod.adjMinus.net        = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_MINUS_NET_TOTAL").toString();
                    itemStatN.eod.adjMinus.discount   = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_MINUS_DISCOUNT_TOTAL").toString();
                    itemStatN.eod.adjMinus.surcharge  = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_MINUS_SURCHARGE_TOTAL").toString();
                    itemStatN.eod.adjMinus.tax        = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_MINUS_TAX_TOTAL").toString();
                    itemStatN.eod.adjMinus.gross      = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_MINUS_GROSS_TOTAL").toString();

                    //--------------------------------------------------------------------------------

                    // ITEM-REVOLVING-RECEIVED
                    itemStatN.revolving.received.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_RECEIVED").toString();
                    itemStatN.revolving.received.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.expense   = Util.Database.getValString(rowN, "REVOLVING_ITM_EXPENSE_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_RECEIVED").toString();
                    //itemStatN.revolving.received.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.received.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-REVOLVING-SENT
                    itemStatN.revolving.sent.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_SENT").toString();
                    itemStatN.revolving.sent.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.expense   = Util.Database.getValString(rowN, "REVOLVING_ITM_EXPENSE_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_SENT").toString();
                    //itemStatN.revolving.sent.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.sent.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-REVOLVING-SOLD
                    itemStatN.revolving.sold.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_CR_SOLD").toString();
                    itemStatN.revolving.sold.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_CR_SOLD").toString();
                    itemStatN.revolving.sold.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_CR_SOLD").toString();
                    itemStatN.revolving.sold.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_CR_SOLD").toString();
                    itemStatN.revolving.sold.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_CR_SOLD").toString();
                    itemStatN.revolving.sold.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_CR_SOLD").toString();
                    //itemStatN.revolving.sold.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.sold.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-REVOLVING-REFUND
                    itemStatN.revolving.refund.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_CR_REFUND").toString();
                    itemStatN.revolving.refund.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_CR_REFUND").toString();
                    itemStatN.revolving.refund.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_CR_REFUND").toString();
                    itemStatN.revolving.refund.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_CR_REFUND").toString();
                    itemStatN.revolving.refund.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_CR_REFUND").toString();
                    itemStatN.revolving.refund.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_CR_REFUND").toString();
                    //itemStatN.revolving.refund.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.refund.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // REVOLVING-adj (+)
                    itemStatN.revolving.adjPlus.quantity   = Util.Database.getValString(rowN, "REVOLVING_QUANTITY_ADJ_PLUS").toString();
                    itemStatN.revolving.adjPlus.net        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_NET_TOTAL").toString();
                    itemStatN.revolving.adjPlus.discount   = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_DISCOUNT_TOTAL").toString();
                    itemStatN.revolving.adjPlus.surcharge  = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_SURCHARGE_TOTAL").toString();
                    itemStatN.revolving.adjPlus.tax        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.revolving.adjPlus.expense    = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_EXPENSE_TOTAL").toString();
                    itemStatN.revolving.adjPlus.gross      = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_GROSS_TOTAL").toString();
                    //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

                    // REVOLVING-adj (-)
                    itemStatN.revolving.adjMinus.quantity   = Util.Database.getValString(rowN, "REVOLVING_QUANTITY_ADJ_MINUS").toString();
                    itemStatN.revolving.adjMinus.net        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_NET_TOTAL").toString();
                    itemStatN.revolving.adjMinus.discount   = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_DISCOUNT_TOTAL").toString();
                    itemStatN.revolving.adjMinus.surcharge  = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_SURCHARGE_TOTAL").toString();
                    itemStatN.revolving.adjMinus.tax        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_TAX_TOTAL").toString();
                    itemStatN.revolving.adjMinus.expense    = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_EXPENSE_TOTAL").toString();
                    itemStatN.revolving.adjMinus.gross      = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_GROSS_TOTAL").toString();

                    // OPTION-CURRENT
                    itemStatN.optStats.current.received     = Util.Database.getValString(rowN, "OPT_ENTERED").toString();
                    itemStatN.optStats.current.sent         = Util.Database.getValString(rowN, "OPT_RETURNED").toString();
                    itemStatN.optStats.current.sold         = Util.Database.getValString(rowN, "OPT_CR_SOLD").toString();
                    itemStatN.optStats.current.refund       = Util.Database.getValString(rowN, "OPT_CR_REFUND").toString();
                    itemStatN.optStats.current.finAdjPlus   = Util.Database.getValString(rowN, "OPT_ADJ_PLUS").toString();
                    itemStatN.optStats.current.finAdjMinus  = Util.Database.getValString(rowN, "OPT_ADJ_MINUS").toString();

                    // OPTION-EOD
                    itemStatN.optStats.eod.received     = Util.Database.getValString(rowN, "OPT_EOD_ENTERED").toString();
                    itemStatN.optStats.eod.sent         = Util.Database.getValString(rowN, "OPT_EOD_RETURNED").toString();
                    itemStatN.optStats.eod.sold         = Util.Database.getValString(rowN, "OPT_EOD_CR_SOLD").toString();
                    itemStatN.optStats.eod.refund       = Util.Database.getValString(rowN, "OPT_EOD_CR_REFUND").toString();
                    itemStatN.optStats.eod.finAdjPlus   = Util.Database.getValString(rowN, "OPT_EOD_ADJ_PLUS").toString();
                    itemStatN.optStats.eod.finAdjMinus  = Util.Database.getValString(rowN, "OPT_EOD_ADJ_MINUS").toString();

                    // OPTION-REV
                    itemStatN.optStats.revolving.received     = Util.Database.getValString(rowN, "OPT_REV_ENTERED").toString();
                    itemStatN.optStats.revolving.sent         = Util.Database.getValString(rowN, "OPT_REV_RETURNED").toString();
                    itemStatN.optStats.revolving.sold         = Util.Database.getValString(rowN, "OPT_REV_CR_SOLD").toString();
                    itemStatN.optStats.revolving.refund       = Util.Database.getValString(rowN, "OPT_REV_CR_REFUND").toString();
                    itemStatN.optStats.revolving.finAdjPlus   = Util.Database.getValString(rowN, "OPT_REV_ADJ_PLUS").toString();
                    itemStatN.optStats.revolving.finAdjMinus  = Util.Database.getValString(rowN, "OPT_REV_ADJ_MINUS").toString();

                    bdTotalAmountAdjPlus = bdTotalAmountAdjPlus.add(new BigDecimal(itemStatN.current.adjPlus.quantity))
                                                               .add(new BigDecimal(itemStatN.eod.adjPlus.quantity))
                                                               .add(new BigDecimal(itemStatN.revolving.adjPlus.quantity));

                    bdTotalAmountAdjMinus = bdTotalAmountAdjMinus.add(new BigDecimal(itemStatN.current.adjMinus.quantity))
                                                                 .add(new BigDecimal(itemStatN.eod.adjMinus.quantity))
                                                                 .add(new BigDecimal(itemStatN.revolving.adjMinus.quantity));

                    // -------------- ADJS --------------
                    bdTotalQReceived = bdTotalQReceived.add(new BigDecimal(itemStatN.current.received.quantity))
                                                       .add(new BigDecimal(itemStatN.eod.received.quantity))
                                                       .add(new BigDecimal(itemStatN.revolving.received.quantity));
                    bdTotalQSent     = bdTotalQSent.add(new BigDecimal(itemStatN.current.sent.quantity))
                                                       .add(new BigDecimal(itemStatN.eod.sent.quantity))
                                                       .add(new BigDecimal(itemStatN.revolving.sent.quantity));
                    bdTotalQSold     = bdTotalQSold.add(new BigDecimal(itemStatN.current.sold.quantity))
                                                       .add(new BigDecimal(itemStatN.eod.sold.quantity))
                                                       .add(new BigDecimal(itemStatN.revolving.sold.quantity));
                    bdTotalQRefund   = bdTotalQRefund.add(new BigDecimal(itemStatN.current.refund.quantity))
                                                       .add(new BigDecimal(itemStatN.eod.refund.quantity))
                                                       .add(new BigDecimal(itemStatN.revolving.refund.quantity));

                    //------------ TOTALS ---------------

                    bdTotalAmountReceived = bdTotalAmountReceived.add(new BigDecimal(itemStatN.current.received.gross))
                                                                 .add(new BigDecimal(itemStatN.eod.received.gross))
                                                                 .add(new BigDecimal(itemStatN.revolving.received.gross));

                    bdTotalAmountSent     = bdTotalAmountSent.add(new BigDecimal(itemStatN.current.sent.gross))
                                                             .add(new BigDecimal(itemStatN.eod.sent.gross))
                                                             .add(new BigDecimal(itemStatN.revolving.sent.gross));

                    bdTotalAmountSold     = bdTotalAmountSold.add(new BigDecimal(itemStatN.current.sold.gross))
                                                             .add(new BigDecimal(itemStatN.eod.sold.gross))
                                                             .add(new BigDecimal(itemStatN.revolving.sold.gross));

                    bdTotalAmountRefund   = bdTotalAmountRefund.add(new BigDecimal(itemStatN.current.refund.gross))
                                                               .add(new BigDecimal(itemStatN.eod.refund.gross))
                                                               .add(new BigDecimal(itemStatN.revolving.refund.gross));

                    itemStatN.summary.received.quantity = bdTotalQReceived.toString();
                    itemStatN.summary.sent.quantity     = bdTotalQSent.toString();
                    itemStatN.summary.sold.quantity     = bdTotalQSold.toString();
                    itemStatN.summary.refund.quantity   = bdTotalQRefund.toString();
                    itemStatN.summary.adjPlus.gross     = bdTotalAmountAdjPlus.toString();
                    itemStatN.summary.adjMinus.gross    = bdTotalAmountAdjMinus.toString();

                    itemStatN.summary.netRecSent.quantity = bdTotalQReceived.subtract(bdTotalQSent).toString();
                    itemStatN.summary.netSoldRef.quantity = bdTotalQSold.subtract(bdTotalQRefund).toString();

                    itemStatN.summary.netRecSent.amount   = bdTotalAmountReceived.subtract(bdTotalAmountSent)
                                                                                        .add(bdTotalAmountAdjPlus)
                                                                                        .subtract(bdTotalAmountAdjMinus).toString();

                    itemStatN.summary.netSoldRef.amount   = bdTotalAmountSold.subtract(bdTotalAmountRefund).toString();

                    itemStats.add(itemStatN);

                }//end of bAdd
            }

            return itemStats;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static SsAccInvVendorStats getItemStats(   EntityManager   pem,
                                                      long            pAccountId,
                                                      long            pBrandId,
                                                      String          pItemCode,
                                                      boolean         pbCleanMemory) throws Exception
    {
        SsAccInvVendorStats brandAcc = new SsAccInvVendorStats();
        try
        {
            if(pbCleanMemory==true)
                pem.flush();
            
            //int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));
            Query stmt = pem.createNamedQuery("SsAccInvBrands.findByAccIdNBrand", SsAccInvItemStats.class);

            int index = 1;
            stmt.SetParameter(index++, pAccountId , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId   , "BRAND_ID");

            List<SsAccInvVendorStats> rs = stmt.getResultList(SsAccInvItemStats.class);

            if(rs.size()>0)
            {
                brandAcc = rs.get(0);
            }

            return brandAcc;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // Supplier Brand
    public static SsAccInvVendorStats createVendorStats( EntityManager   pem, 
                                                         BigInteger            pAccountId,
                                                         BigInteger            pBrandId
                                                         ) throws Exception
    {
        try
        {
            SsAccInvVendorStats brandAcc = new SsAccInvVendorStats();

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            brandAcc.financialYear = ThisYear;
            brandAcc.vendorId      = pBrandId;
            brandAcc.accountId     = pAccountId;

            brandAcc.quantityEntered    = new BigDecimal(BigInteger.ZERO);
            brandAcc.quantityReturned   = new BigDecimal(BigInteger.ZERO);
            brandAcc.quantityCrSold       = new BigDecimal(BigInteger.ZERO);

            brandAcc.grossTotalEntered  = new BigDecimal(BigInteger.ZERO);
            brandAcc.grossTotalReturned = new BigDecimal(BigInteger.ZERO);
            brandAcc.grossTotalCrSold     = new BigDecimal(BigInteger.ZERO);

            brandAcc.discountTotalEntered  = new BigDecimal(BigInteger.ZERO);
            brandAcc.discountTotalReturned = new BigDecimal(BigInteger.ZERO);
            brandAcc.discountTotalCrSold     = new BigDecimal(BigInteger.ZERO);

            brandAcc.surchargeTotalEntered  = new BigDecimal(BigInteger.ZERO);
            brandAcc.surchargeTotalReturned = new BigDecimal(BigInteger.ZERO);
            brandAcc.surchargeTotalCrSold     = new BigDecimal(BigInteger.ZERO);

            brandAcc.taxTotalEntered        = new BigDecimal(BigInteger.ZERO);
            brandAcc.taxTotalReturned       = new BigDecimal(BigInteger.ZERO);
            brandAcc.taxTotalCrSold           = new BigDecimal(BigInteger.ZERO);

            brandAcc.rawTotalEntered        = new BigDecimal(BigInteger.ZERO);
            brandAcc.rawTotalReturned       = new BigDecimal(BigInteger.ZERO);
            brandAcc.rawTotalCrSold           = new BigDecimal(BigInteger.ZERO);

            brandAcc.finAdjPlusRawTotal      = new BigDecimal(BigInteger.ZERO);
            brandAcc.finAdjMinusRawTotal     = new BigDecimal(BigInteger.ZERO);
            // balance is auto - generated (22.03.2024)
            //brandAcc.balance                = new BigDecimal(BigInteger.ZERO);

            //brandAcc.revolvingBalance = BigDecimal.ZERO;

            brandAcc.uid = pem.persist(brandAcc);

            return brandAcc;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBrandDets getVendorDetailByName(EntityManager pem, BigInteger pUserId, String pName) throws Exception
    {
        try
        {
            ArrayList<ssoBrandDets> vendors = new ArrayList<ssoBrandDets>();
            
            vendors = getAllVendorDetails(pem, pUserId);
            for(ssoBrandDets vendorN: vendors)
            {
                if (vendorN.brand.trim().equals(pName)==true)
                {
                    return vendorN;
                }
            }

            return null;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoBrandDets> getAllVendorDetails(EntityManager pem, BigInteger pUserId) throws Exception
    {
        ArrayList<ssoBrandDets> allBrands = new ArrayList<ssoBrandDets>();

        try
        {
            /*
            ArrayList<ssoMerchant> accs = new ArrayList<ssoMerchant>();

            accs = DictionaryOps.User.getListOfAccounts4User(pem, pUserId, false);
            for(ssoMerchant accN: accs)
            {
                ArrayList<ssoBrandDets> dets = new ArrayList<ssoBrandDets>();

                dets = getAccountVendors(pem, accN.Id);
                if (dets!=null)
                {
                    allBrands.addAll(dets);
                }
            }
            */
            allBrands = getAccountVendors(pem, pUserId);
            
            return allBrands;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBrandDets getVendorDetail(EntityManager pem, 
                                               BigInteger          pUserId, 
                                               BigInteger          pAccId, //no longer used
                                               BigInteger          pVendorId) throws Exception
    {

        try
        {
            Query stmtQry = pem.createNamedQuery("SsAccInvBrandDets.getMyBrands", SsAccInvVendors.class);

            int ParIndex = 1;
            //stmtQry.SetParameter(ParIndex++, pAccId     , "ACC_ID");//THIS WILL BE CHANGED TO ACCOUNT
            stmtQry.SetParameter(ParIndex++, pUserId     , "USER_ID");

            List<List<RowColumn>> rs =  stmtQry.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoBrandDets detN = new ssoBrandDets();

                detN.uid               = new BigInteger(Util.Database.getValString(rowN, "UID"));
                //if (detN.uid == pVendorId)
                if (detN.uid.compareTo(pVendorId)==0)
                {
                    detN.brand             = Util.Database.getValString(rowN, "BRAND");
                    detN.contactName       = Util.Database.getValString(rowN, "CONTACT_NAME");
                    detN.phoneCountryCode  = Util.Database.getValString(rowN, "PHONE_COUNTRY_CODE");
                    detN.phoneAreaCode     = Util.Database.getValString(rowN, "PHONE_AREA_CODE");
                    detN.phoneNumber       = Util.Database.getValString(rowN, "PHONE_NUMBER");
                    detN.taxNo             = Util.Database.getValString(rowN, "TAX_NO");
                    detN.email             = Util.Database.getValString(rowN, "EMAIL");
                    detN.city              = Util.Database.getValString(rowN, "CITY");
                    detN.notes             = Util.Database.getValString(rowN, "NOTES");
                    detN.address           = Util.Database.getValString(rowN, "ADDRESS");
                    
                    return detN;
                }

            }

            return null;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    //public static ArrayList<ssoBrandDets> getAccountVendors(EntityManager pem, long pAccId) throws Exception
    public static ArrayList<ssoBrandDets> getAccountVendors(EntityManager pem, BigInteger pUserId) throws Exception
    {
        ArrayList<ssoBrandDets> brandDets = new ArrayList<ssoBrandDets>();

        try
        {
            Query stmtQry = pem.createNamedQuery("SsAccInvBrandDets.getMyBrands", SsAccInvVendors.class);

            int ParIndex = 1;
            stmtQry.SetParameter(ParIndex++, pUserId     , "USER_ID");//THIS WILL BE CHANGED TO ACCOUNT

            List<List<RowColumn>> rs =  stmtQry.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoBrandDets newDet = new ssoBrandDets();

                newDet.uid               = new BigInteger(Util.Database.getValString(rowN, "UID"));
                //if(newDet.uid==46406585)
                //    newDet.uid = newDet.uid;

                newDet.vnd_stt_id        = new BigInteger(Util.Database.getValString(rowN, "VND_STT_ID"));
                newDet.brand             = Util.Database.getValString(rowN, "BRAND");
                newDet.contactName       = Util.Database.getValString(rowN, "CONTACT_NAME");
                newDet.phoneCountryCode  = Util.Database.getValString(rowN, "PHONE_COUNTRY_CODE");
                newDet.phoneAreaCode     = Util.Database.getValString(rowN, "PHONE_AREA_CODE");
                newDet.phoneNumber       = Util.Database.getValString(rowN, "PHONE_NUMBER");
                newDet.taxNo             = Util.Database.getValString(rowN, "TAX_NO");
                newDet.email             = Util.Database.getValString(rowN, "EMAIL");
                newDet.city              = Util.Database.getValString(rowN, "CITY");
                newDet.notes             = Util.Database.getValString(rowN, "NOTES");
                newDet.address           = Util.Database.getValString(rowN, "ADDRESS");

                brandDets.add(newDet);
            }

            return brandDets;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static BigInteger updatePayment( EntityManager  pem, 
                                            BigInteger     pUserId,
                                            BigInteger     pAccId,
                                            BigInteger     pPaymentId,
                                            BigInteger     pPaymentAccId,
                                            String         pPaymentType,
                                            String         pVendorId,
                                            String         pVendorName,
                                            String         pAmountPrincipal,
                                            String         pAmountInterest,
                                            String         pEntryDate,
                                            String         pDueDate,
                                            String         pInstallmentNumber,
                                            String         pReference,
                                            boolean        pbPaymentSent,
                                            String         psPaymentGroupId,
                                            String         psPaymentGroupName,
                                            long           pTxnDateTime
                                    ) throws Exception
    {
        try
        {
            BigInteger lVendorId = new BigInteger(pVendorId);
            
            /*
            PaymentOps.deletePayment(pem, 
                                     pUserId, 
                                     pAccId, 
                                     pPaymentId, 
                                     pPaymentAccId, 
                                     lVendorId,
                                     pAmountPrincipal,
                                     pAmountInterest);
            */
            deletePaymentRecord(pem, 
                                pUserId, 
                                pAccId, 
                                pPaymentId, 
                                pPaymentAccId, 
                                lVendorId, 
                                pAmountPrincipal, 
                                pAmountInterest);

            // UPDATE STATS
            //------------------------------------------------------------------
            VendorFinancial.updateBalanceByUpdate(  pem, 
                                                    pUserId,
                                                    pAccId,
                                                    lVendorId, 
                                                    pPaymentId, 
                                                    pAmountPrincipal,
                                                    pAmountInterest);//false = update payment

            BigInteger lUID = VendorOps.addPayment4Vendor(  pem,
                                                            pUserId,
                                                            pAccId, 
                                                            pPaymentType,
                                                            "",
                                                            pVendorId,
                                                            pVendorName,
                                                            pAmountPrincipal,
                                                            pAmountInterest,
                                                            pEntryDate.replace("-", "").replace(".",""), 
                                                            pDueDate.replace("-", "").replace(".",""), 
                                                            pInstallmentNumber,
                                                            pReference,
                                                            psPaymentGroupId,
                                                            psPaymentGroupName,
                                                            pbPaymentSent,
                                                            pTxnDateTime);
            
            resetPaymentRelatedMemory(pem, pPaymentAccId, lVendorId);
            
            return lUID;
        }
        catch(Exception e)
        {
          throw e;  
        }
    }

    public static BigInteger addPayment4Vendor( EntityManager  pem,
                                                BigInteger     pUserId,
                                                BigInteger     pAccId,
                                                String         pPaymentType,
                                                String         pPaymentEffect,
                                                String         pVendorId,
                                                String         pVendorName,
                                                String         pAmountPrincipal,
                                                String         pAmountInterest,
                                                String         pEntryDate,
                                                String         pDueDate,
                                                String         pInstallmentNumber,
                                                String         pReference,
                                                String         psPaymentGroupId,
                                                String         psPaymentGroupName,
                                                boolean        pbPaymentSent,
                                                long           pTxnDateTime) throws Exception
    {
        BigInteger lPaymentUID = BigInteger.valueOf(-1);

        try
        {
            BigInteger lVendorId = new BigInteger(pVendorId);

            lPaymentUID = createPaymentRow4Vendor(  pem, 
                                                    pUserId, 
                                                    pAccId, 
                                                    pPaymentType, 
                                                    pPaymentEffect, 
                                                    pVendorId, 
                                                    pVendorName, 
                                                    pAmountPrincipal, 
                                                    pAmountInterest, 
                                                    pEntryDate, 
                                                    pDueDate, 
                                                    pInstallmentNumber, 
                                                    pReference,
                                                    psPaymentGroupId,
                                                    psPaymentGroupName,
                                                    pbPaymentSent,
                                                    pTxnDateTime);

            // UPDATE STATS
            //----------------------------------------------------------------
            if(pVendorId.equals("0")==false)//not vendor related
            {
                VendorFinancial.updateBalanceByNewPayment(pem, 
                                                          pUserId, 
                                                          pAccId, 
                                                          lVendorId, 
                                                          pAmountPrincipal, 
                                                          pAmountInterest,
                                                          pbPaymentSent);
            }

            return lPaymentUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static BigInteger createPaymentRow4Vendor(EntityManager  pem,
                                                BigInteger           pUserId,
                                                BigInteger           pAccId,
                                                String         pPaymentType,
                                                String         pPaymentEffect,
                                                String         pVendorId,
                                                String         pVendorName,
                                                String         pAmountPrincipal,
                                                String         pAmountInterest,
                                                String         pEntryDate,
                                                String         pDueDate,
                                                String         pInstallmentNumber,
                                                String         pReference,
                                                String         psPaymentGroupId,
                                                String         psPaymentGroupName,
                                                boolean        pbPaymentSent,
                                                long           pTxnDateTime) throws Exception
    {
        int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

        try
        {
            SsTxnInvPayments payment = new SsTxnInvPayments();
            ssoBrandDets vendorDets = new ssoBrandDets();

            if (pbPaymentSent==true)
            {
                if(pPaymentType.trim().equals("CHK")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_SENT_CHECK;
                else if(pPaymentType.trim().equals("CSH")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_SENT_CASH;
                else if(pPaymentType.trim().equals("EFT")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_SENT_WIRE;
                else if(pPaymentType.trim().equals("CRD")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_SENT_CARD;
                else
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_SENT_CASH;//DEFAULT
            }
            else
            {
                if(pPaymentType.trim().equals("CHK")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_RECEIVED_CHECK;
                else if(pPaymentType.trim().equals("CSH")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_RECEIVED_CASH;
                else if(pPaymentType.trim().equals("EFT")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_RECEIVED_WIRE;
                else if(pPaymentType.trim().equals("CRD")==true)
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_RECEIVED_CARD;
                else
                    payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_RECEIVED_CASH;//DEFAULT
            }

            BigInteger lPymGroupId = new BigInteger(psPaymentGroupId);
            if(psPaymentGroupName.trim().length()>0)
            {
                // Get Payment Group Info
                //lPymGroupId = DictionaryOps.getPaymentGroupIDByName(pem, pUserId, psPaymentGroup);
                if(lPymGroupId.compareTo(BigInteger.valueOf(-1))==0)
                {
                    lPymGroupId = DictionaryOps.addNewPaymentGroup(pem, pUserId, pAccId, psPaymentGroupName);
                }
            }

            payment.txnEffect = txnDefs.getTransactionEffect(payment.txnCode);

            BigInteger bdVendorId = new BigInteger(pVendorId);
            if(bdVendorId.compareTo(BigInteger.ZERO)!=0)
            {
                // PAYMENT FOR VENDOR
                vendorDets = getVendorDetail(pem, pUserId, pAccId, bdVendorId);
                if (vendorDets==null)
                    return BigInteger.valueOf(-1);

            }
            else
            {
                // PAYMENT FOR ACCOUNT
                bdVendorId = BigInteger.ZERO;// Means Payment for Account
            }

            payment.paymentType   = pPaymentType;
            payment.financialYear = ThisYear;
            payment.vendorDesc    = pVendorName;//pVendorId;
            payment.vendorId      = bdVendorId;//vendorDets.uid;
            payment.accountId     = pAccId;
            payment.pymGroupDctId = lPymGroupId;

            /*
            if (pPaymentEffect.trim().equals("C")==true)//we received
                payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_RECEIVED_CHECK;
            else if (pPaymentEffect.trim().equals("D")==true)//we sent - alacakli
                payment.txnCode   = txnDefs.TXN_CODE_PAYMENT_SENT_CHECK;
            else
                payment.txnCode   = "";
            */

            payment.amountPrincipal = new BigDecimal(BigInteger.ZERO);
            payment.amountInterest  = new BigDecimal(BigInteger.ZERO);

            if (pAmountPrincipal.trim().length()>0)
                payment.amountPrincipal = new BigDecimal(pAmountPrincipal.replaceAll(",", ""));

            if (pAmountInterest.trim().length()>0)
                payment.amountInterest = new BigDecimal(pAmountInterest.replaceAll(",", ""));

            if(pInstallmentNumber.trim().length()>0)
                payment.installmentNumber = Integer.parseInt(pInstallmentNumber);
            else
                payment.installmentNumber = 0;

            if (pEntryDate.trim().length()>0)
                payment.entryDate     = Integer.parseInt(pEntryDate);
            else
                payment.entryDate = 0;

            payment.dueDate       = Integer.parseInt(pDueDate);
            payment.reference     = pReference;

            BigInteger lUID = pem.persist(payment);

            return lUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    
    }
    
    // Gets this year payments only
    public static ArrayList<ssoVendorPaymentSummary> getHistorySummaryData(     EntityManager  pem, 
                                                                                BigInteger           pAccId,
                                                                                long           pThisYear,
                                                                                boolean        pbCleanMemory) throws Exception
    {
        ArrayList<ssoVendorPaymentSummary> summary = new ArrayList<ssoVendorPaymentSummary>();

        try
        {
            if(pbCleanMemory==true)
                pem.flush();

            Query stmt = pem.createNamedQuery("SsAccInvBrandsPayments.getPaymentsSummary", SsTxnInvPayments.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pThisYear        , "P_DATE");//START DATE
            stmt.SetParameter(index++, pThisYear - 1    , "P_DATE_MINUS_1");//START DATE
            stmt.SetParameter(index++, pThisYear - 2    , "P_DATE_MINUS_2");//START DATE

            List<List<RowColumn>> rs = stmt.getResultList();
            for(int i=0;i<rs.size();i++)
            {
                ssoVendorPaymentSummary sumN = new ssoVendorPaymentSummary();

                sumN.Id                      = Util.Database.getValString(rs.get(i), "PYM_ID");
                sumN.vendorId                = Util.Database.getValString(rs.get(i), "BRAND_ID");
                sumN.vendorName              = Util.Database.getValString(rs.get(i), "BRAND");

                sumN.year                    = Util.Database.getValString(rs.get(i), "FINANCIAL_YEAR");
                sumN.entryDate               = Util.Database.getValString(rs.get(i), "INSERTDATE");

                sumN.accId                   = pAccId.toString();
                sumN.accName                 = Util.Database.getValString(rs.get(i), "PROFILENAME");

                sumN.tot_quantity            = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY");
                sumN.tot_amount_principal    = Util.Database.getValString(rs.get(i), "TOTAL_PRINCIPAL");
                sumN.tot_amount_interest     = Util.Database.getValString(rs.get(i), "TOTAL_INTEREST");

                summary.add(sumN);
            }

            return summary;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // Payment History Data
    public static ArrayList<ssoVendorPayment> getHistoryDetailedData( EntityManager  pem, 
                                                                      BigInteger           pAccId,
                                                                      BigInteger           pVendorId,
                                                                      long           pYear,
                                                                      boolean        pbCleanMemory,
                                                                      int            pLastRowTxnDate,
                                                                      boolean        pbFullRows) throws Exception
    {
        ArrayList<ssoVendorPayment> payments = new ArrayList<ssoVendorPayment>();

        try
        {
            /*
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);
            else
                pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);
            */

            //pem.setRowStartIndex(piStartRowIndex);

            ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();

            if(pbCleanMemory==true)
                pem.flush();//this will execute flush before executing the query ahead

            Query stmt = pem.createNamedQuery("SsAccInvBrandsPayments.getHistoryOfLast2Years", SsTxnInvPayments.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId               , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId            , "BRAND_ID");
            stmt.SetParameter(index++, pLastRowTxnDate      , "P_LAST_TXN_DATE");//END DATE
            stmt.SetParameter(index++, pYear                , "P_YEAR");//year

            // This filter thru after the resultset fetched
            /*
            ssoKeyField keyVendId = new ssoKeyField();
            keyVendId.ColumnName = "BRAND_ID";
            keyVendId.Value      = pVendorId;
            criterias.add(keyVendId);
            */

            List<List<RowColumn>> rs = stmt.getResultList(true, criterias);// CACHE MUST BE SKIPPED
            for(int i=0;i<rs.size();i++)
            {

                ssoVendorPayment paymentN = new ssoVendorPayment();

                paymentN.Id                 = Util.Database.getValString(rs.get(i), "PYM_ID");

                paymentN.vendName           = Util.Database.getValString(rs.get(i), "BRAND");//brand name
                paymentN.vendId             = Util.Database.getValString(rs.get(i), "BRAND_ID");//brand Id
                
                paymentN.txnEffect          = Util.Database.getValString(rs.get(i), "TXN_EFFECT");
                paymentN.paymentType        = Util.Database.getValString(rs.get(i), "PAYMENT_TYPE");
                paymentN.paymentTypeName    = Util.Database.getValString(rs.get(i), "PAYMENT_TYPE_NAME");
                paymentN.paymentTypeName_TR = Util.Database.getValString(rs.get(i), "PAYMENT_TYPE_NAME_TR");
                paymentN.amount_principal   = Util.Database.getValString(rs.get(i), "AMOUNT_PRINCIPAL");
                paymentN.amount_interest    = Util.Database.getValString(rs.get(i), "AMOUNT_INTEREST");
                paymentN.entryDate          = Util.Database.getValString(rs.get(i), "INSERTDATE");
                paymentN.writingDate        = Util.Database.getValString(rs.get(i), "TXN_DATE");
                paymentN.dueDate            = Util.Database.getValString(rs.get(i), "DUE_DATE");
                paymentN.bank               = Util.Database.getValString(rs.get(i), "BANK");
                paymentN.installmentNumber  = Util.Database.getValString(rs.get(i), "INSTALLMENT_NUMBER");
                paymentN.sequence           = Integer.toString(i);
                
                String sDesc = Util.Database.getValString(rs.get(i), "REFERENCE");
                JSONObject jsoDesc = Util.JSON.parseJSON(sDesc);
                String sRef = "";
                if(jsoDesc.get("ref")!=null)
                    sRef = jsoDesc.get("ref").toString();
                
                paymentN.reference          += sRef;//Util.Database.getValString(rs.get(i), "REFERENCE");

                paymentN.accId              = pAccId.toString();
                paymentN.accName            = Util.Database.getValString(rs.get(i), "PROFILENAME");

                payments.add(paymentN);
            }

            return payments;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    /*
    public static ssoInventoryParams registerNewBrandNCodeNCategory(EntityManager  pem,
                                                                    long           pUserId,
                                                                    long           pAccountId,
                                                                    String         pBrandName,
                                                                    String         pItemCode,
                                                                    String         pCategory) throws Exception
    {
        String sItemCode     = "";
        String sCategoryCode = "";
        
        ssoInventoryParams InvParams = new ssoInventoryParams();
        
        try
        {

            StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_PRM_MRC_REGISTER_NEW_BRAND_N_CODE_N_CATEGORY");

            SP.registerStoredProcedureParameter("P_ACC_ID"        , Long.class       , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_BRAND_NAME"    , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_ITEM_CODE"     , String.class     , ParameterMode.IN);
            SP.registerStoredProcedureParameter("P_CATEGORY"      , String.class     , ParameterMode.IN);

            int Colindex = 1;
            SP.SetParameter(Colindex++, pAccountId          , "P_ACC_ID");
            SP.SetParameter(Colindex++, pBrandName          , "P_BRAND_NAME");
            SP.SetParameter(Colindex++, pItemCode           , "P_ITEM_CODE");
            SP.SetParameter(Colindex++, pCategory           , "P_CATEGORY");

            SP.execute();

            List<List<RowColumn>> rs =  SP.getResultList();

            if (rs.size()>0)
            {
                List<RowColumn> RowN = rs.get(0);

                sItemCode     = Util.Database.getValString(RowN, "UID_ITEM_CODE");
                sCategoryCode = Util.Database.getValString(RowN, "UID_CATEGORY_CODE");
                
                InvParams.itemUID = Long.parseLong(sItemCode);
                InvParams.categoryUID = Long.parseLong(sCategoryCode);

            }

            return InvParams;
            
        }
        catch(Exception e)
        {
            throw e;
        }

    }
    */

    public static boolean update_VendorStats(EntityManager       pem,
                                             String              pTxnType,
                                             SsAccInvVendorStats pBrandAcc) throws Exception
    {
        try
        {
            String stSQL = "UPDATE ss_acc_inv_vendor_stats " + 
                           "SET ";

            if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true)
            {
                stSQL += "QUANTITY_ENTERED = QUANTITY_ENTERED + ?, " +
                         "RAW_TOTAL_ENTERED = RAW_TOTAL_ENTERED + ?, " + 
                         "DISCOUNT_TOTAL_ENTERED = DISCOUNT_TOTAL_ENTERED + ?, " + 
                         "SURCHARGE_TOTAL_ENTERED = SURCHARGE_TOTAL_ENTERED + ?, " + 
                         "TAX_TOTAL_ENTERED = TAX_TOTAL_ENTERED + ?, " + 
                         "EXPENSE_TOTAL_ENTERED = EXPENSE_TOTAL_ENTERED + ?, " +
                         "GROSS_TOTAL_ENTERED = GROSS_TOTAL_ENTERED + ?, ";
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                stSQL += "QUANTITY_RETURNED = QUANTITY_RETURNED + ?, " + 
                         "RAW_TOTAL_RETURNED = RAW_TOTAL_RETURNED + ?, " + 
                         "DISCOUNT_TOTAL_RETURNED = DISCOUNT_TOTAL_RETURNED + ?, " + 
                         "SURCHARGE_TOTAL_RETURNED = SURCHARGE_TOTAL_RETURNED + ?, " +    
                         "TAX_TOTAL_RETURNED = TAX_TOTAL_RETURNED + ?, " + 
                         "EXPENSE_TOTAL_RETURNED = EXPENSE_TOTAL_RETURNED + ?, " +
                         "GROSS_TOTAL_RETURNED = GROSS_TOTAL_RETURNED + ?, ";
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INV_LOAD)==true)
            {
                stSQL += "QUANTITY_ADJ_MINUS = QUANTITY_ADJ_MINUS + ( QUANTITY_ENTERED - QUANTITY_RETURNED - QUANTITY_CR_SOLD + QUANTITY_CR_REFUND ), ";//neturalize the existing value by resetting
                stSQL += "QUANTITY_ENTERED = ?, ";// THIS LINE MUST BE AFTER THE PREVIOUS LINE. / This txn overwrites the value !!! (Not incremental)
            }

            stSQL += "BYUSER = ?, " + 
                     "LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%y%m%d%H%i%s000') AS UNSIGNED INTEGER) " +
                     "WHERE " +
                     "STAT = 1 " + 
                     "AND " +
                     "ACCOUNT_ID = ? " + 
                     "AND " +
                     "VENDOR_ID = ?";

            Query stmtBrandAcc = pem.CreateNativeQuery(stSQL);
            int index = 1;

            if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true)
            {
                stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityEntered        , "QUANTITY_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.rawTotalEntered        , "RAW_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.discountTotalEntered   , "DISCOUNT_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.surchargeTotalEntered  , "SURCHARGE_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.taxTotalEntered        , "TAX_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.expenseTotalEntered    , "EXPENSE_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.grossTotalEntered      , "GROSS_TOTAL_ENTERED");
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityReturned       , "QUANTITY_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.rawTotalReturned       , "RAW_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.discountTotalReturned  , "DISCOUNT_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.surchargeTotalReturned , "SURCHARGE_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.taxTotalReturned       , "TAX_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.expenseTotalReturned   , "EXPENSE_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.grossTotalReturned     , "GROSS_TOTAL_RETURNED");
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INV_LOAD)==true)
            {
                stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityEntered  , "QUANTITY_ENTERED");
            }

            stmtBrandAcc.SetParameter(index++, pBrandAcc.accountId                  , "BY_USER");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.accountId                  , "ACCOUNT_ID");
            stmtBrandAcc.SetParameter(index++, pBrandAcc.vendorId                   , "VENDOR_ID");

            stmtBrandAcc.executeUpdate();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    // UB = Update Bill
    // AB = Append Bill (options) - this is real time
    // DB = Delete Bill
    // NB = New Bill
    public static boolean addWork4PendingOperations(   EntityManager pem, 
                                                       BigInteger    pAccountId,
                                                       BigInteger    pVendorId,
                                                       BigInteger    pId,
                                                       String        pOperationCode ) throws Exception
    {
        try
        {
            String sQuery =     "UPDATE ss_acc_inv_vendor_stats " + 
                                "SET " +
                                "PENDING_OPS = JSON_ARRAY_APPEND(IFNULL(PENDING_OPS,'[]'), " +
                                                                 Util.Str.QUOTE("$") + "," +
                                                                 //"'{" + Util.Str.QUOTE("id") + ":" + pId + "," + Util.Str.QUOTE("op") + ":" + Util.Str.QUOTE(pOperationCode) + "}') " +
                                                                 "JSON_OBJECT('id',?, 'op', ?)) " + 
                                "WHERE " + 
                                "STAT = 1 " + 
                                "AND " + 
                                "ACCOUNT_ID = ? " + 
                                "AND " +
                                "VENDOR_ID = ? ";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            stmt.SetParameter(index++, pId                  , "OP_ID");
            stmt.SetParameter(index++, pOperationCode       , "OP_CODE");
            stmt.SetParameter(index++, pAccountId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId            , "VENDOR_ID");

            stmt.executeUpdate();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // THIS NEEDS TO BE REPLACED BY REMOVE FROM QUEUE BY BILL ID IN LATER VERSIONS 
    public static boolean resetPendingQueue(    EntityManager pem, 
                                                BigInteger    pAccountId,
                                                BigInteger    pVendorId
                                           ) throws Exception
    {
        try
        {
                String sQuery = "UPDATE ss_acc_inv_vendor_stats " + 
                                "SET " +
                                "PENDING_OPS = '[]' " + 
                                "WHERE " + 
                                "STAT = 1 " + 
                                "AND " + 
                                "ACCOUNT_ID = ? " + 
                                "AND " +
                                "VENDOR_ID = ? ";

            Query stmt = pem.CreateNativeQuery(sQuery);
            int index = 1;
            stmt.SetParameter(index++, pAccountId           , "ACCOUNT_ID");
            stmt.SetParameter(index++, pVendorId            , "VENDOR_ID");

            stmt.executeUpdate();

            return true;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
        
    
}
