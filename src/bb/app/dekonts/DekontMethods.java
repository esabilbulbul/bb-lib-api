/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import java.io.File;
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
    public static final int BANK_CODE_YKB    = 0;
    public static final int BANK_CODE_ISBANK = 1;
    public static final int BANK_CODE_KUVEYT = 2;

    /**
     * This app reads receipts (dekont) from the file and generates / output a excel decont file
     * 
     * @param args the command line arguments
     */
    public static ArrayList<DekontFields> processFile(String psInFilePath, String psOutFilePath, boolean bSkipFile)
    {
        // TODO code application logic here
        ArrayList<DekontFields> AllRecords = new ArrayList<DekontFields>();
        
        try
        {
            System.out.println("Working Directory = " + System.getProperty("user.dir"));

            //String sFilePath = "/Users/esabil/Documents/files/Hesap_Hareket_Detay_64265549_TL.pdf";
            //String sFilePath = "C:/NEOTEMP/CODE/SHIPSHUK/files/Hesap_Hareket_Detay_64265549_TL.pdf";//ykb
            //String sFilePath = "C:/Users/Administrator/Downloads/64050199824_20190829_16393162_HesapOzeti.pdf";//isbank
            //String sFilePath = "/Users/esabil/Documents/files/64050199824_20190829_16393162_HesapOzeti.pdf";//isbank
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

            //KUVEYT DEKONT FORMAT/DEFINITIONS
            //------------------------------------------------------------------
            DekontStructure KuveytDefs = new DekontStructure();
            KuveytDefs.BankCode        = BANK_CODE_KUVEYT;
            KuveytDefs.TMP_LBL_END     = "Ticaret Sicil No ";
            KuveytDefs.TMP_LBL_START   = "Referans Kodu Tutar Bakiye";
            KuveytDefs.DATA_TXNTYPE_SALE             = "A0";
            KuveytDefs.DATA_TXNTYPE_INSTALLMENT      = "";
            KuveytDefs.DATA_TXNTYPE_COMM_SALE        = "";
            KuveytDefs.DATA_TXNTYPE_COMM_INSTALLMENT = "";
            KuveytDefs.DATA_TXNTYPE_EFT              = "";
            
            CurDefs = KuveytDefs;
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
                    boolean bRecordYes = false;
                    
                    //FILTER HERE
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    String sTxnType = "";
                    //bRecordYes = lineN.contains("PEŞİNSATIŞ");
                    sTxnType = findTxnType(CurDefs, lineN);
                    if (sTxnType.trim().length()>0)
                        bRecordYes = true;
    
                    //REFORMAT HERE
                    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    if (bRecordYes==true)
                    {
                        //lineN = sTxnType + "\t" + lineN;
                        String sNewLine = "";
                        DekontFields fields = new DekontFields();
                        fields = parseDataLine(BANK_CODE_KUVEYT, sTxnType, lineN);
                        //fields = parseDataLine(BANK_CODE_ISBANK, sTxnType, lineN);
                        sNewLine = generateNewFormattedLine(fields);
                        //sNewLine = parseYKBDataLine(sTxnType, lineN);

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
    
    public static String findTxnType(DekontStructure pBankDefs, String pDataLine)
    {
        boolean bRecordYes = false;
        String lineN = pDataLine;
        String sTxnType = "";
        
        String sLBL_DEF = pBankDefs.DATA_TXNTYPE_SALE;
        bRecordYes = lineN.contains(sLBL_DEF);
        if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
        {
            //PESIN SATIS
            sTxnType = "PESIN";
        }
        else
        {
            //bRecordYes = lineN.contains("TAKSİTSATIŞ"); 
            sLBL_DEF = pBankDefs.DATA_TXNTYPE_INSTALLMENT;
            bRecordYes = lineN.contains(sLBL_DEF);
            if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
            {
                sTxnType = "TAKSIT";
            }
            else
            {
                //bRecordYes = lineN.contains("PEŞSTKOM");
                sLBL_DEF = pBankDefs.DATA_TXNTYPE_COMM_SALE;
                bRecordYes = lineN.contains(sLBL_DEF);
                if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                {
                    sTxnType = "KOMISYON";
                }
                else
                {
                    //bRecordYes = lineN.contains("TAKST KOM");
                    sLBL_DEF = pBankDefs.DATA_TXNTYPE_COMM_INSTALLMENT;
                    bRecordYes = lineN.contains(sLBL_DEF);
                    if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                    {
                        sTxnType = "KOMISYON";
                        lineN = lineN.replaceAll(pBankDefs.DATA_TXNTYPE_COMM_INSTALLMENT,"TAKSTKOM");//works for YKB only
                    }
                    else
                    {
                        sLBL_DEF = pBankDefs.DATA_TXNTYPE_EFT;
                        bRecordYes = lineN.contains(sLBL_DEF);
                        if ((bRecordYes==true) && (sLBL_DEF.trim().length()>0))
                        {
                            sTxnType = "EFT";
                        }
                    }
                }

            }
        }

        return sTxnType;
    }

    /*
        This returns 
    */
    public static DekontFields parseDataLine(int piBankCode, String psTxnType, String pDataLine)
    {
        int i = 0;
        
        switch(piBankCode)
        {
            case BANK_CODE_YKB:

                return parseYKBDataLine(psTxnType, pDataLine);

            case BANK_CODE_ISBANK:

                return parseIsbankDataLine(psTxnType, pDataLine);
                
            case BANK_CODE_KUVEYT:
                
                return parseKuveytDataLine(psTxnType, pDataLine);
                
                
        }

        return new DekontFields();
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
    public static DekontFields parseKuveytDataLine(String psTxnType, String pDataLine)
    {
        DekontFields fields = new DekontFields();
        fields.TxnType = psTxnType;

        String[] sCols  = pDataLine.split(" ");

        for(int i=0;i<sCols.length;i++)
        {
            String sColData = sCols[i];
            switch(i)
            {
                case 0://date time
                    
                    String[] sDateTime = sColData.split("-");
                    fields.Date = sDateTime[0];
                    //fields.Time = sDateTime[1];
                    
                    String[] sDateParts = fields.Date.split("\\.");

                    //sColMonthNo = sDateParts[1];
                    fields.MonthNo = sDateParts[1];
                    
                    break;
                case 1:
                    
                    //txn type
                    
                    break;
                case 2:
                    
                    boolean rc = Util.Str.isAmount(sColData);
                    if (rc==false)//if desc interveneted skip until the amount
                    {
                        for(int j=0;j<100;j++)
                        {
                            sColData = sCols[i++];
                            rc = Util.Str.isAmount(sColData);
                            if (rc==true)
                                break;
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
                    
                    break;
                case 1:
                    
                    //channel
                    
                    break;
                case 2:
                    
                    //Amount (format should be changed commas to dot, dot to comma) otherwise excel doesn't understand
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

        for (int i=0;i<sCols.length;i++)
        {
            String sColData = sCols[i];

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
    
}
