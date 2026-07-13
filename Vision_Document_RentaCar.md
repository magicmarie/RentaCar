# Vision Document for "AI Assisted RentaCar - Car Rental Management System"

Mariam Natukunda
618983

---

## 1. Introduction

Managing a car rental business today involves tracking vehicle availability, processing
customer reservations, handling check-ins and check-outs, and generating accurate billing
records. In small operations, this is often done using spreadsheets and paper forms —
a manual process that is slow, error-prone, and difficult to scale.

As a rental business grows its fleet and customer base, the complexity of managing
concurrent reservations across multiple vehicle categories increases significantly. Staff
must quickly determine which vehicles are available for a given date range, process
bookings efficiently, and ensure that no vehicle is double-booked. Customers expect
a convenient way to browse available vehicles and make reservations online, at any time.

AI Assisted RentaCar is a new web-based software system that will provide a centralized
platform for managing all aspects of a car rental business: fleet inventory, customer
accounts, reservations, vehicle check-out and return processing, and billing. The
system also offers customers a simple AI-assisted vehicle recommendation to help them
pick a suitable vehicle for their trip. The system will serve three types of users —
administrators, counter staff, and customers — each with their own role-specific
interface and privileges.

---

## 2. Positioning

### 2.1 Problem Statement

| | |
|---|---|
| The problem of | managing vehicle fleet availability, customer reservations, and rental billing |
| Affects | rental business administrators, counter staff, and customers |
| the impact of which is | double bookings, inaccurate availability information, slow check-in/check-out processes, and inconsistent billing |
| a successful solution would be | one integrated system that manages vehicle inventory, automates availability checking, processes reservations and returns, and generates accurate bills — with a user-friendly interface accessible to administrators, staff, and customers, and a simple AI-assisted recommendation to help customers pick the right vehicle |

### 2.2 Product Position Statement

| | |
|---|---|
| For | car rental businesses and their customers |
| Who | need an efficient and reliable way to manage vehicle reservations and fleet operations |
| The (AI Assisted RentaCar) is | a web-based car rental management application |
| That | automates the full rental lifecycle from reservation to return, with real-time availability checking, automated billing, and a simple AI-assisted vehicle recommendation |
| Unlike | manual spreadsheet tracking or fragmented paper-based processes |
| Our product | provides a single integrated platform accessible to admins, staff, and customers with role-based access control, a shared database, and a lightweight AI-assisted recommendation that helps customers pick a suitable vehicle |

---

## 3. Stakeholder Descriptions

### 3.1 Stakeholder Summary

| Name | Description | Responsibilities |
|---|---|---|
| Admin | Admins configure and manage the system, including the vehicle fleet, pricing, and user accounts | Admins are responsible for adding, editing, or removing vehicles and vehicle categories; setting pricing; creating staff and customer accounts; and overseeing overall system data |
| Counter Staff | Staff handle in-person customer interactions at the rental location | Staff are responsible for verifying customer reservations, processing vehicle check-outs, recording vehicle returns, and confirming billing |
| Customers | Customers browse available vehicles and make or manage their own reservations online | Customers are responsible for providing accurate personal information, selecting a vehicle and date range, and returning vehicles on time |
| Developers | Developers build and maintain the AI Assisted RentaCar system | Developers are responsible for implementing system features, fixing bugs, and maintaining system availability |
| Testers | Testers validate system functionality using JUnit and integration testing tools | Testers are responsible for verifying that all features work correctly and that business rules are properly enforced |

### 3.2 User Environment

The system will be used across three contexts:

- **Administrators and Counter Staff** will access the system from desktop or laptop
  computers in an office or front-desk environment, using a standard web browser.
  Staff interact with the system frequently throughout the day — processing check-outs,
  recording returns, and responding to customer inquiries.

- **Customers** will access the system from any device (desktop, mobile, or tablet)
  via a web browser, primarily to make reservations before visiting the rental location.

- **Platform**: AI Assisted RentaCar is a web application built on Java with Spring MVC and JPA
  for database persistence. It will be deployed on a Java-compatible application server
  and accessed through modern web browsers. No dedicated mobile app is required for
  the initial release.

The system will store all data in a relational database. No integration with external
payment gateways is required in the initial release — billing records are tracked
internally by the system.

---

## 4. Product Overview

### 4.1 Product Perspective

AI Assisted RentaCar is a standalone web application with a relational database
backend. It consists of three major subsystems:

- **Fleet Management Subsystem**: Manages the vehicle inventory, categories, and
  pricing configuration.
- **Reservation Subsystem**: Handles bookings, real-time availability checking,
  scheduling constraints, and a simple AI-assisted recommendation that suggests
  a suitable vehicle to a customer based on their stated trip needs.
- **Customer & Billing Subsystem**: Manages customer accounts and generates
  rental bills upon vehicle return.

These subsystems share a common database and are accessed through a role-based
web interface that presents different views and capabilities to admins, staff,
and customers.

### 4.2 Assumptions and Dependencies

- The system will be deployed on a server with Java EE support (e.g., Apache Tomcat).
- A relational database (e.g., MySQL) will be available for data persistence.
- Users will access the system via a modern web browser (Chrome, Firefox, Edge, or Safari).
- No external payment gateway integration is required for the initial release;
  billing amounts are calculated and stored internally.
- Each vehicle belongs to exactly one vehicle category (e.g., Economy, SUV, Luxury).
- A customer must have a registered account to make a reservation.
- Pricing is defined per vehicle category and calculated based on the number of
  rental days.
- The system will support a single rental location for the initial release.
- The vehicle recommendation is generated using simple rule-based filtering
  (e.g., passenger count, budget, vehicle category) rather than a trained
  machine learning model or third-party AI service.

### 4.3 Needs and Features

| No | Problem | Need | Priority | Features | Planned Release |
|---|---|---|---|---|---|
| **Fleet Admin** | | | | | |
| 1 | The business manages a fleet of vehicles of different types | Vehicle inventory must be maintained in the system | High | Admin must be able to add, edit, or delete vehicles in the fleet database, including details such as make, model, year, category, and license plate | |
| 2 | Vehicles belong to categories that determine pricing | Vehicle categories must be defined in the system | High | Admin must be able to add or delete vehicle categories (e.g., Economy, SUV, Luxury) and assign vehicles to a category | |
| 3 | Different categories have different rental pricing | Rental rates must be defined per category | High | Admin must be able to set and update a daily rental rate for each vehicle category | |
| 4 | Staff accounts must be managed | Only authorized staff should access admin functions | High | Admin must be able to create, update, or deactivate staff user accounts with appropriate roles | |
| **Reservations** | | | | | |
| 5 | Customers need to see which vehicles are available for their dates | Real-time availability must be shown to the customer | High | The system must display available vehicles for a customer-specified date range, filtered by category | |
| 6 | Customers want to reserve a vehicle online | Customers must be able to book a vehicle | High | Customers must be able to select an available vehicle and submit a reservation with a start date and end date after logging in | |
| 7 | A vehicle cannot be rented to two customers at the same time | Double-booking must be prevented | High | The system must enforce availability constraints and reject any reservation that overlaps with an existing confirmed reservation for the same vehicle | |
| 8 | Customers may need to cancel a reservation | Cancellation must be supported | Medium | Customers must be able to cancel a pending reservation. Staff and admins must also be able to cancel on behalf of a customer | |
| 9 | Staff process vehicle pick-ups | The start of a rental must be recorded | High | Staff must be able to confirm a reservation and mark a vehicle as checked out, recording the actual pick-up date and time | |
| 10 | Staff process vehicle returns | The end of a rental must be recorded | High | Staff must be able to record a vehicle return, including the return date and any notes about the vehicle condition | |
| **Customers** | | | | | |
| 11 | Customers need a personal account to make reservations | Customer registration and login must be supported | High | Customers must be able to register with their personal details (name, email, driver's license number) and log in with a username and password | |
| 12 | Customers want to review their past and current reservations | Reservation history must be accessible | Medium | Customers must be able to view a list of their reservations — both active and past — after logging in | |
| **Billing** | | | | | |
| 13 | Rentals must be charged based on the number of days and vehicle category | The system must calculate the rental cost automatically | High | The system must calculate the total rental cost upon vehicle return: daily rate × number of days rented | |
| 14 | Customers and staff need a record of the transaction | A bill must be generated at the end of each rental | Medium | The system must generate a billing summary for each completed rental, showing vehicle details, rental dates, number of days, daily rate, and total amount due | |
| **AI-Assisted Features** | | | | | |
| 15 | Customers may not know which vehicle category best fits their trip | Customers need guidance choosing a suitable vehicle | Low | The system must recommend a vehicle to a customer based on simple inputs such as passenger count and budget, using rule-based filtering over available vehicles | |
| **System** | | | | | |
| 16 | All vehicles must be accounted for at all times | No vehicle should have an unknown status | High | The system must track vehicle status at all times: Available, Reserved, Rented, or Under Maintenance | |
| 17 | Admins need an overview of business activity | Summary reports are needed for decision-making | Low | The system must provide admins with a basic dashboard showing fleet status, active rentals, and upcoming reservations | |

### 4.4 Alternatives and Competition

Alternatives available to a car rental business include:

- **Manual spreadsheet management**: Low cost but highly error-prone, does not
  enforce availability rules, and does not scale with business growth.
- **Commercial rental software (e.g., Rent Centric, HQ Rental Software)**:
  Feature-rich but expensive, requires licensing fees, and may include unnecessary
  complexity for a small to mid-size operation.
- **General-purpose booking platforms**: Not tailored to the specific workflows
  of car rental (e.g., vehicle check-out/check-in, condition tracking, fleet
  categorization).

AI Assisted RentaCar differentiates itself by offering a purpose-built, lightweight,
and customizable system aligned to the specific needs of a car rental operation,
built on an open-source Java stack with no licensing overhead. It also gives
customers a simple AI-assisted vehicle recommendation, which most spreadsheet-based
and commercial alternatives do not offer.

---

## 5. Other Product Requirements

**Platform & Standards**
- The system must be implemented using Java, Spring MVC, and JPA as the core
  technology stack, consistent with the course technology requirements.
- The system must be deployable on Apache Tomcat and use a standard relational
  database (MySQL or equivalent).

**Performance**
- The system must respond to availability search queries within 3 seconds under
  normal load.
- The reservation submission process must complete within 5 seconds.

**Usability**
- The user interface must be intuitive enough for counter staff to use with minimal
  training.
- Customer-facing pages must be accessible from both desktop and mobile browsers.

**Reliability**
- Reservation data must be persisted reliably; no confirmed reservation should be
  lost due to a system error.

**Security**
- User authentication is required for all non-public pages.
- Role-based access control must ensure customers cannot access admin or staff
  functions, and staff cannot access admin configuration.

**Documentation**
- The system must be accompanied by a user manual covering the admin, staff,
  and customer workflows.
- All code must be documented with inline comments where logic is non-obvious.
