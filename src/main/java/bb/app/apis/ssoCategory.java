/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.apis;

/**
 *
 * @author Administrator
 */
public class ssoCategory {
    public String category = "";
    public String id = "";
    public String quantity = "";//how many under this quantity
    
    public ssoCategory(String pCategory, String pId, String pQuantity)
    {
        category = pCategory;
        id = pId;
        quantity = pQuantity;
    }
}
