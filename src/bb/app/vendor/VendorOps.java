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
import bb.app.obj.ssoMerchant;
import bb.app.obj.ssoVendorItemStats;
import bb.app.obj.ssoVendorOptionStats;
import bb.app.obj.ssoVendorStats;
import bb.app.payment.PaymentOps;
import static bb.app.payment.PaymentOps.deletePaymentRecord;
import static bb.app.payment.PaymentOps.resetPaymentRelatedMemory;
import bb.app.settings.UXParams;
import bb.app.txn.txnDefs;
import entity.acc.SsAccInvItemStats;
import entity.txn.SsTxnInvVendorPayments;
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
    public static SsAccInvVendors createNewVendor(  EntityManager  pem,
                                                    long           pUserId,
                                                    long           pAccId,
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
            brandDets.accId  = pAccId;
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

            long lUID = pem.persist(brandDets);
            brandDets.uid = lUID;
            
            return brandDets;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void cleanVendorSummary(  EntityManager  pem, 
                                            long           pUserId,
                                            long           pAccId)
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
                                                        long            pAccountId,
                                                        long            pBrandId,
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
        vendorStats.current.received.net        = poStats.netTotalEntered.toString();
        //vendorStats.current.received.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.received.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.received.cumulative = poStats.cumulativeEntered.toString();

        vendorStats.current.sent.quantity   = poStats.quantityReturned.toString();
        vendorStats.current.sent.gross      = poStats.grossTotalReturned.toString();
        vendorStats.current.sent.discount   = poStats.discountTotalReturned.toString();
        vendorStats.current.sent.surcharge  = poStats.surchargeTotalReturned.toString();
        vendorStats.current.sent.tax        = poStats.taxTotalReturned.toString();
        vendorStats.current.sent.net        = poStats.netTotalReturned.toString();
        //vendorStats.current.sent.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.sent.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.sent.cumulative = poStats.cumulativeReturned.toString();

        vendorStats.current.sold.quantity   = poStats.quantitySold.toString();
        vendorStats.current.sold.gross      = poStats.grossTotalSold.toString();
        vendorStats.current.sold.discount   = poStats.discountTotalSold.toString();
        vendorStats.current.sold.surcharge  = poStats.surchargeTotalSold.toString();
        vendorStats.current.sold.tax        = poStats.taxTotalSold.toString();
        vendorStats.current.sold.net        = poStats.netTotalSold.toString();
        //vendorStats.current.sold.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.sold.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.sold.cumulative = poStats.cumulativeSold.toString();

        vendorStats.current.refund.quantity   = poStats.quantityRefund.toString();
        vendorStats.current.refund.gross      = poStats.grossTotalRefund.toString();
        vendorStats.current.refund.discount   = poStats.discountTotalRefund.toString();
        vendorStats.current.refund.surcharge  = poStats.surchargeTotalRefund.toString();
        vendorStats.current.refund.tax        = poStats.taxTotalRefund.toString();
        vendorStats.current.refund.net        = poStats.netTotalRefund.toString();
        //vendorStats.current.refund.finAdjPlus = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.refund.finAdjMinus= poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.refund.cumulative = poStats.cumulativeRefund.toString();

        // adj (+)
        vendorStats.current.adjPlus.quantity   = poStats.quantityAdjPlus.toString();
        vendorStats.current.adjPlus.gross      = poStats.finAdjPlusGrossTotal.toString();
        vendorStats.current.adjPlus.discount   = poStats.finAdjPlusDiscountTotal.toString();
        vendorStats.current.adjPlus.surcharge  = poStats.finAdjPlusSurchargeTotal.toString();
        vendorStats.current.adjPlus.tax        = poStats.finAdjPlusTaxTotal.toString();
        vendorStats.current.adjPlus.net        = poStats.finAdjPlusNetTotal.toString();
        //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

        // adj (-)
        vendorStats.current.adjMinus.quantity   = poStats.quantityAdjMinus.toString();
        vendorStats.current.adjMinus.gross      = poStats.finAdjMinusGrossTotal.toString();
        vendorStats.current.adjMinus.discount   = poStats.finAdjMinusDiscountTotal.toString();
        vendorStats.current.adjMinus.surcharge  = poStats.finAdjMinusSurchargeTotal.toString();
        vendorStats.current.adjMinus.tax        = poStats.finAdjMinusTaxTotal.toString();
        vendorStats.current.adjMinus.net        = poStats.finAdjMinusNetTotal.toString();
        //vendorStats.current.adjMinus.cumulative = poStats.cumulativeRefund.toString();

        // EOD
        //------------------------------------------------------------------
        vendorStats.eod.received.quantity   = poStats.lastEodQuantityEntered.toString();
        vendorStats.eod.received.gross      = poStats.lastEodGrossTotalEntered.toString();
        vendorStats.eod.received.discount   = poStats.lastEodDiscountTotalEntered.toString();
        vendorStats.eod.received.surcharge  = poStats.lastEodSurchargeTotalEntered.toString();
        vendorStats.eod.received.tax        = poStats.lastEodTaxTotalEntered.toString();
        vendorStats.eod.received.net        = poStats.lastEodNetTotalEntered.toString();
        //vendorStats.eod.received.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.received.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.received.cumulative = poStats.cumulativeEntered.toString();

        vendorStats.eod.sent.quantity   = poStats.lastEodQuantityReturned.toString();
        vendorStats.eod.sent.gross      = poStats.lastEodGrossTotalReturned.toString();
        vendorStats.eod.sent.discount   = poStats.lastEodDiscountTotalReturned.toString();
        vendorStats.eod.sent.surcharge  = poStats.lastEodSurchargeTotalReturned.toString();
        vendorStats.eod.sent.tax        = poStats.lastEodTaxTotalReturned.toString();
        vendorStats.eod.sent.net        = poStats.lastEodNetTotalReturned.toString();
        //vendorStats.eod.sent.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.sent.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.sent.cumulative = poStats.cumulativeReturned.toString();

        vendorStats.eod.sold.quantity   = poStats.lastEodQuantitySold.toString();
        vendorStats.eod.sold.gross      = poStats.lastEodGrossTotalSold.toString();
        vendorStats.eod.sold.discount   = poStats.lastEodDiscountTotalSold.toString();
        vendorStats.eod.sold.surcharge  = poStats.lastEodSurchargeTotalSold.toString();
        vendorStats.eod.sold.tax        = poStats.lastEodTaxTotalSold.toString();
        vendorStats.eod.sold.net        = poStats.lastEodNetTotalSold.toString();
        //vendorStats.eod.sold.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.sold.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.sold.cumulative = poStats.cumulativeSold.toString();

        vendorStats.eod.refund.quantity   = poStats.lastEodQuantityRefund.toString();
        vendorStats.eod.refund.gross      = poStats.lastEodGrossTotalRefund.toString();
        vendorStats.eod.refund.discount   = poStats.lastEodDiscountTotalRefund.toString();
        vendorStats.eod.refund.surcharge  = poStats.lastEodSurchargeTotalRefund.toString();
        vendorStats.eod.refund.tax        = poStats.lastEodTaxTotalRefund.toString();
        vendorStats.eod.refund.net        = poStats.lastEodNetTotalRefund.toString();
        //vendorStats.eod.refund.finAdjPlus = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.eod.refund.finAdjMinus= poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.eod.refund.cumulative = poStats.cumulativeRefund.toString();

        // adj (+)
        vendorStats.eod.adjPlus.quantity   = poStats.lastEodQuantityAdjPlus.toString();
        vendorStats.eod.adjPlus.gross      = poStats.lastEodFinAdjPlusGrossTotal.toString();
        vendorStats.eod.adjPlus.discount   = poStats.lastEodFinAdjPlusDiscountTotal.toString();
        vendorStats.eod.adjPlus.surcharge  = poStats.lastEodFinAdjPlusSurchargeTotal.toString();
        vendorStats.eod.adjPlus.tax        = poStats.lastEodFinAdjPlusTaxTotal.toString();
        vendorStats.eod.adjPlus.net        = poStats.lastEodFinAdjPlusNetTotal.toString();
        //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

        // adj (-)
        vendorStats.eod.adjMinus.quantity   = poStats.lastEodQuantityAdjMinus.toString();
        vendorStats.eod.adjMinus.gross      = poStats.lastEodFinAdjMinusGrossTotal.toString();
        vendorStats.eod.adjMinus.discount   = poStats.lastEodFinAdjMinusDiscountTotal.toString();
        vendorStats.eod.adjMinus.surcharge  = poStats.lastEodFinAdjMinusSurchargeTotal.toString();
        vendorStats.eod.adjMinus.tax        = poStats.lastEodFinAdjMinusTaxTotal.toString();
        vendorStats.eod.adjMinus.net        = poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.current.adjMinus.cumulative = poStats.cumulativeRefund.toString();

        // REVOLVING
        //------------------------------------------------------------------
        vendorStats.revolving.received.quantity   = poStats.revolvingQuantityEntered.toString();
        vendorStats.revolving.received.gross      = poStats.revolvingGrossTotalEntered.toString();
        vendorStats.revolving.received.discount   = poStats.revolvingDiscountTotalEntered.toString();
        vendorStats.revolving.received.surcharge  = poStats.revolvingSurchargeTotalEntered.toString();
        vendorStats.revolving.received.tax        = poStats.revolvingTaxTotalEntered.toString();
        vendorStats.revolving.received.net        = poStats.revolvingNetTotalEntered.toString();
        //vendorStats.revolving.received.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.received.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.received.cumulative = poStats.cumulativeEntered.toString();

        vendorStats.revolving.sent.quantity   = poStats.revolvingQuantityReturned.toString();
        vendorStats.revolving.sent.gross      = poStats.revolvingGrossTotalReturned.toString();
        vendorStats.revolving.sent.discount   = poStats.revolvingDiscountTotalReturned.toString();
        vendorStats.revolving.sent.surcharge  = poStats.revolvingSurchargeTotalReturned.toString();
        vendorStats.revolving.sent.tax        = poStats.revolvingTaxTotalReturned.toString();
        vendorStats.revolving.sent.net        = poStats.revolvingNetTotalReturned.toString();
        //vendorStats.revolving.sent.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.sent.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.sent.cumulative = poStats.cumulativeReturned.toString();

        vendorStats.revolving.sold.quantity   = poStats.revolvingQuantitySold.toString();
        vendorStats.revolving.sold.gross      = poStats.revolvingGrossTotalSold.toString();
        vendorStats.revolving.sold.discount   = poStats.revolvingDiscountTotalSold.toString();
        vendorStats.revolving.sold.surcharge  = poStats.revolvingSurchargeTotalSold.toString();
        vendorStats.revolving.sold.tax        = poStats.revolvingTaxTotalSold.toString();
        vendorStats.revolving.sold.net        = poStats.revolvingNetTotalSold.toString();
        //vendorStats.revolving.sold.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.sold.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.sold.cumulative = poStats.cumulativeSold.toString();

        vendorStats.revolving.refund.quantity   = poStats.revolvingQuantityRefund.toString();
        vendorStats.revolving.refund.gross      = poStats.revolvingGrossTotalRefund.toString();
        vendorStats.revolving.refund.discount   = poStats.revolvingDiscountTotalRefund.toString();
        vendorStats.revolving.refund.surcharge  = poStats.revolvingSurchargeTotalRefund.toString();
        vendorStats.revolving.refund.tax        = poStats.revolvingTaxTotalRefund.toString();
        vendorStats.revolving.refund.net        = poStats.revolvingNetTotalRefund.toString();
        //vendorStats.revolving.refund.finAdjPlus = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.revolving.refund.finAdjMinus= poStats.revolvingFinAdjMinusNetTotal.toString();
        //vendorStats.revolving.refund.cumulative = poStats.cumulativeRefund.toString();

        // adj (+)
        vendorStats.eod.adjPlus.quantity   = poStats.revolvingQuantityAdjPlus.toString();
        vendorStats.eod.adjPlus.gross      = poStats.revolvingFinAdjPlusGrossTotal.toString();
        vendorStats.eod.adjPlus.discount   = poStats.revolvingFinAdjPlusDiscountTotal.toString();
        vendorStats.eod.adjPlus.surcharge  = poStats.revolvingFinAdjPlusSurchargeTotal.toString();
        vendorStats.eod.adjPlus.tax        = poStats.revolvingFinAdjPlusTaxTotal.toString();
        vendorStats.eod.adjPlus.net        = poStats.revolvingFinAdjPlusNetTotal.toString();
        //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

        // adj (-)
        vendorStats.eod.adjMinus.quantity   = poStats.lastEodQuantityAdjMinus.toString();
        vendorStats.eod.adjMinus.gross      = poStats.lastEodFinAdjMinusGrossTotal.toString();
        vendorStats.eod.adjMinus.discount   = poStats.lastEodFinAdjMinusDiscountTotal.toString();
        vendorStats.eod.adjMinus.surcharge  = poStats.lastEodFinAdjMinusSurchargeTotal.toString();
        vendorStats.eod.adjMinus.tax        = poStats.lastEodFinAdjMinusTaxTotal.toString();
        vendorStats.eod.adjMinus.net        = poStats.lastEodFinAdjMinusNetTotal.toString();
        //vendorStats.current.adjMinus.cumulative = poStats.cumulativeRefund.toString();
        
        
        return vendorStats;
    }

    public static ArrayList<ssoVendorItemStats> getItemStats4Vendor(EntityManager   pem,
                                                                    long            pAccountId,
                                                                    long            pBrandId,
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
                    // Item Options
                    itemStatN.OptGroups = Util.Database.getValString(rowN, "OPT_GROUPS").toString();

                    // ITEM-CURRENT-RECEIVED
                    itemStatN.current.received.quantity  = Util.Database.getValString(rowN, "ITM_Q_RECEIVED").toString();
                    itemStatN.current.received.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_RECEIVED").toString();
                    itemStatN.current.received.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_RECEIVED").toString();
                    //itemStatN.current.received.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.received.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-CURRENT-SENT
                    itemStatN.current.sent.quantity  = Util.Database.getValString(rowN, "ITM_Q_SENT").toString();
                    itemStatN.current.sent.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_SENT").toString();
                    itemStatN.current.sent.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_SENT").toString();
                    itemStatN.current.sent.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_SENT").toString();
                    itemStatN.current.sent.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_SENT").toString();
                    itemStatN.current.sent.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_SENT").toString();
                    //itemStatN.current.sent.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.sent.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-CURRENT-SOLD
                    itemStatN.current.sold.quantity  = Util.Database.getValString(rowN, "ITM_Q_SOLD").toString();
                    itemStatN.current.sold.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_SOLD").toString();
                    itemStatN.current.sold.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_SOLD").toString();
                    itemStatN.current.sold.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_SOLD").toString();
                    itemStatN.current.sold.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_SOLD").toString();
                    itemStatN.current.sold.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_SOLD").toString();
                    //itemStatN.current.sold.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.sold.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-CURRENT-REFUND
                    itemStatN.current.refund.quantity  = Util.Database.getValString(rowN, "ITM_Q_REFUND").toString();
                    itemStatN.current.refund.gross     = Util.Database.getValString(rowN, "ITM_GROSS_TOTAL_REFUND").toString();
                    itemStatN.current.refund.discount  = Util.Database.getValString(rowN, "ITM_DISC_TOTAL_REFUND").toString();
                    itemStatN.current.refund.surcharge = Util.Database.getValString(rowN, "ITM_SRCHG_TOTAL_REFUND").toString();
                    itemStatN.current.refund.tax       = Util.Database.getValString(rowN, "ITM_TAX_TOTAL_REFUND").toString();
                    itemStatN.current.refund.net       = Util.Database.getValString(rowN, "ITM_NET_TOTAL_REFUND").toString();
                    //itemStatN.current.refund.finAdjPlus = Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.current.refund.finAdjMinus= Util.Database.getValString(rowN, "ITM_Q_FIN_ADJ_MINUS").toString();

                    // current-adj (+)
                    itemStatN.current.adjPlus.quantity   = Util.Database.getValString(rowN, "QUANTITY_ADJ_PLUS").toString();
                    itemStatN.current.adjPlus.net        = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_NET_TOTAL").toString();
                    itemStatN.current.adjPlus.discount   = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_DISCOUNT_TOTAL").toString();
                    itemStatN.current.adjPlus.surcharge  = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_SURCHARGE_TOTAL").toString();
                    itemStatN.current.adjPlus.tax        = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.current.adjPlus.gross      = Util.Database.getValString(rowN, "FIN_ADJ_PLUS_GROSS_TOTAL").toString();
                    //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

                    // current-adj (-)
                    itemStatN.current.adjMinus.quantity   = Util.Database.getValString(rowN, "QUANTITY_ADJ_MINUS").toString();
                    itemStatN.current.adjMinus.net        = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_NET_TOTAL").toString();
                    itemStatN.current.adjMinus.discount   = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_DISCOUNT_TOTAL").toString();
                    itemStatN.current.adjMinus.surcharge  = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_SURCHARGE_TOTAL").toString();
                    itemStatN.current.adjMinus.tax        = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_TAX_TOTAL").toString();
                    itemStatN.current.adjMinus.gross      = Util.Database.getValString(rowN, "FIN_ADJ_MINUS_GROSS_TOTAL").toString();

                    //--------------------------------------------------------------------------------

                    // ITEM-EOD-RECEIVED
                    itemStatN.eod.received.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_RECEIVED").toString();
                    itemStatN.eod.received.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_RECEIVED").toString();
                    itemStatN.eod.received.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_RECEIVED").toString();
                    //itemStatN.eod.received.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.received.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-EOD-SENT
                    itemStatN.eod.sent.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_SENT").toString();
                    itemStatN.eod.sent.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_SENT").toString();
                    itemStatN.eod.sent.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_SENT").toString();
                    itemStatN.eod.sent.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_SENT").toString();
                    itemStatN.eod.sent.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_SENT").toString();
                    itemStatN.eod.sent.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_SENT").toString();
                    //itemStatN.eod.sent.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.sent.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-EOD-SOLD
                    itemStatN.eod.sold.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_SOLD").toString();
                    itemStatN.eod.sold.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_SOLD").toString();
                    itemStatN.eod.sold.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_SOLD").toString();
                    itemStatN.eod.sold.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_SOLD").toString();
                    itemStatN.eod.sold.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_SOLD").toString();
                    itemStatN.eod.sold.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_SOLD").toString();
                    //itemStatN.eod.sold.finAdjPlus = Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.eod.sold.finAdjMinus= Util.Database.getValString(rowN, "EOD_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-EOD-REFUND
                    itemStatN.eod.refund.quantity  = Util.Database.getValString(rowN, "EOD_ITM_Q_REFUND").toString();
                    itemStatN.eod.refund.gross     = Util.Database.getValString(rowN, "EOD_ITM_GROSS_TOTAL_REFUND").toString();
                    itemStatN.eod.refund.discount  = Util.Database.getValString(rowN, "EOD_ITM_DISC_TOTAL_REFUND").toString();
                    itemStatN.eod.refund.surcharge = Util.Database.getValString(rowN, "EOD_ITM_SRCHG_TOTAL_REFUND").toString();
                    itemStatN.eod.refund.tax       = Util.Database.getValString(rowN, "EOD_ITM_TAX_TOTAL_REFUND").toString();
                    itemStatN.eod.refund.net       = Util.Database.getValString(rowN, "EOD_ITM_NET_TOTAL_REFUND").toString();
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
                    itemStatN.eod.adjMinus.tax        = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.eod.adjMinus.gross      = Util.Database.getValString(rowN, "LAST_EOD_FIN_ADJ_PLUS_GROSS_TOTAL").toString();

                    //--------------------------------------------------------------------------------

                    // ITEM-REVOLVING-RECEIVED
                    itemStatN.revolving.received.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_RECEIVED").toString();
                    itemStatN.revolving.received.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_RECEIVED").toString();
                    itemStatN.revolving.received.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_RECEIVED").toString();
                    //itemStatN.revolving.received.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.received.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-REVOLVING-SENT
                    itemStatN.revolving.sent.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_SENT").toString();
                    itemStatN.revolving.sent.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_SENT").toString();
                    itemStatN.revolving.sent.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_SENT").toString();
                    //itemStatN.revolving.sent.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.sent.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-REVOLVING-SOLD
                    itemStatN.revolving.sold.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_SOLD").toString();
                    itemStatN.revolving.sold.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_SOLD").toString();
                    itemStatN.revolving.sold.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_SOLD").toString();
                    itemStatN.revolving.sold.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_SOLD").toString();
                    itemStatN.revolving.sold.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_SOLD").toString();
                    itemStatN.revolving.sold.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_SOLD").toString();
                    //itemStatN.revolving.sold.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.sold.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // ITEM-REVOLVING-REFUND
                    itemStatN.revolving.refund.quantity  = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_REFUND").toString();
                    itemStatN.revolving.refund.gross     = Util.Database.getValString(rowN, "REVOLVING_ITM_GROSS_TOTAL_REFUND").toString();
                    itemStatN.revolving.refund.discount  = Util.Database.getValString(rowN, "REVOLVING_ITM_DISC_TOTAL_REFUND").toString();
                    itemStatN.revolving.refund.surcharge = Util.Database.getValString(rowN, "REVOLVING_ITM_SRCHG_TOTAL_REFUND").toString();
                    itemStatN.revolving.refund.tax       = Util.Database.getValString(rowN, "REVOLVING_ITM_TAX_TOTAL_REFUND").toString();
                    itemStatN.revolving.refund.net       = Util.Database.getValString(rowN, "REVOLVING_ITM_NET_TOTAL_REFUND").toString();
                    //itemStatN.revolving.refund.finAdjPlus = Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_PLUS").toString();
                    //itemStatN.revolving.refund.finAdjMinus= Util.Database.getValString(rowN, "REVOLVING_ITM_Q_FIN_ADJ_MINUS").toString();

                    // REVOLVING-adj (+)
                    itemStatN.revolving.adjPlus.quantity   = Util.Database.getValString(rowN, "REVOLVING_QUANTITY_ADJ_PLUS").toString();
                    itemStatN.revolving.adjPlus.net        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_NET_TOTAL").toString();
                    itemStatN.revolving.adjPlus.discount   = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_DISCOUNT_TOTAL").toString();
                    itemStatN.revolving.adjPlus.surcharge  = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_SURCHARGE_TOTAL").toString();
                    itemStatN.revolving.adjPlus.tax        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.revolving.adjPlus.gross      = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_GROSS_TOTAL").toString();
                    //vendorStats.current.adjPlus.cumulative = poStats.cumulativeRefund.toString();

                    // REVOLVING-adj (-)
                    itemStatN.revolving.adjMinus.quantity   = Util.Database.getValString(rowN, "REVOLVING_QUANTITY_ADJ_MINUS").toString();
                    itemStatN.revolving.adjMinus.net        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_NET_TOTAL").toString();
                    itemStatN.revolving.adjMinus.discount   = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_DISCOUNT_TOTAL").toString();
                    itemStatN.revolving.adjMinus.surcharge  = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_MINUS_SURCHARGE_TOTAL").toString();
                    itemStatN.revolving.adjMinus.tax        = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_TAX_TOTAL").toString();
                    itemStatN.revolving.adjMinus.gross      = Util.Database.getValString(rowN, "REVOLVING_FIN_ADJ_PLUS_GROSS_TOTAL").toString();

                    // OPTION-CURRENT
                    itemStatN.optStats.current.received     = Util.Database.getValString(rowN, "OPT_ENTERED").toString();
                    itemStatN.optStats.current.sent         = Util.Database.getValString(rowN, "OPT_RETURNED").toString();
                    itemStatN.optStats.current.sold         = Util.Database.getValString(rowN, "OPT_SOLD").toString();
                    itemStatN.optStats.current.refund       = Util.Database.getValString(rowN, "OPT_REFUND").toString();
                    itemStatN.optStats.current.finAdjPlus   = Util.Database.getValString(rowN, "OPT_ADJ_PLUS").toString();
                    itemStatN.optStats.current.finAdjMinus  = Util.Database.getValString(rowN, "OPT_ADJ_MINUS").toString();

                    // OPTION-EOD
                    itemStatN.optStats.eod.received     = Util.Database.getValString(rowN, "OPT_EOD_ENTERED").toString();
                    itemStatN.optStats.eod.sent         = Util.Database.getValString(rowN, "OPT_EOD_RETURNED").toString();
                    itemStatN.optStats.eod.sold         = Util.Database.getValString(rowN, "OPT_EOD_SOLD").toString();
                    itemStatN.optStats.eod.refund       = Util.Database.getValString(rowN, "OPT_EOD_REFUND").toString();
                    itemStatN.optStats.eod.finAdjPlus   = Util.Database.getValString(rowN, "OPT_EOD_ADJ_PLUS").toString();
                    itemStatN.optStats.eod.finAdjMinus  = Util.Database.getValString(rowN, "OPT_EOD_ADJ_MINUS").toString();

                    // OPTION-REV
                    itemStatN.optStats.revolving.received     = Util.Database.getValString(rowN, "OPT_REV_ENTERED").toString();
                    itemStatN.optStats.revolving.sent         = Util.Database.getValString(rowN, "OPT_REV_RETURNED").toString();
                    itemStatN.optStats.revolving.sold         = Util.Database.getValString(rowN, "OPT_REV_SOLD").toString();
                    itemStatN.optStats.revolving.refund       = Util.Database.getValString(rowN, "OPT_REV_REFUND").toString();
                    itemStatN.optStats.revolving.finAdjPlus   = Util.Database.getValString(rowN, "OPT_REV_ADJ_PLUS").toString();
                    itemStatN.optStats.revolving.finAdjMinus  = Util.Database.getValString(rowN, "OPT_REV_ADJ_MINUS").toString();

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
                                                         long            pAccountId,
                                                         long            pBrandId
                                                         ) throws Exception
    {
        try
        {
            SsAccInvVendorStats brandAcc = new SsAccInvVendorStats();

            int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

            brandAcc.financialYear = ThisYear;
            brandAcc.vendorId       = pBrandId;
            brandAcc.accountId     = pAccountId;

            brandAcc.quantityEntered    = new BigDecimal(BigInteger.ZERO);
            brandAcc.quantityReturned   = new BigDecimal(BigInteger.ZERO);
            brandAcc.quantitySold       = new BigDecimal(BigInteger.ZERO);

            brandAcc.grossTotalEntered  = new BigDecimal(BigInteger.ZERO);
            brandAcc.grossTotalReturned = new BigDecimal(BigInteger.ZERO);
            brandAcc.grossTotalSold     = new BigDecimal(BigInteger.ZERO);

            brandAcc.discountTotalEntered  = new BigDecimal(BigInteger.ZERO);
            brandAcc.discountTotalReturned = new BigDecimal(BigInteger.ZERO);
            brandAcc.discountTotalSold     = new BigDecimal(BigInteger.ZERO);

            brandAcc.surchargeTotalEntered  = new BigDecimal(BigInteger.ZERO);
            brandAcc.surchargeTotalReturned = new BigDecimal(BigInteger.ZERO);
            brandAcc.surchargeTotalSold     = new BigDecimal(BigInteger.ZERO);

            brandAcc.taxTotalEntered        = new BigDecimal(BigInteger.ZERO);
            brandAcc.taxTotalReturned       = new BigDecimal(BigInteger.ZERO);
            brandAcc.taxTotalSold           = new BigDecimal(BigInteger.ZERO);

            brandAcc.netTotalEntered        = new BigDecimal(BigInteger.ZERO);
            brandAcc.netTotalReturned       = new BigDecimal(BigInteger.ZERO);
            brandAcc.netTotalSold           = new BigDecimal(BigInteger.ZERO);

            brandAcc.finAdjPlusNetTotal      = new BigDecimal(BigInteger.ZERO);
            brandAcc.finAdjMinusNetTotal     = new BigDecimal(BigInteger.ZERO);
            // balance is auto - generated (22.03.2024)
            //brandAcc.balance                = new BigDecimal(BigInteger.ZERO);

            brandAcc.revolvingBalance = BigDecimal.ZERO;

            brandAcc.uid = pem.persist(brandAcc);

            return brandAcc;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBrandDets getVendorDetailByName(EntityManager pem, long pUserId, String pName) throws Exception
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

    public static ArrayList<ssoBrandDets> getAllVendorDetails(EntityManager pem, long pUserId) throws Exception
    {
        ArrayList<ssoBrandDets> allBrands = new ArrayList<ssoBrandDets>();

        try
        {
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
            
            return allBrands;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ssoBrandDets getVendorDetail(EntityManager pem, long pAccId, long pVendorId) throws Exception
    {

        try
        {
            Query stmtQry = pem.createNamedQuery("SsAccInvBrandDets.getMyBrands", SsAccInvVendors.class);

            int ParIndex = 1;
            stmtQry.SetParameter(ParIndex++, pAccId     , "ACC_ID");//THIS WILL BE CHANGED TO ACCOUNT

            List<List<RowColumn>> rs =  stmtQry.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoBrandDets detN = new ssoBrandDets();

                detN.uid               = Long.parseLong(Util.Database.getValString(rowN, "UID"));
                if (detN.uid == pVendorId)
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
    
    public static ArrayList<ssoBrandDets> getAccountVendors(EntityManager pem, long pAccId) throws Exception
    {
        ArrayList<ssoBrandDets> brandDets = new ArrayList<ssoBrandDets>();

        try
        {
            Query stmtQry = pem.createNamedQuery("SsAccInvBrandDets.getMyBrands", SsAccInvVendors.class);

            int ParIndex = 1;
            stmtQry.SetParameter(ParIndex++, pAccId     , "ACC_ID");//THIS WILL BE CHANGED TO ACCOUNT

            List<List<RowColumn>> rs =  stmtQry.getResultList();
            for(int i=0; i<rs.size(); i++)
            {
                List<RowColumn> rowN = rs.get(i);

                ssoBrandDets newDet = new ssoBrandDets();

                newDet.uid               = Long.parseLong(Util.Database.getValString(rowN, "UID"));
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

    public static long updatePayment(    EntityManager  pem, 
                                            long           pUserId,
                                            long           pAccId,
                                            long           pPaymentId,
                                            long           pPaymentAccId,
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
                                            String         psPaymentGroup
                                    ) throws Exception
    {
        try
        {
            long lVendorId = Long.parseLong(pVendorId);
            
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

            long lUID = VendorOps.addPayment4Vendor(pem,
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
                                                    psPaymentGroup,
                                                    pbPaymentSent);
            
            resetPaymentRelatedMemory(pem, pPaymentAccId, lVendorId);
            
            return lUID;
        }
        catch(Exception e)
        {
          throw e;  
        }
    }

    public static long addPayment4Vendor(   EntityManager  pem,
                                            long           pUserId,
                                            long           pAccId,
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
                                            String         psPaymentGroup,
                                            boolean        pbPaymentSent) throws Exception
    {
        long lPaymentUID = -1;

        try
        {
            long lVendorId = Long.parseLong(pVendorId);
            
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
                                                    psPaymentGroup, 
                                                    pbPaymentSent);

            // UPDATE STATS
            //----------------------------------------------------------------
            VendorFinancial.updateBalanceByNewPayment(pem, 
                                                      pUserId, 
                                                      pAccId, 
                                                      lVendorId, 
                                                      pAmountPrincipal, 
                                                      pAmountInterest);

            return lPaymentUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    public static long createPaymentRow4Vendor(EntityManager  pem,
                                                long           pUserId,
                                                long           pAccId,
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
                                                String         psPaymentGroup,
                                                boolean        pbPaymentSent) throws Exception
    {
        int ThisYear = Integer.parseInt(Util.DateTime.GetDateTime_s().substring(0, 4));

        try
        {
            SsTxnInvVendorPayments payment = new SsTxnInvVendorPayments();
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

            long lPymGroupId = 0;
            if(psPaymentGroup.trim().length()>0)
            {
                // Get Payment Group Info
                lPymGroupId = DictionaryOps.getPaymentGroupIDByName(pem, pUserId, psPaymentGroup);
                if(lPymGroupId==-1)
                {
                    lPymGroupId = DictionaryOps.addNewPaymentGroup(pem, pUserId, pAccId, psPaymentGroup);
                }
            }

            payment.txnEffect = txnDefs.getTransactionEffect(payment.txnCode);

            long lVendorId = Long.parseLong(pVendorId);
            vendorDets = getVendorDetail(pem, pAccId, lVendorId);
            if (vendorDets!=null)
            {
                payment.paymentType   = pPaymentType;
                payment.financialYear = ThisYear;
                payment.vendorDesc    = pVendorName;//pVendorId;
                payment.vendorId      = vendorDets.uid;
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
                    payment.amountPrincipal = new BigDecimal(pAmountPrincipal);

                if (pAmountInterest.trim().length()>0)
                    payment.amountInterest = new BigDecimal(pAmountInterest);

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

                long lUID = pem.persist(payment);

                return lUID;
            }

            return -1;
        }
        catch(Exception e)
        {
            throw e;
        }
    
    }
    
    // Gets this year payments only
    public static ArrayList<ssoVendorPaymentSummary> getHistorySummaryData(     EntityManager  pem, 
                                                                                long           pAccId,
                                                                                long           pThisYear,
                                                                                boolean        pbCleanMemory) throws Exception
    {
        ArrayList<ssoVendorPaymentSummary> summary = new ArrayList<ssoVendorPaymentSummary>();

        try
        {
            if(pbCleanMemory==true)
                pem.flush();

            Query stmt = pem.createNamedQuery("SsAccInvBrandsPayments.getPaymentsSummary", SsTxnInvVendorPayments.class);
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

                sumN.accId                   = Long.toString(pAccId);
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
                                                                      long           pAccId,
                                                                      long           pVendorId,
                                                                      long           pYear,
                                                                      boolean        pbCleanMemory,
                                                                      int            piStartRowIndex,
                                                                      boolean        pbFullRows) throws Exception
    {
        ArrayList<ssoVendorPayment> payments = new ArrayList<ssoVendorPayment>();

        try
        {
            if (pbFullRows==true)
                pem.setMaxRowNumber(UXParams.UX_MAX_ROW_NUMBER_PER_PAGE_FULL_LOAD);
            else
                pem.setMaxRowNumber(UXParams.UX_DEFAULT_ROW_NUMER_PER_PAGE_LOAD);

            pem.setRowStartIndex(piStartRowIndex);
            
            ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();

            if(pbCleanMemory==true)
                pem.flush();//this will execute flush before executing the query ahead

            Query stmt = pem.createNamedQuery("SsAccInvBrandsPayments.getHistoryOfLast2Years", SsTxnInvVendorPayments.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId       , "ACCOUNT_ID");
            stmt.SetParameter(index++, pYear        , "P_DATE");//START DATE
            //stmt.SetParameter(index++, pTodaysDate      , "P_DATE");//END DATE
            
            // This filter thru after the resultset fetched
            ssoKeyField keyVendId = new ssoKeyField();
            keyVendId.ColumnName = "BRAND_ID";
            keyVendId.Value      = (int)pVendorId;
            criterias.add(keyVendId);

            List<List<RowColumn>> rs = stmt.getResultList(false, criterias);
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
                String sRef = jsoDesc.get("ref").toString();
                
                paymentN.reference          += sRef;//Util.Database.getValString(rs.get(i), "REFERENCE");

                paymentN.accId              = Long.toString(pAccId);
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
                         "NET_TOTAL_ENTERED = NET_TOTAL_ENTERED + ?, " + 
                         "DISCOUNT_TOTAL_ENTERED = DISCOUNT_TOTAL_ENTERED + ?, " + 
                         "SURCHARGE_TOTAL_ENTERED = SURCHARGE_TOTAL_ENTERED + ?, " + 
                         "TAX_TOTAL_ENTERED = TAX_TOTAL_ENTERED + ?, " + 
                         "GROSS_TOTAL_ENTERED = GROSS_TOTAL_ENTERED + ?, ";
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                stSQL += "QUANTITY_RETURNED = QUANTITY_RETURNED + ?, " + 
                         "NET_TOTAL_RETURNED = NET_TOTAL_RETURNED + ?, " + 
                         "DISCOUNT_TOTAL_RETURNED = DISCOUNT_TOTAL_RETURNED + ?, " + 
                         "SURCHARGE_TOTAL_RETURNED = SURCHARGE_TOTAL_RETURNED + ?, " +    
                         "TAX_TOTAL_RETURNED = TAX_TOTAL_RETURNED + ?, " + 
                         "GROSS_TOTAL_RETURNED = GROSS_TOTAL_RETURNED + ?, ";
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true)
            {
                stSQL += "QUANTITY_ADJ_MINUS = QUANTITY_ADJ_MINUS + ( QUANTITY_ENTERED - QUANTITY_RETURNED - QUANTITY_SOLD + QUANTITY_REFUND ), ";//neturalize the existing value by resetting
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
                stmtBrandAcc.SetParameter(index++, pBrandAcc.netTotalEntered        , "NET_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.discountTotalEntered   , "DISCOUNT_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.surchargeTotalEntered  , "SURCHARGE_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.taxTotalEntered        , "TAX_TOTAL_ENTERED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.grossTotalEntered      , "GROSS_TOTAL_ENTERED");
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
            {
                stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityReturned       , "QUANTITY_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.netTotalReturned       , "NET_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.discountTotalReturned  , "DISCOUNT_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.surchargeTotalReturned , "SURCHARGE_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.taxTotalReturned       , "TAX_TOTAL_RETURNED");
                stmtBrandAcc.SetParameter(index++, pBrandAcc.grossTotalReturned     , "GROSS_TOTAL_RETURNED");
            }
            else if(pTxnType.equals(txnDefs.TXN_TYPE_INV_UPDATE)==true)
            {
                stmtBrandAcc.SetParameter(index++, pBrandAcc.quantityEntered  , "QUANTITY_ENTERED");
            }

            stmtBrandAcc.SetParameter(index++, Long.toString(pBrandAcc.accountId)   , "BY_USER");
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
}
