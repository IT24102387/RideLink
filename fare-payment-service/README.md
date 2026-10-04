# RideLink - Fare & Payment Microservice

A core microservice in the **RideLink Microservices Architecture**, responsible for dynamic ride fare estimation, trip fare finalization, and secure simulated payment processing.

---

## 📋 Features

- **Fare Estimation:** Dynamically computes estimated fares based on ride distance ($/km) and estimated trip duration ($/min) with a configured base fare.
- **Fare Finalization:** Computes final payable fares when the driver completes a ride.
- **Payment Processing:** Supports `CASH` and `CARD` payments with idempotent transaction verification and unique transaction reference generation.
- **Cloud Database:** Seamless connection to MongoDB Atlas with `.env` and fallback configuration.
- **API Documentation:** Interactive Swagger UI and OpenAPI v3 documentation.
- **Validation & Exception Handling:** Strict Bean Validation (Jakarta) and centralized Global Exception Handler with standardized HTTP error responses.

---

## 🛠️ Technology Stack

- **Java 21 (LTS)**
- **Spring Boot 4.1.1** (WebMVC, Data MongoDB, Validation, DevTools)
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

# MongoDB Database URI (Database-per-service boundary: ridelink_fare_payment)
MONGODB_URI=mongodb+srv://udaranirmal2001_db_user:YIl2OEYnno18edjn@cluster0.kzznury.mongodb.net/ridelink_fare_payment?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=ridelink_fare_payment
```

> [!NOTE]
> `.env` is ignored by Git to ensure database credentials remain secure.

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

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/fares/estimate` | Estimate ride fare based on distance & duration |
| `POST` | `/api/fares/{fareId}/finalize` | Finalize fare after ride completion |
| `GET` | `/api/fares/{fareId}` | Get fare details by Fare ID |
| `GET` | `/api/fares/ride/{rideId}` | Get fare details by Ride ID |
| `POST` | `/api/payments` | Process simulated payment (CASH / CARD) |
| `GET` | `/api/payments/{paymentId}` | Get payment receipt by Payment ID |
| `GET` | `/api/payments/ride/{rideId}` | Get payment receipt by Ride ID |

### Interactive Swagger UI:
Once the service is running, navigate to:
👉 `http://localhost:8084/swagger-ui.html`

---

## 🧪 Testing with Postman

Import the included Postman collection for 1-click testing:
📁 [`RideLink-FarePayment.postman_collection.json`](./RideLink-FarePayment.postman_collection.json)
