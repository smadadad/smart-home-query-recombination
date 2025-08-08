```
smart-home-query-recombination/
├── simulation/
│   ├── SmartHomeSimulation.java
├── docs/
│   ├── README.md
├── results/
│   ├── edge_ward_ifogsim.txt
│   ├── cloud_only_ifogsim.txt
│   ├── edge_ward_recombined_ifogsim.txt
│   └── cloud_only_recombined_ifogsim.txt
└── requirements.txt
```

# Smart Home Temperature Monitoring System

This repository implements a simulated Smart Home Temperature Monitoring System using the iFogSim toolkit, based on the Query Recombination framework (Shi et al., 2022). It processes top-k queries (e.g., top 3 warmest rooms) from 5 simulated DS18B20 sensors, utilizing a Raspberry Pi 4 as the edge node and an AWS EC2/S3-like cloud for processing and storage. The simulation is defined in `SmartHomeSimulation.java`, adapting the structure of `FarmFogSimulation.java`, and supports four configurations: `cloud-only`, `cloud-only-recombined`, `edge-ward`, and `edge-ward-recombined`. Results are logged to text files in the `results/` directory.

## iFogSim Utilization

The implementation leverages the following iFogSim features:
- **Topology Modeling**: Defines sensors (`temp-sensor_0` to `temp-sensor_4`), edge node (`raspberry_pi`, 1,500 MIPS), and cloud node (`cloud`, 44,800 MIPS) with realistic parameters.
- **Application Modeling**: Uses `AppModule`, `AppEdge`, and `AppLoop` to simulate MQTT data collection (`data_collector`), top-k query processing (`query_processor`), query recombination (`query_recombiner` if enabled), and cloud storage (`cloud_storage`).
- **Network Simulation**: Models latencies (e.g., ~2 ms edge, ~100 ms cloud) and bandwidth (e.g., ~32 kbps edge, ~100 kbps cloud).
- **Energy Modeling**: Tracks energy consumption (e.g., ~5 W edge, ~10 W cloud).
- **Dynamic Deployment**: Uses `ModuleMapping` and `ModulePlacementEdgewards` for module placement based on `cloudMode`.
- **Simulation Metrics**: Outputs execution time, energy consumption, and network usage to text files.

## Prerequisites

- **OS**: Linux (Ubuntu recommended), Windows, or Mac.
- **Software**:
  - Java 8 (`openjdk-8-jdk`)
  - Maven (`maven`)
- **Hardware**: Minimum 4 GB RAM, 10 GB disk space.

## Setup Instructions

1. **Clone Repository**:
   ```bash
   git clone https://github.com/your-username/smart-home-query-recombination.git
   cd smart-home-query-recombination
   ```

2. **Install Java and Maven**:
   ```bash
   sudo apt update
   sudo apt install openjdk-8-jdk maven -y
   java -version  # Should show 1.8.x
   mvn -version   # Should show Maven 3.x
   ```
   - **Windows**: Download Java 8 from Oracle, Maven from Apache, add to PATH.
   - **Mac**: `brew install openjdk@8 maven`.

3. **Install iFogSim**:
   ```bash
   git clone https://github.com/Cloudslab/iFogSim.git ~/iFogSim
   cd ~/iFogSim
   mvn compile
   ```

4. **Copy Simulation File**:
   ```bash
   cp simulation/SmartHomeSimulation.java ~/iFogSim/src/main/java/org/fog/test/perfeval/
   ```

5. **Configure Project**:
   - Ensure all iFogSim JARs are in `~/iFogSim/lib/` and added to the build path in your IDE (e.g., Eclipse: **Project > Properties > Java Build Path > Libraries > Add JARs**).
   - Set Java 8 compiler level (e.g., Eclipse: **Project > Properties > Java Compiler** > 1.8).

## Running Simulations

The simulation supports four configurations based on `cloudMode` (true for cloud-only, false for edge-ward) and `recombinationMode` (true for recombined queries, false for standard). Run `SmartHomeSimulation.java` with the appropriate arguments. Results are saved in `results/`.

### Cloud-Only Configuration
- **Description**: Processes top-k queries on the cloud (~100 ms latency, ~100 kbps bandwidth).
- **Command**:
  ```bash
  cd ~/iFogSim
  mvn compile
  mvn exec:java -Dexec.mainClass="org.fog.test.perfeval.SmartHomeSimulation" -Dexec.args="true false" > results/cloud_only_ifogsim.txt 2>&1
  ```
- **Expected Output**: `results/cloud_only_ifogsim.txt` shows execution time, energy, and network usage (e.g., ~346 ms, ~2,683,218 mJ cloud energy).

### Cloud-Only with Recombination
- **Description**: Processes recombined top-k queries on the cloud with `query_recombiner`.
- **Command**:
  ```bash
  cd ~/iFogSim
  mvn compile
  mvn exec:java -Dexec.mainClass="org.fog.test.perfeval.SmartHomeSimulation" -Dexec.args="true true" > results/cloud_only_recombined_ifogsim.txt 2>&1
  ```
- **Expected Output**: `results/cloud_only_recombined_ifogsim.txt` includes recombination effects on latency and energy.

### Edge-Ward Configuration
- **Description**: Processes top-k queries on Raspberry Pi 4 (~2 ms latency, ~32 kbps bandwidth).
- **Command**:
  ```bash
  cd ~/iFogSim
  mvn compile
  mvn exec:java -Dexec.mainClass="org.fog.test.perfeval.SmartHomeSimulation" -Dexec.args="false false" > results/edge_ward_ifogsim.txt 2>&1
  ```
- **Expected Output**: `results/edge_ward_ifogsim.txt` shows ~260 ms execution time, ~6,184 mJ edge energy.

### Edge-Ward with Recombination
- **Description**: Processes recombined top-k queries on Raspberry Pi 4 with `query_recombiner`.
- **Command**:
  ```bash
  cd ~/iFogSim
  mvn compile
  mvn exec:java -Dexec.mainClass="org.fog.test.perfeval.SmartHomeSimulation" -Dexec.args="false true" > results/edge_ward_recombined_ifogsim.txt 2>&1
  ```
- **Expected Output**: `results/edge_ward_recombined_ifogsim.txt` includes recombination impact.

## Results Interpretation
- **Execution Time**: Time to complete the simulation (e.g., ~346 ms for `cloud_only`, ~260 ms for `edge_ward`).
- **Energy Consumption**: Energy used by devices (e.g., ~2,683,218 mJ cloud for `cloud_only`, ~6,184 mJ edge for `edge_ward`).
- **Network Usage**: Data transferred (e.g., ~388 units for `cloud_only`, ~0 units for `edge_ward`).
- **Recombination Impact**: `recombinationMode` adds `query_recombiner`, potentially increasing latency (~10-20 ms) but optimizing query processing.
- **Limitations**: Null loop delays indicate incomplete actuator integration.

## Latest Changes
- **Argument Structure**: Updated to use `cloudMode` (true/false) and `recombinationMode` (true/false) as command-line arguments.
- **Module Updates**: Renamed modules to `data_collector`, `query_processor`, `query_recombiner`, and `cloud_storage`.
- **File Output**: Results are written to `C:\\Users\\johna\\Downloads\\iFogSim-main\\results\\{config}_ifogsim.txt` with console duplication.
- **Error Handling**: Added try-with-resources for file output and exception handling.

## Troubleshooting
- **iFogSim Errors**:
  - Verify Java 8: `java -version`.
  - Increase JVM memory: `export MAVEN_OPTS="-Xmx2g"`.
- **No Results File**:
  - Ensure `\\iFogSim-main\\results\\` is writable (e.g., adjust permissions).
  - Check console for errors.
- **Simulation Hang**:
  - If stuck at "Starting Simulation...", share the full console output.
- **Argument Errors**:
  - Run with `java SmartHomeSim <cloudMode> <recombinationMode>` (e.g., `true false`).

## Future Work
- Address null loop delays by enhancing actuator logic.
- Optimize energy and latency with real-world sensor data.
- Explore additional recombination strategies.
