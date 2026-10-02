/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.apis;

/**
 *
 * @author Administrator
 */
public class ssoEStoreHomeSummary 
{
    public String Sections       = "";
    public String Categories     = "";
    public String Options        = "";
    public String showroomItems  = "";
    
    public ssoEStoreHomeSummary()
    {
    }

    public ssoEStoreHomeSummary(String pSection, String pCategories, String pOptions, String pItems)
    {
        Sections = pSection;
        Categories = pCategories;
        Options = pOptions;
        showroomItems = pItems;
    }

}

