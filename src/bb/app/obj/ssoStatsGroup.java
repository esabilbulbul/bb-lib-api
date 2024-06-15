/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.obj;

/**
 *
 * @author Administrator
 */
public class ssoStatsGroup 
{
    public ssoStatsObj received   = new ssoStatsObj();
    public ssoStatsObj sent       = new ssoStatsObj();
    public ssoStatsObj sold       = new ssoStatsObj();
    public ssoStatsObj refund     = new ssoStatsObj();

    public ssoStatsObj adjPlus    = new ssoStatsObj();
    public ssoStatsObj adjMinus   = new ssoStatsObj();
}
