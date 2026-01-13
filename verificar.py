import sqlite3
import os

versiones = ["NVI'22.SQLite3", "LBLA.SQLite3", "DHHS'94.SQLite3", "PDT.SQLite3"]

print("=== REPORTE DE ESTRUCTURA 2.0 ===")
for v in versiones:
    path = f"app/src/main/assets/{v}"
    if not os.path.exists(path):
        print(f"❌ ARCHIVO NO ENCONTRADO EN ASSETS: {path}")
        continue
    
    conn = sqlite3.connect(path)
    cursor = conn.cursor()
    print(f"\n--- ANALIZANDO: {v} ---")
    
    # Ver tablas
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tablas = [t[0] for t in cursor.fetchall()]
    print(f"Tablas encontradas: {tablas}")
    
    # Ver columnas de 'verses'
    cursor.execute("PRAGMA table_info(verses);")
    cols = [c[1] for c in cursor.fetchall()]
    print(f"Columnas en 'verses': {cols}")
    
    conn.close()