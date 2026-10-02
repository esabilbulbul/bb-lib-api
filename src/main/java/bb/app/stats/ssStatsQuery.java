/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.stats;

import bb.app.global.ssoAppGlobal;
import bb.app.txn.txnDefs;

/**
 *
 * @author Administrator
 */
public class ssStatsQuery 
{
    // OPTS(?) = INV_JSON_MERGE('{"": 14.0}', //CURRENT
    //                          '{"L": [18,1], "M": 2, "2XL": -1}', // NEW ENTERED
    //                          '+');
    public static String generateUpdateOptionStatsQuery4CashRegister(boolean pbTxnReturn)
    {
        String sQuery = "UPDATE ss_acc_inv_option_stats ";

        sQuery +=  "SET ";
        if (pbTxnReturn==false)//entered
        {
            sQuery +=  "OPTIONS_CR_SOLD  = INV_JSON_MERGE(IFNULL(OPTIONS_CR_SOLD,'{}'), ?, '+') ";
        }
        else
        {
            sQuery +=  "OPTIONS_CR_REFUND = INV_JSON_MERGE(IFNULL(OPTIONS_CR_REFUND,'{}'), ?, '+') ";
        }

        sQuery +=  "WHERE ";
        sQuery +=  "STAT = 1 ";
        sQuery +=  "AND ";
        sQuery +=  "ACCOUNT_ID = ? ";
        sQuery +=  "AND ";
        sQuery +=  "VENDOR_ID = ? ";
        sQuery +=  "AND ";
        sQuery +=  "ITEM_CODE_ID = ? ";
        sQuery +=  "AND ";
        sQuery +=  "OPTION_GROUP = ? ";

        return sQuery;
    }

    /// NOT RETURN REFUND IS DIFFERENT TXN 
    public static String generateUpdateVendorStatsQuery4CashRegister(boolean pbTxnRefund)
    {
        String sQuery = "UPDATE ss_acc_inv_vendor_stats SET ";

        if(pbTxnRefund==false)
        {
            //init
            sQuery += " QUANTITY_CR_SOLD        = QUANTITY_CR_SOLD + ? , ";
            sQuery += " GROSS_TOTAL_CR_SOLD     = GROSS_TOTAL_CR_SOLD + ? , ";
            sQuery += " DISCOUNT_TOTAL_CR_SOLD  = DISCOUNT_TOTAL_CR_SOLD + ? , ";
            //sQuery += " SURCHARGE_TOTAL_ENTERED = SURCHARGE_TOTAL_ENTERED + ? , ";
            sQuery += " TAX_TOTAL_CR_SOLD       = TAX_TOTAL_CR_SOLD + ? , ";
            sQuery += " RAW_TOTAL_CR_SOLD       = RAW_TOTAL_CR_SOLD + ?  ";
        }
        else
        {
            sQuery += " QUANTITY_CR_REFUND        = QUANTITY_CR_REFUND + ? , ";
            sQuery += " GROSS_TOTAL_CR_REFUND     = GROSS_TOTAL_CR_REFUND + ? , ";
            sQuery += " DISCOUNT_TOTAL_CR_REFUND  = DISCOUNT_TOTAL_CR_REFUND + ? , ";
            //sQuery += " SURCHARGE_TOTAL_CR_REFUND = SURCHARGE_TOTAL_CR_REFUND + ? , ";
            sQuery += " TAX_TOTAL_CR_REFUND       = TAX_TOTAL_CR_REFUND + ? , ";
            sQuery += " RAW_TOTAL_CR_REFUND       = RAW_TOTAL_CR_REFUND + ? ";
        }

        sQuery += " WHERE ";
        sQuery += "  STAT = 1 ";
        sQuery += "  AND ";
        sQuery += "  ACCOUNT_ID = ? ";
        sQuery += "  AND ";
        sQuery += "  VENDOR_ID = ? ";

        return sQuery;
    }

    /// NOT RETURN REFUND IS DIFFERENT TXN 
    //  ITEM STATS
    //--------------------------------------------------------------------------
    public static String generateUpdateItemStatsQuery4CashRegister(boolean pbTxnRefund)
    {
        String sQuery = "UPDATE ss_acc_inv_item_stats SET ";

        if(pbTxnRefund==false)
        {
            //init
            sQuery += " QUANTITY_CR_SOLD        = QUANTITY_CR_SOLD + ? , ";
            sQuery += " GROSS_TOTAL_CR_SOLD     = GROSS_TOTAL_CR_SOLD + ? , ";
            sQuery += " DISCOUNT_TOTAL_CR_SOLD  = DISCOUNT_TOTAL_CR_SOLD + ? , ";
            //sQuery += " SURCHARGE_TOTAL_ENTERED = SURCHARGE_TOTAL_ENTERED + ? , ";
            sQuery += " TAX_TOTAL_CR_SOLD       = TAX_TOTAL_CR_SOLD + ? , ";
            sQuery += " RAW_TOTAL_CR_SOLD       = RAW_TOTAL_CR_SOLD + ?  ";
        }
        else
        {
            sQuery += " QUANTITY_CR_REFUND        = QUANTITY_CR_REFUND + ? , ";
            sQuery += " GROSS_TOTAL_CR_REFUND     = GROSS_TOTAL_CR_REFUND + ? , ";
            sQuery += " DISCOUNT_TOTAL_CR_REFUND  = DISCOUNT_TOTAL_CR_REFUND + ? , ";
            //sQuery += " SURCHARGE_TOTAL_CR_REFUND = SURCHARGE_TOTAL_CR_REFUND + ? , ";
            sQuery += " TAX_TOTAL_CR_REFUND       = TAX_TOTAL_CR_REFUND + ? , ";
            sQuery += " RAW_TOTAL_CR_REFUND       = RAW_TOTAL_CR_REFUND + ? ";
        }

        sQuery += " WHERE ";
        sQuery += "  STAT = 1 ";
        sQuery += "  AND ";
        sQuery += "  ACCOUNT_ID = ? ";
        sQuery += "  AND ";
        sQuery += "  VENDOR_ID = ? ";
        sQuery += "  AND ";
        sQuery += "  ITEM_CODE = ? ";

        return sQuery;
    }

    
    
    // OPTS(?) = INV_JSON_MERGE('{"": 14.0}', //CURRENT
    //                          '{"L": 18, "M": 2, "2XL": -1}', // NEW ENTERED
    //                          '+');
    public static String generateUpdateOptionStatsQuery4Update(boolean pbTxnReturn)
    {
        String sQuery = "UPDATE ss_acc_inv_option_stats ";

        sQuery +=  "SET ";
        if (pbTxnReturn==false)//entered
        {
            sQuery +=  "OPTIONS_ENTERED  = INV_JSON_MERGE(IFNULL(OPTIONS_ENTERED,'{}'), ?, '+') ";
        }
        else
        {
            sQuery +=  "OPTIONS_RETURNED = INV_JSON_MERGE(IFNULL(OPTIONS_RETURNED,'{}'), ?, '+')";
        }

        sQuery +=  "WHERE ";
        sQuery +=  "STAT = 1 ";
        sQuery +=  "AND ";
        sQuery +=  "ACCOUNT_ID = ? ";
        sQuery +=  "AND ";
        sQuery +=  "VENDOR_ID = ? ";
        sQuery +=  "AND ";
        sQuery +=  "ITEM_CODE_ID = ? ";
        sQuery +=  "AND ";
        sQuery +=  "OPTION_GROUP = ? ";

        return sQuery;
    }

    public static String generateUpdateOptionStatsQuery4New(boolean pbTxnReturn)
    {
        String sQuery = "INSERT INTO ss_acc_inv_option_stats ";
        
        sQuery +=  "(";
        
        sQuery +=  "UID, ";
        sQuery +=  "SERVER_ID, ";
        sQuery +=  "STAT, ";
        sQuery +=  "INSERTDATE, ";
        sQuery +=  "LASTUPDATE, ";
        sQuery +=  "VERSION, ";
        sQuery +=  "CLIENT_IP, ";
        sQuery +=  "CLIENT_DTIME, ";
        sQuery +=  "BYUSER, ";
        sQuery +=  "SYS_GMT, ";

        sQuery +=  "OPTION_GROUP, ";
        sQuery +=  "FINANCIAL_YEAR, ";

        sQuery +=  "OPTIONS_ENTERED, ";
        sQuery +=  "OPTIONS_RETURNED, ";
        sQuery +=  "OPTIONS_CR_SOLD, ";
        sQuery +=  "OPTIONS_CR_REFUND, ";
        sQuery +=  "OPTIONS_ADJ_PLUS, ";
        sQuery +=  "OPTIONS_ADJ_MINUS, ";

        sQuery +=  "LAST_EOD_OPTIONS_ENTERED, ";
        sQuery +=  "LAST_EOD_OPTIONS_RETURNED, ";
        sQuery +=  "LAST_EOD_OPTIONS_CR_SOLD, ";
        sQuery +=  "LAST_EOD_OPTIONS_CR_REFUND, ";
        sQuery +=  "LAST_EOD_OPTIONS_ADJ_PLUS, ";
        sQuery +=  "LAST_EOD_OPTIONS_ADJ_MINUS, ";

        sQuery +=  "OPTIONS_REVOLVING_ENTERED, ";
        sQuery +=  "OPTIONS_REVOLVING_RETURNED, ";
        sQuery +=  "OPTIONS_REVOLVING_CR_SOLD, ";
        sQuery +=  "OPTIONS_REVOLVING_CR_REFUND, ";
        sQuery +=  "OPTIONS_REVOLVING_ADJ_PLUS, ";
        sQuery +=  "OPTIONS_REVOLVING_ADJ_MINUS, ";

        sQuery +=  "ACCOUNT_ID, ";
        sQuery +=  "VENDOR_ID, ";
        sQuery +=  "ITEM_CODE_ID, ";
        sQuery +=  "PRM_CATEGORY_ID ";

        sQuery +=  ") ";
        
        sQuery +=  "VALUES ";
        
        sQuery +=  "( ";
        sQuery += "NextVal_fn('" + ssoAppGlobal.DEFAULT_SEQUENCE_NAME + "'), ";
        sQuery += "0, ";//SERVER ID
        sQuery += "1, ";//STAT
        sQuery += "CAST(DATE_FORMAT(NOW(), \"%Y%m%d%H%i%s000\") AS UNSIGNED INTEGER), ";//INSERT DATE TIME
        sQuery += "0, ";//last update
        sQuery += "0, ";//version
        sQuery += "'0', ";//client Ip
        sQuery += "'0', ";//client datetime
        sQuery += "'callback-1st', ";//by user
        sQuery += "0, ";//sys gmt

        sQuery += "?, ";//option_group
        sQuery += "YEAR(CURDATE()), ";

        if (pbTxnReturn==false)//entered
        {
            sQuery +=  "?, ";//OPTIONS_ENTERED
            sQuery +=  "'{}', ";//OPTIONS_RETURNED
        }
        else
        {
            sQuery +=  "'{}', ";//OPTIONS_ENTERED
            sQuery +=  "?, ";//OPTIONS_RETURNED
        }
        
        sQuery +=  "'{}', ";//OPTIONS_CR_SOLD
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";//OPTIONS_ADJ_MINUS

        sQuery +=  "'{}', ";//LAST_EOD_OPTIONS_ENTERED
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";//LAST_EOD_OPTIONS_ADJ_MINUS

        sQuery +=  "'{}', ";//OPTIONS_REVOLVING_ENTERED
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";
        sQuery +=  "'{}', ";//OPTIONS_REVOLVING_ADJ_MINUS

        sQuery +=  "?, ";//ACC_ID
        sQuery +=  "?, ";//VENDOR_ID
        sQuery +=  "?, ";//ITEM_CODE_ID
        sQuery +=  "? ";//PRM_CATEGORY_ID
        
        sQuery +=  ")";
        
        return sQuery;
    }

    // Reverse occurs say an bill entered and then the bill line edited. And
    // itemcode has changed. For the old item code reverse record will be added
    // for that previous item code, fin adj record will be added
    public static String generateUpdateItemStatsQuery4Reverse(boolean pbTxnReturn)
    {
        try
        {
            String sQuery = "UPDATE ss_acc_inv_item_stats SET ";

            if (pbTxnReturn==false)
            {
                sQuery += " QUANTITY_ADJ_MINUS           = QUANTITY_ADJ_MINUS + ? , ";
                sQuery += " FIN_ADJ_MINUS_GROSS_TOTAL    = FIN_ADJ_MINUS_GROSS_TOTAL + ? , ";
                sQuery += " FIN_ADJ_MINUS_DISCOUNT_TOTAL = FIN_ADJ_MINUS_DISCOUNT_TOTAL + ? , ";  
                sQuery += " FIN_ADJ_MINUS_SURCHARGE_TOTAL= FIN_ADJ_MINUS_SURCHARGE_TOTAL + ? , ";
                sQuery += " FIN_ADJ_MINUS_TAX_TOTAL      = FIN_ADJ_MINUS_TAX_TOTAL + ? , ";
                sQuery += " FIN_ADJ_MINUS_RAW_TOTAL      = FIN_ADJ_MINUS_RAW_TOTAL + ? , ";
            }
            else
            {
                sQuery += " QUANTITY_ADJ_PLUS           = QUANTITY_ADJ_PLUS + ? , ";
                sQuery += " FIN_ADJ_PLUS_GROSS_TOTAL    = FIN_ADJ_PLUS_GROSS_TOTAL + ? , ";
                sQuery += " FIN_ADJ_PLUS_DISCOUNT_TOTAL = FIN_ADJ_PLUS_DISCOUNT_TOTAL + ? , ";  
                sQuery += " FIN_ADJ_PLUS_SURCHARGE_TOTAL= FIN_ADJ_PLUS_SURCHARGE_TOTAL + ? , ";
                sQuery += " FIN_ADJ_PLUS_TAX_TOTAL      = FIN_ADJ_PLUS_TAX_TOTAL + ? , ";
                sQuery += " FIN_ADJ_PLUS_RAW_TOTAL      = FIN_ADJ_PLUS_RAW_TOTAL + ? , ";
            }

            sQuery += " WHERE ";
            sQuery += "  STAT = 1 ";
            sQuery += "  AND ";
            sQuery += "  ACCOUNT_ID = ? ";
            sQuery += "  AND ";
            sQuery += "  VENDOR_ID = ? ";
            sQuery += "  AND ";
            sQuery += "  ITEM_CODE = ? ";

            return sQuery;
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    // New = Init 1st time entry of the item
    // TxnType = INV-ENTRY, INV-RETURN, INV-EXST, SALE, REFUND
    public static String generateUpdateItemStatsQuery4New(String pTxnType)
    {
       String sQuery = "INSERT INTO ss_acc_inv_item_stats ";

       sQuery += "(";
        
       sQuery +=  "UID, ";
       sQuery +=  "SERVER_ID, ";
       sQuery +=  "STAT, ";
       sQuery +=  "INSERTDATE, ";
       sQuery +=  "LASTUPDATE, ";
       sQuery +=  "VERSION, ";
       sQuery +=  "CLIENT_IP, ";
       sQuery +=  "CLIENT_DTIME, ";
       sQuery +=  "BYUSER, ";
       sQuery +=  "SYS_GMT, ";

       sQuery +=  "PRICE_ID, ";
       sQuery +=  "LAST_ENTRY_PRICE, ";
       sQuery +=  "LAST_SALE_PRICE, ";

       //if(pbTxnReturn==false)//txnDefs.TXN_TYPE_INVENTORY_RECEIVED
       if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_RECEIVED)==true)
       {
            sQuery += "QUANTITY_ENTERED , ";
            sQuery += "RAW_TOTAL_ENTERED, ";
            sQuery += "DISCOUNT_TOTAL_ENTERED, ";  
            sQuery += "SURCHARGE_TOTAL_ENTERED, ";
            sQuery += "TAX_TOTAL_ENTERED, ";
            sQuery += "EXPENSE_TOTAL_ENTERED, ";
            sQuery += "GROSS_TOTAL_ENTERED, ";
            
       }
       else if(pTxnType.equals(txnDefs.TXN_TYPE_INVENTORY_SENT)==true)
       {
            sQuery += "QUANTITY_RETURNED , ";
            sQuery += "RAW_TOTAL_RETURNED, ";
            sQuery += "DISCOUNT_TOTAL_RETURNED, ";  
            sQuery += "SURCHARGE_TOTAL_RETURNED, ";
            sQuery += "TAX_TOTAL_RETURNED, ";
            sQuery += "EXPENSE_TOTAL_RETURNED, ";
            sQuery += "GROSS_TOTAL_RETURNED, ";
            
       }
       else if(pTxnType.equals(txnDefs.TXN_TYPE_SALES)==true)
       {
            sQuery += "QUANTITY_CR_SOLD , ";
            sQuery += "RAW_TOTAL_CR_SOLD, ";
            sQuery += "DISCOUNT_TOTAL_CR_SOLD, ";  
            sQuery += "SURCHARGE_TOTAL_CR_SOLD, ";
            sQuery += "TAX_TOTAL_CR_SOLD, ";
            sQuery += "EXPENSE_TOTAL_CR_SOLD, ";
            sQuery += "GROSS_TOTAL_CR_SOLD, ";

       }
       else if(pTxnType.equals(txnDefs.TXN_TYPE_REFUND)==true)
       {
            sQuery += "QUANTITY_CR_REFUND , ";
            sQuery += "RAW_TOTAL_CR_REFUND, ";
            sQuery += "DISCOUNT_TOTAL_CR_REFUND, ";  
            sQuery += "SURCHARGE_TOTAL_CR_REFUND, ";
            sQuery += "TAX_TOTAL_CR_REFUND, ";
            sQuery += "EXPENSE_TOTAL_CR_REFUND, ";
            sQuery += "GROSS_TOTAL_CR_REFUND, ";

       }

       sQuery +=  "ACCOUNT_ID, ";
       sQuery +=  "VENDOR_ID, ";
       sQuery +=  "ITEM_CODE, ";
       sQuery +=  "PRM_CATEGORY_ID ";
       //sQuery +=  "ITEM_CODE_ID ";

       sQuery += ")";

       sQuery += " VALUES ";

       sQuery += "(";

       sQuery += "NextVal_fn('" + ssoAppGlobal.DEFAULT_SEQUENCE_NAME + "'), ";
       sQuery += "0, ";//SERVER ID
       sQuery += "1, ";//STAT
       sQuery += "CAST(DATE_FORMAT(NOW(), \"%Y%m%d%H%i%s000\") AS UNSIGNED INTEGER), ";//INSERT DATE TIME
       sQuery += "0, ";//last update
       sQuery += "0, ";//version
       sQuery += "'0', ";//client Ip
       sQuery += "'0', ";//client datetime
       sQuery += "'', ";//by user
       sQuery += "0, ";//sys gmt
       
       sQuery += "?, ";//PRICE_ID
       sQuery += "?, ";//LAST_ENTRY_PRICE
       sQuery += "?, ";//LAST_SALE_PRICE

       sQuery += "?, ";//QUANTITY
       sQuery += "?, ";//NET
       sQuery += "?, ";//DISCOUNT
       sQuery += "?, ";//SURCHARGE
       sQuery += "?, ";//TAX
       sQuery += "?, ";//EXPENSE = FIXED TAX
       sQuery += "?, ";//GROSS
       
       sQuery += "?, ";//ACC ID
       sQuery += "?, ";//VENDOR ID
       sQuery += "?, ";//ITEM CODE
       sQuery += "?  ";//CATEGORY ID

       sQuery += ")";

       return sQuery;
    }


    // pbHasEdit = true then the bill entered first then this time coming for the update
    // and edits go to FIN_ADJ
    public static String generateUpdateItemStatsQuery4Change(boolean pbTxnReturn,
                                                             boolean pbHasEdit,
                                                             boolean pbUpdatePriceId,
                                                             boolean pbUpdateCategoryId)
    {

        String sQuery = "UPDATE ss_acc_inv_item_stats SET ";

        if(pbTxnReturn==false)
        {
            if(pbUpdatePriceId==true)
            {
                sQuery += " PRICE_ID            = ? ,";
                sQuery += " LAST_SALE_PRICE     = ? , ";//this is extra that RETURN_TXN doesn't have nor BILL UPDATE TXN
                sQuery += " LAST_ENTRY_PRICE    = ? , ";
            }

        }

        if(pbUpdateCategoryId==true)
        {
            sQuery += " PRM_CATEGORY_ID        = ? ,";
        }

        if(pbTxnReturn==false)
        {
            //init 

            sQuery += " QUANTITY_ENTERED        = QUANTITY_ENTERED + ? , ";
            sQuery += " RAW_TOTAL_ENTERED       = RAW_TOTAL_ENTERED + ?,  ";
            sQuery += " DISCOUNT_TOTAL_ENTERED  = DISCOUNT_TOTAL_ENTERED + ? , ";  
            sQuery += " SURCHARGE_TOTAL_ENTERED = SURCHARGE_TOTAL_ENTERED + ? , ";
            sQuery += " TAX_TOTAL_ENTERED       = TAX_TOTAL_ENTERED + ? , ";
            sQuery += " EXPENSE_TOTAL_ENTERED   = EXPENSE_TOTAL_ENTERED + ? , ";
            sQuery += " GROSS_TOTAL_ENTERED     = GROSS_TOTAL_ENTERED + ? ";

            if(pbHasEdit==true)
            {
                sQuery += " , ";
                // ADJ(-) RECORDS
                sQuery += " QUANTITY_ADJ_MINUS           = QUANTITY_ADJ_MINUS + ? , ";
                sQuery += " FIN_ADJ_MINUS_RAW_TOTAL      = FIN_ADJ_MINUS_RAW_TOTAL + ?, ";
                sQuery += " FIN_ADJ_MINUS_DISCOUNT_TOTAL = FIN_ADJ_MINUS_DISCOUNT_TOTAL + ? , ";  
                sQuery += " FIN_ADJ_MINUS_SURCHARGE_TOTAL= FIN_ADJ_MINUS_SURCHARGE_TOTAL + ? , ";
                sQuery += " FIN_ADJ_MINUS_TAX_TOTAL      = FIN_ADJ_MINUS_TAX_TOTAL + ? , ";
                sQuery += " FIN_ADJ_MINUS_EXPENSE_TOTAL  = FIN_ADJ_MINUS_EXPENSE_TOTAL + ? , ";
                sQuery += " FIN_ADJ_MINUS_GROSS_TOTAL    = FIN_ADJ_MINUS_GROSS_TOTAL + ? ";
            }
        }
        else// return = true
        {
            sQuery += " QUANTITY_RETURNED        = QUANTITY_RETURNED + ? , ";
            sQuery += " RAW_TOTAL_RETURNED       = RAW_TOTAL_RETURNED + ?, ";
            sQuery += " DISCOUNT_TOTAL_RETURNED  = DISCOUNT_TOTAL_RETURNED + ? , ";  
            sQuery += " SURCHARGE_TOTAL_RETURNED = SURCHARGE_TOTAL_RETURNED + ? , ";
            sQuery += " TAX_TOTAL_RETURNED       = TAX_TOTAL_RETURNED + ? , ";
            sQuery += " EXPENSE_TOTAL_RETURNED   = EXPENSE_TOTAL_RETURNED + ? , ";
            sQuery += " GROSS_TOTAL_RETURNED     = GROSS_TOTAL_RETURNED + ? ";

            if(pbHasEdit==true)
            {
                sQuery += " , ";
                // ADJ(+) RECORDS
                sQuery += " QUANTITY_ADJ_PLUS            = QUANTITY_ADJ_PLUS + ? , ";
                sQuery += " FIN_ADJ_PLUS_RAW_TOTAL       = FIN_ADJ_PLUS_RAW_TOTAL + ?, ";
                sQuery += " FIN_ADJ_PLUS_DISCOUNT_TOTAL  = FIN_ADJ_PLUS_DISCOUNT_TOTAL + ? , ";  
                sQuery += " FIN_ADJ_PLUS_SURCHARGE_TOTAL = FIN_ADJ_PLUS_SURCHARGE_TOTAL + ? , ";
                sQuery += " FIN_ADJ_PLUS_TAX_TOTAL       = FIN_ADJ_PLUS_TAX_TOTAL + ? , ";
                sQuery += " FIN_ADJ_PLUS_EXPENSE_TOTAL   = FIN_ADJ_PLUS_EXPENSE_TOTAL + ? , ";
                sQuery += " FIN_ADJ_PLUS_GROSS_TOTAL     = FIN_ADJ_PLUS_GROSS_TOTAL + ?  ";
            }
        }

        sQuery += " WHERE ";
        sQuery += "  STAT = 1 ";
        sQuery += "  AND ";
        sQuery += "  ACCOUNT_ID = ? ";
        sQuery += "  AND ";
        sQuery += "  VENDOR_ID = ? ";
        sQuery += "  AND ";
        sQuery += "  ITEM_CODE = ? ";

        return sQuery;
    }

    public static String generateUpdateItemStatsQuery4ExistingEntry()
    {
        String sQuery = "UPDATE ss_acc_inv_item_stats SET ";

        sQuery += "QUANTITY_ADJ_MINUS = QUANTITY_ADJ_MINUS + ( QUANTITY_ENTERED - QUANTITY_RETURNED - QUANTITY_CR_SOLD + QUANTITY_CR_REFUND ), ";//neturalize the existing value by resetting
        sQuery += "QUANTITY_ENTERED = ?, ";// THIS LINE MUST BE AFTER THE PREVIOUS LINE. / This txn overwrites the value !!! (Not incremental)

        sQuery += " PRICE_ID        = ? ,";
        sQuery += " LAST_SALE_PRICE = ? , ";//this is extra that RETURN_TXN doesn't have nor BILL UPDATE TXN
        sQuery += " PRM_CATEGORY_ID = ? ,";

        sQuery += "LASTUPDATE = CAST(DATE_FORMAT(NOW(), '%y%m%d%H%i%s000') AS UNSIGNED INTEGER) " +
                  "WHERE " +
                  "STAT = 1 " + 
                  "AND " +
                  "ACCOUNT_ID = ? " + 
                  "AND " +
                  "VENDOR_ID = ? "  +
                  "AND " +
                  "ITEM_CODE = ?";
    
        return sQuery;
    }

    public static String generateUpdateItemStatsQuery4ExistingEntry_NewRecord()
    {
        String sQuery = "INSERT INTO ss_acc_inv_item_stats ";

        sQuery += "(";

        sQuery +=  "UID, ";
        sQuery +=  "SERVER_ID, ";
        sQuery +=  "STAT, ";
        sQuery +=  "INSERTDATE, ";
        sQuery +=  "LASTUPDATE, ";
        sQuery +=  "VERSION, ";
        sQuery +=  "CLIENT_IP, ";
        sQuery +=  "CLIENT_DTIME, ";
        sQuery +=  "BYUSER, ";
        sQuery +=  "SYS_GMT, ";

        sQuery +=  "PRICE_ID, ";
        sQuery +=  "LAST_SALE_PRICE, ";
        sQuery +=  "QUANTITY_ENTERED, ";

        sQuery +=  "ACCOUNT_ID, ";
        sQuery +=  "VENDOR_ID, ";
        sQuery +=  "ITEM_CODE ";

        sQuery += ") ";

        sQuery += " VALUES ";
        sQuery += " ( ";

        sQuery += "NextVal_fn('" + ssoAppGlobal.DEFAULT_SEQUENCE_NAME + "'), ";
        sQuery += "0, ";//SERVER ID
        sQuery += "1, ";//STAT
        sQuery += "CAST(DATE_FORMAT(NOW(), \"%Y%m%d%H%i%s000\") AS UNSIGNED INTEGER), ";//INSERT DATE TIME
        sQuery += "0, ";//last update
        sQuery += "0, ";//version
        sQuery += "'0', ";//client Ip
        sQuery += "'0', ";//client datetime
        sQuery += "'', ";//by user
        sQuery += "0, ";//sys gmt
       
        sQuery += "?, ";//PRICE_ID
        sQuery += "?, ";//LAST_SALE_PRICE
        sQuery += "?, ";//QUANTITY

        sQuery += "?,";//ACCOUNT ID
        sQuery += "?,";//VENDOR ID
        sQuery += "?";//ITEM CODE

        sQuery += ") ";

        return sQuery;
    }

}
