const admin = require('firebase-admin');

const serviceAccount = require('./service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function actualizarSolicitudes() {
  console.log("Actualizando solicitudes pendientes sin distrito...");
  const snapshot = await db.collection('solicitudes')
    .where('estado', '==', 'pendiente')
    .get();

  const batch = db.batch();
  let count = 0;

  snapshot.forEach(doc => {
    const data = doc.data();
    if (!data.distritoServicio || data.distritoServicio === 'N/A') {
      const docRef = db.collection('solicitudes').doc(doc.id);
      
      // Asignamos Ventanilla a Tatiana y Villa El Salvador a Sebastian
      let nuevoDistrito = "Ventanilla";
      if (data.nombreCliente && data.nombreCliente.includes("Sebastian")) {
        nuevoDistrito = "Villa El Salvador";
      }

      batch.update(docRef, { distritoServicio: nuevoDistrito });
      console.log(`- Solicitud ${doc.id} de ${data.nombreCliente} actualizada a distrito: ${nuevoDistrito}`);
      count++;
    }
  });

  if (count > 0) {
    await batch.commit();
    console.log(`¡Se actualizaron ${count} solicitudes con éxito!`);
  } else {
    console.log("No se encontraron solicitudes pendientes sin distrito.");
  }
}

actualizarSolicitudes().catch(console.error);
