package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Smart Home IoT Simulation with Query Recombination
 * Based on the paper "Query Recombination: To Process a Large Number of Concurrent Top-k Queries towards IoT Data on an Edge Server"
 * This simulation models two scenarios: a baseline edge processing and an enhanced edge processing with query recombination.
 */
public class SmartHomeSim {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>(); // FIXED: Removed nested List<>
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	static boolean CLOUD = false; // Set to true for a cloud-only deployment
	static boolean RECOMBINATION = true; // Set to true to enable query recombination on the edge node
	
	static int numOfSensors = 5;
	static double TEMP_TRANSMISSION_TIME = 10; // Temperature reading every 10 seconds
	
	public static void main(String[] args) {
		Log.printLine("Starting Smart Home Simulation with Query Recombination...");

		try {
			Log.disable();
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "query_recomb_app";
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			
			createFogDevices(broker.getId(), appId);
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
			
			// Debug: Print configuration
			System.out.println("Configuration: CLOUD=" + CLOUD + ", RECOMBINATION=" + RECOMBINATION);
			
			if(CLOUD){
				// Cloud-based deployment: all processing is done on the cloud.
				moduleMapping.addModuleToDevice("data_collector", "cloud");
				moduleMapping.addModuleToDevice("query_processor", "cloud");
				moduleMapping.addModuleToDevice("cloud_storage", "cloud");
				if(RECOMBINATION){
					// Even in a cloud-only scenario, we can simulate recombination logic on the cloud
					moduleMapping.addModuleToDevice("query_recombiner", "cloud");
				}
			} else {
				// Edge-based deployment (default)
				moduleMapping.addModuleToDevice("data_collector", "raspberry_pi");
				moduleMapping.addModuleToDevice("query_processor", "raspberry_pi"); // ADDED: Explicit placement
				if(RECOMBINATION){
					// Place the recombination engine on the edge for the recombination scenario
					moduleMapping.addModuleToDevice("query_recombiner", "raspberry_pi");
				}
				moduleMapping.addModuleToDevice("cloud_storage", "cloud"); // ADDED: Explicit placement
			}
			
			// Debug: Print module mappings
			System.out.println("Module Mappings:");
			for (String module : moduleMapping.getModuleMapping().keySet()) {
				System.out.println("  " + module + " -> " + moduleMapping.getModuleMapping().get(module));
			}
			
			Controller controller = new Controller("recomb-controller", fogDevices, sensors, actuators);
			
			// Use the appropriate placement policy - ALWAYS use ModulePlacementMapping for guaranteed placement
			controller.submitApplication(application, 0, 
					new ModulePlacementMapping(fogDevices, application, moduleMapping));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			// Create the configuration string for the output filename
			String config = (CLOUD ? "cloud-only" : "edge-ward") + (RECOMBINATION ? "-recombined" : "");

			// --- START OF FILE OUTPUT CODE ---
			String resultFile = "C:\\Users\\johna\\Downloads\\iFogSim-main\\results\\" + config + "_ifogsim.txt";
			java.io.File dir = new java.io.File("C:\\Users\\johna\\Downloads\\iFogSim-main\\results\\");
			if (!dir.exists()) dir.mkdirs();
			
			java.io.PrintStream originalOut = System.out;
			
			try (
				java.io.FileOutputStream fileOut = new java.io.FileOutputStream(resultFile);
				java.io.PrintStream filePrint = new java.io.PrintStream(fileOut)
			) {
				System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
					@Override
					public void write(int b) throws java.io.IOException {
						originalOut.write(b);
						filePrint.write(b);
					}
					
					@Override
					public void write(byte[] b, int off, int len) throws java.io.IOException {
						originalOut.write(b, off, len);
						filePrint.write(b, off, len);
					}
				}));
			
				CloudSim.startSimulation();
				CloudSim.stopSimulation();
				
			} finally {
				// Reset System.out to its original state after the simulation is done
				System.setOut(originalOut);
			}
			// --- END OF FILE OUTPUT CODE ---

			Log.printLine("Smart Home Simulation finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Simulation error occurred");
		}
	}

	/**
	 * Creates the fog devices in the physical topology
	 */
	private static void createFogDevices(int userId, String appId) {
		// Cloud device at the apex of hierarchy (level 0)
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		
		// Edge device - Raspberry Pi 4 (level 1)
		FogDevice raspberryPi = createFogDevice("raspberry_pi", 1500, 4096, 32000, 32000, 1, 0.0, 5.0, 2.0);
		raspberryPi.setParentId(cloud.getId());
		raspberryPi.setUplinkLatency(100); // Internet latency to cloud
		fogDevices.add(raspberryPi);
		
		// Create temperature sensors and display actuators
		for(int i = 0; i < numOfSensors; i++){
			String sensorId = "temp_sensor_" + i;
			Sensor tempSensor = new Sensor("s-" + sensorId, "TEMPERATURE", userId, appId, 
					new DeterministicDistribution(TEMP_TRANSMISSION_TIME));
			sensors.add(tempSensor);
			
			Actuator display = new Actuator("a-display_" + i, userId, appId, "DISPLAY");
			actuators.add(display);
			
			tempSensor.setGatewayDeviceId(raspberryPi.getId());
			tempSensor.setLatency(2.0); // Local network latency
			
			display.setGatewayDeviceId(raspberryPi.getId());
			display.setLatency(1.0);
		}
	}
	
	/**
	 * Creates a fog device with specified parameters
	 */
	private static FogDevice createFogDevice(String nodeName, long mips, int ram, 
			long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		long bw = 10000; // Changed to long to match upBw/downBw

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking((long)bw), // Cast to long
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}

	/**
	 * Creates the Smart Home application using the DDF model.
	 * This method is modified to include a query_recombiner module
	 * to simulate the paper's core concept.
	 */
	@SuppressWarnings({"serial"})
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId);
		
		/*
		 * Adding application modules
		 */
		application.addAppModule("data_collector", 10); // Module to collect raw sensor data
		application.addAppModule("query_processor", 50); // Module to process a single top-k query
		application.addAppModule("cloud_storage", 20); // Data storage module (simulates S3)
		if (RECOMBINATION) {
			application.addAppModule("query_recombiner", 30); // NEW: Module to combine multiple queries into one
		}
		
		/*
		 * Adding edges between modules
		 */
		// Sensor to data_collector
		application.addAppEdge("TEMPERATURE", "data_collector", 1000, 8, "raw_temp", Tuple.UP, AppEdge.SENSOR);
		
		// If recombination is enabled, data goes to the recombiner first
		if (RECOMBINATION) {
			application.addAppEdge("data_collector", "query_recombiner", 1000, 8, "top_k_query_request", Tuple.UP, AppEdge.MODULE);
			application.addAppEdge("query_recombiner", "query_processor", 100, 8, "recombined_query", Tuple.UP, AppEdge.MODULE);
		} else {
			// Without recombination, each query is a separate tuple from the data collector to the processor
			application.addAppEdge("data_collector", "query_processor", 1000, 8, "top_k_query_request", Tuple.UP, AppEdge.MODULE);
		}
		
		// query_processor processing and results
		application.addAppEdge("query_processor", "data_collector", 100, 24, "top_k_results", Tuple.DOWN, AppEdge.MODULE);
		
		// Results to display
		application.addAppEdge("data_collector", "DISPLAY", 1000, 24, "display_results", Tuple.DOWN, AppEdge.ACTUATOR);
		
		// Periodic data aggregation to cloud storage
		application.addAppEdge("query_processor", "cloud_storage", 60000, 40, "aggregated_data", Tuple.UP, AppEdge.MODULE);
		
		/*
		 * Defining selectivity relationships for the application modules
		 */
		application.addTupleMapping("data_collector", "raw_temp", "top_k_query_request", new FractionalSelectivity(1.0));
		
		if (RECOMBINATION) {
			// When recombination is active, the query processor receives a recombined query
			application.addTupleMapping("query_processor", "recombined_query", "top_k_results", new FractionalSelectivity(1.0));
			application.addTupleMapping("query_processor", "recombined_query", "aggregated_data", new FractionalSelectivity(0.1));
			application.addTupleMapping("query_recombiner", "top_k_query_request", "recombined_query", new FractionalSelectivity(0.2));
		} else {
			// Without recombination, the query processor receives a standard query
			application.addTupleMapping("query_processor", "top_k_query_request", "top_k_results", new FractionalSelectivity(1.0));
			application.addTupleMapping("query_processor", "top_k_query_request", "aggregated_data", new FractionalSelectivity(0.1));
		}
		
		application.addTupleMapping("data_collector", "top_k_results", "display_results", new FractionalSelectivity(1.0));
		
		/*
		 * Defining application loops for latency monitoring
		 */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{
			add("TEMPERATURE");
			add("data_collector");
			if (RECOMBINATION) add("query_recombiner");
			add("query_processor");
			add("data_collector");
			add("DISPLAY");
		}});
		
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
}
