# Pharmacy Dispensing and Inventory System

A Java 21 application for pharmacy dispensing workflows, inventory management, controlled-substance validation, and replenishment suggestions.

## Features

### Inventory and FEFO Dispensing
- Manage drugs, batches, lot numbers, expiration dates, and quantities.
- Apply First Expired, First Out (FEFO) allocation.
- Split fills across eligible batches.
- Reject expired, recalled, or quarantined stock.
- Check batch eligibility through the prescription course.
- Reserve stock for pending prescriptions and revalidate it before dispensing.

### Prescription and Controlled-Substance Rules
- Represent prescriptions, prescribers, dispensing events, and workflow results.
- Support non-controlled medications and Schedules II–V.
- Validate DEA registration dates for controlled substances.
- Apply configurable refill and days-supply policies.
- Check early refills and support pharmacist override reasons.

### Inventory Reservations and Replenishment
- Reserve stock for pending prescriptions.
- Prevent competing prescriptions from using the same reserved inventory.
- Record dispensing events and batch allocations.
- Calculate reorder points using average daily usage, lead time, and safety stock.
- Generate purchase suggestions when inventory reaches its reorder threshold.

### Unit Conversion
- Convert tablets, strips, and boxes into tablet quantities.
- Convert tablet quantities back into whole strips or boxes.
- Reject invalid quantities, package sizes, and partial-pack conversions.

## Technology Stack
- Java 21
- Apache Maven Wrapper
- JUnit Jupiter
- Git and GitHub

## Project Structure

```text
pharmacy-dispensing-inventory/
├── .mvn/wrapper/
├── src/
│   ├── main/java/com/pharmacy/
│   └── test/java/com/pharmacy/
├── .gitignore
├── mvnw.cmd
└── pom.xml
```

## Prerequisites
- JDK 21
- Git, if cloning the repository

The Maven Wrapper is included, so a separate global Maven installation is not required.

## Getting Started

Clone the repository:

```bash
git clone https://github.com/sunnykotha/pharmacy-dispensing-inventory.git
cd pharmacy-dispensing-inventory
```

## Build and Test

Run the complete test suite on Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Package the project:

```powershell
.\mvnw.cmd package
```

The last verified test run before this README update reported 64 tests, with zero failures, zero errors, and zero skipped tests.

## Test Coverage
- FEFO batch ordering and split fills
- Expired, recalled, and quarantined stock
- Expiry across a prescription course
- Controlled-substance rules and DEA registration validation
- Early-refill validation and overrides
- Inventory reservations and dispensing workflow
- Replenishment suggestions
- Tablet, strip, and box conversions

## Learning Objectives
Object-oriented Java, records and enums, business-rule validation, date handling, inventory allocation, reservations, unit testing, Maven builds, and Git version control.

## Repository
https://github.com/sunnykotha/pharmacy-dispensing-inventory

## Disclaimer
This project is for educational and demonstration purposes. Its configurable controlled-substance rules do not guarantee compliance with all current federal, state, or local regulations. Do not use it for real-world dispensing decisions without appropriate professional, regulatory, security, and operational validation.