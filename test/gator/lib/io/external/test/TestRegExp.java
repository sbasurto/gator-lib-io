/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gator.lib.io.external.test;

import java.util.regex.Pattern;

/**
 *
 * @author sbasurto
 */
public class TestRegExp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) { 
        
            String otherThanQuote = " [^\"] ";
            String quotedString = String.format(" \" %s* \" ", otherThanQuote);
            String commaRegex = String.format("(?x),(?=(?:%s*%s)*%s*$)",otherThanQuote, quotedString, otherThanQuote);
            String semicolonRegex = String.format("(?x);(?=(?:%s*%s)*%s*$)",otherThanQuote, quotedString, otherThanQuote);
            String productoDescripcion = "7615537126777;\"5\\\"\\\" Lightweight Shorts\";PZ;PZ;PT;;;;1ME11561573;S;CH;Blue;ON;7615537126777;;;Apparel;M;FW24;;;;;;;";            
            System.out.println(otherThanQuote);
            System.out.println(quotedString);
            System.out.println(commaRegex);
            System.out.println(productoDescripcion.split(semicolonRegex)[1]);
            Pattern utf8Patt = Pattern.compile("^(?i)utf-8");
            
            System.out.println(utf8Patt.matcher("UTF-16-SIG").find());
            String str = "Esta es una prueba de expresiones regulares, o que la cancióón";
            System.out.println(str.replaceAll("[ó]+", "---"));
            
            Pattern fechaPattSlash = Pattern.compile("^[0-9]{2}/[0-9]{2}/[0-9]{4}");
            Pattern fechaPattHyphen= Pattern.compile("^[0-9]{4}-[0-9]{2}-[0-9]{2}");
            String fecha1 = "14/04/2021";
            String fecha2 = "I.V.A. ORDEN DE PAGO SPEI";
            String fecha3 = "2021-04-14";
            String fecha4 = "14-04-2021";
            
            System.out.println(fechaPattSlash.matcher(fecha1).find());
            System.out.println(fechaPattSlash.matcher(fecha2).find());
            System.out.println(fechaPattSlash.matcher(fecha3).find());
            System.out.println(fechaPattSlash.matcher(fecha4).find());
            
            System.out.println(fechaPattHyphen.matcher(fecha1).find());
            System.out.println(fechaPattHyphen.matcher(fecha2).find());
            System.out.println(fechaPattHyphen.matcher(fecha3).find());
            System.out.println(fechaPattHyphen.matcher(fecha4).find());
            
    }
    
}
