const admin = require('firebase-admin');

// Inicializar Firebase Admin
const serviceAccount = require('./service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function inspect() {
  console.log("=== INSPECCIONANDO USUARIOS ===");
  const usersSnapshot = await db.collection('usuarios').get();
  console.log(`Total usuarios: ${usersSnapshot.size}`);
  usersSnapshot.forEach(doc => {
    const data = doc.data();
    console.log(`- UID: ${doc.id}`);
    console.log(`  Nombre: ${data.nombreCompleto || data.nombre || 'N/A'}`);
    console.log(`  Rol: ${data.rol}`);
    console.log(`  Disponible: ${data.disponible}`);
    console.log(`  Distrito Residencia: ${data.distritoResidencia || 'N/A'}`);
    console.log(`  Distritos Cobertura: ${JSON.stringify(data.distritos || [])}`);
    console.log(`  Especialidad: ${data.especialidad || 'N/A'}`);
    console.log("-----------------------------------------");
  });

  console.log("\n=== INSPECCIONANDO SOLICITUDES ===");
  const solSnapshot = await db.collection('solicitudes').get();
  console.log(`Total solicitudes: ${solSnapshot.size}`);
  solSnapshot.forEach(doc => {
    const data = doc.data();
    console.log(`- ID: ${doc.id}`);
    console.log(`  Cliente: ${data.nombreCliente} (ID: ${data.clienteId})`);
    console.log(`  Técnico ID: ${data.tecnicoId}`);
    console.log(`  Estado: ${data.estado}`);
    console.log(`  Especialidad Requerida: ${data.especialidadRequerida}`);
    console.log(`  Dirección: ${data.direccionServicio}`);
    console.log(`  Distrito Servicio: ${data.distritoServicio || 'N/A'}`);
    console.log("-----------------------------------------");
  });

  console.log("\n=== INSPECCIONANDO DISTRITOS ===");
  const distSnapshot = await db.collection('distritos').get();
  console.log(`Total distritos en base de datos: ${distSnapshot.size}`);
}

inspect().catch(err => {
  console.error("Error al inspeccionar:", err);
});
