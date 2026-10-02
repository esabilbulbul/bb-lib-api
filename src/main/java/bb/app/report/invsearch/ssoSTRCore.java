/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.report.invsearch;

/**
 *
 * @author Administrator
 */
public class ssoSTRCore {
    
    public String str = "";// Sold / Bought
    public String cBought = "";//Cost of Bought(s)
    public String cSold   = "";//Cost of Sold(s)
    
    public String qBought = "";//quantity of Bought
    public String qSold   = "";//quantity of Sold

    public ssoProfitCore profitBase = new ssoProfitCore();
    public ssoProfitCore profitUSD  = new ssoProfitCore();
}
