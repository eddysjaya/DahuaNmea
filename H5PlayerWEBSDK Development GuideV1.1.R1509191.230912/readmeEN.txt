Before using websdk, read H5PlayerWEBSDK Development Guide.pdf in the doc directory. Pay attention to the environment configuration. Check whether the device supports no plug-ins. Refer to section 1.2 [Supported Devices] in this file.

================== Service Mount ========================
For the APIs such as PTZ, (see H5PlayerWEBSDK Development Guide.pdf in the doc directory), you need to use the proxy function of the http server. Follow these steps.
1. Change the IP address of the device to which you have logged in. Edit from row 15 of webs/index.html.
2. Open nginx-1.10.2 and double-click start.bat to start the service.
3. Enter http://127.0.0.1/index.html.
4. Open nginx-1.10.2 and double-click stop.bat to stop the service.
After starting the service, you can access index.html.
For more information about how to call the APIs, see the method in index.js embedded in index.html.

If you do not need PTZ and only need to pull streams and capture images. You can place webs in any http server (for example, tomcat or iis).
For more information about the call process, see the simple demo (indexsimple.html). The modified IP address is located in lines 46 and 47.


===================File Description =========================
Folder description:	
doc		     Storage directory of related files
	nginx-1.10.2	 Directory of web server
	webs		 Demo directory

================== Precautions =========================

1. Q: If the development environment is react, vue or other frameworks, how to load the WEB SDK?
A: You cannot use the import method to load data. You need to use the script method to mount the playerControl class under under window. For example,
		<script src="module/playerControl.js"></script>
2. Q: Failed to pull the stream and video image cannot be displayed.
A: Edit line 15 of webs/index.html and change the IP address, port number, username and password. Generally, the default port for pulling streams is 80 instead of 37777.
3. Q: The system prompts a login failure.
A: Make sure that the username and password are correct, and the WEB SDK without plug-ins run on the http server.
	If you double-click index.html in the resource manager and open it in the browser, the login fails. For more information, see [Service Mount].
4.. The browser prompts errors like WebSocket connection to 'ws://192.168.1.105/' failed: Error in connection.
A: Check whether the network connection of the IP address is available, whether the port is correct and whether the device supports no plug-ins. Refer to section 1.2 [Support Devices] of H5PlayerWEBSDK Development Guide.pdf in the doc directory.
		


