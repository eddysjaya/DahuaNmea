// Test NMEA parsing
using System;
using System.Globalization;

string nmea = "$GPGGA,175616.000,0611.8622,S,10647.7169,E,1,08,0.7,25.8,M,0.0,M,,*46";
var parts = nmea.Split(',');

Console.WriteLine($"NMEA: {nmea}");
Console.WriteLine($"Parts count: {parts.Length}");
Console.WriteLine($"Lat value: {parts[2]}");
Console.WriteLine($"Lat dir: {parts[3]}");
Console.WriteLine($"Lon value: {parts[4]}");
Console.WriteLine($"Lon dir: {parts[5]}");
Console.WriteLine();

// Parse latitude
string latValue = parts[2];
string latDir = parts[3];
bool isLatitude = (latDir == "N" || latDir == "S");
int degreeDigits = isLatitude ? 2 : 3;

int degrees = int.Parse(latValue.Substring(0, degreeDigits));
double minutes = double.Parse(latValue.Substring(degreeDigits), CultureInfo.InvariantCulture);
double lat = degrees + (minutes / 60.0);
if (latDir == "S") lat = -lat;

Console.WriteLine($"Latitude parsing:");
Console.WriteLine($"  Degree digits: {degreeDigits}");
Console.WriteLine($"  Degrees: {degrees}");
Console.WriteLine($"  Minutes: {minutes}");
Console.WriteLine($"  Result: {lat}");
Console.WriteLine();

// Parse longitude
string lonValue = parts[4];
string lonDir = parts[5];
isLatitude = (lonDir == "N" || lonDir == "S");
degreeDigits = isLatitude ? 2 : 3;

degrees = int.Parse(lonValue.Substring(0, degreeDigits));
minutes = double.Parse(lonValue.Substring(degreeDigits), CultureInfo.InvariantCulture);
double lon = degrees + (minutes / 60.0);
if (lonDir == "W") lon = -lon;

Console.WriteLine($"Longitude parsing:");
Console.WriteLine($"  Degree digits: {degreeDigits}");
Console.WriteLine($"  Degrees: {degrees}");
Console.WriteLine($"  Minutes: {minutes}");
Console.WriteLine($"  Result: {lon}");
Console.WriteLine();

Console.WriteLine($"Final coordinates: {lat}, {lon}");
Console.WriteLine($"Google Maps: https://www.google.com/maps?q={lat},{lon}");
