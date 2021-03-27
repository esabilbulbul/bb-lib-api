/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apicallbacks;

import bb.app.inv.InventoryOps;
import bb.app.inv.ssoInventoryParams;
import bb.app.inv.ssoItemOption;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entity.acc.SsAccInvBrandItemCodes;
import entity.acc.SsAccInvBrands;
import java.util.ArrayList;
import jaxesa.api.callback.ssoCallbackParam;
import jaxesa.persistence.EntityManager;
import jaxesa.util.Util;


/**
 *
 * @author Administrator
 */

// INFO : CALLBACK methods are called sequential in other words main thread calling them is a single thread

// DONT RENAME 
public class InventoryCallbacks 
{
    // DONT RENAME 
    // INFO:
    // This callback manager updates inventory stats. However, this is only designed to update at runtime. So that 
    // the entries by users will affect the inventory quantities immediately. Yet, the main process 
    // will take place at the end of day. Which will recount the inventories entered and update the final stat. 
    // In other words, the quantities affected might show one value however the actual can be (eod) another (in case of a problem)
    public void newInventoryTransaction_Callback(String psCallbackId, 
                                                 String psUserId, 
                                                 EntityManager pem,
                                                 String pjaParams)
                                                 //ArrayList<ssoCallbackParam> paParams)
    {
        try
        {
            JsonArray jsaParams = new JsonArray();

            jsaParams = Util.JSON.toArray(pjaParams);

            //JsonObject prm1 = jsaParams.get(0).getAsJsonObject();
            //ssoCallbackParam cbParam = new ssoCallbackParam();

            ArrayList<ssoCallbackParam> aParams = new ArrayList<ssoCallbackParam>();
            for(int i =0; i<jsaParams.size(); i++)
            {
                ssoCallbackParam prmN = new ssoCallbackParam();

                prmN = (ssoCallbackParam)Util.JSON.toObject(jsaParams.get(i).toString(), ssoCallbackParam.class);

                aParams.add(prmN);
            }

            String sTxnId     = psCallbackId;//txn uid
            long   lTxnId     = Long.parseLong(sTxnId);
            String sUserId    = psUserId;
            String sAccId     = (String)Util.Callback.getParamValue(aParams, "aid");
            String sBrand     = (String)Util.Callback.getParamValue(aParams, "bnm");
            String sItemCode  = (String)Util.Callback.getParamValue(aParams, "icd");
            String sCategory  = (String)Util.Callback.getParamValue(aParams, "ctg");
            String sTxnType   = (String)Util.Callback.getParamValue(aParams, "txn");
            String sQuantity  = (String)Util.Callback.getParamValue(aParams, "qnt");
            String sEntryPrice = (String)Util.Callback.getParamValue(aParams, "eprc");
            String sDiscount   = (String)Util.Callback.getParamValue(aParams, "dso");
            String sTax        = (String)Util.Callback.getParamValue(aParams, "tx");
            String sOptions    = (String)Util.Callback.getParamValue(aParams, "opt");
            String sSalesPrice = (String)Util.Callback.getParamValue(aParams, "sprc");

            long lUserId = Long.parseLong(sUserId);
            long lAccId  = Long.parseLong(sAccId);

            sBrand    = sBrand.trim();
            sCategory = sCategory.trim();
            sItemCode = sItemCode.trim();
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1!!
            //
            // Steps to new inventory transaction
            // 1. Register Category Category (can be cachable)
            // 2. Read + Update/Write Brand Balance 
            // 3. Add Item Price Info
            // 4. Read + Update/Write Item Quantity
            // 5. Read + Update/Write Item Options
            // 6. Register Brand & Item Code 
            //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            // 1. Register New Category (if new)
            long lCategoryID = 0;
            lCategoryID = InventoryOps.addNewCategory(pem, lAccId, sCategory.toUpperCase().trim());

            // 3. Update Brand Balance
            SsAccInvBrands brandAcc = new SsAccInvBrands();
            brandAcc = InventoryOps.updateBrandBalance(pem, lTxnId, sTxnType, lAccId, sBrand, sQuantity, sEntryPrice, sDiscount, sTax);

            // 3. Add Item Price Info
            long lPriceId = InventoryOps.addItemPrice(pem, lTxnId, lAccId, sEntryPrice, sDiscount, sTax, sSalesPrice);

            // 4. Update Item 
            SsAccInvBrandItemCodes brndItem = new SsAccInvBrandItemCodes();
            brndItem = InventoryOps.updateItemInfo( pem, 
                                                    lTxnId, 
                                                    sTxnType, 
                                                    lAccId, 
                                                    brandAcc.uid, 
                                                    sItemCode, 
                                                    sQuantity,
                                                    lCategoryID,
                                                    lPriceId
                                                    );

            // 5. Update Item Options Quantity
            ArrayList<ssoItemOption> options = new ArrayList<ssoItemOption>();
            options = InventoryOps.getItemOptions(sOptions);
            InventoryOps.updateItemOptionsQuantity( pem, 
                                                    lTxnId, 
                                                    brndItem, 
                                                    options, 
                                                    sTxnType, 
                                                    sQuantity,
                                                    lCategoryID
                                                    );

            // 7. Register New Brand & Item Code (if new)
            //long lBrandItemID = 0;
            InventoryOps.addNewBrandItem(   pem, 
                                            lAccId, 
                                            sBrand.toUpperCase().trim(), 
                                            sItemCode,
                                            brandAcc.uid,
                                            brndItem.uid,
                                            sSalesPrice);

            return ;
        }
        catch(Exception e)
        {
            
        }
    }
}
