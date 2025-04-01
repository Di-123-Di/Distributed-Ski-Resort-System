# Distributed Ski Resort System

A comprehensive distributed system for managing ski resort lift operations, consisting of multiple components deployed across AWS EC2 instances.
## Components

### Client
- Generates lift ride events using multiple threads
- Sends HTTP requests to the server API
- Implements retry logic for failed requests
- Implements circuit breaker pattern for handling server errors
-  Reports performance statistics

### Server (Spring Boot)
- Processes HTTP requests
- Validates data and enforces business rules
- Publishes events to RabbitMQ queue
- Provides RESTful API endpoints
- Deployed on multiple EC2 instances for high availability and load balancing

### Consumer
- Uses multiple concurrent threads to process messages
- Implements dynamic thread scaling (32-128 threads based on queue load)
- Persists data in Amazon DynamoDB for durable storage
- Provides a query API for data retrieval and analysis
- Tracks and reports processing rates and performance statistics


### RabbitMQ

- Message broker for asynchronous processing
- Deployed on a dedicated EC2 instance
- Ensures reliable message delivery

## Technology Stack

- **Java 17**: Core programming language
- **Spring Boot**: Server framework
- **RabbitMQ**: Message queuing
- **Amazon DynamoDB**: NoSQL database for data persistence
- **AWS SDK for Java**: DynamoDB integration
- **Maven**: Build and dependency management
- **Swagger**: API documentation and client generation
- **Concurrent Collections**: Thread-safe data structures

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Access to AWS EC2 instances
- AWS Account with DynamoDB access
- SSH key pair for EC2 access

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
### DynamoDB Setup

1. Create a DynamoDB table named SkierLiftRides with the following structure:

   - Primary Key:

     - Partition Key: skierID (Number)
     - Sort Key: timestamp_liftID (String)

2. Create a Global Secondary Index named ResortDayIndex:

     - Partition Key: resortDay (String)
     - Sort Key: timestamp (String)
     - Projection Type: ALL


### Deployment Architecture
The system is deployed using multiple AWS EC2 instances for high availability and scalability:

- **Server**: Deployed on 2 EC2 instances behind a load balancer
- **Consumer**: Deployed on 1 EC2 instance
- **RabbitMQ**: Deployed on a dedicated EC2 instance
- **DynamoDB**: Managed AWS service (no EC2 instance required)

### Running the Application

#### Starting the Services
1. **Start RabbitMQ Server**:
```bash
ssh -i ~/path/to/your-key-pair.pem ubuntu@[rabbitmq-instance-ip]
sudo systemctl status rabbitmq-server  # Check status
sudo systemctl start rabbitmq-server   # Start if needed
```

2. **Start Consumer Service**:
```bash
   ssh -i ~/path/to/your-key-pair.pem ubuntu@[consumer-instance-ip]
   java -jar consumer-1.0-SNAPSHOT-jar-with-dependencies.jar --host [rabbitmq-instance-ip] --username myuser --password mypassword --threads 64
```

3. **Start Server Instances (both instances)**:
```bash
# First server instance
ssh -i ~/path/to/your-key-pair.pem ubuntu@[server1-instance-ip]
chmod +x start-spring-server.sh
./start-spring-server.sh [rabbitmq-instance-ip]

# Second server instance
ssh -i ~/path/to/your-key-pair.pem ubuntu@[server2-instance-ip]
chmod +x start-spring-server.sh
./start-spring-server.sh [rabbitmq-instance-ip]
```

4. **Run Client**:
```bash
java -jar client-part1/target/client-part1-1.0-SNAPSHOT-jar-with-dependencies.jar --server http://[your-load-balancer-dns]
```




### Accessing the Services
- Server API: Available at http://load-balancer-dns-or-ip:8081
- Consumer Query API: Available at http://consumer-ec2-ip:8082/api
- RabbitMQ Management Console: Available at http://rabbitmq-ec2-ip:15672 (if enabled)

### Configuration

- Server runs on port 8081 by default
- Consumer API runs on port 8082 by default
- RabbitMQ connection details can be configured through command line arguments:

  - Host: Default is localhost, override with --host
  - Port: Default is 5672, override with --port
  - Username: Default is myuser, override with --username
- Password: Default is mypassword, override with --password

- For the server, RabbitMQ host is passed as a parameter to the start script

### Data Access API
The Consumer API provides the following endpoints:

- GET /api/skiers/{skierId}/seasons/{seasonId}/days/count - Get days count for a skier
- GET /api/skiers/{skierId}/seasons/{seasonId}/vertical - Get vertical totals by day
- GET /api/skiers/{skierId}/seasons/{seasonId}/days/{dayId}/lifts - Get lifts ridden on a specific day
- GET /api/resorts/{resortId}/seasons/{seasonId}/days/{dayId}/skiers/count - Get unique skiers count
- GET /api/stats - Get overall processing statistics

### Consumer API Endpoints
The Consumer API provides the following endpoints:

   - GET /api/skiers/{skierId}/seasons/{seasonId}/days/count - Get days count for a skier
   - GET /api/skiers/{skierId}/seasons/{seasonId}/vertical - Get vertical totals by day
   - GET /api/skiers/{skierId}/seasons/{seasonId}/days/{dayId}/lifts - Get lifts ridden on a specific day
   - GET /api/resorts/{resortId}/seasons/{seasonId}/days/{dayId}/skiers/count - Get unique skiers count
   - GET /api/stats - Get overall processing statistics



### Performance

- The system is capable of processing 3900+ requests per second
- Consumer dynamically scales threads based on queue depth
- DynamoDB configured for high throughput with appropriate capacity units
- Server optimized for high throughput with minimal latency
- Comprehensive error handling and retry mechanisms



### License
This project is proprietary. All rights reserved.
You are NOT allowed to modify, copy, distribute, or use this code for commercial purposes without
the author's explicit permission.
