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
    <title>Datos del Ciclista</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <link rel="stylesheet" href="css/styles.css">
    <link rel="icon" type="image/jpeg" href="img/logo_navegador.jpeg">
</head>
<body>
    <nav class="navbar">
        <div class="logo-container">
            <img src="img/logo_navegador.jpeg" alt="Logo">
            <span class="brand-name">SensorBike</span>
        </div>
        <ul>
            <li><a href="index.php" class="active">Visualizar Datos</a></li>
            <li><a href="tiempo_real.php">Visualizar en Tiempo Real</a></li>
        </ul>
    </nav>

    <div class="page-header">
        <div class="header-content">
            <h1>Análisis de Datos de Entrenamiento</h1>
        </div>
    </div>

    <div class="container">
        <div class="session-selector">
            <h3><i class="fas fa-bicycle"></i> Seleccionar Sesi&oacute;n de Entrenamiento</h3>
            <select id="sessionSelect" class="form-select mb-3">
                <option value="">Seleccione una sesi&oacute;n...</option>
                <?php foreach ($sessions as $session): ?>
                    <option value="<?php echo htmlspecialchars($session['id_sesion']); ?>">
                        <?php 
                        echo htmlspecialchars($session['nombre_ciclista']) . ' - ' . 
                             date('d/m/Y H:i', strtotime($session['fecha_inicio'])); 
                        ?>
                    </option>
                <?php endforeach; ?>
            </select>
            <button class="btn btn-primary" onclick="loadSessionData()">
                <i class="fas fa-chart-line"></i> Cargar Datos
            </button>
        </div>

        <div id="statistics" class="statistics mb-4 d-none">
            <div class="row">
                <div class="col-md-3 mb-4">
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-tachometer-alt"></i>
                        </div>
                        <h4>Velocidad</h4>
                        <div class="stat-values">
                            <div class="stat-item">
                                <span class="stat-label">Promedio:</span>
                                <span class="stat-value" id="avgSpeed">-- km/h</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">M&aacute;xima:</span>
                                <span class="stat-value" id="maxSpeed">-- km/h</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">M&iacute;nima:</span>
                                <span class="stat-value" id="minSpeed">-- km/h</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-4">
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-bolt"></i>
                        </div>
                        <h4>Potencia</h4>
                        <div class="stat-values">
                            <div class="stat-item">
                                <span class="stat-label">Promedio:</span>
                                <span class="stat-value" id="avgPower">-- W</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">M&aacute;xima:</span>
                                <span class="stat-value" id="maxPower">-- W</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">M&iacute;nima:</span>
                                <span class="stat-value" id="minPower">-- W</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-4">
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-sync"></i>
                        </div>
                        <h4>Cadencia</h4>
                        <div class="stat-values">
                            <div class="stat-item">
                                <span class="stat-label">Promedio:</span>
                                <span class="stat-value" id="avgCadence">-- rpm</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">M&aacute;xima:</span>
                                <span class="stat-value" id="maxCadence">-- rpm</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">M&iacute;nima:</span>
                                <span class="stat-value" id="minCadence">-- rpm</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-4">
                    <div class="stat-card">
                        <div class="stat-icon">
                            <i class="fas fa-fire"></i>
                        </div>
                        <h4>Zona de Esfuerzo</h4>
                        <div class="stat-values">
                            <div class="stat-item">
                                <span class="stat-label">M&aacute;s frecuente:</span>
                                <span class="stat-value" id="mostFrequentZone">--</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">Tiempo total:</span>
                                <span class="stat-value" id="totalTime">-- min</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-label">Zonas usadas:</span>
                                <span class="stat-value" id="zonesUsed">--</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div id="charts" class="charts">
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-heartbeat"></i> Frecuencia Card&iacute;aca</h3>
                        <canvas id="heartRateChart"></canvas>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-tachometer-alt"></i> Velocidad</h3>
                        <canvas id="speedChart"></canvas>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-mountain"></i> Altimetr&iacute;a</h3>
                        <canvas id="altimetryChart"></canvas>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-map-marker-alt"></i> Ubicaci&oacute;n</h3>
                        <canvas id="locationChart"></canvas>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-sync"></i> Cadencia</h3>
                        <canvas id="cadenceChart"></canvas>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-bolt"></i> Potencia</h3>
                        <canvas id="powerChart"></canvas>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="chart-container">
                        <h3><i class="fas fa-fire"></i> Zona de Esfuerzo</h3>
                        <canvas id="effortZoneChart"></canvas>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap Bundle with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- Google Maps JavaScript API -->
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyBjWY41n6OHAP3BENJ0eZIon_bHJXWYXxk&callback=initMap&v=weekly" async defer></script>
    <script>
        let charts = {
            heartRate: null,
            speed: null,
            altimetry: null,
            location: null,
            cadence: null,
            power: null,
            effortZone: null
        };

        let map = null;
        let routePath = null;
        let markers = [];

        function initMap() {
            // Coordenadas iniciales (puedes ajustarlas según tu ubicación preferida)
            const initialPosition = { lat: 19.4326, lng: -99.1332 }; // Ciudad de México como ejemplo
            map = new google.maps.Map(document.getElementById('map'), {
                zoom: 15,
                center: initialPosition,
                mapTypeId: 'terrain',
                styles: [
                    {
                        featureType: "poi",
                        elementType: "labels",
                        stylers: [{ visibility: "off" }]
                    }
                ]
            });
        }

        function updateLocationMap(latData, lngData) {
            if (!map) return;

            // Limpiar marcadores anteriores
            markers.forEach(marker => marker.setMap(null));
            markers = [];

            // Crear array de coordenadas para la polilínea
            const coordinates = latData.map((lat, i) => ({
                lat: lat,
                lng: lngData[i]
            }));

            // Eliminar la polilínea anterior si existe
            if (routePath) {
                routePath.setMap(null);
            }

            // Crear nueva polilínea
            routePath = new google.maps.Polyline({
                path: coordinates,
                geodesic: true,
                strokeColor: '#FF0000',
                strokeOpacity: 1.0,
                strokeWeight: 3
            });
            routePath.setMap(map);

            // Agregar marcadores al inicio y fin
            if (coordinates.length > 0) {
                const startMarker = new google.maps.Marker({
                    position: coordinates[0],
                    map: map,
                    title: 'Inicio',
                    icon: {
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 8,
                        fillColor: '#00FF00',
                        fillOpacity: 1,
                        strokeColor: '#FFFFFF',
                        strokeWeight: 2
                    }
                });

                const endMarker = new google.maps.Marker({
                    position: coordinates[coordinates.length - 1],
                    map: map,
                    title: 'Fin',
                    icon: {
                        path: google.maps.SymbolPath.CIRCLE,
                        scale: 8,
                        fillColor: '#FF0000',
                        fillOpacity: 1,
                        strokeColor: '#FFFFFF',
                        strokeWeight: 2
                    }
                });

                markers.push(startMarker, endMarker);
            }

            // Ajustar el mapa para mostrar toda la ruta
            if (coordinates.length > 0) {
                const bounds = new google.maps.LatLngBounds();
                coordinates.forEach(coord => bounds.extend(coord));
                map.fitBounds(bounds);
            }
        }

        function loadSessionData() {
            const sessionId = document.getElementById('sessionSelect').value;
            if (!sessionId) {
                alert('Por favor, seleccione una sesión');
                return;
            }

            // Destruir gráficos existentes
            Object.values(charts).forEach(chart => {
                if (chart) chart.destroy();
            });

            const chartsDiv = document.getElementById('charts');
            chartsDiv.innerHTML = '<div class="loading-message">Cargando datos...</div>';
            chartsDiv.style.display = 'block';

            fetch(`get_session_data.php?session_id=${sessionId}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.error) {
                        throw new Error(data.error);
                    }
                    processData(data);
                })
                .catch(error => {
                    console.error('Error:', error);
                    chartsDiv.innerHTML = `
                        <div class="error-message">
                            <h4>Error al cargar los datos:</h4>
                            <p>${error.message}</p>
                        </div>`;
                });
        }

        function processData(data) {
            // Restaurar el contenedor de gráficos
            const chartsDiv = document.getElementById('charts');
            chartsDiv.innerHTML = `
                <div class="row">
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-heartbeat"></i> Frecuencia Cardíaca</h3>
                            <canvas id="heartRateChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-tachometer-alt"></i> Velocidad</h3>
                            <canvas id="speedChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-mountain"></i> Altimetría</h3>
                            <canvas id="altimetryChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-map-marker-alt"></i> Ubicación</h3>
                            <canvas id="locationChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-sync"></i> Cadencia</h3>
                            <canvas id="cadenceChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-bolt"></i> Potencia</h3>
                            <canvas id="powerChart"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6 mb-4">
                        <div class="chart-container">
                            <h3><i class="fas fa-fire"></i> Zona de Esfuerzo</h3>
                            <canvas id="effortZoneChart"></canvas>
                        </div>
                    </div>
                </div>`;

            // Crear gráficos
            charts.heartRate = createChart(
                'heartRateChart',
                'Frecuencia Cardíaca (BPM)',
                data.labels,
                data.datasets.pulsos,
                'rgb(255, 99, 132)'
            );

            charts.speed = createChart(
                'speedChart',
                'Velocidad (km/h)',
                data.labels,
                data.datasets.velocidad,
                'rgb(54, 162, 235)'
            );

            charts.altimetry = createChart(
                'altimetryChart',
                'Altitud (m)',
                data.labels,
                data.datasets.altitud,
                'rgb(75, 192, 192)'
            );

            charts.location = createLocationChart(
                'locationChart',
                'Ruta',
                data.labels,
                data.datasets.latitud,
                data.datasets.longitud
            );

            charts.cadence = createChart(
                'cadenceChart',
                'Cadencia (rpm)',
                data.labels,
                data.datasets.cadencia,
                'rgb(255, 159, 64)'
            );

            charts.power = createChart(
                'powerChart',
                'Potencia (W)',
                data.labels,
                data.datasets.potencia,
                'rgb(75, 192, 192)'
            );

            charts.effortZone = createEffortZoneChart(
                'effortZoneChart',
                'Zona de Esfuerzo',
                data.labels,
                data.datasets.zona_esfuerzo
            );

            // Calcular estadísticas
            calculateStatistics(data);
        }

        function calculateStatistics(data) {
            // Mostrar la sección de estadísticas
            document.getElementById('statistics').classList.remove('d-none');

            // Velocidad
            const speeds = data.datasets.velocidad.filter(speed => speed >= 0); // Filtrar valores negativos
            const avgSpeed = speeds.reduce((a, b) => a + b, 0) / speeds.length;
            document.getElementById('avgSpeed').textContent = avgSpeed.toFixed(1) + ' km/h';
            document.getElementById('maxSpeed').textContent = Math.max(...speeds).toFixed(1) + ' km/h';
            document.getElementById('minSpeed').textContent = Math.min(...speeds).toFixed(1) + ' km/h';

            // Potencia
            const powers = data.datasets.potencia;
            const avgPower = powers.reduce((a, b) => a + b, 0) / powers.length;
            document.getElementById('avgPower').textContent = avgPower.toFixed(0) + ' W';
            document.getElementById('maxPower').textContent = Math.max(...powers).toFixed(0) + ' W';
            document.getElementById('minPower').textContent = Math.min(...powers).toFixed(0) + ' W';

            // Cadencia
            const cadences = data.datasets.cadencia;
            const avgCadence = cadences.reduce((a, b) => a + b, 0) / cadences.length;
            document.getElementById('avgCadence').textContent = avgCadence.toFixed(0) + ' rpm';
            document.getElementById('maxCadence').textContent = Math.max(...cadences).toFixed(0) + ' rpm';
            document.getElementById('minCadence').textContent = Math.min(...cadences).toFixed(0) + ' rpm';

            // Zona de Esfuerzo
            const zones = data.datasets.zona_esfuerzo;
            const groupedZones = {
                1: 0,
                2: 0,
                3: 0,
                4: 0,
                5: 0
            };

            zones.forEach(zone => {
                if (zone) {
                    const zoneNumber = parseFloat(zone);
                    const mainZone = Math.floor(zoneNumber);
                    if (mainZone >= 1 && mainZone <= 5) {
                        groupedZones[mainZone]++;
                    }
                }
            });
            
            // Encontrar la zona más frecuente
            const mostFrequentZone = Object.entries(groupedZones)
                .reduce((a, b) => a[1] > b[1] ? a : b)[0];
            
            document.getElementById('mostFrequentZone').textContent = `Zona ${mostFrequentZone}`;
            document.getElementById('totalTime').textContent = (zones.length / 60).toFixed(1) + ' min';
            document.getElementById('zonesUsed').textContent = Object.keys(groupedZones).filter(zone => groupedZones[zone] > 0).length;
        }

        function createChart(canvasId, label, labels, data, color) {
            const ctx = document.getElementById(canvasId);
            return new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: label,
                        data: data,
                        borderColor: color,
                        backgroundColor: color.replace(')', ', 0.2)').replace('rgb', 'rgba'),
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4,
                        pointRadius: 3,
                        pointHoverRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top'
                        },
                        tooltip: {
                            mode: 'index',
                            intersect: false
                        }
                    },
                    scales: {
                        x: {
                            display: true,
                            title: {
                                display: true,
                                text: 'Tiempo'
                            }
                        },
                        y: {
                            display: true,
                            title: {
                                display: true,
                                text: label
                            }
                        }
                    }
                }
            });
        }

        function createLocationChart(canvasId, label, labels, latData, lngData) {
            const ctx = document.getElementById(canvasId);
            // Combinar latitud y longitud en puntos
            const points = latData.map((lat, i) => ({
                x: lngData[i],
                y: lat
            }));

            return new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Ruta',
                        data: points,
                        borderColor: 'rgb(75, 192, 192)',
                        backgroundColor: 'rgba(75, 192, 192, 0.2)',
                        borderWidth: 2,
                        fill: false,
                        tension: 0.1,
                        pointRadius: 2,
                        pointHoverRadius: 5
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'top'
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const point = context.raw;
                                    return `Lat: ${point.y.toFixed(6)}, Long: ${point.x.toFixed(6)}`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            type: 'linear',
                            position: 'bottom',
                            title: {
                                display: true,
                                text: 'Longitud'
                            },
                            grid: {
                                display: true,
                                color: 'rgba(0, 0, 0, 0.1)'
                            }
                        },
                        y: {
                            type: 'linear',
                            title: {
                                display: true,
                                text: 'Latitud'
                            },
                            grid: {
                                display: true,
                                color: 'rgba(0, 0, 0, 0.1)'
                            }
                        }
                    }
                }
            });
        }

        function createEffortZoneChart(canvasId, label, labels, data) {
            const ctx = document.getElementById(canvasId);
            const zoneColors = {
                1: 'rgb(0, 255, 0)',    // Verde
                2: 'rgb(255, 255, 0)',  // Amarillo
                3: 'rgb(255, 165, 0)',  // Naranja
                4: 'rgb(255, 0, 0)',    // Rojo
                5: 'rgb(128, 0, 0)'     // Rojo oscuro
            };

            // Agrupar las zonas
            const groupedZones = {
                1: 0,
                2: 0,
                3: 0,
                4: 0,
                5: 0
            };

            data.forEach(zone => {
                if (zone) {
                    // Convertir a número y obtener la zona principal
                    const zoneNumber = parseFloat(zone);
                    const mainZone = Math.floor(zoneNumber);
                    if (mainZone >= 1 && mainZone <= 5) {
                        groupedZones[mainZone]++;
                    }
                }
            });

            // Filtrar zonas que tienen datos
            const filteredZones = Object.entries(groupedZones)
                .filter(([_, count]) => count > 0)
                .map(([zone, count]) => ({
                    label: `Zona ${zone}`,
                    data: count,
                    color: zoneColors[zone]
                }));

            return new Chart(ctx, {
                type: 'pie',
                data: {
                    labels: filteredZones.map(item => item.label),
                    datasets: [{
                        data: filteredZones.map(item => item.data),
                        backgroundColor: filteredZones.map(item => item.color),
                        borderColor: '#fff',
                        borderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: true,
                            position: 'right',
                            labels: {
                                font: {
                                    size: 12
                                }
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const label = context.label;
                                    const value = context.raw;
                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    const percentage = ((value / total) * 100).toFixed(1);
                                    return `${label}: ${value} registros (${percentage}%)`;
                                }
                            }
                        }
                    }
                }
            });
        }
    </script>

    <style>
    .statistics {
        margin-top: 20px;
    }

    .stat-card {
        background: #fff;
        border-radius: 10px;
        padding: 20px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        height: 100%;
        transition: transform 0.3s ease;
    }

    .stat-card:hover {
        transform: translateY(-5px);
    }

    .stat-icon {
        font-size: 2.5rem;
        margin-bottom: 15px;
        color: #4285F4;
    }

    .stat-card h4 {
        color: #333;
        margin-bottom: 15px;
        font-size: 1.2rem;
    }

    .stat-values {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .stat-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    .stat-label {
        color: #666;
        font-size: 0.9rem;
    }

    .stat-value {
        color: #333;
        font-weight: bold;
        font-size: 1rem;
    }

    /* Colores específicos para cada tarjeta */
    .stat-card:nth-child(1) .stat-icon { color: #4285F4; } /* Velocidad - Azul */
    .stat-card:nth-child(2) .stat-icon { color: #EA4335; } /* Potencia - Rojo */
    .stat-card:nth-child(3) .stat-icon { color: #FBBC05; } /* Cadencia - Amarillo */
    .stat-card:nth-child(4) .stat-icon { color: #34A853; } /* Zona de Esfuerzo - Verde */
    </style>
</body>
</html>
