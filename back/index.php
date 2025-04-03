<?php
// Desactivar la visualizaci√≥n de errores en producci√≥n
//error_reporting(0);
//ini_set('display_errors', 0);

// Configuraci√≥n de la base de datos
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', '2008P4P3lucho');
define('DB_NAME', 'ciclistadb');


error_log("Este es un mensaje en la consola del servidor PHP");


// Configuraci√≥n de headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Decodificar el JSON recibido
$data = json_decode(file_get_contents('php://input'), true);

// Verificar si la decodificacion fue exitosa
/*if (json_last_error() !== JSON_ERROR_NONE) {
    
    echo json_encode(["error" => "Error al decodificar JSON: " . json_last_error_msg()]);
    exit;
}*/

if (!isset($data['tipo'])) {
    echo json_encode(["error" => "No se encontr√≥ el campo 'tipo' en los datos recibidos"]);
    exit;
}

// Conexion a la base de datos
try {
    $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo json_encode(["status" => "success", "message" => "Conexi√≥n a la base de datos establecida correctamente"]);
} catch (PDOException $e) {
    echo json_encode(["error" => "Error al conectar a la base de datos"]);
    exit;
}

// FunciÛn para verificar y actualizar sesiones inactivas
function actualizarSesionesInactivas($pdo) {
    try {
        $stmt = $pdo->prepare("
            UPDATE sesiones s
            INNER JOIN (
                SELECT id_sesion, MAX(fecha_hora) as ultima_fecha
                FROM registros
                GROUP BY id_sesion
            ) r ON s.id_sesion = r.id_sesion
            SET s.fecha_fin = r.ultima_fecha
            WHERE s.fecha_fin IS NULL 
            AND r.ultima_fecha < DATE_SUB(NOW(), INTERVAL 2 MINUTE)
        ");
        $stmt->execute();
        return true;
    } catch (PDOException $e) {
        error_log("Error al actualizar sesiones inactivas: " . $e->getMessage());
        return false;
    }
}

// Acciones basadas en el tipo de operacion
$actions = [
    'sesion' => function ($data, $pdo) {
        try {
            $stmt = $pdo->prepare("SELECT id_usuario FROM usuarios WHERE mac_dispositivo = :mac");
            $stmt->execute([':mac' => $data['session_id']]);
            $usuario = $stmt->fetch(PDO::FETCH_ASSOC);

            if (!$usuario) {
                $stmt = $pdo->prepare("INSERT INTO usuarios (nombre_usuario, apellido_usuario, tipo_usuario, mac_dispositivo, clave) 
                                     VALUES (:nombre, '', 'C', :mac, :clave)");
                
                $clave = password_hash($data['session_id'], PASSWORD_DEFAULT);
                $stmt->execute([
                    ':nombre' => $data['nombre'],
                    ':mac' => $data['session_id'],
                    ':clave' => $clave
                ]);
                
                $id_usuario = $pdo->lastInsertId();
            } else {
                $id_usuario = $usuario['id_usuario'];
            }

            $stmt = $pdo->prepare("INSERT INTO sesiones (id_usuario, mac_dispositivo, nombre_ciclista, edad_ciclista, diametro_rueda, fecha_inicio)
                                 VALUES (:id_usuario, :mac, :nombre, :edad, :diametro, NOW())");
            
            $stmt->execute([
                ':id_usuario' => $id_usuario,
                ':mac' => $data['session_id'],
                ':nombre' => $data['nombre'],
                ':edad' => $data['edad'],
                ':diametro' => $data['diametro']
            ]);
            
            echo json_encode(["status" => "success", "message" => "Sesi√≥n iniciada correctamente", "id_usuario" => $id_usuario]);
        } catch (PDOException $e) {
            echo json_encode(["error" => "Error al iniciar la sesi√≥n"]);
        }
    },
    'fin_sesion' => function ($data, $pdo) {
        try {
            $stmt = $pdo->prepare("UPDATE sesiones SET fecha_fin = NOW() WHERE mac_dispositivo = :mac");
            $stmt->execute([':mac' => $data['session_id']]);
            echo json_encode(["status" => "success", "message" => "Sesi√≥n finalizada correctamente"]);
        } catch (PDOException $e) {
            logDebug("Error al finalizar la sesi√≥n: " . $e->getMessage());
            echo json_encode(["error" => "Error al finalizar la sesi√≥n"]);
        }
    },
    'registro' => function ($data, $pdo) {
        try {
            // Verificar si existe un usuario con la MAC proporcionada
            $stmt = $pdo->prepare("SELECT id_usuario, nombre_usuario FROM usuarios WHERE mac_dispositivo = :mac");
            $stmt->execute([':mac' => $data['mac_dispositivo']]);
            $usuario = $stmt->fetch(PDO::FETCH_ASSOC);

            if (!$usuario) {
                // Crear nuevo usuario si no existe
                $stmt = $pdo->prepare("INSERT INTO usuarios (nombre_usuario, apellido_usuario, tipo_usuario, mac_dispositivo, clave) 
                                     VALUES (:nombre, '', 'C', :mac, :clave)");
                
                $clave = password_hash('123', PASSWORD_DEFAULT);
                $stmt->execute([
                    ':nombre' => $data['nombre'],
                    ':mac' => $data['mac_dispositivo'],
                    ':clave' => $clave
                ]);
                
                $id_usuario = $pdo->lastInsertId();
                $nombre_usuario = $data['nombre'];
            } else {
                $id_usuario = $usuario['id_usuario'];
                $nombre_usuario = $usuario['nombre_usuario'];
            }

            // Verificar si existe una sesiÛn activa
            $stmt = $pdo->prepare("
                SELECT id_sesion 
                FROM sesiones 
                WHERE mac_dispositivo = :mac 
                AND fecha_fin IS NULL 
                ORDER BY fecha_inicio DESC 
                LIMIT 1
            ");
            $stmt->execute([':mac' => $data['mac_dispositivo']]);
            $sesion = $stmt->fetch(PDO::FETCH_ASSOC);

            if (!$sesion) {
                // Crear nueva sesiÛn
                $stmt = $pdo->prepare("INSERT INTO sesiones (id_usuario, mac_dispositivo, nombre_ciclista, edad_ciclista, diametro_rueda, fecha_inicio)
                                     VALUES (:id_usuario, :mac, :nombre, :edad, :diametro, NOW())");
                
                $stmt->execute([
                    ':id_usuario' => $id_usuario,
                    ':mac' => $data['mac_dispositivo'],
                    ':nombre' => $nombre_usuario,
                    ':edad' => $data['edad'],
                    ':diametro' => $data['diametro']
                ]);
                
                $sesion = ['id_sesion' => $pdo->lastInsertId()];
            }
            
            // Insertar el registro
            $stmt = $pdo->prepare("INSERT INTO registros (id_sesion, zona_esfuerzo, pulsaciones, velocidad, altitud, distancia, latitud, longitud, cadencia, potencia, fecha_hora)
                                 VALUES (:id_sesion, :zona, :pulsos, :velocidad, :altitud, :distancia, :latitud, :longitud, :cadencia, :potencia, :fecha_hora)");
            
            $stmt->execute([
                ':id_sesion' => $sesion['id_sesion'],
                ':zona' => $data['zona_esfuerzo'],
                ':pulsos' => $data['pulsaciones'],
                ':velocidad' => $data['velocidad'],
                ':altitud' => $data['altitud'],
                ':distancia' => $data['distancia'],
                ':latitud' => $data['latitud'],
                ':longitud' => $data['longitud'],
                ':cadencia' => $data['cadencia'],
                ':potencia' => $data['potencia'],
                ':fecha_hora' => $data['fecha_hora']
            ]);
            
            echo json_encode([
                "status" => "success", 
                "message" => "Datos registrados correctamente",
                "sesion_id" => $sesion['id_sesion']
            ]);
        } catch (PDOException $e) {
            error_log("Error en registro: " . $e->getMessage());
            echo json_encode(["error" => "Error al insertar el registro: " . $e->getMessage()]);
        }
    },
    'finalizar' => function ($data, $pdo) {
        try {
            // Buscar la sesiÛn activa y establecer fecha_fin
            $stmt = $pdo->prepare("
                UPDATE sesiones 
                SET fecha_fin = :fecha_fin 
                WHERE mac_dispositivo = :mac 
                AND fecha_fin IS NULL
            ");
            
            $stmt->execute([
                ':mac' => $data['mac_dispositivo'],
                ':fecha_fin' => $data['fecha_fin']
            ]);
            
            if ($stmt->rowCount() > 0) {
                echo json_encode([
                    "status" => "success", 
                    "message" => "SesiÛn finalizada correctamente"
                ]);
            } else {
                echo json_encode([
                    "status" => "warning", 
                    "message" => "No se encontrÛ sesiÛn activa para finalizar"
                ]);
            }
        } catch (PDOException $e) {
            error_log("Error al finalizar sesiÛn: " . $e->getMessage());
            echo json_encode(["error" => "Error al finalizar la sesiÛn: " . $e->getMessage()]);
        }
    }
];

if (isset($actions[$data['tipo']])) {
    $actions[$data['tipo']]($data, $pdo);
} else {
    echo json_encode(["error" => "Tipo de operaci√≥n no v√°lido"]);
}
