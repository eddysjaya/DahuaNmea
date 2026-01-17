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
        /* Force polyline visibility */
        .leaflet-overlay-pane svg {{
            z-index: 400 !important;
        }}
        .road-polyline, .road-polyline-snapped {{
            stroke: #00ff00 !important;
            stroke-width: 8 !important;
            stroke-opacity: 1 !important;
            fill: none !important;
            pointer-events: auto !important;
        }}
        path.road-polyline, path.road-polyline-snapped {{
            stroke: #00ff00 !important;
            stroke-width: 8 !important;
        }}
    </style>
</head>
<body>
    <div id='map'></div>
    
    <script>
        try {{
            if (typeof L === 'undefined') {{
                throw new Error('Leaflet library not loaded');
            }}
            
            var map = L.map('map').setView([{centerLat}, {centerLon}], 16);
            
            L.tileLayer('https://{{s}}.tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {{
                attribution: '&copy; OpenStreetMap contributors',
                maxZoom: 19
            }}).addTo(map);
            
            var coordinates = [
                {coordinates}
            ];
            
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
            
            // Create animated current position marker (DARK BLUE)
            var currentMarker = L.circleMarker(coordinates[0], {{
                radius: 14,
                fillColor: '#1a237e',
                color: '#ffffff',
                weight: 4,
                opacity: 1,
                fillOpacity: 1,
                zIndexOffset: 2000
            }}).addTo(map);
            
            // Add pulsing animation effect (BLUE)
            var pulseIcon = L.divIcon({{
                className: 'pulse-icon',
                html: '<div style=""' +
                      'width: 28px; ' +
                      'height: 28px; ' +
                      'border-radius: 50%; ' +
                      'background: rgba(26, 35, 126, 0.6); ' +
                      'border: 3px solid #1a237e; ' +
                      'animation: pulse 1.5s infinite;"">' +
                      '</div>' +
                      '<style>' +
                      '@keyframes pulse {{' +
                      '0% {{ transform: scale(0.8); opacity: 1; }}' +
                      '50% {{ transform: scale(1.3); opacity: 0.6; }}' +
                      '100% {{ transform: scale(1.8); opacity: 0; }}' +
                      '}}' +
                      '</style>',
                iconSize: [28, 28],
                iconAnchor: [14, 14]
            }});
            
            var pulseMarker = L.marker(coordinates[0], {{
                icon: pulseIcon,
                zIndexOffset: 1999
            }}).addTo(map);
            
            // Connect points with thin line for reference (original GPS points)
            var polyline = L.polyline(coordinates, {{
                color: '#e74c3c',
                weight: 3,
                opacity: 0.5,
                dashArray: '10, 10',
                zIndexOffset: -100
            }}).addTo(map);
            
            // ALWAYS draw green road line (fallback to original GPS if snap fails)
            var roadPolyline = L.polyline(coordinates, {{
                color: '#00ff00',
                weight: 8,
                opacity: 1,
                smoothFactor: 1.0,
                zIndexOffset: 100,
                className: 'road-polyline'
            }}).addTo(map);
            
            roadPolyline.bringToFront();
            
            // Snap to road coordinates (will replace roadPolyline if successful)
            var snappedCoordinates = [];
            
            // Function to snap GPS points to road using OSRM API
            async function snapToRoad() {{
                try {{
                    document.getElementById('debug').innerHTML = '🛣️ Getting road route...';
                    console.log('GPS points:', coordinates.length);
                    
                    if (coordinates.length < 2) {{
                        console.warn('Not enough points for routing');
                        snappedCoordinates = coordinates;
                        return;
                    }}
                    
                    // Use ROUTE API (not MATCH) for sparse GPS points
                    // This creates a route along roads between GPS waypoints
                    var coordsString = coordinates.map(c => c[1] + ',' + c[0]).join(';');
                    var osrmUrl = 'https://router.project-osrm.org/route/v1/driving/' + coordsString + 
                                  '?overview=full&geometries=geojson&steps=true&continue_straight=false';
                    
                    console.log('OSRM Route API URL:', osrmUrl);
                    
                    var response = await fetch(osrmUrl);
                    var data = await response.json();
                    
                    console.log('OSRM Response:', data);
                    
                    if (data.code === 'Ok' && data.routes && data.routes[0]) {{
                        // Extract route coordinates
                        var geometry = data.routes[0].geometry.coordinates;
                        snappedCoordinates = geometry.map(c => [c[1], c[0]]); // Convert [lon,lat] to [lat,lon]
                        
                        console.log('✅ Route created:', snappedCoordinates.length, 'points along roads');
                        
                        // Remove old polyline
                        map.removeLayer(roadPolyline);
                        
                        // Draw NEW road route - should follow actual roads
                        roadPolyline = L.polyline(snappedCoordinates, {{
                            color: '#00ff00',
                            weight: 8,
                            opacity: 1,
                            smoothFactor: 1.0,
                            zIndexOffset: 100,
                            className: 'road-polyline-snapped'
                        }}).addTo(map);
                        
                        roadPolyline.bringToFront();
                        
                        var distance = (data.routes[0].distance / 1000).toFixed(2);
                        var duration = Math.round(data.routes[0].duration / 60);
                        console.log('✅ Road route active:', snappedCoordinates.length, 'points,', distance, 'km');
                    }} else {{
                        console.warn('⚠️ OSRM routing failed:', data.code);
                        console.log('Error details:', data);
                        snappedCoordinates = coordinates;
                    }}
                }} catch (error) {{
                    console.error('❌ Routing error:', error);
                    snappedCoordinates = coordinates;
                }}
            }}
            
            // Call snap-to-road
            snapToRoad();
            
            // Fit bounds to show all points
            if (coordinates.length > 1) {{
                map.fitBounds(polyline.getBounds(), {{ padding: [50, 50] }});
            }} else {{
                map.setView(coordinates[0], 17);
            }}
        
        // Function to interpolate between two points
        function interpolatePoint(p1, p2, fraction) {{
            return [
                p1[0] + (p2[0] - p1[0]) * fraction,
                p1[1] + (p2[1] - p1[1]) * fraction
            ];
        }}
        
        // Function to find closest point on snapped route
        function getPositionOnRoute(progress) {{
            var coords = snappedCoordinates.length > 0 ? snappedCoordinates : coordinates;
            
            // Clamp progress to 0-1 range
            progress = Math.max(0, Math.min(1, progress));
            
            if (coords.length === 0) return [0, 0];
            if (coords.length === 1) return coords[0];
            if (progress === 0) return coords[0];
            if (progress === 1) return coords[coords.length - 1];
            
            var totalSegments = coords.length - 1;
            var exactPosition = progress * totalSegments;
            var segmentIndex = Math.floor(exactPosition);
            var segmentProgress = exactPosition - segmentIndex;
            
            // Safety check
            if (segmentIndex >= totalSegments) {{
                return coords[coords.length - 1];
            }}
            
            return interpolatePoint(coords[segmentIndex], coords[segmentIndex + 1], segmentProgress);
        }}
        
        // Function to update marker position - map video time directly to GPS point number
        var totalPoints = coordinates.length;
        var videoDuration = 1; // Will be updated from actual video duration
        var videoStarted = false;
        var pointsPerSecond = 1; // Will be calculated when video duration is set
        
        window.updateMarkerPosition = function(currentSeconds) {{
            // Set video as started
            if (!videoStarted && currentSeconds > 0) {{
                videoStarted = true;
            }}
            
            // Map current video time directly to GPS point index
            // If video = 293s and points = 293, then at 146s we show point 146
            var pointIndex = currentSeconds * pointsPerSecond;
            pointIndex = Math.max(0, Math.min(totalPoints - 1, pointIndex));
            
            // ALWAYS use GPS coordinates for marker position (not snapped route!)
            // Snapped route is only for the green line, marker follows actual GPS points
            var baseIndex = Math.floor(pointIndex);
            var nextIndex = Math.min(baseIndex + 1, totalPoints - 1);
            var fraction = pointIndex - baseIndex;
            
            // Interpolate between GPS points directly
            var pos = interpolatePoint(coordinates[baseIndex], coordinates[nextIndex], fraction);
            
            // Update animated marker and pulse
            currentMarker.setLatLng(pos);
            pulseMarker.setLatLng(pos);
            
            // Smooth pan to position (don't jump)
            if (map.getZoom() >= 15) {{ // Only pan if zoomed in enough
                map.panTo(pos, {{ animate: true, duration: 0.1, noMoveStart: true }});
            }}
            
            // Round to nearest point for display
            var displayIndex = Math.round(pointIndex);
            
            // Expose displayIndex to global scope for table sync
            window.currentDisplayIndex = displayIndex;
        }}
        // Update video duration when known
        window.setVideoDuration = function(durationSeconds) {{
            videoDuration = durationSeconds;
            
            // Calculate how many GPS points per second
            // If 293 points in 293 seconds → 1 point/second
            // If 293 points in 146.5 seconds → 2 points/second
            pointsPerSecond = (totalPoints - 1) / videoDuration;
            
            console.log('========================================');
            console.log('📍 GPS ANIMATION SYNC CALCULATION');
            console.log('========================================');
            console.log('Video duration: ' + durationSeconds.toFixed(2) + ' seconds');
            console.log('GPS points: ' + totalPoints);
            console.log('Points per second: ' + pointsPerSecond.toFixed(4));
            console.log('Expected behavior:');
            console.log('  0s → Point 1');
            console.log('  ' + (durationSeconds/2).toFixed(1) + 's → Point ' + Math.round(totalPoints/2));
            console.log('  ' + durationSeconds.toFixed(1) + 's → Point ' + totalPoints);
            console.log('========================================');
        }}
        
    }} catch (error) {{
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
        
        public static string GenerateTableHtml(List<GpsPoint> points)
        {
            var validPoints = points.Where(p => p.Latitude != 0 || p.Longitude != 0).ToList();
            
            var tableRows = new System.Text.StringBuilder();
            for (int i = 0; i < validPoints.Count; i++)
            {
                var point = validPoints[i];
                var timeStr = point.Time; // Time is already a string in GpsPoint
                tableRows.AppendLine($@"
                    <tr data-index='{i}'>
                        <td class='point-num'>{i + 1}</td>
                        <td class='point-time'>{timeStr}</td>
                        <td class='point-lat'>{point.Latitude:F6}</td>
                        <td class='point-lon'>{point.Longitude:F6}</td>
                    </tr>");
            }
            
            return $@"
<!DOCTYPE html>
<html>
<head>
    <meta charset='utf-8' />
    <title>GPS Data Table</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        body {{
            font-family: Arial, sans-serif;
            background: #f5f5f5;
            height: 100vh;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        }}
        #gps-table-header {{
            background: #2C3E50;
            color: white;
            padding: 10px;
            font-weight: bold;
            font-size: 14px;
            text-align: center;
            border-bottom: 2px solid #1ABC9C;
        }}
        #gps-table-wrapper {{
            flex: 1;
            overflow-y: auto;
            background: white;
        }}
        #gps-table {{
            width: 100%;
            border-collapse: collapse;
        }}
        #gps-table thead {{
            position: sticky;
            top: 0;
            background: #34495E;
            color: white;
            z-index: 10;
        }}
        #gps-table th {{
            padding: 8px;
            text-align: center;
            font-size: 12px;
            border-bottom: 2px solid #1ABC9C;
        }}
        #gps-table td {{
            padding: 6px 8px;
            text-align: center;
            font-size: 11px;
            border-bottom: 1px solid #ecf0f1;
        }}
        #gps-table tbody tr:hover {{
            background: #ecf0f1;
        }}
        #gps-table tbody tr.selected {{
            background: #fffacd !important;
            font-weight: bold;
        }}
        .point-num {{
            color: #3498db;
            font-weight: bold;
        }}
        .point-time {{
            color: #e67e22;
        }}
        .point-lat, .point-lon {{
            font-family: 'Courier New', monospace;
            font-size: 10px;
            color: #27ae60;
        }}
        
        /* Scrollbar styling */
        #gps-table-wrapper::-webkit-scrollbar {{
            width: 8px;
        }}
        #gps-table-wrapper::-webkit-scrollbar-track {{
            background: #f1f1f1;
        }}
        #gps-table-wrapper::-webkit-scrollbar-thumb {{
            background: #888;
            border-radius: 4px;
        }}
        #gps-table-wrapper::-webkit-scrollbar-thumb:hover {{
            background: #555;
        }}
    </style>
</head>
<body>
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
    
    <script>
        var tableRows = Array.from(document.querySelectorAll('#gps-table tbody tr'));
        
        // Function to highlight row (called from C#)
        window.highlightRow = function(pointIndex) {{
            tableRows.forEach(function(r, i) {{
                if (i === pointIndex) {{
                    r.classList.add('selected');
                    // Auto-scroll to keep selected row visible
                    r.scrollIntoView({{ 
                        behavior: 'smooth', 
                        block: 'center',
                        inline: 'nearest'
                    }});
                }} else {{
                    r.classList.remove('selected');
                }}
            }});
        }};
        
        console.log('GPS Table loaded with ' + tableRows.length + ' rows');
    </script>
</body>
</html>";
        }
    }
}
