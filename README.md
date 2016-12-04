# Cps 706 Final Project

A simple content distribution network.

## Group #30 Members
- Frank Vumbaca
- Mitchell Mohorovich
- Yan Jedrasik

## Usage

### Compiling

- To compile the project, simply run `javac -d PROJECT_ROOT *.java` on the src directory, where `PROJECT_ROOT` can be an explicit, or relative path to the project root folder. This is so that the correct configuration assets are available at runtime.

### Running
The application contains multiple sub-commands for each component of the project. After compiling, you can run each sub-application as follows:

- ```java Main client LOCAL_DNS_IP```

  > Where `LOCAL_DNS_IP` is the IP address of the local DNS server
- ```java Main dns config_dns.txt```
  
  > Where `config_dns.txt` is a file describing the initial DNS records for the server
- ```java Main web [index.txt[F1.mp4[...]]]```
  
  > Where the passed in files make up the available content (index.txt is used for all requests to `/`)

**NOTE:** Default port configurations are loaded from `config.txt`. All outgoing requests are sent to the default ports.

#### To Run The demo, Run These Commands

- *machine 1 (hercdn.ca)*
 - `java Main web F1.mp4 F2.mp4 F3.mp4 F4.mp4 F5.mp4`
 - `java Main dns herdns.txt`
- *machine 2 (hiscdn.ca)*
 - `java Main web index.txt`
 - `java Main dns hisdns.txt`
- *machine 3 (local DNS)*
 - `java Main dns localdns.txt`
- *machine 4 (client)*
 - `java Main client LOCAL_DNS_IP`
   
   > where `LOCAL_DNS_IP` is the IP of the local DNS machine

### Configuration Files
Pre-configured files for the demo:

- `config.txt` ***must be consistent across all connected instances - this defines the protocol ports**

 ```
 web 40295
 dns 40296
 ```
- `localdns.txt`
 
  ```
  hiscinema.com A 141.117.232.30
  www.hiscinema.com CNAME hiscinema.com
  hercdn.ca A 141.117.232.29
  www.hercdn.ca CNAME hercdn.ca
  hiscinema.com NS 141.117.232.30
  hercdn.ca NS 141.117.232.29
  ```
- `hisdns.txt`

 ```
 video.hercdn.ca V hercdn.ca
 ```
- `herdns.txt	`

  ```
  video.hercdn.ca V hercdn.ca
  ```
  