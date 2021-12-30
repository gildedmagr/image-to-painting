package ru.pechat55;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

public class Bootstrap {
    Logger logger = LoggerFactory.getLogger(getClass());

    public Bootstrap(){
        Spark.port(8888);
        Spark.staticFiles.location("/");
        Spark.staticFiles.externalLocation("/code");
        Spark.get("/", (request, response) -> {
            //Application.generateFiles();
            //response.body("Hello");
            return "Hello World";
        });
    }

    public static void start(){
        new Bootstrap();
    }
}
