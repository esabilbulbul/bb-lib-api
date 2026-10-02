/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.apis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
//import entity.img.SsImgUrls;
import entity.web.SsWebAccPageParams;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import jaxesa.persistence.EntityManager;
import jaxesa.persistence.Query;
import jaxesa.persistence.misc.RowColumn;
import jaxesa.redis.ssoRedisLettuce;
import jaxesa.redis.ssoRedisUnionKey;
import jaxesa.redis.ssoUnionMatchScript;
import jaxesa.util.Util;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 *
 * @author Administrator
 */
public final class APIOps {
    
    static String KEY_NAME_ESTORE_PAGE_DEFAULT  = "page.default";
    
    static String KEY_NAME_ESTORE_FILTER_SECTION  = "filter.section";
    static String KEY_NAME_ESTORE_FILTER_CATEGORY = "filter.category";
    static String KEY_NAME_ESTORE_FILTER_OPTIONS  = "filter.options";
    static String KEY_NAME_ESTORE_SHOWROOM_ITEMS  = "showroom_items";

    public static ssoPageParamsHome getTestEStorePageParams()
    {
            ssoPageParamsHome EStoreUIParams = new ssoPageParamsHome();
            // GENDERS
            EStoreUIParams.filters.genders.add("men");
            EStoreUIParams.filters.genders.add("women");

            // Categories
            EStoreUIParams.filters.categories.add(new ssoCategory("Pants", "1", "11"));
            EStoreUIParams.filters.categories.add(new ssoCategory("Shirts", "2", "8"));
            EStoreUIParams.filters.categories.add(new ssoCategory("Dresses", "3", "29"));

            // Categories
            EStoreUIParams.filters.options.add(new ssoOption("Pants", "Black", "38", "1232", "2"));
            EStoreUIParams.filters.options.add(new ssoOption("Pants", "Black", "40", "1232", "3"));
            EStoreUIParams.filters.options.add(new ssoOption("Pants", "Black", "42", "1232", "4"));

            // Sections (Trends, Discounts, New Season ...)
            String sSectionKeyNewSeason = "NS";
            EStoreUIParams.filters.sections.add(new ssoSection("New Season",sSectionKeyNewSeason));
            EStoreUIParams.filters.sections.add(new ssoSection("Discount","SO"));
            EStoreUIParams.filters.sections.add(new ssoSection("New Year - Special","NY"));

            ArrayList<ssoImage> itemImages = new ArrayList<ssoImage>();
            itemImages.add(new ssoImage("https://shipshuk.com/assets/logo/SHIPSHUK.png", true));
            itemImages.add(new ssoImage("https://shipshuk.com/assets/logo/SHIPSHUK.png", false));

            EStoreUIParams.items.add(new ssoProduct("shipshuk.com/storename/12334", sSectionKeyNewSeason,  "200.00", "200.00", "3", itemImages));
            EStoreUIParams.items.add(new ssoProduct("shipshuk.com/storename/12334", sSectionKeyNewSeason,  "200.00", "150.00", "2", itemImages));

            EStoreUIParams.items.add(new ssoProduct("shipshuk.com/storename/12334", sSectionKeyNewSeason,  "200.00", "340.00", "1", itemImages));
            EStoreUIParams.items.add(new ssoProduct("shipshuk.com/storename/12334", sSectionKeyNewSeason,  "200.00", "250.00", "4", itemImages));

            return EStoreUIParams;
    }
    
    public static ArrayList<ssoImage> getImgUrls(EntityManager pem, String pItemCode)
    {
        ArrayList<ssoImage> images = new ArrayList<ssoImage>();

        try
        {
            Query stmt= pem.CreateNativeQuery(""); //pem.createNamedQuery("SsImgUrls.findByItemCode", SsImgUrls.class);

            int index = 1;
            stmt.SetParameter(index++, pItemCode, "ITEM_CODE");

            List<List<RowColumn>> rs = stmt.getResultList();

            for (int i = 0; i < rs.size(); i++)
            {
                String  url        = Util.Database.getValString(rs.get(i), "URL");
                boolean bDefault   = Util.Database.getValString(rs.get(i), "IS_DEFAULT").equals("1");

                images.add(new ssoImage(url, bDefault));
            }
        }
        catch (Exception e)
        {

        }

        return images;
    }

    public static String getEStorePageParams(  EntityManager     pem,
                                                BigInteger       pUserId,
                                                BigInteger       pAccId,
                                                boolean          pbUser) throws Exception
    {
        try
        {
            String sKeyPrefixAccType = "";
            String sKeyPrefixAccId   = "";
            BigInteger biAccId = BigInteger.ZERO;
            boolean bRedisOn = false;

            ssoRedisLettuce lettuce = null;

            if(pbUser==false)
            {
                sKeyPrefixAccType = "a";
                sKeyPrefixAccId = pAccId.toString();
                biAccId = pAccId;
            }
            else
            {
                sKeyPrefixAccType = "u";
                sKeyPrefixAccId = pUserId.toString();
            }

            lettuce = Util.Redis.getConnectionAsync("getEStorePageParams");
            bRedisOn = true;

            // Read from cache
            String sPageParams = readEStoreParams(lettuce, sKeyPrefixAccType, sKeyPrefixAccId, KEY_NAME_ESTORE_PAGE_DEFAULT);
            lettuce.close();

            if(sPageParams!=null)
            {
                if(sPageParams.trim().length()>0)
                {
                    return sPageParams;
                }
            }

            ssoEStoreHomeSummary oPageParams = new ssoEStoreHomeSummary();
            oPageParams = reloadEStorePageParams(pem, pUserId, pAccId, pbUser);

            return Util.JSON.Convert2JSON(oPageParams).toString();
            
        }
        catch(Exception e)
        {
            throw e;
        }
    }

    public static boolean isEStorePageParamsLoaded(BigInteger       pUserId, 
                                                   BigInteger       pAccId,
                                                   boolean          pbUser) throws Exception
    {
        boolean bRedisOn = false;
        ssoRedisLettuce lettuce = null;
        String sKeyPrefixAccType = "";
        String sKeyPrefixAccId   = "";
        BigInteger biAccId = BigInteger.ZERO;

        try
        {
            if(pbUser==false)
            {
                sKeyPrefixAccType = "a";
                sKeyPrefixAccId = pAccId.toString();
                biAccId = pAccId;
            }
            else
            {
                sKeyPrefixAccType = "u";
                sKeyPrefixAccId = pUserId.toString();
            }

            lettuce = Util.Redis.getConnection("isEStorePageParamsLoaded");
            bRedisOn = true;

            // Read from cache 
            String sPageParams = readEStoreParams(lettuce, sKeyPrefixAccType, sKeyPrefixAccId, KEY_NAME_ESTORE_PAGE_DEFAULT);
            lettuce.close();
            
            if(sPageParams==null)
                return false;

            if(sPageParams.trim().length()>0)
                return true;
            else
                return false;
        }
        catch(Exception e)
        {
            if(bRedisOn==true)
                lettuce.close();
            
            throw e;
        }

    }

    // Store data to REDIS 
    public static ssoEStoreHomeSummary reloadEStorePageParams(EntityManager    pem, 
                                                                BigInteger       pUserId, 
                                                                BigInteger       pAccId,
                                                                boolean          pbUser// true: User Data false: Account Data
                                                            ) throws Exception
    {
        ssoEStoreAccPageParams params = new ssoEStoreAccPageParams();
        ssoEStoreHomeSummary eStoreSummary = new ssoEStoreHomeSummary();
        
        String sKeyPrefixAccType = "";
        String sKeyPrefixAccId   = "";
        BigInteger biAccId = BigInteger.ZERO;
        boolean bRedisOn = false;

        ssoRedisLettuce lettuce = null;

        try
        {
            if(pbUser==false)
            {
                sKeyPrefixAccType = "a";
                sKeyPrefixAccId = pAccId.toString();
                biAccId = pAccId;
            }
            else
            {
                sKeyPrefixAccType = "u";
                sKeyPrefixAccId = pUserId.toString();
            }

            Query stmt = pem.createNamedQuery("SsWebAccPageParams.getPageParams", SsWebAccPageParams.class);

            int index = 1;
            stmt.SetParameter(index++, pUserId, "USER_ID");
            stmt.SetParameter(index++, biAccId, "ACCOUNT_ID");

            List<List<RowColumn>> rs = stmt.getResultList();

            if (rs.size() > 0)
            {
                params.summarySections      = Util.Database.getValString(rs.get(0), "SUMMARY_SECTIONS");//{"newseason":2.000,"discount":0.000,"trend":0.000}
                params.summaryCategories    = Util.Database.getValString(rs.get(0), "SUMMARY_CATEGORIES");//{"SHIRT":11.000,"BLUZ":297.000,"TEST":0.000,"TAKIM":0.000,"CEKET":0.000}
                params.summaryOptions       = Util.Database.getValString(rs.get(0), "SUMMARY_OPTIONS");//{"SHIRT":{"-B":1.00,"BLACK-B":0.00,"RED-C":0.00,"WHITE-M":0.00,"WHITE-L":0.00,"BLACK-M":0.00},"TEST":{"BLACK-L":0.00,"BLACK-M":0.00}}

                params.topItemsBySection    = Util.Database.getValString(rs.get(0), "TOP_ITEMS_BY_SECTION");//{"newseason":[1,2,3,4,5],"discount":[2,3,4,5,6],"trend":[1,2,3,4,5]}
                params.topItemsByCategories = Util.Database.getValString(rs.get(0), "TOP_ITEMS_BY_CATEGORIES");//{"MEN":[1,2,3,4,5],"WOMEN":[2,3,4,5,6],"CHILDREN":[1,2,3,4,5],"UNISEX":[1,2,3,4,5]}
                params.topItemsByOptions    = Util.Database.getValString(rs.get(0), "TOP_ITEMS_BY_OPTIONS");//{"BLUZ":[46874862,46870654],"CEKET":[46874466],"SHIRT":[46887150,46884944,],"TAKIM":[46874464,46874465,46874467,46874486]}
                params.showroomItemsManual  = Util.Database.getValString(rs.get(0), "SHOWROOM_ITEMS_MANUAL");//[46871019,46887150,46878262]

            }

            try
            {
                lettuce = Util.Redis.getConnectionAsync("storeWebAccPageParams");
                bRedisOn = true;

                // FILTERS & SHOWROOM STORED (summary)
                //-------------------------------------
                
                eStoreSummary.Sections   = params.summarySections;
                eStoreSummary.Categories = params.summaryCategories;
                eStoreSummary.Options    = params.summaryOptions;
                eStoreSummary.showroomItems = params.showroomItemsManual;
                
                String sEStoreSummary = Util.JSON.Convert2JSON(eStoreSummary).toString();
                saveEStoreParams(lettuce, sKeyPrefixAccType, sKeyPrefixAccId, KEY_NAME_ESTORE_PAGE_DEFAULT, sEStoreSummary);
                
                //storeEStoreParams(lettuce, sKeyPrefixAccType, sKeyPrefixAccId, KEY_NAME_ESTORE_FILTER_SECTION, );
                //storeEStoreParams(lettuce, sKeyPrefixAccType, sKeyPrefixAccId, KEY_NAME_ESTORE_FILTER_CATEGORY, );
                //storeEStoreParams(lettuce, sKeyPrefixAccType, sKeyPrefixAccId, KEY_NAME_ESTORE_FILTER_OPTIONS, params.summaryOptions);

                // ITEMS STORE (rows)
                //-------------------------------------
                storeIDs(   lettuce, pUserId, pAccId, sKeyPrefixAccType, sKeyPrefixAccId, params.topItemsBySection);
                storeIDs(   lettuce, pUserId, pAccId, sKeyPrefixAccType, sKeyPrefixAccId, params.topItemsByCategories);
                storeIDs(   lettuce, pUserId, pAccId, sKeyPrefixAccType, sKeyPrefixAccId, params.topItemsByOptions);
                storeIDs(   lettuce, pUserId, pAccId, sKeyPrefixAccType, sKeyPrefixAccId, params.showroomItemsManual);

                lettuce.close();

                //Util.Redis.JString.set(lettuce, pKey, pVal)
            }
            catch(Exception e)
            {
                if(bRedisOn==true)
                    lettuce.close();

                throw e;
            }

        }
        catch (Exception e)
        {
            if(bRedisOn==true)
                lettuce.close();

            throw e;
        }

        return eStoreSummary;
    }
    
    public static String readEStoreParams(ssoRedisLettuce  plettuce, 
                                         String           psKeyPrefixAccType,
                                         String           psKeyPrefixAccId,
                                         String           psKeyName
                                        )
    {
        String sKey = generateKey4General(psKeyPrefixAccType, psKeyPrefixAccId, psKeyName);
        return Util.Redis.JString.getSync(plettuce, sKey);
    }

    public static void saveEStoreParams(ssoRedisLettuce  plettuce, 
                                         String           psKeyPrefixAccType,
                                         String           psKeyPrefixAccId,
                                         String           psKeyName,
                                         String           psData)
    {
        String sKey = generateKey4General(psKeyPrefixAccType, psKeyPrefixAccId, psKeyName);
        Util.Redis.JString.setSync(plettuce, sKey, psData);
    }
    
    public static void storeIDs( ssoRedisLettuce plettuce,
                                BigInteger       pUserId, 
                                BigInteger       pAccId,
                                String           psKeyPrefixAccType,
                                String           psKeyPrefixAccId,
                                String           pjsIDs
                              ) throws Exception
    {
        boolean bErrFlag = false;
        String sErrMsg  = "";
        boolean allOk = false;
        try
        {
            plettuce.connection.setAutoFlushCommands(false);//MUST BE Before starting batch insert on redis
                
            //ArrayList<String> aSections = new ArrayList<>();
            //aSections = Util.JSON.getKeys(params.summarySections);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(pjsIDs);

            // READING SECTIONS
            //---------------------------------------------
            Iterator<String> aFields = root.getFieldNames();
            List<RedisFuture<?>> futures = new ArrayList<>();
            
            while (aFields.hasNext()) 
            {
                String fieldN = aFields.next();

                JsonNode jsnSectionN = root.get(fieldN);//newseason

                // RETRIEVING IDS
                //---------------------------------------------
                ArrayList<String> items = new ArrayList<>();
                for (JsonNode IDNode : jsnSectionN) 
                {
                    items.add(IDNode.asText()); // asText() works whether the JSON value is numeric or string
                }

                // STORING on REDIS
                //---------------------------------------------
                //String key = "acc." + psKeyPrefixAccType + "." + psKeyPrefixAccId + "." + sectionN;
                String key =generateKey4Items(psKeyPrefixAccType, psKeyPrefixAccId, fieldN.trim().toLowerCase());
                
                String[] saItemIDs = items.toArray(new String[0]);

                if(saItemIDs.length>0)
                    futures.add(Util.Redis.Sinter.set(plettuce, key, saItemIDs));

            }
            
            plettuce.connection.flushCommands(); 
            
            allOk = LettuceFutures.awaitAll(Duration.ofSeconds(5), futures.toArray(new RedisFuture[0])); // wait for all replies
            
        }
        catch(Exception e)
        {
            bErrFlag = true;
            sErrMsg = e.getMessage();
        }
        finally
        {
            plettuce.connection.setAutoFlushCommands(true); // 5. MUST restore before returning to pool
            
        }
        
    }
    
    public static String generateKey4Items(String psKeyPrefixAccType, String psKeyPrefixAccId, String psFilterName)
    {
        String key = "acc." + psKeyPrefixAccType + "." + psKeyPrefixAccId + "." + psFilterName;
        
        return key;
    }

    public static String generateKey4General(String psKeyPrefixAccType, String psKeyPrefixAccId, String psFilterName)
    {
        String key = "acc." + psKeyPrefixAccType + "." + psKeyPrefixAccId + "." + psFilterName;
        
        return key;
    }

    // Data is on redis and will be scripted thru LUA script
    public static ArrayList<ssoWebItem> searchItems(EntityManager    pem, 
                                                    BigInteger       pUserId,
                                                    BigInteger       pAccId,
                                                    boolean          pbUser,
                                                    int              pPageNumber,
                                                    String[]           pFilterSection, //newseason, trend, discount
                                                    String[]           pFilterCategory,
                                                    String[]           pFilterGender,
                                                    String[]           pFilterOptions,
                                                    String             pPriceRange,
                                                    String[]           pFilterOther
                                                    ) throws Exception
    {
        String sKeyPrefixAccType = "";
        String sKeyPrefixAccId   = "";
        BigInteger biAccId = BigInteger.ZERO;

        boolean bRedisOn = false;

        ssoRedisLettuce lettuce = null;
        
        try
        {
            //String sNewSeasonIdsKey = "acc.a.38482645.newseason";
            //String sTrendIdsKey     = "acc.a.38482645.trend";

            if(pbUser==false)
            {
                sKeyPrefixAccType = "a";
                sKeyPrefixAccId = pAccId.toString();
                biAccId = pAccId;
            }
            else
            {
                sKeyPrefixAccType = "u";
                sKeyPrefixAccId = pUserId.toString();
            }

            ArrayList<String> aFilters = new ArrayList<String>();
            
            //ssoUnionMatchScript redisLunaUnionMatchScript = new ssoUnionMatchScript();

            ArrayList<ssoRedisUnionKey> aRedisUnionKeys = new ArrayList<ssoRedisUnionKey>();

            //String[] aSections    = pFilterSection.split(",");
            String[] aSections    = pFilterSection;
            for(String sectionN:aSections)
            {
                String sKeySection  = generateKey4Items(sKeyPrefixAccType, sKeyPrefixAccId, sectionN);
                aFilters.add(sKeySection);

                aRedisUnionKeys.add(new ssoRedisUnionKey("section", sKeySection));
            }

            //String[] aCategory    = pFilterCategory.split(",");
            String[] aCategory    = pFilterCategory;
            for(String categoryN:aCategory)
            {
                if(categoryN.trim().length()>0)
                {
                    String sKeyCategory  = generateKey4Items(sKeyPrefixAccType, sKeyPrefixAccId, categoryN);
                    aFilters.add(sKeyCategory);

                    aRedisUnionKeys.add(new ssoRedisUnionKey("category", sKeyCategory));
                }
            }
            
            //String[] aGenders    = pFilterGender.split(",");
            String[] aGenders    = pFilterGender;
            for(String genderN:aGenders)
            {
                if(genderN.trim().length()>0)
                {
                    String sKeyGender  = generateKey4Items(sKeyPrefixAccType, sKeyPrefixAccId, genderN);
                    aFilters.add(sKeyGender);

                    aRedisUnionKeys.add(new ssoRedisUnionKey("gender", sKeyGender));
                }
            }

            //String[] aOptions    = pFilterOptions.split(",");
            String[] aOptions    = pFilterOptions;
            for(String optionN:aGenders)
            {
                if(optionN.trim().length()>0)
                {
                    String sKeyOptions  = generateKey4Items(sKeyPrefixAccType, sKeyPrefixAccId, optionN);
                    aFilters.add(sKeyOptions);

                    aRedisUnionKeys.add(new ssoRedisUnionKey("options", sKeyOptions));
                }
            }

            String[] filters = aFilters.toArray(new String[0]);

            lettuce = Util.Redis.getConnectionAsync("storeWebAccPageParams");
            bRedisOn = true;

            // FIND MATCHES ON REDIS
            //--------------------------------------
            ssoUnionMatchScript redisLunaUnionMatchScript = new ssoUnionMatchScript();
            
            redisLunaUnionMatchScript = Util.Redis.Lua.prepareUnionMatchScript(aRedisUnionKeys);
            
            Set<String> matchedIds = Util.Redis.Lua.execute(lettuce, redisLunaUnionMatchScript);
            
            //Set<String> matchedIds = Util.Redis.Sinter.findMatches(lettuce, filters);
            lettuce.close();

            // Get Ids From DB
            //--------------------------------------
            ArrayList<ssoWebItem> items = getItemStatsByUIDs(pem, matchedIds);

            return items;
        }
        catch(Exception e)
        {
            String s = e.getMessage();
            if(bRedisOn==true)
                lettuce.close();
            
            throw e;
        }
        
    }

    public static ArrayList<ssoWebItem> getItemStatsByUIDs(EntityManager pem, Set<String> pUIDs) throws Exception
    {
        try
        {
            ArrayList<ssoWebItem> items = new ArrayList<ssoWebItem>();

            if(pUIDs==null || pUIDs.isEmpty()==true)
                return items;

            int iMaxUIDNumber = 100;
            if(pUIDs.size()>iMaxUIDNumber)
                iMaxUIDNumber = 100;
            else
                iMaxUIDNumber = pUIDs.size();

            StringBuilder sbPlaceholders = new StringBuilder();
            int i = 0;
            for(i = 0; i < iMaxUIDNumber; i++)
            {
                if(i>0)
                    sbPlaceholders.append(", ");

                sbPlaceholders.append("?");
            }

            String sQuery = "SELECT ITM.ITEM_CODE, ITM.TITLE, ITM.IMG_IDS, ITM.SPD_STAR, ITM.PRICE_ID, ITM.LAST_SALE_PRICE, " + 
                            "CONCAT('[',GROUP_CONCAT(FN_DOUBLE_QUOTE(CONCAT(IF(IMG.ISDEFAULT='Y','>>default>>',''), IFNULL(IFNULL(IMG.URL_CDN,IMG.URL),'-')))),']') as IMGURLS " + 
                            "FROM ss_acc_inv_item_stats ITM " + 
                            "LEFT JOIN ss_img_urls IMG ON IMG.ITEM_ID = ITM.UID " +
                            "WHERE " + 
                            "IMG.STAT = 1 " +
                            "AND " + 
                            "ITM.UID IN (" + sbPlaceholders.toString() + ") " + 
                            "GROUP BY ITM.UID ";

            Query stmt = pem.CreateNativeQuery(sQuery);

            int index = 1;
            for(String sUID : pUIDs)
            {
                stmt.SetParameter(index++, new BigInteger(sUID), "UID");
                
                if((index-1)>=iMaxUIDNumber)
                    break;
                
            }

            List<List<RowColumn>> rs = stmt.getResultList();

            for (int j = 0; j < rs.size(); j++)
            {
                ssoWebItem item = new ssoWebItem();

                item.itemCode      = Util.Database.getValString(rs.get(j), "ITEM_CODE");
                item.title         = Util.Database.getValString(rs.get(j), "TITLE");
                item.imgIds        = Util.Database.getValString(rs.get(j), "IMG_IDS");
                item.spdStar       = Util.Database.getValString(rs.get(j), "SPD_STAR");
                item.priceId       = Util.Database.getValString(rs.get(j), "PRICE_ID");
                item.lastSalePrice = Util.Database.getValString(rs.get(j), "LAST_SALE_PRICE");
                item.imgUrls       = Util.Database.getValString(rs.get(j), "IMGURLS");

                items.add(item);
            }

            return items;

        }
        catch(Exception e)
        {
            throw e;
        }
    }

}
