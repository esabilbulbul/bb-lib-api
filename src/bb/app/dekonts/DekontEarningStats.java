/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

/**
 *
 * @author esabil
 */
public class DekontEarningStats 
{
    public long   id = 0;//merchant id
    public String name = "";
    
    public String dtime = "";
    public int    dayNo = 0;    
    public String refDate = "";    
    
    public String avgYearEarning = "";
    public String avgYTDEarning = "";
    
    public String   YearEarning = "";
    public String   YTDEarning = "";//Year to Date

    public double   diffYTD = 0;
    public double   diffYearEarning = 0;//in value
    
    public String   changeYearEarning = "";//in perc
    public String   changeYTDEarning = "";//Year to Date
    
    public double   diffYearEarning2Yesterday = 0;
    public String   changeYearEarning2Yesterday = "";//in perc
    

}
