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
 * Simulation setup for Smart Home IoT Simulation with Query Recombination
 * @author Adams Animashaun
 * Run this class 4 times with different command line arguments to get all scenarios
 */
public class SmartHomeSim {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	static int numOfSensors = 5;
	static double TEMP_TRANSMISSION_TIME = 10;
	
	public static void main(String[] args) {
		// Parse command line arguments
		boolean cloudMode = true;
		boolean recombinationMode = false;
		
		if (args.length >= 2) {
			cloudMode = Boolean.parseBoolean(args[0]);
			recombinationMode = Boolean.parseBoolean(args[1]);
		} else {
			System.out.println("Usage: java SmartHomeSim <cloudMode> <recombinationMode>");
			System.out.println("Example: java SmartHomeSim true false");
			System.out.println("Running with default values: cloudMode=false, recombinationMode=false");
		}
		
		Log.printLine("Starting Smart Home Simulation with Query Recombination...");
		Log.disable();
		
		runSimulation(cloudMode, recombinationMode);
		Log.printLine("Simulation finished!");
	}

	private static void runSimulation(boolean cloudMode, boolean recombinationMode) {
		try {
			// Print configuration
			System.out.println("-------------------------------------------------------");
			System.out.println("Running simulation for: CLOUD=" + cloudMode + ", RECOMBINATION=" + recombinationMode);
			System.out.println("-------------------------------------------------------");

			String config = (cloudMode ? "cloud-only" : "edge-ward") + (recombinationMode ? "-recombined" : "");
			System.out.println("Configuration: " + config);

			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "query_recomb_app";
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId(), recombinationMode);
			application.setUserId(broker.getId());
			
			createFogDevices(broker.getId(), appId, application, cloudMode);
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
			
			if(cloudMode){
				moduleMapping.addModuleToDevice("data_collector", "cloud");
				moduleMapping.addModuleToDevice("query_processor", "cloud");
				moduleMapping.addModuleToDevice("cloud_storage", "cloud");
				if(recombinationMode){
					moduleMapping.addModuleToDevice("query_recombiner", "cloud");
				}
			} else {
				moduleMapping.addModuleToDevice("data_collector", "raspberry_pi");
				moduleMapping.addModuleToDevice("query_processor", "raspberry_pi");
				if(recombinationMode){
					moduleMapping.addModuleToDevice("query_recombiner", "raspberry_pi");
				}
				moduleMapping.addModuleToDevice("cloud_storage", "cloud");
			}
			
			Controller controller = new Controller("recomb-controller", fogDevices, sensors, actuators);
			
			controller.submitApplication(application, 0, 
					(cloudMode) ? (new ModulePlacementMapping(fogDevices, application, moduleMapping)) 
					: (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			// File output
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
				System.setOut(originalOut);
			}

			System.out.println("Results written to: " + resultFile);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Simulation error occurred for scenario: " + 
				(cloudMode ? "cloud-only" : "edge-ward") + (recombinationMode ? "-recombined" : ""));
		}
	}

	private static void createFogDevices(int userId, String appId, Application application, boolean cloudMode) {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		
		FogDevice raspberryPi = createFogDevice("raspberry_pi", 1500, 4096, 32000, 32000, 1, 0.0, 5.0, 2.0);
		raspberryPi.setParentId(cloud.getId());
		raspberryPi.setUplinkLatency(100);
		fogDevices.add(raspberryPi);
		
		// Attach sensors and actuators to appropriate device based on mode
		int attachId = cloudMode ? cloud.getId() : raspberryPi.getId();
		
		for(int i = 0; i < numOfSensors; i++){
			addSensorAndActuator("sensor_" + i, userId, appId, attachId, application, cloudMode);
		}
	}
	
	/**
     * Adds a sensor and actuator to the simulation.
     * @param id The ID suffix for the sensor and actuator.
     * @param userId The user ID.
     * @param appId The application ID.
     * @param gatewayId The gateway device ID.
     * @param application The application object.
     * @param cloudMode Whether running in cloud mode.
     */
    private static void addSensorAndActuator(String id, int userId, String appId, int gatewayId, Application application, boolean cloudMode) {
        double sensorLatency = cloudMode ? 100.0 : 2.0; // Higher latency in cloud mode
        double displayLatency = cloudMode ? 100.0 : 1.0; // Higher latency in cloud mode

        Sensor tempSensor = new Sensor("temp-" + id, "TEMPERATURE", userId, appId, new DeterministicDistribution(TEMP_TRANSMISSION_TIME));
        sensors.add(tempSensor);
        tempSensor.setGatewayDeviceId(gatewayId);
        tempSensor.setLatency(sensorLatency);
        tempSensor.setApp(application);

        Actuator display = new Actuator("display-" + id, userId, appId, "DISPLAY");
        actuators.add(display);
        display.setGatewayDeviceId(gatewayId);
        display.setLatency(displayLatency);
        display.setApp(application);
    }
	
	private static FogDevice createFogDevice(String nodeName, long mips, int ram, 
			long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		long bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
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

	@SuppressWarnings({"serial"})
	private static Application createApplication(String appId, int userId, boolean recombinationMode){
		
		Application application = Application.createApplication(appId, userId);
		
		application.addAppModule("data_collector", 10);
		application.addAppModule("query_processor", 50);
		application.addAppModule("cloud_storage", 20);
		if (recombinationMode) {
			application.addAppModule("query_recombiner", 30);
		}
		
		// Connecting the application modules with edges
		application.addAppEdge("TEMPERATURE", "data_collector", 1000, 500, "TEMPERATURE", Tuple.UP, AppEdge.SENSOR);
		
		if (recombinationMode) {
			application.addAppEdge("data_collector", "query_recombiner", 1000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
			application.addAppEdge("query_recombiner", "query_processor", 100, 500, "RECOMBINED_DATA", Tuple.UP, AppEdge.MODULE);
			application.addAppEdge("query_processor", "cloud_storage", 60000, 500, "STORAGE_DATA", Tuple.UP, AppEdge.MODULE);
			application.addAppEdge("query_processor", "data_collector", 100, 28, 1000, "PROCESSED_DATA", Tuple.DOWN, AppEdge.MODULE);
			application.addAppEdge("data_collector", "DISPLAY", 1000, 500, "DISPLAY_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);
		} else {
			application.addAppEdge("data_collector", "query_processor", 1000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
			application.addAppEdge("query_processor", "cloud_storage", 60000, 500, "STORAGE_DATA", Tuple.UP, AppEdge.MODULE);
			application.addAppEdge("query_processor", "data_collector", 100, 28, 1000, "PROCESSED_DATA", Tuple.DOWN, AppEdge.MODULE);
			application.addAppEdge("data_collector", "DISPLAY", 1000, 500, "DISPLAY_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);
		}
		
		// Defining the input-output relationships of modules
		application.addTupleMapping("data_collector", "TEMPERATURE", "RAW_DATA", new FractionalSelectivity(1.0));
		
		if (recombinationMode) {
			application.addTupleMapping("query_recombiner", "RAW_DATA", "RECOMBINED_DATA", new FractionalSelectivity(0.8));
			application.addTupleMapping("query_processor", "RECOMBINED_DATA", "PROCESSED_DATA", new FractionalSelectivity(1.0));
			application.addTupleMapping("query_processor", "RECOMBINED_DATA", "STORAGE_DATA", new FractionalSelectivity(0.1));
			application.addTupleMapping("data_collector", "PROCESSED_DATA", "DISPLAY_UPDATE", new FractionalSelectivity(1.0));
		} else {
			application.addTupleMapping("query_processor", "RAW_DATA", "PROCESSED_DATA", new FractionalSelectivity(1.0));
			application.addTupleMapping("query_processor", "RAW_DATA", "STORAGE_DATA", new FractionalSelectivity(0.1));
			application.addTupleMapping("data_collector", "PROCESSED_DATA", "DISPLAY_UPDATE", new FractionalSelectivity(1.0));
		}
		
		// Defining application loops to monitor latency
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{
			add("TEMPERATURE");
			add("data_collector");
			if (recombinationMode) {
				add("query_recombiner");
			}
			add("query_processor");
			add("data_collector");
			add("DISPLAY");
		}});
		
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
}
