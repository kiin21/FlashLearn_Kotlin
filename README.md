# FlashLearn Kotlin - System Architecture & Mechanisms

## 1. Data Loading Mechanisms

### Topics
- **Source**: All topics (System & User) are stored in **NeonDB (PostgreSQL)** in the `topics` table.
- **Loading Logic**:
  - **Public/System Topics**: Queried where `is_system_topic = true OR is_public = true`.
  - **User Topics**: Queried where `created_by = current_user_id`.
  - **When**: Loaded immediately on `TopicScreen` initialization via `TopicRepository.getVisibleTopics`.

### Flashcards (Words)
- **Source**: Stored in **NeonDB** in the `flashcards` table.
- **Loading Logic (`FlashcardRepositoryImpl.getFlashcardsByTopicId`)**:
  1.  **Check DB**: Tries to load cards from Postgres for the given `topicId`.
  2.  **Fallback (Hidden Mechanism)**: If DB returns **0 cards** AND the topic is a **System Topic**:
      -   **Fetch**: Calls **Datamuse API** to get words related to the topic name.
      -   **Enrich**: Immediately fetches IPA & Images for these new words (see below).
      -   **Persist**: Saves these new enriched cards to Postgres so next load is fast.

### Images & IPA (Pronunciation)
- **Source**:
  -   **Images**: **Pixabay API**.
  -   **IPA**: **FreeDictionary API**.
-   **When are they loaded?**
    1.  **On Creation (System Topics)**: When the "Fallback" above occurs, the system loops through all fetched words and calls `enrichFlashcardData()` *before* saving to DB.
    2.  **On Creation (User Topics)**: When you add a word manually (if implemented via Repository), it presumably saves directly.
    3.  **Lazy Retry (Hidden)**: In `LearningSessionViewModel`, when a session starts, it checks the **first card**. If `image_url` or `ipa` is missing, it triggers a background retry to fetch and update the DB. *Note: Currently this only seems to run for the first card of the session.*

## 2. Data Storage Responsibilities

### Firebase (Auth)
-   **Purpose**: User Authentication & Identity.
-   **Stores**: Email, Password (hashed), User UID, Display Name.
-   **Role**: Provides the `currentUserId` used to query User Topics in Postgres.

### NeonDB (PostgreSQL)
-   **Purpose**: Primary Data Persistence.
-   **Stores**:
    -   `topics`: ID, Name, Description, Owner (User UID), Visibility.
    -   `flashcards`: Word, Definition, Example, **IPA**, **Image URL**, Topic ID.
-   **Role**: Serves as the implementation of truth. All API data (Datamuse/Pixabay) is cached here permanently after first fetch.

## 3. Hidden Mechanisms Summary

| Trigger | Mechanism | Details |
| :--- | :--- | :--- |
| **Opening Empty System Topic** | **Auto-Refill** | If DB has 0 cards, app fetches ~10-20 words from Datamuse, enriches them with Pixabay/FreeDict, and saves to DB. |
| **First Load** | **Enrichment** | During the Auto-Refill (above), the app makes parallel calls to Pixabay and FreeDictionary to fill `image_url` and `ipa`. |
| **Start Learning Session** | **Lazy Repair** | If the *first* card of the session misses an image/IPA, the app silently tries to fetch it again and update the DB. |
| **Marking Mastered** | **Local Memory** | Mastered/Review status is currently tracked in **Memory** (`ConcurrentHashMap`) in `FlashcardRepositoryImpl`, not yet persisted to DB (this resets on app restart). |
