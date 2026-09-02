package gator.lib.io.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gator.lib.db.GappSQLStatement;
import gator.lib.db.helpers.GappDBHelper;
import gator.lib.db.helpers.GappResponse;
import gator.lib.io.external.data.GatorFileInfo;
import java.util.UUID;

public final class GatorFileImportPressureCheck {
    public static void main(String[] args) {
        if (args.length < 9 || args.length > 10) {
            throw new IllegalArgumentException("Se requieren: archivo|config|cuenta|bodega|usuario|idctrl|sesion|filas|tipoProceso|generar");
        }
        GatorFileInfo info = new GatorFileInfo();
        info.setFileName(args[0]);
        info.setConfFile(args[1]);
        info.setAccount(args[2]);
        info.setWarehouse(args[3]);
        info.setUser(args[4]);
        info.setIdctrl(args[5]);
        info.setSessionId(args[6]);
        info.setProcessType(args[8]);

        long started = System.nanoTime();
        GatorFileImportProcessor processor = new GatorFileImportProcessor(info);
        GappResponse response = processor.getStatusAsResponse();
        int rows = processor.getProcessLinesAsHashMap().size();
        int expected = Integer.parseInt(args[7]);
        if (!"0".equals(response.getResNum())) throw new AssertionError(response.getResDesc());
        if (rows != expected) throw new AssertionError("Se procesaron " + rows + " de " + expected + " filas");
        System.out.printf("PRESSURE IMPORT OK rows=%d elapsedMs=%d%n", rows,
                (System.nanoTime() - started) / 1_000_000);
        if (args.length == 10 && Boolean.parseBoolean(args[9])) {
            started = System.nanoTime();
            JsonObject create = new JsonObject();
            create.addProperty("action", "create");
            create.addProperty("fileGroupId", args[0]);
            create.addProperty("generator", processor.getGenerador());
            create.addProperty("executor", processor.getEjecutor());
            create.add("parameters", JsonParser.parseString(processor.getParametros()));
            create.addProperty("idctrl", args[5]);
            create.addProperty("userId", args[4]);
            create.addProperty("sessionId", args[6]);
            create.addProperty("stagingId", args[6]);
            create.addProperty("cuentaId", args[2]);
            create.addProperty("bodegaId", args[3]);
            String jobId = call(args[1], create).get("jobId").getAsString();

            JsonObject queue = context(args);
            queue.addProperty("action", "queue");
            queue.addProperty("jobId", jobId);
            call(args[1], queue);
            call(args[1], queue);

            String leaseOwner = UUID.randomUUID().toString();
            JsonObject claim = new JsonObject();
            claim.addProperty("action", "claim");
            claim.addProperty("leaseOwner", leaseOwner);
            if (!jobId.equals(call(args[1], claim).get("jobId").getAsString())) {
                throw new AssertionError("Se reclamó un trabajo distinto");
            }
            processor.executeJob(jobId, leaseOwner);
            response = processor.getStatusAsResponse();
            if (!"0".equals(response.getResNum())) {
                throw new AssertionError(String.valueOf(response.getResponse().get("message")));
            }
            JsonObject status = context(args);
            status.addProperty("action", "status");
            status.addProperty("jobId", jobId);
            if (!"completed".equals(call(args[1], status).get("status").getAsString())) {
                throw new AssertionError("El trabajo no terminó en completed");
            }
            System.out.printf("PRESSURE JOB OK jobId=%s elapsedMs=%d%n",
                    jobId, (System.nanoTime() - started) / 1_000_000);
        }
    }

    private static JsonObject context(String[] args) {
        JsonObject request = new JsonObject();
        request.addProperty("userId", args[4]);
        request.addProperty("sessionId", args[6]);
        request.addProperty("cuentaId", args[2]);
        request.addProperty("bodegaId", args[3]);
        return request;
    }

    private static JsonObject call(String config, JsonObject request) {
        GappSQLStatement statement = new GappSQLStatement();
        statement.setStoreProcedure("app_fn_import_job");
        statement.addParam(request.toString());
        JsonObject response = JsonParser.parseString(
                new GappDBHelper(config).executeStore(statement)).getAsJsonObject()
                .getAsJsonArray("responses").get(0).getAsJsonObject();
        if (!"0".equals(response.get("resNum").getAsString())) {
            throw new AssertionError(response.getAsJsonObject("response").get("message").getAsString());
        }
        return response.getAsJsonObject("response");
    }
}
