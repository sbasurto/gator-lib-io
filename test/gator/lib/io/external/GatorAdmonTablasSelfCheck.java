package gator.lib.io.external;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class GatorAdmonTablasSelfCheck {
    public static void main(String[] args) {
        GatorAdmonTablas tables = new GatorAdmonTablas();
        tables.addTable("inventory");
        tables.addAccion("alta");
        tables.addFields(new ArrayList<>(java.util.List.of("sku")));
        tables.addValues(new ArrayList<>(java.util.List.of("A-1")));
        assert "inventory".equals(tables.getObject().getAsJsonArray("tablas").get(0).getAsString());
        assert "alta".equals(tables.getObject().getAsJsonArray("acciones").get(0).getAsString());
        List<List<String>> csv = GatorFileImportProcessor.parseDelimited(
                "id,description,notes\r\n1,\"a,b\",\"line 1\nline 2\"\r\n2,\"x\"\"y\",\r\n", ',');
        assert csv.size() == 3;
        assert csv.get(1).get(1).equals("a,b");
        assert csv.get(1).get(2).equals("line 1\nline 2");
        assert csv.get(2).get(1).equals("x\"y");
        assert csv.get(2).get(2).isEmpty();

        boolean rejected = false;
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

        CountDownLatch ready = new CountDownLatch(2);
        List<java.util.concurrent.Callable<String>> concurrent = List.of(
                () -> { ready.countDown(); assert ready.await(2, TimeUnit.SECONDS); return "a"; },
                () -> { ready.countDown(); assert ready.await(2, TimeUnit.SECONDS); return "b"; });
        assert GatorFileImportProcessor.runParallel(concurrent).size() == 2;
    }
}
