import paho.mqtt.client as mqtt
import numpy as np
import json
import boto3
from datetime import datetime
import time

# MQTT settings (simulated)
broker = "broker.hivemq.com"
port = 8883
topics = ["sensors/s1", "sensors/s2", "sensors/s3", "sensors/s4", "sensors/s5"]

# AWS settings (optional)
s3_client = boto3.client("s3", aws_access_key_id="YOUR_KEY", aws_secret_access_key="YOUR_SECRET")
bucket_name = "your-s3-bucket"

# Simulated sensor data (for testing without hardware)
temps = {f"s{i+1}": [] for i in range(5)}
last_upload = time.time()

def on_message(client, userdata, msg):
    sensor = msg.topic.split("/")[-1]
    temp = float(msg.payload.decode())
    temps[sensor].append(temp)
    if len(temps[sensor]) > 6:  # 1 minute (6 readings at 10s)
        temps[sensor].pop(0)

def top_k_query(k, sensors):
    current_temps = [(s, temps[s][-1]) for s in sensors if temps[s]]
    sorted_temps = sorted(current_temps, key=lambda x: x[1], reverse=True)
    return sorted_temps[:k]

def aggregate_data():
    return {s: np.mean(temps[s]) for s in temps if temps[s]}

# Simulate MQTT data (for testing)
def simulate_sensor_data():
    for sensor in temps:
        temps[sensor].append(np.random.uniform(20, 30))  # Random temps 20-30°C

# MQTT client setup
client = mqtt.Client()
client.tls_set()
client.on_message = on_message
client.connect(broker, port)
for topic in topics:
    client.subscribe(topic)
client.loop_start()

while True:
    simulate_sensor_data()  # Simulate sensor data
    top_3 = top_k_query(3, ["s1", "s2", "s3", "s4", "s5"])
    top_2 = top_k_query(2, ["s1", "s2", "s3"])
    print(f"Top 3: {top_3}")
    print(f"Top 2: {top_2}")

    if time.time() - last_upload > 60:
        agg_data = aggregate_data()
        s3_client.put_object(Bucket=bucket_name, Key=f"temps/{datetime.now()}.json", Body=json.dumps(agg_data))
        last_upload = time.time()
    
    time.sleep(10)