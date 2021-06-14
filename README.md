# HTTP-over-Kafka for Flink StateFun

🎆 Ververica 2021 Hackathon 🎆

Enable interactive queries to StateFun clusters over HTTP without sacrificing the unique features of Statefun (processing guarantees, etc.). 

## Goals

* Create an HTTP server that synchronizes messages to StateFun with a Kafka Request-Reply architecture
* Allow HTTP polling on a “request ID” for either the already completed or still pending result
* Embed this HTTP server (w/o Kafka :question_mark:) in the StateFun Runtime 
