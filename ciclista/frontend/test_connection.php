<?php
require_once 'config.php';

header('Content-Type: application/json');
error_reporting(E_ALL);
ini_set('display_errors', 1);

try {
    // Probar conexión
    $pdo->query("SELECT 1");
    echo "✅ Conexión a la base de datos exitosa\n";

    // Verificar tabla sesiones
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM sesiones");
    $sessionCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "📊 Número de sesiones disponibles: $sessionCount\n";

    // Mostrar algunas sesiones de ejemplo
    $stmt = $pdo->query("SELECT session_id, nombre_ciclista, fecha_inicio FROM sesiones LIMIT 3");
    $sessions = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo "\nEjemplos de sesiones:\n";
    foreach ($sessions as $session) {
        echo "- ID: {$session['session_id']}, Ciclista: {$session['nombre_ciclista']}, Fecha: {$session['fecha_inicio']}\n";
    }

    // Verificar registros
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM registros");
    $recordCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "\n📝 Número total de registros: $recordCount\n";

    // Verificar una sesión específica si hay sesiones disponibles
    if ($sessionCount > 0) {
        $firstSession = $sessions[0]['session_id'];
        $stmt = $pdo->prepare("SELECT COUNT(*) as count FROM registros WHERE session_id = ?");
        $stmt->execute([$firstSession]);
        $sessionRecords = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
        echo "📊 Registros en la primera sesión ($firstSession): $sessionRecords\n";

        // Mostrar algunos datos de ejemplo
        $stmt = $pdo->prepare("
            SELECT timestamp, pulsos, velocidad, altitud, latitud, longitud 
            FROM registros 
            WHERE session_id = ? 
            LIMIT 3
        ");
        $stmt->execute([$firstSession]);
        $records = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        echo "\nEjemplos de registros de la sesión $firstSession:\n";
        foreach ($records as $record) {
            echo "- Tiempo: " . date('H:i:s', $record['timestamp']) . 
                 ", Pulsos: {$record['pulsos']}" .
                 ", Velocidad: {$record['velocidad']}" .
                 ", Altitud: {$record['altitud']}\n";
        }
    }

} catch(PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?> 