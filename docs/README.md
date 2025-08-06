```
smart-home-query-recombination/
├── edge/
│   ├── query_processing.py
│   ├── query_processing_edge_cloud.py
│   ├── cloud_provisioning.py
│   └── Dockerfile
├── simulation/
│   ├── SmartHomeSimulation.java
├── docs/
│   ├── README.md
│   └── model_diagram.mmd
├── results/
│   ├── edge_ward_python.txt
│   ├── edge_ward_ifogsim.txt
│   ├── edge_cloud_python.txt
│   ├── edge_cloud_ifogsim.txt
│   └── cloud_only_ifogsim.txt
└── requirements.txt
```

## README.md
<xaiArtifact artifact_id="2299b53e-557b-4e9c-be34-fdc6a45ab308" artifact_version_id="03148fc8-cb26-4eed-b43c-759477bbf685" title="README.md" contentType="text/markdown">

# Smart Home Temperature Monitoring System

This repository implements a Smart Home Temperature Monitoring System using the iFogSim toolkit, based on the Query Recombination framework (Shi et al., 2022). It processes top-k queries (e.g., top 3 warmest rooms) from 5 simulated DS18B20 sensors, using a Raspberry Pi 4 as the edge node and AWS EC2/S3 for cloud processing/storage. The implementation fully utilizes iFogSim's capabilities, with a single Java file (`SmartHomeSimulation.java`) handling edge-ward, cloud-only, and edge+cloud configurations, adapted to a structure similar to `FarmFogSimulation.java`. Cloud provisioning is simulated via Python, and results are validated with Dockerized application logic.

## iFogSim Utilization
The implementation leverages the following iFogSim features:
- **Topology Modeling**: Defines sensors (`ds18b20_sensor`), edge nodes (`raspberry_pi_4`), and cloud nodes (`aws_ec2_t3_micro`) with realistic parameters (CPU, RAM, storage, energy).
- **Application Modeling**: Uses `AppModule`, `AppEdge`, and `AppLoop` to model data flows for top-k queries and aggregation across configurations.
- **Network Simulation**: Models MQTT (~10 ms) and HTTPS (~100 ms) latencies, bandwidth (~2.5 kbps edge, ~5 kbps cloud, ~0.1 kbps storage).
- **Energy Modeling**: Tracks energy consumption (~50 mW sensors, ~5 W edge, ~10 W cloud).
- **Dynamic Deployment**: Uses `ModuleMapping` and `ModulePlacementEdgewards` for module placement, supporting edge and cloud deployments.
- **Simulation Metrics**: Outputs latency, bandwidth, and energy to CSV files for each configuration, validated with Python/Docker logs.

## Prerequisites
- **OS**: Linux (Ubuntu recommended), Windows, or Mac.
- **Software**:
  - Java 8 (`openjdk-8-jdk`)
  - Maven (`maven`)
  - Python 3.9 (`python3.9`, `pip3`)
  - Docker (`docker.io` or Docker Desktop)
  - AWS CLI (`awscli`, required for cloud provisioning)
- **Hardware**: Minimum 4 GB RAM, 10 GB disk space.
- **AWS Account**: Required for cloud simulations (IAM role with `AmazonEC2FullAccess`, `AmazonS3FullAccess`, access keys).

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

3. **Install Python and Dependencies**:
   ```bash
   sudo apt install python3.9 python3-pip -y
   python3.9 --version  # Should show 3.9.x
   pip3 install -r requirements.txt
   pip3 list  # Confirm paho-mqtt==1.6.1, numpy==1.23.5, boto3==1.24.0
   ```

4. **Install Docker**:
   ```bash
   sudo apt install docker.io -y
   sudo systemctl enable docker
   sudo systemctl start docker
   sudo usermod -aG docker $USER
   newgrp docker
   docker --version
   ```
   - **Windows/Mac**: Install Docker Desktop, ensure it’s running.

5. **Install iFogSim**:
   ```bash
   git clone https://github.com/cloudslab/iFogSim.git ~/iFogSim
   cd ~/iFogSim
   mvn compile
   ```

6. **Set Up AWS CLI**:
   ```bash
   pip3 install awscli
   aws configure  # Enter access key, secret key, region (e.g., us-east-1)
   ```

7. **Create AWS Resources**:
   - Run the cloud provisioning script to simulate EC2/S3 setup:
     ```bash
     cd smart-home-query-recombination/edge
     nano cloud_provisioning.py  # Update AWS credentials, region, AMI
     python3.9 cloud_provisioning.py
     ```
   - **Output**: EC2 instance ID, S3 bucket name, logged to `results/cloud_provisioning.txt`.

## Running Simulations
The repository supports three configurations (edge-ward, cloud-only, edge+cloud) within `SmartHomeSimulation.java`, using iFogSim for system-level simulation and Python/Docker for application logic validation. Results are saved in `results/`.

### Edge-Ward Configuration
- **Description**: Processes top-k queries on Raspberry Pi 4 (~60 ms latency, ~2.5 kbps).
- **Python (Edge Processing)**:
  ```bash
  cd smart-home-query-recombination/edge
  docker build -t query-processing .
  docker run -d --name query-processing query-processing
  docker logs query-processing > ../results/edge_ward_python.txt
  docker stop query-processing
  docker rm query-processing
  ```
  - **Expected Output**: `results/edge_ward_python.txt` shows top-k queries every 10s, e.g., `Top 3: [('s1', 25.5), ('s2', 24.0), ('s3', 23.8)]`.
- **iFogSim**:
  ```bash
  cd ~/iFogSim
  cp ~/smart-home-query-recombination/simulation/SmartHomeSimulation.java src/main/java/
  mvn compile
  mvn exec:java -Dexec.mainClass="SmartHomeSimulation" -Dexec.args="edge_ward" > ~/smart-home-query-recombination/results/edge_ward_ifogsim.txt
  ```
  - **Expected Output**: `results/edge_ward_ifogsim.txt` shows ~60 ms latency, ~2.5 kbps bandwidth, ~50 mW sensors, ~5 W edge.

### Cloud-Only Configuration
- **Description**: Processes top-k queries on AWS EC2 (~150 ms latency, ~5 kbps).
- **iFogSim**:
  ```bash
  cd ~/iFogSim
  cp ~/smart-home-query-recombination/simulation/SmartHomeSimulation.java src/main/java/
  mvn compile
  mvn exec:java -Dexec.mainClass="SmartHomeSimulation" -Dexec.args="cloud_only" > ~/smart-home-query-recombination/results/cloud_only_ifogsim.txt
  ```
  - **Expected Output**: `results/cloud_only_ifogsim.txt` shows ~150 ms latency, ~5 kbps bandwidth, ~50 mW sensors.

### Edge+Cloud Configuration
- **Description**: Processes top-k queries on Raspberry Pi 4 (~60 ms) and AWS EC2 (~150 ms), with cloud storage (~160 ms, ~0.1 kbps).
- **Python (Edge+Cloud Processing)**:
  ```bash
  cd smart-home-query-recombination/edge
  nano query_processing_edge_cloud.py  # Set aws_access_key_id, aws_secret_access_key, bucket_name
  docker build -t query-processing-edge-cloud .
  docker run -d --name query-processing-edge-cloud query-processing-edge-cloud
  docker logs query-processing-edge-cloud > ../results/edge_cloud_python.txt
  docker stop query-processing-edge-cloud
  docker rm query-processing-edge-cloud
  ```
  - **Expected Output**: `results/edge_cloud_python.txt` shows edge and cloud top-k queries, e.g., `Edge Top 3: [('s1', 25.5), ...], Cloud Top 3: [('s1', 25.5), ...]`.
  - **Verify S3**:
    ```bash
    aws s3 ls s3://smart-home-query-recombination-2025/temps/cloud_input/
    aws s3 ls s3://smart-home-query-recombination-2025/temps/agg/
    ```
- **iFogSim**:
  ```bash
  cd ~/iFogSim
  cp ~/smart-home-query-recombination/simulation/SmartHomeSimulation.java src/main/java/
  mvn compile
  mvn exec:java -Dexec.mainClass="SmartHomeSimulation" -Dexec.args="edge_cloud" > ~/smart-home-query-recombination/results/edge_cloud_ifogsim.txt
  ```
  - **Expected Output**: `results/edge_cloud_ifogsim.txt` shows edge loop (~60 ms), cloud loop (~150 ms), storage loop (~160 ms), ~2.5 kbps (edge), ~0.1 kbps (storage).

## Diagram
- **File**: `docs/model_diagram.mmd`
- **Render**: Use [Mermaid Live Editor](https://mermaid.live) or VS Code with Mermaid Preview extension. Export as PNG/SVG for reports.

## Troubleshooting
- **Cloud Provisioning Errors**:
  - Verify AWS credentials in `cloud_provisioning.py` and `query_processing_edge_cloud.py`.
  - Ensure IAM role has `AmazonEC2FullAccess`, `AmazonS3FullAccess`.
  - Check EC2 instance status: `aws ec2 describe-instances`.
- **iFogSim Errors**:
  - Verify Java 8: `java -version`.
  - Increase JVM memory: `export MAVEN_OPTS="-Xmx2g"`.
- **Python/Docker Errors**:
  - Ensure MQTT broker (`broker.hivemq.com:8883`) is accessible or use local Mosquitto:
    ```bash
    sudo apt install mosquitto
    sudo systemctl start mosquitto
    # Update query_processing*.py to use 'localhost'
    ```
  - Check Docker permissions: `sudo usermod -aG docker $USER; newgrp docker`.
- **Results**:
  - Compare `results/*.txt` for latency, bandwidth, energy across configurations.
  - Expected: Edge-ward (~60 ms, ~2.5 kbps), cloud-only (~150 ms, ~5 kbps), edge+cloud (~60 ms edge, ~150 ms cloud).