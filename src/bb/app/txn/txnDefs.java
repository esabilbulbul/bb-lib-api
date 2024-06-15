/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.txn;

/**
 *
 * @author Administrator
 */
public final class txnDefs
{
    public static String INV_TXN_EFFECT_CREDIT = "C";// Means Vendor CREDITS US
    public static String INV_TXN_EFFECT_DEBIT  = "D";// Means We Credit Vendor

    /* ****************************************************************
                        TXN CODES (ss_prm_txn_codes)

        TXN_CODE = OTC[2] + OTS[2] + OTE[2]

    **************************************************************** */
    public static String TXN_CODE_OTC_SALE                 = "1";//1x
    public static String TXN_CODE_OTC_INVENTORY            = "2";//2x
    public static String TXN_CODE_OTC_PAYMENT              = "3";//3x...

    public static String TXN_CODE_REVOLVING                = "000000";

    public static String TXN_CODE_SALE_PURCHASE            = "001000";
    public static String TXN_CODE_SALE_REFUND              = "001010";
    //-------------------------------------------------------------------
    // For either inventory or payments 
    // When we give it is Debit 
    // When we take it is Credit 
    //-------------------------------------------------------------------
    public static String TXN_CODE_INVENTORY_RECEIVED       = "002000";//Credit
    public static String TXN_CODE_INVENTORY_SENT           = "002010";//Debit
    public static String TXN_CODE_INVENTORY_UPDATE         = "002020";//Debit

    public static String TXN_CODE_PAYMENT_RECEIVED_CHECK   = "003000";//C
    public static String TXN_CODE_PAYMENT_SENT_CHECK       = "003010";//D

    public static String TXN_CODE_PAYMENT_RECEIVED_CASH    = "003100";//C
    public static String TXN_CODE_PAYMENT_SENT_CASH        = "003110";//D

    public static String TXN_CODE_PAYMENT_RECEIVED_WIRE    = "003200";//C
    public static String TXN_CODE_PAYMENT_SENT_WIRE        = "003210";//D

    public static String TXN_CODE_PAYMENT_RECEIVED_CARD    = "003300";//C
    public static String TXN_CODE_PAYMENT_SENT_CARD        = "003310";//D

    /* ****************************************************************
                       TXN TYPES (ss_prm_txn_types)
    **************************************************************** */
    public static String TXN_TYPE_INVENTORY_RECEIVED       = "INVP";//Plus = P
    public static String TXN_TYPE_INVENTORY_SENT           = "INVM";// Minus - or RETURN
    public static String TXN_TYPE_INV_UPDATE               = "INUP";//up = update EXISTING TXN_CODE_INVENTORY_UPDATE

    public static String TXN_TYPE_FIN_ADJ_PLUS             = "FIN+";
    public static String TXN_TYPE_FIN_ADJ_MINUS            = "FIN-";

    public static String TXN_TYPE_SALES                    = "SALE";
    public static String TXN_TYPE_REFUND                   = "RFND";

    /* ****************************************************************
                    PAYMENT TYPES (ss_prm_payment_types)
    **************************************************************** */
    public static String TXN_TYPE_PAYMENT_CHECK            = "CHK";
    public static String TXN_TYPE_PAYMENT_CASH             = "CSH";
    public static String TXN_TYPE_PAYMENT_CARD             = "CRD";
    public static String TXN_TYPE_PAYMENT_WIRE             = "EFT";
    
    // TXN CODE = OTC + OTS + OTE
    public static String getTransactionCodeFromType(String psTxnType)
    {
        if (psTxnType.trim().equals(TXN_TYPE_INVENTORY_RECEIVED)==true)
        {
            return TXN_CODE_INVENTORY_RECEIVED;
        }
        else if (psTxnType.trim().equals(TXN_TYPE_INVENTORY_SENT)==true)
        {
            return TXN_CODE_INVENTORY_SENT;
        }
        else if (psTxnType.trim().equals(TXN_TYPE_INV_UPDATE)==true)
        {
            return TXN_CODE_INVENTORY_UPDATE;
        }
        else if (psTxnType.trim().equals(TXN_TYPE_SALES)==true)
        {
            return TXN_CODE_SALE_PURCHASE;
        }
        else if (psTxnType.trim().equals(TXN_TYPE_SALES)==true)
        {
            return TXN_CODE_SALE_REFUND;
        }

        return "xxxxxx";
    }

    public static String getTransactionEffect(String psTxnCode)
    {
        String sOTE = psTxnCode.substring(4,6);

        if (sOTE.equals("00")==true)//RECEIVED
            return INV_TXN_EFFECT_CREDIT;

        if (sOTE.equals("10")==true)//SENT
            return INV_TXN_EFFECT_DEBIT;

        return "";
    }

}


