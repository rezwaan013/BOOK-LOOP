# BookLoop - Borrow,read,return, repeat

BookLoop is a peer-to-peer book lending and borrowing application designed for university students. It allows users to list books they own and request to borrow books from others in their community.

## Features
- **Authentication**: Secure registration and login with hashed passwords.
- **Book Management**: Add your own books or browse books from others.
- **API Integration**: Auto-fill book details by title via the Open Library API.
- **Reward Points**: +50 signup, +10 per book added, 1 pt/day borrow cost, with live popups.
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

## Course Requirements Map (where to demo each concept)

### 1. Advanced OOP Concepts
| Concept | Location |
|---|---|
| Classes + encapsulation | `model/` — `User`, `Book`, `BorrowRequest`, `Notification` (private fields, getters/setters) |
| Interface | `dao/CrudRepository.java` (generic `save`/`findById`/`deleteById` contract); `service/RewardPolicy.java` (Strategy interface for the points economy) |
| Abstract class | `dao/AbstractDAO.java` (holds shared `Connection`, abstract `map()`); extended by `BookDAO` |
| Inheritance | `BookDAO extends AbstractDAO<Book>` |
| Polymorphism | Services use `RewardPolicy` interface type with `StandardRewardPolicy` implementation; DAO used via `CrudRepository` contract |
| Enum | `model/RequestStatus.java` (`PENDING`, `ACCEPTED`, `DECLINED`, `RETURNED`) |

### 2. JavaFX UI Design (panes + controls showcase)
- **Panes**: `BorderPane` (dashboard shell), `StackPane` (toast overlay + notification badge), `AnchorPane` (content area), `VBox`/`HBox` (forms, cards), `ScrollPane` (book lists), `TabPane` (incoming/outgoing requests), `Region` spacers, `Separator`.
- **Controls**: `TextField`, `PasswordField` (login/register), `ComboBox` (duration, category), `Button`, `Hyperlink` (login↔register links), `Label` (incl. badge labels), `ProgressIndicator` (auto-fill spinner), `Alert` dialogs (confirm/info/error via `AlertUtil`).

### 3. Layout Responsiveness
- `NavigationUtil.loadInto()` pins loaded views to all four `AnchorPane` edges so content stretches with the window.
- Book-card info columns use `HBox.setHgrow(..., ALWAYS)`; forms use `maxWidth="Infinity"` + `Hgrow` so inputs grow/shrink.
- `DashboardController` binds `toastBox.maxWidthProperty()` to `contentArea.widthProperty()` (`Bindings.min(320, w*0.45)`) — live property constraint relative to window size.
- Sub-views center with `alignment="TOP_CENTER"` + `maxWidth="720"` inner container.

### 4. Concurrency (multi-threading + thread pools)
- `util/NotificationPoller.java` — `ScheduledExecutorService` single-thread pool, daemon thread, ticks every 5s; UI updates via `Platform.runLater()`; clean `shutdown()` on logout/app close.
- Live multi-window feel: keep browsing while a borrow-request notification or points change pops up as a toast overlay (`util/ToastUtil.java` + `toastBox` in `dashboard.fxml`).
- `MyBooksController.networkPool` — cached `ExecutorService` pool for the Open Library auto-fill HTTP call, results marshalled back with `Platform.runLater()` + `ProgressIndicator`.

### 5. Database Integration (SQLite)
- `dao/DatabaseManager.java` — singleton, one reusable `Connection` (`jdbc:sqlite:bookloop.db`), `PRAGMA foreign_keys = ON`, auto-creates empty schema on first run, `ALTER TABLE` migrations for existing DBs.
- Tables: `users(id PK, ..., reward_points)`, `books(id PK, owner_id FK→users, ..., current_address, category)`, `borrow_requests(id PK, book_id FK→books, requester_id FK→users, status, duration_days, due_date)`, `notifications(id PK, user_id FK→users, message, is_read)`.
- Relationships: books owned by users (one-to-many), requests join books+users (JOINs in `BookDAO`/`BorrowRequestDAO` fetch `owner_name`/`book_title` display fields).
- All queries use parameterized `PreparedStatement` (no string concatenation).

### 6. Data Manipulation (full CRUD)
| Op | Example |
|---|---|
| Create | `BookDAO.save()` (add book), `UserDAO.save()` (register), `BorrowRequestDAO.save()` |
| Read | `findByOwner`, `findAvailableExcludingOwner`, `search(...)` with category filter, `findById` |
| Update | `updateAvailability()` (accept/return), `UserDAO.addPoints()`, `updateStatus()`, `markRead/markAllRead` |
| Delete | `BookDAO.deleteById()` via `BookService.deleteBook()` (owner-only, blocked while borrowed) + Delete button on My Library cards |

### 7. Networking & Data Parsing
- `service/BookApiService.java` — Java `HttpClient` GET to `https://openlibrary.org/search.json?title=...&limit=1`, parses JSON with Jackson (`ObjectMapper` → `docs[0]` → publisher, `cover_i` → cover URL, `first_publish_year`).
- Demo: My Library → `+ Add Book` → type Title → **Auto-fill from Open Library** → publisher/cover filled in (runs on a background thread pool, spinner shown, graceful fallback offline).
