# BookLoop - P2P Book Lending System

BookLoop is a peer-to-peer book lending and borrowing application designed for university students. It allows users to list books they own and request to borrow books from others in their community.

## Features
- **Authentication**: Secure registration and login with hashed passwords.
- **Book Management**: Add your own books or browse books from others.
- **API Integration**: Automatically fetch book details using ISBN via the Open Library API.
- **Borrow Requests**: Request books for 7, 14, or 21 days with owner approval workflow.
- **Real-time Notifications**: Background polling for incoming borrow requests.
- **Database**: Local SQLite database for persistent storage.

## Tech Stack
- **Java 17+**
- **JavaFX** (GUI)
- **Maven** (Build Tool)
- **SQLite** (Database)
- **Jackson** (JSON Parsing)

## Setup and Installation

### Prerequisites
- JDK 17 or higher
- Maven 3.x

### Running the Application
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/BOOK-LOOP.git
   cd BOOK-LOOP
   ```
2. Run the application using the Maven JavaFX plugin:
   ```bash
   mvn clean javafx:run
   ```

## Project Structure
- `com.bookloop.model`: Data entities (User, Book, etc.)
- `com.bookloop.dao`: Database Access Objects for CRUD operations.
- `com.bookloop.service`: Business logic layer.
- `com.bookloop.controller`: JavaFX FXML controllers.
- `com.bookloop.util`: Helper classes for navigation, alerts, and session management.
