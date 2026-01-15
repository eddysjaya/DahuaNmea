using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;

namespace DahuaNmeaViewer
{
    public class NmeaParser
    {
        public static List<GpsPoint> ParseNmeaFile(string filePath)
        {
            var points = new List<GpsPoint>();
            
            try
            {
                Console.WriteLine($"[NMEA Parser] Reading file: {filePath}");
                Console.WriteLine($"[NMEA Parser] File exists: {File.Exists(filePath)}");
                
                if (!File.Exists(filePath))
                {
                    Console.WriteLine($"[NMEA Parser] ERROR: File not found!");
                    return points;
                }
                
                var lines = File.ReadAllLines(filePath);
                Console.WriteLine($"[NMEA Parser] Total lines in file: {lines.Length}");
                
                int validSentences = 0;
                int invalidSentences = 0;
                
                foreach (var line in lines)
                {
                    if (line.StartsWith("$GPGGA") || line.StartsWith("$GNGGA"))
                    {
                        validSentences++;
                        var point = ParseGGA(line);
                        if (point != null)
                        {
                            Console.WriteLine($"[NMEA Parser] Point #{points.Count + 1}: Lat={point.Latitude:F6}, Lon={point.Longitude:F6}, Alt={point.Altitude:F1}m, Time={point.Time}");
                            points.Add(point);
                        }
                        else
                        {
                            invalidSentences++;
                            Console.WriteLine($"[NMEA Parser] Skipped invalid GGA: {line.Substring(0, Math.Min(50, line.Length))}");
                        }
                    }
                }
                
                Console.WriteLine($"[NMEA Parser] Summary: {points.Count} valid points from {validSentences} GGA sentences ({invalidSentences} invalid)");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[NMEA Parser] ERROR: {ex.Message}");
                System.Diagnostics.Debug.WriteLine($"Error parsing NMEA: {ex.Message}");
            }
            
            return points;
        }

        private static GpsPoint? ParseGGA(string sentence)
        {
            try
            {
                var parts = sentence.Split(',');
                
                if (parts.Length < 10)
                    return null;
                
                // Check fix quality
                if (parts[6] == "0")
                    return null; // No fix
                
                var lat = ParseCoordinate(parts[2], parts[3]);
                var lon = ParseCoordinate(parts[4], parts[5]);
                
                if (lat == 0 && lon == 0)
                    return null;
                
                return new GpsPoint
                {
                    Latitude = lat,
                    Longitude = lon,
                    Altitude = ParseDouble(parts[9]),
                    Time = parts[1],
                    Quality = int.Parse(parts[6])
                };
            }
            catch
            {
                return null;
            }
        }

        private static double ParseCoordinate(string value, string direction)
        {
            if (string.IsNullOrEmpty(value) || string.IsNullOrEmpty(direction))
                return 0;
            
            try
            {
                // NMEA format: DDMM.MMMM or DDDMM.MMMM
                // Example: 0611.8622 = 06 degrees, 11.8622 minutes
                //          10647.7169 = 106 degrees, 47.7169 minutes
                
                var dotIndex = value.IndexOf('.');
                if (dotIndex < 2)
                    return 0;
                
                // For latitude: DD (2 digits), for longitude: DDD (3 digits)
                // Determine by checking if direction is N/S (lat) or E/W (lon)
                var isLatitude = (direction == "N" || direction == "S");
                var degreeDigits = isLatitude ? 2 : 3;
                
                // Extract degrees
                var degrees = int.Parse(value.Substring(0, degreeDigits));
                
                // Extract minutes (rest of the string)
                var minutes = double.Parse(value.Substring(degreeDigits), CultureInfo.InvariantCulture);
                
                // Convert to decimal degrees
                var result = degrees + (minutes / 60.0);
                
                // Apply direction
                if (direction == "S" || direction == "W")
                    result = -result;
                
                return result;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error parsing coordinate '{value}' '{direction}': {ex.Message}");
                return 0;
            }
        }

        private static double ParseDouble(string value)
        {
            if (double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out var result))
                return result;
            return 0;
        }
    }

    public class GpsPoint
    {
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public double Altitude { get; set; }
        public string Time { get; set; } = "";
        public int Quality { get; set; }
    }
}
