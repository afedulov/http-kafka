### Start StateFun runtime + Kafka
cd tools
docker-compose -f docker-compose-local.yaml up
### Start remote functions
cd function-local/test
python functions.py
### Start invoke-responses consumer
cd your-kafka-dist/bin
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic invoke-results
### Verify loop via kafka
cd tools/function-local/test
./run.sh
### Verify Spring-based loop
//start spring app
curl localhost:8080/v1alpha1/invocation:in-payload  --request POST  --header "Content-Type: application/json" --header "Accept: application/json"  --data-binary @invocation.json

