import paho.mqtt.client as mqtt
import numpy as np
import json
import boto3
import time
from datetime import datetime

# MQTT setup
client = mqtt.Client()
client.tls_set()  # Enable TLS
client.connect("broker.hivemq.com", 8883)
temperatures = {}
last_upload = time.time()
s3_client = boto3.client("s3", aws_access_key_id="YOUR_KEY", aws_secret_access_key="YOUR_SECRET")
bucket_name = "your-s3-bucket"

def on_message(client, userdata, message):
    sensor_id, temp = json.loads(message.payload.decode()).values()
    temperatures[sensor_id] = float(temp)

client.subscribe("smart_home/temperature")
client.on_message = on_message
client.loop_start()

def top_k_query(k, sensors):
    valid_temps = {s: temperatures.get(s, 0) for s in sensors}
    return sorted(valid_temps.items(), key=lambda x: x[1], reverse=True)[:k]

def aggregate_data():
    return {s: temperatures.get(s, 0) for s in temperatures}

while True:
    # Edge processing
    top_3 = top_k_query(3, ["s1", "s2", "s3", "s4", "s5"])
    top_2 = top_k_query(2, ["s1", "s2", "s3"])
    print(f"Edge Top 3: {top_3}")
    print(f"Edge Top 2: {top_2}")

    # Cloud processing
    if time.time() - last_upload > 10:
        temp_data_cloud = json.dumps(temperatures)
        s3_client.put_object(Bucket=bucket_name, Key=f"temps/cloud_input/{datetime.now()}.json", Body=temp_data_cloud)
        cloud_top_3 = top_k_query(3, ["s1", "s2", "s3", "s4", "s5"])
        cloud_top_2 = top_k_query(2, ["s1", "s2", "s3"])
        print(f"Cloud Top 3: {cloud_top_3}")
        print(f"Cloud Top 2: {cloud_top_2}")
        agg_data = aggregate_data()
        s3_client.put_object(Bucket=bucket_name, Key=f"temps/agg/{datetime.now()}.json", Body=json.dumps(agg_data))
        last_upload = time.time()

    time.sleep(10)