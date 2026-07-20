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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gator.lib.db.GappSQLStatement;
import gator.lib.db.helpers.GappDBHelper;
import gator.lib.db.helpers.GappResponse;
import gator.lib.db.helpers.GappResponses;
import gator.lib.io.external.data.GatorFileInfo;
import gator.lib.io.external.data.GatorFormatoFechas;
import gator.lib.io.files.GappFiles;
import gator.lib.logs.GappLog;
import gator.lib.logs.GappLogging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GatorFileImportProcessor {
        /**
         * Regular expression for commas.
         */
         String otherThanQuote = " [^\"] ";
         String quotedString = String.format(" \" %s* \" ", otherThanQuote);
         String commaRegex = String.format("(?x),(?=(?:%s*%s)*%s*$)",otherThanQuote, quotedString, otherThanQuote);

         /**
          * Regular expression for semicolons.
          */
        String semicolonRegex = String.format("(?x);(?=(?:%s*%s)*%s*$)",otherThanQuote, quotedString, otherThanQuote);

        /**
          * Regular expression for pipes.
          */
        String pipeRegex = String.format("(?x)\\|(?=(?:%s*%s)*%s*$)",otherThanQuote, quotedString, otherThanQuote);

        /**
	 * File manager.
	 */
	private final GappFiles gappFiles = new GappFiles();

        /**
         * Separator.
         */
        private String separator = ",";

        /**
         * Separator name.
         */
        private String separatorName = "::comma::";

        /**
         * Original read lines.
         */
        private String[] lines;

        /**
         * File headers.
         */
        private ArrayList<String> fileHeaders = new ArrayList<>();

        /**
         * Lines
         */
        private final ArrayList<HashMap<String,String>> processLinesAsHashMap = new ArrayList<>();

        /**
 	 * Log instance to do the log.
	 */
	private final GappLogging gappLogger = new GappLogging();
        private final GappLog gappLog = new GappLog();

        /**
	 * DB helper.
	 */
	private final GappDBHelper dbhelper;                

        /**
         * Responses that will be returned by database.
         */
        private GappResponses responses;

        /**
         * Returned integer.
         */
        private String resNum = "-1";

        /**
         * Procedure to execute on success.
         */
        private String procedure = "";
                
        /**
         * Procedure to execute for insert the document.
         */
        private String generador = "";
        
        /**
         * Executor for insert the document.
         */
        private String executor = "";

        /**
         * Real column names.
         */
        private ArrayList<String> columnNames = new ArrayList<>();
        private final ArrayList<ArrayList<String>> values = new ArrayList<>();
        private HashMap<String, String> parametros = new HashMap<>();
        private String parametrosStr = "";

        /**
         * General status of the processed file.
         */
        private String status = ""; 
        
        private String uniqcodeRegExp = "[\ufeff]+";
        
        private GatorFileInfo gatorFileInfo;
        
        /**
         * Default Constructor for this class.
         * @param fileInfo An object with all the information needed.         
         */
        public GatorFileImportProcessor(GatorFileInfo fileInfo) {
                
                gappLog.startNewLog("GatorFileImportProcessor", "Default");
                gappLog.setName("constructor-default");
                gappLog.setFileToLog("import");
                gatorFileInfo = fileInfo;
                gappLogger.logIt(gappLog);
                gappLog.addMessage("with file name", 1);
                dbhelper = new GappDBHelper(gatorFileInfo.getConfFile());                
                gappFiles.setDir(GappFiles.FILES_DIR);
                gappLog.addMessage("Importing file:" + GappFiles.FILES_DIR + gatorFileInfo.getFileName(), 1);                
                String encoding = gappFiles.getCharacterEncoding(GappFiles.FILES_DIR + gatorFileInfo.getFileName()).toUpperCase();
                Pattern utf8Patt = Pattern.compile("^(?i)utf-8");
                if(utf8Patt.matcher(encoding).find()) {
                        encoding = "UTF-8";
                }
                gappLog.addMessage("Encoding:" + encoding, 1);
                
                gappFiles.readFromFile(GappFiles.FILES_DIR + gatorFileInfo.getFileName(), encoding);
                lines = gappFiles.getReadedLines();
                gappLog.addMessage("Lines: " + lines.length, 1);                
                gappLogger.logIt(gappLog);
                detectSeparator();                
                setHeaders();               
                if(resNum.equals("0")) {
                        Gson gson = new Gson();                                        
                        processLines();
                        responses = gson.fromJson(status, GappResponses.class);  
                        gappLog.addMessage("After processing lines about to call afterProcessLine", 1);                
                        gappLogger.logIt(gappLog);                        
                        if(responses.getResponses().get(0).getResNum().equals("0")) {
                                afterProcessLines();
                        }
                }
        }
        
        /**
         * Default Constructor for this class.
         * @param fileInfo An object with all the information needed. 
         * @param isSecond Flag to distinguish the moment of the construction.
         */
        public GatorFileImportProcessor(GatorFileInfo fileInfo, boolean isSecond) {
                gappLog.startNewLog("GatorFileImportProcessor", "GatorFileImportProcessor");
                gappLog.setName("fileinfo-issecond");
                gatorFileInfo = fileInfo;
                gappLogger.logIt("GatorFileImportProcessor", "","import", "system", 0);                
                gappLog.addMessage("Just config", 1);
                dbhelper = new GappDBHelper(gatorFileInfo.getConfFile());
                gappLogger.logIt(gappLog);
        }

        /**
         * Identify the separator used in the file, the separator could be
         * either of the following: (,), (;), (|).
         */
        private void detectSeparator() {
                gappLog.startNewLog("GatorFileImportProcessor", "detectSeparator");
                gappLog.setName("import-detect-separator");
                String tokens[] = lines[0].split(commaRegex);
                String tokens1[] = lines[1].split(commaRegex);
                String realRegex = "";

                if(tokens.length > 1 && tokens.length == tokens1.length) {
                        separator = ",";
                        separatorName = "::comma::";
                        gappLog.addMessage("Sep:" + separator, 1);
                        gappLog.addMessage("Lengths: (" + tokens.length + "), (" + tokens1.length + ")", 1);
                        realRegex = commaRegex;
                }

                tokens = lines[0].split(semicolonRegex);
                tokens1 = lines[1].split(semicolonRegex);
                if(tokens.length > 1 && tokens.length == tokens1.length) {
                        separator = ";";
                        separatorName = "::semicolon::";
                        gappLog.addMessage("Sep:" + separator, 1);
                        gappLog.addMessage("Lengths: (" + tokens.length + "), (" + tokens1.length + ")", 1);
                        realRegex = semicolonRegex;
                }

                tokens = lines[0].split(pipeRegex);
                tokens1 = lines[1].split(pipeRegex);
                if(tokens.length > 1 && tokens.length == tokens1.length) {
                        separator = "|";
                        separatorName = "::pipe::";
                        gappLog.addMessage("Sep:" + separator, 1);
                        gappLog.addMessage("Lengths: (" + tokens.length + "), (" + tokens1.length + ")", 1);
                        realRegex = pipeRegex;
                }
                fileHeaders = new ArrayList(Arrays.asList(lines[0].replaceAll(uniqcodeRegExp, "").split(realRegex)));
                status = "Header detected successfuly";                        
                gappLog.addMessage("Detect separator finished.", 0);  
                gappLogger.logIt(gappLog);
        }
        /**
         * Process file lines.
         */
        private void processLines() {
                gappLog.startNewLog("GatorFileImportProcessor", "processLines");
                gappLog.setName("import-process-lines");
                String lineSplited[];
                HashMap<String, String> procLineAsHash;
                ArrayList<String> tmpLine = new ArrayList<>();
                int lineNumber = -1;
                String line = "";
                try{
                        gappLog.addMessage("Process Lines started.", 0);
                        gappLogger.logIt(gappLog);
	                if(separatorName.equals("::comma::")) {
	                        for(int i = 1; i < lines.length; i++) {
	                                lineNumber = i;
	                                line = lines[i];
	                                lineSplited = lines[i].replaceAll(uniqcodeRegExp, "").split(commaRegex);
	                                procLineAsHash = new HashMap<>();
                                        tmpLine = new ArrayList(Arrays.asList(lineSplited));	                                
	                                for(int j = 0; j < columnNames.size(); j++){
	                                        procLineAsHash.put(columnNames.get(j), lineSplited[j]);;
	                                }
	                                processLinesAsHashMap.add(procLineAsHash);
                                        values.add(tmpLine);
	                        }
	                }
	                if(separatorName.equals("::semicolon::")) {
	                        for(int i = 1; i < lines.length; i++) {
	                                lineNumber = i;
	                                line = lines[i];
	                                lineSplited = lines[i].replaceAll(uniqcodeRegExp, "").split(semicolonRegex);
	                                procLineAsHash = new HashMap<>();
                                        tmpLine = new ArrayList(Arrays.asList(lineSplited));
	                                for(int j = 0; j < columnNames.size(); j++){
	                                        procLineAsHash.put(columnNames.get(j), lineSplited[j]);
	                                }
	                                processLinesAsHashMap.add(procLineAsHash);
                                        values.add(tmpLine);
	                        }
	                }
	                if(separatorName.equals("::pipe::")) {
	                        for(int i = 1; i < lines.length; i++) {
	                                lineNumber = i;
	                                line = lines[i];
	                                lineSplited = lines[i].replaceAll(uniqcodeRegExp, "").split(pipeRegex);
	                                procLineAsHash = new HashMap<>();
                                        tmpLine = new ArrayList(Arrays.asList(lineSplited));
	                                for(int j = 0; j < columnNames.size(); j++){
	                                        procLineAsHash.put(columnNames.get(j), lineSplited[j]);
	                                }
	                                processLinesAsHashMap.add(procLineAsHash);
                                        values.add(tmpLine);
	                        }
	                }                        
			status = "{\"responses\" : [{\"resNum\" : \"0\", \"response\" : {\"message\" : \"Lines processed successfuly\"}}]}";                        
                        gappLog.addMessage("Process Lines finished.", 0);
                        gappLogger.logIt(gappLog);
                } catch (Exception e) {
                        gappLog.addMessage("Error at line:" + lineNumber, 2);
                        gappLog.addMessage("Error at data:" + line, 2);
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);  
                        gappLogger.logIt(gappLog);
			status = "{\"responses\" : [{\"resNum\" : \"-1\", \"response\" : {\"message\" : \"Error at line:" + lineNumber + " " + "line:" + line + "\"}}]}";
                }
        }

        /**
         * Returns the processed lines ready to manage.
         *
         * @return Processed lines.
         */
        /*public ArrayList<ArrayList<GatorKeyValue>> getProcessLines() {
                return processLines;
        }*/

        /**
         * Returns the processed lines ready to manage.
         *
         * @return Processed lines.
         */
        public ArrayList<HashMap<String,String>> getProcessLinesAsHashMap() {
                return processLinesAsHashMap;
        }
        /**
         * Check if process exists and get headers.
         */
        private void setHeaders() {
                gappLog.startNewLog("GatorFileImportProcessor", "setHeaders");
                gappLog.setName("import-set-header");
                Gson gson = new Gson();
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("cuentaId", gatorFileInfo.getAccount());
                jsonObj.addProperty("bodegaId", gatorFileInfo.getWarehouse());
                jsonObj.addProperty("debugLevel", 0);
                jsonObj.addProperty("usuario",  gatorFileInfo.getUser());
                jsonObj.addProperty("sesionId",  gatorFileInfo.getSeionId());
                jsonObj.addProperty("idctrl",  gatorFileInfo.getIdctrl());
                JsonArray jsonArray = gson.toJsonTree(fileHeaders).getAsJsonArray();
                jsonObj.add("headers", jsonArray);
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure("app_fn_get_proceso2");
                gappSQLStmt.addParam(gson.toJson(jsonObj));                
                String jsonProcess = dbhelper.executeStore(gappSQLStmt);
                status = jsonProcess;
                responses = gson.fromJson(jsonProcess, GappResponses.class);
                resNum = responses.getResponses().get(0).getResNum();
                columnNames = (ArrayList<String>)responses.getResponses().get(0).getResponse().get("columnas");
                procedure = (String) responses.getResponses().get(0).getResponse().get("procedimiento");
                generador = (String) responses.getResponses().get(0).getResponse().get("generador");
                executor = (String) responses.getResponses().get(0).getResponse().get("ejecutor");		   
                parametrosStr = gson.toJson(responses.getResponses().get(0).getResponse().get("parametros"));
                parametros = gson.fromJson(parametrosStr, HashMap.class);
                gappLog.addMessage("Set headers done.", 1);
                gappLogger.logIt(gappLog);                
        }
        /**
         * Check if process exists and get headers.
         */
        private void afterProcessLines() {
                gappLog.startNewLog("GatorFileImportProcessor", "afterProcessLines");
                gappLog.setName("import-after-process");
                gappLog.addMessage("afterProcessLines started.", 0);                        
                gappLogger.logIt(gappLog);
                Gson gson = new Gson();                
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("cuentaId", gatorFileInfo.getAccount());
                jsonObj.addProperty("bodegaId", gatorFileInfo.getWarehouse());
                jsonObj.addProperty("debugLevel", 0);
                jsonObj.addProperty("usuario",  gatorFileInfo.getUser());
                jsonObj.addProperty("sesionId",  gatorFileInfo.getSeionId());
                jsonObj.addProperty("idctrl",  gatorFileInfo.getIdctrl());
                jsonObj.addProperty("parametros",  parametrosStr);
                JsonArray colsAsJsonArr = gson.toJsonTree(columnNames).getAsJsonArray();
                jsonObj.add("columnas", colsAsJsonArr);
                JsonArray jsonArrayAsHM = gson.toJsonTree(processLinesAsHashMap).getAsJsonArray();                
                jsonObj.add("registrosArray", jsonArrayAsHM);
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure(procedure);
                gappSQLStmt.addParam(gson.toJson(jsonObj));                
                String jsonProcess = dbhelper.executeStore(gappSQLStmt);
                responses = gson.fromJson(jsonProcess, GappResponses.class);
                status = jsonProcess;                                
                gappLog.addMessage("Verifying lines done.", 0);
                gappLogger.logIt(gappLog);
                if(responses.getResponses().get(0).getResNum().equals("0")) {			
			String tabla = (String) responses.getResponses().get(0).getResponse().get("tabla");
                        String conds = gson.toJson(responses.getResponses().get(0).getResponse().get("condiciones"));
                        ArrayList<String> condiciones = gson.fromJson(conds, ArrayList.class);
	                String refs = gson.toJson(responses.getResponses().get(0).getResponse().get("referencias"));
                        String formatos = gson.toJson(responses.getResponses().get(0).getResponse().get("formatos"));                        
	                ArrayList<String> referencias = gson.fromJson(refs, ArrayList.class);
                        GatorFormatoFechas formatoFechas = gson.fromJson(formatos, GatorFormatoFechas.class);
                        addImport(tabla, condiciones, referencias, formatoFechas);                        
                }
        }
	/**
	 * Insert the registries to database.
	 * 
	 */
	private void addImport(String tabla, ArrayList<String> condiciones, ArrayList<String> referencias, GatorFormatoFechas formatoFechas) {
                gappLog.startNewLog("GatorFileImportProcessor", "addImport");
                gappLog.setName("import-add-import");                
                gappLog.addMessage("addImport started.", 0);                        
                gappLogger.logIt(gappLog);
                Gson gson = new Gson();
                if(formatoFechas == null) {
                    formatoFechas = new GatorFormatoFechas();
                } 
                ExecutorService pool = Executors.newFixedThreadPool(600);  
                ArrayList<String> tmpList = columnNames;
                ArrayList<String> value;
                tmpList.add("cuenta_id");
                tmpList.add("bodega_id");
                tmpList.add("idctrl");
                tmpList.add("sesion_id");
                tmpList.add("usuario_id");                
                for(int i = 0; i < referencias.size(); i++) {
                        final GatorAdmonTablas  admonTablas0 = new GatorAdmonTablas();
                        admonTablas0.setDebugLevel("0");
                        admonTablas0.addTable(tabla);
                        if(i == 0) {
                            admonTablas0.addAccion("baja");
                        } else {
                            admonTablas0.addAccion("alta");
                        }
                        admonTablas0.setIdCtrl(gatorFileInfo.getIdctrl());
                        admonTablas0.addFields(tmpList);
                        admonTablas0.addConditionFields(condiciones);
                        admonTablas0.setUsuarioId(gatorFileInfo.getUser());
                        admonTablas0.setSesionId(gatorFileInfo.getSeionId());
                        value = values.get(i);
                        value.add(gatorFileInfo.getAccount());
                        value.add(gatorFileInfo.getWarehouse());
                        value.add(gatorFileInfo.getIdctrl());
                        value.add(gatorFileInfo.getSeionId());
                        value.add(gatorFileInfo.getUser());
                        if(formatoFechas.haveFormats()) {
                                for(int columna: formatoFechas.getLinea(i).getColumnas()){                        
                                        gappLog.addMessage("Formatos columna: " + columna, 1);                                     
                                        gappLog.addMessage("Formatos formato: " + formatoFechas.getLinea(i).getFormato(columna), 1);
                                        String tmp = value.get(columna);
                                        gappLog.addMessage("Formatos valor: " + tmp, 1);
                                        if(tmp.isEmpty()) {
                                                value.set(columna, "::fn::null::fn::");
                                        } else {                                        
                                                value.set(columna, "::fn::to_timestamp(::quote::" + tmp + "::quote::,::quote::" + formatoFechas.getLinea(i).getFormato(columna) + "::quote::)::fn::");
                                        }
                                }
                        }
                        admonTablas0.addValues(value);                        
                        admonTablas0.addConditionValues(new ArrayList<>(Arrays.asList("::in::" + referencias.get(i) + "::in::","::noteq::1::noteq::")));                                
                        
                        if(i == 0) {
                            doImport(gson.toJson(admonTablas0.getObject()));
                        } else {
                            pool.execute(() -> {                            
                                doImport(gson.toJson(admonTablas0.getObject()));
                            });                            
                        }
                        
                }
                gappLog.addMessage("Importing done.", 0);
                gappLogger.logIt(gappLog);                
                pool.shutdown();
	}
        private synchronized void doImport(String param) {
                Gson gson = new Gson();               
                String jsonProcess;
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure("app_fn_add_import");
                gappSQLStmt.addParam(param);
                jsonProcess = dbhelper.executeStoreSync(gappSQLStmt);					
                responses = gson.fromJson(jsonProcess, GappResponses.class);
                status = jsonProcess;
        }
        /**
         * Get the status for this processor.
         * @return JSON string with the status.
         */
        public String getStatus() {
                gappLog.startNewLog("GatorFileImportProcessor", "getStatus");
                gappLog.setName("import-get-status");
                Gson gson = new Gson();
                gappLog.addMessage("Importing getting status:" + status, 1);
                gappLogger.logIt(gappLog);
                GappResponses obj = gson.fromJson(status, GappResponses.class);
                obj.getResponses().get(0).getResponse().put("generador", generador);
                obj.getResponses().get(0).getResponse().put("parametros", parametros);
                obj.getResponses().get(0).getResponse().put("ejecutor", executor);
                return gson.toJson(obj);
        }
        /**
         * Get the status as a GappReponse object.
         * @return The status as a response object.
         */
        public GappResponse getStatusAsResponse() {
                gappLog.startNewLog("GatorFileImportProcessor", "getStatusAsResponse");
                gappLog.setName("import-get-status");
                Gson gson = new Gson();
                gappLog.addMessage("Importing getting status:" + status, 1);
                gappLogger.logIt(gappLog);                
                GappResponses obj = gson.fromJson(status, GappResponses.class);
                obj.getResponses().get(0).getResponse().put("generador", generador);
                obj.getResponses().get(0).getResponse().put("parametros", parametros);
                obj.getResponses().get(0).getResponse().put("ejecutor", executor);
                return obj.getResponses().get(0);
        }
        /**
         * Allow to execute the generator of this process.
         * 
         * @param generador A string representing the generator.
         * @param parametros A string representing the parameters as JSON object.
         */
        public void execGenerator(String generador, String parametros) {
                gappLog.startNewLog("GatorFileImportProcessor", "execGenerator");
                gappLog.setName("import-exec-generator");
                Gson gson = new Gson();
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("cuentaId", gatorFileInfo.getAccount());
                jsonObj.addProperty("bodegaId", gatorFileInfo.getWarehouse());
                jsonObj.addProperty("debugLevel", 0);                
                jsonObj.addProperty("usuario",  gatorFileInfo.getUser());
                jsonObj.addProperty("sesionId",  gatorFileInfo.getSeionId());
                jsonObj.addProperty("idctrl",  gatorFileInfo.getIdctrl());
                JsonObject jsonParametros = gson.fromJson(parametros, JsonObject.class);
                jsonObj.add("parametros", jsonParametros);
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure(generador);
                gappSQLStmt.addParam(gson.toJson(jsonObj));
                String jsonProcess = dbhelper.executeStore(gappSQLStmt);
                responses = gson.fromJson(jsonProcess, GappResponses.class);                
		status = jsonProcess;  
                gappLog.addMessage("Execute generator done.", 1);
                gappLogger.logIt(gappLog);                
        }
        /**
         * Allow to execute the generator of this process.
         * 
         * @param generador A string representing the generator.
         * @param parametros A string representing the parameters as JSON object.
         */
        public void execGenerator3(String generador, String parametros) {
                gappLog.startNewLog("GatorFileImportProcessor", "execGenerator3");
                gappLog.setName("import-exec-generator3");
	        Gson gson = new Gson();
	        JsonObject jsonObj;
	        JsonObject jsonParametros = gson.fromJson(parametros, JsonObject.class);
	        GappSQLStatement gappSQLStmt;
	        String jsonProcess;
	       	ArrayList<HashMap<String,String>> procedimientos;
                boolean goesOn = true;
                ExecutorService pool = Executors.newFixedThreadPool(2400);
		int limit = 600, offset = 0;
                while(goesOn) {
	                jsonObj = new JsonObject();
	                jsonObj.addProperty("cuentaId", gatorFileInfo.getAccount());
	                jsonObj.addProperty("bodegaId", gatorFileInfo.getWarehouse());
	                jsonObj.addProperty("debugLevel", 0);
	                jsonObj.addProperty("usuario",  gatorFileInfo.getUser());
                        jsonObj.addProperty("sesionId",  gatorFileInfo.getSeionId());
                        jsonObj.addProperty("idctrl",  gatorFileInfo.getIdctrl());
	                jsonObj.addProperty("limit",  limit);
	                jsonObj.addProperty("offset",  offset);
	                jsonObj.add("parametros", jsonParametros);  

			offset += limit;

	                gappSQLStmt = new GappSQLStatement();
	                gappSQLStmt.setStoreProcedure(generador);
	                gappSQLStmt.addParam(gson.toJson(jsonObj));

	                jsonProcess = dbhelper.executeStoreSync(gappSQLStmt);
	                
	                responses = gson.fromJson(jsonProcess, GappResponses.class);  
	                status = jsonProcess;
	                //gappLogger.logIt("execGenerator3", "Execute generator 3 done.","import", "system", 0);

	                procedimientos = (ArrayList) responses.getResponses().get(0).getResponse().get("procedimientos");
	                
	                if(procedimientos == null) {                                
	                        procedimientos = new ArrayList<>();
	                        goesOn = false;
	                }
                        gappLog.addMessage("Procedimientos:" + procedimientos.size(), 1);                        
	                for(int i = 0; i < procedimientos.size(); i++) {
	                        String tmpProc = gson.toJson(procedimientos.get(i));
	                        HashMap<String,String> proc = gson.fromJson(tmpProc, HashMap.class);
	                        String isFirst = proc.get("amIFirst") == null?"0":proc.get("amIFirst");
	                        final String procedimiento = proc.get("procedimiento");
	                        final String params = tmpProc;
	                        if(i == 0 || isFirst.equals("1")) {
	                                doGenerate(procedimiento, params);
	                        } else {                                        
	                                pool.execute(new Runnable(){                                            
	                                        public void run(){                                                
	                                                doGenerate(procedimiento, params);
	                                        }                                        
	                                });
	                        }
                                gappLog.addMessage("procedimientos (i): " + i + ", (offset):" + offset, 1);                                
	                }
                }                
                gappLogger.logIt(gappLog);
                pool.shutdown();
        }
        /**
         * Allow to execute the generator of this process.
         * 
         * @param generador A string representing the generator.
         * @param parametros A string representing the parameters as JSON object.
         */
        public void execGenerator2(String generador, String parametros) {
                gappLog.startNewLog("GatorFileImportProcessor", "execGenerator2");
                gappLog.setName("import-exec-generator2");
                Gson gson = new Gson();
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("cuentaId", gatorFileInfo.getAccount());
                jsonObj.addProperty("bodegaId", gatorFileInfo.getWarehouse());
                jsonObj.addProperty("debugLevel", 0);
                jsonObj.addProperty("usuario",  gatorFileInfo.getUser());                        
                jsonObj.addProperty("sesionId",  gatorFileInfo.getSeionId());                        
                jsonObj.addProperty("idctrl",  gatorFileInfo.getIdctrl());
                JsonObject jsonParametros = gson.fromJson(parametros, JsonObject.class);
                jsonObj.add("parametros", jsonParametros);  
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure(generador);
                gappSQLStmt.addParam(gson.toJson(jsonObj));
                String jsonProcess = dbhelper.executeStore(gappSQLStmt);
                gappLog.addMessage("Execute generator2 result:" + jsonProcess, 1);

                responses = gson.fromJson(jsonProcess, GappResponses.class);  
                status = jsonProcess;
                
                gappLog.addMessage("About to start pool", 1);
                ArrayList<HashMap<String,String>> procedimientos = (ArrayList)responses.getResponses().get(0).getResponse().get("procedimientos");
                ExecutorService pool = Executors.newFixedThreadPool(50);
                if(procedimientos == null) {
                        procedimientos = new ArrayList<>();
                        
                }
                for(int i = 0; i < procedimientos.size(); i++) {
                        String tmpProc = gson.toJson(procedimientos.get(i));
                        HashMap<String,String> proc = gson.fromJson(tmpProc, HashMap.class);
                        String isFirst = proc.get("amIFirst");
                        final String procedimiento = proc.get("procedimiento");
                        final String params = tmpProc;
                        if(i == 0 || isFirst.equals("1")) {
                                doGenerate(procedimiento, params);
                        } else {
                                pool.execute(new Runnable(){                                            
                                        public void run(){                                                
                                                doGenerate(procedimiento, params);
                                        }                                        
                                });
                        }
                }
                gappLog.addMessage("Execute generator done.", 1);
                gappLogger.logIt(gappLog);
                pool.shutdown();
        }
        /**
         * Allow to retrieve the generator for this process.
         * 
         * @return A string representing the generator.
         */
        public String getGenerador() {
                return this.generador;
        }
        /**
         * Allow to execute the generator of this process.
         * 
         * @param procedimiento A string representing the generator.
         * @param parametros A string representing the parameters as JSON object.
         */
        public synchronized void doGenerate(String procedimiento, String parametros) { 
                gappLog.startNewLog("GatorFileImportProcessor", "doGenerate");
                gappLog.setName("import-exec-doGenerate");
                GappSQLStatement gappSQLStmt = new GappSQLStatement();
                gappSQLStmt.setStoreProcedure(procedimiento);
                gappSQLStmt.addParam(parametros);                
                String jsonProcess = dbhelper.executeStoreSync(gappSQLStmt);
                gappLog.addMessage("respuesta: " + jsonProcess, 1); 
        }
        /**
         * Allow to retrieve the generator for this process.
         * 
         * @return A string representing the parameters.
         */
        public String getParametros() {
                return this.parametrosStr;
        }
        /**
         * Allow to retrieve the generator for this process.
         * 
         * @return A string representing the parameters.
         */
        public String getEjecutor() {
                return this.executor;
        }
}
