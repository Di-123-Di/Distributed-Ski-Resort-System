# Distributed Ski Resort System

A comprehensive distributed system for managing ski resort lift operations, consisting of multiple components:

## Components

### Client
- Generates lift ride events using multiple threads
- Sends HTTP requests to the server API
- Implements retry logic for failed requests
- Reports performance statistics

### Server (Spring Boot)
- Processes HTTP requests
- Validates data and enforces business rules
- Publishes events to RabbitMQ queue
- Provides RESTful API endpoints
- Deployed on multiple EC2 instances for high availability and load balancing

### Consumer
- Uses 64 concurrent threads to process messages
- Implements dynamic thread scaling (32-128 threads based on load)
- Maintains a thread-safe data store for skier records
- Tracks and reports processing rates and performance statistics

## Technology Stack

- **Java 17**: Core programming language
- **Spring Boot**: Server framework
- **RabbitMQ**: Message queuing (deployed on AWS EC2)
- **Maven**: Build and dependency management
- **Swagger**: API documentation and client generation
- **Concurrent Collections**: Thread-safe data structures
- **AWS EC2**: Deployment platform

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Access to RabbitMQ Server (running on EC2)
- Git

### Installation

1. Clone the repository:
```bash
git clone https://github.com/Di-123-Di/Distributed-Ski-Resort-System.git
cd DistributedSkiResortSystem
```
2. Build the project:
```bash
mvn clean install
```

### Running the Application

1. **Setup RabbitMQ**:
    - Install and run RabbitMQ on your own server or local machine
    - Create a user account with appropriate permissions
    - Note your RabbitMQ host, port, username and password
   

2. **Start the Server**:
```bash
java -jar server-spring/target/server-spring-1.0-SNAPSHOT.jar --spring.rabbitmq.host=YOUR_RABBITMQ_HOST --spring.rabbitmq.port=5672 --spring.rabbitmq.username=YOUR_USERNAME --spring.rabbitmq.password=YOUR_PASSWORD
```

3. **Start the Consumer**:
```bash
java -jar consumer/target/consumer-1.0-SNAPSHOT-jar-with-dependencies.jar --host YOUR_RABBITMQ_HOST --port 5672 --username YOUR_USERNAME --password YOUR_PASSWORD
```

4. **Run the Client:**:
```bash
java -jar client-part1/target/client-part1-1.0-SNAPSHOT-jar-with-dependencies.jar
```


### Configuration

- Server runs on port 8081 by default
- Multiple server instances can be deployed behind a load balancer for scalability
- RabbitMQ connection details:
   - Server configuration in `server-spring/src/main/resources/application.properties`
   - Consumer configuration in `consumer/src/main/java/neu/cs6650/consumer/ConsumerApplication.java`
   - Both configurations should be updated with your RabbitMQ host, port, username and password

### Performance

- The system is capable of processing 3900+ requests per second
- Consumer dynamically scales threads based on queue depth
- Server optimized for high throughput with minimal latency
- Comprehensive error handling and retry mechanisms

### Project Structure
```
DistributedSkiResortSystem/
├── client-part1/               # Client application
├── consumer/                   # Queue consumer application
├── server-spring/              # Spring Boot server
├── swagger-client/             # Swagger-generated API client
└── pom.xml                     # Root Maven POM file
```

### License
This project is proprietary. All rights reserved.
You are NOT allowed to modify, copy, distribute, or use this code for commercial purposes without
the author's explicit permission.
