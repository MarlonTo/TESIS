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
            <h3><i class="fas fa-bicycle"></i> Seleccionar Sesión de Entrenamiento</h3>
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
            <button class="btn btn-primary" onclick="loadSessionData()">
                <i class="fas fa-chart-line"></i> Cargar Datos
            </button>
        </div>

        <div id="charts" class="charts">
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
            </div>
        </div>
    </div>

    <!-- Bootstrap Bundle with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        let charts = {
            heartRate: null,
            speed: null,
            altimetry: null,
            location: null,
            cadence: null,
            power: null
        };

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

            charts.location = createScatterChart(
                'locationChart',
                'Ubicación',
                data.datasets.longitud,
                data.datasets.latitud,
                'rgb(153, 102, 255)'
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

        function createScatterChart(canvasId, label, xData, yData, color) {
            const ctx = document.getElementById(canvasId);
            const data = xData.map((x, i) => ({
                x: x,
                y: yData[i]
            }));

            return new Chart(ctx, {
                type: 'scatter',
                data: {
                    datasets: [{
                        label: label,
                        data: data,
                        backgroundColor: color,
                        pointRadius: 4,
                        pointHoverRadius: 8
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
                                    return `Lat: ${context.parsed.y.toFixed(6)}, Long: ${context.parsed.x.toFixed(6)}`;
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
                            }
                        },
                        y: {
                            title: {
                                display: true,
                                text: 'Latitud'
                            }
                        }
                    }
                }
            });
        }
    </script>
</body>
</html>
