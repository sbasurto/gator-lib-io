package gator.lib.io.external;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import gator.lib.io.external.data.GatorFileInfo;

public final class GatorAdmonTablasSelfCheck {
    public static void main(String[] args) {
        GatorAdmonTablas tables = new GatorAdmonTablas();
        tables.addTable("inventory");
        tables.addAccion("alta");
        tables.addFields(new ArrayList<>(java.util.List.of("sku")));
        tables.addValues(new ArrayList<>(java.util.List.of("A-1")));
        assert "inventory".equals(tables.getObject().getAsJsonArray("tablas").get(0).getAsString());
        assert "alta".equals(tables.getObject().getAsJsonArray("acciones").get(0).getAsString());
        assert GatorFileImportProcessor.importRequestEnd(0, 543) == 1;
        assert GatorFileImportProcessor.importRequestEnd(1, 543) == 543;
        List<List<String>> csv = GatorFileImportProcessor.parseDelimited(
                "id,description,notes\r\n1,\"a,b\",\"line 1\nline 2\"\r\n2,\"x\"\"y\",\r\n", ',');
        assert csv.size() == 3;
        assert csv.get(1).get(1).equals("a,b");
        assert csv.get(1).get(2).equals("line 1\nline 2");
        assert csv.get(2).get(1).equals("x\"y");
        assert csv.get(2).get(2).isEmpty();
        List<List<String>> leadingZero = GatorFileImportProcessor.parseDelimited(
                "REFERENCIA,PRODUCTO,CANTIDAD\nR1,0219,1\nR2,0918,2\n", ',');
        assert leadingZero.get(1).get(1).equals("0219");
        assert leadingZero.get(2).get(1).equals("0918");

        List<String> configuredColumns = List.of("{none}", "sku", "{none}", "fecha", "none");
        List<Integer> importedIndexes = GatorFileImportProcessor.importColumnIndexes(configuredColumns);
        assert GatorFileImportProcessor.selectImportColumns(configuredColumns, importedIndexes)
                .equals(List.of("sku", "fecha"));
        List<List<String>> ignoredColumns = GatorFileImportProcessor.parseDelimited(
                "EXTRA,SKU,NOTAS,FECHA,ANTERIOR\nlleno,0219,,2026-10-20,otro\n,0918,lleno,2026-10-21,\n", ',');
        assert GatorFileImportProcessor.selectImportColumns(ignoredColumns.get(1), importedIndexes)
                .equals(List.of("0219", "2026-10-20"));
        assert GatorFileImportProcessor.selectImportColumns(ignoredColumns.get(2), importedIndexes)
                .equals(List.of("0918", "2026-10-21"));
        assert GatorFileImportProcessor.importColumnIndexes(List.of("sku", "fecha"))
                .equals(List.of(0, 1));
        boolean duplicateRejected = false;
        try {
            GatorFileImportProcessor.importColumnIndexes(List.of("{none}", "sku", "none", "sku"));
        } catch (IllegalArgumentException expected) {
            duplicateRejected = true;
        }
        assert duplicateRejected : "Las columnas destino reales duplicadas deben rechazarse";

        GatorFileInfo generatorInfo = new GatorFileInfo();
        generatorInfo.setConfFile("db");
        generatorInfo.setAccount("1");
        generatorInfo.setWarehouse("1");
        generatorInfo.setUser("user");
        generatorInfo.setIdctrl("control");
        generatorInfo.setSessionId("session");
        generatorInfo.setProcessType("entrada");
        assert "entrada".equals(generatorInfo.getProcessType());
        assert GatorFileImportProcessor.requireFileInfo(generatorInfo, false) == generatorInfo;

        boolean rejected = false;
        try {
            GatorFileImportProcessor.requireFileInfo(generatorInfo, true);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assert rejected;

        rejected = false;
        try {
            GatorFileImportProcessor.requireProcedure("safe;drop", "procedimiento");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assert rejected;

        rejected = false;
        try {
            GatorFileImportProcessor.parseResponses("{", "prueba");
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        assert rejected;

        Map<String, Object> generated = new LinkedHashMap<>();
        generated.put("procedimiento", "app_fn_admon_producto");
        generated.put("acciones", List.of("alta"));
        generated.put("campos", List.of(List.of("producto_sku", "cuenta_id")));
        Map<String, Object> call = GatorFileImportProcessor.procedureCalls(List.of(generated)).get(0);
        assert call.get("acciones") instanceof List<?>;
        assert call.get("campos") instanceof List<?>;

    }
}
