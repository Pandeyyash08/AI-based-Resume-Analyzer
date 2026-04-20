# 🔐 API Keys & Environment Variables Setup

## **Frontend Setup**

### **1. Create your local `.env` file**
```bash
cd frontend
cp .env.example .env
```

### **2. Edit `frontend/.env` with your real API key**
```
VITE_GROQ_KEY=gsk_your_actual_api_key_here
VITE_API_URL=http://localhost:8080/api/resume
```

### **3. Get Groq API Key**
1. Go to https://console.groq.com
2. Sign up or login
3. Create an API key
4. Paste it in `.env`

---

## **Backend Setup**

### **1. Create your local `application.properties`**
```bash
cd resume-analyzer/src/main/resources
cp application.properties.example application.properties
```

### **2. Add environment variables (Windows PowerShell)**

**Option A: Set for current session only**
```powershell
$env:SPRING_MONGODB_URI = "mongodb+srv://username:password@cluster.mongodb.net/database"
$env:GROQ_API_KEY = "gsk_your_actual_key"
```

**Option B: Set permanently (System Environment Variables)**
1. Press `Win + X` → Select "System"
2. Click "Advanced system settings"
3. Click "Environment Variables"
4. Click "New" under "User variables"
5. Add:
   - Variable: `SPRING_MONGODB_URI`
   - Value: `mongodb+srv://...`
6. Add another:
   - Variable: `GROQ_API_KEY`
   - Value: `gsk_xxx`
7. Restart PowerShell/IDE

**Option C: Set in `application.properties` locally (NOT tracked by git)**
Edit `resume-analyzer/src/main/resources/application.properties`:
```properties
spring.mongodb.uri=mongodb+srv://your_user:your_pass@cluster.mongodb.net/db
groq.api.key=gsk_your_actual_key
groq.api.url=https://api.groq.com/openai/v1/chat/completions
```

---

## **⚠️ Important: Your API Key Was Exposed**

**GitHub detected your Groq API key in the commit history:**
- ✅ You've already unblocked it
- ⚠️ **IMPORTANT**: Regenerate your Groq API key immediately!

### **Regenerate Groq API Key:**
1. Go to https://console.groq.com/keys
2. Find your old key and delete it
3. Create a new API key
4. Update it in your `.env` file

---

## **Verify `.gitignore` Protections**

Your `.gitignore` already includes:
```
# Environment variables
.env
.env.local
.env.*.local
```

✅ This ensures `.env` files are **never** committed!

---

## **Running the Application Safely**

### **Frontend**
```bash
cd frontend
npm install
npm run dev
```

### **Backend**
```bash
cd resume-analyzer
mvn clean install
java -jar target/analyzer-0.0.1-SNAPSHOT.jar
```

Environment variables will be automatically loaded from:
1. System environment variables
2. `application.properties` defaults

---

## **MongoDB Security**

⚠️ **Your MongoDB credentials are now secured!**

To use MongoDB securely:
1. Go to MongoDB Atlas: https://www.mongodb.com/cloud/atlas
2. Create an account / Login
3. Create a cluster
4. Create a database user (username + password)
5. Get your connection string: `mongodb+srv://username:password@cluster.mongodb.net/database`
6. Set it as: `$env:SPRING_MONGODB_URI`

---

## **Summary: What's Protected**

| Item | Status | Location |
|------|--------|----------|
| `.env` file | ✅ Gitignored | `/frontend/.env` |
| Groq API Key | ✅ Protected | `.env` (not in git) |
| MongoDB URI | ✅ Protected | Environment variable |
| Templates | ✅ Public | `.env.example`, `application.properties.example` |

All sensitive data is now **hidden from GitHub**! 🔐
