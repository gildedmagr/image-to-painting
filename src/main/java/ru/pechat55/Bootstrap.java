package ru.pechat55;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pechat55.controllers.IndexController;
import spark.Spark;

public class Bootstrap {
    Logger logger = LoggerFactory.getLogger(getClass());

    public Bootstrap(){
        Spark.port(8888);
        Spark.staticFiles.location("/");
        Spark.staticFiles.externalLocation("/code");
        Spark.get("/", (request, response) -> {
            IndexController.generateFiles();
            return "Hello World";
        });
    }

    public static void start(){
        new Bootstrap();
    }
}
