import time
import os
import sys
from pymavlink import mavutil

# Conectar al Pixhawk
m = mavutil.mavlink_connection("/dev/pixhawk", baud=115200)
m.wait_heartbeat(timeout=10)

print("🚀 Iniciando calibración de brújula...")

# Enviar comando de calibración (MAV_CMD_DO_START_MAG_CAL)
# El comando 42424 inicia la calibración
m.mav.command_long_send(
    m.target_system, m.target_component,
    42424, 0,
    1, 1, 0, 0, 0, 0, 0  # Calibrar brújula 1
)

# Esperar a que el usuario gire el dron y el Pixhawk termine
print("⚠️  Gira el dron lentamente en todas las direcciones...")
t0 = time.time()
while time.time() - t0 < 60:  # Esperar máximo 60 segundos
    msg = m.recv_match(type=['MAG_CAL_PROGRESS', 'MAG_CAL_REPORT', 'STATUSTEXT'], blocking=True, timeout=1)
    if msg:
        if msg.get_type() == 'MAG_CAL_PROGRESS':
            print(f"📊 Progreso: {msg.completion_pct}%")
        elif msg.get_type() == 'MAG_CAL_REPORT':
            print(f"✅ Calibración terminada. Resultado: {msg.cal_status}")
            break
        elif msg.get_type() == 'STATUSTEXT':
            print(f"📡 {msg.text}")

print("✅ Proceso finalizado.")
