### Start StateFun runtime + Kafka
cd tools/local-e2e/docker
docker-compose -f docker-compose-local.yaml up -V
### Start remote functions
cd tools/local-e2e
source venv/bin/activate.fish
python3 -m venv venv
python functions.py
### Start invoke-responses consumer
cd your-kafka-dist/bin
./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic invoke-results
### Verify loop via kafka
cd tools/local-e2-e/test-utils
./run.sh
### Verify Spring-based loop
//start spring app
curl localhost:8080/v1alpha1/invocation:in-payload  --request POST  --header "Content-Type: application/json" --header "Accept: application/json"  --data-binary @invocation.json
### Show counter:
curl localhost:8080/v1alpha1/invocation:in-payload  --request POST  --header "Content-Type: application/json" --header "Accept: application/json"  --data-binary @invocation.json | jq '.response.value' -r | base64 --decode