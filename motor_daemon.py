import time
import os
import json
from pymavlink import mavutil

FILE_CMD = "/tmp/motor_command.json"
print("Demonio de motores iniciado. Liberando puerto USB...")

while True:
    try:
        # Revisar si hay una nueva orden
        if os.path.exists(FILE_CMD):
            print("Orden recibida. Conectando al Pixhawk...")
            
            # Conectar solo en este momento
            m = mavutil.mavlink_connection("/dev/pixhawk", baud=115200)
            m.wait_heartbeat(timeout=5)
            
            with open(FILE_CMD, 'r') as f:
                cmd = json.load(f)
            
            motor = cmd.get('motor', 1)
            throttle = cmd.get('throttle', 50)
            duration = cmd.get('duration', 2)
            
            print(f"🚀 Test: Motor {motor} al {throttle}%")
            
            m.mav.param_set_send(m.target_system, m.target_component, b"MOT_PWM_TYPE", 0.0, 4)
            time.sleep(0.5)
            m.mav.command_long_send(
                m.target_system, m.target_component,
                209, 0, 
                motor, 0, throttle, duration, 1, 0, 0
            )
            time.sleep(duration + 0.5)
            
            # Cerrar la conexión para liberar el puerto
            m.close()
            os.remove(FILE_CMD)
            print("✅ Test finalizado. Puerto liberado.")
            
    except Exception as e:
        print(f"⚠️ Error: {e}")
        time.sleep(1)
        
    time.sleep(0.1) # Pausa para no consumir CPU
