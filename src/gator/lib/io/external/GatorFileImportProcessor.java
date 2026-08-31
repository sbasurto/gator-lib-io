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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author <a href="mailto:sbasurto@soft-gator.com">Sergio Basurto Juárez</a>
 */
public class GatorFileImportProcessor {
        private static final int IMPORT_BATCH_SIZE = 500;
        static final ThreadPoolExecutor IMPORT_POOL = new ThreadPoolExecutor(
                0,
                100,
                60,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                runnable -> {
                        Thread thread = new Thread(runnable, "gator-file-import");
                        thread.setDaemon(true);
                        return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());

        /**
         * Separator.
         */
        private char separator = ',';

        /**
         * Parsed records.
         */
        private List<List<String>> records = List.of();

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
        private Map<String, Object> parametros = new HashMap<>();
        private String parametrosStr = "";

        /**
         * General status of the processed file.
         */
        private String status = ""; 
        
        private GatorFileInfo gatorFileInfo;

        private static final Pattern PROCEDURE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_.$#]*");
        
        /**
         * Default Constructor for this class.
         * @param fileInfo An object with all the information needed.         
         */
        public GatorFileImportProcessor(GatorFileInfo fileInfo) {
                
                gappLog.startNewLog("GatorFileImportProcessor", "Default");
                gappLog.setName("constructor-default");
                gappLog.setFileToLog("import");
                gatorFileInfo = requireFileInfo(fileInfo, true);
                gappLogger.logIt(gappLog);
                gappLog.addMessage("with file name", 1);
                dbhelper = new GappDBHelper(gatorFileInfo.getConfFile());                
                try {
                        Path file = validatedFile();
                        gappLog.addMessage("Importing file:" + file, 1);
                        String encoding = detectEncoding(file);
                        gappLog.addMessage("Encoding:" + encoding, 1);

                        detectSeparator(Files.readString(file, Charset.forName(encoding)));
                        require(records.size() >= 2, "El archivo debe contener encabezado y al menos una fila");
                        gappLog.addMessage("Records: " + records.size(), 1);
                        gappLogger.logIt(gappLog);
                        setHeaders();
                        if(resNum.equals("0")) {
                                processLines();
                                responses = parseResponses(status, "procesamiento de filas");
                                gappLog.addMessage("After processing lines about to call afterProcessLine", 1);
                                gappLogger.logIt(gappLog);
                                if(firstResponse(responses, "procesamiento de filas").getResNum().equals("0")) {
                                        afterProcessLines();
                                }
                        }
                } catch (Exception e) {
                        setErrorStatus(e.getMessage());
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);
                        gappLogger.logIt(gappLog);
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
                gappLog.setFileToLog("import");
                gatorFileInfo = requireFileInfo(fileInfo, false);
                gappLogger.logIt("GatorFileImportProcessor", "","import", "system", 0);                
                gappLog.addMessage("Just config", 1);
                dbhelper = new GappDBHelper(gatorFileInfo.getConfFile());
                gappLogger.logIt(gappLog);
        }

        /**
         * Identify the separator used in the file, the separator could be
         * either of the following: (,), (;), (|).
         */
        private void detectSeparator(String content) {
                gappLog.startNewLog("GatorFileImportProcessor", "detectSeparator");
                gappLog.setName("import-detect-separator");
                char[] separators = {',', ';', '|'};
                int bestColumns = 1;
                int bestIndex = -1;
                boolean ambiguous = false;
                List<List<String>> bestRecords = List.of();
                IllegalArgumentException parseError = null;
                for(int i = 0; i < separators.length; i++) {
                        List<List<String>> candidate;
                        try {
                                candidate = parseDelimited(content, separators[i]);
                        } catch(IllegalArgumentException e) {
                                if(parseError == null) parseError = e;
                                continue;
                        }
                        if(candidate.size() < 2) continue;
                        int headerColumns = candidate.get(0).size();
                        int firstRowColumns = candidate.get(1).size();
                        if(headerColumns > bestColumns && headerColumns == firstRowColumns) {
                                bestColumns = headerColumns;
                                bestIndex = i;
                                bestRecords = candidate;
                                ambiguous = false;
                        } else if(headerColumns == bestColumns && headerColumns > 1 && headerColumns == firstRowColumns) {
                                ambiguous = true;
                        }
                }
                if(bestIndex < 0 && parseError != null) throw parseError;
                require(bestIndex >= 0, "No se detectó un separador válido");
                require(!ambiguous, "El separador del archivo es ambiguo");
                separator = separators[bestIndex];
                records = bestRecords;
                fileHeaders = new ArrayList<>(records.get(0));
                if(!fileHeaders.isEmpty()) {
                        fileHeaders.set(0, fileHeaders.get(0).replaceFirst("^\ufeff", ""));
                }
                require(fileHeaders.stream().noneMatch(String::isBlank), "El encabezado contiene columnas vacías");
                require(new HashSet<>(fileHeaders).size() == fileHeaders.size(), "El encabezado contiene columnas duplicadas");
                gappLog.addMessage("Sep:" + separator, 1);
                gappLog.addMessage("Columns:" + bestColumns, 1);
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
                HashMap<String, String> procLineAsHash;
                ArrayList<String> tmpLine = new ArrayList<>();
                int lineNumber = -1;
                String line = "";
                try{
                        gappLog.addMessage("Process Lines started.", 0);
                        gappLogger.logIt(gappLog);
                        for(int i = 1; i < records.size(); i++) {
                                lineNumber = i;
                                List<String> record = records.get(i);
                                line = record.toString();
                                require(!(record.size() == 1 && record.get(0).isBlank()),
                                        "El registro " + (i + 1) + " está vacío");
                                require(record.size() == columnNames.size(),
                                        "El registro " + (i + 1) + " tiene " + record.size()
                                        + " columnas; se esperaban " + columnNames.size());
                                procLineAsHash = new HashMap<>();
                                tmpLine = new ArrayList<>(record);
                                for(int j = 0; j < columnNames.size(); j++){
                                        procLineAsHash.put(columnNames.get(j), record.get(j));
                                }
                                processLinesAsHashMap.add(procLineAsHash);
                                values.add(tmpLine);
                        }
			status = "{\"responses\" : [{\"resNum\" : \"0\", \"response\" : {\"message\" : \"Lines processed successfuly\"}}]}";                        
                        gappLog.addMessage("Process Lines finished.", 0);
                        gappLogger.logIt(gappLog);
                } catch (Exception e) {
                        gappLog.addMessage("Error at line:" + lineNumber, 2);
                        gappLog.addMessage("Error at data:" + line, 2);
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);  
                        gappLogger.logIt(gappLog);
                        setErrorStatus("Error en línea " + (lineNumber + 1) + ": " + line);
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
                jsonObj.addProperty("tipoProceso", gatorFileInfo.getProcessType());
                JsonArray jsonArray = gson.toJsonTree(fileHeaders).getAsJsonArray();
                jsonObj.add("headers", jsonArray);
                String jsonProcess = executeStore(dbhelper, "app_fn_get_proceso2", gson.toJson(jsonObj));
                status = jsonProcess;
                responses = parseResponses(jsonProcess, "app_fn_get_proceso2");
                GappResponse response = firstResponse(responses, "app_fn_get_proceso2");
                resNum = response.getResNum();
                if(!resNum.equals("0")) return;
                columnNames = requireStringList(response.getResponse().get("columnas"), "columnas");
                require(columnNames.size() == fileHeaders.size(),
                        "La configuración devuelve " + columnNames.size()
                        + " columnas; el archivo contiene " + fileHeaders.size());
                require(new HashSet<>(columnNames).size() == columnNames.size(),
                        "La configuración contiene columnas duplicadas");
                procedure = requireProcedure(response.getResponse().get("procedimiento"), "procedimiento");
                generador = optionalProcedure(response.getResponse().get("generador"), "generador");
                executor = optionalString(response.getResponse().get("ejecutor"));
                Object responseParameters = response.getResponse().get("parametros");
                require(responseParameters == null || responseParameters instanceof Map<?, ?>,
                        "parametros debe ser un objeto");
                parametrosStr = gson.toJson(responseParameters == null ? Map.of() : responseParameters);
                parametros = new HashMap<>();
                if(responseParameters instanceof Map<?, ?> map) {
                        for(Map.Entry<?, ?> entry : map.entrySet()) {
                                require(entry.getKey() instanceof String,
                                        "Las claves de parametros deben ser texto");
                                parametros.put((String) entry.getKey(), entry.getValue());
                        }
                }
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
                String jsonProcess = executeStore(dbhelper, procedure, gson.toJson(jsonObj));
                responses = parseResponses(jsonProcess, procedure);
                status = jsonProcess;
                gappLog.addMessage("Verifying lines done.", 0);
                gappLogger.logIt(gappLog);
                GappResponse response = firstResponse(responses, procedure);
                if(response.getResNum().equals("0")) {
                        String tabla = requireString(response.getResponse().get("tabla"), "tabla");
                        ArrayList<String> condiciones = requireStringList(response.getResponse().get("condiciones"), "condiciones");
                        require(condiciones.size() == 2,
                                "La validación debe devolver exactamente dos condiciones");
                        ArrayList<String> referencias = requireStringList(response.getResponse().get("referencias"), "referencias");
                        require(referencias.size() == values.size(),
                                "La validación devolvió " + referencias.size()
                                + " referencias para " + values.size() + " filas");
                        Object formatos = response.getResponse().get("formatos");
                        GatorFormatoFechas formatoFechas = formatos == null
                                ? new GatorFormatoFechas()
                                : gson.fromJson(gson.toJson(formatos), GatorFormatoFechas.class);
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
                ArrayList<String> tmpList = new ArrayList<>(columnNames);
                tmpList.add("cuenta_id");
                tmpList.add("bodega_id");
                tmpList.add("idctrl");
                tmpList.add("sesion_id");
                tmpList.add("usuario_id");                
                ArrayList<Callable<String>> imports = new ArrayList<>();
                for(int start = 0; start < referencias.size();) {
                        final GatorAdmonTablas  admonTablas0 = new GatorAdmonTablas();
                        admonTablas0.setDebugLevel("0");
                        admonTablas0.setIdCtrl(gatorFileInfo.getIdctrl());
                        admonTablas0.setUsuarioId(gatorFileInfo.getUser());
                        admonTablas0.setSesionId(gatorFileInfo.getSeionId());
                        int end = importBatchEnd(start, referencias.size());
                        for(int i = start; i < end; i++) {
                                admonTablas0.addTable(tabla);
                                admonTablas0.addAccion(i == 0 ? "baja" : "alta");
                                admonTablas0.addFields(tmpList);
                                admonTablas0.addConditionFields(condiciones);
                                ArrayList<String> value = new ArrayList<>(values.get(i));
                                value.add(gatorFileInfo.getAccount());
                                value.add(gatorFileInfo.getWarehouse());
                                value.add(gatorFileInfo.getIdctrl());
                                value.add(gatorFileInfo.getSeionId());
                                value.add(gatorFileInfo.getUser());
                                if(formatoFechas.haveFormats()) {
                                        require(i < formatoFechas.size(),
                                                "Falta la configuración de fechas para la fila " + (i + 1));
                                        for(int columna: formatoFechas.getLinea(i).getColumnas()){
                                                require(columna >= 0 && columna < value.size(),
                                                        "Columna de fecha inválida: " + columna);
                                                String formato = requireText(
                                                        formatoFechas.getLinea(i).getFormato(columna),
                                                        "formato de fecha para columna " + columna);
                                                String tmp = value.get(columna);
                                                value.set(columna, tmp.isEmpty()
                                                        ? "::fn::null::fn::"
                                                        : "::fn::to_timestamp(::quote::" + tmp + "::quote::,::quote::" + formato + "::quote::)::fn::");
                                        }
                                }
                                admonTablas0.addValues(value);
                                admonTablas0.addConditionValues(new ArrayList<>(Arrays.asList(
                                        "::in::" + referencias.get(i) + "::in::", "::noteq::1::noteq::")));
                        }
                        String importParam = gson.toJson(admonTablas0.getObject());
                        if(start == 0) {
                                status = executeImport(importParam);
                                successfulResponse(status, "app_fn_add_import");
                        } else {
                                imports.add(() -> executeImport(importParam));
                        }
                        start = end;
                }
                for(String result : runParallel(imports)) {
                        successfulResponse(result, "app_fn_add_import");
                        status = result;
                }
                gappLog.addMessage("Importing done.", 0);
                gappLogger.logIt(gappLog);                
		}

        static int importBatchEnd(int start, int rows) {
                return Math.min(rows, start == 0 ? 1 : start + IMPORT_BATCH_SIZE);
        }

        private String executeImport(String param) {
                return executeStore(new GappDBHelper(gatorFileInfo.getConfFile()),
                        "app_fn_add_import", param);
        }

        static List<List<String>> parseDelimited(String content, char delimiter) {
                ArrayList<List<String>> parsed = new ArrayList<>();
                ArrayList<String> record = new ArrayList<>();
                StringBuilder field = new StringBuilder();
                boolean quoted = false;
                boolean closedQuote = false;
                boolean touched = false;
                for(int i = 0; i < content.length(); i++) {
                        char current = content.charAt(i);
                        if(quoted) {
                                if(current == '"' && i + 1 < content.length()
                                        && content.charAt(i + 1) == '"') {
                                        field.append('"');
                                        i++;
                                } else if(current == '"') {
                                        quoted = false;
                                        closedQuote = true;
                                } else {
                                        field.append(current);
                                }
                                continue;
                        }
                        if(current == '"') {
                                require(field.isEmpty() && !closedQuote,
                                        "Comilla inesperada en campo sin comillas");
                                quoted = true;
                                touched = true;
                        } else if(current == delimiter) {
                                record.add(field.toString());
                                field.setLength(0);
                                closedQuote = false;
                                touched = true;
                        } else if(current == '\r' || current == '\n') {
                                if(current == '\r' && i + 1 < content.length()
                                        && content.charAt(i + 1) == '\n') i++;
                                record.add(field.toString());
                                parsed.add(record);
                                record = new ArrayList<>();
                                field.setLength(0);
                                closedQuote = false;
                                touched = false;
                        } else {
                                require(!closedQuote, "Contenido inesperado después de cerrar comillas");
                                field.append(current);
                                touched = true;
                        }
                }
                require(!quoted, "Campo con comillas sin cerrar");
                if(touched || !record.isEmpty()) {
                        record.add(field.toString());
                        parsed.add(record);
                }
                return parsed;
        }

        static GatorFileInfo requireFileInfo(GatorFileInfo fileInfo, boolean fileRequired) {
                require(fileInfo != null, "La información del archivo es obligatoria");
                if(fileRequired) requireText(fileInfo.getFileName(), "fileName");
                requireText(fileInfo.getConfFile(), "confFile");
                requireText(fileInfo.getAccount(), "cuentaId");
                requireText(fileInfo.getWarehouse(), "bodegaId");
                requireText(fileInfo.getUser(), "usuario");
                requireText(fileInfo.getIdctrl(), "idctrl");
                requireText(fileInfo.getSeionId(), "sesionId");
                return fileInfo;
        }

        private Path validatedFile() {
                try {
                        Path root = Path.of(GappFiles.FILES_DIR).toRealPath();
                        Path file = root.resolve(gatorFileInfo.getFileName()).normalize();
                        require(file.startsWith(root), "El archivo debe estar dentro de " + root);
                        file = file.toRealPath();
                        require(file.startsWith(root), "El archivo no puede ser un enlace fuera de " + root);
                        require(Files.isRegularFile(file), "No existe el archivo: " + file);
                        require(Files.isReadable(file), "No se puede leer el archivo: " + file);
                        return file;
                } catch(java.io.IOException e) {
                        throw new IllegalArgumentException("No se puede resolver el archivo", e);
                }
        }

        private String detectEncoding(Path file) {
                try {
                        Process process = new ProcessBuilder("chardetect", file.toString())
                                .redirectErrorStream(true).start();
                        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        if(process.waitFor() == 0) {
                                var matcher = Pattern.compile(":\\s*([^\\s]+)").matcher(output);
                                if(matcher.find()) return Charset.forName(matcher.group(1)).name();
                        }
                } catch(Exception ignored) {
                        // UTF-8 is the safe fallback when chardetect is unavailable.
                }
                return StandardCharsets.UTF_8.name();
        }

        static GappResponses parseResponses(String json, String operation) {
                requireText(json, "respuesta de " + operation);
                try {
                        return new Gson().fromJson(json, GappResponses.class);
                } catch(Exception e) {
                        throw new IllegalStateException("Respuesta inválida de " + operation, e);
                }
        }

        private static GappResponse firstResponse(GappResponses parsed, String operation) {
                require(parsed != null && parsed.getResponses() != null
                        && !parsed.getResponses().isEmpty(),
                        "La respuesta de " + operation + " no contiene responses");
                GappResponse response = parsed.getResponses().get(0);
                require(response != null, "La respuesta de " + operation + " es nula");
                requireText(response.getResNum(), "resNum de " + operation);
                return response;
        }

        private static GappResponse successfulResponse(String json, String operation) {
                GappResponse response = firstResponse(parseResponses(json, operation), operation);
                require("0".equals(response.getResNum()),
                        operation + " falló: " + optionalString(response.getResponse().get("message")));
                return response;
        }

        private static String requireString(Object value, String name) {
                require(value instanceof String, name + " debe ser texto");
                return requireText((String) value, name);
        }

        private static String optionalString(Object value) {
                return value == null ? "" : String.valueOf(value);
        }

        static String requireProcedure(Object value, String name) {
                String procedure = requireString(value, name);
                require(PROCEDURE_NAME.matcher(procedure).matches(),
                        "Nombre de procedimiento inválido: " + procedure);
                return procedure;
        }

        private static String optionalProcedure(Object value, String name) {
                return value == null || String.valueOf(value).isBlank()
                        ? "" : requireProcedure(value, name);
        }

        private static ArrayList<String> requireStringList(Object value, String name) {
                require(value instanceof List<?>, name + " debe ser un arreglo");
                ArrayList<String> result = new ArrayList<>();
                for(Object item : (List<?>) value) result.add(requireString(item, name));
                return result;
        }

        private static String requireText(String value, String name) {
                require(value != null && !value.isBlank(), name + " es obligatorio");
                return value;
        }

        private static void require(boolean condition, String message) {
                if(!condition) throw new IllegalArgumentException(message);
        }

        private void setErrorStatus(String message) {
                JsonObject response = new JsonObject();
                response.addProperty("message", message == null ? "Error no especificado" : message);
                JsonObject item = new JsonObject();
                item.addProperty("resNum", "-1");
                item.add("response", response);
                JsonArray items = new JsonArray();
                items.add(item);
                JsonObject root = new JsonObject();
                root.add("responses", items);
                status = root.toString();
        }

        private String generatorParams(String parameters, Integer limit, Integer offset) {
                Gson gson = new Gson();
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("cuentaId", gatorFileInfo.getAccount());
                jsonObj.addProperty("bodegaId", gatorFileInfo.getWarehouse());
                jsonObj.addProperty("debugLevel", 0);
                jsonObj.addProperty("usuario", gatorFileInfo.getUser());
                jsonObj.addProperty("sesionId", gatorFileInfo.getSeionId());
                jsonObj.addProperty("idctrl", gatorFileInfo.getIdctrl());
                JsonObject jsonParameters = parameters == null || parameters.isBlank()
                        ? new JsonObject() : gson.fromJson(parameters, JsonObject.class);
                require(jsonParameters != null, "parametros debe ser un objeto JSON");
                jsonObj.add("parametros", jsonParameters);
                if(limit != null) jsonObj.addProperty("limit", limit);
                if(offset != null) jsonObj.addProperty("offset", offset);
                return gson.toJson(jsonObj);
        }

        static List<Map<String, Object>> procedureCalls(Object value) {
                if(value == null) return List.of();
                require(value instanceof List<?>, "procedimientos debe ser un arreglo");
                ArrayList<Map<String, Object>> result = new ArrayList<>();
                for(Object item : (List<?>) value) {
                        require(item instanceof Map<?, ?>, "Cada procedimiento debe ser un objeto");
                        HashMap<String, Object> call = new HashMap<>();
                        for(Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                                require(entry.getKey() instanceof String,
                                        "Las claves de procedimiento deben ser texto");
                                call.put((String) entry.getKey(), entry.getValue());
                        }
                        call.put("procedimiento",
                                requireProcedure(call.get("procedimiento"), "procedimiento"));
                        result.add(call);
                }
                return result;
        }

        private void executeProcedures(List<Map<String, Object>> procedures) {
                Gson gson = new Gson();
                ArrayList<Callable<String>> parallel = new ArrayList<>();
                for(int i = 0; i < procedures.size(); i++) {
                        Map<String, Object> call = procedures.get(i);
                        String procedure = requireProcedure(call.get("procedimiento"), "procedimiento");
                        String params = gson.toJson(call);
                        if(i == 0 || "1".equals(optionalString(call.get("amIFirst")))) {
                                successfulResponse(executeGeneratedProcedure(procedure, params), procedure);
                        } else {
                                parallel.add(() -> executeGeneratedProcedure(procedure, params));
                        }
                }
                for(String result : runParallel(parallel)) {
                        successfulResponse(result, "procedimiento generado");
                }
        }

        private String executeGeneratedProcedure(String procedure, String params) {
                return executeStore(new GappDBHelper(gatorFileInfo.getConfFile()), procedure, params);
        }

        private String executeStore(GappDBHelper helper, String procedure, String param) {
                GappSQLStatement statement = new GappSQLStatement();
                statement.setStoreProcedure(procedure);
                statement.addParam(param);
                return helper.executeStore(statement);
        }

        static List<String> runParallel(List<Callable<String>> tasks) {
                if(tasks.isEmpty()) return List.of();
                try {
                        ArrayList<String> results = new ArrayList<>();
                        for(Future<String> future : IMPORT_POOL.invokeAll(tasks)) results.add(future.get());
                        return results;
                } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("La ejecución concurrente fue interrumpida", e);
                } catch(ExecutionException e) {
                        throw new IllegalStateException("Falló una ejecución concurrente", e.getCause());
                }
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
                GappResponses obj = parseResponses(status, "estado");
                GappResponse response = firstResponse(obj, "estado");
                response.getResponse().put("generador", generador);
                response.getResponse().put("parametros", parametros);
                response.getResponse().put("ejecutor", executor);
                return gson.toJson(obj);
        }
        /**
         * Get the status as a GappReponse object.
         * @return The status as a response object.
         */
        public GappResponse getStatusAsResponse() {
                gappLog.startNewLog("GatorFileImportProcessor", "getStatusAsResponse");
                gappLog.setName("import-get-status");
                gappLog.addMessage("Importing getting status:" + status, 1);
                gappLogger.logIt(gappLog);                
                GappResponse response = firstResponse(parseResponses(status, "estado"), "estado");
                response.getResponse().put("generador", generador);
                response.getResponse().put("parametros", parametros);
                response.getResponse().put("ejecutor", executor);
                return response;
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
                try {
                        status = executeStore(dbhelper, requireProcedure(generador, "generador"),
                                generatorParams(parametros, null, null));
                        responses = parseResponses(status, generador);
                        firstResponse(responses, generador);
                        gappLog.addMessage("Execute generator done.", 1);
                } catch(Exception e) {
                        setErrorStatus(e.getMessage());
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);
                }
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
                try {
                        generador = requireProcedure(generador, "generador");
                        int limit = 600;
                        for(int offset = 0; ; offset += limit) {
                                status = executeStore(dbhelper, generador,
                                        generatorParams(parametros, limit, offset));
                                responses = parseResponses(status, generador);
                                List<Map<String, Object>> procedimientos = procedureCalls(
                                        successfulResponse(status, generador).getResponse().get("procedimientos"));
                                gappLog.addMessage("Procedimientos:" + procedimientos.size(), 1);
                                if(procedimientos.isEmpty()) break;
                                executeProcedures(procedimientos);
                        }
                } catch(Exception e) {
                        setErrorStatus(e.getMessage());
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);
                }
                gappLogger.logIt(gappLog);
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
                try {
                        generador = requireProcedure(generador, "generador");
                        status = executeStore(dbhelper, generador, generatorParams(parametros, null, null));
                        responses = parseResponses(status, generador);
                        executeProcedures(procedureCalls(
                                successfulResponse(status, generador).getResponse().get("procedimientos")));
                        gappLog.addMessage("Execute generator done.", 1);
                } catch(Exception e) {
                        setErrorStatus(e.getMessage());
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);
                }
                gappLogger.logIt(gappLog);
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
        public void doGenerate(String procedimiento, String parametros) {
                gappLog.startNewLog("GatorFileImportProcessor", "doGenerate");
                gappLog.setName("import-exec-doGenerate");
                try {
                        String result = executeGeneratedProcedure(
                                requireProcedure(procedimiento, "procedimiento"), parametros);
                        successfulResponse(result, procedimiento);
                        gappLog.addMessage("respuesta: " + result, 1);
                } catch(Exception e) {
                        setErrorStatus(e.getMessage());
                        gappLog.addMessage(gappLogger.getStackTraceString(e), 2);
                }
                gappLogger.logIt(gappLog);
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
