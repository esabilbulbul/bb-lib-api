/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.inv;

import bb.app.pages.ssoInvBrandItemCodes;
import entity.prm.SsPrmInvItemCodes;
import java.util.ArrayList;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.ssoKeyField;

/**
 *
 * @author Administrator
 */
public final class InventoryMisc
{
    public static ArrayList<ssoBrand> findMatchedBrandsForAccount(EntityManager pem, long pAccId, String pKeyword) throws Exception
    {
        try
        {
            ArrayList<SsPrmInvItemCodes> aPrmItemCodes = new ArrayList<SsPrmInvItemCodes>();

            ArrayList<ssoBrand> aBrands = new ArrayList<ssoBrand>();
            ArrayList<ssoInvBrandItemCodes> aItemsFound = new ArrayList<ssoInvBrandItemCodes>();

            ArrayList<ssoKeyField> criterias = new ArrayList<ssoKeyField>();

            ssoKeyField keyAccId = new ssoKeyField();
            keyAccId.ColumnName = "ACCOUNT_ID";
            keyAccId.Value      = pAccId;
            criterias.add(keyAccId);

            boolean rc = false;

            aPrmItemCodes = (ArrayList<SsPrmInvItemCodes>)pem.findAll(SsPrmInvItemCodes.class, criterias);

            for(SsPrmInvItemCodes itemCodeN:aPrmItemCodes)
            {
                if (itemCodeN.brandName.toLowerCase().indexOf(pKeyword.toLowerCase().trim())>=0)
                {
                    rc = isBrandIdUniqueInList(aBrands, itemCodeN.accBrandId);
                    if (rc==true)
                    {
                        ssoBrand newBrand = new ssoBrand();
                        newBrand.Id   = itemCodeN.accBrandId;
                        newBrand.name = itemCodeN.brandName;
                        aBrands.add(newBrand);
                    }
                }
            }

            return aBrands;
        }
        catch(Exception e)
        {
            throw e;
        }
    }
    
    static boolean isBrandIdUniqueInList(ArrayList<ssoBrand> paList, long pBrandId)
    {
        boolean bFound = false;
        
        for(ssoBrand brandN:paList)
        {
            if (brandN.Id==pBrandId)
            {
                //bFound = true;
                //break;
                return false;
            }
        }
        
        return true;
        //if (bFound==false)
        //    paList.add(pNew);
    }

}
