package gator.lib.io.external;

import gator.lib.db.helpers.GappResponse;
import gator.lib.io.external.data.GatorFileInfo;

public final class GatorFileImportPressureCheck {
    public static void main(String[] args) {
        if (args.length < 8 || args.length > 9) {
            throw new IllegalArgumentException("Se requieren: archivo|config|cuenta|bodega|usuario|idctrl|sesion|filas|generar");
        }
        GatorFileInfo info = new GatorFileInfo();
        info.setFileName(args[0]);
        info.setConfFile(args[1]);
        info.setAccount(args[2]);
        info.setWarehouse(args[3]);
        info.setUser(args[4]);
        info.setIdctrl(args[5]);
        info.setSessionId(args[6]);

        long started = System.nanoTime();
        GatorFileImportProcessor processor = new GatorFileImportProcessor(info);
        GappResponse response = processor.getStatusAsResponse();
        int rows = processor.getProcessLinesAsHashMap().size();
        int expected = Integer.parseInt(args[7]);
        if (!"0".equals(response.getResNum())) throw new AssertionError(response.getResDesc());
        if (rows != expected) throw new AssertionError("Se procesaron " + rows + " de " + expected + " filas");
        System.out.printf("PRESSURE IMPORT OK rows=%d elapsedMs=%d%n", rows,
                (System.nanoTime() - started) / 1_000_000);
        if (args.length == 9 && Boolean.parseBoolean(args[8])) {
            started = System.nanoTime();
            processor.execGenerator(processor.getGenerador(), processor.getParametros());
            response = processor.getStatusAsResponse();
            if (!"0".equals(response.getResNum())) throw new AssertionError(response.getResDesc());
            System.out.printf("PRESSURE GENERATOR OK elapsedMs=%d%n",
                    (System.nanoTime() - started) / 1_000_000);
        }
    }
}
