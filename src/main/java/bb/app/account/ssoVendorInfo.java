/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.account;

/**
 *
 * @author Administrator
 */
public class ssoVendorInfo 
{
    public String name;
    public String city;
    
    public String balance;
    public String ytdNetTotal; // ytdNetTotal = balance (must be same)

    public String categorySummary;

    public String ytdNetTaxEntered;
    public String ytdNetTaxReturned;
    public String ytdNetTaxSold;
    public String ytdNetTaxRefund;
    public String ytdNetTax;
    public String ytdNetTaxFinAdjPlus;
    public String ytdNetTaxFinAdjMinus;

    public String itemsTaxEntered_Cumulative;
    public String itemsTaxReturned_Cumulative;
    public String itemsTaxSold_Cumulative;
    public String itemsTaxRefund_Cumulative;
    public String itemsTaxFinAdjPlus_Cumulative;
    public String itemsTaxFinAdjMinus_Cumulative;

    public String ytdNetExpenseEntered;
    public String ytdNetExpenseReturned;
//    public String ytdNetExpenseSold;
//    public String ytdNetExpenseRefund;
    public String ytdNetExpense;
    public String ytdNetExpenseFinAdjPlus;
    public String ytdNetExpenseFinAdjMinus;

    public String itemsExpenseEntered_Cumulative;
    public String itemsExpenseReturned_Cumulative;
    public String itemsExpenseSold_Cumulative;
    public String itemsExpenseRefund_Cumulative;

    public String itemsExpenseFinAdjPlus_Cumulative;
    public String itemsExpenseFinAdjMinus_Cumulative;

    public String revolvingBalance;
    public String revolvingQuantity;

    public String itemsNetQuantityEntered_YTD;
    public String itemsNetQuantityEntered_Cumulative;

    public String itemsNetQuantityReturned_YTD;
    public String itemsNetQuantityReturned_Cumulative;

    public String itemsNetQuantitySold_YTD;
    public String itemsNetQuantitySold_Cumulative;

    public String itemsNetQuantityRefund_YTD;
    public String itemsNetQuantityRefund_Cumulative;

    public String itemsNetQuantityFinAdjPlus_YTD;
    public String itemsNetQuantityFinAdjPlus_Cumulative;

    public String itemsNetQuantityFinAdjMinus_YTD;
    public String itemsNetQuantityFinAdjMinus_Cumulative;

    public String itemsNetQuantity_YTD;//net 
    public String itemsNetQuantity_Cumulative;

    public String itemsReceivedAmount_YTD;
    //public String itemsReceivedQuantity_YTD;
    public String itemsReceivedAmount_Cumulative;//all years
    //public String itemsReceivedQuantity_Cumulative;

    public String itemsSentAmount_YTD;
    //public String itemsSentQuantity_YTD;
    public String itemsSentAmount_Cumulative;
    //public String itemsSentQuantity_Cumulative;


    public String itemsSoldAmount_YTD;
    //public String itemsSoldQuantity_YTD;
    public String itemsSoldAmount_Cumulative;
    //public String itemsSoldQuantity_Cumulative;

    public String itemsRefundAmount_YTD;
    //public String itemsSoldQuantity_YTD;
    public String itemsRefundAmount_Cumulative;
    //public String itemsSoldQuantity_Cumulative;

    //public String itemsPaidAmount_YTD;
    public String itemsNetPaidAmount_YTD;
    public String itemsPaymentSentAmount_Cumulative;//All years
    public String itemsPaymentReceivedAmount_Cumulative;//All years

    public String itemsFinAdjPlusNetAmount_YTD;
    public String itemsFinAdjMinusNetAmount_YTD;

    public String allTimeVolume;
    public String profitability;
}
