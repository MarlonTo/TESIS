<?php
// Desactivar la visualización de errores en producción
error_reporting(0);
ini_set('display_errors', 0);
echo json_encode(["status" => "success", "message" => "PTO1"]);
// Configuración de la base de datos
define('DB_HOST', 'localhost');
define('DB_USER', 'root');
define('DB_PASS', '2008P4P3lucho');
define('DB_NAME', 'sensor_bike');

// Configuración de headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

try {
    // Conexión a la base de datos con charset para seguridad
    $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4', DB_USER, DB_PASS);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    // Guardar errores en el log
    error_log($e->getMessage());
    echo json_encode(["error" => "Error al conectar a la base de datos"]);
    exit;
}

// Obtener datos del JSON
$data = json_decode(file_get_contents('php://input'), true);

// Validar que los datos están presentes y correctos
$requiredFields = ['tipo', 'mac', 'zona', 'pulsos', 'velocidad', 'altitud', 'distancia', 'latitud', 'longitud', 'cadencia', 'potencia', 'fecha_hora', 'nombre', 'diametro', 'edad'];

/*foreach ($requiredFields as $field) {
    if (!isset($data[$field]) || empty($data[$field])) {
        echo json_encode(["error" => "Campo '$field' es requerido"]);
        exit;
    }
}*/

// Validar el tipo
if ($data['tipo'] !== 'registro') {
    echo json_encode(["error" => "Tipo de registro incorrecto"]);
    exit;
}
// Filtrar y limpiar datos para evitar ataques XSS o SQL Injection
$mac       = filter_var($data['mac'], FILTER_SANITIZE_STRING);
$zona      = filter_var($data['zona'], FILTER_SANITIZE_NUMBER_FLOAT, FILTER_FLAG_ALLOW_FRACTION);
$pulsos    = filter_var($data['pulsos'], FILTER_VALIDATE_INT);
$velocidad = filter_var($data['velocidad'], FILTER_VALIDATE_FLOAT);
$altitud   = filter_var($data['altitud'], FILTER_VALIDATE_FLOAT);
$distancia = filter_var($data['distancia'], FILTER_VALIDATE_FLOAT);
$latitud   = filter_var($data['latitud'], FILTER_VALIDATE_FLOAT);
$longitud  = filter_var($data['longitud'], FILTER_VALIDATE_FLOAT);
$cadencia  = filter_var($data['cadencia'], FILTER_VALIDATE_INT);
$potencia  = filter_var($data['potencia'], FILTER_VALIDATE_INT);
$fecha_hora = DateTime::createFromFormat('d/m/Y H:i:s', $data['fecha_hora'])->format('Y-m-d H:i:s');
$nombre    = filter_var($data['nombre'], FILTER_SANITIZE_STRING);
$diametro  = filter_var($data['diametro'], FILTER_VALIDATE_INT);
$edad      = filter_var($data['edad'], FILTER_VALIDATE_INT);
echo json_encode(["status" => "success", "message" => "PTO2"]);

// Verificar que no haya valores nulos o inválidos
/*if (!$mac || !$zona || !$pulsos || !$velocidad || !$altitud || !$distancia || !$latitud || !$longitud || !$cadencia || !$potencia || !$fecha_hora || !$nombre || !$diametro || !$edad) {
    echo json_encode(["error" => "Datos inválidos o formato incorrecto"]);
    exit;
}*/

// Buscar si ya existe una sesión con esa mac
$stmt = $pdo->prepare("SELECT id FROM sesiones WHERE session_id = :mac LIMIT 1");
$stmt->execute([':mac' => $mac]);
$session = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$session) {
    // Si no existe sesión, crear una nueva
    $stmt = $pdo->prepare("INSERT INTO sesiones (session_id, nombre_ciclista, edad, diametro_rueda, fecha_inicio)
                           VALUES (:mac, :nombre, :edad, :diametro, :fecha_inicio)");
    $stmt->execute([
        ':mac'         => $mac,
        ':nombre'      => $nombre,
        ':edad'        => $edad,
        ':diametro'    => $diametro,
        ':fecha_inicio'=> $fecha_hora
    ]);
    $session_id = $pdo->lastInsertId();
} else {
    $session_id =$mac;
}

echo json_encode(["sesion_id" => "success", "message" => $mac]);
try {
    // Insertar el registro en la tabla de registros
    $stmt = $pdo->prepare("INSERT INTO registros (session_id, zona, pulsos, velocidad, altitud, distancia, latitud, longitud, cadencia, potencia, timestamp)
                           VALUES (:session_id, :zona, :pulsos, :velocidad, :altitud, :distancia, :latitud, :longitud, :cadencia, :potencia, :timestamp)");
    
    $stmt->execute([
        ':session_id' => $session_id,
        ':zona'       => $zona,
        ':pulsos'     => $pulsos,
        ':velocidad'  => $velocidad,
        ':altitud'    => $altitud,
        ':distancia'  => $distancia,
        ':latitud'    => $latitud,
        ':longitud'   => $longitud,
        ':cadencia'   => $cadencia,
        ':potencia'   => $potencia,
        ':timestamp'  => $fecha_hora
    ]);
    
    echo "Registro insertado correctamente.";
} catch (PDOException $e) {
    // Mostrar el mensaje de error
    echo "Error al insertar el registro: " . $e->getMessage();
}
// Respuesta en caso de éxito
echo json_encode(["status" => "success", "message" => "Datos registrados correctamente"]);

?>
