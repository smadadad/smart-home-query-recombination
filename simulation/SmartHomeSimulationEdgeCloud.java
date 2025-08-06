import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.FogUtils;
import java.util.ArrayList;
import java.util.List;

public class SmartHomeSimulationEdgeCloud {
    public static void main(String[] args) {
        try {
            // Load edge+cloud config
            String configFile = "src/main/java/ifogsim_config_edge_cloud.json";
            FogUtils.loadConfig(configFile);

            // Create application
            Application app = new Application("query_recombination_edge_cloud");
            app.addAppModule(new AppModule("sensor_reader", 10));
            app.addAppModule(new AppModule("query_processor_edge", 50));
            app.addAppModule(new AppModule("query_processor_cloud", 50));
            app.addAppModule(new AppModule("cloud_storage", 20));
            app.addAppEdge(new AppEdge("raw_temp", "sensor_reader", "temp_data", 8));
            app.addAppEdge(new AppEdge("temp_data", "query_processor_edge", "top_k_results_edge", 24));
            app.addAppEdge(new AppEdge("temp_data", "query_processor_edge", "temp_data_cloud", 8));
            app.addAppEdge(new AppEdge("temp_data_cloud", "query_processor_cloud", "top_k_results_cloud", 24));
            app.addAppEdge(new AppEdge("temp_data", "query_processor_edge", "agg_data", 40));
            app.addAppEdge(new AppEdge("agg_data", "cloud_storage", "stored_data", 40));

            // Define AppLoops
            List<String> edgeLoop = new ArrayList<>();
            edgeLoop.add("ds18b20_sensor");
            edgeLoop.add("sensor_reader");
            edgeLoop.add("query_processor_edge");
            edgeLoop.add("local_display");
            app.addAppLoop(new AppLoop(edgeLoop));

            List<String> cloudLoop = new ArrayList<>();
            cloudLoop.add("ds18b20_sensor");
            cloudLoop.add("sensor_reader");
            cloudLoop.add("query_processor_edge");
            cloudLoop.add("query_processor_cloud");
            cloudLoop.add("local_display");
            app.addAppLoop(new AppLoop(cloudLoop));

            List<String> storageLoop = new ArrayList<>();
            storageLoop.add("ds18b20_sensor");
            storageLoop.add("sensor_reader");
            storageLoop.add("query_processor_edge");
            storageLoop.add("cloud_storage");
            storageLoop.add("s3_storage");
            app.addAppLoop(new AppLoop(storageLoop));

            // Run simulation
            FogUtils.runSimulation(app);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}