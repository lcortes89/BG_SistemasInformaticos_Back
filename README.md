# <img src="src/main/resources/pdf/logo.png" alt="BG Sistemas Informáticos logo" height="48" align="center"> BG Sistemas Informáticos

This is my individual final project for the **Factoría F5 Full Stack Development Bootcamp**.

BG Sistemas Informáticos is a company in Ribadeo (Lugo) that sells and installs POS software, cash management systems and service robots for restaurants and shops. The application rebuilds its website: visitors discover the brands and products, calculate an estimated monthly price and ask for a quote without logging in, and the company manages all the content from a private administration panel.

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/downloads/) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot) [![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security) [![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white)](https://jwt.io/) [![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/) [![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)](https://www.docker.com/) [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/) [![Supabase](https://img.shields.io/badge/Supabase-3FCF8E?logo=supabase&logoColor=white)](https://supabase.com/) [![JUnit 5](https://img.shields.io/badge/tested%20with-JUnit%205-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/) [![Mockito](https://img.shields.io/badge/mocking-Mockito-78A641)](https://site.mockito.org/) [![JaCoCo](https://img.shields.io/badge/coverage-JaCoCo-B22222)](https://www.jacoco.org/jacoco/)

<a id="index"></a>

# 📑 Index

- [🗂️ Project Structure](#structure)
- [✨ Features](#features)
- [📖 Project Documentation](#documentation)
- [👩‍💻 Author](#author)

<a id="structure"></a>

## 🗂️ Project Structure

The project uses a decoupled architecture, with the frontend and backend maintained in separate repositories. This repository contains the backend: a REST API built with Java 21, Spring Boot 4 and Spring Security.

- <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" alt="GitHub" width="20" height="20"> **[Frontend repository](https://github.com/lcortes89/BG_SistemasInformaticos_Front)**
- <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" alt="GitHub" width="20" height="20"> **[Backend repository](https://github.com/lcortes89/BG_SistemasInformaticos_Back)**

[↑ Index](#index) • [Features →](#features)

<a id="features"></a>

## ✨ Features

### 🏠 Home & Solutions

- Explore the website without an account.
- Discover the official brands and their products, with a full product sheet and a link to the maker's page.
- See the featured products on the home page and the hero image chosen by the company.

### 🧮 Rates Wizard

- Answer step-by-step questions about the business.
- Get an **estimated monthly price** calculated by the API.

### ⭐ Clients & Contact

- Read the success stories of the clients, with their testimonial and rating.
- Send a contact form with the product of interest and the GDPR consent.
- The company receives an **email** for every new contact.

### 🛠️ Administration Panel

- Access a private panel restricted to administrators.
- See a summary with counters and the latest contacts.
- Manage the contacts: search, filter by status, add notes and delete.
- Create, edit, hide and delete brands, products and clients, with search, filters and pagination.
- Manage the questions and prices of the rates wizard.
- Export brands, products and clients to **Excel** or **PDF**.
- Upload images and change the hero image and the notification email.
- Invite, block, unblock and delete other administrators.

### 🔐 Security & Account

- Log in with email and password and receive a **JWT**.
- Turn on **two-step verification (2FA)** with an authenticator app and recovery codes.
- Confirm the password again before deleting or making other sensitive changes.
- Reset a forgotten password and change the email with a link sent by email.
- After 5 wrong passwords the login is locked for 15 minutes.

[← Project Structure](#structure) • [↑ Index](#index) • [Project Documentation →](#documentation)

<a id="documentation"></a>

## 📖 Project Documentation

The full project documentation is available in the project Wiki:

### [1. 🏠Home](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki)

### [2. 🚀Getting Started](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/Getting-Started)

### [3. 📋Project Management & User Stories](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/Project-Management-&-User-Stories)

### [4. 🗺️Analysis and Diagrams](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/Analysis-and-Diagrams)

### [5. 🏛️Architecture](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/Architecture)

### [6. 🔐Security](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/Security)

### [7. 🔌API Reference](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/API-Reference)

### [8. 🧪Testing](https://github.com/lcortes89/BG_SistemasInformaticos_Back/wiki/Testing)

[← Features](#features) • [↑ Index](#index) • [Author →](#author)

<a id="author"></a>

## 👩‍💻 Author

**[Luisa María Cortés](https://github.com/lcortes89)**

[← Project Documentation](#documentation) • [↑ Index](#index)
