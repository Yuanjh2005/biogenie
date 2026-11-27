# BioGenie

### An AI-powered workspace for biomedical discovery

BioGenie brings growth factor knowledge, scientific literature exploration, and design guidance into one conversational platform. It helps researchers move from a question to relevant evidence and an informed next step while keeping their work organized.

## ✨ Project highlights

### 🧠 Three specialized research modules

| Module | Focus | What users can do |
| --- | --- | --- |
| **SmartDRGF** | Growth factor knowledge | Explore mechanisms, functions, and potential applications of growth factors. |
| **DeepLiterMed** | Scientific literature | Ask literature questions, filter by publication period or article type, and examine findings. |
| **DesignMCP** | Design guidance | Discuss protein design approaches, assess protocols, and compare possible strategies. |

### 💬 One continuous research experience

BioGenie combines natural-language questions, streamed AI responses, file attachments, searchable conversation history, and conversation sharing. Users can build on earlier work instead of starting each research question from scratch.

### 🔐 Access for individuals and teams

Phone verification, WeChat sign-in, and external single sign-on support different entry points. Administrative features provide user and operational oversight where managed access is needed.

## 🎯 Problems BioGenie addresses

- **Fragmented knowledge:** Researchers often move between separate sources to understand a biological topic, examine papers, and consider a design direction. BioGenie brings these activities into one workspace.
- **Time-consuming evidence review:** Conversational literature exploration helps users identify and interpret relevant findings more efficiently.
- **Disconnected research context:** Saved conversations, attachments, and sharing make it easier to revisit questions and communicate a line of reasoning.

## 🚀 Core capabilities

| Capability | Description |
| --- | --- |
| Specialized AI conversations | Choose a module suited to growth factor knowledge, literature, or design guidance. |
| Streaming responses | Read answers as they are generated. |
| Literature filters | Narrow DeepLiterMed questions by publication period and article type. |
| File-supported discussion | Attach materials for context in supported conversations. |
| Conversation management | Search, revisit, archive, delete, and share research conversations. |
| Multilingual interface | Use the application in English or Chinese. |

## 🛠️ Technology overview

```text
Researcher
   │
   ▼
Next.js + React + TypeScript frontend
   │  Research modules, chat, files, and history
   ▼
Java + Spring Boot backend
   ├─ Authentication and user management
   ├─ Conversation and file services
   ├─ MySQL, Redis, and MinIO
   └─ Alibaba Cloud DashScope AI applications
```

The frontend provides the research experience. The backend coordinates access, stores conversations and files, and connects the specialized modules to their configured AI applications. Docker and Nginx are used for deployment.

## 📁 Project structure

```text
BioGenie/
├── frontend/                    # Web application
├── backend/biogenie-backend/   # Backend services and AI integration
└── deploy/                      # Deployment configuration
```

## 📖 Further documentation

For setup and API details, see the [backend README](backend/biogenie-backend/README.md) and [deployment guide](backend/biogenie-backend/docs/deployment-env.md).

> BioGenie supports research exploration. AI-generated content should be checked against primary literature and expert judgment before use in scientific or clinical decisions.
