#!/usr/bin/env python3
"""
WiFi File Receiver Server for Dahua NMEA App
Receives video and NMEA files transferred from Android device via WiFi
"""

import socket
import os
import sys
from datetime import datetime

SERVER_PORT = 8888
SAVE_FOLDER = "received_files"
BUFFER_SIZE = 8192

def main():
    # Create save folder
    os.makedirs(SAVE_FOLDER, exist_ok=True)
    
    # Get local IP
    try:
        hostname = socket.gethostname()
        local_ip = socket.gethostbyname(hostname)
    except:
        local_ip = "unknown"
    
    # Create server socket
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    try:
        server.bind(('0.0.0.0', SERVER_PORT))
        server.listen(5)
        
        print("=" * 60)
        print("Dahua NMEA WiFi File Receiver")
        print("=" * 60)
        print(f"Server IP: {local_ip}")
        print(f"Server Port: {SERVER_PORT}")
        print(f"Save Folder: {os.path.abspath(SAVE_FOLDER)}")
        print("=" * 60)
        print("Waiting for connections from Android device...")
        print("Press Ctrl+C to stop")
        print("=" * 60)
        print()
        
        file_count = 0
        
        while True:
            try:
                # Accept connection
                client, addr = server.accept()
                client_ip, client_port = addr
                
                timestamp = datetime.now().strftime("%H:%M:%S")
                print(f"[{timestamp}] Connection from {client_ip}:{client_port}")
                
                try:
                    # Receive header byte-by-byte until newline (filename|size\n)
                    header = b""
                    while True:
                        byte = client.recv(1)
                        if not byte:
                            raise Exception("Connection closed while reading header")
                        if byte == b'\n':
                            break
                        header += byte
                    
                    # Parse header: filename|size
                    header_str = header.decode('utf-8').strip()
                    
                    if "|" not in header_str:
                        print(f"[{timestamp}] Invalid header format (no '|'): {header_str[:100]}")
                        client.close()
                        continue
                    
                    filename, filesize_str = header_str.split('|', 1)
                    try:
                        filesize = int(filesize_str)
                    except ValueError:
                        print(f"[{timestamp}] Invalid file size: '{filesize_str[:100]}'")
                        client.close()
                        continue
                    
                    print(f"[{timestamp}] Receiving: {filename}")
                    print(f"[{timestamp}] Size: {filesize:,} bytes ({filesize / (1024*1024):.2f} MB)")
                    
                    # Receive file
                    filepath = os.path.join(SAVE_FOLDER, filename)
                    
                    with open(filepath, 'wb') as f:
                        received = 0
                        last_percent = -1
                        
                        while received < filesize:
                            remaining = filesize - received
                            chunk_size = min(BUFFER_SIZE, remaining)
                            data = client.recv(chunk_size)
                            
                            if not data:
                                break
                            
                            f.write(data)
                            received += len(data)
                            
                            # Show progress
                            percent = int((received / filesize) * 100)
                            if percent != last_percent and percent % 10 == 0:
                                print(f"[{timestamp}] Progress: {percent}% ({received:,}/{filesize:,} bytes)")
                                last_percent = percent
                    
                    file_count += 1
                    print(f"[{timestamp}] ✓ Saved: {filepath}")
                    print(f"[{timestamp}] Total files received: {file_count}")
                    print()
                    
                except Exception as e:
                    print(f"[{timestamp}] Error receiving file: {e}")
                
                finally:
                    client.close()
                    
            except KeyboardInterrupt:
                print("\n\nServer stopped by user")
                break
            except Exception as e:
                print(f"Connection error: {e}")
                continue
    
    except OSError as e:
        if e.errno == 10048:  # Address already in use
            print(f"\nError: Port {SERVER_PORT} is already in use!")
            print("Please close other applications using this port or change SERVER_PORT")
        else:
            print(f"\nError starting server: {e}")
        sys.exit(1)
    
    finally:
        server.close()
        print(f"\nServer closed. Total files received: {file_count}")
        if file_count > 0:
            print(f"Files saved to: {os.path.abspath(SAVE_FOLDER)}")

if __name__ == "__main__":
    main()
