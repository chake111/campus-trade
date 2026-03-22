# trade (Spring Boot + MyBatis + MySQL)

## Requirements
- Java 17
- Maven
- MySQL 8+ (adjust version as needed)

## MySQL Setup
1. Create database (schema):
   - Database name: `trade`
2. Create table:

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL
);
```

## Configure application
Edit `src/main/resources/application.properties` or set environment variables:
- `DB_URL` (default: `jdbc:mysql://localhost:3306/trade?useSSL=false&serverTimezone=UTC`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default: `root`)

## Run
```bash
mvn clean package
mvn spring-boot:run
```

## Test API
- Create user: `POST http://localhost:8080/api/users`
  - Body:
```json
{
  "username": "alice",
  "email": "alice@example.com"
}
```
- Get user by id: `GET http://localhost:8080/api/users/{id}`

