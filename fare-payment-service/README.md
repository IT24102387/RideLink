# RideLink - Fare & Payment Microservice

A core microservice in the **RideLink Microservices Architecture**, responsible for dynamic ride fare estimation, trip fare finalization, simulated payment processing, and seamless inter-service communication with peer microservices (`ride-management-service`, `account-service`, and `driver-and-vehicle-service`).

---

## 📋 Key Features

- **Fare Estimation:** Dynamically computes estimated fares based on ride distance (LKR/km), trip duration (LKR/min), vehicle type multiplier (CAR, BIKE, TUK_TUK, VAN), and surge pricing.
- **Peer Service Inter-connectivity:** Directly integrates with `ride-management-service`'s `PaymentServiceClient` (`/api/v1/fares/estimate` and `/api/v1/fares/calculate`).
- **Resilient Fallback Design:** If peer services are offline during local isolated testing, `fare-payment-service` continues smoothly without errors.
- **Ride Status Synchronization:** Automatically notifies and updates ride completion and payment attachment in `ride-management-service` upon successful payment.
- **Payment Processing:** Supports `CASH` and `CARD` payments with idempotent transaction verification and unique transaction reference (`TXN-...`) generation.
- **Connectivity & Simulation APIs:** Includes `/api/v1/integration/status` for 1-click health diagnostics of peer services and `/api/v1/integration/simulate-complete-flow`.
- **Cloud Database:** Seamless connection to MongoDB Atlas with `.env` and fallback configuration.
- **API Documentation:** Interactive Swagger UI and OpenAPI v3 documentation.

---

## 🛠️ Technology Stack

- **Java 21 (LTS)**
- **Spring Boot 4.1.1** (WebMVC, Data MongoDB, Validation, DevTools)
- **Spring RestClient** with `JdkClientHttpRequestFactory` (HTTP 1.1/2, GET/POST/PATCH)
- **MongoDB Atlas**
- **SpringDoc OpenAPI 3.0.0** (Swagger UI)
- **Project Lombok**
- **Apache Maven Wrapper**

---

## ⚙️ Environment Configuration

Copy `.env.example` to `.env` in the service root directory:

```properties
# Server Port
PORT=8084

# MongoDB Atlas Configuration
MONGODB_URI=mongodb+srv://udaranirmal2001_db_user:YIl2OEYnno18edjn@cluster0.kzznury.mongodb.net/ridelink_fare_payment?retryWrites=true&w=majority&appName=Cluster0
FARE_PAYMENT_MONGODB_URI=mongodb+srv://udaranirmal2001_db_user:YIl2OEYnno18edjn@cluster0.kzznury.mongodb.net/ridelink_fare_payment?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=ridelink_fare_payment

# Peer Microservices URLs
RIDE_SERVICE_URL=http://localhost:8082
ACCOUNT_SERVICE_URL=http://localhost:8080
DRIVER_SERVICE_URL=http://localhost:8081
```

---

## 🚀 Running the Service

### Using Maven Wrapper:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The service starts on port **8084** by default.

---

## 📚 API Endpoints

### 1. Fare Management
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/fares/estimate` | Estimate ride fare (supports vehicle types & surge) |
| `POST` | `/api/v1/fares/calculate` | Calculate final fare breakdown (called by Ride Service) |
| `POST` | `/api/v1/fares/{fareId}/finalize` | Finalize fare after ride completion |
| `GET` | `/api/v1/fares/{fareId}` | Get fare details by Fare ID |
| `GET` | `/api/v1/fares/ride/{rideId}` | Get fare details by Ride ID |

### 2. Payment Processing
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/payments` | Process payment (CASH / CARD) & notify Ride Service |
| `GET` | `/api/v1/payments/{paymentId}` | Get payment receipt by Payment ID |
| `GET` | `/api/v1/payments/ride/{rideId}` | Get payment receipt by Ride ID |

### 3. Inter-Service Diagnostics & Testing
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/integration/status` | Check live status of MongoDB & peer microservices |
| `GET` | `/api/v1/integration/rides/{rideId}` | Fetch ride data directly from Ride Management Service |
| `POST` | `/api/v1/integration/simulate-complete-flow` | 1-click test simulating full fare -> payment -> ride sync |

---

## 🧪 Testing with Postman

Import the updated Postman collection:
📁 [`RideLink-FarePayment.postman_collection.json`](./RideLink-FarePayment.postman_collection.json)

---

## 📖 Interactive Swagger UI

Navigate to:
👉 **`http://localhost:8084/swagger-ui/index.html`**
