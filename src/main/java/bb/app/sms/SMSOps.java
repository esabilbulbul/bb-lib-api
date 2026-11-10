/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.sms;

import bb.app.account.ssoVendorSalesSummary;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.StoredProcedureQuery;
import jaxesa.persistence.annotations.ParameterMode;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.redis.ssoRedisLettuce;
import jaxesa.sms.smsUtil;
import jaxesa.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import ss.app.defs.ssQueues;
import ss.sms.ssSMSCommon;
import ss.sms.ssoSMSRequest;

/**
 *
 * @author Administrator
 */
public final class SMSOps 
{
        public static int    gMAX_SMS_NUMBER_PER_PACKAGE = 2500;

        public static String gEN_POWERED_BY_SHIPSHUK = "\n- powered by shipshuk.com";
        public static String gTR_POWERED_BY_SHIPSHUK = "\n- Mesaj shipshuk.com ile gönderilmiştir";

        public static String gSMS_TYPE_USER   = "U";
        public static String gSMS_TYPE_SYSTEM = "S";
        
        public static String createTestSMSRequest(EntityManager pem, 
                                                  String        psLang, 
                                                  BigInteger    pUserId, 
                                                  BigInteger    pAccId, 
                                                  String        pRawText) throws Exception
        {
            ssoRedisLettuce lettuce = new ssoRedisLettuce();

            ssoSMSPackage smsPackage = new ssoSMSPackage();
            ssoSMSRequest smsReq     = new ssoSMSRequest();

            String sRawTestContent   = "Test mesajidir.";
            String sFinalTestContent = "";

            try
            {
                // GET SMS SETTINGS PACKAGE
                smsPackage = getSMSSettings(pem, pUserId, pAccId);
                
                // Prepare SMS batches
                // 1. Generate SMS Group Id
                // 2. Fetch SMS Batches
                // 3. Send to SMS Worker
                
                if(smsPackage.smsBalance<=0)
                    return "Insufficient Balance";
                
                if(pRawText.trim().length()>0)
                {
                    sRawTestContent = pRawText;
                }

                smsPackage = getSMSSettings(pem, pUserId, pAccId);

                String sNumbers = "["+smsPackage.senderPhoneNumber+"]";

                smsReq = generateSMSRequest(psLang, 
                                            pUserId.toString(), 
                                            pAccId.toString(), 
                                            sRawTestContent, 
                                            sNumbers, 
                                            1,
                                            smsPackage.smsCancelCode, 
                                            smsPackage.smsCancelNumber,
                                            smsPackage.smsUser,
                                            smsPackage.smsPwd,
                                            smsPackage.textHeader);

                String sSMSReq = Util.JSON.toString(smsReq);

                lettuce = Util.Redis.getConnection("redis-sms-sender-connection");

                Util.Redis.JLists.push(lettuce, ssQueues.QUEUE_SMS_ENGINE, sSMSReq, true);

                // THIS IS IMPORTANT -- UPDATING THE BALANCE
                ssSMSCommon.updateSMSCredit4Queue(pem, pUserId, 1);//UPDATES SMS_QUEUE on USER

                Util.Redis.releaseConnection(lettuce);

                return "";

            }
            catch(Exception e)
            {
                if(lettuce!=null)
                {
                    Util.Redis.releaseConnection(lettuce);
                }
                
                throw e;
            }
        }

        // returns Error code if fails, otherwise empty 
        //
        // SMS SENING FLOW
        // 
        // Text sent updates SMS_QUEUE on User table (SERVER - API)
        // Then server API sends the numbers to SMS WORKER
        // SMS Worker sends the numbers to the SMS Operator
        // SMS Worker update SMS_SENT
        //
        // SMS_BALANCE = SMS_LOAD - (SMS_QUEUE (updated by server + worker) + SMS_SENT (updated by worker))
        //
        public static String createSMSRequest4Customers(EntityManager   pem, 
                                                        String          psLang, 
                                                        BigInteger      pUserId, 
                                                        BigInteger      pAccId, 
                                                        String          pRawText) throws Exception
        {
            ssoRedisLettuce lettuce = new ssoRedisLettuce();

            ssoSMSPackage smsPackage = new ssoSMSPackage();

            int iPhoneNumberPerBatch = gMAX_SMS_NUMBER_PER_PACKAGE;

            String sRawContent   = "";
            String sFinalContent = "";

            try
            {
                if(pRawText.trim().length()>0)
                {
                    sRawContent = pRawText;
                }
                
                // GET SMS SETTINGS PACKAGE
                smsPackage = getSMSSettings(pem, pUserId, pUserId);
                
                // Prepare SMS batches
                // 1. Generate SMS Group Id
                // 2. Fetch SMS Batches
                // 3. Send to SMS Worker
                
                if(smsPackage.smsBalance<=0)
                    return "Insufficient Balance";
                
                String sQuery = "";

                StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_SMS_GET_PHONE_NUMBER_BATCHES");

                SP.registerStoredProcedureParameter("P_USR_ID"                  , BigInteger.class , ParameterMode.IN);
                SP.registerStoredProcedureParameter("P_PHONE_NUMBER_PER_BATCH"  , String.class     , ParameterMode.IN);

                int Colindex = 1;
                SP.SetParameter(Colindex++, pUserId                , "P_USR_ID");
                SP.SetParameter(Colindex++, iPhoneNumberPerBatch   , "P_PHONE_NUMBER_PER_BATCH");

                SP.execute();

                List<List<RowColumn>> rs =  SP.getResultList();

                lettuce = Util.Redis.getConnection("redis-sms-sender-connection");

                long nTotalSent = 0;
                for (List<RowColumn> RowN:rs)
                {
                    ssoSMSRequest smsReq     = new ssoSMSRequest();

                    String sNumbers      = Util.Database.getValString(RowN, "PHONE_NUMBERS");

                    long nNumberOfPhones = new JSONArray(sNumbers).length();
                    long nNumberOfPhones2BeSent = 0;

                    if(nNumberOfPhones>0)
                    {
                        // Calculate how many to be texted
                        //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        long nRemainingBalance = smsPackage.smsBalance - (nTotalSent + nNumberOfPhones);
                        if(nRemainingBalance >= 0)
                        {
                            nNumberOfPhones2BeSent = nNumberOfPhones;
                        }
                        else
                        {
                            // Not enough credit / balance 
                            nNumberOfPhones2BeSent = smsPackage.smsBalance - nTotalSent;//send as remaining
                        }

                        if(nNumberOfPhones2BeSent>0)
                        {

                            smsReq = generateSMSRequest(psLang, 
                                                        pUserId.toString(), 
                                                        pAccId.toString(), 
                                                        sRawContent, 
                                                        sNumbers, 
                                                        nNumberOfPhones2BeSent,
                                                        smsPackage.smsCancelCode, 
                                                        smsPackage.smsCancelNumber,
                                                        smsPackage.smsUser,
                                                        smsPackage.smsPwd,
                                                        smsPackage.textHeader);

                            String sSMSReq = Util.JSON.toString(smsReq);

                            Util.Redis.JLists.push(lettuce, ssQueues.QUEUE_SMS_ENGINE, sSMSReq, true);

                            nTotalSent += nNumberOfPhones2BeSent;

                        }
                        else
                            break;//end of balance credit
                        
                    }

                }
                
                // THIS IS IMPORTANT -- UPDATING THE BALANCE
                ssSMSCommon.updateSMSCredit4Queue(pem, pUserId, nTotalSent);//UPDATES SMS_QUEUE on USER
                
                Util.Redis.releaseConnection(lettuce);
                
                return "";

            }
            catch(Exception e)
            {
                if(lettuce!=null)
                {
                    Util.Redis.releaseConnection(lettuce);
                }

                throw e;
            }
        }

        public static String createSMSRequest4SelectedNumbers(  EntityManager   pem, 
                                                                String          psLang, 
                                                                BigInteger      pUserId, 
                                                                BigInteger      pAccId, 
                                                                String          pRawText,
                                                                String          pNumbers // delimited by comma (,) 
                                                              ) throws Exception
        {
            ssoRedisLettuce lettuce = new ssoRedisLettuce();

            ssoSMSPackage smsPackage = new ssoSMSPackage();

            int iPhoneNumberPerBatch = gMAX_SMS_NUMBER_PER_PACKAGE;

            String sRawContent   = "";
            String sFinalContent = "";

            try
            {
                if(pRawText.trim().length()>0)
                {
                    sRawContent = pRawText;
                }

                // GET SMS SETTINGS PACKAGE
                smsPackage = getSMSSettings(pem, pUserId, pUserId);

                // Prepare SMS batches
                // 1. Generate SMS Group Id
                // 2. Fetch SMS Batches
                // 3. Send to SMS Worker

                if(smsPackage.smsBalance<=0)
                    return "Insufficient Balance";

                String sQuery = "";

                lettuce = Util.Redis.getConnection("redis-sms-sender-connection-selected-numbers");

                String[] aNumbers = pNumbers.split(",");

                String sNumbersBatch = "";
                ssoSMSRequest smsReq     = new ssoSMSRequest();
                int nTotalSent = 0;
                for(int i=0;i<aNumbers.length;i++)
                {
                    if(sNumbersBatch.trim().length()!=0)
                        sNumbersBatch += ",";

                    if(i+1%gMAX_SMS_NUMBER_PER_PACKAGE==0)
                    {
                        String sNumbersBatchFormatted = "[" + sNumbersBatch + "]";

                        smsReq = generateSMSRequest(psLang, 
                                                    pUserId.toString(), 
                                                    pAccId.toString(), 
                                                    sRawContent, 
                                                    sNumbersBatchFormatted, 
                                                    gMAX_SMS_NUMBER_PER_PACKAGE,
                                                    smsPackage.smsCancelCode, 
                                                    smsPackage.smsCancelNumber,
                                                    smsPackage.smsUser,
                                                    smsPackage.smsPwd,
                                                    smsPackage.textHeader);

                        String sSMSReq = Util.JSON.toString(smsReq);

                        Util.Redis.JLists.push(lettuce, ssQueues.QUEUE_SMS_ENGINE, sSMSReq, true);
                        
                        nTotalSent += gMAX_SMS_NUMBER_PER_PACKAGE;
                        sNumbersBatch = "";
                    }
                    else
                    {
                        sNumbersBatch += StringUtils.right(aNumbers[i], 10);
                    }

                }

                int nRemaining = aNumbers.length - nTotalSent;

                if(nRemaining>0)//remaining
                {
                    String sNumbersBatchFormatted = "[" + sNumbersBatch + "]";

                    // SEND REMAINING
                    smsReq = generateSMSRequest(psLang, 
                                                pUserId.toString(), 
                                                pAccId.toString(), 
                                                sRawContent, 
                                                sNumbersBatchFormatted, 
                                                nRemaining,
                                                smsPackage.smsCancelCode, 
                                                smsPackage.smsCancelNumber,
                                                smsPackage.smsUser,
                                                smsPackage.smsPwd,
                                                smsPackage.textHeader);

                    String sSMSReq = Util.JSON.toString(smsReq);

                    Util.Redis.JLists.push(lettuce, ssQueues.QUEUE_SMS_ENGINE, sSMSReq, true);

                }

                ssSMSCommon.updateSMSCredit4Queue(pem, pUserId, aNumbers.length);//UPDATES SMS_QUEUE on USER
                
                Util.Redis.releaseConnection(lettuce);
                
                return "";
            }
            catch(Exception e)
            {
                if(lettuce!=null)
                {
                    Util.Redis.releaseConnection(lettuce);
                }

                throw e;
            }
        }

        public static ssoSMSRequest generateSMSRequest(String psLang, 
                                                       String pUserId, 
                                                       String pAccId, 
                                                       String pContent, 
                                                       String pPhoneNumbers, 
                                                       long   pNumberOfPhones2BeSent,
                                                       String pSMSCancelCode,
                                                       String pSMSCancelNumber,
                                                       String pSMSUser,
                                                       String pSMSPassword,
                                                       String pSMSHeader) throws Exception
        {
            ssoSMSRequest smsReq     = new ssoSMSRequest();

            String sFinalContent = generateFinalContent(psLang, pContent, pSMSCancelCode, pSMSCancelNumber );

            smsReq.userId  = pUserId.toString();
            smsReq.content = sFinalContent;
            smsReq.noOfNumbers2Sent  = pNumberOfPhones2BeSent;
            smsReq.phoneNumbers      = pPhoneNumbers;
            smsReq.accId   = pAccId.toString();
            smsReq.hash    = smsUtil.generateSMSSignature(pUserId.toString(), pPhoneNumbers);
            smsReq.type    = gSMS_TYPE_USER;
            
            smsReq.smsUser = pSMSUser;//smsUtil.getEncryptedSMSData(pSMSUser);
            smsReq.smsPwd  = pSMSPassword;//smsUtil.getEncryptedSMSData(pSMSPassword);
            smsReq.smsHeader = pSMSHeader;

            return smsReq;
        }

        public static ssoSMSPackage getSMSSettings(EntityManager pem, BigInteger pUserId, BigInteger pAccId) throws Exception
        {
            ssoSMSPackage smsPackage = new ssoSMSPackage();

            try
            {
                   String sQuery = "SELECT IFNULL(SMS_STT.SMS_BALANCE,0) AS SMS_BALANCE, SMS_TEXT_HEADER, FN_DOUBLE_QUOTE(RIGHT(PHONE_NUMBER_SMS,10)) as PHONE_NUMBER_SMS, SMS_CANCEL_CODE, SMS_CANCEL_NUMBER, SMS.USER_SMS_TEXT, SMS.PWD_SMS_TEXT " +
                                    "FROM ss_usr_users USR " + 
                                    "INNER JOIN ss_sms_settings SMS ON SMS.UID = USR.SMS_SETTINGS_ID " + 
                                    "LEFT JOIN ss_acc_usr_sms_stats SMS_STT ON SMS_STT.USER_ID = USR.UID AND SMS_STT.ACCOUNT_ID = 0 " + 
                                    "WHERE " + 
                                    "USR.UID = ?";

                   Query stmt = pem.CreateNativeQuery(sQuery);
                   int index = 1;
                   stmt.SetParameter(index++, pUserId           , "USER_ID");

                   List<List<RowColumn>> rs = stmt.getResultList();
                   for(int i=0;i<rs.size();i++)
                   {
                         smsPackage.textHeader          = Util.Database.getValString(rs.get(i), "SMS_TEXT_HEADER");
                         smsPackage.senderPhoneNumber   = Util.Database.getValString(rs.get(i), "PHONE_NUMBER_SMS");
                         smsPackage.smsCancelCode       = Util.Database.getValString(rs.get(i), "SMS_CANCEL_CODE");
                         smsPackage.smsCancelNumber     = Util.Database.getValString(rs.get(i), "SMS_CANCEL_NUMBER");

                         smsPackage.smsBalance          = Long.parseLong(Util.Database.getValString(rs.get(i), "SMS_BALANCE"));

                         smsPackage.smsUser             = Util.Database.getValString(rs.get(i), "USER_SMS_TEXT");//ENCRYPTED
                         smsPackage.smsPwd              = Util.Database.getValString(rs.get(i), "PWD_SMS_TEXT");//ENCRYPTED

                   }

                   return smsPackage;
            }
            catch(Exception e)
            {
                throw e;
            }
        }

        // Add SMS Cancel Code
        // Add Shipshuk Powered by statement (Footer)
        public static String generateFinalContent(String pLang, String pContentByUser, String pSMSCancelCode, String pSMSCancelNumber)
        {
            String sFinalContent = pContentByUser;

            // ADD SMS Cancel Code
            //---------------------------------------------------------
            if(pSMSCancelCode.trim().length()>0)
            {
                sFinalContent += "\n\n" + "Iptal icin " + pSMSCancelCode + " yazip " + pSMSCancelNumber + "'a gonder";
            }

            // ADD Footer
            //---------------------------------------------------------
            if(pLang.toLowerCase().equals("en")==true)
            {
                sFinalContent += " " + gEN_POWERED_BY_SHIPSHUK;
            }
            else
            {
                // Default TR
                sFinalContent += " " + gTR_POWERED_BY_SHIPSHUK;
            }

            return sFinalContent;
        }

        // Add SMS Cancel Code
        // Add Shipshuk Powered by statement (Footer)
        public static ArrayList<ssoSMSReportRow> getSMSSendingReport(EntityManager pem, BigInteger pbiUserId, BigInteger pbiAccId) throws Exception
        {
            ArrayList<ssoSMSReportRow> aResults = new ArrayList<ssoSMSReportRow>();

            try
            {
                StoredProcedureQuery SP = pem.createStoredProcedureQuery("SP_SMS_GET_SMS_SENDING_RESULTS");

                SP.registerStoredProcedureParameter("P_USR_ID"                  , BigInteger.class       , ParameterMode.IN);
                SP.registerStoredProcedureParameter("P_ACC_ID"                  , BigInteger.class       , ParameterMode.IN);
                SP.registerStoredProcedureParameter("P_LAST_N_DAY"              , String.class     , ParameterMode.IN);

                int Colindex = 1;
                SP.SetParameter(Colindex++, pbiUserId                , "P_USR_ID");
                SP.SetParameter(Colindex++, pbiAccId                 , "P_ACC_ID");
                SP.SetParameter(Colindex++, 365                      , "P_LAST_N_DAY");

                SP.execute();

                List<List<RowColumn>> rs =  SP.getResultList();

                long nTotalSent = 0;
                for (List<RowColumn> RowN:rs)
                {
                    ssoSMSReportRow reportN = new ssoSMSReportRow();

                    reportN.txnDate              = Util.Database.getValString(RowN, "INSERTDATE");
                    reportN.groupId              = Util.Database.getValString(RowN, "MSG_GROUP_ID");
                    reportN.smsQueueQuantity     = Util.Database.getValString(RowN, "QUANTITY_SMS_QUEUE");
                    reportN.smsSentQuantity      = Util.Database.getValString(RowN, "QUANTITY_SMS_SENT");
                    reportN.smsDeliveredQuantity = Util.Database.getValString(RowN, "QUANTITY_SMS_SENT_VERIFIED");

                    aResults.add(reportN);
                }
                
                return aResults;
            }
            catch(Exception e)
            {
                throw e;
            }
        }
}
