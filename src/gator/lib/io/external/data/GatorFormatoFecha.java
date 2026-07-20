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
package gator.lib.io.external.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GatorFormatoFecha {
        private ArrayList<Integer> columnas = new ArrayList<>();
        private HashMap<String, String> formatos = new HashMap<>();
        public ArrayList<Integer> getColumnas() {
                return columnas;
        }
        public String getFormato(int columna) {
                return formatos.get("columna" + columna);
        }
}
