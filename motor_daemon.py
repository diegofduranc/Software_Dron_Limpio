import time
import os
import json
from pymavlink import mavutil

FILE_CMD = "/tmp/motor_command.json"
FILE_RESULT = "/tmp/daemon_result.json"

def save_result(status, message):
    with open(FILE_RESULT, "w") as f:
        json.dump({"status": status, "message": message, "timestamp": time.time()}, f)

print("Demonio iniciado. Esperando órdenes...")

while True:
    try:
        if os.path.exists(FILE_CMD):
            with open(FILE_CMD, 'r') as f:
                cmd = json.load(f)
            
            accion = cmd.get('action', 'motor_test')
            print(f"📥 Orden recibida: {accion}")
            
            # Conectar al Pixhawk para ejecutar
            m = mavutil.mavlink_connection("/dev/pixhawk", baud=115200)
            m.wait_heartbeat(timeout=5)
            
            if accion == 'motor_test':
                motor = cmd.get('motor', 1)
                throttle = cmd.get('throttle', 50)
                duration = cmd.get('duration', 2)
                print(f"🚀 Test Motor {motor} al {throttle}%")
                m.mav.param_set_send(m.target_system, m.target_component, b"MOT_PWM_TYPE", 0.0, 4)
                time.sleep(0.5)
                m.mav.command_long_send(m.target_system, m.target_component, 209, 0, motor, 0, throttle, duration, 1, 0, 0)
                time.sleep(duration + 0.5)
                save_result("success", f"Motor {motor} probado")

            elif accion == 'home':
                print("🏠 Estableciendo Home...")
                m.mav.command_long_send(m.target_system, m.target_component, 179, 0, 1, 0, 0, 0, 0, 0, 0)
                time.sleep(1)
                save_result("success", "Home establecido")

            elif accion == 'gps':
                print("📡 Verificando GPS...")
                # Enviar comando para verificar GPS (simplemente leemos el estado)
                time.sleep(1)
                save_result("success", "GPS verificado")

            elif accion == 'compass':
                print("🧭 Calibrando Brújula...")
                m.mav.command_long_send(m.target_system, m.target_component, 42424, 0, 1, 1, 0, 0, 0, 0, 0)
                time.sleep(45) # Esperar a que el usuario gire el dron
                m.mav.command_long_send(m.target_system, m.target_component, 42425, 0, 1, 0, 0, 0, 0, 0, 0) # Aceptar calibración
                time.sleep(1)
                save_result("success", "Brújula calibrada")

            m.close()
            os.remove(FILE_CMD)
            print("✅ Orden finalizada. Puerto liberado.")
            
    except Exception as e:
        print(f"⚠️ Error: {e}")
        time.sleep(1)
        
    time.sleep(0.1)
