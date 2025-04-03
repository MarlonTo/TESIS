<?php
require_once 'config.php';

header('Content-Type: application/json');

// Activar reporte de errores
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Verificar conexión a la base de datos
try {
    $pdo->query("SELECT 1");
} catch(PDOException $e) {
    echo json_encode(['error' => 'Error de conexión a la base de datos: ' . $e->getMessage()]);
    exit;
}

if (!isset($_GET['session_id'])) {
    echo json_encode(['error' => 'No se proporcionó session_id']);
    exit;
}

$session_id = $_GET['session_id'];
$latest = isset($_GET['latest']) && $_GET['latest'] === 'true';

try {
    // Verificar que la sesión existe
    $stmt = $pdo->prepare("SELECT id_sesion FROM sesiones WHERE id_sesion = ?");
    $stmt->execute([$session_id]);
    
    if (!$stmt->fetch()) {
        echo json_encode(['error' => 'Sesión no encontrada']);
        exit;
    }

    // Obtener los datos de la sesión
    $query = "
        SELECT 
            latitud,
            longitud,
            TIME(fecha_hora) as tiempo,
            fecha_hora,
            velocidad,
            altitud,
            pulsaciones as pulsos,
            cadencia,
            potencia
        FROM registros 
        WHERE id_sesion = ?";
    
    if ($latest) {
        $query .= " ORDER BY fecha_hora DESC LIMIT 1";
    } else {
        $query .= " ORDER BY fecha_hora ASC";
    }
    
    $stmt = $pdo->prepare($query);
    $stmt->execute([$session_id]);
    $data = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (empty($data)) {
        echo json_encode(['error' => 'No hay datos para esta sesión']);
        exit;
    }

    // Preparar los datos para los gráficos
    $chartData = [
        'labels' => array_map(function($point) {
            return $point['tiempo'];
        }, $data),
        'datasets' => [
            'pulsos' => array_map(function($point) {
                return floatval($point['pulsos']);
            }, $data),
            'velocidad' => array_map(function($point) {
                return floatval($point['velocidad']);
            }, $data),
            'altitud' => array_map(function($point) {
                return floatval($point['altitud']);
            }, $data),
            'latitud' => array_map(function($point) {
                return floatval($point['latitud']);
            }, $data),
            'longitud' => array_map(function($point) {
                return floatval($point['longitud']);
            }, $data),
            'cadencia' => array_map(function($point) {
                return floatval($point['cadencia']);
            }, $data),
            'potencia' => array_map(function($point) {
                return floatval($point['potencia']);
            }, $data)
        ]
    ];

    echo json_encode($chartData);

} catch(PDOException $e) {
    echo json_encode(['error' => 'Error al obtener los datos: ' . $e->getMessage()]);
}
?> 