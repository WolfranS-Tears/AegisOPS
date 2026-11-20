"""
AegisOPS - Generador de datos iniciales sintéticos.

Genera database/02_seed.sql de forma determinista (semilla fija), por lo que
cualquier persona puede reproducir exactamente los mismos datos y cálculos:

    python database/generador/generar_datos.py

Reglas de coherencia aplicadas:
- 300 incidentes distribuidos en 26 semanas (2026-05-18 a 2026-11-15).
- Fechas de resolución siempre >= fecha de creación y <= FECHA_CORTE.
- El tiempo de resolución depende de la prioridad y la categoría.
- Incidentes recientes tienen más probabilidad de seguir abiertos.
- Conectividad crece en las últimas semanas (tendencia observable).
"""

import math
import random
from datetime import datetime, timedelta
from pathlib import Path

SEMILLA = 2026
FECHA_INICIO = datetime(2026, 5, 18)          # lunes
SEMANAS = 26
FECHA_CORTE = datetime(2026, 11, 12, 8, 0)    # "hoy" para los datos
TOTAL_INCIDENTES = 300

random.seed(SEMILLA)

# ---------------------------------------------------------------------------
# Catálogos
# ---------------------------------------------------------------------------
CATEGORIAS = [
    ("Conectividad y redes", "Internet, Wi-Fi, VPN, DNS, proxy y red cableada."),
    ("Hardware", "Equipos, discos, periféricos de video y fallas físicas."),
    ("Aplicaciones y software", "Errores de ejecución, instalaciones y aplicaciones de negocio."),
    ("Sistema operativo", "Windows, actualizaciones, rendimiento y perfiles."),
    ("Correo y colaboración", "Outlook, buzones y Microsoft Teams."),
    ("Seguridad", "Phishing, malware, certificados y alertas de seguridad."),
    ("Impresión y periféricos", "Impresoras, escáneres y colas de impresión."),
    ("Accesos y cuentas", "Cuentas bloqueadas, MFA y permisos."),
]

USUARIOS = [
    ("Andrea Lozano", "andrea.lozano@aegisops.internal", "Mesa de ayuda"),
    ("Carlos Rincón", "carlos.rincon@aegisops.internal", "Mesa de ayuda"),
    ("Diana Pérez", "diana.perez@aegisops.internal", "Mesa de ayuda"),
    ("Felipe Gómez", "felipe.gomez@aegisops.internal", "Redes"),
    ("Laura Martínez", "laura.martinez@aegisops.internal", "Redes"),
    ("Julián Herrera", "julian.herrera@aegisops.internal", "Infraestructura"),
    ("Sofía Ramírez", "sofia.ramirez@aegisops.internal", "Infraestructura"),
    ("Mateo Castro", "mateo.castro@aegisops.internal", "Aplicaciones"),
    ("Valentina Ortiz", "valentina.ortiz@aegisops.internal", "Aplicaciones"),
    ("Santiago Moreno", "santiago.moreno@aegisops.internal", "Seguridad"),
    ("Camila Vargas", "camila.vargas@aegisops.internal", "Seguridad"),
    ("Andrés Torres", "andres.torres@aegisops.internal", "Soporte en sitio"),
]

# Área preferida por categoría (índice 1-based de categoría -> áreas)
AREAS_POR_CATEGORIA = {
    1: ["Redes", "Mesa de ayuda"],
    2: ["Soporte en sitio", "Infraestructura"],
    3: ["Aplicaciones", "Mesa de ayuda"],
    4: ["Infraestructura", "Mesa de ayuda", "Soporte en sitio"],
    5: ["Mesa de ayuda", "Aplicaciones"],
    6: ["Seguridad"],
    7: ["Soporte en sitio", "Mesa de ayuda"],
    8: ["Mesa de ayuda", "Seguridad"],
}

# (categoria, titulo, problema, sintomas, causa, solucion, estado, verificado)
ARTICULOS = [
    (1, "Equipo sin acceso a internet por falla de DHCP",
     "El equipo no obtiene dirección IP válida y no navega.",
     "Ícono de red con advertencia; ipconfig muestra una IP 169.254.x.x; no hay acceso a internet ni a recursos internos.",
     "El cliente DHCP no recibe respuesta del servidor (cable suelto, puerto de switch deshabilitado o ámbito DHCP agotado).",
     "1. Verificar cable y luces del puerto de red. 2. Ejecutar ipconfig /release y luego ipconfig /renew. 3. Si persiste, ejecutar netsh winsock reset y netsh int ip reset y reiniciar. 4. Validar con Redes que el puerto del switch esté activo y que el ámbito DHCP tenga direcciones libres.",
     "ACTIVO", True),
    (1, "Wi-Fi corporativo se desconecta de forma intermitente",
     "La conexión inalámbrica se cae varias veces al día.",
     "Pérdida de conexión de pocos segundos; videollamadas congeladas; el equipo cambia entre redes.",
     "Controlador de la tarjeta inalámbrica desactualizado o ahorro de energía que apaga el adaptador; interferencia con red de invitados.",
     "1. Actualizar el controlador Wi-Fi desde el portal del fabricante. 2. En Administrador de dispositivos > adaptador > Administración de energía, desmarcar 'Permitir que el equipo apague este dispositivo'. 3. Olvidar la red de invitados y priorizar la red corporativa. 4. Si afecta a varios usuarios de la misma zona, escalar a Redes para revisar el punto de acceso.",
     "ACTIVO", True),
    (1, "Los nombres de dominio no resuelven (falla de DNS)",
     "Se navega por IP pero no por nombre de sitio.",
     "Mensaje DNS_PROBE_FINISHED_NXDOMAIN; ping a 8.8.8.8 responde pero ping a un dominio falla.",
     "Servidores DNS mal configurados en el adaptador o caché DNS corrupta.",
     "1. Ejecutar ipconfig /flushdns. 2. Verificar que el adaptador use DNS automático (DHCP) o los DNS corporativos. 3. Probar con nslookup dominio servidor_dns. 4. Si el servidor DNS corporativo no responde, escalar a Infraestructura.",
     "ACTIVO", True),
    (1, "La VPN corporativa no conecta",
     "El cliente VPN no establece el túnel desde casa.",
     "Error de tiempo de espera o error 809; la conexión se queda en 'Conectando'.",
     "Bloqueo de puertos en el router doméstico, certificado de cliente vencido o reloj del equipo desfasado.",
     "1. Verificar que haya internet sin VPN. 2. Sincronizar la hora de Windows. 3. Reiniciar el cliente VPN y volver a iniciar sesión. 4. Revisar vigencia del certificado del cliente. 5. Probar desde otra red (hotspot) para descartar bloqueo del proveedor; si funciona, el usuario debe habilitar IPsec/UDP 500 y 4500 en su router.",
     "ACTIVO", True),
    (1, "Lentitud de red por negociación de velocidad en cable",
     "La red cableada va muy lenta.",
     "Transferencias a 10 Mbps; el adaptador muestra 10 Mbps half duplex.",
     "Negociación incorrecta de velocidad/dúplex entre el equipo y el switch antiguo.",
     "Forzar 100 Mbps full duplex en el adaptador. (Procedimiento obsoleto tras la renovación de switches.)",
     "INACTIVO", True),
    (1, "El proxy bloquea un sitio web necesario para el trabajo",
     "Un sitio legítimo aparece bloqueado por el proxy corporativo.",
     "Página de bloqueo del proxy con categoría del sitio; otros sitios funcionan.",
     "La categoría del sitio está restringida en la política de filtrado web.",
     "1. Confirmar la URL exacta y la justificación de negocio. 2. Registrar solicitud de excepción con aprobación del jefe inmediato. 3. Seguridad agrega la URL a la lista permitida. 4. Pedir al usuario limpiar caché del navegador y reintentar.",
     "ACTIVO", True),
    (2, "El equipo de escritorio no enciende",
     "Al presionar el botón de encendido no hay respuesta.",
     "Sin luces, sin ventiladores, sin pitidos.",
     "Fuente de poder dañada, cable de energía desconectado o regleta apagada.",
     "1. Verificar regleta y cable de poder probando otro tomacorriente. 2. Revisar el interruptor trasero de la fuente. 3. Mantener presionado el botón 30 s sin corriente para descargar. 4. Si no enciende, reemplazar la fuente o solicitar equipo de respaldo mientras se tramita garantía.",
     "ACTIVO", True),
    (2, "Portátil se sobrecalienta y se apaga solo",
     "El portátil se apaga sin aviso cuando está en uso intensivo.",
     "Ventilador muy ruidoso, carcasa caliente, apagados durante videollamadas.",
     "Acumulación de polvo en disipador o pasta térmica degradada.",
     "1. Verificar temperaturas con la herramienta del fabricante. 2. Usar el equipo sobre superficie dura. 3. Programar mantenimiento preventivo: limpieza de ventiladores y cambio de pasta térmica. 4. Actualizar BIOS si el fabricante publicó mejoras de gestión térmica.",
     "ACTIVO", True),
    (2, "Disco duro lento o con sectores dañados",
     "El equipo tarda mucho en abrir archivos y se congela.",
     "Uso de disco al 100%, ruidos en el disco, errores de lectura, SMART con advertencia.",
     "Degradación física del disco mecánico.",
     "1. Respaldar inmediatamente la información del usuario. 2. Ejecutar chkdsk /f /r. 3. Revisar estado SMART. 4. Si hay advertencias, reemplazar por SSD y restaurar el respaldo.",
     "ACTIVO", True),
    (2, "Monitor externo no detectado",
     "El segundo monitor queda en negro o sin señal.",
     "Mensaje 'Sin señal'; Windows no lista la segunda pantalla.",
     "Cable o adaptador defectuoso, entrada incorrecta en el monitor o controlador de video desactualizado.",
     "1. Verificar la entrada seleccionada en el monitor. 2. Cambiar cable o adaptador HDMI/USB-C. 3. Presionar Win+P y elegir 'Extender'. 4. Actualizar el controlador de video. 5. Probar el monitor en otro equipo para aislar la falla.",
     "ACTIVO", True),
    (3, "Aplicación de escritorio se cierra al iniciar por falta de componentes",
     "Una aplicación se cierra inmediatamente después de abrirla.",
     "Error de ejecución 'The application was unable to start correctly (0xc000007b)' o falta de MSVCP140.dll / VCRUNTIME140.dll.",
     "Faltan o están dañados los paquetes redistribuibles de Visual C++ o .NET requeridos por la aplicación.",
     "1. Revisar el Visor de eventos > Aplicación para identificar el módulo con error. 2. Instalar o reparar Microsoft Visual C++ Redistributable (x86 y x64) y la versión de .NET requerida. 3. Reinstalar la aplicación como administrador. 4. Reiniciar y validar.",
     "ACTIVO", True),
    (3, "Error de memoria insuficiente (Java heap space) en aplicación",
     "Una aplicación Java falla al procesar archivos grandes.",
     "Error de ejecución java.lang.OutOfMemoryError: Java heap space; la aplicación se congela y se cierra.",
     "El parámetro de memoria máxima de la JVM (-Xmx) es insuficiente para la carga de trabajo.",
     "1. Confirmar el error en el log de la aplicación. 2. Aumentar -Xmx en el archivo de configuración o acceso directo (por ejemplo -Xmx2048m) sin superar el 50% de la RAM. 3. Cerrar otras aplicaciones pesadas. 4. Si persiste, reportar a Aplicaciones con el log para revisar fugas de memoria.",
     "ACTIVO", True),
    (3, "El ERP muestra error de conexión con la base de datos",
     "El sistema ERP no carga datos.",
     "Mensaje 'Timeout expired' o 'No se pudo conectar con el servidor de base de datos' al iniciar sesión.",
     "Caída de conectividad con el servidor de base de datos, servicio detenido o cadena de conexión desactualizada tras migración.",
     "1. Verificar si el error afecta a uno o varios usuarios. 2. Probar conectividad al servidor (ping y telnet al puerto). 3. Si afecta a todos, escalar a Infraestructura para validar el servicio de base de datos. 4. Si afecta a uno, actualizar el archivo de configuración del cliente ERP con la cadena vigente.",
     "ACTIVO", True),
    (3, "Aplicación web interna muestra error 500 o página en blanco",
     "Un portal web interno no carga correctamente.",
     "Error HTTP 500, página en blanco o estilos rotos tras una actualización.",
     "Caché del navegador con versiones antiguas de archivos o error en el despliegue del servidor.",
     "1. Recargar sin caché (Ctrl+F5) y probar en ventana privada. 2. Borrar caché y cookies del sitio. 3. Si el error persiste para varios usuarios, escalar a Aplicaciones con hora, URL y captura del error para revisar logs del servidor.",
     "ACTIVO", True),
    (3, "Macros de Excel bloqueadas",
     "Las macros de un libro de Excel no se ejecutan.",
     "Barra roja 'Microsoft ha bloqueado la ejecución de macros'.",
     "Política de seguridad de Office para archivos descargados de internet.",
     "Desbloquear el archivo en Propiedades. (Reemplazado por la política de ubicaciones de confianza.)",
     "INACTIVO", True),
    (4, "Las actualizaciones de Windows fallan con error 0x80070002",
     "Windows Update no instala actualizaciones.",
     "Error 0x80070002 o 0x8024402f; actualizaciones pendientes por semanas.",
     "Archivos de la carpeta SoftwareDistribution dañados o servicio de actualización detenido.",
     "1. Ejecutar el solucionador de problemas de Windows Update. 2. Detener los servicios wuauserv y bits, renombrar C:\\Windows\\SoftwareDistribution y reiniciar los servicios. 3. Ejecutar sfc /scannow y DISM /Online /Cleanup-Image /RestoreHealth. 4. Reintentar la actualización.",
     "ACTIVO", True),
    (4, "Pantalla azul después de instalar un controlador",
     "El equipo muestra pantalla azul al iniciar.",
     "BSOD con códigos como DRIVER_IRQL_NOT_LESS_OR_EQUAL; reinicios en bucle.",
     "Controlador incompatible instalado recientemente.",
     "1. Iniciar en modo seguro (reparación automática > opciones avanzadas). 2. Desinstalar el controlador reciente o usar Restaurar sistema. 3. Instalar la versión certificada del fabricante. 4. Revisar el volcado de memoria si se repite.",
     "ACTIVO", True),
    (4, "Equipo muy lento al iniciar sesión",
     "El inicio de Windows tarda varios minutos.",
     "Escritorio sin responder tras iniciar sesión; uso alto de CPU y disco al arrancar.",
     "Exceso de programas de inicio, poco espacio en disco o falta de memoria RAM.",
     "1. Deshabilitar programas innecesarios en Administrador de tareas > Inicio. 2. Liberar espacio (mínimo 15% libre) con Liberador de espacio. 3. Verificar que el antivirus no esté en análisis completo al inicio. 4. Si la RAM es menor a 8 GB, solicitar ampliación.",
     "ACTIVO", True),
    (4, "El usuario inicia sesión con un perfil temporal",
     "Al iniciar sesión el escritorio aparece vacío.",
     "Mensaje 'Se inició sesión con un perfil temporal'; no aparecen archivos ni configuraciones.",
     "Perfil de usuario dañado en el registro (clave .bak en ProfileList).",
     "1. Respaldar datos de C:\\Users\\usuario. 2. En regedit, HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProfileList, corregir la clave duplicada .bak. 3. Reiniciar y validar. 4. Si no funciona, crear perfil nuevo y migrar los datos.",
     "ACTIVO", True),
    (5, "Outlook no sincroniza y pide contraseña repetidamente",
     "Outlook no recibe correos nuevos.",
     "Ventana de credenciales que reaparece; estado 'Desconectado' o 'Intentando conectar'.",
     "Credenciales almacenadas desactualizadas tras cambio de contraseña o perfil de Outlook dañado.",
     "1. Eliminar las credenciales de Office en el Administrador de credenciales de Windows. 2. Cerrar Outlook y abrirlo de nuevo para autenticar con la contraseña actual. 3. Si persiste, crear un nuevo perfil de Outlook desde Panel de control > Correo.",
     "ACTIVO", True),
    (5, "Buzón de correo lleno",
     "El usuario no puede enviar ni recibir correos.",
     "Aviso 'Su buzón ha superado el límite'; correos rebotados.",
     "Se alcanzó la cuota de almacenamiento del buzón.",
     "1. Vaciar Elementos eliminados y Correo no deseado. 2. Ordenar por tamaño y archivar adjuntos grandes en OneDrive. 3. Habilitar archivo en línea si la licencia lo permite. 4. Solicitar aumento de cuota solo con justificación.",
     "ACTIVO", True),
    (5, "Microsoft Teams sin audio o sin micrófono en reuniones",
     "Los participantes no escuchan al usuario o el usuario no escucha.",
     "Micrófono silenciado o no detectado en Teams; otras aplicaciones sí funcionan.",
     "Dispositivo de audio incorrecto seleccionado en Teams o permisos de micrófono deshabilitados en Windows.",
     "1. En Teams > Configuración > Dispositivos, seleccionar el micrófono y altavoz correctos y hacer llamada de prueba. 2. En Windows > Privacidad > Micrófono, permitir acceso a las aplicaciones. 3. Limpiar la caché de Teams y reiniciar. 4. Actualizar el controlador de audio o probar con otra diadema.",
     "ACTIVO", True),
    (6, "Reporte de correo de phishing",
     "Un usuario recibió un correo sospechoso que solicita credenciales.",
     "Remitente desconocido o suplantado, enlaces a sitios falsos, urgencia para iniciar sesión.",
     "Campaña de phishing dirigida a la organización.",
     "1. No hacer clic en enlaces ni abrir adjuntos. 2. Reportar con el botón 'Reportar phishing' o reenviar a seguridad. 3. Si el usuario ingresó credenciales, cambiar contraseña de inmediato y revocar sesiones. 4. Seguridad bloquea el remitente y las URL y busca el mismo correo en otros buzones.",
     "ACTIVO", True),
    (6, "El antivirus detectó malware en el equipo",
     "Alerta del antivirus sobre un archivo malicioso.",
     "Notificación de amenaza detectada, archivo en cuarentena, equipo lento o ventanas emergentes.",
     "Descarga de archivo infectado o dispositivo USB comprometido.",
     "1. Desconectar el equipo de la red si hay comportamiento anómalo. 2. Verificar que el archivo esté en cuarentena. 3. Ejecutar análisis completo actualizado. 4. Revisar en la consola de seguridad si hay más equipos afectados. 5. Si el análisis no limpia, reinstalar el sistema desde imagen corporativa.",
     "ACTIVO", True),
    (6, "Certificado SSL vencido en sitio interno",
     "El navegador advierte que la conexión no es privada.",
     "NET::ERR_CERT_DATE_INVALID en un portal interno.",
     "El certificado del servidor expiró.",
     "Borrador: renovar el certificado con la CA interna y desplegarlo en el servidor web.",
     "BORRADOR", False),
    (7, "Cola de impresión atascada",
     "Los documentos no salen de la impresora.",
     "Trabajos en estado 'Error' o 'En cola' que no avanzan; la impresora está encendida.",
     "Trabajo de impresión dañado bloquea la cola del servicio Spooler.",
     "1. Cancelar todos los documentos de la cola. 2. Detener el servicio 'Cola de impresión' (spooler), borrar C:\\Windows\\System32\\spool\\PRINTERS y reiniciar el servicio. 3. Apagar y encender la impresora. 4. Enviar una página de prueba.",
     "ACTIVO", True),
    (7, "El escáner no envía documentos al correo",
     "La multifuncional escanea pero el correo no llega.",
     "Error de envío SMTP en el panel de la impresora.",
     "Configuración SMTP desactualizada.",
     "Borrador: actualizar servidor SMTP y credenciales en la multifuncional.",
     "BORRADOR", False),
    (8, "Cuenta de usuario bloqueada por intentos fallidos",
     "El usuario no puede iniciar sesión en Windows ni en aplicaciones.",
     "Mensaje 'La cuenta de referencia está bloqueada'.",
     "Varios intentos con contraseña incorrecta, frecuentemente por credenciales antiguas guardadas en un celular o unidad de red.",
     "1. Verificar la identidad del usuario según el protocolo. 2. Desbloquear la cuenta en Active Directory. 3. Identificar el origen de los intentos (registro de eventos 4740). 4. Pedir al usuario actualizar la contraseña en celular, unidades mapeadas y credenciales guardadas.",
     "ACTIVO", True),
    (8, "No llega el código MFA o se cambió de celular",
     "El usuario no puede completar la autenticación multifactor.",
     "No llega la notificación de Authenticator o el usuario cambió de teléfono.",
     "El método MFA registrado ya no está disponible en el dispositivo.",
     "1. Verificar identidad del usuario. 2. Restablecer los métodos MFA en el portal de administración. 3. Guiar al usuario para registrar de nuevo Microsoft Authenticator. 4. Validar inicio de sesión de prueba.",
     "ACTIVO", True),
    (8, "Sin permisos para acceder a una carpeta compartida",
     "El usuario recibe acceso denegado a una carpeta de red.",
     "Mensaje 'No tiene permiso para obtener acceso'; otros compañeros sí acceden.",
     "El usuario no pertenece al grupo de seguridad que tiene permisos sobre la carpeta.",
     "1. Confirmar la ruta y el nivel de acceso requerido. 2. Obtener aprobación del dueño de la información. 3. Agregar al usuario al grupo de seguridad correspondiente. 4. Pedir cerrar sesión e iniciar de nuevo para aplicar la membresía.",
     "ACTIVO", True),
]

# Plantillas de incidentes por categoría: (titulo, descripcion)
PLANTILLAS = {
    1: [
        ("Sin internet en el equipo", "El equipo no tiene acceso a internet desde esta mañana. El ícono de red muestra advertencia y no abren páginas."),
        ("Wi-Fi se cae constantemente", "La red inalámbrica corporativa se desconecta varias veces al día y se congelan las videollamadas."),
        ("No abren páginas por nombre", "Puedo hacer ping a una IP pero las páginas web no cargan por nombre, aparece error DNS."),
        ("VPN no conecta desde casa", "El cliente VPN se queda en conectando y luego marca tiempo de espera agotado."),
        ("Sitio bloqueado por proxy", "Necesito acceder a un portal de proveedor y el proxy muestra página de bloqueo."),
        ("Red lenta en el piso 3", "La navegación y las carpetas compartidas están muy lentas en todo el piso."),
    ],
    2: [
        ("Computador no enciende", "El equipo de escritorio no enciende, no prenden luces ni ventiladores."),
        ("Portátil se apaga solo", "El portátil se calienta mucho y se apaga durante las reuniones."),
        ("Disco hace ruido y el equipo está lento", "El equipo se congela al abrir archivos y el disco hace ruidos extraños."),
        ("Segundo monitor sin señal", "Conecté el monitor externo y aparece sin señal."),
    ],
    3: [
        ("Error al iniciar aplicación contable", "La aplicación se cierra al abrir con el error 0xc000007b y falta VCRUNTIME140.dll."),
        ("Aplicación Java se cierra con OutOfMemory", "Al cargar el reporte mensual la aplicación muestra java.lang.OutOfMemoryError: Java heap space."),
        ("ERP no conecta con la base de datos", "Al iniciar sesión en el ERP aparece 'Timeout expired' y no carga información."),
        ("Portal interno con error 500", "El portal de solicitudes muestra error 500 después de la actualización de anoche."),
        ("Instalación de software solicitado", "Se requiere instalar el software de diseño autorizado en el equipo del área."),
    ],
    4: [
        ("Windows Update falla", "Las actualizaciones fallan con el error 0x80070002 desde hace dos semanas."),
        ("Pantalla azul al iniciar", "Después de instalar el controlador de la impresora el equipo muestra pantalla azul."),
        ("Equipo muy lento al iniciar sesión", "El equipo tarda más de 5 minutos en mostrar el escritorio."),
        ("Inicia con perfil temporal", "Al iniciar sesión aparece el mensaje de perfil temporal y no veo mis archivos."),
    ],
    5: [
        ("Outlook pide contraseña", "Outlook pide la contraseña todo el tiempo y no llegan correos nuevos."),
        ("Buzón lleno", "No puedo enviar correos porque el buzón superó el límite."),
        ("Sin audio en Teams", "En las reuniones de Teams no me escuchan aunque el micrófono funciona en otras aplicaciones."),
    ],
    6: [
        ("Correo sospechoso recibido", "Recibí un correo que pide actualizar mi contraseña con un enlace extraño."),
        ("Alerta de antivirus", "El antivirus detectó una amenaza en un archivo descargado y lo puso en cuarentena."),
        ("Advertencia de certificado en portal", "El navegador indica que la conexión no es privada al entrar al portal interno."),
    ],
    7: [
        ("Impresora no imprime", "Los documentos se quedan en la cola de impresión y no salen."),
        ("Escáner no envía al correo", "La multifuncional escanea pero el correo nunca llega."),
    ],
    8: [
        ("Cuenta bloqueada", "Mi cuenta aparece bloqueada y no puedo iniciar sesión."),
        ("No llega código MFA", "Cambié de celular y no puedo aprobar la autenticación multifactor."),
        ("Acceso denegado a carpeta", "No tengo permisos para abrir la carpeta compartida del área financiera."),
    ],
}

# Peso base de cada categoría y crecimiento semanal (tendencia)
PESO_CATEGORIA = {1: 22, 2: 12, 3: 18, 4: 12, 5: 11, 6: 8, 7: 7, 8: 10}
CRECIMIENTO = {1: 0.045, 3: 0.01, 6: 0.02}  # conectividad crece

PRIORIDADES = ["BAJA", "MEDIA", "ALTA", "CRITICA"]
PESO_PRIORIDAD = [25, 40, 25, 10]
# Horas medianas de resolución por prioridad
HORAS_MEDIANA = {"CRITICA": 3.0, "ALTA": 8.0, "MEDIA": 22.0, "BAJA": 52.0}
# Factor por categoría (hardware requiere repuestos, seguridad es urgente)
FACTOR_CATEGORIA = {1: 1.0, 2: 1.6, 3: 1.2, 4: 1.1, 5: 0.8, 6: 0.7, 7: 0.9, 8: 0.6}


def sql(valor):
    """Convierte un valor de Python en literal SQL seguro."""
    if valor is None:
        return "NULL"
    if isinstance(valor, bool):
        return "TRUE" if valor else "FALSE"
    if isinstance(valor, (int, float)):
        return str(valor)
    if isinstance(valor, datetime):
        return "'" + valor.strftime("%Y-%m-%d %H:%M:%S") + "'"
    return "'" + str(valor).replace("'", "''") + "'"


def elegir_ponderado(opciones, pesos):
    return random.choices(opciones, weights=pesos, k=1)[0]


def fecha_laboral_en_semana(semana):
    """Fecha y hora aleatoria dentro de la semana, sesgada a horario laboral."""
    dia = elegir_ponderado(range(7), [20, 20, 20, 20, 18, 1, 1])
    hora = elegir_ponderado(range(24), [0] * 6 + [2, 6, 10, 10, 9, 8, 6, 8, 9, 9, 8, 6, 3, 2, 1, 0, 0, 0])
    minuto = random.randint(0, 59)
    return FECHA_INICIO + timedelta(weeks=semana, days=dia, hours=hora, minutes=minuto)


def generar_incidentes():
    usuarios_por_area = {}
    for idx, (_, _, area) in enumerate(USUARIOS, start=1):
        usuarios_por_area.setdefault(area, []).append(idx)

    incidentes = []
    # Reparto del total por semana, con leve aumento en el tiempo
    pesos_semana = [1 + 0.02 * s for s in range(SEMANAS)]
    semanas = random.choices(range(SEMANAS), weights=pesos_semana, k=TOTAL_INCIDENTES)
    semanas.sort()

    for semana in semanas:
        pesos = [PESO_CATEGORIA[c] * (1 + CRECIMIENTO.get(c, 0) * semana) for c in range(1, 9)]
        cat = elegir_ponderado(range(1, 9), pesos)
        titulo, descripcion = random.choice(PLANTILLAS[cat])
        prioridad = elegir_ponderado(PRIORIDADES, PESO_PRIORIDAD)
        if cat == 6 and prioridad == "BAJA":
            prioridad = "MEDIA"  # seguridad nunca es baja

        creacion = fecha_laboral_en_semana(semana)
        if creacion >= FECHA_CORTE:
            creacion = FECHA_CORTE - timedelta(hours=random.randint(2, 60))

        area = random.choice(AREAS_POR_CATEGORIA[cat])
        usuario = random.choice(usuarios_por_area[area])

        # Tiempo de resolución log-normal alrededor de la mediana
        mediana = HORAS_MEDIANA[prioridad] * FACTOR_CATEGORIA[cat]
        horas = max(0.25, random.lognormvariate(math.log(mediana), 0.55))
        resolucion = creacion + timedelta(hours=round(horas, 2))

        dias_antiguedad = (FECHA_CORTE - creacion).days
        # Pequeño porcentaje de incidentes antiguos queda abierto (backlog)
        queda_abierto = random.random() < (0.04 if dias_antiguedad > 14 else 0.0)

        if resolucion <= FECHA_CORTE and not queda_abierto:
            dias_desde_res = (FECHA_CORTE - resolucion).days
            estado = "CERRADO" if dias_desde_res > 3 and random.random() < 0.6 else "RESUELTO"
            fecha_res = resolucion
            actualizacion = resolucion + (timedelta(days=random.randint(1, 3)) if estado == "CERRADO" else timedelta())
            if actualizacion > FECHA_CORTE:
                actualizacion = resolucion
        else:
            estado = random.choice(["ABIERTO", "EN_PROGRESO"])
            fecha_res = None
            actualizacion = min(FECHA_CORTE, creacion + timedelta(hours=random.randint(0, 6)))

        etiqueta = f" #{len(incidentes) + 1:03d}"
        incidentes.append((titulo + etiqueta, descripcion, cat, prioridad, estado,
                           creacion.replace(second=0, microsecond=0),
                           actualizacion.replace(microsecond=0),
                           fecha_res.replace(microsecond=0) if fecha_res else None,
                           usuario))
    return incidentes


def main():
    lineas = [
        "-- =====================================================================",
        "-- AegisOPS - Datos iniciales (GENERADO, no editar a mano)",
        f"-- Generado por database/generador/generar_datos.py con semilla {SEMILLA}",
        f"-- {len(CATEGORIAS)} categorías, {len(USUARIOS)} usuarios, {len(ARTICULOS)} artículos, {TOTAL_INCIDENTES} incidentes",
        f"-- Fecha de corte de los datos: {FECHA_CORTE:%Y-%m-%d %H:%M}",
        "-- =====================================================================",
        "",
        "BEGIN;",
        "",
    ]

    lineas.append("INSERT INTO categoria (id, nombre, descripcion) VALUES")
    lineas.append(",\n".join(f"({i}, {sql(n)}, {sql(d)})" for i, (n, d) in enumerate(CATEGORIAS, 1)) + ";")
    lineas.append("")

    lineas.append("INSERT INTO usuario (id, nombre, correo, area) VALUES")
    lineas.append(",\n".join(f"({i}, {sql(n)}, {sql(c)}, {sql(a)})" for i, (n, c, a) in enumerate(USUARIOS, 1)) + ";")
    lineas.append("")

    lineas.append("INSERT INTO articulo_conocimiento (id, titulo, problema, sintomas, causa, solucion, categoria_id, estado, verificado, fecha_creacion, fecha_actualizacion) VALUES")
    filas = []
    for i, (cat, titulo, problema, sintomas, causa, solucion, estado, verificado) in enumerate(ARTICULOS, 1):
        creado = FECHA_INICIO - timedelta(days=60 - i)
        filas.append(f"({i}, {sql(titulo)}, {sql(problema)}, {sql(sintomas)}, {sql(causa)}, {sql(solucion)}, "
                     f"{cat}, {sql(estado)}, {sql(verificado)}, {sql(creado)}, {sql(creado)})")
    lineas.append(",\n".join(filas) + ";")
    lineas.append("")

    lineas.append("INSERT INTO incidente (id, titulo, descripcion, categoria_id, prioridad, estado, fecha_creacion, fecha_actualizacion, fecha_resolucion, usuario_id) VALUES")
    filas = []
    for i, inc in enumerate(generar_incidentes(), 1):
        titulo, desc, cat, prio, estado, creado, actualizado, resuelto, usuario = inc
        filas.append(f"({i}, {sql(titulo)}, {sql(desc)}, {cat}, {sql(prio)}, {sql(estado)}, "
                     f"{sql(creado)}, {sql(actualizado)}, {sql(resuelto)}, {usuario})")
    lineas.append(",\n".join(filas) + ";")
    lineas.append("")

    # Ajustar secuencias para que los nuevos registros continúen la numeración
    for tabla in ["categoria", "usuario", "articulo_conocimiento", "incidente"]:
        lineas.append(f"SELECT setval(pg_get_serial_sequence('{tabla}', 'id'), (SELECT MAX(id) FROM {tabla}));")
    lineas += ["", "COMMIT;", ""]

    destino = Path(__file__).resolve().parent.parent / "02_seed.sql"
    destino.write_text("\n".join(lineas), encoding="utf-8")
    print(f"Generado {destino}")


if __name__ == "__main__":
    main()
