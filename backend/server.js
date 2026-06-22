const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');
const crypto = require('crypto');


const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// --- CONFIGURACIÓN DE BASE DE DATOS ---
const isPostgres = !!process.env.DATABASE_URL;
let pool = null;

// En-Memoria Fallback
let memoryStudents = [];
const memoryLikes = new Set(); // Guarda "likerId_likedId"
let memoryMessages = [];

if (isPostgres) {
  console.log("Conectando a base de datos PostgreSQL real...");
  pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: {
      rejectUnauthorized: false // Requerido para la conexión SSL segura de Render
    }
  });

  // Inicializar tablas si no existen
  const initDb = async () => {
    try {
      await pool.query(`
        CREATE TABLE IF NOT EXISTS students (
          id VARCHAR(255) PRIMARY KEY,
          name VARCHAR(255) NOT NULL,
          career VARCHAR(255) NOT NULL,
          interests TEXT NOT NULL,
          bio TEXT,
          "avatarUri" TEXT,
          "matchType" VARCHAR(50) DEFAULT 'Educativo',
          "isMatched" BOOLEAN DEFAULT FALSE
        );
      `);
      
      await pool.query(`
        CREATE TABLE IF NOT EXISTS likes (
          "likerId" VARCHAR(255),
          "likedId" VARCHAR(255),
          PRIMARY KEY ("likerId", "likedId")
        );
      `);

      await pool.query(`
        CREATE TABLE IF NOT EXISTS messages (
          id VARCHAR(255) PRIMARY KEY,
          "matchId" VARCHAR(255) NOT NULL,
          "senderId" VARCHAR(255) NOT NULL,
          content TEXT NOT NULL,
          timestamp VARCHAR(50) NOT NULL,
          "createdAt" BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)
        );
      `);
      
      // Asegurar migración para instalaciones previas agregando la columna si no existe
      await pool.query(`
        ALTER TABLE messages ADD COLUMN IF NOT EXISTS "createdAt" BIGINT DEFAULT round(extract(epoch from now()) * 1000);
      `);
      console.log("Tablas de base de datos PostgreSQL inicializadas con éxito.");
    } catch (err) {
      console.error("Error inicializando tablas en PostgreSQL:", err);
    }
  };
  initDb();
} else {
  console.log("No se detectó DATABASE_URL. Ejecutando con Servidor en memoria local (In-Memory).");
}

// --- ENDPOINTS DE LA API ---

// 1. GET /api/v1/students: Obtener catálogo global
app.get('/api/v1/students', async (req, res) => {
  try {
    if (isPostgres) {
      const result = await pool.query('SELECT * FROM students');
      res.json(result.rows);
    } else {
      res.json(memoryStudents);
    }
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error del servidor al obtener estudiantes' });
  }
});

// 2. POST /api/v1/students: Registrar / Actualizar perfil
app.post('/api/v1/students', async (req, res) => {
  const { id, name, career, interests, bio, avatarUri, matchType, isMatched } = req.body;

  if (!id || !name || !career) {
    return res.status(400).json({ error: 'Faltan campos obligatorios: id, name o career.' });
  }

  try {
    if (isPostgres) {
      await pool.query(`
        INSERT INTO students (id, name, career, interests, bio, "avatarUri", "matchType", "isMatched")
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        ON CONFLICT (id) DO UPDATE SET
          name = EXCLUDED.name,
          career = EXCLUDED.career,
          interests = EXCLUDED.interests,
          bio = EXCLUDED.bio,
          "avatarUri" = EXCLUDED."avatarUri",
          "matchType" = EXCLUDED."matchType"
      `, [id, name, career, interests || '', bio || '', avatarUri || null, matchType || 'Educativo', isMatched || false]);
      res.status(200).send();
    } else {
      // Remover duplicados en memoria
      memoryStudents = memoryStudents.filter(s => s.id !== id);
      memoryStudents.push({
        id,
        name,
        career,
        interests: interests || '',
        bio: bio || '',
        avatarUri: avatarUri || null,
        matchType: matchType || 'Educativo',
        isMatched: isMatched || false
      });
      res.status(200).send();
    }
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error al registrar estudiante' });
  }
});

// 3. POST /api/v1/matches/like: Registrar like y verificar match
app.post('/api/v1/matches/like', async (req, res) => {
  const likerId = req.query.likerId;
  const likedId = req.query.likedId;

  if (!likerId || !likedId) {
    return res.status(400).json({ error: 'Faltan parámetros de consulta: likerId y likedId' });
  }

  try {
    let isMatch = false;
    const matchId = likerId < likedId ? `${likerId}_${likedId}` : `${likedId}_${likerId}`;

    if (isPostgres) {
      // Registrar el like
      await pool.query(`
        INSERT INTO likes ("likerId", "likedId")
        VALUES ($1, $2)
        ON CONFLICT ("likerId", "likedId") DO NOTHING
      `, [likerId, likedId]);

      // Verificar si el otro ya dio like
      const checkMatch = await pool.query(`
        SELECT 1 FROM likes WHERE "likerId" = $1 AND "likedId" = $2
      `, [likedId, likerId]);

      if (checkMatch.rows.length > 0) {
        isMatch = true;
        // Marcar ambos estudiantes como matched
        await pool.query(`
          UPDATE students SET "isMatched" = TRUE WHERE id IN ($1, $2)
        `, [likerId, likedId]);
      }
    } else {
      // Registrar el like en memoria
      memoryLikes.add(`${likerId}_${likedId}`);

      // Verificar si el otro ya dio like
      if (memoryLikes.has(`${likedId}_${likerId}`)) {
        isMatch = true;
        // Actualizar estados matched
        memoryStudents = memoryStudents.map(student => {
          if (student.id === likerId || student.id === likedId) {
            return { ...student, isMatched: true };
          }
          return student;
        });
      }
    }

    res.json({
      isMatch,
      matchId: isMatch ? matchId : null
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error al procesar el like' });
  }
});

// 4. GET /api/v1/matches/chat/{matchId}: Obtener historial de chat
app.get('/api/v1/matches/chat/:matchId', async (req, res) => {
  const matchId = req.params.matchId;

  try {
    if (isPostgres) {
      const result = await pool.query(`
        SELECT * FROM messages WHERE "matchId" = $1 ORDER BY "createdAt" ASC, id ASC
      `, [matchId]);
      res.json(result.rows);
    } else {
      const chatMessages = memoryMessages.filter(m => m.matchId === matchId);
      chatMessages.sort((a, b) => (a.createdAt || 0) - (b.createdAt || 0));
      res.json(chatMessages);
    }
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error al descargar mensajes del chat' });
  }
});

// 5. POST /api/v1/matches/chat: Enviar un mensaje de chat
app.post('/api/v1/matches/chat', async (req, res) => {
  const { id, matchId, senderId, content, timestamp, createdAt } = req.body;

  if (!matchId || !senderId || !content) {
    return res.status(400).json({ error: 'Faltan campos obligatorios: matchId, senderId o content.' });
  }

  // Formatear timestamp como "h:mm a" si no se provee
  const options = { hour: 'numeric', minute: 'numeric', hour12: true };
  const formattedTime = timestamp || new Date().toLocaleTimeString('en-US', options);
  const msgId = id || crypto.randomUUID();
  const created = createdAt ? parseInt(createdAt, 10) : Date.now();

  try {
    if (isPostgres) {
      await pool.query(`
        INSERT INTO messages (id, "matchId", "senderId", content, timestamp, "createdAt")
        VALUES ($1, $2, $3, $4, $5, $6)
        ON CONFLICT (id) DO NOTHING
      `, [msgId, matchId, senderId, content, formattedTime, created]);
      res.status(200).send();
    } else {
      // In-Memory: evitar duplicar si ya existe
      if (!memoryMessages.some(m => m.id === msgId)) {
        memoryMessages.push({
          id: msgId,
          matchId,
          senderId,
          content,
          timestamp: formattedTime,
          createdAt: created
        });
      }
      res.status(200).send();
    }
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Error al enviar mensaje' });
  }
});

// --- RUTA DE SALUD ---
app.get('/', (req, res) => {
  res.send('PuceMatch Backend está activo y ejecutándose en ' + (isPostgres ? 'PostgreSQL' : 'In-Memory Mode'));
});

// Iniciar Servidor
app.listen(PORT, () => {
  console.log(`Servidor de PuceMatch escuchando en el puerto ${PORT}`);
});
