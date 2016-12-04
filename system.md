# Cps 706 - Final Assignment

## Group #30 Members
- Frank Vumbaca
- Mitchell Mohorovich
- Yan Jedrasik

## Application Overview
The application we developed provides a cli to launch different sub-applications:

- **client** to make requests and ultimately download a video file.
- **dns servers** to resolve host names.
- **web servers** to host content to be viewed/downloaded.

For each computer in the setup, it will run one or more of these sub-applications with a configuration.
> For usage information, refer to the `README.md` file

## The Application Instances
Each machine has a slightly different configuration, but the whole system revolves around the same three sub-applications. Below are the specific configurations per machine.

> **NOTE:** All ports for the following applications are automatically read from `config.txt` at runtime. These ports are assumed to be consistent across all communicating servers as it defines the 'protocol ports' for our system.

### Client
The client is a simple terminal interface that will make requests to our Web server applications. 
When the client is given a hostname that is not an IP, it will query the local DNS server for the ip.

The client can be run like this:
<center>

```java Main client 141.117.232.X```

***Replace X with the matching IP for the local DNS machine**
</center>

### Local DNS
The local DNS runs on its own computer. To run the server run the command:

<center>

```java Main dns localdns.txt```
</center>

The DNS record table for the local DNS contains:
<center>

|        Key        |   Type   |     Value     |
|:-----------------:|:--------:|:-------------:|
|   hiscinema.com   |     A    | 141.117.232.X |
| www.hiscinema.com |   CNAME  | hiscinema.com |
|   hiscinema.com   |     NS   | 141.117.232.X |
|     hercdn.ca     |     A    | 141.117.232.Y |
|   www.hercdn.ca   |   CNAME  |   hercdn.ca   |
|     hercdn.ca     |     NS   | 141.117.232.Y |

***Replace X and Y with the matching IPs for hiscinema.com and hercdn.ca respectivly**
</center>

### hiscinema.com
Runs both a DNS and web server. This host provides a web page to 

- Web Server is configured and run like so:

	<center>```java Main web index.txt```</center>
	> `index.txt` is the only content of this web server:

- The DNS server is configured and run like so:
	
	<center>```java Main dns hisdns.txt```</center>
	> `hisdns.txt` provides the initial DNS records for *hiscinema.com*

<center>

|         Key         |    Type  |      Value      | 
|:-------------------:|:--------:|:---------------:|
| video.hiscinema.com |     V    | video.hercdn.ca |
</center>
### hercdn.ca
Runs both a DNS and web server. The web server serves video content. The available video files are `F1.mp4`, `F2.mp4`, `F3.mp4`, `F4.mp4`, `F5.mp4`.

- Web Server is configured and run like so:

	<center>```java Main web F1.mp4 F2.mp4 F3.mp4 F4.mp4 F5.mp4```</center>
	> This web server server serves all the video files

- The DNS server is configured and run like so:
	
	<center>```java Main dns herdns.txt```</center>
	> `hisdns.txt` provides the initial DNS records for *hiscinema.com*

<center>

|        Key       |  Type |   Value   | 
|:----------------:|:-----:|:---------:|
| video.hercdn.com | CNAME | hercdn.ca |
</center>

