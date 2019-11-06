/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

/**
 *
 * @author Administrator
 */
public class DekontStructure 
{
    public int    BankCode;
    
    public String TMP_LBL_START;//Indicates that the data starts after this
    public String TMP_LBL_END;//Indicates that the data ends after this
    
    public String DATA_TXNTYPE_SALE;//Satis
    public String DATA_TXNTYPE_INSTALLMENT;//Taksit
    public String DATA_TXNTYPE_COMM_SALE;//Satis Komisyon
    public String DATA_TXNTYPE_COMM_INSTALLMENT;//Taksitli Komisyon
    
    public String DATA_TXNTYPE_EFT;
    public String DATA_TXNTYPE_ACCOUNT;//Debit (deduct) / Credit (add)
    public String DATA_TXNTYPE_ACCOUNT2;//Debit (deduct) / Credit (add)
    

    public DekontStructure()
    {
        TMP_LBL_START = "";
        TMP_LBL_END   = "";
        DATA_TXNTYPE_SALE = "";
        DATA_TXNTYPE_INSTALLMENT = "";
        DATA_TXNTYPE_COMM_SALE = "";
        DATA_TXNTYPE_COMM_INSTALLMENT = "";
        DATA_TXNTYPE_EFT = "";
        DATA_TXNTYPE_ACCOUNT = "";
        DATA_TXNTYPE_ACCOUNT2 = "";
        
    }
}
