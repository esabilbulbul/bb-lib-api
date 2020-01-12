/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import java.util.ArrayList;

/**
 *
 * @author esabil
 */
public class DekontSummaryYearN 
{
    public ArrayList<DekontSummaryQuarterDay>   Qdays  = new ArrayList<DekontSummaryQuarterDay>();
    public ArrayList<DekontSummaryQuarterWeek>  Qweeks = new ArrayList<DekontSummaryQuarterWeek>();

    public ArrayList<DekontSummaryWeek>            weeks   = new ArrayList<DekontSummaryWeek>();//ALL WEEKS 52 weeks
}
