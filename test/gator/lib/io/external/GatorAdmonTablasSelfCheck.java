package gator.lib.io.external;

import java.util.ArrayList;

public final class GatorAdmonTablasSelfCheck {
    public static void main(String[] args) {
        GatorAdmonTablas tables = new GatorAdmonTablas();
        tables.addTable("inventory");
        tables.addAccion("alta");
        tables.addFields(new ArrayList<>(java.util.List.of("sku")));
        tables.addValues(new ArrayList<>(java.util.List.of("A-1")));
        assert "inventory".equals(tables.getObject().getAsJsonArray("tablas").get(0).getAsString());
        assert "alta".equals(tables.getObject().getAsJsonArray("acciones").get(0).getAsString());
    }
}
