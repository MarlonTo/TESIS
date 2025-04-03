<?php
require_once 'config.php';

header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 1);

try {
    // Probar conexión
    $pdo->query("SELECT 1");
    echo "? Conexión a la base de datos exitosa\n";

    // Verificar tabla sesiones
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM sesiones");
    $sessionCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "?? Número de sesiones disponibles: $sessionCount\n";

    // Mostrar algunas sesiones de ejemplo
    $stmt = $pdo->query("SELECT id_sesion, nombre_ciclista, fecha_inicio FROM sesiones LIMIT 3");
    $sessions = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "\nEjemplos de sesiones:\n";
    foreach ($sessions as $session) {
        echo "- ID: {$session['id_sesion']}, Ciclista: {$session['nombre_ciclista']}, Fecha: {$session['fecha_inicio']}\n";
    }

    // Verificar registros
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM registros");
    $recordCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "\n?? Número total de registros: $recordCount\n";

    // Verificar una sesión específica si hay sesiones disponibles
    if ($sessionCount > 0) {
        $firstSession = $sessions[0]['id_sesion'];
        $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM registros WHERE id_sesion = ?");
        $stmt->execute([$firstSession]);
        $sessionRecords = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
        echo "?? Registros en la primera sesión ($firstSession): $sessionRecords\n";

        // Mostrar algunos datos de ejemplo
        $stmt = $pdo->prepare("
            SELECT fecha_hora, pulsaciones, velocidad, altitud, latitud, longitud, cadencia, potencia 
            FROM registros 
            WHERE id_sesion = ? 
            LIMIT 3
        ");
        $stmt->execute([$firstSession]);
        $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        echo "\nEjemplos de registros de la sesión $firstSession:\n";
        foreach ($records as $record) {
            echo "- Tiempo: " . date('H:i:s', strtotime($record['fecha_hora'])) . 
                 ", Pulsos: {$record['pulsaciones']}" .
                 ", Velocidad: {$record['velocidad']}" .
                 ", Altitud: {$record['altitud']}" .
                 ", Cadencia: {$record['cadencia']}" .
                 ", Potencia: {$record['potencia']}\n";
        }
    }

} catch(PDOException $e) {
    echo "? Error: " . $e->getMessage() . "\n";
}
?> 