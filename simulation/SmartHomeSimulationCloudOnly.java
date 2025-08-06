import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.FogUtils;
import java.util.ArrayList;
import java.util.List;

public class SmartHomeSimulation {
    public static void main(String[] args) {
        try {
            // Load edge-ward config
            String configFile = "src/main/java/ifogsim_config.json";
            FogUtils.loadConfig(configFile);

            // Create application
            Application app = new Application("query_recombination");
            app.addAppModule(new AppModule("sensor_reader", 10));
            app.addAppModule(new AppModule("query_processor", 50));
            app.addAppEdge(new AppEdge("raw_temp", "sensor_reader", "temp_data", 8));
            app.addAppEdge(new AppEdge("temp_data", "query_processor", "top_k_results", 24));

            // Define AppLoop
            List<String> edgeLoop = new ArrayList<>();
            edgeLoop.add("ds18b20_sensor");
            edgeLoop.add("sensor_reader");
            edgeLoop.add("query_processor");
            edgeLoop.add("local_display");
            app.addAppLoop(new AppLoop(edgeLoop));

            // Run simulation
            FogUtils.runSimulation(app);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}