/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.backup;

import bb.app.obj.backup.ssoBackupRow;
import bb.app.obj.backup.ssoInvBackupStatsItm;
import bb.app.obj.backup.ssoInvBackupStatsVnd;
import bb.app.obj.backup.ssoVndBackupSalesSummary;
import entity.acc.SsAccInvItemStats;
import entity.acc.SsAccInvVendorStats;
import entity.eod.SsEodVendorSalesSummary;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.util.Util;

/**
 *
 * @author Administrator
 */
public final class BackupOps 
{

    public static ArrayList<ssoVndBackupSalesSummary> getVendorSalesSummaryData(    EntityManager       pem, 
                                                                                    BigInteger          pAccountId,
                                                                                    BigInteger          pVendorId,
                                                                                    String              pVendor) throws Exception
    {
        ArrayList<ssoVndBackupSalesSummary> allSummary = new ArrayList<>();
        
        try
        {
            allSummary = getSalesSummaryData(pem, pAccountId, pVendorId, pVendor, false);

            return allSummary;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoVndBackupSalesSummary> getSalesSummaryData(  EntityManager pem, 
                                                                            BigInteger    pAccountId,
                                                                            BigInteger    pBrandId,
                                                                            String        pBrand,
                                                                            boolean       pbArchive) throws Exception
    {
        ArrayList<ssoVndBackupSalesSummary> allSummary = new ArrayList<>();

        try
        {
            String sQuery = "SsEodVendorSalesSummary.backup4vendor";
            //if(pbArchive==true)
            //    sQuery = "SsAccInvBrandItemCodes.getItemStatsArchive4Backup";

            Query stmt = pem.createNamedQuery(sQuery, SsEodVendorSalesSummary.class, true);//skip pbSkipClassConversion must be true here
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId            , "VENDOR_ID");

            List<List<RowColumn>> rs = stmt.getResultList(true);
            for(int i=0;i<rs.size();i++)
            {
                ssoVndBackupSalesSummary summaryN = new ssoVndBackupSalesSummary();

                String sVendor     = pBrand;//Util.Database.getValString(rs.get(i), "BRAND");//NAME MUST BE
                String sInsertDate = Util.Database.getValString(rs.get(i), "INSERTDATE");

                // ADDING NEW ROW
                //-----------------------------------------------------------------
                ssoInvBackupStatsItm itemN = new ssoInvBackupStatsItm();

                summaryN.insertdate         = Util.Database.getValString(rs.get(i), "INSERTDATE");
                summaryN.accountId          = Util.Database.getValString(rs.get(i), "ACCOUNT_ID");
                summaryN.vendorId           = Util.Database.getValString(rs.get(i), "VENDOR_ID");
                summaryN.txnDate            = Util.Database.getValString(rs.get(i), "TXN_DATE");
                summaryN.txnYear            = Util.Database.getValString(rs.get(i), "TXN_YEAR");
                summaryN.totalQPurchase     = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY_PURCHASE");
                summaryN.totalQRefund       = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY_REFUND");
                summaryN.totalQNet          = Util.Database.getValString(rs.get(i), "TOTAL_QUANTITY_NET");
                summaryN.totalNetPriceTag   = Util.Database.getValString(rs.get(i), "TOTAL_NET_PRICE_TAG");
                summaryN.totalNetPriceGiven = Util.Database.getValString(rs.get(i), "TOTAL_NET_PRICE_GIVEN");
                summaryN.totalNetPriceSold  = Util.Database.getValString(rs.get(i), "TOTAL_NET_PRICE_SOLD");
                summaryN.summary            = Util.Database.getValString(rs.get(i), "SUMMARY");
                
                allSummary.add(summaryN);
            }

            return allSummary;
        }
        catch(Exception e)
        {
            throw e;
        }
    }


    public static ArrayList<ssoBackupRow> getVendorItemStatsData(   EntityManager       pem, 
                                                                    BigInteger          pAccountId,
                                                                    BigInteger          pVendorId,
                                                                    String              pVendor) throws Exception
    {
        Map<String, ssoInvBackupStatsItm> current = new HashMap<>();//year, vndobj
        Map<String, ssoInvBackupStatsItm> archive = new HashMap<>();//year, vndobj
        ArrayList<ssoBackupRow> all = new ArrayList<>();//year, vndobj

        try
        {
            current = getInventoryStats(pem, pAccountId, pVendorId, pVendor, false);

            archive = getInventoryStats(pem, pAccountId, pVendorId, pVendor, true);

            for (Map.Entry<String, ssoInvBackupStatsItm> entry : current.entrySet())
            {
                ssoBackupRow rowN = new ssoBackupRow();

                rowN.key  = entry.getKey();//year
                rowN.data = entry.getValue();

                all.add(rowN);
            }

            for (Map.Entry<String, ssoInvBackupStatsItm> entry : archive.entrySet())
            {
                ssoBackupRow rowN = new ssoBackupRow();

                rowN.key  = entry.getKey();
                rowN.data = entry.getValue();

                all.add(rowN);
            }

            return all;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static ArrayList<ssoBackupRow> getVendorStatsData(   EntityManager       pem, 
                                                                BigInteger          pAccountId,
                                                                BigInteger          pVendorId,
                                                                String              pVendor) throws Exception
    {
        Map<String, ssoInvBackupStatsVnd> current = new HashMap<>();//year, vndobj
        Map<String, ssoInvBackupStatsVnd> archive = new HashMap<>();//year, vndobj
        ArrayList<ssoBackupRow> all = new ArrayList<>();//year, vndobj

        try
        {
            current = getVendorStats(pem, pAccountId, pVendorId, pVendor, false);

            archive = getVendorStats(pem, pAccountId, pVendorId, pVendor, true);

            for (Map.Entry<String, ssoInvBackupStatsVnd> entry : current.entrySet()) 
            {
                ssoBackupRow rowN = new ssoBackupRow();

                rowN.key  = entry.getKey();//year
                rowN.data = entry.getValue();
                
                all.add(rowN);
            }
            
            for (Map.Entry<String, ssoInvBackupStatsVnd> entry : archive.entrySet()) 
            {
                ssoBackupRow rowN = new ssoBackupRow();

                rowN.key  = entry.getKey();
                rowN.data = entry.getValue();
                
                all.add(rowN);
            }

            return all;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static Map<String, ssoInvBackupStatsItm> getInventoryStats(  EntityManager pem, 
                                                                        BigInteger    pAccountId,
                                                                        BigInteger    pBrandId,
                                                                        String        pBrand,
                                                                        boolean       pbArchive) throws Exception
    {
        Map<String, ssoInvBackupStatsItm> stats = new HashMap<>();

        try
        {
            String sQuery = "SsAccInvBrandItemCodes.getItemStats4Backup";
            if(pbArchive==true)
                sQuery = "SsAccInvBrandItemCodes.getItemStatsArchive4Backup";

            Query stmt = pem.createNamedQuery(sQuery, SsAccInvItemStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId            , "VENDOR_ID");

            List<List<RowColumn>> rs = stmt.getResultList(true);
            for(int i=0;i<rs.size();i++)
            {

                //ssoInvBackupStatsItm itemN = new ssoInvBackupStatsItm();

                String sVendor     = pBrand;//Util.Database.getValString(rs.get(i), "BRAND");//NAME MUST BE
                String sItemCode   = Util.Database.getValString(rs.get(i), "ITEM_CODE");
                String sInsertDate = Util.Database.getValString(rs.get(i), "INSERTDATE");

                // ADDING NEW ROW
                //-----------------------------------------------------------------
                ssoInvBackupStatsItm itemN = new ssoInvBackupStatsItm();

                String sYear = "";
                if(pbArchive==false)
                    sYear = Util.DateTime.getYear().toString();
                else
                    sYear = Util.Database.getValString(rs.get(i), "FINANCIAL_YEAR");

                // itemCode 
                stats.putIfAbsent(sYear, new ssoInvBackupStatsItm());
                itemN = stats.get(sYear);
                itemN.insertdate = sInsertDate;

                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //
                //  INVENTORY STATS - ITEMS 
                // 
                //  Vendor > Item Code > Stats
                //  
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                
                //stats.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "ENTRY_PRICE"));
                itemN.itemCode = sItemCode;
                if(pbArchive==false)
                    itemN.options = Util.Database.getValString(rs.get(i), "OPTION_SUMMARY");

                itemN.revolving.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_ENTERED"));
                itemN.revolving.quantity.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_RETURNED"));
                itemN.revolving.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_CR_SOLD"));
                itemN.revolving.quantity.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_CR_REFUND"));
                itemN.revolving.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_ADJ_PLUS"));
                itemN.revolving.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_ADJ_MINUS"));

                itemN.revolving.base.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_ENTERED"));
                itemN.revolving.base.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_RETURNED"));
                itemN.revolving.base.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_CR_SOLD"));
                itemN.revolving.base.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_CR_REFUND"));
                itemN.revolving.base.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_DISCOUNT_TOTAL"));
                itemN.revolving.base.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_DISCOUNT_TOTAL"));

                itemN.revolving.discount.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_ENTERED"));
                itemN.revolving.discount.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_RETURNED"));
                itemN.revolving.discount.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_CR_SOLD"));
                itemN.revolving.discount.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_CR_REFUND"));
                itemN.revolving.discount.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_DISCOUNT_TOTAL"));
                itemN.revolving.discount.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_DISCOUNT_TOTAL"));

                itemN.revolving.surcharge.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_SURCHARGE_TOTAL_ENTERED"));
                itemN.revolving.surcharge.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_SURCHARGE_TOTAL_RETURNED"));
                itemN.revolving.surcharge.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_SURCHARGE_TOTAL_CR_SOLD"));
                itemN.revolving.surcharge.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_SURCHARGE_TOTAL_CR_REFUND"));
                itemN.revolving.surcharge.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_SURCHARGE_TOTAL"));
                itemN.revolving.surcharge.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_SURCHARGE_TOTAL"));

                itemN.revolving.tax.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_ENTERED"));
                itemN.revolving.tax.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_RETURNED"));
                itemN.revolving.tax.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_CR_SOLD"));
                itemN.revolving.tax.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_CR_REFUND"));
                itemN.revolving.tax.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_TAX_TOTAL"));
                itemN.revolving.tax.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_TAX_TOTAL"));

                itemN.revolving.expense.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_EXPENSE_TOTAL_ENTERED"));
                itemN.revolving.expense.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_EXPENSE_TOTAL_RETURNED"));

                itemN.revolving.gross.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_ENTERED"));
                itemN.revolving.gross.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_RETURNED"));
                itemN.revolving.gross.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_CR_SOLD"));
                itemN.revolving.gross.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_CR_REFUND"));
                itemN.revolving.gross.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_GROSS_TOTAL"));
                itemN.revolving.gross.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_GROSS_TOTAL"));

                //itemN.revolving.net.balance = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE"));
                //itemN.revolving.net.balanceManualOverwrite = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE_MNL"));

                itemN.eod.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ENTERED"));
                itemN.eod.quantity.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_RETURNED"));
                itemN.eod.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_CR_SOLD"));
                itemN.eod.quantity.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_CR_REFUND"));
                itemN.eod.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ADJ_PLUS"));
                itemN.eod.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ADJ_MINUS"));

                itemN.eod.base.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_ENTERED"));
                itemN.eod.base.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_RETURNED"));
                itemN.eod.base.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_CR_SOLD"));
                itemN.eod.base.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_CR_REFUND"));
                itemN.eod.base.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_RAW_TOTAL"));
                itemN.eod.base.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_RAW_TOTAL"));

                itemN.eod.discount.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_ENTERED"));
                itemN.eod.discount.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_RETURNED"));
                itemN.eod.discount.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_CR_SOLD"));
                itemN.eod.discount.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_CR_REFUND"));
                itemN.eod.discount.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_DISCOUNT_TOTAL"));
                itemN.eod.discount.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_DISCOUNT_TOTAL"));

                itemN.eod.surcharge.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_SURCHARGE_TOTAL_ENTERED"));
                itemN.eod.surcharge.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_SURCHARGE_TOTAL_RETURNED"));
                itemN.eod.surcharge.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_SURCHARGE_TOTAL_CR_SOLD"));
                itemN.eod.surcharge.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_SURCHARGE_TOTAL_CR_REFUND"));
                itemN.eod.surcharge.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_SURCHARGE_TOTAL"));
                itemN.eod.surcharge.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_SURCHARGE_TOTAL"));

                itemN.eod.tax.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_ENTERED"));
                itemN.eod.tax.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_RETURNED"));
                itemN.eod.tax.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_CR_SOLD"));
                itemN.eod.tax.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_CR_REFUND"));
                itemN.eod.tax.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_TAX_TOTAL"));
                itemN.eod.tax.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_TAX_TOTAL"));

                itemN.eod.expense.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_ENTERED"));
                itemN.eod.expense.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_RETURNED"));
                //itemN.eod.expense.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_CR_SOLD"));
                //itemN.eod.expense.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_CR_REFUND"));
                itemN.eod.expense.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_EXPENSE_TOTAL"));
                itemN.eod.expense.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_EXPENSE_TOTAL"));

                itemN.eod.gross.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_ENTERED"));
                itemN.eod.gross.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_RETURNED"));
                itemN.eod.gross.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_CR_SOLD"));
                itemN.eod.gross.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_CR_REFUND"));
                itemN.eod.gross.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_GROSS_TOTAL"));
                itemN.eod.gross.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_GROSS_TOTAL"));

                itemN.summary.QSales        = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_SALES");
                itemN.summary.QBills        = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_BILLS");

                itemN.summary.SeasonSales   = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_SALES");
                itemN.summary.SeasonBills   = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_BILLS");

                itemN.category        = Util.Database.getValString(rs.get(i), "CATEGORY");
                itemN.lastEntryPrice  = Util.Database.getValString(rs.get(i), "LAST_ENTRY_PRICE");
                itemN.lastSalePrice   = Util.Database.getValString(rs.get(i), "LAST_SALE_PRICE");

                itemN.sellCounter   = Util.Database.getValString(rs.get(i), "SPD_N_DAY_SELL_COUNTER");
                itemN.sellThru      = Util.Database.getValString(rs.get(i), "SPD_N_DAY_STR");//STR = Sell Thru Rate
                itemN.sellStar      = Util.Database.getValString(rs.get(i), "SPD_STAR");

                itemN.onMenu        = Util.Database.getValString(rs.get(i), "ON_MENU");
                itemN.onMenuImgUrl  = Util.Database.getValString(rs.get(i), "ON_MENU_IMG_URL");//STR = Sell Thru Rate
                itemN.onMenuIcon    = Util.Database.getValString(rs.get(i), "ON_MENU_ICON");

            }

            return stats;
        }
        catch(Exception e)
        {
            throw e;
        }
    }


    // 
    public static Map<String, ssoInvBackupStatsVnd> getVendorStats( EntityManager       pem,
                                                                    BigInteger          pAccountId,
                                                                    BigInteger          pBrandId,
                                                                    String              pBrand,
                                                                    boolean             pbArchive) throws Exception
    {
        //Map<String, ssoInvState> stats = new HashMap<>();
        Map<String, ssoInvBackupStatsVnd> vndStats = new HashMap<>();// year , value
        
        try
        {
            String sQuery = "SsAccInvBrands.getVendorStats4Backup";
            if(pbArchive==true)
                sQuery = "SsAccInvBrands.getVendorStatsArchive4Backup";

            Query stmt = pem.createNamedQuery(sQuery, SsAccInvVendorStats.class);
            int index = 1;
            stmt.SetParameter(index++, pAccountId          , "ACCOUNT_ID");
            stmt.SetParameter(index++, pBrandId            , "VENDOR_ID");

            //String sCurrentYear = Util.DateTime.getYear().toString();

            List<List<RowColumn>> rs = stmt.getResultList(true);
            for(int i=0;i<rs.size();i++)
            {
                //ssoInvState eod = new ssoInvState();
                //ssoInvState revolvingN = new ssoInvState();
                ssoInvBackupStatsVnd vndN = new ssoInvBackupStatsVnd();
                vndN.vendor = pBrand;
                String sYear = Util.Database.getValString(rs.get(i), "FINANCIAL_YEAR");
                String sInsertDate = Util.Database.getValString(rs.get(i), "INSERTDATE");

                // ADDING NEW ROW 
                //-----------------------------------------------------------------

                vndStats.putIfAbsent(sYear, new ssoInvBackupStatsVnd());
                vndN = vndStats.get(sYear);
                vndN.insertdate = sInsertDate;

                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //
                //  VENDOR STATS
                //  
                //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                vndN.revolving.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_ENTERED"));
                vndN.revolving.quantity.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_RETURNED"));
                vndN.revolving.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_CR_SOLD"));
                vndN.revolving.quantity.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_CR_REFUND"));
                vndN.revolving.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_ADJ_PLUS"));
                vndN.revolving.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_QUANTITY_ADJ_MINUS"));

                vndN.revolving.base.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_ENTERED"));
                vndN.revolving.base.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_RETURNED"));
                vndN.revolving.base.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_CR_SOLD"));
                vndN.revolving.base.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_RAW_TOTAL_CR_REFUND"));
                vndN.revolving.base.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_RAW_TOTAL"));
                vndN.revolving.base.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_RAW_TOTAL"));

                vndN.revolving.discount.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_ENTERED"));
                vndN.revolving.discount.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_RETURNED"));
                vndN.revolving.discount.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_CR_SOLD"));
                vndN.revolving.discount.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_DISCOUNT_TOTAL_CR_REFUND"));
                vndN.revolving.discount.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_DISCOUNT_TOTAL"));
                vndN.revolving.discount.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_DISCOUNT_TOTAL"));

                vndN.revolving.tax.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_ENTERED"));
                vndN.revolving.tax.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_RETURNED"));
                vndN.revolving.tax.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_CR_SOLD"));
                vndN.revolving.tax.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_TAX_TOTAL_CR_REFUND"));
                vndN.revolving.tax.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_TAX_TOTAL"));
                vndN.revolving.tax.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_TAX_TOTAL"));

                vndN.revolving.expense.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_EXPENSE_TOTAL_ENTERED"));
                vndN.revolving.expense.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_EXPENSE_TOTAL_RETURNED"));
                vndN.revolving.expense.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_EXPENSE_TOTAL_CR_SOLD"));
                vndN.revolving.expense.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_EXPENSE_TOTAL_CR_REFUND"));
                vndN.revolving.expense.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_EXPENSE_TOTAL"));
                vndN.revolving.expense.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_EXPENSE_TOTAL"));

                vndN.revolving.gross.received = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_ENTERED"));
                vndN.revolving.gross.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_RETURNED"));
                vndN.revolving.gross.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_CR_SOLD"));
                vndN.revolving.gross.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_GROSS_TOTAL_CR_REFUND"));
                vndN.revolving.gross.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_PLUS_GROSS_TOTAL"));
                vndN.revolving.gross.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_FIN_ADJ_MINUS_GROSS_TOTAL"));

                vndN.revolving.net.balance                = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE"));
                vndN.revolving.net.balanceManualOverwrite = new BigDecimal(Util.Database.getValString(rs.get(i), "REVOLVING_BALANCE_MNL"));

                vndN.eod.quantity.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ENTERED"));
                vndN.eod.quantity.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_RETURNED"));
                vndN.eod.quantity.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_CR_SOLD"));
                vndN.eod.quantity.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_CR_REFUND"));
                vndN.eod.quantity.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ADJ_PLUS"));
                vndN.eod.quantity.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_QUANTITY_ADJ_MINUS"));

                vndN.eod.base.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_ENTERED"));
                vndN.eod.base.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_RETURNED"));
                vndN.eod.base.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_CR_SOLD"));
                vndN.eod.base.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_RAW_TOTAL_CR_REFUND"));
                vndN.eod.base.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_RAW_TOTAL"));
                vndN.eod.base.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_RAW_TOTAL"));

                vndN.eod.discount.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_ENTERED"));
                vndN.eod.discount.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_RETURNED"));
                vndN.eod.discount.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_CR_SOLD"));
                vndN.eod.discount.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_DISCOUNT_TOTAL_CR_REFUND"));
                vndN.eod.discount.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_DISCOUNT_TOTAL"));
                vndN.eod.discount.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_DISCOUNT_TOTAL"));

                vndN.eod.tax.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_ENTERED"));
                vndN.eod.tax.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_RETURNED"));
                vndN.eod.tax.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_CR_SOLD"));
                vndN.eod.tax.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_TAX_TOTAL_CR_REFUND"));
                vndN.eod.tax.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_TAX_TOTAL"));
                vndN.eod.tax.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_TAX_TOTAL"));

                vndN.eod.expense.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_ENTERED"));
                vndN.eod.expense.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_RETURNED"));
                vndN.eod.expense.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_CR_SOLD"));
                vndN.eod.expense.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_EXPENSE_TOTAL_CR_REFUND"));
                vndN.eod.expense.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_EXPENSE_TOTAL"));
                vndN.eod.expense.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_EXPENSE_TOTAL"));

                vndN.eod.gross.received = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_ENTERED"));
                vndN.eod.gross.sent     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_RETURNED"));
                vndN.eod.gross.sold     = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_CR_SOLD"));
                vndN.eod.gross.refund   = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_GROSS_TOTAL_CR_REFUND"));
                vndN.eod.gross.adjPlus  = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_PLUS_GROSS_TOTAL"));
                vndN.eod.gross.adjMinus = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_FIN_ADJ_MINUS_GROSS_TOTAL"));

                vndN.eod.net.balance                = new BigDecimal(Util.Database.getValString(rs.get(i), "LAST_EOD_BALANCE"));

                vndN.summary.QSales        = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_SALES");
                vndN.summary.QBills        = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_BILLS");
                vndN.summary.QPayment      = Util.Database.getValString(rs.get(i), "QUARTER_SUMMARY_PAYMENTS");

                vndN.summary.SeasonSales   = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_SALES");
                vndN.summary.SeasonBills   = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_BILLS");
                vndN.summary.SeasonPayment = Util.Database.getValString(rs.get(i), "SEASON_SUMMARY_PAYMENTS");

                vndN.summary.CategorySummary      = Util.Database.getValString(rs.get(i), "CATEGORY_SUMMARY");
            }

            return vndStats;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

}


