```
smart-home-query-recombination/
├── simulation/
│   ├── SmartHomeSimulation.java
├── docs/
│   ├── README.md
│   └── model_diagram.mmd
├── results/
│   ├── edge_ward_ifogsim.txt
│   ├── cloud_only_ifogsim.txt
│   └── edge_cloud_ifogsim.txt
└── requirements.txt
```

## README.md

# Smart Home Temperature Monitoring System

This repository implements a fully simulated Smart Home Temperature Monitoring System using the iFogSim toolkit, based on the Query Recombination framework (Shi et al., 2022). It processes top-k queries (e.g., top 3 warmest rooms) from 5 simulated DS18B20 sensors, using a Raspberry Pi 4 as the edge node and an AWS EC2/S3-like cloud for processing and storage. All functionality, including MQTT-based data collection, top-k query processing, and cloud interactions (simulated EC2 provisioning and S3 storage), is modeled within a single Java file (`SmartHomeSimulation.java`), adapting the structure of `FarmFogSimulation.java`. The simulation runs edge-ward, cloud-only, and edge+cloud configurations, with results logged to text files.

## iFogSim Utilization

The implementation leverages the following iFogSim features:

- **Topology Modeling**: Defines sensors (`Sensor-s1` to `Sensor-s5`), edge node (`raspberry_pi_4`), and cloud node (`aws_ec2_t3_micro`) with realistic parameters (CPU, RAM, storage, energy).
- **Application Modeling**: Uses `AppModule`, `AppEdge`, and `AppLoop` to simulate MQTT data collection, top-k query processing, and cloud storage, replicating Python logic.
- **Network Simulation**: Models MQTT (\~10 ms) and HTTPS (\~100 ms) latencies, bandwidth (\~2.5 kbps edge, \~5 kbps cloud, \~0.1 kbps storage).
- **Energy Modeling**: Tracks energy consumption (\~50 mW sensors, \~5 W edge, \~10 W cloud).
- **Dynamic Deployment**: Uses `ModuleMapping` and `ModulePlacementEdgewards` for module placement across edge and cloud.
- **Simulation Metrics**: Outputs latency, bandwidth, and energy to text files for each configuration.

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
   git clone https://github.com/cloudslab/iFogSim.git ~/iFogSim
   cd ~/iFogSim
   mvn compile
   ```

## Running Simulations

The repository supports three configurations (edge-ward, cloud-only, edge+cloud) within `SmartHomeSimulation.java`. Results are saved in `results/`.

### Edge-Ward Configuration

- **Description**: Processes top-k queries on Raspberry Pi 4 (\~60 ms latency, \~2.5 kbps).
- **iFogSim**:

  ```bash
  cd ~/iFogSim
  cp ~/smart-home-query-recombination/simulation/SmartHomeSimulation.java src/main/java/
  mvn compile
  mvn exec:java -Dexec.mainClass="SmartHomeSimulation" -Dexec.args="edge_ward" > ~/smart-home-query-recombination/results/edge_ward_ifogsim.txt
  ```
  - **Expected Output**: `results/edge_ward_ifogsim.txt` shows \~60 ms latency, \~2.5 kbps bandwidth, \~50 mW sensors, \~5 W edge.

### Cloud-Only Configuration

- **Description**: Processes top-k queries on AWS EC2 (\~150 ms latency, \~5 kbps).
- **iFogSim**:

  ```bash
  cd ~/iFogSim
  cp ~/smart-home-query-recombination/simulation/SmartHomeSimulation.java src/main/java/
  mvn compile
  mvn exec:java -Dexec.mainClass="SmartHomeSimulation" -Dexec.args="cloud_only" > ~/smart-home-query-recombination/results/cloud_only_ifogsim.txt
  ```
  - **Expected Output**: `results/cloud_only_ifogsim.txt` shows \~150 ms latency, \~5 kbps bandwidth, \~50 mW sensors.

### Edge+Cloud Configuration

- **Description**: Processes top-k queries on Raspberry Pi 4 (\~60 ms) and AWS EC2 (\~150 ms), with cloud storage (\~160 ms, \~0.1 kbps).
- **iFogSim**:

  ```bash
  cd ~/iFogSim
  cp ~/smart-home-query-recombination/simulation/SmartHomeSimulation.java src/main/java/
  mvn compile
  mvn exec:java -Dexec.mainClass="SmartHomeSimulation" -Dexec.args="edge_cloud" > ~/smart-home-query-recombination/results/edge_cloud_ifogsim.txt
  ```
  - **Expected Output**: `results/edge_cloud_ifogsim.txt` shows edge loop (\~60 ms), cloud loop (\~150 ms), storage loop (\~160 ms), \~2.5 kbps (edge), \~0.1 kbps (storage).

## 

## Troubleshooting

- **iFogSim Errors**:
  - Verify Java 8: `java -version`.
  - Increase JVM memory: `export MAVEN_OPTS="-Xmx2g"`.
- **Results**:
  - Compare `results/*.txt` for latency, bandwidth, energy across configurations.
  - Expected: Edge-ward (\~60 ms, \~2.5 kbps), cloud-only (\~150 ms, \~5 kbps), edge+cloud (\~60 ms edge, \~150 ms cloud).