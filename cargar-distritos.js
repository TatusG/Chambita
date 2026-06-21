const admin = require('firebase-admin');
const fs = require('fs');

// Inicializar Firebase Admin con la llave de acceso privada
const serviceAccount = require('./service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Leer el archivo distritos.json
const distritosRaw = fs.readFileSync('distritos.json', 'utf8');
const distritos = JSON.parse(distritosRaw);

async function cargarDatos() {
  const batch = db.batch();
  const collectionRef = db.collection('distritos');

  console.log(`Iniciando carga de ${distritos.length} distritos...`);

  distritos.forEach((distrito) => {
    // Usar el id del JSON como el ID del documento en Firestore
    const docRef = collectionRef.doc(distrito.id);
    
    // Extraemos el ID y dejamos el resto de campos (nombre, codigoPostal, distritosVecinos)
    const { id, ...datos } = distrito;
    
    batch.set(docRef, datos);
  });

  // Ejecutar la operación masiva en un solo lote
  await batch.commit();
  console.log('¡Carga masiva completada con éxito en Firestore!');
}

cargarDatos().catch((error) => {
  console.error('Error durante la carga:', error);
});
