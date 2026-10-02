/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.apis;

/**
 *
 * @author Administrator
 */
public class ssoOption {

    public String category = "";
    public String groupName = "";
    public String optionName = "";
    public String id = "";
    public String quantity = "";

    public ssoOption(String pCategory, String pGroup, String pOption, String pId, String pQuantity)
    {
        category = pCategory;
        groupName = pGroup;
        optionName = pOption;
        id = pId;
        quantity = pQuantity;
    }
}

