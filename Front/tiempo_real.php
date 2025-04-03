<?php
require_once 'config.php';

// Obtener todas las sesiones disponibles
try {
    $stmt = $pdo->query("
        SELECT id_sesion, nombre_ciclista, fecha_inicio 
        FROM sesiones 
        ORDER BY fecha_inicio DESC
    ");
    $sessions = $stmt->fetchAll(PDO::FETCH_ASSOC);
} catch(PDOException $e) {
    echo "Error: " . $e->getMessage();
}
?>
<!DOCTYPE html>
<html>
<head>
    <title>Datos en Tiempo Real - Ciclista</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <link rel="stylesheet" href="css/styles.css">
    <link rel="icon" type="image/jpeg" href="img/logo_navegador.jpeg">
    <style>
        #map {
            height: 400px;
            width: 100%;
            border-radius: 10px;
            margin-bottom: 20px;
        }
        #map-error {
            display: none;
            padding: 20px;
            background-color: #ffebee;
            border: 1px solid #ffcdd2;
            border-radius: 10px;
            margin-bottom: 20px;
            color: #c62828;
        }
    </style>
    <!-- Google Maps JavaScript API -->
    <script>
        // Función que se llamará cuando la API de Google Maps esté lista
        function initGoogleMaps() {
            // La API está cargada y disponible
            initMap();
        }

        // Función que se llamará si hay un error al cargar la API
        function handleGoogleMapsError() {
            console.error('Error al cargar Google Maps');
            document.getElementById('map').style.display = 'none';
            document.getElementById('map-error').style.display = 'block';
        }
    </script>
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyBjWY41n6OHAP3BENJ0eZIon_bHJXWYXxk&callback=initGoogleMaps&v=weekly" async defer onerror="handleGoogleMapsError()"></script>
</head>
<body>
    <nav class="navbar">
        <div class="logo-container">
            <img src="img/logo_navegador.jpeg" alt="Logo">
            <span class="brand-name">SensorBike</span>
        </div>
        <ul>
            <li><a href="index.php">Visualizar Datos</a></li>
            <li><a href="tiempo_real.php" class="active">Visualizar en Tiempo Real</a></li>
        </ul>
    </nav>

    <div class="page-header">
        <div class="header-content">
            <h1>Monitoreo en Tiempo Real</h1>
        </div>
    </div>

    <div class="container">
        <div class="session-selector">
            <h3><i class="fas fa-bicycle"></i> Seleccionar Sesión Activa</h3>
            <select id="sessionSelect" class="form-select mb-3">
                <option value="">Seleccione una sesión...</option>
                <?php foreach ($sessions as $session): ?>
                    <option value="<?php echo htmlspecialchars($session['id_sesion']); ?>">
                        <?php 
                        echo htmlspecialchars($session['nombre_ciclista']) . ' - ' . 
                             date('d/m/Y H:i', strtotime($session['fecha_inicio'])); 
                        ?>
                    </option>
                <?php endforeach; ?>
            </select>
            <div class="btn-group" role="group">
                <button class="btn btn-primary" onclick="startRealTimeUpdates()">
                    <i class="fas fa-play"></i> Iniciar Monitoreo
                </button>
                <button class="btn btn-danger" onclick="stopRealTimeUpdates()">
                    <i class="fas fa-stop"></i> Detener Monitoreo
                </button>
            </div>
        </div>

        <div id="realTimeData" class="real-time-container d-none">
            <div class="row">
                <div class="col-md-6 col-lg-3 mb-4">
                    <div class="data-card">
                        <h4><i class="fas fa-heartbeat"></i> Frecuencia Cardíaca</h4>
                        <div class="data-value" id="heartRate">-- BPM</div>
                        <div class="data-zone" id="heartRateZone">Zona --</div>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3 mb-4">
                    <div class="data-card">
                        <h4><i class="fas fa-tachometer-alt"></i> Velocidad</h4>
                        <div class="data-value" id="speed">-- km/h</div>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3 mb-4">
                    <div class="data-card">
                        <h4><i class="fas fa-mountain"></i> Altitud</h4>
                        <div class="data-value" id="altitude">-- m</div>
                    </div>
                </div>
                <div class="col-md-6 col-lg-3 mb-4">
                    <div class="data-card">
                        <h4><i class="fas fa-sync"></i> Cadencia</h4>
                        <div class="data-value" id="cadence">-- rpm</div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-md-12 mb-4">
                    <div class="data-card">
                        <h4><i class="fas fa-map-marker-alt"></i> Ubicación en Tiempo Real</h4>
                        <div id="map-error">
                            <i class="fas fa-exclamation-triangle"></i> 
                            No se pudo cargar el mapa. Por favor, verifica tu conexión a internet y recarga la página.
                        </div>
                        <div id="map"></div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="data-card">
                        <h4><i class="fas fa-bolt"></i> Potencia</h4>
                        <div class="data-value" id="power">-- W</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap Bundle with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        let updateInterval = null;
        let currentSessionId = null;
        let map = null;
        let marker = null;
        let routePath = null;
        let routeCoordinates = [];
        let infoWindow = null;

        // Inicializar el mapa
        function initMap() {
            try {
                // Coordenadas iniciales (puedes ajustarlas según tu ubicación preferida)
                const initialPosition = { lat: 19.4326, lng: -99.1332 }; // Ciudad de México como ejemplo
                map = new google.maps.Map(document.getElementById('map'), {
                    zoom: 15,
                    center: initialPosition,
                    mapTypeId: 'terrain', // Tipo de mapa más adecuado para ciclismo
                    styles: [
                        {
                            featureType: "poi",
                            elementType: "labels",
                            stylers: [{ visibility: "off" }]
                        }
                    ]
                });

                // Crear el marcador inicial
                marker = new google.maps.Marker({
                    position: initialPosition,
                    map: map,
                    title: 'Posición del ciclista',
                    icon: {
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 10,
                        fillColor: '#4285F4',
                        fillOpacity: 1,
                        strokeColor: '#ffffff',
                        strokeWeight: 2
                    }
                });

                // Inicializar InfoWindow
                infoWindow = new google.maps.InfoWindow({
                    content: 'Cargando datos...'
                });

                // Mostrar InfoWindow al hacer clic en el marcador
                marker.addListener('click', () => {
                    infoWindow.open(map, marker);
                });

                // Inicializar la línea de ruta
                routePath = new google.maps.Polyline({
                    path: [],
                    geodesic: true,
                    strokeColor: '#FF0000',
                    strokeOpacity: 1.0,
                    strokeWeight: 3
                });
                routePath.setMap(map);
            } catch (error) {
                console.error('Error al inicializar el mapa:', error);
                handleGoogleMapsError();
            }
        }

        function getHeartRateZone(bpm) {
            if (bpm < 123) return 'Zona 1 - Recuperación';
            if (bpm < 143) return 'Zona 2 - Resistencia';
            if (bpm < 163) return 'Zona 3 - Tempo';
            if (bpm < 178) return 'Zona 4 - Umbral';
            return 'Zona 5 - Máximo';
        }

        function updateInfoWindowContent(lat, lng, altitude, speed) {
            const content = `
                <div style="padding: 10px;">
                    <h6 style="margin: 0 0 8px 0;"><i class="fas fa-info-circle"></i> Datos de Ubicación</h6>
                    <p style="margin: 0 0 5px 0;"><strong>Latitud:</strong> ${lat.toFixed(6)}</p>
                    <p style="margin: 0 0 5px 0;"><strong>Longitud:</strong> ${lng.toFixed(6)}</p>
                    <p style="margin: 0 0 5px 0;"><strong>Altitud:</strong> ${altitude.toFixed(0)} m</p>
                    <p style="margin: 0;"><strong>Velocidad:</strong> ${speed.toFixed(1)} km/h</p>
                </div>
            `;
            infoWindow.setContent(content);
        }

        function startRealTimeUpdates() {
            const sessionId = document.getElementById('sessionSelect').value;
            if (!sessionId) {
                alert('Por favor, seleccione una sesión');
                return;
            }

            currentSessionId = sessionId;
            document.getElementById('realTimeData').classList.remove('d-none');
            if (!map) {
                initMap();
            }
            // Reiniciar la ruta
            routeCoordinates = [];
            if (routePath) {
                routePath.setPath([]);
            }
            updateInterval = setInterval(updateData, 1000);
        }

        function stopRealTimeUpdates() {
            if (updateInterval) {
                clearInterval(updateInterval);
                updateInterval = null;
            }
        }

        function updateData() {
            if (!currentSessionId) return;

            fetch(`get_session_data.php?session_id=${currentSessionId}&latest=true`)
                .then(response => response.json())
                .then(data => {
                    if (data.error) {
                        console.error('Error:', data.error);
                        return;
                    }

                    const lastIndex = data.datasets.pulsos.length - 1;
                    if (lastIndex >= 0) {
                        const heartRate = data.datasets.pulsos[lastIndex];
                        document.getElementById('heartRate').textContent = heartRate + ' BPM';
                        document.getElementById('heartRateZone').textContent = getHeartRateZone(heartRate);
                        document.getElementById('speed').textContent = data.datasets.velocidad[lastIndex].toFixed(1) + ' km/h';
                        document.getElementById('altitude').textContent = data.datasets.altitud[lastIndex].toFixed(0) + ' m';
                        
                        // Actualizar coordenadas y mapa
                        const lat = data.datasets.latitud[lastIndex];
                        const lng = data.datasets.longitud[lastIndex];
                        const altitude = data.datasets.altitud[lastIndex];
                        const speed = data.datasets.velocidad[lastIndex];
                        
                        if (map && marker) {
                            const newPosition = { lat: lat, lng: lng };
                            
                            // Actualizar marcador y contenido del InfoWindow
                            marker.setPosition(newPosition);
                            map.panTo(newPosition);
                            updateInfoWindowContent(lat, lng, altitude, speed);
                            infoWindow.open(map, marker);

                            // Actualizar línea de ruta
                            routeCoordinates.push(newPosition);
                            if (routePath) {
                                routePath.setPath(routeCoordinates);
                            }

                            // Ajustar el zoom para mostrar toda la ruta
                            if (routeCoordinates.length > 1) {
                                const bounds = new google.maps.LatLngBounds();
                                routeCoordinates.forEach(coord => bounds.extend(coord));
                                map.fitBounds(bounds);
                            }
                        }
                        
                        // Si existen los datos de cadencia y potencia
                        if (data.datasets.cadencia) {
                            document.getElementById('cadence').textContent = data.datasets.cadencia[lastIndex] + ' rpm';
                        }
                        if (data.datasets.potencia) {
                            document.getElementById('power').textContent = data.datasets.potencia[lastIndex] + ' W';
                        }
                    }
                })
                .catch(error => {
                    console.error('Error al actualizar datos:', error);
                });
        }
    </script>
</body>
</html> 