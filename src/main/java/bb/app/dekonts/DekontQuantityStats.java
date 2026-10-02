/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bb.app.dekonts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 *
 * @author esabil
 */
public class DekontQuantityStats
{

    public long   id    = 0;//merchant id
    public String name  = "";//don't change this name
    public String lastUpdate = "";

    public ArrayList<DekontQuantityStatCore> acc    = new ArrayList<DekontQuantityStatCore>();
    public ArrayList<DekontQuantityStatCore> market = new ArrayList<DekontQuantityStatCore>();
}

