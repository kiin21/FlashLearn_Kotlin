# FlashLearn

## Introduction

FlashLearn is an English vocabulary learning app using **Flashcards** with **images** and **IPA pronunciation** for effective memorization.

---

## System Architecture

```plantuml
@startuml
skinparam componentStyle rectangle

package "Presentation Layer" {
    [Compose UI] as UI
    [ViewModels] as VM
    note right of VM
        TopicViewModel
        TopicDetailViewModel
        LearningSessionViewModel
        AddWordViewModel
        SignInViewModel
    end note
}

package "Domain Layer" {
    [Use Cases] as UC
    [Repository Interfaces] as RI
}

package "Data Layer" {
    package "Local" {
        database "Room Database" as Room {
            [FlashcardDao]
            [TopicDao]
            [UserProgressDao]
        }
    }
    
    package "Remote" {
        cloud "Retrofit APIs" as APIs {
            [PostgresApi]
            [DatamuseApi]
            [PixabayApi]
            [FreeDictionaryApi]
        }
    }
    
    package "Background" {
        component "WorkManager" as WM {
            [SyncProgressWorker]
            [EnrichmentWorker]
        }
    }
}

UI --> VM : StateFlow
VM --> UC : suspend fun
UC --> RI
RI --> Room : Flow<T>
RI --> APIs : suspend fun
RI --> WM : enqueue()
WM --> APIs : doWork()
@enduml
```

---

## �️ Database Schemas

### Firebase Authentication
| Field | Type | Description |
|-------|------|-------------|
| `uid` | String | Unique user ID (used as `createdBy` in topics) |
| `email` | String | User's email address |
| `displayName` | String? | User's display name |
| `photoUrl` | String? | Profile picture URL |

---

### NeonDB PostgreSQL (Remote)

#### Table: `topics`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `name` | VARCHAR | Topic name, e.g., "B1 Environment" |
| `description` | TEXT | Topic description |
| `icon_type` | VARCHAR | Icon identifier, default "book" |
| `is_system_topic` | BOOLEAN | `true` = Pre-built topic by app |
| `is_public` | BOOLEAN | `true` = Visible to all users |
| `created_by` | VARCHAR? | Firebase UID of creator (NULL for system) |
| `word_count` | INT | Number of flashcards in topic |
| `image_url` | VARCHAR? | Cover image URL (from Pixabay) |

#### Table: `flashcards`
| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `topic_id` | UUID | Foreign key → topics.id |
| `word` | VARCHAR | Vocabulary word, e.g., "Pollution" |
| `pronunciation` | VARCHAR | Pronunciation guide |
| `part_of_speech` | VARCHAR | "NOUN", "VERB", "ADJECTIVE", etc. |
| `definition` | TEXT | Word definition |
| `example_sentence` | TEXT | Usage example |
| `ipa` | VARCHAR | IPA phonetic, e.g., "/pəˈluːʃn/" |
| `image_url` | VARCHAR | Image URL (from Pixabay) |

---

### Room Database (Local Cache)

#### Table: `topics` (mirrors PostgreSQL)
| Column | Type | Description |
|--------|------|-------------|
| `id` | String | **PK** - UUID |
| `name` | String | Topic name |
| `description` | String | Topic description |
| `iconType` | String | Icon identifier |
| `isSystemTopic` | Boolean | Pre-built by app? |
| `isPublic` | Boolean | Visible to all? |
| `createdBy` | String? | Firebase UID of creator |
| `wordCount` | Int | Number of flashcards |
| `imageUrl` | String? | Cover image URL |
| `lastUpdated` | Long | Cache timestamp |

#### Table: `flashcards` (mirrors PostgreSQL)
| Column | Type | Description |
|--------|------|-------------|
| `id` | String | **PK** - UUID |
| `topicId` | String | **FK** → topics.id (CASCADE delete) |
| `word` | String | Vocabulary word |
| `pronunciation` | String | Pronunciation guide |
| `partOfSpeech` | String | NOUN, VERB, ADJECTIVE... |
| `definition` | String | Word definition |
| `exampleSentence` | String | Usage example |
| `ipa` | String | IPA phonetic (from FreeDictionary) |
| `imageUrl` | String | Image URL (from Pixabay) |
| `createdAt` | Long | Creation timestamp |
| `lastUpdated` | Long | Last update timestamp |

#### Table: `user_progress` (LOCAL ONLY)
| Column | Type | Description |
|--------|------|-------------|
| `id` | String | **PK** - Format: `{userId}_{flashcardId}` |
| `userId` | String | Firebase UID |
| `flashcardId` | String | Reference to flashcard |
| `status` | ProgressStatus | `LEARNING` / `REVIEW` / `MASTERED` |
| `updatedAt` | Long | Last status change |
| `syncedToRemote` | Boolean | Pending sync to NeonDB? |

> **Note:** `user_progress` is currently local-only. WorkManager syncs to NeonDB in background.

---

## �🔌 External APIs

### 1. NeonDB PostgreSQL (Primary Database)
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/sql` | POST | Execute SQL queries over HTTP |

```kotlin
// PostgresApi.kt
@POST("sql")
suspend fun executeQuery(
    @Header("Neon-Connection-String") connectionString: String,
    @Body request: PostgresSqlRequest
): PostgresSqlResponse
```

### 2. Datamuse API (Vocabulary Provider)
| Endpoint | Example | Purpose |
|----------|---------|---------|
| `/sug?s={prefix}` | `/sug?s=voca` | Autocomplete suggestions |
| `/words?topics={topic}&md=d` | `/words?topics=environment` | Get words by topic |
| `/words?ml={meaning}&md=d` | `/words?ml=happy` | Get synonyms |

```kotlin
// DatamuseApi.kt
@GET("words")
suspend fun getWordsByMeaning(
    @Query("ml") meaning: String,
    @Query("md") metadata: String = "d",
    @Query("max") max: Int = 20
): List<WordWithDefinitionDto>
```

### 3. Pixabay API (Image Provider)
| Endpoint | Purpose |
|----------|---------|
| `/api/?key={key}&q={word}` | Search images for word |

```kotlin
// PixabayApi.kt
@GET("api/")
suspend fun searchImages(
    @Query("key") apiKey: String,
    @Query("q") query: String,
    @Query("per_page") perPage: Int = 3
): PixabayResponse
```

### 4. FreeDictionary API (IPA Provider)
| Endpoint | Purpose |
|----------|---------|
| `/api/v2/entries/en/{word}` | Get phonetics/IPA |

```kotlin
// FreeDictionaryApi.kt
@GET("api/v2/entries/en/{word}")
suspend fun getWordDetails(@Path("word") word: String): List<DictionaryEntryDto>
```

---

## ⚡ Key Mechanisms

### 1. Parallel Enrichment with Semaphore
**Problem:** Loading 20 flashcards = 40 API calls (image + IPA) → Rate limit (429)

**Solution:** Kotlin Coroutines + Semaphore throttling

```kotlin
// FlashcardRepositoryImpl.kt
private val enrichmentSemaphore = Semaphore(5) // Max 5 concurrent

suspend fun enrichFlashcardsParallel(cards: List<Flashcard>): List<Flashcard> {
    return coroutineScope {
        cards.map { card ->
            async {
                enrichmentSemaphore.withPermit {
                    enrichFlashcardData(card)  // Pixabay + FreeDictionary
                }
            }
        }.awaitAll()
    }
}
```

### 2. Persistent Progress with Room
**Problem:** Mastered status lost on app kill

**Solution:** Room Database + immediate local save

```kotlin
// FlashcardRepositoryImpl.kt
override suspend fun markFlashcardAsMastered(flashcardId: String, userId: String) {
    userProgressDao.upsert(UserProgressEntity(
        id = "${userId}_${flashcardId}",
        status = ProgressStatus.MASTERED,
        syncedToRemote = false  // Pending sync
    ))
}
```

### 3. Background Sync with WorkManager
**Problem:** Network call fails if app killed

**Solution:** WorkManager with exponential backoff

```kotlin
// SyncProgressWorker.kt
@HiltWorker
class SyncProgressWorker : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        return try {
            repository.syncToRemote(...)
            Result.success()
        } catch (e: Exception) {
            Result.retry()  // Auto-retry with backoff
        }
    }
}

// Scheduling
val request = OneTimeWorkRequestBuilder<SyncProgressWorker>()
    .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
    .build()
WorkManager.getInstance(context).enqueue(request)
```

### 4. Auto-Refill for Empty Topics
**Trigger:** User opens system topic with 0 flashcards

```plantuml
@startuml
start
:User opens "B1 Environment";
:FlashcardRepository.getFlashcardsByTopicId();
if (Cards in DB?) then (no)
    if (System Topic?) then (yes)
        :DatamuseApi.getWordsByMeaning("environment");
        :enrichFlashcardsParallel() via Semaphore;
        :PostgresApi.executeQuery(INSERT);
    endif
endif
:Return flashcards;
stop
@enduml
```

---

## Project Structure

```
app/src/main/java/com/kotlin/flashlearn/
├── data/
│   ├── local/
│   │   ├── FlashLearnDatabase.kt      # Room @Database
│   │   ├── entity/                    # @Entity classes
│   │   └── dao/                       # @Dao interfaces
│   ├── remote/
│   │   ├── DatamuseApi.kt             # Retrofit interface
│   │   ├── PostgresApi.kt             # SQL over HTTP
│   │   ├── PixabayApi.kt              # Image search
│   │   └── FreeDictionaryApi.kt       # IPA lookup
│   ├── repository/
│   │   ├── FlashcardRepositoryImpl.kt # Orchestrates all APIs
│   │   └── TopicRepositoryImpl.kt
│   └── worker/
│       ├── SyncProgressWorker.kt      # Background sync
│       └── EnrichmentWorker.kt        # Background enrichment
├── domain/
│   ├── model/                         # Flashcard, Topic
│   └── repository/                    # Interfaces
├── presentation/
│   ├── topic/                         # TopicScreen, ViewModel
│   ├── learning_session/              # Flashcard UI
│   └── components/                    # Reusable composables
└── di/
    ├── AppModule.kt                   # Hilt bindings
    ├── NetworkModule.kt               # Retrofit setup
    └── DatabaseModule.kt              # Room setup
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Material 3 |
| State | StateFlow, ViewModel |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Network | Retrofit + OkHttp + Gson |
| Background | WorkManager + CoroutineWorker |
| Auth | Firebase Authentication |
| Image Loading | Coil |

---

## 🚀 Setup

### Required API Keys
```properties
# local.properties
NEON_CONNECTION_STRING=postgresql://user:pass@host/db
PIXABAY_API_KEY=your_pixabay_key
DATAMUSE_BASE_URL=https://api.datamuse.com/
```

### Build
```bash
./gradlew assembleDebug
```
