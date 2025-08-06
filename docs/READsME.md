Smart Home Temperature Monitoring System (iFogSim Simulation)
This repository simulates a smart home temperature monitoring system using the Query Recombination framework (Shi et al., 2022). The system models 5 DS18B20 sensors, a Raspberry Pi 4 edge node, and optional AWS S3 storage, using MQTT for communication and iFogSim for performance validation.
Setup Instructions

Install Dependencies: Java 8, Maven, Python 3.9, Docker.
Simulation: Run iFogSim with simulation/ifogsim_config.json and SmartHomeSimulation.java.
Edge Processing: Simulate edge node with edge/query_processing.py in Docker.
Validation: Compare iFogSim metrics (latency, bandwidth) with real-world assumptions.

Artifacts

Edge processing: edge/query_processing.py
iFogSim config: simulation/ifogsim_config.json
iFogSim runner: simulation/SmartHomeSimulation.java

Requirements

Software: Java 8, Maven, Python 3.9, Docker, iFogSim.
Network: Simulated MQTT (10 ms latency), HTTPS (100 ms for cloud).

Validation

Latency: ~60 ms (10 ms MQTT + 50 ms processing).
Bandwidth: ~5 kbps raw, ~2.5 kbps aggregated.
Energy: ~50 mW (sensors), ~5 W (edge, simulated).

# smart-home-query-recombination

