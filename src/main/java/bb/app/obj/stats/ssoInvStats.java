/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.stats;

/**
 *
 * @author Administrator
 */
public class ssoInvStats 
{
    public ssoInvState inv       = new ssoInvState();//inv = current + eod + revolving
    public ssoInvState current   = new ssoInvState();
    public ssoInvState eod       = new ssoInvState();
    public ssoInvState revolving = new ssoInvState();
    public ssoInvState ytd       = new ssoInvState();
    public ssoInvState cumulative= new ssoInvState();
}


