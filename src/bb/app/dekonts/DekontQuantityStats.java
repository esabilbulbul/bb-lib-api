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
public class DekontQuantityStats {
    
    public long   id = 0;//merchant id
    public String name= "";//don't change this name
    public int    dayNo = 0;    
    
    public String refDate = "";
    
    public double value = 0;
    public double diff;//dif in value
    
    public double   change = 0;//dif in perc

}
