/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.cashier;

import bb.app.account.AccountMisc;
import bb.app.obj.ssoCashRegisterParams;
import bb.app.obj.ssoCashierPOSMenuItem;
import bb.app.obj.ssoMerchantPreferences;
import bb.app.obj.ssoSalesConsultant;
import bb.app.txn.txnDefs;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvVendorStats;
import entity.cashier.SsCshMenu;
import entity.cashier.SsCshTerminalBatches;
import entity.dct.SsDctInvCategories;
import entity.eod.SsEodTxnSales;
import entity.txn.SsTxnSalesCashier;
import entity.user.SsUsrPersonnel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.persistence.ssoKeyField;
import jaxesa.util.Util;
import jaxesa.util.ssoDBRowLimits;

/**
 *
 * @author Administrator
 */
public final class CashRegister 
{
    public static ssoCashRegisterParams getParams(  EntityManager pem, 
                                                    BigInteger          pUserId,
                                                    BigInteger          pAccId,
                                                    boolean       bResetCache) throws Exception
    {
        try
        {
            //ArrayList<String> categoryItems = new ArrayList<String>();
            ssoCashRegisterParams CRParams = new ssoCashRegisterParams();

            //SsCshMenu
            ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();

            /*
            ssoKeyField criteria1 = new ssoKeyField();
            criteria1.ColumnName = "ACCOUNT_ID";
            criteria1.Value      = pAccId;
            criterias.add(criteria1);
            */

            if(bResetCache==true)
            {
                ssbMenuOps.updateCashierMenu(pem, pAccId);
                //pem.flush();
            }

            //ArrayList<SsCshMenu> rows = new ArrayList<SsCshMenu>();
            //rows = (ArrayList<SsCshMenu>)pem.findAll(SsCshMenu.class, criterias);
            ArrayList<ssoCashierPOSMenuItem> menuItems = new ArrayList<ssoCashierPOSMenuItem>();
            ArrayList<String> categoryItems = new ArrayList<String>();

            menuItems = getParams_MenuItems(pem, pUserId, pAccId, bResetCache);

            for(ssoCashierPOSMenuItem menuN: menuItems)
            {
                add2Category(categoryItems, menuN.category);
            }

            CRParams.menuItems      = menuItems;
            CRParams.categoryItems  = categoryItems;

            // FINANCIAL SETTINGS
            //-----------------------------------------------------------------
            ssoMerchantPreferences mrcPrefs = new ssoMerchantPreferences();
            mrcPrefs = AccountMisc.getAccountSettings(pem, pUserId, pAccId);
            if(mrcPrefs!=null)
            {
                CRParams.taxSet.isTaxIncludedInPrice = mrcPrefs.isTaxInSalesPrice;
                CRParams.taxSet.taxRate              = mrcPrefs.taxRate;
                CRParams.taxSet.currencyCode         = mrcPrefs.CurrencyCode;
            }
            else
                throw new Exception("Tax settings not received");

            // SALES PEOPLE
            //-----------------------------------------------------------------
            CRParams.consultants = getParams_SalesConsultant(pem, pUserId, pAccId, bResetCache);

            return CRParams;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoSalesConsultant> getParams_SalesConsultant(  EntityManager       pem,
                                                                            BigInteger          pUserId,
                                                                            BigInteger          pAccId,
                                                                            boolean             pbResetCache) throws Exception
    {
        ArrayList<ssoSalesConsultant> salesPersonnel = new ArrayList<ssoSalesConsultant>();

        try
        {
            if(pbResetCache==true)
            {
                pem.flush();
            }
            
            Query stmt = pem.createNamedQuery("SsUsrPersonnel.getSalesPeople", SsUsrPersonnel.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");

            // CASHIER MENU 
            //-----------------------------------------------------------------
            List<List<RowColumn>> rows = stmt.getResultList();
            for (List<RowColumn> rowN: rows)
            {
                ssoSalesConsultant consultantN = new ssoSalesConsultant();

                consultantN.Id   = Util.Database.getValString(rowN, "UID");
                consultantN.name = Util.Database.getValString(rowN, "PROFILENAME");

                salesPersonnel.add(consultantN);
            }

            return salesPersonnel;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoCashierPOSMenuItem> getParams_MenuItems( EntityManager pem,
                                                                        BigInteger          pUserId,
                                                                        BigInteger          pAccId,
                                                                        boolean             pbResetCache) throws Exception
    {
        ArrayList<ssoCashierPOSMenuItem> menuItems = new ArrayList<ssoCashierPOSMenuItem>();


        try
        {
            if(pbResetCache==true)
            {
                pem.flush();
            }

            Query stmt = pem.createNamedQuery("SsCshMenu.getCashierMenu", SsCshMenu.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");

            // CASHIER MENU
            //-----------------------------------------------------------------
            List<SsCshMenu> rows = stmt.getResultList(SsCshMenu.class);
            for (SsCshMenu rowN: rows)
            {
                ssoCashierPOSMenuItem menuItemN = new ssoCashierPOSMenuItem();

                if (rowN.onMenu.equals("Y")==true)
                {
                    if (rowN.vendorId==46422637)
                    {
                        index = index;
                    }

                    menuItemN.vendorId    = Long.toString(rowN.vendorId);
                    menuItemN.vendorName  = rowN.vendorName;
                    menuItemN.category    = rowN.prmCategoryName;
                    menuItemN.itemCode    = rowN.itemCode;
                    menuItemN.itemCodeId  = Long.toString(rowN.itemCodeId);
                    menuItemN.options     = rowN.optionsSummary;
                    menuItemN.quantity    = rowN.quantitySummary.toString();
                    menuItemN.icon        = rowN.onMenuIcon;
                    menuItemN.imgURL      = rowN.onMenuImgUrl;
                    menuItemN.referenceId = rowN.itemHashMd5;
                    menuItemN.priceEntry  = rowN.itemEntryPrice.toString();
                    menuItemN.price       = rowN.itemPrice.toString();
                    menuItemN.currency    = "TL";
                    menuItemN.currencyCode = "TRY";

                    //SIGNATURES CLOSED AS IT TAKES TOO LONG
                    menuItemN.cSign   = "";//calcContentSignature(pAccId, menuItemN.vendorId, menuItemN.itemCodeId, menuItemN.referenceId, menuItemN.price);
                    menuItemN.vSign   = "";//calcVisualSignature(rowN.vendorName, rowN.itemCode, menuItemN.price);
                    menuItems.add(menuItemN);

                }
            }

            return menuItems;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // integrity of the elements sent to online 
    // verified on server side 
    public static String calcContentSignature(long      pAccId, 
                                              String    pVendorId,
                                              String    pItemCodeId,
                                              String    pItemHashMd5,//referenceId
                                              String    pItemPrice
                                             )
    {
        String sHashData = pAccId + "-" + pVendorId.trim() + "-" + pItemCodeId.trim() + "-" + pItemHashMd5.trim() + "-" + pItemPrice.trim();
        return Util.crypto.sha256.calculateSHA256(sHashData);
    }

    // integrity of the elements displayed on screen
    // this is verified on client side as well 
    public static String calcVisualSignature(String pVendorName,
                                             String pItemCode,
                                             String pPrice
                                             )
    {
        String sHashData = pVendorName.trim() + pItemCode.trim() + pPrice.trim();
        return Util.crypto.sha256.calculateSHA256(sHashData);
    }

    public static void add2Category(ArrayList<String> paCategories, String pNewCategory)
    {
        boolean bAdd = true;
        for(String ctgN: paCategories)
        {
            if (ctgN.toLowerCase().trim().equals(pNewCategory.toLowerCase().trim())==true)
            {
                bAdd = false;
            }
        }

        if (bAdd==true)
        {
            paCategories.add(Util.Str.wordNormalize(pNewCategory));
        }
    }
    
    public static void addNewBatch( EntityManager       pem, 
                                    BigInteger          pUserId,
                                    BigInteger          pAccId,
                                    String              pBrowserId,
                                    int                 pBatchNo,
                                    int                 pStan) throws Exception
    {
        try
        {
            SsCshTerminalBatches newBatch = new SsCshTerminalBatches();
            
            newBatch.accountId = pAccId;
            newBatch.browserId = pBrowserId;
            newBatch.batchNo   = pBatchNo;
            newBatch.stans     = "[\"" + pStan + "\"]";
            
            pem.persist(newBatch);
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static void addNewStan(  EntityManager       pem, 
                                    BigInteger          pUserId,
                                    BigInteger          pAccId,
                                    String              pBrowserId,
                                    int                 pBatchNo,
                                    int                 pStan) throws Exception
    {
        try
        {
            Query stmt = pem.createNamedQuery("SsCshTerminalBatches.updateStans", SsCshTerminalBatches.class);
            int index = 1;
            stmt.SetParameter(index++, Integer.toString(pStan)      , "STAN");//CUSTNAME 2
            stmt.SetParameter(index++, pAccId                       , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrowserId                   , "BROWSER_ID");
            stmt.SetParameter(index++, pBatchNo                     , "BATCH_NO");//CUSTNAME 1
            stmt.SetParameter(index++, Integer.toString(pStan)      , "STAN");//CUSTNAME 1

            stmt.executeUpdate();
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // A transaction under the same batch 
    // with the same stan number from the same browser 
    // can't be accepted more than once 
    // false = No repeat
    public static ssoBatchCheckResult isTransactionRepeat(  EntityManager       pem, 
                                                            BigInteger          pUserId,
                                                            BigInteger          pAccId,
                                                            String              pBrowserId,
                                                            int                 pBatchNo,
                                                            int                 pStan) throws Exception
    {
        ssoBatchCheckResult result = new ssoBatchCheckResult();

        try
        {
            Query stmt = pem.createNamedQuery("SsCshTerminalBatches.stanExists", SsCshTerminalBatches.class);
            int index = 1;
            stmt.SetParameter(index++, Integer.toString(pStan)  , "STAN");//CUSTNAME 2
            stmt.SetParameter(index++, pAccId                   , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrowserId               , "BROWSER_ID");
            stmt.SetParameter(index++, pBatchNo                 , "BATCH_NO");//CUSTNAME 1

            // CASHIER MENU
            //-----------------------------------------------------------------
            //boolean bNewBatch = true;
            result.newBatch = true;
            List<List<RowColumn>> rows = stmt.getResultList();
            for (List<RowColumn> rowN: rows)
            {
                result.newBatch = false;
                
                String sSTAN = Util.Database.getValString(rowN, "STAN").trim();
                String sSTAN2Check = Integer.toString(pStan);
                if(sSTAN.trim().equals(sSTAN2Check.trim())==true)
                {
                    // STAN found 
                    result.stanFound = true;
                }
                else
                {
                    result.stanFound = false;
                }
            }
            
            return result;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static BigInteger saveSaleTransaction( EntityManager pem, 
                                            BigInteger          pUserId,
                                            BigInteger          pAccId,
                                            String              pBrowserId,
                                            String              pjsOrders,
                                            String              pjsTotals,
                                            String              pjsPayments,
                                            String              pjsCustomer,
                                            boolean             pbRefund,
                                            long                pTxnDateTime,
                                            int                 pBatchNo,
                                            int                 pStan) throws Exception
    {
        String CASHIER_TXN_TYPE_NEW_SALES  = "S";
        String CASHIER_TXN_TYPE_NEW_REFUND = "R";

        SsTxnSalesCashier cashierTxn = new SsTxnSalesCashier();

        try
        {
            JsonObject jsoTxnTotals   = Util.JSON.toJsonObject(pjsTotals);
            JsonObject jsoTxnPayments = Util.JSON.toJsonObject(pjsPayments);
            JsonObject jsoTxnCustomer = Util.JSON.toJsonObject(pjsCustomer);

            // Totals
            //----------------------------------------------------------------------------
            String sQuantity      = jsoTxnTotals.get("quantity").toString().replace("\"", "");
            String sGrossTotal    = jsoTxnTotals.get("grossTotal").toString().replace("\"", "");
            String sItemDiscount  = jsoTxnTotals.get("itemDiscount").toString().replace("\"", "");
            String sTotalB4Counter= jsoTxnTotals.get("totalB4Counter").toString().replace("\"", "");
            String sDeskDiscount  = jsoTxnTotals.get("counterDiscount").toString().replace("\"", "");
            String sTaxIn         = jsoTxnTotals.get("taxIn").toString().replace("\"", "");
            String sTaxRate       = jsoTxnTotals.get("taxRate").toString().replace("\"", "");
            String sTax           = jsoTxnTotals.get("tax").toString().replace("\"", "");
            

            // Payments
            //----------------------------------------------------------------------------
            String sPayCash   = jsoTxnPayments.get("csh").toString().replace("\"", "");
            String sPayCard   = jsoTxnPayments.get("crd").toString().replace("\"", "");
            String sPayWire   = jsoTxnPayments.get("wre").toString().replace("\"", "");
            String sPayIns    = jsoTxnPayments.get("ins").toString().replace("\"", "");//installment
            String sPayAtDoor = jsoTxnPayments.get("dlv").toString().replace("\"", "");//pay at door

            // Customer
            //----------------------------------------------------------------------------
            String sCustomerName    = "";
            String sPhoneNumber     = "";
            String sCustomerId      = "";
            String sNotes           = "";
            String sSalesPersonnelId= "-1";
            if(pjsCustomer.trim().length()>0)
            {
                sCustomerName    = jsoTxnCustomer.get("cst").toString().replace("\"", "");
                sPhoneNumber     = jsoTxnCustomer.get("phn").toString().replace("\"", "");
                sCustomerId      = jsoTxnCustomer.get("cid").toString().replace("\"", "");
                sNotes           = jsoTxnCustomer.get("nt").toString().replace("\"", "");
                sSalesPersonnelId= jsoTxnCustomer.get("sp").toString().replace("\"", "");
            }
            
            cashierTxn.accountId            = pAccId;
            cashierTxn.browserId            = pBrowserId;
            if(pbRefund==true)
            {
                cashierTxn.cashierTxnType       = CASHIER_TXN_TYPE_NEW_REFUND;
                cashierTxn.txnCode              = txnDefs.TXN_CODE_SALE_REFUND;
                cashierTxn.txnEffect            = "C";//CREDIT = we gave money
            }
            else
            {
                cashierTxn.cashierTxnType       = CASHIER_TXN_TYPE_NEW_SALES;
                cashierTxn.txnCode              = txnDefs.TXN_CODE_SALE_PURCHASE;
                cashierTxn.txnEffect            = "D";//DEBIT = we take money
            }
            //cashierTxn.txnEffect            = txnDefs

            cashierTxn.txnDate              = Util.DateTime.GetDateTime_l("yyyyMMdd").intValue();
            cashierTxn.txnTime              = Integer.parseInt(Util.DateTime.GetDateTime_s("HHmmss"));
            cashierTxn.txnDateTime          = pTxnDateTime;

            cashierTxn.stan                 = pStan;
            cashierTxn.batchNo              = pBatchNo;

            //cart items
            cashierTxn.cartItems            =  pjsOrders;
            //customer
            cashierTxn.custId               = sCustomerId;
            cashierTxn.custName             = sCustomerName;
            cashierTxn.custPhoneCountryCode = "";
            cashierTxn.custPhoneNumber      = sPhoneNumber;
            cashierTxn.note                 = sNotes;
            
            BigInteger biPersonnelId = new BigInteger(sSalesPersonnelId);
            if(biPersonnelId.compareTo(BigInteger.ZERO)<0)
                biPersonnelId = BigInteger.ZERO;

            cashierTxn.salesConsultantId    = biPersonnelId;//for now

            //payments
            cashierTxn.payByCash = new BigDecimal(sPayCash);
            cashierTxn.payByCard = new BigDecimal(sPayCard);
            cashierTxn.payByWire = new BigDecimal(sPayWire);
            cashierTxn.payByRetailIns = new BigDecimal(sPayIns);
            cashierTxn.payByDelivery  = new BigDecimal(sPayAtDoor);

            cashierTxn.totalPayment = cashierTxn.payByCash.add(cashierTxn.payByCard).add(cashierTxn.payByWire);
            //Totals
            cashierTxn.quantity  = new BigDecimal(sQuantity);
            cashierTxn.totalGross  = new BigDecimal(sGrossTotal);

            cashierTxn.discountItems  = new BigDecimal(sItemDiscount);
            cashierTxn.discountCounter   = new BigDecimal(sDeskDiscount);
            cashierTxn.discountTotal  = cashierTxn.discountItems.add(cashierTxn.discountCounter);

            cashierTxn.taxRate   = new BigDecimal(sTaxRate);
            cashierTxn.taxIncluded = sTaxIn;

            if (sTaxIn.trim().equals("Y")==true)//tax included in price
            {
                // total = subtotal - discount
                // tax   = (subtotal - discount) * (100 / 100 + taxrate) -> tax included
                //--------------------------------------------------------------

                // (100 / 100 + taxrate) -> tax included
                BigDecimal taxDraft = new BigDecimal(BigInteger.ZERO);
                if(cashierTxn.taxRate!=new BigDecimal(BigInteger.ZERO))
                    taxDraft = new BigDecimal(100).divide(new BigDecimal(100).add(cashierTxn.taxRate), 4, RoundingMode.HALF_UP);
                else
                    taxDraft = new BigDecimal(0);

                BigDecimal SubtotalMinusTax = new BigDecimal(sTaxRate);

                SubtotalMinusTax     = cashierTxn.totalGross.subtract(cashierTxn.discountTotal).multiply(taxDraft).setScale(2, RoundingMode.HALF_UP);
                cashierTxn.tax       = cashierTxn.totalGross.subtract(cashierTxn.discountTotal).subtract(SubtotalMinusTax);
                cashierTxn.totalNet  = cashierTxn.totalGross.subtract(cashierTxn.discountTotal);
            }
            else
            {
                cashierTxn.tax       = cashierTxn.totalGross.subtract(cashierTxn.discountTotal).multiply(cashierTxn.taxRate);
                cashierTxn.totalNet  = cashierTxn.totalGross.subtract(cashierTxn.discountTotal).add(cashierTxn.tax);
            }

            // This is the amount before counter desk discount applied
            cashierTxn.totalB4CounterDisc = cashierTxn.totalNet.add(cashierTxn.discountCounter);

            BigInteger lTxnUID = pem.persist(cashierTxn);

            return lTxnUID;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoCashRegTxnSummary> getTxnHistory(EntityManager       pem, 
                                                                BigInteger          pUserId,
                                                                BigInteger          pAccId,
                                                                String              pKeyword,
                                                                String              pPageNumber) throws Exception 
    {
        ArrayList<ssoCashRegTxnSummary> txnHistory = new ArrayList<ssoCashRegTxnSummary>();

        try
        {
            int iRowPerPage = 10;//100;//DEFAULT
            int iPageIndex  = Integer.parseInt(pPageNumber);

            ssoDBRowLimits rowLimits = new ssoDBRowLimits();
            rowLimits = Util.Database.calculateRowLimits(iRowPerPage, iPageIndex);
            int iOffset     = rowLimits.offset;
            int iLimit      = rowLimits.limit;

            Query stmt = pem.createNamedQuery("SsEodTxnSales.getTxnSalesHistory", SsEodTxnSales.class);
            int index = 1;
            stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pKeyword        , "CUST_PHONE_NUMBER");
            stmt.SetParameter(index++, pKeyword        , "CUST_NAME");//CUSTNAME 1
            stmt.SetParameter(index++, pKeyword        , "CUST_NAME");//CUSTNAME 2
            stmt.SetParameter(index++, pKeyword        , "CUST_ID");
            stmt.SetParameter(index++, 12              , "LAST_N_MONTHS"); 
            stmt.SetParameter(index++, iLimit          , "LIMIT");
            stmt.SetParameter(index++, iOffset         , "OFFSET");

            // CASHIER MENU
            //-----------------------------------------------------------------
            List<List<RowColumn>> rows = stmt.getResultList();
            for (List<RowColumn> rowN: rows)
            {
                ssoCashRegTxnSummary summaryN = new ssoCashRegTxnSummary();

                summaryN.txnId     =  Util.Database.getValString(rowN, "UID");
                summaryN.stan      =  Util.Database.getValString(rowN, "STAN");
                summaryN.dateTime  =  Util.Database.getValString(rowN, "INSERTDATE");
                summaryN.txnType   =  Util.Database.getValString(rowN, "CASHIER_TXN_TYPE");
                summaryN.cartItems =  Util.Database.getValString(rowN, "CART_ITEMS");
                summaryN.quantity  =  Util.Database.getValString(rowN, "QUANTITY");
                summaryN.subTotal  =  Util.Database.getValString(rowN, "SUBTOTAL");
                summaryN.tax       =  Util.Database.getValString(rowN, "TAX");
                summaryN.total     =  Util.Database.getValString(rowN, "TOTAL");
                summaryN.balance   =  Util.Database.getValString(rowN, "BALANCE");
                summaryN.custName  =  Util.Database.getValString(rowN, "CUST_NAME");
                summaryN.phoneNumber   =  Util.Database.getValString(rowN, "CUST_PHONE_NUMBER");

                summaryN.custId    =  Util.Database.getValString(rowN, "CUST_ID");
                summaryN.custId    = Util.Format.maskDigits(summaryN.custId, summaryN.custId.length() - 4, true, '*');

                summaryN.salesConsultant   =  Util.Database.getValString(rowN, "PROFILENAME");
                summaryN.paidByCash   =  Util.Database.getValString(rowN, "PAY_BY_CASH");
                summaryN.paidByCard   =  Util.Database.getValString(rowN, "PAY_BY_CARD");
                summaryN.paidByWire   = Util.Database.getValString(rowN, "PAY_BY_WIRE");

                txnHistory.add(summaryN);
            }

            return txnHistory;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoEODTxnRow> getTxnHistoryByDate(  EntityManager pem, 
                                                                BigInteger          pUserId,
                                                                BigInteger          pAccId,
                                                                String              pDateYYYYMMDD,
                                                                String              pKeyword,
                                                                String              pPageNumber) throws Exception
    {
        ArrayList<ssoEODTxnRow> aEODTxnRows = new ArrayList<ssoEODTxnRow>();

        try
        {
            int lDateYYYYMMDD          = Integer.parseInt(pDateYYYYMMDD);
            int lDateYYYYMMDD_Today    = (int)Util.DateTime.GetDateTime("yyyyMMdd").intValue();

            int iRowPerPage = 50;//100;//DEFAULT
            int iPageIndex  = Integer.parseInt(pPageNumber);

            ssoDBRowLimits rowLimits = new ssoDBRowLimits();
            rowLimits = Util.Database.calculateRowLimits(iRowPerPage, iPageIndex);
            int iOffset     = rowLimits.offset;
            int iLimit      = rowLimits.limit;

            Query stmt;
            if(lDateYYYYMMDD==lDateYYYYMMDD_Today)
            {
                stmt = pem.createNamedQuery("SsTxnSalesCashier.getTxnsOfToday", SsTxnSalesCashier.class);

                int index = 1;
                stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");//query 1
                stmt.SetParameter(index++, lDateYYYYMMDD   , "TXN_DATE");//query 1
                stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");//query 2
                stmt.SetParameter(index++, lDateYYYYMMDD   , "TXN_DATE");//query 2
                stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");//query 3
                stmt.SetParameter(index++, iLimit          , "LIMIT");
                stmt.SetParameter(index++, iOffset         , "OFFSET");

            }
            else
            {
                stmt = pem.createNamedQuery("SsEodTxnSales.getTxnsByDate", SsEodTxnSales.class);

                int index = 1;
                stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");//query 1
                stmt.SetParameter(index++, lDateYYYYMMDD   , "TXN_DATE");//query 1
                stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");//query 2
                stmt.SetParameter(index++, lDateYYYYMMDD   , "TXN_DATE");//query 2
                stmt.SetParameter(index++, pAccId          , "ACCOUNT_ID");//query 3
                stmt.SetParameter(index++, iLimit          , "LIMIT");
                stmt.SetParameter(index++, iOffset         , "OFFSET");
            }

            String sSearchKey = pKeyword.trim().toLowerCase();
            // CASHIER MENU
            //-----------------------------------------------------------------
            boolean bAdd = true;//default
            List<List<RowColumn>> rows = stmt.getResultList();
            for (List<RowColumn> rowN: rows)
            {
                bAdd = true;//default - reset

                ssoEODTxnRow eodTxnRow = new ssoEODTxnRow();

                boolean bMasterRow = false;
                String sIsMasterRow = Util.Database.getValString(rowN, "MASTER_FLAG");
                if(sIsMasterRow.trim().equals("Y")==true)
                    bMasterRow = true;

                if(bMasterRow==true)
                {
                    eodTxnRow.tableKeys.key       = Util.Database.getValString(rowN, "UID");
                    eodTxnRow.tableKeys.parentKey = "";
                }
                else
                {
                    eodTxnRow.tableKeys.key       = "-";//Util.Database.getValString(rowN, "TXN_DATE") + "-" + Util.Database.getValString(rowN, "TXN_TIME");//"-";//shouldn't be empty
                    eodTxnRow.tableKeys.parentKey = Util.Database.getValString(rowN, "UID");
                }

                eodTxnRow.name          = Util.Database.getValString(rowN, "CUST_NAME");
                eodTxnRow.phoneNumber   = Util.Database.getValString(rowN, "PHONE_NUMBER");

                if( (pKeyword.trim().length()>0) && (eodTxnRow.tableKeys.parentKey.equals("")==true) )
                {
                    int iKeywordIndex = eodTxnRow.name.toLowerCase().trim().indexOf(sSearchKey);// Search thru Customer Name 
                    if(iKeywordIndex<0)
                    {
                        iKeywordIndex = eodTxnRow.phoneNumber.toLowerCase().trim().indexOf(sSearchKey);// Search thru Phone Number
                        if(iKeywordIndex<0)
                        {
                            bAdd=false;
                        }
                    }
                }

                if(bAdd==true)
                {
                    eodTxnRow.Id           = Util.Database.getValString(rowN, "UID");
                    eodTxnRow.stan         = Util.Database.getValString(rowN, "STAN");
                    eodTxnRow.txnCode      = Util.Database.getValString(rowN, "TXN_CODE");
                    eodTxnRow.txnDate      = Util.Database.getValString(rowN, "TXN_DATE");
                    eodTxnRow.txnTime      = Util.Database.getValString(rowN, "TXN_TIME");
                    eodTxnRow.txnName      = Util.Database.getValString(rowN, "TXN_NAME");
                    eodTxnRow.quantity     = Util.Database.getValString(rowN, "QUANTITY");

                    eodTxnRow.consultant   = Util.Database.getValString(rowN, "PERSONNEL_NAME");
                    eodTxnRow.consultantId = Util.Database.getValString(rowN, "PERSONNEL_UID");

                    eodTxnRow.totalGross   = Util.Database.getValString(rowN, "TOTAL_GROSS");
                    eodTxnRow.discount     = Util.Database.getValString(rowN, "DISCOUNT_TOTAL");
                    if(bMasterRow==true)
                        eodTxnRow.discountCounter = Util.Database.getValString(rowN, "DISCOUNT_COUNTER");//desk discount only on final level
                    else
                        eodTxnRow.discountCounter = "0";

                    eodTxnRow.totalFinal   = Util.Database.getValString(rowN, "TOTAL_NET");//TOTAL OF CART
                    eodTxnRow.totalPayment = Util.Database.getValString(rowN, "TOTAL_PAYMENT");

                    eodTxnRow.paidBy       = Util.Database.getValString(rowN, "PAID_BY");
                    eodTxnRow.note         = Util.Database.getValString(rowN, "NOTE");

                    aEODTxnRows.add(eodTxnRow);
                }

            }

            return aEODTxnRows;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
}
