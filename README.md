# TechOptima - Enterprise Technology Transformation & Optimization System

TechOptima is an enterprise-grade portfolio optimization platform designed to evaluate legacy application portfolios, select the optimal subset of modernization initiatives within financial constraints, and generate a dependency-respecting execution sequence.

---

## Architecture & Component Design

```
com.techoptima
+-- algorithm
|   +-- graph          # Application Dependency Graph & Builder
|   +-- knapsack       # 0/1 Knapsack Dynamic Programming Optimizer
|   +-- priority       # Multi-criteria Priority Queue & Comparator
|   +-- topology       # Kahn's Algorithm Topological Sorter
+-- database           # Secure JDBC Connection Provider
+-- model              # Domain Entities (Application, Budget, Enums)
+-- repository         # Transactional Data Access (CRUD & Dependencies)
+-- service            # Portfolio Optimization Workflow Orchestrator
+-- validation         # Dependency Closure & Input Validation Rules
+-- web                # Concurrent HTTP Server & Dashboard UI
```

---

## Algorithmic Optimization Pipeline & Complexity Analysis

| Algorithm / Stage | Time Complexity | Space Complexity | Purpose |
|---|:---:|:---:|---|
| **0/1 Knapsack Optimizer** | $\mathcal{O}(N \cdot W)$ | $\mathcal{O}(N \cdot W)$ | Dynamic programming optimization maximizing business benefit within transformation budget. Scaled to minor cent units for exact decimal precision. |
| **Dependency Validator** | $\mathcal{O}(V + E)$ | $\mathcal{O}(V)$ | Validates transitive dependency closure across candidate modernizations. Iteratively prunes unsatisfied dependency branches. |
| **Application PriorityQueue** | $\mathcal{O}(V \log V)$ | $\mathcal{O}(V)$ | Multi-criteria priority drain ordering: `CRITICAL` > `HIGH` > `MEDIUM` > `LOW`, benefit, cost, and ID. |
| **Kahn's Topological Sorter** | $\mathcal{O}(V + E \log V)$ | $\mathcal{O}(V + E)$ | Generates dependency-safe execution sequence with PriorityQueue tie-breaking and cycle detection. |

---

## Database Schema & Relational Design

- **`applications`**:
  - `application_id` (BIGINT PRIMARY KEY)
  - `application_name` (VARCHAR(150))
  - `modernization_cost` (DECIMAL(15,2))
  - `business_benefit` (INT)
  - `criticality` (ENUM: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
  - `department` (ENUM: `SALES`, `FINANCE`, `OPERATIONS`, `MARKETING`, `INFORMATION_TECHNOLOGY`, `HUMAN_RESOURCES`, `CUSTOMER_SERVICE`)

- **`application_dependencies`**:
  - `application_id` (BIGINT)
  - `dependency_application_id` (BIGINT)
  - Composite Primary Key `(application_id, dependency_application_id)`
  - Foreign key relations to `applications` with transactional cascade safety.

- **`transformation_budget`**:
  - `budget_id` (BIGINT PRIMARY KEY AUTO_INCREMENT)
  - `budget_amount` (DECIMAL(15,2))

---

## Web & REST Endpoints

| HTTP Method | Route | Description |
|---|---|---|
| `GET` | `/` | Main dashboard displaying current budget, application table, and actions. |
| `POST` | `/budget` | Sets or updates the global transformation budget. |
| `POST` | `/application` | Registers a new enterprise application and dependencies. |
| `GET` | `/application/edit?id={id}` | Prefilled edit form for modifying application parameters. |
| `POST` | `/application/update` | Updates application details and dependency mappings. |
| `POST` | `/application/delete` | Deletes an application and cleans up associated dependencies. |
| `GET` | `/optimize` | Executes the end-to-end optimization pipeline and renders recommendations. |

---

## Security & Reliability Safeguards

- **SQL Injection Prevention:** 100% parameterized `PreparedStatement` queries with typed placeholders.
- **Output Escaping:** Strict HTML escaping on all rendered entities to prevent Cross-Site Scripting (XSS).
- **HTTP Security Headers:** Emits `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, and `Content-Security-Policy`.
- **Path Traversal Protection:** Normalized frontend root bounds checking on all static asset requests.
- **Error Masking:** Database and internal exceptions return clean HTTP error pages without leaking stack traces or credentials.
- **Configuration Security:** Database credentials and port configurations are read strictly from environment variables.

---

## Deployment Guide (Render, Railway & Vercel)

### 1. Deploying on Render
Render supports native Docker web services with automated SSL:
1. Create a new **Web Service** on [Render](https://render.com) and link your GitHub repository.
2. Select **Docker** environment (Render will automatically detect the root `Dockerfile`).
3. Add a managed **MySQL Database** on Render (or external MySQL instance).
4. Configure the following **Environment Variables** in the Render Dashboard:
   - `TECHOPTIMA_DB_URL`: `jdbc:mysql://<db-host>:3306/<db-name>?useSSL=false&serverTimezone=UTC`
   - `TECHOPTIMA_DB_USER`: `<db-user>`
   - `TECHOPTIMA_DB_PASSWORD`: `<db-password>`
   - `TECHOPTIMA_PORT`: `8080`
5. Deploy! Render will build the multi-stage Docker container and expose your app.

### 2. Deploying on Railway
Railway provides seamless container deployments and one-click MySQL provisioning:
1. Create a **New Project** on [Railway](https://railway.app) and select **Provision MySQL**.
2. Click **New Service** -> **GitHub Repo** and connect TechOptima.
3. Under Service **Settings** -> **Variables**, link Railway's MySQL environment variables:
   - `TECHOPTIMA_DB_URL`: `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&serverTimezone=UTC`
   - `TECHOPTIMA_DB_USER`: `${{MySQL.MYSQLUSER}}`
   - `TECHOPTIMA_DB_PASSWORD`: `${{MySQL.MYSQLPASSWORD}}`
   - `PORT`: `8080`
4. Railway uses the committed `railway.json` and `Dockerfile` to build and launch the service.

### 3. Deploying on Vercel
Vercel can host the static frontend assets and reverse-proxy requests to your Render/Railway backend:
1. Import the repository on [Vercel](https://vercel.com).
2. Set the Root Directory to `./`.
3. Set the Environment Variable:
   - `TECHOPTIMA_BACKEND_URL`: `your-backend-app.onrender.com` (or Railway domain).
4. Vercel utilizes `vercel.json` to route static assets and forward backend API calls.

---

## Local Development & Testing

### 1. Environment Variables
```bash
export TECHOPTIMA_DB_URL="jdbc:mysql://localhost:3306/techoptima?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export TECHOPTIMA_DB_USER="root"
export TECHOPTIMA_DB_PASSWORD="your_password"
export TECHOPTIMA_PORT=8080
```

### 2. Build & Package
```bash
mvn clean package
```

### 3. Run Web Server
```bash
java -jar backend/target/TechOptima-1.0-SNAPSHOT.jar
```

### 4. Run Automated Test Suite (97 Tests)
```bash
mvn test
```
