import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.simulation.Simulation;
import java.io.FileReader;
import com.google.gson.Gson;

public class SmartHomeSimulation {
    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();
        FileReader reader = new FileReader("simulation/ifogsim_config.json");
        Simulation sim = gson.fromJson(reader, Simulation.class);
        sim.run();
        System.out.println("Latency: " + sim.getAverageLatency() + " ms");
        System.out.println("Bandwidth: " + sim.getTotalBandwidth() + " kbps");
        System.out.println("Energy: " + sim.getTotalEnergy() + " W");
    }
}