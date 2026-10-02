/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import bb.app.obj.ssoMerchantPreferences;
import java.util.ArrayList;

/**
 *
 * @author esabil
 */
public class DekontSummary
{
    public ssoMerchantPreferences accPrefs = new ssoMerchantPreferences();
    
    public String currency = "";

    public ArrayList<DekontSummaryTots>           banks    = new ArrayList<DekontSummaryTots>();
    public ArrayList<DekontSummaryRec>            rows     = new ArrayList<DekontSummaryRec>();
    public ArrayList<DekontSummaryYear>           years    = new ArrayList<DekontSummaryYear>();

    // Quarter Days & Weeks
    //-------------------------------------------------------------------------------
    public ArrayList<DekontSummaryQuarterDay>     Qdays     = new ArrayList<DekontSummaryQuarterDay>();
    public ArrayList<DekontSummaryQuarterWeek>   Qweeks     = new ArrayList<DekontSummaryQuarterWeek>();
    public ArrayList<DekontSummaryQuarterMonth>   Qmonths   = new ArrayList<DekontSummaryQuarterMonth>();

    // All Weeks  (52 Weeks)
    //-------------------------------------------------------------------------------
    public ArrayList<DekontSummaryWeek>            weeks   = new ArrayList<DekontSummaryWeek>();
    
    // FIRST NTH DAYS
    public ArrayList<DekontSummaryNthDay>          days   = new ArrayList<DekontSummaryNthDay>();

    // Current Year
    //-------------------------------------------------------------------------------
    public DekontSummaryYearN                thisYear   = new DekontSummaryYearN();//Current Year
    public DekontSummaryYearN                lastYear   = new DekontSummaryYearN();//Current Year

    // Overall (years)
    //-------------------------------------------------------------------------------
    public ArrayList<DekontSummaryYear> overall = new ArrayList<DekontSummaryYear>();//years
    
    public DekontSummaryUseRates useRates12 = new DekontSummaryUseRates();//last 12 months
    public DekontSummaryUseRates useRates24 = new DekontSummaryUseRates();//last 24 months

    //Current Month
    //-------------------------------------------------------------------------------
    public DekontSummaryMonth currentMonth = new DekontSummaryMonth();
    
    // Target Month Weeks
    //-------------------------------------------------------------------------------
    public ArrayList<DekontSummaryWeek>            MonthNweeks   = new ArrayList<DekontSummaryWeek>();

    // Target Month Weeks
    //-------------------------------------------------------------------------------
    
    // INDEX - Earning Stats
    //-------------------------------------------------------------------------------
    public DekontEarning earnings = new DekontEarning();
    public DekontEarning changes = new DekontEarning();
    
    // INDEX - Quantity Stats
    //-------------------------------------------------------------------------------
    public DekontQuantityStats quantities = new DekontQuantityStats();
    
    public DekontNews news = new DekontNews();
    
    //Stats Subtotals
    public DekontAccDashboard dashboard = new DekontAccDashboard();
    
    public String baseYearDate = "";
    public String lastYearDate = "";
    
    public String targetMonth = "";
}
