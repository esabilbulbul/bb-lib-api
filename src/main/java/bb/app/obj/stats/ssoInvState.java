/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bb.app.obj.stats;

/**
 *
 * @author Administrator
 */
public class ssoInvState 
{
    public ssoInvStateCore net = new ssoInvStateCore();// THIS IS FOR SUMMARY

    // THESE ARE MAIN STATS 
    public ssoInvStateCore quantity = new ssoInvStateCore();
    public ssoInvStateCore base = new ssoInvStateCore();
    public ssoInvStateCore discount = new ssoInvStateCore();
    public ssoInvStateCore surcharge = new ssoInvStateCore();
    public ssoInvStateCore tax = new ssoInvStateCore();
    public ssoInvStateCore expense = new ssoInvStateCore();
    public ssoInvStateCore gross = new ssoInvStateCore();
}


