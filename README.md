# 🕹️ Online Multiplayer Survival Game

This is an **online multiplayer survival game** built as an **internship graduation project**.  
Originally developed and managed on **Bitbucket**, but due to branch mirroring issues, the project was moved.

---

## 💡 Idea

A **survival game** consisting of multiple rounds.  
In each round, a **unique survival scenario** is presented to all players.  
Each player must submit how they would act to survive in that situation.

OpenAI API is used to:
- Generate realistic survival scenarios.
- Rate each player's response with detailed feedback:  
  - Why it’s a good or bad choice.
- Provide a **final decision**: whether the player survived or not.

---

## 🚀 Features

- **User Authentication:**
  - Login and Registration
  - Email verification (via code)
  - JWT-based authentication and authorization

- **Game Mechanics:**
  - Players can **create rooms** and **invite** others (up to 5 players per room)
  - Real-time communication using **WebSockets**
  - Game events (rounds, responses, results) are broadcasted in real time

- **AI Integration:**
  - Scenario generation per round
  - Rating system with justification
  - Final survival verdict based on responses

---

## 🛠️ Tech Stack

- **Backend:** Spring Boot + Java
- **Database:** PostgreSQL
- **Cache/Temporary Store:** Redis (used for storing verification codes)
- **AI Service:** OpenAI API
- **Real-time Communication:** WebSockets
- **Project Management:** Jira
- **Version Control:** Bitbucket

---

## ⚠️ Notes

> 🔄 Mirroring Bitbucket branches failed — manual migration was required.

---

## 📌 Status

This project demonstrates backend engineering, real-time systems, and AI service integration in a collaborative team environment.

---

