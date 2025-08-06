FROM python:3.9-slim
WORKDIR /app
COPY query_processing.py .
RUN pip install paho-mqtt numpy boto3
CMD ["python", "query_processing.py"]