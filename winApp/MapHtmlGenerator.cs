using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace DahuaNmeaViewer
{
    public class MapHtmlGenerator
    {
        public static string GenerateHtml(List<GpsPoint> points)
        {
            if (points == null || points.Count == 0)
            {
                return GenerateEmptyMapHtml();
            }
            
            // Validate coordinates
            var validPoints = points.Where(p => 
                p.Latitude != 0 && p.Longitude != 0 &&
                !double.IsNaN(p.Latitude) && !double.IsNaN(p.Longitude) &&
                !double.IsInfinity(p.Latitude) && !double.IsInfinity(p.Longitude)
            ).ToList();
            
            if (validPoints.Count == 0)
            {
                return GenerateEmptyMapHtml();
            }
            
            var center = validPoints[validPoints.Count / 2];
            var centerLat = center.Latitude.ToString(System.Globalization.CultureInfo.InvariantCulture);
            var centerLon = center.Longitude.ToString(System.Globalization.CultureInfo.InvariantCulture);
            var coordinates = string.Join(",\n", validPoints.Select(p => 
                $"[{p.Latitude.ToString(System.Globalization.CultureInfo.InvariantCulture)}, {p.Longitude.ToString(System.Globalization.CultureInfo.InvariantCulture)}]"));
            
            // Generate table rows for GPS points
            var tableRows = new StringBuilder();
            for (int i = 0; i < validPoints.Count; i++)
            {
                var point = validPoints[i];
                var rowClass = i == 0 ? "start-row" : (i == validPoints.Count - 1 ? "end-row" : "");
                
                // Format time from HHMMSS.SSS to HH:MM:SS
                var formattedTime = point.Time;
                if (!string.IsNullOrEmpty(point.Time) && point.Time.Length >= 6)
                {
                    var hours = point.Time.Substring(0, 2);
                    var minutes = point.Time.Substring(2, 2);
                    var seconds = point.Time.Substring(4, 2);
                    formattedTime = $"{hours}:{minutes}:{seconds}";
                }
                
                tableRows.AppendLine($@"
                <tr class='{rowClass}' data-index='{i}'>
                    <td class='point-number'>{i + 1}</td>
                    <td class='point-time'>{formattedTime}</td>
                    <td class='point-lat'>{point.Latitude:F6}</td>
                    <td class='point-lon'>{point.Longitude:F6}</td>
                </tr>");
            }
            
            System.Diagnostics.Debug.WriteLine($"Generating map with {validPoints.Count} valid points");
            System.Diagnostics.Debug.WriteLine($"Center: {centerLat}, {centerLon}");
            System.Diagnostics.Debug.WriteLine($"First point: {validPoints[0].Latitude}, {validPoints[0].Longitude}");
            
            var html = $@"
<!DOCTYPE html>
<html>
<head>
    <meta charset='utf-8' />
    <title>GPS Track</title>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />
    <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
    <style>
        body {{ margin: 0; padding: 0; display: flex; }}
        #map {{ flex: 1; height: 100vh; }}
        #gps-table-container {{
            width: 350px;
            height: 100vh;
            background: white;
            border-left: 2px solid #bdc3c7;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }}
        #gps-table-header {{
            background: #34495e;
            color: white;
            padding: 12px;
            font-family: Arial, sans-serif;
            font-size: 14px;
            font-weight: bold;
            text-align: center;
            border-bottom: 2px solid #2c3e50;
        }}
        #gps-table-wrapper {{
            flex: 1;
            overflow-y: auto;
            overflow-x: hidden;
        }}
        #gps-table {{
            width: 100%;
            border-collapse: collapse;
            font-family: Arial, sans-serif;
            font-size: 12px;
        }}
        #gps-table th {{
            position: sticky;
            top: 0;
            background: #2c3e50;
            color: white;
            padding: 10px 8px;
            text-align: center;
            font-weight: bold;
            border-bottom: 2px solid #34495e;
            z-index: 10;
        }}
        #gps-table td {{
            padding: 8px;
            border-bottom: 1px solid #ecf0f1;
            text-align: center;
        }}
        #gps-table tr:hover {{
            background: #e8f4f8;
            cursor: pointer;
        }}
        #gps-table tr.selected {{
            background: #3498db !important;
            color: white;
            font-weight: bold;
        }}
        #gps-table tr.start-row {{
            background: #d5f4e6;
        }}
        #gps-table tr.end-row {{
            background: #fadbd8;
        }}
        .point-number {{
            font-weight: bold;
            color: #3498db;
        }}
        .point-time {{
            color: #7f8c8d;
            font-size: 11px;
        }}
        .point-lat, .point-lon {{
            font-family: 'Courier New', monospace;
            font-size: 11px;
        }}
        .info-box {{
            position: absolute;
            top: 10px;
            left: 10px;
            background: white;
            padding: 10px;
            border-radius: 5px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.3);
            z-index: 1000;
            font-family: Arial, sans-serif;
            font-size: 12px;
        }}
        #debug {{
            position: absolute;
            bottom: 10px;
            left: 10px;
            background: rgba(255,255,255,0.9);
            padding: 5px;
            font-size: 10px;
            font-family: monospace;
            z-index: 1000;
            max-width: 300px;
            word-wrap: break-word;
        }}
    </style>
</head>
<body>
    <div id='map'></div>
    
    <div id='gps-table-container'>
        <div id='gps-table-header'>
            📍 GPS Track Data ({validPoints.Count} Points)
        </div>
        <div id='gps-table-wrapper'>
            <table id='gps-table'>
                <thead>
                    <tr>
                        <th>No</th>
                        <th>Time</th>
                        <th>Latitude</th>
                        <th>Longitude</th>
                    </tr>
                </thead>
                <tbody>
                    {tableRows}
                </tbody>
            </table>
        </div>
    </div>
    
    <div class='info-box'>
        <b>GPS Track</b><br>
        Points: {points.Count}<br>
        <span id='current-pos'>Position: Start</span>
    </div>
    <div id='debug'>Initializing map...</div>
    
    <script>
        try {{
            document.getElementById('debug').innerHTML = 'Loading Leaflet...';
            
            if (typeof L === 'undefined') {{
                document.getElementById('debug').innerHTML = 'ERROR: Leaflet not loaded!';
                throw new Error('Leaflet library not loaded');
            }}
            
            document.getElementById('debug').innerHTML = 'Creating map...';
            var map = L.map('map').setView([{centerLat}, {centerLon}], 16);
            
            document.getElementById('debug').innerHTML = 'Loading tiles...';
            L.tileLayer('https://{{s}}.tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {{
                attribution: '&copy; OpenStreetMap contributors',
                maxZoom: 19
            }}).addTo(map);
            
            document.getElementById('debug').innerHTML = 'Processing coordinates...';
            var coordinates = [
                {coordinates}
            ];
            
            document.getElementById('debug').innerHTML = 'Drawing {validPoints.Count} GPS points...';
            
            // Draw each GPS point as a circle marker with number label
            var markers = [];
            coordinates.forEach(function(coord, index) {{
                var pointNumber = index + 1;
                var isStart = index === 0;
                var isEnd = index === coordinates.length - 1;
                var markerColor = isStart ? '#27ae60' : (isEnd ? '#e74c3c' : '#3498db');
                
                // Create circle marker
                var marker = L.circleMarker(coord, {{
                    radius: 12,
                    fillColor: markerColor,
                    color: '#ffffff',
                    weight: 2,
                    opacity: 1,
                    fillOpacity: 0.8
                }}).addTo(map);
                
                // Add number label on top of marker
                var numberIcon = L.divIcon({{
                    className: 'number-label',
                    html: '<div style=""' +
                          'background: ' + markerColor + '; ' +
                          'color: white; ' +
                          'width: 24px; ' +
                          'height: 24px; ' +
                          'border-radius: 50%; ' +
                          'border: 2px solid white; ' +
                          'display: flex; ' +
                          'align-items: center; ' +
                          'justify-content: center; ' +
                          'font-weight: bold; ' +
                          'font-size: 11px; ' +
                          'box-shadow: 0 2px 5px rgba(0,0,0,0.3); ' +
                          'font-family: Arial, sans-serif;"">' +
                          pointNumber + '</div>',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                }});
                
                var numberMarker = L.marker(coord, {{
                    icon: numberIcon,
                    zIndexOffset: 1000
                }}).addTo(map);
                
                // Add popup with detailed point info
                var popupContent = 
                    '<b>Point ' + pointNumber + ' of ' + coordinates.length + '</b><br>' +
                    'Lat: ' + coord[0].toFixed(6) + '<br>' +
                    'Lon: ' + coord[1].toFixed(6);
                
                if (isStart) {{
                    popupContent += '<br><span style=""color: #27ae60; font-weight: bold; font-size: 14px;"">🚩 START</span>';
                }}
                if (isEnd) {{
                    popupContent += '<br><span style=""color: #e74c3c; font-weight: bold; font-size: 14px;"">🏁 END</span>';
                }}
                
                marker.bindPopup(popupContent);
                numberMarker.bindPopup(popupContent);
                
                markers.push({{ circle: marker, label: numberMarker }});
            }});
            
            // Connect points with thin line for reference
            var polyline = L.polyline(coordinates, {{
                color: '#95a5a6',
                weight: 2,
                opacity: 0.4,
                dashArray: '5, 5'
            }}).addTo(map);
            
            // Fit bounds to show all points
            if (coordinates.length > 1) {{
                map.fitBounds(polyline.getBounds(), {{ padding: [50, 50] }});
            }} else {{
                map.setView(coordinates[0], 17);
            }}
            
            // Add click event to table rows
            var tableRows = document.querySelectorAll('#gps-table tbody tr');
            tableRows.forEach(function(row, index) {{
                row.addEventListener('click', function() {{
                    // Remove previous selection
                    tableRows.forEach(function(r) {{ r.classList.remove('selected'); }});
                    
                    // Select current row
                    row.classList.add('selected');
                    
                    // Zoom to point on map
                    var coord = coordinates[index];
                    map.setView(coord, 18, {{ animate: true, duration: 0.5 }});
                    
                    // Open popup for this marker
                    markers[index].circle.openPopup();
                    
                    // Scroll row into view
                    row.scrollIntoView({{ behavior: 'smooth', block: 'center' }});
                }});
            }});
        
        // Function to update marker position based on video time
        var totalPoints = coordinates.length;
        var videoDuration = {points.Count}; // Approximate, will be updated from video
        
        window.updateMarkerPosition = function(currentSeconds) {{
            var index = Math.floor((currentSeconds / videoDuration) * totalPoints);
            if (index >= 0 && index < totalPoints) {{
                var pos = coordinates[index];
                currentMarker.setLatLng(pos);
                map.panTo(pos, {{ animate: true, duration: 0.5 }});
                document.getElementById('current-pos').innerHTML = 
                    'Position: ' + (index + 1) + '/' + totalPoints + 
                    '<br>Lat: ' + pos[0].toFixed(6) + 
                    '<br>Lon: ' + pos[1].toFixed(6);
            }}
        }}
        
        document.getElementById('debug').innerHTML = 'Map loaded successfully! Points: ' + coordinates.length;
        setTimeout(function() {{ document.getElementById('debug').style.display = 'none'; }}, 3000);
        
    }} catch (error) {{
        document.getElementById('debug').innerHTML = 'ERROR: ' + error.message + ' - ' + error.stack;
        console.error('Map initialization error:', error);
    }}
    </script>
</body>
</html>";
            
            return html;
        }

        private static string GenerateEmptyMapHtml()
        {
            return @"
<!DOCTYPE html>
<html>
<head>
    <meta charset='utf-8' />
    <title>No GPS Data</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            font-family: Arial, sans-serif;
            background: #ecf0f1;
        }
        .message {
            text-align: center;
            color: #7f8c8d;
            font-size: 24px;
        }
    </style>
</head>
<body>
    <div class='message'>
        🗺️<br>
        No GPS tracking data available
    </div>
</body>
</html>";
        }
    }
}
