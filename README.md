# DynDNSV2CloudflareProxy (Work in progress)
The Project is still Work in Progress. Once finished this will be removed...<br><br>
Accepts DynDnsV2 Updates and redirects them to Cloudflare. 
Uses [NanoHTTP](https://github.com/NanoHttpd/nanohttpd) with maven for hosting the server.

## Usage
Compile the program or download the [FAT-JAR](#fat-jar)/ [JAR with dependencies](#jar-with-dependencies) from [Releases](#releases).

Define a config and log file with the `` CONFIG_PATH`` und ``LOG_PATH`` environment variables 
or let them generate at ``./proxy.conf`` and ``./dyndnsproxy.log`` by running the program once.
The ``config-file`` should look something like this:
````lombok.config
BasicAuth_active=false
BasicAuth_password=
BasicAuth_username=
HTTPS_active=false
HTTPS_keyStoreFilePath=
HTTPS_keyStorePassphrase=
HTTPS_port=443
HTTP_active=false
HTTP_port=80
IPAddress=127.0.0.1
````


Where

| Option                       | Description                                                                                                                                                                                             |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ``BasicAuth_active``         | ``true``: use the 'Basic' authentication method.<br/>``false``: dont authenticate (only use in trusted networks)                                                                                        |
| ``BasicAuth_password``       | password for authentication                                                                                                                                                                             |
| ``BasicAuth_username``       | username for authentication                                                                                                                                                                             |
| ``HTTPS_active``             | ``true``: run HTTPS server on port ``HTTPS_port``                                                                                                                                                       |
| ``HTTPS_keyStoreFilePath``   | Filepath to keystore-file, containing a certificate and private key. <br/>Depending on your setup a self-signed Certificate may be enough. <br/>(ex. ``D:\\test\\cert.jks`` or ``/etc/certs/cert.jks``) |
| ``HTTPS_keyStorePassphrase`` | Passphrase for keystore file                                                                                                                                                                            |
| ``HTTPS_port``               | Port on which the HTTPS server will listen                                                                                                                                                              |
| ``HTTP_active``              | ``true``: run HTTP server on port ``HTTP_port`` (be careful with authentication credentials)                                                                                                            |
| ``HTTP_port``                | Port on which the HTTPS server will listen                                                                                                                                                              |
| ``IPAddress``                | IP-Address the HTTP or HTTPS server is listening on.<br/>Use ``0.0.0.0`` or so if it should be accessible from outside.                                                                                 |

Both ``HTTP`` and ``HTTPS`` may be used at the same time if needed.

### Docker
Docker-Integration should be relative straight forward. <br>
The ``Dockerfile`` and ``docker-compose.yml`` contain an example using the ``openjdk:8-alpine`` base image and ports ``5000`` and ``5001`` in the config.

TODO: update before release to a working example


## Supported Features
Supports Raw HTTP Requests as specified in https://help.dyn.com/remote-access-api/perform-update/. <br>
Returns Return-Codes as specified in https://help.dyn.com/remote-access-api/return-codes/.
### Differences and Missing Features
(will probably not be implemented, unless I need them at some point)
- ``offline`` Update Parameter is ignored
- URL-Authentication is not supported
- Accepts every URI (Header, ex. /test/bla)
- Accepts all Request-Types (GET, POST, ...)
- Does not validate Host (Header)

## Releases
None so far lol
### Fat JAR

### JAR with dependencies
