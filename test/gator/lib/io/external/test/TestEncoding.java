/*
 * Copyright (C) 2018 Sergio Basurto Juarez
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package gator.lib.io.external.test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 *
 * <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class TestEncoding {
        public static void main(String args[]) {
            try {
                //GatorFileImportProcessor gappImportProcessor = new GatorFileImportProcessor("entrada_almacen_consorcio2.csv","pgmasterwms","1","1","admin");
                //System.out.println(gappImportProcessor.getStatus());
                System.out.println(URLDecoder.decode("KTU%3A%20CS1RpVzUVBT7zehaCVCYMfw5cGm0Yiw/AFkNLqD0D54%3D","UTF-8"));
                /*if(gappImportProcessor.getEjecutor().equals("1")) {
                gappImportProcessor.execGenerator2(gappImportProcessor.getGenerador(), gappImportProcessor.getParametros());
                } else if(gappImportProcessor.getEjecutor().equals("2")) {
                gappImportProcessor.execGenerator2(gappImportProcessor.getGenerador(), gappImportProcessor.getParametros());
                }*/
            } catch (UnsupportedEncodingException ex) {
                ;
            }
        }
}
