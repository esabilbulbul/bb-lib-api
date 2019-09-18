/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import java.util.ArrayList;

/**
 *
 * @author Administrator
 */
public class DekontSummary
{
    public String LastReportName;
    public ArrayList<MonthSummary> MonthsLastReport;
    public ArrayList<MonthSummary> MonthsAllReports;

    public DekontSummary()
    {
        LastReportName   = "";
        MonthsLastReport = new ArrayList<MonthSummary>();
        MonthsAllReports = new ArrayList<MonthSummary>();
    }
}


