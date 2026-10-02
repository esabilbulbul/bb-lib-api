/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.dekonts;

/**
 *
 * @author Administrator
 */
public class DekontAccDashboard 
{
    public String name = "";
    public String city = "";
    public String addr = "";//TBA
    public String logo = "";

    public String ytdSales           = "";
    public String ytdSalesChangeVal  = "";//TBA as of 18.01.2025
    public String ytdSalesChangePerc = "";//TBA as of 18.01.2025

    public String yeSales = "";
    public String yeSalesChangeVal  = "";
    public String yeSalesChangePerc = "";

    public String accVolumeChangePerc = "";//perc
    public String accVolumeChangeVal  = "";
    
    public String marketChangePerc = "";//perc
    public String marketChangeVal  = "";

    //public String marketChange = "";
    
    // Suppliers
    //----------------------------------------
    public String totalDelivered = "";
    public String totalSent = "";
    public String totalPayments = "";
    public String countSuppliers = "";
    
    public DekontInvSummary invSummary = new DekontInvSummary();
    public DekontPaymentSummary pymSummary = new DekontPaymentSummary();
    
    public DekontActiveVendorsSummary vndSummary = new DekontActiveVendorsSummary();
}
