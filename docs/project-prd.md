
---

# PRODUCT REQUIREMENTS DOCUMENT (PRD)

# Binimoy – Book Exchange Platform

---

# 1. Introduction

## 1.1 Project Title

**Binimoy – Book Exchange Platform**

## 1.2 Project Context

"Binimoy" is a web-based platform designed to connect readers and allow them to exchange books within a community. The platform enables users to list books they own, discover books from other users, and request exchanges.

The system also includes a delivery mechanism for exchanging physical books between users and calculates delivery charges based on the geographic distance between participants.

This project combines concepts from database design, full-stack web development, and software engineering practices to demonstrate a complete production-ready application.

The project was inspired by the **KUET CSE database course project environment**, where the system concept draws from implementations using **Next.js, Node.js, and Oracle SQL**, and integrates additional software engineering requirements including **Spring Boot architecture, DevOps pipeline, Dockerization, testing, and deployment**.

---

# 2. Problem Statement

Students and readers often purchase books that they use only once or for a limited period. After usage, these books remain unused and are rarely reused by others.

Existing book-sharing solutions are often:

* geographically limited
* unstructured
* unsafe
* lacking accountability
* inefficient in terms of logistics

The absence of a centralized digital platform prevents efficient sharing and reuse of books within communities.

This project addresses this problem by building a **secure, scalable, and transparent platform for exchanging books**.

---

# 3. Project Objectives

The objectives of the system include:

1. Create a **community-driven book exchange platform**
2. Allow users to **list books they own**
3. Enable users to **discover books available for exchange**
4. Provide a **structured exchange request system**
5. Implement **secure user authentication**
6. Provide **delivery management for physical books**
7. Automatically calculate **delivery costs based on distance**
8. Ensure **data security and privacy**
9. Maintain **exchange history for transparency**
10. Demonstrate **full software engineering workflow including DevOps and deployment**

---

# 4. Target Users

The platform is designed primarily for:

* university students
* academic communities
* book enthusiasts
* local reading communities

---

# 5. System Overview

The system operates as a **book-sharing marketplace** where users can:

1. Register and create accounts
2. List books they own
3. Browse books listed by other users
4. Request exchanges
5. Deliver books via a managed delivery system
6. Confirm exchange completion

The platform also includes administrative monitoring to ensure system integrity.

---

# 6. User Roles

The system includes three main roles.

---

# 6.1 User (Reader)

The primary user of the platform who exchanges books.

Capabilities include:

* account creation
* authentication
* profile management
* book listing
* browsing books
* requesting exchanges
* responding to exchange requests
* tracking exchanges
* confirming exchange completion

---

# 6.2 Deliveryman

Delivery personnel responsible for transporting books between users.

Capabilities include:

* viewing assigned deliveries
* updating delivery status
* confirming pickups
* confirming deliveries

---

# 6.3 Administrator

Responsible for managing the platform.

Capabilities include:

* viewing platform analytics
* managing users
* moderating books
* monitoring exchanges
* managing deliveries
* disabling suspicious accounts

---

# 7. Functional Requirements

---

# 7.1 User Registration and Authentication

Users must create accounts to access the platform.

Registration requires:

* full name
* email address
* password
* location

Authentication features include:

* login
* logout
* session management
* role-based authorization

Passwords are stored using secure hashing techniques to protect user data.

---

# 7.2 Profile Management

Users can manage their personal profiles.

Profile information includes:

* name
* email
* location
* listed books
* exchange history
* active exchanges

Users can update their profile information when necessary.

---

# 7.3 Book Listing

Users can upload books they want to exchange.

Each book listing contains:

* title
* author
* description
* condition
* market price
* availability
* book images
* owner information

Books become visible in the marketplace after submission.

---

# 7.4 Book Discovery

Users can explore books available for exchange.

Features include:

* search functionality
* browsing categories
* viewing book details
* viewing book owners
* checking availability status

---

# 7.5 Exchange Request System

Users initiate exchanges by requesting books.

Exchange process:

1. User browses available books
2. User selects a book
3. User submits an exchange request
4. User selects one of their books as an offer
5. Owner receives request notification
6. Owner accepts or rejects request

Exchange statuses include:

* pending
* accepted
* rejected
* in delivery
* completed

---

# 7.6 Delivery Management

After an exchange is accepted, the system arranges delivery.

Delivery process:

1. Exchange accepted
2. System calculates distance between users
3. Delivery cost calculated
4. Deliveryman assigned
5. Books collected
6. Books delivered

Delivery statuses include:

* assigned
* picked up
* in transit
* delivered

---

# 7.7 Delivery Cost Calculation

Delivery charges are calculated based on distance.

Formula:

Delivery Cost = Base Fee + (Distance × Rate per KM)

Where:

Base Fee = fixed starting cost
Rate per KM = delivery cost per kilometer
Distance = geographic distance between users

Distance can be calculated using geographic coordinates.

---

# 7.8 Exchange Confirmation

After delivery, both users confirm the exchange.

Once confirmed:

Exchange status becomes **completed**.

This ensures accountability and prevents fraud.

---

# 8. Non-Functional Requirements

---

# 8.1 Performance

The system must support fast retrieval of book listings.

Performance optimizations include:

* optimized SQL queries
* indexed tables
* caching where necessary

---

# 8.2 Security

Security mechanisms include:

* password hashing
* secure authentication
* role-based access control
* protected API endpoints

---

# 8.3 Scalability

The modular architecture allows scaling of:

* users
* books
* exchange records
* delivery operations

---

# 8.4 Reliability

All transactions are recorded in the database to ensure system integrity.

---

# 9. System Architecture

The platform follows a layered architecture.

Layers include:

1. Controller Layer
2. Service Layer
3. Repository Layer
4. Entity Layer
5. DTO Layer

Data flow:

Client → Controller → Service → Repository → Database

This architecture ensures separation of concerns and maintainability.

---

# 10. Frontend Architecture

The user interface is implemented using **Next.js**.

Frontend responsibilities include:

* page rendering
* UI interaction
* API requests
* user authentication
* displaying book listings
* managing user actions

Key pages include:

* login page
* registration page
* book marketplace
* book details page
* exchange requests
* profile page
* delivery dashboard
* admin dashboard

---

# 11. Backend Architecture

Two backend implementations are considered.

---

# Node.js Backend (KUET CSE Database Project Version)

Handles:

* API endpoints
* authentication
* database queries
* file upload
* business logic

---

# Spring Boot Backend (Software Engineering Lab Version)

Uses layered architecture with modules including:

Controllers
Services
Repositories
Entities
DTOs
Mappers
Security
Configurations
Utilities
Exception handlers

---

# 12. Database Design

The database is designed using **Boyce-Codd Normal Form (BCNF)**.

Benefits include:

* minimal redundancy
* improved data consistency
* easier maintenance
* efficient queries

---

# 12.1 Database Technologies

KUET Database Project Version

Oracle SQL

Software Engineering Lab Implementation

PostgreSQL

---

# 12.2 Core Database Tables

User
Role
Book
ExchangeRequest
Delivery

---

# 12.3 Database Relationships

Role → User (1:M)

User → Book (1:M)

Book → ExchangeRequest (1:M)

ExchangeRequest → Delivery (1:1)

---

# 13. Media Storage

User-uploaded book images are stored in cloud storage.

Advantages include:

* reduced server storage usage
* global accessibility
* improved scalability

---

# 14. REST API Design

Examples of API endpoints:

Authentication

POST /api/auth/register
POST /api/auth/login

Books

GET /api/books
POST /api/books
PUT /api/books/{id}
DELETE /api/books/{id}

Exchange

POST /api/exchange/request
POST /api/exchange/{id}/accept
POST /api/exchange/{id}/reject

Delivery

POST /api/delivery/assign
PUT /api/delivery/{id}/status

---

# 15. Testing Strategy

Testing tools include:

* JUnit
* Mockito
* SpringBootTest
* MockMvc

Testing includes:

Service layer unit tests

Controller integration tests

Minimum requirements:

15 unit tests
3 integration tests

---

# 16. Dockerization

The system is containerized using Docker.

Required files:

Dockerfile
docker-compose.yml

Containers include:

* application container
* database container

The application can be started using:

docker compose up --build

---

# 17. Git Workflow

Branching strategy:

main → production
develop → integration branch
feature branches → development

Rules:

* no direct push to main
* pull requests required
* code review required

---

# 18. CI/CD Pipeline

CI/CD is implemented using GitHub Actions.

Pipeline steps:

1. build application
2. run tests
3. build docker image
4. deploy automatically

Deployment occurs when code merges into main.

---

# 19. Deployment

The application is deployed using cloud platforms such as Render.

Deployment requirements include:

* public application URL
* GitHub repository
* automated deployment pipeline

---

# 20. Deliverables

Final project deliverables include:

* GitHub repository
* deployed application URL
* system documentation
* architecture diagrams
* ER diagram
* API documentation
* Docker configuration
* CI/CD pipeline
* project presentation

---

# 21. Conclusion

**Binimoy – Book Exchange Platform** demonstrates the design and implementation of a full-stack web system that integrates modern software engineering practices including secure authentication, optimized database design, modular architecture, containerized deployment, and automated CI/CD workflows.

The system provides an efficient, secure, and scalable solution for community-based book sharing.

---

