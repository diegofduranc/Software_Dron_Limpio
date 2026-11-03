# Estado del Proyecto Dron - 03/11/2026

## Lo que ya funciona
- Backend FastAPI estable (arranca solo, logs limpios, sin errores 500).
- Test de motores desde la App (vía demonio).
- Puerto virtual socat configurado para compartir el USB.
- Calibración de brújula y Home funcionan por consola.
- Repositorio Git sincronizado.

## Tareas pendientes (en orden de ejecución)

### 1. Al conectar el dron de nuevo (Físico)
- Conectar el Pixhawk por USB.
- Ejecutar en la Raspberry:
  sudo systemctl start socat-pixhawk.service
  sudo systemctl restart dron-backend
- Verificar que el puerto virtual se creó: ls -l /dev/ttyVIRTUAL

### 2. Organizar el cableado (Físico)
- Alejar los cables de corriente (batería/ESCs) del Pixhawk y del mástil del GPS.
- Usar bridas para fijarlos al chasis.

### 3. Calibrar la brújula (Software)
- Ir a la App > Mantenimiento > Calibrar Brújula.
- Girar el dron en todos los ejes hasta que el log muestre 100%.
- Verificar que el error "Check mag field" desaparezca.

### 4. Prueba de despegue (Software)
- Hacer despegue automático a 1 metro por consola (para seguridad).
- Verificar estabilidad y aterrizar.

### 5. Mejoras en la App Android (Código)
- Implementar alertas visuales de éxito/error en la calibración (el backend ya tiene el endpoint /api/calibration/status).
- Ajustar el botón Armar para que suelte el RC Override antes de enviar la orden.

## Notas importantes
- El archivo telemetry.py fue modificado para silenciar los logs [MAVLINK-DEBUG]. Si necesitas depurar, busca los "pass # Log silenciado" y vuelve a poner los print.
- Si el socat falla al arrancar, revisa que el Pixhawk esté conectado (el puerto /dev/pixhawk debe existir).
