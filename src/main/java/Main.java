import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import tra.helpers.JsonHelper;
import tra.models.Codes;
import tra.models.FileDeps;
import tra.models.Pair;
import tra.models.temp.Rule;
import tra.v5.ElpisParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {

    static Queue<String> codeProcessingQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        ElpisParser ep = new ElpisParser();
        ep.parse();
    }

    /*public static void main(String[] args) {
        File outputFolder = new File("output");
        if (outputFolder.mkdirs()) {
            compile(args[0]);
            for (FileDeps fileDeps : ElpisParser.allDependencies) {
                fileDeps.filePath = fileDeps.filePath.substring(0,
                        fileDeps.filePath.length() - ".elpis".length()) + ".elp";
                for (int counter = 0; counter < fileDeps.depsPath.size(); counter++) {
                    fileDeps.depsPath.set(counter, fileDeps.depsPath.get(counter).substring(0,
                            fileDeps.depsPath.get(counter).length() - ".elpis".length()) + ".elp");
                }
            }
            Collections.reverse(ElpisParser.allDependencies);
            String uglyJSONString = JsonHelper.toJson(ElpisParser.allDependencies);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(uglyJSONString);
            String buildTree = gson.toJson(je);
            System.out.println(buildTree);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream("output/build.txt"), StandardCharsets.UTF_8);
                writer.write(buildTree);
                writer.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }*/

    /*private static void compile(String entryPoint) {
        ElpisParser.clientSideFunctions = new ArrayList<>();
        try {
            File file = new File(entryPoint);
            FileReader fr = new FileReader(file);
            ElpisLexer lexer = new ElpisLexer(fr);
            ElpisParser traParser = new ElpisParser(entryPoint, file.getName(), lexer);
            List<String> dependencies = traParser.parse();
            FileDeps fileDeps = new FileDeps();
            fileDeps.filePath = entryPoint;
            fileDeps.depsPath = dependencies;
            ElpisParser.allDependencies.add(fileDeps);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (ElpisParser.clientSideFunctions.size() > 0) {
            try {
                File finalFile = new File("output/@ClientSideFunctions.elp");
                finalFile.getParentFile().mkdirs();
                finalFile.delete();
                finalFile.createNewFile();
                FileOutputStream stream = new FileOutputStream(finalFile);
                stream.write(ElpisParser.convertCodeToBytes(ElpisParser.clientSideFunctions));
                stream.flush();
                stream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }*/
}
