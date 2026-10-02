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
public class DekontSummaryMonth 
{
    public ArrayList<DekontSummaryDay>            days   = new ArrayList<DekontSummaryDay>();
    
    public ArrayList<DekontSummaryWeek>           weeks   = new ArrayList<DekontSummaryWeek>();//ALL WEEKS 52 weeks
    
    public ArrayList<DekontSummaryDay>            dayAvgs = new ArrayList<DekontSummaryDay>();
}
