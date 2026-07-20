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
package gator.lib.io.external;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;

/**
 *
 * <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GatorAdmonTablas {
        /**
         * The Gson object.
         */
         Gson gson = new Gson();
         
        /**
         * The object to be prepared.
         */
         private JsonObject obj = new JsonObject();
         
         
         /**
          * Tables
          */
         private ArrayList<String> tables = new ArrayList<>();
         
         /**
          * Tables
          */
         private ArrayList<String> acciones = new ArrayList<>();
         
         /**
          * Fields
          */
         private ArrayList<ArrayList<String>> campos = new ArrayList<>();
         
         /**
          * Values
          */
         private ArrayList<ArrayList<String>> valores = new ArrayList<>();
         
         /**
          * Condition fields
          */
         private ArrayList<ArrayList<String>> condsCampos = new ArrayList<>();
         
         /**
          * Condition values;
          */
         private ArrayList<ArrayList<String>> condsValores = new ArrayList<>();
         
         /**
          * Set debug level.
          * 
          * @param debugLevel The debug level, goes from 0..1000.
          */
         public void setDebugLevel(String debugLevel) {
                obj.addProperty("debugLevel", debugLevel);
         }
         
         /**
          * Set debug level.
          * 
          * @param idctrl The debug level, goes from 0..1000.
          */
         public void setIdCtrl(String idctrl) {
                obj.addProperty("idctrl", idctrl);
         }
         
         /**
          * Set debug level.
          * 
          * @param usuarioId The debug level, goes from 0..1000.
          */
         public void setUsuarioId(String usuarioId) {
                obj.addProperty("usuario", usuarioId);
         }
         
         /**
          * Set debug level.
          * 
          * @param sesionId The debug level, goes from 0..1000.
          */
         public void setSesionId(String sesionId) {
                obj.addProperty("sesionId", sesionId);
         }
         
         /**
          * Allow to add tables to object.
          * 
          * @param table The table to be added.
          */
         public void addTable(String table) {
                tables.add(table);
                obj.add("tablas", gson.toJsonTree(tables).getAsJsonArray());
         }
         
         /**
          * Allow to add tables to object.
          * 
          * @param action Action to be performed.
          */
         public void addAccion(String action) {
                acciones.add(action);                
         }
         
         /**
          * Allow to add fields to object.
          * 
          * @param fields An array list of string that contains the fields for this object.
          */
         public void addFields(ArrayList<String> fields) {
                campos.add(fields);
                obj.add("campos", gson.toJsonTree(campos).getAsJsonArray());
         }
         
         /**
          * Allow to add condition fields to object.
          * 
          * @param condsFields An array list of string that contains the condition fields for this object.
          */
         public void addConditionFields(ArrayList<String> condsFields) {
                condsCampos.add(condsFields);
                obj.add("condsCampos", gson.toJsonTree(condsCampos).getAsJsonArray());
         }
         
         /**
          * Allow to add condition values to object.
          * 
          * @param condsValues An array list of string that contains the condition fields for this object.
          */
         public void addConditionValues(ArrayList<String> condsValues) {
                condsValores.add(condsValues);
                obj.add("condsValores", gson.toJsonTree(condsValores).getAsJsonArray());
         }
         
         /**
          * Allow to set action in specific index.
          * 
          * @param action The action to be settled.
          * @param index    The index where the action will be settled.
          */
         public void setAction(String action, int index) {
                acciones.set(index, action);
         }
         
         /**
          * Allow to add values to object.
          * 
          * @param values An array list of string that contains the fields for this object.
          */
         public void addValues(ArrayList<String> values) {
                valores.add(values);
                obj.add("valores", gson.toJsonTree(valores).getAsJsonArray());
         }
         
         /**
          * Allow to get the object.
          * 
          * @return An JSON object ready to use.
          */
         public JsonObject getObject() {
                obj.add("acciones", gson.toJsonTree(acciones).getAsJsonArray());
                return obj;
         }
}
