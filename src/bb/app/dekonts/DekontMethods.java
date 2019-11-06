/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import jaxesa.util.Util;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

/**
 *
 * @author esabil
 */
public final class DekontMethods 
{
    public static final int BANK_CODE_YKB    = 1;
    public static final int BANK_CODE_ISBANK = 2;
    public static final int BANK_CODE_KUVEYT = 3;

    public static String TXN_TYPE_PESIN    = "PESIN";
    public static String TXN_TYPE_TAKSIT   = "TAKSIT";
    public static String TXN_TYPE_KOMISYON = "KOMISYON";
    public static String TXN_TYPE_EFT      = "EFT";
    public static String TXN_TYPE_DEBIT    = "DEBIT";//deduct MONEY
    public static String TXN_TYPE_CREDIT   = "CREDIT";//add MONEY

    public static String TXN_TYPE_CODE_PESIN    = "P";
    public static String TXN_TYPE_CODE_TAKSIT   = "T";
    public static String TXN_TYPE_CODE_KOMISYON = "K";
    public static String TXN_TYPE_CODE_EFT      = "E";
    public static String TXN_TYPE_CODE_DEBIT1   = "D0";//deduct MONEY
    public static String TXN_TYPE_CODE_DEBIT2   = "D1";//deduct MONEY
    public static String TXN_TYPE_CODE_CREDIT1  = "C0";//add MONEY
    public static String TXN_TYPE_CODE_CREDIT2  = "C1";//add MONEY

    /**
     * This app reads receipts (dekont) from the file and generates / output a excel decont file
     * 
     * @param args the command line arguments
     */
    public static ArrayList<DekontFields> processFile(String psInFilePath, String psOutFilePath, boolean bSkipFile, String psAccountOwnerName)
    {
        int iRecNo=0;
        // TODO code application logic here
        ArrayList<DekontFields> AllRecords = new ArrayList<DekontFields>();
        
        try
        {
            System.out.println("Working Directory = " + System.getProperty("user.dir"));

            //String sFilePath = "/Users/esabil/Documents/files/Hesap_Hareket_Detay_64265549_TL.pdf";
            //String sFilePath = "C:/NEOTEMP/CODE/SHIPSHUK/files/Hesap_Hareket_Detay_64265549_TL.pdf";//ykb
            //String sFilePath = "C:/Users/Administrator/Downloads/64050199824_20190829_16393162_HesapOzeti.pdf";//isbank
            //String sFilePath = "/Users/esabil/Documents/files/64050199824_20190829_16393162_HesapOzeti.pdf";//isbank
            int iBankCode = findBankCode(psInFilePath);
            
            String sFilePath = psInFilePath;
            
            //String sPathFormattedFile = "/Users/esabil/Documents/files/dekont_summary.txt";
            String sPathFormattedFile = psOutFilePath;

            PDDocument document = null; 
            document = PDDocument.load(new File(sFilePath));
            document.getClass();
            String st = "";
            if( !document.isEncrypted() )
            {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition( true );
                PDFTextStripper Tstripper = new PDFTextStripper();
                st = Tstripper.getText(document);
                //System.out.println("Text:"+st);

            }
            
            //PDF FILE LINES
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            String[] Lines = st.split("\\n");
            
            DekontStructure CurDefs = new DekontStructure();

            switch(iBankCode)
            {
                case BANK_CODE_YKB:
            
                    //YKB DEKONT FORMAT/DEFINITIONS
                    //------------------------------------------------------------------
                    DekontStructure YKBDefs = new DekontStructure();
                    YKBDefs.BankCode        = BANK_CODE_YKB;
                    YKBDefs.TMP_LBL_END     = "YAPI VE KREDİ BANKASI A.Ş";
                    YKBDefs.TMP_LBL_START   = "İşlem Tutarı";
                    YKBDefs.DATA_TXNTYPE_SALE             = "PEŞİNSATIŞ";
                    YKBDefs.DATA_TXNTYPE_INSTALLMENT      = "TAKSİTSATIŞ";
                    YKBDefs.DATA_TXNTYPE_COMM_SALE        = "PEŞSTKOM";
                    YKBDefs.DATA_TXNTYPE_COMM_INSTALLMENT = "TAKST KOM";
                    YKBDefs.DATA_TXNTYPE_EFT              = "";
                    
                    CurDefs = YKBDefs;

                    break;
                case BANK_CODE_ISBANK:

                    //ISBANK DEKONT FORMAT/DEFINITIONS
                    //------------------------------------------------------------------
                    DekontStructure IsbankDefs = new DekontStructure();
                    IsbankDefs.BankCode        = BANK_CODE_ISBANK;
                    IsbankDefs.TMP_LBL_END     = "Ticaret Sicil No";
                    IsbankDefs.TMP_LBL_START   = "Bakiyesi İşlem İşlem Tipi Açıklama";
                    IsbankDefs.DATA_TXNTYPE_SALE             = "NET SATIŞ TUTAR";
                    IsbankDefs.DATA_TXNTYPE_INSTALLMENT      = "";
                    IsbankDefs.DATA_TXNTYPE_COMM_SALE        = "";
                    IsbankDefs.DATA_TXNTYPE_COMM_INSTALLMENT = "";
                    IsbankDefs.DATA_TXNTYPE_EFT              = "EFT";
                    
                    CurDefs = IsbankDefs;
                    
                    break;
                case BANK_CODE_KUVEYT:
                    
                    //KUVEYT DEKONT FORMAT/DEFINITIONS
                    //------------------------------------------------------------------
                    DekontStructure KuveytDefs = new DekontStructure();
                    KuveytDefs.BankCode        = BANK_CODE_KUVEYT;
                    KuveytDefs.TMP_LBL_END     = "Ticaret Sicil No ";
                    KuveytDefs.TMP_LBL_START   = "İşlem Tarihi Referans Kodu";//"Referans Kodu Tutar Bakiye";//some files gives as commented block some as the active
                    KuveytDefs.DATA_TXNTYPE_SALE             = "Çözüm";
                    KuveytDefs.DATA_TXNTYPE_INSTALLMENT      = "";
                    KuveytDefs.DATA_TXNTYPE_COMM_SALE        = "";
                    KuveytDefs.DATA_TXNTYPE_COMM_INSTALLMENT = "";
                    KuveytDefs.DATA_TXNTYPE_EFT              = "";
                    KuveytDefs.DATA_TXNTYPE_ACCOUNT          = "Fon Yönetimi";
                    KuveytDefs.DATA_TXNTYPE_ACCOUNT2         = "Hesabına Yatırılan";
                    

                    CurDefs = KuveytDefs;

                    break;
            }


            
            // REFORMATTING LINE
            //------------------------------------------------------------------
            // - Title Words (YKB)
            //   - Şube
            //   - Müşteri Numarası
            //   - Hesap Şubesi
            //   - Bitiş Tarihi
            //   - Müşteri Bilgileri
            //   - Müşteri Tipi
            //   - SBU
            //   - Ad-Soyad/Unvan
            //   - Hesap Numarası
            //   - Başlangıç Tarihi
            //   - IBAN Numarası
            //
            // - Data Start (Columns Starting)
            //   - İşlem Tutarı
            //   
            // - Data Ending (Footnote)
            //   - YAPI VE KREDİ BANKASI A.Ş
            //
            // - Content Filters (The records will be saved)
            //   - Contains PEŞİNSATIŞ (txn type PESIN)
            //   - Contains TAKSİTSATIŞ (TXN type TAKSIT)
            //   - Contains PEŞSTKOM (Txn type = KOMISYON)
            //   - Contains TAKST KOM (Txn type = KOMISYON)

            
            //String sPathFormattedFile = "C:/NEOTEMP/CODE/SHIPSHUK/files/dekont_summary.txt";
            boolean bNoFilter = false;
            boolean bDataStarted = false;
            for (String lineN: Lines)
            {
    
                //int index = lineN.indexOf( "YAPI VE KREDİ BANKASI A.Ş");
                int index = lineN.indexOf(CurDefs.TMP_LBL_END);
                if (index ==0)
                    bDataStarted = false;//ending
                
                if (bDataStarted==true) 
                {
                    iRecNo++;
                    if(iRecNo==11)
                        iRecNo = iRecNo;
                    boolean bRecordYes = false;
                    
                    //FILTER HERE
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    String sTxnType = "";
                    DekontTxnType txnType = new DekontTxnType();
                    //bRecordYes = lineN.contains("PEŞİNSATIŞ");
                    txnType = findTxnType(CurDefs, lineN, psAccountOwnerName);
                    sTxnType = txnType.name;
                    if (sTxnType.trim().length()>0)
                        bRecordYes = true;
    
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    //REFORMAT HERE
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    if (bRecordYes==true)
                    {
                        //lineN = sTxnType + "\t" + lineN;
                        String sNewLine = "";
                        DekontFields fields = new DekontFields();

                        //if (fields.refNo.equals("A0IYW")==true)
                        if (lineN.contains("A0IYW")==true)
                        {
                            iRecNo = iRecNo;
                        }
                        
                        fields = parseDataLine(CurDefs.BankCode, txnType.name, txnType.code, lineN);
                        
                        //if (fields.TraceNo)//A0IYW
                        
                        sNewLine = generateNewFormattedLine(fields);
                        

                        System.out.println(lineN);
                        System.out.println(sNewLine);

                        //if (bNoFilter==false)
                        if (fields.bSkip==false)
                        {
                            AllRecords.add(fields);
                            if (bSkipFile==false)
                                Util.Files.Write2File(sPathFormattedFile, sNewLine);
                        }
                        //else
                            
                        
                    }
                }
                
                if (bNoFilter==true)
                {
                    if (bSkipFile==false)
                        Util.Files.Write2File(sPathFormattedFile, lineN);//no filter - write full data
                }
                
                //index = lineN.indexOf( "İşlem Tutarı");
                index = lineN.indexOf( CurDefs.TMP_LBL_START );
                if ((index == 0) || ((CurDefs.BankCode==BANK_CODE_KUVEYT) && (lineN.trim().length()==0)))
                {
                    bDataStarted = true;
                }
                
            }
            
            String sEnd = "end";
            
            // FULL-TEXT READ
            /*
            PDDocument document = PDDocument.load(new File(sFilePath));
            if (!document.isEncrypted()) 
            {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                System.out.println("Text:" + text);
            }
            document.close();
            */
            return AllRecords;
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    //pAccountName = NECATI BULBUL (for instance)
    public static DekontTxnType findTxnType(DekontStructure pBankDefs, String pDataLine, String pAccountName)
    {
        DekontTxnType txnType = new DekontTxnType();
        boolean bRecordYes = false;
        String lineN = pDataLine;
        String sTxnType = "";
        String[] aLineParts = lineN.split(" ");
        
        String sLBL_DEF = pBankDefs.DATA_TXNTYPE_SALE;
        bRecordYes = lineN.contains(sLBL_DEF);
        if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
        {
            //PESIN SATIS
            sTxnType = TXN_TYPE_PESIN;//"PESIN";
            txnType.name = TXN_TYPE_PESIN;
            txnType.code = TXN_TYPE_CODE_PESIN;
        }
        else
        {
            //bRecordYes = lineN.contains("TAKSİTSATIŞ"); 
            sLBL_DEF = pBankDefs.DATA_TXNTYPE_INSTALLMENT;
            bRecordYes = lineN.contains(sLBL_DEF);
            if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
            {
                sTxnType = "TAKSIT";
                txnType.name = TXN_TYPE_TAKSIT;
                txnType.code = TXN_TYPE_CODE_TAKSIT;
            }
            else
            {
                //bRecordYes = lineN.contains("PEŞSTKOM");
                sLBL_DEF = pBankDefs.DATA_TXNTYPE_COMM_SALE;
                bRecordYes = lineN.contains(sLBL_DEF);
                if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                {
                    sTxnType = "KOMISYON";
                    txnType.name = TXN_TYPE_KOMISYON;
                    txnType.code = TXN_TYPE_CODE_KOMISYON;
                }
                else
                {
                    //bRecordYes = lineN.contains("TAKST KOM");
                    sLBL_DEF = pBankDefs.DATA_TXNTYPE_COMM_INSTALLMENT;
                    bRecordYes = lineN.contains(sLBL_DEF);
                    if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                    {
                        sTxnType = "KOMISYON";
                        txnType.name = TXN_TYPE_KOMISYON;
                        txnType.code = TXN_TYPE_CODE_KOMISYON;
                        lineN = lineN.replaceAll(pBankDefs.DATA_TXNTYPE_COMM_INSTALLMENT,"TAKSTKOM");//works for YKB only
                    }
                    else
                    {
                        sLBL_DEF = pBankDefs.DATA_TXNTYPE_EFT;
                        bRecordYes = lineN.contains(sLBL_DEF);
                        if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                        {
                            sTxnType = "EFT";
                            txnType.name = TXN_TYPE_EFT;
                            txnType.code = TXN_TYPE_CODE_EFT;
                        }
                        else
                        {
                            sLBL_DEF = pBankDefs.DATA_TXNTYPE_ACCOUNT;//Amir= NECATİ BÜLBÜL-BÜLBÜLLER MAĞZASI Gönd.Bank.Kodu= 111Aciklama= KUVEYTURK HESABIMA - Fond Yonetimi
                            bRecordYes = lineN.contains(sLBL_DEF);
                            if (bRecordYes==false)
                            {
                                sLBL_DEF = pBankDefs.DATA_TXNTYPE_ACCOUNT2;//Format: NECATI BULBUL tarafindan .... Hesabina Yatirilan
                                bRecordYes = lineN.contains(sLBL_DEF);
                            }

                            if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                            {
                                String sAccountName = pAccountName;
                                boolean rc = lineN.toLowerCase().contains(sAccountName.toLowerCase());
                                if (rc==true)//
                                {
                                    //29.08.2019 A02BE KUVEYTTURK HESABIMA 1,000.00 6,489.79 Fon Yönetimi // -> Para hareketi (Debit / Credit)
                                    sTxnType = "FON";
                                    int ilen = aLineParts.length;
                                    String sAmount = aLineParts[ilen-4];//just before fon yonetimi
                                    boolean bMinus = sAmount.contains("-");//MINUS = DEBIT
                                    if (bMinus==true)
                                    {
                                        txnType.name = TXN_TYPE_DEBIT;
                                        txnType.code = TXN_TYPE_CODE_DEBIT1;
                                    }
                                    else
                                    {
                                        txnType.name = TXN_TYPE_CREDIT;
                                        txnType.code = TXN_TYPE_CODE_CREDIT1;
                                    }
                                }
                                else
                                {
                                    //transfer from others (utility, bill, rent payment and etc
                                    txnType.name = TXN_TYPE_EFT;
                                    txnType.code = TXN_TYPE_CODE_EFT;
                                }
                            }
                            
                        }
                    }
                }

            }
        }

        if (sTxnType.trim().length()==0)
        {
            if (pBankDefs.BankCode==BANK_CODE_KUVEYT)
            {
                int index = pDataLine.toLowerCase().indexOf("devir");
                if (!(index>=0))//not exist
                {
                    
                    index = pDataLine.toLowerCase().indexOf("A0");//must be txn record
                    if(index>0)
                    {
                        // Some oter formats for KUVEYTTURK
                        // A. 29.08.2019 A00C2 28-08-2019 273.09 5,489.79 // -> Satis with release date
                        // B. 08.08.2019 A009A 07-08-2019 585.19 3,211.22 // => Satis with release date
                        // C. 29.08.2019 A02BE KUVEYTTURK HESABIMA 1,000.00 6,489.79 Fon Yönetimi // -> Para hareketi (Debit / Credit)
                        // D. 28.08.2019 A01MI -650.00 4,972.87 // => Para hareketi (Debit / Credit)
                        //
                        // Note: C doesnt fall here (stays in previous block)
                        //

                        // A. & B. Satis with Release Date
                        String[] aDataParts = pDataLine.split(" ");
                        if (aDataParts.length==5)
                        {
                            //sTxnType = TXN_TYPE_PESIN;//"PESIN";
                            txnType.name = TXN_TYPE_PESIN;
                            txnType.code = TXN_TYPE_CODE_PESIN;
                        }
                        else if (aDataParts.length==4)
                        {
                            //------------------------------------------------------
                            // D. 28.08.2019 A01MI -650.00 4,972.87 // => Para hareketi (Debit / Credit)
                            //------------------------------------------------------

                            //if minus = Debit-money (withdraw) if plus credit-money (add)
                            boolean rc = aDataParts[2].contains("-");//if minus
                            if (rc==true)
                            {
                                txnType.name = TXN_TYPE_DEBIT;
                                txnType.code = TXN_TYPE_CODE_DEBIT2;//1 in previous if block
                                //sTxnType = TXN_TYPE_DEBIT;
                            }
                            else
                            {
                                txnType.name = TXN_TYPE_CREDIT;
                                txnType.code = TXN_TYPE_CODE_CREDIT2;//1 in previous if block
                                //sTxnType = TXN_TYPE_CREDIT;
                            }
                        }
                    }
                }
                
            }
        }
        
        return txnType;
    }

    /*
        This returns 
    */
    public static DekontFields parseDataLine(int piBankCode, String psTxnType, String psTxnTypeCode, String pDataLine)
    {
        DekontFields fields = new DekontFields();
        
        int i = 0;
        
        switch(piBankCode)
        {
            case BANK_CODE_YKB:

                fields = parseYKBDataLine(psTxnType, pDataLine);

                break;
            case BANK_CODE_ISBANK:

                fields = parseIsbankDataLine(psTxnType, pDataLine);
                
                break;
            case BANK_CODE_KUVEYT:
                
                fields = parseKuveytDataLine(psTxnType, psTxnTypeCode, pDataLine);
                
                break;
                
        }

        fields.Date     = DekontMisc.formatDate(fields.Date);
        fields.bankCode = Integer.toString(piBankCode);
        if (piBankCode==BANK_CODE_KUVEYT)
        {
            fields.TraceNo = fields.Date + fields.TraceNo;
        }
        
        return fields;
    }

    /*
        Warning:

        Read order from pdf file (record)
         - date time
         - txn type
         - desc (conditional)
         - amount
         - balance
    */
    public static DekontFields parseKuveytDataLine(String psTxnType, String psTxnTypeCode, String pDataLine)
    {
        DekontFields fields = new DekontFields();
        fields.TxnType = psTxnType;

        String[] sCols  = pDataLine.split(" ");
        
        if (!(sCols.length>4))//if not data missing
        {
            fields.bSkip = true;
            return fields;
        }

        if (!((psTxnType.equals(TXN_TYPE_DEBIT)==true) || (psTxnType.equals(TXN_TYPE_CREDIT)==true)))
        {
            for(int i=0;i<sCols.length;i++)
            {
                String sColData = sCols[i];
                switch(i)
                {
                    case 0://date time

                        String[] sDateTime = sColData.split("-");
                        fields.Date = sDateTime[0];
                        
                        fields.MonthNo = DekontMisc.getMonthNumber(fields.Date);
                        //fields.Time = sDateTime[1];

                        //String[] sDateParts = fields.Date.split("\\.");

                        //sColMonthNo = sDateParts[1];
                        //fields.MonthNo = sDateParts[1];

                        break;
                    case 1:

                        //trace no
                        fields.TraceNo = sColData;


                        break;
                    case 2:

                        String sDesc = "";
                        boolean rc = Util.Str.isAmount(sColData);
                        if (rc==false)//if desc interveneted skip until the amount
                        {
                            for(int j=0;j<100;j++)
                            {
                                sColData = sCols[i++];
                                rc = Util.Str.isAmount(sColData);
                                if (rc==true)
                                    break;
                                sDesc += sColData + " ";
                            }
                        }
                        /*
                        boolean rc = Util.Str.isAmount(sColData);
                        if (rc==false)
                        {
                            i+=2;
                            sColData = sCols[i];

                        }
                        */
                        fields.Desc = sDesc;
                        
                        if (sColData.trim().substring(0, 1).equals("-")==true)
                            fields.bSkip = true;

                        //Amount (format should be changed commas to dot, dot to comma) otherwise excel doesn't understand
                        fields.Amount = sColData;

                        /*
                        fields.Amount = fields.Amount.replace(",", "#");
                        fields.Amount = fields.Amount.replace(".", "&");

                        fields.Amount = fields.Amount.replace("#", ".");
                        fields.Amount = fields.Amount.replace("&", ",");
                        */

                        break;
                    case 3:

                        //balance
                        fields.Balance = sColData;

                        break;

                }
            }
        }
        else//DEBIT OR CREDIT
        {
            if ((psTxnTypeCode.equals(TXN_TYPE_CODE_DEBIT2)==true) || (psTxnTypeCode.equals(TXN_TYPE_CODE_CREDIT2)==true))
            {
                // Format = 28.08.2019 A01MI -650.00 4,972.87 // => Para hareketi (Debit / Credit) 
                fields.Date    = sCols[0];
                fields.TraceNo = sCols[1];
                
                String sDesc = "";
                for (int j=2;j<sCols.length-4; j++)
                    sDesc += sCols[j] + " ";
                
                fields.Desc    = sDesc;
                fields.Amount  = sCols[2];
                fields.Balance = sCols[3];
                fields.refNo   = fields.TraceNo;
                fields.MonthNo = DekontMisc.getMonthNumber(fields.Date);
            }
            else
            {
                //DEBIT1 or CREDIT1
                // 29.08.2019 A02BE KUVEYTTURK HESABIMA 1,000.00 6,489.79 Fon Yönetimi // -> Para hareketi (Debit / Credit)
                fields.Date    = sCols[0];
                fields.TraceNo = sCols[1];
                
                String sDesc = "";
                for (int j=2;j<sCols.length-4; j++)
                    sDesc += sCols[j] + " ";
                
                fields.Desc    = sDesc;
                fields.Amount  = sCols[sCols.length-4];
                fields.Balance = sCols[sCols.length-3];
                fields.refNo   = fields.TraceNo;
                fields.MonthNo = DekontMisc.getMonthNumber(fields.Date);
            }

        }
            
        return fields;

    }
    
    /*
        Warning:

        Read order from pdf file (record)
         - date time
         - channel
         - amount
         - balance
         - 2nd balance
         - txn code
         - txn type
    */
    public static DekontFields parseIsbankDataLine(String psTxnType, String pDataLine)
    {
        DekontFields fields = new DekontFields();
        fields.TxnType = psTxnType;
        
        String[] sCols  = pDataLine.split(" ");
        
        for(int i=0;i<sCols.length;i++)
        {
            String sColData = sCols[i];
            switch(i)
            {
                case 0:
                    
                    String[] sDateTime = sColData.split("-");
                    fields.Date = sDateTime[0];
                    fields.Time = sDateTime[1];
                    
                    String[] sDateParts = fields.Date.split("/");

                    //sColMonthNo = sDateParts[1];
                    fields.MonthNo = sDateParts[1];
                    
                    fields.TraceNo = fields.Date + fields.Time;
                    
                    fields.TraceNo = fields.TraceNo.replaceAll("/", "");
                    fields.TraceNo = fields.TraceNo.replaceAll(":", "");
                    fields.TraceNo = fields.TraceNo.trim();
                    //fields.TraceNo = fields.TraceNo.replaceAll(".", "");
                    
                    break;
                case 1:
                    
                    //channel
                    
                    break;
                case 2:
                    
                    //Amount (format should be changed commas to dot, dot to comma) otherwise excel doesn't understand
                    //For instance; 1,000.50 correct and 1.500,00 incorrect
                    fields.Amount = sColData;
                    
                    fields.Amount = fields.Amount.replace(",", "#");
                    fields.Amount = fields.Amount.replace(".", "&");
                    
                    fields.Amount = fields.Amount.replace("#", ".");
                    fields.Amount = fields.Amount.replace("&", ",");
                    
                    
                    break;
                case 3:
                    
                    //balance
                    fields.Balance = sColData;
                    
                    break;
                case 4:
                    
                    //2nd balance
                    
                    break;
                case 5:
                    
                    //txn code
                    
                    break;
                case 6:
                    
                    //txn type
                    //fields.TxnType = sColData;
                    
                    break;
                default://>7
                    
                    String sDesc = "";
                    //remaining goes to description field
                    for(int j=7;j<sCols.length;j++)
                    {
                        sDesc += sCols[j];
                    }
                    
                    fields.Desc = sDesc;
                    
                    return fields;
            }
        }

        return fields;
    }

    /*
        Warning: Columns order in pdf file comes different order when it is read.
        The order in pdf is following
        Tarih(1), Valor(2), Aciklama(3), Islem Tutari(4), Islem Saati(5), Bakiye(6), Dekont No(7)
    
        However, when read
        Tarih(1), Tutar(2), Bakiye + Valor (3) + ....
    */
    public static DekontFields parseYKBDataLine(String psTxnType, String pYKBDataLine)
    {
        String sColTxnType = psTxnType;
        DekontFields fields = new DekontFields();
        fields.TxnType = psTxnType;
        /*
        String sColDate = "";
        String sColRelease = "";
        String sColDesc = "";
        String sColAmount = "";
        String sColTime = "";
        String sColBalance = "";
        String sColTraceNo = "";
        String sColMonthNo = "";
        */

        //String sNewLine = lineN.replaceAll(" ", "\t");
        String[] sCols  = pYKBDataLine.split(" ");

//        for (int i=0;i<sCols.length;i++)
//        {
//            String sColData = sCols[i];
            
            //Date & Month No
            fields.Date = sCols[0];
            String[] sDateParts = fields.Date.split("\\.");
            fields.MonthNo = sDateParts[1];
            
            //Time Date
            fields.Time = sCols[sCols.length-2];

            //TraceNo
            fields.TraceNo = sCols[sCols.length-1];

            //Balance & Release
            String sBalNRelease = sCols[sCols.length-3];
            int index1stDot = sBalNRelease.indexOf(".");
            fields.Balance = sBalNRelease.substring(0, index1stDot + 2 + 1);
            fields.Release = sBalNRelease.substring(index1stDot + 2 + 1);

            //Amount
            fields.Amount = sCols[sCols.length-4];

            //Desc
            String sDesc = "";
            for (int j=1; j<sCols.length-4;j++)
            {
                sDesc += sCols[j] + " ";
            }
            fields.Desc = sDesc;


            /*
            switch(i)
            {
                case 0:
                    //Txn Date
                    //sColDate = sColData;
                    fields.Date = sColData;
                    String[] sDateParts = fields.Date.split("\\.");

                    //sColMonthNo = sDateParts[1];
                    fields.MonthNo = sDateParts[1];

                    break;
                case 6:
                    //Time Date
                    //sColTime = sColData;
                    fields.Time = sColData;
                    
                    break;
                case 1:
                    //Desc
                    //sColDesc = sColData;
                    fields.Desc = sColData;
                    
                    break;
                case 2:
                    //Amount
                    //sColAmount = sColData;
                    fields.Amount = sColData;

                    break;
                case 3:
                    //Balance + Release (No space between) (To be parsed)

                    String sBalNRelease = sColData;

                    int index1stDot = sBalNRelease.indexOf(".");

                    //sColBalance = sBalNRelease.substring(0, index1stDot + 2 + 1);
                    //sColRelease = sBalNRelease.substring(index1stDot + 2 + 1);

                    fields.Balance = sBalNRelease.substring(0, index1stDot + 2 + 1);
                    fields.Release = sBalNRelease.substring(index1stDot + 2 + 1);
                            
                    break;
                case 5:
                    // Trance No
                    //sColTraceNo = sColData;
                    fields.TraceNo = sColData;
                    
                    break;
            }
        }
        */

        return fields;
        /*
        String sNewLine =   sColTxnType + "\t" + 
                            sColDate + "\t" + 
                            sColRelease + "\t" + 
                            sColDesc + "\t" + 
                            sColAmount + "\t" + 
                            sColTime + "\t" + 
                            sColBalance + "\t" + 
                            sColTraceNo + "\t" + 
                            sColMonthNo;
        
        return sNewLine;
        */
    }
    
    public static String generateNewFormattedLine(DekontFields pFields)
    {
        String sNewLine =   pFields.TxnType + "\t" + 
                            pFields.Date + "\t" + 
                            pFields.Release + "\t" + 
                            pFields.Desc + "\t" + 
                            pFields.Amount + "\t" + 
                            pFields.Time + "\t" + 
                            pFields.Balance + "\t" + 
                            pFields.TraceNo + "\t" + 
                            pFields.MonthNo;
        
        return sNewLine;
    }

    public static int findBankCode(String psFilePath)
    {
        try
        {
            String sFilePath = psFilePath;
            
            //FileReader fr = new FileReader(psFilePath);
            //FileWriter  fw = new FileWriter(pFileName, pbAppend);
            
            PDDocument document = null; 
            document = PDDocument.load(new File(sFilePath));
            document.getClass();
            String st = "";
            if( !document.isEncrypted() )
            {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition( true );
                PDFTextStripper Tstripper = new PDFTextStripper();
                st = Tstripper.getText(document);
                //System.out.println("Text:"+st);

            }

            //PDF FILE LINES
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            int i =0;//for testt 
            String[] Lines = st.split("\\n");
            for (String lineN: Lines)
            {
                String lwLineN = lineN.toLowerCase();
                
                if (i>270)
                    i=i;
                
                i++;
                
                int ix = lwLineN.indexOf("kuveytt");//kuveytturk
                if (ix>=0)
                    return BANK_CODE_KUVEYT;
                
                ix = lwLineN.indexOf("yapı kredi");//kuveytturk
                if (ix>=0)
                    return BANK_CODE_YKB;

                ix = lwLineN.indexOf("ş bankas");//kuveytturk
                if (ix>=0)
                    return BANK_CODE_ISBANK;
            }
            
            return 0;
            
        }
        catch(Exception e)
        {
            return -1;
        }

    }
    
}
